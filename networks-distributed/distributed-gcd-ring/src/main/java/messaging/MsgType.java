package messaging;

/**
 * Alle im System verwendeten Nachrichtentypen.
  * 
 * @author Emre Öztürk
 * @date 25.11.2025
 */
public enum MsgType {
    /** Anfrage zum Starten eines Workers mit Konfiguration. */
    SPAWN,
    /** Startsignal fuer bereits konfigurierte Worker. */
    START,
    /** Anfrage nach aktuellem Worker-Wert. */
    QUERY,
    /** Terminierungssignal fuer laufende Prozesse. */
    KILL,
    /** Aktualisierung eines Worker-Werts im Ring. */
    UPDATE,
    /** Antwortnachricht eines Workers an den Chef. */
    RESULT
}
