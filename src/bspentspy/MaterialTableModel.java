package bspentspy;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

@SuppressWarnings("serial")
public class MaterialTableModel extends AbstractTableModel{
	private ArrayList<String> materials;
	
	public MaterialTableModel(ArrayList<String> materials) {
		super();
		this.materials = materials;
	}
	
	public void setMaterials(ArrayList<String> materials) {
		this.materials = materials;
	}
	
	public int getRowCount() {
		return materials.size();
	}

	@Override
	public int getColumnCount() {
		return 2;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if(columnIndex == 0)
			return rowIndex;
		return materials.get(rowIndex);
	}
	
	public void setValueAt(Object o, int rowIndex, int columnIndex) {
		if(columnIndex <= 0)
			return;
		
		materials.set(rowIndex, o.toString());
	}
	
	public String getColumnName(int col) {
		final String[] header = new String[] { "Index", "Material name" };
		return header[col];
	}
	
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return columnIndex > 0;
	}

}
