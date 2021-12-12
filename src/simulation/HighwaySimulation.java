package simulation;

import de.tudresden.sumo.cmd.Lane;
import de.tudresden.sumo.cmd.Simulation;
import de.tudresden.sumo.cmd.Vehicle;
import de.tudresden.ws.container.SumoLeader;
import it.polito.appeal.traci.SumoTraciConnection;

import java.util.*;

public class HighwaySimulation {
	/**
	 * Class implementing a simulation of the highway with SUMO traffic simulator
	 */

	/** Parameters **/
	int  maxNrAgents = 400; //note it's 400 but they will be reused as new agents
	double  trucks;
	double  viol;
	int  observationPeriod;
	double  vehiclesRate;
	Configuration systemConfig;
	Random r;

	SumoTraciConnection conn;
	int tick = 0;
	int nextVehicleIndex = 0;
	Map<String, HighwayVehicle> vehicles = new HashMap<>();
	int arrivedStep = 0;
	Queue<HighwayVehicle> agentsQueue = new LinkedList<>();
	ArrayList <Trace> labeledTraces = new ArrayList<Trace>();
	LinkedHashMap <String,Trace> inProgressTraces = new LinkedHashMap<String, Trace>();
	ArrayList <Integer> arrivedSteps = new ArrayList<Integer>();
	ArrayList <Double> arrivedStepsTravelTime = new ArrayList<Double>();

	int syncJavaSUMO_delay_steps = 4; //a delay just to be sure the sim is synchronized with sumo
	VehiclesDelay syncJavaSUMO_delay = new VehiclesDelay(syncJavaSUMO_delay_steps);
	VehiclesDelay highway = new VehiclesDelay(-1);


	public HighwaySimulation (Configuration systemConfig, double trucks, double viol, int observationPeriod, double vehiclesRate, Random r) {
		/**
		 * Startup. Initialize the random generator and the systemConfig (the simulation.norms)
		 */
		this.r = r;
		this.systemConfig = systemConfig;
		this.trucks = trucks;
		this.viol = viol;
		this.observationPeriod = observationPeriod;
		this.vehiclesRate = vehiclesRate;

		/** Initializing SUMO and the agents **/
//		String sumo_bin = "sumo-gui.exe";
		String sumo_bin = "sumo.exe";
		String config_file = "map/highway.sumo.cfg";
		try{
			conn = new SumoTraciConnection(sumo_bin, config_file);
			conn.addOption("step-length", "1");
			conn.addOption("start", ""); //start sumo immediately
			conn.addOption("collision.action", "none"); //what to do in case of collision
			conn.addOption("max-depart-delay", "0"); //what to do if the vehicle can't enter
			conn.runServer();
			//INIT AGENTS that will be used thoughout the simulation (some of them will be reused, with new ids, after they exit the highway
			for(int i=0; i<maxNrAgents; i++)
				agentsQueue.add(new HighwayVehicle(r));
		} catch(Exception ex){
			ex.printStackTrace(); 
			System.exit(0);
		}
	}


	public ArrayList<Trace> run() {
		/**
		 *
		 * This is the function that runs the simulation. It is invoked externally, and returns a dataset of traces
		 */
		System.out.println("Running the system with config: "+systemConfig);
		/*Perform one simulation step until enough data is collected */
		while (labeledTraces.size()<=observationPeriod) {
			doStep();
		}
		/* Here enough data is collected */
		finishSimulation();
		return labeledTraces;
	}
	private void finishSimulation() {
		conn.close();
	}

	private void doStep() {
		/**
		 * Function to advance the simulation of one step
		 */
		try {
			// do a step in sumo
			conn.do_timestep();
			tick++;
			//agents currently in the sim
			List<String> presentIds = (List<String>) conn.do_job_get(Vehicle.getIDList());
			//collect the state traces of the agents
			updateStates(presentIds);
			//make them process the tick (e.g., to adapt their speed to the regulation)
			processTick(presentIds);
			//move in the flow the agents that exited the highway
			List<String> removedIds = (List<String>)conn.do_job_get(Simulation.getArrivedIDList());
			setArrived(removedIds);
			//add the co2emissions in this state
		} catch(Exception ex){ ex.printStackTrace(); System.exit(0);}

		/* Adding to the simulation vehiclesRate at the current step*/
		for(int i=0; i<vehiclesRate; i++) {
			HighwayVehicle v = agentsQueue.remove(); //take a new agent form the list of agents
			try {
				/*initialize the agent and add it to the simulation*/
				String id = "veh-"+nextVehicleIndex;
				v.id = id;
				String type = r.nextDouble()>trucks?"car":"truck";
				v.type = type;
				conn.do_job_set(Vehicle.add(id, v.type, "r1", (int)conn.do_job_get(Simulation.getCurrentTime()), 0, 9, (byte) -2));
				v.defaultMaxSpeed = (double)conn.do_job_get(Vehicle.getMaxSpeed(id));
				v.defaultMinDist = (double)conn.do_job_get(Vehicle.getMinGap(id));
				v.determinePreferences(systemConfig.normsTypes.keySet(), viol);
				v.init_step = tick;
				vehicles.put(id, v);
				nextVehicleIndex++;
				/* And I add each of them in a delay, so to be sure to synchronize with SUMO*/
				syncJavaSUMO_delay.take(v);
			} catch(Exception ex){ ex.printStackTrace();}
		}
		/* Check if those that exit the delay at the current step are in sumo */
		ArrayList<HighwayVehicle> entered = syncJavaSUMO_delay.step();
		for (HighwayVehicle v: entered) {
			if(vehicleEntered(v.id)) { //if they are in sumo I add them to the highway queue
				highway.take(v);
			}
			else {
				agentsQueue.add(v); //otherwise they could not enter, so I readd them to the agentsQueue for reuse
			}
		}

	}

	void processTick( List<String> ids ) {
		/**
		 * For every vehicle in the list I execute a simulation step
		 */
		for(String id : ids) {
			vehicles.get(id).processTick(systemConfig, conn);
		}
	}

	boolean vehicleEntered( String agent_id ) {
		/**
		 * Function that returns true if vehicle with id agent_id is currently in the SUMO simulation
		 */
		try{
			//agents currently in the sim
			List<String> presentIds = (List<String>) conn.do_job_get(Vehicle.getIDList());
			return presentIds.contains(agent_id);
		} catch(Exception ex){ex.printStackTrace();}
		return false;
	}

	void setArrived( List<String> ids ) {
		/**
		 * FUnction called for all agents that exited the simulation at the current step.
		 * This function calls the function to produce the trace for the agent
		 */
		arrivedStep = ids.size();
		ids.forEach(id->{
			/*for the travel time*/
			(vehicles.get(id)).end_step = tick;
			arrivedStepsTravelTime.add((tick - (vehicles.get(id)).init_step)*1.0);
			/**/
			//highway_old.stopDelay(vehicles.get(id));
			HighwayVehicle v = highway.stopDelay(id);
			vehicles.remove(id);
			if(v!=null) {
				produceLabeledTrace(v);
				agentsQueue.add(v);
			}
		});
		arrivedSteps.add(arrivedStep);
	}

	void produceLabeledTrace( HighwayVehicle vehicle ) {
		/**
		 * Function that given a vehicle/agent that exited the simulation, produces a trace that describes its behavior
		 */
		if(inProgressTraces.containsKey(vehicle.id)) {
			labeledTraces.add(inProgressTraces.get(vehicle.id));
			inProgressTraces.remove(vehicle.id);
		}
	}

	void updateStates( List<String> ids ) {
		/**
		 * For each agent/vehicle with id id, updates its inProgressTrace by adding info to the current state
		 */
		ids.forEach(id->{
			try {
				String lane = (String)conn.do_job_get(Vehicle.getLaneID(id));
				String edge = (String)conn.do_job_get(Lane.getEdgeID(lane));
				if(edge.contains("km")) {
					int edgeIndex = getEdgeIndex(edge);
					double speed = (double)conn.do_job_get(Vehicle.getSpeed(id));
					double lead_dist = ((SumoLeader)conn.do_job_get(Vehicle.getLeader(id, 300.0))).dist;
					double travel_time = vehicles.get(id).init_step;
					double co2 = (double)conn.do_job_get(Vehicle.getCO2Emission(id))/1000.0; //convert from mg/s to g/s
					travel_time = tick-travel_time;
					if(inProgressTraces.containsKey(id)) { //inProgressTrace already created
						Trace aipTrace = inProgressTraces.get(id);
						if(aipTrace.getLength()==edgeIndex) {
							//The last state is about the current edge
							//I update it with the current speed
							//(it updates only if its bigger, since we are enforcing a norm on the speed limit)
							//todo davide note the comment above. this is inconsistent with what you write now in the paper
							aipTrace.updateLastStateSpeed(speed);
							//same for the distance
							aipTrace.updateLastStateDist(lead_dist);
							//i also update the co2emission, again only updates if its bigger
							aipTrace.updateLastStateCO2Emission(co2);
							//and same thing for time
							aipTrace.updateLastStateTime(travel_time);
						}
						else {
							//the current edge is new -> i create a new state
							aipTrace.add(new State(edge, speed, lead_dist, vehicles.get(id).type, co2, travel_time));
						}
					}
					else { //need to create a new trace, but only if it starts from the first edge
						if(edgeIndex==1 && travel_time>=0) {
							inProgressTraces.put(id, new Trace(new State(edge, speed, lead_dist, vehicles.get(id).type, co2, travel_time)));
						}
					}
				}
			} catch(Exception ex){ex.printStackTrace();}
		});
	}

	int getEdgeIndex( String edge ) {
		return Integer.parseInt(edge.replace("km",""));
	}
}