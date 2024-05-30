package entspy;

//By jakgor471

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Callable;
import entspy.FGDEntry.*;

public class FGD {
	public double mapMin = -65536;
	public double mapMax = 65536;
	public ArrayList<String> loadedFgds;
	public ArrayList<FGDEntry> classes;
	public HashMap<String, Integer> classMap;
	
	public FGD() {
		loadedFgds = new ArrayList<String>();
		classes = new ArrayList<FGDEntry>();
		classMap = new HashMap<String, Integer>();
	}
	
	public void loadFromStream(InputStreamReader in, String fgdName, OnIncludeCallback onInclude) throws Exception, FGDException {
		FGDLexer lexer = new FGDLexer(in, fgdName);
		
		while(lexer.getToken() != null) {
			FGDToken tok = lexer.getToken();
			if(tok.isAt()) {
				lexer.consume();
				
				if(lexer.line > 673) {
					int x = 0;
				}

				tok = lexer.expect(TokenType.ident);
				if(tok.value.equals("mapsize")) {
					lexer.consume();
					lexer.expect(TokenType.symbol, "(");
					lexer.consume();
					mapMin = Double.valueOf(parseNumber(lexer));
					lexer.consume();
					lexer.expect(TokenType.symbol, ",");
					lexer.consume();
					mapMax = Double.valueOf(parseNumber(lexer));
					lexer.consume();
					lexer.expect(TokenType.symbol, ")");
					lexer.consume();
					
					continue;
				} 
				
				if(tok.value.equals("include")) {
					lexer.consume();
					lexer.expect(TokenType.string);
					
					onInclude.fileToLoad = lexer.getToken().value;
					if(!onInclude.call()) {
						throw new FGDException(lexer, "Could not include '" + lexer.getToken().value + "'!");
					}
					
					lexer.consume();
					
					continue;
				}
				
				if(tok.value.equals("MaterialExclusion") || tok.value.equals("AutoVisGroup")) {
					System.out.println("Skipped class at line " + lexer.line);
					skipClass(lexer);
					continue;
				}
				
				FGDEntry newClass = parseClass(lexer, this);
				classMap.put(newClass.classname, classes.size());
				classes.add(newClass);
				
				continue;
			}
			
			lexer.consume();
		}
		
		loadedFgds.add(fgdName);
	}
	
	private static ArrayList<String> parseCommaList(FGDLexer lexer, TokenType type) throws IOException, FGDException{
		FGDToken tok = lexer.expect();
		ArrayList<String> list = new ArrayList<String>();
		while(tok != null && (tok.type == type || tok.isComma())) {
			if(!tok.isComma()) {
				list.add(tok.value);
			}
			
			lexer.consume();
			tok = lexer.getToken();
		}
		
		return list;
	}
	
	private static ArrayList<String> parseBaseClassesAndSkip(FGDLexer lexer) throws IOException, FGDException{
		FGDToken tok = lexer.getToken();
		ArrayList<String> baseclasses = null;
		
		while(tok != null && !tok.isEqualSign()) {
			if(tok.isIdent()) {
				if(tok.value.equals("base")) {					
					lexer.consume();
					lexer.expect(TokenType.symbol, "(");
					lexer.consume();
					
					baseclasses = parseCommaList(lexer, TokenType.ident);
					
					lexer.expect(TokenType.symbol, ")");
					lexer.consume();
				} else {
					lexer.consume();
					if(lexer.getToken().isOpenParen()) {
						lexer.consume();
						while(lexer.getToken() != null && !lexer.getToken().isCloseParen()) {lexer.consume();}
						lexer.expect(TokenType.symbol, ")");
						lexer.consume();
					}
				}
			}
			
			tok = lexer.getToken();
		}
		
		return baseclasses;
	}
	
	private static InputOutput parseInputOutput(FGDLexer lexer) throws IOException, FGDException{
		InputOutput io = new InputOutput();
		
		lexer.expect(TokenType.ident);
		io.name = lexer.getToken().value;
		lexer.consume();
		
		lexer.expect(TokenType.symbol, "(");
		lexer.consume();
		
		String type = lexer.expect(TokenType.ident).value.toLowerCase();
		if(!FGDEntry.isValidDataType(type)) {
			type = "string";
		};
		
		io.setDataType(type);
		lexer.consume();
		
		lexer.expect(TokenType.symbol, ")");
		lexer.consume();
		
		if(lexer.getToken().isColon()) {
			lexer.consume();
			io.description = parseDescription(lexer);
		}
		
		return io;
	}
	
	private static ArrayList<PropChoicePair> parseChoices(FGDLexer lexer) throws IOException, FGDException{
		lexer.expect(TokenType.symbol, "[");
		lexer.consume();
		
		ArrayList<PropChoicePair> choices = new ArrayList<PropChoicePair>();
		
		while(lexer.getToken() != null && !lexer.getToken().isClassClose()) {
			PropChoicePair choice = new PropChoicePair();
			if(lexer.getToken().isString()) {
				choice.value = lexer.getToken().value;
			} else if(lexer.getToken().isNumericOrSign()) {
				choice.value = parseNumber(lexer);
			}
			lexer.consume();
			
			lexer.expect(TokenType.symbol, ":");
			lexer.consume();
			
			choice.name = lexer.expect().value;
			lexer.consume();
			
			if(lexer.expect().isColon()) {
				lexer.consume();
				choice.flagTicked = !lexer.expect().value.equals("0");
				lexer.consume();
			}
			
			choices.add(choice);
		}
		
		lexer.expect(TokenType.symbol, "]");
		lexer.consume();
		
		return choices;
	}
	
	private static Property parseProperty(FGDLexer lexer) throws IOException, FGDException {
		Property prop = null;
		
		String name = lexer.expect(TokenType.ident).value;
		lexer.consume();
		
		lexer.expect(TokenType.symbol, "(");
		lexer.consume();
		
		String type = lexer.expect(TokenType.ident).value.toLowerCase(); //flags are uppercase, whaat?
		
		if(!FGDEntry.isValidDataType(type)) {
			type = "string";
		}
		
		lexer.consume();
		lexer.expect(TokenType.symbol, ")");
		lexer.consume();
		
		if(lexer.expect().isIdent() && lexer.getToken().value.equals("readonly"))
			lexer.consume();
		
		String displayname = null;
		String defaultValue = null;
		String description = "";
		
		if(lexer.expect().isColon()) {
			lexer.consume();
			displayname = lexer.expect(TokenType.string).value;
			lexer.consume();
		}
		
		if(lexer.expect().isColon()) {
			lexer.consume();
			
			if(lexer.expect().isString()) {
				defaultValue = lexer.getToken().value;
				lexer.consume();
			}
			if(lexer.getToken().isNumericOrSign()) {
				defaultValue = parseNumber(lexer);
				lexer.consume();
			}
			
		}
		
		if(lexer.expect().isColon()) {
			lexer.consume();
			description = parseDescription(lexer);
		}
		
		if(type.equals("integer") || type.equals("float") || type.equals("string")) {
			prop = new Property();
		}else if(type.equals("flags") || type.equals("choices")) {
			prop = new PropertyChoices();
			
			lexer.expect(TokenType.symbol, "=");
			lexer.consume();
			
			((PropertyChoices)prop).choices = parseChoices(lexer); 
		}
		
		prop.setDataType(type);
		
		prop.name = name;
		prop.displayName = displayname;
		prop.defaultVal = defaultValue;
		prop.description = description;
		
		return prop;
	}
	
	private static FGDEntry parseClass(FGDLexer lexer, FGD fgdData) throws FGDException, Exception {
		FGDEntry newClass = new FGDEntry();
		
		if(!FGDEntry.isValidClass(lexer.expect(TokenType.ident).value)) throw new FGDException(lexer, "Unknown class type '" + lexer.getToken().value + "'!");
		newClass.setClass(lexer.getToken().value);
		lexer.consume();
		
		ArrayList<String> baseClasses = parseBaseClassesAndSkip(lexer);
		
		if(baseClasses != null) {
			newClass.baseclasses = new ArrayList<FGDEntry>();
			for(String s : baseClasses) {
				if(!fgdData.classMap.containsKey(s)) throw new Exception("Class '" + s + "' derives from a not existing class '" + s + "'. " +
						"Are all the necessary FGD files included?");
				
				newClass.baseclasses.add(fgdData.classes.get(fgdData.classMap.get(s)));
			}
		}
		lexer.expect(TokenType.symbol, "=");
		lexer.consume();
		
		newClass.classname = lexer.expect().value;
		lexer.consume();
		
		if(lexer.expect().isColon()) {
			lexer.consume();
			lexer.expect(TokenType.string);
			
			newClass.description = parseDescription(lexer);
		}
		
		lexer.expect(TokenType.symbol, "[");
		lexer.consume();
		
		while(lexer.getToken() != null && !lexer.getToken().isClassClose()) {
			lexer.expect(TokenType.ident);
			
			//Input / Output
			if(lexer.getToken().value.equals("input")) {
				lexer.consume();
				newClass.inputs.add(parseInputOutput(lexer));
				continue;
			}
			
			if(lexer.getToken().value.equals("output")) {
				lexer.consume();
				newClass.outputs.add(parseInputOutput(lexer));
				continue;
			}
			
			//Property
			Property prop = parseProperty(lexer);
			newClass.addProperty(prop);
		}
		
		lexer.expect(TokenType.symbol, "]");
		lexer.consume();
		
		return newClass;
	}
	
	private static void skipClass(FGDLexer lexer) throws IOException {
		while(lexer.getToken() != null && lexer.getToken().isClassClose()) {}
		lexer.consume();
	}
	
	private static String parseNumber(FGDLexer lexer) throws IOException, FGDException {
		FGDToken tok = lexer.expect();
		if(tok.isNumeric())
			return tok.value;
		
		if(tok.isSymbol() && (tok.value.equals("-") || tok.value.equals("+"))) {
			String number = tok.value;
			lexer.consume();
			
			tok = lexer.expect();
			if(!tok.isNumeric())
				throw new FGDException(lexer, "Numerical value expected!");
			number += tok.value;
			return number;
		}
		
		throw new FGDException(lexer, "Numerical value expected!");
	}
	
	private static String parseDescription(FGDLexer lexer) throws IOException, FGDException {
		FGDToken tok = lexer.expect();
		
		if(!tok.isString())
			return "";
		
		StringBuilder sb = new StringBuilder();
		
		boolean multiLineValid = true;
		while(tok != null) {
			if(tok.isString() && multiLineValid) {
				multiLineValid = false;
				sb.append(tok.value);
				lexer.consume();
			} else if(tok.isSymbol() && tok.value.equals("+") && !multiLineValid) {
				lexer.consume();
				multiLineValid = true;
			} else break;
				
			tok = lexer.getToken();
		}

		String desc = sb.toString().replace("\\n", "\n");
		
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
		
		public boolean isAt() {
			return value.equals("@") && type == TokenType.symbol;
		}
		
		public boolean isString() {
			return type == TokenType.string;
		}
		
		public boolean isIdent() {
			return type == TokenType.ident;
		}
		
		public boolean isNumeric() {
			return type == TokenType.numeric;
		}
		
		public boolean isNumericOrSign() {
			return type == TokenType.numeric || ((value.equals("-") || value.equals("+")) && type == TokenType.symbol);
		}
		
		public boolean isSymbol() {
			return type == TokenType.symbol;
		}
		
		public boolean isClassOpen() {
			return type == TokenType.symbol && value.equals("[");
		}
		
		public boolean isClassClose() {
			return type == TokenType.symbol && value.equals("]");
		}
		
		public boolean isEqualSign() {
			return type == TokenType.symbol && value.equals("=");
		}
		
		public boolean isComma() {
			return type == TokenType.symbol && value.equals(",");
		}
		
		public boolean isOpenParen() {
			return type == TokenType.symbol && value.equals("(");
		}
		
		public boolean isCloseParen() {
			return type == TokenType.symbol && value.equals(")");
		}
		
		public boolean isColon() {
			return type == TokenType.symbol && value.equals(":");
		}
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
		
		//throws exception if token is null
		public FGDToken expect() throws IOException, FGDException {
			if(getToken() == null) {
				throw new FGDException(this, "Unexpected EOF!!!");
			}
			return currToken;
		}
		
		//throws exception if token is null or token is not of a specified type
		public FGDToken expect(TokenType type) throws IOException, FGDException {
			if(getToken() == null) {
				throw new FGDException(this, "Unexpected EOF!!!");
			}
			if(currToken.type != type)
				throw new FGDException(this, "Unexpected token type. Expected '" + type.toString() + "', got '" + currToken.type.toString() + "'!");
			
			return currToken;
		}
		
		//throws exception if token is null or token is not of a specified type or it's value differs from that specified
		public FGDToken expect(TokenType type, String value) throws IOException, FGDException {
			if(getToken() == null) {
				throw new FGDException(this, "Unexpected EOF!!!");
			}
			if(currToken.type != type || !currToken.value.equals(value))
				throw new FGDException(this, "Unexpected token. Expected '" + value.toString() + "', got '" + currToken.value + "'!");
			
			return currToken;
		}
		
		public void consume() throws IOException {
			if(currToken != null) {
				currToken = null;
				return;
			}
			
			nextToken();
			currToken = null;
		}
		
		private FGDToken nextToken() throws IOException {
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
		
		private boolean ensureCapacity(int cap) throws IOException {
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
			sb.append("File: ").append(fgdName).append(", Line ").append(line);
			
			return sb.toString();
		}
	}
	
	public static class FGDException extends Throwable{
		private static final long serialVersionUID = 2131L;
		String message;
		public FGDException(FGDLexer lexer, String message) {
			this.message = lexer.toString() + ": " + message;
		}
		
		public String getMessage() {
			return message;
		}
	}
	
	public static class OnIncludeCallback implements Callable<Boolean>{
		public String fileToLoad;
		
		public Boolean call() throws Exception {
			return false;
		}
	}
}
