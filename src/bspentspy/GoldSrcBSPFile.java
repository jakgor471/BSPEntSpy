package bspentspy;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class GoldSrcBSPFile extends BSPFile{
	private GenericLump[] lumps;
	private int bspVersion;
	
	public boolean read(RandomAccessFile file) throws IOException {
		file.seek(0);
		bspVersion = Integer.reverseBytes(file.readInt());
		
		if(bspVersion != 30 && bspVersion != 29) {
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
		
		updateLinks();
	}

	public void save(RandomAccessFile out, boolean updateSelf) throws IOException {
		GenericLump[] newLumps = new GenericLump[lumps.length];
		
		List<GenericLump> sorted = new LinkedList<GenericLump>();
		int i = 0;
		for(i = 0; i < lumps.length; ++i) {
			newLumps[i] = (GenericLump) lumps[i].clone();
			sorted.add(newLumps[i]);
		}
		byte[] entData = getEntityBytes();
		newLumps[ENTLUMP].length = entData.length;
		//for whatever reason the size is always bigger by one ?(O.o)?
		//maybe because of a null terminator, weird. Why make things weird?!
		//yes, that is the null terminator.
		
		Collections.sort(sorted);
		
		GenericLump prev = sorted.get(0);
		prev.offset = 124;
		prev = null;
		
		for(i = 0; i < sorted.size(); ++i) {
			GenericLump to = sorted.get(i);
			GenericLump from;
			
			if(prev != null)
				to.offset = alignToFour(prev.offset + prev.length);
			prev = to;
			
			if(to.length == 0)
				continue;
			
			if(to.index == ENTLUMP) {
				out.seek(to.offset);
				out.write(entData);
				to.length = entData.length + 1;
				continue;
			}
			from = lumps[to.index];
			
			copy(out, from, to);
		}
		
		byte[] headerbytes = new byte[124];
		ByteBuffer header = ByteBuffer.wrap(headerbytes);
		header.order(ByteOrder.LITTLE_ENDIAN);
		
		header.putInt(bspVersion);
		for(i = 0; i < newLumps.length; ++i) {
			header.putInt((int)newLumps[i].offset);
			header.putInt((int)newLumps[i].length);
		}
		out.seek(0);
		out.write(headerbytes);
		GenericLump last = sorted.get(sorted.size() - 1);
		out.setLength(last.offset + last.length);
		
		if(updateSelf) {
			lumps = newLumps;
		}
	}
	
	public static final int ENTLUMP = 0;
}
