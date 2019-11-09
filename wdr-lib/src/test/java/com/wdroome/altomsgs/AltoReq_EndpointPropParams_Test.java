package com.wdroome.altomsgs;

import static org.junit.Assert.*;

import org.junit.Test;

import com.wdroome.json.JSONException;
import com.wdroome.json.JSONFieldMissingException;
import com.wdroome.json.JSONValueTypeException;
import com.wdroome.altomsgs.*;

/**
 * @author wdr
 */
public class AltoReq_EndpointPropParams_Test extends CommonTestMethods
{

	/**
	 * Test method for {@link unused.misc.x_msgs.AltoReq_EndpointPropParams#AltoReq_EndpointPropParams()}.
	 * @throws JSONException 
	 */
	@Test
	public void testAltoReq_EndpointPropParams() throws JSONException
	{
		AltoReq_EndpointPropParams req0 = new AltoReq_EndpointPropParams();
		req0.addEndpoint("1.2.3.4");
		req0.addEndpoints(new String[] {"4.3.2.1", "192.0.0.1", "pid:pid1"});
		req0.addProperties(new String[] {"pid", "foo"});
		
		AltoReq_EndpointPropParams req1 = new AltoReq_EndpointPropParams(req0.getJSON());
		String actual = req1.toString();
		String expected =
			      "{\n"
				+ "\"endpoints\": [\"ipv4:1.2.3.4\", \"ipv4:4.3.2.1\", \"ipv4:192.0.0.1\", \"pid:pid1\"], \n"
				+ "\"properties\": [\"pid\", \"foo\"]\n"
				+ "}";
		assertEquals("JSON", expected, actual);
		assertEquals("getEndpoints", "String[ipv4:1.2.3.4,ipv4:4.3.2.1,ipv4:192.0.0.1,pid:pid1]",
						catArray(req1.getEndpoints()));
		assertEquals("getProperties", "String[pid,foo]", catArray(req1.getProperties()));
	}

	@Test
	public void testEndpointPropParams2() throws JSONException
	{
		try {
			new AltoReq_EndpointPropParams("{\"endpoints\":[]}");
			fail("c'tor succeeded");
		} catch (JSONFieldMissingException e) {
			assertEquals("missing field", "/properties", e.getField());
		}
		try {
			new AltoReq_EndpointPropParams("{\"properties\":[]}");
			fail("c'tor succeeded");
		} catch (JSONFieldMissingException e) {
			assertEquals("missing field", "/endpoints", e.getField());
		}
	}

	@Test
	public void testEndpointPropParams3() throws JSONException
	{
		try {
			AltoReq_EndpointPropParams req = new AltoReq_EndpointPropParams("{\"properties\":\"foobar\", \"endpoints\":[] }");
			req.getProperties();
			fail("ctor & getProperties succeeded");
		} catch (JSONValueTypeException e) {
			assertEquals("field type", "/properties", e.getField());
		}
	}

	public static void main(String[] args) throws JSONException
	{
		AltoReq_EndpointPropParams req0 = new AltoReq_EndpointPropParams();
		req0.addEndpoint("1.2.3.4");
		req0.addEndpoints(new String[] {"4.3.2.1", "192.0.0.1", "pid:pid1"});
		req0.addProperties(new String[] {"pid", "foo"});
		
		System.out.println(req0.toString());
		System.out.println("endpoints: " + catArray(req0.getEndpoints()));
	}
}
