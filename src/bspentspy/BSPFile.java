package bspentspy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public abstract class BSPFile implements AutoCloseable{
	public boolean dirty;
	protected RandomAccessFile bspfile;
	protected ArrayList<Entity> entities;
	
	protected BSPFile() {
		entities = new ArrayList<Entity>();
		dirty = false;
	}
	
	public ArrayList<Entity> getEntities(){
		return entities;
	}
	
	public void close() throws IOException {
		if(bspfile == null)
			return;
		bspfile.close();
		bspfile = null;
	}
	
	protected void copy(RandomAccessFile out, GenericLump from, GenericLump to) throws IOException {
		byte[] block = new byte[Math.min(20480, (int)from.length)];
		int blocks = (int)from.length / block.length;
		int remainder = (int)from.length % 20480;
		
		bspfile.seek(from.offset);
		out.seek(to.offset);
		
		for(int i = 0; i < blocks; ++i) {
			bspfile.read(block);
			out.write(block);
		}
		
		if(remainder == 0)
			return;
		bspfile.read(block, 0, remainder);
		out.write(block, 0, remainder);
	}
	
	protected byte[] getEntityBytes() throws IOException {
		StringBuilder sb = new StringBuilder();
		
		for(Entity e : entities) {
			sb.append("{\n");
			for(int i = 0; i < e.keyvalues.size(); ++i) {
				sb.append("\"").append(e.keyvalues.get(i).key).append("\" \"").append(e.keyvalues.get(i).value).append("\"\n");
			}
			sb.append("}\n");
		}
		
		return sb.toString().getBytes(StandardCharsets.UTF_8);
	} 
	
	public abstract boolean read(RandomAccessFile in) throws IOException;
	public abstract void save(RandomAccessFile out, boolean updateSelf) throws IOException;
	
	public static long alignToFour(long offset) {
		return offset + (offset % 4);
	}
	
	public static BSPFile readFile(RandomAccessFile bspfile) throws IOException {
		BSPFile[] supported = {new SourceBSPFile(), new GoldSrcBSPFile()};
		
		for(BSPFile f : supported) {
			if(f.read(bspfile)) {
				return f;
			}
		}
		
		return null;
	}
	
	protected void readEntities(BufferedReader br) throws IOException {
		String line;
		Entity currEnt = null;
		while((line = br.readLine()) != null) {
			if (line.equals("{")) {
				currEnt = new Entity();
				currEnt.index = entities.size();
				continue;
			}
			
			if(line.equals("}")) {
				entities.add(currEnt);
				currEnt = null;
				continue;
			}
			
			String[] fields = line.split("\"", -1);
			if (fields.length == 5) {
				String ckey = fields[1];
				String cval = fields[3];
				//seems like commas are replaced with ESC character in newer versions of BSP (Gmod, TF2)
				currEnt.addKeyVal(ckey, cval);
			}
		}
	}
	
	public static class GenericLump implements Comparable<GenericLump>, Cloneable{
		int index;
		long offset;
		long length;

		public int compareTo(GenericLump o) {
			return (int)(this.offset - o.offset);
		}
		
		public Object clone() {
			GenericLump clone = new GenericLump();
			clone.index = index;
			clone.offset = offset;
			clone.length = length;
			
			return clone;
		}
		
		public String toString() {
			return "index: " + index + String.format("\toffset: %,d\tlen: %,d", offset, length);
		}
	}
}
