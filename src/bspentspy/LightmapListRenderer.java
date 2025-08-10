package bspentspy;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import bspentspy.SourceBSPFile.Lightmap;

public class LightmapListRenderer extends JPanel implements ListCellRenderer<Lightmap> {
	private static final long serialVersionUID = 1L;
	private Lightmap lightmap;

	public LightmapListRenderer() {
		setPreferredSize(new Dimension(200, 200));
	}

	@Override
	public Component getListCellRendererComponent(JList<? extends Lightmap> list, Lightmap value, int index,
			boolean isSelected, boolean cellHasFocus) {

		this.lightmap = value;
		setBackground(isSelected ? Color.LIGHT_GRAY : Color.WHITE);
		return this;
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		BufferedImage image = lightmap.images[0];
		if (image != null) {
			double scale = (double) Math.min((double)getWidth() / (double)image.getWidth(), (double)getHeight() / (double)image.getHeight());
			
			int realWidth = (int) (image.getWidth() * scale) - 20;
			int realHeight = (int) (image.getHeight() * scale) - 20;
			g.drawImage(image, (getWidth() - realWidth) / 2, (getHeight() - realHeight) / 2, realWidth, realHeight, this);
			g.drawString(lightmap.toString() + ", total images: " + lightmap.images.length, 20, 20);
			
			String material = lightmap.underlyingMaterial;
			
			if(material.length() > 50) {
				material = "..." + material.substring(material.length() - 50);
			}
			
			g.drawString("Mat: " + material, 20, getHeight() - 50);
			g.drawString("Origin: (" + lightmap.origin[0] + ", " + lightmap.origin[1] + ", " + lightmap.origin[2] + ")", 20, getHeight() - 35);
			g.drawString("Size [samples]: " + image.getWidth() + ", " + image.getHeight(), 20, getHeight() - 20);
			
			if(lightmap.hdr) {
				g.setColor(Color.BLUE);
				g.drawString("HDR", 20, 35);
			}
			
			if(lightmap.obscured) {
				Graphics2D g2d = (Graphics2D)g;
				g2d.translate(getWidth()/2 - 10, getHeight()/2 + 10);
				g2d.rotate(-0.7853982);
				
				g.setColor(Color.RED);
				g.drawString("OBSCURED", 0, 0);
			}
		}
	}
}
