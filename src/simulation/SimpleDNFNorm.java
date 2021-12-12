package simulation;

import java.io.Serializable;
import java.util.*;

public abstract class SimpleDNFNorm extends DNFNorm {
    /**
     * A DNFNorm where each component is composed by only one Disunct
     */

	public int SPACE_PARAM;

    public SimpleDNFNorm(String id, Random r) {
    	super(id, r);
    	this.SPACE_PARAM = 8;
    }
    
    /**
     * Case of simple norm, with ONE disjunct per component
     * @param cond
     * @param proh
     * @param dead
     */
    public SimpleDNFNorm(String id, Disjunct cond, Disjunct proh, Disjunct dead, Random r) {
    	super(id, r);
    	List<Disjunct> c = new ArrayList<>();
    	c.add(cond);
		this.cond = c;
		List<Disjunct> p = new ArrayList<>();
    	p.add(proh);
		this.proh = p;
		List<Disjunct> d = new ArrayList<>();
    	d.add(dead);
		this.dead = d;
		consistencyCheck();

    }

    public SimpleDNFNorm(String id, List<Disjunct> cond, List<Disjunct> proh, List<Disjunct> dead, Random r) {
        super(id, r);
        List<Disjunct> c = new ArrayList<>();
        c.add(cond.get(0));
        this.cond = c;
        List<Disjunct> p = new ArrayList<>();
        p.add(proh.get(0));
        this.proh = p;
        List<Disjunct> d = new ArrayList<>();
        d.add(dead.get(0));
        this.dead = d;
        consistencyCheck();
    }
    
    public abstract boolean appliesToType(String agentType);
    
    
    /**
     * the list passed is the list of propositions in conjunction in the first (and only) disjunct
     * @param cond
     */
    public void updateSimpleCondition(LinkedHashMap<String, String> cond) {
    	List<Disjunct> c = new ArrayList<>();
    	c.add(new Disjunct(cond));
    	super.updateCondition(c);
		//this.cond = c;
    }
    public void updateSimpleProhibition(LinkedHashMap<String, String> proh) {
    	List<Disjunct> p = new ArrayList<>();
    	p.add(new Disjunct(proh));
		super.updateProhibition(p);
    }
    public void updateSimpleDeadline(LinkedHashMap<String, String> dead) {
    	List<Disjunct> d = new ArrayList<>();
    	d.add(new Disjunct(dead));
    	super.updateDeadline(d);
    }
    
    /**
     * Returns the the list of literals in the first and only disjunct
     * @return
     */
    
    public Disjunct getSimpleCondition() {
    	return this.cond.get(0); 
    }
    public Disjunct getSimpleProhibition() {
    	return this.proh.get(0); 
    }
    public Disjunct getSimpleDeadline() {
    	return this.dead.get(0); 
    }
    
    protected abstract boolean isDisabled();
    
    public abstract List getConditionRelatedProp(State s);
    public abstract List getProhibitionRelatedProp(State s); 
    public abstract List getDeadlineRelatedProp(State s); 
   
    
    /**
     * To retrieve the sets of propositions in the components of the norm
     * @return
     */
    public Set getConditionProp() {
    	Set prop = new HashSet<>();
    	prop.addAll(cond.get(0).getLiteralsList());
    	return prop;
    }
    
    public Set getProhibitionProp() {
    	Set prop = new HashSet<>();
    	prop.addAll(proh.get(0).getLiteralsList());
    	return prop;
    }
    
    public Set getDeadlineProp() {
    	Set prop = new HashSet<>();
    	prop.addAll(dead.get(0).getLiteralsList());
    	return prop;
    }

    @Override
    public Set<String> getRelProp(Set<State> set_states, String component) {
        Set<String> prop = new HashSet<String>();
        switch (component) {
            case "cond":
                for(State s: set_states) {
                    prop.addAll(this.getConditionRelatedProp(s)); //note that for example in a state with km_2 there is no explicit "km_1" prop
                }
                //todo to understand if to remove or not (also the others below)
//                prop.removeAll(this.getConditionProp()); //prop(CS) \ prop(φ_C)
                break;
            case "proh":
                for(State s: set_states) {
                    prop.addAll(this.getProhibitionRelatedProp(s)); //I get both type and speed. //note that for example in a state with sp_10 there is no explicit "sp_9", sp_8, 7, etc prop, but as for the km they are implicit
                }
//                prop.removeAll(this.getProhibitionProp()); //prop(PS) \ prop(φ_P). I remove the current ones (e.g., I remove {sp_10, car} from {sp_10, sp_11, sp_12, car, truck} )
                break;
            case "dead":
                for(State s: set_states) {
                    prop.addAll(this.getDeadlineRelatedProp(s));
                }
//                prop.removeAll(this.getDeadlineProp());
                break;
        }
        return  prop;
    }

    @Override
    public Set<LinkedHashMap<String, String>> buildConj(Set<String> prop, String component, String formula_type) {
        Set<LinkedHashMap<String, String>> conj = null;
        switch (component) {
            case "cond":
                switch (formula_type) {
                    case "more_spec":
                        return buildConjMoreSpecCond(prop);
                    case "less_spec":
                        return buildConjLessSpecCond(prop);
                }
                break;
            case "proh":
                switch (formula_type) {
                    case "more_spec":
                        return buildConjMoreSpecProh(prop);
                    case "less_spec":
                        return buildConjLessSpecProh(prop);
                }
                break;
            case "dead":
                switch (formula_type) {
                    case "more_spec":
                        return buildConjMoreSpecDead(prop);
                    case "less_spec":
                        return buildConjLessSpecDead(prop);
                }
                break;
        }
        return conj;
    }

    protected abstract Set<LinkedHashMap<String, String>> buildConjMoreSpecCond(Set<String> prop);
    protected abstract Set<LinkedHashMap<String, String>> buildConjLessSpecCond(Set<String> prop);
    protected abstract Set<LinkedHashMap<String, String>> buildConjMoreSpecProh(Set<String> prop);
    protected abstract Set<LinkedHashMap<String, String>> buildConjLessSpecProh(Set<String> prop);
    protected abstract Set<LinkedHashMap<String, String>> buildConjMoreSpecDead(Set<String> prop);
    protected abstract Set<LinkedHashMap<String, String>> buildConjLessSpecDead(Set<String> prop);


//    /*for weakening the norm - these are norm-specific*/
//    public abstract Set<Disjunct> getMoreSpecificConditions(List<Trace> traces);
//    public abstract Set<Disjunct> getMoreSpecificProhibitions(List<Trace> traces);
//    public abstract Set<Disjunct> getLessSpecificDeadlines(List<Trace> traces);
//    /*for strengthening the norm - these are norm-specific*/
//    public abstract Set<Disjunct> getLessSpecificConditions(List<Trace> traces);
//    public abstract Set<Disjunct> getLessSpecificProhibitions(List<Trace> traces);
//    public abstract Set<Disjunct> getMoreSpecificDeadlines(List<Trace> traces);
    

	@Override
	public String toString() {
		return super.toString();
	}


}