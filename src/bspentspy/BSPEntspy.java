package bspentspy;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
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
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
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
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import bspentspy.ClassPropertyPanel.GotoEvent;
import bspentspy.Lexer.LexerException;
import util.Cons;
import util.SwingWorker;

public class BSPEntspy {
	BSP m;
	String filename;
	File infile;
	RandomAccessFile raf;
	JFrame frame = null;
	final Entity blank = new Entity("");
	JList<Entity> entList;
	MapInfo info;
	Preferences preferences;
	FGD fgdFile = null;
	HashSet<Entity> previouslySelected = new HashSet<Entity>();
	
	static ImageIcon esIcon = new ImageIcon(JTBRenderer.class.getResource("/images/newicons/entspy.png"));
	public static final String entspyTitle = "BSPEntSpy v1.0";

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
		
		final ClassPropertyPanel rightEntPanel = new ClassPropertyPanel();
		rightEntPanel.fgdContent = fgdFile;
		
		boolean shouldSmartEdit = fgdFile != null && preferences.getBoolean("SmartEdit", false);
		boolean shouldAddDefaultParams = fgdFile != null && preferences.getBoolean("AutoAddParams", false);
		rightEntPanel.setSmartEdit(shouldSmartEdit);
		rightEntPanel.shouldAddDefaultParameters(shouldAddDefaultParams);
		
		JMenu optionmenu = new JMenu("Options");
		
		JCheckBoxMenuItem msmartEditOption = new JCheckBoxMenuItem("Smart Edit");
		msmartEditOption.setToolTipText("If FGD file is loaded Smart Edit can be enabled. See more in Help");
		msmartEditOption.setEnabled(fgdFile != null);
		msmartEditOption.setState(shouldSmartEdit);
		
		JCheckBoxMenuItem maddDefaultOption = new JCheckBoxMenuItem("Auto-add default parameters");
		maddDefaultOption.setToolTipText("If FGD file is loaded the default class parameters will be added but not applied unless edited");
		maddDefaultOption.setEnabled(fgdFile != null);
		maddDefaultOption.setState(shouldAddDefaultParams);
		
		maddDefaultOption.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				rightEntPanel.shouldAddDefaultParameters(maddDefaultOption.getState());
				preferences.putBoolean("AutoAddParams", maddDefaultOption.getState());
			}
		});
		
		msmartEditOption.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				rightEntPanel.setSmartEdit(msmartEditOption.getState());
				preferences.putBoolean("SmartEdit", msmartEditOption.getState());
			}
		});
		
		optionmenu.add(msmartEditOption);
		optionmenu.add(maddDefaultOption);
		
		JMenuBar menubar = new JMenuBar();
		menubar.add(filemenu);
		menubar.add(optionmenu);
		menubar.add(helpmenu);
		this.frame.setJMenuBar(menubar);
		
		mload.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				try {
					if (BSPEntspy.this.checkchanged("Load BSP")) {
						return;
					}
					if (!BSPEntspy.this.loadfile()) {
						return;
					}
					BSPEntspy.this.m = new BSP(BSPEntspy.this.raf);
					BSPEntspy.this.m.loadheader();
					BSPEntspy.this.loaddata();
				} catch (IOException ex) {
					Cons.println(ex);
				}
			}
		});
		msave.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				BSPEntspy.this.frame.setCursor(Cursor.getPredefinedCursor(3));
				SwingWorker worker = new SwingWorker() {

					public Object construct() {
						BSPEntspy.this.savefile();
						return null;
					}

					public void finished() {
						BSPEntspy.this.frame.setTitle(entspyTitle + " - " + BSPEntspy.this.filename);
						BSPEntspy.this.frame.setCursor(null);
					}
				};
				worker.start();
			}

		});
		mquit.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (BSPEntspy.this.checkchanged("Quit Entspy")) {
					return;
				}
				System.exit(0);
			}
		});
		minfo.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				BSPEntspy.this.m.setfile(BSPEntspy.this.raf);
				if (BSPEntspy.this.info != null) {
					BSPEntspy.this.info.dispose();
				}
				BSPEntspy.this.info = new MapInfo(BSPEntspy.this.frame, BSPEntspy.this.m, BSPEntspy.this.filename);
			}
		});
		
		mloadfgd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fgdFile = null;
				
				JFileChooser chooser = new JFileChooser(preferences.get("LastFGDDir", System.getProperty("user.dir")));
				chooser.setDialogTitle(entspyTitle + " - Open FGD File");
				if(chooser.showOpenDialog(frame) == 1)
					return;
				
				File f = chooser.getSelectedFile();
				if(loadfgdfiles(f)) {
					preferences.put("LastFGDFile", f.toString());
					preferences.put("LastFGDDir", f.getAbsolutePath());
					JOptionPane.showMessageDialog(frame, f.getName() + " successfuly loaded. It will load automaticaly on program start.");
				} else {
					preferences.remove("LastFGDFile");
					rightEntPanel.setSmartEdit(false);
					msmartEditOption.setState(false);
				}
				
				msmartEditOption.setEnabled(fgdFile != null);
				maddDefaultOption.setEnabled(fgdFile != null);
				
				rightEntPanel.fgdContent = fgdFile;
			}
		});
		
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
		final JButton findbutton = new JButton("Go to");
		findbutton.setToolTipText("Go to linking entity");
		findpanel.add(findbutton);
		findbutton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Entity jump = ((Entity)findmodel.getSelectedItem());
				
				int ind = m.el.indexOf(jump);
				entList.setSelectedIndex(ind);
				entList.ensureIndexIsVisible(ind);
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
				} catch(Exception | LexerException e) {
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
					if(i > 0)
						++i;
					
					for(Entity e : ents) {
						m.el.add(i++, e);
					}
					
					entList.setModel(new EntspyListModel(m.getData()));
				} catch(Exception | LexerException e) {
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
				for (int i : BSPEntspy.this.entList.getSelectedIndices()) {
					toremove.add(m.el.get(i));
					++j;
				}
				m.el.removeAll(toremove);
				
				j = entList.getMaxSelectionIndex() - j;

				BSPEntspy.this.entList.setModel(new EntspyListModel(BSPEntspy.this.m.getData()));
				
				entList.setSelectedIndex(j + 1);
				
				BSPEntspy.this.m.dirty = true;
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

				BSPEntspy.this.m.dirty = true;
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
				BSPEntspy.this.entList.setModel(new EntspyListModel(m.getData()));
				entList.setSelectedIndex(index);
				entList.ensureIndexIsVisible(index);

			}
		});

		this.entList.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent ev) {
				if(ev.getValueIsAdjusting())
					return;
				int[] selected = entList.getSelectedIndices();
				
				boolean enable = selected.length > 0;
				
				delent.setEnabled(enable);
				cpyent.setEnabled(enable);
				exportEntity.setEnabled(enable);
				cpToClipEnt.setEnabled(enable);
				findbutton.setEnabled(selected.length == 1);
				findcombo.setEnabled(selected.length == 1);
				findmodel.removeAllElements();
				
				/*
				 * Emulates hammer editor behaviour. If a new selection is made
				 * that does not contain the previously selected entities,
				 * then the changes are applied automatically
				 */
				HashSet<Entity> newSelection = new HashSet<Entity>();
				boolean shouldApply = selected.length > 0;
				for(int i : selected) {
					Entity e = m.el.get(i);
					newSelection.add(e);
					
					if(previouslySelected.contains(e)) {
						shouldApply = false;
					}
				}
				previouslySelected = newSelection;
				if(shouldApply) {
					rightEntPanel.apply();
				}
				
				rightEntPanel.clearEntities();
				for(int i : selected) {
					rightEntPanel.addEntity(m.el.get(i), false);
				}
				rightEntPanel.gatherKeyValues();
				
				if(selected.length <= 1) {
					setfindlist(m.el.get(selected[0]), findmodel);
				}
			}

		});
		
		rightEntPanel.addApplyListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int[] selected = entList.getSelectedIndices();
				entList.setModel(new EntspyListModel(m.getData()));
				entList.setSelectedIndices(selected);
			}
		});
		
		rightEntPanel.addGotoListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				GotoEvent ge = (GotoEvent)e;
				String name = ge.entname.trim();
				
				boolean found = false;
				int j = 0;
				for(int i = 0; i < 2 && !found; ++i) {
					for(; j < m.el.size(); ++j) {
						if(m.el.get(j).targetname.equals(name)) {
							entList.setSelectedIndex(j);
							found = true;
							break;
						}
					}
					j = entList.getSelectedIndex() + 1;
				}
				
				if(!found) {
					JOptionPane.showMessageDialog(frame, "Couldn't find entity named '" + name + "'.");
				}
			}
		});
		
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

	public boolean setfindlist(Entity sel, DefaultComboBoxModel<Entity> model) {
		model.removeAllElements();
		loop1: for (int i = 0; i < this.m.el.size(); ++i) {
			Entity lent = this.m.el.get(i);
			if (lent.keyvalues == null)
				continue;
			for (int j = 0; j < lent.keyvalues.size(); ++j) {
				Entity linkent = lent.keyvalues.get(j).link;
				if (linkent == null || !linkent.targetname.equals(sel.targetname))
					continue;
				model.addElement(lent);
				continue loop1;
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
			fgdFile = null;
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
				prog.setValue(BSPEntspy.this.m.loadtaskprogress());
			}
		});
		SwingWorker worker = new SwingWorker() {

			public Object construct() {
				try {
					BSPEntspy.this.m.loadentities();
					BSPEntspy.this.m.loadglumps();
				} catch (IOException ex) {
					Cons.println(ex);
				}
				return null;
			}

			public void finished() {
				timer.stop();
				BSPEntspy.this.entList.setModel(new EntspyListModel(BSPEntspy.this.m.getData()));
				BSPEntspy.this.frame.setCursor(null);
				prog.end();
				BSPEntspy.this.frame.setTitle(entspyTitle + " - " + BSPEntspy.this.filename);
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
	
	public ArrayList<Entity> loadEntsFromReader(Reader in) throws Exception, LexerException {
		VMF temp = new VMF();
		
		final HashSet<String> ignoredClasses = new HashSet<String>();
		//these entity classes get removed during VBSP, these should not be present in final BSP file
		ignoredClasses.add("prop_static");
		ignoredClasses.add("prop_detail");
		ignoredClasses.add("func_instance");
		ignoredClasses.add("prop_detail_sprite");
		ignoredClasses.add("env_cubemap");
		ignoredClasses.add("info_lighting");
		ignoredClasses.add("func_detail");
		ignoredClasses.add("func_ladder");
		ignoredClasses.add("func_viscluster");
		
		temp.loadFromReader(in, "clipboard");
		
		for(int i = 0; i < temp.ents.size(); ++i) {
			if(ignoredClasses.contains(temp.ents.get(i).classname)) {
				temp.ents.remove(i--);
			}
		}
		
		return temp.ents;
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
		BSPEntspy inst = new BSPEntspy();
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
			String ftext = this.textf.getText().trim();
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
			HelpWindow help = HelpWindow.openHelp("Help");
			
			try(BufferedReader rd = new BufferedReader(
					new InputStreamReader(BSPEntspy.class.getResourceAsStream(file)))) {
				StringBuilder sb = new StringBuilder();
				
				String line = rd.readLine();
				while(line != null) {
					sb.append(line);
					line = rd.readLine();
				}
				
				help.setText(sb.toString());
			} catch (IOException | NullPointerException e) {
				help.setText("Couldn't find " + file + "<br>"+e);
			}
			
			help.setSize(720, 520);
			help.setVisible(true);
		}
	}

	class FindSelectListen extends FindListen implements ActionListener {
		public FindSelectListen(JTextField textf) {
			super(textf);
		}

		public void actionPerformed(ActionEvent ae) {
			String ftext = this.textf.getText().trim();
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
