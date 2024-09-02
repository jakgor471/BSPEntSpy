package bspentspy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import bspentspy.Entity.KeyValue;

public abstract class BSPFile implements AutoCloseable{
	public boolean entDirty;
	protected RandomAccessFile bspfile;
	protected ArrayList<Entity> entities;
	protected HashMap<String, ArrayList<Entity>> links;
	
	protected BSPFile() {
		entities = new ArrayList<Entity>();
		entDirty = false;
		links = new HashMap<String, ArrayList<Entity>>();
	}
	
	public ArrayList<Entity> getEntities(){
		return entities;
	}
	
	public ArrayList<Entity> getLinkedEntities(Entity e){
		return links.get(e.targetname);
	}
	
	public void updateLinks() {
		links.clear();
		
		HashSet<String> nameSet = new HashSet<String>();
		for(Entity e : entities) {
			if(e.targetname == null || e.targetname.isEmpty())
				continue;
			
			nameSet.add(e.targetname);
			links.put(e.targetname, new ArrayList<Entity>());
		}
		for(Entity e : entities) {
			for(KeyValue kv : e.keyvalues) {
				if(kv.key.equals("targetname"))
					continue;
				String target = kv.getTarget();
				
				if(nameSet.contains(target)) {
					links.get(target).add(e);
				}
			}
		}
	}
	
	public void close() throws IOException {
		if(bspfile == null)
			return;
		bspfile.close();
		bspfile = null;
	}
	
	protected void copy(RandomAccessFile out, GenericLump from, GenericLump to) throws IOException {
		/*
		 * Forward copy is when there's no risk of overriding data
		 */
		boolean forward = from.offset >= to.offset || from.offset + to.length <= to.offset;
		int buffSize = 20480;
		
		if(!forward)
			buffSize = Math.min(buffSize, (int)(to.offset - from.offset));
		
		byte[] block = new byte[Math.min(buffSize, (int)to.length)];
		int blocks = (int)to.length / block.length;
		int remainder = (int)to.length % block.length; //was ... %buffSize, but buffSize is not always equal to block.length!!!! FREAKING BUG ALMOST TORN ALL MY HAIR OFF MY FREAKING SCULP!!!!!!!!!
		
		if(forward) {
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
		} else {
			long off1 = from.offset + to.length - remainder - 1;
			long off2 = to.offset + to.length - remainder - 1;
			
			bspfile.seek(off1);
			bspfile.read(block, 0, remainder);
			out.seek(off2);
			out.write(block, 0, remainder);
			
			off1 -= block.length;
			off2 -= block.length;
			
			for(int i = 0; i < blocks; ++i) {
				bspfile.seek(off1);
				bspfile.read(block);
				out.seek(off2);
				out.write(block);
				
				off1 -= block.length;
				off2 -= block.length;
			}
		}
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
		sb.append('\0');
		
		return sb.toString().getBytes(StandardCharsets.UTF_8);
	} 
	
	public abstract boolean read(RandomAccessFile in) throws IOException;
	public abstract void save(RandomAccessFile out, boolean updateSelf) throws IOException;
	
	public static long alignToFour(long offset) {
		return ((long)(offset + 3) / 4) * 4;
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
