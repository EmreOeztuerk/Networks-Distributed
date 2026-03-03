import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

/**
 * Unit-Tests fuer Marshalling-Funktionalitaet.
 *
 * @author Emre Öztürk
 * @date 04.11.2025
 */
public class Tests {
    /**
     * Test-Marshaller.
     */
    private I_Marshaller marshaller = new Marshaller();

    /**
     * Prüft, ob ein FunctionBody korrekt nach JSON serialisiert wird.
     */
    @Test
    void testAdd() {
        FunctionBody request = new FunctionBody(2, 3, E_Functions.MULTIPLY);
        String json = marshaller.marshall(request);
        System.out.println("Marshalled JSON: " + json);
        assertThat(json)
        .contains("\"a\":2")
        .contains("\"b\":3")
        .contains("\"function\":\"MULTIPLY\"");
    }
}   

