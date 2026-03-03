package Messaging;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import com.google.gson.Gson;

import Misc.Log;

/**
 * Verwaltet die JSON-basierte Socket-Kommunikation zwischen den Komponenten.
 *
 * @author Emre Öztürk
 * @date 16.12.25
 */
public class COM implements AutoCloseable {
    private final ServerSocket listening_socket;
    private Socket clientSocket;
    private PrintWriter out = null;
    private BufferedReader in;
    private Semaphore pending_messages = new Semaphore(0);
    private Gson gson = new Gson();
    private int listening_port;

    private boolean isRunning = true;
    private Log logger = new Log("COM");
    private Queue<Message> message_queue = new ConcurrentLinkedQueue<>();
    private Semaphore token_semaphore = new Semaphore(0);

    /**
     * Startet COM auf einem festen Listening-Port.
     *
     * @param listening_port Port fuer eingehende Verbindungen
     * @throws IOException falls der ServerSocket nicht erstellt werden kann
     */
    public COM(int listening_port) throws IOException {
        this.listening_socket = new ServerSocket(listening_port);
        this.listening_port = listening_port;
        logger.info("COM listening on port: " + this.listening_port);
        enqueue_messages();
    }

    /**
     * Startet COM auf einem zufaelligen freien Port.
     *
     * @throws IOException falls der ServerSocket nicht erstellt werden kann
     */
    public COM() throws IOException {
        this.listening_socket = new ServerSocket(0);
        this.listening_port = listening_socket.getLocalPort();
        logger.info("COM listening on port: " + this.listening_port);
        enqueue_messages();
    }

    /**
     * Liefert den aktuellen Listening-Port.
     *
     * @return lokaler Port
     */
    public int getListeningPort() {
        return listening_port;
    }

    /**
     * Sendet eine Nachricht an die angegebene Zieladresse.
     *
     * @param message zu sendende Nachricht
     * @param address Zieladresse (IP oder Hostname)
     * @param port Zielport
     * @throws IOException falls Verbindungsaufbau oder Senden fehlschlaegt
     */
    public void sendTo(Message message, String address, int port) throws IOException {
        if (address.equals("localhost")) {
            address = "127.0.0.1";
        }
        InetAddress socket_address = InetAddress.getByName(address);
        Socket tempSocket = new Socket(socket_address, port);
        PrintWriter tempOut = new PrintWriter(tempSocket.getOutputStream(), true);
        message.payload.port = listening_port;
        tempOut.println(new Gson().toJson(message));
        tempOut.close();
        tempSocket.close();
    }

    /**
     * Wartet blockierend auf einen neuen Client und initialisiert den Reader.
     *
     * @throws IOException falls die Verbindung nicht angenommen werden kann
     */
    private void acceptClient() throws IOException {
        clientSocket = listening_socket.accept();
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    /**
     * Startet einen Hintergrund-Thread, der eingehende Nachrichten in Queues einreiht.
     */
    private void enqueue_messages() {
        new Thread(() -> {
            while (isRunning) {
                try {
                    acceptClient();
                    Message msg = get_next_message_internal();
                    if (msg.command == E_Query.TOKEN) {
                        token_semaphore.release();
                    } else {
                        message_queue.add(msg);
                        pending_messages.release();
                    }
                } catch (IOException | InterruptedException e) {
                    logger.info("Client disconnected, stopping message enqueuer.");
                }
            }
        }).start();
    }

    /**
     * Blockiert, bis ein TOKEN eingetroffen ist.
     *
     * @throws InterruptedException falls der wartende Thread unterbrochen wird
     */
    public void wait_for_token() throws InterruptedException {
        token_semaphore.acquire();
    }

    /**
     * Liefert die naechste verfuegbare Nachricht aus der Queue.
     *
     * @return naechste Nachricht
     * @throws InterruptedException falls der Aufruf unterbrochen wird
     */
    public Message get_next_message() throws InterruptedException {
        pending_messages.acquire();
        Message msg = message_queue.poll();
        if (msg == null) {
            throw new InterruptedException("Message queue returned null after acquiring semaphore.");
        }
        return msg;
    }

    /**
     * Liest eine Nachricht vom verbundenen Client und deserialisiert sie.
     *
     * @return gelesene Nachricht
     * @throws IOException falls die Verbindung getrennt wurde oder Lesen fehlschlaegt
     * @throws InterruptedException falls die Verarbeitung unterbrochen wird
     */
    private Message get_next_message_internal() throws IOException, InterruptedException {
        String receivedMessage = null;
        if (in == null) {
            throw new IOException("Client disconnected");
        } else {
            receivedMessage = in.readLine();
            if (receivedMessage == null) {
                in = null;
                throw new IOException("Client disconnected");
            }
        }

        Message msg = gson.fromJson(receivedMessage, Message.class);
        msg.payload.ip = clientSocket.getInetAddress().getHostAddress();
        return msg;
    }

    /**
     * Liefert die lokale Server-IP des Listening-Sockets.
     *
     * @return Server-IP
     */
    public String getServerIP() {
        return listening_socket.getInetAddress().getHostAddress();
    }

    /**
     * Stoppt den Kommunikationsdienst und schliesst alle offenen Ressourcen.
     */
    @Override
    public void close() {
        try {
            isRunning = false;
            token_semaphore.release();

            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
                logger.info("Client socket closed");
            }

            if (listening_socket != null && !listening_socket.isClosed()) {
                listening_socket.close();
                logger.info("Server socket closed");
            }

            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            pending_messages.release(Integer.MAX_VALUE);

        } catch (IOException e) {
            logger.error("Error closing Receiver: " + e.getMessage());
        }
    }
}
