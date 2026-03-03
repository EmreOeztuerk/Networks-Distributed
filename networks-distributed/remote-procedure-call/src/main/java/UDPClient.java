import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * UDP-Client fuer Versand und Empfang von JSON-Nachrichten.
 *
 * @author Emre Öztürk
 * @date 04.11.2025
 */
public class UDPClient implements AutoCloseable {
    /**
     * UDP-Socket des Clients.
     */
    private DatagramSocket socket;
    /**
     * Server-Adresse.
     */
    private InetAddress serverAddress;
    /**
     * Server-Port.
     */
    private int port;

    /**
     * Erstellt einen UDP-Client fuer die Zieladresse.
     *
     * @param ip_address IP-Adresse des Servers
     * @param port Port des Servers
     * @throws IOException bei Fehlern bei Socket- oder Address-Erstellung
     */
    public UDPClient(String ip_address, int port) throws IOException {
        this.socket = new DatagramSocket();
        this.serverAddress = InetAddress.getByName(ip_address);
        this.port = port;  // FIXED: Store the port parameter
    }

    /**
     * Sendet eine Nachricht an den Server.
     *
     * @param message zu sendender Inhalt
     * @throws IOException bei Netzwerkfehlern
     */
    public void send(String message) throws IOException {
        byte[] sendData = message.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, port);
        socket.send(sendPacket);
    }

    /**
     * Empfaengt eine Antwort vom Server.
     *
     * @return empfangener Nachrichteninhalt
     * @throws IOException bei Netzwerkfehlern
     */
    public String receive() throws IOException {
        byte[] receive_buffer = new byte[1024];
        DatagramPacket receive_packet = new DatagramPacket(receive_buffer, receive_buffer.length);
        //socket.setSoTimeout(5000);
        socket.receive(receive_packet);
        String response = new String(receive_packet.getData(), 0, receive_packet.getLength());
        return response;
    }

    /**
     * Schliesst den Client-Socket, falls geoeffnet.
     */
    @Override
    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}

