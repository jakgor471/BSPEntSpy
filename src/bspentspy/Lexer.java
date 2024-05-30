package bspentspy;

import java.io.IOException;
import java.io.Reader;

public abstract class Lexer<T extends bspentspy.Lexer.LexerToken>{
	public static enum BasicTokenType{
		ident,
		string,
		numeric,
		symbol
	}
	
	public static interface LexerToken{
		public boolean isSymbol();
		public boolean isIdent();
		public boolean isNumeric();
		public boolean isString();
	}
	
	public static class BasicToken implements LexerToken{
		public BasicTokenType type;
		public String value;
		
		public boolean isString() {
			return type == BasicTokenType.string;
		}
		
		public boolean isIdent() {
			return type == BasicTokenType.ident;
		}
		
		public boolean isNumeric() {
			return type == BasicTokenType.numeric;
		}
		
		public boolean isSymbol() {
			return type == BasicTokenType.symbol;
		}
	}
	
	public String fileName;
	public int line;
	
	protected char[] buffer;
	protected T currToken;
	protected int bufferCapacity;
	protected int offset;
	protected Reader reader;
	
	public Lexer(Reader in, String name) {
		reader = in;
		fileName = name;
		buffer = new char[4096];
		currToken = null;
		offset = 0;
		bufferCapacity = 0;
		line = 1;
		
		if(fileName == null)
			fileName = "unknown";
	}
	
	public T getToken() throws LexerException {
		if(currToken != null)
			return (T)currToken;
		
		return (T)nextToken();
	}
	
	public void consume() throws LexerException {
		if(currToken != null) {
			currToken = null;
			return;
		}
		
		nextToken();
		currToken = null;
	}
	
	protected abstract T nextToken() throws LexerException;
	
	protected boolean ensureCapacity(int cap) throws IOException {
		if(offset + cap < bufferCapacity)
			return true;
		
		int newCapacity = Math.max(cap, buffer.length);
		
		char[] newbuff = new char[newCapacity];
		int i = bufferCapacity - offset;
		int readCount = reader.read(newbuff, i, newCapacity - i);
		
		if(readCount < 1)
			return offset < bufferCapacity;
		
		i = 0;
		for(int j = offset; j < bufferCapacity; ++j) {
			newbuff[i++] = buffer[j];
		}
		
		offset = 0;
		
		bufferCapacity = readCount + i;
		buffer = newbuff;
		
		return true;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.setLength(0);
		sb.append("File: ").append(fileName).append(", Line ").append(line);
		
		return sb.toString();
	}
	
	@SuppressWarnings("serial")
	public static class LexerException extends Throwable{
		String message;
		public LexerException(Object lexer, String message) {
			this.message = lexer.toString() + ": " + message;
		}
		
		public String getMessage() {
			return message;
		}
	}
}
