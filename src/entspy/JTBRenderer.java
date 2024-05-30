package entspy;

import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

public class JTBRenderer
extends JButton
implements TableCellRenderer {
    static ImageIcon linkIcon = new ImageIcon(JTBRenderer.class.getResource("/images/link.gif"));
    private TableCellRenderer defaultRenderer;

    public JTBRenderer() {
        super(linkIcon);
        this.setOpaque(true);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean sel, boolean hasfocus, int row, int col) {
        if (value != null) {
            if (sel) {
                this.setForeground(table.getSelectionForeground());
                this.setBackground(table.getSelectionBackground());
            } else {
                this.setForeground(table.getForeground());
                this.setBackground(UIManager.getColor("Button.background"));
            }
            //Entity ent = ((KeyValLinkModel)table.getModel()).getLink(row);
            
            //if(ent != null) {
            	//this.setToolTipText(ent.toString());
            //}
            return this;
        }
        return null;
    }
}

