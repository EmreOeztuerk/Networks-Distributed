package models;

import java.util.List;

/**
 * Reprasentiert einen Rechner bzw. Ambassador mit zugewiesenen Workern.
  * 
 * @author Emre Öztürk
 * @date 25.11.2025
 */
public class Computer {
    /** Queue-Name des Ambassadors. */
    public String name;
    /** Liste der Worker, die diesem Rechner zugeordnet sind. */
    public List<WorkerStructure> workers_list;

    /**
     * Erstellt ein Computer-Modell fuer die Verteilungsplanung.
     *
     * @param id Queue-Name des Ambassadors
     * @param workers_list diesem Rechner zugewiesene Worker
     */
    public Computer(String id, List<WorkerStructure> workers_list) {
        this.name = id;
        this.workers_list = workers_list;
    }
}
