package entspy;

import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import entspy.Entspy.TextListen;

@SuppressWarnings("serial")
public class ClassPropertyPanel extends JPanel {
	public JTextField classTextField;
	public JTextField originTextField;
	
	public ClassPropertyPanel() {
		JPanel grid = new JPanel();
		GridLayout gridLayout = new GridLayout(2, 2);
		gridLayout.setHgap(10);
		gridLayout.setVgap(5);
		grid.setLayout(gridLayout);
		
		classTextField = new JTextField(" ");
		originTextField = new JTextField(" ");
		//classTextField.addActionListener(new TextListen(0));
		//originTextField.addActionListener(new TextListen(3));
		classTextField.setEnabled(false);
		originTextField.setEnabled(false);
		
		grid.add(new JLabel("Class", 4));
		grid.add(classTextField);
		grid.add(new JLabel("Origin", 4));
		grid.add(originTextField);
	}
}
