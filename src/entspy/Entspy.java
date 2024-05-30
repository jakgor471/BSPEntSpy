package entspy;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import javax.swing.tree.DefaultMutableTreeNode;

import entspy.Lexer.LexerException;
import util.Cons;
import util.SwingWorker;

public class Entspy {
	BSP m;
	String filename;
	File infile;
	RandomAccessFile raf;
	JFrame frame = null;
	final Entity blank = new Entity("");
	JList<Entity> entList;
	JTable table;
	MapInfo info;
	Preferences preferences;
	FGD fgdFile = null;
	static final String VERSION = "v0.9";
	static ImageIcon esIcon = new ImageIcon(JTBRenderer.class.getResource("/images/newicons/entspy.png"));
	
	public static final String entspyTitle = "Entspy v2.0";

	public int exec() throws IOException {
		preferences = Preferences.userRoot().node(getClass().getName());
		
		this.frame = new JFrame(entspyTitle);
		this.frame.setIconImage(esIcon.getImage());
		if (!this.loadfile()) {
			System.exit(0);
		}
		this.m = new BSP(this.raf);
		this.m.loadheader();
		
		if(loadfgdfiles(null)) {
			System.out.println("FGD loaded: " + String.join(", ", fgdFile.loadedFgds));
		} else {
			preferences.remove("LastFGDFile");
		}
		
		this.entList = new JList<Entity>();
		DefaultListSelectionModel selmodel = new DefaultListSelectionModel();
		selmodel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		entList.setSelectionModel(selmodel);
		entList.setCellRenderer(new LERenderer());

		this.frame.setTitle(entspyTitle + " - " + this.filename);
		JMenu filemenu = new JMenu("File");
		JMenuItem mload = new JMenuItem("Load BSP");
		JMenuItem msave = new JMenuItem("Save BSP");
		mload.setToolTipText("Load an new map file");
		msave.setToolTipText("Save the current map file");
		mload.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK));
		msave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
		filemenu.add(mload);
		filemenu.add(msave);
		
		filemenu.addSeparator();
		
		JMenuItem mloadfgd = new JMenuItem("Load FGD file");
		mloadfgd.setToolTipText("Load an FGD file to enable Smart Edit");
		filemenu.add(mloadfgd);
		
		filemenu.addSeparator();
		JMenuItem minfo = new JMenuItem("Map info...");
		minfo.setToolTipText("Map header information");
		minfo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_DOWN_MASK));
		filemenu.add(minfo);
		filemenu.addSeparator();
		JMenuItem mquit = new JMenuItem("Quit");
		mquit.setToolTipText("Quit Entspy");
		mquit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK));
		filemenu.add(mquit);

		JMenu helpmenu = new JMenu("Help");
		JMenuItem mhelpSearch = new JMenuItem("Search help");
		helpmenu.add(mhelpSearch);

		mhelpSearch.addActionListener(new HelpActionListener("/text/searchhelp.html"));
		
		JMenuItem mexportHelp = new JMenuItem("Export / Import help");
		helpmenu.add(mexportHelp);

		mexportHelp.addActionListener(new HelpActionListener("/text/exporthelp.html"));
		
		JMenuItem fgdhelp = new JMenuItem("FGD help");
		helpmenu.add(fgdhelp);

		fgdhelp.addActionListener(new HelpActionListener("/text/fgdhelp.html"));
		
		helpmenu.addSeparator();
		
		JMenuItem mcreditHelp = new JMenuItem("Credits");
		helpmenu.add(mcreditHelp);

		mcreditHelp.addActionListener(new HelpActionListener("/text/credits.html"));

		JMenuBar menubar = new JMenuBar();
		menubar.add(filemenu);
		menubar.add(helpmenu);
		this.frame.setJMenuBar(menubar);
		mload.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				try {
					if (Entspy.this.checkchanged("Load BSP")) {
						return;
					}
					if (!Entspy.this.loadfile()) {
						return;
					}
					Entspy.this.m = new BSP(Entspy.this.raf);
					Entspy.this.m.loadheader();
					Entspy.this.loaddata();
				} catch (IOException ex) {
					Cons.println(ex);
				}
			}
		});
		msave.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				Entspy.this.frame.setCursor(Cursor.getPredefinedCursor(3));
				SwingWorker worker = new SwingWorker() {

					public Object construct() {
						Entspy.this.savefile();
						return null;
					}

					public void finished() {
						Entspy.this.frame.setTitle(entspyTitle + " - " + Entspy.this.filename);
						Entspy.this.frame.setCursor(null);
					}
				};
				worker.start();
			}

		});
		mquit.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (Entspy.this.checkchanged("Quit Entspy")) {
					return;
				}
				System.exit(0);
			}
		});
		minfo.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				Entspy.this.m.setfile(Entspy.this.raf);
				if (Entspy.this.info != null) {
					Entspy.this.info.dispose();
				}
				Entspy.this.info = new MapInfo(Entspy.this.frame, Entspy.this.m, Entspy.this.filename);
			}
		});
		
		mloadfgd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser(preferences.get("LastFGDDir", System.getProperty("user.dir")));
				chooser.setDialogTitle(entspyTitle + " - Open FGD File");
				if(chooser.showOpenDialog(frame) == 1)
					return;
				
				File f = chooser.getSelectedFile();
				if(loadfgdfiles(f)) {
					preferences.put("LastFGDFile", f.toString());
					preferences.put("LastFGDDir", f.getAbsolutePath());
					JOptionPane.showMessageDialog(frame, f.getName() + " successfuly loaded. It will load automaticaly on program start.");
				}
			}
		});
		
		BorderLayout panelBLayout = new BorderLayout();
		panelBLayout.setVgap(5);
		
		JPanel rightEntPanel = new JPanel(panelBLayout);
		JPanel grid = new JPanel();
		GridLayout gridLayout = new GridLayout(2, 2);
		gridLayout.setHgap(10);
		gridLayout.setVgap(5);
		grid.setLayout(gridLayout);
		
		final JTextField classTextField = new JTextField(" ");
		final JTextField originTextField = new JTextField(" ");
		classTextField.addActionListener(new TextListen(0));
		originTextField.addActionListener(new TextListen(3));
		classTextField.setEnabled(false);
		originTextField.setEnabled(false);
		
		grid.add(new JLabel("Class", 4));
		grid.add(classTextField);
		grid.add(new JLabel("Origin", 4));
		grid.add(originTextField);
		
		rightEntPanel.add((Component) grid, "North");
		
		JPanel keyvalPanel = new JPanel();
		final KeyValLinkModel kvOldModel = new KeyValLinkModel();
		kvOldModel.setMapping(this.entList);
		this.table = new JTable(kvOldModel);
		this.table.setSelectionMode(0);
		
		TableColumn keycol = this.table.getColumn("Value");
		keycol.setPreferredWidth(175);
		TableColumn linkcol = this.table.getColumn("Link");
		linkcol.setMaxWidth(30);
		linkcol.setMinWidth(30);
		linkcol.setResizable(false);
		linkcol.setCellRenderer(new JTBRenderer());
		linkcol.setCellEditor(new JTBEditor(new JCheckBox()));
		
		keyvalPanel.setLayout(new GridLayout(1, 1));
		keyvalPanel.add(new JScrollPane(this.table));
		
		rightEntPanel.add((Component) keyvalPanel, "Center");
		
		JPanel findpanel = new JPanel();
		
		final JLabel findlabel = new JLabel("Linked from ");
		findpanel.add(findlabel);
		final DefaultComboBoxModel findmodel = new DefaultComboBoxModel();
		final JComboBox findcombo = new JComboBox(findmodel);
		findcombo.setPreferredSize(new Dimension(200, findcombo.getPreferredSize().height));
		Font cfont = findcombo.getFont();
		findcombo.setFont(new Font(cfont.getName(), 0, cfont.getSize() - 1));
		findcombo.setToolTipText("Entity that links to this entity");
		findpanel.add(findcombo);
		final JTBRenderer findbutton = new JTBRenderer();
		findbutton.setToolTipText("Go to linking entity");
		findpanel.add(findbutton);
		findbutton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				/*
				 * Entity targetent = (Entity)findmodel.getSelectedItem();
				 * DefaultMutableTreeNode currentnode =
				 * (DefaultMutableTreeNode)Entspy.this.entList.getModel().getRoot(); do { if
				 * (Entspy.this.getNodeEntity(currentnode) != targetent) continue; TreePath tp =
				 * new TreePath(currentnode.getPath());
				 * Entspy.this.entList.setSelectionPath(tp);
				 * Entspy.this.entList.scrollPathToVisible(tp); return; } while ((currentnode =
				 * currentnode.getNextNode()) != null);
				 * Cons.println("Cannot find node for target ent: " + targetent);
				 */
			}
		});
		findlabel.setEnabled(false);
		findbutton.setEnabled(false);
		findcombo.setEnabled(false);

		JPanel leftPanel = new JPanel(new BorderLayout());
		leftPanel.add((Component) new JScrollPane(this.entList), "Center");
		JPanel entcpl = new JPanel();
		entcpl.setLayout(new BoxLayout(entcpl, BoxLayout.PAGE_AXIS));

		JPanel entbut = new JPanel();
		
		JButton updent = new JButton("Update");
		updent.setToolTipText("Update entity links");
		JButton addent = new JButton("Add");
		addent.setToolTipText("Add a new entity");
		final JButton cpyent = new JButton("Duplicate");
		cpyent.setToolTipText("Duplicate the selected entities");
		cpyent.setEnabled(false);
		final JButton delent = new JButton("Del");
		delent.setToolTipText("Delete the selected entities");
		delent.setEnabled(false);
		entbut.add(updent);
		entbut.add(addent);
		entbut.add(cpyent);
		entbut.add(delent);

		JPanel entexp = new JPanel();

		final JButton importEntity = new JButton("Import");
		importEntity.setToolTipText("Import entities from a file");
		importEntity.setEnabled(true);
		entexp.add(importEntity);

		final JButton exportEntity = new JButton("Export");
		exportEntity.setToolTipText("Export selected entities to a file");
		exportEntity.setEnabled(false);
		entexp.add(exportEntity);
		
		exportEntity.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				ArrayList<Entity> ents = new ArrayList<Entity>();
				
				for(int i : entList.getSelectedIndices()) {
					ents.add(m.el.get(i));
				}
				
				JFileChooser chooser = new JFileChooser(preferences.get("LastFolder", System.getProperty("user.dir")));
				chooser.setDialogTitle(entspyTitle + " - Export entities to a file");
				chooser.setDialogType(JFileChooser.SAVE_DIALOG);
				
				int result = chooser.showOpenDialog(frame);
				if (result == 1) {
					return;
				}
				
				File f = chooser.getSelectedFile();
				
				String ext = ".ent";
				if(f.getName().indexOf('.') > -1)
					ext = "";
				
				try(FileWriter fw = new FileWriter(f + ext)){
					writeEntsToWriter(ents, fw);
					
					fw.close();
					
					preferences.put("LastFolder", f.getParent());
					JOptionPane.showMessageDialog(frame, ents.size() + " entities successfuly exported to " + f, "Info", JOptionPane.INFORMATION_MESSAGE);
				} catch(IOException e) {
					JOptionPane.showMessageDialog(frame, e.getMessage(), "ERROR!", JOptionPane.ERROR_MESSAGE);
				}
				
				return;
			}
		});
		
		importEntity.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				JFileChooser chooser = new JFileChooser(preferences.get("LastFolder", System.getProperty("user.dir")));
				chooser.setDialogTitle(entspyTitle + " - Export entities to a file");
				chooser.setDialogType(JFileChooser.SAVE_DIALOG);
				
				int result = chooser.showOpenDialog(frame);
				if (result == 1) {
					return;
				}
				
				File f = chooser.getSelectedFile();
				
				try(FileReader fr = new FileReader(f)){
					ArrayList<Entity> ents = loadEntsFromReader(fr);
					
					fr.close();
					
					for(Entity e : ents) {
						m.el.add(e);
					}
					
					entList.setModel(new EntspyListModel(m.getData()));
					preferences.put("LastFolder", f.getParent());
					
					JOptionPane.showMessageDialog(frame, ents.size() + " entities successfuly imported from " + f, "Info", JOptionPane.INFORMATION_MESSAGE);
				} catch(Exception e) {
					JOptionPane.showMessageDialog(frame, e.getMessage(), "ERROR!", JOptionPane.ERROR_MESSAGE);
				}
				
				return;
			}
		});
		
		final JButton cpToClipEnt = new JButton("Copy");
		cpToClipEnt.setToolTipText("Copy selected entities to clipboard");
		cpToClipEnt.setEnabled(false);
		entexp.add(cpToClipEnt);
		
		final JButton pstFromClipEnt = new JButton("Paste");
		pstFromClipEnt.setToolTipText("Paste entities from clipboard");
		pstFromClipEnt.setEnabled(true);
		entexp.add(pstFromClipEnt);
		
		cpToClipEnt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				ArrayList<Entity> ents = new ArrayList<Entity>();
				
				for(int i : entList.getSelectedIndices()) {
					ents.add(m.el.get(i));
				}
				
				try(StringWriter sw = new StringWriter(ents.size() * 256)){
					writeEntsToWriter(ents, sw);
					
					sw.close();
					
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					cb.setContents(new StringSelection(sw.toString()), null);
				} catch(IOException e) {
					JOptionPane.showMessageDialog(frame, e.getMessage(), "ERROR!", JOptionPane.ERROR_MESSAGE);
				}
				
				return;
			}
		});
		
		pstFromClipEnt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				try{
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					Transferable cbcontent = cb.getContents(null);
					
					if(cbcontent == null)
						return;
					
					StringReader sr = new StringReader(cbcontent.getTransferData(DataFlavor.stringFlavor).toString());
					ArrayList<Entity> ents = loadEntsFromReader(sr);
					
					sr.close();
					
					int i = entList.getMaxSelectionIndex();
					
					if(i < 0)
						i = Math.max(m.el.size() - 1, 0);
					
					for(Entity e : ents) {
						m.el.add(++i, e);
					}
					
					entList.setModel(new EntspyListModel(m.getData()));
				} catch(Exception e) {
					JOptionPane.showMessageDialog(frame, "Could not parse data from clipboard!\n" + e.getMessage(), "ERROR!", JOptionPane.ERROR_MESSAGE);
				}
				
				return;
			}
		});

		JButton findent = new JButton("Find Next");
		findent.setToolTipText("Find entity, hold Shift to add to selection");
		findent.setMnemonic(KeyEvent.VK_F);
		JButton findall = new JButton("Find all");
		findall.setToolTipText("Select all matching entities, hold Shift to add to selection");
		JTextField findtext = new JTextField();
		findtext.setToolTipText("Text to search for");
		Box fbox = Box.createHorizontalBox();
		
		fbox.add(findtext);
		fbox.add(findent);
		fbox.add(findall);

		entcpl.add((Component) fbox);
		entcpl.add((Component) entbut);
		entcpl.add((Component) entexp);

		leftPanel.add((Component) entcpl, "South");
		findent.addActionListener(new FindListen(findtext));
		findtext.addActionListener(new FindListen(findtext));
		findall.addActionListener(new FindSelectListen(findtext));
		updent.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				entList.setModel(new EntspyListModel(m.getData()));
			}
		});
		delent.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				ArrayList<Entity> toremove = new ArrayList<Entity>();
				int j = 0;
				for (int i : Entspy.this.entList.getSelectedIndices()) {
					toremove.add(m.el.get(i));
					++j;
				}
				m.el.removeAll(toremove);
				
				j = entList.getMaxSelectionIndex() - j;

				Entspy.this.entList.setModel(new EntspyListModel(Entspy.this.m.getData()));
				
				entList.setSelectedIndex(j + 1);
				
				Entspy.this.m.dirty = true;
			}
		});
		cpyent.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {				
				int[] selected = entList.getSelectedIndices();
				for(int j = 0; j < selected.length; ++j) {
					selected[j] += j;
					Entity old = m.el.get(selected[j]);
					++selected[j];
					
					if(old != null) {
						Entity newE = old.copy();
						m.el.add(selected[j], newE);
					}
				}
				
				entList.setModel(new EntspyListModel(m.getData()));
				entList.setSelectedIndices(selected);

				Entspy.this.m.dirty = true;
			}
		});
		addent.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				Entity newent = new Entity();
				int index = 0;

				if (entList.getMaxSelectionIndex() > 0) {
					m.el.add(entList.getMaxSelectionIndex() + 1, newent);
					index = entList.getMaxSelectionIndex() + 1;
				} else {
					m.el.add(newent);
					index = m.el.size() - 1;
				}

				newent.autoedit = true;
				Entspy.this.entList.setModel(new EntspyListModel(m.getData()));
				entList.setSelectedIndex(index);
				entList.ensureIndexIsVisible(index);

			}
		});
		
		JPanel cpanel = new JPanel();
		final JButton addkv = new JButton("Add");
		addkv.setToolTipText("Add an entity property");
		cpanel.add(addkv);
		addkv.setEnabled(false);
		final JButton cpykv = new JButton("Copy");
		cpykv.setToolTipText("Copy the selected property");
		cpanel.add(cpykv);
		cpykv.setEnabled(false);
		final JButton delkv = new JButton("Delete");
		delkv.setToolTipText("Delete the selected property");
		cpanel.add(delkv);
		delkv.setEnabled(false);

		this.entList.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				TableCellEditor tce = Entspy.this.table.getCellEditor();
				if (tce != null && Entspy.this.table.isEditing())
					tce.stopCellEditing();
				Entity selEnt = Entspy.this.getSelectedEntity();

				if (selEnt != null) {
					exportEntity.setEnabled(true);
					cpToClipEnt.setEnabled(true);

					classTextField.setText(selEnt.classname);
					classTextField.setEnabled(true);
					originTextField.setText(selEnt.origin);
					originTextField.setEnabled(true);
					Entspy.this.settable(selEnt, kvOldModel);

					delent.setEnabled(true);
					cpyent.setEnabled(true);
					addkv.setEnabled(true);
					if (selEnt.autoedit) {
						selEnt.autoedit = false;
						classTextField.setText("new_entity");
						classTextField.selectAll();
						classTextField.requestFocus();
					}
					if (Entspy.this.setfindlist(selEnt, findmodel)) {
						findlabel.setEnabled(true);
						findbutton.setEnabled(true);
						findcombo.setEnabled(true);
					} else {
						findlabel.setEnabled(false);
						findbutton.setEnabled(false);
						findcombo.setEnabled(false);
					}
				} else {
					exportEntity.setEnabled(false);
					cpToClipEnt.setEnabled(false);

					classTextField.setText(" ");
					classTextField.setEnabled(false);
					originTextField.setText(" ");
					originTextField.setEnabled(false);
					Entspy.this.settable(Entspy.this.blank, kvOldModel);
					findlabel.setEnabled(false);
					findbutton.setEnabled(false);
					findcombo.setEnabled(false);
					delent.setEnabled(false);
					cpyent.setEnabled(false);
					addkv.setEnabled(false);
				}
			}

		});

		addkv.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ae) {
				Entity selEnt = Entspy.this.getSelectedEntity();
				selEnt.addKeyVal("", "");
				Entspy.this.settable(selEnt, kvOldModel);
				int lastrow = selEnt.size() - 1;
				Entspy.this.table.changeSelection(lastrow, 0, false, false);
				Entspy.this.table.editCellAt(lastrow, 0);
				Entspy.this.table.scrollRectToVisible(Entspy.this.table.getCellRect(lastrow, 0, true));
				Entspy.this.table.getEditorComponent().requestFocus();
				Entspy.this.m.dirty = true;
			}
		});
		cpykv.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ae) {
				Entity selEnt = Entspy.this.getSelectedEntity();
				int selrow = Entspy.this.table.getSelectedRow();
				if (selrow == -1) {
					return;
				}
				selEnt.addKeyVal(selEnt.keys.get(selrow), selEnt.values.get(selrow));
				selEnt.setnames();
				Entspy.this.settable(selEnt, kvOldModel);
				kvOldModel.reselect();
				int lastrow = selEnt.size() - 1;
				Entspy.this.table.changeSelection(lastrow, 1, false, false);
				Entspy.this.table.editCellAt(lastrow, 1);
				Entspy.this.table.scrollRectToVisible(Entspy.this.table.getCellRect(lastrow, 1, true));
				Entspy.this.table.getEditorComponent().requestFocus();
				Entspy.this.m.dirty = true;
			}
		});
		delkv.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ae) {
				Entity selEnt = Entspy.this.getSelectedEntity();
				int selrow = Entspy.this.table.getSelectedRow();
				if (selrow == -1) {
					return;
				}
				selEnt.delKeyVal(selrow);
				selEnt.setnames();
				kvOldModel.setlinklisteners();
				
				if(kvOldModel.getRowCount() > 0) {
					selrow = Math.min(selrow, kvOldModel.getRowCount() - 1);
					table.setRowSelectionInterval(selrow, selrow);
				}
				
				Entspy.this.m.dirty = true;
			}
		});
		this.table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			public void valueChanged(ListSelectionEvent lse) {
				if (lse.getValueIsAdjusting()) {
					return;
				}
				if (Entspy.this.table.getSelectedRowCount() != 0) {
					delkv.setEnabled(true);
					cpykv.setEnabled(true);
				} else {
					delkv.setEnabled(false);
					cpykv.setEnabled(false);
				}
			}
		});
		
		rightEntPanel.add(cpanel, "South");
		rightEntPanel.setBorder(BorderFactory.createEtchedBorder());
		
		JPanel rightPanel = new JPanel(new BorderLayout());
		rightPanel.add((Component) findpanel, "South");
		rightPanel.add((Component) rightEntPanel, "Center");
		
		JSplitPane mainSplit = new JSplitPane(1, leftPanel, rightPanel);
		mainSplit.setDividerLocation(275);
		this.frame.setDefaultCloseOperation(3);
		this.frame.setSize(720, 520);
		this.frame.getContentPane().add(mainSplit);
		this.frame.setVisible(true);
		this.loaddata();
		return 0;
	}

	public void settable(Entity ent, KeyValLinkModel model) {
		model.set(ent);
	}

	public boolean setfindlist(Entity sel, DefaultComboBoxModel model) {
		model.removeAllElements();
		block0: for (int i = 0; i < this.m.el.size(); ++i) {
			Entity lent = this.m.el.get(i);
			if (lent.keys == null)
				continue;
			for (int j = 0; j < lent.keys.size(); ++j) {
				Entity linkent = lent.links.get(j);
				if (linkent == null || !linkent.targetname.equals(sel.targetname))
					continue;
				model.addElement(lent);
				continue block0;
			}
		}
		if (model.getSize() == 0) {
			return false;
		}
		return true;
	}

	public int ubyte(byte b) {
		return b & 255;
	}

	public boolean loadfile() {
		try {
			JFileChooser chooser = new JFileChooser(preferences.get("LastFolder", System.getProperty("user.dir")));
			
			chooser.setDialogTitle(entspyTitle + " - Open a BSP file");
			chooser.setFileFilter(new EntFileFilter());
			int result = chooser.showOpenDialog(this.frame);
			if (result == 1) {
				return false;
			}
			this.infile = chooser.getSelectedFile();
			chooser = null;
			this.filename = this.infile.getName();
			if (!(this.infile.exists() && this.infile.canRead())) {
				Cons.println("Can't read " + this.filename + "!");
				return false;
			}
			Cons.println("Reading map file " + this.filename);
			this.raf = new RandomAccessFile(this.infile, "r");
			
			preferences.put("LastFolder", this.infile.getParent());
			
			return true;
		} catch (IOException ioe) {
			Cons.println(ioe);
			return false;
		}
	}

	public void savefile() {
		try {
			JRadioButton rb;
			ButtonGroup bgroup;
			Object[] opts;
			int result;
			this.m.calcentitylump();
			int entopt = 0;
			if (!(this.m.entdiff <= 0 || this.m.isentaftergl)) {
				opts = new Object[3];
				opts[0] = "Entity lump exceeds previously stored size by " + this.strsize(this.m.entdiff)
						+ ".\nChoose an option:";
				bgroup = new ButtonGroup();
				rb = new JRadioButton("Optimize storage (minimum file size, breaks checksum).", true);
				bgroup.add(rb);
				rb.setActionCommand("Optimize");
				opts[1] = rb;
				rb = new JRadioButton(
						"Store at end (wastes " + this.strsize(this.m.entlumpsize) + ", preserves checksum).");
				bgroup.add(rb);
				rb.setActionCommand("Preserve");
				opts[2] = rb;
				result = JOptionPane.showOptionDialog(this.frame, opts, "Save BSP file", 2, 3, null, null, null);
				if (result == 2) {
					return;
				}
				if (bgroup.getSelection().getActionCommand().equals("Preserve")) {
					entopt = 1;
				}
			}
			if (!(this.m.entdiff >= 0 || this.m.isentaftergl)) {
				opts = new Object[3];
				opts[0] = "Entity lump is smaller than previously stored size by " + this.strsize(-this.m.entdiff)
						+ ".\nChoose an option:";
				bgroup = new ButtonGroup();
				rb = new JRadioButton("Optimize storage (minimum file size, breaks checksum).", true);
				bgroup.add(rb);
				rb.setActionCommand("Optimize");
				opts[1] = rb;
				rb = new JRadioButton("Store without optimization (wastes " + this.strsize(-this.m.entdiff)
						+ ", preserves checksum).");
				bgroup.add(rb);
				rb.setActionCommand("Preserve2");
				opts[2] = rb;
				result = JOptionPane.showOptionDialog(this.frame, opts, "Save BSP file", 2, 3, null, null, null);
				if (result == 2) {
					return;
				}
				if (bgroup.getSelection().getActionCommand().equals("Preserve2")) {
					entopt = 2;
				}
			}
			JProgFrame prog = new JProgFrame(this.frame, entspyTitle + " - Save BSP file");
			this.m.setprog(prog);
			JFileChooser chooser = new JFileChooser(this.infile);
			chooser.setSelectedFile(this.infile);
			chooser.setDialogTitle(entspyTitle + " - Save BSP file - " + this.filename);
			chooser.setFileFilter(new EntFileFilter());
			int result2 = chooser.showSaveDialog(this.frame);
			if (result2 == 1) {
				return;
			}
			File outfile = chooser.getSelectedFile();
			chooser = null;
			String outfilename = outfile.getName();
			if (outfile.exists()) {
				result2 = JOptionPane.showConfirmDialog(this.frame,
						"Map file " + outfile + " exists.\nAre you sure you want to overwrite?", "Save BSP file", 0);
				if (result2 == 1) {
					return;
				}
				if (this.infile.getCanonicalPath().equals(outfile.getCanonicalPath())) {
					long ilength = this.infile.length();
					File renfile = new File(this.infile.getAbsolutePath() + ".bak");
					Cons.print("Copying current map file to " + renfile.getAbsolutePath() + "...");
					prog.start("Copying current map", true);
					RandomAccessFile copyraf = new RandomAccessFile(renfile, "rw");
					copyraf.setLength(0);
					this.raf.seek(0);
					this.m.blockcopy(this.raf, copyraf, ilength);
					copyraf.close();
					Cons.println("Done");
					this.infile = renfile;
					if (!this.infile.exists()) {
						Cons.println("Cannot find renamed file - map save aborted");
						this.frame.setVisible(false);
						return;
					}
					this.raf.close();
					this.raf = new RandomAccessFile(this.infile, "r");
				}
			}
			Cons.print("Writing " + outfilename + "...");
			this.raf.seek(0);
			RandomAccessFile outraf = new RandomAccessFile(outfile, "rw");
			prog.start("Saving map...", true);
			outraf.setLength(0);
			this.m.setfile(this.raf);
			Cons.print("BSP header... ");
			this.m.saveheader(outraf, entopt);
			Cons.print("Pre-entity data... ");
			prog.setString("Writing pre-entity data...");
			this.m.savepre(outraf);
			Cons.print("Entity data... ");
			prog.setString("Writing entity data...");
			this.m.saveent(outraf);
			Cons.print("Post-entity data... ");
			prog.setString("Writing post-entity data...");
			this.m.savepost(outraf, entopt);
			this.m.saveglumps(outraf);
			outraf.close();
			Cons.println("Done");
			this.raf.close();
			this.infile = outfile;
			this.raf = new RandomAccessFile(this.infile, "r");
			this.filename = this.infile.getName();
			prog.end();
			this.m.setfile(this.raf);
			this.m.dirty = false;
		} catch (IOException ioe) {
			Cons.println(ioe);
		}
	}
	
	public boolean loadfgdfiles(File file) {	
		if(file == null) {
			String lastFgd = preferences.get("LastFGDFile", null);
			if(lastFgd == null)
				return false;
			
			file = new File(lastFgd);
		}
		
		if(!file.exists() || !file.canRead())
			return false;
		
		if(fgdFile == null)
			fgdFile = new FGD();
		
		try(FileReader fr = new FileReader(file)) {
			final String path = file.getParent();
			
			FGD.OnIncludeCallback callback = new FGD.OnIncludeCallback() {
				public Boolean call() {
					File f = new File(path + "/" + this.fileToLoad);
					return loadfgdfiles(f);
				}
			};
			
			fgdFile.loadFromReader(fr, file.getName(), callback);
			fr.close();
		} catch(Exception | LexerException e) {
			JOptionPane.showMessageDialog(frame, e.getMessage(), "ERROR!", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		return true;
	}

	public void loaddata() {
		final JProgFrame prog = new JProgFrame(this.frame, entspyTitle + " - Load BSP file");
		this.m.setprog(prog);
		prog.setMaximum(this.m.loadtasklength());
		prog.start("Loading entities...", true);
		this.frame.setCursor(Cursor.getPredefinedCursor(3));
		final Timer timer = new Timer(100, new ActionListener() {

			public void actionPerformed(ActionEvent evt) {
				prog.setValue(Entspy.this.m.loadtaskprogress());
			}
		});
		SwingWorker worker = new SwingWorker() {

			public Object construct() {
				try {
					Entspy.this.m.loadentities();
					Entspy.this.m.loadglumps();
				} catch (IOException ex) {
					Cons.println(ex);
				}
				return null;
			}

			public void finished() {
				timer.stop();
				Entspy.this.entList.setModel(new EntspyListModel(Entspy.this.m.getData()));
				Entspy.this.frame.setCursor(null);
				prog.end();
				Entspy.this.frame.setTitle(entspyTitle + " - " + Entspy.this.filename);
			}
		};
		timer.start();
		worker.start();
	}

	public boolean checkchanged(String title) {
		if (!this.m.dirty) {
			return false;
		}
		int result = JOptionPane.showConfirmDialog(this.frame,
				new Object[] { "File " + this.filename + " has been edited.", "Discard changes?" }, title, 0);
		if (result == 0) {
			return false;
		}
		return true;
	}

	public String strsize(int bytes) {
		if (bytes < 2000) {
			return "" + bytes + " bytes";
		}
		return new DecimalFormat("0.0").format((float) bytes / 1024.0f) + " kbytes";
	}

	public Entity getSelectedEntity() {
		int index = entList.getSelectedIndex();

		if (index < 0 || index >= m.el.size())
			return null;

		return m.el.get(index);
	}
	
	public ArrayList<Entity> loadEntsFromReader(Reader in) throws Exception {
		BufferedReader r = new BufferedReader(in);
		ArrayList<Entity> ents = new ArrayList<Entity>();
		
		Entity e = null;
		String line = r.readLine();
		int lineIndex = 1;
		
		Pattern p = Pattern.compile("\\s*\"(.*?)\"\\s*\"(.*?)\"");
		while(line != null) {
			if(line.startsWith("{")) {
				if(e != null) throw new Exception("Error! Unexpected '{' at line " + lineIndex + "!");
				e = new Entity();
			} else if(line.startsWith("}")) {
				e.setnames();
				ents.add(e);
				
				e = null;
			} else {
				if(e == null) throw new Exception("Error at line " + lineIndex);
				
				Matcher match = p.matcher(line);
				if(match.find()) {
					e.kvmap.put(match.group(1), e.values.size());
					e.keys.add(match.group(1));
					e.values.add(match.group(2));
					e.links.add(null);
				}
			}
			
			++lineIndex;
			line = r.readLine();
		}
		
		if(e != null) throw new Exception("Unexpected EOF!");
		
		return ents;
	}
	
	public void writeEntsToWriter(List<Entity> ents, Writer out) throws IOException {
		BufferedWriter w = new BufferedWriter(out);
		
		for(Entity e : ents) {
			w.append(e.toStringSpecial());
		}
		
		w.flush();
	}
	
	public static void main(String[] args) throws Exception {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		Entspy inst = new Entspy();
		inst.exec();
	}

	class FindListen implements ActionListener {
		JTextField textf;

		public FindListen(JTextField textf) {
			this.textf = textf;
		}

		public boolean isSpecialMatch(String text, List<String> keys, List<String> values) throws Exception {
			if (!text.startsWith("@") || keys == null || values == null)
				return false;

			Pattern reg = Pattern.compile("@\"(.*?)\"\\s*=\\s*\"(.*?)\"");
			Matcher match = reg.matcher(text);

			boolean valid = false;
			while (match.find()) {
				valid = true;

				keys.add(match.group(1));
				values.add(match.group(2).toLowerCase());
			}

			if (!valid) {
				throw new Exception("Invalid syntax!");
			}

			return true;
		}

		public void actionPerformed(ActionEvent ae) {
			boolean found = false;
			String ftext = this.textf.getText().strip();
			if (ftext.equals("")) {
				return;
			}

			ArrayList<String> keysToSearch = new ArrayList<String>();
			ArrayList<String> valuesToSearch = new ArrayList<String>();
			boolean special = false;

			try {
				special = isSpecialMatch(ftext, keysToSearch, valuesToSearch);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(frame, e.getMessage());
			}

			int i = entList.getSelectedIndex() + 1;
			for(int j = 0; j < 2 && !found; ++j) {
				if (special) {
					for (; i < ((EntspyListModel) entList.getModel()).entities.size(); ++i) {
						if (((EntspyListModel) entList.getModel()).entities.get(i).isMatch(keysToSearch, valuesToSearch)) {
							found = true;
							break;
						}
					}
				} else {
					for (; i < ((EntspyListModel) entList.getModel()).entities.size(); ++i) {
						if (((EntspyListModel) entList.getModel()).entities.get(i).isMatch(ftext)) {
							found = true;
							break;
						}
					}
				}
				
				if(!found)
					i = 0;
			}

			if (found) {
				ArrayList<Integer> selected = new ArrayList<Integer>();

				if ((ae.getModifiers() & ActionEvent.SHIFT_MASK) == 1) {
					for (int j : entList.getSelectedIndices()) {
						selected.add(j);
					}
				}

				selected.add(i);

				int[] indices = new int[selected.size()];

				for (int j = 0; j < selected.size(); ++j) {
					indices[j] = selected.get(j);
				}

				entList.setSelectedIndices(indices);
				entList.ensureIndexIsVisible(indices[indices.length - 1]);
			}
		}
	}
	
	class HelpActionListener implements ActionListener{
		String file;
		public HelpActionListener(String file) {
			this.file = file;
		}
		
		public void actionPerformed(ActionEvent ev) {
			JFrame hframe = new JFrame("Help");
			hframe.setIconImage(Entspy.esIcon.getImage());
			
			JTextPane textp = new JTextPane();
			textp.setEditable(false);
			
			HTMLEditorKit ek = new HTMLEditorKit();
			textp.setEditorKit(ek);
			
			try(BufferedReader rd = new BufferedReader(
					new InputStreamReader(Entspy.class.getResourceAsStream(file)))) {
				StringBuilder sb = new StringBuilder();
				
				String line = rd.readLine();
				while(line != null) {
					sb.append(line);
					line = rd.readLine();
				}
				
				textp.setText(sb.toString());
			} catch (IOException e) {
				textp.setText("Couldn't find " + file + "<br>"+e);
			} catch (NullPointerException e) {
				textp.setText("Couldn't find " + file + "<br>"+e);
			}
			
			textp.setCaretPosition(0);
			JScrollPane scp = new JScrollPane(textp);

			hframe.add(scp);
			scp.getVerticalScrollBar().setValue(0);
			
			hframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			hframe.setSize(720, 520);
			hframe.setVisible(true);
		}
	}

	class FindSelectListen extends FindListen implements ActionListener {
		public FindSelectListen(JTextField textf) {
			super(textf);
		}

		public void actionPerformed(ActionEvent ae) {
			String ftext = this.textf.getText().strip();
			if (ftext.equals("")) {
				return;
			}

			ArrayList<Integer> indices = new ArrayList<Integer>();
			for (int i = 0; i < ((EntspyListModel) entList.getModel()).entities.size(); ++i) {
				if (((EntspyListModel) entList.getModel()).entities.get(i).isMatch(ftext)) {
					indices.add(i);
				}
			}

			if ((ae.getModifiers() & ActionEvent.SHIFT_MASK) == 1) {
				for (int j : entList.getSelectedIndices()) {
					indices.add(j);
				}
			}

			if (indices.size() > 0) {
				int[] arr = new int[indices.size()];

				for (int i = 0; i < indices.size(); ++i) {
					arr[i] = indices.get(i);
				}

				entList.setSelectedIndices(arr);
			}
		}
	}

	class TextListen implements ActionListener {
		int type;

		public TextListen(int type) {
			this.type = type;
		}

		public void actionPerformed(ActionEvent ae) {
			String text = ((JTextField) ae.getSource()).getText().trim();
			Entity selEnt = Entspy.this.getSelectedEntity();
			if (selEnt == null) {
				return;
			}
			selEnt.setDefinedValue(this.type, text);
			((KeyValLinkModel) Entspy.this.table.getModel()).refreshtable();
		}
	}

	class EntspyListModel implements ListModel<Entity> {
		ArrayList<Entity> entities;

		public EntspyListModel(ArrayList<Entity> ents) {
			entities = ents;
		}

		@Override
		public int getSize() {
			return entities.size();
		}

		@Override
		public Entity getElementAt(int index) {
			return entities.get(index);
		}

		@Override
		public void addListDataListener(ListDataListener l) {
		}

		@Override
		public void removeListDataListener(ListDataListener l) {
		}

	}

}
