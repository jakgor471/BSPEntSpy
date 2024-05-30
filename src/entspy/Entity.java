package entspy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Entity {
    int index;
    boolean mark = false;
    boolean autoedit = false;
    String classname;
    String targetname;
    String parentname;
    String origin;
    public HashMap<String, Integer> kvmap = new HashMap();
    public ArrayList<String> key = new ArrayList();
    public ArrayList<String> value = new ArrayList();
    public ArrayList<Entity> link = new ArrayList();
    static final String[] definedkey = new String[]{"classname", "targetname", "parentname", "origin"};

    public Entity() {
    }

    public Entity(String s) {
        this.classname = s;
    }

    public void clear() {
        this.classname = null;
        this.targetname = null;
        this.parentname = null;
        this.origin = null;
        if (this.key != null) {
            this.key.clear();
        }
        if (this.value != null) {
            this.value.clear();
        }
        if (this.link != null) {
            this.link.clear();
        }
    }

    public int getkeyindex(String keyword) {
    	Integer k = kvmap.get(keyword);
    	
    	if(k != null)
    		return k.intValue();

        return -1;
    }

    public String getkeyvalue(String keyword) {
        int ik = this.getkeyindex(keyword);
        if (ik >= 0) {
            return this.value.get(ik);
        }
        return "";
    }

    public void setnames() {
        this.classname = this.getkeyvalue("classname");
        this.targetname = this.getkeyvalue("targetname");
        this.parentname = this.getkeyvalue("parentname");
        this.origin = this.getkeyvalue("origin");
    }

    public String getkeyvalstring(int i) {
        if (i < 0 || i >= this.key.size()) {
            return null;
        }
        char quote = '\"';
        return "" + quote + this.key.get(i) + quote + " " + quote + this.value.get(i) + quote;
    }

    public void addkeyval(String k, String v) {
    	kvmap.put(k, value.size());
    	
        this.key.add(k);
        this.value.add(v);
        this.link.add(null);
        this.setnames();
    }

    public void delkeyval(int i) {
        if (i < 0 || i >= this.size()) {
            return;
        }
        
        if(kvmap.containsKey(key.get(i)))
        	kvmap.remove(key.get(i));
        
        this.key.remove(i);
        this.value.remove(i);
        this.link.remove(i);
        this.setnames();
    }

    public int size() {
        if (this.key != null) {
            return this.key.size();
        }
        return 0;
    }

    public String toString() {
        return this.classname + (this.targetname == null ? "" : new StringBuilder().append(" (").append(this.targetname).append(")").toString());
    }

    public Entity copy() {
        Entity ret = new Entity();
        ret.index = this.index;
        ret.mark = this.mark;
        ret.autoedit = this.autoedit;
        for (int i = 0; i < this.size(); ++i) {
        	ret.kvmap.put(this.key.get(i), ret.value.size());
            ret.key.add(this.key.get(i));
            ret.value.add(this.value.get(i));
        }
        ret.link = (ArrayList)this.link.clone();
        ret.setnames();
        return ret;
    }

    public int bytesize() {
        int length = 4;
        for (int i = 0; i < this.size(); ++i) {
            length+=this.key.get(i).length() + 2;
            length+=this.value.get(i).length() + 2;
            length+=2;
        }
        return length;
    }

    public void setDefinedValue(int dk, String val) {
        int ki = this.getkeyindex(definedkey[dk]);
        if (ki < 0) {
            if (!val.equals("")) {
                this.addkeyval(definedkey[dk], val);
            }
        } else if (!val.equals("")) {
            this.value.set(ki, val);
        } else {
            this.delkeyval(ki);
        }
        this.setnames();
    }
    
    public boolean ismatch(List<String> keys, List<String> values) {
    	if(values.size() < keys.size())
    		return false;
    	
    	for(int i = 0; i < keys.size(); ++i) {
    		if(!kvmap.containsKey(keys.get(i))) return false;
    		
    		int index = kvmap.get(keys.get(i));
    		if(index >= value.size() || index < 0 || value.get(index).toLowerCase().indexOf(values.get(i)) == -1) {
				return false;
			}
    	}
    	
    	return true;
    }

    public boolean ismatch(String text) {
    	String lower = text.toLowerCase();
    	
    	if(getkeyvalue("classname").toLowerCase().indexOf(lower) != -1 || getkeyvalue("targetname").toLowerCase().indexOf(lower) != -1)
    		return true;
    	
    	/*for(String key : key) {
    		if(kvmap.containsKey(key)) {
    			int index = kvmap.get(key);
    			
    			if(index >= value.size() || index < 0)
    				continue;
    			
    			if(value.get(index).toLowerCase().indexOf(lower) > -1)
    				return true;
    		}
    	}*/
    	
        return false;
    }
}

