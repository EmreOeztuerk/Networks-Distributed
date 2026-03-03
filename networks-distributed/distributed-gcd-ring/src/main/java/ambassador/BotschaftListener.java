package ambassador;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import core.IMessageBroker;
import core.INetworkListener;
import messaging.Message;
import messaging.MessageBroker;
import messaging.MsgType;
import models.WorkerDTO;

/**
 * Verarbeitet eingehende Steuer-Nachrichten fuer einen Ambassador.
 * <p>
 * Die Klasse sammelt Worker-Konfigurationen (SPAWN), startet daraufhin die
 * zugehoerigen Worker-Prozesse (START) und beendet sie bei KILL.
 * 
 * @author Emre \u00D6zt\u00FCrk
 * @date 25.11.2025
 */
public class BotschaftListener implements INetworkListener {

    /** Broker-Adapter fuer den Nachrichtenaustausch. */
    private IMessageBroker broker;
    /** Lokal gestartete Worker-Prozesse zur spaeteren Terminierung. */
    private List<Process> workerProcesses = new ArrayList<>();
    /** JSON-Serializer/Deserializer fuer Worker-Konfigurationen. */
    private Gson gson;
    /** Zwischengespeicherte Spawn-Nachrichten bis zum START-Signal. */
    private List<Message> worker_configs = new ArrayList<>();
    /** IP-Adresse des verwendeten Message-Brokers. */
    private String brokerIp;

    /**
     * Erstellt den Listener und verbindet ihn mit der lokalen Ambassador-Queue.
     *
     * @param brokerIp IP-Adresse des Message-Brokers
     * @param queueName Queue-Name dieses Ambassadors
     * @throws Exception falls die Broker-Verbindung fehlschlaegt
     */
    public BotschaftListener(String brokerIp, String queueName) throws Exception {
        this.brokerIp = brokerIp;
        gson = new Gson();
        broker = new MessageBroker(brokerIp, queueName);
        broker.setListener(this);
        broker.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Received shutdown signal. Killing all workers...");
            killAllWorkers();
        }));
    }

    /**
     * Startet einen einzelnen Worker-Prozess mit den uebergebenen Parametern.
     *
     * @param number Startwert des Workers
     * @param configJson serialisierte Ring-Konfiguration des Workers
     * @throws IOException falls der Prozess nicht gestartet werden kann
     */
    private void startWorkerProcess(int number, String configJson) throws IOException {
        WorkerDTO dto = gson.fromJson(configJson, WorkerDTO.class);
        ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-jar",
                "./build/libs/Worker.jar",
                String.valueOf(number),
                configJson,
                brokerIp,
                Integer.toString(dto.port)
            );
        pb.redirectErrorStream(true);

        Process process = pb.start();
        workerProcesses.add(process);

        System.out.println("Started worker process for number: " + number + " (PID: " + getProcessId(process) + ")");

        new Thread(() -> {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("Local Worker: " + number + ": " + line);
                }
            } catch (IOException e) {
                System.err.println("Error reading worker output: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Beendet alle aktuell laufenden Worker-Prozesse dieses Ambassadors.
     */
    private void killAllWorkers() {
        System.out.println("Killing " + workerProcesses.size() + " worker processes...");
        for (Process process : workerProcesses) {
            if (process.isAlive()) {
                try {
                    System.out.println("Killing worker process (PID: " + getProcessId(process) + ")");
                    process.destroy();

                    if (!process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) {
                        System.out.println("Forcing kill of worker process (PID: " + getProcessId(process) + ")");
                        process.destroyForcibly();
                        process.waitFor();
                    }

                    System.out.println("Worker process terminated successfully. Ready to accept new tasks.");

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Interrupted while killing worker process");
                }
            }
        }
        workerProcesses.clear();
    }

    /**
     * Liefert die Prozess-ID eines gestarteten Workers.
     *
     * @param process gestarteter Prozess
     * @return PID oder {@code -1}, falls nicht verfuegbar
     */
    private long getProcessId(Process process) {
        try {
            return process.pid();
        } catch (UnsupportedOperationException e) {
            return -1;
        }
    }

    /**
     * Reagiert auf eingehende Steuer-Nachrichten.
     *
     * @param message empfangene Nachricht vom Chef
     */
    @Override
    public void onMessage(Message message) {
        switch (message.type) {
            case MsgType.SPAWN:
                worker_configs.add(message);
                break;
            case MsgType.KILL:
                killAllWorkers();
                break;
            case MsgType.START:
                for (Message current_config : worker_configs) {
                    try {
                        startWorkerProcess(current_config.value, current_config.configJson);
                    } catch (IOException e) {
                        System.err.println("Error starting worker process for number: " + current_config.value);
                        e.printStackTrace();
                    }
                }
                break;
            default:
                System.out.println(gson.toJson(message, Message.class));
                throw new UnsupportedOperationException("Unimplemented method 'onMessage'");
        }
    }
}
