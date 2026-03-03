package RobotNode;

import java.io.IOException;
import java.net.InetAddress;

import Messaging.COM;
import Messaging.E_Endpoint;
import Messaging.E_Query;
import Messaging.Message;
import Misc.Log;
import RobotArm.Robotarm;

import org.cads.vs.roboticArm.hal.ICaDSRoboticArm;
import org.cads.vs.roboticArm.hal.real.CaDSRoboticArmReal;

/**
 * Robot-Node mit Registry-Anbindung und einfacher MOVE-RPC-Verarbeitung.
 *
 * @author Emre Öztürk
 * @date 16.12.25
 */
public class RobotNode {

    private static boolean running = true;
    private static String name;

    private static COM com;
    private static Robotarm robotarm;
    private static final Log logger = new Log("RobotNode");

    /**
     * Startpunkt des Robot-Nodes.
     *
     * @param args args[0]=Name, args[1]=Registry-IP (optional)
     * @throws Exception falls Initialisierung oder Laufzeit fehlschlaegt
     */
    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            System.out.println("Usage: RobotNode <nodeName> <registryIp=localhost> <nodePort=default>");
            System.exit(0);
        }
        String registryIp;
        int nodePort;

        if (args.length == 1) {
            System.out.println("Defaulting to localhost and random port for RobotNode");
            args = new String[] { args[0], "localhost" };
            com = new COM();
        }

        if (args.length == 2) {
            System.out.println("Defaulting to random port for RobotNode");
            com = new COM();
        }

        name = args[0];
        registryIp = args[1];
        nodePort = com.getListeningPort();

        int registryPort = 8080;

        ICaDSRoboticArm arm = new CaDSRoboticArmReal("172.16.1.61", 50055);
        robotarm = new Robotarm(arm);

        try {
            registerAtRegistry(registryIp, registryPort, nodePort);
        } catch (IOException e) {
            logger.error("STARTUP FAILED: " + e.getMessage());
            com.close();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown requested. Cleaning up...");
            running = false;
            try {
                unregisterAtRegistry(registryIp, registryPort);
            } catch (Exception ignored) {
            }
            try {
                com.close();
            } catch (Exception ignored) {
            }
        }));

        logger.info("RobotNode '" + name + "' is running on port " + nodePort + ". Type 'quit' to stop.");

        while (running) {
            try {
                Message msg = com.get_next_message();
                handleIncoming(msg);
            } catch (InterruptedException e) {
                logger.info("COM interrupted/closed.");
                break;
            } catch (Exception e) {
                logger.error("Error in RPC loop: " + e.getMessage());
            }
        }

        try {
            unregisterAtRegistry(registryIp, registryPort);
        } catch (Exception ignored) {
        }
        try {
            com.close();
        } catch (Exception ignored) {
        }
    }

    /**
     * Registriert den Robot-Node bei der Registry.
     *
     * @param registryIp Registry-IP
     * @param registryPort Registry-Port
     * @param nodePort lokaler Robot-Node-Port
     * @throws IOException bei Verbindungs- oder Protokollfehlern
     * @throws InterruptedException falls das Warten auf Antwort unterbrochen wird
     */
    private static void registerAtRegistry(String registryIp, int registryPort, int nodePort)
            throws IOException, InterruptedException {
        Message msg = new Message();
        msg.command = E_Query.REGISTER;
        msg.payload.type = E_Endpoint.ROBOT_NODE;
        msg.payload.name = name;
        msg.payload.ip = InetAddress.getLocalHost().getHostAddress();
        msg.payload.port = nodePort;

        try {
            com.sendTo(msg, registryIp, registryPort);
        } catch (IOException e) {
            throw new IOException(
                    "Registry Server at " + registryIp + ":" + registryPort + " is NOT reachable. Is it started?");
        }

        Message resp = com.get_next_message();

        if (resp.command == E_Query.ERROR) {
            throw new IOException("Registration rejected by Registry: " + resp.payload.message);
        }

        if (resp.command != E_Query.REGISTER_RESPONSE) {
            throw new IOException("Unexpected response from registry: " + resp.command);
        }

        logger.info("Successfully registered at registry as ROBOT_NODE name='" + name + "'");
    }

    /**
     * Meldet den Robot-Node bei der Registry ab.
     *
     * @param registryIp Registry-IP
     * @param registryPort Registry-Port
     * @throws IOException falls Senden fehlschlaegt
     */
    private static void unregisterAtRegistry(String registryIp, int registryPort) throws IOException {
        Message msg = new Message();
        msg.command = E_Query.UNREGISTER;
        msg.payload.type = E_Endpoint.ROBOT_NODE;
        msg.payload.name = name;
        try {
            com.sendTo(msg, registryIp, registryPort);
            logger.info("Unregistered from registry.");
        } catch (IOException e) {
            logger.warning("Could not unregister (Registry might be down): " + e.getMessage());
        }
    }

    /**
     * Verteilt eingehende RPC-Anfragen auf passende Handler.
     *
     * @param msg eingehende Nachricht
     * @throws IOException falls eine Antwort nicht gesendet werden kann
     */
    private static void handleIncoming(Message msg) throws IOException {
        if (msg == null || msg.command == null) {
            return;
        }

        switch (msg.command) {
            case MOVE:
                handleMove(msg);
                break;
            default:
                sendError(msg, "Unknown command: " + msg.command);
        }
    }

    /**
     * Verarbeitet einen MOVE-Befehl und antwortet mit MOVE_RESPONSE.
     *
     * @param req MOVE-Anfrage
     * @throws IOException falls das Senden der Antwort fehlschlaegt
     */
    private static void handleMove(Message req) throws IOException {
        Message resp = new Message();
        resp.command = E_Query.MOVE_RESPONSE;
        resp.payload.rpcId = req.payload.rpcId;

        try {
            int joint = req.payload.joint;
            if (joint < 1 || joint > 4) {
                throw new IllegalArgumentException("joint must be 1..4");
            }
            int p = req.payload.percentage;
            robotarm.bewegeGelenk(joint, p);

            resp.payload.status = "OK";
            resp.payload.message = "Moved joint " + joint + " to " + p + "%";
        } catch (Exception e) {
            resp.payload.status = "ERROR";
            resp.payload.message = e.getMessage();
        }

        com.sendTo(resp, req.payload.ip, req.payload.port);
    }

    /**
     * Sendet eine ERROR-Antwort an den Aufrufer.
     *
     * @param req Ursprungsanfrage
     * @param reason Fehlerursache
     * @throws IOException falls die Antwort nicht gesendet werden kann
     */
    private static void sendError(Message req, String reason) throws IOException {
        Message err = new Message();
        err.command = E_Query.ERROR;
        err.payload.status = "ERROR";
        err.payload.message = reason;
        err.payload.rpcId = req.payload.rpcId;
        com.sendTo(err, req.payload.ip, req.payload.port);
    }
}
