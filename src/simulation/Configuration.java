package simulation;
import java.util.*;

import static java.util.stream.Collectors.*;
import static java.util.Map.Entry.*;


public class Configuration {
	/**
	 * Class representing a configuration of norms
	 * It maintains a map normID -> DNFNorm which contains all norms in the configuration (with their id)
	 * and a map normID -> Class of the norm, which contains the class of the norms in the configuration, used to
	 * synthesise new norms of the sam type
	 */
	LinkedHashMap<String, DNFNorm> map;
	LinkedHashMap<String, Class> normsTypes;
	/**
     * Default constructor
     */
    public Configuration() {
    	this.map = new LinkedHashMap<String, DNFNorm>();
		this.normsTypes = new LinkedHashMap<String, Class>();
    }
    
    public Configuration(LinkedHashMap<String, DNFNorm> map, LinkedHashMap<String, Class> normsTypes) {
    	boolean isEmpty = false;
    	for(DNFNorm n : map.values())
    		if(n.isEmpty()) {
    			isEmpty = true;
    			break;
    		}
    	
    	if(isEmpty) 
    		this.map = new LinkedHashMap<String, DNFNorm>();
    	else
    		this.map = map;

		this.normsTypes = normsTypes;

    }
    
    public DNFNorm get(String id) {
    	if(map.containsKey(id))
			return map.get(id);
		else
			return null;
    }
    
    public LinkedHashMap<String, DNFNorm> getMap() {
    	return map;
    }

	public LinkedHashMap<String, Class> getNormsTypes() {
		return normsTypes;
	}
    
    public Collection<DNFNorm> getNorms() {
    	return map.values();
    }
    
    public int size() {
    	return map.size();
    }
    
    public boolean isEmpty() {
    	return map==null || map.isEmpty();
    }
    
	@Override
	public String toString() {
		String str = "";
		int i = 0;
		Iterator<Map.Entry<String, DNFNorm>> iter = this.map.entrySet().iterator();
		while (iter.hasNext()) //this loops for all simulation.norms, but now is only one
		{
			Map.Entry<String, DNFNorm> n_conf = iter.next();
			str = str+(i==0?"":", ")+n_conf.getKey()+": "+n_conf.getValue().toString();
			i++;
		}
		
		return str;
	}
	
	@Override
    public boolean equals(Object obj) {
		if(this == obj) 
            return true; 
          
        if(obj == null || obj.getClass()!= this.getClass()) 
            return false; 
          
        Configuration c = (Configuration) obj; 
        return (c.toString().equals(this.toString()));         
    }
	
	
	private LinkedHashMap<String, DNFNorm> getSortedMap() {
		return map.entrySet().stream().sorted(comparingByKey()).collect(toMap(e -> e.getKey(), e -> e.getValue(),(e1, e2) -> e2, LinkedHashMap::new));
	}
	
	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		Iterator<Map.Entry<String, DNFNorm>> iter = getSortedMap().entrySet().iterator();
        while (iter.hasNext())
		{
		    result = result * prime + iter.next().getValue().toString().hashCode();
		}
		return result;
	}

	public void evalTrace(Trace t) {
		/**
		 * Function that, given a trace, adds to it an evaluation of the norms in the configuration
		 */
		Iterator<Map.Entry<String, DNFNorm>> iter = getMap().entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, DNFNorm> n_map = iter.next();
			int isviolAt = n_map.getValue().isViol(t);
			t.addNormEval(n_map.getKey(), isviolAt>-1);
		}
	}

	public void evalTraces(ArrayList<Trace> traces) {
		/**
		 * Function that add to each trace in a dataset of traces an evaluation of the norms in the configuration
		 */
		for(Trace t: traces)
			evalTrace(t);
	}
}