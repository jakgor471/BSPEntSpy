package bspentspy;

import java.io.IOException;
import java.util.ArrayList;

import javax.swing.AbstractListModel;

import bspentspy.SourceBSPFile.Lightmap;

public class LightmapListModel extends AbstractListModel<Lightmap> {
	private static final long serialVersionUID = 1L;
	ArrayList<Lightmap> lightmaps;
	ArrayList<Lightmap> filtered;
	float[] origin;
	float radius;
	
	public LightmapListModel(SourceBSPFile bspmap) {
		filtered = new ArrayList<>();
		radius = -1;
		origin = new float[3];
		
		try {
			lightmaps = bspmap.getLightmaps();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		filter(0, 0, 0, -1);
	}
	
	public void filter(float x, float y, float z, float radius) {
		origin[0] = x;
		origin[1] = y;
		origin[2] = z;
		
		this.radius = radius;
		
		filtered.clear();
		
		for(Lightmap l : lightmaps) {
			float distX = l.origin[0] - x;
			float distY = l.origin[1] - y;
			float distZ = l.origin[2] - z;
			
			float dist = distX * distX + distY * distY + distZ * distZ;
			
			if(dist <= radius * radius || radius < 0)
				filtered.add(l);
		}
		
		this.fireContentsChanged(this, 0, getSize());
	}

	@Override
	public int getSize() {
		return filtered.size();
	}

	@Override
	public Lightmap getElementAt(int index) {
		return filtered.get(index);
	}
	
	public void reload() {
		this.fireContentsChanged(this, 0, getSize());
	}

}
