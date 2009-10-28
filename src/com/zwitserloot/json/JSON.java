package com.zwitserloot.json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public final class JSON {
	private Object[] path;
	private final Object object;
	private static final Object UNDEFINED = new Object();
	
	private static String typeOf(Object o) {
		if (o == null) return "null";
		if (o == UNDEFINED) return "undefined";
		if (o instanceof String) return "string";
		if (o instanceof Number) return "number";
		if (o instanceof Boolean) return "boolean";
		if (o instanceof List<?>) return "array";
		if (o instanceof Map<?, ?>) return "object";
		
		return "??" + o.getClass();
	}
	
	private JSON(Object o) {
		this.object = o;
		this.path = new Object[0];
	}
	
	private JSON(Object[] path, Object o) {
		this.object = o;
		this.path = path;
	}
	
	private Object dig() {
		return dig(path.length);
	}
	
	private Object dig(int l) {
		Object out = object;
		
		for (int i = 0; i < l; i++) {
			Object x = path[i];
			if (out == null) out = UNDEFINED;
			if (out == UNDEFINED) continue;
			if (x instanceof Integer) {
				int idx = ((Integer)x).intValue();
				try {
					out = ((List<?>)out).get(idx);
				} catch (Exception e) {
					out = UNDEFINED;
				}
			} else {
				String key = (String)x;
				try {
					Map<?, ?> m = (Map<?, ?>)out;
					if (!m.containsKey(key)) out = UNDEFINED;
					else out = m.get(key);
				} catch (Exception e) {
					out = UNDEFINED;
				}
			}
		}
		
		return out;
	}
	
	public static JSON newMap() {
		return new JSON(new HashMap<Object, Object>());
	}
	
	public static JSON newList() {
		return new JSON(new ArrayList<Object>());
	}
	
	public static JSON parse(String s) {
		return new JSON(new JSONParser(s).parseObject());
	}
	
	public String toJSON() {
		return JSONWriter.toJSON(dig());
	}
	
	public boolean exists() {
		return dig() != UNDEFINED;
	}
	
	public String getPath() {
		return getPath(-1);
	}
	
	public String getPath(int len) {
		StringBuilder out = new StringBuilder("~");
		len = len == -1 ? path.length : len;
		for (int i = 0; i < len; i++)
			out.append("/").append(path[i]);
		return out.toString();
	}
	
	private void indexError(int index) {
		throw new JSONException("Not a valid list index: " + index);
	}
	
	private void structureError(int ct, String expected) {
		throw new JSONException("Key " + getPath(ct) + " contains a " +
				typeOf(dig(ct)) + " while a " + expected + " was expected");
	}
	
	private void listTooSmallError(int ct) {
		throw new JSONException("Key " + getPath(ct) + " contains a list that is too small");
	}
	
	private void notABoolean(String s) {
		throw new JSONException("Key " + getPath() + " contains '" + s + "' which is not a known boolean value.");
	}

	private void invalidType(String targetType) {
		if (dig() == UNDEFINED) throw new JSONException("Key " + getPath() + " does not exist");
		else {
			String jsType = typeOf(dig());
			throw new JSONException("Key " + getPath() + " contains a " + jsType + " which is not convertable to a " + targetType);
		}
	}
	
	private void notIntegral(double v) {
		throw new JSONException("Key " + getPath() + " contains " + v + ", which is not an integral number");
	}
	
	public boolean isNull() {
		Object o = dig();
		if (o == UNDEFINED) {
			invalidType("null");
			return false;
		} else return o == null;
	}
	
	public Object asObject() {
		Object o = dig();
		
		if (o == UNDEFINED) {
			invalidType("object");
			return null;
		}
		
		return o;
	}
	
	public Object asObject(Object alt) {
		try {
			return asObject();
		} catch (Exception e) {
			return alt;
		}
	}
	
	public String asString() {
		Object o = dig();
		if (o == null) return null;
		String t = typeOf(o);
		if (t.equals("number") || t.equals("string") || t.equals("boolean")) return String.valueOf(o);
		invalidType("string");
		return null;
	}
	
	public String asString(String alt) {
		try {
			String x = asString();
			return x == null ? alt : x;
		} catch (Exception e) {
			return alt;
		}
	}
	
	private Number asNumber(String expected) {
		Object o = dig();
		Number n = null;
		try {
			if (o instanceof Number) n = (Number)o;
			else if (o instanceof String) {
				String s = String.valueOf(o);
				if (s.indexOf(".") > -1) n = Double.parseDouble(s);
				if (expected.equals("int")) n = Integer.parseInt(s);
				else if (expected.equals("long")) n = Long.parseLong(s);
				else n = Double.parseDouble(s);
			} else throw new IllegalArgumentException();
		} catch (Exception e) {
			invalidType(expected);
			return 0.0;
		}
		
		return n;
	}
	
	public double asDouble() {
		return asNumber("double").doubleValue();
	}
	
	public double asDouble(double alt) {
		try {
			return asDouble();
		} catch (Exception e) {
			return alt;
		}
	}
	
	public int asInt() {
		double v = asNumber("int").doubleValue();
		int v2 = (int)v;
		if (v != v2) {
			notIntegral(v);
			return 0;
		} else return v2;
	}
	
	public int asInt(int alt) {
		try {
			return asInt();
		} catch (Exception e) {
			return alt;
		}
	}
	
	private static final long MAXIMUM_PRECISION_DOUBLE = 1L << 52 -1;
	
	public long asLong() {
		Number n = asNumber("long");
		if (n instanceof Double) {
			long l = n.longValue();
			if (l <= MAXIMUM_PRECISION_DOUBLE && l>= -MAXIMUM_PRECISION_DOUBLE) {
				double l2 = n.doubleValue();
				double l3 = l;
				if (l3 != l2) {
					invalidType("long");
					return 0L;
				} else return l;
			} else return l; //Checking is sort of pointless at this point, though converting
			//doubles of this magnitude to longs is inprecise, so a future version might error anyway.
		} else return n.longValue();
	}
	
	public long asLong(long alt) {
		try {
			return asLong();
		} catch (Exception e) {
			return alt;
		}
	}
	
	public char asChar() {
		String s = asString();
		if (s.length() > 0) return s.charAt(0);
		throw new JSONException("Key " + getPath() + " contains the empty string, which is not convertable to a char");
	}
	
	public char asChar(char alt) {
		try {
			return asChar();
		} catch (Exception e) {
			return alt;
		}
	}
	
	public <E extends Enum<E>> E asEnum(Class<E> enumType) {
		Object o = dig();
		if (o == null) return null;
		if (!(o instanceof String)) {
			invalidType("enum");
			return null;
		}
		
		try {
			return Enum.valueOf(enumType, (String)o);
		} catch (Exception e) {
			throw new JSONException("Key " + getPath() + " contains '" + o + "' which is not a value for enum '" + enumType.getName());
		}
	}
	
	public <E extends Enum<E>> E asEnum(Class<E> enumType, E alt) {
		E value;
		try {
			value = asEnum(enumType);
		} catch (Exception e) {
			return alt;
		}
		
		return value == null ? alt : value;
	}
	
	public boolean asBoolean() {
		Object o = dig();
		String t = typeOf(o);
		if (t.equals("number")) return ((Number)o).doubleValue() != 0;
		if (t.equals("string")) {
			String s = " " + o.toString().toLowerCase() + " ";
			if (" true yes 1 t y on ".indexOf(s) > -1) return true;
			if (" false no 0 f n off ".indexOf(s) > -1) return false;
			notABoolean(s);
			return false;
		}
		if (t.equals("boolean")) return ((Boolean)o).booleanValue();
		invalidType("boolean");
		return false;
	}
	
	public boolean asBoolean(boolean alt) {
		try {
			return asBoolean();
		} catch (Exception e) {
			return alt;
		}
	}
	
	private final static class JSONList implements List<JSON> {
		private final JSON json;
		private final int offset, limit;
		
		private JSONList(JSON json, int offset, int limit) {
			this.json = json;
			this.offset = offset;
			this.limit = limit;
		}
		
		public boolean add(JSON o) {
			throw new UnsupportedOperationException();
		}
		
		public void add(int index, JSON o) {
			throw new UnsupportedOperationException();
		}
		
		public boolean addAll(Collection<? extends JSON> c) {
			throw new UnsupportedOperationException();
		}
		
		public boolean addAll(int index, Collection<? extends JSON> c) {
			throw new UnsupportedOperationException();
		}
		
		public void clear() {
			throw new UnsupportedOperationException();
		}
		
		public boolean contains(Object o) {
			throw new UnsupportedOperationException();
		}
		
		public boolean containsAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}
		
		public JSON get(int index) {
			if (index < 0 || limit > -1 && index >= limit) throw new IndexOutOfBoundsException();
			Object o = json.dig();
			if (o == UNDEFINED) {
				json.invalidType("array");
				return null;
			}
			
			if (o instanceof List<?>) {
				if (index < size()) return json.get(index + offset);
				else throw new IndexOutOfBoundsException();
			} else if (o == null) throw new IndexOutOfBoundsException();
			else if (index == offset) return json;
			else throw new IndexOutOfBoundsException();
		}
		
		public int indexOf(Object o) {
			throw new UnsupportedOperationException();
		}
		
		public boolean isEmpty() {
			return size() == 0;
		}
		
		public Iterator<JSON> iterator() {
			return new Iterator<JSON>() {
				private int pos = 0;
				
				public boolean hasNext() {
					return pos < size();
				}
				
				public JSON next() {
					if (!hasNext()) throw new NoSuchElementException();
					return get(pos++);
				}
				
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}
		
		public int lastIndexOf(Object o) {
			throw new UnsupportedOperationException();
		}
		
		public ListIterator<JSON> listIterator() {
			throw new UnsupportedOperationException();
		}
		
		public ListIterator<JSON> listIterator(int index) {
			throw new UnsupportedOperationException();
		}
		
		public boolean remove(Object o) {
			throw new UnsupportedOperationException();
		}
		
		public JSON remove(int index) {
			throw new UnsupportedOperationException();
		}
		
		public boolean removeAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}
		
		public boolean retainAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}
		
		public JSON set(int index, JSON element) {
			throw new UnsupportedOperationException();
		}
		
		public int size() {
			if (limit >= 0) return limit - offset;
			
			Object o = json.dig();
			if (o instanceof List<?>) return ((List<?>)o).size();
			if (o == UNDEFINED) {
				json.invalidType("array");
				return 0;
			}
			if (o == null) return 0;
			return 1;
		}
		
		public List<JSON> subList(int fromIndex, int toIndex) {
			if (toIndex < fromIndex) throw new IllegalArgumentException();
			if (fromIndex < 0) throw new IndexOutOfBoundsException();
			if (toIndex > size()) throw new IndexOutOfBoundsException();
			
			return new JSONList(json, offset + fromIndex, offset + toIndex);
		}
		
		public Object[] toArray() {
			throw new UnsupportedOperationException();
		}
		
		public <T> T[] toArray(T[] a) {
			throw new UnsupportedOperationException();
		}
		
		@Override public String toString() {
			Object o = json.dig();
			if (o == UNDEFINED) return "[List: UNDEFINED]";
			StringBuilder sb = new StringBuilder();
			sb.append("[");
			for (int i = 0; i < size(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(get(i));
			}
			sb.append("]");
			return sb.toString();
		}
	}
	
	public List<JSON> asList() {
		Object o = dig();
		if (o == null || o == UNDEFINED) return Collections.emptyList();
		else return new JSONList(this, 0, -1);
	}
	
	public Set<String> keySet() {
		Set<String> set = new HashSet<String>();
		
		Object o = dig();
		if (o instanceof Map<?, ?>) {
			for (Object k : ((Map<?, ?>)o).keySet()) set.add(String.valueOf(k));
		} else if (o != null && o != UNDEFINED) {
			invalidType("object");
			return null;
		}
		
		return set;
	}
	
	private static Object[] addToPath(Object[] in, Object add) {
		Object[] newPath = new Object[in.length +1];
		System.arraycopy(in, 0, newPath, 0, in.length);
		newPath[in.length] = add;
		return newPath;
	}
	
	public JSON add() {
		Object o = dig();
		int i = 0;
		if (o instanceof List<?>) i = ((List<?>)o).size();
		else if (o == UNDEFINED || o == null) i = 0;
		else structureError(path.length, "array");
		
		return new JSON(addToPath(path, i), object);
	}
	
	public JSON get(int idx) {
		if (idx < 0) {
			indexError(idx);
			return null;
		}
		
		return new JSON(addToPath(path, idx), object);
	}
	
	public JSON get(String path) {
		return new JSON(addToPath(this.path, path), object);
	}
	
	public void setInt(int value) {
		createAndSet((double)value);
	}
	
	public void setDouble(double value) {
		createAndSet(value);
	}
	
	public void setLong(long value) {
		if (value <= -MAXIMUM_PRECISION_DOUBLE || value >= MAXIMUM_PRECISION_DOUBLE) setString(String.valueOf(value));
		else setDouble(value);
	}
	
	public void setString(String value) {
		createAndSet(value);
	}
	
	public void setBoolean(boolean value) {
		createAndSet(value);
	}
	
	public void setEnum(Enum<?> value) {
		if (value == null) createAndSet(null);
		setString(value.name());
	}
	
	public void setChar(char value) {
		setString(String.valueOf(value));
	}
	
	public void setNull() {
		createAndSet(null);
	}
	
	public void setEmptyList() {
		createAndSet(new ArrayList<Object>());
	}
	
	public void setEmptyMap() {
		createAndSet(new HashMap<Object, Object>());
	}
	
	public void setWithJSON(JSON json) {
		createAndSet(json.dig());
	}
	
	@SuppressWarnings("unchecked")
	private void createAndSet(Object toSet) {
		int l = path.length;
		if (l == 0) throw new IllegalStateException();
		
		Object o = object;
		Object k = path[0];
		boolean x = k instanceof Number;
		Object m = null;
		boolean y = false;
		for (int i = 1; i <= l; i++) {
			if (i < l) {
				m = path[i];
				y = m instanceof Number;
				
				if (o instanceof List) {
					if (!x) {
						structureError(i, "array");
						return;
					}
					List<?> list = (List<?>)o;
					int idx = ((Number)k).intValue();
					if (idx < -1 || idx > list.size()) {
						listTooSmallError(i -1);
						return;
					}
					if (idx < list.size()) {
						o = list.get(idx);
						k = m; x = y;
						continue;
					}
				} else if (o instanceof Map) {
					if (x) {
						structureError(i, "object");
						return;
					}
					
					String key = String.valueOf(k);
					Map<?, ?> map = (Map<?, ?>)o;
					if (map.containsKey(key)) {
						o = map.get(key);
						k = m; x = y;
						continue;
					}
				}
			}
			
			Object z = i < l ? (y ? new ArrayList<Object>() : new HashMap<Object, Object>()) : toSet;
			if (x) {
				int len = ((List<?>)o).size();
				int idx = ((Number)k).intValue();
				if (idx == -1 || idx == len) {
					path[i-1] = len;
					((List<Object>)o).add(z);
				} else ((List<Object>)o).set(idx, z);
			} else ((Map<Object, Object>)o).put(String.valueOf(k), z);
			o = z;
			k = m; x = y;
		}
	}
	
	@Override public String toString() {
		Object o = dig();
		if (o == UNDEFINED) return getPath() + ": UNDEFINED";
		else return getPath() + ": " + JSONWriter.toJSON(o);
	}
}
