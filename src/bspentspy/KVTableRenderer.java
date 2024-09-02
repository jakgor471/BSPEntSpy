package bspentspy;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import bspentspy.ClassPropertyPanel.KVEntry;

public class KVTableRenderer implements TableCellRenderer {
	public static final Color LIGHT_BLUE = new Color(239, 239, 255);

	private DefaultTableCellRenderer def;
	private DefaultTableCellRenderer autoAdded;

	boolean shouldDifferentiate;

	public KVTableRenderer(boolean shouldDifferentiate) {
		def = new DefaultTableCellRenderer();
		autoAdded = new DefaultTableCellRenderer();

		this.shouldDifferentiate = shouldDifferentiate;

		def.setBackground(LIGHT_BLUE);
	}

	public void shouldDifferentiateColors(boolean should) {
		shouldDifferentiate = should;
	}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		KVEntry entry = ((KeyValTableModel) table.getModel()).getKVEntryAt(row, column);
		if (!shouldDifferentiate || entry != null && entry.autoAdded) {
			return autoAdded.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		}

		return def.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	}

}
