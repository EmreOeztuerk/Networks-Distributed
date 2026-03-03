package RobotArm;

/**
 * Schnittstelle zur Ansteuerung eines CaDS-Roboterarms.
 *
 * @author Emre Öztürk
 * @date 16.12.25
 */
public interface ICaDSRoboticArm {
    /**
     * Setzt das Hoch/Runter-Gelenk auf einen Prozentwert.
     *
     * @param var1 Zielwert in Prozent
     */
    void setUpDownPercentageTo(int var1);

    /**
     * Liest den aktuellen Prozentwert des Hoch/Runter-Gelenks.
     *
     * @return aktueller Wert in Prozent
     */
    int getUpDownPercentage();

    /**
     * Setzt das Vor/Zurueck-Gelenk auf einen Prozentwert.
     *
     * @param var1 Zielwert in Prozent
     */
    void setBackForthPercentageTo(int var1);

    /**
     * Liest den aktuellen Prozentwert des Vor/Zurueck-Gelenks.
     *
     * @return aktueller Wert in Prozent
     */
    int getBackForthPercentage();

    /**
     * Setzt das Links/Rechts-Gelenk auf einen Prozentwert.
     *
     * @param var1 Zielwert in Prozent
     */
    void setLeftRightPercentageTo(int var1);

    /**
     * Liest den aktuellen Prozentwert des Links/Rechts-Gelenks.
     *
     * @return aktueller Wert in Prozent
     */
    int getLeftRightPercentage();

    /**
     * Setzt den Greifer (Auf/Zu) auf einen Prozentwert.
     *
     * @param var1 Zielwert in Prozent
     */
    void setOpenClosePercentageTo(int var1);

    /**
     * Liest den aktuellen Prozentwert des Greifers.
     *
     * @return aktueller Wert in Prozent
     */
    int getOpenClosePercentage();

    /**
     * Prueft, ob der Arm erreichbar ist.
     *
     * @return true bei erfolgreichem Heartbeat
     */
    boolean heartbeat();

    /**
     * Gibt Ressourcen des Roboterarms frei.
     */
    void teardown();

    /**
     * Initialisiert den Roboterarm.
     *
     * @return true bei erfolgreicher Initialisierung
     */
    boolean init();

    /**
     * Wartet blockierend, bis die Initialisierung abgeschlossen ist.
     */
    void waitUntilInitIsFinished();
}
