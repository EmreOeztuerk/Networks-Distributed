package Messaging;

/**
 * Modelliert eine Protokollnachricht mit Befehl und Nutzdaten.
 *
 * @author Emre Öztürk
 * @date 16.12.25
 */
public class Message {
    /** Auszufuehrender Befehl (z. B. REGISTER, LIST, MOVE). */
    public E_Query command;

    /** Nutzdaten der Nachricht. */
    public Payload payload;

    /**
     * Erzeugt eine leere Nachricht mit initialisiertem Payload.
     */
    public Message() {
        payload = new Payload();
    }

    /**
     * Erzeugt eine Nachricht mit Befehl und Payload.
     *
     * @param command Nachrichtentyp
     * @param payload Nutzdaten
     */
    public Message(E_Query command, Payload payload) {
        this.command = command;
        this.payload = payload;
    }
}
