package test.junit.altomsgs;

import java.util.Arrays;
import java.io.StringReader;

import com.wdroome.json.JSONException;
import com.wdroome.json.JSONParseException;
import com.wdroome.json.JSONValueTypeException;
import com.wdroome.json.JSONFieldMissingException;
import com.wdroome.json.JSONValue_Object;
import com.wdroome.altomsgs.AltoResp_CostMap;

import org.junit.*;

import static org.junit.Assert.*;

/**
 * @author wdr
 */
public class AltoResp_Cost_MapTest extends CommonTestMethods
{
	@Test
	public void testCostMap() throws JSONException
	{
		AltoResp_CostMap map0 = new AltoResp_CostMap();
		map0.setDependentVtag("map", "12349876");
		map0.setCostMode("numerical");
		map0.setCostMetric("routingcost");
		map0.setCost("PID1", "PID1", 1);
		map0.setCost("PID1", "PID2", 5);
		map0.setCost("PID1", "PID3", 10);
		map0.setCost("PID2", "PID1", 5);
		map0.setCost("PID2", "PID2", 1);
		map0.setCost("PID2", "PID3", 15);
		map0.setCost("PID3", "PID1", 20);
		map0.setCost("PID3", "PID2", 15);
		map0.setCost("PID3", "PID3", 1);
		String actual = map0.toString();
		
		String expectedCostIds =
			  "{\n"
			+ "  \"cost-map\": {\n"
			+ "    \"PID1\": {\n"
			+ "      \"PID1\": 1,\n"
			+ "      \"PID2\": 5,\n"
			+ "      \"PID3\": 10\n"
			+ "    },\n"
			+ "    \"PID2\": {\n"
			+ "      \"PID1\": 5,\n"
			+ "      \"PID2\": 1,\n"
			+ "      \"PID3\": 15\n"
			+ "    },\n"
			+ "    \"PID3\": {\n"
			+ "      \"PID1\": 20,\n"
			+ "      \"PID2\": 15,\n"
			+ "      \"PID3\": 1\n"
			+ "    }\n"
			+ "  },\n"
			+ "  \"meta\": {\n"
			+ "    \"cost-type\": {\n"
			+ "      \"cost-metric\": \"routingcost\",\n"
			+ "      \"cost-mode\": \"numerical\"\n"
			+ "    },\n"
			+ "    \"dependent-vtags\": [{\n"
			+ "      \"resource-id\": \"map\",\n"
			+ "      \"tag\": \"12349876\"\n"
			+ "    }]\n"
			+ "  }\n"
			+ "}";
				
		assertEquals("JSON", expectedCostIds.replaceAll("[ \n]",""), actual.replaceAll("[ \n]",""));
		
		AltoResp_CostMap map1 = new AltoResp_CostMap(actual);
		assertEquals("map-id", "map", map1.getDependentResourceId());
		assertEquals("map-vtag", "12349876", map1.getDependentTag());
		assertEquals("cost-mode", "numerical", map1.getCostMode());
		assertEquals("cost-type", "routingcost", map1.getCostMetric());
		assertEquals("src-PIDs", "String[PID1,PID2,PID3]",
				catArray(sort(map1.getSrcPIDs())));
		assertEquals("dest-PIDs", "String[PID1,PID2,PID3]",
				catArray(sort(map1.getDestPIDs("PID1"))));
		assertEquals("PID1=>PID1",  1.0, map1.getCost("PID1", "PID1"), .001);
		assertEquals("PID1=>PID2",  5.0, map1.getCost("PID1", "PID2"), .001);
		assertEquals("PID1=>PID3", 10.0, map1.getCost("PID1", "PID3"), .001);
		assertEquals("PID2=>PID1",  5.0, map1.getCost("PID2", "PID1"), .001);
		assertEquals("PID2=>PID2",  1.0, map1.getCost("PID2", "PID2"), .001);
		assertEquals("PID2=>PID3", 15.0, map1.getCost("PID2", "PID3"), .001);
		assertEquals("PID3=>PID1", 20.0, map1.getCost("PID3", "PID1"), .001);
		assertEquals("PID3=>PID2", 15.0, map1.getCost("PID3", "PID2"), .001);
		assertEquals("PID3=>PID3",  1.0, map1.getCost("PID3", "PID3"), .001);
		
		assertTrue("Def-cost", Double.isNaN(map1.getDefaultCost()));
	}

	@Test
	public void testCostMapDef() throws JSONException
	{
		AltoResp_CostMap map0 = new AltoResp_CostMap();
		map0.setDependentVtag("map", "12349876");
		map0.setCostMode("numerical");
		map0.setCostMetric("routingcost");
		map0.setCost("PID1", "PID1", 1);
		map0.setCost("PID1", "PID2", 5);
		map0.setCost("PID1", "PID3", 10);
		map0.setCost("PID2", "PID1", 5);
		map0.setCost("PID2", "PID2", 1);
		map0.setCost("PID2", "PID3", 15);
		map0.setCost("PID3", "PID1", 20);
		map0.setCost("PID3", "PID2", 15);
		map0.setCost("PID3", "PID3", 1);
		map0.setDefaultCost(42);
		String actual = map0.toString();
		
		String expectedCostIds =
				  "{\n"
				+ "  \"cost-map\": {\n"
				+ "    \"PID1\": {\n"
				+ "      \"PID1\": 1,\n"
				+ "      \"PID2\": 5,\n"
				+ "      \"PID3\": 10\n"
				+ "    },\n"
				+ "    \"PID2\": {\n"
				+ "      \"PID1\": 5,\n"
				+ "      \"PID2\": 1,\n"
				+ "      \"PID3\": 15\n"
				+ "    },\n"
				+ "    \"PID3\": {\n"
				+ "      \"PID1\": 20,\n"
				+ "      \"PID2\": 15,\n"
				+ "      \"PID3\": 1\n"
				+ "    }\n"
				+ "  },\n"
				+ "  \"meta\": {\n"
				+ "    \"cost-type\": {\n"
				+ "      \"cost-metric\": \"routingcost\",\n"
				+ "      \"cost-mode\": \"numerical\"\n"
				+ "    },\n"
				+ "    \"default-cost\": 42,\n"
				+ "    \"dependent-vtags\": [{\n"
				+ "      \"resource-id\": \"map\",\n"
				+ "      \"tag\": \"12349876\"\n"
				+ "    }]\n"
				+ "  }\n"
				+ "}";
		
		assertEquals("JSON", expectedCostIds.replaceAll("[ \n]",""), actual.replaceAll("[ \n]",""));
		
		AltoResp_CostMap map1 = new AltoResp_CostMap(actual);
		assertEquals("map-id", "map", map1.getDependentResourceId());
		assertEquals("map-vtag", "12349876", map1.getDependentTag());
		assertEquals("cost-mode", "numerical", map1.getCostMode());
		assertEquals("cost-type", "routingcost", map1.getCostMetric());
		assertEquals("src-PIDs", "String[PID1,PID2,PID3]",
				catArray(sort(map1.getSrcPIDs())));
		assertEquals("dest-PIDs", "String[PID1,PID2,PID3]",
				catArray(sort(map1.getDestPIDs("PID1"))));
		assertEquals("PID1=>PID1",  1.0, map1.getCost("PID1", "PID1"), .001);
		assertEquals("PID1=>PID2",  5.0, map1.getCost("PID1", "PID2"), .001);
		assertEquals("PID1=>PID3", 10.0, map1.getCost("PID1", "PID3"), .001);
		assertEquals("PID2=>PID1",  5.0, map1.getCost("PID2", "PID1"), .001);
		assertEquals("PID2=>PID2",  1.0, map1.getCost("PID2", "PID2"), .001);
		assertEquals("PID2=>PID3", 15.0, map1.getCost("PID2", "PID3"), .001);
		assertEquals("PID3=>PID1", 20.0, map1.getCost("PID3", "PID1"), .001);
		assertEquals("PID3=>PID2", 15.0, map1.getCost("PID3", "PID2"), .001);
		assertEquals("PID3=>PID3",  1.0, map1.getCost("PID3", "PID3"), .001);
		
		assertEquals("Def-cost", 42, map1.getDefaultCost(), 0.001);
	}
	
	@Test
	public void testCostMapReader() throws JSONException
	{
		AltoResp_CostMap map0 = new AltoResp_CostMap();
		map0.setDependentVtag("map", "12349876");
		map0.setCostMode("numerical");
		map0.setCostMetric("routingcost");
		map0.setCost("PID1", "PID1", 1);
		map0.setCost("PID1", "PID2", 5);
		map0.setCost("PID1", "PID3", 10);
		map0.setCost("PID2", "PID1", 5);
		map0.setCost("PID2", "PID2", 1);
		map0.setCost("PID2", "PID3", 15);
		map0.setCost("PID3", "PID1", 20);
		map0.setCost("PID3", "PID2", 15);
		map0.setCost("PID3", "PID3", 1);
		String actual = map0.toString();
		
		String expectedCostIds =
				  "{\n"
				+ "  \"cost-map\": {\n"
				+ "    \"PID1\": {\n"
				+ "      \"PID1\": 1,\n"
				+ "      \"PID2\": 5,\n"
				+ "      \"PID3\": 10\n"
				+ "    },\n"
				+ "    \"PID2\": {\n"
				+ "      \"PID1\": 5,\n"
				+ "      \"PID2\": 1,\n"
				+ "      \"PID3\": 15\n"
				+ "    },\n"
				+ "    \"PID3\": {\n"
				+ "      \"PID1\": 20,\n"
				+ "      \"PID2\": 15,\n"
				+ "      \"PID3\": 1\n"
				+ "    }\n"
				+ "  },\n"
				+ "  \"meta\": {\n"
				+ "    \"cost-type\": {\n"
				+ "      \"cost-metric\": \"routingcost\",\n"
				+ "      \"cost-mode\": \"numerical\"\n"
				+ "    },\n"
				+ "    \"dependent-vtags\": [{\n"
				+ "      \"resource-id\": \"map\",\n"
				+ "      \"tag\": \"12349876\"\n"
				+ "    }]\n"
				+ "  }\n"
				+ "}";
		
		assertEquals("JSON", expectedCostIds.replaceAll("[ \n]",""), actual.replaceAll("[ \n]",""));
		
		AltoResp_CostMap map1 = new AltoResp_CostMap(new StringReader(actual));
		assertEquals("map-id", "map", map1.getDependentResourceId());
		assertEquals("map-vtag", "12349876", map1.getDependentTag());
		assertEquals("cost-mode", "numerical", map1.getCostMode());
		assertEquals("cost-type", "routingcost", map1.getCostMetric());
		assertEquals("src-PIDs", "String[PID1,PID2,PID3]",
				catArray(sort(map1.getSrcPIDs())));
		assertEquals("dest-PIDs", "String[PID1,PID2,PID3]",
				catArray(sort(map1.getDestPIDs("PID1"))));
		assertEquals("PID1=>PID1",  1.0, map1.getCost("PID1", "PID1"), .001);
		assertEquals("PID1=>PID2",  5.0, map1.getCost("PID1", "PID2"), .001);
		assertEquals("PID1=>PID3", 10.0, map1.getCost("PID1", "PID3"), .001);
		assertEquals("PID2=>PID1",  5.0, map1.getCost("PID2", "PID1"), .001);
		assertEquals("PID2=>PID2",  1.0, map1.getCost("PID2", "PID2"), .001);
		assertEquals("PID2=>PID3", 15.0, map1.getCost("PID2", "PID3"), .001);
		assertEquals("PID3=>PID1", 20.0, map1.getCost("PID3", "PID1"), .001);
		assertEquals("PID3=>PID2", 15.0, map1.getCost("PID3", "PID2"), .001);
		assertEquals("PID3=>PID3",  1.0, map1.getCost("PID3", "PID3"), .001);
		
		assertTrue("Def-cost", Double.isNaN(map1.getDefaultCost()));
	}
	
	@Test(expected = JSONFieldMissingException.class)
	public void nullTest1() throws JSONException
	{
		AltoResp_CostMap map1 = getCostMap();
		assertEquals("PID1=>PID1",  1.0, map1.getCost("PID1", "PID1"), .001);
		assertEquals("PID1=>PID2",  5.0, map1.getCost("PID1", "PID2"), .001);
		map1.getCost("PID1", "PID3");
	}
	
	@Test(expected = JSONFieldMissingException.class)
	public void nullTest2() throws JSONException
	{
		AltoResp_CostMap map1 = getCostMap();
		map1.getCost("PID1", "PID4");
	}
	
	@Test(expected = JSONFieldMissingException.class)
	public void nullTest3() throws JSONException
	{
		AltoResp_CostMap map1 = getCostMap();
		map1.getCost("PID4", "PID1");
	}
	
	public static AltoResp_CostMap getCostMap()
			throws JSONParseException, JSONValueTypeException
	{
		String jsonSrc =
				  "{\n"
				+ "  \"cost-map\": {\n"
				+ "    \"PID1\": {\n"
				+ "      \"PID1\": 1,\n"
				+ "      \"PID2\": 5,\n"
				+ "      \"PID3\": null\n"
				+ "    },\n"
				+ "    \"PID2\": {\n"
				+ "      \"PID1\": 5,\n"
				+ "      \"PID2\": 1,\n"
				+ "      \"PID3\": null\n"
				+ "    },\n"
				+ "    \"PID3\": {\n"
				+ "      \"PID1\": 20,\n"
				+ "      \"PID2\": 15,\n"
				+ "      \"PID3\": 1\n"
				+ "    }\n"
				+ "  },\n"
				+ "  \"meta\": {\n"
				+ "    \"cost-type\": {\n"
				+ "      \"cost-metric\": \"routingcost\",\n"
				+ "      \"cost-mode\": \"numerical\"\n"
				+ "    },\n"
				+ "    \"dependent-vtags\": [{\n"
				+ "      \"resource-id\": \"map\",\n"
				+ "      \"tag\": \"12349876\"\n"
				+ "    }]\n"
				+ "  }\n"
				+ "}";
		return new AltoResp_CostMap(new StringReader(jsonSrc));
	}

	public static void main(String[] args) throws JSONException
	{
		AltoResp_CostMap map0 = new AltoResp_CostMap();
		map0.setDependentVtag("map", "12349876");
		map0.setCostMode("numerical");
		map0.setCostMetric("routingcost");
		map0.setCost("PID1", "PID1", 1);
		map0.setCost("PID1", "PID2", 5);
		map0.setCost("PID1", "PID3", 10);
		map0.setCost("PID2", "PID1", 5);
		map0.setCost("PID2", "PID2", 1);
		map0.setCost("PID2", "PID3", 15);
		map0.setCost("PID3", "PID1", 20);
		map0.setCost("PID3", "PID2", 15);
		map0.setCost("PID3", "PID3", 1);
		
		String x = map0.getJSON();
		System.out.println(map0.toString());
		AltoResp_CostMap map1 = new AltoResp_CostMap(new StringReader(x));
		System.out.println(map1.toString());
		System.out.println("cost-mode: " + map1.getCostMode());
		System.out.println("map-id: " + map1.getDependentResourceId());
		System.out.println("map-vtag: " + map1.getDependentTag());
		String[] keys = map1.getSrcPIDs();
		Arrays.sort(keys);
		System.out.println("Src PIDs: " + catArray(keys));
		for (String src:keys) {
			for (String dest:keys) {
				System.out.println("  " + src + "->" + dest + ": " + map1.getCost(src,dest));
			}
		}
		try {
			double v = map0.getCost("PID1", "PID4");
			System.out.println("OOPS: PID1=>PID4 returned " + v + "!");
		} catch (JSONException e) {
			System.out.println("PID1=>PID4: " + e.toString());
		}
		try {
			double v = map0.getCost("PID4", "PID1");
			System.out.println("OOPS: PID4=>PID1 returned " + v + "!");
		} catch (JSONException e) {
			System.out.println("PID4=>PID1: " + e.toString());
		}
	}
}
