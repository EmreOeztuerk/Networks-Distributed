package Messaging;

/**
 * Enthält die unterstützten Nachrichtentypen des Protokolls.
 *
 * @author Emre Öztürk
 * @date 16.12.25
 */
public enum E_Query {
    REGISTER,
    REGISTER_RESPONSE,
    TOKEN,
    UNREGISTER,
    LIST,
    MOVE,
    MOVE_RESPONSE,
    ERROR
}
