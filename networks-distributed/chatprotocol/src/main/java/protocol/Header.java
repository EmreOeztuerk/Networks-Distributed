package protocol;

import com.google.gson.JsonObject;

public class Header {
    private int type;
    private String senderAddress;
    private int senderPort;
    private String targetAddress;
    private int targetPort;
    private int ttl;
    private long checksum;

    public Header(int type, String senderAddress, int senderPort, String targetAddress, int targetPort, int ttl) {
        this.type = type;
        this.senderAddress = senderAddress;
        this.senderPort = senderPort;
        this.targetAddress = targetAddress;
        this.targetPort = targetPort;
        this.ttl = ttl;
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

    public void setChecksum(long checksum) {
        this.checksum = checksum;
    }

    public long getChecksum() {
        return checksum;
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
        return json;
    }

    public static Header fromJson(JsonObject json) {
        int type = json.get("Type").getAsInt();
        String senderAddress = json.get("SenderAddress").getAsString();
        int senderPort = json.get("SenderPort").getAsInt();
        String targetAddress = json.get("TargetAddress").getAsString();
        int targetPort = json.get("TargetPort").getAsInt();
        int ttl = json.get("TTL").getAsInt();
        long checksum = json.get("Checksum").getAsLong();

        Header header = new Header(type, senderAddress, senderPort, targetAddress, targetPort, ttl);
        header.setChecksum(checksum);
        return header;
    }
}