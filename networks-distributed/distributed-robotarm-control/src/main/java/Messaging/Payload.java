package Messaging;

import java.util.List;

/**
 * Enthält Metadaten fuer Registry-Operationen und RPC-Aufrufe.
 *
 * @author Emre Öztürk
 * @date 16.12.25
 */
public class Payload {
    /** Anzeigename des Endpunkts. */
    public String name;

    /** IP-Adresse des Endpunkts. */
    public String ip;

    /** Port des Endpunkts. */
    public int port;

    /** Fortlaufende ID fuer Clients im Token-Ring. */
    public int id = -1;

    /** Endpunkttyp (CLIENT oder ROBOT_NODE). */
    public E_Endpoint type;

    /** Statusfeld fuer Antworten (z. B. OK oder ERROR). */
    public String status;

    /** Textuelle Nachricht, z. B. Fehlerbeschreibung. */
    public String message;

    /** Liste registrierter Endpunkte bei LIST-Antworten. */
    public List<Payload> list;

    /** Korrelations-ID fuer Request/Response-Zuordnung. */
    public String rpcId;

    /** Gelenkindex fuer MOVE (1..4). */
    public int joint;

    /** Zielwert in Prozent (0..100). */
    public int percentage;

    /**
     * Erzeugt ein leeres Payload-Objekt.
     */
    public Payload() {
    }

    /**
     * Erzeugt ein Payload-Objekt fuer Registrierungsdaten.
     *
     * @param name Anzeigename
     * @param ip IP-Adresse
     * @param port Port
     * @param type Endpunkttyp
     * @param id Endpunkt-ID
     */
    public Payload(String name, String ip, int port, E_Endpoint type, int id) {
        this.name = name;
        this.ip = ip;
        this.port = port;
        this.type = type;
        this.id = id;
    }
}
