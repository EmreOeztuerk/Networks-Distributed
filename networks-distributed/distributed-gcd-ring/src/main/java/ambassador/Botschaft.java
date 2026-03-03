package ambassador;

/**
 * Einstiegspunkt fuer einen Ambassador-Prozess.
 * <p>
 * Der Prozess verbindet sich mit dem Message Broker und wartet auf Befehle vom
 * Chef. Er startet lokale Worker-Prozesse, wenn SPAWN/START-Nachrichten
 * eintreffen, und beendet sie bei KILL.
 * @author Emre \u00D6zt\u00FCrk
 * @date 25.11.2025
 */
public class Botschaft {
    /**
     * Startet den Ambassador.
     *
     * @param args {@code args[0]} = Broker-IP, {@code args[1]} = Queue-Name des
     *             Ambassadors
     * @throws Exception falls der Listener nicht initialisiert werden kann
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: <IP-Address of broker> <name of local queue>");
            return;
        }
        // args[0] = "172.20.10.3" ;
        // args[1] = ":maverick";
        System.out.println("Ambassador started. Broker ip is: "+ args[0]);
        System.out.println("This Botschaft Queue name is: " + args[1]);

        new BotschaftListener(args[0], args[1]);
        
    }
}
