/**
 * Serverseitiges Skeleton fuer den UDP-basierten RPC-Ablauf.
 * Nimmt Requests entgegen, fuehrt die Logik aus und sendet Responses zurueck.
 *
 * @author Emre \u00D6zt\u00FCrk
 * @date 04.11.2025
 */
public class Skeleton {

    /**
     * Startet den RPC-Server auf Port 1234.
     *
     * @param args Kommandozeilenargumente (aktuell ungenutzt)
     * @throws Exception bei Fehlern in Netzwerkkommunikation oder Thread-Unterbrechung
     */
    public static void main(String[] args) throws Exception {
        I_Marshaller marshaller = new Marshaller();
        UDPServer udpServer = new UDPServer(1234);
        System.out.println("Skeleton is running on: " + udpServer.getServerIP() + ":"+"1234");

        while(true){
            // 1. Auf Anfrage warten
            String requestJSON = udpServer.listen(); //
            // 2. Unmarshall
            FunctionBody incoming_request = marshaller.unmarshall(requestJSON);
            int a = incoming_request.a;
            int b = incoming_request.b;
            // --- HIER IST DER TEST ---
            System.out.println("[Server] Anfrage erhalten. Schlafe jetzt 3 Sekunden...");
            Thread.sleep(3000); // Simuliert eine langsame Berechnung
            // 3. Echte Implementierung aufrufen
            Datastore server = new Server();
            int result = server.multiply(a, b).get();

            // 4. Antwort vorbereiten
            FunctionBody response = new FunctionBody(incoming_request);
            response.result = result;

            // 5. Marshall
            String responseJSON = marshaller.marshall(response);

            // 6. Antwort senden
            udpServer.respond(responseJSON);
        }
    }
}

