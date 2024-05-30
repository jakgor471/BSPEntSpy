package entspy;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import javax.swing.JFrame;
import javax.swing.JProgressBar;

public class JProgFrame
extends JFrame {
    JProgressBar progbar;
    Component frame;

    public JProgFrame(Component frame, String title) {
        super(title);
        if (frame instanceof Frame) {
            this.setIconImage(((Frame)frame).getIconImage());
        }
        this.frame = frame;
        this.progbar = new JProgressBar(0, 0);
        this.progbar.setStringPainted(true);
        this.progbar.setString("");
        this.getContentPane().add(this.progbar);
        this.progbar.setPreferredSize(new Dimension(420, 24));
        this.setResizable(false);
        this.setDefaultCloseOperation(0);
        this.pack();
        if (frame == null) {
            this.setLocationRelativeTo(null);
        } else {
            int x = frame.getX() + (frame.getWidth() - this.getWidth()) / 2;
            int y = frame.getY() + (frame.getHeight() - this.getHeight()) / 2;
            this.setLocation(x, y);
        }
        this.setVisible(false);
    }

    public void start(String progstr, boolean hide) {
        this.progbar.setString(progstr);
        this.setVisible(true);
        this.requestFocus();
        this.progbar.setValue(0);
        if (hide && this.frame != null) {
            this.frame.setEnabled(false);
        }
    }

    public void end() {
        if (this.frame != null) {
            this.frame.setEnabled(true);
        }
        this.setVisible(false);
        this.dispose();
    }

    public void setString(String progstr) {
        this.progbar.setString(progstr);
    }

    public void setMaximum(int max) {
        this.progbar.setMaximum(max);
    }

    public void setValue(int val) {
        this.progbar.setValue(val);
    }

    public void setValue(long val) {
        this.setValue((int)val);
    }
}

