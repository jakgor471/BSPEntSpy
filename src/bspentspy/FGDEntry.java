package bspentspy;

import java.util.ArrayList;
import java.util.HashMap;

public class FGDEntry {
	public String classname;
	public String description;
	public ArrayList<FGDEntry> baseclasses;
	public ClassType classtype;
	public ArrayList<Property> properties;
	public HashMap<String, Property> propmap;
	public ArrayList<InputOutput> inputs;
	public ArrayList<InputOutput> outputs;
	public HashMap<String, InputOutput> outmap;
	public int fgdDefinedIndex; // index of fgd file in which this class was declared
	public int fgdDefinedLine; // line on which this class was defined

	public FGDEntry(String classname) {
		this.classname = classname;
		description = "";
		baseclasses = null;
		classtype = ClassType.BaseClass;
		properties = new ArrayList<Property>();
		propmap = new HashMap<String, Property>();
		outmap = new HashMap<String, InputOutput>();
		inputs = new ArrayList<InputOutput>();
		outputs = new ArrayList<InputOutput>();
		fgdDefinedIndex = -1;
		fgdDefinedIndex = -1;
	}

	public FGDEntry() {
		this("");
	}

	public void setClass(String str) {
		classtype = typeMap.get(str);

		if (classtype == null)
			classtype = ClassType.PointClass;
	}

	public void addProperty(Property prop, boolean appendChoices) {
		String propname = prop.name.toLowerCase();

		if (!propmap.containsKey(propname)) {
			propmap.put(propname, prop);
			properties.add(prop);
			return;
		}

		Property existing = propmap.get(propname);

		if (appendChoices && existing instanceof PropertyChoices && prop instanceof PropertyChoices) {
			PropertyChoices propChoices = (PropertyChoices) prop;
			PropertyChoices propChoicesEx = (PropertyChoices) existing;

			for (PropChoicePair ch : propChoices.choices) {
				propChoicesEx.addChoice(ch);
			}
		}
	}

	public void addProperty(Property prop) {
		addProperty(prop, false);
	}

	public void addOutput(InputOutput io) {
		outputs.add(io);
		outmap.put(io.name, io);
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
		dataTypeMap.put("origin", DataType.origin);
		dataTypeMap.put("angle", DataType.angle);
	}

	public static enum ClassType {
		BaseClass, PointClass, NPCClass, SolidClass, KeyFrameClass, MoveClass, FilterClass, ExtendClass
	}

	public static enum DataType {
		integer("Integer"), string("String"), floating("Float"), bool("Boolean"), flags("Flags"), choices("Choices"),
		voidT("void"), targetDestination("Target entity"), origin("Vector"), angle("Angle");

		public String name;

		DataType(String name) {
			this.name = name;
		}
	}

	// used for int, string, float
	public static class Property {
		public String name;
		public String displayName;
		public String description;
		public String defaultVal;
		public DataType type;
		public boolean readOnly = false;

		public void setDataType(String str) {
			type = dataTypeMap.get(str);

			if (type == null)
				type = DataType.voidT;
		}

		public String getDisplayName() {
			if (displayName != null)
				return displayName;
			return name;
		}

		public boolean hasChoices() {
			return false;
		}

		public Object copy() {
			Property newProp = new Property();

			newProp.name = name;
			newProp.displayName = displayName;
			newProp.description = description;
			newProp.defaultVal = defaultVal;
			newProp.type = type;
			newProp.readOnly = readOnly;

			return newProp;
		}
	}

	public static class PropChoicePair {
		public String value;
		public String description;
		public int intValue = 0;
		public boolean flagTicked = false;
	}

	// used for boolean, flags, choices
	public static class PropertyChoices extends Property {
		public ArrayList<PropChoicePair> choices;
		public HashMap<String, PropChoicePair> chMap;

		public PropertyChoices() {
			choices = new ArrayList<PropChoicePair>();
			chMap = new HashMap<String, PropChoicePair>();
		}

		public void addChoice(String value, String name) {
			PropChoicePair pair = new PropChoicePair();
			pair.value = value;
			pair.description = name;
			choices.add(pair);
		}

		public void addChoice(PropChoicePair ch) {
			String chval = ch.value.toLowerCase().trim();

			if (!chMap.containsKey(chval)) {
				choices.add(ch);
				chMap.put(chval, ch);
			}
		}

		public boolean hasChoices() {
			return choices != null;
		}

		public Object copy() {
			PropertyChoices newProp = new PropertyChoices();

			newProp.name = name;
			newProp.displayName = displayName;
			newProp.description = description;
			newProp.defaultVal = defaultVal;
			newProp.type = type;
			newProp.readOnly = readOnly;

			for (PropChoicePair ch : choices) {
				newProp.choices.add(ch);
				newProp.chMap.put(ch.value.toLowerCase().trim(), ch);
			}

			return newProp;
		}
	}

	public static class InputOutput {
		public String name;
		public String description;
		public DataType type;

		public void setDataType(String str) {
			type = dataTypeMap.get(str);

			if (type == null)
				type = DataType.voidT;
		}
	}
}
