package bspentspy;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import SevenZip.Compression.LZMA.Decoder;
import SevenZip.Compression.LZMA.Encoder;
import bspentspy.BSPFile.GenericLump;
import util.RandomAccessByteOutputStream;

public class SourceBSPFile extends BSPFile{
	protected int bspVersion;
	
	private BSPLump[] lumps;
	private GameLump[] glumps;
	private int mapRev;
	
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
		GenericLump last = sorted.get(sorted.size() - 1);
		out.setLength(last.offset + last.length);
		
		if(updateSelf) {
			lumps = newLumps;
			glumps = newGlumps;
		}
	}
	
	private byte[] getEntityBytes(BSPLump entLump) throws IOException {
		byte[] uncompressed = getEntityBytes();
		
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
		
		updateLinks();
	}
	
	public static final int ID_BSP = 0x56425350; //big endian
	public static final int ID_LZMA = 0x4C5A4D41; //big endian
	
	private static final int ENTLUMP = 0;
	private static final int GAMELUMP = 35;
	
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
