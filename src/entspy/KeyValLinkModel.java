package entspy;

import entspy.ClassPropertyPanel.KVEntry;
import entspy.Entspy.EntspyListModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.table.AbstractTableModel;

class KeyValLinkModel extends AbstractTableModel {
	ArrayList<KVEntry> keyvalues;
	JList list;

	KeyValLinkModel() {
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
		if(col == 0)
			return keyvalues.get(row).key;
		
		return keyvalues.get(row).getValue();
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
