/*
 * Decompiled with CFR 0_102.
 */
package entspy;

import entspy.Entity;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintStream;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JTree;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

class KeyValLinkModel
extends AbstractTableModel {
    Entity ent;
    ArrayList<JButton> button;
    JTree tree;

    KeyValLinkModel() {
    }

    public void setTreeMapping(JTree t) {
        this.tree = t;
    }

    public int getRowCount() {
        if (this.ent == null) {
            return 0;
        }
        if (this.ent.key == null) {
            return 0;
        }
        return this.ent.key.size();
    }

    public int getColumnCount() {
        return 3;
    }

    public Object getValueAt(int row, int col) {
        int rowcopy = row;
        if (col == 0) {
            return this.ent.key.get(row);
        }
        if (col == 1) {
            return this.ent.value.get(row);
        }
        return this.button.get(row);
    }

    public void setValueAt(Object setval, int row, int col) {
        if (row < 0 || row >= this.ent.size()) {
            return;
        }
        if (col == 0) {
            this.ent.key.set(row, (String)setval);
        } else if (col == 1) {
            this.ent.value.set(row, (String)setval);
        } else {
            return;
        }
        this.ent.setnames();
        this.fireTableDataChanged();
        this.reselect();
    }

    public Class getColumnClass(int col) {
        if (col == 2) {
            return JButton.class;
        }
        return String.class;
    }

    public String getColumnName(int col) {
        String[] header = new String[]{"Key", "Value", "Link"};
        return header[col];
    }

    public void clear() {
        this.ent = null;
        this.button.clear();
        this.fireTableDataChanged();
    }

    public boolean isCellEditable(int row, int col) {
        if (col == 2 && this.ent.link.get(row) == null) {
            return false;
        }
        return true;
    }

    public void set(Entity e) {
        this.ent = e;
        this.setlinklisteners();
    }

    public void setlinklisteners() {
        this.button = new ArrayList();
        for (int i = 0; i < this.ent.key.size(); ++i) {
            final int rowcopy = i;
            if (this.ent.link.get(i) != null) {
                JButton lb = new JButton();
                lb.addActionListener(new ActionListener(){

                    public void actionPerformed(ActionEvent ae) {
                        Entity targetent = KeyValLinkModel.this.ent.link.get(rowcopy);
                        Object[] tna = KeyValLinkModel.this.findtreenode(targetent, KeyValLinkModel.this.tree);
                        if (tna != null) {
                            TreePath tp = new TreePath(tna);
                            KeyValLinkModel.this.tree.setSelectionPath(tp);
                            KeyValLinkModel.this.tree.scrollPathToVisible(tp);
                            return;
                        }
                        System.out.println("Cannot find node for target ent: " + targetent);
                    }
                });
                this.button.add(lb);
                continue;
            }
            this.button.add(null);
        }
        this.fireTableDataChanged();
    }

    public Entity getLink(int row) {
        return this.ent.link.get(row);
    }

    public TreeNode[] findtreenode(Object target, JTree t) {
        TreeModel tmodel = this.tree.getModel();
        DefaultMutableTreeNode currentnode = (DefaultMutableTreeNode)tmodel.getRoot();
        do {
            if (currentnode.getUserObject() != target) continue;
            return currentnode.getPath();
        } while ((currentnode = currentnode.getNextNode()) != null);
        return null;
    }

    public void reselect() {
        TreePath tp = this.tree.getSelectionPath();
        if (tp != null) {
            ((DefaultTreeModel)this.tree.getModel()).nodeChanged((DefaultMutableTreeNode)tp.getLastPathComponent());
            this.tree.clearSelection();
            this.tree.setSelectionPath(tp);
        }
    }

    public void refreshtable() {
        this.setlinklisteners();
        this.reselect();
    }

}

