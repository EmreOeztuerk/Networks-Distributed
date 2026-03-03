import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * UDP-Server fuer Empfang von Requests und Versand von Responses.
 *
 * @author Emre Öztürk
 * @date 04.11.2025
 */
public class UDPServer implements AutoCloseable {
    /**
     * Gebundener UDP-Socket.
     */
    private final DatagramSocket socket;
    /**
     * Letzter empfangener Request inklusive Clientadresse.
     */
    private DatagramPacket lastRequest;

    /**
     * Erstellt und bindet den UDP-Server auf einen Port.
     *
     * @param port Zielport fuer eingehende Pakete
     * @throws IOException bei Fehlern beim Binden des Sockets
     */
    public UDPServer(int port) throws IOException {
        this.socket = new DatagramSocket(port);
        System.out.println("Server listening on port: " + port);
    }

    /**
     * Wartet auf ein eingehendes Paket und speichert Clientdaten fuer Antworten.
     *
     * @return empfangener Nachrichteninhalt
     * @throws IOException bei Netzwerkfehlern
     */
    public String listen() throws IOException {
        byte[] receive_buffer = new byte[1024];
        lastRequest = new DatagramPacket(receive_buffer, receive_buffer.length);
        socket.receive(lastRequest);

        String receivedMessage = new String(lastRequest.getData(), 0, lastRequest.getLength());

        System.out.println("Received from " + lastRequest.getAddress() + ":" + lastRequest.getPort() + " - "
                + receivedMessage);

        return receivedMessage;
    }

    /**
     * Sendet eine Antwort an den Client des zuletzt empfangenen Requests.
     *
     * @param message Antwortnachricht
     * @throws IOException bei Netzwerkfehlern
     * @throws IllegalStateException falls noch kein Request empfangen wurde
     */
    public void respond(String message) throws IOException {
        if (lastRequest == null) {
            throw new IllegalStateException("No client request received yet");
        }

        byte[] sendData = message.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, lastRequest.getAddress(),
                lastRequest.getPort());
        socket.send(sendPacket);

        System.out.println("Sent to " + lastRequest.getAddress() + ":" + lastRequest.getPort() + " - " + message);
    }

    /**
     * Sendet eine Nachricht an eine explizit angegebene Adresse.
     *
     * @param message Nachrichtentext
     * @param address Zieladresse
     * @param port Zielport
     * @throws IOException bei Netzwerkfehlern
     */
    public void sendTo(String message, InetAddress address, int port) throws IOException {
        byte[] sendData = message.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
        socket.send(sendPacket);
    }

    /**
     * Liefert die lokale IP-Adresse des Serversockets.
     *
     * @return lokale IP-Adresse als String
     */
    public String getServerIP() {
        return socket.getLocalAddress().getHostAddress();
    }

    /**
     * Schliesst den Server-Socket, falls geoeffnet.
     */
    @Override
    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
            System.out.println("Server socket closed");
        }
    }
}

