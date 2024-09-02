package bspentspy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bspentspy.Undo.Command;

public class Entity {
	int index;
	float[] origin = new float[] {0, 0, 0};
	boolean mark = false;
	boolean autoedit = false;
	String classname;
	String targetname;
	private HashMap<String, Integer> duplicates = new HashMap<String, Integer>();
	private HashMap<Integer, Integer> uniqueKvmap = new HashMap<Integer, Integer>();
	private HashMap<String, Integer> kvmap = new HashMap<String, Integer>();
	public ArrayList<KeyValue> keyvalues = new ArrayList<KeyValue>();

	private int uniqueInt = 0;

	public Entity() {
	}

	public Entity(String s) {
		this.classname = s;
	}
	
	public long keyValHash() {
		long hash = 76646989558427L;
		for(KeyValue kvl : keyvalues) {
			if(kvl.key.equals("targetname"))
				continue;
			int keyhash = kvl.key.hashCode();
			int valhash = kvl.value.hashCode();
			hash ^= keyhash + 0x9e3779b9 + (hash << 6) + (hash >> 2);
			hash ^= valhash + 0x9e3779b9 + (hash << 6) + (hash >> 2);
		}
		
		return hash;
	}
	
	public long entHash() {
		long hash = keyValHash();
		hash ^= getKeyValue("targetname").hashCode() + 0x9e3779b9 + (hash << 6) + (hash >> 2);
		
		return hash;
	}
	
	public int getUniqueInt() {
		return uniqueInt;
	}

	public void clear() {
		this.classname = null;
		this.targetname = null;
		this.origin = null;
		keyvalues.clear();
		kvmap.clear();
		uniqueKvmap.clear();
	}
	
	public void setClass(String classname) {
		setKeyVal("classname", classname);
	}
	
	public void setName(String targetname) {
		setKeyVal("targetname", targetname);
	}
	
	public void setOrigin(double x, double y, double z) {
		setKeyVal("origin", String.format("%4f", x) + " " + String.format("%4f", y) + " " + String.format("%4f", z));
		origin[0] = (float)x;
		origin[1] = (float)y;
		origin[2] = (float)z;
	}

	public String getKeyValue(String keyword) {
		Integer index = kvmap.get(keyword);
		if (index != null) {
			return keyvalues.get(index).value;
		}
		return "";
	}

	public void setnames() {
		this.classname = this.getKeyValue("classname");
		this.targetname = this.getKeyValue("targetname");
		
		String[] split = this.getKeyValue("origin").trim().split("\\s+");
		
		origin[0] = origin[1] = origin[2] = 0;
		
		if(split.length < 3)
			return;
		
		try {
			origin[0] = Float.valueOf(split[0]);
			origin[1] = Float.valueOf(split[1]);
			origin[2] = Float.valueOf(split[2]);
		} catch(NumberFormatException e) {
			origin[0] = origin[1] = origin[2] = 0;
			setKeyVal("origin", "0 0 0");
		}
	}

	public String getKeyValString(int i) {
		if (i < 0 || i >= this.keyvalues.size()) {
			return null;
		}
		return '\"' + this.keyvalues.get(i).key + "\" \"" + this.keyvalues.get(i).value + '\"';
	}

	public void setKeyVal(String k, String v) {
		if (!kvmap.containsKey(k)) {
			addKeyVal(k, v);
			return;
		}

		CommandSetVal command = new CommandSetVal();
		if (duplicates.getOrDefault(k, 0) > 0) {
			for (KeyValue kvl : keyvalues) {
				if (kvl.key.equals(k)) {
					command.add(kvl, kvl.value, v);
					kvl.value = v;
				}
			}
		} else {
			KeyValue toChange = keyvalues.get(kvmap.get(k));
			command.add(toChange, toChange.value, v);
			toChange.value = v;
		}

		Undo.addCommand(command);
		setnames();
	}

	public int setKeyVal(Integer uniqueId, String key, String v) {
		if (uniqueId != null && uniqueKvmap.containsKey(uniqueId)) {
			KeyValue kvl = keyvalues.get(uniqueKvmap.get(uniqueId));

			CommandSetVal command = new CommandSetVal(kvl, kvl.value, v);
			Undo.addCommand(command);

			kvl.value = v;

			changeKey(uniqueId, key);
			setnames();
			return uniqueId;
		}

		return addKeyVal(key, v);
	}

	public int addKeyVal(String k, String v) {
		KeyValue kv = new KeyValue();
		kv.key = k;
		kv.value = v;

		int hash1 = (k + " " + v).hashCode();
		uniqueInt ^= hash1 + 0x9e3779b9 + (uniqueInt << 6) + (uniqueInt >>> 2);

		kv.uniqueId = uniqueInt;

		insertKeyVal(keyvalues.size(), kv);
		setnames();

		return uniqueInt;
	}

	public void insertKeyVal(int index, KeyValue kvl) {
		if (index < 0 || index > keyvalues.size())
			return;
		keyvalues.add(index, kvl);

		CommandInsertKV command = new CommandInsertKV(kvl, index);
		Undo.addCommand(command);

		if (duplicates.containsKey(kvl.key))
			duplicates.put(kvl.key, duplicates.get(kvl.key) + 1);

		for (int i = index; i < size(); ++i) {
			kvmap.put(keyvalues.get(i).key, i);
			uniqueKvmap.put(keyvalues.get(i).uniqueId, i);
		}

		this.setnames();
	}

	public void changeKey(String from, String to) {
		if (!kvmap.containsKey(from)) {
			addKeyVal(to, "");
			return;
		}

		int index = kvmap.get(from);

		if (duplicates.getOrDefault(from, 0) > 0) {
			CommandChangeKey command = new CommandChangeKey(from, to);
			duplicates.put(to, duplicates.get(from));
			duplicates.remove(from);

			for (KeyValue kvl : keyvalues) {
				if (kvl.key.equals(from)) {
					kvl.key = to;
					command.add(kvl);
				}
			}
			Undo.addCommand(command);
			setnames();
		} else {
			changeKey(keyvalues.get(index).uniqueId, to);
		}
	}

	public void changeKey(Integer uniqueId, String to) {
		if (uniqueId == null || !uniqueKvmap.containsKey(uniqueId))
			return;

		int index = uniqueKvmap.get(uniqueId);
		KeyValue toChange = keyvalues.get(index);

		if (toChange.key.equals(to))
			return;

		CommandChangeKey command = new CommandChangeKey(toChange, toChange.key, to);
		Undo.addCommand(command);

		duplicates.put(toChange.key, uniqueKvmap.getOrDefault(toChange.key, 1) - 1);
		duplicates.put(to, uniqueKvmap.getOrDefault(to, -1) + 1);

		kvmap.remove(keyvalues.get(index).key);
		keyvalues.get(index).key = to;
		kvmap.put(to, index);

		setnames();
	}

	public void rehash() {
		kvmap.clear();
		uniqueKvmap.clear();

		for (int i = 0; i < size(); ++i) {
			KeyValue kvl = keyvalues.get(i);
			kvmap.put(kvl.key, i);
			uniqueKvmap.put(kvl.uniqueId, i);
		}
	}

	public void delKeyVal(String k) {
		if (!kvmap.containsKey(k))
			return;

		if (duplicates.getOrDefault(k, 0) > 0) {
			duplicates.remove(k);
			CommandDeleteKV command = new CommandDeleteKV();

			for (int i = 0; i < size(); ++i) {
				if (keyvalues.get(i).key.equals(k)) {
					command.add(keyvalues.get(i), i);
					keyvalues.remove(i--);
				}
			}

			Undo.addCommand(command);

			rehash();
			setnames();
		} else {
			delKeyVal(kvmap.get(k));
		}
	}

	public void delKeyValById(int uniqueId) {
		if (!uniqueKvmap.containsKey(uniqueId))
			return;
		delKeyVal(uniqueKvmap.get(uniqueId));
	}

	public void delKeyVal(int i) {
		if (i < 0 || i >= this.size()) {
			return;
		}

		KeyValue toRemove = keyvalues.get(i);

		CommandDeleteKV command = new CommandDeleteKV(toRemove, i);
		Undo.addCommand(command);

		kvmap.remove(toRemove.key);
		uniqueKvmap.remove(toRemove.uniqueId);
		duplicates.put(toRemove.key, duplicates.getOrDefault(toRemove.key, 1) - 1);

		keyvalues.remove(i);
		for (int j = i; j < keyvalues.size(); ++j) {
			kvmap.put(keyvalues.get(j).key, j);
			uniqueKvmap.put(keyvalues.get(j).uniqueId, j);
		}

		setnames();
	}
	
	public boolean hasKeyValue(String name) {
		return kvmap.containsKey(name);
	}

	public int size() {
		if (this.keyvalues != null) {
			return this.keyvalues.size();
		}
		return 0;
	}

	public String toString() {
		return this.classname + (this.targetname == null ? ""
				: new StringBuilder().append(" (").append(this.targetname).append(")").toString());
	}

	public String toStringSpecial() {
		StringBuilder sb = new StringBuilder();

		sb.append("entity\n{\n");
		for (int i = 0; i < keyvalues.size(); ++i) {
			sb.append("\t\"" + keyvalues.get(i).key + "\" \"" + keyvalues.get(i).value + "\"\n");
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
			ret.addKeyVal(keyvalues.get(i).key, keyvalues.get(i).value);
		}
		ret.setnames();
		return ret;
	}

	public int byteSize() {
		int length = 4;
		for (int i = 0; i < this.size(); ++i) {
			length += this.keyvalues.get(i).key.length() + 2;
			length += this.keyvalues.get(i).value.length() + 2;
			length += 2;
		}
		return length;
	}

	public boolean isMatch(List<String> keys, List<String> values) {
		if (values.size() < keys.size())
			return false;

		for (int i = 0; i < keys.size(); ++i) {
			if (!kvmap.containsKey(keys.get(i)))
				return false;

			int index = kvmap.get(keys.get(i));
			if (index >= values.size() || index < 0
					|| keyvalues.get(index).value.toLowerCase().indexOf(values.get(i)) == -1) {
				return false;
			}
		}

		return true;
	}

	public boolean isMatch(String text) {
		String lower = text.toLowerCase();

		if (getKeyValue("classname").toLowerCase().indexOf(lower) != -1
				|| getKeyValue("targetname").toLowerCase().indexOf(lower) != -1)
			return true;

		return false;
	}

	public static abstract class CommandKV implements Command {
		ArrayList<KeyValue> keyvalues;

		public CommandKV() {
			keyvalues = new ArrayList<KeyValue>();
		}

		public CommandKV(KeyValue kvl) {
			this();
			keyvalues.add(kvl);
		}

		public Command join(Command previous) {
			CommandKV prev = (CommandKV) previous;
			prev.keyvalues.addAll(keyvalues);

			return null;
		}

		public int size() {
			return keyvalues.size();
		}
	}

	public static class CommandInsertKV extends CommandKV {
		ArrayList<Integer> indices;

		public CommandInsertKV() {
			super();
			indices = new ArrayList<Integer>();
		}

		public CommandInsertKV(KeyValue kvl, int index) {
			super(kvl);
			indices = new ArrayList<Integer>();
			indices.add(index);
		}

		public void add(KeyValue kvl, int index) {
			keyvalues.add(kvl);
			indices.add(index);
		}

		public Command join(Command previous) {
			CommandInsertKV prev = (CommandInsertKV) previous;
			prev.keyvalues.addAll(keyvalues);
			prev.indices.addAll(indices);

			return null;
		}

		public void undo(Object target) {
			ListIterator<KeyValue> it = keyvalues.listIterator(keyvalues.size());

			while (it.hasPrevious()) {
				((Entity) target).delKeyValById(it.previous().uniqueId);
			}
		}

		public void redo(Object target) {
			ListIterator<KeyValue> it = keyvalues.listIterator();

			while (it.hasNext()) {
				((Entity) target).insertKeyVal(indices.get(it.nextIndex()), it.next());
			}
		}

		public String toString(String indent) {
			StringBuilder sb = new StringBuilder();
			System.out.println("ident" + indent);

			sb.append(indent).append(this.getClass()).append("\n");

			for (int i = 0; i < keyvalues.size(); ++i) {
				sb.append(indent).append("\t\t").append(keyvalues.get(i).toString()).append(" at index ")
						.append(indices.get(i)).append("\n");
			}

			return sb.toString();
		}
	}

	public static class CommandDeleteKV extends CommandInsertKV {
		public CommandDeleteKV(KeyValue toRemove, int i) {
			super(toRemove, i);
		}

		public CommandDeleteKV() {
			super();
		}

		public void undo(Object target) {
			ListIterator<KeyValue> it = keyvalues.listIterator(keyvalues.size());

			while (it.hasPrevious()) {
				((Entity) target).insertKeyVal(indices.get(it.previousIndex()), it.previous());
			}
		}

		public void redo(Object target) {
			ListIterator<KeyValue> it = keyvalues.listIterator();

			while (it.hasNext()) {
				((Entity) target).delKeyValById(it.previous().uniqueId);
			}
		}
	}

	public static class CommandSetVal extends CommandKV {
		ArrayList<String> oldVal;
		ArrayList<String> newVal;

		public CommandSetVal() {
			super();
			oldVal = new ArrayList<String>();
			newVal = new ArrayList<String>();
		}

		public CommandSetVal(KeyValue kvl, String oldVal, String newVal) {
			super(kvl);
			this.oldVal = new ArrayList<String>();
			this.newVal = new ArrayList<String>();
			this.oldVal.add(oldVal);
			this.newVal.add(newVal);
		}

		public void add(KeyValue kvl, String oldVal, String newVal) {
			keyvalues.add(kvl);
			this.oldVal.add(oldVal);
			this.newVal.add(newVal);
		}

		public Command join(Command previous) {
			CommandSetVal prev = (CommandSetVal) previous;
			prev.keyvalues.addAll(keyvalues);
			prev.oldVal.addAll(oldVal);
			prev.newVal.addAll(newVal);

			return null;
		}

		public void undo(Object target) {
			ListIterator<KeyValue> it = keyvalues.listIterator(keyvalues.size());

			while (it.hasPrevious()) {
				int index = it.previousIndex();
				KeyValue kvl = it.previous();

				((Entity) target).setKeyVal(kvl.uniqueId, kvl.key, oldVal.get(index));
			}
		}

		public void redo(Object target) {
			ListIterator<KeyValue> it = keyvalues.listIterator();

			while (it.hasNext()) {
				int index = it.nextIndex();
				KeyValue kvl = it.next();

				((Entity) target).setKeyVal(kvl.uniqueId, kvl.key, newVal.get(index));
			}
		}

		public String toString(String indent) {
			StringBuilder sb = new StringBuilder();

			sb.append(indent).append(this.getClass()).append("\n");

			for (int i = 0; i < keyvalues.size(); ++i) {
				sb.append(indent).append("\t\t").append(keyvalues.get(i).toString()).append(", oldVal: \"")
						.append(oldVal.get(i)).append("\", newVal: \"").append(newVal.get(i)).append("\"\n");
			}

			return sb.toString();
		}
	}

	public static class CommandChangeKey extends CommandKV {
		String oldKey;
		String newKey;

		public CommandChangeKey() {
			super();
		}

		public CommandChangeKey(KeyValue kvl, String oldKey, String newKey) {
			super(kvl);
			this.oldKey = oldKey;
			this.newKey = newKey;
		}

		public CommandChangeKey(String oldKey, String newKey) {
			super();
			this.oldKey = oldKey;
			this.newKey = newKey;
		}

		public void add(KeyValue kvl) {
			keyvalues.add(kvl);
		}

		public Command join(Command previous) {
			CommandChangeKey prev = (CommandChangeKey) previous;

			if (prev.oldKey.equals(oldKey) && prev.newKey.equals(newKey)) {
				prev.keyvalues.addAll(keyvalues);
				return null;
			}

			return this;
		}

		public void undo(Object target) {
			ListIterator<KeyValue> it = keyvalues.listIterator(keyvalues.size());

			while (it.hasPrevious()) {
				int index = it.previousIndex();
				KeyValue kvl = it.previous();

				((Entity) target).changeKey(kvl.uniqueId, oldKey);
			}
		}

		public void redo(Object target) {
			ListIterator<KeyValue> it = keyvalues.listIterator();

			while (it.hasNext()) {
				int index = it.nextIndex();
				KeyValue kvl = it.next();

				((Entity) target).changeKey(kvl.uniqueId, newKey);
			}
		}

		public String toString(String indent) {
			StringBuilder sb = new StringBuilder();

			sb.append(indent).append(this.getClass()).append(", change key from: \"").append(oldKey).append("\" to \"")
					.append(newKey).append("\"\n");

			for (int i = 0; i < keyvalues.size(); ++i) {
				sb.append(indent).append("\t\t").append(keyvalues.get(i).toString()).append("\n");
			}

			return sb.toString();
		}
	}

	public static class KeyValue {
		public int uniqueId;
		public String key;
		public String value;
		
		public String toString() {
			return "(\"" + key + "\" \"" + value + "\")";
		}
		
		public String getTarget() {
			return getTarget(value);
		}
		
		public static String getTarget(String value) {
			//returns first argument of Output or just value
			int index1 = value.indexOf(',');
			int index2 = value.indexOf('\033');
			int index = (index1 >= 0 && index1 < index2 || index2 <= 0) ? index1 : index2;
			index = index >= 0 ? index : value.length();
			
			return value.substring(0, index).trim();
		}
	}
	
	public static class OutputData{
		public String outputName;
		public String targetEnt;
		public String inputName;
		public String param;
		public float delay;
		public boolean onlyOnce;
		
		public OutputData() {
			
		}
		
		public String getValue() {
			return getValue(targetEnt, inputName, param, delay, onlyOnce);
		}
		
		public static String getValue(String targetEnt, String inputName, String param, float delay, boolean onlyOnce) {
			return targetEnt + "," + inputName + "," + param + "," + String.format("%2f", delay) + "," + (onlyOnce ? "1" : "-1");
		}
		
		public static OutputData parseOutput(KeyValue kvl) {
			Matcher match = regex.matcher(kvl.value);
			
			if(!match.matches())
				return null;
			OutputData output = new OutputData();
			
			output.outputName = kvl.key;
			output.targetEnt = match.group(1);
			output.inputName = match.group(2);
			output.param = match.group(3);
			output.delay = Float.valueOf(match.group(4));
			output.onlyOnce = match.group(4).equals("1");
			
			return output;
		}
		
		private static Pattern regex = Pattern.compile("(.*?),(.*?),(.*?),(.*?),(.*)");
	}
}
