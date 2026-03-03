/**
 * Transportobjekt fuer Request- und Response-Daten im RPC-Protokoll.
 *
 * @author Emre Öztürk
 * @date 04.11.2025
 */
public class FunctionBody {

    /**
     * Leerer Standardkonstruktor, z. B. fuer JSON-Deserialisierung.
     */
    public FunctionBody(){}

    /**
     * Erstellt einen Request mit zwei Operanden und Funktionskennung.
     *
     * @param a erster Operand
     * @param b zweiter Operand
     * @param function Funktionskennung
     */
    public FunctionBody(int a, int b, E_Functions function){
        this.a = a;
        this.b = b;
        this.function = function;
    }

    /**
     * Erster Operand.
     */
    public int a;
    /**
     * Zweiter Operand.
     */
    public int b;
    /**
     * Aufzurufende Funktion.
     */
    public E_Functions function;
    /**
     * Ergebniswert der Antwort, kann bei Requests {@code null} sein.
     */
    public Integer result = null;

    /**
     * Kopierkonstruktor fuer bestehende Transportobjekte.
     *
     * @param other zu kopierendes Objekt
     */
    public FunctionBody(FunctionBody other) {
        this.a = other.a;
        this.b = other.b;
        this.function = other.function;
    }
}

