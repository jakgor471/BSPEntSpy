package entspy;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTable;

public class JTBEditor
extends DefaultCellEditor {
    protected JButton button = new JTBRenderer();
    private JButton linkbutton;
    private boolean isPushed;

    public JTBEditor(JCheckBox checkBox) {
        super(checkBox);
        this.button.setOpaque(true);
        this.button.addActionListener(new ActionListener(){

            public void actionPerformed(ActionEvent e) {
                JTBEditor.this.fireEditingStopped();
            }
        });
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        if (isSelected) {
            this.button.setForeground(table.getSelectionForeground());
            this.button.setBackground(table.getSelectionBackground());
        } else {
            this.button.setForeground(table.getForeground());
            this.button.setBackground(table.getBackground());
        }
        this.linkbutton = value == null ? null : (JButton)value;
        this.isPushed = true;
        return this.button;
    }

    public Object getCellEditorValue() {
        if (this.isPushed && this.linkbutton != null) {
            this.linkbutton.doClick();
        }
        this.isPushed = false;
        return this.linkbutton;
    }

    public boolean stopCellEditing() {
        this.isPushed = false;
        return super.stopCellEditing();
    }

    protected void fireEditingStopped() {
        super.fireEditingStopped();
    }

}

