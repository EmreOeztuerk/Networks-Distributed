package TerminalClient;

import java.io.IOException;

import Messaging.COM;
import Messaging.E_Endpoint;
import Messaging.E_Query;
import Messaging.Message;

/**
 * Kapselt Registrierung und Deregistrierung eines Clients an der Registry.
 *
 * @author Emre Öztürk
 * @date 16.12.25
 */
public class RegistrationUtility {
    private COM com;
    private String server_ip;
    private int server_port;
    private String name;

    /**
     * Erstellt eine neue Utility-Instanz auf Basis eines COM-Objekts.
     *
     * @param com Kommunikationsobjekt
     */
    public RegistrationUtility(COM com) {
        this.com = com;
    }

    /**
     * Registriert den Client bei der Registry.
     *
     * @param server_ip Registry-IP
     * @param server_port Registry-Port
     * @param self_name Name des Clients
     * @return zugewiesene ID oder -1 bei ERROR-Antwort
     * @throws Exception bei unerwarteter Antwort oder Kommunikationsfehler
     */
    public int register(String server_ip, int server_port, String self_name) throws Exception {
        this.server_ip = server_ip;
        this.server_port = server_port;
        this.name = self_name;

        Message msg = new Message();
        msg.payload.name = new String(name);
        msg.payload.type = E_Endpoint.CLIENT;
        msg.command = E_Query.REGISTER;
        com.sendTo(msg, server_ip, server_port);
        Message response = com.get_next_message();
        switch (response.command) {
            case E_Query.REGISTER_RESPONSE:
                return response.payload.id;
            case E_Query.ERROR:
                return -1;
            default:
                throw new Exception("Unexpected response from server.");
        }
    }

    /**
     * Meldet den Client bei der Registry ab.
     *
     * @throws IOException falls der Client nicht registriert ist oder Senden fehlschlaegt
     */
    public void unregister() throws IOException {
        if (server_ip == null || server_port == 0 || name == null) {
            throw new IOException("Client is not yet registered.");
        }

        Message msg = new Message();
        msg.payload.name = new String(name);
        msg.payload.type = E_Endpoint.CLIENT;
        msg.command = E_Query.UNREGISTER;
        com.sendTo(msg, server_ip, server_port);
    }
}
