import java.util.concurrent.CompletableFuture;

/**
 * Lokale Datastore-Implementierung fuer Multiplikationen ohne Netzwerkzugriff.
 *
 * @author Emre Öztürk
 * @date 04.11.2025
 */
public class DatastoreImplementation implements Datastore {

    /**
     * Multipliziert zwei Zahlen lokal und liefert ein bereits abgeschlossenes Future.
     *
     * @param a erster Faktor
     * @param b zweiter Faktor
     * @return abgeschlossenes Future mit dem Ergebnis
     */
    @Override
    public CompletableFuture<Integer> multiply(int a, int b) {
        int result = a * b;

        // Da wir das Ergebnis sofort haben, geben wir ein
        // bereits "erfÃ¼lltes" Future zurÃ¼ck.
        return CompletableFuture.completedFuture(result);
    }
}

