package routing;

/**
 * Represents a single entry in the routing table for the chat protocol.
 * It stores destination information, the next hop, and distance metrics (hop count).
 */
public class RoutingEntry {
    /** The IP address of the destination node. */
    private final String targetAddress;
    /** The port of the destination node. */
    private final int targetPort;
    /** The number of hops required to reach the destination. */
    private int hopCount;
    /** The IP address of the next node on the path to the destination. */
    private final String nextHopAddress;
    /** The port of the next hop node. */
    private final int nextHopPort;
    /** Timestamp of the last update for this route. */
    private long timestamp;
    /** Flag indicating if this entry was recently added. */
    private boolean isNew;

    /**
     * Constructs a new RoutingEntry.
     * @param targetAddress  The destination IP address.
     * @param targetPort     The destination port.
     * @param hopCount       The distance to the target in hops.
     * @param nextHopAddress The IP address of the intermediate node.
     * @param nextHopPort    The port of the intermediate node.
     *
     */
    public RoutingEntry(String targetAddress, int targetPort, int hopCount, String nextHopAddress, int nextHopPort) {
        this.targetAddress = targetAddress;
        this.targetPort = targetPort;
        this.hopCount = hopCount;
        this.nextHopAddress = nextHopAddress;
        this.nextHopPort = nextHopPort;
        this.timestamp = System.currentTimeMillis();
        this.isNew = true;
    }

    public String getTargetAddress() {
        return targetAddress;
    }
    public int getTargetPort() {
        return targetPort;
    }
    public int getHopCount() {
        return hopCount;
    }
    public void setHopCount(int hopCount) {
        this.hopCount = hopCount;
    }
    public String getNextHopAddress() {
        return nextHopAddress;
    }
    public int getNextHopPort() {
        return nextHopPort;
    }
    public boolean isStale(long currentTime) {
        return (currentTime - timestamp) > 10000; // Timeout after 10 seconds
    }
    public void refreshTimestamp() {
        this.timestamp = System.currentTimeMillis();
    }
    public boolean isNew() {
        return isNew;
    }
    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }
}

