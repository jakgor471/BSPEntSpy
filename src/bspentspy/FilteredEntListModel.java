package bspentspy;

import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.event.ListDataListener;

@SuppressWarnings("serial")
public class FilteredEntListModel extends AbstractListModel<Entity> {
	public List<Entity> entities;
	
	public int getSize() {
		if(entities == null)
			return 0;
		return entities.size();
	}
	
	public void setEntityList(List<Entity> ents) {
		entities = ents;
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

	@Override
	public void addListDataListener(ListDataListener l) {
	}

	@Override
	public void removeListDataListener(ListDataListener l) {
	}

}
