package protocol;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.ByteOrder;
import java.util.zip.CRC32;
import com.google.gson.*;

public class Packet {

    private int preHeader; // Length of the message (Header + Payload)
    private Header header;
    private JsonObject payload;

    public Packet(int type, String senderAddress, int senderPort, String targetAddress, int targetPort, int ttl, JsonObject payload) {
        this.header = new Header(type, senderAddress, senderPort, targetAddress, targetPort, ttl);
        this.payload = payload;
        this.preHeader = calculatePreHeader();
        this.header.setChecksum(calculateChecksum());
    }

    private int calculatePreHeader() {
        JsonObject packet = new JsonObject();
        packet.add("Header", header.toJson());
        if (payload != null) {
            packet.add("Payload", payload);
        }
        return packet.toString().getBytes(StandardCharsets.UTF_8).length;
    }

    private long calculateChecksum() {
        CRC32 crc = new CRC32();
        crc.update(header.toJson().toString().getBytes(StandardCharsets.UTF_8));
        if (payload != null) {
            crc.update(payload.toString().getBytes(StandardCharsets.UTF_8));
        }
        return crc.getValue();
    }

    public byte[] toBytes() {
        JsonObject packet = new JsonObject();
        packet.add("Header", header.toJson());
        if (payload != null) {
            packet.add("Payload", payload);
        }

        byte[] packetBytes = packet.toString().getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(2 + packetBytes.length); // Pre-Header (2 bytes) + Packet bytes
        buffer.order(ByteOrder.BIG_ENDIAN); // Ensure consistent byte order
        buffer.putShort((short) preHeader);
        buffer.put(packetBytes);
        return buffer.array();
    }

    public static Packet fromBytes(byte[] data) throws PacketDeserializationException {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.BIG_ENDIAN); // Ensure consistent byte order

        try {
            int preHeader = buffer.getShort();

            byte[] packetBytes = new byte[preHeader];
            buffer.get(packetBytes);
            String packetJson = new String(packetBytes, StandardCharsets.UTF_8);

            JsonObject packetObject = JsonParser.parseString(packetJson).getAsJsonObject();
            JsonObject headerJson = packetObject.getAsJsonObject("Header");
            JsonObject payloadJson = packetObject.has("Payload") ? packetObject.getAsJsonObject("Payload") : null;

            Header header = Header.fromJson(headerJson);

            Packet packet = new Packet(header.getType(), header.getSenderAddress(), header.getSenderPort(),
                    header.getTargetAddress(), header.getTargetPort(), header.getTtl(), payloadJson);

            if (packet.header.getChecksum() != packet.calculateChecksum()) {
                throw new PacketDeserializationException("Invalid checksum");
            }
            return packet;
        } catch (Exception e) {
            throw new PacketDeserializationException("Failed to deserialize packet", e);
        }
    }

    public Header getHeader() {
        return header;
    }

    public JsonObject getPayload() {
        return payload;
    }

    public static class PacketDeserializationException extends Exception {
        public PacketDeserializationException(String message) {
            super(message);
        }

        public PacketDeserializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}