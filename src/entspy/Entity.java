package entspy;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Entity {
    int index;
    boolean mark = false;
    boolean autoedit = false;
    String classname;
    String targetname;
    String origin;
    public HashMap<String, Integer> kvmap = new HashMap();
    public ArrayList<String> keys = new ArrayList();
    public ArrayList<String> values = new ArrayList();
    public ArrayList<Entity> links = new ArrayList();

    public Entity() {
    }

    public Entity(String s) {
        this.classname = s;
    }

    public void clear() {
        this.classname = null;
        this.targetname = null;
        this.origin = null;
        if (this.keys != null) {
            this.keys.clear();
        }
        if (this.values != null) {
            this.values.clear();
        }
        if (this.links != null) {
            this.links.clear();
        }
    }

    public int getKeyIndex(String keyword) {
    	Integer k = kvmap.get(keyword);
    	
    	if(k != null)
    		return k;

        return -1;
    }

    public String getKeyValue(String keyword) {
        int ik = this.getKeyIndex(keyword);
        if (ik >= 0) {
            return this.values.get(ik);
        }
        return "";
    }

    public void setnames() {
    	this.classname = this.getKeyValue("classname");
        this.targetname = this.getKeyValue("targetname");
        this.origin = this.getKeyValue("origin");
    }

    public String getKeyValString(int i) {
        if (i < 0 || i >= this.keys.size()) {
            return null;
        }
        char quote = '\"';
        return "" + quote + this.keys.get(i) + quote + " " + quote + this.values.get(i) + quote;
    }
    
    public void setKeyVal(String k, String v) {
    	if(kvmap.containsKey(k)) {
    		values.set(kvmap.get(k), v);
    		setnames();
    		return;
    	}
    	
    	addKeyVal(k, v);
    }

    public void addKeyVal(String k, String v) {
    	kvmap.put(k, values.size());
    	
        this.keys.add(k);
        this.values.add(v);
        this.links.add(null);
        this.setnames();
    }

    public void delKeyVal(int i) {
        if (i < 0 || i >= this.size()) {
            return;
        }
        
        if(kvmap.containsKey(keys.get(i)))
        	kvmap.remove(keys.get(i));
        
        for(int j = i + 1; j < keys.size(); ++j) {
        	kvmap.put(keys.get(j), j - 1);
        }
        
        this.keys.remove(i);
        this.values.remove(i);
        this.links.remove(i);
        this.setnames();
    }

    public int size() {
        if (this.keys != null) {
            return this.keys.size();
        }
        return 0;
    }

    public String toString() {
        return this.classname + (this.targetname == null ? "" : new StringBuilder().append(" (").append(this.targetname).append(")").toString());
    }
    
    public String toStringSpecial() {
    	StringBuilder sb = new StringBuilder();
		
		sb.append("entity\n{\n");
		for(int i = 0; i < keys.size(); ++i) {
			sb.append("\t\"" + keys.get(i) + "\" \"" + values.get(i) + "\"\n");
		}
		sb.append("}");

		return sb.toString();
    }

    public Entity copy() {
        Entity ret = new Entity();
        ret.index = this.index;
        ret.mark = this.mark;
        ret.autoedit = this.autoedit;
        for (int i = 0; i < this.size(); ++i) {
        	ret.kvmap.put(this.keys.get(i), ret.values.size());
            ret.keys.add(this.keys.get(i));
            ret.values.add(this.values.get(i));
        }
        ret.links = (ArrayList)this.links.clone();
        ret.setnames();
        return ret;
    }

    public int byteSize() {
        int length = 4;
        for (int i = 0; i < this.size(); ++i) {
            length+=this.keys.get(i).length() + 2;
            length+=this.values.get(i).length() + 2;
            length+=2;
        }
        return length;
    }
    
    public boolean isMatch(List<String> keys, List<String> values) {
    	if(values.size() < keys.size())
    		return false;
    	
    	for(int i = 0; i < keys.size(); ++i) {
    		if(!kvmap.containsKey(keys.get(i))) return false;
    		
    		int index = kvmap.get(keys.get(i));
    		if(index >= values.size() || index < 0 || values.get(index).toLowerCase().indexOf(values.get(i)) == -1) {
				return false;
			}
    	}
    	
    	return true;
    }

    public boolean isMatch(String text) {
    	String lower = text.toLowerCase();
    	
    	if(getKeyValue("classname").toLowerCase().indexOf(lower) != -1 || getKeyValue("targetname").toLowerCase().indexOf(lower) != -1)
    		return true;
    	
        return false;
    }
}

