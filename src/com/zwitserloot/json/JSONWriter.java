package com.zwitserloot.json;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class JSONWriter {
	private static final Object MARKER = new Object();
	private static final Set<Class<? extends Number>> VALID_NUMBER_TYPES;
	
	static {
		HashSet<Class<? extends Number>> set = new HashSet<Class<? extends Number>>();
		set.add(Byte.class);
		set.add(Short.class);
		set.add(Integer.class);
		set.add(Long.class);
		set.add(Float.class);
		set.add(Double.class);
		set.add(BigInteger.class);
		set.add(BigDecimal.class);
		VALID_NUMBER_TYPES = Collections.unmodifiableSet(set);
	}
	
	private JSONWriter() {
		//Utility class
	}
	
	static String toJSON(String s) {
		if ( s == null ) return "null";
		StringBuilder out = new StringBuilder();
		out.append('"');
		for ( int i = 0 ; i < s.length(); i++ ) {
			char c = s.charAt(i);
			if ( c < 0x20 || c == 127 || c == '"' || c == '\\' ) {
				out.append(String.format("\\" + "u%04x", (int)c));
			} else out.append(c);
		}
		out.append('"');
		return out.toString();
	}
	
	static String toJSON(Number i) {
		if ( i == null ) return "null";
		String x = i.toString();
		if ( x.endsWith(".0") ) return x.substring(0, x.length() - 2);
		else return x;
	}
	
	static String toJSON(Character i) {
		if ( i == null ) return "null";
		return toJSON(new String(new char[] {i}));
	}
	
	static String toJSON(Boolean i) {
		if ( i == null ) return "null";
		else return i ? "true" : "false";
	}
	
	static String toJSON(Object o) {
		return toJSON0(o, new IdentityHashMap<Object, Object>());
	}
	
	private static String toJSON0(Object o, IdentityHashMap<Object, Object> refs) {
		if ( o == null ) return "null";
		if ( o instanceof Map<?, ?> ) return toJSON0((Map<?, ?>)o, refs);
		if ( o instanceof Collection<?> ) return toJSON0((Collection<?>)o, refs);
		if ( o instanceof String ) return toJSON((String)o);
		if ( o instanceof Character ) return toJSON((Character)o);
		if ( o instanceof Boolean ) return toJSON((Boolean)o);
		if ( o.getClass().isArray() ) {
			if ( refs.put(o, MARKER) != null ) throw new JSONException(
			  "Circular references not supported (eg: An array containing itself)");
			int size = Array.getLength(o);
			List<Object> list = new ArrayList<Object>();
			for ( int i = 0 ; i < size ; i++ ) list.add(Array.get(o, i));
			return toJSON0(list, refs);
		}
		if ( VALID_NUMBER_TYPES.contains(o.getClass()) ) return toJSON((Number)o);
		
		throw new JSONException("Only basic objects can be turned into JSON");
	}
	
	private static String toJSON0(Map<?, ?> in, IdentityHashMap<Object, Object> refs) {
		if ( in == null ) return "null";
		if ( refs.put(in, MARKER) != null ) throw new JSONException(
		  "Circular references not supported (eg: A map containing itself)");
		
		StringBuilder builder = new StringBuilder();
		builder.append('{');
		boolean first = true;
		for ( Map.Entry<?, ?> e : in.entrySet() ) {
			if ( first ) first = false;
			else builder.append(',');
			
			if ( !(e.getKey() instanceof String) ) throw new JSONException(
					"Only Strings allowed as keys in maps");
			builder.append(toJSON(e.getKey().toString()));
			builder.append(':');
			builder.append(toJSON0(e.getValue(), new IdentityHashMap<Object, Object>(refs)));
		}
		
		builder.append('}');
		
		return builder.toString();
	}
	
	private static String toJSON0(Collection<?> in, IdentityHashMap<Object, Object> refs) {
		if ( in == null ) return "null";
		if ( refs.put(in, MARKER) != null ) throw new JSONException(
		  "Circular references not supported (eg: A list containing itself)");
		
		StringBuilder builder = new StringBuilder();
		builder.append('[');
		boolean first = true;
		for ( Object item : in ) {
			if ( first ) first = false;
			else builder.append(',');
			
			builder.append(toJSON0(item, new IdentityHashMap<Object, Object>(refs)));
		}
		builder.append(']');
		
		return builder.toString();
	}
}
