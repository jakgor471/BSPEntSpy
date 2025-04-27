package bspentspy;

import java.util.ArrayList;
import java.util.Comparator;

import javax.swing.table.AbstractTableModel;

import bspentspy.ClassPropertyPanel.KVEntry;
import bspentspy.FGDEntry.PropChoicePair;
import bspentspy.FGDEntry.PropertyChoices;

@SuppressWarnings("serial")
public class FlagTableModel extends AbstractTableModel {
	private FGDEntry fgdData;
	private ArrayList<PropChoicePair> flags;
	private int[] bits;
	private int intValue;
	private int orgIntValue;
	private boolean canEdit;

	public void setFGD(FGDEntry fgd) {
		fgdData = fgd;

		if (fgd == null) {
			flags = null;
			return;
		}
		PropertyChoices choices = (PropertyChoices) fgd.propmap.get("spawnflags");
		if (choices == null)
			return;

		flags = (ArrayList<PropChoicePair>) choices.choices.clone();
		flags.sort(new Comparator<PropChoicePair>() {
			public int compare(PropChoicePair o1, PropChoicePair o2) {
				return Long.compareUnsigned(o1.intValue, o2.intValue);
			}
		});

		bits = new int[flags.size()];

		int i = 0;
		for (PropChoicePair fl : flags) {
			bits[i++] = fl.intValue;
		}
	}

	public boolean isChanged() {
		return intValue != orgIntValue;
	}

	public String getValue() {
		return Integer.toUnsignedString(intValue);
	}

	public void setFlags(KVEntry flagKV) {
		intValue = 0;
		orgIntValue = 0;
		canEdit = false;
		if (flagKV == null) {
			this.fireTableDataChanged();
			return;
		}
		try {
			intValue = (int) Long.valueOf(flagKV.getValue()).intValue();
			orgIntValue = intValue;
		} catch (IllegalArgumentException e) {

		}

		canEdit = true;
		this.fireTableDataChanged();
	}

	public String getColumnName(int col) {
		final String[] header = new String[] { "Set", "Flag" };
		return header[col];
	}

	public int getRowCount() {
		if (flags == null)
			return 0;

		return flags.size();
	}

	public int getColumnCount() {
		return 2;
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		if (columnIndex == 0)
			return (intValue & bits[rowIndex]) > 0;
		return flags.get(rowIndex).description;
	}

	public void setValueAt(Object val, int row, int col) {
		if (col != 0)
			return;
		int mask = bits[row];
		intValue &= ~mask;

		if (((Boolean) val).booleanValue())
			intValue |= mask;
	}

	public Class getColumnClass(int col) {
		if (col == 0)
			return Boolean.class;
		return String.class;
	}

	public boolean isCellEditable(int row, int col) {
		return col == 0 && canEdit;
	}

}
