package com.wdroome.json;

import static org.junit.Assert.*;

import org.junit.Test;

import com.wdroome.json.*;

/**
 * @author wdr
 */
public class JSONValue_String_Test
{

	@Test
	public void test() throws JSONParseException
	{
		check("simple", "hello there", "\"hello there\"");
		check("esc1", "hello \"there\"", "\"hello \\\"there\\\"\"");
		check("esc1b", "hello 'there'", "\"hello 'there'\"");
		check("esc2", "\t \b \n \r \f \\ /", "\"\\t \\b \\n \\r \\f \\\\ /\"");
		check("esc3", "\u1234 \u8765 \u9abc \u0fed", "\"\\u1234 \\u8765 \\u9abc \\u0fed\"");
		check2("rev1", "\"\\/\"", "/");
		check2("rev2", "\"a\\/b\"", "a/b");
	}
	
	private void check(String desc, String src, String expectedJson)
				throws JSONParseException
	{
		JSONValue_String jvalue = new JSONValue_String(src);
		String json = jvalue.toString();
		assertEquals(desc + "/JSON", expectedJson, json);
		
		JSONValue jvalue2 = new JSONParser().parse(new JSONLexan(json));
		if (!(jvalue2 instanceof JSONValue_String)) {
			fail(desc + "/PARSE: Not JSONValue_String");
		}
		assertEquals(desc + "/PARSE", src, ((JSONValue_String)jvalue2).m_value);
	}
	
	private void check2(String desc, String json, String expectedStr)
				throws JSONParseException
	{
		JSONValue jvalue = new JSONParser().parse(new JSONLexan(json));
		if (!(jvalue instanceof JSONValue_String)) {
			fail(desc + "/PARSE: Not JSONValue_String");
		}
		assertEquals(desc, expectedStr, ((JSONValue_String)jvalue).m_value);
	}
}
