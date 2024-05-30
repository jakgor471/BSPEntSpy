package entspy;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import entspy.Lexer.LexerException;
import entspy.Lexer.LexerToken;
import entspy.Lexer.TokenType;

public class VMF {
	public ArrayList<Entity> ents;
	
	public VMF() {
		ents = new ArrayList<Entity>();
	}
	
	public void loadFromReader(Reader in, String vmfName) throws IOException, LexerException {
		VMFLexer lexer = new VMFLexer(in, vmfName);
		
		while(lexer.getToken() != null) {
			if(lexer.getToken().type == TokenType.ident) {
				if(lexer.getToken().value == "entity") {
					
				} else {
					lexer.consume();
					skipClass(lexer);
				}
				continue;
			}
			
			throw new LexerException(lexer, "Unexpected token!");
		}
	}
	
	private static void skipClass(VMFLexer lexer) throws IOException, LexerException {
		lexer.expect(TokenType.symbol, "{");
	}
	
	private static class VMFToken implements LexerToken{
		public TokenType type;
		public String value;
		
		public boolean isString() {
			return type == TokenType.string;
		}
		
		public boolean isIdent() {
			return type == TokenType.ident;
		}
		
		public boolean isNumeric() {
			return false; //not applicable to VMF
		}
		
		public boolean isSymbol() {
			return type == TokenType.symbol;
		}
	}
	
	private class VMFLexer extends Lexer<VMFToken>{
		private StringBuilder sb;
		public VMFLexer(Reader in, String name) {
			super(in, name);
			sb = new StringBuilder();
		}
		
		public VMFToken expect(TokenType type, String value) throws IOException, LexerException {
			if(getToken() == null) {
				throw new LexerException(this, "Unexpected EOF!!!");
			}
			if(currToken.type != type || !currToken.value.equals(value))
				throw new LexerException(this, "Unexpected token. Expected '" + value.toString() + "', got '" + currToken.value + "'!");
			
			return (VMFToken) currToken;
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
						currToken.type = TokenType.ident;
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
						currToken.type = TokenType.string;
						currToken.value = sb.toString();
						
						return currToken;
					}
					
					//symbols
					if(allowedSymbols.indexOf(buffer[offset]) > -1) {
						currToken = new VMFToken();
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
			}catch(IOException e) {
				throw new LexerException(this, "IO Exception has occured!" + e.getMessage());
			}
			
			return null;
		}
	}
}
