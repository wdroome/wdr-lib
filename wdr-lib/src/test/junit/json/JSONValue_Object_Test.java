package test.junit.json;

import static org.junit.Assert.*;

import java.util.HashMap;

import org.junit.Test;

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
}
