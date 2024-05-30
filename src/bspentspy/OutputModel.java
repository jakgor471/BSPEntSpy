package bspentspy;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

import bspentspy.ClassPropertyPanel.KVEntry;

@SuppressWarnings("serial")
public class OutputModel extends AbstractTableModel {
	ArrayList<KVEntry> outputs;
	ArrayList<IOSplit> split;
	
	public OutputModel() {
		split = new ArrayList<IOSplit>();
	}
	
	public int getRowCount() {
		if(outputs == null)
			return 0;
		
		return outputs.size();
	}

	@Override
	public int getColumnCount() {
		return 6;
	}
	
	public Class getColumnClass(int col) {
		return String.class;
	}
	
	public String getColumnName(int col) {
		final String[] header = new String[] { "Output name", "Target entity", "Target input", "Parameter", "Delay", "Only once" };
		return header[col];
	}
	
	public boolean isCellEditable(int row, int col) {
		return true;
	}

	@Override
	public Object getValueAt(int row, int col) {
		if(row < 0 || row >= outputs.size())
			return null;
		
		if(col == 0)
			return outputs.get(row).key;
		
		String[] split = outputs.get(row).value.split("\\s*\\,\\s*");
		
		if(col <= split.length)
			return split[col - 1];
		
		return null;
	}
	
	public void setValueAt(Object o, int row, int col) {
		if(row < 0 || row >= outputs.size())
			return;
		
		switch(col) {
		case 0:
			outputs.get(row).key = (String)o;
			break;
		case 1:
			split.get(row).target = (String)o;
			break;
		case 2:
			split.get(row).input = (String)o;
			break;
		case 3:
			split.get(row).param = (String)o;
			break;
		case 4:
			split.get(row).delay = (String)o;
			break;
		case 5:
			split.get(row).once = (String)o;
			break;
		}
		
		outputs.get(row).value = split.get(row).toString();
	}
	
	public void fireTableDataChanged() {
		super.fireTableDataChanged();
		
		split.clear();
		
		if(outputs != null) {
			for(KVEntry io : outputs) {
				String[] spl = io.value.split("\\s*\\,\\s*");
				
				if(spl.length < 5)
					continue;
				
				split.add(new IOSplit(spl[0], spl[1], spl[2], spl[3], spl[4]));
			}
		}
	}
	
	public void set(ArrayList<KVEntry> outputs) {
		this.outputs = outputs;
		this.fireTableDataChanged();
	}
	
	private class IOSplit{
		public String target;
		public String input;
		public String param;
		public String delay;
		public String once;
		
		public IOSplit(String t, String i, String p, String d, String o) {
			target = t;
			input = i;
			param = p;
			delay = d;
			once = o;
		}
		
		public String toString() {
			return target + "," + input + "," + param + "," + delay + "," + once;
		}
	}
}
