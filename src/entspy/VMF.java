package entspy;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import entspy.Lexer.BasicToken;
import entspy.Lexer.LexerException;
import entspy.Lexer.BasicTokenType;

public class VMF {
	public ArrayList<Entity> ents;
	
	public VMF() {
		ents = new ArrayList<Entity>();
	}
	
	public void loadFromReader(Reader in, String vmfName) throws LexerException {
		VMFLexer lexer = new VMFLexer(in, vmfName);
		
		while(lexer.getToken() != null) {
			if(lexer.getToken().type == BasicTokenType.ident) {
				//care only about entities
				if(lexer.getToken().value.equals("entity")) {
					lexer.consume();
					ents.add(parseEntity(lexer));
				} else {
					lexer.consume();
					skipClass(lexer);
				}
				continue;
			}
			
			throw new LexerException(lexer, "Unexpected token!");
		}
	}
	
	private static Entity parseEntity(VMFLexer lexer) throws LexerException {
		Entity ent = new Entity();
		
		lexer.expect(BasicTokenType.symbol, "{");
		lexer.consume();
		
		while(lexer.getToken().value != null) {
			if(lexer.getToken().isSymbol() && lexer.getToken().value.equals("}"))
				break;
			
			if(lexer.getToken().isIdent()) {
				if(lexer.getToken().value.equals("solid")) {
					ent.addKeyVal("model", "TODO: DEFINE IT!!!");
				}
				lexer.consume();
				skipClass(lexer);
				continue;
			}
			
			String key = lexer.expect(BasicTokenType.string).value;
			lexer.consume();
			String value = lexer.expect(BasicTokenType.string).value;
			lexer.consume();
			
			if(key.equals("id"))
				key = "hammerid";
			
			ent.addKeyVal(key, value);
		}
		
		lexer.consume(); //consume '}'
		
		ent.setnames();
		return ent;
	}
	
	private static void skipClass(VMFLexer lexer) throws LexerException {
		lexer.expect(BasicTokenType.symbol, "{");
		lexer.consume();
		int depth = 1;
		
		while(depth > 0 && lexer.getToken() != null) {
			if(lexer.getToken().isSymbol() && lexer.getToken().value.equals("{"))
				++depth;
			else if(lexer.getToken().isSymbol() && lexer.getToken().value.equals("}"))
				--depth;
			lexer.consume();
		}
	}
	
	private static class VMFToken extends BasicToken{		
		public boolean isNumeric() {
			return false; //not applicable to VMF
		}
	}
	
	private class VMFLexer extends Lexer<VMFToken>{
		private StringBuilder sb;
		public VMFLexer(Reader in, String name) {
			super(in, name);
			sb = new StringBuilder();
		}
		
		//throws exception if token is null
		public VMFToken expect() throws LexerException {
			if(getToken() == null) {
				throw new LexerException(this, "Unexpected EOF!!!");
			}
			return currToken;
		}
		
		//throws exception if token is null or token is not of a specified type
		public VMFToken expect(BasicTokenType type) throws LexerException {
			if(getToken() == null) {
				throw new LexerException(this, "Unexpected EOF!!!");
			}
			if(currToken.type != type)
				throw new LexerException(this, "Unexpected token type. Expected '" + type.toString() + "', got '" + currToken.type.toString() + "'!");
			
			return currToken;
		}
		
		//throws exception if token is null or token is not of a specified type or it's value differs from that specified
		public VMFToken expect(BasicTokenType type, String value) throws LexerException {
			if(getToken() == null) {
				throw new LexerException(this, "Unexpected EOF!!!");
			}
			if(currToken.type != type || !currToken.value.equals(value))
				throw new LexerException(this, "Unexpected token. Expected '" + value.toString() + "', got '" + currToken.value + "'!");
			
			return currToken;
		}

		protected VMFToken nextToken() throws LexerException {
			currToken = null;
			final String allowedSymbols = "{}";
			
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
						
						currToken = new VMFToken();
						currToken.type = BasicTokenType.ident;
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
						
						currToken = new VMFToken();
						currToken.type = BasicTokenType.string;
						currToken.value = sb.toString();
						
						return currToken;
					}
					
					//symbols
					if(allowedSymbols.indexOf(buffer[offset]) > -1) {
						currToken = new VMFToken();
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
			}catch(IOException e) {
				throw new LexerException(this, "IO Exception has occured!" + e.getMessage());
			}
			
			return null;
		}
	}
}
