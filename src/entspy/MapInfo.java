package entspy;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

public class MapInfo
extends JFrame {
    Component frame;
    BSP m;
    String filename;

    public MapInfo(Component frame, BSP m, String filename) {
        super("Entspy - Map Info - " + filename + " (stored on disk)");
        if (frame instanceof Frame) {
            this.setIconImage(((Frame)frame).getIconImage());
        }
        this.frame = frame;
        this.m = m;
        this.filename = filename;
        JPanel panel = new JPanel(new BorderLayout());
        try {
            m.computecrc();
            JLabel toplab = new JLabel("Map length: " + m.raf.length() + " bytes     CRC (excludes entity lump): " + Long.toHexString(m.crc.getValue()));
            panel.add((Component)toplab, "North");
            Lumpdata ld = new Lumpdata(m);
            JTable table = new JTable(ld);
            table.getColumn(Lumpdata.header[0]).setMaxWidth(20);
            table.getColumn(Lumpdata.header[2]).setMaxWidth(60);
            table.getColumn(Lumpdata.header[3]).setMaxWidth(60);
            panel.add((Component)new JScrollPane(table), "Center");
            this.getContentPane().add(panel);
            this.setDefaultCloseOperation(2);
            this.pack();
            this.setLocationRelativeTo(frame);
            this.setVisible(true);
        }
        catch (IOException ioe) {
            System.out.println(ioe);
        }
    }
}

