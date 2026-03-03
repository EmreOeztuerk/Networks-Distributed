package TerminalClient;

import Messaging.Address;
import Messaging.COM;
import Messaging.E_Endpoint;
import Messaging.E_Query;
import Messaging.Message;
import Messaging.Payload;
import Misc.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

/**
 * Terminal-Client fuer Registry-Abfragen, Token-Ring-Koordination
 * und direkte RPC-Steuerung von Robot-Nodes.
 *
 * @author Emre Öztürk
 * @date 16.12.25
 */
public class Client {
    private static boolean running = true;
    private static Scanner scan;
    private static String name;
    private static COM com = null;
    private static Log logger;
    private static Log token_logger = new Log("TokenManager");
    private static String server_ip;
    private static int server_port;
    private static Semaphore token_semaphore = new Semaphore(0);
    private static int self_id = -1;
    private static RegistrationUtility registry;
    private static boolean registered = false;

    /**
     * Versucht, das Token an den naechsten Nachbarn im Ring weiterzuleiten.
     *
     * @return true, falls das Token erfolgreich weitergeleitet wurde
     */
    private static boolean attempt_send_token() {
        token_logger.info("Token is no longer used. Asking available nodes from registry...");
        List<Payload> available_nodes = fetch_available_nodes();
        Address next_neighbor = extract_neighbors(available_nodes);
        if (next_neighbor == null) {
            token_logger.warning("No neighbor found to forward the token to. Retaining token.");
            return false;
        }
        token_logger.info("Forwarding token to " + next_neighbor.ip + ":" + next_neighbor.port);
        Message token_msg = new Message();
        token_msg.command = E_Query.TOKEN;
        try {
            com.sendTo(token_msg, next_neighbor.ip, next_neighbor.port);
            token_logger.info("Token has been forwarded to " + next_neighbor.ip + ":" + next_neighbor.port);
        } catch (Exception e) {
            logger.error("Error while forwarding token: " + e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Startet den Hintergrund-Thread fuer den Token-Empfang und die Token-Weitergabe.
     */
    public static void start_token_management() {
        token_logger.disable();
        new Thread(() -> {
            while (running) {
                try {
                    token_logger.info("Listening for token...");
                    com.wait_for_token();
                    token_logger.info("Token has arried.");
                } catch (InterruptedException e) {
                    logger.warning("Token listening interrupted: " + e.getMessage());
                    return;
                }
                boolean send_status = false;
                while (!send_status) {
                    token_logger.info("Releasing token for usage...");
                    token_semaphore.release();
                    try {
                        Thread.sleep(500);
                        token_logger.info("No-usage-timeout has exceeded. Waiting till token is available...");
                        token_semaphore.acquire();
                    } catch (InterruptedException e) {
                        logger.warning("Sleep interrupted or token semaphore are interrupted: " + e.getMessage());
                        return;
                    }
                    send_status = attempt_send_token();
                }
            }

        }).start();
    }

    /**
     * Fuehrt einen geordneten Shutdown inklusive Deregistrierung aus.
     */
    private static void shutdown() {
        logger.info("Received shutdown signal. Shuttting down gracefully...");
        running = false;
        com.close();
        token_semaphore.release();
        if (!registered) {
            return;
        }
        try {
            registry.unregister();
            logger.info("Client unregistered successfully!");
        } catch (Exception e) {
            logger.error("Failed to unregister client.");
        }
    }

    /**
     * Zeigt das Hauptmenue und verarbeitet einen Befehl.
     */
    private static void handle_root() {
        System.err.println();
        System.out.println("Available commands:");
        System.out.println("(1) Wait for my turn to control the robot");
        System.out.println("(2) Provide a list of the nodes");
        System.out.println("or ctrl+c to exit");

        String command = scan.nextLine();
        try {
            if (command.equals("1")) {
                request_control();
            } else if (command.equals("2")) {
                list_nodes();
            }
        } catch (Exception e) {
            logger.error("Error while processing command: " + e.getMessage());
            System.out.println("Please try again.");
        }
    }

    /**
     * Startet den Terminal-Client.
     *
     * @param args args[0]=Clientname, args[1]=Registry-IP (optional)
     * @throws Exception falls Initialisierung fehlschlaegt
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: <name> <registry_service_ip>");
            return;
        }

        if (args.length == 1) {
            System.out.println("Defaulting to localhost for registry service IP");
            args = new String[] { args[0], "localhost" };
        }
        name = args[0];
        logger = new Log("Client");
        server_ip = args[1];
        server_port = 8080;

        Runtime.getRuntime().addShutdownHook(new Thread(Client::shutdown));
        scan = new Scanner(System.in);
        logger.info("Running client...");
        do {
            try {
                com = new COM();
                registry = new RegistrationUtility(com);
            } catch (Exception e) {
                logger.error("Failed to start COM. Trying again in 1 second...");
                Thread.sleep(1000);
            }
        } while (com == null);

        logger.info("Registering client to registry service at " + server_ip + ":" + server_port);
        while (true) {
            try {
                self_id = registry.register(server_ip, server_port, name);
                break;
            } catch (Exception e) {
                logger.error("Failed to register client. Trying again in 1 second...");
                Thread.sleep(1000);
            }
        }
        if (self_id == -1) {
            logger.error("Registration rejected by server. Name '" + name + "' is already taken.");
            shutdown();
            return;
        }
        logger.info("Client registered successfully!");
        registered = true;
        start_token_management();
        while (running) {
            handle_root();
        }
    }

    /**
     * Fragt die aktuelle Liste registrierter Endpunkte beim Registry-Server ab.
     *
     * @return Liste der registrierten Endpunkte oder null bei Fehlern
     */
    private static List<Payload> fetch_available_nodes() {
        Message msg = new Message();
        msg.command = E_Query.LIST;
        msg.payload.id = self_id;
        try {
            com.sendTo(msg, server_ip, server_port);
        } catch (IOException e) {
            logger.error("Failed to send LIST request to registry service at " + server_ip + ":" + server_port);
            return null;
        }
        Message response;
        try {
            response = com.get_next_message();
        } catch (InterruptedException e) {
            logger.error("fetching nodes was interrupted: " + e.getMessage());
            return null;
        }
        return response.payload.list;
    }

    /**
     * Gibt alle verfuegbaren Endpunkte auf der Konsole aus.
     *
     * @throws Exception falls die Abfrage fehlschlaegt
     */
    private static void list_nodes() throws Exception {
        List<Payload> payload_list = fetch_available_nodes();
        System.out.println("Available nodes:");
        for (final Payload payload : payload_list) {
            System.out.println("- " + payload.name + " (" + payload.ip + ":" + payload.port + ")");
        }
    }

    /**
     * Wartet auf das Token und startet danach die RPC-Robotersteuerung.
     *
     * @throws Exception falls Token-Wartevorgang oder RPC fehlschlaegt
     */
    private static void request_control() throws Exception {
        logger.info("Waiting to acquire token...");
        token_semaphore.acquire();
        logger.info("Token has been acquired. You can now control the robot.");
        do_robot_control_rpc();
        token_semaphore.release();
        logger.info("Robot control done. Token released.");
    }

    /**
     * Bestimmt den naechsten Client-Nachbarn fuer die Token-Weitergabe.
     *
     * @param nodes alle registrierten Endpunkte
     * @return Adresse des Nachbarn oder null, wenn keiner vorhanden ist
     */
    private static Address extract_neighbors(List<Payload> nodes) throws RuntimeException {
        List<Payload> clients = new ArrayList<>();
        for (Payload p : nodes) {
            if (p.type == E_Endpoint.CLIENT) {
                clients.add(p);
            }
        }
        if (clients.isEmpty()) {
            logger.error("No other clients found to forward the token to.");
            return null;
        }

        clients.sort((a, b) -> Integer.compare(a.id, b.id));

        int my_index = -1;
        for (int i = 0; i < clients.size(); i++) {
            if (clients.get(i).id == self_id) {
                my_index = i;
                break;
            }
        }
        if (my_index == -1) {
            logger.error("Self ID not found in client list.");
            return null;
        }

        int neighbor_index = (my_index + 1) % clients.size();
        Payload neighbor = clients.get(neighbor_index);

        if (neighbor.id == self_id) {
            return null;
        }

        return new Address(neighbor.ip, neighbor.port);
    }

    /**
     * Fragt Robot-Nodes ab, waehlt einen Roboter und sendet einen MOVE-RPC.
     *
     * @throws Exception falls Kommunikation oder Eingabe fehlschlaegt
     */
    private static void do_robot_control_rpc() throws Exception {
        Message listMsg = new Message();
        listMsg.command = E_Query.LIST;
        com.sendTo(listMsg, server_ip, server_port);

        Message listResp = com.get_next_message();
        List<Payload> all = new ArrayList<>(listResp.payload.list);

        System.out.println("Available robot nodes:");
        int idx = 0;
        for (Payload p : all) {
            if (p.type == E_Endpoint.ROBOT_NODE) {
                System.out.println("[" + idx + "] " + p.name + " (" + p.ip + ":" + p.port + ")");
            }
            idx++;
        }

        System.out.print("Enter robot name to control: ");
        String robotName = scan.nextLine().trim();

        Payload selected = null;
        for (Payload p : all) {
            if (p.type == E_Endpoint.ROBOT_NODE && p.name != null && p.name.equals(robotName)) {
                selected = p;
                break;
            }
        }

        if (selected == null) {
            System.out.println("Robot not found: " + robotName);
            return;
        }

        System.out.print("Joint (1=Links/Rechts, 2=Unten/Oben, 3=Vor/Zurueck, 4=Auf/Zu): ");
        int joint = Integer.parseInt(scan.nextLine().trim());

        System.out.print("Percentage (0..100): ");
        int pct = Integer.parseInt(scan.nextLine().trim());

        Message move = new Message();
        move.command = E_Query.MOVE;
        move.payload.rpcId = java.util.UUID.randomUUID().toString();
        move.payload.joint = joint;
        move.payload.percentage = pct;

        com.sendTo(move, selected.ip, selected.port);

        Message resp = com.get_next_message();
        System.out.println("Robot response: " + resp.payload.status + " - " + resp.payload.message);
    }
}
