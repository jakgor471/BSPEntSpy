package bspentspy;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import SevenZip.Compression.LZMA.Decoder;
import SevenZip.Compression.LZMA.Encoder;
import bspentspy.Octree.IOriginThing;
import bspentspy.Octree.Vector;
import util.RandomAccessByteOutputStream;

public class SourceBSPFile extends BSPFile{
	public static final float MAPSIZE = 65536;
	
	protected int bspVersion;
	protected Octree<BSPWorldLight> theWorldofLightsLDR;
	protected Octree<BSPWorldLight> theWorldofLightsHDR;
	protected boolean writeLights = true;
	protected boolean hasLights = false;
	
	private BSPLump[] lumps;
	private GameLump[] glumps;
	private ArrayList<BSPWorldLight> hdrLights;
	private ArrayList<BSPWorldLight> ldrLights;
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
		loadLights();
		loadPak();
		
		return true;
	}
	
	private void loadPak() throws IOException {
		bspfile.seek(lumps[PAKLUMP].offset);
		
		FileInputStream is = new FileInputStream(bspfile.getFD());
		ZipInputStream zis = new ZipInputStream(is);
		ArrayList<ZIPPart> pakfiles = new ArrayList<ZIPPart>();
		
		ZipEntry ze = null;
		while((ze = zis.getNextEntry()) != null) {
			byte[] data = new byte[(int)ze.getSize()];
			zis.read(data);
			
			ZIPPart part = new ZIPPart(ze, data);
			pakfiles.add(part);
		}
		
		System.out.println("PakLump size: " + (double)lumps[PAKLUMP].length / 1000000.0D + "MB");
	}
	
	public void WritePakToStream(OutputStream fo) throws IOException {
		bspfile.seek(lumps[PAKLUMP].offset);
		
		long blocks = lumps[PAKLUMP].length / 20480;
		int remainder = (int)(lumps[PAKLUMP].length % 20480);
		byte[] block = new byte[20480];
		
		for(int i = 0; i < blocks; ++i) {
			bspfile.read(block);
			fo.write(block);
		}
		
		bspfile.read(block, 0, remainder);
		fo.write(block, 0, remainder);
	}
	
	public void removeLightAt(Vector pos) {
		BSPWorldLight ldr = theWorldofLightsLDR.findClosest(pos.x, pos.y, pos.z);
		BSPWorldLight hdr = theWorldofLightsHDR.findClosest(pos.x, pos.y, pos.z);
		
		if(pos.distToSqr(ldr.origin) < 0.125f) {
			theWorldofLightsLDR.remove(ldr);
			ldrLights.remove(ldr);
		}
		if(pos.distToSqr(hdr.origin) < 0.125f) {
			theWorldofLightsHDR.remove(hdr);
			hdrLights.remove(hdr);
		}
	}
	
	private byte[] getLightData(int type) {
		if(!writeLights)
			return new byte[0];
		
		ArrayList<BSPWorldLight> arr = ldrLights;
		
		if(type == WORLDLIGHTLUMP_HDR)
			arr = hdrLights;
		byte[] bytes = new byte[arr.size() * 88];
		ByteBuffer buff = ByteBuffer.wrap(bytes);
		buff.order(ByteOrder.LITTLE_ENDIAN);
		
		for(BSPWorldLight l : arr) {
			buff.putFloat(l.origin.x);
			buff.putFloat(l.origin.y);
			buff.putFloat(l.origin.z);
			buff.putFloat(l.intensity[0]);
			buff.putFloat(l.intensity[1]);
			buff.putFloat(l.intensity[2]);
			buff.putFloat(l.normal[0]);
			buff.putFloat(l.normal[1]);
			buff.putFloat(l.normal[2]);
			
			buff.putInt(l.cluster);
			buff.putInt(l.type);
			buff.putInt(l.style);
			
			buff.putFloat(l.stopdot);
			buff.putFloat(l.stopdot2);
			buff.putFloat(l.exponent);
			buff.putFloat(l.radius);
			buff.putFloat(l.constant_attn);
			buff.putFloat(l.linear_attn);
			buff.putFloat(l.quadratic_attn);
			
			buff.putInt(l.flags);
			buff.putInt(l.texinfo);
			buff.putInt(l.owner);
		}
		
		return bytes;
	}
	
	private void loadLights() throws IOException {
		int numldrLights = (int)(lumps[WORLDLIGHTLUMP_LDR].length / 88);
		int numhdrLights = (int)(lumps[WORLDLIGHTLUMP_HDR].length / 88);
		
		final float halfmapsize = MAPSIZE / 2;
		//theWorldofLightsLDR = new Octree<BSPWorldLight>(-halfmapsize, -halfmapsize, -halfmapsize, MAPSIZE);
		//theWorldofLightsHDR = new Octree<BSPWorldLight>(-halfmapsize, -halfmapsize, -halfmapsize, MAPSIZE);
		
		hasLights = numldrLights + numhdrLights > 0;
		
		ldrLights = new ArrayList<BSPWorldLight>(numldrLights);
		hdrLights = new ArrayList<BSPWorldLight>(numhdrLights);
		
		int num = numldrLights;
		BSPLump lump = lumps[WORLDLIGHTLUMP_LDR];
		ArrayList<BSPWorldLight> arr = ldrLights;
		
		Octree<BSPWorldLight> world = theWorldofLightsLDR;
		for(int i = 0; i < 2; ++i) {			
			bspfile.seek(lump.offset);
			byte[] lightBytes = new byte[(int)lump.length];
			ByteBuffer lightBuff = ByteBuffer.wrap(lightBytes);
			lightBuff.order(ByteOrder.LITTLE_ENDIAN);
			
			bspfile.read(lightBytes);
			
			System.out.println("Lights: ");
			for(int j = 0; j < num; ++j) {
				BSPWorldLight lgt = new BSPWorldLight();
				lgt.origin = new Vector();
				lgt.intensity = new float[3];
				lgt.normal = new float[3];
				lgt.origin.x = lightBuff.getFloat();
				lgt.origin.y = lightBuff.getFloat();
				lgt.origin.z = lightBuff.getFloat();
				lgt.intensity[0] = lightBuff.getFloat(); // R * A * 39.215
				lgt.intensity[1] = lightBuff.getFloat();
				lgt.intensity[2] = lightBuff.getFloat();
				lgt.normal[0] = lightBuff.getFloat();
				lgt.normal[1] = lightBuff.getFloat();
				lgt.normal[2] = lightBuff.getFloat();
				
				lgt.cluster = lightBuff.getInt();
				lgt.type = lightBuff.getInt();
				lgt.style = lightBuff.getInt();
				
				lgt.stopdot = lightBuff.getFloat();
				lgt.stopdot2 = lightBuff.getFloat();
				lgt.exponent = lightBuff.getFloat();
				lgt.radius = lightBuff.getFloat();
				lgt.constant_attn = lightBuff.getFloat();
				lgt.linear_attn = lightBuff.getFloat();
				lgt.quadratic_attn = lightBuff.getFloat();
				
				lgt.flags = lightBuff.getInt();
				lgt.texinfo = lightBuff.getInt();
				lgt.owner = lightBuff.getInt();
				
				//System.out.println(lgt + "\n");
				arr.add(lgt);
				//world.insert(lgt);
			}
			
			num = numhdrLights;
			lump = lumps[WORLDLIGHTLUMP_HDR];
			arr = hdrLights;
			//world = theWorldofLightsHDR;
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
		
		ArrayList<GenericLump> sorted = new ArrayList<GenericLump>();
		
		int i = 0;
		for(i = 0; i < lumps.length; ++i) {
			newLumps[i] = (BSPLump)lumps[i].clone();
			
			sorted.add(newLumps[i]);
		}
		for(i = 0; i < glumps.length; ++i) {
			newGlumps[i] = (GameLump)glumps[i].clone();
		}
		byte[] entData = getEntityBytes(newLumps[ENTLUMP]);
		newLumps[ENTLUMP].length = entData.length;
		
		/*byte[] hdrLightData = null;
		byte[] ldrLightData = null;
		hdrLightData = getLightData(WORLDLIGHTLUMP_HDR);
		ldrLightData = getLightData(WORLDLIGHTLUMP_LDR);*/
		if(!writeLights) {
			newLumps[WORLDLIGHTLUMP_HDR].length = 0;
			newLumps[WORLDLIGHTLUMP_HDR].offset = 0;
			newLumps[WORLDLIGHTLUMP_LDR].length = 0;
			newLumps[WORLDLIGHTLUMP_LDR].offset = 0;
		}
		
		Collections.sort(sorted);
		/*int entIndex = sorted.indexOf(newLumps[ENTLUMP]);
		
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
		}*/
		GenericLump cur = sorted.get(0);
		cur.offset = Math.max(cur.offset, 1036);
		System.out.println("=================");
		long[] diffs = new long[sorted.size()];
		for(i = 0; i < sorted.size() - 1; ++i) {
			cur = sorted.get(i);
			diffs[i] = cur.offset - lumps[cur.index].offset;
			
			GenericLump next = sorted.get(i + 1);
			System.out.println(next.index +"\nPRE\t" + next.offset);
			next.offset = alignToFour(cur.offset + cur.length);
			System.out.println("POST\t" + next.offset);
		}
		
		cur = sorted.get(sorted.size() - 1);
		long totalLen = cur.offset + cur.length;
		
		for(i = 0; i < sorted.size(); ++i) {
			GenericLump to = sorted.get(i);
			if(to.length == 0)
				continue;
			
			if(diffs[i] <= 0) {
				//difference of offsets is non positive, we can forward-copy them
				if(to.index == GAMELUMP) {
					copy(out, lumps[GAMELUMP], to);
					saveGameLumps(out, newLumps[GAMELUMP], newGlumps);
					continue;
				} else if(to.index == ENTLUMP) {
					out.seek(newLumps[ENTLUMP].offset);
					out.write(entData);
					continue;
				}
				
				copy(out, lumps[to.index], to);
			} else {
				//difference of offsets is positive, we must backward-copy
				int startIndex = i;
				++i;
				while(i < sorted.size() && diffs[i] > 0)
					++i;
				
				for(int j = i; j >= startIndex; --j) {
					to = sorted.get(j);
					if(to.index == GAMELUMP) {
						copy(out, lumps[GAMELUMP], to);
						saveGameLumps(out, newLumps[GAMELUMP], newGlumps);
						continue;
					} else if(to.index == ENTLUMP) {
						out.seek(newLumps[ENTLUMP].offset);
						out.write(entData);
						continue;
					}
					
					if(to.length == 0)
						continue;
					
					copy(out, lumps[to.index], to);
				}
			}
		}
		
		/*for(i = sorted.size() - 1; i >= 0; --i) {
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
			
			if(to.index >= GLUMP_INDEX_OFF)
				from = glumps[to.index - GLUMP_INDEX_OFF];
			else
				from = lumps[to.index];
			
			copy(out, from, to);
		}*/
		
		writeHeader(out, newLumps);
		out.setLength(totalLen);
		
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
			glumps[i].index = GLUMP_INDEX_OFF + i;
			glumps[i].id = glumpBuff.getInt();
			glumps[i].flags = glumpBuff.getShort();
			glumps[i].version = Short.toUnsignedInt(glumpBuff.getShort());
			glumps[i].offset = Integer.toUnsignedLong(glumpBuff.getInt()) - lumps[GAMELUMP].offset; //make it relative to GameLump
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
			glumpBuff.putInt((int)(newGlumps[i].offset + gameLump.offset));
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
	
	private static final int GLUMP_INDEX_OFF = 4096;
	
	private static final int ENTLUMP = 0;
	private static final int GAMELUMP = 35;
	private static final int WORLDLIGHTLUMP_LDR = 15;
	private static final int WORLDLIGHTLUMP_HDR = 54;
	private static final int PAKLUMP = 40;
	
	public static class BSPWorldLight implements IOriginThing {
		public static final int EMIT_SURF = 0;
		public static final int EMIT_POINT = 1;
		public static final int EMIT_SPOTLIGHT = 2;
		public static final int EMIT_SKYLIGHT = 3;
		public static final int EMIT_QUAKELIGHT = 4;
		public static final int EMIT_AMBIENT = 5;
		
		public static final String[] EMIT = {"Surface", "Point", "Spotlight", "Skylight", "Quakelight", "Ambient"};
		
		Vector origin;
		float[] intensity;
		float[] normal;
		int cluster;
		int type;
	    int style;
		float stopdot;
		float stopdot2;
		float exponent;
		float radius;
		float constant_attn;
		float linear_attn;
		float quadratic_attn;
		int flags;
		int texinfo;
		int	owner;
		
		public Vector getOrigin() {
			return origin;
		}
		
		public String toString() {
			return String.format("origin: (%.4f %.4f %.4f)"
					+ "\nintensity: (%.4f %.4f %.4f)"
					+ "\nnormal: (%.4f %.4f %.4f)"
					+ "\ncluster: %d\ntype: %s\nstyle: %d",
					origin.x, origin.y, origin.z, intensity[0], intensity[1], intensity[2], normal[0], normal[1], normal[2],
					cluster, EMIT[type], style
					);
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
	
	private static class ZIPPart{
		ZipEntry entry;
		byte[] data;
		
		public ZIPPart(ZipEntry entry, byte[] data) {
			this.entry = entry;
			this.data = data;
		}
	}
	
}
