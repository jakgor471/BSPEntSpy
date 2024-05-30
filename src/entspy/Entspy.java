/*
 * Decompiled with CFR 0_102.
 */
package entspy;

import entspy.BSP;
import entspy.EntFileFilter;
import entspy.Entity;
import entspy.JProgFrame;
import entspy.JTBEditor;
import entspy.JTBRenderer;
import entspy.KeyValLinkModel;
import entspy.MapInfo;
import entspy.TERenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
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
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import util.Cons;
import util.SwingWorker;

public class Entspy {
    BSP m;
    String filename;
    File infile;
    RandomAccessFile raf;
    JFrame frame = null;
    final Entity blank = new Entity("");
    JList entList;
    JTable table;
    MapInfo info;
    static final String VERSION = "v0.9";
    static ImageIcon esIcon = new ImageIcon(JTBRenderer.class.getResource("/images/entspy.gif"));

    public int exec() throws IOException {
        Cons.println("Entspy v0.8.X rof@mellish.org.uk");
        this.frame = new JFrame("Entspy");
        this.frame.setIconImage(esIcon.getImage());
        if (!this.loadfile()) {
            System.exit(0);
        }
        this.m = new BSP(this.raf);
        this.m.loadheader();
        this.entList = new JList<Entity>();
        DefaultListSelectionModel selmodel = new DefaultListSelectionModel();
        selmodel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        entList.setSelectionModel(selmodel);
       

        this.frame.setTitle("Entspy - " + this.filename);
        JMenu filemenu = new JMenu("File");
        JMenuItem mload = new JMenuItem("Load BSP");
        JMenuItem msave = new JMenuItem("Save BSP");
        mload.setToolTipText("Load an new map file");
        msave.setToolTipText("Save the current map file");
        mload.setAccelerator(KeyStroke.getKeyStroke(76, 2));
        msave.setAccelerator(KeyStroke.getKeyStroke(83, 2));
        filemenu.add(mload);
        filemenu.add(msave);
        filemenu.addSeparator();
        JMenuItem minfo = new JMenuItem("Map info...");
        minfo.setToolTipText("Map header information");
        minfo.setAccelerator(KeyStroke.getKeyStroke(73, 2));
        filemenu.add(minfo);
        filemenu.addSeparator();
        JMenuItem mquit = new JMenuItem("Quit");
        mquit.setToolTipText("Quit Entspy");
        mquit.setAccelerator(KeyStroke.getKeyStroke(81, 2));
        filemenu.add(mquit);
        JMenuBar menubar = new JMenuBar();
        menubar.add(filemenu);
        this.frame.setJMenuBar(menubar);
        mload.addActionListener(new ActionListener(){

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
                }
                catch (IOException ex) {
                    Cons.println(ex);
                }
            }
        });
        msave.addActionListener(new ActionListener(){

            public void actionPerformed(ActionEvent e) {
                Entspy.this.frame.setCursor(Cursor.getPredefinedCursor(3));
                SwingWorker worker = new SwingWorker(){

                    public Object construct() {
                        Entspy.this.savefile();
                        return null;
                    }

                    public void finished() {
                        Entspy.this.frame.setTitle("Entspy - " + Entspy.this.filename);
                        Entspy.this.frame.setCursor(null);
                    }
                };
                worker.start();
            }

        });
        mquit.addActionListener(new ActionListener(){

            public void actionPerformed(ActionEvent e) {
                if (Entspy.this.checkchanged("Quit Entspy")) {
                    return;
                }
                System.exit(0);
            }
        });
        minfo.addActionListener(new ActionListener(){

            public void actionPerformed(ActionEvent e) {
                Entspy.this.m.setfile(Entspy.this.raf);
                if (Entspy.this.info != null) {
                    Entspy.this.info.dispose();
                }
                Entspy.this.info = new MapInfo(Entspy.this.frame, Entspy.this.m, Entspy.this.filename);
            }
        });
        JPanel panel = new JPanel(new BorderLayout());
        JPanel grid = new JPanel();
        grid.setLayout(new GridLayout(4, 2));
        final JTextField classlabel = new JTextField(" ");
        final JTextField targetlabel = new JTextField(" ");
        final JTextField parentlabel = new JTextField(" ");
        final JTextField originlabel = new JTextField(" ");
        classlabel.addActionListener(new TextListen(0));
        targetlabel.addActionListener(new TextListen(1));
        parentlabel.addActionListener(new TextListen(2));
        originlabel.addActionListener(new TextListen(3));
        classlabel.setEnabled(false);
        targetlabel.setEnabled(false);
        parentlabel.setEnabled(false);
        originlabel.setEnabled(false);
        grid.add(new JLabel(" Classname ", 4));
        grid.add(classlabel);
        grid.add(new JLabel(" Targetname ", 4));
        grid.add(targetlabel);
        grid.add(new JLabel(" Parentname ", 4));
        grid.add(parentlabel);
        grid.add(new JLabel(" Origin ", 4));
        grid.add(originlabel);
        panel.add((Component)grid, "North");
        JPanel keypanel = new JPanel();
        final KeyValLinkModel tablemodel = new KeyValLinkModel();
        tablemodel.setTreeMapping(this.entList);
        this.table = new JTable(tablemodel);
        this.table.setSelectionMode(0);
        TableColumn keycol = this.table.getColumn("Value");
        keycol.setPreferredWidth(175);
        TableColumn linkcol = this.table.getColumn("Link");
        linkcol.setMaxWidth(30);
        linkcol.setMinWidth(30);
        linkcol.setResizable(false);
        linkcol.setCellRenderer(new JTBRenderer());
        linkcol.setCellEditor(new JTBEditor(new JCheckBox()));
        keypanel.setLayout(new GridLayout(1, 1));
        keypanel.add(new JScrollPane(this.table));
        panel.add((Component)keypanel, "Center");
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
        findbutton.setMnemonic(76);
        findpanel.add(findbutton);
        findbutton.addActionListener(new ActionListener(){

            public void actionPerformed(ActionEvent e) {
                /*Entity targetent = (Entity)findmodel.getSelectedItem();
                DefaultMutableTreeNode currentnode = (DefaultMutableTreeNode)Entspy.this.entList.getModel().getRoot();
                do {
                    if (Entspy.this.getNodeEntity(currentnode) != targetent) continue;
                    TreePath tp = new TreePath(currentnode.getPath());
                    Entspy.this.entList.setSelectionPath(tp);
                    Entspy.this.entList.scrollPathToVisible(tp);
                    return;
                } while ((currentnode = currentnode.getNextNode()) != null);
                Cons.println("Cannot find node for target ent: " + targetent);*/
            }
        });
        findlabel.setEnabled(false);
        findbutton.setEnabled(false);
        findcombo.setEnabled(false);
        panel.add((Component)findpanel, "South");
        JPanel tpanel = new JPanel(new BorderLayout());
        tpanel.add((Component)new JScrollPane(this.entList), "Center");
        JPanel entcpl = new JPanel(new BorderLayout());
        JPanel entbut = new JPanel();
        JButton updent = new JButton("Update");
        updent.setToolTipText("Update entity links");
        updent.setMnemonic(85);
        JButton addent = new JButton("Add");
        addent.setToolTipText("Add a new entity");
        addent.setMnemonic(155);
        final JButton cpyent = new JButton("Copy");
        cpyent.setToolTipText("Copy the selected entity");
        cpyent.setEnabled(false);
        cpyent.setMnemonic(10);
        final JButton delent = new JButton("Del");
        delent.setToolTipText("Delete the selected entity");
        delent.setEnabled(false);
        delent.setMnemonic(127);
        entbut.add(updent);
        entbut.add(addent);
        entbut.add(cpyent);
        entbut.add(delent);
        entcpl.add((Component)entbut, "South");
        JButton findent = new JButton("Find Next");
        findent.setToolTipText("Find entity");
        findent.setMnemonic(114);
        JButton findall = new JButton("Find all");
        findall.setToolTipText("Select all matching entities");
        findall.setMnemonic(114);
        JTextField findtext = new JTextField();
        findtext.setToolTipText("Text to search for");
        Box fbox = Box.createHorizontalBox();
        fbox.add(findtext);
        fbox.add(findent);
        fbox.add(findall);
        entcpl.add((Component)fbox, "Center");
        entcpl.setBorder(BorderFactory.createEtchedBorder());
        tpanel.add((Component)entcpl, "South");
        findent.addActionListener(new FindListen(findtext));
        findtext.addActionListener(new FindListen(findtext));
        findall.addActionListener(new FindSelectListen(findtext));
        updent.addActionListener(new ActionListener(){

            public void actionPerformed(ActionEvent e) {
                //Entspy.this.root = Entspy.this.m.getTree();
                //Entspy.this.entList.setModel(new DefaultTreeModel(Entspy.this.root));
            	entList.setModel(new EntspyListModel(m.el));
            }
        });
        delent.addActionListener(new ActionListener(){

            public void actionPerformed(ActionEvent e) {            	
            	for(int i : Entspy.this.entList.getSelectedIndices()) {
            		Entspy.this.m.el.remove(i);
            	}
            	
                Entspy.this.entList.setModel(new EntspyListModel(Entspy.this.m.el));
                Entspy.this.m.dirty = true;
            }
        });
        cpyent.addActionListener(new ActionListener(){

            public void actionPerformed(ActionEvent e) {
                Entity selEnt = Entspy.this.getSelectedEntity();
                Entity newent = selEnt.copy();
                int i = Entspy.this.m.el.indexOf(selEnt);
                Entspy.this.m.el.add(i + 1, newent);
                entList.setModel(new EntspyListModel(m.el));
                
                entList.setSelectedIndex(m.el.indexOf(newent));
                
                Entspy.this.m.dirty = true;
            }
        });
        addent.addActionListener(new ActionListener(){

            public void actionPerformed(ActionEvent e) {
                Entity newent = new Entity();
                int index = 0;
                
                if(entList.getMaxSelectionIndex() > 0) {
                	m.el.add(entList.getMaxSelectionIndex() + 1, newent);
                	index = entList.getMaxSelectionIndex() + 1;
                } else {
                	m.el.add(newent);
                	index = m.el.size() - 1;
                }
                
                newent.autoedit = true;
                Entspy.this.entList.setModel(new EntspyListModel(m.el));
                entList.setSelectedIndex(index);
                entList.ensureIndexIsVisible(index);
                
            }
        });
        JPanel cpanel = new JPanel();
        final JButton addkv = new JButton("Add");
        addkv.setToolTipText("Add an entity property");
        addkv.setMnemonic(65);
        cpanel.add(addkv);
        addkv.setEnabled(false);
        final JButton cpykv = new JButton("Copy");
        cpykv.setToolTipText("Copy the selected property");
        cpykv.setMnemonic(67);
        cpanel.add(cpykv);
        cpykv.setEnabled(false);
        final JButton delkv = new JButton("Delete");
        delkv.setToolTipText("Delete the selected property");
        delkv.setMnemonic(68);
        cpanel.add(delkv);
        delkv.setEnabled(false);
        
        final JButton entexp = new JButton("Export");
        entexp.setToolTipText("Exports entity to a text file");
        entexp.setMnemonic(69);
        cpanel.add(entexp);
        entexp.setEnabled(true);
        
        this.entList.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				TableCellEditor tce = Entspy.this.table.getCellEditor();
				if(tce != null && Entspy.this.table.isEditing())
					tce.stopCellEditing();
				Entity selEnt = Entspy.this.getSelectedEntity();
				
				if(selEnt != null) {
					classlabel.setText(selEnt.classname);
	                classlabel.setEnabled(true);
	                targetlabel.setText(selEnt.targetname);
	                targetlabel.setEnabled(true);
	                parentlabel.setText(selEnt.parentname);
	                parentlabel.setEnabled(true);
	                originlabel.setText(selEnt.origin);
	                originlabel.setEnabled(true);
	                Entspy.this.settable(selEnt, tablemodel);
	                
	                delent.setEnabled(true);
	                cpyent.setEnabled(true);
	                addkv.setEnabled(true);
	                if (selEnt.autoedit) {
	                    selEnt.autoedit = false;
	                    classlabel.setText("new_entity");
	                    classlabel.selectAll();
	                    classlabel.requestFocus();
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
                    classlabel.setText(" ");
                    classlabel.setEnabled(false);
                    targetlabel.setText(" ");
                    targetlabel.setEnabled(false);
                    parentlabel.setText(" ");
                    parentlabel.setEnabled(false);
                    originlabel.setText(" ");
                    originlabel.setEnabled(false);
                    Entspy.this.settable(Entspy.this.blank, tablemodel);
                    findlabel.setEnabled(false);
                    findbutton.setEnabled(false);
                    findcombo.setEnabled(false);
                    delent.setEnabled(false);
                    cpyent.setEnabled(false);
                    addkv.setEnabled(false);
                }
			}
        	
        });
        
        addkv.addActionListener(new ActionListener(){

            public void actionPerformed(ActionEvent ae) {
                Entity selEnt = Entspy.this.getSelectedEntity();
                selEnt.addkeyval("", "");
                Entspy.this.settable(selEnt, tablemodel);
                int lastrow = selEnt.size() - 1;
                Entspy.this.table.changeSelection(lastrow, 0, false, false);
                Entspy.this.table.editCellAt(lastrow, 0);
                Entspy.this.table.scrollRectToVisible(Entspy.this.table.getCellRect(lastrow, 0, true));
                Entspy.this.table.getEditorComponent().requestFocus();
                Entspy.this.m.dirty = true;
            }
        });
        cpykv.addActionListener(new ActionListener(){

            public void actionPerformed(ActionEvent ae) {
                Entity selEnt = Entspy.this.getSelectedEntity();
                int selrow = Entspy.this.table.getSelectedRow();
                if (selrow == -1) {
                    return;
                }
                selEnt.addkeyval(selEnt.key.get(selrow), selEnt.value.get(selrow));
                selEnt.setnames();
                Entspy.this.settable(selEnt, tablemodel);
                tablemodel.reselect();
                int lastrow = selEnt.size() - 1;
                Entspy.this.table.changeSelection(lastrow, 1, false, false);
                Entspy.this.table.editCellAt(lastrow, 1);
                Entspy.this.table.scrollRectToVisible(Entspy.this.table.getCellRect(lastrow, 1, true));
                Entspy.this.table.getEditorComponent().requestFocus();
                Entspy.this.m.dirty = true;
            }
        });
        delkv.addActionListener(new ActionListener(){

            public void actionPerformed(ActionEvent ae) {
                Entity selEnt = Entspy.this.getSelectedEntity();
                int selrow = Entspy.this.table.getSelectedRow();
                if (selrow == -1) {
                    return;
                }
                selEnt.delkeyval(selrow);
                selEnt.setnames();
                tablemodel.fireTableDataChanged();
                tablemodel.reselect();
                Entspy.this.m.dirty = true;
            }
        });
        this.table.getSelectionModel().addListSelectionListener(new ListSelectionListener(){

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
        JPanel epanel = new JPanel(new BorderLayout());
        epanel.add((Component)panel, "Center");
        epanel.add((Component)cpanel, "South");
        cpanel.setBorder(BorderFactory.createEtchedBorder());
        JSplitPane split = new JSplitPane(1, tpanel, epanel);
        split.setDividerLocation(275);
        this.frame.setDefaultCloseOperation(3);
        this.frame.setSize(720, 520);
        this.frame.getContentPane().add(split);
        this.frame.setVisible(true);
        this.loaddata();
        return 0;
    }

    public void settable(Entity ent, KeyValLinkModel model) {
        model.set(ent);
    }

    public boolean setfindlist(Entity sel, DefaultComboBoxModel model) {
        model.removeAllElements();
        block0 : for (int i = 0; i < this.m.el.size(); ++i) {
            Entity lent = this.m.el.get(i);
            if (lent.key == null) continue;
            for (int j = 0; j < lent.key.size(); ++j) {
                Entity linkent = lent.link.get(j);
                if (linkent == null || !linkent.targetname.equals(sel.targetname)) continue;
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
            JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
            chooser.setDialogTitle("Entspy - Open a BSP file");
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
            return true;
        }
        catch (IOException ioe) {
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
                opts[0] = "Entity lump exceeds previously stored size by " + this.strsize(this.m.entdiff) + ".\nChoose an option:";
                bgroup = new ButtonGroup();
                rb = new JRadioButton("Optimize storage (minimum file size, breaks checksum).", true);
                bgroup.add(rb);
                rb.setActionCommand("Optimize");
                opts[1] = rb;
                rb = new JRadioButton("Store at end (wastes " + this.strsize(this.m.entlumpsize) + ", preserves checksum).");
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
                opts[0] = "Entity lump is smaller than previously stored size by " + this.strsize(- this.m.entdiff) + ".\nChoose an option:";
                bgroup = new ButtonGroup();
                rb = new JRadioButton("Optimize storage (minimum file size, breaks checksum).", true);
                bgroup.add(rb);
                rb.setActionCommand("Optimize");
                opts[1] = rb;
                rb = new JRadioButton("Store without optimization (wastes " + this.strsize(- this.m.entdiff) + ", preserves checksum).");
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
            JProgFrame prog = new JProgFrame(this.frame, "Entspy - Save BSP file");
            this.m.setprog(prog);
            JFileChooser chooser = new JFileChooser(this.infile);
            chooser.setSelectedFile(this.infile);
            chooser.setDialogTitle("Entspy - Save BSP file - " + this.filename);
            chooser.setFileFilter(new EntFileFilter());
            int result2 = chooser.showSaveDialog(this.frame);
            if (result2 == 1) {
                return;
            }
            File outfile = chooser.getSelectedFile();
            chooser = null;
            String outfilename = outfile.getName();
            if (outfile.exists()) {
                result2 = JOptionPane.showConfirmDialog(this.frame, "Map file " + outfile + " exists.\nAre you sure you want to overwrite?", "Save BSP file", 0);
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
        }
        catch (IOException ioe) {
            Cons.println(ioe);
        }
    }

    public void loaddata() {
        final JProgFrame prog = new JProgFrame(this.frame, "Entspy - Load BSP file");
        this.m.setprog(prog);
        prog.setMaximum(this.m.loadtasklength());
        prog.start("Loading entities...", true);
        this.frame.setCursor(Cursor.getPredefinedCursor(3));
        final Timer timer = new Timer(100, new ActionListener(){

            public void actionPerformed(ActionEvent evt) {
                prog.setValue(Entspy.this.m.loadtaskprogress());
            }
        });
        SwingWorker worker = new SwingWorker(){

            public Object construct() {
                try {
                    Entspy.this.m.loadentities();
                    Entspy.this.m.loadglumps();
                }
                catch (IOException ex) {
                    Cons.println(ex);
                }
                return null;
            }

            public void finished() {
                timer.stop();
                Entspy.this.entList.setModel(new EntspyListModel(Entspy.this.m.el));
                Entspy.this.frame.setCursor(null);
                prog.end();
                Entspy.this.frame.setTitle("Entspy - " + Entspy.this.filename);
            }
        };
        timer.start();
        worker.start();
    }

    public boolean checkchanged(String title) {
        if (!this.m.dirty) {
            return false;
        }
        int result = JOptionPane.showConfirmDialog(this.frame, new Object[]{"File " + this.filename + " has been edited.", "Discard changes?"}, title, 0);
        if (result == 0) {
            return false;
        }
        return true;
    }

    public String strsize(int bytes) {
        if (bytes < 2000) {
            return "" + bytes + " bytes";
        }
        return new DecimalFormat("0.0").format((float)bytes / 1024.0f) + " kbytes";
    }

    public Entity getSelectedEntity() {
        //return (DefaultMutableTreeNode)this.entList.getLastSelectedPathComponent();
    	int index = entList.getSelectedIndex();
    	
    	if(index < 0 || index >= m.el.size())
    		return null;
    	
    	return m.el.get(index);
    }

    public Entity getNodeEntity(DefaultMutableTreeNode node) {
        return (Entity)node.getUserObject();
    }

    public static void main(String[] args) throws Exception {
        Entspy inst = new Entspy();
        inst.exec();
    }

    class FindListen
    implements ActionListener {
        JTextField textf;

        public FindListen(JTextField textf) {
            this.textf = textf;
        }

        public void actionPerformed(ActionEvent ae) {
            boolean found = false;
            String ftext = this.textf.getText();
            String searchKey = null;
            if (ftext.equals("")) {
                return;
            }
            
            int i = 0;
            for(i = entList.getSelectedIndex() + 1; i < ((EntspyListModel)entList.getModel()).entities.size(); ++i) {
            	if(((EntspyListModel)entList.getModel()).entities.get(i).ismatch(ftext, searchKey)) {
            		found = true;
            		break;
            	}
            }
            
            if(found) {
            	entList.setSelectedIndex(i);
            	entList.ensureIndexIsVisible(i);
            }
        }
    }
    
    class FindSelectListen extends FindListen
    implements ActionListener {
        public FindSelectListen(JTextField textf) {
            super(textf);
        }

        public void actionPerformed(ActionEvent ae) {
            boolean found = false;
            String ftext = this.textf.getText();
            String searchKey = null;
            
            if(ftext.equals(""))
            	return;
            
            //for(Entspy.this.tree.get)
        }
    }

    class TextListen
    implements ActionListener {
        int type;

        public TextListen(int type) {
            this.type = type;
        }

        public void actionPerformed(ActionEvent ae) {
            String text = ((JTextField)ae.getSource()).getText().trim();
            Entity selEnt = Entspy.this.getSelectedEntity();
            if (selEnt == null) {
                return;
            }
            selEnt.setDefinedValue(this.type, text);
            ((KeyValLinkModel)Entspy.this.table.getModel()).refreshtable();
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
			// TODO Auto-generated method stub
			
		}

		@Override
		public void removeListDataListener(ListDataListener l) {
			// TODO Auto-generated method stub
			
		}
    	
    }

}

