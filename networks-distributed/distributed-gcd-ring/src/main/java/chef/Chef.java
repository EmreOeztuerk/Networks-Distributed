package chef;

import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

import com.google.gson.Gson;

import core.IMessageBroker;
import core.INetworkListener;
import messaging.Message;
import messaging.MessageBroker;
import messaging.MsgType;
import models.Computer;
import models.WorkerStructure;
import models.WorkersRing;

/**
 * Zentrales Startprogramm fuer den verteilten ggT-Algorithmus.
 * <p>
 * Der Chef verteilt Eingabezahlen auf Ambassadors/Worker, sendet Startsignale,
 * wartet eine feste Zeit, fragt Ergebnisse ab und terminiert danach alle
 * gestarteten Worker.
 * 
 * @author Emre Öztürk
 * @date 25.11.2025
 */
public class Chef implements INetworkListener {

    /** Standard-Broker-Host fuer lokale Entwicklung. */
    private static final String BROKER_IP = "localhost";

    /** Broker-Adapter fuer den Nachrichtenaustausch. */
    private IMessageBroker broker;
    /** Synchronisation zum Warten auf alle RESULT-Nachrichten einer Runde. */
    private CountDownLatch latch;

    /**
     * Verarbeitet eingehende Nachrichten fuer den Chef.
     *
     * @param msg empfangene Nachricht
     */
    @Override
    public void onMessage(Message msg) {
        if (msg.type == MsgType.RESULT) {
            System.out.println(">>> Result from " + msg.sender + ": " + msg.value);
            if (latch != null) {
                latch.countDown();
            }
        }
    }

    /**
     * CLI-Einstiegspunkt fuer den Chef.
     *
     * @param args Queue-Namen der erreichbaren Ambassadors
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: <queue name of botschaft 1> <queue name of botschaft 2>");
            return;
        }
        Chef chef = new Chef();
        chef.run(args);
    }

    /**
     * Startet die interaktive Eingabeschleife des Chefs.
     *
     * @param computers Queue-Namen der Ambassadors
     */
    public void run(String[] computers) {
        try {
            System.out.println(">>> Connecting to ActiveMQ (" + BROKER_IP + ")...");
            broker = new MessageBroker(BROKER_IP, "queue.chef");
            broker.setListener(this);
            broker.start();

            Scanner scan = new Scanner(System.in);
            System.out.println(">>> CHEF ready. PCs: " + Arrays.toString(computers));
            System.out.println("Enter numbers (e.g. '108 76 12') or 'exit':");

            boolean running = true;
            while (running) {
                String input = scan.nextLine();
                if (input.equals("exit")) {
                    running = false;
                } else {
                    processInput(input, computers);
                    System.out.println("\nReady for next round. Enter numbers:");
                }
            }

            broker.close();
            scan.close();
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Fuehrt eine komplette Berechnungsrunde fuer eine Eingabezeile aus.
     *
     * @param input Benutzereingabe mit Zahlen
     * @param computers Queue-Namen der Ambassadors
     * @throws Exception bei Netzwerk- oder Threading-Fehlern
     */
    private void processInput(String input, String[] computers) throws Exception {
        String[] parts = input.trim().split("\\s+");
        if (parts.length < 2) {
            System.out.println("At least two numbers!");
            return;
        }
        Integer[] numbers = new Integer[parts.length];
        for (int i = 0; i < parts.length; i++) {
            numbers[i] = Integer.parseInt(parts[i]);
        }

        System.out.println(">>> 1. Planning...");
        WorkersRing ring = new WorkersRing(computers, numbers);
        ring.printRing();

        System.out.println(">>> 2. SPAWN via Ambassador...");
        Gson gson = new Gson();

        for (int i = 0; i < ring.computers.length; i++) {
            Computer comp = ring.computers[i];

            for (WorkerStructure worker : comp.workers_list) {
                int myPort = worker.port;
                Message spawn = new Message();
                spawn.type = MsgType.SPAWN;
                spawn.value = worker.number;
                spawn.targetPort = myPort;

                models.WorkerDTO dto = new models.WorkerDTO(worker.number, worker.port);
                dto.next = Integer.toString(worker.next.port);
                dto.prev = Integer.toString(worker.prev.port);
                spawn.configJson = gson.toJson(dto, models.WorkerDTO.class);

                broker.send(comp.name, spawn);
                System.out.println("Chef -> Ambassador " + comp.name + " start Node " + myPort + " (number: " + worker.number + ")");
            }
        }

        System.out.println(">>> 3. Wait 1s for boot up...");
        Thread.sleep(1000);

        System.out.println(">>> 4. START Signal...");
        for (Computer comp : ring.computers) {
            Message start = new Message();
            start.type = MsgType.START;
            broker.send(comp.name, start);
        }

        System.out.println(">>> 5. Wait for computing (5s) & Query...");
        Thread.sleep(5000);

        latch = new CountDownLatch(numbers.length);

        for (Computer comp : ring.computers) {
            for (WorkerStructure w : comp.workers_list) {
                Message q = new Message();
                q.type = MsgType.QUERY;
                String current_worker = Integer.toString(w.port);
                broker.send(current_worker, q);
            }
        }

        latch.await();
        System.out.println(">>> Round finished.");

        Message kill = new Message();
        kill.type = MsgType.KILL;
        for (Computer comp : ring.computers) {
            String ambassadorQueue = comp.name;
            broker.send(ambassadorQueue, kill);
        }
    }
}
