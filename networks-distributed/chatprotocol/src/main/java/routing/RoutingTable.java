package routing;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the routing information for the local node.
 * It provides mechanisms to update, retrieve, and manage routes to other nodes in the network.
 */
public class RoutingTable {
    /** Constant representing a destination that is considered unreachable. */
    static final int UNREACHABLE_HOP_COUNT = 13;

    /** Thread-safe storage for routing entries. */
    private final Map<String, RoutingEntry> table = new ConcurrentHashMap<>();
    /** The local IP address of this node. */
    private final String localAddress;
    /** The local port of this node. */
    private final int localPort;

    /**
     * Initializes a new routing table for the specified local address and port.
     * @param localAddress The IP address of the current node.
     * @param localPort    The port of the current node.
     */
    public RoutingTable(String localAddress, int localPort) {
        this.localAddress = localAddress;
        this.localPort = localPort;
    }

    /**
     * Updates an entry in the routing table based on received routing information.
     * @param targetAddress  The target node's IP.
     * @param targetPort     The target node's port.
     * @param hopCount       The distance reported by the neighbor.
     * @param nextHopAddress The IP of the neighbor providing the update.
     * @param nextHopPort    The port of the neighbor providing the update.
     * @return true if the table was modified, false otherwise.
     *
     */
    public synchronized boolean updateEntry(String targetAddress, int targetPort, int hopCount, String nextHopAddress, int nextHopPort) {
        String key = generateKey(targetAddress, targetPort);
        RoutingEntry currentEntry = table.get(key);

        if (currentEntry == null || hopCount < currentEntry.getHopCount()) {
            table.put(key, new RoutingEntry(targetAddress, targetPort, hopCount, nextHopAddress, nextHopPort));
            return true; // New entry or better route
        } else if (currentEntry.getNextHopAddress().equals(nextHopAddress) && currentEntry.getNextHopPort() == nextHopPort) {
            currentEntry.refreshTimestamp();
            return false; // No new entry, only timestamps updated
        }
        return false; // No changes
    }

    public synchronized void markAsUnreachable(String targetAddress, int targetPort) {
        String key = generateKey(targetAddress, targetPort);
        RoutingEntry entry = table.get(key);
        if (entry != null) {
            entry.setHopCount(UNREACHABLE_HOP_COUNT); // Mark as unreachable
        }
    }

    public synchronized List<RoutingEntry> generateRoutingUpdate(String excludeAddress, int excludePort) {
        Map<String, RoutingEntry> shortestPaths = new HashMap<>();

        // Calculate shortest paths to each endpoint
        for (RoutingEntry entry : table.values()) {
            String key = generateKey(entry.getTargetAddress(), entry.getTargetPort());
            if (!shortestPaths.containsKey(key) || entry.getHopCount() < shortestPaths.get(key).getHopCount()) {
                shortestPaths.put(key, entry);
            }
        }

        // Split Horizon: Remove entries that come from the neighbor themselves
        List<RoutingEntry> updates = new ArrayList<>();
        for (RoutingEntry entry : shortestPaths.values()) {
            if (!entry.getNextHopAddress().equals(excludeAddress) || entry.getNextHopPort() != excludePort) {
                updates.add(new RoutingEntry(entry.getTargetAddress(), entry.getTargetPort(),
                        entry.getHopCount(), localAddress, localPort));
            }
        }
        return updates;
    }

    public void cleanupStaleEntries() {
        long currentTime = System.currentTimeMillis();
        List<String> staleKeys = new ArrayList<>();

        for (Map.Entry<String, RoutingEntry> entry : table.entrySet()) {
            if (entry.getValue().isStale(currentTime)) {
                staleKeys.add(entry.getKey());
                System.out.println("Stale entry detected: " + entry.getKey());
            }
        }

        for (String key : staleKeys) {
            table.remove(key);
            System.out.println("Removed stale routing entry: " + key);
        }
    }

    public synchronized RoutingEntry getNextHop(String targetAddress, int targetPort) {
        String key = generateKey(targetAddress, targetPort);
        return table.get(key);
    }

    public synchronized void printNeighborsIfUpdated() {
        boolean updated = false;

        for (RoutingEntry entry : table.values()) {
            if (entry.getHopCount() == 1 && entry.isNew()) {
                if (!updated) {
                    System.out.println("Neighbors identified:");
                    updated = true;
                }
                System.out.println(entry.getTargetAddress() + ":" + entry.getTargetPort());
                entry.setNew(false); // Mark the entry as processed
            }
        }
    }

    public List<RoutingEntry> getNeighbors() {
        List<RoutingEntry> neighbors = new ArrayList<>();
        synchronized (table) {
            for (RoutingEntry entry : table.values()) {
                if (entry.getHopCount() == 1) { // Direct neighbors have HopCount == 1
                    neighbors.add(entry);
                }
            }
        }
        return neighbors;
    }

    public Map<String, RoutingEntry> getAllEntries() {
        return new HashMap<>(table);
    }

    public RoutingEntry getEntry(String targetAddress, int targetPort) {
        String key = generateKey(targetAddress, targetPort);
        synchronized (table) {
            return table.get(key);
        }
    }

    /**
     * Generates a unique key for the internal map.
     * @param address IP address.
     * @param port Port number.
     * @return A unique identifier string.
     */
    private String generateKey(String address, int port) {
        return address.replaceFirst("^/+", "") + ":" + port;
    }
}
