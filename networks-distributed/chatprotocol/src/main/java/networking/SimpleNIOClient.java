package networking;

import com.google.gson.*;
import protocol.Message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * A client component using Java NIO to connect to other nodes.
 * Used to initiate connections and send chat or protocol messages.
 */
public class SimpleNIOClient {

    private String serverAddress;
    private int serverPort;
    private Selector selector;
    private SocketChannel clientChannel;
    private int listenerPort;

    /**
     * Initializes the client with target server details.
     * @param serverAddress The target server IP.
     * @param serverPort    The target server port.
     */
    public SimpleNIOClient(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    /**
     * Connects to the server and starts a background thread to listen for incoming data.
     */
    public void start() {
        try {
            selector = Selector.open();
            clientChannel = SocketChannel.open();
            clientChannel.configureBlocking(false);
            clientChannel.connect(new InetSocketAddress(serverAddress, serverPort));
            clientChannel.register(selector, SelectionKey.OP_CONNECT);

            System.out.println("Client gestartet mit folgender IP: " + serverAddress + ":" + serverPort);

            while (true) {
                selector.select();
                for (SelectionKey key : selector.selectedKeys()) {
                    if (key.isConnectable()) {
                        completeConnection(key);
                    } else if (key.isReadable()) {
                        readMessage(key);
                    }
                }
                selector.selectedKeys().clear();
            }
        } catch (IOException e) {
            System.err.println("Error starting client: " + e.getMessage());
        }
    }

    private void completeConnection(SelectionKey key) {
        try {
            if (clientChannel.finishConnect()) {
                clientChannel.register(selector, SelectionKey.OP_READ);
                System.out.println("Erfolgreich mit dem Server verbunden.");

                // Start listener for incoming messages
                startListener();

                // Send a Hello message to the server
                sendHelloMessage();

                // Start a thread for regular routing updates
                new Thread(() -> {
                    while (true) {
                        try {
                            sendRoutingUpdate();
                            Thread.sleep(5000); // Every 5 seconds
                        } catch (InterruptedException e) {
                            System.err.println("Routing update thread interrupted: " + e.getMessage());
                            break;
                        }
                    }
                }).start();

                // Start an interactive chat session
                new Thread(this::startChatSession).start();
            }
        } catch (IOException e) {
            System.err.println("Error completing connection: " + e.getMessage());
            key.cancel();
        }
    }

    private void sendHelloMessage() {
        try {
            Message helloMessage = Message.createHelloMessage(
                    clientChannel.socket().getLocalAddress().toString().replaceFirst("^/", ""),
                    listenerPort, // Use listener port instead of socket port
                    serverAddress, serverPort);
            sendMessage(helloMessage);
        } catch (Exception e) {
            System.err.println("Error sending hello message: " + e.getMessage());
        }
    }

    private void startListener() {
        new Thread(() -> {
            try (ServerSocketChannel clientListener = ServerSocketChannel.open()) {
                clientListener.bind(new InetSocketAddress(0)); // Automatically select free port
                listenerPort = ((InetSocketAddress) clientListener.getLocalAddress()).getPort();
                System.out.println("Client lauscht auf dem Port: " + listenerPort);

                while (true) {
                    SocketChannel incomingConnection = clientListener.accept();
                    handleIncomingMessage(incomingConnection);
                }
            } catch (IOException e) {
                System.err.println("Error in client listener: " + e.getMessage());
            }
        }).start();
    }

    private void handleIncomingMessage(SocketChannel incomingConnection) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int bytesRead = incomingConnection.read(buffer);

            if (bytesRead == -1) {
                System.out.println("Connection closed by sender.");
                incomingConnection.close();
                return;
            }

            buffer.flip();
            String receivedMessage = StandardCharsets.UTF_8.decode(buffer).toString();

            // Parse the message
            JsonObject message = JsonParser.parseString(receivedMessage).getAsJsonObject();
            int type = message.get("Type").getAsInt();

            // More user-friendly display for Type 0 messages
            if (type == 0) {
                JsonObject payload = message.getAsJsonObject("Payload");
                String msgContent = payload.get("Msg").getAsString();
                String senderAddress = message.get("SenderAddress").getAsString();
                int senderPort = message.get("SenderPort").getAsInt();
                System.out.println("Chatnachricht erhalten von " + senderAddress + ":" + senderPort + " : " + msgContent);
            } else if (type != 2) {
                System.out.println("Message received: " + receivedMessage);
            }

            buffer.clear();
        } catch (IOException | JsonSyntaxException e) {
            System.err.println("Error reading incoming message: " + e.getMessage());
        }
    }

    private void startChatSession() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.println("Wähle zwischen folgenden Nachrichten: \n1: Chat\n2: Routinginfo\n3: Abmelden");
                int type = Integer.parseInt(scanner.nextLine());

                switch (type) {
                    case 1 -> sendChatMessage(scanner);
                    case 2 -> sendRoutingInfo(scanner);
                    case 3 -> sendGoodbyeMessage();
                    default -> System.out.println("Invalid selection. Try again.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error in chat session: " + e.getMessage());
        }
    }

    private void sendChatMessage(Scanner scanner) {
        try {
            System.out.print("Empfänger IP-Adresse: ");
            String targetAddress = scanner.nextLine().replaceFirst("^/", "");
            System.out.print("Empfänger Port: ");
            int targetPort = Integer.parseInt(scanner.nextLine());
            System.out.print("Nachricht: ");
            String messageContent = scanner.nextLine();

            Message chatMessage = Message.createChatMessage(
                    clientChannel.socket().getLocalAddress().toString().replaceFirst("^/", ""),
                    listenerPort, // Use the listener port
                    targetAddress, targetPort, messageContent);

            sendMessage(chatMessage);
        } catch (Exception e) {
            System.err.println("Error sending chat message: " + e.getMessage());
        }
    }

    private void sendRoutingInfo(Scanner scanner) {
        try {
            JsonObject routingInfo = new JsonObject();
            System.out.print("Enter number of routing entries: ");
            int numEntries = Integer.parseInt(scanner.nextLine());

            for (int i = 0; i < numEntries; i++) {
                JsonObject route = new JsonObject();
                System.out.print("Enter target address for route " + (i + 1) + ": ");
                route.addProperty("TargetAddress", scanner.nextLine().replaceFirst("^/", ""));
                System.out.print("Enter target port for route " + (i + 1) + ": ");
                route.addProperty("TargetPort", Integer.parseInt(scanner.nextLine()));
                System.out.print("Enter hop count for route " + (i + 1) + ": ");
                route.addProperty("HopCount", Integer.parseInt(scanner.nextLine()));
                routingInfo.add("Route" + i, route);
            }

            Message routingMessage = Message.createRoutingInfoMessage(
                    clientChannel.socket().getLocalAddress().toString().replaceFirst("^/", ""),
                    listenerPort, // Verwende den Listener-Port
                    serverAddress, serverPort, routingInfo);

            sendMessage(routingMessage);
        } catch (Exception e) {
            System.err.println("Error sending routing info: " + e.getMessage());
        }
    }

    private void sendRoutingUpdate() {
        JsonObject routingEntries = new JsonObject();

        // Beispiel: Füge aktuelle Routing-Daten hinzu
        JsonObject entry = new JsonObject();
        entry.addProperty("TargetAddress", clientChannel.socket().getLocalAddress().toString().replaceFirst("^/", ""));
        entry.addProperty("TargetPort", listenerPort);
        entry.addProperty("HopCount", 1);
        routingEntries.add("Route0", entry);

        Message routingUpdate = Message.createRoutingInfoMessage(
                clientChannel.socket().getLocalAddress().toString().replaceFirst("^/", ""),
                listenerPort,
                serverAddress,
                serverPort,
                routingEntries);

        sendMessage(routingUpdate);
    }

    private void sendGoodbyeMessage() {
        try {
            Message goodbyeMessage = Message.createGoodbyeMessage(
                    clientChannel.socket().getLocalAddress().toString().replaceFirst("^/", ""),
                    listenerPort, // Verwende den Listener-Port
                    serverAddress, serverPort);
            sendMessage(goodbyeMessage);
            closeClient();
        } catch (Exception e) {
            System.err.println("Error sending goodbye message: " + e.getMessage());
        }
    }

    private void sendMessage(Message message) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(message.toJson().toString().getBytes(StandardCharsets.UTF_8));
            clientChannel.write(buffer);
        } catch (IOException e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }

    private void readMessage(SelectionKey key) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int bytesRead = clientChannel.read(buffer);
            if (bytesRead == -1) {
                clientChannel.close();
                System.out.println("Server closed the connection.");
                return;
            }
            buffer.flip();
            String response = StandardCharsets.UTF_8.decode(buffer).toString();
            System.out.println("Server: " + response);
        } catch (IOException e) {
            System.err.println("Error reading from server: " + e.getMessage());
        }
    }

    private void closeClient() {
        try {
            System.out.println("Closing client...");
            if (clientChannel != null && clientChannel.isOpen()) {
                clientChannel.close();
            }
            if (selector != null && selector.isOpen()) {
                selector.close();
            }
            System.out.println("Client closed.");
        } catch (IOException e) {
            System.err.println("Error closing client: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Eingabe der Serveradresse: ");
        String serverAddress = scanner.nextLine();
        System.out.print("Eingabe des Serverports: ");
        int serverPort = Integer.parseInt(scanner.nextLine());

        SimpleNIOClient client = new SimpleNIOClient(serverAddress, serverPort);
        client.start();
    }
}