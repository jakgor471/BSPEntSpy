package bspentspy;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.BevelBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;

import bspentspy.Entity.KeyValLink;
import bspentspy.FGDEntry.Property;

@SuppressWarnings("serial")
public class ClassPropertyPanel extends JPanel {
	private JTextField classTextField;
	private JTextField originTextField;
	private JTextField keyTextField;
	private JTextField valueTextField;
	private JLabel valueLabel;
	private KeyValueListener classnameListener;
	private KeyValueListener originListener;
	private KeyValueListener keyListener;
	private KeyValueListener valueListener;
	private KeyValModel kvModel;
	private KVEntry classname;
	private KVEntry origin;
	private JTable kvtable;
	private KVTableRenderer kvrenderer;
	private JButton addkv;
	private JButton cpykv;
	private JButton delkv;
	private JButton help;
	private JButton apply;
	
	private OutputModel outputModel;
	private JTable outputTable;
	private JButton addio;
	private JButton cpyio;
	private JButton delio;
	
	private ArrayList<Entity> editingEntities;
	private ArrayList<KVEntry> outputs;
	private ArrayList<KVEntry> keyvalues;
	private ArrayList<KVEntry> deletedKv;
	private ArrayList<KVEntry> deletedOutputs;
	private HashMap<String, KVEntry> kvMap;
	private ActionListener onApply;
	
	private boolean smartEdit;
	private boolean addDefaultParameters;
	
	public FGD fgdContent;
	
	public ClassPropertyPanel() {
		super(new BorderLayout());
		
		editingEntities = new ArrayList<Entity>();
		outputs = new ArrayList<KVEntry>();
		keyvalues = new ArrayList<KVEntry>();
		deletedKv = new ArrayList<KVEntry>();
		deletedOutputs = new ArrayList<KVEntry>();
		kvMap = new HashMap<String, KVEntry>();
		
		fgdContent = null;
		smartEdit = false;
		addDefaultParameters = false;
		
		/*MAIN PARAMETERS PANEL*/
		BorderLayout panelBLayout = new BorderLayout();
		panelBLayout.setVgap(5);
		
		JPanel paramsPanel = new JPanel(panelBLayout);
		paramsPanel.setBorder(BorderFactory.createEtchedBorder());
		
		JPanel grid = new JPanel(new GridLayout(4, 2));
		((GridLayout)grid.getLayout()).setVgap(5);
		((GridLayout)grid.getLayout()).setHgap(10);
		
		classTextField = new JTextField(" ");
		classTextField.setEnabled(false);
		originTextField = new JTextField(" ");
		originTextField.setEnabled(false);
		
		classnameListener = new KeyValueListener("classname", classTextField);
		originListener = new KeyValueListener("origin", originTextField);
		
		classTextField.getDocument().addDocumentListener(classnameListener);
		originTextField.getDocument().addDocumentListener(originListener);
		
		grid.add(new JLabel("Class:", 4));
		grid.add(classTextField);
		grid.add(new JLabel("Origin:", 4));
		grid.add(originTextField);
		
		JPanel keyvalPanel = new JPanel(new BorderLayout());
		
		kvModel = new KeyValModel();
		kvtable = new JTable(kvModel);
		kvtable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		kvtable.getTableHeader().setReorderingAllowed(false);
		
		kvrenderer = new KVTableRenderer(addDefaultParameters);
		kvtable.getColumnModel().getColumn(0).setCellRenderer(kvrenderer);
		kvtable.getColumnModel().getColumn(1).setCellRenderer(kvrenderer);
		
		TableColumn keycol = kvtable.getColumn("Value");
		keycol.setPreferredWidth(175);
		
		ActionListener kvListener = new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				applyKVChanges();
				int selected = kvtable.getSelectedRow();
				kvModel.refreshtable();
				kvtable.getSelectionModel().setSelectionInterval(selected, selected);
			}
		};
		
		keyTextField = new JTextField(" ");
		keyTextField.setEnabled(false);
		keyTextField.addActionListener(kvListener);
		valueTextField = new JTextField(" ");
		valueTextField.setEnabled(false);
		valueTextField.addActionListener(kvListener);
		
		keyListener = new KeyValueListener(null, keyTextField, true);
		valueListener = new KeyValueListener(null, valueTextField, false);
		
		keyTextField.getDocument().addDocumentListener(keyListener);
		valueTextField.getDocument().addDocumentListener(valueListener);
		
		kvtable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				int selected = kvtable.getSelectedRow();
				boolean enable = !(selected < 0 || selected >= keyvalues.size());
				cpykv.setEnabled(enable);
				delkv.setEnabled(enable);
				
				if(enable && keyvalues.get(selected) != keyListener.kv)
					applyKVChanges();
			}
		});
		
		valueLabel = new JLabel("Value:", 4);
		
		grid.add(new JLabel("Key:", 4));
		grid.add(keyTextField);
		grid.add(valueLabel);
		grid.add(valueTextField);
		
		paramsPanel.add(grid, "North");
		
		keyvalPanel.add(new JScrollPane(this.kvtable), "Center");
		
		paramsPanel.add(keyvalPanel, "Center");
		
		FlowLayout bottomFlowLayout = new FlowLayout(FlowLayout.LEADING);
		bottomFlowLayout.setVgap(10);
		JPanel bottomLeftPanel = new JPanel(bottomFlowLayout);
		addkv = new JButton("Add");
		addkv.setToolTipText("Add an entity property");
		bottomLeftPanel.add(addkv);
		addkv.setEnabled(true);
		cpykv = new JButton("Copy");
		cpykv.setToolTipText("Copy the selected property");
		bottomLeftPanel.add(cpykv);
		cpykv.setEnabled(false);
		delkv = new JButton("Delete");
		delkv.setToolTipText("Delete the selected property");
		bottomLeftPanel.add(delkv);
		delkv.setEnabled(false);
		
		FlowLayout bottomRightFlow = new FlowLayout(FlowLayout.TRAILING);
		bottomRightFlow.setVgap(10);
		JPanel bottomRightPanel = new JPanel(bottomRightFlow);
		
		apply = new JButton("Apply");
		apply.setEnabled(false);
		apply.setToolTipText("Apply to all selected entities.");
		apply.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae) {
				apply();
				
				if(onApply != null)
					onApply.actionPerformed(new ActionEvent(this, 1, "apply"));
			}
		});
		
		help = new JButton("Help");
		help.setEnabled(false);
		help.setToolTipText("Display help about the entity");
		help.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {			
				HelpWindow help = HelpWindow.openHelp("Entity help");
				help.setText(getHelpText());
				help.setSize(720, 520);
				help.setVisible(true);
			}
		});
		
		bottomRightPanel.add(apply);
		bottomRightPanel.add(help);
		
		paramsPanel.add(bottomLeftPanel, "South");
		
		addkv.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				KVEntry newkv = new KVEntry();
				newkv.key = "";
				newkv.value = "";
				keyvalues.add(newkv);
				
				int lastrow = keyvalues.size() - 1;
				kvModel.refreshtable();
				kvtable.changeSelection(lastrow, 0, false, false);
				kvtable.scrollRectToVisible(kvtable.getCellRect(lastrow, 0, true));
			}
		});
		cpykv.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ae) {
				int selected = kvtable.getSelectedRow();
				
				if(selected < 0 || selected >= keyvalues.size())
					return;
				
				KVEntry entry = keyvalues.get(selected);
				KVEntry newkv = new KVEntry();
				
				newkv.key = entry.key;
				
				while(kvMap.containsKey(newkv.key) && smartEdit)
					fixDuplicateKey(newkv);
				
				newkv.value = entry.value;
				newkv.different = false;
				newkv.renamed = true;
				newkv.edited = true;
				
				kvMap.put(newkv.key, newkv);
				keyvalues.add(++selected, newkv);
				kvModel.refreshtable();
				kvtable.changeSelection(selected, 0, false, false);
				kvtable.scrollRectToVisible(kvtable.getCellRect(selected, 0, true));
				
			}
		});
		delkv.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				int selected = kvtable.getSelectedRow();
				
				if(selected < 0 || selected >= keyvalues.size())
					return;
				
				KVEntry entry = keyvalues.get(selected);
				kvMap.remove(entry.key);
				keyvalues.remove(selected);
				
				//if uniqueId is null it means this kv exists only in editor, not in actual entity
				if(entry.uniqueId != null)
					deletedKv.add(entry);
				
				kvModel.refreshtable();
				kvtable.clearSelection();
			}
		});
		
		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Parameters", paramsPanel);
		/*=== END OF MAIN PARAMETERS PANEL ===*/
		
		/*OUTPUTS PANEL*/
		outputModel = new OutputModel();
		outputTable = new JTable(outputModel);
		outputTable.getColumnModel().getColumn(0).setPreferredWidth(175);
		outputTable.getColumnModel().getColumn(1).setPreferredWidth(175);
		outputTable.getColumnModel().getColumn(2).setPreferredWidth(175);
		outputTable.getColumnModel().getColumn(3).setPreferredWidth(40);
		outputTable.getColumnModel().getColumn(4).setPreferredWidth(40);
		outputTable.getColumnModel().getColumn(5).setPreferredWidth(40);
		outputTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		outputTable.getTableHeader().setReorderingAllowed(false);
		
		outputTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				int selected = outputTable.getSelectedRow();
				
				boolean enable = !(selected < 0 || selected >= outputs.size());
				cpyio.setEnabled(enable);
				delio.setEnabled(enable);
				
				applyIOChanges();
			}
		});
		
		JPanel bottomLeftIOPanel = new JPanel(bottomFlowLayout);
		addio = new JButton("Add");
		addio.setToolTipText("Add an entity property");
		bottomLeftIOPanel.add(addio);
		addio.setEnabled(true);
		cpyio = new JButton("Copy");
		cpyio.setToolTipText("Copy the selected property");
		bottomLeftIOPanel.add(cpyio);
		cpyio.setEnabled(false);
		delio = new JButton("Delete");
		delio.setToolTipText("Delete the selected property");
		bottomLeftIOPanel.add(delio);
		delio.setEnabled(false);
		
		addio.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				KVEntry newIO = new KVEntry();
				newIO.key = "";
				newIO.value = "";
				outputs.add(newIO);
				
				int lastrow = keyvalues.size() - 1;
				outputModel.fireTableDataChanged();
				outputTable.changeSelection(lastrow, 0, false, false);
				outputTable.scrollRectToVisible(outputTable.getCellRect(lastrow, 0, true));
			}
		});
		cpyio.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				int selected = outputTable.getSelectedRow();
				
				if(selected < 0 || selected >= outputs.size())
					return;
				
				KVEntry entry = outputs.get(selected);
				KVEntry newIO = new KVEntry();
				newIO.key = entry.key;			
				newIO.value = entry.value;
				newIO.different = false;
				newIO.renamed = true;
				newIO.edited = true;
				
				outputs.add(++selected, newIO);
				
				outputModel.fireTableDataChanged();
				
				outputTable.changeSelection(selected, 0, false, false);
				outputTable.scrollRectToVisible(outputTable.getCellRect(selected, 0, true));
			}
		});
		delio.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				int selected = outputTable.getSelectedRow();
				
				if(selected < 0 || selected >= outputs.size())
					return;
				
				KVEntry entry = outputs.get(selected);
				outputs.remove(selected);
				
				if(entry.uniqueId != null)
					deletedOutputs.add(entry);
				
				outputModel.fireTableDataChanged();
				outputTable.clearSelection();
			}
		});
		
		JPanel outputPanel = new JPanel(new BorderLayout());
		outputPanel.add(new JScrollPane(outputTable), "Center");
		outputPanel.add(bottomLeftIOPanel, "South");
		tabs.addTab("Outputs", outputPanel);
		/*=== END OF OUTPUTS PANEL ===*/
		tabs.addTab("Flags", new JPanel());
		
		this.add(tabs, "Center");
		this.add(bottomRightPanel, "South");
	}
	
	public void applyKVChanges() {
		if(keyListener.keyChanged()) {
			//while(kvMap.containsKey(keyListener.kv.key) && smartEdit)
			//	fixDuplicateKey(keyListener.kv);
			kvMap.remove(keyListener.oldKey);
			kvMap.put(keyListener.kv.key, keyListener.kv);
		}
		
		int selectedIndex = kvtable.getSelectedRow();
		if(selectedIndex < 0 || selectedIndex >= keyvalues.size()) {
			keyListener.setKey(null);
			valueListener.setKey(null);
			keyTextField.setEnabled(false);
			keyTextField.setText("");
			valueTextField.setText("");
			valueTextField.setEnabled(false);
			valueLabel.setText("Value: ");
			
			return;
		}
		
		keyListener.setKey(null);
		valueListener.setKey(null);
		keyTextField.setEnabled(true);
		valueTextField.setEnabled(true);
		
		KVEntry entry = keyvalues.get(selectedIndex);
		keyTextField.setText(entry.key);
		valueTextField.setText(entry.getValue());
		
		if(kvModel.fgdContent != null && kvModel.fgdContent.propmap.containsKey(entry.key.toLowerCase()))
			valueLabel.setText("Value (" + kvModel.fgdContent.propmap.get(entry.key.toLowerCase()).type.name + "):");
		else
			valueLabel.setText("Value:");
		
		keyListener.setKey(entry);
		valueListener.setKey(entry);
		
		refreshClassOriginInfo();
	}
	
	public void applyIOChanges() {
		if(outputTable.isEditing())
			outputTable.getCellEditor().stopCellEditing();
		
	}
	
	public void setSmartEdit(boolean smartedit) {
		smartEdit = smartedit;
		if(fgdContent != null && smartedit && classname != null)
			kvModel.fgdContent = fgdContent.getFGDClass(classname.value);
		else
			kvModel.fgdContent = null;
		
		//kvrenderer.shouldDifferentiateColors(smartEdit && addDefaultParameters);
		kvModel.fireTableDataChanged();
		outputModel.fireTableDataChanged();
	}
	
	public void shouldAddDefaultParameters(boolean should) {
		addDefaultParameters = should;
		
		kvrenderer.shouldDifferentiateColors(addDefaultParameters);
	}
	
	public int entityCount() {
		return editingEntities.size();
	}
	
	public Entity getCurrentEntity() {
		if(editingEntities.size() != 1)
			return null;
		
		return editingEntities.get(0);
	}
	
	public void setEntity(Entity e) {
		editingEntities.clear();
		editingEntities.add(e);
		
		gatherKeyValues();
	}
	
	public void addEntity(Entity e, boolean refresh) {
		editingEntities.add(e);
		
		if(refresh)
			gatherKeyValues();
	}
	
	public void refreshClassOriginInfo() {
		origin = kvMap.get("origin");
		if(origin != null) {
			keyvalues.remove(origin);
			originListener.kv = null;
			originTextField.setText(origin.getValue());
			originTextField.setEnabled(true);
			originListener.setKey(origin);
		} else {
			originListener.kv = null;
			originTextField.setText("");
			originTextField.setEnabled(false);
		}
		
		classname = kvMap.get("classname");
		if(classname != null) {
			keyvalues.remove(classname);
			classnameListener.kv = null;
			classTextField.setText(classname.getValue());
			
			if(!classname.different) {
				classnameListener.setKey(classname);
				classTextField.setEnabled(true);
				
				if(fgdContent != null && smartEdit)
					kvModel.fgdContent = fgdContent.getFGDClass(classname.value);
			} else {
				classTextField.setEnabled(false);
				kvModel.fgdContent = null;
			}
		} else {
			classnameListener.setKey(null);
			classTextField.setText("");
			classTextField.setEnabled(false);
		}
	}
	
	public void gatherKeyValues() {
		keyvalues.clear();
		deletedKv.clear();
		deletedOutputs.clear();
		kvMap.clear();
		outputs.clear();
		
		for(Entity e : editingEntities) {
			FGDEntry fgdent = null;
			
			if(fgdContent != null && e.classname != null) {
				fgdent = fgdContent.getFGDClass(e.classname);
			}
			
			for(int i = 0; i < e.size(); ++i) {
				KeyValLink kvl = e.keyvalues.get(i);
				/*if(fgdent != null && fgdent.outmap.containsKey(kvl.key)) {
					KVEntry kv = new KVEntry();
					kv.key = kvl.key;
					kv.value = kvl.value;
					kv.uniqueId = kvl.uniqueId;
					
					outputs.add(kv);
					continue;
				}*/
				KVEntry entry = kvMap.get(kvl.key);
				
				if(entry == null || editingEntities.size() <= 1) {
					entry = addKeyValue(kvl.key, kvl.value, kvl.uniqueId);
				}
				
				if(!entry.different) {
					entry.different = !entry.value.equals(kvl.value);
				}
			}
		}
		
		if(!kvMap.containsKey("classname"))
			addKeyValue("classname", "info_target", null);
		
		refreshClassOriginInfo();
		
		if(fgdContent != null && classname != null && !classname.different && smartEdit) {
			FGDEntry entry = fgdContent.getFGDClass(classname.value);
			
			if(entry != null) {
				if(addDefaultParameters) {
					for(Property p : entry.properties) {
						KVEntry kventry = kvMap.get(p.name);
						
						if(kventry == null) {
							kventry = addKeyValue(p.name, p.defaultVal, null);
							kventry.autoAdded = true;
						}
					}
				}
			}
		}
		
		KVEntry name = kvMap.get("targetname");
		if(name != null) {
			keyvalues.remove(name);
			keyvalues.add(0, name);
		}
		
		outputModel.set(outputs);
		kvModel.set(keyvalues);
		
		boolean enable = editingEntities.size() > 0;
		apply.setEnabled(enable);
		help.setEnabled(enable);
	}
	
	public void clearEntities(){
		editingEntities.clear();
		keyvalues.clear();
		deletedKv.clear();
		kvMap.clear();
		
		keyTextField.setText("");
		keyTextField.setEnabled(false);
		valueTextField.setText("");
		valueTextField.setEnabled(false);
		
		classname = null;
		origin = null;
		refreshClassOriginInfo();
	}
	
	public void apply() {
		applyKVChanges();
		applyIOChanges();
		
		ArrayList<KVEntry> edited = new ArrayList<KVEntry>();
		ArrayList<KVEntry> renamed = new ArrayList<KVEntry>();
		
		for(KVEntry e : keyvalues) {
			if(e.edited)
				edited.add(e);
			if(e.renamed)
				renamed.add(e);
		}
		
		for(Entity e : editingEntities) {
			if(editingEntities.size() <= 1) {
				for(KVEntry entry : deletedKv) {
					e.delKeyValById(entry.uniqueId);
				}
				
				for(KVEntry entry : renamed) {
					e.changeKey(entry.originalKey, entry.key);
				}
				
				for(KVEntry entry : edited) {
					if(entry.uniqueId == null) {
						entry.uniqueId = e.addKeyVal(entry.key, entry.value);
					} else
						e.setKeyVal(entry.key, entry.value);
				}
			}
			
			if(classname != null && !classname.different)
				e.setKeyVal("classname", classname.value);
			if(origin != null && origin.edited)
				e.setKeyVal("origin", origin.value);
			
			e.setnames();
		}
	}
	
	public void addApplyListener(ActionListener ls) {
		onApply = ls;
	}
	
	public void removeApplyListener() {
		onApply = null;
	}
	
	public String getHelpText() {
		if(fgdContent == null)
			return "<h2>FGD not found! Cannot provide any help</h2><hr><p>Please try loading FGD file (<b>File > Load FGD file</b>).</p>";
		if(classname == null || classname.different)
			return "<h2>Entity classes differ or no class! Cannot provide any help</h2><hr>";
		
		if(!fgdContent.classMap.containsKey(classname.getValue()))
			return "<h2>'"+ classname.getValue() +"' not found in FGD</h2><hr><p>Is this a valid class? Try loading a different FGD</p>";
		
		return fgdContent.getClassHelp(classname.getValue());
	}
	
	private KVEntry addKeyValue(String key, String value, Integer unique) {
		KVEntry entry = new KVEntry();
		entry.key = key;
		entry.value = value;
		entry.uniqueId = unique;
		
		keyvalues.add(entry);
		entry.originalKey = key;
		kvMap.put(key, entry);
		
		return entry;
	}
	
	private void fixDuplicateKey(KVEntry newkv) {
		Pattern reg = Pattern.compile("(.*?)#(\\d+)");
		Matcher match = reg.matcher(newkv.key);
		
		if(match.matches()) {
			newkv.key = match.group(1) + "#" + (Integer.valueOf(match.group(2)) + 1);
		} else {
			newkv.key = newkv.key + "#1";
		}
	}
	
	class KeyValueListener implements DocumentListener {
		KVEntry kv;
		JTextField textField;
		String oldKey;
		String oldVal;
		boolean key = false;

		public KeyValueListener(String keyname, JTextField field) {
			this(keyname, field, false);
		}
		
		public KeyValueListener(String keyname, JTextField field, boolean key) {
			this.kv = kvMap.get(keyname);
			textField = field;
			this.key = key;
			oldKey = keyname;
		}
		
		public void setKey(KVEntry kv) {
			this.kv = kv;
			
			if(kv == null) {
				oldKey = null;
				oldVal = null;
				return;
			}
			oldKey = kv.key;
			oldVal = kv.getValue();
		}
		
		public boolean keyChanged() {
			if(!key || kv == null)
				return false;
			if(oldKey == null)
				return true;
			
			return !oldKey.equals(kv.key);
		}
		
		public boolean valChanged() {
			if(kv == null)
				return false;
			if(oldVal == null)
				return true;
			
			return !oldVal.equals(kv.value);
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
			
			if(kv == null)
				return;
			
			kv.autoAdded = false;
			
			if(key) {
				kv.key = text;
				kv.renamed = true;
			}else {
				kv.value = text;
				kv.different = false;
				kv.edited = true;
			}
		}

		public void changedUpdate(DocumentEvent e) {
		}
	}
	
	protected static class KVEntry{
		public String originalKey = null;
		public String key = "";
		public String value = "";
		public Integer uniqueId = null;

		public boolean different = false;
		public boolean edited = false;
		public boolean renamed = false;
		public boolean autoAdded = false;
		
		public String getValue() {
			if(different) return "(different)";
			
			return value;
		}
	}
}
