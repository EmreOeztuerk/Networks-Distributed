package RobotArm;

/**
 * Einfache Mock-Implementierung eines Roboterarms fuer Tests ohne Hardware.
 *
 * @author Emre Öztürk
 * @date 16.12.25
 */
public class CaDSRoboticArmMock implements ICaDSRoboticArm {

    public void setUpDownPercentageTo(int var1) {
        System.out.println("setUpDownPercentageTo: " + var1);
    }

    public int getUpDownPercentage() {
        return 50;
    }

    public void setBackForthPercentageTo(int var1) {
        System.out.println("setBackForthPercentageTo: " + var1);
    }

    public int getBackForthPercentage() {
        return 50;
    }

    public void setLeftRightPercentageTo(int var1) {
        System.out.println("setLeftRightPercentageTo: " + var1);
    }

    public int getLeftRightPercentage() {
        return 50;
    }

    public void setOpenClosePercentageTo(int var1) {
        System.out.println("setOpenClosePercentageTo: " + var1);
    }

    public int getOpenClosePercentage() {
        return 50;
    }

    public boolean heartbeat() {
        return true;
    }

    public void teardown() {
        System.out.println("teardown called");
    }

    public boolean init() {
        System.out.println("init called");
        return true;
    }

    public void waitUntilInitIsFinished() {
        System.out.println("waitUntilInitIsFinished called");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Initialization finished");
    }
}
