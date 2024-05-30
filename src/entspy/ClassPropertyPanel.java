package entspy;

import java.awt.BorderLayout;
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
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;

import entspy.FGDEntry.Property;

@SuppressWarnings("serial")
public class ClassPropertyPanel extends JPanel {
	private JTextField classTextField;
	private JTextField originTextField;
	private JTextField keyTextField;
	private JTextField valueTextField;
	private JLabel valueLabel;
	private TextListen classnameListener;
	private TextListen originListener;
	private TextListen keyListener;
	private TextListen valueListener;
	private JTable table;
	private JButton addkv;
	private JButton cpykv;
	private JButton delkv;
	private JButton help;
	private JButton apply;
	private KeyValLinkModel kvModel;
	private KVEntry classname;
	private KVEntry origin;
	private ArrayList<Entity> editingEntities;
	private ArrayList<KVEntry> keyvalues;
	private ArrayList<KVEntry> deletedKv;
	private HashMap<String, KVEntry> kvMap;
	private ActionListener onApply;
	
	private boolean smartEdit;
	private boolean addDefaultParameters;
	
	public FGD fgdContent;
	
	public ClassPropertyPanel() {
		editingEntities = new ArrayList<Entity>();
		keyvalues = new ArrayList<KVEntry>();
		deletedKv = new ArrayList<KVEntry>();
		kvMap = new HashMap<String, KVEntry>();
		
		fgdContent = null;
		smartEdit = false;
		addDefaultParameters = false;
		
		BorderLayout panelBLayout = new BorderLayout();
		panelBLayout.setVgap(5);
		
		this.setLayout(panelBLayout);
		this.setBorder(BorderFactory.createEtchedBorder());
		
		GridLayout gridLayout = new GridLayout(2, 2);
		JPanel grid = new JPanel(gridLayout);
		gridLayout.setHgap(10);
		gridLayout.setVgap(5);
		
		classTextField = new JTextField(" ");
		classTextField.setEnabled(false);
		originTextField = new JTextField(" ");
		originTextField.setEnabled(false);
		
		classnameListener = new TextListen("classname", classTextField);
		originListener = new TextListen("origin", originTextField);
		
		classTextField.getDocument().addDocumentListener(classnameListener);
		originTextField.getDocument().addDocumentListener(originListener);
		
		grid.add(new JLabel("Class", 4));
		grid.add(classTextField);
		grid.add(new JLabel("Origin", 4));
		grid.add(originTextField);
		
		this.add(grid, "North");
		
		JPanel keyvalPanel = new JPanel(new BorderLayout());
		keyvalPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		
		kvModel = new KeyValLinkModel();
		table = new JTable(kvModel);
		table.setSelectionMode(0);
		table.getTableHeader().setReorderingAllowed(false);
		
		TableColumn keycol = table.getColumn("Value");
		keycol.setPreferredWidth(175);
		
		JPanel kveditPanel = new JPanel(gridLayout);
		
		ActionListener kvListener = new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				applyKVChanges();
				int selected = table.getSelectedRow();
				kvModel.refreshtable();
				table.getSelectionModel().setSelectionInterval(selected, selected);
			}
		};
		
		keyTextField = new JTextField(" ");
		keyTextField.setEnabled(false);
		keyTextField.addActionListener(kvListener);
		valueTextField = new JTextField(" ");
		valueTextField.setEnabled(false);
		valueTextField.addActionListener(kvListener);
		
		keyListener = new TextListen(null, keyTextField, true);
		valueListener = new TextListen(null, valueTextField, false);
		
		keyTextField.getDocument().addDocumentListener(keyListener);
		valueTextField.getDocument().addDocumentListener(valueListener);
		
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				int selected = table.getSelectedRow();
				
				if(selected < 0 || selected >= keyvalues.size())
					return;
				
				if(keyvalues.get(selected) != keyListener.kv)
					applyKVChanges();
			}
		});
		
		kveditPanel.add(new JLabel("Key:", 4));
		kveditPanel.add(keyTextField);
		valueLabel = new JLabel("Value:", 4);
		kveditPanel.add(valueLabel);
		kveditPanel.add(valueTextField);
		
		keyvalPanel.add(kveditPanel, "North");
		keyvalPanel.add(new JScrollPane(this.table), "Center");
		
		this.add(keyvalPanel, "Center");
		
		JPanel bottomLeftPanel = new JPanel();
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
		
		GridLayout bottomGridLayout = new GridLayout(1,2);
		bottomGridLayout.setVgap(10);
		JPanel bottom = new JPanel(bottomGridLayout);
		
		JPanel bottomRightPanel = new JPanel();
		
		apply = new JButton("Apply");
		apply.setEnabled(false);
		apply.setToolTipText("Apply to all selected entities.");
		apply.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae) {
				ClassPropertyPanel.this.apply();
				
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
		
		bottom.add(bottomLeftPanel);
		bottom.add(bottomRightPanel);
		this.add(bottom, "South");
		
		addkv.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				KVEntry newkv = new KVEntry();
				newkv.key = "";
				newkv.value = "";
				keyvalues.add(newkv);
				
				int lastrow = keyvalues.size() - 1;
				kvModel.refreshtable();
				table.changeSelection(lastrow, 0, false, false);
				table.scrollRectToVisible(table.getCellRect(lastrow, 0, true));
			}
		});
		cpykv.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ae) {
				int selected = table.getSelectedRow();
				
				if(selected < 0 || selected >= keyvalues.size())
					return;
				
				KVEntry entry = keyvalues.get(selected);
				KVEntry newkv = new KVEntry();
				
				newkv.key = entry.key;
				
				while(kvMap.containsKey(newkv.key))
					fixDuplicateKey(newkv);
				
				newkv.value = entry.value;
				newkv.different = false;
				newkv.renamed = true;
				newkv.edited = true;
				
				kvMap.put(newkv.key, newkv);
				keyvalues.add(++selected, newkv);
				kvModel.refreshtable();
				table.changeSelection(selected, 0, false, false);
				table.scrollRectToVisible(table.getCellRect(selected, 0, true));
				
			}
		});
		delkv.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				int selected = table.getSelectedRow();
				
				if(selected < 0 || selected >= keyvalues.size())
					return;
				
				KVEntry entry = keyvalues.get(selected);
				kvMap.remove(entry.key);
				keyvalues.remove(selected);
				deletedKv.add(entry);
				
				kvModel.refreshtable();
				table.clearSelection();
			}
		});
	}
	
	public void applyKVChanges() {
		if(keyListener.keyChanged()) {
			while(kvMap.containsKey(keyListener.kv.key))
				fixDuplicateKey(keyListener.kv);
			kvMap.remove(keyListener.oldKey);
			kvMap.put(keyListener.kv.key, keyListener.kv);
		}
		
		int selectedIndex = table.getSelectedRow();
		if(selectedIndex < 0 || selectedIndex >= keyvalues.size()) {
			keyListener.setKey(null);
			valueListener.setKey(null);
			keyTextField.setEnabled(false);
			keyTextField.setText("");
			valueTextField.setText("");
			valueTextField.setEnabled(false);
			valueLabel.setText("Value: ");
			
			cpykv.setEnabled(false);
			delkv.setEnabled(false);
			return;
		}
		
		cpykv.setEnabled(true);
		delkv.setEnabled(true);
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
	
	public void setSmartEdit(boolean smartedit) {
		smartEdit = smartedit;
		if(fgdContent != null && smartedit && classname != null)
			kvModel.fgdContent = fgdContent.getFGDClass(classname.value);
		else
			kvModel.fgdContent = null;
		
		kvModel.refreshtable();
	}
	
	public void shouldAddDefaultParameters(boolean should) {
		addDefaultParameters = should;
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
	
	//TODO: add default fgd values, add an option for that etc.
	public void gatherKeyValues() {
		keyvalues.clear();
		deletedKv.clear();
		kvMap.clear();
		
		if(editingEntities.size() > 1) {
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
		
		refreshClassOriginInfo();
		
		if(fgdContent != null && classname != null && !classname.different && addDefaultParameters) {
			Integer index = fgdContent.classMap.get(classname.value);
			
			if(index != null) {
				FGDEntry entry = fgdContent.classes.get(index);
				
				for(Property p : entry.properties) {
					KVEntry kventry = kvMap.get(p.name);
					
					if(kventry == null) {
						kventry = addKeyValue(p.name, "");
						kventry.autoAdded = true;
					}
				}
			}
		}
		
		Collections.sort(keyvalues, new Comparator<KVEntry>(){
			public int compare(KVEntry o1, KVEntry o2) {								
				return o1.key.compareToIgnoreCase(o2.key);
			}
		});
		
		KVEntry name = kvMap.get("targetname");
		if(name != null) {
			keyvalues.remove(name);
			keyvalues.add(0, name);
		}
		
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
		
		cpykv.setEnabled(false);
		delkv.setEnabled(false);
		keyTextField.setText("");
		keyTextField.setEnabled(false);
		valueTextField.setText("");
		valueTextField.setEnabled(false);
		
		classname = null;
		origin = null;
		refreshClassOriginInfo();
	}
	
	public void apply() {
		ArrayList<KVEntry> edited = new ArrayList<KVEntry>();
		ArrayList<KVEntry> renamed = new ArrayList<KVEntry>();
		
		for(KVEntry e : keyvalues) {
			if(e.edited)
				edited.add(e);
		}
		
		for(KVEntry e : keyvalues) {
			if(e.renamed) {
				renamed.add(e);
				if(e.originalKey == null)
					edited.add(e);
			}
		}
		
		for(Entity e : editingEntities) {
			for(KVEntry entry : deletedKv) {
				e.delKeyVal(entry.key);
			}
			
			for(KVEntry entry : renamed) {
				e.changeKey(entry.originalKey, entry.key);
			}
			
			for(KVEntry entry : edited) {
				e.setKeyVal(entry.key, entry.value);
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
	
	private KVEntry addKeyValue(String key, String value) {
		KVEntry entry = new KVEntry();
		entry.key = key;
		entry.value = value;
		
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
	
	class TextListen implements DocumentListener {
		KVEntry kv;
		JTextField textField;
		String oldKey;
		String oldVal;
		boolean key = false;

		public TextListen(String keyname, JTextField field) {
			this(keyname, field, false);
		}
		
		public TextListen(String keyname, JTextField field, boolean key) {
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
