package bspentspy;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import SevenZip.Compression.LZMA.Decoder;
import SevenZip.Compression.LZMA.Encoder;
import util.RandomAccessByteOutputStream;

public class BSPFile implements AutoCloseable{
	public boolean dirty;
	
	protected RandomAccessFile bspfile;
	protected ArrayList<Entity> entities;
	protected int bspVersion;
	
	private BSPLump[] lumps;
	private GameLump[] glumps;
	private int mapRev;
	
	protected BSPFile() {
		entities = new ArrayList<Entity>();
		dirty = false;
	}
	
	public ArrayList<Entity> getData(){
		return entities;
	}
	
	public void close() throws IOException {
		if(bspfile == null)
			return;
		bspfile.close();
		bspfile = null;
	}
	
	public boolean read(RandomAccessFile file) throws IOException {
		file.seek(0);
		int id = file.readInt();
		
		if(id != ID_BSP) {
			file.seek(file.getFilePointer() - 4);
			return false;
		}
		
		bspfile = file;
		
		bspVersion = Integer.reverseBytes(bspfile.readInt());
		byte[] lumpBytes = new byte[1024]; //64 lumps, 16 bytes each
		bspfile.read(lumpBytes);
		ByteBuffer lumpBuffer = ByteBuffer.wrap(lumpBytes);
		lumpBuffer.order(ByteOrder.LITTLE_ENDIAN);
		lumps = new BSPLump[64];
		
		for(int i = 0; i < 64; ++i) {
			lumps[i] = new BSPLump();
			lumps[i].index = i;
			lumps[i].offset = Integer.toUnsignedLong(lumpBuffer.getInt());
			lumps[i].length = Integer.toUnsignedLong(lumpBuffer.getInt());
			lumps[i].version = lumpBuffer.getInt();
			lumps[i].fourCC = lumpBuffer.getInt();
		}
		
		mapRev = Integer.reverseBytes(bspfile.readInt());
		
		loadGameLumps();
		loadEntities();
		
		return true;
	}
	
	public void readEntities(BufferedReader br) throws IOException {
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
	
	private void writeHeader(RandomAccessFile out, BSPLump[] newLumps) throws IOException {
		out.seek(0);
		byte[] headerBytes = new byte[1036];
		ByteBuffer buff = ByteBuffer.wrap(headerBytes);
		buff.order(ByteOrder.LITTLE_ENDIAN);
		
		buff.putInt(Integer.reverseBytes(ID_BSP));
		buff.putInt(bspVersion);
		
		for(int i = 0; i < 64; ++i) {
			buff.putInt((int)newLumps[i].offset);
			buff.putInt((int)newLumps[i].length);
			buff.putInt(newLumps[i].version);
			buff.putInt(newLumps[i].fourCC);
		}
		
		buff.putInt(mapRev);
		out.write(headerBytes);
	}
	
	//if saving to the same file set updateSelf to true
	public void save(RandomAccessFile out, boolean updateSelf) throws IOException{
		if(bspfile == null || out == null) return;
		
		BSPLump[] newLumps = new BSPLump[64];
		GameLump[] newGlumps = new GameLump[glumps.length];
		
		List<GenericLump> sorted = new LinkedList<GenericLump>();
		
		int i = 0;
		for(i = 0; i < lumps.length; ++i) {
			newLumps[i] = (BSPLump)lumps[i].clone();
			sorted.add(newLumps[i]);
		}
		byte[] entData = getEntityBytes(newLumps[ENTLUMP]);
		newLumps[ENTLUMP].length = entData.length;
		
		for(i = 0; i < glumps.length; ++i) {
			newGlumps[i] = (GameLump)glumps[i].clone();
			sorted.add(newGlumps[i]);
		}
		
		Collections.sort(sorted);
		int entIndex = sorted.indexOf(newLumps[ENTLUMP]);
		
		if(entIndex < sorted.size() - 1) {
			GenericLump entLump = newLumps[ENTLUMP];
			GenericLump nextLump = sorted.get(entIndex + 1);
			
			if(nextLump.offset < entLump.offset + entLump.length) {
				sorted.remove(entIndex);
				newLumps[ENTLUMP].offset = alignToFour(sorted.get(sorted.size() - 1).offset + sorted.get(sorted.size() - 1).length);
				sorted.add(entLump);
				long orgLen = lumps[ENTLUMP].length;
				
				for(i = entIndex; i < sorted.size() - 1; ++i) {
					sorted.get(i).offset = alignToFour(sorted.get(i).offset - orgLen);
				}
			}
		}
		
		for(i = 0; i < sorted.size(); ++i) {
			GenericLump to = sorted.get(i);
			GenericLump from;
			
			if(to.length == 0)
				continue;
			
			if(to.index == GAMELUMP) {
				saveGameLumps(out, newLumps[GAMELUMP], newGlumps);
				continue;
			} else if(to.index == ENTLUMP) {
				out.seek(newLumps[ENTLUMP].offset);
				out.write(entData);
				continue;
			}
			
			if(to.index >= 4096)
				from = glumps[to.index - 4096];
			else
				from = lumps[to.index];
			
			copy(out, from, to);
		}
		
		writeHeader(out, newLumps);
		
		if(updateSelf) {
			lumps = newLumps;
			glumps = newGlumps;
		}
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
	
	private byte[] getEntityBytes(BSPLump entLump) throws IOException {
		StringBuilder sb = new StringBuilder();
		
		for(Entity e : entities) {
			sb.append("{\n");
			for(int i = 0; i < e.keyvalues.size(); ++i) {
				sb.append("\"").append(e.keyvalues.get(i).key).append("\" \"").append(e.keyvalues.get(i).value).append("\"\n");
			}
			sb.append("}\n");
		}
		
		byte[] uncompressed = sb.toString().getBytes(StandardCharsets.UTF_8);
		
		if(!lumps[ENTLUMP].lzma)
			return uncompressed;
		
		RandomAccessByteOutputStream out = new RandomAccessByteOutputStream();
		ByteArrayInputStream in = new ByteArrayInputStream(uncompressed);
		
		Encoder encoder = new Encoder();
		encoder.SetEndMarkerMode(false);
		encoder.SetMatchFinder(Encoder.EMatchFinderTypeBT4);
		encoder.SetDictionarySize(16777216);
		
		out.seek(17);
		encoder.Code(in, out, -1, -1, null);
		
		out.seek(0);
		out.writeInt(Integer.reverseBytes(ID_LZMA));
		out.writeInt(uncompressed.length);
		out.writeInt(out.size());
		encoder.WriteCoderProperties(out);
		
		if(entLump != null) {
			entLump.fourCC = uncompressed.length;
		}
		
		return out.toByteArray();
	} 

	private void loadGameLumps() throws IOException {
		BSPLump glump = lumps[GAMELUMP];
		
		if(glump.offset == 0)
			return;
		
		bspfile.seek(glump.offset);
		
		int lumpCount = Integer.reverseBytes(bspfile.readInt());
		glumps = new GameLump[lumpCount];
		
		byte[] glumpBytes = new byte[16 * lumpCount];
		bspfile.read(glumpBytes);
		ByteBuffer glumpBuff = ByteBuffer.wrap(glumpBytes);
		glumpBuff.order(ByteOrder.LITTLE_ENDIAN);
		
		for(int i = 0; i < lumpCount; ++i) {
			glumps[i] = new GameLump();
			glumps[i].index = 4096 + i;
			glumps[i].id = glumpBuff.getInt();
			glumps[i].flags = glumpBuff.getShort();
			glumps[i].version = Short.toUnsignedInt(glumpBuff.getShort());
			glumps[i].offset = Integer.toUnsignedLong(glumpBuff.getInt());
			glumps[i].length = Integer.toUnsignedLong(glumpBuff.getInt());
		}
	}
	
	private void saveGameLumps(RandomAccessFile out, GenericLump gameLump, GameLump[] newGlumps) throws IOException {
		out.seek(gameLump.offset);
		
		byte[] glumpBytes = new byte[4 + 16 * newGlumps.length];
		ByteBuffer glumpBuff = ByteBuffer.wrap(glumpBytes);
		glumpBuff.order(ByteOrder.LITTLE_ENDIAN);
		
		glumpBuff.putInt(newGlumps.length);
		for(int i = 0; i < newGlumps.length; ++i) {
			glumpBuff.putInt(newGlumps[i].id);
			glumpBuff.putShort(newGlumps[i].flags);
			glumpBuff.putShort((short)newGlumps[i].version);
			glumpBuff.putInt((int)newGlumps[i].offset);
			glumpBuff.putInt((int)newGlumps[i].length);
		}
		
		out.write(glumpBytes);
	}
	
	private byte[] decompress() throws IOException {
		int id = bspfile.readInt();
		int actualSize = Integer.reverseBytes(bspfile.readInt());
		int lzmaSize = Integer.reverseBytes(bspfile.readInt());
		byte[] properties = new byte[5];
		bspfile.read(properties);
		
		if(id != ID_LZMA) {
			bspfile.seek(bspfile.getFilePointer() - 17);
			return null;
		}
		
		byte[] contents = new byte[lzmaSize];
		bspfile.read(contents);
		ByteArrayInputStream instream = new ByteArrayInputStream(contents);
		ByteArrayOutputStream outstream = new ByteArrayOutputStream(actualSize);
		
		Decoder decoder = new Decoder();
		decoder.SetDecoderProperties(properties);
		decoder.Code(instream, outstream, actualSize);
		
		return outstream.toByteArray();
	}
	
	private void loadEntities() throws IOException {
		BSPLump entLump = lumps[ENTLUMP];
		
		if(entLump.offset == 0)
			return;
		
		bspfile.seek(entLump.offset);
		
		byte[] entData = null;
		if(entLump.fourCC != 0) {
			entLump.lzma = true;
			entData = decompress();
		}
		
		if (entData == null){
			entLump.lzma = false;
			entData = new byte[(int) entLump.length];
			bspfile.read(entData);
		}
		
		BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(entData)));
		readEntities(br);
	}
	
	public static long alignToFour(long offset) {
		return offset + (offset % 4);
	}
	
	public static BSPFile readFile(RandomAccessFile bspfile) throws IOException {
		BSPFile[] supported = {new BSPFile(), new GoldSrcBSPFile()};
		
		for(BSPFile f : supported) {
			if(f.read(bspfile)) {
				return f;
			}
		}
		
		return null;
	}
	
	public static final int ID_BSP = 0x56425350; //big endian
	public static final int ID_LZMA = 0x4C5A4D41; //big endian
	
	private static final int ENTLUMP = 0;
	private static final int GAMELUMP = 35;
	
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
	
	public static class BSPLump extends GenericLump{
		int version;
		int fourCC;
		boolean lzma;
		
		public Object clone() {
			BSPLump clone = new BSPLump();
			clone.index = index;
			clone.offset = offset;
			clone.length = length;
			clone.version = version;
			clone.fourCC = fourCC;
			clone.lzma = lzma;
			
			return clone;
		}
	}
	
	private static class GameLump extends GenericLump{
		int id;
		short flags;
		int version;
		
		public Object clone() {
			GameLump clone = new GameLump();
			clone.index = index;
			clone.offset = offset;
			clone.length = length;
			clone.id = id;
			clone.flags = flags;
			clone.version = version;
			
			return clone;
		}
	}
}
