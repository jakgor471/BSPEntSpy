package entspy;

//By jakgor471

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class FGD {
	public ArrayList<String> loadedFgds;
	public ArrayList<FGDEntry> entities;
	public HashMap<String, Integer> entMap;
	
	public FGD() {
		loadedFgds = new ArrayList<String>();
		entities = new ArrayList<FGDEntry>();
		entMap = new HashMap<String, Integer>();
	}
	
	public void loadFromStream(InputStreamReader in, String fgdName) throws Exception, FGDException {
		FGDLexer lexer = new FGDLexer(in, fgdName);
		
		while(lexer.getToken() != null) {
			System.out.println(lexer.line + " --- " + lexer.getToken().value);
			
			if(lexer.line > 2000) {
				parseNumber(lexer);
				break;
			}
			
			lexer.consume();
		}
		
		loadedFgds.add(fgdName);
	}
	
	private static String parseNumber(FGDLexer lexer) throws IOException, FGDException {
		FGDToken tok = lexer.getToken();
		if(tok.type == TokenType.numeric)
			return tok.value;
		
		if(tok.type == TokenType.symbol && (tok.value.equals("-") || tok.value.equals("+"))) {
			String number = tok.value;
			lexer.consume();
			
			tok = lexer.getToken();
			if(tok.type != TokenType.numeric)
				throw new FGDException(lexer, "Numerical value expected!");
			number += tok.value;
			return number;
		}
		
		throw new FGDException(lexer, "Numerical value expected!");
	}
	
	private static String parseDescription(FGDLexer lexer) throws IOException {
		FGDToken tok = lexer.getToken();
		
		if(tok.type != TokenType.string)
			return "";
		
		StringBuilder sb = new StringBuilder();
		
		boolean multiLineValid = false;
		while(tok != null) {
			if(tok.type == TokenType.string && multiLineValid) {
				multiLineValid = false;
				sb.append(tok.value);
				lexer.consume();
			} else if(tok.type == TokenType.symbol && tok.value.equals("+") && !multiLineValid) {
				lexer.consume();
				multiLineValid = true;
			} else break;
				
			tok = lexer.getToken();
		}
		sb.append(tok.value);
		String desc = sb.toString().replaceAll("\\n", " ");
		
		return desc;
	}
	
	private enum TokenType{
		ident,
		string,
		numeric,
		symbol
	}
	
	private class FGDToken{
		public TokenType type;
		public String value;
	}
	
	private class FGDLexer{
		public InputStreamReader reader;
		public StringBuilder sb;
		public String fgdName;
		public FGDToken currToken = null;
		public char[] buffer;
		public int offset;
		public int bufferCapacity;
		public int line = 1;
		
		public FGDLexer(InputStreamReader in, String name) {
			sb = new StringBuilder();
			reader = in;
			fgdName = name;
			buffer = new char[4096];
			offset = 0;
			bufferCapacity = 0;
			
			if(fgdName == null)
				fgdName = ".fgd";
		}
		
		public FGDToken getToken() throws IOException {
			if(currToken != null)
				return currToken;
			
			return nextToken();
		}
		
		public void consume() throws IOException {
			if(currToken != null) {
				currToken = null;
				return;
			}
			
			nextToken();
			currToken = null;
		}
		
		public FGDToken nextToken() throws IOException {
			currToken = null;
			final String allowedSymbols = "=[]:(),-+@";
			
			while(ensureCapacity(1)) {
				//identificator
				if(Character.isLetter(buffer[offset]) || buffer[offset] == '_') {
					sb.setLength(0);
					
					if(!ensureCapacity(256))
						return null;
					
					sb.append(buffer[offset++]);
					while(Character.isLetterOrDigit(buffer[offset]) || buffer[offset] == '_'){
						sb.append(buffer[offset++]);
						
						if(offset >= bufferCapacity && !ensureCapacity(64)) {
							return null;
						}
					}
					
					currToken = new FGDToken();
					currToken.type = TokenType.ident;
					currToken.value = sb.toString();
					return currToken;
				}
				
				//integer decimal
				if(Character.isDigit(buffer[offset])) {
					sb.setLength(0);
					
					if(!ensureCapacity(64))
						return null;
					
					sb.append(buffer[offset++]);
					while(Character.isDigit(buffer[offset])){
						sb.append(buffer[offset++]);
						
						if(offset >= bufferCapacity && !ensureCapacity(64)) {
							return null;
						}
					}
					
					currToken = new FGDToken();
					currToken.type = TokenType.numeric;
					currToken.value = sb.toString();
					return currToken;
				}
				
				//quoted string literals
				if(buffer[offset] == '"') {
					sb.setLength(0);
					
					if(!ensureCapacity(256))
						return null;
					
					++offset;
					while(buffer[offset] != '"'){
						sb.append(buffer[offset++]);
						
						if(offset >= bufferCapacity && !ensureCapacity(64)) {
							return null;
						}
					}
					++offset;
					
					currToken = new FGDToken();
					currToken.type = TokenType.string;
					currToken.value = sb.toString();
					
					if(line == 6542) {
						System.out.println("BREAK");
					}
					return currToken;
				}
				
				//symbols
				if(allowedSymbols.indexOf(buffer[offset]) > -1) {
					currToken = new FGDToken();
					currToken.type = TokenType.symbol;
					currToken.value = String.valueOf(buffer[offset++]);
					return currToken;
				}
				
				//skip comments
				if(buffer[offset] == '/') {
					if(ensureCapacity(1) && buffer[++offset] == '/') {
						while(ensureCapacity(256) && buffer[++offset] != '\n') {}
					}
				}
				
				if(buffer[offset] == '\n')
					++line;
				
				++offset; //anything left is a whitespace
			}
			
			return null;
		}
		
		public boolean ensureCapacity(int cap) throws IOException {
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
			sb.setLength(0);
			sb.append("Line ").append(line).append(", near \"");
			
			String part = new String(buffer, offset, Math.min(10, bufferCapacity - offset));
			sb.append(part).append("\".");
			
			return sb.toString();
		}
	}
	
	public static class FGDException extends Throwable{
		private static final long serialVersionUID = 2131L;
		String message;
		public FGDException(FGDLexer lexer, String message) {
			this.message = lexer.fgdName + ": " + message + " " + lexer.toString();
		}
		
		public String getMessage() {
			return message;
		}
	}
}
