package entspy;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableColumn;

@SuppressWarnings("serial")
public class ClassPropertyPanel extends JPanel {
	private JTextField classTextField;
	private JTextField originTextField;
	private JTable table;
	private JButton addkv;
	private JButton cpykv;
	private JButton delkv;
	private KeyValLinkModel kvModel;
	private KVEntry classname;
	private KVEntry origin;
	private ArrayList<Entity> editingEntities;
	private ArrayList<KVEntry> keyvalues;
	private HashMap<String, KVEntry> kvMap;
	private TextListen classnameListener;
	private TextListen originListener;
	
	public ClassPropertyPanel() {
		editingEntities = new ArrayList<Entity>();
		keyvalues = new ArrayList<KVEntry>();
		kvMap = new HashMap<String, KVEntry>();
		
		BorderLayout panelBLayout = new BorderLayout();
		panelBLayout.setVgap(5);
		
		this.setLayout(panelBLayout);
		this.setBorder(BorderFactory.createEtchedBorder());
		
		JPanel grid = new JPanel();
		GridLayout gridLayout = new GridLayout(2, 2);
		gridLayout.setHgap(10);
		gridLayout.setVgap(5);
		grid.setLayout(gridLayout);
		
		classTextField = new JTextField(" ");
		classTextField.setEnabled(false);
		originTextField = new JTextField(" ");
		originTextField.setEnabled(false);
		
		classnameListener = new TextListen("classname", classTextField);
		originListener = new TextListen("origin", originTextField);
		
		grid.add(new JLabel("Class", 4));
		grid.add(classTextField);
		grid.add(new JLabel("Origin", 4));
		grid.add(originTextField);
		
		this.add(grid, "North");
		
		JPanel keyvalPanel = new JPanel();
		
		kvModel = new KeyValLinkModel();
		this.table = new JTable(kvModel);
		this.table.setSelectionMode(0);
		this.table.getTableHeader().setReorderingAllowed(false);
		
		TableColumn keycol = this.table.getColumn("Value");
		keycol.setPreferredWidth(175);
		
		keyvalPanel.setLayout(new GridLayout(1, 1));
		keyvalPanel.add(new JScrollPane(this.table));
		
		this.add(keyvalPanel, "Center");
		
		JPanel bottomLeftPanel = new JPanel();
		addkv = new JButton("Add");
		addkv.setToolTipText("Add an entity property");
		bottomLeftPanel.add(addkv);
		addkv.setEnabled(false);
		cpykv = new JButton("Copy");
		cpykv.setToolTipText("Copy the selected property");
		bottomLeftPanel.add(cpykv);
		cpykv.setEnabled(false);
		delkv = new JButton("Delete");
		delkv.setToolTipText("Delete the selected property");
		bottomLeftPanel.add(delkv);
		delkv.setEnabled(false);
		
		GridLayout bottomGridLayout = new GridLayout(1,2);
		bottomGridLayout.setVgap(10);
		JPanel bottom = new JPanel(bottomGridLayout);
		
		JPanel bottomRightPanel = new JPanel();
		
		JButton apply = new JButton("Apply");
		apply.setEnabled(true);
		apply.setToolTipText("Apply to all selected entities.");
		apply.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae) {
				ClassPropertyPanel.this.apply();
			}
		});
		
		bottomRightPanel.add(apply);
		
		bottom.add(bottomLeftPanel);
		bottom.add(bottomRightPanel);
		this.add(bottom, "South");
		
		/*addkv.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ae) {
				Entity selEnt = Entspy.this.getSelectedEntity();
				selEnt.addKeyVal("", "");
				//Entspy.this.settable(selEnt, kvOldModel);
				int lastrow = selEnt.size() - 1;
				ClassPropertyPanel.this.table.changeSelection(lastrow, 0, false, false);
				ClassPropertyPanel.this.table.editCellAt(lastrow, 0);
				ClassPropertyPanel.this.table.scrollRectToVisible(ClassPropertyPanel.this.table.getCellRect(lastrow, 0, true));
				ClassPropertyPanel.this.table.getEditorComponent().requestFocus();
			}
		});
		cpykv.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ae) {
				Entity selEnt = ClassPropertyPanel.this.getSelectedEntity();
				int selrow = ClassPropertyPanel.this.table.getSelectedRow();
				if (selrow == -1) {
					return;
				}
				selEnt.addKeyVal(selEnt.keys.get(selrow), selEnt.values.get(selrow));
				selEnt.setnames();
				ClassPropertyPanel.this.settable(selEnt, kvModel);
				kvModel.reselect();
				int lastrow = selEnt.size() - 1;
				ClassPropertyPanel.this.table.changeSelection(lastrow, 1, false, false);
				ClassPropertyPanel.this.table.editCellAt(lastrow, 1);
				ClassPropertyPanel.this.table.scrollRectToVisible(ClassPropertyPanel.this.table.getCellRect(lastrow, 1, true));
				ClassPropertyPanel.this.table.getEditorComponent().requestFocus();
			}
		});
		delkv.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ae) {
				Entity selEnt = ClassPropertyPanel.this.getSelectedEntity();
				int selrow = ClassPropertyPanel.this.table.getSelectedRow();
				if (selrow == -1) {
					return;
				}
				selEnt.delKeyVal(selrow);
				selEnt.setnames();
				
				if(kvModel.getRowCount() > 0) {
					selrow = Math.min(selrow, kvModel.getRowCount() - 1);
					table.setRowSelectionInterval(selrow, selrow);
				}
				
			}
		});*/
	}
	
	public void updateClassAndOrigin() {
		
	}
	
	public void setEntity(Entity e) {
		editingEntities.clear();
		editingEntities.add(e);
		
		gatherKeyValues(false);
	}
	
	public void addEntity(Entity e) {
		if(editingEntities.size() < 1) {
			setEntity(e);
			return;
		}
		
		editingEntities.add(e);
		gatherKeyValues(true);
	}
	
	public void gatherKeyValues(boolean multipleEnts) {
		keyvalues.clear();
		kvMap.clear();
		
		if(multipleEnts) {
			for(Entity e : editingEntities) {
				for(int i = 0; i < e.size(); ++i) {
					KVEntry entry = kvMap.get(e.keys.get(i));
					
					if(entry == null) {
						entry = addKeyValue(e.keys.get(i), e.values.get(i));
					}
					
					if(!entry.different) {
						entry.different = !entry.value.equals(e.values.get(i));
					}
				}
			}
		} else if(editingEntities.size() > 0) {
			Entity entity = editingEntities.get(0);
			for(int i = 0; i < entity.size(); ++i) {
				addKeyValue(entity.keys.get(i), entity.values.get(i));
			}
		}
		
		kvModel.set(keyvalues);
		
		if(origin != null) {
			originTextField.getDocument().removeDocumentListener(originListener);
			originTextField.setText(origin.getValue());
			originTextField.setEnabled(true);
			originTextField.getDocument().addDocumentListener(originListener);
		} else {
			originTextField.getDocument().removeDocumentListener(originListener);
			originTextField.setText("");
			originTextField.setEnabled(false);
		}
		
		if(classname != null) {
			classTextField.getDocument().removeDocumentListener(classnameListener);
			classTextField.setText(classname.getValue());
			
			if(!classname.different) {
				classTextField.getDocument().addDocumentListener(classnameListener);
				classTextField.setEnabled(true);
			} else
				classTextField.setEnabled(false);
		} else {
			classTextField.getDocument().removeDocumentListener(classnameListener);
			classTextField.setText("");
			classTextField.setEnabled(false);
		}
	}
	
	public void clearEntities(){
		editingEntities.clear();
		
		classname = null;
		origin = null;
		gatherKeyValues(false);
	}
	
	public void apply() {
		ArrayList<KVEntry> edited = new ArrayList<KVEntry>();
		
		for(KVEntry e : keyvalues) {
			if(e.edited)
				edited.add(e);
		}
		
		for(Entity e : editingEntities) {
			for(KVEntry entry : edited) {
				e.setKeyVal(entry.key, entry.value);
			}
			
			if(classname != null && !classname.different)
				e.setKeyVal("classname", classname.value);
			if(origin != null && origin.edited)
				e.setKeyVal("origin", origin.value);
		}
	}
	
	private KVEntry addKeyValue(String key, String value) {
		KVEntry entry = new KVEntry();
		entry.key = key;
		entry.value = value;
		
		if(key.equals("classname")) {
			classname = entry;
		} else if(key.equals("origin")) {
			origin = entry;
		} else
			keyvalues.add(entry);
		
		kvMap.put(key, entry);
		
		return entry;
	}
	
	class TextListen implements DocumentListener {
		String type;
		JTextField textField;

		public TextListen(String type, JTextField field) {
			this.type = type;
			textField = field;
		}

		@Override
		public void insertUpdate(DocumentEvent e) {
			changed(e);
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			changed(e);
		}

		public void changed(DocumentEvent e) {
			String text = textField.getText().trim();
			
			if(!ClassPropertyPanel.this.kvMap.containsKey(type)) return;
			
			ClassPropertyPanel.this.kvMap.get(type).value = text;
			ClassPropertyPanel.this.kvMap.get(type).edited = true;
		}

		public void changedUpdate(DocumentEvent e) {
		}
	}
	
	protected static class KVEntry{
		public String key = "";
		public String value = "";

		public boolean different = false;
		public boolean edited = false;
		
		public String getValue() {
			if(different) return "(different)";
			
			return value;
		}
	}
}
