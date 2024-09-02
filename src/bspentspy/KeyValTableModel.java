package bspentspy;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

import bspentspy.ClassPropertyPanel.KVEntry;
import bspentspy.FGDEntry.DataType;
import bspentspy.FGDEntry.PropChoicePair;
import bspentspy.FGDEntry.Property;
import bspentspy.FGDEntry.PropertyChoices;

class KeyValTableModel extends AbstractTableModel {
	ArrayList<KVEntry> keyvalues;
	FGDEntry fgdData;

	KeyValTableModel() {
		fgdData = null;
	}

	public int getRowCount() {
		if (keyvalues == null)
			return 0;

		return keyvalues.size();
	}

	public int getColumnCount() {
		return 2;
	}

	public Object getValueAt(int row, int col) {
		if (row < 0 || row >= keyvalues.size())
			return null;

		KVEntry keyval = keyvalues.get(row);
		String key = keyval.key.toLowerCase();
		Property prop = null;
		if (fgdData != null)
			prop = fgdData.propmap.get(key);

		if (col == 0) {
			if (prop != null)
				return prop.getDisplayName();

			return keyval.key;
		}

		if (!keyval.different && prop != null && prop instanceof PropertyChoices) {
			String val = keyval.value.toLowerCase().trim();
			PropertyChoices propChoices = (PropertyChoices) prop;

			if (prop.type == DataType.flags) {
				StringBuilder sb = new StringBuilder();
				int bits = 0;
				try {
					bits = Integer.valueOf(val);
				} catch (NumberFormatException e) {
				}

				for (PropChoicePair ch : propChoices.choices) {
					if ((bits & ch.intValue) != 0)
						sb.append(ch.description).append("; ");
				}

				return sb.toString();
			}
			PropChoicePair ch = propChoices.chMap.get(val);
			if (ch != null)
				return ch.description;
		}

		return keyval.getValue();
	}

	public Class getColumnClass(int col) {
		return String.class;
	}

	public String getColumnName(int col) {
		final String[] header = new String[] { "Property", "Value" };
		return header[col];
	}

	public void clear() {
		this.fireTableDataChanged();
	}

	public KVEntry getKVEntryAt(int row, int col) {
		if (row < 0 || row >= keyvalues.size())
			return null;

		return keyvalues.get(row);
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
