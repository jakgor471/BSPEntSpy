package bspentspy;

//By jakgor471

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;

import bspentspy.FGDEntry.DataType;
import bspentspy.FGDEntry.InputOutput;
import bspentspy.FGDEntry.PropChoicePair;
import bspentspy.FGDEntry.Property;
import bspentspy.FGDEntry.PropertyChoices;
import bspentspy.Lexer.BasicToken;
import bspentspy.Lexer.BasicTokenType;
import bspentspy.Lexer.LexerException;

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
	
	public FGDEntry getFGDClass(String classname) {
		if(classMap.containsKey(classname))
			return classes.get(classMap.get(classname));
		
		return null;
	}
	
	public String getClassHelp(String classname) {
		if(!classMap.containsKey(classname))
			return "No such class";
		FGDEntry entry = classes.get(classMap.get(classname));
		
		StringBuilder sb = new StringBuilder();
		sb.append("<h1>").append(entry.classname).append("</h1>");
		sb.append("<p>").append(entry.description).append("</p><br>");
		
		sb.append("<h3>PROPERTIES</h3><hr>");
		
		for(Property e : entry.properties) {
			if(e.name.equals("spawnflags"))
				continue;
			sb.append("<p><b>").append(e.getDisplayName()).append("</b> <i>").append(e.name).append("</i> &lt;").append(e.type.name).append("&gt; ");
			sb.append(e.description).append("</p>");
			
			if(e.type == DataType.choices) {
				ArrayList<PropChoicePair> choices = ((PropertyChoices)e).choices;
				
				sb.append("<ul>");
				for(PropChoicePair ch : choices) {
					sb.append("<li>").append(ch.value).append(" : ").append(ch.description).append("</li>");
				}
				sb.append("</ul>");
			}
		}
		
		if(entry.propmap.containsKey("spawnflags")) {
			sb.append("<br><h3>SPAWN FLAGS</h3><hr>");
			
			ArrayList<PropChoicePair> choices = ((PropertyChoices)entry.propmap.get("spawnflags")).choices;
			
			sb.append("<ul>");
			for(PropChoicePair ch : choices) {
				sb.append("<li>").append(ch.value).append(" : ").append(ch.description).append("</li>");
			}
			sb.append("</ul>");
		}
		
		
		if(entry.inputs.size() > 0) {
			sb.append("<br><h3>INPUTS</h3><hr>");
			
			for(InputOutput e : entry.inputs) {
				if(e.name.equals("spawnflags"))
					continue;
				sb.append("<p><b>").append(e.name).append("</b> ").append("&lt;").append(e.type.name).append("&gt; ");
				sb.append(e.description).append("</p>");
			}
		}
		
		if(entry.outputs.size() > 0) {
			sb.append("<br><h3>OUTPUTS</h3><hr>");
			
			for(InputOutput e : entry.outputs) {
				if(e.name.equals("spawnflags"))
					continue;
				sb.append("<p><b>").append(e.name).append("</b> ").append("&lt;").append(e.type.name).append("&gt; ");
				sb.append(e.description).append("</p>");
			}
		}
		
		sb.append("<hr>");
		if(entry.baseclasses.size() > 0) {
			sb.append("<p><i>Derives from: </i>");
			for(FGDEntry e : entry.baseclasses) {
				sb.append("<b>").append(e.classname).append("</b>, ");
			}
			sb.delete(sb.length() - 2, sb.length());
			sb.append("</p>");
		}
		if(entry.fgdDefinedIndex > -1 && entry.fgdDefinedIndex < loadedFgds.size()) {
			String filename = loadedFgds.get(entry.fgdDefinedIndex);
			
			sb.append("<p><i>Defined in '").append(filename).append("' at line ").append(entry.fgdDefinedLine).append("</i><p>");
		}
		
		return sb.toString();
	}
	
	public void loadFromReader(Reader in, String fgdName, OnIncludeCallback onInclude) throws LexerException {
		FGDLexer lexer = new FGDLexer(in, fgdName);
		
		int fgdIndex = loadedFgds.size();
		loadedFgds.add(fgdName);
		
		while(lexer.getToken() != null) {
			FGDToken tok = lexer.getToken();
			if(tok.isAt()) {
				lexer.consume();

				tok = lexer.expect(BasicTokenType.ident);
				if(tok.value.equals("mapsize")) {
					lexer.consume();
					lexer.expect(BasicTokenType.symbol, "(");
					lexer.consume();
					mapMin = Double.valueOf(parseNumber(lexer));
					lexer.consume();
					lexer.expect(BasicTokenType.symbol, ",");
					lexer.consume();
					mapMax = Double.valueOf(parseNumber(lexer));
					lexer.consume();
					lexer.expect(BasicTokenType.symbol, ")");
					lexer.consume();
					
					continue;
				} 
				
				if(tok.value.equals("include")) {
					lexer.consume();
					lexer.expect(BasicTokenType.string);
					
					onInclude.fileToLoad = lexer.getToken().value;
					try {
						if(onInclude == null || !onInclude.call()) {
							throw new LexerException(lexer, "Could not include '" + lexer.getToken().value + "'!");
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					lexer.consume();
					
					continue;
				}
				
				if(tok.value.equals("MaterialExclusion") || tok.value.equals("AutoVisGroup")) {
					skipClass(lexer);
					continue;
				}
				
				FGDEntry newClass = parseClass(lexer, this, fgdIndex);
				classMap.put(newClass.classname, classes.size());
				classes.add(newClass);
				
				continue;
			}
			
			throw new LexerException(lexer, "Unknown token!");
		}
	}
	
	//TODO: Replace html unsafe characters with safe ones!!!
	private static String makeHTMLSafe(String s) {
		return s;
	}
	
	private static ArrayList<String> parseCommaList(FGDLexer lexer, BasicTokenType type) throws LexerException{
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
	
	private static ArrayList<String> parseBaseClassesAndSkip(FGDLexer lexer) throws LexerException{
		FGDToken tok = lexer.getToken();
		ArrayList<String> baseclasses = null;
		
		while(tok != null && !tok.isEqualSign()) {
			if(tok.isIdent()) {
				if(tok.value.equals("base")) {					
					lexer.consume();
					lexer.expect(BasicTokenType.symbol, "(");
					lexer.consume();
					
					baseclasses = parseCommaList(lexer, BasicTokenType.ident);
					
					lexer.expect(BasicTokenType.symbol, ")");
					lexer.consume();
				} else {
					lexer.consume();
					if(lexer.getToken().isOpenParen()) {
						lexer.consume();
						while(lexer.getToken() != null && !lexer.getToken().isCloseParen()) {lexer.consume();}
						lexer.expect(BasicTokenType.symbol, ")");
						lexer.consume();
					}
				}
			}
			
			tok = lexer.getToken();
		}
		
		return baseclasses;
	}
	
	private static InputOutput parseInputOutput(FGDLexer lexer) throws LexerException{
		InputOutput io = new InputOutput();
		
		lexer.expect(BasicTokenType.ident);
		io.name = lexer.getToken().value;
		lexer.consume();
		
		lexer.expect(BasicTokenType.symbol, "(");
		lexer.consume();
		
		String type = lexer.expect(BasicTokenType.ident).value.toLowerCase();
		if(!FGDEntry.isValidDataType(type)) {
			type = "string";
		};
		
		io.setDataType(type);
		lexer.consume();
		
		lexer.expect(BasicTokenType.symbol, ")");
		lexer.consume();
		
		if(lexer.getToken().isColon()) {
			lexer.consume();
			io.description = parseDescription(lexer);
		}
		
		return io;
	}
	
	private static void parseChoices(FGDLexer lexer, PropertyChoices propChoices) throws LexerException{
		lexer.expect(BasicTokenType.symbol, "[");
		lexer.consume();
		
		while(lexer.getToken() != null && !lexer.getToken().isClassClose()) {
			PropChoicePair choice = new PropChoicePair();
			if(lexer.getToken().isString()) {
				choice.value = lexer.getToken().value;
			} else if(lexer.getToken().isNumericOrSign()) {
				choice.value = parseNumber(lexer);
			}
			lexer.consume();
			
			lexer.expect(BasicTokenType.symbol, ":");
			lexer.consume();
			
			choice.description = lexer.expect().value;
			lexer.consume();
			
			if(lexer.expect().isColon()) {
				lexer.consume();
				choice.flagTicked = !lexer.expect().value.equals("0");
				lexer.consume();
			}
			
			propChoices.choices.add(choice);
			propChoices.chMap.put(choice.value.toLowerCase().trim(), choice);
		}
		
		lexer.expect(BasicTokenType.symbol, "]");
		lexer.consume();
	}
	
	private static Property parseProperty(FGDLexer lexer) throws LexerException {
		Property prop = null;
		
		String name = lexer.expect(BasicTokenType.ident).value;
		lexer.consume();
		
		lexer.expect(BasicTokenType.symbol, "(");
		lexer.consume();
		
		String type = lexer.expect(BasicTokenType.ident).value.toLowerCase(); //flags are uppercase, whaat?
		
		if(!FGDEntry.isValidDataType(type)) {
			type = "string";
		}
		
		lexer.consume();
		lexer.expect(BasicTokenType.symbol, ")");
		lexer.consume();
		
		boolean readOnly = false;
		
		if(lexer.expect().isIdent() && lexer.getToken().value.equals("readonly")) {
			readOnly = true;
			lexer.consume();
		}
		
		String displayname = null;
		String defaultValue = "";
		String description = "";
		
		if(lexer.expect().isColon()) {
			lexer.consume();
			displayname = lexer.expect(BasicTokenType.string).value;
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
		
		if(type.equals("flags") || type.equals("choices")) {
			prop = new PropertyChoices();
			PropertyChoices propChoices = (PropertyChoices)prop;
			
			lexer.expect(BasicTokenType.symbol, "=");
			lexer.consume();
			
			parseChoices(lexer, propChoices); 
		} else {
			prop = new Property();
		}
		
		prop.setDataType(type);
		
		prop.readOnly = readOnly;
		prop.name = name;
		prop.displayName = displayname;
		prop.defaultVal = defaultValue;
		prop.description = description;
		
		return prop;
	}
	
	private static FGDEntry parseClass(FGDLexer lexer, FGD fgdData, int fileIndex) throws LexerException {
		FGDEntry newClass = new FGDEntry();
		newClass.fgdDefinedIndex = fileIndex;
		newClass.fgdDefinedLine = lexer.line;
		
		if(!FGDEntry.isValidClass(lexer.expect(BasicTokenType.ident).value)) throw new LexerException(lexer, "Unknown class type '" + lexer.getToken().value + "'!");
		newClass.setClass(lexer.getToken().value);
		lexer.consume();
		
		ArrayList<String> baseClasses = parseBaseClassesAndSkip(lexer);
		
		lexer.expect(BasicTokenType.symbol, "=");
		lexer.consume();
		
		newClass.classname = lexer.expect().value;
		lexer.consume();
		
		if(baseClasses != null) {
			newClass.baseclasses = new ArrayList<FGDEntry>();
			for(String s : baseClasses) {
				if(!fgdData.classMap.containsKey(s)) throw new LexerException(lexer, "Class '" + newClass.classname + "' derives from not existing '" + s + "'!!!");
				
				newClass.baseclasses.add(fgdData.classes.get(fgdData.classMap.get(s)));
				
				FGDEntry base = fgdData.classes.get(fgdData.classMap.get(s));
				
				for(Property p : base.properties)
					newClass.addProperty(p);
				
				for(InputOutput p : base.inputs)
					newClass.inputs.add(p);
				
				for(InputOutput p : base.outputs)
					newClass.outputs.add(p);
			}
		}
		
		if(lexer.expect().isColon()) {
			lexer.consume();
			lexer.expect(BasicTokenType.string);
			
			newClass.description = parseDescription(lexer);
		}
		
		lexer.expect(BasicTokenType.symbol, "[");
		lexer.consume();
		
		while(lexer.getToken() != null && !lexer.getToken().isClassClose()) {
			lexer.expect(BasicTokenType.ident);
			
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
		
		lexer.expect(BasicTokenType.symbol, "]");
		lexer.consume();
		
		return newClass;
	}
	
	private static void skipClass(FGDLexer lexer) throws LexerException {	
		int depth = -1;
		while((depth > 0 || depth == -1) && lexer.getToken() != null) {
			if(lexer.getToken().isSymbol() && lexer.getToken().value.equals("[")) {
				if(depth == -1)
					depth = 0;
				++depth;
			}
			else if(lexer.getToken().isSymbol() && lexer.getToken().value.equals("]"))
				--depth;
			lexer.consume();
		}
	}
	
	private static String parseNumber(FGDLexer lexer) throws LexerException {
		FGDToken tok = lexer.expect();
		if(tok.isNumeric())
			return tok.value;
		
		if(tok.isSymbol() && (tok.value.equals("-") || tok.value.equals("+"))) {
			String number = tok.value;
			lexer.consume();
			
			tok = lexer.expect();
			if(!tok.isNumeric())
				throw new LexerException(lexer, "Numerical value expected!");
			number += tok.value;
			return number;
		}
		
		throw new LexerException(lexer, "Numerical value expected!");
	}
	
	private static String parseDescription(FGDLexer lexer) throws LexerException {
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
	
	private static class FGDToken extends BasicToken{		
		public boolean isAt() {
			return value.equals("@") && type == BasicTokenType.symbol;
		}
		
		public boolean isNumericOrSign() {
			return type == BasicTokenType.numeric || ((value.equals("-") || value.equals("+")) && type == BasicTokenType.symbol);
		}
		
		public boolean isClassClose() {
			return type == BasicTokenType.symbol && value.equals("]");
		}
		
		public boolean isEqualSign() {
			return type == BasicTokenType.symbol && value.equals("=");
		}
		
		public boolean isComma() {
			return type == BasicTokenType.symbol && value.equals(",");
		}
		
		public boolean isOpenParen() {
			return type == BasicTokenType.symbol && value.equals("(");
		}
		
		public boolean isCloseParen() {
			return type == BasicTokenType.symbol && value.equals(")");
		}
		
		public boolean isColon() {
			return type == BasicTokenType.symbol && value.equals(":");
		}
	}
	
	private class FGDLexer extends Lexer<FGDToken>{
		private StringBuilder sb;
		
		public FGDLexer(Reader in, String name) {
			super(in, name);
			
			sb = new StringBuilder();
			if(fileName == null)
				fileName = ".fgd";
		}
		
		//throws exception if token is null
		public FGDToken expect() throws LexerException {
			if(getToken() == null) {
				throw new LexerException(this, "Unexpected EOF!!!");
			}
			return currToken;
		}
		
		//throws exception if token is null or token is not of a specified type
		public FGDToken expect(BasicTokenType type) throws LexerException {
			if(getToken() == null) {
				throw new LexerException(this, "Unexpected EOF!!!");
			}
			if(currToken.type != type)
				throw new LexerException(this, "Unexpected token type. Expected '" + type.toString() + "', got '" + currToken.type.toString() + "'!");
			
			return (FGDToken) currToken;
		}
		
		//throws exception if token is null or token is not of a specified type or it's value differs from that specified
		public FGDToken expect(BasicTokenType type, String value) throws LexerException {
			if(getToken() == null) {
				throw new LexerException(this, "Unexpected EOF!!!");
			}
			if(currToken.type != type || !currToken.value.equals(value))
				throw new LexerException(this, "Unexpected token. Expected '" + value.toString() + "', got '" + currToken.value + "'!");
			
			return (FGDToken) currToken;
		}
		
		protected FGDToken nextToken() throws LexerException {
			currToken = null;
			final String allowedSymbols = "=[]:(),-+@";
			
			try {
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
						currToken.type = BasicTokenType.ident;
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
						currToken.type = BasicTokenType.numeric;
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
						currToken.type = BasicTokenType.string;
						currToken.value = sb.toString();
						
						return currToken;
					}
					
					//symbols
					if(allowedSymbols.indexOf(buffer[offset]) > -1) {
						currToken = new FGDToken();
						currToken.type = BasicTokenType.symbol;
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
			} catch(IOException e) {
				throw new LexerException(this, "IO Exception has occured!" + e.getMessage());
			}
			
			return null;
		}
	}
	
	public static class OnIncludeCallback implements Callable<Boolean>{
		public String fileToLoad;
		
		public Boolean call() throws Exception {
			return false;
		}
	}
}
