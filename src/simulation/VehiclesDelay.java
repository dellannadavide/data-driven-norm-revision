package simulation;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class VehiclesDelay {
    /**
     * A utils class used to keep vehicles (agents) in a delay
     * Delay can be used a simple list, but more importantly
     * it is used to make sure that agents in the simulation are the synch with the agents in SUMO
     * (it may happen in SUMO that some vehicles entering the road are actually discarded)
     */
    int steps_delay;
    LinkedHashMap<Integer, ArrayList<HighwayVehicle>> delayed = new LinkedHashMap<>();

    public VehiclesDelay(int steps) {
        /**
         * Constructor.
         * If steps==-1 then the delay is simply a list
         * otherwise every agent is kept in the delay for steps steps
         */
        this.steps_delay = steps;
        if(steps_delay==-1) { // in this case the delay acts as a simple queue
            delayed.put(0, new ArrayList<>());
        }
        else {
            for (int i = 0; i < this.steps_delay; i++) {
                delayed.put(i, new ArrayList<>());
            }
        }
    }
    public void take (HighwayVehicle v) {
        /**
         * Function to add new vehicle in the delay
         */
        delayed.get(this.delayed.size()-1).add(v);
    }

    public ArrayList<HighwayVehicle> step() {
        /**
         * Progresses agents in the delay, until they have spent enough steps.
         * At that point they are removed from the delay and returned
         */
        ArrayList<HighwayVehicle> extracted = new ArrayList<>();
        if(steps_delay>-1) { //I only extract vehicles if the delay is not a simple queue, otherwise the vehicles will be extracted only if they are called out via stopDelay
            extracted = (ArrayList<HighwayVehicle>) delayed.get(0).clone();
            for (int i = 1; i < this.delayed.size(); i++) {
                delayed.replace(i - 1, (ArrayList<HighwayVehicle>) delayed.get(i).clone());
            }
            delayed.get(delayed.size()-1).clear();
        }
        return extracted;
    }

    public HighwayVehicle stopDelay(String vid) {
        /**
         * Removes a vehicle with id vid from the delay
         */
        HighwayVehicle vehicle = null;
        for (int i = 0; i < this.delayed.size(); i++) {
            for (HighwayVehicle v:
                 delayed.get(i)) {
                if(v.id.equals(vid)) {
                    vehicle = v;
                    delayed.get(i).remove(v);
                    return vehicle;
                }
            }
        }
        return vehicle;
    }

}
