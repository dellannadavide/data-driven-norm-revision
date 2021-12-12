package simulation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * simulation.Trace
 */	
public class Trace implements Cloneable {
	/**
	 * A trace representing the vehicle behavior throuhgout the highway
	 */
	List<State> states = new ArrayList<>();
	LinkedHashMap<String,String> norms_eval = new LinkedHashMap<String,String>();
	double co2_eval = 0.0;
	double traveltime_eval = 0.0;
	boolean objEval;

	public List<State> getStates() {
		return states;
	}
	public void setStates(List<State> states) {
		this.states = states;
	}

	public void setNorms_eval(LinkedHashMap<String, String> norms_eval) {
		this.norms_eval = norms_eval;
	}

	public double getCo2_eval() {
		return co2_eval;
	}

	public double getTraveltime_eval() {
		return traveltime_eval;
	}

	public void setCo2_eval(double co2_eval) {
		this.co2_eval = co2_eval;
	}

	public void setTraveltime_eval(double traveltime_eval) {
		this.traveltime_eval = traveltime_eval;
	}

	public void setObjEval(boolean objEval) {
		this.objEval = objEval;
	}

	public boolean getObjEval() {
		return this.objEval;
	}

	/**
     * Default constructor
     */
    public Trace() {
    }

    /**
     * Constructor initializing the fields
     */
    public Trace(List<State> states) {
		this.states.addAll(states);
    }
    
    public Trace(State state) {
		this.states.add(state);
    }
    
    public void add(State state) {
    	this.states.add(state);
    }
    

    
    public void addNormEval(String norm_id, boolean isviol) {
    	this.norms_eval.put(norm_id, isviol?"viol":"ob");
    }
    public void addNormEval(String norm_id, String isviol) {
    	this.norms_eval.put(norm_id, isviol);
    }

    public boolean normObeyed(String norm_id) {
    	return this.norms_eval.get(norm_id).equals("ob");
    }
    
    public int getLength() {
    	return states.size();
    }
    
    public void updateLastStateSpeed(double speed) {
    	State lastState = states.get(states.size()-1);
    	lastState.updateSpeed(speed);
    }
    public void updateLastStateDist(double dist) {
    	State lastState = states.get(states.size()-1);
    	lastState.updateDist(dist);
    }
    public void updateLastStateCO2Emission(double co2) {
    	State lastState = states.get(states.size()-1);
    	lastState.updateCO2Emission(co2);
    }
    public void updateLastStateTime(double tt) {
    	State lastState = states.get(states.size()-1);
    	lastState.updateTime(tt);
    }
    
    public String stringifyStates() {
    	String str = "";
    	for(int i=0; i<states.size();  i++) {
    		str = str+states.get(i).toString()+(i==states.size()-1?"":";");
    	}
    	return str;
    }

	@Override
	public String toString() {
		return  states + ": " + norms_eval.values() + ", " + co2_eval + ", " + traveltime_eval;
	}

	public Object clone() {
		// Assign the shallow copy to
		// new reference variable t
		Trace t = null;
		try {
			t = (Trace)super.clone();
			// Creating a deep copy
			t.setStates(new ArrayList<>(states));
			t.setNorms_eval(new LinkedHashMap<>(norms_eval));
			t.setCo2_eval(co2_eval);
			t.setTraveltime_eval(traveltime_eval);
			t.setObjEval(objEval);
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return t;
	}

}