package entspy;

import java.util.ArrayList;
import java.util.HashMap;

public class FGDEntry {
	public String classname;
	public String description;
	public ArrayList<FGDEntry> baseclasses;
	public ClassType classtype;
	public ArrayList<Property> properties;
	public HashMap<String, Integer> propmap;
	public ArrayList<InputOutput> inputs;
	public ArrayList<InputOutput> outputs;
	
	public FGDEntry(String classname) {
		this.classname = classname;
		description = "";
		baseclasses = null;
		classtype = ClassType.BaseClass;
		properties = new ArrayList<Property>();
		propmap = new HashMap<String, Integer>();
		inputs = new ArrayList<InputOutput>();
		outputs = new ArrayList<InputOutput>();
	}
	
	public FGDEntry() {
		this("");
	}
	
	public void setClass(String str) {
		classtype = typeMap.get(str);
		
		if(classtype == null)
			classtype = ClassType.PointClass;
	}
	
	public void addProperty(Property prop) {
		propmap.put(prop.name, properties.size());
		properties.add(prop);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("@").append(classtype).append(" ").append(classname);
		
		return sb.toString();
	}
	
	public static boolean isValidClass(String str) {
		return typeMap.containsKey(str);
	}
	
	public static boolean isValidDataType(String str) {
		return dataTypeMap.containsKey(str);
	}
	
	private static HashMap<String, ClassType> typeMap;
	private static HashMap<String, DataType> dataTypeMap;
	
	static {
		typeMap = new HashMap<String, ClassType>();
		typeMap.put("BaseClass", ClassType.BaseClass);
		typeMap.put("PointClass", ClassType.PointClass);
		typeMap.put("NPCClass", ClassType.NPCClass);
		typeMap.put("SolidClass", ClassType.SolidClass);
		typeMap.put("KeyFrameClass", ClassType.KeyFrameClass);
		typeMap.put("MoveClass", ClassType.MoveClass);
		typeMap.put("FilterClass", ClassType.FilterClass);
		typeMap.put("ExtendClass", ClassType.ExtendClass);
		
		dataTypeMap = new HashMap<String, DataType>();
		dataTypeMap.put("integer", DataType.integer);
		dataTypeMap.put("string", DataType.string);
		dataTypeMap.put("float", DataType.floating);
		dataTypeMap.put("boolean", DataType.bool);
		dataTypeMap.put("bool", DataType.bool);
		dataTypeMap.put("flags", DataType.flags);
		dataTypeMap.put("choices", DataType.choices);
		dataTypeMap.put("void", DataType.voidT);
		dataTypeMap.put("target_destination", DataType.targetDestination);
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
		voidT,
		targetDestination
	}
	
	//used for int, string, float
	public static class Property{
		public String name;
		public String displayName;
		public String description;
		public String defaultVal;
		public DataType type;
		public boolean readOnly = false;
		
		public void setDataType(String str) {
			type = dataTypeMap.get(str);
			
			if(type == null)
				type = DataType.voidT;
		}
		
		public boolean hasChoices() {
			return false;
		}
	}
		
	public static class PropChoicePair{
		public String value;
		public String name;
		public boolean flagTicked;
	}
	
	//used for boolean, flags, choices
	public static class PropertyChoices extends Property{
		public ArrayList<PropChoicePair> choices;
		
		public PropertyChoices() {
			choices = new ArrayList<PropChoicePair>();
		}
		
		public void addChoice(String value, String name) {
			PropChoicePair pair = new PropChoicePair();
			pair.value = value;
			pair.name = name;
			choices.add(pair);
		}
		
		public boolean hasChoices() {
			return choices != null;
		}
		
		public Object getChoices() {
			return choices;
		}
	}
	
	public static class InputOutput{
		public String name;
		public String description;
		public DataType type;
		
		public void setDataType(String str) {
			type = dataTypeMap.get(str);
			
			if(type == null)
				type = DataType.voidT;
		}
	}
}
