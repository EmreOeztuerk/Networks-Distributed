package models;

/**
 * Serialisierbares Konfigurationsobjekt fuer einen Worker-Prozess.
  * 
 * @author Emre Öztürk
 * @date 25.11.2025
 */
public class WorkerDTO {
    /** Startwert des Workers. */
    public int number;
    /** Eigener Port bzw. Queue-Schluessel des Workers. */
    public int port;

    /** Queue/Port des rechten Nachbarn im Ring. */
    public String next = null;
    /** Queue/Port des linken Nachbarn im Ring. */
    public String prev = null;

    /**
     * Erstellt ein DTO fuer den Worker-Start.
     *
     * @param number Startwert des Workers
     * @param port eigener Port des Workers
     */
    public WorkerDTO(int number, int port) {
        this.number = number;
        this.port = port;
    }
}
