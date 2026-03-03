package messaging;

import java.util.List;

/**
 * Transportobjekt fuer alle Nachrichten zwischen Chef, Ambassador und Worker.
  * 
 * @author Emre Öztürk
 * @date 25.11.2025
 */
public class Message {

    /** Nachrichtentyp zur Steuerung der Verarbeitung. */
    public MsgType type;
    /** Queue-Name des Senders. */
    public String sender;
    /** Numerischer Nutzwert, z. B. aktueller Worker-Wert. */
    public int value;
    /** Zielport fuer Worker-relevante Nachrichten. */
    public int targetPort;
    /** JSON-Konfiguration fuer Worker-Start (SPAWN). */
    public String configJson;
    /** Optionale Nachbar-Queues (falls verwendet). */
    public List<String> neighbors;

    /**
     * Leerer Konstruktor fuer Serialisierung/Deserialisierung.
     */
    public Message() {}
}
