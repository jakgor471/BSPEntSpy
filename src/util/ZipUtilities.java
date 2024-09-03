package util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import util.ZipUtilities.ZipReadCallback;

public class ZipUtilities {
	public static void copyReadZip(InputStream in, OutputStream out, ZipReadCallback callback) throws IOException {
		ZipInputStream zin = new ZipInputStream(in);
		ZipOutputStream zout = new ZipOutputStream(out);
		ZipEntry entry = null;
		
		while((entry = zin.getNextEntry()) != null) {
			String newName = callback.onRename(entry.getName(), entry.isDirectory());
			ZipEntry modified = entry;
			
			if(newName != null) {
				modified = new ZipEntry(newName);
				modified.setComment(entry.getComment());
				modified.setCompressedSize(entry.getCompressedSize());
				modified.setSize(entry.getSize());
				modified.setMethod(entry.getMethod());
				modified.setCrc(entry.getCrc());
			}
			
			zout.putNextEntry(modified);
			byte[] block = new byte[20480];
			int len;
			
			if(!callback.onRead(zin, zout, modified)) {
				while((len = zin.read(block)) > 0) {
					zout.write(block, 0, len);
				}
			}
			
			zout.closeEntry();
		}
	}
	
	public static void renameMapInZip(InputStream in, OutputStream out, String newName) throws IOException {
		copyReadZip(in, out, new ZipReadCallback() {
			String originalName = null;
			
			public boolean onRead(InputStream in, OutputStream out, ZipEntry entry) {
				if(!entry.getName().endsWith(".vmt"))
					return false;
				
				return false;
			}
			
			public String onRename(String name, boolean isDirectory) {
				String[] split = name.split("[\\\\/]");
				String newName2 = null;
				
				if(split.length > 3 && split[0].equals("materials") && split[1].equals("maps")) {
					if(originalName == null)
						originalName = split[2];
					
					split[2] = newName;
					
					newName2 = String.join("/", split);
				}
				
				return newName2;
			}
		});
	}
	
	public static abstract class ZipReadCallback{
		public abstract String onRename(String name, boolean isDirectory);
		public abstract boolean onRead(InputStream in, OutputStream out, ZipEntry entry);
	}
}
