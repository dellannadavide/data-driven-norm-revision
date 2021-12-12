package simulation;

import com.google.common.collect.Sets;

import java.util.*;


public class MaxSpeedNorm extends SimpleDNFNorm {
	/**
	 * Class representing a MaxSpeedNorm, i.e. a norm like
	 * (c, P(x), d) which prohibits agents to go to a speed higher than x from km c to km d
	 * The norm can also express a type of agent (APPL, e.g., truck or car) to which the norm applies
	 */

	public static final String COND_POS = "condpos";
    public static final String COND_APPL = "appl"; //+ the index
    public static final String PROH_SPEED = "speed";
    public static final String PROH_APPL = "appl"; //+ the index
    public static final String DEAD_POS = "deadpos";
    
    public static final int MIN_POS = 1;
    public static final int MAX_POS = 10;
    
    public static final int MIN_SPEED = 10;
    public static final int MAX_SPEED = 40; 
    
    public static final String[] POSSIBLE_APPL = {"both", "false", "car", "truck"};

    /**
     * Constructor used to create a random norm
     */
    public MaxSpeedNorm(String id, Random a) {
    	super(id, a);
    	LinkedHashMap<String, String> hm_cond = new LinkedHashMap<String, String>();
    	LinkedHashMap<String, String> hm_proh = new LinkedHashMap<String, String>();
    	LinkedHashMap<String, String> hm_dead = new LinkedHashMap<String, String>();
    	
    	//applicability
    	String ran_appl = this.POSSIBLE_APPL[Utils.uniform_discr(a, 0, this.POSSIBLE_APPL.length-1)];
    	if(!ran_appl.equals("both")) {
    		if(ran_appl.equals("false")) {
    			hm_cond.put(this.COND_APPL+"1", "car");    			
    			hm_cond.put(this.COND_APPL+"2", "truck");
    			hm_proh.put(this.PROH_APPL+"1", "car");    			
    			hm_proh.put(this.PROH_APPL+"2", "truck");
    		}
    		else {
    			hm_cond.put(this.COND_APPL+"1", ran_appl);  
    			hm_proh.put(this.PROH_APPL+"1", ran_appl);    	
    		}
    	}
    	
    	//condition
    	int cond_pos = Utils.uniform_discr(a, this.MIN_POS, this.MAX_POS-1);
    	hm_cond.put(this.COND_POS, "km"+cond_pos);
    	Disjunct ran_c = new Disjunct(hm_cond);

    	//prohibition (same applicability as condition)
    	hm_proh.put(this.PROH_SPEED, ""+ Utils.uniform_discr(a, this.MIN_SPEED, this.MAX_SPEED));
    	Disjunct ran_p = new Disjunct(hm_proh);

    	//deadline
    	hm_dead.put(this.DEAD_POS, "km"+ Utils.uniform_discr(a, cond_pos+1, this.MAX_POS));
    	Disjunct ran_d = new Disjunct(hm_dead);

    	List<Disjunct> c = new ArrayList<>();
    	c.add(ran_c);
    	this.cond = c;
    	List<Disjunct> p = new ArrayList<>();
    	p.add(ran_p);
    	this.proh = p;
    	List<Disjunct> d = new ArrayList<>();
    	d.add(ran_d);
    	this.dead = d;
    	
    }
    
    public MaxSpeedNorm(String id, Disjunct cond, Disjunct proh, Disjunct dead, Random r) {
    	super(id, cond, proh, dead, r);
    }

	public MaxSpeedNorm(String id, List<Disjunct> cond, List<Disjunct> proh, List<Disjunct> dead, Random r) {
		super(id, cond, proh, dead, r);
	}
    public MaxSpeedNorm(String id, String cond, String proh, String dead, Random r) {
    	super(id, r);
    	List<Disjunct> c = new ArrayList<>();
    	LinkedHashMap h_cond = new LinkedHashMap();
    	h_cond.put(this.COND_POS, cond);
    	c.add(new Disjunct(h_cond));
		this.cond = c;
		
		List<Disjunct> p = new ArrayList<>();
		LinkedHashMap h_proh = new LinkedHashMap();
		h_proh.put(this.PROH_SPEED, proh);
    	p.add(new Disjunct(h_proh));
		this.proh = p;
		
		List<Disjunct> d = new ArrayList<>();
		LinkedHashMap h_dead = new LinkedHashMap();
		h_dead.put(this.DEAD_POS, dead);
    	d.add(new Disjunct(h_dead));
		this.dead = d;
		
		consistencyCheck();
    }
    
    
    
    @Override
    public List getConditionRelatedProp(State s) {
		return Arrays.asList(s.position, s.type);
	}
    @Override
    public List getProhibitionRelatedProp(State s) {
		return Arrays.asList(Math.round(s.speed)+"", s.type);
	}
    @Override
    public List getDeadlineRelatedProp(State s) {
		return Arrays.asList(s.position);
	}


	public double getSpeed() {
    	return Double.parseDouble(this.getSimpleProhibition().getLiterals().get(this.PROH_SPEED));
    }

    @Override
    protected boolean isSat(String comp_type, State state) {
		/**
		 * Function to determine whether the norm component comp_type is satisfied in the state state
		 *     	//to check if a disjunct is sat in a state
		 *     	// I check if all literals of the disjunct hold in the state
		 *     	//which means that for each literal, it has to be either in position, speed, or type
		 *     	//Note that I use also domain knowledge/leverage the semantics of the propositions,
		 * 		// which means that if there is a literal
		 *     	//related to speed (e.g., sp50) this means speed 50 or higher
		 *     	//which means that all values of speed above satisfy the formula
		 *     	//similarly for position in the condition for instance km_i means km_i passed,
		 *     	//therefore in the highway if km_i passed then also km_j passed for j<i
		 */
    	switch(comp_type) {
	    	case "cond":
	    		for(int j=0;j<cond.size();j++) {
	    			Disjunct phi = cond.get(j);
	    			LinkedHashMap<String, String> literals = phi.getLiterals();
	    			Set<String> keys = literals.keySet();
	    	        for(String k:keys){
	    	            if(k.equals(this.COND_POS)) {
	    	            	if(Integer.parseInt(state.position.replace("km",""))<(Integer.parseInt(literals.get(k).replace("km",""))))
		    					return false;
	    	            }
	    	            else {
	    	            	if(!state.type.equals(literals.get(k)))
	    	            		return false;
	    	            }
	    	        }
				}
	    		break;
	    	case "proh":
	    		for(int j=0;j<proh.size();j++) {
	    			Disjunct phi = proh.get(j);
	    			LinkedHashMap<String, String> literals = phi.getLiterals();
	    			Set<String> keys = literals.keySet();
	    			for(String k:keys){
	    				if(k.equals(this.PROH_SPEED)) {
	    					if(state.speed<Double.parseDouble(literals.get(this.PROH_SPEED)))
		    					return false;
	    	            }
	    			}
				}
	    		break;
	    	case "dead":
	    		for(int j=0;j<dead.size();j++) {
	    			Disjunct phi = dead.get(j);
	    			LinkedHashMap<String, String> literals = phi.getLiterals();
	    			if(Integer.parseInt(state.position.replace("km",""))<(Integer.parseInt(literals.get(this.DEAD_POS).replace("km",""))))
	    				return false;
				}
	    		break;
    	}
    	return true;
    	
    }
    
    @Override
    protected boolean isDisabled() {
		/**
		 * Function to determine whether the norm is equivalent to a disabled norm
		 */
    	boolean condContainsCar = false;
		boolean condContainsTruck = false;
		boolean prohContainsCar = false;
		boolean prohContainsTruck = false;
		for(int i=0;i<cond.size();i++) {
			if(cond.get(i).toString().contains("car")) {
				condContainsCar=true;
			}
			if(cond.get(i).toString().contains("truck")) {
				condContainsTruck=true;
			}
		}
		for(int i=0;i<proh.size();i++) {
			if(proh.get(i).toString().contains("car")) {
				prohContainsCar=true;
			}
			if(proh.get(i).toString().contains("truck")) {
				prohContainsTruck=true;
			}
		}
		return (condContainsCar && condContainsTruck) || (prohContainsCar && prohContainsTruck);
    }

	@Override
	public String toString() {
		return super.toString();
	}
	
    public boolean appliesToType(String agentType) {
		/**
		 * Function to determine if the norm applies to the give agentType
		 */
    	LinkedHashMap<String, String> cond_map = this.getSimpleCondition().getLiterals();
    	LinkedHashMap<String, String> proh_map = this.getSimpleProhibition().getLiterals();
    	if(!cond_map.containsKey(this.COND_APPL+"1")) { //COND APPLIES TO BOTH CARS AND TRUCKS
    		if(!proh_map.containsKey(this.PROH_APPL+"1")) //SAME FOR PROHIBITION
    			return true;
    		else { //PROHIBITION DOES NOT APPLY TO BOTH
    			if(proh_map.get(this.PROH_APPL+"1").equals(agentType)) //PROH APPLIES TO THE AGENTTYPE
    				if(!proh_map.containsKey(this.PROH_APPL+"2"))
    	    			return true;
    				else return false; //PROH DOES NOT APPLY TO ANYTHING
    			else return false; //PROH DOES NOT APPLY TO THE AGENT TYPE
    		}
    	}
    	else { //cond applies to sth specific
    		if(cond_map.get(this.COND_APPL+"1").equals(agentType) && !cond_map.containsKey(this.COND_APPL+"2")) {//cond applies only to the agent type
    			if(!proh_map.containsKey(this.PROH_APPL+"1")) //proh applies to both types
        			return true;
        		else { //PROHIBITION DOES NOT APPLY TO BOTH
        			if(proh_map.get(this.PROH_APPL+"1").equals(agentType) && !proh_map.containsKey(this.PROH_APPL+"2")) //PROH APPLIES only TO THE AGENTTYPE
        	    			return true;
        			else return false; //PROH DOES NOT APPLY TO THE AGENT TYPE
        		}
    		}
    		else return false; //cond either applies to only another type or it is false
    	}
    }

	public Set<LinkedHashMap<String, String>> buildConjMoreSpecCond(Set<String> CS_prop) {
		/**
		 * Function to build conjunctions from propositions in CS_prop to later construct more specific conditions
		 */
		Set<LinkedHashMap<String, String>> builtConj = new HashSet<>();
		LinkedHashMap<String, String> cond_literals = this.cond.get(0).getLiterals();
		Set<String> bounded_CS_prop = new HashSet<String>();
		ArrayList<Integer> km = new ArrayList<Integer>();
		for(String s : CS_prop)
			if(s.startsWith("km")) {
				km.add(Integer.parseInt(s.replace("km","")));
			}
			else bounded_CS_prop.add(s);
		if(km.size()>this.SPACE_PARAM) {
			Collections.sort(km);
			for(int i=0;i<this.SPACE_PARAM;i++)
				bounded_CS_prop.add("km"+km.get(i));
		} else
			for(int i=0;i<km.size();i++)
				bounded_CS_prop.add("km"+km.get(i));
		if(bounded_CS_prop.size()>0)
			bounded_CS_prop.add(cond_literals.get(this.COND_POS));

		Set<Set<String>> power = Sets.powerSet(bounded_CS_prop);
		for (Set<String> gen_s : power) {
			LinkedHashMap<String, String> new_literals = new LinkedHashMap<String, String>(cond_literals); //i create the new one identical to the current one
			int i = new_literals.keySet().size(); //assuming there is always only one position, the remaining are the appl
			int max_km = -1;
			for(String str : gen_s)
				if(str.startsWith("km"))
					max_km = Math.max(max_km, Integer.parseInt(str.replace("km", "")));
				else {
					//add the appl
					if(!new_literals.values().contains(str)) {
						new_literals.put(this.COND_APPL+""+i, str);
						i++;
					}
				}
			if(max_km>-1) {
				new_literals.put(this.COND_POS, "km"+max_km); //replace the current one with the new pos
				builtConj.add(new_literals);
			}
		}
		return builtConj;
	}
//
//	/**
//	 *
//	 * @param violatingTraces
//	 * @return
//	 */
//    public Set<Disjunct> getMoreSpecificConditions(List<Trace> violatingTraces) {
//    	Set<Disjunct> moreSpecificConditions = new HashSet<>();
//
//    	LinkedHashMap<String, String> cond_literals = this.cond.get(0).getLiterals(); //the current condition. get(0) because simpleDNFNorm
//		Set<State> CS = new HashSet<State>();
//    	for(Trace t : violatingTraces) {
//    		CS.addAll(this.getDetachmentStates(t));
//    	}
//
//    	Set<String> CS_prop = getRelProp(CS, "cond");
//
//    	Set<String> bounded_CS_prop = new HashSet<String>();
//    	ArrayList<Integer> km = new ArrayList<Integer>();
//    	for(String s : CS_prop)
//    		if(s.startsWith("km")) {
//    			km.add(Integer.parseInt(s.replace("km","")));
//    		}
//    		else bounded_CS_prop.add(s);
//    	if(km.size()>this.SPACE_PARAM) {
//    		Collections.sort(km);
//    		for(int i=0;i<this.SPACE_PARAM;i++)
//    			bounded_CS_prop.add("km"+km.get(i));
//    	} else
//    		for(int i=0;i<km.size();i++)
//    			bounded_CS_prop.add("km"+km.get(i));
//    	if(bounded_CS_prop.size()>0)
//    		bounded_CS_prop.add(cond_literals.get(this.COND_POS));
//    	//now CS_prop contains the curr position in the cond + at most 4 more + the prop for the appl
//
//    	Set<Set<String>> power = Sets.powerSet(bounded_CS_prop); //this is PHI
//    	//System.out.println(power);
//
//    	moreSpecificConditions.add(new Disjunct(cond_literals)); //one of the new weaening is also the current one
//
//		for (Set<String> gen_s : power) {
//    		LinkedHashMap<String, String> new_literals = new LinkedHashMap<String, String>(cond_literals); //i create the new one identical to the current one
//    		int i = new_literals.keySet().size(); //assuming there is always only one position, the remaining are the appl
//    		int max_km = -1;
//    		for(String str : gen_s)
//    			if(str.startsWith("km"))
//    				max_km = Math.max(max_km, Integer.parseInt(str.replace("km", "")));
//    			else {
//    				//add the appl
//    				if(!new_literals.values().contains(str)) {
//	    				new_literals.put(this.COND_APPL+""+i, str);
//		        		i++;
//    				}
//    			}
//    		if(max_km>-1) {
//    			//I add also the pos
//    			new_literals.put(this.COND_POS, "km"+max_km); //replace the current one with the new pos
//    			//add the new condition
//    			moreSpecificConditions.add(new Disjunct(new_literals));
//    		}
//    		// else I ignore this set, as I always want at least the position literal
//		}
//    	//}
//    	return moreSpecificConditions;
//    }
//

	public Set<LinkedHashMap<String, String>> buildConjMoreSpecProh(Set<String> PS_prop) {
		/**
		 * Function to build conjunctions from propositions in PS_prop to later construct more specific prohibitions
		 */
		Set<LinkedHashMap<String, String>> builtConj = new HashSet<>();
		LinkedHashMap<String, String> curr_proh_literals = this.proh.get(0).getLiterals(); //the current proh
		Set<String> bounded_PS_prop = new HashSet<String>();
		ArrayList<Integer> sp = new ArrayList<Integer>();
		for(String s : PS_prop)
			try {
				int possible_speed = Integer.parseInt(s);
				//if here it is actually the speed and not the type
				sp.add(possible_speed);
			} catch (NumberFormatException nfe) {
				//here it means it was a type
				bounded_PS_prop.add(s);
			}
		if(sp.size()>this.SPACE_PARAM) {
			Collections.sort(sp);
			for(int i=0;i<this.SPACE_PARAM;i++)
				bounded_PS_prop.add(sp.get(i)+"");
		} else
			for(int i=0;i<sp.size();i++)
				bounded_PS_prop.add(sp.get(i)+"");
		if(bounded_PS_prop.size()>0)
			bounded_PS_prop.add(curr_proh_literals.get(this.PROH_SPEED));

		Set<Set<String>> power = Sets.powerSet(bounded_PS_prop); //this is PHI
		//System.out.println(power);
		//for(String sp : getKMoreSpecificSpeed(curr_proh_literals.get(this.PROH_SPEED), this.MAX_SPEED+"", 5)) { //it's a simplification for now
		//i do correction as per condition
		for (Set<String> gen_s : power) {
			LinkedHashMap<String, String> new_literals = new LinkedHashMap<String, String>(curr_proh_literals); //i create the new one identical to the current one
			int i = new_literals.keySet().size(); //assuming there is always only one speed, the remaining are the appl
			int max_sp = -1;
			for(String str : gen_s) {
				try {
					int possible_speed = Integer.parseInt(str);
					//if here it is actually the speed and not the type
					max_sp = Math.max(max_sp, possible_speed);
				} catch (NumberFormatException nfe) {
					//here it means it was a type
					//add the appl
					if(!new_literals.values().contains(str)) {
						new_literals.put(this.PROH_APPL+""+i, str);
						i++;
					}
				}
			}
			if(max_sp>-1) {
				//I add also the speed
				new_literals.put(this.PROH_SPEED, max_sp+""); //replace the current one with the new speed
				//add the new proh
				builtConj.add(new_literals);
			}
			// else I ignore this set, as I always want at least the position literal
		}
		//}
		return builtConj;
	}
//
//    public Set<Disjunct> getMoreSpecificProhibitions(List<Trace> violatingTraces) {
//    	Set<Disjunct> moreSpecificProhibitions = new HashSet<>();
//
//    	LinkedHashMap<String, String> curr_proh_literals = this.proh.get(0).getLiterals(); //the current proh
//
//    	//1. Take PS, set of states s from TN traces s.t. φ_P holds.
//    	Set<State> PS = new HashSet<State>();
//    	for(Trace t : violatingTraces) {
//    		PS.addAll(this.getViolatingStates(t)); //see comments inside
//    	}
//    	//System.out.println(PS);
//
//		Set<String> PS_prop = getRelProp(PS, "proh");
//
//    	/*HERE I ADD THE POLINOMIAL BOUNDING
//    	 * I pick in PS_prop, at most 5 more specific speeds, instead of all possible ones
//    	 * -
//    	 * I go through PS_prop, I select all the ones different from the possible types, I sort them, i take the first 4+1(the curr one)
//    	 * */
//    	Set<String> bounded_PS_prop = new HashSet<String>();
//    	ArrayList<Integer> sp = new ArrayList<Integer>();
//    	for(String s : PS_prop)
//    		try {
//    	        int possible_speed = Integer.parseInt(s);
//    	        //if here it is actually the speed and not the type
//    	        sp.add(possible_speed);
//    	    } catch (NumberFormatException nfe) {
//    	        //here it means it was a type
//    	    	bounded_PS_prop.add(s);
//    	    }
//    	if(sp.size()>this.SPACE_PARAM) {
//    		Collections.sort(sp);
//    		for(int i=0;i<this.SPACE_PARAM;i++)
//    			bounded_PS_prop.add(sp.get(i)+"");
//    	} else
//    		for(int i=0;i<sp.size();i++)
//    			bounded_PS_prop.add(sp.get(i)+"");
//    	if(bounded_PS_prop.size()>0)
//    		bounded_PS_prop.add(curr_proh_literals.get(this.PROH_SPEED));
//
//    	Set<Set<String>> power = Sets.powerSet(bounded_PS_prop); //this is PHI
//    	//System.out.println(power);
//
//    	// 3. Possible weakened prohibitions: {φ_P ∧ φ |φ in Φ}
//    	moreSpecificProhibitions.add(new Disjunct(curr_proh_literals)); //one of the new weaening is also the current one
//
//    	//for(String sp : getKMoreSpecificSpeed(curr_proh_literals.get(this.PROH_SPEED), this.MAX_SPEED+"", 5)) { //it's a simplification for now
//		//i do correction as per condition
//    	for (Set<String> gen_s : power) {
//    		LinkedHashMap<String, String> new_literals = new LinkedHashMap<String, String>(curr_proh_literals); //i create the new one identical to the current one
//    		int i = new_literals.keySet().size(); //assuming there is always only one speed, the remaining are the appl
//    		int max_sp = -1;
//    		for(String str : gen_s) {
//    			try {
//        	        int possible_speed = Integer.parseInt(str);
//        	        //if here it is actually the speed and not the type
//        	        max_sp = Math.max(max_sp, possible_speed);
//        	    } catch (NumberFormatException nfe) {
//        	        //here it means it was a type
//        	    	//add the appl
//    				if(!new_literals.values().contains(str)) {
//	    				new_literals.put(this.PROH_APPL+""+i, str);
//		        		i++;
//    				}
//        	    }
//    		}
//    		if(max_sp>-1) {
//    			//I add also the speed
//    			new_literals.put(this.PROH_SPEED, max_sp+""); //replace the current one with the new speed
//    			//add the new proh
//    			moreSpecificProhibitions.add(new Disjunct(new_literals));
//    		}
//    		// else I ignore this set, as I always want at least the position literal
//		}
//    	//}
//    	return moreSpecificProhibitions;
//
//    }
//

	public Set<LinkedHashMap<String, String>> buildConjLessSpecDead(Set<String> CPS_prop) {
		/**
		 * Function to build conjunctions from propositions in CPS_prop to later construct less specific deadlines
		 */
		Set<LinkedHashMap<String, String>> builtConj = new HashSet<>();
		for (String s : CPS_prop) {
			//System.out.println(s);
			LinkedHashMap<String, String> new_literals = new LinkedHashMap<String, String>();
			new_literals.put(this.DEAD_POS, s);
			builtConj.add(new_literals);
		}
		return builtConj;
	}
//
//    public Set<Disjunct> getLessSpecificDeadlines(List<Trace> violatingTraces) {
//    	Set<Disjunct> lessSpecificDeadlines = new HashSet<>();
//
//    	//1. Take CPS, set of states s from FN traces between states where φ_C and  φ_P hold.
//    	Set<State> CPS = new HashSet<State>();
//    	for(Trace t : violatingTraces) {
//    		//System.out.println(t);
//    		CPS.addAll(this.getStatesBetweenCondandProh(t)); //see comments inside
//    	}
//    	//System.out.println(CPS);
//
//    	//2. Let PHI = set of possible conjunctions from prop(CPS)
//    	Set<String> CPS_prop = getRelProp(CPS, "dead");
//    	//since in the deadline there is only position this becomes directly PHI
//    	/*System.out.println("Dead weakening. prop(CPS) = ");
//    	System.out.println(CPS_prop);*/
//
//    	lessSpecificDeadlines.add(this.getSimpleDeadline());//the current deadline is a possible less specific deadline
//
//    	for (String s : CPS_prop) {
//    	 	//System.out.println(s);
//    		LinkedHashMap<String, String> new_literals = new LinkedHashMap<String, String>();
//    		new_literals.put(this.DEAD_POS, s);
//    	 	lessSpecificDeadlines.add(new Disjunct(new_literals));
//    	}
//    	return lessSpecificDeadlines;
//    }
//
//    /*for strengthening the norm - these are norm-specific*/

	public Set<LinkedHashMap<String, String>> buildConjLessSpecCond(Set<String> OPS_prop) {
		/**
		 * Function to build conjunctions from propositions in OPS_prop to later construct less specific conditions
		 */
		Set<LinkedHashMap<String, String>> builtConj = new HashSet<>();
		LinkedHashMap<String, String> cond_literals = this.cond.get(0).getLiterals(); //the current condition
		Set<String> bounded_OPS_prop = new HashSet<String>();
		ArrayList<Integer> km = new ArrayList<Integer>();
		for(String s : OPS_prop)
			if(s.startsWith("km")) {
				km.add(Integer.parseInt(s.replace("km","")));
			}
			else bounded_OPS_prop.add(s);

		Collections.sort(km);
		Collections.reverse(km);
		int c = 0;
		for(int i=0;i<km.size() && c<this.SPACE_PARAM;i++) {
			if(km.get(i)<Integer.parseInt(cond_literals.get(this.COND_POS).replace("km",""))) {
				bounded_OPS_prop.add("km"+km.get(i));
				c++;
			}
		}
		if(bounded_OPS_prop.size()>0)
			bounded_OPS_prop.add(cond_literals.get(this.COND_POS));

		Set<Set<String>> power = Sets.powerSet(bounded_OPS_prop); //this is PHI

		for (Set<String> gen_s : power) {
			LinkedHashMap<String, String> new_literals = new LinkedHashMap<String, String>();
			int i = 1;
			int max_km = -1;
			for(String str : gen_s)
				if(str.startsWith("km"))
					max_km = Math.max(max_km, Integer.parseInt(str.replace("km", "")));
				else {
					//add the appl
					if(!new_literals.values().contains(str)) {
						new_literals.put(this.COND_APPL+""+i, str);
						i++;
					}
				}
			if(max_km>-1) {
				LinkedHashMap<String, String> curr_literals = new LinkedHashMap<String, String>(cond_literals);
				curr_literals.remove(this.COND_POS);
				if(new_literals.values().size() == 0 ||
						(curr_literals.values().containsAll(new_literals.values()))) {
					new_literals.put(this.COND_POS, "km"+max_km); //replace the current one with the new pos
					builtConj.add(new_literals);
				}
			}
		}
		return builtConj;
	}
//
//    public Set<Disjunct> getLessSpecificConditions(List<Trace> obeyingTraces){
//    	//to modify only the condition is possible only if there is some state in the obeying traces
//    	//that violates the prohibition (even though the norm is obeyed)
//
//    	//i'm assuming that thre is always a deadline in the traces
//
//    	Set<Disjunct> lessSpecificConditions = new HashSet<>();
//
//
//    	LinkedHashMap<String, String> cond_literals = this.cond.get(0).getLiterals(); //the current condition
//
//    	//1. Find states OPS in the FP traces where φ_P holds (if any, they are surely outside a possible window [c, d]).
//    	//for simplicity I take just BEFORE the deadline, as in the highway domain is pointless after.
//    	//notice however that in case the norm never detaches (e.g., because cond is km2 and car, and the trace is generated by a truck)
//    	//the km in the obtained states may be AFTER the km in the condition
//    	Set<State> OPS = new HashSet<State>();
//    	for(Trace t : obeyingTraces) {
////    		OPS.addAll(this.getStatesBeforeDeadWhereProhHold(t));
//    		OPS.addAll(this.getStatesOutsideWindowWhereProhHold(t));
//    	}
//    	//System.out.println(OPS);
//
//    	//2. Let PHI = set of possible conjunctions from prop(OPS)
//    	Set<String> OPS_prop = getRelProp(OPS, "cond");
//    	/*System.out.println("Cond strengthening. Prop(OPS) = ");
//    	System.out.println(OPS_prop);*/
//
//    	/*HERE I ADD THE POLINOMIAL BOUNDING
//    	 * i pick in OPS_prop, at most 5 less specific positions, instead of all possible ones
//    	 * -
//    	 * I go through OPS_prop, I select all the ones starting with km, I sort them in DESCENDING ORDER, i take the first 4 smaller than the current one, +1(the curr one)
//    	 * */
//    	Set<String> bounded_OPS_prop = new HashSet<String>();
//    	ArrayList<Integer> km = new ArrayList<Integer>();
//    	for(String s : OPS_prop)
//    		if(s.startsWith("km")) {
//    			km.add(Integer.parseInt(s.replace("km","")));
//    		}
//    		else bounded_OPS_prop.add(s);
//
//		Collections.sort(km);
//		Collections.reverse(km);
//		int c = 0;
//		for(int i=0;i<km.size() && c<this.SPACE_PARAM;i++) {
//			if(km.get(i)<Integer.parseInt(cond_literals.get(this.COND_POS).replace("km",""))) {
//				bounded_OPS_prop.add("km"+km.get(i));
//				c++;
//			}
//		}
//		if(bounded_OPS_prop.size()>0)
//			bounded_OPS_prop.add(cond_literals.get(this.COND_POS));
//    	//now bounded_OPS_prop contains the curr position in the cond + at most 4 more + the prop for the appl
//
//    	Set<Set<String>> power = Sets.powerSet(bounded_OPS_prop); //this is PHI
//    	//System.out.println(power);
//
//    	//3. Possible strengthened conditions: {φ_C ∨ φ |φ in Φ}
//
//    	lessSpecificConditions.add(new Disjunct(cond_literals)); //one of the new strengthening is also the current one
//
//    	//NOTE THAT NOW IN POWER THERE MAY BE SOME "INVALID" SETs, so i want to correct
//    	//FOR EXAMPLE the TWO sets {km1, km3} and {km2, km3}, are both equivalent to {km3} in our domain.
//    	//so i just want to have km3,
//    	//also, since I want to STRENGTHEN the norm (i.e., MAKE IT TRUE IN MORE STATES),
//    	//i need to be sure not to create an alteration instead, for example by replacing (km2, car) with (km1, truck), which would be possible given the above
//    	for (Set<String> gen_s : power) {
//    		LinkedHashMap<String, String> new_literals = new LinkedHashMap<String, String>();
//    		int i = 1;
//    		int max_km = -1;
//    		for(String str : gen_s)
//    			if(str.startsWith("km"))
//    				max_km = Math.max(max_km, Integer.parseInt(str.replace("km", "")));
//    			else {
//    				//add the appl
//    				if(!new_literals.values().contains(str)) {
//	    				new_literals.put(this.COND_APPL+""+i, str);
//		        		i++;
//    				}
//    			}
//    		if(max_km>-1) {
//    			LinkedHashMap<String, String> curr_literals = new LinkedHashMap<String, String>(cond_literals);
//    			curr_literals.remove(this.COND_POS);
//    			//now in curr_literals tehre ionly the appl prop
//    			//ok cases are the ones where there is no appl prop (so it holds for all types),
//    			//or the prop in the new cond are a subset (so it holds for more types) than the current prop
//    			if(new_literals.values().size() == 0 ||
//    					(curr_literals.values().containsAll(new_literals.values()))) {
//
//    				//I add also the pos
//        			new_literals.put(this.COND_POS, "km"+max_km); //replace the current one with the new pos
//        			//add the new condition
//
//        			lessSpecificConditions.add(new Disjunct(new_literals));
//    			}
//    			// else I ignore this set, as it's not a strengthening
//    		}
//    		// else I ignore this set, as I always want at least the position literal
//		}
//
//    	return lessSpecificConditions;
//
//    }

	public Set<LinkedHashMap<String, String>> buildConjLessSpecProh(Set<String> IPS_prop) {
		/**
		 * Function to build conjunctions from propositions in IPS_prop to later construct less specific prohibitions
		 */
		Set<LinkedHashMap<String, String>> builtConj = new HashSet<>();
		LinkedHashMap<String, String> proh_literals = this.getSimpleProhibition().getLiterals(); //the current prohibition
		Set<String> bounded_IPS_prop = new HashSet<String>();
		ArrayList<Integer> speeds = new ArrayList<Integer>();
		for(String str : IPS_prop) {
			try {
				int possible_speed = Integer.parseInt(str);
				speeds.add(possible_speed);
			} catch (NumberFormatException nfe) {
				bounded_IPS_prop.add(str);
			}
		}
		Collections.sort(speeds);
		Collections.reverse(speeds);
		int c = 0;
		for(int i=0;i<speeds.size() && c<this.SPACE_PARAM;i++) {
			int sp = speeds.get(i);
			if(sp>=this.MIN_SPEED && sp<Integer.parseInt(proh_literals.get(this.PROH_SPEED))) {
				bounded_IPS_prop.add(sp+"");
				c++;
			}
		}
		if(bounded_IPS_prop.size()>0)
			bounded_IPS_prop.add(proh_literals.get(this.PROH_SPEED));

		Set<Set<String>> power = Sets.powerSet(bounded_IPS_prop);

		for (Set<String> gen_s : power) {
			LinkedHashMap<String, String> new_literals = new LinkedHashMap<String, String>();
			int i = 1;
			int max_sp = -1;
			for(String str : gen_s) {
				try {
					int possible_speed = Integer.parseInt(str);
					max_sp = Math.max(max_sp, possible_speed);
				} catch (NumberFormatException nfe) {
					if(!new_literals.values().contains(str)) {
						new_literals.put(this.PROH_APPL+""+i, str);
						i++;
					}
				}
			}
			if(max_sp>-1) {
				LinkedHashMap<String, String> curr_literals = new LinkedHashMap<String, String>(proh_literals);
				curr_literals.remove(this.PROH_SPEED);
				if(new_literals.values().size() == 0 ||
						(curr_literals.values().containsAll(new_literals.values()))) {
					new_literals.put(this.PROH_SPEED, max_sp+""); //replace the current one with the new pos
					builtConj.add(new_literals);
				}
			}
		}

		return builtConj;
	}
//
//    public Set<Disjunct> getLessSpecificProhibitions(List<Trace> obeyingTraces){
//    	Set<Disjunct> lessSpecificProhibitions = new HashSet<>();
//
//    	LinkedHashMap<String, String> proh_literals = this.getSimpleProhibition().getLiterals(); //the current prohibition
//
//    	//1. Find states IPS in the FP traces between states where φ_C and φ_D hold (or just before the deadline, if norm never detached)
//    	Set<State> IPS = new HashSet<State>();
//    	for(Trace t : obeyingTraces) {
//    		IPS.addAll(this.getStatesBetweenCondandDead(t)); //assuming that in our system the deadline is always reached this is the only case we care about
////    		if(IPS.isEmpty()) //if it is empty, assuming the deadline is always reached, is because the norm is not detached, in this case I consider ALL the states
////    			IPS.addAll(this.getStatesBeforeDead(t));
//    	}
//    	//System.out.println(IPS);
//
//    	//2. Let PHI = set of possible conjunctions from prop(IPS)
//    	Set<String> IPS_prop = getRelProp(IPS, "proh");
//    	/*System.out.println("Proh strengthening. prop(IPS) = ");
//    	System.out.println(IPS_prop);*/
//
//    	/*HERE I ADD THE POLINOMIAL BOUNDING
//    	 * i pick in IPS_prop, at most 5 less specific speeds, instead of all possible ones
//    	 * -
//    	 * I go through iPS_prop, I select all the ones that are numbers, I sort them in DESCENDING ORDER, i take the first 4 smaller than the current one, +1(the curr one)
//    	 * */
//    	Set<String> bounded_IPS_prop = new HashSet<String>();
//    	ArrayList<Integer> speeds = new ArrayList<Integer>();
//    	for(String str : IPS_prop) {
//    		try {
//    	        int possible_speed = Integer.parseInt(str);
//    	        //if here it is actually the speed and not the type
//    	        speeds.add(possible_speed);
//    	    } catch (NumberFormatException nfe) {
//    	    	bounded_IPS_prop.add(str);
//    	    }
//    	}
//		Collections.sort(speeds);
//		Collections.reverse(speeds);
//		int c = 0;
//		for(int i=0;i<speeds.size() && c<this.SPACE_PARAM;i++) {
//			int sp = speeds.get(i);
//			if(sp>=this.MIN_SPEED && sp<Integer.parseInt(proh_literals.get(this.PROH_SPEED))) {
//				bounded_IPS_prop.add(sp+"");
//				c++;
//			}
//		}
//		if(bounded_IPS_prop.size()>0)
//			bounded_IPS_prop.add(proh_literals.get(this.PROH_SPEED));
//    	//now bounded_iPS_prop contains the curr speed in the proh + at most 4 more + the prop for the appl
//
//		Set<Set<String>> power = Sets.powerSet(bounded_IPS_prop); //this is PHI
//    	//System.out.println(power);
//
//    	//3. Possible strengthened prohibitions: {φ_P ∨ φ |φ in Φ}
//
//    	lessSpecificProhibitions.add(new Disjunct(proh_literals)); //one of the new strengthening is also the current one
//
//    	//NOTE THAT NOW IN POWER THERE MAY BE SOME "INVALID" SETs, so i want to correct
//    	//FOR EXAMPLE the TWO sets {SP1, SP3} and {SP2, SP3}, are both equivalent to {SP3} in our domain.
//    	//so i just want to have sp3,
//    	//also, since I want to STRENGTHEN the norm (i.e., MAKE IT TRUE IN MORE STATES),
//    	//i need to be sure not to create an alteration instead, for example by replacing (sp2, car) with (sp1, truck), which would be possible given the above
//    	for (Set<String> gen_s : power) {
//    		LinkedHashMap<String, String> new_literals = new LinkedHashMap<String, String>();
//    		int i = 1;
//    		int max_sp = -1;
//    		for(String str : gen_s) {
//    			try {
//        	        int possible_speed = Integer.parseInt(str);
//        	        //if here it is actually the speed and not the type
//        	        max_sp = Math.max(max_sp, possible_speed);
//        	    } catch (NumberFormatException nfe) {
//        	    	//add the appl
//    				if(!new_literals.values().contains(str)) {
//	    				new_literals.put(this.PROH_APPL+""+i, str);
//		        		i++;
//    				}
//        	    }
//    		}
//    		if(max_sp>-1) {
//    			LinkedHashMap<String, String> curr_literals = new LinkedHashMap<String, String>(proh_literals);
//    			curr_literals.remove(this.PROH_SPEED);
//    			//now in curr_literals tehre ionly the appl prop
//    			//ok cases are the ones where there is no appl prop (so it holds for all types),
//    			//or the prop in the new cond are a subset (so it holds for more types) than the current prop
//    			if(new_literals.values().size() == 0 ||
//    					(curr_literals.values().containsAll(new_literals.values()))) {
//
//    				//I add also the pos
//        			new_literals.put(this.PROH_SPEED, max_sp+""); //replace the current one with the new pos
//        			//add the new condition
//
//        			lessSpecificProhibitions.add(new Disjunct(new_literals));
//    			}
//    			// else I ignore this set, as it's not a strengthening
//    		}
//    		// else I ignore this set, as I always want at least the position literal
//		}
//
//    	return lessSpecificProhibitions;
//
//    }


	public Set<LinkedHashMap<String, String>> buildConjMoreSpecDead(Set<String> DS_prop) {
		/**
		 * Function to build conjunctions from propositions in DS_prop to later construct more specific deadlines
		 */
		Set<LinkedHashMap<String, String>> builtConj = new HashSet<>();

		Set<String> bounded_DS_prop = new HashSet<String>();
		if(DS_prop.size()>this.SPACE_PARAM) {
			ArrayList<String> km_str = new ArrayList<String>(DS_prop);
			Collections.sort(km_str);
			for(int i=0;i<this.SPACE_PARAM;i++)
				bounded_DS_prop.add(km_str.get(i));
		} else bounded_DS_prop.addAll(DS_prop);
		for (String s : bounded_DS_prop) {
			LinkedHashMap<String, String> new_literals = new LinkedHashMap<String, String>();
			new_literals.put(this.DEAD_POS, s);
			builtConj.add(new_literals);
		}
		return builtConj;
	}
//
//    public Set<Disjunct> getMoreSpecificDeadlines(List<Trace> obeyingTraces){
//    	Set<Disjunct> moreSpecificDeadlines = new HashSet<>();
//
//
//
//    	//1. Find states DS in the TP traces where φ_D hold
//    	Set<State> DS = new HashSet<State>();
//    	for(Trace t : obeyingTraces) {
//    		DS.addAll(this.getStatesWhereDeadlineHolds(t));
//    	}
//    	//System.out.println(DS);
//    	//2. Let PHI = set of possible conjunctions of prop from prop(DS)\ prop(φ_D)
//    	Set<String> DS_prop = getRelProp(DS, "dead");
//    	/*System.out.println("Dead strengthening. prop(DS) = ");
//    	System.out.println(DS_prop);*/
//
//    	/*HERE I ADD THE POLINOMIAL BOUNDING
//    	 * I pick in DS_prop, at most 5 more specific positions, instead of all possible oneS
//    	 * -
//    	 * I go through DS_prop, I select all the ones starting with km, I sort them, i take the first 4+1(the curr one)
//    	 * */
//    	Set<String> bounded_DS_prop = new HashSet<String>();
//    	if(DS_prop.size()>this.SPACE_PARAM) {
//    		//since i know that the language for deadline is only the pos then I just create a list
//    		ArrayList<String> km_str = new ArrayList<String>(DS_prop);
//    		Collections.sort(km_str);
//    		for(int i=0;i<this.SPACE_PARAM;i++)
//    			bounded_DS_prop.add(km_str.get(i));
//    	} else bounded_DS_prop.addAll(DS_prop);
//
//    	//since we have only positions, bounded_ds_prop is directly PHI
//    	//and i do not need to create all possible conjunctions (because e.g., km1 and km4 = km4, so it is sufficient to just use km4)
//    	//System.out.println(bounded_DS_prop);
//
//    	//3. Possible strengthened deadlines: {φ_D ∧ φ |φ in Φ}
//    	moreSpecificDeadlines.add(this.getSimpleDeadline()); //the current deadline is also one of the more specific ones
//
//    	for (String s : bounded_DS_prop) {
//    	 	LinkedHashMap<String, String> new_literals = new LinkedHashMap<String, String>();
//    		new_literals.put(this.DEAD_POS, s);
//    		moreSpecificDeadlines.add(new Disjunct(new_literals));
//    	}
//
//
//    	return moreSpecificDeadlines;
//    }

	@Override
	protected void consistencyCheck() {
		/**
		 * Function to verify the norm is consistent (domain based)
		 * 		 * in particular I focus on the relationship between THE POSITIONS in condition and deadline
		 * 		 * i.e., deadline's position cannot be before condition's position in this domain
		 * 		 * Furthermore if the condition holds for something also the prohibition has to hold for the same thing
		 * 		 * e.g., if condition is km1 and car, then the prohibition cannot relate to trucks
		 */
		boolean isConsistent = true;
		LinkedHashMap<String, String> cond_literals = getSimpleCondition().getLiterals();
		LinkedHashMap<String, String> proh_literals = getSimpleProhibition().getLiterals();
		LinkedHashMap<String, String> dead_literals = getSimpleDeadline().getLiterals();
		/*DEADLINE POSITION SHOULD NOT BE BEFORE CONDITION POSITION*/
		String cond_pos = cond_literals.get(this.COND_POS);
		String dead_pos = dead_literals.get(this.DEAD_POS);
		if(Integer.parseInt(cond_pos.replace("km",""))>=Integer.parseInt(dead_pos.replace("km","")))
			isConsistent = false;
		else {
			LinkedHashMap<String, String> hm_cond_temp = new LinkedHashMap<String, String>(cond_literals);
			hm_cond_temp.remove(this.COND_POS);
			LinkedHashMap<String, String> hm_proh_temp = new LinkedHashMap<String, String>(proh_literals);
			hm_proh_temp.remove(this.PROH_SPEED);
			Set cond_set = new HashSet<String>(hm_cond_temp.values());
			Set proh_set = new HashSet<String>(hm_proh_temp.values());
			if(!cond_set.equals(proh_set))
				isConsistent = false;
		}
		if(!isConsistent) {
			this.cond = null;
			this.proh = null;
			this.dead = null;
		}
	}

}
