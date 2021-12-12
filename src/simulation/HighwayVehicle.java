package simulation;

import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Vehicle;
import de.tudresden.sumo.cmd.Lane;

import java.util.*;

public class HighwayVehicle {
	/**
	 * This class implements Agents that will represent vehicles in the highway.
	 * The agents are associated to a type (e.g., car or truck +
	 * a probability that determines if the agent will be ignoring or not the norms) which will deterimne their behavior
	 */
	
	String id;
	String type;
	double defaultMaxSpeed;
	double defaultMinDist;
	LinkedHashMap<String, Boolean> isViolating = new LinkedHashMap<String, Boolean>();
	int init_step;
	int end_step;
	Random r;


	public HighwayVehicle(Random r) {
		this.r = r;
	}

	public void processTick(Configuration config, SumoTraciConnection conn) {
		/**
		 * Called at every simulation step. It progresses the vehicle in the highway.
		 * It also determines the max speed and min distance of the vehicle based on the current norms
		 * (this allows norms to change runtime, even though still a todo for now)
		 */
		Iterator<Map.Entry<String, DNFNorm>> iter = config.getMap().entrySet().iterator();
		while (iter.hasNext()) { //for every norm
			Map.Entry<String, DNFNorm> n_map = iter.next();
			//if an agent is a violating agent, it doesn't matter it will keep its default speed
			//so I don't do anything
			try {
				if(isViolating.get(n_map.getKey())) {
					if(n_map.getValue() instanceof MaxSpeedNorm)
						conn.do_job_set(Vehicle.setMaxSpeed(id, defaultMaxSpeed));
					else if(n_map.getValue() instanceof MinDistNorm)
						conn.do_job_set(Vehicle.setMinGap(id, defaultMinDist));
				} 
				else { //the agents wants to obey the norm
					String lane = (String)conn.do_job_get(Vehicle.getLaneID(id));
					String edge = (String)conn.do_job_get(Lane.getEdgeID(lane));
					if(edge.contains("km")) {
						int kmIndex = Integer.parseInt(edge.replace("km",""));
						int condpos = -1;
						int deadpos = -1;
						
						if(n_map.getValue() instanceof MaxSpeedNorm) {
							//if before condition or after deadline, or the norm doesn't applie to the agent type: keep the default max speed
							condpos = Integer.parseInt(((SimpleDNFNorm)n_map.getValue()).getSimpleCondition().getLiterals().get(MaxSpeedNorm.COND_POS).replace("km", ""));
							deadpos = Integer.parseInt(((SimpleDNFNorm)n_map.getValue()).getSimpleDeadline().getLiterals().get(MaxSpeedNorm.DEAD_POS).replace("km", ""));
							
							
							//NOTE!!!! the -1 for the condition. I am implementating an agent that sets its maximum speed starting from 1 km BEFORE the norm is detached.
							//this is a sort of lookahead, and helps achieving the correct number of violations
							//it avoids situations where the agent sets the speed when it is then too late to avoid to violate the norm
							if(kmIndex<condpos-1 || kmIndex>=deadpos || !n_map.getValue().appliesToType(type)) { 
								conn.do_job_set(Vehicle.setMaxSpeed(id, defaultMaxSpeed));
							}
							else {//if norm applies AND DETACHED
								//double speed = (double)conn.do_job_get(Vehicle.getSpeed(id));
								//conn.do_job_set(Vehicle.setMaxSpeed(id, Math.min(defaultMaxSpeed, ((simulation.MaxSpeedNorm)n_map.getValue()).getSpeed())));
								conn.do_job_set(Vehicle.setMaxSpeed(id, Math.min(defaultMaxSpeed, ((MaxSpeedNorm)n_map.getValue()).getSpeed())));
							}
								
						}
						else if(n_map.getValue() instanceof MinDistNorm) {
							condpos = Integer.parseInt(((SimpleDNFNorm)n_map.getValue()).getSimpleCondition().getLiterals().get(MinDistNorm.COND_POS).replace("km", ""));
							deadpos = Integer.parseInt(((SimpleDNFNorm)n_map.getValue()).getSimpleDeadline().getLiterals().get(MinDistNorm.DEAD_POS).replace("km", ""));
							
							if(kmIndex<condpos-1 || kmIndex>=deadpos || !n_map.getValue().appliesToType(type)) {
								conn.do_job_set(Vehicle.setMinGap(id, defaultMinDist));
							}
							else
								conn.do_job_set(Vehicle.setMinGap(id, Math.max(defaultMinDist, ((MinDistNorm)n_map.getValue()).getDist())));
						}
					}
				}
			} catch(Exception ex){ ex.printStackTrace();}
		}
	}

	public void determinePreferences(Collection<String> normIDS, double viol) {
		/**
		 * Determines wether the agent will be an agent that ignores the norms (called isViolating) or not
		 */
		for(String nid : normIDS)
			if(this.r.nextDouble()<=viol)
				isViolating.put(nid, true);
			else
				isViolating.put(nid, false);
	}


}