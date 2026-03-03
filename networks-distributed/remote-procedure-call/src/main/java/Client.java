import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/**
 * Einstiegspunkt fuer den RPC-Client.
 * Liest Benutzereingaben ein, ruft die Multiplikation ueber das Netzwerk auf
 * und vergleicht die Laufzeit mit einer lokalen Ausfuehrung.
 *
 * @author Emre Öztürk
 * @date 04.11.2025
 */
public class Client {

    /**
     * Startet den Client-Prozess.
     *
     * @param args Kommandozeilenargumente, optional mit Ziel-IP an Position 0
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            args = new String[1];
            args[0] = "localhost";
        }
        System.out.println("Hello from client! Let's do multiplication! Running on ip: " + args[0] + " and port: 1234");
        System.out.println("---------------------------------------");
        I_Marshaller marshaller = new Marshaller();
        InputReader reader = new InputReader();
        Thread readerThread = new Thread(reader);
        readerThread.start();
        System.out.println("InputReader thread started.");
        while (readerThread.isAlive()) {
            Datastore d = new ClientStub(marshaller, args[0], 1234);
            // int value = 0;
            try {
                ArrayList<Integer> input = reader.getNextInput();
                if(input == null) {
                    reader.stop();
                    readerThread.join();
                    return;
                }
                int first_number = input.get(0);
                int second_number = input.get(1);
                long timestamp_1 = System.currentTimeMillis();

                // --- Asynchroner Server-Aufruf ---
                // 1. Aufruf starten (blockiert NICHT)
                CompletableFuture<Integer> futureValue = d.multiply(first_number, second_number);
                //System.out.println("... Anfrage an Server gesendet, warte auf Antwort ...");

                // 2. Auf das Ergebnis warten (blockiert an DIESER Stelle)
                int value = futureValue.get();

                long timestamp_2 = System.currentTimeMillis();
                System.out.println();
                System.out.printf("\nThe result returned from the server is: %d, time required is %d ms\n", value,
                        timestamp_2 - timestamp_1);

                // --- Lokaler Aufruf ---
                // BUGFIX: Du hast DatastoreImplementation() geschrieben, aber die Klasse heiÃŸt
                // Server()
                Datastore d_local = new Server();
                timestamp_1 = System.currentTimeMillis();

                // Der lokale Aufruf ist immer synchron
                CompletableFuture<Integer> localFuture = d_local.multiply(first_number, second_number);
                value = localFuture.get(); // Holt das Ergebnis

                timestamp_2 = System.currentTimeMillis();
                System.out.printf("\nThe result returned from the client is: %d, time required is %d ms\n", value,
                        timestamp_2 - timestamp_1);
            } catch (Exception e) {
                System.out.println("Error occurred during multiplication:");
                e.printStackTrace();
            }
        }
    }
}

