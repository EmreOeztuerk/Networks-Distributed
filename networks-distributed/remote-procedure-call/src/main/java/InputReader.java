import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

/**
 * Liest fortlaufend Konsoleneingaben und stellt sie thread-sicher in einer Queue bereit.
 *
 * @author Emre Öztürk
 * @date 04.11.2025
 */
public class InputReader implements Runnable {
    /**
     * Scanner fuer Konsoleneingaben.
     */
    private Scanner scan = new Scanner(System.in);
    /**
     * Queue fuer eingelesene Zahlenpaare.
     */
    private LinkedList<ArrayList<Integer>> queue;
    /**
     * Laufstatus des Readers.
     */
    private volatile boolean isRunning;
    /**
     * Monitorobjekt fuer Synchronisation zwischen Produzent und Konsument.
     */
    private final Object mutex = new Object();
    
    /**
     * Initialisiert Reader, Queue und Laufstatus.
     */
    public InputReader() {
        this.queue = new LinkedList<>();
        this.isRunning = true;
    }

    /**
     * Liefert den naechsten Eintrag aus der Queue oder {@code null} beim Beenden.
     *
     * @return naechstes Zahlenpaar oder {@code null}
     * @throws InterruptedException wenn das Warten auf neue Daten unterbrochen wird
     */
    public ArrayList<Integer> getNextInput() throws InterruptedException {
        synchronized (mutex) {
            while (queue.isEmpty() && isRunning) {
                mutex.wait();
            }
            if (!isRunning && queue.isEmpty()) {
                return null;
            }
            ArrayList<Integer> nextInput = queue.remove();
            return nextInput;
        }
    }

    /**
     * Fragt ein Zahlenpaar interaktiv von der Konsole ab.
     *
     * @return Liste mit zwei Zahlen oder {@code null} bei "exit"
     */
    private List<Integer> checkInput() {
        while (true) {
            try {
                System.out.print("Please enter the first number, or 'exit' to quit: ");
                if (!scan.hasNextLine()) {
                    return new ArrayList<>(); // No input available
                }

                String first_input = scan.nextLine().trim();
                if (first_input.equalsIgnoreCase("exit")) {
                    return null;
                }
                int first_number = Integer.parseInt(first_input);

                System.out.print("Please enter the second number, or 'exit' to quit: ");
                if (!scan.hasNextLine()) {
                    return null; // No input available
                }

                String second_input = scan.nextLine().trim();
                if (second_input.equalsIgnoreCase("exit")) {
                    return null;
                }
                int second_number = Integer.parseInt(second_input);

                return Arrays.asList(first_number, second_number);
            } catch (Exception e) {
                System.out.println("Invalid input! Please enter numbers only.");
            }
        }
    }

    /**
     * Produzenten-Loop fuer Konsoleneingaben.
     */
    @Override
    public void run() {
        while (isRunning) {
            List<Integer> input = checkInput();
            if (input == null) {
                System.out.println("Exit command received, stopping...");
                synchronized (mutex) {
                    isRunning = false;
                    mutex.notifyAll();
                }
                break;
            }
            
            synchronized (mutex) {
                queue.add(new ArrayList<>(input));
                mutex.notifyAll();
            }
        }
        
        scan.close();
    }
    
    /**
     * Stoppt den Reader kontrolliert und weckt wartende Threads auf.
     */
    public void stop() {
        synchronized (mutex) {
            isRunning = false;
            mutex.notifyAll();
        }
    }
}

