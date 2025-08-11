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
	String material;
	
	public LightmapListModel(SourceBSPFile bspmap) {
		filtered = new ArrayList<>();
		radius = -1;
		origin = new float[3];
		
		try {
			lightmaps = bspmap.getLightmaps();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		filter(false, 0, 0, 0, -1, null);
	}
	
	public void filter(boolean posFilter, float x, float y, float z, float radius, String material) {
		filtered.clear();
		
		if(posFilter) {
			origin[0] = x;
			origin[1] = y;
			origin[2] = z;
			
			this.radius = radius;
		} else {
			this.material = material;
			
			if(this.material != null)
				this.material = this.material.toLowerCase();
		}
		
		if(this.radius < 0) {
			for(Lightmap l : lightmaps) {
				filtered.add(l);
			}
		} else {			
			for(Lightmap l : lightmaps) {
				float distX = l.origin[0] - origin[0];
				float distY = l.origin[1] - origin[1];
				float distZ = l.origin[2] - origin[2];
				
				float dist = distX * distX + distY * distY + distZ * distZ;
				
				if(dist <= this.radius * this.radius || this.radius < 0)
					filtered.add(l);
			}
		}
		
		if(this.material != null) {
			for(int i = filtered.size() - 1; i >= 0; --i) {
				if(!filtered.get(i).underlyingMaterial.toLowerCase().contains(this.material))
					filtered.remove(i);
			}
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
