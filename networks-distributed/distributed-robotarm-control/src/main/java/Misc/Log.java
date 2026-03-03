package Misc;

/**
 * Einfacher Logger für Konsolenausgaben mit Tagging.
 *
 * @author Emre Öztürk
 * @date 16.12.25
 */
public class Log {
    private String tag;
    private boolean enabled = true;

    public Log(String tag) {
        this.tag = tag;
    }

    public void info(String message) {
        if (!enabled) return;
        System.out.println(String.format("[%-6s]  INFO: %s", tag, message));
    }

    public void error(String message) {
        if (!enabled) return;
        System.err.println(String.format("[%-6s]  ERROR: %s", tag, message));
    }

    public void warning(String message) {
        if (!enabled) return;
        System.out.println(String.format("[%-6s]  WARNING: %s", tag, message));
    }

    public void disable() {
        enabled = false;
    }

    public void enable() {
        enabled = true;
    }
}
