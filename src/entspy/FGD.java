package entspy;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

public class FGD {
	public ArrayList<String> loadedFgds;
	public ArrayList<FGDEntry> entities;
	public HashMap<String, Integer> entMap;
	
	public FGD() {
		loadedFgds = new ArrayList<String>();
		entities = new ArrayList<FGDEntry>();
		entMap = new HashMap<String, Integer>();
	}
	
	public void loadFromStream(InputStreamReader in, String fgdName) throws Exception {
		BufferedReader br = new BufferedReader(in);
		
		
		loadedFgds.add(fgdName);
	}
}
