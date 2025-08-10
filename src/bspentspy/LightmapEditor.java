package bspentspy;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import bspentspy.SourceBSPFile.Lightmap;

public class LightmapEditor extends JPanel {
	private static final long serialVersionUID = 1L;
	JList<Lightmap> lightmapList;
	LightmapListModel listModel;
	SourceBSPFile bspmap;
	
	public LightmapEditor(SourceBSPFile bspmap) {
		super(new BorderLayout(10, 10));
		
		this.bspmap = bspmap;
		
		listModel = new LightmapListModel(bspmap);
		lightmapList = new JList<>(listModel);
		lightmapList.setCellRenderer(new LightmapListRenderer());
		
		add(new JScrollPane(lightmapList), "Center");
		
		Box fbox = Box.createHorizontalBox();
		Box fbox2 = Box.createHorizontalBox();
		
		JTextField findtext = new JTextField();
		findtext.setToolTipText("Text to search for");
		JButton findent = new JButton("Find faceID");
		findent.setToolTipText("Find and select next entity, hold Shift to select all");
		fbox.add(findtext);
		fbox.add(findent);
		findent.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				int startIndex = lightmapList.getMaxSelectionIndex();
				int size = listModel.getSize();
				int number = -1;
				try {
					number = Integer.parseInt(findtext.getText());
				}catch(NumberFormatException e) {
					return;
				}
				
				if(startIndex < 0)
					startIndex = 0;
				
				boolean found = false;
				
				for(int i = 0; i < 2 && !found; ++i) {
					for(int j = startIndex; j < size; ++j) {
						if(listModel.getElementAt(j).faceId == number) {
							lightmapList.setSelectedIndex(j);
							lightmapList.ensureIndexIsVisible(j);
							found = true;
							break;
						}
					}
					
					size = startIndex + 1;
					startIndex = 0;
				}
			}
		});
		
		JTextField coordX = new JTextField();
		coordX.setText("0");
		JTextField radius = new JTextField();
		radius.setText("-1");
		
		fbox2.add(new JLabel(" X Y Z: "));
		fbox2.add(coordX);
		fbox2.add(new JLabel(" Radius: "));
		fbox2.add(radius);
		
		JButton filter = new JButton("Filter");
		filter.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				String[] coords = (coordX.getText() + " 0 0 0").split("[\\s,]+");
				listModel.filter(Float.parseFloat(coords[0]), Float.parseFloat(coords[1]), Float.parseFloat(coords[2]), Float.parseFloat(radius.getText()));
			}
		});
		
		fbox2.add(filter);
		fbox.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
		
		JPanel searchPanel = new JPanel();
		BoxLayout bl = new BoxLayout(searchPanel, BoxLayout.Y_AXIS);
		searchPanel.setLayout(bl);
		searchPanel.add(fbox);
		searchPanel.add(fbox2);
		
		add(searchPanel, "South");
	}
	
	public Lightmap[] getSelectedLightmaps() {
		int[] selectedIndices = lightmapList.getSelectedIndices();
		Lightmap[] lightmaps = new Lightmap[selectedIndices.length];
		
		for(int i = 0; i < selectedIndices.length; ++i) {
			lightmaps[i] = listModel.getElementAt(selectedIndices[i]);
		}
		
		return lightmaps;
	}
	
	public void refreshList() {
		listModel.reload();
	}
}
