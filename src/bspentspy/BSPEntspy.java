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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.prefs.Preferences;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
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
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import bspentspy.ClassPropertyPanel.GotoEvent;
import bspentspy.Entity.KeyValLink;
import bspentspy.Lexer.LexerException;
import bspentspy.Undo.Command;
import util.SwingWorker;

public class BSPEntspy {
	BSP m;
	String filename;
	File infile;
	RandomAccessFile raf;
	JFrame frame = null;
	JList<Entity> entList;
	FilteredEntListModel entModel;
	MapInfo info;
	Preferences preferences;
	FGD fgdFile = null;
	HashSet<Entity> previouslySelected = new HashSet<Entity>();
	Obfuscator obfuscator;

	static ImageIcon esIcon = new ImageIcon(BSPEntspy.class.getResource("/images/newicons/entspy.png"));
	public static final String entspyTitle = "BSPEntSpy v1.275";
	
	private void updateEntList(ArrayList<Entity> ents) {
		entModel.setEntityList(ents);
		entList.setModel(entModel);
	}

	public int exec() throws IOException {
		preferences = Preferences.userRoot().node(getClass().getName());

		this.frame = new JFrame(entspyTitle);
		this.frame.setIconImage(esIcon.getImage());
		if (!this.loadfile()) {
			System.exit(0);
		}
		this.m = new BSP(this.raf);
		this.m.loadheader();

		if (loadfgdfiles(null)) {
			System.out.println("FGD loaded: " + String.join(", ", fgdFile.loadedFgds));
		} else {
			preferences.remove("LastFGDFile");
		}
		
		DefaultListModel<Entity> dfm = new DefaultListModel<Entity>();
		dfm.add(0, new Entity());
		
		entModel = new FilteredEntListModel();
		this.entList = new JList<Entity>();
		DefaultListSelectionModel selmodel = new DefaultListSelectionModel();
		selmodel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		entList.setSelectionModel(selmodel);
		entList.setCellRenderer(new EntListRenderer());

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

		JMenuItem mpatchvmf = new JMenuItem("Patch from VMF");
		mpatchvmf.setToolTipText("Update entity properties based on a VMF file (see more in Help)");
		filemenu.add(mpatchvmf);

		filemenu.addSeparator();

		JMenuItem mloadfgd = new JMenuItem("Load FGD file");
		mloadfgd.setToolTipText("Load an FGD file to enable Smart Edit");
		filemenu.add(mloadfgd);

		filemenu.addSeparator();
		JMenuItem minfo = new JMenuItem("Map info...");
		minfo.setToolTipText("Map header information");
		filemenu.add(minfo);
		filemenu.addSeparator();
		JMenuItem mquit = new JMenuItem("Quit");
		mquit.setToolTipText("Quit Entspy");
		mquit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK));
		filemenu.add(mquit);

		JMenu editmenu = new JMenu("Edit");
		JMenuItem mUndo = new JMenuItem("Undo");
		mUndo.setToolTipText("Undo last edit");
		mUndo.setEnabled(false);
		mUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK));

		JMenuItem mRedo = new JMenuItem("Redo");
		mRedo.setEnabled(false);
		mRedo.setToolTipText("Redo last edit");
		mRedo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK));

		mUndo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				Undo.undo();
				updateEntList(m.getData());
				mRedo.setEnabled(Undo.canRedo());
				mUndo.setEnabled(Undo.canUndo());
			}
		});
		mRedo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				Undo.redo();
				updateEntList(m.getData());
				mRedo.setEnabled(Undo.canRedo());
				mUndo.setEnabled(Undo.canUndo());
			}
		});
		editmenu.add(mUndo);
		editmenu.add(mRedo);

		Undo.addUpdateListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				mUndo.setEnabled(Undo.canUndo());
				mRedo.setEnabled(Undo.canRedo());
			}
		});
		
		JMenuItem mInvertSel = new JMenuItem("Invert selection");
		mInvertSel.setEnabled(true);
		mInvertSel.setToolTipText("Invert the selection");
		mInvertSel.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_DOWN_MASK));
		editmenu.addSeparator();
		editmenu.add(mInvertSel);
		
		mInvertSel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				int[] selected = entList.getSelectedIndices();
				
				entList.clearSelection();
				
				int j = 0;
				int intervalStart = -1;
				int intervalEnd = 0;
				
				for(int i = 0; i < entModel.getSize(); ++i) {
					if(j < selected.length && selected[j] == i) {
						entList.addSelectionInterval(intervalStart, intervalEnd - 1);
						intervalStart = -1;
						++j;
						continue;
					}
					
					if(intervalStart < 0) {
						intervalStart = i;
						intervalEnd = i;
					}
					
					++intervalEnd;
				}
				
				entList.addSelectionInterval(intervalStart, intervalEnd - 1);
			}
		});

		JMenu helpmenu = new JMenu("Help");
		JMenuItem mhelpSearch = new JMenuItem("Search help");
		helpmenu.add(mhelpSearch);

		mhelpSearch.addActionListener(new HelpActionListener("/text/searchhelp.html"));

		JMenuItem mexportHelp = new JMenuItem("Export / Import help");
		helpmenu.add(mexportHelp);

		mexportHelp.addActionListener(new HelpActionListener("/text/exporthelp.html"));

		JMenuItem mpatchHelp = new JMenuItem("Patching help");
		helpmenu.add(mpatchHelp);

		mpatchHelp.addActionListener(new HelpActionListener("/text/patchhelp.html"));

		JMenuItem fgdhelp = new JMenuItem("FGD help");
		helpmenu.add(fgdhelp);

		fgdhelp.addActionListener(new HelpActionListener("/text/fgdhelp.html"));

		helpmenu.addSeparator();

		JMenuItem mcreditHelp = new JMenuItem("Credits");
		helpmenu.add(mcreditHelp);

		mcreditHelp.addActionListener(new HelpActionListener("/text/credits.html"));

		final ClassPropertyPanel rightEntPanel = new ClassPropertyPanel();
		rightEntPanel.setFGD(fgdFile);

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
		maddDefaultOption.setToolTipText(
				"If FGD file is loaded the default class parameters will be added but not applied unless edited");
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
		
		JMenu entitymenu = new JMenu("Entity");
		final JMenuItem importEntity = new JMenuItem("Import");
		importEntity.setToolTipText("Import entities from a file");
		importEntity.setEnabled(true);
		entitymenu.add(importEntity);

		final JMenuItem exportEntity = new JMenuItem("Export");
		exportEntity.setToolTipText("Export selected entities to a file");
		exportEntity.setEnabled(false);
		entitymenu.add(exportEntity);
		
		/*final JMenuItem obfEntity = new JMenuItem("Obfuscate");
		obfEntity.setToolTipText("Obfuscate selected entities (see more in Help)");
		obfEntity.setEnabled(false);
		entitymenu.add(obfEntity); //Work in progress*/

		JMenuBar menubar = new JMenuBar();
		menubar.add(filemenu);
		menubar.add(editmenu);
		menubar.add(entitymenu);
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
					ex.printStackTrace();
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

		mpatchvmf.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser(
						preferences.get("LastVMFFolder", System.getProperty("user.dir")));
				chooser.setDialogTitle(entspyTitle + " - Open FGD File");
				if (chooser.showOpenDialog(frame) == 1)
					return;

				File f = chooser.getSelectedFile();
				try {
					patchFromVMF(f);
				} catch (FileNotFoundException | LexerException e1) {
					JOptionPane.showMessageDialog(frame, "Error while loading VMF: " + e1.getMessage(), "ERROR!",
							JOptionPane.ERROR_MESSAGE);
					e1.printStackTrace();
				}

				preferences.put("LastVMFFolder", f.getParent());
				updateEntList(m.getData());
			}

		});

		mquit.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (BSPEntspy.this.checkchanged("Quit Entspy")) {
					return;
				}
				frame.dispose();
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
				if (chooser.showOpenDialog(frame) == 1)
					return;

				File f = chooser.getSelectedFile();
				if (loadfgdfiles(f)) {
					preferences.put("LastFGDFile", f.toString());
					preferences.put("LastFGDDir", f.getAbsolutePath());
					JOptionPane.showMessageDialog(frame,
							f.getName() + " successfuly loaded. It will load automaticaly on program start.");
				} else {
					preferences.remove("LastFGDFile");
					rightEntPanel.setSmartEdit(false);
					msmartEditOption.setState(false);
				}

				msmartEditOption.setEnabled(fgdFile != null);
				maddDefaultOption.setEnabled(fgdFile != null);

				rightEntPanel.setFGD(fgdFile);
			}
		});
		
		obfuscator = new Obfuscator();
		/*obfEntity.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				ArrayList<Entity> ents = getSelectedEntities();
				
				if(fgdFile == null) {
					int result = JOptionPane.showConfirmDialog(frame, "No FGD files loaded! Obfuscator's functionality will be limited to only name mangling. Do you want to continue?");
					
					if(result != 0)
						return;
				}
				
				obfuscator.setFGD(fgdFile);
				obfuscator.obfuscate(m.el, ents);
			}
		});*/

		JPanel findpanel = new JPanel();

		final JLabel findlabel = new JLabel("Linked from ");
		findpanel.add(findlabel);
		final DefaultComboBoxModel<Entity> findmodel = new DefaultComboBoxModel<Entity>();
		final JComboBox<Entity> findcombo = new JComboBox<Entity>(findmodel);
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
				Entity jump = ((Entity) findmodel.getSelectedItem());
				
				if(jump == null)
					return;
				
				int ind = m.el.indexOf(jump);
				
				if(entModel.indexOf(ind) > -1) {
					entList.setSelectedIndex(ind);
					entList.ensureIndexIsVisible(ind);
				} else {
					rightEntPanel.apply();
					rightEntPanel.setEntity(jump);
				}
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

		exportEntity.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				ArrayList<Entity> ents = getSelectedEntities();
				
				JFileChooser chooser = new JFileChooser(preferences.get("LastFolder", System.getProperty("user.dir")));
				chooser.setDialogTitle(entspyTitle + " - Export entities to a file");
				chooser.setDialogType(JFileChooser.SAVE_DIALOG);

				int result = chooser.showOpenDialog(frame);
				if (result == 1) {
					return;
				}

				File f = chooser.getSelectedFile();

				String ext = ".ent";
				if (f.getName().indexOf('.') > -1)
					ext = "";

				try (FileWriter fw = new FileWriter(f + ext)) {
					writeEntsToWriter(ents, fw);

					fw.close();

					preferences.put("LastFolder", f.getParent());
					JOptionPane.showMessageDialog(frame, ents.size() + " entities successfuly exported to " + f, "Info",
							JOptionPane.INFORMATION_MESSAGE);
				} catch (IOException e) {
					JOptionPane.showMessageDialog(frame, e.getMessage(), "ERROR!", JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
				}

				return;
			}
		});

		importEntity.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				JFileChooser chooser = new JFileChooser(preferences.get("LastFolder", System.getProperty("user.dir")));
				chooser.setDialogTitle(entspyTitle + " - Import entities from a file");
				chooser.setDialogType(JFileChooser.SAVE_DIALOG);

				int result = chooser.showOpenDialog(frame);
				if (result == 1) {
					return;
				}

				File f = chooser.getSelectedFile();

				try (FileReader fr = new FileReader(f)) {
					ArrayList<Entity> ents = loadEntsFromReader(fr);

					fr.close();

					CommandAddEntity command = new CommandAddEntity();
					for (Entity e : ents) {
						command.addEntity(e, m.el.size());
						m.el.add(e);
					}
					Undo.create();
					Undo.setTarget(m.el);
					Undo.addCommand(command);
					Undo.finish();
					
					updateEntList(m.getData());
					preferences.put("LastFolder", f.getParent());

					JOptionPane.showMessageDialog(frame, ents.size() + " entities successfuly imported from " + f,
							"Info", JOptionPane.INFORMATION_MESSAGE);
				} catch (Exception | LexerException e) {
					JOptionPane.showMessageDialog(frame, e.getMessage(), "ERROR!", JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
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
				ArrayList<Entity> ents = getSelectedEntities();

				try (StringWriter sw = new StringWriter(ents.size() * 256)) {
					writeEntsToWriter(ents, sw);

					sw.close();

					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					cb.setContents(new StringSelection(sw.toString()), null);
				} catch (IOException e) {
					JOptionPane.showMessageDialog(frame, e.getMessage(), "ERROR!", JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
				}

				return;
			}
		});

		pstFromClipEnt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				try {
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					Transferable cbcontent = cb.getContents(null);

					if (cbcontent == null)
						return;

					StringReader sr = new StringReader(cbcontent.getTransferData(DataFlavor.stringFlavor).toString());
					ArrayList<Entity> ents = loadEntsFromReader(sr);

					sr.close();
					
					int i = entList.getMaxSelectionIndex();
					
					if(i > -1)
						i = Math.min(i + 1, entModel.getSize() - 1);
					else
						i = entModel.getSize() - 1;
					
					int[] selectedIndices = new int[ents.size()];
					int j = 0;
					CommandAddEntity command = new CommandAddEntity();
					for (Entity e : ents) {
						int originalIndex = entModel.getIndexAt(i);
						command.addEntity(e, originalIndex);
						selectedIndices[j++] = i++;
						m.el.add(originalIndex, e);
					}
					Undo.create();
					Undo.setTarget(m.el);
					Undo.addCommand(command);
					Undo.finish();
					
					updateEntList(m.getData());
					
					entList.setSelectedIndices(selectedIndices);
				} catch (Exception | LexerException e) {
					JOptionPane.showMessageDialog(frame, "Could not parse data from clipboard!\n" + e.getMessage(),
							"ERROR!", JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
				}

				return;
			}
		});

		JButton findent = new JButton("Find");
		findent.setToolTipText("Find and select next entity, hold Shift to select all");
		findent.setMnemonic(KeyEvent.VK_F);
		JButton filterEnt = new JButton("Filter");
		filterEnt.setToolTipText("Filter the entitiy list, hold Shift to clear the filter");
		JTextField findtext = new JTextField();
		findtext.setToolTipText("Text to search for");
		Box fbox = Box.createHorizontalBox();

		fbox.add(findtext);
		fbox.add(findent);
		fbox.add(filterEnt);

		entcpl.add((Component) fbox);
		entcpl.add((Component) entbut);
		entcpl.add((Component) entexp);

		leftPanel.add((Component) entcpl, "South");
		findent.addActionListener(new GotoListen(findtext));
		findtext.addActionListener(new FilterListen(findtext));
		filterEnt.addActionListener(new FilterListen(findtext));
		updent.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateEntList(m.getData());
			}
		});
		delent.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				CommandRemoveEntity command = new CommandRemoveEntity();
				
				int[] selected = entList.getSelectedIndices();
				for(int i = 0; i < selected.length; ++i) {
					int index = entModel.getIndexAt(selected[i]);
					command.addEntity(m.el.get(index), index);
					selected[i] = index;
				}
				
				for(int i = selected.length - 1; i >= 0; --i) {
					m.el.remove(selected[i]);
				}
				
				Undo.create();
				Undo.setTarget(m.el);
				Undo.addCommand(command);
				Undo.finish();
				
				int j = entList.getMaxSelectionIndex() - selected.length;
				updateEntList(m.getData());

				entList.setSelectedIndex(j + 1);

				m.dirty = true;
			}
		});
		cpyent.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				int[] selected = entList.getSelectedIndices();
				int[] transIndices = entModel.translateIndices(selected);

				CommandAddEntity command = new CommandAddEntity();
				for (int j = 0; j < selected.length; ++j) {
					selected[j] += j;
					transIndices[j] += j;
					Entity old = m.el.get(transIndices[j]);
					++selected[j];
					++transIndices[j];

					if (old != null) {
						Entity newE = old.copy();
						command.addEntity(newE, transIndices[j]);
						m.el.add(transIndices[j], newE);
					}
				}
				Undo.create();
				Undo.setTarget(m.el);
				Undo.addCommand(command);
				Undo.finish();
				
				updateEntList(m.getData());
				entList.setSelectedIndices(selected);

				BSPEntspy.this.m.dirty = true;
			}
		});
		addent.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				Entity newent = new Entity();
				int index = 0;

				CommandAddEntity command;
				if (entList.getMaxSelectionIndex() > 0) {
					index = entList.getMaxSelectionIndex() + 1;
					command = new CommandAddEntity(newent, index);
					m.el.add(index, newent);
				} else {
					index = m.el.size() - 1;
					command = new CommandAddEntity(newent, m.el.size());
					m.el.add(newent);
				}
				Undo.create();
				Undo.setTarget(m.el);
				Undo.addCommand(command);
				Undo.finish();

				newent.autoedit = true;
				
				updateEntList(m.getData());
				entList.setSelectedIndex(index);
				entList.ensureIndexIsVisible(index);

			}
		});

		this.entList.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent ev) {
				if (ev.getValueIsAdjusting())
					return;
				int[] selected = entList.getSelectedIndices();

				boolean enable = selected.length > 0;

				delent.setEnabled(enable);
				cpyent.setEnabled(enable);
				exportEntity.setEnabled(enable);
				cpToClipEnt.setEnabled(enable);
				//obfEntity.setEnabled(enable);
				findbutton.setEnabled(selected.length == 1);
				findcombo.setEnabled(selected.length == 1);
				findmodel.removeAllElements();

				/*
				 * Emulates hammer editor behaviour. If a new selection is made that does not
				 * contain the previously selected entities, then the changes are applied
				 * automatically
				 */
				HashSet<Entity> newSelection = new HashSet<Entity>();
				boolean shouldApply = selected.length > 0 && previouslySelected.size() > 0;
				for (int i : selected) {
					Entity e = entModel.getElementAt(i);
					newSelection.add(e);

					if (previouslySelected.contains(e)) {
						shouldApply = false;
					}
				}
				previouslySelected = newSelection;
				if (shouldApply) {
					rightEntPanel.apply();
				}

				rightEntPanel.clearEntities();
				for (int i : selected) {
					rightEntPanel.addEntity(entModel.getElementAt(i), false);
				}
				rightEntPanel.gatherKeyValues();

				if (selected.length == 1) {
					setfindlist(entModel.getElementAt(selected[0]), findmodel);
				}
			}

		});

		rightEntPanel.addApplyListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int[] selected = entList.getSelectedIndices();
				updateEntList(m.getData());
				entList.setSelectedIndices(selected);
			}
		});

		rightEntPanel.addGotoListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				GotoEvent ge = (GotoEvent) e;
				String name = ge.entname.trim();

				boolean found = false;
				int j = 0;
				for (int i = 0; i < 2 && !found; ++i) {
					for (; j < m.el.size(); ++j) {
						if (m.el.get(j).targetname.equals(name)) {
							entList.setSelectedIndex(j);
							found = true;
							break;
						}
					}
					j = entList.getSelectedIndex() + 1;
				}

				if (!found) {
					JOptionPane.showMessageDialog(frame, "Couldn't find entity named '" + name + "'.");
				}
			}
		});

		JPanel rightPanel = new JPanel(new BorderLayout());
		rightPanel.add((Component) findpanel, "South");
		rightPanel.add((Component) rightEntPanel, "Center");

		JSplitPane mainSplit = new JSplitPane(1, leftPanel, rightPanel);
		mainSplit.setDividerLocation(275);

		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent ev) {
				if (BSPEntspy.this.checkchanged("Quit Entspy")) {
					return;
				}
				frame.dispose();
			}
		});
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
				System.out.println("Can't read " + this.filename + "!");
				return false;
			}
			System.out.println("Reading map file " + this.filename);
			this.raf = new RandomAccessFile(this.infile, "r");

			preferences.put("LastFolder", this.infile.getParent());

			return true;
		} catch (IOException ioe) {
			ioe.printStackTrace();
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
					System.out.print("Copying current map file to " + renfile.getAbsolutePath() + "...");
					prog.start("Copying current map", true);
					RandomAccessFile copyraf = new RandomAccessFile(renfile, "rw");
					copyraf.setLength(0);
					this.raf.seek(0);
					this.m.blockcopy(this.raf, copyraf, ilength);
					copyraf.close();
					System.out.println("Done");
					this.infile = renfile;
					if (!this.infile.exists()) {
						System.out.println("Cannot find renamed file - map save aborted");
						this.frame.setVisible(false);
						return;
					}
					this.raf.close();
					this.raf = new RandomAccessFile(this.infile, "r");
				}
			}
			System.out.print("Writing " + outfilename + "...");
			this.raf.seek(0);
			RandomAccessFile outraf = new RandomAccessFile(outfile, "rw");
			prog.start("Saving map...", true);
			outraf.setLength(0);
			this.m.setfile(this.raf);
			System.out.print("BSP header... ");
			this.m.saveheader(outraf, entopt);
			System.out.print("Pre-entity data... ");
			prog.setString("Writing pre-entity data...");
			this.m.savepre(outraf);
			System.out.print("Entity data... ");
			prog.setString("Writing entity data...");
			this.m.saveent(outraf);
			System.out.print("Post-entity data... ");
			prog.setString("Writing post-entity data...");
			this.m.savepost(outraf, entopt);
			this.m.saveglumps(outraf);
			outraf.close();
			System.out.println("Done");
			this.raf.close();
			this.infile = outfile;
			this.raf = new RandomAccessFile(this.infile, "r");
			this.filename = this.infile.getName();
			prog.end();
			this.m.setfile(this.raf);
			this.m.dirty = false;
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public boolean loadfgdfiles(File file) {
		if (file == null) {
			String lastFgd = preferences.get("LastFGDFile", null);
			if (lastFgd == null)
				return false;

			file = new File(lastFgd);
		}

		if (!file.exists() || !file.canRead())
			return false;

		if (fgdFile == null)
			fgdFile = new FGD();

		try (FileReader fr = new FileReader(file)) {
			final String path = file.getParent();

			FGD.OnIncludeCallback callback = new FGD.OnIncludeCallback() {
				public Boolean call() {
					File f = new File(path + "/" + this.fileToLoad);
					return loadfgdfiles(f);
				}
			};

			fgdFile.loadFromReader(fr, file.getName(), callback);
			fr.close();
		} catch (Exception | LexerException e) {
			JOptionPane.showMessageDialog(frame, e.getMessage(), "ERROR!", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
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
					ex.printStackTrace();
				}
				return null;
			}

			public void finished() {
				timer.stop();
				updateEntList(m.getData());
				frame.setCursor(null);
				prog.end();
				frame.setTitle(entspyTitle + " - " + BSPEntspy.this.filename);
			}
		};
		timer.start();
		worker.start();
	}

	public boolean checkchanged(String title) {
		if (!this.m.dirty && Undo.isEmpty()) {
			return false;
		}
		int result = JOptionPane.showConfirmDialog(frame,
				new Object[] { "File " + filename + " has been edited.", "Discard changes?" }, title, 0);
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
	
	public ArrayList<Entity> getSelectedEntities(){
		ArrayList<Entity> ents = new ArrayList<Entity>();

		for (int i : entList.getSelectedIndices()) {
			ents.add(entModel.getElementAt(i));
		}
		
		return ents;
	}

	public boolean patchFromVMF(File vmfFile) throws LexerException, FileNotFoundException {
		VMF temp = new VMF();

		FileReader fr = new FileReader(vmfFile);
		temp.loadFromReader(fr, vmfFile.getName());

		Object[] options = new Object[] { "Only named entities", "All entities" };
		Object[] options2 = new Object[] { "Continue", "Continue all", "Abort" };

		int selected = JOptionPane.showOptionDialog(frame,
				"Patch only named entities (safe) or try to patch entities based on order?", "Patch VMF",
				JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		boolean ignoreDisorder = false;

		int replaced = 0;
		int missmatches = 0;
		CommandReplaceEntity command = new CommandReplaceEntity();
		if (selected == 0) {
			HashMap<String, Integer> nameMap = new HashMap<String, Integer>();
			HashMap<String, Integer> dupMap = new HashMap<String, Integer>();

			for (int i = 0; i < m.el.size(); ++i) {
				Entity ent = m.el.get(i);
				if (ent.targetname.isEmpty())
					continue;
				String key = "\"" + ent.classname + "\" \"" + ent.targetname + "\"";

				if (nameMap.containsKey(key)) {
					int count = dupMap.getOrDefault(key, 1);
					dupMap.put(key, count + 1);
					key += count;
				}

				nameMap.put(key, i);
			}

			dupMap.clear();
			for (int i = 0; i < temp.ents.size(); ++i) {
				Entity ent = temp.ents.get(i);
				if (ent.targetname.isEmpty())
					continue;
				String key = "\"" + ent.classname + "\" \"" + ent.targetname + "\"";

				int count = dupMap.getOrDefault(key, -1);
				dupMap.put(key, ++count);

				if (count > 0) {
					key += count;
				}

				if (!nameMap.containsKey(key)) {
					continue;
				}

				int index = nameMap.get(key);
				Entity original = m.el.get(index);
				Entity finalReplacement = replaceEntity(original, ent);
				m.el.set(index, finalReplacement);
				command.addEntity(original, finalReplacement, index);
				++replaced;
			}
		} else {
			for (int i = 0, j = 0; i < m.el.size() && j < temp.ents.size();) {
				Entity original = m.el.get(i);
				Entity replacement = temp.ents.get(j);

				if (VMF.ignoredClasses.contains(original.classname)) {
					++i;
					continue;
				}
				if (VMF.ignoredClasses.contains(replacement.classname)
						|| selected == 0 && replacement.targetname.isEmpty()) {
					++j;
					continue;
				}

				// only classname needs to match
				boolean shouldReplace = original.classname.equals(replacement.classname);

				if (!shouldReplace)
					++missmatches;

				if (selected == 1 && !(ignoreDisorder || shouldReplace)) {
					int selected2 = JOptionPane.showOptionDialog(frame,
							"Entity mismatch detected (" + original.classname + "[\"" + original.targetname + "\"], "
									+ replacement.classname + "[\"" + replacement.targetname + "\"])!\n"
									+ "The program will try to match entities as best as possible. Continue?",
							"Patch VMF", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options2,
							options2[1]);
					ignoreDisorder = selected2 == 1;

					if (selected2 == 2)
						break;
				}

				if (shouldReplace) {
					Entity finalReplacement = replaceEntity(original, replacement);
					m.el.set(i, finalReplacement);
					command.addEntity(original, finalReplacement, i);

					++i;
					++j;
					++replaced;
				} else // if entities do not match advance bsp entities until they match with vmf
						// entities
					++i;
			}
		}

		Undo.create();
		Undo.setTarget(m.el);
		Undo.addCommand(command);
		Undo.finish();

		JOptionPane.showMessageDialog(frame, "Patched " + replaced + " entities (" + missmatches + " mismatches).");

		return false;
	}

	private static Entity replaceEntity(Entity original, Entity replacement) {
		for (KeyValLink kvl : original.keyvalues) {
			if (!replacement.kvmap.containsKey(kvl.key)) {
				replacement.addKeyVal(kvl.key, kvl.value);
			}

			if (kvl.key.equals("model") && kvl.value.startsWith("*"))
				replacement.setKeyVal("model", kvl.value);
		}
		return replacement;
	}

	public ArrayList<Entity> loadEntsFromReader(Reader in) throws Exception, LexerException {
		VMF temp = new VMF();

		temp.loadFromReader(in, "clipboard");

		for (int i = 0; i < temp.ents.size(); ++i) {
			if (VMF.ignoredClasses.contains(temp.ents.get(i).classname)) {
				temp.ents.remove(i--);
			}
		}

		return temp.ents;
	}

	public void writeEntsToWriter(List<Entity> ents, Writer out) throws IOException {
		BufferedWriter w = new BufferedWriter(out);

		for (Entity e : ents) {
			w.append(e.toStringSpecial());
		}

		w.flush();
	}

	public static void main(String[] args) throws Exception {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

		BSPEntspy inst = new BSPEntspy();
		inst.exec();
	}

	class FilterListen implements ActionListener {
		JTextField textf;

		public FilterListen(JTextField textf) {
			this.textf = textf;
		}

		public void actionPerformed(ActionEvent ae) {
			entList.clearSelection();
			String ftext = textf.getText().trim();
			if(ftext.equals("") || (ae.getModifiers() & ActionEvent.SHIFT_MASK) > 0) {
				entModel.setFilter(null);
				return;
			}
			try {
				entModel.setFilter(SimpleFilter.create(ftext));
			} catch(Exception e) {
				JOptionPane.showMessageDialog(frame, "Invalid filter format!", "ERROR", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	class GotoListen implements ActionListener {
		JTextField textf;

		public GotoListen(JTextField textf) {
			this.textf = textf;
		}

		public void actionPerformed(ActionEvent ae) {
			String ftext = textf.getText().trim();
			if(ftext.equals("")) {
				return;
			}
			
			IFilter filter;
			try {
				filter = SimpleFilter.create(ftext);
			} catch(Exception e) {
				JOptionPane.showMessageDialog(frame, "Invalid filter format!", "ERROR", JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			if((ae.getModifiers() & ActionEvent.SHIFT_MASK) > 0) {
				List<Entity> filtered = entModel.getFilteredEntities();
				
				for(int i = 0; i < filtered.size(); ++i) {
					if(filter.match(filtered.get(i))) {
						entList.addSelectionInterval(i, i);
					}
				}
			} else {
				int found = -1;
				int j = entList.getMaxSelectionIndex() + 1;
				int k = entModel.getSize();
				for(int i = 0; i < 2 && found < 0; ++i) {
					List<Entity> filtered = entModel.getFilteredEntities();
					
					for(; j < k; ++j) {
						if(filter.match(filtered.get(j))) {
							found = j;
							break;
						}
					}
					
					j = 0;
					k = entList.getMinSelectionIndex();
				}
				
				if(found > -1) {
					entList.setSelectedIndex(found);
					entList.ensureIndexIsVisible(found);
				}
			}
		}
	}

	class HelpActionListener implements ActionListener {
		String file;

		public HelpActionListener(String file) {
			this.file = file;
		}

		public void actionPerformed(ActionEvent ev) {
			HelpWindow help = HelpWindow.openHelp("Help");

			try (BufferedReader rd = new BufferedReader(
					new InputStreamReader(BSPEntspy.class.getResourceAsStream(file)))) {
				StringBuilder sb = new StringBuilder();

				String line = rd.readLine();
				while (line != null) {
					sb.append(line);
					line = rd.readLine();
				}

				help.setText(sb.toString());
			} catch (IOException | NullPointerException e) {
				help.setText("Couldn't find " + file + "<br>" + e);
				e.printStackTrace();
			}

			help.setSize(720, 520);
			help.setVisible(true);
		}
	}

	private static abstract class CommandEntity implements Command {
		ArrayList<Entity> entities;

		public CommandEntity() {
			entities = new ArrayList<Entity>();
		}

		public CommandEntity(Entity e) {
			entities = new ArrayList<Entity>();
			entities.add(e);
		}

		public Command join(Command previous) {
			CommandEntity prev = (CommandEntity) previous;
			prev.entities.addAll(entities);

			return null;
		}

		public int size() {
			return entities.size();
		}
	}

	private static class CommandReplaceEntity extends CommandEntity {
		ArrayList<Entity> replacements;
		ArrayList<Integer> indices;

		public CommandReplaceEntity() {
			indices = new ArrayList<Integer>();
			replacements = new ArrayList<Entity>();
		}

		public void addEntity(Entity original, Entity replacement, int index) {
			indices.add(index);
			entities.add(original);
			replacements.add(replacement);
		}

		public void undo(Object target) {
			ListIterator<Integer> it = indices.listIterator();

			while (it.hasNext()) {
				int index = it.nextIndex();
				int entindex = it.next();
				((ArrayList<Entity>) target).set(entindex, entities.get(index));
			}
		}

		public void redo(Object target) {
			ListIterator<Integer> it = indices.listIterator();

			while (it.hasNext()) {
				int index = it.nextIndex();
				int entindex = it.next();
				((ArrayList<Entity>) target).set(entindex, replacements.get(index));
			}
		}

		public String toString(String indent) {
			return "";
		}
	}

	private static class CommandAddEntity extends CommandEntity {
		ArrayList<Integer> indices;

		public CommandAddEntity() {
			indices = new ArrayList<Integer>();
		}

		public CommandAddEntity(Entity e, int index) {
			super(e);
			indices = new ArrayList<Integer>();
			indices.add(index);
		}

		public void addEntity(Entity e, int index) {
			entities.add(e);
			indices.add(index);
		}

		public void undo(Object target) {
			ListIterator<Integer> it = indices.listIterator();
			int offset = 0;

			while (it.hasNext()) {
				((ArrayList<Entity>) target).remove(it.next().intValue() - offset++);
			}
		}

		public void redo(Object target) {
			ListIterator<Entity> it = entities.listIterator();
			while (it.hasNext()) {
				((ArrayList<Entity>) target).add(indices.get(it.nextIndex()), it.next());
			}
		}

		public String toString(String indent) {
			StringBuilder sb = new StringBuilder();
			System.out.println("ident" + indent);

			sb.append(indent).append(this.getClass()).append("\n");

			for (int i = 0; i < entities.size(); ++i) {
				sb.append(indent).append("\t\t").append(entities.get(i).toString()).append(" at index ")
						.append(indices.get(i)).append("\n");
			}

			return sb.toString();
		}
	}

	private static class CommandRemoveEntity extends CommandAddEntity {
		public CommandRemoveEntity() {

		}

		public CommandRemoveEntity(Entity e, int index) {
			super(e, index);
		}

		public void undo(Object target) {
			ListIterator<Entity> it = entities.listIterator();

			while (it.hasNext()) {
				((ArrayList<Entity>) target).add(indices.get(it.nextIndex()), it.next());
			}
		}

		public void redo(Object target) {
			ListIterator<Integer> it = indices.listIterator();
			int offset = 0;

			while (it.hasNext()) {
				((ArrayList<Entity>) target).remove(it.next().intValue() - offset++);
			}
		}
	}

}
