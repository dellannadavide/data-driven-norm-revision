package simulation;

import java.util.*;
import com.google.common.collect.Sets;


public class MinDistNorm extends HighwayNorm {
	/**
	 * Class representing a MinDist, i.e. a norm like
	 * (c, P(x), d) which prohibits agents to have a distance lower than x from the next vehicle from km c to km d
	 * The norm can also express a type of agent (APPL, e.g., truck or car) to which the norm applies
	 */

     /**
     * Constructor used to create a random norm
     */
    public MinDistNorm(String id, Random a) {
    	super(id, a);
		Conjunction hm_cond = new Conjunction();
		Conjunction hm_proh = new Conjunction();
		Conjunction hm_dead = new Conjunction();
    	
    	//applicability
    	String ran_appl = POSSIBLE_APPL[Utils.uniform_discr(a,0, POSSIBLE_APPL.length-1)];
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

    	//prohibition (same applicability as condition)
		hm_proh.addLiteral(PROH_DIST, ""+ Utils.uniform_discr(a, MIN_DIST, MAX_DIST));
//    	Conjunction ran_p = new Conjunction(hm_proh);

    	//deadline
    	hm_dead.addLiteral(DEAD_POS, Constants.KM+ Utils.uniform_discr(a,cond_pos+1, MAX_POS));
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

	public MinDistNorm(String id, List<Conjunction> cond_list, List<Conjunction> proh_list, List<Conjunction> dead_list, Random r) {
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
			if(d.getNumberOfLiteralsOfType(PROH_DIST)>1) {
				//since they are in AND (dist2 and dist3) to be true must necessary be dist2 in the highway, so I need to remove the biggest
				int min_dist_val = Constants.BIGMPOSITIVE;
				String min_dist_str = "";
				for (String dist : d.getLiteralsListFromKeyType(PROH_DIST)) {
					int dist_val = Integer.parseInt(dist);
					if(dist_val<min_dist_val) {
						min_dist_val = dist_val;
						min_dist_str = dist;
					}
				}
				d.cleanLiteralsType(PROH_DIST, min_dist_str);
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
		return Arrays.asList(Math.round(s.dist)+"", s.type);
	}

	public double getDist(String veh_type) {
		/**
		 * Returns the required safety fistance for the vehicle of type veh_type
		 */
		double prohibited_dist = MIN_DIST;
		for (Conjunction d: this.getProhibition()) {
			if(d.containsKeyType(PROH_DIST) &&
					((d.containsKey(PROH_APPL+"1") && d.getLiteral(PROH_APPL+"1").equals(veh_type)) ||
							(d.containsKey(PROH_APPL+"2") && d.getLiteral(PROH_APPL+"2").equals(veh_type)))){
				double d_dst = Double.parseDouble(d.getLiteral(PROH_DIST));
				if(d_dst>prohibited_dist)
					prohibited_dist = d_dst;
			}
		}
		return prohibited_dist;
	}


	@Override
    protected boolean isSat(String comp_type, State state) { //this is norm-specific
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
					if(state.dist <= Double.parseDouble(phi.getLiteral(PROH_DIST)))
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
			Set<String> bounded_PS_prop = new HashSet<String>();
			ArrayList<Integer> distances = new ArrayList<Integer>();
			for(String s : PS_prop)
				try {
					int possible_speed = Integer.parseInt(s);
					distances.add(possible_speed);
				} catch (NumberFormatException nfe) {
					bounded_PS_prop.add(s);
				}
			Collections.sort(distances);
			Collections.reverse(distances);
			int c = 0;
			for(int i = 0; i<distances.size() && c< SPACE_PARAM; i++) {
				int dist = distances.get(i);
				if(dist>Integer.parseInt(d.getLiteral(PROH_DIST))) {
					bounded_PS_prop.add(dist+"");
					c++;
				}
			}
			if(bounded_PS_prop.size()>0)
				bounded_PS_prop.add(d.getLiteral(PROH_DIST));

			Set<Set<String>> power = Sets.powerSet(bounded_PS_prop); //this is PHI

			for (Set<String> gen_s : power) {
				Conjunction new_conjunction = new Conjunction();
				new_conjunction.addLiterals(d.getLiteralsMap());
				int min_dist = Constants.BIGMPOSITIVE;
				for(String str : gen_s) {
					try {
						int possible_dist = Integer.parseInt(str);
						min_dist = Math.min(min_dist, possible_dist);
					} catch (NumberFormatException nfe) {
						new_conjunction.addLiteral(PROH_APPL, str);
					}
				}
				if(min_dist<Constants.BIGMPOSITIVE) {
					new_conjunction.addLiteral(PROH_DIST, min_dist+"");
					builtConj.add(new_conjunction);
				}
			}
		}
		return builtConj;
	}

	public Set<Conjunction> buildConjLessSpecProh(Set<String> IPS_prop) {
		/**
		 * Function to build conjunctions from propositions in IPS_prop to later construct less specific prohibitions
		 */
		Set<Conjunction> builtConj = new HashSet<>();
		for (Conjunction d: this.proh) {
//			LinkedHashMap<String, String> proh_literals = d.getLiterals(); //the current prohibition
			Set<String> bounded_IPS_prop = new HashSet<String>();
			ArrayList<Integer> dist = new ArrayList<Integer>();
			for (String str : IPS_prop) {
				try {
					int possible_dist = Integer.parseInt(str);
					if (possible_dist <= MAX_DIST)
						dist.add(possible_dist);
					else
						dist.add(MAX_DIST);
				} catch (NumberFormatException nfe) {
					bounded_IPS_prop.add(str);
				}
			}
			if (dist.size() > SPACE_PARAM) {
				Collections.sort(dist);
				for (int i = 0; i < SPACE_PARAM; i++)
					bounded_IPS_prop.add(dist.get(i) + "");
			} else
				for (int i = 0; i < dist.size(); i++)
					bounded_IPS_prop.add(dist.get(i) + "");
			if (bounded_IPS_prop.size() > 0)
				bounded_IPS_prop.add(d.getLiteral(PROH_DIST));

			Set<Set<String>> power = Sets.powerSet(bounded_IPS_prop);

			for (Set<String> gen_s : power) {
				Conjunction new_conjunction = new Conjunction();
				int BIG_M = MAX_DIST + 1000;
				int min_dist = BIG_M;
				for (String str : gen_s) {
					try {
						int possible_dist = Integer.parseInt(str);
						min_dist = Math.min(min_dist, possible_dist);
					} catch (NumberFormatException nfe) {
						new_conjunction.addLiteral(PROH_APPL, str);
					}
				}
				if (min_dist < BIG_M) {
					if(new_conjunction.literals.size()==0 ||
							d.getLiteralsListFromKeyType(PROH_APPL).containsAll(new_conjunction.getLiteralsListFromKeyType(PROH_APPL))) {
						new_conjunction.addLiteral(PROH_DIST, min_dist + "");
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
