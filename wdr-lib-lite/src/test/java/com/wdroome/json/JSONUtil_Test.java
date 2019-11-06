package com.wdroome.json;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.math.BigInteger;

import com.wdroome.json.*;

/**
 * @author wdr
 *
 */
public class JSONUtil_Test
{

	@Test
	public void test1()
	{
		JSONValue jval = new JSONValue_String("abcd");
		assertEquals("  \"abcd\"", JSONUtil.toJSONString(jval, " ", "  "));
	}
	
	@Test
	public void test2()
	{
		JSONValue jval = new JSONValue_Array(new String[] {"abcd", "edfg"});
		assertEquals("  [\"abcd\", \"edfg\"]", JSONUtil.toJSONString(jval, " ", "  "));
	}
	
	@Test
	public void test3()
	{
		JSONValue jval = JSONUtil.toValue("abcd");
		assertEquals("  \"abcd\"", JSONUtil.toJSONString(jval, " ", "  "));
	}
	
	@Test
	public void test4()
	{
		ArrayList<Integer> src = new ArrayList<Integer>();
		for (int i = 0; i < 10; i++) {
			src.add(i);
		}
		JSONValue jval = JSONUtil.toValue(src);
		String expected =
				  "  [\n"
				+ "  0, 1, 2, 3, 4, \n"
				+ "  5, 6, 7, 8, 9\n"
				+ "  ]";
		assertEquals(expected, JSONUtil.toJSONString(jval, " ", "  "));
	}
	
	@Test
	public void test5()
	{
		HashMap<String, Integer> src = new HashMap<String, Integer>();
		for (int i = 0; i < 1; i++) {
			src.put("key" + i, i);
		}
		JSONValue jval = JSONUtil.toValue(src);
		String expected =
				  "  {\"key0\": 0}";
		assertEquals(expected, JSONUtil.toJSONString(jval, " ", "  "));
	}
	
	@Test
	public void test6()
	{
		HashMap<String, Integer> src = new HashMap<String, Integer>();
		for (int i = 0; i < 6; i++) {
			src.put("key" + i, i);
		}
		JSONValue jval = JSONUtil.toValue(src);
		String expected =
				    "  {\n"
				  + "  \"key0\": 0, \n"
				  + "  \"key1\": 1, \n"
				  + "  \"key2\": 2, \n"
				  + "  \"key3\": 3, \n"
				  + "  \"key4\": 4, \n"
				  + "  \"key5\": 5\n"
				  + "  }";
		// System.out.println(JSONUtil.toJSONString(jval, " ", "  "));
		assertEquals(expected, JSONUtil.toJSONString(jval, " ", "  "));
	}
	
	@Test
	public void test7()
	{
		HashMap<String, Integer> src = new HashMap<String, Integer>();
		StringBuffer expected = new StringBuffer();
		String prefix = "{";
		for (int i = 0; i < 10; i++) {
			src.put("key" + i, i);
			expected.append(prefix);
			prefix = ",";
			expected.append("\"key" + i + "\":" + i);
		}
		expected.append("}");
		
		JSONValue jval = JSONUtil.toValue(src);
		// System.out.println(expected);
		// System.out.println(JSONUtil.toJSONString(jval, true, false));
		assertEquals(expected.toString(), JSONUtil.toJSONString(jval, true, false));
	}
	
	@Test
	public void test8()
	{
		ArrayList<String> src = new ArrayList<String>();
		StringBuffer expected = new StringBuffer();
		String prefix = "[";
		for (int i = 0; i < 25; i++) {
			src.add("s" + i);
			expected.append(prefix);
			prefix = ",";
			expected.append("\"s" + i + "\"");
		}
		expected.append("]");
		
		JSONValue jval = JSONUtil.toValue(src);
		// System.out.println(expected);
		// System.out.println(JSONUtil.toJSONString(jval, true, false));
		assertEquals(expected.toString(), JSONUtil.toJSONString(jval, true, false));
	}
	
	@Test
	public void test9()
	{
		StringBuilder buff = new StringBuilder();
		for (int j = 0; j < 25; j++) {
			buff.append("9");
			JSONValue val = JSONUtil.toValue(new BigInteger(buff.toString()));
			assertTrue("value type " + buff.toString(), val instanceof JSONValue_BigInt);
			assertEquals("value cmp " + buff.toString(),
						new BigInteger(buff.toString()), ((JSONValue_BigInt)val).m_value);
		}
	}
}
