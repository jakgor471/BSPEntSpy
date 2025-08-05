package bspentspy;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import bspentspy.SourceBSPFile.Lightmap;

public class LightmapEditor extends JPanel {
	private static final long serialVersionUID = 1L;
	JList<Lightmap> lightmapList;
	DefaultListModel<Lightmap> listModel;
	SourceBSPFile bspmap;
	
	public LightmapEditor(SourceBSPFile bspmap) {
		super(new BorderLayout());
		
		this.bspmap = bspmap;
		
		listModel = new DefaultListModel<>();
		lightmapList = new JList<>(listModel);
		
		try {
			ArrayList<Lightmap> l = bspmap.getLightmaps();
			for(Lightmap l2 : l)
				listModel.addElement(l2);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		add(new JScrollPane(lightmapList), "Center");
	}
}
