package com.wdroome.altomsgs;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;

import com.wdroome.json.JSONException;
import com.wdroome.altomsgs.*;
import com.wdroome.util.inet.EndpointAddress;
import com.wdroome.json.JSONValue_Object;

import org.junit.*;

import static org.junit.Assert.*;

/**
 * @author wdr
 */
public class AltoResp_NetworkMap_Test extends CommonTestMethods
{
	@Test
	public void testNetworkMap() throws JSONException, UnknownHostException
	{
		AltoResp_NetworkMap map0 = new AltoResp_NetworkMap();
		map0.setThisVtag("map", "12349876");
		map0.addCIDR("PID1", "ipv4", "192.0.2.0/24");
		map0.addCIDR("PID1", "ipv4", "198.51.100.0/25");
		map0.addCIDR("PID2", "ipv4", "192.0.3.0/24");
		map0.addCIDR("PID2", "ipv4", "198.51.100.128/25");
		map0.addCIDR("PID3", "ipv4", "0.0.0.0/0");
		map0.addCIDR("PID3", "ipv6", "::0/0");
		map0.addPID("PID4");
		AltoResp_NetworkMap map1 = new AltoResp_NetworkMap(map0.getJSON());
		String actual = map1.toString();
		
		String expected =
			  "{\n"
			+ "  \"meta\": {\"vtag\": {\n"
			+ "    \"resource-id\": \"map\",\n"
			+ "    \"tag\": \"12349876\"\n"
			+ "  }},\n"
			+ "  \"network-map\": {\n"
			+ "    \"PID1\": {\"ipv4\": [\n"
			+ "      \"192.0.2.0/24\",\n"
			+ "      \"198.51.100.0/25\"\n"
			+ "    ]},\n"
			+ "    \"PID2\": {\"ipv4\": [\n"
			+ "      \"192.0.3.0/24\",\n"
			+ "      \"198.51.100.128/25\"\n"
			+ "    ]},\n"
			+ "    \"PID3\": {\n"
			+ "      \"ipv4\": [\"0.0.0.0/0\"],\n"
			+ "      \"ipv6\": [\"::0/0\"]\n"
			+ "    },\"PID4\":{}\n"
			+ "  }\n"
			+ "}";
		assertEquals("JSON", expected.replaceAll("[ \n]",""), actual.replaceAll("[ \n]",""));
		
		assertEquals("map-vtag", "12349876", map1.getThisTag());
		assertEquals("PIDs", "String[PID1,PID2,PID3,PID4]",
				catArray(sort(map1.getPIDs())));
		assertEquals("PID1", "String[192.0.2.0/24,198.51.100.0/25]",
				catArray(sort(map1.getCIDRs("PID1", EndpointAddress.IPV4_PREFIX))));
		assertEquals("PID1-types", "String[ipv4]",
				catArray(sort(iterToArray(map1.getAddressTypes("PID1")))));
		assertEquals("PID1", "String[]",
				catArray(sort(map1.getCIDRs("PID1", EndpointAddress.IPV6_PREFIX))));
		assertEquals("PID2", "String[192.0.3.0/24,198.51.100.128/25]",
				catArray(sort(map1.getCIDRs("PID2", EndpointAddress.IPV4_PREFIX))));
		assertEquals("PID3", "String[0.0.0.0/0]",
				catArray(sort(map1.getCIDRs("PID3", EndpointAddress.IPV4_PREFIX))));
		assertEquals("PID3-all", "String[ipv4:0.0.0.0/0,ipv6:::0/0]",
				catArray(sort(map1.getCIDRs("PID3"))));
		assertEquals("PID3-types", "String[ipv4,ipv6]",
				catArray(sort(iterToArray(map1.getAddressTypes("PID3")))));
		assertEquals("PID4", "String[]",
				catArray(sort(map1.getCIDRs("PID4"))));
		assertEquals("PID4-all", "String[]",
				catArray(sort(map1.getCIDRs("PID4", EndpointAddress.IPV4_PREFIX))));
		assertEquals("PID4-types", "String[]",
				catArray(sort(iterToArray(map1.getAddressTypes("PID4")))));
		assertEquals("PID?-types", "String[]",
				catArray(sort(iterToArray(map1.getAddressTypes("PID?")))));
		
		assertEquals("Initial-cost", -1, map1.getInitialCost(), .001);

		assertEquals("findPID/1", "PID1", map1.findPID(new EndpointAddress("192.0.2.1"), null, null));
		assertEquals("findPID/2", "PID3", map1.findPID(new EndpointAddress("192.0.4.1"), null, null));
	}
	
	@Test
	public void testInitialCost() throws JSONException
	{
		AltoResp_NetworkMap map0 = new AltoResp_NetworkMap();
		map0.setThisVtag("map", "12349876");
		map0.addCIDR("PID1", "ipv4", "192.0.2.0/24");
		map0.addCIDR("PID1", "ipv4", "198.51.100.0/25");
		map0.addCIDR("PID2", "ipv4", "192.0.3.0/24");
		map0.addCIDR("PID2", "ipv4", "198.51.100.128/25");
		map0.addCIDR("PID3", "ipv4", "0.0.0.0/0");
		map0.addCIDR("PID3", "ipv6", "::0/0");
		map0.setInitialCost(12);
		AltoResp_NetworkMap map1 = new AltoResp_NetworkMap(map0.getJSON());
		String actual = map1.toString();
		
		String expected =
			  "{\n"
			+ "  \"meta\": {\n"
			+ "    \"initial-cost\": 12,\n"
			+ "    \"vtag\": {\n"
			+ "      \"resource-id\": \"map\",\n"
			+ "      \"tag\": \"12349876\"\n"
			+ "    }\n"
			+ "  },\n"
			+ "  \"network-map\": {\n"
			+ "    \"PID1\": {\"ipv4\": [\n"
			+ "      \"192.0.2.0/24\",\n"
			+ "      \"198.51.100.0/25\"\n"
			+ "    ]},\n"
			+ "    \"PID2\": {\"ipv4\": [\n"
			+ "      \"192.0.3.0/24\",\n"
			+ "      \"198.51.100.128/25\"\n"
			+ "    ]},\n"
			+ "    \"PID3\": {\n"
			+ "      \"ipv4\": [\"0.0.0.0/0\"],\n"
			+ "      \"ipv6\": [\"::0/0\"]\n"
			+ "    }\n"
			+ "  }\n"
			+ "}";
		assertEquals("JSON", expected.replaceAll("[ \n]",""), actual.replaceAll("[ \n]",""));
		
		assertEquals("map-vtag", "12349876", map1.getThisTag());
		assertEquals("PIDs", "String[PID1,PID2,PID3]",
				catArray(sort(map1.getPIDs())));
		assertEquals("PID1", "String[192.0.2.0/24,198.51.100.0/25]",
				catArray(sort(map1.getCIDRs("PID1", EndpointAddress.IPV4_PREFIX))));
		assertEquals("PID1-types", "String[ipv4]",
				catArray(sort(iterToArray(map1.getAddressTypes("PID1")))));
		assertEquals("PID1", "String[]",
				catArray(sort(map1.getCIDRs("PID1", EndpointAddress.IPV6_PREFIX))));
		assertEquals("PID2", "String[192.0.3.0/24,198.51.100.128/25]",
				catArray(sort(map1.getCIDRs("PID2", EndpointAddress.IPV4_PREFIX))));
		assertEquals("PID3", "String[0.0.0.0/0]",
				catArray(sort(map1.getCIDRs("PID3", EndpointAddress.IPV4_PREFIX))));
		assertEquals("PID3-types", "String[ipv4,ipv6]",
				catArray(sort(iterToArray(map1.getAddressTypes("PID3")))));
		assertEquals("PID?-types", "String[]",
				catArray(sort(iterToArray(map1.getAddressTypes("PID?")))));
		
		assertEquals("Initial-cost", 12, map1.getInitialCost(), .001);
	}
	
	@Test
	public void testEmptyPID() throws JSONException
	{
		String json =
				  "{"
				+ "  \"meta\": {\"vtag\": {"
				+ "    \"resource-id\": \"map\","
				+ "    \"tag\": \"12349876\""
				+ "  }},"
				+ "  \"network-map\": {"
				+ "    \"PID1\": {\"ipvx\": []},"
				+ "    \"PID2\": {},"
				+ "    \"PID3\": {"
				+ "      \"ipv4\": [\"0.0.0.0/0\"],"
				+ "      \"ipv6\": [\"::0/0\"]"
				+ "    }"
				+ "  }"
				+ "}";
		AltoResp_NetworkMap map = new AltoResp_NetworkMap(json);
		assertEquals("PIDs", "String[PID1,PID2,PID3]",
				catArray(sort(map.getPIDs())));
		assertEquals("PID1", "String[]",
				catArray(sort(map.getCIDRs("PID1", EndpointAddress.IPV4_PREFIX))));
		assertEquals("PID1-types", "String[ipvx]",
				catArray(sort(iterToArray(map.getAddressTypes("PID1")))));
		assertEquals("PID2", "String[]",
				catArray(sort(map.getCIDRs("PID2", EndpointAddress.IPV4_PREFIX))));
		assertEquals("PID2-types", "String[]",
				catArray(sort(iterToArray(map.getAddressTypes("PID2")))));
		
		assertArrayEquals("PID-names", new String[] {"PID1", "PID2", "PID3"},
				sort(map.getPIDs()));
	}

	public static void main(String[] args) throws JSONException
	{
		AltoResp_NetworkMap map0 = new AltoResp_NetworkMap();
		map0.setThisVtag("map", "12349876");
		map0.addCIDR("PID1", "ipv4", "192.0.2.0/24");
		map0.addCIDR("PID1", "ipv4", "198.51.100.0/25");
		map0.addCIDR("PID2", "ipv4", "192.0.3.0/24");
		map0.addCIDR("PID2", "ipv4", "198.51.100.128/25");
		map0.addCIDR("PID3", "ipv4", "0.0.0.0/0");
		map0.addCIDR("PID3", "ipv6", "::0/0");
		
		String x = map0.getJSON();
		AltoResp_NetworkMap map1 = new AltoResp_NetworkMap(x);
		System.out.println(map1.toString());
		System.out.println("map-vtag: " + map1.getThisTag());
		String[] keys = map1.getPIDs();
		Arrays.sort(keys);
		System.out.println("PIDs: " + catArray(keys));
		for (String key:keys) {
			System.out.println("  " + key + ": " + catArray(map1.getCIDRs(key, EndpointAddress.IPV4_PREFIX)));
		}
	}
}
