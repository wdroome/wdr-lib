package com.wdroome.json;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.wdroome.util.ArrayToList;
import com.wdroome.json.*;

/**
 * @author wdr
 */
public class JSONValue_Object_Test
{
	
	@Test
	public void test1() throws JSONParseException, JSONValueTypeException, JSONFieldMissingException
	{
		int nkeys = 10;
		String pkey = "key";
		JSONValue_Object jobj = new JSONValue_Object();
		StringBuffer expected = new StringBuffer();
		String prefix = "{";
		for (int i = 0; i < nkeys; i++) {
			jobj.put(pkey + i, i);
			expected.append(prefix);
			prefix = ",";
			expected.append("\"" + pkey + i + "\":" + i);
		}
		expected.append("}");
		
		String json = JSONUtil.toJSONString(jobj, true, false);
		// System.out.println(expected);
		// System.out.println(json);
		assertEquals(expected.toString(), json);
		
		JSONValue_Object jobj2 = JSONParser.parseObject(new JSONLexan(json), true);
		assertEquals("size", nkeys, jobj2.size());
		for (int i = 0; i < nkeys; i++) {
			assertEquals("key" + i, i, jobj2.getNumber("key" + i), .001);
		}
	}
	
	@Test
	public void test2() throws JSONParseException, JSONValueTypeException, JSONFieldMissingException
	{
		int nkeys = 10;
		String pkey = "key";
		String pval = "value";
		JSONValue_Object jobj = new JSONValue_Object();
		StringBuffer expected = new StringBuffer();
		String prefix = "{";
		for (int i = 0; i < nkeys; i++) {
			jobj.put(pkey + i, pval + i);
			expected.append(prefix);
			prefix = ",";
			expected.append("\"" + pkey + i + "\":\"" + pval + i + "\"");
		}
		expected.append("}");
		
		String json = JSONUtil.toJSONString(jobj, true, false);
		// System.out.println(expected);
		// System.out.println(json);
		assertEquals(expected.toString(), json);
		
		JSONValue_Object jobj2 = JSONParser.parseObject(new JSONLexan(json), true);
		assertEquals("size", nkeys, jobj2.size());
		for (int i = 0; i < nkeys; i++) {
			assertEquals(pkey + i, pval + i, jobj2.getString("key" + i));
		}
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testPutPath() throws IOException
	{
		JSONValue_Object obj = new JSONValue_Object();
		List<String>[] paths;
		paths = new List[] {
			new ArrayToList<String>(new String[] {"key0"}),
			new ArrayToList<String>(new String[] {"key0a", "key1"}),
			new ArrayToList<String>(new String[] {"key0a", "key1a", "key2a"}),
			new ArrayToList<String>(new String[] {"key0a", "key1a", "key2b"}),
			new ArrayToList<String>(new String[] {"key0b", "key1b", "key2a"}),
			new ArrayToList<String>(new String[] {"key0b", "key1c"}),
			new ArrayToList<String>(new String[] {"key0c"}),
		};
		for (List<String> path: paths) {
			obj.putPath(path, new JSONValue_String(path.toString()));
		}
		for (List<String> path: paths) {
			JSONValue actual = obj.getPath(path, JSONValue_Null.NULL);
			assertEquals("path " + path, new JSONValue_String(path.toString()), actual);
		}

		if (false) {
			StringBuilder buff = new StringBuilder();
			JSONWriter writer = new JSONWriter(buff);
			writer.setSorted(true);
			writer.setIndented(true);
			obj.writeJSON(writer);
			System.out.println(buff.toString());
		}
		
		int n = countLeaves(obj);
		assertEquals("before removal count", paths.length, n);
		for (List<String> path: paths) {
			obj.removePath(path);
			assertEquals("count after removing " + path.toString(), --n, countLeaves(obj));
		}
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testMergePatch() throws IOException
	{
		JSONValue_Object patch = new JSONValue_Object();
		List<String>[] paths;
		paths = new List[] {
			new ArrayToList<String>(new String[] {"key0"}),
			new ArrayToList<String>(new String[] {"key0a", "key1"}),
			new ArrayToList<String>(new String[] {"key0a", "key1a", "key2a"}),
			new ArrayToList<String>(new String[] {"key0a", "key1a", "key2b"}),
			new ArrayToList<String>(new String[] {"key0b", "key1b", "key2a"}),
			new ArrayToList<String>(new String[] {"key0b", "key1c"}),
			new ArrayToList<String>(new String[] {"key0c"}),
		};
		for (List<String> path: paths) {
			patch.putPath(path, new JSONValue_String(path.toString()));
		}
		
		JSONValue_Object obj = new JSONValue_Object();
		obj.applyMergePatch(patch);
		
		for (List<String> path: paths) {
			JSONValue actual = obj.getPath(path, JSONValue_Null.NULL);
			assertEquals("string patch path " + path, new JSONValue_String(path.toString()), actual);
		}

		if (false) {
			printObject(obj);
		}
		
		patch = new JSONValue_Object();
		int n = 0;
		for (List<String> path: paths) {
			patch.putPath(path, new JSONValue_Number(n));
			n++;
		}

		obj.applyMergePatch(patch);
		
		n = 0;
		for (List<String> path: paths) {
			JSONValue actual = obj.getPath(path, JSONValue_Null.NULL);
			assertEquals("number patch path " + path, new JSONValue_Number(n), actual);
			n++;
		}

		patch = new JSONValue_Object();
		for (List<String> path: paths) {
			patch.putPath(path, JSONValue_Null.NULL);
		}
		obj.applyMergePatch(patch);
		assertEquals("delete all leaf count", 0, countLeaves(obj));
		
		HashSet<List<String>> objPaths = new HashSet<List<String>>();
		for (List<String> path: paths) {
			if (path.size() > 1) {
				objPaths.add(path.subList(0, path.size()-1));
			}
		}
		if (false) {
			System.out.println(objPaths);
			printObject(obj);
		}
		assertEquals("delete all object count", objPaths.size(), countObjects(obj));
	}
	
	/**
	 * Return the total number of non-object elements in an object tree.
	 */
	private int countLeaves(JSONValue_Object obj)
	{
		int n = 0;
		for (JSONValue value: obj.values()) {
			if (value instanceof JSONValue_Object) {
				n += countLeaves((JSONValue_Object)value);
			} else {
				n++;
			}
		}
		return n;
	}
	
	/**
	 * Return the total number of object elements in an object tree,
	 * not counting "obj".
	 */
	private int countObjects(JSONValue_Object obj)
	{
		int n = 0;
		for (JSONValue value: obj.values()) {
			if (value instanceof JSONValue_Object) {
				n += 1 + countObjects((JSONValue_Object)value);
			}
		}
		return n;
	}
	
	private void printObject(JSONValue_Object obj) throws IOException
	{
		StringBuilder buff = new StringBuilder();
		JSONWriter writer = new JSONWriter(buff);
		writer.setSorted(true);
		writer.setIndented(true);
		obj.writeJSON(writer);
		System.out.println(buff.toString());
	}
	
	@Test
	public void testFindInvalidKey()
	{
		List<String> validNames = List.of("F1", "F23");
		List<String> validRegexes = List.of("#.*");
		List<String> validRegexNames = List.of("##1", "#");
		List<String> badNames = List.of("badname1", "badname2", "bad##name3");

		JSONValue_Object obj = new JSONValue_Object();
		for (String key:validNames) {
			obj.put(key, key+"-value");
		}
		for (String key:validRegexNames) {
			obj.put(key, key+"-value");
		}
		assertTrue("Test good names", obj.findInvalidKeys(validNames, validRegexes) == null);
		
		List<String> badNameResults = obj.findInvalidKeys(null, validRegexes);
		assertTrue("Test no simple names", badNameResults.containsAll(validNames)
									&& validNames.containsAll(badNameResults));
		
		badNameResults = obj.findInvalidKeys(validNames, null);
		assertTrue("Test no regex", badNameResults.containsAll(validRegexNames)
				&& validRegexNames.containsAll(badNameResults));
		
		for (String key: badNames) {
			obj.put(key, key+"-value");
		}
		badNameResults = obj.findInvalidKeys(validNames, validRegexes);
		assertEquals("Test bad names", badNameResults, badNames);
		
		obj = new JSONValue_Object();
		for (String key:validRegexNames) {
			obj.put(key, key+"-value");
		}
		badNameResults = obj.findInvalidKeys(validNames, validRegexes);
		assertTrue("Test good regex", obj.findInvalidKeys(validNames, validRegexes) == null);
	}
	
	@Test
	public void testRestrictTypes()
	{
		Map<String, JSONValue> stringSrc = Map.of(
				"str1", new JSONValue_String("str1-value"),
				"str2", new JSONValue_String("str2-value"));
		Map<String, JSONValue> numberSrc = Map.of(
				"num1", new JSONValue_Number(100),
				"num2", new JSONValue_Number(101));
		Map<String, JSONValue> booleanSrc = Map.of(
				"bool1", new JSONValue_Boolean(true),
				"bool2", new JSONValue_Boolean(false));
		Map<String, JSONValue> arraySrc = Map.of(
				"arr1", new JSONValue_Array(),
				"arr2", new JSONValue_Array());
		Map<String, JSONValue> objectSrc = Map.of(
				"obj1", new JSONValue_Object(),
				"obj2", new JSONValue_Object());
		
		JSONValue_Object stringObj = new JSONValue_Object(stringSrc);
		JSONValue_Object numberObj = new JSONValue_Object(numberSrc);
		JSONValue_Object booleanObj = new JSONValue_Object(booleanSrc);
		JSONValue_Object arrayObj = new JSONValue_Object(arraySrc);
		JSONValue_Object objectObj = new JSONValue_Object(objectSrc);
		
		JSONValue_Object obj = makeObj(stringObj, numberObj, booleanObj, arrayObj, objectObj);
		assertEquals("obj size", obj.size(), 10);
		
		JSONValue_Object testObj;
		
		testObj = obj.restrictValueTypes(List.of(JSONValue_String.class, JSONValue_Number.class));
		assertEquals("String & Number", testObj, makeObj(stringObj, numberObj));
		assertEquals("String & Number size", testObj.size(), 4);
		
		testObj = obj.restrictValueTypes(List.of(JSONValue_Boolean.class));
		assertEquals("Boolean", testObj, makeObj(booleanObj));
		assertNotEquals("Boolean2", testObj, makeObj(booleanObj, stringObj));

		testObj = obj.restrictValueTypes(List.of(JSONValue_Array.class, JSONValue_Object.class));
		assertEquals("Array & Object", testObj, makeObj(objectObj, arrayObj));
		assertNotEquals("Array & Object2", testObj, makeObj(objectObj, arrayObj, numberObj));
	}
	
	private static JSONValue_Object makeObj(JSONValue_Object... srcObjs)
	{
		JSONValue_Object obj = new JSONValue_Object();
		for (Map<String, ? extends JSONValue> srcMap: srcObjs) {
			obj.putAll(srcMap);
		}
		return obj;
	}
	
	@Test
	public void testCvtArray()
	{
		JSONValue_Object obj = new JSONValue_Object();
		obj.put("key1", new JSONValue_String("key1-value"));
		JSONValue_Array key2Arr = new JSONValue_Array(List.of(new JSONValue_String("key2-value")));
		obj.put("key2", key2Arr);
		assertEquals("no cvt 1", obj.getArray("key1", null), null);
		assertEquals("no cvt 2", obj.getArray("key2", null), key2Arr);
		
		obj.setAutoCvt2Array(true);
		assertEquals("cvt 1", obj.getArray("key1", null),
				new JSONValue_Array(List.of(new JSONValue_String("key1-value"))));
		assertEquals("cvt 2", obj.getArray("key2", null), key2Arr);
	}
}
