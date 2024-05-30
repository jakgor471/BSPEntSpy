package bspentspy;

import java.util.ArrayList;
import java.util.Collections;

import javax.swing.table.AbstractTableModel;

class Lumpdata
extends AbstractTableModel {
    static String[] header = new String[]{"No.", "Lump", "Offset", "Length"};
    BSP m;
    Object[][] data;

    public Lumpdata(BSP m) {
        this.m = m;
        this.getData();
    }

    public int getRowCount() {
        return this.data.length;
    }

    public int getColumnCount() {
        return this.data[0].length;
    }

    public String getColumnName(int col) {
        return header[col];
    }

    public Object getValueAt(int row, int col) {
        return this.data[row][col];
    }

    public void getData() {
        ArrayList<Lumpentry> list = new ArrayList<Lumpentry>();
        list.add(new Lumpentry("", "Header", 0, 1036));
        for (int i = 0; i < 64; ++i) {
            if (this.m.lump[i].len <= 0) continue;
            Lumpentry l = new Lumpentry(this.m.lump[i]);
            list.add(l);
        }
        Collections.sort(list);
        ArrayList<Lumpentry> temp = new ArrayList<Lumpentry>();
        for (int i2 = 0; i2 < list.size() - 1; ++i2) {
            int gap = ((Lumpentry)list.get((int)(i2 + 1))).offset - (((Lumpentry)list.get((int)i2)).offset + ((Lumpentry)list.get((int)i2)).length);
            if (gap <= 4) continue;
            temp.add(new Lumpentry("*", "Gap", ((Lumpentry)list.get((int)i2)).offset + ((Lumpentry)list.get((int)i2)).length, gap));
        }
        list.addAll(temp);
        Collections.sort(list);
        this.data = new Object[list.size()][4];
        int s = list.size();
        for (int i3 = 0; i3 < s; ++i3) {
            Lumpentry l = (Lumpentry)list.get(i3);
            this.data[i3][0] = l.id;
            this.data[i3][1] = l.name;
            this.data[i3][2] = l.offset;
            this.data[i3][3] = l.length;
        }
    }

    public String strint(int in) {
        char[] c = new char[]{(char)(in & 255), (char)(in >> 8 & 255), (char)(in >> 16 & 255), (char)(in >>> 24 & 255)};
        return new String(c);
    }
}

