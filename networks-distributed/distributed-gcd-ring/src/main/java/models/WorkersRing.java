package models;

import java.util.ArrayList;
import java.util.List;

/**
 * Plant die Verteilung der Worker auf Rechner und verknuepft sie als Ring.
  * 
 * @author Emre Öztürk
 * @date 25.11.2025
 */
public class WorkersRing {
    /** Erster Worker im Ring. */
    private WorkerStructure head;
    /** Letzter Worker im Ring. */
    private WorkerStructure tail;

    /** Laufender Portzaehler fuer neue Worker. */
    private int port = 5000;

    /** Geplante Rechner mit jeweils zugewiesenen Workern. */
    public Computer[] computers;

    /**
     * Erstellt einen leeren Ring.
     */
    public WorkersRing() {
        this.head = null;
        this.tail = null;
    }

    /**
     * Erstellt Ring und Verteilung anhand Rechner- und Zahlenliste.
     *
     * @param computers Queue-Namen der Ambassadors
     * @param input_numbers Eingabezahlen fuer die ggT-Berechnung
     */
    public WorkersRing(final String[] computers, final Integer[] input_numbers) {
        this.head = null;
        this.tail = null;
        Computer[] computer_array = new Computer[computers.length];

        for (int i = 0; i < computers.length; i++) {
            List<WorkerStructure> workers_list = new ArrayList<>();
            for (int j = 0; j < input_numbers.length; j++) {
                if (j % computers.length == i) {
                    int current_number = input_numbers[j];
                    WorkerStructure worker = new WorkerStructure(current_number, port++);
                    this.addWorker(worker);
                    workers_list.add(worker);
                }
            }
            computer_array[i] = new Computer(computers[i], workers_list);
        }
        this.computers = computer_array;
    }

    /**
     * Fuegt einen Worker an das Ende des zirkularen Rings an.
     *
     * @param worker einzufuegender Worker
     */
    private void addWorker(WorkerStructure worker) {
        if (head == null) {
            head = worker;
            tail = worker;
            worker.next = worker;
            worker.prev = worker;
        } else {
            tail.next = worker;
            worker.prev = tail;
            worker.next = head;
            head.prev = worker;
            tail = worker;
        }
    }

    /**
     * Gibt die geplante Ringstruktur in der Konsole aus.
     */
    public void printRing() {
        if (head == null) {
            System.out.println("The ring is empty.");
            return;
        }

        System.out.println("=== Computers Ring ===");
        for (final Computer computer : computers) {
            System.out.println("Computer ID: " + computer.name);
            System.out.print("Workers' numbers: ");
            for (final WorkerStructure worker : computer.workers_list) {
                System.out.print(worker.number + " ");
            }
            System.out.println();
        }

        System.out.printf("Circular connection: Head(%d) <-> Tail(%d)\n", head.number, tail.number);
    }
}
