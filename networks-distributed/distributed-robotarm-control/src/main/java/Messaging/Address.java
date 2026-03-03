package Messaging;

/**
 * Repräsentiert eine Netzwerkadresse aus IP und Port.
 *
 * @author Emre Öztürk
 * @date 16.12.25
 */
public class Address {
    /** IP-Adresse des Zielknotens. */
    public String ip;

    /** Port des Zielknotens. */
    public int port;

    /**
     * Erzeugt eine neue Adresse.
     *
     * @param ip IP-Adresse
     * @param port Port
     */
    public Address(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }
}
