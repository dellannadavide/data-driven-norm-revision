package simulation;

import java.util.*;
import com.google.common.collect.Sets;


public class MaxSpeedNorm extends HighwayNorm {
	/**
	 * Class representing a MaxSpeedNorm, i.e. a norm like
	 * (c, P(x), d) which prohibits agents to go to a speed higher than x from km c to km d
	 * The norm can also express a type of agent (APPL, e.g., truck or car) to which the norm applies
	 */

    /**
     * Constructor used to create a random norm
     */
    public MaxSpeedNorm(String id, Random a) {
    	super(id, a);
    	Conjunction hm_cond = new Conjunction();
		Conjunction hm_proh = new Conjunction();
		Conjunction hm_dead = new Conjunction();
    	
    	//applicability
    	String ran_appl = POSSIBLE_APPL[Utils.uniform_discr(a, 0, POSSIBLE_APPL.length-1)];
    	if(!ran_appl.equals(Constants.BOTH)) {
    		if(ran_appl.equals(Constants.FALSE)) {
				hm_cond.addLiteral(COND_APPL, Constants.CAR);
				hm_cond.addLiteral(COND_APPL, Constants.TRUCK);
    			hm_proh.addLiteral(PROH_APPL, Constants.CAR);
    			hm_proh.addLiteral(PROH_APPL, Constants.TRUCK);
    		}
    		else {
    			hm_cond.addLiteral(COND_APPL, ran_appl);
    			hm_proh.addLiteral(PROH_APPL, ran_appl);
    		}
    	}
    	
    	//condition
    	int cond_pos = Utils.uniform_discr(a, MIN_POS, MAX_POS-1);
    	hm_cond.addLiteral(COND_POS, Constants.KM+cond_pos);
//    	Conjunction ran_c = new Conjunction(hm_cond);

    	//prohibition (same applicability as condition)
    	hm_proh.addLiteral(PROH_SPEED, ""+ Utils.uniform_discr(a, MIN_SPEED, MAX_SPEED));
//    	Conjunction ran_p = new Conjunction(hm_proh);

    	//deadline
    	hm_dead.addLiteral(DEAD_POS, Constants.KM+ Utils.uniform_discr(a, cond_pos+1, MAX_POS));
//    	Conjunction ran_d = new Conjunction(hm_dead);

    	List<Conjunction> c = new ArrayList<>();
    	c.add(hm_cond);
    	this.cond = c;
    	List<Conjunction> p = new ArrayList<>();
    	p.add(hm_proh);
    	this.proh = p;
    	List<Conjunction> d = new ArrayList<>();
    	d.add(hm_dead);
    	this.dead = d;
    	
    }

	public MaxSpeedNorm(String id, List<Conjunction> cond_list, List<Conjunction> proh_list, List<Conjunction> dead_list, Random r) {
		/**
		 * When I create a norm, I make sure that every disjunct respects the domain constraints
		 * I also discard (i.e., set all components to null) in case the condition and deadlines are at the same position
		 */
		super(id, r);
		List<Conjunction> cond = new ArrayList<>();
		List<Conjunction> proh = new ArrayList<>();
		List<Conjunction> dead = new ArrayList<>();

		for (Conjunction d: cond_list) {
			if(d.getNumberOfLiteralsOfType(COND_POS)>1) {
				//since they are in AND  (km2 and km3) to be true must necessary be km3 in the highway, so I need to remove the smallest
				int max_pos_val = Constants.BIGMNEGATIVE;
				String max_pos_str = "";
				for (String pos : d.getLiteralsListFromKeyType(COND_POS)) {
					int pos_val = Integer.parseInt(pos.replace(Constants.KM,""));
					if(pos_val>max_pos_val) {
						max_pos_val = pos_val;
						max_pos_str = pos;
					}
				}
				d.cleanLiteralsType(COND_POS, max_pos_str);
			}
			cond.add(d);
		}
		for (Conjunction d: proh_list) {
			if(d.getNumberOfLiteralsOfType(PROH_SPEED)>1) {
				//since they are in AND (SP2 and SP3) to be true must necessary be sp3 in the highway, so I need to remove the smallest
				int max_speed_val = Constants.BIGMNEGATIVE;
				String max_speed_str = "";
				for (String speed : d.getLiteralsListFromKeyType(PROH_SPEED)) {
					int speed_val = Integer.parseInt(speed);
					if(speed_val>max_speed_val) {
						max_speed_val = speed_val;
						max_speed_str = speed;
					}
				}
				d.cleanLiteralsType(PROH_SPEED, max_speed_str);
			}
			proh.add(d);
		}
		for (Conjunction d: dead_list) {
			if(d.getNumberOfLiteralsOfType(DEAD_POS)>1) {
				//since they are in AND  (km2 and km3) to be true must necessary be km3 in the highway, so I need to remove the smallest
				int max_pos_val = Constants.BIGMNEGATIVE;
				String max_pos_str = "";
				for (String pos : d.getLiteralsListFromKeyType(DEAD_POS)) {
					int pos_val = Integer.parseInt(pos.replace(Constants.KM,""));
					if(pos_val>max_pos_val) {
						max_pos_val = pos_val;
						max_pos_str = pos;
					}
				}
				d.cleanLiteralsType(DEAD_POS, max_pos_str);
			}
			dead.add(d);
		}

		this.cond = cond;
		this.proh = proh;
		this.dead = dead;

		if(this.isDisabled()) {
			this.cond = null;
			this.proh = null;
			this.dead = null;
		}
	}
    

    @Override
    public List getProhibitionRelatedProp(State s) {
		return Arrays.asList(Math.round(s.speed)+"", s.type);
	}



	public double getSpeed(String veh_type) {
		/**
		 * Returns the speed limit for the vehicle of type veh_type
		 *
		 * a prohibition in dnf could be something like
		 * Prohibition p = d1 OR d2
		 * where d1 is (sp10 and sp18)
		 * and d2 is (sp8 and sp20)
		 * d1 is equivalent to sp18 (THE MAX BETWEEN SP10 AND SP18) because the semantics is that it is prohibited a state
		 * where both sp10 ad sp18 are true, which can be true only if sp18 is true, which means that we are probhiting speeds hihger than 18
		 * and analogously for d2, which should lead to sp20
		 * when I consider the entire DNF I have to take instead THE MINIMUM
		 * so p in the example should prohibit speeds higher than 18
		 *
		 * Furthermore the speed in every disjunct should apply to the particular type
		 */
		double prohibited_speed = MAX_SPEED;
		for (Conjunction d: this.getProhibition()) {
			if(d.containsKeyType(PROH_SPEED) &&
					((d.containsKey(PROH_APPL+"1") && d.getLiteral(PROH_APPL+"1").equals(veh_type)) ||
							(d.containsKey(PROH_APPL+"2") && d.getLiteral(PROH_APPL+"2").equals(veh_type)))){
					double d_sp = Double.parseDouble(d.getLiteral(PROH_SPEED));
					if(d_sp<prohibited_speed)
						prohibited_speed = d_sp;
			}
		}
    	return prohibited_speed;
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
		boolean at_least_one_disj_sat = false;
    	switch(comp_type) {
	    	case Constants.CONDITION:
				for (Conjunction phi : cond) {
					//IT IS ASSUEMED THAT EVERY DISJUNCT CONTAINS AT LEAST THE POSITION FOR THE CONDITION. THIS IS A DOMAIN SPECIFIC CONSTRAINT THAT SHOULD BE SATISFIED HERE
					if ((Integer.parseInt(state.position.replace(Constants.KM, "")) >= (Integer.parseInt(phi.getLiteral(COND_POS).replace(Constants.KM, ""))))
							&& (phi.getNumberOfLiteralsOfType(COND_APPL)==0 || (phi.getNumberOfLiteralsOfType(COND_APPL)==1 && state.type.equals(phi.getLiteral(COND_APPL))))
					)
						at_least_one_disj_sat = true;
				}
	    		break;
			case Constants.PROHIBITION:
				for (Conjunction phi : proh) {
					if(state.speed >= Double.parseDouble(phi.getLiteral(PROH_SPEED)))
						at_least_one_disj_sat = true;
				}
	    		break;
	    	case Constants.DEADLINE:
				for (Conjunction phi : dead) {
					if (Integer.parseInt(state.position.replace(Constants.KM, "")) >= (Integer.parseInt(phi.getLiteral(DEAD_POS).replace(Constants.KM, ""))))
						at_least_one_disj_sat = true;
				}
	    		break;
    	}
    	return at_least_one_disj_sat;
    }
    


	public Set<Conjunction> buildConjMoreSpecProh(Set<String> PS_prop) {
		/**
		 * Function to build conjunctions from propositions in PS_prop to later construct more specific prohibitions
		 */
		Set<Conjunction> builtConj = new HashSet<>();
		for (Conjunction d: this.proh) {
			Set<String> bounded_PS_prop = new HashSet<>();
			ArrayList<Integer> sp = new ArrayList<>();
			for(String s : PS_prop)
				try {
					int possible_speed = Integer.parseInt(s);
					//if here it is actually the speed and not the type
					sp.add(possible_speed);
				} catch (NumberFormatException nfe) {
					//here it means it was a type
					bounded_PS_prop.add(s);
				}
			if(sp.size()> SPACE_PARAM) {
				Collections.sort(sp);
				for(int i = 0; i< SPACE_PARAM; i++)
					bounded_PS_prop.add(sp.get(i)+"");
			} else
				for (Integer integer : sp) bounded_PS_prop.add(integer + "");
			if(bounded_PS_prop.size()>0)
				bounded_PS_prop.add(d.getLiteral(PROH_SPEED));

			Set<Set<String>> power = Sets.powerSet(bounded_PS_prop);

			for (Set<String> gen_s : power) {
				Conjunction new_conjunction = new Conjunction();
				new_conjunction.addLiterals(d.getLiteralsMap());//i create the new one identical to the current one
				int max_sp = Constants.BIGMNEGATIVE;
				for(String str : gen_s) {
					try {
						int possible_speed = Integer.parseInt(str);
						//if here it is actually the speed and not the type
						max_sp = Math.max(max_sp, possible_speed);
					} catch (NumberFormatException nfe) {
						//here it means it was an application type
						new_conjunction.addLiteral(PROH_APPL, str);
					}
				}
				if(max_sp>Constants.BIGMNEGATIVE) {
					//I add also the speed
					new_conjunction.addLiteral(PROH_SPEED, max_sp+""); //replace the current one with the new speed
					//add the new proh
					builtConj.add(new_conjunction);
				}
				// else I ignore this set, as I always want at least the speed literal
			}
		}
		return builtConj;
	}

	public Set<Conjunction> buildConjLessSpecProh(Set<String> IPS_prop) {
		/**
		 * Function to build conjunctions from propositions in IPS_prop to later construct less specific prohibitions
		 */
		Set<Conjunction> builtConj = new HashSet<>();
		for (Conjunction d: this.proh) { //for every disjunct
			Set<String> bounded_IPS_prop = new HashSet<>();
			ArrayList<Integer> speeds = new ArrayList<>();
			for (String str : IPS_prop) { //retrieve from the states the propositions. Collect for now the speeds and add them later
				try {
					int possible_speed = Integer.parseInt(str);
					speeds.add(possible_speed);
				} catch (NumberFormatException nfe) {
					bounded_IPS_prop.add(str);
				}
			}
			Collections.sort(speeds); //sort the speeds in ascending order
			Collections.reverse(speeds); //reverse to obtain descending first (since we want to create less specific formula)
			//I select the first SPACE_PARAM in the list that are lower than the current speed
			//i want lower speeds since I want to create less specific formula
			int c = 0;
			for (int i = 0; i < speeds.size() && c < SPACE_PARAM; i++) {
				int sp = speeds.get(i);
				if (sp >= MIN_SPEED && sp < Integer.parseInt(d.getLiteral(PROH_SPEED))) {
					bounded_IPS_prop.add(sp + "");
					c++;
				}
			}
			if (bounded_IPS_prop.size() > 0)
				bounded_IPS_prop.add(d.getLiteral(PROH_SPEED));

			//I create the powerset to combine all of the propositions
			Set<Set<String>> power = Sets.powerSet(bounded_IPS_prop);

			//for every subset of propositions in the powerset
			for (Set<String> gen_s : power) {
				//I create a new conjunction
				Conjunction new_conjunction = new Conjunction();
				int max_sp = -1;
				for (String str : gen_s) { //for every proposition in the set
					try {
						int possible_speed = Integer.parseInt(str);
						max_sp = Math.max(max_sp, possible_speed);
					} catch (NumberFormatException nfe) { //if it's not a speed is an applicability proposition (e.g., car or truck)
						new_conjunction.addLiteral(PROH_APPL, str);
					}
				}
				if (max_sp > -1) { //if the set contained a speed
					if(new_conjunction.literals.size()==0 ||
								d.getLiteralsListFromKeyType(PROH_APPL).containsAll(new_conjunction.getLiteralsListFromKeyType(PROH_APPL))) {
						new_conjunction.addLiteral(PROH_SPEED, max_sp + "");
						builtConj.add(new_conjunction);
					}
//					else {
//						System.out.println("Discarding conjunction "+new_conjunction);
//					}
				}
			}
		}
		return builtConj;
	}
}

