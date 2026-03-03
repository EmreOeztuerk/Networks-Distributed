import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Clientseitiger Stub fuer den entfernten Datastore.
 * Baut Requests, versendet sie per UDP und liefert Ergebnisse asynchron als
 * {@link CompletableFuture}.
 *
 * @author Emre Öztürk
 * @date 04.11.2025
 */
public class ClientStub implements Datastore {

    /**
     * Marshaller fuer JSON-Serialisierung.
     */
    private I_Marshaller marshaller;
    /**
     * Ziel-IP des Servers.
     */
    private String ip_address;
    /**
     * Zielport des Servers.
     */
    private int port;

    /**
     * Factory fuer Daemon-Threads im Threadpool.
     */
    private final ThreadFactory daemonThreadFactory = new ThreadFactory() {
        /**
         * Erstellt einen neuen Daemon-Thread.
         *
         * @param r auszufuehrender Runnable-Task
         * @return konfigurierte Thread-Instanz
         */
        public Thread newThread(Runnable r) {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        }
    };

    /**
     * Threadpool fuer Netzwerkjobs.
     */
    private final ExecutorService threadPool = Executors.newFixedThreadPool(5, daemonThreadFactory);
    
    /**
     * Erstellt einen neuen Client-Stub.
     *
     * @param marshaller Marshaller fuer Request/Response-Objekte
     * @param ip_address IP-Adresse des Servers
     * @param port UDP-Port des Servers
     */
    public ClientStub(I_Marshaller marshaller, String ip_address, int port) {
        this.marshaller = marshaller;
        this.ip_address = ip_address;
        this.port = port;
    }

    /**
     * Fuehrt die Multiplikation asynchron ueber den entfernten Server aus.
     *
     * @param a erster Faktor
     * @param b zweiter Faktor
     * @return Future mit dem Ergebnis oder einer Exception
     */
    @Override
    public CompletableFuture<Integer> multiply(int a, int b) {
        
        // 1. Erstelle das "Versprechen", das wir SOFORT zurÃ¼ckgeben
        CompletableFuture<Integer> future = new CompletableFuture<>();

        // 2. Definiere den "Job" (deine alte, blockierende Logik)
        Runnable networkJob = () -> {
            try {
                // --- Diese Logik lÃ¤uft jetzt in einem HINTERGRUND-Thread ---
                if (a > Integer.MAX_VALUE || b > Integer.MAX_VALUE || a < Integer.MIN_VALUE || b < Integer.MIN_VALUE) {
                    throw new RuntimeException("Invalid Arguments");
                }
                FunctionBody request = new FunctionBody(a, b, E_Functions.MULTIPLY);
                String json_msg = marshaller.marshall(request);
                
                // UDPClient wird im Hintergrund erstellt und genutzt
                UDPClient udp = new UDPClient(ip_address, port);
                udp.send(json_msg);
                
                // DIESER AUFRUF BLOCKIERT JETZT NUR DEN HINTERGRUND-THREAD
                json_msg = udp.receive();
                
                FunctionBody response = marshaller.unmarshall(json_msg);
                udp.close(); // Socket nach dem Empfang schlieÃŸen

                if (response.result == null) {
                    throw new RuntimeException("No result received from server.");
                }

                // 3. Versprechen mit dem Ergebnis ERFÃœLLEN
                future.complete(response.result);
                // --- Ende der Hintergrund-Logik ---
                
            } catch (Exception e) {
                // 4. Versprechen bei einem Fehler als "gescheitert" markieren
                future.completeExceptionally(e);
            }
        };

        // 5. Ãœbergib den Job an den Thread-Pool zur AusfÃ¼hrung
        threadPool.submit(networkJob);

        // 6. Gib das (noch leere) Versprechen SOFORT zurÃ¼ck
        return future;
    }
}

