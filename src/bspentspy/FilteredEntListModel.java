package bspentspy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractListModel;

@SuppressWarnings("serial")
public class FilteredEntListModel extends AbstractListModel<Entity> {
	private IFilter filter;
	private List<Entity> entities;
	private List<Entity> original;
	private ArrayList<Integer> originalIndices = new ArrayList<Integer>();
	private int[] indexMap = new int[0];
	
	public List<Entity> getFilteredEntities(){
		return entities;
	}
	
	public void setFilter(IFilter filter) {
		IFilter prev = this.filter;
		this.filter = filter;
		
		if(filter != prev)
			filter();
	}
	
	public int getSize() {
		if(entities == null)
			return 0;
		return entities.size();
	}
	
	public void setEntityList(List<Entity> ents) {
		original = ents;
		filter();
		this.fireContentsChanged(this, 0, ents.size());
	}
	
	public void reload() {
		this.fireContentsChanged(this, 0, getSize());
	}
	
	public void reload(int start, int finish) {
		this.fireContentsChanged(this, start, finish);
	}

	@Override
	public Entity getElementAt(int index) {
		return entities.get(index);
	}
	
	//filtered index -> original index
	public int getIndexAt(int index) {
		if(index >= originalIndices.size() || index < 0)
			return -1;
		return originalIndices.get(index);
	}
	
	//original index -> filtered index, inverse of getIndexAt
	public int indexOf(int index) {
		if(index >= indexMap.length || index < 0)
			return -1;
		return indexMap[index];
	}
	
	public int[] translateIndices(int[] indices) {
		int[] newIndices = new int[indices.length];
		
		for(int i = 0; i < newIndices.length; ++i) {
			newIndices[i] = originalIndices.get(indices[i]);
		}
		
		return newIndices;
	}
	
	public int[] detranslateIndices(int[] indices) {
		int[] newIndices = new int[indices.length];
		
		int j = 0;
		for(int i = 0; i < newIndices.length; ++i) {
			newIndices[i] = indexMap[indices[i]];
			if(newIndices[i] > -1)
				++j;
		}
		
		return Arrays.copyOf(newIndices, j);
	}
	
	private void filter() {
		ArrayList<Entity> filtered = new ArrayList<Entity>();
		originalIndices.clear();
		
		indexMap = new int[original.size()];
		
		for(int i = 0; i < original.size(); ++i) {
			indexMap[i] = -1;
			if(filter == null || filter.match(original.get(i))) {
				originalIndices.add(i);
				indexMap[i] = filtered.size();
				filtered.add(original.get(i));
			}
		}
		
		entities = filtered;
		this.fireContentsChanged(this, 0, getSize());
	}
}
