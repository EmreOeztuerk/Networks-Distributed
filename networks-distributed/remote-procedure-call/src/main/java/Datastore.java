import java.util.concurrent.CompletableFuture;

/**
 * Abstraktion fuer Multiplikationsoperationen, die asynchron ein Ergebnis liefern.
 *
 * @author Emre Öztürk
 * @date 04.11.2025
 */
public interface Datastore {
    /**
     * Multipliziert zwei ganze Zahlen.
     *
     * @param a erster Faktor
     * @param b zweiter Faktor
     * @return Future mit dem Multiplikationsergebnis
     */
    CompletableFuture<Integer> multiply (int a, int b);
}

