package bspentspy;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;

import SevenZip.Compression.LZMA.Decoder;

public class BSPFile {
	public boolean dirty;
	
	protected RandomAccessFile bspfile;
	protected ArrayList<Entity> entities;
	protected int bspVersion;
	protected ArrayList<GenericLump> usedLumps;
	
	private BSPLump[] lumps;
	private int mapRev;
	
	protected BSPFile() {
		entities = new ArrayList<Entity>();
		dirty = false;
	}
	
	public ArrayList<Entity> getData(){
		return entities;
	}
	
	public boolean read(RandomAccessFile file) throws IOException {
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
		
		usedLumps = new ArrayList<GenericLump>();
		
		for(int i = 0; i < 64; ++i) {
			lumps[i] = new BSPLump();
			lumps[i].index = i;
			lumps[i].offset = lumpBuffer.getInt();
			lumps[i].length = lumpBuffer.getInt();
			lumps[i].version = lumpBuffer.getInt();
			lumps[i].fourCC = lumpBuffer.getInt();

			if(lumps[i].length > 0) {
				usedLumps.add(lumps[i]);
			}
		}
		
		Collections.sort(usedLumps);
		
		for(int i = 0; i < usedLumps.size(); ++i) {
			GenericLump g = usedLumps.get(i);
			System.out.println(i + ", index: " + g.index + ". length\t\t" + g.length);
		}
		
		mapRev = Integer.reverseBytes(bspfile.readInt());
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
	
	private byte[] decompress(byte[] properties) throws IOException {
		int id = bspfile.readInt();
		int actualSize = Integer.reverseBytes(bspfile.readInt());
		int lzmaSize = Integer.reverseBytes(bspfile.readInt());
		if(properties == null)
			properties = new byte[5];
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
		BSPLump entLump = lumps[0];
		bspfile.seek(entLump.offset);
		
		byte[] entData = null;
		if(entLump.fourCC != 0) {
			entLump.lzmaProperties = new byte[5];
			entData = decompress(entLump.lzmaProperties);
		}
		
		if (entData == null){
			entLump.lzmaProperties = null;
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
			bspfile.seek(0);
			if(f.read(bspfile)) {
				return f;
			}
		}
		
		return null;
	}
	
	public static final int ID_BSP = 0x56425350; //big endian
	public static final int ID_LZMA = 0x4C5A4D41; //big endian
	
	public static class GenericLump implements Comparable<GenericLump>{
		int index;
		long offset;
		long length;

		public int compareTo(GenericLump o) {
			return (int)(this.offset - o.offset);
		}
	}
	
	public static class BSPLump extends GenericLump{
		int version;
		int fourCC;
		byte[] lzmaProperties = null;
	}
}
