package util;

import java.io.OutputStream;
import java.util.Arrays;

public class RandomAccessByteOutputStream extends OutputStream{
	protected byte[] data;
	protected int size;
	protected int pos;
	
	public RandomAccessByteOutputStream(int capacity) {
		size = 0;
		pos = 0;
		data = new byte[capacity];
	}
	
	public RandomAccessByteOutputStream() {
		this(128);
	}
	
	public void write(int b) {
		ensureCapacity(pos + 1);
		data[pos++] = (byte)b;
		
		size = Math.max(size, pos);
	}
	
	public void write(byte b[], int off, int len) {
		if(off + len > b.length)
			throw new IndexOutOfBoundsException();
		ensureCapacity(pos + len);
		System.arraycopy(b, off, data, pos, len);
		pos += len - off;
		size = Math.max(size, pos);
	}
	
	public void writeBytes(byte b[]) {
        write(b, 0, b.length);
    }
	
	public void writeInt(int num) {
		ensureCapacity(pos + 4);
		data[pos] = (byte)(num);
		data[pos + 1] = (byte)(num >> 8);
		data[pos + 2] = (byte)(num >> 16);
		data[pos + 3] = (byte)(num >> 24);
		pos += 4;
		
		size = Math.max(size, pos);
	}
	
	public void writeShort(short num) {
		ensureCapacity(pos + 2);
		data[pos] = (byte)(num);
		data[pos + 1] = (byte)(num >> 8);
		pos += 2;
		
		size = Math.max(size, pos);
	}
	
	public void writeFloat(float num) {
		writeInt(Float.floatToIntBits(num));
	}
	
	public void seek(int pos) {
		if(pos < 0)
			pos = size + pos;
		if(pos < 0)
			throw new IndexOutOfBoundsException();
		this.pos = pos;
	}
	
	public int tell() {
		return pos;
	}
	
	public int size() {
		return size;
	}
	
	public byte[] toByteArray() {
		return Arrays.copyOf(data, size);
	}
	
	protected void ensureCapacity(int minCapacity) {
		int minGrow = minCapacity - data.length;
		
		if(minGrow > 0){
			int prefCapacity = Math.min(data.length + (data.length >> 1), Integer.MAX_VALUE - 8) + minGrow;
			if(prefCapacity < 0 || prefCapacity >= Integer.MAX_VALUE - 8)
				throw new OutOfMemoryError();
			data = Arrays.copyOf(data, prefCapacity);
		}
	}
}
