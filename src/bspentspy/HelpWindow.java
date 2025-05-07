package bspentspy;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.html.HTMLEditorKit;

@SuppressWarnings("serial")
public class HelpWindow extends JDialog {
	private static HelpWindow opened = null;

	JTextPane textPane;

	private HelpWindow(JFrame parent, String title) {
		super(parent, title);

		textPane = new JTextPane();
		textPane.setEditable(false);

		HTMLEditorKit ek = new HTMLEditorKit();
		textPane.setEditorKit(ek);

		JScrollPane scp = new JScrollPane(textPane);

		this.getContentPane().add(scp);
		scp.getVerticalScrollBar().setValue(0);

		this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
	}

	public void setText(String text) {
		textPane.setText(text);
		textPane.setCaretPosition(0);
	}
	
	public static void closeHelp() {
		if(opened != null)
			opened.dispose();
		opened = null;
	}

	public static HelpWindow openHelp(JFrame parent, String title) {
		if (opened != null) {
			opened.setTitle(title);
			opened.setText("");

			return opened;
		}

		opened = new HelpWindow(parent, title);
		opened.setLocation(BSPEntspy.frame.getLocation());
		return opened;
	}
}
