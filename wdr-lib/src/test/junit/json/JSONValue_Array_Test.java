package test.junit.json;

import static org.junit.Assert.*;

import org.junit.Test;

import com.wdroome.json.*;

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
}
