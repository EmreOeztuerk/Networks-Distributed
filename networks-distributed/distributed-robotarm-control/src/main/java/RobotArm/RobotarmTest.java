package RobotArm;

import org.cads.vs.roboticArm.hal.ICaDSRoboticArm;
import org.cads.vs.roboticArm.hal.simulation.CaDSRoboticArmSimulation;

/**
 * Fuehrt einen einfachen manuellen Integrationstest fuer den Roboterarm aus.
 *
 * @author Emre Öztürk
 * @date 16.12.25
 */
public class RobotarmTest {

    /**
     * Startet den Testablauf mit mehreren Gelenkbewegungen.
     *
     * @param args Kommandozeilenargumente (ungenutzt)
     */
    public static void main(String[] args) {
        System.out.println("TEST: Starte Roboter-Test...");
        ICaDSRoboticArm arm = new CaDSRoboticArmSimulation();
        Robotarm testRoboter = new Robotarm(arm);

        try {
            Thread.sleep(2000);

            System.out.println("Test: Drehe links...");
            testRoboter.bewegeGelenk(1, 0);
            Thread.sleep(1000);

            System.out.println("Test: Drehe rechts...");
            testRoboter.bewegeGelenk(1, 100);
            Thread.sleep(1000);

            System.out.println("Test: Mitte...");
            testRoboter.bewegeGelenk(1, 50);
            Thread.sleep(1000);

            System.out.println("Test: Greifer auf...");
            testRoboter.bewegeGelenk(4, 100);
            Thread.sleep(1000);

            System.out.println("Test: Greifer zu...");
            testRoboter.bewegeGelenk(4, 50);
            Thread.sleep(1000);

            System.out.println("TEST: Erfolgreich abgeschlossen.");

        } catch (InterruptedException e) {
            System.err.println("TEST: Wurde unterbrochen!");
            e.printStackTrace();
        } finally {
            if (testRoboter != null) {
                testRoboter.teardown();
            }
        }
    }
}
