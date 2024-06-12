package bspentspy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleFilter implements IFilter {
	private String criterium;
	private int hash;
	private ArrayList<String> paramNames;
	private ArrayList<Object> values;
	
	private static Pattern p = Pattern.compile("\\s*(.*?)\\s*;");
	private static Pattern p2 = Pattern.compile("\\\"(.*?)\\\"\\s*(in|=|like)\\s*(.*)");
	private static Pattern inp = Pattern.compile("\\\"([^\\\"]*?)\\\"\\s*[,\\)]");
	private static Pattern stripQuotes = Pattern.compile("\\\"*(.*?)\\\"*$");
	
	private static LinkedList<SimpleFilter> recent = new LinkedList<SimpleFilter>();
	
	public static SimpleFilter create(String criterium) throws Exception {
		SimpleFilter found = null;
		int hash = criterium.hashCode();
		
		ListIterator<SimpleFilter> it = recent.listIterator(recent.size());
		while(it.hasPrevious()) {
			int index = it.previousIndex();
			SimpleFilter f = it.previous();
			
			if(f.hash == hash) {
				recent.remove(index);
				found = f;
				break;
			}
		}
		
		if(found == null)
			found = new SimpleFilter(criterium);
		
		recent.add(found);
		
		if(recent.size() > 8)
			recent.removeFirst();
		
		return found;
	}
	
	private SimpleFilter(String criterium) throws Exception {
		hash = criterium.hashCode();
		
		String query = null;
		if((criterium.trim()).startsWith("$"))
			query = criterium.substring(criterium.indexOf("$"));
		
		if(query != null) {
			paramNames = new ArrayList<String>();
			values = new ArrayList<Object>();
			
			Matcher match = p.matcher(query);
			
			int i = 0;
			while(match.find()) {
				++i;
				
				String part = match.group(1);
				Matcher match2 = p2.matcher(part);
				if(!match2.find()) 
					throw new Exception();
				String paramName = match2.group(1);
				String op = match2.group(2);
				
				Matcher strip = stripQuotes.matcher(match2.group(3));
				strip.find();
				String value = strip.group(1);
				Object finalVal = null;
				
				if(op.equals("=")) {
					finalVal = value;
				} else if(op.equals("in")) {
					if(!value.startsWith("(") || !value.endsWith(")"))
						throw new Exception();
					Matcher inmatch = inp.matcher(value);
					HashSet<String> set = new HashSet<String>();
					
					while(inmatch.find()) {
						set.add(inmatch.group(1));
					}
					
					finalVal = set;
				} else {
					Pattern compiled = Pattern.compile(value);
					finalVal = compiled;
				}
				
				paramNames.add(paramName);
				values.add(finalVal);
			}
			
			if(i < 1)
				throw new Exception();
		}
		
		this.criterium = criterium;
	}
	
	public boolean match(Entity ent) {
		if(paramNames != null && values != null) {
			boolean matched = true;
			
			for(int i = 0; i < paramNames.size() && matched; ++i) {
				String keyval = ent.getKeyValue(paramNames.get(i));
				
				Object value = values.get(i);
				if(value.getClass() == String.class) {
					String val = (String)value;
					matched = keyval.equals(val);
				} else if(value.getClass() == HashSet.class) {
					HashSet<String> val = (HashSet<String>)value;
					matched = val.contains(keyval);
				} else {
					Pattern val = (Pattern)value;
					Matcher m = val.matcher(keyval);
					matched = m.matches();
				}
			}
			
			return matched;
		}
		return ent.getKeyValue("classname").indexOf(criterium) > -1 || ent.getKeyValue("targetname").indexOf(criterium) > -1;
	}
}
