package bspentspy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import bspentspy.Entity.OutputData;

public class Obfuscator {
	private HashMap<String, String> nameMap;
	private FGD fgdData;
	private Random rand;
	
	public Obfuscator() {
		nameMap = new HashMap<String, String>();
		rand = new Random();
	}
	
	public void setFGD(FGD fgdData) {
		this.fgdData = fgdData;
	}
	
	public void obfuscate(ArrayList<Entity> allEnts, ArrayList<Entity> obfuscatees) {
		nameMap.clear();
		ArrayList<OutputData> outputsToAdd = new ArrayList<OutputData>();
		
		//mangle names of selected entities
		for(Entity e : obfuscatees) {
			String trimmed = e.targetname.trim();
			int len = trimmed.length();
			
			if(len < 1) {
				continue;
			}
			
			if(nameMap.containsKey(trimmed)) {
				e.setKeyVal("targetname", nameMap.get(trimmed));
			} else {
				rand.setSeed(trimmed.hashCode());
				char[] newname = new char[len];
				for(int i = 0; i < len; ++i) {
					newname[i] = allowedChars.charAt(rand.nextInt(allowedChars.length()));
				}
				
				String sname = new String(newname);
				nameMap.put(trimmed, sname);
				e.setName(sname + "_");

				OutputData output = new OutputData();
				//set name to "<MANGLED_NAME>_"
				output.inputName = "AddOutput";
				output.targetEnt = sname + "_";
				output.param = "targetname " + sname;
				output.delay = 0;
				output.onlyOnce = true;
				outputsToAdd.add(output);
			}
		}
		
		//replace original names with mangled ones
		for(Entity e : allEnts) {
			
		}
	}
	
	private static final String allowedChars = "abcdefghijklmnoprstuwxyzABCDEFGHIJKLMNOPRSTUWXYZ123456789_";
	private static HashSet<String> logicEntities = new HashSet<String>();
	static {
		logicEntities.add("logic_auto");
		logicEntities.add("logic_timer");
		logicEntities.add("logic_case");
		logicEntities.add("logic_relay");
		logicEntities.add("logic_compare");
		logicEntities.add("logic_multicompare");
	}
}
