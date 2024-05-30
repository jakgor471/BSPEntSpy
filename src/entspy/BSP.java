package entspy;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.CRC32;
import javax.swing.tree.DefaultMutableTreeNode;
import util.Cons;

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
        this.dirty = false;
    }

    public void setfile(RandomAccessFile raf) {
        this.raf = raf;
    }

    public void loadheader() throws IOException {
        this.raf.seek(0);
        byte[] buffer = new byte[1036];
        this.raf.read(buffer);
        ByteBuffer b = ByteBuffer.wrap(buffer);
        b.order(ByteOrder.LITTLE_ENDIAN);
        this.readheader(b);
    }

    public void readheader(ByteBuffer b) {
        b.position(0);
        this.ident = b.getInt();
        int idbsp = 1347633750;
        this.version = b.getInt();
        Cons.println("Ident: " + this.ident);
        Cons.println("Version: " + this.version);
        if (this.ident != idbsp) {
            Cons.println("Unknown map file ident!");
            System.exit(1);
        }
        
        if (this.version != 20 && this.version != 19 && this.version != 17) 
            Cons.println("Unknown map file version! EntSpy will run, but results might not be as intended.");
        
        this.lump = new Lump[64];
        this.lumplist = new ArrayList();
        for (int i = 0; i < 64; ++i) {
            this.lump[i] = new Lump();
            this.lump[i].index = i;
            this.lump[i].ofs = b.getInt();
            this.lump[i].len = b.getInt();
            this.lump[i].vers = b.getInt();
            this.lump[i].fourCC = b.getInt();
            if (this.lump[i].len <= 0) continue;
            this.lumplist.add(this.lump[i]);
            Cons.print("" + i + ": ");
            Cons.print("" + this.lump[i].ofs + ", " + this.lump[i].len + ", " + this.lump[i].vers + ", " + this.lump[i].fourCC);
            Cons.print(" " + Lump.name(i) + "  " + this.lump[i].len / Lump.size(i) + (Lump.size(i) == 1 ? " bytes" : ""));
            if (this.lump[i].len % Lump.size(i) != 0) {
                Cons.println("    XXXXX");
            }
            Cons.println();
        }
        this.maprev = b.getInt();
        Cons.println("MapRev: " + this.maprev);
        Collections.sort(this.lumplist);
    }

    public void loadentities() throws IOException {
        this.el = new ArrayList();
        this.raf.seek(this.lump[0].ofs);
        long end = this.lump[0].ofs + this.lump[0].len;
        boolean numents = false;
        boolean last = false;
        this.progress = 0;
        while (!last) {
            long fp = this.raf.getFilePointer();
            if (fp >= end) {
                last = true;
                break;
            }
            this.progress = (int)fp - this.lump[0].ofs;
            String line = this.raf.readLine();
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
            while ((line = this.raf.readLine()) != null) {
                if (line.equals("}")) {
                    cent.index = this.el.size();
                    this.el.add(cent);
                    break;
                }
                String[] fields = line.split("\"", -1);
                if (fields.length == 5) {
                    String ckey = fields[1];
                    String cval = fields[3];
                    cent.kvmap.put(ckey, cent.values.size());
                    cent.keys.add(ckey);
                    cent.values.add(cval);
                    cent.links.add(null);
                }
                if (!line.equals("}")) continue;
            }
            cent.setnames();
        }
        System.out.println("Size: " + this.el.size() + " entities");
    }

    public void buildlinks() {
        Entity lent = new Entity();
        System.out.println("Building targetname map");
        int ntargs = 0;
        this.namemap = new TreeMap<String, Entity>();
        for (int i = 0; i < this.el.size(); ++i) {
            lent = this.el.get(i);
            if (lent.targetname == null) continue;
            this.namemap.put(lent.targetname, lent);
            ++ntargs;
        }
        System.out.println("" + ntargs + " out of " + this.el.size() + " entities have targetnames");
        System.out.println("Building links");
        int nlinks = 0;
        for (int i2 = 0; i2 < this.el.size(); ++i2) {
            lent = this.el.get(i2);
            lent.mark = false;
            if (lent.keys == null) continue;
            for (int j = 0; j < lent.keys.size(); ++j) {
                if (lent.keys.get(j).equals("targetname")) continue;
                String val = lent.values.get(j);
                String[] plink = val.split(",");
                Entity linkent = this.namemap.get(plink[0]);
                if (linkent != null) {
                    lent.links.set(j, linkent);
                    ++nlinks;
                    continue;
                }
                lent.links.set(j, null);
            }
        }
        System.out.println("" + nlinks + " links found");
    }

    public ArrayList<Entity> getData() {
        this.buildlinks();
        
        return el;
    }

    public void setprog(JProgFrame prog) {
        this.prog = prog;
    }

    public void calcentitylump() {
        int i;
        this.entlumpstart = this.lump[0].ofs;
        this.entoriglumpsize = this.roundupto4(this.lump[0].len);
        this.entsize = 1;
        for (i = 0; i < this.el.size(); ++i) {
            this.entsize+=this.el.get(i).byteSize();
        }
        this.entlumpsize = this.roundupto4(this.entsize);
        this.entdiff = this.entlumpsize - this.entoriglumpsize;
        this.biggestgap = 0;
        this.gapoffset = 0;
        for (i = 0; i < this.lumplist.size() - 1; ++i) {
            int gap = this.lumplist.get((int)(i + 1)).ofs - (this.lumplist.get((int)i).ofs + this.lumplist.get((int)i).len);
            if (gap <= 4 || gap <= this.biggestgap) continue;
            this.gapoffset = this.roundupto4(this.lumplist.get((int)i).ofs + this.lumplist.get((int)i).len);
            this.biggestgap = this.lumplist.get((int)(i + 1)).ofs - this.gapoffset;
        }
        this.isroomingap = this.biggestgap >= this.entlumpsize;
        this.isgapaftergl = this.lump[35].ofs < this.gapoffset;
        this.isentaftergl = this.lump[35].len == 0 || this.lump[35].ofs < this.lump[0].ofs;
        this.opt_insert = false;
        this.opt_optimize = false;
        this.opt_preserve = false;
        if (this.isentaftergl) {
            if (!this.isroomingap) {
                this.opt_preserve = true;
            } else {
                this.opt_insert = true;
                this.opt_optimize = true;
            }
        } else {
            this.opt_optimize = true;
            if (!this.isroomingap) {
                this.opt_preserve = true;
                if (this.entdiff == 0) {
                    this.opt_optimize = false;
                }
            } else {
                this.opt_insert = true;
            }
        }
        this.opts = 0;
        if (this.opt_insert) {
            ++this.opts;
        }
        if (this.opt_optimize) {
            ++this.opts;
        }
        if (this.opt_preserve) {
            ++this.opts;
        }
    }

    public void saveheader(RandomAccessFile oraf, int entopt) throws IOException {
        this.entnewlumpstart = this.entlumpstart;
        this.entnewlumpsize = this.entsize;
        if (entopt == 1) {
            this.entnewlumpstart = this.roundupto4((int)this.raf.length());
        }
        if (entopt == 2) {
            this.entnewlumpsize = this.entoriglumpsize;
        }
        this.entnewdiff = this.entnewlumpsize - this.entoriglumpsize;
        oraf.seek(0);
        byte[] buffer = new byte[1036];
        ByteBuffer b = ByteBuffer.wrap(buffer);
        b.order(ByteOrder.LITTLE_ENDIAN);
        b.position(0);
        b.putInt(this.ident);
        b.putInt(this.version);
        for (int i = 0; i < 64; ++i) {
            Lump l = this.lump[i];
            if (l.ofs > this.entnewlumpstart) {
                l.ofs+=this.entnewdiff;
            }
            if (i == 0) {
                l.len = this.entsize;
                l.ofs = this.entnewlumpstart;
            }
            b.putInt(l.ofs);
            b.putInt(l.len);
            b.putInt(l.vers);
            b.putInt(l.fourCC);
        }
        b.putInt(this.maprev);
        oraf.write(buffer);
    }

    public void savepre(RandomAccessFile oraf) throws IOException {
        this.raf.seek(1036);
        this.blockcopy(this.raf, oraf, this.entlumpstart - 1036);
    }

    public void saveent(RandomAccessFile oraf) throws IOException {
        oraf.seek(this.entnewlumpstart);
        this.prog.setMaximum(this.el.size());
        for (int i = 0; i < this.el.size(); ++i) {
            Entity ient = this.el.get(i);
            oraf.writeBytes("{\n");
            for (int j = 0; j < ient.keys.size(); ++j) {
                oraf.writeBytes(ient.getKeyValString(j) + "\n");
            }
            oraf.writeBytes("}\n");
            this.prog.setValue(i);
        }
    }

    public void savepost(RandomAccessFile oraf, int entopt) throws IOException {
        long filelength = this.raf.length();
        this.raf.seek(this.entlumpstart + this.entoriglumpsize);
        if (entopt == 1) {
            oraf.seek(this.entlumpstart + this.entoriglumpsize);
        } else {
            oraf.seek(this.entlumpstart + this.entnewlumpsize);
        }
        long postlength = filelength - this.raf.getFilePointer();
        if (postlength > 0) {
            this.blockcopy(this.raf, oraf, postlength);
        }
    }

    public void loadglumps() throws IOException {
        this.raf.seek(this.lump[35].ofs);
        byte[] buffer = new byte[16];
        ByteBuffer b = ByteBuffer.wrap(buffer);
        b.order(ByteOrder.LITTLE_ENDIAN);
        this.raf.read(buffer, 0, 4);
        this.glumps = b.getInt();
        this.glump = new Gamelump[this.glumps];
        for (int i = 0; i < this.glumps; ++i) {
            this.glump[i] = new Gamelump();
            this.raf.read(buffer);
            b.position(0);
            this.glump[i].id = b.getInt();
            this.glump[i].flags = b.getShort();
            this.glump[i].version = b.getShort();
            this.glump[i].fileofs = b.getInt();
            this.glump[i].filelen = b.getInt();
        }
    }

    public void saveglumps(RandomAccessFile oraf) throws IOException {
        byte[] buffer = new byte[4 + 16 * this.glumps];
        ByteBuffer b = ByteBuffer.wrap(buffer);
        b.order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(this.glumps);
        for (int i = 0; i < this.glumps; ++i) {
            Gamelump gl = this.glump[i];
            if (gl.fileofs > this.entnewlumpstart) {
                gl.fileofs+=this.entnewdiff;
            }
            b.putInt(gl.id);
            b.putShort(gl.flags);
            b.putShort(gl.version);
            b.putInt(gl.fileofs);
            b.putInt(gl.filelen);
        }
        oraf.seek(this.lump[35].ofs);
        oraf.write(buffer);
    }

    public void blockcopy(RandomAccessFile in, RandomAccessFile out, long length) throws IOException {
        byte[] buffer = new byte[10240];
        int blocks = (int)(length / 10240);
        int remainder = (int)(length % 10240);
        this.prog.setMaximum(blocks + 1);
        for (int i = 0; i < blocks; ++i) {
            in.read(buffer);
            out.write(buffer);
            this.prog.setValue(i);
        }
        if (remainder == 0) {
            return;
        }
        in.read(buffer, 0, remainder);
        out.write(buffer, 0, remainder);
        this.prog.setValue(blocks + 1);
    }

    public int roundupto4(int value) {
        return (value + 3) / 4 * 4;
    }

    public int loadtaskprogress() {
        return this.progress;
    }

    public int loadtasklength() {
        return this.lump[0].len;
    }

    public void computecrc() throws IOException {
        int bytesread = 0;
        this.crc = new CRC32();
        byte[] buffer = new byte[1024];
        for (int i = 1; i < 64; ++i) {
            this.raf.seek(this.lump[i].ofs);
            for (int size = this.lump[i].len; size > 0; size-=bytesread) {
                bytesread = size > 1024 ? this.raf.read(buffer, 0, 1024) : this.raf.read(buffer, 0, size);
                if (bytesread <= 0) continue;
                this.crc.update(buffer, 0, bytesread);
            }
        }
    }
}

