import java.util.concurrent.CompletableFuture; // Importieren

/**
 * Serverseitige Implementierung der Multiplikationslogik.
 *
 * @author Emre Öztürk
 * @date 04.11.2025
 */
public class Server implements Datastore {

    /**
     * Berechnet das Multiplikationsergebnis lokal auf dem Server.
     *
     * @param a erster Faktor
     * @param b zweiter Faktor
     * @return abgeschlossenes Future mit dem Ergebnis
     */
    @Override
    public CompletableFuture<Integer> multiply(int a, int b) {
        //System.out.println("Logik fÃ¼hre aus: " + a + " * " + b);
        int result = a * b;
        
        // Da wir das Ergebnis sofort haben, geben wir ein
        // bereits "erfülltes" Future zurück.
        return CompletableFuture.completedFuture(result);
    }
}

