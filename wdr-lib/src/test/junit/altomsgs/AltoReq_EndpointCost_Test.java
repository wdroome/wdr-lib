package test.junit.altomsgs;

import java.util.Arrays;

import com.wdroome.json.JSONException;
import com.wdroome.altomsgs.*;

import org.junit.*;

import static org.junit.Assert.*;

/**
 * @author wdr
 */
public class AltoReq_EndpointCost_Test extends CommonTestMethods
{
	@Test
	public void testCostMap() throws JSONException
	{
		AltoResp_EndpointCost map0 = new AltoResp_EndpointCost();
		// map0.setMapVtag("12349876");
		map0.setCostMode("numerical");
		map0.setCostMetric("routingcost");
		map0.setCost("1.1.1.1", "1.1.1.1", 1);
		map0.setCost("1.1.1.1", "1.1.1.2", 5);
		map0.setCost("1.1.1.1", "1.1.1.3", 10);
		map0.setCost("1.1.1.2", "1.1.1.1", 5);
		map0.setCost("1.1.1.2", "1.1.1.2", 1);
		map0.setCost("1.1.1.2", "1.1.1.3", 15);
		map0.setCost("1.1.1.3", "1.1.1.1", 20);
		map0.setCost("1.1.1.3", "1.1.1.2", 15);
		map0.setCost("1.1.1.3", "1.1.1.3", 1);
		String actual = map0.toString();
		
		String expectedCostIds =
			  "{\n"
			+ "\"endpoint-cost-map\": {\n"
			+ "  \"ipv4:1.1.1.1\": {\"ipv4:1.1.1.1\": 1, \"ipv4:1.1.1.2\": 5, \"ipv4:1.1.1.3\": 10}, \n"
			+ "  \"ipv4:1.1.1.2\": {\"ipv4:1.1.1.1\": 5, \"ipv4:1.1.1.2\": 1, \"ipv4:1.1.1.3\": 15}, \n"
			+ "  \"ipv4:1.1.1.3\": {\"ipv4:1.1.1.1\": 20, \"ipv4:1.1.1.2\": 15, \"ipv4:1.1.1.3\": 1}\n"
			+ "}, \n"
			+ "\"meta\": {\n"
			+ "  \"cost-type\": {\"cost-metric\": \"routingcost\", \"cost-mode\": \"numerical\"}\n"
			+ "}\n"
			+ "}";

		assertEquals("JSON", expectedCostIds, actual);
		
		AltoResp_EndpointCost map1 = new AltoResp_EndpointCost(actual);
		// assertEquals("map-vtag", "12349876", map1.getMapVtag());
		assertEquals("cost-mode", "numerical", map1.getCostMode());
		assertEquals("cost-type", "routingcost", map1.getCostMetric());
		assertEquals("src-Addrs", "String[ipv4:1.1.1.1,ipv4:1.1.1.2,ipv4:1.1.1.3]",
				catArray(sort(map1.getSrcAddrs())));
		assertEquals("dest-Addrs", "String[ipv4:1.1.1.1,ipv4:1.1.1.2,ipv4:1.1.1.3]",
				catArray(sort(map1.getDestAddrs("1.1.1.1"))));
		assertEquals("1.1.1.1=>1.1.1.1",  1.0, map1.getCost("1.1.1.1", "1.1.1.1"), .001);
		assertEquals("1.1.1.1=>1.1.1.2",  5.0, map1.getCost("1.1.1.1", "1.1.1.2"), .001);
		assertEquals("1.1.1.1=>1.1.1.3", 10.0, map1.getCost("1.1.1.1", "1.1.1.3"), .001);
		assertEquals("1.1.1.2=>1.1.1.1",  5.0, map1.getCost("1.1.1.2", "1.1.1.1"), .001);
		assertEquals("1.1.1.2=>1.1.1.2",  1.0, map1.getCost("1.1.1.2", "1.1.1.2"), .001);
		assertEquals("1.1.1.2=>1.1.1.3", 15.0, map1.getCost("1.1.1.2", "1.1.1.3"), .001);
		assertEquals("1.1.1.3=>1.1.1.1", 20.0, map1.getCost("1.1.1.3", "1.1.1.1"), .001);
		assertEquals("1.1.1.3=>1.1.1.2", 15.0, map1.getCost("1.1.1.3", "1.1.1.2"), .001);
		assertEquals("1.1.1.3=>1.1.1.3",  1.0, map1.getCost("1.1.1.3", "1.1.1.3"), .001);
		
		assertEquals("Def-cost", -1, map1.getDefaultCost(), 0.001);
	}

	@Test
	public void testCostMapDef() throws JSONException
	{
		AltoResp_EndpointCost map0 = new AltoResp_EndpointCost();
		// map0.setMapVtag("12349876");
		map0.setCostMode("numerical");
		map0.setCostMetric("routingcost");
		map0.setCost("1.1.1.1", "1.1.1.1", 1);
		map0.setCost("1.1.1.1", "1.1.1.2", 5);
		map0.setCost("1.1.1.1", "1.1.1.3", 10);
		map0.setCost("1.1.1.2", "1.1.1.1", 5);
		map0.setCost("1.1.1.2", "1.1.1.2", 1);
		map0.setCost("1.1.1.2", "1.1.1.3", 15);
		map0.setCost("1.1.1.3", "1.1.1.1", 20);
		map0.setCost("1.1.1.3", "1.1.1.2", 15);
		map0.setCost("1.1.1.3", "1.1.1.3", 1);
		map0.setDefaultCost(42);
		String actual = map0.toString();

		String expectedCostIds =
				  "{\n"
				+ "\"endpoint-cost-map\": {\n"
				+ "  \"ipv4:1.1.1.1\": {\"ipv4:1.1.1.1\": 1, \"ipv4:1.1.1.2\": 5, \"ipv4:1.1.1.3\": 10}, \n"
				+ "  \"ipv4:1.1.1.2\": {\"ipv4:1.1.1.1\": 5, \"ipv4:1.1.1.2\": 1, \"ipv4:1.1.1.3\": 15}, \n"
				+ "  \"ipv4:1.1.1.3\": {\"ipv4:1.1.1.1\": 20, \"ipv4:1.1.1.2\": 15, \"ipv4:1.1.1.3\": 1}\n"
				+ "}, \n"
				+ "\"meta\": {\n"
				+ "  \"cost-type\": {\"cost-metric\": \"routingcost\", \"cost-mode\": \"numerical\"}, \n"
				+ "  \"default-cost\": 42\n"
				+ "}\n"
				+ "}";
				
		assertEquals("JSON", expectedCostIds, actual);
		
		AltoResp_EndpointCost map1 = new AltoResp_EndpointCost(actual);
		// assertEquals("map-vtag", "12349876", map1.getMapVtag());
		assertEquals("cost-mode", "numerical", map1.getCostMode());
		assertEquals("cost-type", "routingcost", map1.getCostMetric());
		assertEquals("src-Addrs", "String[ipv4:1.1.1.1,ipv4:1.1.1.2,ipv4:1.1.1.3]",
				catArray(sort(map1.getSrcAddrs())));
		assertEquals("dest-Addrs", "String[ipv4:1.1.1.1,ipv4:1.1.1.2,ipv4:1.1.1.3]",
				catArray(sort(map1.getDestAddrs("1.1.1.1"))));
		assertEquals("1.1.1.1=>1.1.1.1",  1.0, map1.getCost("1.1.1.1", "1.1.1.1"), .001);
		assertEquals("1.1.1.1=>1.1.1.2",  5.0, map1.getCost("1.1.1.1", "1.1.1.2"), .001);
		assertEquals("1.1.1.1=>1.1.1.3", 10.0, map1.getCost("1.1.1.1", "1.1.1.3"), .001);
		assertEquals("1.1.1.2=>1.1.1.1",  5.0, map1.getCost("1.1.1.2", "1.1.1.1"), .001);
		assertEquals("1.1.1.2=>1.1.1.2",  1.0, map1.getCost("1.1.1.2", "1.1.1.2"), .001);
		assertEquals("1.1.1.2=>1.1.1.3", 15.0, map1.getCost("1.1.1.2", "1.1.1.3"), .001);
		assertEquals("1.1.1.3=>1.1.1.1", 20.0, map1.getCost("1.1.1.3", "1.1.1.1"), .001);
		assertEquals("1.1.1.3=>1.1.1.2", 15.0, map1.getCost("1.1.1.3", "1.1.1.2"), .001);
		assertEquals("1.1.1.3=>1.1.1.3",  1.0, map1.getCost("1.1.1.3", "1.1.1.3"), .001);
		
		assertEquals("Def-cost", 42, map1.getDefaultCost(), 0.001);
	}

	public static void main(String[] args) throws JSONException
	{
		AltoResp_EndpointCost map0 = new AltoResp_EndpointCost();
		// map0.setMapVtag("12349876");
		map0.setCostMode("numerical");
		map0.setCostMetric("routingcost");
		map0.setCost("1.1.1.1", "1.1.1.1", 1);
		map0.setCost("1.1.1.1", "1.1.1.2", 5);
		map0.setCost("1.1.1.1", "1.1.1.3", 10);
		map0.setCost("1.1.1.2", "1.1.1.1", 5);
		map0.setCost("1.1.1.2", "1.1.1.2", 1);
		map0.setCost("1.1.1.2", "1.1.1.3", 15);
		map0.setCost("1.1.1.3", "1.1.1.1", 20);
		map0.setCost("1.1.1.3", "1.1.1.2", 15);
		map0.setCost("1.1.1.3", "1.1.1.3", 1);
		
		String x = map0.getJSON();
		System.out.println(map0.toString());
		AltoResp_EndpointCost map1 = new AltoResp_EndpointCost(x);
		// System.out.println("map-vtag: " + map1.getMapVtag());
		String[] keys = map1.getSrcAddrs();
		Arrays.sort(keys);
		System.out.println("Src Addrs: " + catArray(keys));
		for (String src:keys) {
			for (String dest:keys) {
				System.out.println("  " + src + "->" + dest + ": " + map1.getCost(src,dest));
			}
		}
	}
}
