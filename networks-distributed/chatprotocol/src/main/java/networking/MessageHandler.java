package networking;

import com.google.gson.JsonObject;
import routing.RoutingTable;

import java.nio.channels.SocketChannel;

public class MessageHandler {

    private void handleChatMessage(JsonObject message, SocketChannel clientChannel) {
        String targetAddress = message.get("TargetAddress").getAsString();
        int targetPort = message.get("TargetPort").getAsInt();

        if (targetAddress.equals(serverAddress) && targetPort == port) {
            System.out.println("Chat message received: " + message.getAsJsonObject("Payload").get("Msg").getAsString());
        } else {
            System.out.println("Forwarding chat message...");
            forwardMessage(message);
        }
    }

    private void handleHelloMessage(JsonObject message) {
        String senderAddress = message.get("SenderAddress").getAsString();
        int senderPort = message.get("SenderPort").getAsInt();

        System.out.println("Hello message received from: " + senderAddress + ":" + senderPort);

        boolean updated = routingTable.updateEntry(senderAddress, senderPort, 1, senderAddress, senderPort);
        if (updated) {
            routingTable.printNeighborsIfUpdated();
            printRoutingTable();
        }
    }

    private void handleRoutingInfo(JsonObject message) {
        JsonObject routes = message.getAsJsonObject("Payload").getAsJsonObject("Routes");
        for (String key : routes.keySet()) {
            JsonObject route = routes.getAsJsonObject(key);
            String targetAddress = route.get("TargetAddress").getAsString();
            int targetPort = route.get("TargetPort").getAsInt();
            int hopCount = route.get("HopCount").getAsInt();

            routingTable.updateEntry(targetAddress, targetPort, hopCount + 1,
                    message.get("SenderAddress").getAsString(),
                    message.get("SenderPort").getAsInt());
        }
    }

    private void handleGoodbyeMessage(JsonObject message) {
        String senderAddress = message.get("SenderAddress").getAsString();
        int senderPort = message.get("SenderPort").getAsInt();

        System.out.println("Goodbye message received from: " + senderAddress + ":" + senderPort);
        routingTable.markAsUnreachable(senderAddress, senderPort);
    }
}
