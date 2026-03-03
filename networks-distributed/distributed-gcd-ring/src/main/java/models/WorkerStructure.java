package models;

/**
 * Interne Repraesentation eines Workers bei der Ring-Planung.
  * 
 * @author Emre Öztürk
 * @date 25.11.2025
 */
public class WorkerStructure {
    /** Startzahl dieses Workers. */
    public int number;
    /** Eindeutiger Port/Queue-Schluessel. */
    public int port;

    /** Rechter Nachbar im Ring. */
    public WorkerStructure next = null;
    /** Linker Nachbar im Ring. */
    public WorkerStructure prev = null;

    /**
     * Erstellt eine Worker-Struktur fuer den Ring.
     *
     * @param number Startzahl
     * @param port Port/Queue-Schluessel
     */
    public WorkerStructure(int number, int port) {
        this.number = number;
        this.port = port;
    }
}
