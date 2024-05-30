/*
 * Decompiled with CFR 0_102.
 */
package entspy;

import entspy.Entity;
import java.awt.Component;
import java.net.URL;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

public class LERenderer
extends DefaultListCellRenderer {
    ImageIcon pointIcon;
    ImageIcon modelIcon;
    ImageIcon brushIcon;

    public LERenderer() {
        this.pointIcon = new ImageIcon(this.getClass().getResource("/images/point.gif"));
        this.modelIcon = new ImageIcon(this.getClass().getResource("/images/model.gif"));
        this.brushIcon = new ImageIcon(this.getClass().getResource("/images/brush.gif"));
    }

    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean hasFocus) {
    	super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
    	switch (this.nodetype(value)) {
	        case 0: {
	            this.setIcon(this.pointIcon);
	            break;
	        }
	        case 1: {
	            this.setIcon(this.modelIcon);
	            break;
	        }
	        case 2: {
	            this.setIcon(this.brushIcon);
	        }
	    }
	    return this;
    }
    

    protected int nodetype(Object value) {
        Entity nodeEnt = (Entity)value;
        return nodeEnt.type;
    }
}

