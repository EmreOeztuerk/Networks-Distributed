package protocol;

import com.google.gson.*;

import java.util.zip.CRC32;

/**
 * Represents a standard message within the protocol.
 * Contains both routing headers and the actual data payload.
 */
public class Message {

    private int type;
    private String senderAddress;
    private int senderPort;
    private String targetAddress;
    private int targetPort;
    private int ttl;
    private long checksum;
    /** The data payload stored in JSON format. */
    private JsonObject payload;

    /**
     * Creates a new Message object.
     * @param type          The message type (e.g., chat, routing).
     * @param senderAddress IP address of the origin.
     * @param senderPort    Port of the origin.
     * @param targetAddress IP address of the destination.
     * @param targetPort    Port of the destination.
     * @param ttl           Time-To-Live for the packet.
     * @param payload       The JSON payload containing specific message data.
     *
     */
    public Message(int type, String senderAddress, int senderPort, String targetAddress, int targetPort, int ttl, JsonObject payload) {
        this.type = type;
        this.senderAddress = senderAddress;
        this.senderPort = senderPort;
        this.targetAddress = targetAddress;
        this.targetPort = targetPort;
        this.ttl = ttl;
        this.payload = payload;
        this.checksum = calculateChecksum();
    }

    public int getType() {
        return type;
    }
    public String getSenderAddress() {
        return senderAddress;
    }
    public int getSenderPort() {
        return senderPort;
    }
    public String getTargetAddress() {
        return targetAddress;
    }
    public int getTargetPort() {
        return targetPort;
    }
    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
        this.checksum = calculateChecksum(); // Recalculate checksum when TTL changes
    }

    public long getChecksum() {
        return checksum;
    }

    public JsonObject getPayload() {
        return payload;
    }

    public void setPayload(JsonObject payload) {
        this.payload = payload;
        this.checksum = calculateChecksum(); // Recalculate checksum when payload changes
    }

    /**
     * Calculates a CRC32 checksum for the message integrity.
     * @return The calculated checksum.
     */
    private long calculateChecksum() {
        JsonObject json = toJson();
        json.remove("Checksum"); // Exclude checksum field during calculation
        String data = json.toString();
        CRC32 crc = new CRC32();
        crc.update(data.getBytes());
        return crc.getValue();
    }

    public boolean isChecksumValid() {
        return calculateChecksum() == this.checksum;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("Type", type);
        json.addProperty("SenderAddress", senderAddress);
        json.addProperty("SenderPort", senderPort);
        json.addProperty("TargetAddress", targetAddress);
        json.addProperty("TargetPort", targetPort);
        json.addProperty("TTL", ttl);
        json.addProperty("Checksum", checksum);
        if (payload != null) {
            json.add("Payload", payload);
        }
        return json;
    }

    public static Message createChatMessage(String senderAddress, int senderPort, String targetAddress, int targetPort, String msg) {
        JsonObject payload = new JsonObject();
        payload.addProperty("Msg", msg);
        return new Message(0, senderAddress, senderPort, targetAddress, targetPort, 13, payload);
    }

    public static Message createHelloMessage(String senderAddress, int senderPort, String targetAddress, int targetPort) {
        return new Message(1, senderAddress, senderPort, targetAddress, targetPort, 13, null);
    }

    public static Message createRoutingInfoMessage(String senderAddress, int senderPort, String targetAddress, int targetPort, JsonObject routingEntries) {
        JsonObject payload = new JsonObject();
        payload.add("Routes", routingEntries);
        return new Message(2, senderAddress, senderPort, targetAddress, targetPort, 13, payload);
    }

    public static Message createGoodbyeMessage(String senderAddress, int senderPort, String targetAddress, int targetPort) {
        return new Message(3, senderAddress, senderPort, targetAddress, targetPort, 13, null);
    }

    public static Message fromJson(JsonObject json) {
        try {
            int type = json.has("Type") ? json.get("Type").getAsInt() : -1;
            String senderAddress = json.has("SenderAddress") ? json.get("SenderAddress").getAsString() : null;
            int senderPort = json.has("SenderPort") ? json.get("SenderPort").getAsInt() : -1;
            String targetAddress = json.has("TargetAddress") ? json.get("TargetAddress").getAsString() : null;
            int targetPort = json.has("TargetPort") ? json.get("TargetPort").getAsInt() : -1;
            int ttl = json.has("TTL") ? json.get("TTL").getAsInt() : -1;
            long checksum = json.has("Checksum") ? json.get("Checksum").getAsLong() : 0;
            JsonObject payload = json.has("Payload") ? json.getAsJsonObject("Payload") : null;

            if (type == -1 || senderAddress == null || senderPort == -1 || targetAddress == null || targetPort == -1 || ttl == -1) {
                throw new IllegalArgumentException("Missing required fields in JSON: " + json);
            }

            Message message = new Message(type, senderAddress, senderPort, targetAddress, targetPort, ttl, payload);
            message.checksum = checksum; // Set checksum directly to avoid recalculation
            return message;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse Message from JSON: " + json, e);
        }
    }

    @Override
    public String toString() {
        return toJson().toString();
    }
}
