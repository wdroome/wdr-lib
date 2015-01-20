package test.junit.json;

import static org.junit.Assert.*;

import org.junit.Test;

import com.wdroome.json.*;

/**
 * @author wdr
 */
public class JSONValue_Number_Test
{

	@Test
	public void test() throws JSONParseException
	{
		check(1234, "1234");
		check(-1234, "-1234");
		check(-3.14159, "-3.14159");
		check(-3.0, "-3");
		check(1.234e20, "1.234E20");
		check(1.234e-20, "1.234E-20");
	}
	
	private void check(double src, String expectedJson)
				throws JSONParseException
	{
		JSONValue_Number jvalue = new JSONValue_Number(src);
		String json = jvalue.toString();
		assertEquals(src + "/JSON", expectedJson, json);
		
		JSONValue jvalue2 = new JSONParser().parse(new JSONLexan(json));
		if (!(jvalue2 instanceof JSONValue_Number)) {
			fail(src + "/PARSE: Not JSONValue_Number");
		}
		assertEquals(src + "/PARSE", src, ((JSONValue_Number)jvalue2).m_value, .0001);
	}
}
