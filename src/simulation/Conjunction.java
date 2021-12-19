package simulation;

import java.util.*;
import java.util.stream.Collectors;

public class Conjunction implements Cloneable {
	/**
	 * Class representing a Conjunction (i.e., a conjunction of literals)
	 * The conjunction is simply represented as a list of literals that are true
	 * To be more precise it is a map that links a type of literal (e.g., literal related to the position of the vehicle in the condition of a norm)  with a value
	 */

	LinkedHashMap<String, String> literals = new LinkedHashMap<>();
	LinkedHashMap<String, Integer> keyslastindex = new LinkedHashMap<>();

    /**
     * Default constructor
     */
    public Conjunction() {
    }

    /**
     * Constructor initializing the fields
     */
    public LinkedHashMap<String, String> getLiteralsMap() {
    	return literals;
    }
    public ArrayList<String> getLiteralsList() {
    	return new ArrayList<>(literals.values());
    }
    public ArrayList<String> getLiteralsListFromKeyType(String key) {
		ArrayList<String> list = new ArrayList<>();
		for(int i = 1; i<=getNumberOfLiteralsOfType(key); i++)
			if(literals.containsKey(key+""+i))
				list.add(literals.get(key+""+i));
    	return list;
    }
	public LinkedHashMap<String, Integer> getKeysIndecesMap() { return keyslastindex; }

	@Override
	public Object clone() {
		Conjunction d = new Conjunction();
		try {
			d = (Conjunction) super.clone();
			d.setLiterals(this.literals);
			d.setkeyslastIndeces(this.keyslastindex);
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return d;
	}

	private void setkeyslastIndeces(LinkedHashMap<String, Integer> keyslastindex) {
		this.keyslastindex = new LinkedHashMap<>();
		for(String key: keyslastindex.keySet()) {
			this.keyslastindex.put(key, keyslastindex.get(key));
		}
	}

	private void setLiterals(LinkedHashMap<String, String> literals) {
		this.literals =  new LinkedHashMap<>();
		for(String key: literals.keySet()) {
			this.literals.put(key, literals.get(key));
		}
	}

	public void addLiterals(LinkedHashMap<String, String> newliterals) {
		for(String key: newliterals.keySet()) {
			String k = key.split(Constants.SEPARATOR)[0]+Constants.SEPARATOR;
			addLiteral(k, newliterals.get(key));
		}
	}

	public boolean containsKeyType(String keyType) {
		return this.keyslastindex.containsKey(keyType);
	}
	public boolean containsKey(String key) {
		return this.literals.containsKey(key);
	}
	public String getLiteral(String key) {
		try {
			if(literals.containsKey(key)) {
				return this.literals.get(key);
			}
			else {
				if(keyslastindex.containsKey(key) && keyslastindex.get(key)==1) {
					return this.literals.get(key+"1");
				}
				else {
					System.out.println(this.keyslastindex);
					throw new NullPointerException("WARNING: You are trying to access a literal ("+key+") in Conjunction ("+this.literals+") that does not exist.");
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		return null;
	}

	public int getNumberOfLiteralsOfType(String key) {
		return this.keyslastindex.getOrDefault(key, 0);
	}

	public void addLiteral(String key, String str) {
		if(!literals.containsValue(str)) {
			if (containsKeyType(key)) {
				int new_index_for_key = keyslastindex.get(key) + 1;
				this.literals.put(key + "" + new_index_for_key, str);
				this.keyslastindex.put(key, new_index_for_key);
			} else {
				this.literals.put(key + "1", str);
				this.keyslastindex.put(key, 1);
			}
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
        Conjunction d = (Conjunction) obj;
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

	public void cleanLiteralsType(String keyType, String onlyToKeep) {
		//first I remove all the existing ones
		ArrayList<String> to_remove = new ArrayList<>();
		for(String s : literals.keySet()) {
			if(s.startsWith(keyType))
				to_remove.add(s);
		}
		for(String s : to_remove) {
			literals.remove(s);
		}
		keyslastindex.remove(keyType);
		//then I put the only one to keep
		addLiteral(keyType, onlyToKeep);
	}

}