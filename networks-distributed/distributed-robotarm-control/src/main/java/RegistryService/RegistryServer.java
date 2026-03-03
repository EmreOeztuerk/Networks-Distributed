package RegistryService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import Messaging.COM;
import Messaging.E_Endpoint;
import Messaging.E_Query;
import Messaging.Message;
import Messaging.Payload;
import Misc.Log;

/**
 * Zentraler Registry-Service fuer Clients und Robot-Nodes.
 * Der Server verarbeitet Register-, Unregister- und List-Anfragen
 * und weist Clients fortlaufende IDs fuer den Token-Ring zu.
 *
 * @author Emre Öztürk
 * @date 16.12.25
 */
public class RegistryServer {
    private static COM com;
    private static boolean running;
    private static Log logger = new Log("RegistryServer");
    private static Map<String, Payload> registered_endpoints = new ConcurrentHashMap<>();
    private static final AtomicInteger id_counter = new AtomicInteger(0);
    private static ExecutorService threadPool = Executors.newCachedThreadPool();

    /**
     * Startpunkt des Registry-Servers.
     *
     * @param args Kommandozeilenargumente (derzeit ungenutzt)
     * @throws Exception falls Initialisierung oder Laufzeit fehlschlaegt
     */
    public static void main(String[] args) throws Exception {
        com = new COM(8080);

        running = true;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Received shutdown signal. Shuttting down gracefully...");
            running = false;
            com.close();
            threadPool.shutdown();
        }));

        while (running) {
            try {
                Message msg = com.get_next_message();
                threadPool.execute(() -> handleMessageTask(msg));
            } catch (InterruptedException e) {
                logger.info("Connection is closed.");
            }
        }
    }

    /**
     * Fuehrt die Nachrichtenverarbeitung in einem Thread-Pool-Task aus.
     *
     * @param msg eingegangene Nachricht
     */
    private static void handleMessageTask(Message msg) {
        update_client_registries(msg);
    }

    /**
     * Beantwortet eine LIST-Anfrage mit allen aktuell registrierten Endpunkten.
     *
     * @param msg originale LIST-Anfrage
     */
    private static void handle_list(Message msg) {
        Message response = new Message();
        response.command = E_Query.LIST;
        response.payload.list = new ArrayList<>(registered_endpoints.values());
        try {
            com.sendTo(response, msg.payload.ip, msg.payload.port);
            logger.info("Sent list response to " + msg.payload.ip + ":" + msg.payload.port);
        } catch (IOException e) {
            logger.error("Failed to send list response to " + msg.payload.ip + ":" + msg.payload.port);
        }
    }

    /**
     * Verarbeitet REGISTER-, UNREGISTER- und LIST-Befehle.
     *
     * @param msg eingegangene Nachricht
     */
    private static void update_client_registries(Message msg) {
        Payload entity = msg.payload;
        switch (msg.command) {
            case E_Query.REGISTER:
                synchronized (registered_endpoints) {
                    if (registered_endpoints.containsKey(entity.name)) {
                        sendError(entity, "Name '" + entity.name + "' is already taken.");
                        return;
                    }
                    int new_id = id_counter.incrementAndGet();
                    entity.id = new_id;
                    registered_endpoints.put(entity.name, entity);
                    logger.info("[" + Thread.currentThread().getName() + "] Registered: " + entity.name + " (ID: " + new_id + ")");

                    sendRegisterResponse(entity, new_id);

                    if (entity.type == E_Endpoint.CLIENT) {
                        int client_count = 0;
                        for (Payload p : registered_endpoints.values()) {
                            if (p.type == E_Endpoint.CLIENT) {
                                client_count++;
                            }
                        }

                        if (client_count == 1) {
                            sendToken(entity);
                        }
                    }
                }
                break;

            case E_Query.UNREGISTER:
                Payload removed_client = registered_endpoints.remove(entity.name);
                if (removed_client != null) {
                    logger.info("Unregistered " + entity.name);
                }
                break;
            case E_Query.LIST:
                handle_list(msg);
                break;
            default:
                logger.error("Unknown command received: " + msg.command);
        }
    }

    /**
     * Sendet eine standardisierte Fehlermeldung an den aufrufenden Endpunkt.
     *
     * @param entity Zielendpunkt
     * @param errorMessage Fehlerbeschreibung
     */
    private static void sendError(Payload entity, String errorMessage) {
        Payload errorPayload = new Payload();
        errorPayload.message = errorMessage;
        errorPayload.status = "ERROR";
        Message errorMsg = new Message(E_Query.ERROR, errorPayload);
        try {
            com.sendTo(errorMsg, entity.ip, entity.port);
        } catch (IOException e) {
        }
    }

    /**
     * Sendet die zugewiesene Client-ID nach erfolgreicher Registrierung.
     *
     * @param entity registrierter Endpunkt
     * @param id zugewiesene ID
     */
    private static void sendRegisterResponse(Payload entity, int id) {
        Message tempMessage = new Message();
        tempMessage.command = E_Query.REGISTER_RESPONSE;
        tempMessage.payload.id = id;
        try {
            com.sendTo(tempMessage, entity.ip, entity.port);
            logger.info("Sent REGISTER_RESPONSE (ID: " + id + ") to " + entity.name);
        } catch (IOException e) {
            logger.error("Failed to send REGISTER_RESPONSE to " + entity.ip);
        }
    }

    /**
     * Sendet initial ein TOKEN an den ersten registrierten Client.
     *
     * @param entity erster Client im Ring
     */
    private static void sendToken(Payload entity) {
        logger.info("Sending TOKEN to " + entity.name);
        Message token_msg = new Message();
        token_msg.command = E_Query.TOKEN;
        try {
            com.sendTo(token_msg, entity.ip, entity.port);
        } catch (IOException e) {
        }
    }
}
