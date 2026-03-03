package core;

import javax.jms.JMSException;

import messaging.Message;

/**
 * Abstrahiert die Kommunikation mit der Messaging-Middleware.
 * <p>
 * Der Algorithmus bleibt dadurch unabhaengig von der konkreten Technologie
 * (z. B. ActiveMQ, RabbitMQ).
 * 
 * @author Emre Öztürk
 * @date 25.11.2025
 */
public interface IMessageBroker {

    /**
     * Registriert den Callback fuer eingehende Nachrichten.
     *
     * @param listener Zielobjekt fuer empfangene Nachrichten
     */
    void setListener(INetworkListener listener);

    /**
     * Baut die Verbindung zum Broker auf und startet den Empfang.
     *
     * @throws JMSException falls der Broker nicht erreichbar ist
     */
    void start() throws JMSException;

    /**
     * Sendet eine Nachricht an eine Ziel-Queue.
     *
     * @param targetQueue Name der Ziel-Queue
     * @param msg zu sendendes Nachrichtenobjekt
     */
    void send(String targetQueue, Message msg);

    /**
     * Schliesst die Broker-Verbindung und gibt Ressourcen frei.
     */
    void close();
}
