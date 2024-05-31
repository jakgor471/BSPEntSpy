package bspentspy;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.CRC32;

public class BSP {
	int ident;
	int version;
	Lump[] lump;
	int maprev;
	int glumps;
	Gamelump[] glump;
	RandomAccessFile raf;
	ArrayList<Entity> el;
	Map<String, Entity> namemap;
	boolean dirty;
	CRC32 crc;
	int entlumpstart;
	int entsize;
	int entlumpsize;
	int entoriglumpsize;
	int entdiff;
	int entnewlumpstart;
	int entnewlumpsize;
	int entnewdiff;
	ArrayList<Lump> lumplist;
	int biggestgap;
	int gapoffset;
	boolean isroomingap;
	boolean isentaftergl;
	boolean isgapaftergl;
	boolean opt_preserve;
	boolean opt_optimize;
	boolean opt_insert;
	int opts;
	JProgFrame prog;
	int progress;

	public BSP(RandomAccessFile raf) {
		this.raf = raf;
		dirty = false;
	}

	public void setfile(RandomAccessFile raf) {
		this.raf = raf;
	}

	public void loadheader() throws IOException {
		raf.seek(0);
		byte[] buffer = new byte[1036];
		raf.read(buffer);
		ByteBuffer b = ByteBuffer.wrap(buffer);
		b.order(ByteOrder.LITTLE_ENDIAN);
		readheader(b);
	}

	public void readheader(ByteBuffer b) {
		b.position(0);
		ident = b.getInt();
		int idbsp = 0x50534256;
		version = b.getInt();
		System.out.println("Ident: " + ident);
		System.out.println("Version: " + version);
		if (ident != idbsp) {
			System.out.println("Unknown map file ident!");
			System.exit(1);
		}

		if (version != 20 && version != 19 && version != 17)
			System.out.println("Unknown map file version! EntSpy will run, but results might not be as intended.");

		lump = new Lump[64];
		lumplist = new ArrayList<Lump>();
		for (int i = 0; i < 64; ++i) {
			lump[i] = new Lump();
			lump[i].index = i;
			lump[i].ofs = b.getInt();
			lump[i].len = b.getInt();
			lump[i].vers = b.getInt();
			lump[i].fourCC = b.getInt();
			if (lump[i].len <= 0)
				continue;
			lumplist.add(lump[i]);
			System.out.print("" + i + ": ");
			System.out.print("" + lump[i].ofs + ", " + lump[i].len + ", " + lump[i].vers + ", "
					+ lump[i].fourCC);
			System.out.print(
					" " + Lump.name(i) + "  " + lump[i].len / Lump.size(i) + (Lump.size(i) == 1 ? " bytes" : ""));
			if (lump[i].len % Lump.size(i) != 0) {
				System.out.println("    XXXXX");
			}
			System.out.println();
		}
		maprev = b.getInt();
		System.out.println("MapRev: " + maprev);
		Collections.sort(lumplist);
	}

	public void loadentities() throws IOException {
		el = new ArrayList<Entity>();
		raf.seek(lump[0].ofs);
		long end = lump[0].ofs + lump[0].len;
		boolean numents = false;
		boolean last = false;
		progress = 0;
		while (!last) {
			long fp = raf.getFilePointer();
			if (fp >= end) {
				last = true;
				break;
			}
			progress = (int) fp - lump[0].ofs;
			String line = raf.readLine();
			if (line == null) {
				System.out.println("End of file");
				last = true;
				break;
			}
			if (!line.equals("{")) {
				last = true;
				System.out.println("End of entities");
				continue;
			}
			Entity cent = new Entity();
			while ((line = raf.readLine()) != null) {
				if (line.equals("}")) {
					cent.index = el.size();
					el.add(cent);
					break;
				}
				String[] fields = line.split("\"", -1);
				if (fields.length == 5) {
					String ckey = fields[1];
					String cval = fields[3];
					cent.addKeyVal(ckey, cval);
				}
				if (!line.equals("}"))
					continue;
			}
			cent.setnames();
		}
		System.out.println("Size: " + el.size() + " entities");
	}

	public void buildlinks() {
		Entity lent = new Entity();
		System.out.println("Building targetname map");
		int ntargs = 0;
		namemap = new TreeMap<String, Entity>();
		for (int i = 0; i < el.size(); ++i) {
			lent = el.get(i);
			if (lent.targetname == null)
				continue;
			namemap.put(lent.targetname, lent);
			++ntargs;
		}
		System.out.println("" + ntargs + " out of " + el.size() + " entities have targetnames");
		System.out.println("Building links");
		int nlinks = 0;
		for (int i2 = 0; i2 < el.size(); ++i2) {
			lent = el.get(i2);
			lent.mark = false;
			if (lent.keyvalues == null)
				continue;
			for (int j = 0; j < lent.keyvalues.size(); ++j) {
				if (lent.keyvalues.get(j).key.equals("targetname"))
					continue;
				String val = lent.keyvalues.get(j).value;
				String[] plink = val.split(",");
				Entity linkent = namemap.get(plink[0]);
				if (linkent != null) {
					lent.keyvalues.get(j).link = linkent;
					++nlinks;
					continue;
				}
				lent.keyvalues.get(j).link = null;
			}
		}
		System.out.println("" + nlinks + " links found");
	}

	public ArrayList<Entity> getData() {
		buildlinks();

		return el;
	}

	public void setprog(JProgFrame prog) {
		this.prog = prog;
	}

	public void calcentitylump() {
		int i;
		entlumpstart = lump[0].ofs;
		entoriglumpsize = roundupto4(lump[0].len);
		entsize = 1;
		for (i = 0; i < el.size(); ++i) {
			entsize += el.get(i).byteSize();
		}
		entlumpsize = roundupto4(entsize);
		entdiff = entlumpsize - entoriglumpsize;
		biggestgap = 0;
		gapoffset = 0;
		for (i = 0; i < lumplist.size() - 1; ++i) {
			int gap = lumplist.get((int) (i + 1)).ofs
					- (lumplist.get((int) i).ofs + lumplist.get((int) i).len);
			if (gap <= 4 || gap <= biggestgap)
				continue;
			gapoffset = roundupto4(lumplist.get((int) i).ofs + lumplist.get((int) i).len);
			biggestgap = lumplist.get((int) (i + 1)).ofs - gapoffset;
		}
		isroomingap = biggestgap >= entlumpsize;
		isgapaftergl = lump[35].ofs < gapoffset;
		isentaftergl = lump[35].len == 0 || lump[35].ofs < lump[0].ofs;
		opt_insert = false;
		opt_optimize = false;
		opt_preserve = false;
		if (isentaftergl) {
			if (!isroomingap) {
				opt_preserve = true;
			} else {
				opt_insert = true;
				opt_optimize = true;
			}
		} else {
			opt_optimize = true;
			if (!isroomingap) {
				opt_preserve = true;
				if (entdiff == 0) {
					opt_optimize = false;
				}
			} else {
				opt_insert = true;
			}
		}
		opts = 0;
		if (opt_insert) {
			++opts;
		}
		if (opt_optimize) {
			++opts;
		}
		if (opt_preserve) {
			++opts;
		}
	}

	public void saveheader(RandomAccessFile oraf, int entopt) throws IOException {
		entnewlumpstart = entlumpstart;
		entnewlumpsize = entsize;
		if (entopt == 1) {
			entnewlumpstart = roundupto4((int) raf.length());
		}
		if (entopt == 2) {
			entnewlumpsize = entoriglumpsize;
		}
		entnewdiff = entnewlumpsize - entoriglumpsize;
		oraf.seek(0);
		byte[] buffer = new byte[1036];
		ByteBuffer b = ByteBuffer.wrap(buffer);
		b.order(ByteOrder.LITTLE_ENDIAN);
		b.position(0);
		b.putInt(ident);
		b.putInt(version);
		for (int i = 0; i < 64; ++i) {
			Lump l = lump[i];
			if (l.ofs > entnewlumpstart) {
				l.ofs += entnewdiff;
			}
			if (i == 0) {
				l.len = entsize;
				l.ofs = entnewlumpstart;
			}
			b.putInt(l.ofs);
			b.putInt(l.len);
			b.putInt(l.vers);
			b.putInt(l.fourCC);
		}
		b.putInt(maprev);
		oraf.write(buffer);
	}

	public void savepre(RandomAccessFile oraf) throws IOException {
		raf.seek(1036);
		blockcopy(raf, oraf, entlumpstart - 1036);
	}

	public void saveent(RandomAccessFile oraf) throws IOException {
		oraf.seek(entnewlumpstart);
		prog.setMaximum(el.size());
		for (int i = 0; i < el.size(); ++i) {
			Entity ient = el.get(i);
			oraf.writeBytes("{\n");
			for (int j = 0; j < ient.keyvalues.size(); ++j) {
				oraf.writeBytes(ient.getKeyValString(j) + "\n");
			}
			oraf.writeBytes("}\n");
			prog.setValue(i);
		}
	}

	public void savepost(RandomAccessFile oraf, int entopt) throws IOException {
		long filelength = raf.length();
		raf.seek(entlumpstart + entoriglumpsize);
		if (entopt == 1) {
			oraf.seek(entlumpstart + entoriglumpsize);
		} else {
			oraf.seek(entlumpstart + entnewlumpsize);
		}
		long postlength = filelength - raf.getFilePointer();
		if (postlength > 0) {
			blockcopy(raf, oraf, postlength);
		}
	}

	public void loadglumps() throws IOException {
		raf.seek(lump[35].ofs);
		byte[] buffer = new byte[16];
		ByteBuffer b = ByteBuffer.wrap(buffer);
		b.order(ByteOrder.LITTLE_ENDIAN);
		raf.read(buffer, 0, 4);
		glumps = b.getInt();
		glump = new Gamelump[glumps];
		for (int i = 0; i < glumps; ++i) {
			glump[i] = new Gamelump();
			raf.read(buffer);
			b.position(0);
			glump[i].id = b.getInt();
			glump[i].flags = b.getShort();
			glump[i].version = b.getShort();
			glump[i].fileofs = b.getInt();
			glump[i].filelen = b.getInt();
		}
	}

	public void saveglumps(RandomAccessFile oraf) throws IOException {
		byte[] buffer = new byte[4 + 16 * glumps];
		ByteBuffer b = ByteBuffer.wrap(buffer);
		b.order(ByteOrder.LITTLE_ENDIAN);
		b.putInt(glumps);
		for (int i = 0; i < glumps; ++i) {
			Gamelump gl = glump[i];
			if (gl.fileofs > entnewlumpstart) {
				gl.fileofs += entnewdiff;
			}
			b.putInt(gl.id);
			b.putShort(gl.flags);
			b.putShort(gl.version);
			b.putInt(gl.fileofs);
			b.putInt(gl.filelen);
		}
		oraf.seek(lump[35].ofs);
		oraf.write(buffer);
	}

	public void blockcopy(RandomAccessFile in, RandomAccessFile out, long length) throws IOException {
		byte[] buffer = new byte[10240];
		int blocks = (int) (length / 10240);
		int remainder = (int) (length % 10240);
		prog.setMaximum(blocks + 1);
		for (int i = 0; i < blocks; ++i) {
			in.read(buffer);
			out.write(buffer);
			prog.setValue(i);
		}
		if (remainder == 0) {
			return;
		}
		in.read(buffer, 0, remainder);
		out.write(buffer, 0, remainder);
		prog.setValue(blocks + 1);
	}

	public int roundupto4(int value) {
		return (value + 3) / 4 * 4;
	}

	public int loadtaskprogress() {
		return progress;
	}

	public int loadtasklength() {
		return lump[0].len;
	}

	public void computecrc() throws IOException {
		int bytesread = 0;
		crc = new CRC32();
		byte[] buffer = new byte[1024];
		for (int i = 1; i < 64; ++i) {
			raf.seek(lump[i].ofs);
			for (int size = lump[i].len; size > 0; size -= bytesread) {
				bytesread = size > 1024 ? raf.read(buffer, 0, 1024) : raf.read(buffer, 0, size);
				if (bytesread <= 0)
					continue;
				crc.update(buffer, 0, bytesread);
			}
		}
	}
}
