package RobotNodeTest;

import Messaging.COM;
import Messaging.E_Query;
import Messaging.Message;
import Misc.Log;

/**
 * Fuehrt automatisierte MOVE-Tests gegen einen laufenden RobotNode aus.
 *
 * @author Emre Öztürk
 * @date 16.12.25
 */
public class RobotNodeAutoTester {

    private static final Log logger = new Log("RobotNodeAutoTester");

    /**
     * Startet den automatischen Testlauf.
     *
     * @param args args[0]=RobotNode-IP, args[1]=RobotNode-Port
     * @throws Exception falls Kommunikation oder Validierung fehlschlaegt
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: RobotNodeAutoTester <robotNodeIp> <robotNodePort>");
            return;
        }

        String robotIp = args[0];
        int robotPort = Integer.parseInt(args[1]);

        try (COM com = new COM()) {
            logger.info("Tester listening on port: " + com.getListeningPort());
            logger.info("Starting automatic robot test...");

            for (int joint = 1; joint <= 4; joint++) {
                System.out.println("\n=== Testing joint " + joint + " ===");

                testMove(com, robotIp, robotPort, joint, 0);
                Thread.sleep(700);

                testMove(com, robotIp, robotPort, joint, 100);
                Thread.sleep(700);

                testMove(com, robotIp, robotPort, joint, 0);
                Thread.sleep(700);
            }

            System.out.println("\nAlle Gelenke wurden erfolgreich getestet.");
        }
    }

    /**
     * Sendet einen MOVE-Request und validiert die Antwort.
     *
     * @param com Kommunikationsobjekt
     * @param ip Ziel-IP
     * @param port Zielport
     * @param joint Gelenknummer
     * @param percentage Zielwert
     * @throws Exception falls die Antwort ungueltig ist
     */
    private static void testMove(COM com, String ip, int port, int joint, int percentage) throws Exception {
        Message req = new Message();
        req.command = E_Query.MOVE;
        req.payload.rpcId = java.util.UUID.randomUUID().toString();
        req.payload.joint = joint;
        req.payload.percentage = percentage;

        logger.info("MOVE joint=" + joint + " percentage=" + percentage);

        com.sendTo(req, ip, port);

        Message resp = com.get_next_message();

        if (resp.payload == null) {
            throw new RuntimeException("No payload in response");
        }
        if (!req.payload.rpcId.equals(resp.payload.rpcId)) {
            throw new RuntimeException("rpcId mismatch");
        }
        if (resp.command != E_Query.MOVE_RESPONSE) {
            throw new RuntimeException("Unexpected response command: " + resp.command);
        }
        if (!"OK".equals(resp.payload.status)) {
            throw new RuntimeException("Robot returned ERROR: " + resp.payload.message);
        }

        logger.info("OK: " + resp.payload.message);
    }
}
