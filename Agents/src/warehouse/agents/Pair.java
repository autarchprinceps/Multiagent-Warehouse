package warehouse.agents;

import org.json.JSONObject;

/**
 * @author Patrick Robinson
 * 
 */
class Pair<K, V> {

    private final K first;
    private final V second;

    public Pair(K first, V second) {
        this.first = first;
        this.second = second;
    }
    
    public static Pair<String, Integer> convert(JSONObject obj) {
		String[] keys = JSONObject.getNames(obj);
		return new Pair<String, Integer>(keys[0], obj.getInt(keys[0]));
    }

    public K getFirst() {
        return first;
    }

    public V getSecond() {
        return second;
    }

    @Override
    public boolean equals(Object obj) {
    	if(obj instanceof Pair<?, ?>) {
    		// TODO check K == obj.K && V == obj.V
    		return first.equals(((Pair<K, V>)obj).getFirst()) && second.equals(((Pair<K, V>)obj).getSecond());
    	} else {
    		return false;
    	}
    }
    
    @Override
    public int hashCode() {
    	return first.hashCode() + second.hashCode();
    }
    
    @Override
    public String toString() {
    	return '{' + first.toString() + ':' + second.toString() + '}';
    }
}
