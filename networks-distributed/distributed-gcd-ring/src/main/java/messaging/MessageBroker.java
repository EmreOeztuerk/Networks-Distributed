package messaging;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;

import com.google.gson.Gson;

import core.IMessageBroker;
import core.INetworkListener;

/**
 * ActiveMQ-basierte Implementierung von {@link IMessageBroker}.
 * 
 * @author Emre Öztürk
 * @date 25.11.2025
 */
public class MessageBroker implements IMessageBroker {

    /** Vollstaendige Broker-URL im ActiveMQ-TCP-Format. */
    private String brokerUrl;
    /** Eigener Queue-Name fuer eingehende Nachrichten. */
    private String myQueueName;
    /** JMS-Verbindung zum Broker. */
    private Connection connection;
    /** JMS-Session fuer Producer/Consumer. */
    private Session session;
    /** Producer zum Versenden von Nachrichten an beliebige Queues. */
    private MessageProducer producer;
    /** JSON-Serializer/Deserializer fuer Transportobjekte. */
    private Gson gson = new Gson();
    /** Callback fuer eingehende Nachrichten. */
    private INetworkListener listener;

    /**
     * Erstellt einen Broker fuer einen bestimmten Queue-Endpunkt.
     *
     * @param brokerIp IP-Adresse des ActiveMQ-Brokers
     * @param myQueueName Name der lokalen Queue
     */
    public MessageBroker(String brokerIp, String myQueueName) {
        String host = (brokerIp == null || brokerIp.isEmpty()) ? "localhost" : brokerIp;
        this.brokerUrl = "tcp://" + host + ":61616";
        this.myQueueName = myQueueName;
    }

    /**
     * Setzt den Callback fuer eingehende Nachrichten.
     *
     * @param listener Listener-Implementierung
     */
    @Override
    public void setListener(INetworkListener listener) {
        this.listener = listener;
    }

    /**
     * Baut die ActiveMQ-Verbindung auf und aktiviert den Nachrichtenempfang.
     *
     * @throws JMSException falls die Initialisierung fehlschlaegt
     */
    @Override
    public void start() throws JMSException {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
        connection = factory.createConnection();
        connection.start();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        if (this.listener != null) {
            Destination myDest = session.createQueue(myQueueName);
            MessageConsumer consumer = session.createConsumer(myDest);

            consumer.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(javax.jms.Message jmsMsg) {
                    try {
                        if (jmsMsg instanceof TextMessage) {
                            String json = ((TextMessage) jmsMsg).getText();
                            Message msg = gson.fromJson(json, Message.class);
                            listener.onMessage(msg);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        producer = session.createProducer(null);
        System.out.println("[Net] Verbunden. Queue: " + myQueueName);
    }

    /**
     * Sendet eine Nachricht an die Ziel-Queue.
     *
     * @param targetQueue Name der Ziel-Queue
     * @param msg zu sendende Nachricht
     */
    @Override
    public synchronized void send(String targetQueue, Message msg) {
        try {
            Destination target = session.createQueue(targetQueue);
            String json = gson.toJson(msg);
            TextMessage textMsg = session.createTextMessage(json);
            if (producer != null) {
                producer.send(target, textMsg);
            }

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    /**
     * Schliesst die Broker-Verbindung.
     */
    @Override
    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (Exception e) {
            // no-op
        }
    }
}
