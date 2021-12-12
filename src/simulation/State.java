package simulation;

public class State {
	/**
	 * A class representing a state contained in a trace
	 */

	 String position;
	 double speed;
	 double dist;
	 String type;
	 double co2emission;
	 double time;
	 
	 int NR_ATTR = 6;

    /**
     * Default constructor
     */
    public State() {
    }

	public String getPosition() {
		return position;
	}

	public double getSpeed() {
		return speed;
	}

	public double getDist() {
		return dist;
	}

	public String getType() {
		return type;
	}

	public double getCo2emission() {
		return co2emission;
	}

	public double getTime() {
		return time;
	}

	/**
     * Constructor initializing the fields
     */
    public State(String position, double speed, double dist, String type, double co2emi, double time) {
		this.position = position;
		this.speed = speed;
		if(dist<0)
			this.dist = 1000000;
		else
			this.dist = dist;
		this.type = type;
		this.co2emission =  co2emi;
		this.time = time;
    }
    
    /*
     * Constructor initializing the fields parsing an input string
     * 
     */
    public State(String toParseState) throws Exception {
    	String s = toParseState.replace("(","").replace(")", "");
    	String[] attr = s.split(",");
    	if(attr.length!=this.NR_ATTR)
    		throw new Exception("Invalid input state");
    	else {
    		this.position = attr[0];
    		this.speed = Double.parseDouble(attr[1]);
    		this.dist = Double.parseDouble(attr[2]);
    		this.type = attr[3];
    		this.co2emission =  Double.parseDouble(attr[4]);
    		this.time = Double.parseDouble(attr[5]);
    	}
    			
    }
    
    /*I consider the maximum speed, co2emission and time for each highway section*/
    public void updateSpeed(double speed) {
    	if(this.speed<speed) {
    		this.speed = speed;
    	}
    }
    public void updateDist(double dist) {
    	if(this.dist>dist && dist>=0) {
    		this.dist = dist;
    	}
    }
    public void updateCO2Emission(double co2) {
    	if(this.co2emission<co2) {
    		this.co2emission = co2;
    	}
    }
    public void updateTime(double time) {
    	if(this.time<time) {
    		this.time = time;
    	}
    }
    
    /*public List getConditionRelatedProp() {
    	return Arrays.asList(position, type);
    }
    public List getProhibitionRelatedProp() {
    	//return Arrays.asList(Math.round(speed)+"", type);
    	return Arrays.asList(type);
    }
    public List getDeadlineRelatedProp() {
    	return Arrays.asList(position);
    }*/
    

	@Override
	public String toString() {
		return  "("+position +"," + speed +"," + dist +  "," + type+  "," + co2emission+  "," + time+  ")";
	}

}