package networking;

import com.google.gson.*;
import routing.RoutingEntry;
import routing.RoutingTable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A non-blocking I/O (NIO) server that handles incoming connections.
 * It manages routing updates and acts as a node in the chat network.
 */
public class SimpleNIOServer {

    private String serverAddress;
    /** The default port used for communication. */
    private final int port = 45054;
    private RoutingTable routingTable;
    private Selector selector;
    /** Executor for periodic tasks like sending routing table updates. */
    private ScheduledExecutorService scheduler;

    /**
     * Constructs the server node.
     * @param serverAddress The IP address to bind the server to.
     */
    public SimpleNIOServer(String serverAddress) {
        this.serverAddress = serverAddress;
        this.routingTable = new RoutingTable(serverAddress, port);
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    /**
     * Starts the server loop, accepting connections and processing network events.
     */
    public void start() {
        try (ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
            serverChannel.configureBlocking(false);
            serverChannel.socket().bind(new InetSocketAddress(serverAddress, port));
            selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Server is listening on: " + serverAddress + ":" + port);

            startAutomaticRoutingPropagation();

            while (true) {
                selector.select();
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (key.isAcceptable()) {
                        acceptConnection(serverChannel);
                    } else if (key.isReadable()) {
                        handleClient(key);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        }
    }

    private void acceptConnection(ServerSocketChannel serverChannel) throws IOException {
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));
        System.out.println("New client connected: " + clientChannel.getRemoteAddress());
    }

    private void handleClient(SelectionKey key) {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();

        try {
            int bytesRead = clientChannel.read(buffer);
            if (bytesRead == -1) {
                clientChannel.close();
                System.out.println("Client disconnected.");
                return;
            }

            buffer.flip();
            String messageJson = StandardCharsets.UTF_8.decode(buffer).toString();
            buffer.clear();

            try {
                JsonObject message = JsonParser.parseString(messageJson).getAsJsonObject();
                MessageHandler.processMessage(message, clientChannel);
            } catch (JsonSyntaxException e) {
                System.err.println("Invalid JSON received: " + messageJson);
            }

        } catch (IOException e) {
            System.err.println("Error reading from client: " + e.getMessage());
            try {
                clientChannel.close();
            } catch (IOException ex) {
                System.err.println("Error closing client channel: " + ex.getMessage());
            }
        }
    }

    private void startAutomaticRoutingPropagation() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                propagateRoutingTable();
                routingTable.cleanupStaleEntries();
            } catch (Exception e) {
                System.err.println("Error during routing propagation: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void propagateRoutingTable() {
        try {
            for (RoutingEntry neighbor : routingTable.getNeighbors()) {
                if (neighbor == null || neighbor.getNextHopAddress() == null) {
                    System.err.println("Skipping null neighbor in routing propagation.");
                    continue;
                }

                JsonObject routes = new JsonObject();

                int index = 0;
                for (RoutingEntry entry : routingTable.generateRoutingUpdate(neighbor.getNextHopAddress(), neighbor.getNextHopPort())) {
                    if (entry == null) {
                        System.err.println("Skipping null routing entry.");
                        continue;
                    }

                    JsonObject route = new JsonObject();
                    route.addProperty("TargetAddress", entry.getTargetAddress());
                    route.addProperty("TargetPort", entry.getTargetPort());
                    route.addProperty("HopCount", entry.getHopCount());
                    routes.add("Route" + index, route);
                    index++;
                }

                JsonObject payload = new JsonObject();
                payload.add("Routes", routes);

                JsonObject message = new JsonObject();
                message.addProperty("Type", 2);
                message.addProperty("SenderAddress", serverAddress);
                message.addProperty("SenderPort", port);
                message.addProperty("TTL", 13);
                message.add("Payload", payload);

                sendMessage(neighbor, message);
            }
        } catch (Exception e) {
            System.err.println("Error during routing propagation: " + e.getMessage());
        }
    }

    private void forwardMessage(JsonObject message) {
        try {
            String targetAddress = message.get("TargetAddress").getAsString();
            int targetPort = message.get("TargetPort").getAsInt();

            System.out.println("Attempting to forward message to: " + targetAddress + ":" + targetPort);

            int ttl = message.get("TTL").getAsInt() - 1;
            if (ttl <= 0) {
                System.out.println("TTL expired. Dropping message.");
                return;
            }
            message.addProperty("TTL", ttl);

            RoutingTable.RoutingEntry nextHop = routingTable.getEntry(targetAddress, targetPort);

            if (nextHop == null || nextHop.getHopCount() == RoutingTable.UNREACHABLE_HOP_COUNT) {
                System.out.println("No valid route to target: " + targetAddress + ":" + targetPort + ". Dropping packet.");
                printRoutingTable();
                return;
            }

            System.out.println("Forwarding to next hop: " + nextHop.getNextHopAddress() + ":" + nextHop.getNextHopPort());
            sendMessage(nextHop, message);
        } catch (Exception e) {
            System.err.println("Error forwarding message: " + e.getMessage());
        }
    }

    private void sendMessage(RoutingEntry nextHop, JsonObject message) {
        try (SocketChannel channel = SocketChannel.open()) {
            channel.configureBlocking(false);
            channel.connect(new InetSocketAddress(nextHop.getNextHopAddress().replaceFirst("^/+", ""), nextHop.getNextHopPort()));

            while (!channel.finishConnect()) {
                // Wait for the connection to complete
            }

            ByteBuffer buffer = ByteBuffer.wrap(message.toString().getBytes(StandardCharsets.UTF_8));
            channel.write(buffer);
            System.out.println("Message forwarded to: " + nextHop.getNextHopAddress() + ":" + nextHop.getNextHopPort());
        } catch (IOException e) {
            System.err.println("Error forwarding message to: " + nextHop.getNextHopAddress() + ":" + nextHop.getNextHopPort());
            System.err.println("Exception: " + e.getMessage());
        }
    }

    private void printRoutingTable() {
        System.out.println("Current Routing Table:");
        for (RoutingTable.RoutingEntry entry : routingTable.getNeighbors()) {
            System.out.println("Target: " + entry.getTargetAddress() + ":" + entry.getTargetPort() +
                    ", Next Hop: " + entry.getNextHopAddress() + ":" + entry.getNextHopPort() +
                    ", Hop Count: " + entry.getHopCount());
        }
    }

    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);

            System.out.println("Available network interfaces:");
            Map<Integer, String> interfaceMap = new HashMap<>();
            int index = 1;

            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                    Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress address = addresses.nextElement();
                        if (address instanceof InetAddress) {
                            System.out.println(index + ": " + networkInterface.getName() + " - " + address.getHostAddress());
                            interfaceMap.put(index, address.getHostAddress());
                            index++;
                        }
                    }
                }
            }

            if (interfaceMap.isEmpty()) {
                System.out.println("No active network interfaces found.");
                return;
            }

            System.out.print("Select an interface by number: ");
            int choice = Integer.parseInt(scanner.nextLine());
            if (!interfaceMap.containsKey(choice)) {
                System.out.println("Invalid choice. Exiting.");
                return;
            }

            String selectedAddress = interfaceMap.get(choice);
            System.out.println("Selected address: " + selectedAddress);

            networking.SimpleNIOServer server = new networking.SimpleNIOServer(selectedAddress);
            server.start();

        } catch (SocketException e) {
            System.err.println("Error retrieving network interfaces: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error initializing server: " + e.getMessage());
        }
    }
}

