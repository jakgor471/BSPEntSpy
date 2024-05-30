package bspentspy;

import java.util.ArrayList;

import javax.swing.JList;
import javax.swing.table.AbstractTableModel;

import bspentspy.ClassPropertyPanel.KVEntry;
import bspentspy.FGDEntry.PropChoicePair;
import bspentspy.FGDEntry.Property;
import bspentspy.FGDEntry.PropertyChoices;

class KeyValLinkModel extends AbstractTableModel {
	ArrayList<KVEntry> keyvalues;
	JList list;
	FGDEntry fgdContent;

	KeyValLinkModel() {
		fgdContent = null;
	}

	public void setMapping(JList t) {
		this.list = t;
	}

	public int getRowCount() {
		if(keyvalues == null)
			return 0;
		
		return keyvalues.size();
	}

	public int getColumnCount() {
		return 2;
	}

	public Object getValueAt(int row, int col) {
		KVEntry keyval = keyvalues.get(row);
		String key = keyval.key.toLowerCase();
		Property prop = null;
		if(fgdContent != null)
			prop = fgdContent.propmap.get(key);
		
		if(col == 0) {
			if(prop != null)
				return prop.getDisplayName();
			
			return keyval.key;
		}
		
		if(!keyval.different && prop != null && prop instanceof PropertyChoices) {
			String val = keyval.value.toLowerCase().trim();
			PropertyChoices propChoices = (PropertyChoices)prop;
			PropChoicePair ch = propChoices.chMap.get(val);
			if(ch != null)
				return ch.description;
		}
		
		return keyval.getValue();
	}

	public void setValueAt(Object setval, int row, int col) {
		if (row < 0 || row >= keyvalues.size())
			return;
		
		if (col == 0) {
			keyvalues.get(row).key = (String) setval;
		} else
			keyvalues.get(row).value = (String) setval;
	}

	public Class getColumnClass(int col) {
		return String.class;
	}

	public String getColumnName(int col) {
		String[] header = new String[] { "Property", "Value" };
		return header[col];
	}

	public void clear() {
		this.fireTableDataChanged();
	}

	public boolean isCellEditable(int row, int col) {
		return false;
	}

	public void set(ArrayList<KVEntry> keyvalues) {
		this.keyvalues = keyvalues;
		this.fireTableDataChanged();
	}
	

	public void refreshtable() {
		this.fireTableDataChanged();
	}

}
