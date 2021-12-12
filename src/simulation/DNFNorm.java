package simulation;
import java.util.*;
public abstract class DNFNorm {
	/**
	 * Class representing a norm whose components are expressed in DNF (represented as list of Disjuncts, i.e., a list of lists of conjuncts)
	 * Each norm has three components:
	 * cond: the condition
	 * proh: the prohibition
	 * dead: the deadline
	 * and it is associated to an id
	 *
	 * This class is an abstract class which will be implemented by the particular types of norms
	 */

	protected Random r;
	String id;
	List<Disjunct> cond;
	List<Disjunct> proh;
	List<Disjunct> dead;


    public DNFNorm(String id, Random r) {
		this.id = id;
		this.r = r;
    }

    public DNFNorm(String id, List<Disjunct> cond, List<Disjunct> proh, List<Disjunct> dead) {
		this.id = id;
    	this.cond = cond;
		this.proh = proh;
		this.dead = dead;
		consistencyCheck();
    }
       
    public String getID() {
    	return this.id;
    }
    
    protected abstract void consistencyCheck();
    
    public boolean isEmpty() {
    	return cond==null || proh==null || dead==null;
    }
    
    /**
     * Determines if a trace violates the norm
     * @param trace
     * @return
     */
    public int isViol(Trace trace) {
		boolean detached = false;
		for(int i=0;i<trace.getLength();i++) {
			State state = trace.getStates().get(i);
			if(isSat("cond", state)) {
				detached = true;
			}
			if(detached) {
				boolean isSatDead = isSat("dead", state);
				if(isSat("proh", state) && !isSatDead)
					return i; //System.out.println("norm violated in state "+state);
				if(isSatDead)
					detached = false; //System.out.println("deadline reached in state "+state);
			}
		}
		return -1;
	}
    
    /**
     * Determine if the norm component is sat in the state. It is norm-specific, so it needs to be overridden
     */
    protected abstract boolean isSat(String comp_type, State s);

    /**
     * determines whether in the last state of a trace the norm holds
     * @param trace
     * @return
     */
    public boolean currentlyApplies(Trace trace) {
    	boolean detached = false;
    	for(int i=0;i<trace.getLength();i++) {
			State state = trace.getStates().get(i);
			if(isSat("cond", state))
				detached = true;
			
			if(isSat("dead", state))
				detached = false;
    	}
    	return detached;
    }

	/**
	 * Determines if the norm applies to the particular type of agent
	 * @param agentType
	 * @return
	 */
    public abstract boolean appliesToType(String agentType);


    public Set<State> getDetachmentStates(Trace trace) {
		/**
		 * Determines the set of states in prohibited traces where
		 * n is detached and, after that, a prohibited target state is encountered before reaching the
		 * deadline first.
		 *
		 * $CS \gets \{s_i \mid\ \exists (s_1, \dots,s_i,\dots s_m)\in\Gamma$ s.t.
		 * $s_i\vDash\phi_C$, and
		 * $\exists j \mid i\leq j\leq m$ s.t. $s_j\vDash\phi_P$,
		 * and $\nexists k \mid  i\leq k\leq j$ s.t. $s_k\vDash\phi_D\}$
		 * @param trace
		 * @return
		 */
    	Set<State> detachment_states = new HashSet<>();
    	Set<State> temp_detachment_states = new HashSet<>();
    	boolean detached = false;
    	for(int i=0;i<trace.getLength();i++) {
    		State state = trace.getStates().get(i);
    		if(isSat("cond", state)) {
				detached = true;
				temp_detachment_states.add(state);
    		}
    		
    		if(detached) {
    			boolean isSatDead = isSat("dead", state);
				if(isSat("proh", state) && !isSatDead) {
					detachment_states.addAll(temp_detachment_states);
					temp_detachment_states = new HashSet<>();
					detached = false;
				}
    			if(isSatDead) {
    				temp_detachment_states = new HashSet<>();
					detached = false;
    			}
    		}    		
    	}
    	return detachment_states;
    }

    public Set<State> getViolatingStates(Trace trace) {
		/**
		 * Determines the set of states in prohibited traces where
		 * all literals in the prohibited target state hold after the norm is detached and before the deadline is reached
		 *
		 * $PS \gets \{s_i \mid\ \exists (s_1, \dots,s_i,\dots s_m)\in\Gamma$ s.t. $s_i\vDash\phi_P$, and
		 * $\exists j \mid 1\leq j\leq i$ s.t. $s_j\vDash\phi_C$,
		 * and
		 * $\nexists k \mid  j\leq k\leq i$ s.t. $s_k\vDash\phi_D\}$
		 * @param trace
		 * @return
		 */
    	Set<State> violating_states = new HashSet<>();
    	boolean detached = false;
    	for(int i=0;i<trace.getLength();i++) {
    		State state = trace.getStates().get(i);
    		if(isSat("cond", state))
				detached = true;
    		if(detached) {
				boolean isSatDead = isSat("dead", state);
				if(isSat("proh", state) && !isSatDead) { //here it's violated
					violating_states.add(state);
				}
    			if(isSatDead)
					detached = false;
    		}    		
    	}
    	return violating_states;
    }


    public Set<State> getStatesBetweenCondandProh(Trace trace) {
		/**
		 * Determines the set of states
		 * that are in between states where the condition and the prohibition hold in prohibited
		 * traces.
		 *
		 * $CPS \gets \{s_i \mid\ \exists (s_1, \dots,s_i,\dots s_m)\in\Gamma$ s.t.
		 * $\exists j,k \mid 1\leq j < i < k\leq m$ s.t. $s_j\vDash\phi_C$, $s_k\vDash\phi_P$,
		 * and
		 * $\nexists l \mid  j\leq l\leq k$ s.t. $s_l\vDash\phi_D\}$
		 *
		 */
		Set<State> betcondproh_states = new HashSet<>();
		Set<State> betcondproh_states_temp = new HashSet<>();
        int det_pos = -1;

        boolean detached = false;
        for(int i=0;i<trace.getLength();i++) {
            State state = trace.getStates().get(i);
            if(!detached)
                if(isSat("cond", state)) {
                    detached = true;
                    det_pos = i;
                }

            if(detached) {
                boolean isSatDead = isSat("dead", state);
                if(i>det_pos) {
					betcondproh_states_temp.add(state); //I add it, if the state also sat the deadline it will be removed later
                    if(isSat("proh", state) && !isSatDead) { //here it's violated
                        //add everything found so far
						betcondproh_states.addAll(betcondproh_states_temp);
						betcondproh_states_temp = new HashSet<>();
					}
				}
    			if(isSatDead) {
					//here I found the deadline without violating the proh (otherwise the function already returned), it's not what I need
					detached = false;
					det_pos = -1;
					betcondproh_states_temp = new HashSet<>();
				}
			}
		}
		return betcondproh_states;
    }

    public Set<State> getStatesBetweenCondandDead(Trace trace) {
		/**
		 * Determines the set of states that are in between
		 * states where the condition and the deadline hold in permitted traces, if any
		 *
		 * $IPS \gets \{s_i \mid\ \exists \rho=(s_1, \dots,s_i,\dots s_m)\in\Gamma$ s.t. $n$ is not violated on $\rho$ and
		 * $\exists j,k \mid 1\leq j\leq i<k\leq m$ s.t. $s_j\vDash\phi_C$
		 * and  $s_k\vDash\phi_D\}$
		 * @param trace
		 * @return
		 */
		Set<State> betconddead_states = new HashSet<>();
		Set<State> betconddead_states_temp = new HashSet<>();
    	boolean detached = false;
		for(int i=0;i<trace.getLength();i++) {
			State state = trace.getStates().get(i);
			if(isSat("cond", state)) {
				detached = true;
			}
			if(detached) {
				if(isSat("dead", state)) {//here I found the deadline, i store what found so far
					betconddead_states.addAll(betconddead_states_temp);
					detached = false; //and reinit the detached
					betconddead_states_temp = new HashSet<>();
				}
				else {
					betconddead_states_temp.add(state);
				}
			}
		}
		return betconddead_states;
    }


	public Set<State> getStatesOutsideWindowWhereProhHold(Trace trace) {
		/**
		 * Determines the set of states in permitted traces
		 * where we know that P holds, if any
		 * $OPS \gets \{s_i \mid\ \exists \rho=(s_1, \dots,s_i,\dots s_m)\in\Gamma$ s.t. $s_i\vDash\phi_P$, and $n$ is not violated on $\rho$
		 * @param trace
		 * @return
		 */
		Set<State> ops_states = new HashSet<>();
		if(isViol(trace)==-1){
			for(int i=0;i<trace.getLength();i++) {
				State state = trace.getStates().get(i);
				if(isSat("proh", state)) //here it's violated (notice that here I accept also the deadline state
					ops_states.add(state);
			}
		}
		return ops_states;
	}


    public Set<State> getStatesWhereDeadlineHolds(Trace trace) {
		/**
		 * Determines the set of states where D holds (the deadline
		 * is reached), if any, in permitted traces after the norm is detached
		 *
		 * $DS \gets \{s_i \mid\ \exists \rho=(s_1, \dots,s_i,\dots s_m)\in\Gamma$ s.t. $n$ is not violated on $\rho$ and $s_i\vDash\phi_D$, and
		 * $\exists j \mid 1\leq j\leq i$ s.t. $s_j\vDash\phi_C$
		 * and $\nexists k \mid  j\leq k< i$ s.t. $s_k\vDash\phi_D\}$\\
		 *
		 * @param trace
		 * @return
		 */
    	Set<State> dead_states = new HashSet<>();
    	
    	boolean detached = false;
		for(int i=0;i<trace.getLength();i++) {
			State state = trace.getStates().get(i);
			if(isSat("cond", state)) {
				detached = true;
			}
			if(detached) {
				if(isSat("dead", state)) {
					dead_states.add(state);
					detached = false;
				}
			}
		}
		return dead_states;
    }
    
    
    /**
     * To retrieve the sets of propositions in the components of the norm
     * @return
     */
    public abstract Set getConditionProp();
    public abstract Set getProhibitionProp();
    public abstract Set getDeadlineProp();
    
    
    
    /**
     * Generic update, updates with a new list of disjuncts 
     * @param cond
     */
    public void updateCondition(List<Disjunct> cond) {
    	this.cond = cond;
    }
    public void updateProhibition(List<Disjunct> proh) {
    	this.proh = proh;
    }
    public void updateDeadline(List<Disjunct> dead) {
    	this.dead = dead;
    }
    
    
    public List<Disjunct> getCondition() {
    	return this.cond;
    }
    public List<Disjunct> getProhibition() {
    	return this.proh;
    }
    public List<Disjunct> getDeadline() {
    	return this.dead;
    }

	public Set<List<Disjunct>> getMoreSpecificFormulas(List<Disjunct> dnf_formula, Set<State> set_states, String component) {
		/**
		 * Function to synthesise a number of formulas more specific than dnf_formula
		 * todo: to check if we can generalize a little bit more
		 */
		Set<List<Disjunct>> msfs = new HashSet<>();
		Set<String> rel_prop = getRelProp(set_states, component);
		Set<LinkedHashMap<String, String>> C = buildConj(rel_prop, component, "more_spec");
		msfs.add(dnf_formula);
		for (LinkedHashMap<String, String> c: C) {
			List<Disjunct> msf = new ArrayList<>();
			for(Disjunct d:dnf_formula) {
				Disjunct d1 = new Disjunct();
				d1.addLiterals(d.literals);
				d1.addLiterals(c);
				msf.add(d1);
			}
			msfs.add(msf);
		}
		return  msfs;
	}

	public Set<List<Disjunct>> getLessSpecificFormulas(List<Disjunct> dnf_formula, Set<State> set_states, String component) {
		/**
		 * Function to synthesise a number of formulas less specific than dnf_formula
		 * todo: to check if we can generalize a little bit more
		 */
		Set<List<Disjunct>> lsfs = new HashSet<>();
		Set<String> rel_prop = getRelProp(set_states, component);
		Set<LinkedHashMap<String, String>> C = buildConj(rel_prop, component, "less_spec");
		lsfs.add(dnf_formula);
		for (LinkedHashMap<String, String> c: C) {
			List<Disjunct> lsf = new ArrayList<>();
			Disjunct d1 = new Disjunct();
			d1.addLiterals(c);
			lsf.add(d1);
			lsfs.add(lsf);
		}
		return  lsfs;
	}

	/**
	 * Function to get propositions in states set_states related to the component of the norm
	 * @param set_states
	 * @param component
	 * @return
	 */
	public abstract Set<String> getRelProp(Set<State> set_states, String component);

	public abstract List getConditionRelatedProp(State s);
	public abstract List getProhibitionRelatedProp(State s);
	public abstract List getDeadlineRelatedProp(State s);


	/**
	 * FUnction to determine a set of conjunctions
	 * @param prop
	 * @param component
	 * @param formula_type
	 * @return
	 */
	public abstract Set<LinkedHashMap<String, String>> buildConj(Set<String> prop, String component, String formula_type);

//	/*for weakening the norm - these are norm-specific*/
//    public abstract Set<Disjunct> getMoreSpecificConditions(List<Trace> traces);
//    public abstract Set<Disjunct> getMoreSpecificProhibitions(List<Trace> traces);
//    public abstract Set<Disjunct> getLessSpecificDeadlines(List<Trace> traces);
//    /*for strengthening the norm - these are norm-specific*/
//    public abstract Set<Disjunct> getLessSpecificConditions(List<Trace> traces);
//    public abstract Set<Disjunct> getLessSpecificProhibitions(List<Trace> traces);
//    public abstract Set<Disjunct> getMoreSpecificDeadlines(List<Trace> traces);


	/**
	 * Function to determine whether the norm is equivalent to a disabled norm
	 */
	protected abstract boolean isDisabled();

	@Override
	public String toString() {
		if(this.isEmpty()) {
			return "(,,)";
		}
		
		if(isDisabled())
			return "_|_ (disabled norm)";
		
		String cond_str = "";
		for(int i=0;i<cond.size();i++) {
			cond_str =  cond_str + "("+cond.get(i) + (i==cond.size()-1?")":") || ");
		}
		String proh_str = "";
		for(int i=0;i<proh.size();i++) {
			proh_str =  proh_str + "("+proh.get(i) + (i==proh.size()-1?")":") || ");
		}
		String dead_str = "";
		for(int i=0;i<dead.size();i++) {
			dead_str =  dead_str + "("+ dead.get(i) + (i==dead.size()-1?")":") || ");
		}
		
		return "("+cond_str+", P(" +proh_str+"), "+dead_str+")";
	}

}