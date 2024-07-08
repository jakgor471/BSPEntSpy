package bspentspy;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class GoldSrcBSPFile extends BSPFile{
	private GenericLump[] lumps;
	
	protected GoldSrcBSPFile() {
		super();
	}
	
	public boolean read(RandomAccessFile file) throws IOException {
		file.seek(0);
		bspVersion = Integer.reverseBytes(file.readInt());
		
		if(bspVersion != 30) {
			file.seek(file.getFilePointer() - 4);
			return false;
		}
		
		bspfile = file;
		
		byte[] lumpBytes = new byte[120]; //15 lumps, 8 bytes each
		bspfile.read(lumpBytes);
		ByteBuffer lumpBuffer = ByteBuffer.wrap(lumpBytes);
		lumpBuffer.order(ByteOrder.LITTLE_ENDIAN);
		lumps = new GenericLump[15];
		
		for(int i = 0; i < 15; ++i) {
			lumps[i] = new GenericLump();
			lumps[i].index = i;
			lumps[i].offset = lumpBuffer.getInt();
			lumps[i].length = lumpBuffer.getInt();
		}

		loadEntities();
		
		return true;
	}
	
	private void loadEntities() throws IOException {
		GenericLump entLump = lumps[0];
		bspfile.seek(entLump.offset);
		
		byte[] entData = null;
		
		entData = new byte[(int) entLump.length];
		bspfile.read(entData);
		
		BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(entData)));
		readEntities(br);
	}
}
