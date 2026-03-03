package worker;

import com.google.gson.Gson;

import core.IMessageBroker;
import core.INetworkListener;
import messaging.Message;
import messaging.MessageBroker;
import messaging.MsgType;
import models.WorkerDTO;

/**
 * Enthaelt die Laufzeitlogik eines Workers im verteilten Ring.
 * 
 * @author Emre Öztürk
 * @date 25.11.2025
 */
public class WorkerListener implements INetworkListener {

    /** Aktueller lokaler Zustand des Workers gemaess Algorithmus. */
    private int M;
    /** Eigener Queue-Name (hier: Port als String). */
    private String myQueueName;
    /** Broker-Adapter fuer Nachrichtenversand und -empfang. */
    private IMessageBroker broker;
    /** Queue-Name des rechten Nachbarn. */
    private String right_neighbor;
    /** Queue-Name des linken Nachbarn. */
    private String left_neighbor;
    /** JSON-Serializer/Deserializer. */
    private Gson gson;
    /** Deserialisierte Worker-Konfiguration. */
    WorkerDTO workerStruct;

    /**
     * Initialisiert den Worker und startet die erste UPDATE-Nachricht an beide
     * Nachbarn.
     *
     * @param startingM Startwert des Workers
     * @param port eigener Port/Queue-Name
     * @param broker_ip IP-Adresse des Message-Brokers
     * @param configJson JSON-Konfiguration mit Nachbarinformationen
     * @throws Exception falls der Broker nicht initialisiert werden kann
     */
    public WorkerListener(int startingM, int port, String broker_ip, String configJson) throws Exception {
        this.M = startingM;
        this.myQueueName = Integer.toString(port);
        System.out.println("[Worker ready. M=" + M + "]");
        gson = new Gson();
        workerStruct = gson.fromJson(configJson, WorkerDTO.class);

        this.left_neighbor = workerStruct.prev;
        this.right_neighbor = workerStruct.next;

        System.out.println("Worker" + port + "starting...");
        broker = new MessageBroker(broker_ip, Integer.toString(port));
        broker.setListener(this);
        broker.start();

        System.out.println("Worker starting computation with M = " + M);

        Message update = new Message();
        update.type = MsgType.UPDATE;
        update.sender = myQueueName;
        update.value = M;
        broker.send(left_neighbor, update);
        broker.send(right_neighbor, update);
    }

    /**
     * Verarbeitet eingehende Nachrichten fuer den Worker.
     *
     * @param msg empfangene Nachricht
     */
    @Override
    public void onMessage(Message msg) {
        switch (msg.type) {
            case MsgType.UPDATE:
                processUpdate(msg.value);
                break;
            case MsgType.QUERY:
                System.out.println("Worker received QUERY. Sending result to Chef.");
                sendResultToChef();
                break;
            case MsgType.KILL:
                System.out.println("Worker received KILL. Exiting.");
                System.exit(0);
                break;
            default:
                throw new IllegalArgumentException("Unknown message type: " + msg.type);

        }
    }

    /**
     * Implementiert den Kernschritt des verteilten ggT-Algorithmus.
     *
     * @param y empfangener Nachbarwert
     */
    private synchronized void processUpdate(int y) {
        if (y < M) {
            System.out.println("Received: " + y + " < " + M + ", updating M.");
            M = ((M - 1) % y) + 1;
            System.out.println("New M is: " + M);

            Message update = new Message();
            update.type = MsgType.UPDATE;
            update.sender = myQueueName;
            update.value = M;
            broker.send(left_neighbor, update);
            broker.send(right_neighbor, update);
        }
    }

    /**
     * Sendet den aktuellen Worker-Wert als RESULT an den Chef.
     */
    private void sendResultToChef() {
        Message resultMsg = new Message();
        resultMsg.type = MsgType.RESULT;
        resultMsg.sender = myQueueName;
        resultMsg.value = M;
        broker.send("queue.chef", resultMsg);
    }
}
