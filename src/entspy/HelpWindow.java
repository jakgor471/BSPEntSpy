package entspy;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.html.HTMLEditorKit;

@SuppressWarnings("serial")
public class HelpWindow extends JFrame{
	private static HelpWindow opened = null;
	
	JTextPane textPane;
	private HelpWindow(String title) {
		super(title);
		this.setIconImage(Entspy.esIcon.getImage());
		
		textPane = new JTextPane();
		textPane.setEditable(false);
		
		HTMLEditorKit ek = new HTMLEditorKit();
		textPane.setEditorKit(ek);
		
		JScrollPane scp = new JScrollPane(textPane);

		this.add(scp);
		scp.getVerticalScrollBar().setValue(0);
		
		this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
	}
	
	public void setText(String text) {
		textPane.setText(text);
		textPane.setCaretPosition(0);
	}
	
	public static HelpWindow openHelp(String title) {
		if(opened != null) {
			opened.setTitle(title);
			opened.setText("");
			
			return opened;
		}
		
		opened = new HelpWindow(title);
		return opened;
	}
}
