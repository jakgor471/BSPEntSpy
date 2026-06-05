package bspentspy;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;

public class EntityStaticProp extends Entity {
	public static final int STATIC_PROP_NO_FLASHLIGHT = 0x04; //disableflashlight
	public static final int STATIC_PROP_IGNORE_NORMALS = 0x08; //ignorenormals
	public static final int STATIC_PROP_NO_SHADOW = 0x10; //disableshadows
	public static final int STATIC_PROP_SCREEN_SPACE_FADE = 0x20; //screenspacefade
	public static final int STATIC_PROP_MARKED_FOR_FAST_REFLECTION = 0x20; //drawinfastreflection
	public static final int STATIC_PROP_NO_PER_VERTEX_LIGHTING = 0x40; //disablevertexlighting
	public static final int STATIC_PROP_NO_SELF_SHADOWING = 0x80; //disableselfshadowing
	public static final int STATIC_PROP_NO_PER_TEXEL_LIGHTING = 0x100; //generatelightmaps
	public static final int STATIC_PROP_ENABLE_LIGHT_BOUNCE = 0x200; //enablelightbounce
	
	private static ArrayList<SimpleEntry<String, Integer>> flagTable = new ArrayList<>(9);
	static {
		flagTable.add( new SimpleEntry<String, Integer>("disableflashlight", STATIC_PROP_NO_FLASHLIGHT) );
		flagTable.add( new SimpleEntry<String, Integer>("ignorenormals", STATIC_PROP_IGNORE_NORMALS) );
		flagTable.add( new SimpleEntry<String, Integer>("disableshadows", STATIC_PROP_NO_SHADOW) );
		flagTable.add( new SimpleEntry<String, Integer>("screenspacefade", STATIC_PROP_SCREEN_SPACE_FADE) );
		flagTable.add( new SimpleEntry<String, Integer>("drawinfastreflection", STATIC_PROP_MARKED_FOR_FAST_REFLECTION) );
		flagTable.add( new SimpleEntry<String, Integer>("disablevertexlighting", STATIC_PROP_NO_PER_VERTEX_LIGHTING) );
		flagTable.add( new SimpleEntry<String, Integer>("disableselfshadowing", STATIC_PROP_NO_SELF_SHADOWING) );
		flagTable.add( new SimpleEntry<String, Integer>("generatelightmaps", STATIC_PROP_NO_PER_TEXEL_LIGHTING) );
		flagTable.add( new SimpleEntry<String, Integer>("enablelightbounce", STATIC_PROP_ENABLE_LIGHT_BOUNCE) );
	}
	
	short[] leaves;
	int flags;
	
	public EntityStaticProp() {
		setKeyVal("classname", "prop_static");
	}
	
	public void delKeyVal(String k) {
		return;
	}

	public void delKeyValById(int uniqueId) {
		return;
	}

	public void delKeyVal(int i) {
		return;
	}
	
	public void changeKey(String from, String to) {
		return;
	}

	public void changeKey(Integer uniqueId, String to) {
		return;
	}
	
	public boolean canBeRemoved() {
		return false;
	}
	
	public boolean shouldSave() {
		return false;
	}
	
	public void setFlags(int flags) {
		for(SimpleEntry<String, Integer> entry : flagTable) {
			String has = (flags & entry.getValue().intValue()) > 0 ? "1" : "0";
			this.setKeyVal( entry.getKey(), has );
		}
		
		this.flags = flags;
	}
	
	public int getFlags() {
		for(SimpleEntry<String, Integer> entry : flagTable) {
			boolean set = false;
			try {
				int bit = Integer.parseInt( this.getKeyValue( entry.getKey() ) );
				set = bit > 0;
			} catch(NumberFormatException e) {
				e.printStackTrace();
			}
			
			if(set) {
				flags = flags | entry.getValue();
			} else {
				flags = flags & (~entry.getValue());
			}
		}
		
		return flags;
	}
}
