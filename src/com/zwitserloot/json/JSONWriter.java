package com.zwitserloot.json;

import static com.zwitserloot.json.JSONParser.*;

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
	
	private static void preIndent(StringBuilder sb, int indent) {
		for (int i = 0; i < indent; i++) sb.append("  ");
	}
	
	static void toJSON(StringBuilder sb, String s, int indent) {
		preIndent(sb, indent);
		
		if (s == null) {
			sb.append("null");
			return;
		}
		
		sb.append('"');
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '\n') sb.append("\\n");
			else if (c == '\r') sb.append("\\r");
			else if (c == '\b') sb.append("\\b");
			else if (c == '\f') sb.append("\\f");
			else if (c == '\t') sb.append("\\t");
			else if (c == '"') sb.append("\\\"");
			else if (c == '\\') sb.append("\\\\");
			else if (c < 0x20 || c > 126) {
				sb.append(String.format("\\" + "u%04x", (int)c));
			} else sb.append(c);
		}
		sb.append('"');
	}
	
	static void toJSON(StringBuilder sb, Number i, int indent) {
		preIndent(sb, indent);
		
		if (i == null) {
			sb.append("null");
			return;
		}
		
		if (i instanceof Long) {
			long v = ((Long) i).longValue();
			if (v >= MAXIMUM_PRECISION_DOUBLE || v < -MAXIMUM_PRECISION_DOUBLE) {
				toJSON(sb, String.valueOf(v), indent);
			} else {
				sb.append(i);
			}
			return;
		}
		
		if (i instanceof Short || i instanceof Byte || i instanceof Integer) {
			sb.append(i);
			return;
		}
		
		if (i instanceof Float || i instanceof Double) {
			long v = i.longValue();
			double d = i.doubleValue();
			if (v == d) {
				sb.append(v);
			} else {
				sb.append(i);
			}
			return;
		}
		
		toJSON(sb, String.valueOf(i), indent);
	}
	
	static void toJSON(StringBuilder sb, Character i, int indent) {
		preIndent(sb, indent);
		
		if (i == null) {
			sb.append("null");
			return;
		}
		
		toJSON(sb, new String(new char[] {i}), indent);
	}
	
	static void toJSON(StringBuilder sb, Boolean i, int indent) {
		preIndent(sb, indent);
		
		if (i == null) {
			sb.append("null");
			return;
		}
		
		sb.append(i ? "true" : "false");
	}
	
	static void toJSON(StringBuilder sb, Object o, int indent) {
		toJSON0(sb, o, indent, new IdentityHashMap<Object, Object>());
	}
	
	private static void toJSON0(StringBuilder sb, Object o, int indent, IdentityHashMap<Object, Object> refs) {
		if (o == JSONParser.NULL) {
			preIndent(sb, indent);
			sb.append("null");
			return;
		}
		
		if (o instanceof Map<?, ?>) toJSON0(sb, (Map<?, ?>) o, indent, refs);
		else if (o instanceof Collection<?>) toJSON0(sb, (Collection<?>) o, indent, refs);
		else if (o instanceof String) toJSON(sb, (String) o, indent);
		else if (o instanceof Character) toJSON(sb, (Character) o, indent);
		else if (o instanceof Boolean) toJSON(sb, (Boolean) o, indent);
		else if (o.getClass().isArray()) {
			if (refs.put(o, MARKER) != null) throw new JSONException(
					"Circular references not supported (eg: An array containing itself)");
			int size = Array.getLength(o);
			List<Object> list = new ArrayList<Object>();
			for (int i = 0; i < size; i++) list.add(Array.get(o, i));
			toJSON0(sb, list, indent, refs);
		} else if (VALID_NUMBER_TYPES.contains(o.getClass())) toJSON(sb, (Number) o, indent);
		else throw new JSONException("Only basic objects can be turned into JSON: " + o.getClass());
	}
	
	private static void toJSON0(StringBuilder sb, Map<?, ?> in, int indent, IdentityHashMap<Object, Object> refs) {
		preIndent(sb, indent);
		
		if (in == null) {
			sb.append("null");
			return;
		}
		
		if (refs.put(in, MARKER) != null) throw new JSONException(
				"Circular references not supported (eg: A map containing itself)");
		
		if (in.isEmpty()) {
			sb.append("{}");
			return;
		}
		
		sb.append('{');
		if (indent != Integer.MIN_VALUE) sb.append('\n');
		int elemIndent = indent == Integer.MIN_VALUE ? Integer.MIN_VALUE : ((indent < 0 ? -indent : indent) + 1);
		
		for (Map.Entry<?, ?> e : in.entrySet()) {
			if (!(e.getKey() instanceof String)) throw new JSONException(
					"Only Strings allowed as keys in maps");
			toJSON(sb, e.getKey().toString(), elemIndent);
			sb.append(elemIndent == Integer.MIN_VALUE ? ":" : ": ");
			toJSON0(sb, e.getValue(), -elemIndent, refs);
			sb.append(elemIndent == Integer.MIN_VALUE ? "," : ",\n");
		}
		
		if (elemIndent == Integer.MIN_VALUE) {
			sb.setLength(sb.length() - 1);
			sb.append('}');
			return;
		}
		
		sb.setLength(sb.length() - 2);
		sb.append('\n');
		preIndent(sb, indent < 0 ? -indent : indent);
		sb.append('}');
		refs.remove(in);
	}
	
	private static void toJSON0(StringBuilder sb, Collection<?> in, int indent, IdentityHashMap<Object, Object> refs) {
		preIndent(sb, indent);
		
		if (in == null) {
			sb.append("null");
			return;
		}
		
		if (refs.put(in, MARKER) != null) throw new JSONException(
				"Circular references not supported (eg: A list containing itself)");
		
		if (in.isEmpty()) {
			sb.append("[]");
			return;
		}
		
		sb.append('[');
		if (indent != Integer.MIN_VALUE) sb.append('\n');
		int elemIndent = indent == Integer.MIN_VALUE ? Integer.MIN_VALUE : ((indent < 0 ? -indent : indent) + 1);
		
		for (Object item : in) {
			toJSON0(sb, item, elemIndent, refs);
			sb.append(elemIndent == Integer.MIN_VALUE ? "," : ",\n");
		}
		
		if (elemIndent == Integer.MIN_VALUE) {
			sb.setLength(sb.length() - 1);
			sb.append(']');
			return;
		}
		
		sb.setLength(sb.length() - 2);
		sb.append('\n');
		preIndent(sb, indent < 0 ? -indent : indent);
		sb.append(']');
		refs.remove(in);
	}
}
