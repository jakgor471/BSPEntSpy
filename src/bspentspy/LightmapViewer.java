package bspentspy;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

import javax.swing.JPanel;

public class LightmapViewer extends JPanel{
	private static final long serialVersionUID = 1L;
	
	private BufferedImage canvas;
	private BufferedImage lightmap;
	
	private double exposure;
	
	public LightmapViewer() {
		exposure = 0;
	}
	
	public void setExposure(double exposure) {
		this.exposure = exposure;
		refresh();
	}
	
	public void setLightmap(BufferedImage l) {
		lightmap = l;
		refresh();
	}
	
	public void refresh() {
		if(lightmap == null)
			return;
		
		if(canvas == null || canvas.getWidth() != lightmap.getWidth() || canvas.getHeight() != lightmap.getHeight())
			canvas = new BufferedImage(lightmap.getWidth(), lightmap.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
		
		int[] pixel = new int[4];
		int[] pixel2 = new int[3];
		double[] fpix = new double[3];
		WritableRaster raster = lightmap.getRaster();
		WritableRaster canvasRaster = canvas.getRaster();
		
		for(int y = 0; y < raster.getHeight(); ++y) {
			for(int x = 0; x < raster.getWidth(); ++x) {
				raster.getPixel(x, y, pixel);
				
				double brightness = Math.pow(2.0, pixel[3] - 128);
				
				fpix[0] = ((double)pixel[0] * (1.0D + exposure) / 255.0D) * brightness;
				fpix[1] = ((double)pixel[1] * (1.0D + exposure) / 255.0D) * brightness;
				fpix[2] = ((double)pixel[2] * (1.0D + exposure) / 255.0D) * brightness;
				
				pixel2[0] = (int)Math.min(Math.max(fpix[0] * 255, 0), 255);
				pixel2[1] = (int)Math.min(Math.max(fpix[1] * 255, 0), 255);
				pixel2[2] = (int)Math.min(Math.max(fpix[2] * 255, 0), 255);
				
				canvasRaster.setPixel(x, y, pixel2);
			}
		}
		
		repaint();
	}
	
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		if(canvas == null)
			return;
		
		double scale = Math.min((double)getWidth() / (double)canvas.getWidth(), (double)getHeight() / (double)canvas.getHeight());
		
		int realWidth = (int) (canvas.getWidth() * scale);
		int realHeight = (int) (canvas.getHeight() * scale);
		g.drawImage(canvas, (getWidth() - realWidth) / 2, (getHeight() - realHeight) / 2, realWidth, realHeight, this);
	} 
}
