package com.wdroome.json;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.wdroome.json.*;
import com.wdroome.util.ArrayToList;

/**
 * @author wdr
 */
public class JSONValue_Array_Test
{
	
	@Test
	public void test1() throws JSONParseException, JSONValueTypeException, JSONFieldMissingException
	{
		int nvals = 10;
		JSONValue_Array jarr = new JSONValue_Array();
		StringBuffer expected = new StringBuffer();
		String prefix = "[";
		for (int i = 0; i < nvals; i++) {
			jarr.add(i);
			expected.append(prefix);
			prefix = ",";
			expected.append(i);
		}
		expected.append("]");
		
		String json = JSONUtil.toJSONString(jarr, true, false);
		// System.out.println(expected);
		// System.out.println(json);
		assertEquals(expected.toString(), json);
		
		JSONValue jobj2 = JSONParser.parse(new JSONLexan(json), true);
		if (!(jobj2 instanceof JSONValue_Array)) {
			fail("Parser did not return a JSON Array");
		}
		JSONValue_Array jarr2 = (JSONValue_Array)jobj2;
		assertEquals("size", nvals, jarr2.size());
		for (int i = 0; i < nvals; i++) {
			assertEquals("[" + i + "]", i, jarr2.getNumber(i,-1), .001);
		}
	}
	
	@Test
	public void test2() throws JSONParseException, JSONValueTypeException, JSONFieldMissingException
	{
		int nvals = 10;
		String pval = "value";
		JSONValue_Array jarr = new JSONValue_Array();
		StringBuffer expected = new StringBuffer();
		String prefix = "[";
		for (int i = 0; i < nvals; i++) {
			jarr.add(pval + i);
			expected.append(prefix);
			prefix = ",";
			expected.append("\"" + pval + i + "\"");
		}
		expected.append("]");
		
		String json = JSONUtil.toJSONString(jarr, true, false);
		// System.out.println(expected);
		// System.out.println(json);
		assertEquals(expected.toString(), json);
		
		JSONValue jobj2 = JSONParser.parse(new JSONLexan(json), true);
		if (!(jobj2 instanceof JSONValue_Array)) {
			fail("Parser did not return a JSON Array");
		}
		JSONValue_Array jarr2 = (JSONValue_Array)jobj2;
		assertEquals("size", nvals, jarr2.size());
		for (int i = 0; i < nvals; i++) {
			assertEquals("[" + i + "]", pval + i, jarr2.getString(i));
		}
	}
	
	@Test
	public void test3() throws JSONParseException, JSONValueTypeException, JSONFieldMissingException
	{
		JSONValue_Array jarr = new JSONValue_Array();
		jarr.add("hi");
		jarr.add(1);
		jarr.add(new JSONValue_BigInt(3));
		jarr.add(new JSONValue_Object());
		jarr.add("last");
		
		JSONValue_StringArray sarr1 = new JSONValue_StringArray(jarr, true);
		sarr1.add("end");
		// System.out.println("XXX: " + sarr1.toString());
		assertEquals("size/cvt", 5, sarr1.size());
		assertEquals("get(0)/cvt", "hi", sarr1.get(0));
		assertEquals("get(1)/cvt", "1.0", sarr1.get(1));
		assertEquals("get(2)/cvt", "3", sarr1.get(2));
		assertEquals("get(3)/cvt", "last", sarr1.get(3));
		assertEquals("get(4)/cvt", "end", sarr1.get(4));
		assertEquals("eq1/cvt", "[\"hi\",\"1.0\",\"3\",\"last\",\"end\"]", sarr1.toString());
		sarr1.set(0, "hello");
		assertEquals("eq2/cvt", "[\"hello\",\"1.0\",\"3\",\"last\",\"end\"]", sarr1.toString());
		sarr1.remove(1);
		assertEquals("eq3/cvt", "[\"hello\",\"3\",\"last\",\"end\"]", sarr1.toString());
		
		List<String> iterRes = new ArrayList<>();
		for (String s: sarr1) {
			iterRes.add(s);
		}
		assertEquals("size/2", 4, iterRes.size());
		List<String> expected = new ArrayToList<>(new String[] {"hello","3","last","end"});
		assertEquals("eq3/cvt", expected, iterRes);
	}
	
	@Test
	public void test4() throws JSONParseException, JSONValueTypeException, JSONFieldMissingException
	{
		JSONValue_Array jarr = new JSONValue_Array();
		jarr.add("hi");
		jarr.add(1);
		jarr.add(new JSONValue_BigInt(3));
		jarr.add(new JSONValue_Object());
		jarr.add("last");
		
		JSONValue_StringArray sarr1 = new JSONValue_StringArray(jarr, false);
		sarr1.add("end");
		// System.out.println("XXX: " + sarr1.toString());
		assertEquals("size/no-cvt", 3, sarr1.size());
		assertEquals("get(0)/no-cvt", "hi", sarr1.get(0));
		assertEquals("get(1)/no-cvt", "last", sarr1.get(1));
		assertEquals("get(2)/no-cvt", "end", sarr1.get(2));
		assertEquals("eq1/no-cvt", "[\"hi\",\"last\",\"end\"]", sarr1.toString());
		sarr1.set(0, "hello");
		assertEquals("eq2/no-cvt", "[\"hello\",\"last\",\"end\"]", sarr1.toString());
		sarr1.remove(1);
		assertEquals("eq3/no-cvt", "[\"hello\",\"end\"]", sarr1.toString());
		
		List<String> iterRes = new ArrayList<>();
		for (String s: sarr1) {
			iterRes.add(s);
		}
		assertEquals("size/2", 2, iterRes.size());
		List<String> expected = new ArrayToList<>(new String[] {"hello","end"});
		assertEquals("eq3/no-cvt", expected, iterRes);
	}
	
	@Test
	public void test5() throws JSONParseException, JSONValueTypeException, JSONFieldMissingException
	{
		JSONValue_Array jarr = new JSONValue_Array();
		jarr.add("0");
		jarr.add("0x");
		jarr.add(1);
		jarr.add(new JSONValue_BigInt(3));
		jarr.add(new JSONValue_Object());
		jarr.add("last");
		
		JSONValue_DoubleArray darr1 = new JSONValue_DoubleArray(jarr, true);
		darr1.add(42);
		assertEquals("size/cvt", 4, darr1.size());
		assertEquals("get(0)/cvt", 0, darr1.get(0), .001);
		assertEquals("get(1)/cvt", 1, darr1.get(1), .001);
		assertEquals("get(2)/cvt", 3, darr1.get(2), .001);
		assertEquals("get(3)/cvt", 42, darr1.get(3), .001);
		assertEquals("eq1/cvt", "[0,1,3,42]", darr1.toString());
		darr1.set(1, 1.5);
		assertEquals("eq2/cvt", "[0,1.5,3,42]", darr1.toString());
		darr1.remove(1);
		assertEquals("eq3/cvt", "[0,3,42]", darr1.toString());
	}
	
	@Test
	public void tes6() throws JSONParseException, JSONValueTypeException, JSONFieldMissingException
	{
		JSONValue_Array jarr = new JSONValue_Array();
		jarr.add("0");
		jarr.add("0x");
		jarr.add(1);
		jarr.add(new JSONValue_BigInt(3));
		jarr.add(new JSONValue_Object());
		jarr.add("last");
		
		JSONValue_DoubleArray darr1 = new JSONValue_DoubleArray(jarr, false);
		darr1.add(42);
		assertEquals("size", 2, darr1.size());
		assertEquals("get(0)", 1, darr1.get(0), .001);
		assertEquals("get(1)", 42, darr1.get(1), .001);
		assertEquals("eq1", "[1,42]", darr1.toString());
		darr1.set(1, 1.5);
		assertEquals("eq2", "[1,1.5]", darr1.toString());
		darr1.remove(1);
		assertEquals("eq3", "[1]", darr1.toString());
	}
	
	
	@Test
	public void testRestrictTypes()
	{
		List<JSONValue> stringSrc = List.of(
				new JSONValue_String("str1-value"),
				new JSONValue_String("str2-value"));
		List<JSONValue> numberSrc = List.of(
				new JSONValue_Number(100),
				new JSONValue_Number(101));
		List<JSONValue> booleanSrc = List.of(
				new JSONValue_Boolean(true),
				new JSONValue_Boolean(false));
		List<JSONValue> arraySrc = List.of(
				new JSONValue_Array(),
				new JSONValue_Array());
		List<JSONValue> objectSrc = List.of(
				new JSONValue_Array(),
				new JSONValue_Array());
		
		JSONValue_Array stringArr = new JSONValue_Array(stringSrc);
		JSONValue_Array numberArr = new JSONValue_Array(numberSrc);
		JSONValue_Array booleanArr = new JSONValue_Array(booleanSrc);
		JSONValue_Array arrayArr = new JSONValue_Array(arraySrc);
		JSONValue_Array objectArr = new JSONValue_Array(objectSrc);
		
		JSONValue_Array arr = makeArr(stringArr, numberArr, booleanArr, arrayArr, objectArr);
		assertEquals("arr size", arr.size(), 10);
		
		JSONValue_Array testArr;
		
		testArr = arr.restrictValueTypes(List.of(JSONValue_String.class, JSONValue_Number.class));
		assertEquals("String & Number", testArr, makeArr(stringArr, numberArr));
		assertEquals("String & Number size", testArr.size(), 4);
		
		testArr = arr.restrictValueTypes(List.of(JSONValue_Boolean.class));
		assertEquals("Boolean", testArr, makeArr(booleanArr));
		assertNotEquals("Boolean2", testArr, makeArr(booleanArr, stringArr));

		testArr = arr.restrictValueTypes(List.of(JSONValue_Array.class, JSONValue_Array.class));
		assertEquals("Array & Arrect", testArr, makeArr(objectArr, arrayArr));
		assertNotEquals("Array & Arrect2", testArr, makeArr(objectArr, arrayArr, numberArr));
	}
	
	private static JSONValue_Array makeArr(JSONValue_Array... srcArrays)
	{
		JSONValue_Array arr = new JSONValue_Array();
		for (JSONValue_Array srcArray: srcArrays) {
			arr.addAll(srcArray);
		}
		return arr;
	}
	
	@Test
	public void testCvtArray()
	{
		JSONValue_Array arr = new JSONValue_Array();
		arr.add(new JSONValue_String("key1-value"));
		JSONValue_Array arr2 = new JSONValue_Array(List.of(new JSONValue_String("key2-value")));
		arr.add(arr2);
		assertEquals("no cvt 1", arr.getArray(0), null);
		assertEquals("no cvt 2", arr.getArray(1), arr2);
		
		arr.setAutoCvt2Array(true);
		assertEquals("cvt 1", arr.getArray(0),
				new JSONValue_Array(List.of(new JSONValue_String("key1-value"))));
		assertEquals("cvt 2", arr.getArray(1), arr2);
	}
}
