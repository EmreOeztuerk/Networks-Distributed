package RobotArm;

import org.cads.vs.roboticArm.hal.ICaDSRoboticArm;

/**
 * Kapselt die Steuerlogik fuer einen Roboterarm.
 *
 * @author Emre Öztürk
 * @date 16.12.25
 */
public class Robotarm {

    private ICaDSRoboticArm arm;

    /**
     * Initialisiert den Roboterarm und setzt eine neutrale Startposition.
     *
     * @param arm konkrete Roboterarm-Implementierung
     */
    public Robotarm(ICaDSRoboticArm arm) {
        System.out.println("[Hardware] Initialisiere Simulation...");

        this.arm = arm;
        this.arm.init();
        this.arm.waitUntilInitIsFinished();
        bewegeGelenk(1, 50);
        bewegeGelenk(2, 50);
        bewegeGelenk(3, 50);
        bewegeGelenk(4, 50);
    }

    /**
     * Bewegt ein Gelenk auf einen Prozentwert.
     *
     * @param gelenkNummer 1=Basis, 2=Hoehe, 3=Weite, 4=Greifer
     * @param wert Prozentwert (0-100)
     */
    public void bewegeGelenk(int gelenkNummer, int wert) {
        if (wert < 0) {
            wert = 0;
        }
        if (wert > 100) {
            wert = 100;
        }

        System.out.println("[Hardware] Setze Gelenk " + gelenkNummer + " auf " + wert + "%");

        switch (gelenkNummer) {
            case 1:
                arm.setLeftRightPercentageTo(wert);
                break;
            case 2:
                arm.setUpDownPercentageTo(wert);
                break;
            case 3:
                arm.setBackForthPercentageTo(wert);
                break;
            case 4:
                arm.setOpenClosePercentageTo(wert);
                break;
            default:
                System.out.println("Unbekanntes Gelenk: " + gelenkNummer);
        }
    }

    /**
     * Faehrt den Arm in eine Parkposition und beendet die Arm-Instanz.
     */
    public void teardown() {
        System.out.println("[Hardware] Teardown: Parkposition & Shutdown.");
        bewegeGelenk(1, 50);
        bewegeGelenk(2, 0);
        bewegeGelenk(3, 0);
        bewegeGelenk(4, 0);
        arm.teardown();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        System.exit(0);
    }
}
