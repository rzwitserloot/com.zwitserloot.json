package com.zwitserloot.json;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.Test;

public class JSONTest{
	@Test
	public void testStrings() {
		assertEquals("5", JSON.parse("[5]").get(0).asString());
		assertEquals("true", JSON.parse("[true]").get(0).asString());
		assertNull(JSON.parse("[null]").get(0).asString());
		
		try {
			JSON.parse("[1, 2]").asString();
			fail("JSONException expected");
		} catch ( JSONException expected ) {}
		
		try {
			JSON.parse("{\"foo\": 1}").asString();
			fail("JSONException expected");
		} catch ( JSONException expected ) {}
		
		assertEquals("foo", JSON.parse("[null]").get(0).asString("foo"));
	}
	
	@Test
	public void testBooleans() {
		assertTrue(JSON.parse("[\"yes\"]").get(0).asBoolean());
		assertFalse(JSON.parse("[\"off\"]").get(0).asBoolean());
		assertFalse(JSON.parse("[\"FALSE\"]").get(0).asBoolean());
		assertFalse(JSON.parse("[false]").get(0).asBoolean());
		assertTrue(JSON.parse("[true]").get(0).asBoolean());
		
		try {
			JSON.parse("[foo]").get(0).asBoolean();
			fail("'foo' correctly parses as a boolean; it shouldn't.");
		} catch (JSONException expected) {}
	}
	
	@Test
	public void testNulls() {
		assertTrue(JSON.parse("[null]").get(0).exists());
		assertTrue(JSON.parse("[null]").get(0).isNull());
		assertFalse(JSON.parse("[1]").get(0).isNull());
		assertFalse(JSON.parse("[false]").get(0).isNull());
	}
	
	@Test
	public void testNumbers() {
		assertEquals(10, JSON.parse("[1e1]").get(0).asInt());
		assertEquals(10.0, JSON.parse("[10]").get(0).asDouble(), .000001);
		assertEquals(10, JSON.parse("[10.0]").get(0).asInt());
		assertEquals(10.0, JSON.parse("[\"1e1\"]").get(0).asDouble(), .000001);
		assertEquals(10, JSON.parse("[\"10\"]").get(0).asInt());
		
		//The following number is not representable as a double.
		//When wrapped in a string, toLong() should still handle it correctly.
		assertEquals(1152921504606846977L, JSON.parse("[\"1152921504606846977\"]").get(0).asLong());
		
		try {
			JSON.parse("[1.2]").get(0).asInt();
			fail("'1.2' is still parsable as an int.");
		} catch (JSONException expected) {}
		
		try {
			JSON.parse("[1.2]").get(0).asLong();
			fail("'1.2' is still parsable as a long.");
		} catch (JSONException expected) {}
		
		try {
			JSON.parse("[\"1.2\"]").get(0).asInt();
			fail("'\"1.2\"' is still parsable as an int.");
		} catch (JSONException expected) {}
		
		try {
			JSON.parse("[\"1.2\"]").get(0).asLong();
			fail("'\"1.2\"' is still parsable as a long.");
		} catch (JSONException expected) {}
		
		try {
			JSON.parse("[\"foo\"]").get(0).asDouble();
			fail("'foo' is still parsable as a double - probably throwing wrong exception.");
		} catch (JSONException expected) {
		} catch (Exception e) {
			fail("parsing a string for numbers should throw a JSONException when the string is not parsable, not a: " + e);
		}
		
		assertEquals(5, JSON.parse("[1e-1]").get(0).asInt(5));
		assertEquals(0.1, JSON.parse("[1e-1]").get(0).asDouble(2.0), .000001);
	}
	
	@Test(expected=JSONException.class)
	public void testJSONParser1() {
		JSON.parse("del window");
	}
	
	@Test(expected=JSONException.class)
	public void testJSONParser2() {
		JSON.parse("a()");
	}
	
	@Test(expected=JSONException.class)
	public void testJSONParser3() {
		JSON.parse("a=10");
	}
	
	@Test(expected=JSONException.class)
	public void testJSONParser4() {
		JSON.parse("flu]]te");
	}
	
	@Test(expected=JSONException.class)
	public void testJSONParser5() {
		JSON.parse("[{]}");
	}
	
	@Test
	public void testUtilityMethods() {
		JSON json = JSON.parse("{\"foo\": [1, 2]}");
		assertEquals("~", json.getPath());
		assertEquals("~/foo", json.get("foo").getPath());
		assertEquals("~/bar", json.get("bar").getPath());
		assertFalse(json.get("bar").exists());
		assertEquals("~/foo/0", json.get("foo").get(0).getPath());
		assertEquals(1, json.get("foo").get(0).asInt());
		assertTrue(json.get("foo").get(1).exists());
		assertFalse(json.get("foo").get(2).exists());
		assertFalse(json.get("foo").get("foo").exists());
		assertTrue(json.get("foo").get(0).exists());
	}
	
	@Test
	public void testLists() {
		JSON json = JSON.parse("[1, 2, [\"foo\", true], null, [[], [\"foo\"], [[[], [], [[]]]]]]");
		assertEquals(2, json.get(1).asInt());
		assertTrue(json.get(2).get(1).asBoolean());
		assertEquals(0, json.get(4).get(0).asList().size());
		
		List<JSON> list = json.get(4).get(1).asList();
		assertEquals(1, list.size());
		assertEquals("foo", list.get(0).asString());
		assertEquals("~/4/1/0", list.get(0).getPath());
		assertFalse(list.isEmpty());
		Iterator<JSON> iterator = list.iterator();
		assertTrue(iterator.hasNext());
		assertEquals("foo", iterator.next().asString());
		assertFalse(iterator.hasNext());
		try {
			iterator.next();
			fail("JSON.asList().iterator() does not throw an exception when there are no more elements.");
		} catch ( NoSuchElementException expected ) {
		} catch ( Exception e ) {
			fail("JSON.asList().iterator() throws the wrong exception when there are no more elements: " + e);
		}
		
		list = json.get(4).get(2).asList().get(0).asList();
		assertEquals(3, list.size());
		assertEquals(1, list.get(2).asList().size());
		assertEquals(0, list.get(1).asList().size());
		assertEquals(0, list.get(2).get(0).asList().size());
		
		list = json.asList().subList(2, 4);
		assertEquals(2, list.size());
		assertEquals("foo", list.get(0).get(0).asString());
		assertTrue(list.get(1).isNull());
		assertEquals("~/2/1", list.get(0).asList().get(1).getPath());
		
		assertEquals(10, JSON.parse("[10]").get(0).asList().get(0).asInt());
		assertEquals(0, JSON.parse("[null]").get(0).asList().size());
		
		assertEquals(2, JSON.parse("{\"foo\": 1, \"bar\": 2}").asList().get(0).get("bar").asInt());
		assertEquals(1, JSON.parse("{\"foo\": 1, \"bar\": 2}").asList().size());
	}
	
	@Test
	public void testMaps() {
		try {
			JSON.parse("{\"a\": {\"foo\": 1}, \"b\": [1, 2], \"c\": {\"e\": 3, {\"bar\": 2}}, \"d\": 4}");
			fail("Faulty json with a keyless entry in an inner map was accepted");
		} catch ( JSONException expected ) {}
		
		JSON json = JSON.parse("{a: {foo: 1}, b: [1, 2], c: {e: 3, f: {bar: 2}}, d: 4}");
		
		assertEquals(1, json.get("a").get("foo").asInt());
		
		try {
			json.get("d").keySet();
			fail("keySet() on a non-Map entry did not trigger an exception");
		} catch ( JSONException expected ) {}
		
		Set<String> keys = json.keySet();
		assertEquals(4, keys.size());
		assertTrue(keys.contains("a"));
		assertFalse(keys.contains("A"));
		assertTrue(keys.contains("b"));
		assertTrue(keys.contains("c"));
		assertTrue(keys.contains("d"));
		assertFalse(keys.contains("e"));
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testAsObject() {
		JSON json = JSON.parse("[{\"a\": 1.2345e2, \"b\": [true, false, null]}, 10," +
				"[[]], {\"d\": {}, \"e\": \"foo\", \"f\": [[[[[], []]]]]}]");
		
		assertEquals("testing", json.get("foobar").asObject("testing"));
		
		List<?> list = (List<?>) json.asObject();
		assertEquals(4, list.size());
		assertEquals(10.0, ((Number)list.get(1)).doubleValue(), .000001);
		
		List<?> l2 = (List<?>) list.get(2);
		assertEquals(1, l2.size());
		l2 = (List<?>)l2.get(0);
		assertTrue(l2.isEmpty());
		
		Map<String, ?> map = (Map<String, ?>) list.get(0);
		assertEquals(2, map.size());
		assertEquals(123.45, map.get("a"));
		assertFalse(map.containsKey("d"));
		l2 = (List<?>) map.get("b");
		assertEquals(3, l2.size());
		assertTrue((Boolean) l2.get(0));
		assertFalse((Boolean) l2.get(1));
		assertNull(l2.get(2));
		
		map = (Map<String, ?>) list.get(3);
		Map<String, ?> m2 = (Map<String, ?>) map.get("d");
		assertTrue(m2.isEmpty());
		assertEquals("foo", map.get("e"));
		
		l2 = (List<?>) map.get("f");
		for (int i = 0; i < 3; i++) {
			assertEquals(1, l2.size());
			l2 = (List<?>) l2.get(0);
		}
		assertEquals(2, l2.size());
		assertEquals(0, ((List<?>) l2.get(0)).size());
		assertEquals(0, ((List<?>) l2.get(1)).size());
	}
	
	@Test
	public void testExpansionOnSet() {
		JSON json = JSON.newMap();
		json.get("foo").get("bar").get(0).get(0).setInt(10);
		assertEquals("{\"foo\":{\"bar\":[[10]]}}", json.toJSON());
		
		try {
			JSON.newMap().add().setInt(10);
			fail("Attempt to add an array entry to a map did not cause a JSONException");
		} catch ( JSONException expected ) {}
		
		json = JSON.newList();
		JSON i1 = json.add();
		assertEquals("~/0", i1.getPath());
		i1.get(0).setInt(1);
		assertEquals("[[1]]", json.toJSON());
		i1.get(1).setInt(2);
		assertEquals("[[1,2]]", json.toJSON());
		JSON i2 = json.add();
		assertEquals("~/1", i2.getPath());
		i2.get(0).setInt(3);
		assertEquals("[[1,2],[3]]", json.toJSON());
		i2.get(1).setInt(4);
		assertEquals("[[1,2],[3,4]]", json.toJSON());
	}
	
	public enum Colour { RED, GREEN, BLUE; }
	
	public void testSetAndGetEnum() {
		JSON json = JSON.newList();
		json.add().setEnum(Colour.GREEN);
		assertEquals(Colour.GREEN, json.get(0).asEnum(Colour.class));
		assertEquals("[\"GREEN\"]", json.toJSON());
	}
	
	@Test
	public void testSetMethods() {
		JSON json = JSON.newList();
		json.add().setNull();
		json.add().setBoolean(true);
		json.add().setBoolean(false);
		json.add().setDouble(1.23);
		json.add().setInt(12);
		json.add().setChar('a');
		json.add().setString("foobar");
		assertEquals(7, json.asList().size());
		
		int ct = 0;
		assertTrue(json.get(ct++).isNull());
		assertTrue(json.get(ct++).asBoolean());
		assertFalse(json.get(ct++).asBoolean());
		assertEquals(1.23, json.get(ct++).asDouble(), .000001);
		assertEquals(12, json.get(ct++).asInt());
		assertEquals('a', json.get(ct++).asChar());
		assertEquals("foobar", json.get(ct++).asString());
	}
	
	@Test
	public void testLongs() {
		JSON json = JSON.newList();
		json.add().setLong((1L << 55) +2);
		json.add().setLong((1L << 50) + 5);
		json.add().setDouble((1L << 50) + 0.5);
		assertEquals(String.class, json.get(0).asObject().getClass());
		assertEquals(Double.class, json.get(1).asObject().getClass());
		assertEquals((1L << 55) + 2, json.get(0).asLong());
		assertEquals((1L << 50) + 5, json.get(1).asLong());
		
		try {
			json.get(2).asLong();
			fail("A double value (with fraction) was readable as a long");
		} catch ( JSONException expected ) {}
	}
	
	@Test
	public void testSetWithJSON() {
		JSON jsonInner = JSON.newList();
		jsonInner.get(0).get("foo").setInt(5);
		jsonInner.get(1).get("bar").setInt(10);
		JSON jsonOuter = JSON.newMap();
		jsonOuter.get("baz").setWithJSON(jsonInner);
		assertEquals("{\"baz\":[{\"foo\":5},{\"bar\":10}]}", jsonOuter.toJSON());
	}
}
