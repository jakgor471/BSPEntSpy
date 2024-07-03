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
import javax.swing.Box;
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

import bspentspy.Entity.KeyValue;
import bspentspy.FGDEntry.Property;

@SuppressWarnings("serial")
public class ClassPropertyPanel extends JPanel {
	private JTextField classTextField;
	private JTextField originTextField;
	private JTextField keyTextField;
	private JButton gotoEnt;
	private JTextField valueTextField;
	private JLabel valueLabel;
	private KeyValueListener classnameListener;
	private KeyValueListener originListener;
	private KeyValueListener keyListener;
	private KeyValueListener valueListener;
	private KeyValTableModel kvModel;
	private KVEntry classname;
	private KVEntry origin;
	private JTable kvtable;
	private JTable flagtable;
	private FlagTableModel flagModel;
	private KVTableRenderer kvrenderer;
	private JButton addkv;
	private JButton dupkv;
	private JButton delkv;
	private JButton help;
	private JButton apply;

	private ArrayList<Entity> editingEntities;
	private ArrayList<KVEntry> outputs;
	private ArrayList<KVEntry> keyvalues;
	private ArrayList<KVEntry> deletedKv;
	private ArrayList<KVEntry> deletedOutputs;
	private HashMap<String, KVEntry> kvMap;
	private ActionListener onApply;
	private ActionListener onGoto;

	private boolean smartEdit;
	private boolean addDefaultParameters;

	private FGD fgdContent;

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

		/* MAIN PARAMETERS PANEL */
		BorderLayout panelBLayout = new BorderLayout();
		panelBLayout.setVgap(5);

		JPanel paramsPanel = new JPanel(panelBLayout);
		paramsPanel.setBorder(BorderFactory.createEtchedBorder());

		JPanel grid = new JPanel(new GridLayout(4, 2));
		((GridLayout) grid.getLayout()).setVgap(5);
		((GridLayout) grid.getLayout()).setHgap(10);

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

		kvModel = new KeyValTableModel();
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
				dupkv.setEnabled(enable);
				delkv.setEnabled(enable);
				gotoEnt.setEnabled(enable);

				if (enable && keyvalues.get(selected) != keyListener.kv)
					applyKVChanges();
			}
		});

		valueLabel = new JLabel("Value:", 4);

		FlowLayout bottomFlowLayout = new FlowLayout(FlowLayout.LEADING);

		grid.add(new JLabel("Key:", 4));
		grid.add(keyTextField);
		grid.add(valueLabel);
		grid.add(valueTextField);

		paramsPanel.add(grid, "North");

		keyvalPanel.add(new JScrollPane(this.kvtable), "Center");

		paramsPanel.add(keyvalPanel, "Center");

		bottomFlowLayout.setVgap(10);
		JPanel bottomLeftPanel = new JPanel(bottomFlowLayout);
		addkv = new JButton("Add");
		addkv.setToolTipText("Add an entity property");
		bottomLeftPanel.add(addkv);
		addkv.setEnabled(true);
		dupkv = new JButton("Duplicate");
		dupkv.setToolTipText("Duplicate the selected property");
		bottomLeftPanel.add(dupkv);
		dupkv.setEnabled(false);
		delkv = new JButton("Delete");
		delkv.setToolTipText("Delete the selected property");
		bottomLeftPanel.add(delkv);
		delkv.setEnabled(false);

		gotoEnt = new JButton("Go to");
		gotoEnt.setEnabled(false);
		gotoEnt.setToolTipText("Go to entity");
		gotoEnt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if (onGoto != null) {
					String entname = valueTextField.getText();
					String[] split = entname.split("\\s*,\\s*");

					if (split.length > 0)
						entname = split[0];

					onGoto.actionPerformed(new GotoEvent(this, ae.getID(), entname));
				}
			}
		});
		bottomLeftPanel.add(Box.createHorizontalStrut(10));
		bottomLeftPanel.add(gotoEnt);

		FlowLayout bottomRightFlow = new FlowLayout(FlowLayout.TRAILING);
		bottomRightFlow.setVgap(10);
		JPanel bottomRightPanel = new JPanel(bottomRightFlow);

		apply = new JButton("Apply");
		apply.setEnabled(false);
		apply.setToolTipText("Apply to all selected entities.");
		apply.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				apply();

				if (onApply != null)
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
		dupkv.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ae) {
				int selected = kvtable.getSelectedRow();

				if (selected < 0 || selected >= keyvalues.size())
					return;

				KVEntry entry = keyvalues.get(selected);
				KVEntry newkv = new KVEntry();

				newkv.key = entry.key;

				while (kvMap.containsKey(newkv.key) && smartEdit)
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

				if (selected < 0 || selected >= keyvalues.size())
					return;

				KVEntry entry = keyvalues.get(selected);
				kvMap.remove(entry.key);
				keyvalues.remove(selected);

				// if uniqueId is null it means this kv exists only in editor, not in actual
				// entity
				if (entry.uniqueId != null)
					deletedKv.add(entry);

				kvModel.refreshtable();
				kvtable.clearSelection();
			}
		});

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Parameters", paramsPanel);
		/* === END OF MAIN PARAMETERS PANEL === */

		flagModel = new FlagTableModel();
		flagtable = new JTable(flagModel);
		flagtable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		flagtable.getTableHeader().setReorderingAllowed(false);
		flagtable.getColumnModel().getColumn(0).setMaxWidth(50);
		flagtable.getColumnModel().getColumn(0).setMinWidth(40);
		flagtable.setShowGrid(false);
		tabs.addTab("Flags", new JScrollPane(flagtable));

		this.add(tabs, "Center");
		this.add(bottomRightPanel, "South");
	}

	public void setFGD(FGD fgdContent) {
		this.fgdContent = fgdContent;
	}

	public void applyKVChanges() {
		if (keyListener.keyChanged()) {
			while (kvMap.containsKey(keyListener.kv.key) && editingEntities.size() > 1)
				fixDuplicateKey(keyListener.kv);
			kvMap.remove(keyListener.oldKey);
			kvMap.put(keyListener.kv.key, keyListener.kv);
		}

		int selectedIndex = kvtable.getSelectedRow();
		if (selectedIndex < 0 || selectedIndex >= keyvalues.size()) {
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

		if (kvModel.fgdData != null && kvModel.fgdData.propmap.containsKey(entry.key.toLowerCase()))
			valueLabel.setText("Value (" + kvModel.fgdData.propmap.get(entry.key.toLowerCase()).type.name + "):");
		else
			valueLabel.setText("Value:");

		keyListener.setKey(entry);
		valueListener.setKey(entry);

		refreshClassOriginInfo();
	}

	public void setSmartEdit(boolean smartedit) {
		smartEdit = smartedit;
		if (fgdContent != null && smartedit && classname != null)
			kvModel.fgdData = fgdContent.getFGDClass(classname.value);
		else
			kvModel.fgdData = null;

		// kvrenderer.shouldDifferentiateColors(smartEdit && addDefaultParameters);
		kvModel.fireTableDataChanged();
	}

	public void shouldAddDefaultParameters(boolean should) {
		addDefaultParameters = should;

		kvrenderer.shouldDifferentiateColors(addDefaultParameters);
	}

	public int entityCount() {
		return editingEntities.size();
	}

	public Entity getCurrentEntity() {
		if (editingEntities.size() != 1)
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

		if (refresh)
			gatherKeyValues();
	}

	public void refreshClassOriginInfo() {
		origin = kvMap.get("origin");
		if (origin != null) {
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
		if (classname != null) {
			keyvalues.remove(classname);
			classnameListener.kv = null;
			classTextField.setText(classname.getValue());

			if (!classname.different) {
				classnameListener.setKey(classname);
				classTextField.setEnabled(true);

				if (fgdContent != null && smartEdit)
					kvModel.fgdData = fgdContent.getFGDClass(classname.value);
			} else {
				classTextField.setEnabled(false);
				kvModel.fgdData = null;
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
		flagModel.setFlags(null);
		flagModel.setFGD(null);

		if (editingEntities.size() < 1) {
			kvModel.fireTableDataChanged();
			flagModel.fireTableDataChanged();
			return;
		}

		for (Entity e : editingEntities) {
			for (int i = 0; i < e.size(); ++i) {
				KeyValue kvl = e.keyvalues.get(i);
				KVEntry entry = kvMap.get(kvl.key);

				if (entry == null || editingEntities.size() <= 1) {
					entry = addKeyValue(kvl.key, kvl.value, kvl.uniqueId);
				}

				if (!entry.different) {
					entry.different = !entry.value.equals(kvl.value);
				}
			}
		}

		if (!kvMap.containsKey("classname")) {
			addKeyValue("classname", "info_target", null);
			kvMap.get("classname").edited = true;
		}

		refreshClassOriginInfo();

		flagModel.setFGD(null);
		if (fgdContent != null && classname != null && !classname.different) {
			FGDEntry entry = fgdContent.getFGDClass(classname.value);

			if (entry != null) {
				if (addDefaultParameters) {
					for (Property p : entry.properties) {
						KVEntry kventry = kvMap.get(p.name);

						if (kventry == null) {
							kventry = addKeyValue(p.name, p.defaultVal, null);
							kventry.autoAdded = true;
						}
					}
				}

				flagModel.setFGD(entry);
			}
		}

		KVEntry name = kvMap.get("targetname");
		if (name != null) {
			keyvalues.remove(name);
			keyvalues.add(0, name);
		}

		kvModel.set(keyvalues);
		flagModel.setFlags(kvMap.get("spawnflags"));

		boolean enable = editingEntities.size() > 0;
		apply.setEnabled(enable);
		help.setEnabled(enable);
	}

	public void clearEntities() {
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

		ArrayList<KVEntry> edited = new ArrayList<KVEntry>();
		ArrayList<KVEntry> renamed = new ArrayList<KVEntry>();

		if (flagModel.isChanged()) {
			KVEntry flags = kvMap.get("spawnflags");

			if (flags != null) {
				flags.value = flagModel.getValue();
				flags.edited = true;
			}
		}

		if (editingEntities.size() <= 1) {
			for (KVEntry e : keyvalues) {
				// single entity edit
				if (e.edited || e.renamed)
					edited.add(e);
			}
		} else {
			for (KVEntry e : keyvalues) {
				// multi entity edit
				if (e.edited)
					edited.add(e);
				if (e.renamed)
					renamed.add(e);
			}
		}

		boolean isEdited = (edited.size() > 0 || renamed.size() > 0 || deletedKv.size() > 0);
		boolean classOriginEdited = (classname != null && classname.edited) || (origin != null && origin.edited);
		if (!(isEdited || classOriginEdited))
			return;

		// TODO: duplicates etc
		Undo.create();
		for (Entity e : editingEntities) {
			Undo.setTarget(e);
			if (editingEntities.size() <= 1) {
				// single entity edit
				for (KVEntry entry : deletedKv) {
					e.delKeyValById(entry.uniqueId);
				}

				for (KVEntry entry : edited) {
					entry.uniqueId = e.setKeyVal(entry.uniqueId, entry.key, entry.value);
				}
			} else {
				// multi entity edit
				for (KVEntry entry : deletedKv) {
					e.delKeyVal(entry.key);
				}

				for (KVEntry entry : renamed) {
					e.changeKey(entry.originalKey, entry.key);
				}

				for (KVEntry entry : edited) {
					e.setKeyVal(entry.key, entry.value);
				}
			}

			if (classname != null && !classname.different && classname.edited)
				e.setKeyVal("classname", classname.value);
			if (origin != null && origin.edited && origin.edited)
				e.setKeyVal("origin", origin.value);

			e.setnames();
		}
		Undo.finish();
	}

	public void addApplyListener(ActionListener ls) {
		onApply = ls;
	}

	public void addGotoListener(ActionListener ls) {
		onGoto = ls;
	}

	public void removeApplyListener() {
		onApply = null;
	}

	public String getHelpText() {
		if (fgdContent == null)
			return "<h2>FGD not found! Cannot provide any help</h2><hr><p>Please try loading FGD file (<b>File > Load FGD file</b>).</p>";
		if (classname == null || classname.different)
			return "<h2>Entity classes differ or no class! Cannot provide any help</h2><hr>";

		if (!fgdContent.classMap.containsKey(classname.getValue()))
			return "<h2>'" + classname.getValue()
					+ "' not found in FGD</h2><hr><p>Is this a valid class? Try loading a different FGD</p>";

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

		if (match.matches()) {
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

			if (kv == null) {
				oldKey = null;
				oldVal = null;
				return;
			}
			oldKey = kv.key;
			oldVal = kv.getValue();
		}

		public boolean keyChanged() {
			if (!key || kv == null)
				return false;
			if (oldKey == null)
				return true;

			return !oldKey.equals(kv.key);
		}

		public boolean valChanged() {
			if (kv == null)
				return false;
			if (oldVal == null)
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

			if (kv == null)
				return;

			kv.autoAdded = false;

			if (key) {
				kv.key = text;
				kv.renamed = true;
			} else {
				kv.value = text;
				kv.different = false;
				kv.edited = true;
			}
		}

		public void changedUpdate(DocumentEvent e) {
		}
	}

	public static class GotoEvent extends ActionEvent {
		public String entname;

		public GotoEvent(Object source, int id, String entname) {
			super(source, id, "goto");
			this.entname = entname;
		}

	}

	protected static class KVEntry {
		public String originalKey = null;
		public String key = "";
		public String value = "";
		public Integer uniqueId = null;

		public boolean different = false;
		public boolean edited = false;
		public boolean renamed = false;
		public boolean autoAdded = false;

		public String getValue() {
			if (different)
				return "(different)";

			return value;
		}
	}
}
