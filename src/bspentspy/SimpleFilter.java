package bspentspy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleFilter implements IFilter {
	private String criterium;
	private int hash;
	private ArrayList<String> paramNames;
	private ArrayList<Object> values;
	private ArrayList<Replace[]> replaces;
	
	private static Pattern p = Pattern.compile("\\s*(.*?)\\s*;");
	private static Pattern p2 = Pattern.compile("\\\"(.*?)\\\"\\s*(in|=|like)\\s*(.*)");
	private static Pattern inp = Pattern.compile("\\\"([^\\\"]*?)\\\"\\s*[,\\)]");
	private static Pattern stripQuotes = Pattern.compile("\\\"*(.*?)\\\"*$");
	private static Pattern repP = Pattern.compile("\\%\\[(.*?)\\]");
	
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
			query = criterium.substring(criterium.indexOf("$") + 1);
		
		if(query != null) {
			paramNames = new ArrayList<String>();
			values = new ArrayList<Object>();
			replaces = new ArrayList<Replace[]>();
			
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
				Replace[] reps = null;
				
				if(op.equals("=")) {
					reps = new Replace[] {replacePlaceholders(value)};
					finalVal = value;
				} else if(op.equals("in")) {
					if(!value.startsWith("(") || !value.endsWith(")"))
						throw new Exception();
					Matcher inmatch = inp.matcher(value);
					HashSet<String> set = new HashSet<String>();
					ArrayList<Replace> reps2 = new ArrayList<Replace>();
					
					while(inmatch.find()) {
						String found = inmatch.group(1);
						Replace r = replacePlaceholders(found);
						
						if(r != null) {
							reps2.add(r);
						} else {
							set.add(inmatch.group(1));
						}
					}
					
					reps = reps2.toArray(new Replace[reps2.size()]);
					finalVal = set;
				} else {
					reps = new Replace[] {replacePlaceholders(value)};
					
					Pattern compiled = Pattern.compile(value);
					finalVal = compiled;
				}
				
				paramNames.add(paramName);
				values.add(finalVal);
				replaces.add(reps);
			}
			
			if(i < 1)
				throw new Exception();
		}
		
		this.criterium = criterium;
	}
	
	private Replace replacePlaceholders(String value){
		Replace rep = new Replace();
		Matcher m = repP.matcher(value);
		
		ArrayList<String> fragments = new ArrayList<String>();
		ArrayList<String> params = new ArrayList<String>();
		int lastEnd = 0;
		while(m.find()) {		
			String fragment = value.substring(lastEnd, m.start());
			fragments.add(fragment);
			params.add(m.group(1));
			lastEnd = m.end();
		}
		
		String fragment = value.substring(lastEnd);
		fragments.add(fragment);
		
		if(fragments.size() < 1 || params.size() < 1)
			return null;
		
		rep.fragments = fragments.toArray(new String[fragments.size()]);
		rep.params = params.toArray(new String[params.size()]);
		
		return rep;
	}
	
	private String replace(Replace rep, Entity e) {
		StringBuilder sb = new StringBuilder();
		
		int i;
		for(i = 0; i < rep.params.length; ++i) {
			sb.append(rep.fragments[i]);
			sb.append(e.getKeyValue(rep.params[i]));
		}
		sb.append(rep.fragments[i]);
		
		return sb.toString();
	}
	
	public boolean match(Entity ent) {
		if(paramNames != null && values != null) {
			boolean matched = true;
			
			for(int i = 0; i < paramNames.size() && matched; ++i) {
				String keyval = ent.getKeyValue(paramNames.get(i));
				
				Object value = values.get(i);
				if(value.getClass() == String.class) {
					String val = (String)value;
					
					Replace[] reps = replaces.get(i);
					if(reps[0] != null)
						val = replace(reps[0], ent);
					
					matched = keyval.equals(val);
				} else if(value.getClass() == HashSet.class) {
					HashSet<String> val = (HashSet<String>)value;
					
					Replace[] reps = replaces.get(i);
					if(reps != null && reps.length > 0) {
						for(Replace r : reps) {
							val.add(replace(r, ent));
						}
					}
					
					matched = val.contains(keyval);
				} else {
					Pattern val = (Pattern)value;
					
					Replace[] reps = replaces.get(i);
					if(reps[0] != null)
						val = Pattern.compile(replace(reps[0], ent));
					
					Matcher m = val.matcher(keyval);
					matched = m.matches();
				}
			}
			
			return matched;
		}
		return ent.getKeyValue("classname").indexOf(criterium) > -1 || ent.getKeyValue("targetname").indexOf(criterium) > -1;
	}
	
	private static class Replace{
		String[] fragments;
		String[] params;
	}
}
