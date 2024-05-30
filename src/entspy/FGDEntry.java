package entspy;

import java.util.ArrayList;
import java.util.HashMap;

public class FGDEntry {
	public String classname;
	public String description;
	public FGDEntry baseclass;
	public ClassType classtype;
	public ArrayList<KVEntry> keyvalues;
	public HashMap<String, Integer> kvmap;
	public ArrayList<InputOutput> inputs;
	public ArrayList<InputOutput> outputs;
	
	public FGDEntry(String classname) {
		this.classname = classname;
		description = "";
		baseclass = null;
		classtype = ClassType.BaseClass;
		keyvalues = new ArrayList<KVEntry>();
		kvmap = new HashMap<String, Integer>();
		inputs = new ArrayList<InputOutput>();
		outputs = new ArrayList<InputOutput>();
	}
	
	public void addKeyValue(KVEntry kv) {
		kvmap.put(kv.key, keyvalues.size());
		keyvalues.add(kv);
	}
	
	public static enum ClassType{
		BaseClass,
		PointClass,
		NPCClass,
		SolidClass,
		KeyFrameClass,
		MoveClass,
		FilterClass,
		ExtendClass
	}
	
	public static enum DataType{
		integer,
		string,
		floating,
		bool,
		flags,
		choices,
		voidT
	}
	
	public static abstract class KVEntry{
		public String key;
		public String description;
		public String defaultVal;
		public DataType type;
		
		public abstract Object getValue();
	}
	
	//used for int, string, float, bool
	public static abstract class KVEntryGeneric extends KVEntry{
		private String value;
		public Object getValue() {
			return value;
		}
	}
	
	public static class KVChoicePair{
		public String value;
		public String name;
	}
	
	//used for boolean, flags, choices
	public static abstract class KVEntryChoices extends KVEntry{
		private ArrayList<KVChoicePair> choices;
		
		public KVEntryChoices() {
			choices = new ArrayList<KVChoicePair>();
		}
		
		public void addChoice(String value, String name) {
			KVChoicePair pair = new KVChoicePair();
			pair.value = value;
			pair.name = name;
			choices.add(pair);
		}
		
		public Object getValue() {
			return choices;
		}
	}
	
	public static class InputOutput{
		public String name;
		public String description;
		public DataType type;
	}
}
