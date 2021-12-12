package simulation;

import java.util.*;
import java.util.stream.Collectors;

public class Disjunct implements Cloneable {
	/**
	 * Class representing a Disjunct (i.e., a conjunction of literals)
	 * The conjunction is simply represented as a list of literals that are true
	 * To be more precise it is a map that links a type of literal (e.g., literal related to the position of the vehicle in the condition of a norm)  with a value
	 */

	LinkedHashMap<String, String> literals = new LinkedHashMap<>();

    /**
     * Default constructor
     */
    public Disjunct() {
    }

    /**
     * Constructor initializing the fields
     */
    public Disjunct(LinkedHashMap<String, String> literals) {
		this.literals = literals;
    }
    public LinkedHashMap<String, String> getLiterals() {
    	return literals;
    }
    public ArrayList<String> getLiteralsList() {
    	return new ArrayList<>(literals.values());
    }

	@Override
	public Object clone() {
		Disjunct d = new Disjunct();
		try {
			d = (Disjunct) super.clone();
			d.setLiterals(this.literals);
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return d;
	}

	private void setLiterals(LinkedHashMap<String, String> literals) {
		this.literals =  new LinkedHashMap<>();
		for(String key: literals.keySet()) {
			this.literals.put(key, literals.get(key));
		}
	}

	public void addLiterals(LinkedHashMap<String, String> newliterals) {
		this.literals =  new LinkedHashMap<>();
		for(String key: newliterals.keySet()) {
			this.literals.put(key, newliterals.get(key));
		}
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		List<String> sortedval = literals.values().stream().sorted().collect(Collectors.toList());
		int i=0;
        for (String val : sortedval) {
            s.append(val).append(i == sortedval.size() - 1 ? "" : " & ");
            i++;
        }
		
		return s.toString();
	}
	
	@Override
    public boolean equals(Object obj) {
	    if(this == obj)
            return true;
        if(obj == null || obj.getClass()!= this.getClass()) 
            return false;
        Disjunct d = (Disjunct) obj; 
        return (d.literals.toString().equals(this.literals.toString())); 
    } 
	
	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		for (Map.Entry<String, String> stringStringEntry : literals.entrySet()) {
			result = result * prime + stringStringEntry.getValue().hashCode();
		}
		return result;
	}
}