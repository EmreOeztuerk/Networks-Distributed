import com.google.gson.Gson;

/**
 * Gson-basierte Implementierung des {@link I_Marshaller}.
 *
 * @author Emre Öztürk
 * @date 04.11.2025
 */
public class Marshaller implements I_Marshaller {
    /**
     * Gson-Instanz fuer JSON-Konvertierung.
     */
    private final Gson gson;

    /**
     * Erstellt einen Marshaller mit Standard-Gson-Konfiguration.
     */
    public Marshaller() {
        gson = new Gson();
    }

    /**
     * Serialisiert ein FunctionBody-Objekt nach JSON.
     *
     * @param request zu serialisierendes Objekt
     * @return JSON-String
     */
    @Override
    public String marshall(FunctionBody request) {
        String json = gson.toJson(request);
        return json;
    }

    /**
     * Deserialisiert einen JSON-String in ein FunctionBody-Objekt.
     *
     * @param json JSON-Darstellung
     * @return deserialisiertes FunctionBody-Objekt
     * @throws RuntimeException bei nicht verarbeitbarem JSON
     */
    @Override
    public FunctionBody unmarshall(String json) throws RuntimeException {
        FunctionBody body = gson.fromJson(json, FunctionBody.class);
        return body;
    }
}

