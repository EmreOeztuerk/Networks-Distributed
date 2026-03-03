package core;

import messaging.Message;

/**
 * Callback-Schnittstelle fuer eingehende Netzwerk-Nachrichten.
 * 
 * @author Emre Öztürk
 * @date 25.11.2025
 */
public interface INetworkListener {

    /**
     * Wird aufgerufen, sobald eine Nachricht empfangen wurde.
     *
     * @param msg empfangene und deserialisierte Nachricht
     */
    void onMessage(Message msg);
}
