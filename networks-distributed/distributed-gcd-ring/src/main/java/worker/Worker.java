package worker;

/**
 * Einstiegspunkt fuer einen einzelnen Worker-Prozess.
 * 
 * @author Emre Öztürk
 * @date 25.11.2025
 */
public class Worker {
    /**
     * Startet den Worker-Listener mit Konfiguration aus den CLI-Argumenten.
     *
     * @param args {@code args[0]} Startwert M, {@code args[1]} Worker-Config JSON,
     *             {@code args[2]} Broker-IP, {@code args[3]} eigener Port
     * @throws Exception falls die Initialisierung des Listeners fehlschlaegt
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Usage: Worker <StartingM> <ConfigJson> <BrokerIp> <Port>");
            System.out.println("given: " + String.join(" ", args));
            return;
        }
        System.out.printf("Worker started with ggT Zahl: %s\n", args[0]);

        int startingM = Integer.parseInt(args[0]);
        String configJson = args[1];
        String brokerIp = args[2];
        int port = Integer.parseInt(args[3]);

        new WorkerListener(startingM, port, brokerIp, configJson);
    }
}
