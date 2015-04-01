package test.junit.altomsgs;

import static org.junit.Assert.*;

import org.junit.Test;

import com.wdroome.json.JSONException;
import com.wdroome.altomsgs.*;
import com.wdroome.json.JSONFieldMissingException;
import com.wdroome.json.JSONValue_Object;

/**
 * @author wdr
 */
public class AltoReq_FilteredCostMap_Test extends CommonTestMethods
{

	/**
	 * Test method for {@link unused.misc.x_msgs.AltoReq_FilteredCostMap#AltoReq_FilteredCostMap()}.
	 * @throws JSONException 
	 */
	@Test
	public void testFilteredCostMap1() throws JSONException
	{
		AltoReq_FilteredCostMap req0 = new AltoReq_FilteredCostMap();
		req0.addSource("PID1");
		req0.addSources(new String[] {"PID3", "PID2"});
		req0.addDestination("PID6");
		req0.addDestinations(new String[] {"PID7", "PID5"});
		req0.setCostMode("numerical");
		req0.setCostMetric("routingcost");
		
		AltoReq_FilteredCostMap req1 = new AltoReq_FilteredCostMap(req0.getJSON());
		String actual = req1.toString();
		String expected =
			  "{\n"
			+ "  \"cost-type\": {\n"
			+ "    \"cost-metric\": \"routingcost\",\n"
			+ "    \"cost-mode\": \"numerical\"\n"
			+ "  },\n"
			+ "  \"pids\": {\n"
			+ "    \"dsts\": [\n"
			+ "      \"PID6\",\n"
			+ "      \"PID7\",\n"
			+ "      \"PID5\"\n"
			+ "    ],\n"
			+ "    \"srcs\": [\n"
			+ "      \"PID1\",\n"
			+ "      \"PID3\",\n"
			+ "      \"PID2\"\n"
			+ "    ]\n"
			+ "  }\n"
			+ "}";
		assertEquals("JSON", expected.replaceAll("[ \n]",""), actual.replaceAll("[ \n]",""));
		assertEquals("getSources", "String[PID1,PID3,PID2]", catArray(req1.getSources()));
		assertEquals("getDestinations", "String[PID6,PID7,PID5]", catArray(req1.getDestinations()));
	}

	@Test
	public void testFilteredCostMap2() throws JSONException
	{
		AltoReq_FilteredCostMap req = new AltoReq_FilteredCostMap("{}");
		assertTrue(req.getCostMetric() == null);
	}

	@Test
	public void testFilteredCostMap3() throws JSONException
	{
		AltoReq_FilteredCostMap req = new AltoReq_FilteredCostMap("{\"cost-type\": {} }");
		assertTrue(req.getCostMetric() == null);
	}
	
	public static void main(String[] args) throws JSONException
	{
		AltoReq_FilteredCostMap req0 = new AltoReq_FilteredCostMap();
		req0.addSource("PID1");
		req0.addSources(new String[] {"PID3", "PID2"});
		req0.addDestination("PID6");
		req0.addDestinations(new String[] {"PID7", "PID5"});
		req0.setCostMode("numerical");
		req0.setCostMetric("routingcost");
		
		System.out.println(req0.toString());
		System.out.println("Sources: " + catArray(req0.getSources()));
		System.out.println("Destinations: " + catArray(req0.getDestinations()));
	}
}
