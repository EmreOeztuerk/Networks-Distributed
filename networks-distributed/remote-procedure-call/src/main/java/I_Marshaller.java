/**
 * Schnittstelle fuer die Umwandlung zwischen Transportobjekten und JSON.
 *
 * @author Emre Öztürk
 * @date 04.11.2025
 */
public interface I_Marshaller {
    /**
     * Serialisiert ein Request-/Response-Objekt in JSON.
     *
     * @param request zu serialisierendes Objekt
     * @return JSON-Darstellung als String
     */
    String marshall(FunctionBody request);
    
    

    /**
     * Deserialisiert einen JSON-String in ein Transportobjekt.
     *
     * @param json JSON-String mit Request- oder Response-Daten
     * @return deserialisiertes Transportobjekt
     * @throws RuntimeException bei ungueltigem oder nicht verarbeitbarem JSON
     */
    FunctionBody unmarshall(String json) throws RuntimeException;
}

