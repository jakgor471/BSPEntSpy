package entspy;

class Lumpentry
implements Comparable<Lumpentry> {
    String id;
    String name;
    int offset;
    int length;

    public Lumpentry(String id, String name, int offset, int length) {
        this.id = id;
        this.name = name;
        this.offset = offset;
        this.length = length;
    }

    public Lumpentry(Lump lump) {
        this(String.valueOf(lump.index), Lump.lumpname[lump.index], lump.ofs, lump.len);
    }

    @Override
    public int compareTo(Lumpentry that) {
        return this.offset - that.offset;
    }
}

