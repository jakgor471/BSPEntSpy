package bspentspy;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.zip.ZipEntry;

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
	protected boolean writePak = true;
	protected boolean hasLights = true;
	protected File embeddedPak = null;
	
	private int targetSpropVersion = -1;
	private int curSpropVersion = -1;
	private BSPLump[] lumps;
	private GameLump[] glumps;
	private GameLump staticPropLump = null;
	private ArrayList<BSPWorldLight> hdrLights;
	private ArrayList<BSPWorldLight> ldrLights;
	private ArrayList<ZipPart> pakfiles = null;
	private int mapRev;
	private boolean glumpsRelative = false;
	
	private ArrayList<EntityCubemap> cubemaps = null;
	private ArrayList<EntityStaticProp> staticProps = null;
	
	public boolean read(RandomAccessFile file) throws IOException {
		file.seek(0);
		int id = file.readInt();
		
		if(id != ID_BSP) {
			file.seek(file.getFilePointer() - 4);
			return false;
		}
		
		writeLights = true;
		writePak = true;
		hasLights = true;
		embeddedPak = null;
		entDirty = true; //at the moment ALWAYS write entities
		pakfiles = null;
		
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
	
	public void unloadCubemaps() {
		if(cubemaps == null)
			return;
		
		cubemaps = null;
		
		for(int i = 0; i < entities.size(); ++i) {
			if(entities.get(i) instanceof EntityCubemap) {
				entities.remove(i);
				--i;
			}
		}
	}
	
	public void unloadStaticProps() {
		if(staticProps == null)
			return;
		
		staticProps = null;
		
		for(int i = 0; i < entities.size(); ++i) {
			if(entities.get(i) instanceof EntityStaticProp) {
				entities.remove(i);
				--i;
			}
		}
	}
	
	public boolean isSpropLumpLoaded() {
		return staticProps != null;
	}
	
	private byte[] getStaticPropsBytes() throws IOException {
		if(staticProps == null)
			return null;
		
		try(RandomAccessByteOutputStream lumpBytes = new RandomAccessByteOutputStream()){
			lumpBytes.seek(4);
			
			HashMap<String, Integer> dict = new HashMap<String, Integer>();
			short[] nameIndices = new short[staticProps.size()];
			int numNames = 0;
			
			for(int i = 0; i < staticProps.size(); ++i) {
				String name = staticProps.get(i).getKeyValue("model");
				Integer index = dict.get(name);
				
				if(index == null) {
					byte[] strBytes = name.getBytes(utf8Charset);
					lumpBytes.write(strBytes, 0, Math.min(strBytes.length, 128));
					
					int padding = 128 - Math.min(strBytes.length, 128);
					lumpBytes.write(new byte[padding]);
					
					index = numNames;
					++numNames;
					dict.put(name, index);
				}
				
				nameIndices[i] = index.shortValue();
			}
			
			int lastPos = lumpBytes.tell();
			lumpBytes.seek(0);
			lumpBytes.writeInt(numNames);
			lumpBytes.seek(lastPos + 4);
			
			int numLeaves = 0;
			short[] firstLeaves = new short[staticProps.size()];
			short[] leafCounts = new short[staticProps.size()];
			
			for(int i = 0; i < staticProps.size(); ++i) {
				String[] strLeaves = staticProps.get(i).getKeyValue("leaves").split(",\\s*");
				firstLeaves[i] = (short)numLeaves;
				leafCounts[i] = (short)strLeaves.length;
				
				for(String s : strLeaves) {
					lumpBytes.writeShort((short)parseInt(s, 0));
				}
				
				numLeaves += strLeaves.length;
			}
			
			int lastPos2 = lumpBytes.tell();
			lumpBytes.seek(lastPos);
			lumpBytes.writeInt(numLeaves);
			lumpBytes.seek(lastPos2);
			lumpBytes.writeInt(staticProps.size());
			
			for(int i = 0; i < staticProps.size(); ++i) {
				EntityStaticProp prop = staticProps.get(i);
				String[] vecSplit = prop.getKeyValue("origin").split("\\s+");
				
				for(int j = 0; j < 3; ++j) {
					float value = 0;
					
					if(j < vecSplit.length) {
						value = parseFloat(vecSplit[j], 0f);
					}
					
					lumpBytes.writeFloat(value);
				}
				
				vecSplit = prop.getKeyValue("angles").split("\\s+");
				
				for(int j = 0; j < 3; ++j) {
					float value = 0;
					
					if(j < vecSplit.length) {
						value = parseFloat(vecSplit[j], 0f);
					}
					
					lumpBytes.writeFloat(value);
				}
				
				lumpBytes.writeShort(nameIndices[i]);
				lumpBytes.writeShort(firstLeaves[i]);
				lumpBytes.writeShort(leafCounts[i]);
				
				lumpBytes.write(parseByte(prop.getKeyValue("solid"), 0));
				lumpBytes.write(prop.flags);
				lumpBytes.writeInt(parseInt(prop.getKeyValue("skin"), 0));
				lumpBytes.writeFloat(parseFloat(prop.getKeyValue("fademindist"), 0));
				lumpBytes.writeFloat(parseFloat(prop.getKeyValue("fademaxdist"), 0));
				
				vecSplit = prop.getKeyValue("lightingorigin").split("\\s+");
				
				for(int j = 0; j < 3; ++j) {
					float value = 0;
					
					if(j < vecSplit.length) {
						value = parseFloat(vecSplit[j], 0f);
					}
					
					lumpBytes.writeFloat(value);
				}
				
				if(targetSpropVersion >= SPROPLUMP_V5) {
					lumpBytes.writeFloat(parseFloat(prop.getKeyValue("fadescale"), 0));
				}
				
				if(targetSpropVersion == SPROPLUMP_V6) {
					lumpBytes.writeShort((short)parseInt(prop.getKeyValue("mindxlevel"), 0));
					lumpBytes.writeShort((short)parseInt(prop.getKeyValue("maxdxlevel"), 0));
				}
			}
			
			return lumpBytes.toByteArray();
		} catch(IOException e) {
			throw e;
		}
	}
	
	private byte parseByte(String s, int defValue) {
		byte val = (byte)defValue;
		try {
			val = Byte.valueOf(s);
		} catch(NumberFormatException e) {}
		
		return val;
	}
	
	private float parseFloat(String s, float defValue) {
		float val = defValue;
		try {
			val = Float.valueOf(s);
		} catch(NumberFormatException e) {}
		
		return val;
	}
	
	private int parseInt(String s, int defValue) {
		int val = defValue;
		try {
			val = Integer.valueOf(s);
		} catch(NumberFormatException e) {}
		
		return val;
	}
	
	public void setStaticPropVersion(String ver) {
		if(ver.equals("v4"))
			targetSpropVersion = SPROPLUMP_V4;
		else if(ver.equals("v5"))
			targetSpropVersion = SPROPLUMP_V5;
		else if(ver.equals("v6"))
			targetSpropVersion = SPROPLUMP_V6;
		else
			targetSpropVersion = -1; //-1 to save with original sprop version
	}
	
	public String getStaticPropVersion() {
		final String[] lookup = {"v4", "v5", "v6"};
		
		if(curSpropVersion > -1 && curSpropVersion < lookup.length)
			return lookup[curSpropVersion];
		
		if(staticPropLump != null)
			return "v" + Integer.toString(staticPropLump.version);
		
		return "";
	}
	
	public void loadStaticProps() throws Exception{
		if(staticPropLump == null)
			return;
		
		switch(staticPropLump.version) {
		case 4:
			curSpropVersion = SPROPLUMP_V4;
			break;
		case 5:
			curSpropVersion = SPROPLUMP_V5;
			break;
		case 6:
			curSpropVersion = SPROPLUMP_V6;
			break;
		default:
			curSpropVersion = SPROPLUMP_INVALIDVERSION;
		}
		
		if(curSpropVersion == SPROPLUMP_INVALIDVERSION) {
			throw new Exception("Not supported Static prop lump version (" + getStaticPropVersion() + ")!");
		}
		
		bspfile.seek(lumps[GAMELUMP].offset + staticPropLump.offset);
		int dictEntries = Integer.reverseBytes(bspfile.readInt());
		
		byte[] block = new byte[128];		
		String[] dict = new String[dictEntries];
		
		for(int i = 0; i < dictEntries; ++i) {
			bspfile.read(block);
			int j = 0;
			for(; j < block.length && block[j] > 0; ++j) {}
			
			dict[i] = new String(block, 0, j, utf8Charset);
		}
		
		int leafEntries = Integer.reverseBytes(bspfile.readInt());
		short[] leafDict = new short[leafEntries];
		block = new byte[leafEntries * 2];
		bspfile.read(block);
		
		ByteBuffer buff = ByteBuffer.wrap(block);
		buff.order(ByteOrder.LITTLE_ENDIAN);
		
		for(int i = 0; i < leafEntries; ++i) {
			leafDict[i] = buff.getShort();
		}
		
		int numProps = Integer.reverseBytes(bspfile.readInt());
		//long propLumpLen = staticPropLump.length - 4 - dictEntries * 128 - 4 - leafEntries * 2 - 4;
		
		block = new byte[numProps * SPROPLUMP_SIZE[curSpropVersion]];
		bspfile.read(block);
		
		buff = ByteBuffer.wrap(block);
		buff.order(ByteOrder.LITTLE_ENDIAN);
		
		staticProps = new ArrayList<EntityStaticProp>(numProps);
		
		for(int i = 0; i < numProps; ++i) {
			EntityStaticProp prop = new EntityStaticProp();
			float x = buff.getFloat();
			float y = buff.getFloat();
			float z = buff.getFloat();
			
			prop.setKeyVal("origin", Float.toString(x) + " " + Float.toString(y) + " " + Float.toString(z));
			
			x = buff.getFloat();
			y = buff.getFloat();
			z = buff.getFloat();
			
			prop.setKeyVal("angles", Float.toString(x) + " " + Float.toString(y) + " " + Float.toString(z));
			
			short propType = buff.getShort();
			prop.setKeyVal("model", dict[propType]);
			
			int firstLeaf = Short.toUnsignedInt(buff.getShort());
			int leafCount = Short.toUnsignedInt(buff.getShort());
			
			StringBuilder leaves = new StringBuilder();
			for(int j = 0; j < leafCount; ++j) {
				leaves.append(Integer.toUnsignedString(leafDict[firstLeaf + j]));
				
				if(j != leafCount - 1)
					leaves.append(", ");
			}
			
			prop.setKeyVal("leaves", leaves.toString());
			prop.setKeyVal("solid", Integer.toString(buff.get()));
			prop.flags = buff.get();
			prop.setKeyVal("skin", Integer.toString(buff.getInt()));
			prop.setKeyVal("fademindist", Float.toString(buff.getFloat()));
			prop.setKeyVal("fademaxdist", Float.toString(buff.getFloat()));
			
			x = buff.getFloat();
			y = buff.getFloat();
			z = buff.getFloat();
			
			prop.setKeyVal("lightingorigin", Float.toString(x) + " " + Float.toString(y) + " " + Float.toString(z));
			
			if(curSpropVersion >= SPROPLUMP_V5) {
				prop.setKeyVal("fadescale", Float.toString(buff.getFloat()));
			}
			
			if(curSpropVersion == SPROPLUMP_V6) {
				prop.setKeyVal("mindxlevel", Integer.toUnsignedString(buff.getShort()));
				prop.setKeyVal("maxdxlevel", Integer.toUnsignedString(buff.getShort()));
			}
			
			staticProps.add(prop);
			entities.add(prop);
		}
	}
	
	public byte[] getCubemapBytes() throws IOException{
		byte[] cubemapData = new byte[cubemaps.size() * 16];
		ByteBuffer buff = ByteBuffer.wrap(cubemapData);
		buff.order(ByteOrder.LITTLE_ENDIAN);
		
		for(int i = 0; i < cubemaps.size(); ++i) {
			EntityCubemap cubemap = cubemaps.get(i);
			buff.putInt(cubemap.cubemapOrigin[0]);
			buff.putInt(cubemap.cubemapOrigin[1]);
			buff.putInt(cubemap.cubemapOrigin[2]);
			
			int size = 0;
			try {
				size = Integer.valueOf(cubemap.getKeyValue("cubemapsize"));
			} catch(NumberFormatException e) {
				System.out.println("ERROR! Could not parse cubemapsize on cubemap(" + cubemap.cubemapOrigin[0] + " " + cubemap.cubemapOrigin[1] + " " + cubemap.cubemapOrigin[2] + ")!");
			}
			
			buff.putInt(Math.max(Math.min(size, 13), 0));
		}
		
		return cubemapData;
	}

	public void loadCubemaps() throws IOException {
		unloadCubemaps();
		
		int numCubemaps = (int)(lumps[CUBEMAPLUMP].length / 16);
		cubemaps = new ArrayList<EntityCubemap>(numCubemaps);
		
		bspfile.seek(lumps[CUBEMAPLUMP].offset);
		byte[] cubemapData = new byte[(int)lumps[CUBEMAPLUMP].length];
		ByteBuffer buff = ByteBuffer.wrap(cubemapData);
		buff.order(ByteOrder.LITTLE_ENDIAN);
		
		bspfile.read(cubemapData);
		for(int i = 0; i < numCubemaps; ++i) {
			EntityCubemap cubemap = new EntityCubemap();		
			int x = buff.getInt();
			int y = buff.getInt();
			int z = buff.getInt();
			cubemap.cubemapOrigin[0] = x;
			cubemap.cubemapOrigin[1] = y;
			cubemap.cubemapOrigin[2] = z;
			int size = buff.getInt();
			cubemap.setKeyVal("origin(readonly)", x + " " + y + " " + z);
			cubemap.setKeyVal("cubemapsize", String.valueOf(size));
			
			entities.add(cubemap);
			cubemaps.add(cubemap);
		}
	}
	
	public void writePakToStream(OutputStream fo) throws IOException {
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
	
	private void writeHeader(RandomAccessFile out, BSPLump[] newLumps) throws IOException {
		out.seek(0);
		byte[] headerBytes = new byte[1036];
		ByteBuffer buff = ByteBuffer.wrap(headerBytes);
		buff.order(ByteOrder.LITTLE_ENDIAN);
		
		buff.putInt(Integer.reverseBytes(ID_BSP));
		buff.putInt(bspVersion);
		
		for(int i = 0; i < 64; ++i) {
			long off = newLumps[i].offset;
			if(newLumps[i].length <= 0)
				off = 0;
			buff.putInt((int)off);
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
		byte[] entData = null;
		byte[] cubemapData = null;
		byte[][] gameLumpData = new byte[newGlumps.length][];
		
		if(entDirty) {
			entData = getEntityBytes(newLumps[ENTLUMP]);
			newLumps[ENTLUMP].length = entData.length;
		}
		
		if(cubemaps != null) {
			cubemapData = getCubemapBytes();
			newLumps[CUBEMAPLUMP].length = cubemapData.length;
		}
		
		if(!writeLights) {
			newLumps[WORLDLIGHTLUMP_HDR].length = 0;
			newLumps[WORLDLIGHTLUMP_LDR].length = 0;
		}
		
		if(!writePak) {
			newLumps[PAKLUMP].length = 0;
		}
		
		if(staticProps != null && staticPropLump != null) {
			if(targetSpropVersion > -1)
				newGlumps[staticPropLump.index].version = SPROPLUMP_VERSIONLOOKUP[targetSpropVersion];
			byte[] spropData = getStaticPropsBytes();
			newGlumps[staticPropLump.index].length = spropData.length;
			gameLumpData[staticPropLump.index] = spropData;
			staticPropLump = newGlumps[staticPropLump.index];
		}
		
		FileInputStream pakIs = null;
		if(embeddedPak != null) {
			try {
				pakIs = new FileInputStream(embeddedPak);
				newLumps[PAKLUMP].length = pakIs.getChannel().size();
				
				newLumps[PAKLUMP].offset = Long.MAX_VALUE; //let the PAK lump be at the end so writing to it does not require shifting everything
				
			} catch(FileNotFoundException e) {
				e.printStackTrace();
				
				if(pakIs != null)
					pakIs.close();
				embeddedPak = null;
			}
		}
		
		ArrayList<GameLump> sortedGlumps = updateGameLumps(newLumps[GAMELUMP], newGlumps);
		Collections.sort(sorted);

		GenericLump cur = sorted.get(0);
		cur.offset = Math.max(cur.offset, 1036);

		for(i = 0; i < sorted.size() - 1; ++i) {
			cur = sorted.get(i);
			
			GenericLump next = sorted.get(i + 1);
			next.offset = alignToFour(cur.offset + cur.length);
		}
		
		cur = sorted.get(sorted.size() - 1);
		long totalLen = cur.offset + cur.length;
		
		System.out.println("===\tWRITE\t===");
		for(i = 0; i < sorted.size(); ++i) {
			GenericLump l = sorted.get(i);
			BSPLump org = lumps[l.index];
			System.out.println(l.index + " ===================================================");
			System.out.println(String.format("NEW\t %,11d\t %,11d\t %,11d", l.offset, l.length, l.offset + l.length));
			System.out.println(String.format("ORG\t %,11d\t %,11d\t %,11d", org.offset, org.length, org.offset + org.length));
		}
		
		for(i = 0; i < sorted.size(); ++i) {
			GenericLump to = sorted.get(i);
			
			if(to.length == 0)
				continue;
			
			if(i >= 63 || to.offset <= lumps[to.index].offset && to.length <= lumps[to.index].length) {
				//difference of offsets is non positive, we can forward-copy them
				if(to.index == GAMELUMP) {
					saveGameLumps(out, newLumps[GAMELUMP], sortedGlumps, gameLumpData);
					continue;
				} else if(to.index == ENTLUMP && entDirty) {
					out.seek(newLumps[ENTLUMP].offset); //entities are written first and they DESTROY the lump 15!!!!
					out.write(entData);
					continue;
				} else if(to.index == PAKLUMP && pakIs != null) {
					out.seek(newLumps[PAKLUMP].offset);
					copy(out, pakIs, newLumps[PAKLUMP].length);
					continue;
				} else if(to.index == CUBEMAPLUMP && cubemapData != null) {
					out.seek(newLumps[CUBEMAPLUMP].offset);
					out.write(cubemapData);
					continue;
				}
				
				copy(out, lumps[to.index], to);
			} else {
				//difference of offsets is positive, we must backward-copy
				int startIndex = i;
				to = sorted.get(++i);

				for(; i < sorted.size() && (to.offset > lumps[to.index].offset || to.length > lumps[to.index].length); ++i) {
					to = sorted.get(i);
				}
				
				for(int j = i - 1; j >= startIndex; --j) {
					to = sorted.get(j);
					
					if(to.length == 0)
						continue;
					
					if(to.index == GAMELUMP) {
						saveGameLumps(out, newLumps[GAMELUMP], sortedGlumps, gameLumpData);
						continue;
					} else if(to.index == ENTLUMP && entDirty) {
						out.seek(newLumps[ENTLUMP].offset);
						out.write(entData);
						continue;
					} else if(to.index == PAKLUMP && pakIs != null) {
						out.seek(newLumps[PAKLUMP].offset);
						copy(out, pakIs, newLumps[PAKLUMP].length);
						continue;
					} else if(to.index == CUBEMAPLUMP && cubemapData != null) {
						out.seek(newLumps[CUBEMAPLUMP].offset);
						out.write(cubemapData);
						continue;
					}
					
					copy(out, lumps[to.index], to);
				}
			}
		}
		
		if(pakIs != null)
			pakIs.close();
		
		writeHeader(out, newLumps);
		out.setLength(totalLen);
		
		if(updateSelf) {
			lumps = newLumps;
			glumps = newGlumps;
			embeddedPak = null;
			writePak = true;
			writeLights = true;
			curSpropVersion = targetSpropVersion;
		}
	}
	
	private ArrayList<GameLump> updateGameLumps(GenericLump glump, GameLump[] newGlumps) {
		if(newGlumps.length < 1)
			return null;
		
		ArrayList<GameLump> sorted = new ArrayList<GameLump>();
		
		for(GameLump g : newGlumps)
			sorted.add(g);
		
		Collections.sort(sorted);
		int headerSize = 4 + sorted.size() * 16;
		sorted.get(0).offset = alignToFour(headerSize);
		
		for(int i = 0; i < sorted.size() - 1; ++i) {
			GameLump cur = sorted.get(i);
			GameLump next = sorted.get(i + 1);
			
			next.offset = alignToFour(cur.offset + cur.length);
		}
		
		GameLump last = sorted.get(sorted.size() - 1);
		glump.length = last.offset + last.length;
		
		return sorted;
	}
	
	private void saveGameLumps(RandomAccessFile out, GenericLump newGamelump, ArrayList<GameLump> sorted, byte[][] gameLumpData) throws IOException {
		copy(out, lumps[GAMELUMP], newGamelump);
		out.seek(newGamelump.offset);
		
		if(gameLumpData == null)
			gameLumpData = new byte[sorted.size()][];
		
		byte[] glumpHeaderBytes = new byte[4 + 16 * sorted.size()];
		ByteBuffer glumpBuff = ByteBuffer.wrap(glumpHeaderBytes);
		glumpBuff.order(ByteOrder.LITTLE_ENDIAN);
		
		long glumpOffset = newGamelump.offset;
		
		if(glumpsRelative)
			glumpOffset = 0;
		
		glumpBuff.putInt(sorted.size());
		for(int i = 0; i < sorted.size(); ++i) {
			GameLump glump = sorted.get(i);
			glumpBuff.putInt(glump.id);
			glumpBuff.putShort(glump.flags);
			glumpBuff.putShort((short)glump.version);
			glumpBuff.putInt((int)(glump.offset + glumpOffset));
			glumpBuff.putInt((int)glump.length);
			
			//temporarily make glumps relative to start of the file
			glumps[glump.index].offset += lumps[GAMELUMP].offset;
			glump.offset += newGamelump.offset;
		}
		
		System.out.println("===\tGAMELUMPS\t===");
		for(int i = 0; i < sorted.size(); ++i) {
			GenericLump l = sorted.get(i);
			GameLump org = glumps[l.index];
			System.out.println(l.index + " ===================================================");
			System.out.println(String.format("NEW\t %,11d\t %,11d\t %,11d", l.offset, l.length, l.offset + l.length));
			System.out.println(String.format("ORG\t %,11d\t %,11d\t %,11d", org.offset, org.length, org.offset + org.length));
		}
		
		out.write(glumpHeaderBytes);
		
		for(int i = 0; i < sorted.size(); ++i) {
			/*GameLump to = sorted.get(i);
			GameLump from = glumps[to.index];
			
			if(to.length == 0)
				continue;
			
			if(i >= glumps.length - 1 || to.offset <= from.offset && to.offset + to.length <= sorted.get(i + 1).offset) {
				//difference of offsets is non positive, we can forward-copy them
				if(gameLumpData[to.index] != null) {
					out.seek(to.offset);
					out.write(gameLumpData[to.index]);
					continue;
				}
				
				copy(out, glumps[to.index], to);
			} else {
				//difference of offsets is positive, we must backward-copy
				int startIndex = i;
				to = sorted.get(++i);
				from = glumps[to.index];

				for(; i < sorted.size() - 1 && (to.offset > from.offset || to.offset + to.length > sorted.get(i + 1).offset); ++i) {
					to = sorted.get(i);
					from = glumps[to.index];
				}
				
				for(int j = i - 1; j >= startIndex; --j) {
					to = sorted.get(j);
					
					if(to.length == 0)
						continue;
					
					if(gameLumpData[to.index] != null) {
						out.seek(to.offset);
						out.write(gameLumpData[to.index]);
						continue;
					}
					
					copy(out, glumps[to.index], to);
				}
			}*/
		}
		
		for(int i = 0; i < sorted.size(); ++i) {
			GameLump glump = sorted.get(i);

			//make them relative to Glump once again
			glumps[glump.index].offset -= lumps[GAMELUMP].offset;
			glump.offset -= newGamelump.offset;
		}
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
			glumps[i].index = i;
			glumps[i].id = glumpBuff.getInt();
			glumps[i].flags = glumpBuff.getShort();
			glumps[i].version = Short.toUnsignedInt(glumpBuff.getShort());
			glumps[i].offset = Integer.toUnsignedLong(glumpBuff.getInt()); //make it relative to GameLump
			glumps[i].length = Integer.toUnsignedLong(glumpBuff.getInt());
			
			if(glumps[i].offset < lumps[GAMELUMP].offset || glumpsRelative) {
				glumpsRelative = true;
			} else {
				glumps[i].offset -= lumps[GAMELUMP].offset;
			}
			
			if(glumps[i].id == GameLump.STATICPROP_ID)
				staticPropLump = glumps[i];
		}
	}
	
	private void copy(RandomAccessFile out, InputStream in, long maxLen) throws IOException {
		int len = 0;
		long totalLen = 0;
		byte[] data = new byte[20480];
		
		int toRead = Math.min(data.length, (int)maxLen & 0x7FFFFFFF);
		
		while((len = in.read(data, 0, toRead)) > 0 && toRead > 0) {
			out.write(data, 0, len);
			
			totalLen += len;
			toRead = Math.min(data.length, (int)(maxLen - totalLen) & 0x7FFFFFFF);
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
		
		hasLights = numldrLights + numhdrLights > 0;
		
		ldrLights = new ArrayList<BSPWorldLight>(numldrLights);
		hdrLights = new ArrayList<BSPWorldLight>(numhdrLights);
		
		int num = numldrLights;
		BSPLump lump = lumps[WORLDLIGHTLUMP_LDR];
		ArrayList<BSPWorldLight> arr = ldrLights;
		
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
				
				arr.add(lgt);
			}
			
			num = numhdrLights;
			lump = lumps[WORLDLIGHTLUMP_HDR];
			arr = hdrLights;
		}
	}
	
	public static final int ID_BSP = 0x56425350; //big endian
	public static final int ID_LZMA = 0x4C5A4D41; //big endian
	
	private static final int GLUMP_INDEX_OFF = 4096;
	
	private static final int ENTLUMP = 0;
	private static final int GAMELUMP = 35;
	private static final int WORLDLIGHTLUMP_LDR = 15;
	private static final int WORLDLIGHTLUMP_HDR = 54;
	private static final int PAKLUMP = 40;
	private static final int CUBEMAPLUMP = 42;
	private static final int VERTEXLUMP = 3;
	private static final int DISPVERTLUMP = 33;
	
	private static final Charset utf8Charset = Charset.forName("UTF-8");
	
	private static final int[] SPROPLUMP_SIZE = {60, 60, 64};
	private static final int[] SPROPLUMP_VERSIONLOOKUP = {4, 5, 6};
	
	private static final int SPROPLUMP_V4 = 0;
	private static final int SPROPLUMP_V5 = 1;
	private static final int SPROPLUMP_V6 = 2;
	private static final int SPROPLUMP_INVALIDVERSION = -1;
	
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
		public static final int STATICPROP_ID = 0x73707270;
		
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
	
	private static class ZipPart{
		ZipEntry entry;
		byte[] data;
		
		public ZipPart(ZipEntry entry, byte[] data) {
			this.entry = entry;
			this.data = data;
		}
	}
	
}
