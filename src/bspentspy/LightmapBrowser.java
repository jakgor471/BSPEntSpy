package bspentspy;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import bspentspy.SourceBSPFile.Lightmap;

public class LightmapBrowser extends JPanel {
	private static final long serialVersionUID = 1L;
	JList<Lightmap> lightmapList;
	LightmapListModel listModel;
	SourceBSPFile bspmap;
	ArrayList<ActionListener> onSelect;
	
	public void addActionListener(ActionListener l) {
		onSelect.add(l);
	}
	
	public void removeActionListener(ActionListener l) {
		onSelect.remove(l);
	}
	
	public LightmapBrowser(SourceBSPFile bspmap) {
		super(new BorderLayout(10, 10));
		
		onSelect = new ArrayList<>();
		
		this.bspmap = bspmap;
		
		listModel = new LightmapListModel(bspmap);
		lightmapList = new JList<>(listModel);
		lightmapList.setCellRenderer(new LightmapListRenderer());
		
		add(new JScrollPane(lightmapList), "Center");
		
		Box fbox = Box.createHorizontalBox();
		Box fbox2 = Box.createHorizontalBox();
		Box fbox3 = Box.createHorizontalBox();
		
		JTextField findtext = new JTextField();
		JButton findent = new JButton("Find faceID");
		fbox.add(new JLabel("Face ID: "));
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
		coordX.setText("0 0 0");
		JTextField radius = new JTextField();
		radius.setText("-1");
		
		fbox2.add(new JLabel("X Y Z: "));
		fbox2.add(coordX);
		fbox2.add(new JLabel(" Radius: "));
		fbox2.add(radius);
		
		JButton filter = new JButton("Filter");
		filter.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				String[] coords = (coordX.getText() + " 0 0 0").split("[\\s,]+");
				
				int i = 0;
				
				if(coords[0].equals("getpos"))
					++i;
				
				listModel.filter(true, Float.parseFloat(coords[i]), Float.parseFloat(coords[i + 1]), Float.parseFloat(coords[i + 2]), Float.parseFloat(radius.getText()), null);
			}
		});
		
		fbox2.add(filter);
		
		JTextField findmat = new JTextField();
		JButton findmatbutton = new JButton("Filter");
		
		fbox3.add(new JLabel("Material: "));
		fbox3.add(findmat);
		fbox3.add(findmatbutton);
		
		findmatbutton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				String mat = null;
				if(!findmat.getText().isEmpty())
					mat = findmat.getText();
				listModel.filter(false, -1, -1, -1, -1, mat);
			}
		});
		
		fbox.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 0));
		fbox2.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 0));
		fbox3.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 0));
		
		JPanel searchPanel = new JPanel();
		BoxLayout bl = new BoxLayout(searchPanel, BoxLayout.Y_AXIS);
		searchPanel.setLayout(bl);
		searchPanel.add(fbox);
		searchPanel.add(fbox2);
		searchPanel.add(fbox3);
		
		add(searchPanel, "South");
		
		lightmapList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if(!e.getValueIsAdjusting()) {
					for(ActionListener l : onSelect)
						l.actionPerformed(new ActionEvent(this, 0, "select"));
				}
			}
		});
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
