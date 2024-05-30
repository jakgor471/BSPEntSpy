package entspy;

import java.util.ArrayList;
import java.util.HashMap;

public class FGDEntry {
	public String classname;
	public String description;
	public FGDEntry baseclass;
	public ClassType classtype;
	public ArrayList<KVEntry> keyvalues;
	public HashMap<String, KVEntry> kvmap;
	public ArrayList<KVEntry> inputs;
	
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
	
	public static enum KVType{
		integer,
		string,
		floating,
		bool,
		flags,
		choices,
		empty //void
	}
	
	public static abstract class KVEntry{
		public String key;
		public String description;
		public KVType type;
		
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
}
