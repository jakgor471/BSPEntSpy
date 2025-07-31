package bspentspy;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import SevenZip.Compression.LZMA.Decoder;
import SevenZip.Compression.LZMA.Encoder;
import util.RandomAccessByteOutputStream;

public class SourceBSPFile extends BSPFile{
	public static final float MAPSIZE = 65536;
	private static final Pattern VMTREPLACEPATTERN = Pattern.compile("[\\\"']?(?:\\$\\w+|include)\\b[\\\"']?\\s*[\\s\\\"'][\\/\\\\]?([\\w\\/\\d-]*maps/[\\w\\/\\d-]*)[\\n\\\"']?", Pattern.CASE_INSENSITIVE);
	
	protected int bspVersion;
	protected boolean writeLights = true;
	protected boolean writePak = true;
	protected boolean hasLights = true;
	protected File embeddedPak = null;
	
	protected String newMapName = null;
	protected boolean randomMats = false;
	
	protected ArrayList<String> materials;
	
	private int targetSpropVersion = -1;
	private int curSpropVersion = -1;
	private BSPLump[] lumps;
	private GameLump[] glumps;
	private GameLump staticPropLump = null;
	private String originalMapName;
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
		
		loadMaterials();
		loadGameLumps();
		loadEntities();
		
		return true;
	}
	
	private static class ligthmapInfo{
		int width;
		int height;
		int faceId;
		boolean bump;
	}
	
	public ArrayList<BufferedImage> getLightmaps() throws IOException {
		int numFaces = (int) (lumps[FACELUMP].length / 56);
		byte[] faceBytes = new byte[(int)lumps[FACELUMP].length];
		byte[] texBytes = new byte[(int)lumps[TEXTURELUMP].length];
		
		ArrayList<BufferedImage> lightmaps = new ArrayList<>();

		bspfile.seek(lumps[FACELUMP].offset);
		bspfile.read(faceBytes);
		bspfile.seek(lumps[TEXTURELUMP].offset);
		bspfile.read(texBytes);
		
		ByteBuffer faceBuffer = ByteBuffer.wrap(faceBytes);
		faceBuffer.order(ByteOrder.LITTLE_ENDIAN);
		
		ByteBuffer texBuffer = ByteBuffer.wrap(texBytes);
		texBuffer.order(ByteOrder.LITTLE_ENDIAN);
		
		byte[] style = new byte[4];
		
		for(int i = 0; i < numFaces; ++i) {			
			int texInfo = Short.toUnsignedInt(faceBuffer.getShort(i * 56 + 10));
			boolean bump = ((texBuffer.getInt(texInfo * 72 + 64)) & 0x0800) != 0;
			
			faceBuffer.position(i * 56 + 16);
			faceBuffer.get(style);
			
			int numStyles = 0;
			int lightsof = faceBuffer.getInt(i * 56 + 20);
			int width = faceBuffer.getInt(i * 56 + 36) + 1;
			int height = faceBuffer.getInt(i * 56 + 40) + 1;
			int origFace = faceBuffer.getInt(i * 56 + 44);
			
			for(; numStyles < style.length && style[numStyles] != -1; ++numStyles);
			
			int samples = 1;
			
			if(bump)
				samples = 4;
			
			if(numStyles == 0 || lightsof == -1)
				continue;
			
			byte[] lightmapBytes = new byte[4 * width * height * numStyles * samples];
			bspfile.seek(lumps[LIGHTINGLUMP].offset + lightsof);
			bspfile.read(lightmapBytes);
			
			BufferedImage lightmap = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			
			for(int y = 0; y < height; ++y) {
				for(int x = 0; x < width; ++x) {
					int pixIndex = (y * width + x) * 4;
					int pixel = (lightmapBytes[pixIndex]) | (lightmapBytes[pixIndex + 1] << 8)
							| (lightmapBytes[pixIndex + 2] << 16) | (lightmapBytes[pixIndex + 3] << 24);
					lightmap.setRGB(x, y, pixel);
				}
			}
			
			lightmaps.add(lightmap);
			
			System.out.println(origFace + " " + Arrays.toString(style) + numStyles + " " + bump + " lightsof: " + lightsof);
		}
		
		return lightmaps;
	}
	
	public String getOriginalName() {
		if(materials != null && originalMapName == null) {
			for(String s : materials) {
				String s2 = s.toLowerCase();
				if(s2.startsWith("maps/")) {
					int index1 = s2.indexOf("/") + 1;
					int index2 = s2.indexOf("/", index1);
					
					if(index1 < s2.length() && index2 <= s2.length()) {
						originalMapName = s2.substring(index1, index2);
						break;
					}
				}
			}
		}
		
		return originalMapName;
	}
	
	public void changeMapName(String mapname) {
		if(mapname == null) {
			newMapName = null;
			return;
		}
		newMapName = mapname;
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
	
	private void updateCubemaps() {
		ArrayList<Entity> cubemapEnts = new ArrayList<Entity>();
		
		for(Entity e : entities) {
			if(e instanceof EntityCubemap || !e.getKeyValue("classname").equals("env_cubemap"))
				continue;
			
			String[] split = e.getKeyValue("origin").split(" ");
			
			try {
				for(int i = 0; i < split.length; ++i) {
					e.origin[i] = Float.parseFloat(split[i]);
				}
			} catch(NumberFormatException ex) {
				ex.printStackTrace();
				continue;
			}
			
			cubemapEnts.add(e);
		}
		
		for(EntityCubemap e : cubemaps) {
			double minDist = Double.MAX_VALUE;
			int closestIndex = 0;
			Entity closest = null;
			
			int size = cubemapEnts.size();
			
			for(int i = 0; i < size; ++i) {
				Entity e2 = cubemapEnts.get(i);
				float x = (float)e.cubemapOrigin[0] - e2.origin[0];
				float y = (float)e.cubemapOrigin[1] - e2.origin[1];
				float z = (float)e.cubemapOrigin[2] - e2.origin[2];
				
				double dist = x*x + y*y + z*z;
				if(dist < minDist) {
					minDist = dist;
					closest = e2;
					closestIndex = i;
				}
			}
			
			if(minDist > 2 || closest == null)
				continue;
			
			e.setKeyVal("cubemapsize", closest.getKeyValue("cubemapsize"));
			
			Entity last = cubemapEnts.get(size - 1);
			cubemapEnts.set(size - 1, closest);
			cubemapEnts.set(closestIndex, last);
			--size;
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
		if(bspfile == null || out == null)
			return;
		
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
		
		if(!writeLights) {
			newLumps[WORLDLIGHTLUMP_HDR].length = 0;
			newLumps[WORLDLIGHTLUMP_LDR].length = 0;
		}
		
		if(!writePak) {
			newLumps[PAKLUMP].length = 0;
		}
		
		boolean rename = false;
		if(getOriginalName() != null && newMapName != null) {
			renameMaterials();
			rename = true;
		}
		
		if(materials != null && !materials.isEmpty() && randomMats) {
			Random rand = new Random(materials.get(0).hashCode());
			
			for(i = 0; i < materials.size(); ++i) {
				int index = rand.nextInt(materials.size());
				String mat = materials.get(index);
				materials.set(index, materials.get(i));
				materials.set(i, mat);
			}
		}
		
		newLumps[PAKLUMP].offset = Long.MAX_VALUE; //let the PAK lump be at the end so writing to it does not require shifting everything
		Collections.sort(sorted);

		GenericLump cur = sorted.get(0);
		cur.offset = Math.max(cur.offset, 1036);
		
		GenericLump prev = null;
		
		for(i = 0; i < sorted.size(); ++i) {
			GenericLump to = sorted.get(i);
			if(prev != null)
				to.offset = alignToFour(prev.offset + prev.length);
			prev = to;
			
			if(to.length == 0)
				continue;

			if(to.index == GAMELUMP) {
				saveGameLumps(out, newLumps[GAMELUMP], newGlumps);
				continue;
			} else if(to.index == ENTLUMP && entDirty) {
				byte[] entData = getEntityBytes((BSPLump) to);
				to.length = entData.length;
				
				out.seek(to.offset);
				out.write(entData);
				continue;
			} else if(to.index == PAKLUMP) {
				savePak(out, lumps[PAKLUMP], newLumps[PAKLUMP], rename);
				continue;
			} else if(to.index == CUBEMAPLUMP && cubemaps != null) {
				updateCubemaps();
				byte[] cubemapData = getCubemapBytes();
				to.length = cubemapData.length;
				out.seek(to.offset);
				out.write(cubemapData);
				continue;
			} else if(to.index == TEXSTRINGDATALUMP_DATA && materials != null) {
				ByteArrayOutputStream bytes = new ByteArrayOutputStream(materials.size() * 128);
				
				((BSPLump)to).fourCC = 0; //no compression at the moment
				
				for(String mat : materials) {
					bytes.write(mat.getBytes(utf8Charset));
					bytes.write(0);
				}
				
				to.length = bytes.size();
				out.seek(to.offset);
				bytes.writeTo(Channels.newOutputStream(out.getChannel()));
				bytes.close();
				
				continue;
			} else if(to.index == TEXSTRINGDATALUMP_TABLE && materials != null) {
				byte[] bytes = new byte[materials.size() * 4];
				int offset = 0;
				
				((BSPLump)to).fourCC = 0;
				
				ByteBuffer buff = ByteBuffer.wrap(bytes);
				buff.order(ByteOrder.LITTLE_ENDIAN);
				
				to.length = bytes.length;
				
				for(String mat : materials) {
					buff.putInt(offset);
					offset += mat.length() + 1;
				}
				
				out.seek(to.offset);
				out.write(bytes);
				
				continue;
			}
			
			copy(out, lumps[to.index], to);
		}
		
		writeHeader(out, newLumps);
		
		cur = sorted.get(sorted.size() - 1);
		out.setLength(cur.offset + cur.length);
		
		if(updateSelf) {
			close();
			bspfile = out;
			
			lumps = newLumps;
			glumps = newGlumps;
			embeddedPak = null;
			writePak = true;
			writeLights = true;
			curSpropVersion = targetSpropVersion;
			
			staticPropLump = findStaticPropLump();
			
			newMapName = null;
			originalMapName = null;
		}
	}
	
	private void savePak(RandomAccessFile out, GenericLump oldPakLump, GenericLump newPakLump, boolean rename) throws IOException {
		if(embeddedPak == null && !rename) {
			copy(out, oldPakLump, newPakLump);
			return;
		}
		
		FileInputStream pakIs = null;
		try {
			ZipInputStream zis;
			if(embeddedPak != null) {
				pakIs = new FileInputStream(embeddedPak);
				zis = new ZipInputStream(pakIs);
				copyRenamePak(out, zis, oldPakLump, newPakLump, rename);
				pakIs.close();
				pakIs = null;
			} else {
				bspfile.seek(oldPakLump.offset);
				zis = new ZipInputStream(Channels.newInputStream(bspfile.getChannel()));
				copyRenamePak(out, zis, oldPakLump, newPakLump, rename);
			}
		} catch(IOException e) {
			if(pakIs != null)
				pakIs.close();
			embeddedPak = null;
			throw e;
		}
	}
	
	private void copyRenamePak(RandomAccessFile out, ZipInputStream zis, GenericLump pakFrom, GenericLump pakTo, boolean rename) throws IOException {
		out.seek(pakTo.offset);
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream((int)pakFrom.length);
		ZipOutputStream zos = new ZipOutputStream(bos);
		
		OutputStream os = Channels.newOutputStream(out.getChannel());
		
		ZipEntry ze = null;
		
		byte[] buffer = new byte[81920];
		long totalSize = 0;
		
		String orgMapName = null;
		String mapName = null;
		
		if(rename) {
			orgMapName = "maps/" + getOriginalName().toLowerCase();
			mapName = "maps/" + newMapName.toLowerCase();
		}
		
		CRC32 crc = new CRC32();
		
		while((ze = zis.getNextEntry()) != null) {
			String newName = ze.getName();
			
			if(rename) {
				int index = newName.toLowerCase().indexOf(orgMapName);
				
				if(index > -1)
					newName = newName.substring(0, index) + mapName + newName.substring(index + orgMapName.length());
			}

			if(rename && newName.substring(newName.lastIndexOf('.')).toLowerCase().equals(".vmt")) {
				ZipEntry newEntry = new ZipEntry(newName);
				newEntry.setMethod(ZipEntry.STORED);
				
				byte[] bytes = new byte[(int) ze.getSize()];
				zis.read(bytes);
				
				ByteBuffer buff = ByteBuffer.wrap(bytes);
				CharsetDecoder chd = utf8Charset.newDecoder();
				CharBuffer chb = chd.decode(buff);
				
				String modified = renameVmt(chb);
				
				if(modified != null)
					bytes = modified.getBytes(utf8Charset);
				
				crc.update(bytes);
				
				newEntry.setSize(bytes.length);
				newEntry.setCompressedSize(bytes.length);
				newEntry.setCrc(crc.getValue());
				
				zos.putNextEntry(newEntry);
				zos.write(bytes);
				
				crc.reset();
			} else {
				ZipEntry newEntry = new ZipEntry(newName);
				newEntry.setMethod(ZipEntry.STORED);
				
				newEntry.setSize(ze.getSize());
				newEntry.setCompressedSize(ze.getSize());
				newEntry.setCrc(ze.getCrc());
				zos.putNextEntry(newEntry);
				
				int length = 0;
				while ((length = zis.read(buffer)) > 0) {
					zos.write(buffer, 0, length);
				}
			}
			
			zis.closeEntry();
			zos.closeEntry();
			
			totalSize += bos.size();
			bos.writeTo(os);
			bos.reset();
		}
		
		zos.finish();
		
		totalSize += bos.size();
		bos.writeTo(os);
		
		pakTo.length = totalSize;
	}
	
	private String renameVmt(CharSequence content) {
		Matcher match = VMTREPLACEPATTERN.matcher(content);
		
		StringBuilder newContent = new StringBuilder(content.length());
		
		int beg = 0;
		boolean found = false;
		while(match.find()) {
			found = true;
			String foundStr = match.group(1);
			String[] split = foundStr.split("[\\\\/]");
			
			newContent.append(content.subSequence(beg, match.start(1)));
			beg = match.end(1);
			boolean foundSplit = false;
			
			for(int i = 0; i < split.length - 1; ++i) {
				if(!foundSplit && split[i].equalsIgnoreCase(getOriginalName())) {
					foundSplit = true;
					split[i] = newMapName.toLowerCase();
				}
				newContent.append(split[i]).append("/");
			}
			
			newContent.append(split[split.length - 1]);
		}
		
		newContent.append(content.subSequence(beg, content.length()));
		
		if(found)
			return newContent.toString();
		
		return null;
	}
	
	private void renameMaterials() {
		if(getOriginalName() == null || materials == null)
			return;
		
		String lowerMapName = "maps/" + newMapName.toLowerCase();
		String lowerOrgName = "maps/" + getOriginalName().toLowerCase();
		
		for(int i = 0; i < materials.size(); ++i) {
			String org = materials.get(i);
			String lower = org.toLowerCase();
			int index = lower.indexOf(lowerOrgName);
			
			if(index > -1) {
				String newMat = org.substring(0, index) + lowerMapName + org.substring(index + lowerOrgName.length());
				materials.set(i, newMat);
			}
		}
	}
	
	private void saveGameLumps(RandomAccessFile out, GenericLump newGamelump, GameLump[] newGlumps) throws IOException {
		byte[] glumpHeaderBytes = new byte[4 + 16 * newGlumps.length];
		
		ArrayList<GameLump> sorted = new ArrayList<GameLump>();
		
		for(int i = 0; i < newGlumps.length; ++i) {
			sorted.add(newGlumps[i]);
		}
		
		Collections.sort(sorted);
		
		GameLump prev = sorted.get(0);
		prev.offset = alignToFour(glumpHeaderBytes.length);
		prev = null;
		
		for(int i = 0; i < sorted.size(); ++i) {
			GameLump to = sorted.get(i);
			if(prev != null)
				to.offset = alignToFour(prev.offset + prev.length);
			prev = to;
			
			if(to.length == 0)
				continue;
			
			if(to.id == GameLump.STATICPROP_ID && staticProps != null) {
				if(targetSpropVersion > -1)
					newGlumps[staticPropLump.index].version = SPROPLUMP_VERSIONLOOKUP[targetSpropVersion];
				byte[] spropData = getStaticPropsBytes();
				to.length = spropData.length;
				out.seek(to.offset + newGamelump.offset);
				out.write(spropData);
				continue;
			}
			
			copy(out, glumps[to.index], to, newGamelump.offset);
		}
		
		prev = sorted.get(sorted.size() - 1);
		newGamelump.length = prev.offset + prev.length;
		
		//copy(out, lumps[GAMELUMP], newGamelump);
		
		ByteBuffer glumpBuff = ByteBuffer.wrap(glumpHeaderBytes);
		glumpBuff.order(ByteOrder.LITTLE_ENDIAN);
		long glumpOffset = newGamelump.offset;
		if(glumpsRelative)
			glumpOffset = 0;
		
		glumpBuff.putInt(newGlumps.length);
		for(int i = 0; i < newGlumps.length; ++i) {
			GameLump glump = newGlumps[i];
			
			glumpBuff.putInt(glump.id);
			glumpBuff.putShort(glump.flags);
			glumpBuff.putShort((short)glump.version);
			glumpBuff.putInt((int)(glump.offset + glumpOffset));
			glumpBuff.putInt((int)glump.length);
		}
		
		out.seek(newGamelump.offset);
		out.write(glumpHeaderBytes);
	}
	
	private GameLump findStaticPropLump() {
		for(int i = 0; i < glumps.length; ++i) {
			if(glumps[i].id == GameLump.STATICPROP_ID)
				return glumps[i];
		}
		
		return null;
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
	
	private void copy(RandomAccessFile out, GameLump from, GameLump to, long glumpOffset) throws IOException {
		from.offset += lumps[GAMELUMP].offset;
		to.offset += glumpOffset;
		copy(out, from, to);
		from.offset -= lumps[GAMELUMP].offset;
		to.offset -= glumpOffset;
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
	
	private byte[] decompress(BSPLump lump) throws IOException {
		if(lump.offset == 0)
			return null;
		
		bspfile.seek(lump.offset);
		
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
		
		byte[] entData = null;
		if(entLump.fourCC != 0) {
			entLump.lzma = true;
			entData = decompress(entLump);
		}
		
		bspfile.seek(entLump.offset);
		
		if (entData == null){
			entLump.lzma = false;
			entData = new byte[(int) entLump.length];
			bspfile.read(entData);
		}
		
		BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(entData)));
		readEntities(br);
		
		updateLinks();
	}
	
	private void loadMaterials() throws IOException {
		byte[] stringData = null;
		byte[] tableData = null;
		
		if(lumps[TEXSTRINGDATALUMP_DATA].fourCC == 0) {
			stringData = new byte[(int)lumps[TEXSTRINGDATALUMP_DATA].length];
			bspfile.seek(lumps[TEXSTRINGDATALUMP_DATA].offset);
			bspfile.read(stringData);
		}else 
			stringData = decompress(lumps[TEXSTRINGDATALUMP_DATA]);
		
		if(lumps[TEXSTRINGDATALUMP_TABLE].fourCC == 0) {
			tableData = new byte[(int)lumps[TEXSTRINGDATALUMP_TABLE].length];
			bspfile.seek(lumps[TEXSTRINGDATALUMP_TABLE].offset);
			bspfile.read(tableData);
		}else 
			tableData = decompress(lumps[TEXSTRINGDATALUMP_TABLE]);
		
		if(stringData == null || tableData == null)
			return;
		
		ByteBuffer tableBuff = ByteBuffer.wrap(tableData);
		tableBuff.order(ByteOrder.LITTLE_ENDIAN);
		
		int numTex = tableData.length / 4;
		materials = new ArrayList<String>(numTex);
		
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < numTex; ++i) {
			byte c;
			int j = tableBuff.getInt();
			
			while((c = stringData[j++]) != 0)
				sb.append((char)c);
			
			materials.add(sb.toString());
			sb.setLength(0);
		}
	}
	
	public static final int ID_BSP = 0x56425350; //big endian
	public static final int ID_LZMA = 0x4C5A4D41; //big endian
	
	private static final int GLUMP_INDEX_OFF = 4096;
	
	private static final int ENTLUMP = 0;
	private static final int TEXTURELUMP = 6;
	private static final int FACELUMP = 7;
	private static final int LIGHTINGLUMP = 8;
	private static final int GAMELUMP = 35;
	private static final int WORLDLIGHTLUMP_LDR = 15;
	private static final int WORLDLIGHTLUMP_HDR = 54;
	private static final int PAKLUMP = 40;
	private static final int CUBEMAPLUMP = 42;
	private static final int VERTEXLUMP = 3;
	private static final int DISPVERTLUMP = 33;
	private static final int TEXSTRINGDATALUMP_DATA = 43;
	private static final int TEXSTRINGDATALUMP_TABLE = 44;
	
	private static final Charset utf8Charset = Charset.forName("UTF-8");
	
	private static final int[] SPROPLUMP_SIZE = {60, 60, 64};
	private static final int[] SPROPLUMP_VERSIONLOOKUP = {4, 5, 6};
	
	private static final int SPROPLUMP_V4 = 0;
	private static final int SPROPLUMP_V5 = 1;
	private static final int SPROPLUMP_V6 = 2;
	private static final int SPROPLUMP_INVALIDVERSION = -1;
	
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
	
}
