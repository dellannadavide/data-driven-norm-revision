package simulation;

import com.google.common.collect.Sets;

import java.util.*;

public abstract class HighwayNorm extends DNFNorm{
    public static final int SPACE_PARAM = 8;
    public static final String COND_POS = "condpos"+Constants.SEPARATOR;
    public static final String COND_APPL = "appl"+Constants.SEPARATOR; //+ the index
    public static final String PROH_SPEED = "speed"+Constants.SEPARATOR;
    public static final String PROH_DIST = "dist"+Constants.SEPARATOR;
    public static final String PROH_APPL = "appl"+Constants.SEPARATOR; //+ the index
    public static final String DEAD_POS = "deadpos"+Constants.SEPARATOR;

    public static final int MIN_SPEED = 10;
    public static final int MAX_SPEED = 40;
    public static final int MIN_DIST = 0;
    public static final int MAX_DIST = 15;

    public static final String[] POSSIBLE_APPL = {Constants.BOTH, Constants.FALSE, Constants.CAR, Constants.TRUCK};

    public static final int MIN_POS = 1;
    public static final int MAX_POS = 10;

    public HighwayNorm(String id, Random r) {
        super(id, r);
    }

    public HighwayNorm(String id, List<Conjunction> cond, List<Conjunction> proh, List<Conjunction> dead, Random r) {
        super(id, cond, proh, dead, r);
    }

    @Override
    public List getConditionRelatedProp(State s) {
        return Arrays.asList(s.position, s.type);
    }
    public abstract List getProhibitionRelatedProp(State s);
    @Override
    public List getDeadlineRelatedProp(State s) {
        return Arrays.asList(s.position);
    }

    public int getDetachmentPosition(String veh_type) {
        int detachment_pos = MAX_POS+1;
        for (Conjunction d: this.getCondition()) {
            if(d.containsKeyType(COND_POS) &&
                    ((d.containsKey(COND_APPL+"1") && d.getLiteral(COND_APPL+"1").equals(veh_type)) ||
                            (d.containsKey(COND_APPL+"2") && d.getLiteral(COND_APPL+"2").equals(veh_type)))){
                int d_cond = Integer.parseInt(d.getLiteral(COND_POS).replace(Constants.KM, ""));
                if(d_cond<detachment_pos)
                    detachment_pos = d_cond;
            }
        }
        return detachment_pos;
    }

    public int getDeadlinePosition(String veh_type) {
        int deadline_pos = MIN_POS;
        for (Conjunction d: this.getDeadline()) {
            if(d.containsKeyType(DEAD_POS)){
                int d_dead = Integer.parseInt(d.getLiteral(DEAD_POS).replace(Constants.KM, ""));
                if(d_dead>deadline_pos)
                    deadline_pos = d_dead;
            }
        }
        return deadline_pos;
    }

    @Override
    protected boolean isDisabled() {
        /**
         * Function to determine whether the norm is equivalent to a disabled norm
         * A norm is considered to be equivalent to a disabled norm when it is impossible to be violated
         * either because it can never be detached or because te prohibition will never be satisfied given the condition
         * A norm can never be detached in the particular highway domain if the number of cond_appl is higher than 1 in all disjuncts in the ocndition
         * The prohibition can never be satisfied for the same reason
         * Anotehr reason for the norm to be equivalent to a disabled norm is when the condition refers e.g., to cars but the prohibition refers to trucks
         * More precisely, there must not exist a combination of disjuncts from the ocndition and prohibition that apply to the same vehicles
         * or, in other words, all combinations of disjucts from cond and proh apply to different (incompatible) vehicles
         */
//        System.out.println("--------- Testing if norm ("+cond+", "+proh+", "+dead+") is disabled: ");
        boolean conditionIsPossible = false;
        boolean prohibitionIsPossible = false;

        //first check if the condition is possible
        for (Conjunction conjunction : cond) {
            if(conjunction.getNumberOfLiteralsOfType(COND_APPL)<=1) {
                conditionIsPossible = true;
                break;
            }
        }
        if(!conditionIsPossible)
            return true;

        //then check if the prohibition is possible
        for (Conjunction conjunction : proh) {
            if(conjunction.getNumberOfLiteralsOfType(PROH_APPL)<=1){
                prohibitionIsPossible = true;
                break;
            }
        }
        if(!prohibitionIsPossible)
            return true;

//        return false;

        //if I'm here both conditions and prohibition are possible
        boolean isInvalid = true;
        //I check the last case to see if the cond and the proh are compatible (there must be a pair where they are identical)
        for (Conjunction cond_d : cond) {
            for (Conjunction proh_d : proh) {
                if(cond_d.getNumberOfLiteralsOfType(COND_APPL)==0 && proh_d.getNumberOfLiteralsOfType(PROH_APPL) == 0) {
                    isInvalid = false;
                    break;
                }
                else {
                    //it must be 1 (otherwise I returned already in the previous cases), so I take it
                    if(cond_d.getNumberOfLiteralsOfType(COND_APPL)==proh_d.getNumberOfLiteralsOfType(PROH_APPL)) {
                        String condappl = cond_d.getLiteral(COND_APPL);
                        String prohappl = proh_d.getLiteral(PROH_APPL);
                        if (condappl.equals(prohappl)) {
                            isInvalid = false;
                            break;
                        }
                    }
                }
            }
        }
//        for (Conjunction cond_d : cond) {
//            if(cond_d.getNumberOfLiteralsOfType(COND_APPL)==0)
//                isInvalid = false;
//            else { //it must be 1 (otherwise I returned already in the previous cases), so I get it
//                String condappl = cond_d.getLiteral(COND_APPL);
//                for (Conjunction proh_d : proh) {
//                    if (proh_d.getNumberOfLiteralsOfType(PROH_APPL) == 0)
//                        isInvalid = false;
//                    else {
//                        String prohappl = proh_d.getLiteral(PROH_APPL);
//                        if(condappl.equals(prohappl))
//                            isInvalid = false;
//                    }
//
//                }
//            }
//        }
        if(isInvalid)
            return true;


        for (Conjunction cond_d: cond) {
            for(Conjunction dead_d: dead) {
                if(Integer.parseInt(cond_d.getLiteral(COND_POS).replace(Constants.KM,"")) >=
                        Integer.parseInt(dead_d.getLiteral(DEAD_POS).replace(Constants.KM,"")))
                    isInvalid = true;
            }
        }

        return isInvalid;
    }

    @Override
    public String toString() {
        return super.toString();
    }



    public boolean appliesToType(String agentType) {
        /**
         * Function to determine if the norm applies to the give agentType.
         * The norm applies if in at least one of the Disjuncts in either the condition or the prohibition the norm specifies something about the agentType
         */

        if(!isDisabled()) {
            for (Conjunction cond_d : cond) {
                if (cond_d.getNumberOfLiteralsOfType(COND_APPL) == 0 ||
                        (cond_d.getLiteral(COND_APPL).equals(agentType))) {
                    //here we now that this disjunct applies, so I check if there is any proh that also applies
                    for (Conjunction proh_d : proh) {
                        if (proh_d.getNumberOfLiteralsOfType(PROH_APPL) == 0 ||
                                (proh_d.getLiteral(PROH_APPL).equals(agentType)))
                            return true; //if it's the case I return
                    }
                }
            }
            //if I reach here then the norm does not apply
            return false;
        }
        return false;
    }

    public Set<Conjunction> buildConjMoreSpecCond(Set<String> CS_prop) {
        /**
         * Function to build conjunctions from propositions in CS_prop to later construct more specific conditions
         */
        Set<Conjunction> builtConj = new HashSet<>();
        for (Conjunction d: this.cond) {
//			LinkedHashMap<String, String> cond_literals = d.getLiterals();
            Set<String> bounded_CS_prop = new HashSet<>();
            ArrayList<Integer> km = new ArrayList<>();
            for(String s : CS_prop)
                if(s.startsWith(Constants.KM)) {
                    km.add(Integer.parseInt(s.replace(Constants.KM,"")));
                }
                else bounded_CS_prop.add(s);
            if(km.size()> SPACE_PARAM) {
                Collections.sort(km);
                for(int i = 0; i< SPACE_PARAM; i++)
                    bounded_CS_prop.add(Constants.KM+km.get(i));
            } else
                for (Integer integer : km) bounded_CS_prop.add(Constants.KM + integer);
            if(bounded_CS_prop.size()>0)
                bounded_CS_prop.add(d.getLiteral(COND_POS));

            Set<Set<String>> power = Sets.powerSet(bounded_CS_prop);
            for (Set<String> gen_s : power) {
                Conjunction new_conjunction = new Conjunction(); //i create the new one identical to the current one
                new_conjunction.addLiterals(d.getLiteralsMap());
//				int i = new_literals.keySet().size(); //assuming there is always only one position, the remaining are the appl
                int max_km = -1;
                for(String str : gen_s)
                    if(str.startsWith(Constants.KM))
                        max_km = Math.max(max_km, Integer.parseInt(str.replace(Constants.KM, "")));
                    else {
                        //add the appl
//						if(!new_literals.containsValue(str)) {
//							new_literals.put(COND_APPL+""+i, str);
//							i++;
//						}
                        new_conjunction.addLiteral(COND_APPL, str);
                    }
                if(max_km>-1) {
                    new_conjunction.addLiteral(COND_POS, Constants.KM+max_km); //replace the current one with the new pos
                    builtConj.add(new_conjunction);
                }
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
//    public Set<Conjunction> getMoreSpecificConditions(List<Trace> violatingTraces) {
//    	Set<Conjunction> moreSpecificConditions = new HashSet<>();
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
//    	moreSpecificConditions.add(new Conjunction(cond_literals)); //one of the new weaening is also the current one
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
//    			moreSpecificConditions.add(new Conjunction(new_literals));
//    		}
//    		// else I ignore this set, as I always want at least the position literal
//		}
//    	//}
//    	return moreSpecificConditions;
//    }
//
public Set<Conjunction> buildConjLessSpecDead(Set<String> CPS_prop) {
    /**
     * Function to build conjunctions from propositions in CPS_prop to later construct less specific deadlines
     */
    Set<Conjunction> builtConj = new HashSet<>();
    for (String s : CPS_prop) {
        //System.out.println(s);
        Conjunction new_conjunction = new Conjunction();
        new_conjunction.addLiteral(DEAD_POS, s);
        builtConj.add(new_conjunction);
    }
    return builtConj;
}
//
//    public Set<Conjunction> getLessSpecificDeadlines(List<Trace> violatingTraces) {
//    	Set<Conjunction> lessSpecificDeadlines = new HashSet<>();
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
//    	 	lessSpecificDeadlines.add(new Conjunction(new_literals));
//    	}
//    	return lessSpecificDeadlines;
//    }
//
//    /*for strengthening the norm - these are norm-specific*/

    public Set<Conjunction> buildConjLessSpecCond(Set<String> OPS_prop) {
        /**
         * Function to build conjunctions from propositions in OPS_prop to later construct less specific conditions
         */
        Set<Conjunction> builtConj = new HashSet<>();
        for (Conjunction d: this.cond) {
//			LinkedHashMap<String, String> cond_literals = d.getLiterals(); //the current condition
            Set<String> bounded_OPS_prop = new HashSet<>();
            ArrayList<Integer> km = new ArrayList<>();
            for (String s : OPS_prop)
                if (s.startsWith(Constants.KM)) {
                    km.add(Integer.parseInt(s.replace(Constants.KM, "")));
                } else bounded_OPS_prop.add(s);

            Collections.sort(km);
            Collections.reverse(km);
            int c = 0;
            for (int i = 0; i < km.size() && c < SPACE_PARAM; i++) {
                if (km.get(i) < Integer.parseInt(d.getLiteral(COND_POS).replace(Constants.KM, ""))) {
                    bounded_OPS_prop.add(Constants.KM + km.get(i));
                    c++;
                }
            }
            if (bounded_OPS_prop.size() > 0)
                bounded_OPS_prop.add(d.getLiteral(COND_POS));

            Set<Set<String>> power = Sets.powerSet(bounded_OPS_prop); //this is PHI

            for (Set<String> gen_s : power) {
                Conjunction new_conjunction = new Conjunction();
//				int i = 1;
                int max_km = -1;
                for (String str : gen_s)
                    if (str.startsWith(Constants.KM))
                        max_km = Math.max(max_km, Integer.parseInt(str.replace(Constants.KM, "")));
                    else {
                        //add the appl
//						if (!new_literals.containsValue(str)) {
//							new_literals.put(COND_APPL + "" + i, str);
//							i++;
//						}
                        new_conjunction.addLiteral(COND_APPL, str);
                    }
                if (max_km > -1) {
//					LinkedHashMap<String, String> curr_literals = new LinkedHashMap<>(cond_literals);
//					curr_literals.remove(COND_POS);
//					if (new_literals.values().size() == 0 ||
//							(curr_literals.values().containsAll(new_literals.values()))) {
//						new_literals.put(COND_POS, "km" + max_km); //replace the current one with the new pos
//						builtConj.add(new_literals);
//					}
                    if(new_conjunction.literals.size()==0 ||
                            d.getLiteralsListFromKeyType(COND_APPL).containsAll(new_conjunction.getLiteralsListFromKeyType(COND_APPL))) { //or if it applies exactly to the same vehicles as the original norm
                        new_conjunction.addLiteral(COND_POS, Constants.KM + max_km);
                        builtConj.add(new_conjunction);
                    }
                }
            }
        }
        return builtConj;
    }
//
//    public Set<Conjunction> getLessSpecificConditions(List<Trace> obeyingTraces){
//    	//to modify only the condition is possible only if there is some state in the obeying traces
//    	//that violates the prohibition (even though the norm is obeyed)
//
//    	//i'm assuming that thre is always a deadline in the traces
//
//    	Set<Conjunction> lessSpecificConditions = new HashSet<>();
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
//    	lessSpecificConditions.add(new Conjunction(cond_literals)); //one of the new strengthening is also the current one
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
//        			lessSpecificConditions.add(new Conjunction(new_literals));
//    			}
//    			// else I ignore this set, as it's not a strengthening
//    		}
//    		// else I ignore this set, as I always want at least the position literal
//		}
//
//    	return lessSpecificConditions;
//
//    }

    public Set<Conjunction> buildConjMoreSpecDead(Set<String> DS_prop) {
        /**
         * Function to build conjunctions from propositions in DS_prop to later construct more specific deadlines
         */
        Set<Conjunction> builtConj = new HashSet<>();
        Set<String> bounded_DS_prop = new HashSet<>();
        if(DS_prop.size()> SPACE_PARAM) {
            ArrayList<String> km_str = new ArrayList<>(DS_prop);
            Collections.sort(km_str);
            for(int i = 0; i< SPACE_PARAM; i++)
                bounded_DS_prop.add(km_str.get(i));
        } else bounded_DS_prop.addAll(DS_prop);
        for (String s : bounded_DS_prop) {
            Conjunction new_conjunction = new Conjunction();
            new_conjunction.addLiteral(DEAD_POS, s);
            builtConj.add(new_conjunction);
        }
        return builtConj;
    }
//
//    public Set<Conjunction> getMoreSpecificDeadlines(List<Trace> obeyingTraces){
//    	Set<Conjunction> moreSpecificDeadlines = new HashSet<>();
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
//    		moreSpecificDeadlines.add(new Conjunction(new_literals));
//    	}
//
//
//    	return moreSpecificDeadlines;
//    }


//    protected void checkIfToDiscard() {
//        /**+
//         * Checks if the condition of the norm refers to a km that is equal (a "disabled" norm) or after the km of the deadline (a non-sense).
//         * In that case sets to null te components of the norm.
//         */
//        boolean to_discard = false;
//        for (Conjunction cond_d: cond) {
//            for(Conjunction dead_d: dead) {
//                if(Integer.parseInt(cond_d.getLiteral(COND_POS).replace("km","")) >=
//                        Integer.parseInt(dead_d.getLiteral(DEAD_POS).replace("km","")))
//                    to_discard = true;
//            }
//        }
//        if(to_discard) {
//            this.cond = null;
//            this.proh = null;
//            this.dead = null;
//        }
//    }

}
