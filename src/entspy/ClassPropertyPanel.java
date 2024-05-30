package entspy;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
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

import entspy.FGDEntry.DataType;
import entspy.FGDEntry.InputOutput;
import entspy.FGDEntry.PropChoicePair;
import entspy.FGDEntry.Property;
import entspy.FGDEntry.PropertyChoices;

@SuppressWarnings("serial")
public class ClassPropertyPanel extends JPanel {
	private JTextField classTextField;
	private JTextField originTextField;
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
	private TextListen classnameListener;
	private TextListen originListener;
	
	private ActionListener onApply;
	
	public FGD fgdContent;
	public boolean smartEdit;
	
	public ClassPropertyPanel() {
		editingEntities = new ArrayList<Entity>();
		keyvalues = new ArrayList<KVEntry>();
		deletedKv = new ArrayList<KVEntry>();
		kvMap = new HashMap<String, KVEntry>();
		fgdContent = null;
		smartEdit = false;
		
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
		
		classTextField.getDocument().addDocumentListener(classnameListener);
		originTextField.getDocument().addDocumentListener(originListener);
		
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
				
				Pattern reg = Pattern.compile("(.*?)_(\\d+)");
				Matcher match = reg.matcher(entry.key);
				
				if(match.matches()) {
					newkv.key = match.group(1) + "_" + (Integer.valueOf(match.group(2)) + 1);
				} else {
					newkv.key = entry.key + "_1";
				}
				
				newkv.value = entry.value;
				newkv.different = false;
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
	
	public void updateClassAndOrigin() {
		
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
		if(editingEntities.size() < 1) {
			setEntity(e);
			return;
		}
		
		editingEntities.add(e);
		
		if(refresh)
			gatherKeyValues();
	}
	
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
		
		kvModel.set(keyvalues);
		
		if(origin != null) {
			originListener.key = null;
			originTextField.setText(origin.getValue());
			originTextField.setEnabled(true);
			originListener.key = "origin";
		} else {
			originListener.key = null;
			originTextField.setText("");
			originTextField.setEnabled(false);
		}
		
		if(classname != null) {
			classnameListener.key = null;
			classTextField.setText(classname.getValue());
			
			if(!classname.different) {
				classnameListener.key = "classname";
				classTextField.setEnabled(true);
			} else
				classTextField.setEnabled(false);
		} else {
			classnameListener.key = null;
			classTextField.setText("");
			classTextField.setEnabled(false);
		}
		
		boolean enable = editingEntities.size() > 0;
		apply.setEnabled(enable);
		help.setEnabled(enable);
		
		enable = keyvalues.size() > 0;
		addkv.setEnabled(enable);
		delkv.setEnabled(enable);
		cpykv.setEnabled(enable);
	}
	
	public void clearEntities(){
		editingEntities.clear();
		
		classname = null;
		origin = null;
		gatherKeyValues();
	}
	
	public void apply() {
		ArrayList<KVEntry> edited = new ArrayList<KVEntry>();
		
		for(KVEntry e : keyvalues) {
			if(e.edited)
				edited.add(e);
		}
		
		for(Entity e : editingEntities) {
			for(KVEntry entry : deletedKv) {
				e.delKeyVal(entry.key);
			}
			
			for(KVEntry entry : edited) {
				e.setKeyVal(entry.key, entry.value);
			}
			
			if(classname != null && !classname.different)
				e.setKeyVal("classname", classname.value);
			if(origin != null && origin.edited)
				e.setKeyVal("origin", origin.value);
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
		String key = null;
		JTextField textField;

		public TextListen(String type, JTextField field) {
			this.key = type;
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
			
			if(key == null || !ClassPropertyPanel.this.kvMap.containsKey(key)) return;
			
			ClassPropertyPanel.this.kvMap.get(key).value = text;
			ClassPropertyPanel.this.kvMap.get(key).edited = true;
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
