package com.wdroome.altodata;

import java.net.*;
import java.util.*;

import com.wdroome.json.JSONException;
import com.wdroome.altodata.NetworkMap;
import com.wdroome.altomsgs.AltoResp_NetworkMap;
import com.wdroome.util.inet.CIDRAddress;
import com.wdroome.util.inet.CIDRSet;
import com.wdroome.util.inet.EndpointAddress;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author wdr
 */
public class NetworkMapTest
{
	private static NetworkMap makeMap(boolean freeze) throws UnknownHostException
	{
		NetworkMap map = new NetworkMap("DUMMY");
		map.addCIDRs("PID1", "  192.0.2.0/24  198.51.100.0/25 ");
		map.addCIDRs("PID2", "198.51.100.128/25, 135.0.1.0/24");
		map.addCIDRs("PID1", "192.0.3.24/24");
		map.addCIDRs("PID0", "0.0.0.0/0");
		if (freeze)
			map.freeze();
		return map;
	}
	
	@Test
	public void testPidNumbers() throws UnknownHostException
	{
		NetworkMap map = makeMap(true);
		assertEquals("getNumPids", 3, map.getNumPids());
		assertEquals("indexToPid", "PID0", map.indexToPid(0));
		assertEquals("indexToPid", "PID1", map.indexToPid(1));
		assertEquals("indexToPid", "PID2", map.indexToPid(2));
		assertEquals("pidToIndex", 0, map.pidToIndex("PID0"));
		assertEquals("pidToIndex", 1, map.pidToIndex("PID1"));
		assertEquals("pidToIndex", 2, map.pidToIndex("PID2"));
		assertEquals("pidToIndex", -1, map.pidToIndex("PIDX"));
		assertArrayEquals("getPidNames", new String[] {"PID0", "PID1", "PID2"},
						map.getPidNames());
	}

	/**
	 * Test method for {@link NetworkMap#addCIDRs(java.lang.String, java.lang.String)}.
	 * @throws UnknownHostException 
	 */
	@Test
	public void testAddCIDRsStringString() throws UnknownHostException
	{
		NetworkMap map = makeMap(true);
		for (int iPass = 0; iPass < 2; iPass++) {
			ArrayList<String> actList = new ArrayList<String>();
			for (String pid: map.allPids()) {
				actList.add(pid);
			}
			String[] actual = actList.toArray(new String[actList.size()]);
			Arrays.sort(actual);
			String[] expected = new String[] {
					"PID0", "PID1", "PID2"
			};
			assertArrayEquals("PIDs", expected, actual);
			
			actList.clear();
			for (Map.Entry<String,CIDRSet> ent:map.allPidEntries()) {
				actList.add(ent.getKey() + ": " + ent.getValue().toSortedString());
			}
			actual = actList.toArray(new String[actList.size()]);
			Arrays.sort(actual);
			expected = new String[] {
					"PID0: [0.0.0.0/0]",
					"PID1: [198.51.100.0/25 192.0.2.0/24 192.0.3.0/24]",
					"PID2: [198.51.100.128/25 135.0.1.0/24]",
			};
			assertArrayEquals("PIDs->CIDRs", expected, actual);
			
			actList.clear();
			for (Map.Entry<CIDRAddress,String> ent:map.allCIDREntries()) {
				actList.add(ent.getKey().toString() + ": " + ent.getValue());
			}
			actual = actList.toArray(new String[actList.size()]);
			expected = new String[] {
					"198.51.100.0/25: PID1",
					"198.51.100.128/25: PID2",
					"135.0.1.0/24: PID2",
					"192.0.2.0/24: PID1",
					"192.0.3.0/24: PID1",
					"0.0.0.0/0: PID0",
			};
			assertArrayEquals("CIDRs->PID", expected, actual);
			map.freeze();
		}
	}

	/**
	 * Test method for {@link NetworkMap#getCIDRs(java.lang.String)}.
	 * @throws UnknownHostException 
	 */
	@Test
	public void testGetCIDRs() throws UnknownHostException
	{
		NetworkMap map = makeMap(true);
		for (int i = 0; i < 2; i++) {
			assertEquals("PID0", new CIDRSet("0.0.0.0/0"), map.getCIDRs("PID0"));
			assertEquals("PID1", new CIDRSet("192.0.2.0/24 192.0.3.0/24 198.51.100.0/25"), map.getCIDRs("PID1"));
			assertEquals("PID2", new CIDRSet("135.0.1.0/24 198.51.100.128/25"), map.getCIDRs("PID2"));
			assertEquals("PIDXXX", null, map.getCIDRs("PIDXXX"));
			map.freeze();
		}
	}

	/**
	 * Test method for {@link NetworkMap#getPID(com.wdroome.util.EndpointAddress)}.
	 * @throws UnknownHostException 
	 */
	@Test
	public void testGetPIDEndpointAddress() throws UnknownHostException
	{
		NetworkMap map = makeMap(true);
		for (int i = 0; i < 2; i++) {
			assertEquals("127.0.0.1", "PID0", map.getPID(new EndpointAddress("127.0.0.1")));
			assertEquals("192.0.2.27", "PID1", map.getPID(new EndpointAddress("192.0.2.27")));
			assertEquals("192.0.1.27", "PID0", map.getPID(new EndpointAddress("192.0.1.27")));
			assertEquals("198.51.100.1", "PID1", map.getPID(new EndpointAddress("198.51.100.1")));
			assertEquals("198.51.100.129", "PID2", map.getPID(new EndpointAddress("198.51.100.129")));
			map.freeze();
		}
	}
	
	@Test
	public void testCheckFullCoverage() throws UnknownHostException
	{
		NetworkMap map = new NetworkMap("DUMMY");
		map.addCIDRs("PID1", "  192.0.2.0/24  198.51.100.0/25 ");
		map.addCIDRs("PID2", "198.51.100.128/25, 135.0.1.0/24");
		map.addCIDRs("PID1", "192.0.3.24/24");
		assertEquals("Test1/fail", "Network map does not cover all IPV4 addresses", map.checkFullCoverage(true));
		map.addCIDRs("PID1", "0.0.0.0/1");
		map.addCIDRs("PID2", "128.0.0.0/1");
		// System.out.println(map);
		assertEquals("Test2/fail", "Network map does not cover all IPV6 addresses", map.checkFullCoverage(true));
		map.addCIDRs("PID2", "::/0");
		// System.out.println(map);
		assertEquals("Test3/okay", null, map.checkFullCoverage(true));
	}
	
	@Test
	public void testContainingPid() throws UnknownHostException
	{
		NetworkMap map = new NetworkMap("DUMMY");
		map.addCIDRs("PID0", "0.0.0.0/0, ::0/0");
		for (int i = 0; i < 256; i++) {
			map.addCIDRs("PIDa-" + i, i + ".0.0.0/8");
		}
		for (int i = 0; i < 256; i++) {
			map.addCIDRs("PIDb-" + i, i + ".0.0.0/24 " + i + ".0.64.0/24");
		}
		for (int i = 0; i < 256; i++) {
			map.addCIDRs("PIDc-" + i, i + ".0.0.0/28 " + i + ".0.64.0/28");
		}
		for (int i = 0; i < 32; i++) {
			map.addCIDRs("PIDd-" + i, i + ".0.1.0/24 " + (i+128) + ".0.1.0/24");
		}
		map.freeze();
		for (int i = 0; i < 256; i++) {
			assertEquals("PIDa-" + i, "PID0", map.getContainingPid("PIDa-" + i));
		}
		for (int i = 0; i < 256; i++) {
			assertEquals("PIDb-" + i, "PIDa-" + i, map.getContainingPid("PIDb-" + i));
		}
		for (int i = 0; i < 256; i++) {
			assertEquals("PIDc-" + i, "PIDb-" + i, map.getContainingPid("PIDc-" + i));
		}
		for (int i = 0; i < 32; i++) {
			assertEquals("PIDd-" + i, null, map.getContainingPid("PIDd-" + i));
		}
	}
	
	@Test
	public void testUnnormalizedCIDRs() throws UnknownHostException
	{
		NetworkMap map = new NetworkMap("DUMMY");
		map.addCIDRs("PID1", "10.0.0.0/16 10.1.0.0/16 10.2.0.0/16 10.3.0.0/16");
		map.addCIDRs("PID2", "10.0.0.0/15");
		map.addCIDRs("PID0", "0.0.0.0/0");
		map.freeze();
		assertArrayEquals("getPidNames", new String[] {"PID0", "PID1", "PID2"},
				map.getPidNames());
		assertEquals("10.0.0.0", "PID1", map.getPID(new EndpointAddress("10.0.0.0")));
		assertEquals("PID0 parent", null, map.getContainingPid("PID0"));
		assertEquals("PID1 parent", null, map.getContainingPid("PID1"));
		assertEquals("PID2 parent", "PID0", map.getContainingPid("PID2"));
	}
	
	@Test
	public void testUnnormalizedCIDRs2() throws UnknownHostException
	{
		NetworkMap map = new NetworkMap("DUMMY");
		map.addCIDRs("PID1", "10.0.0.0/16 10.1.0.0/16 10.2.0.0/16");
		map.addCIDRs("PID2", "10.0.0.0/15");
		map.addCIDRs("PID0", "0.0.0.0/0");
		map.freeze();
		assertEquals("PID0 parent", null, map.getContainingPid("PID0"));
		assertEquals("PID1 parent", null, map.getContainingPid("PID1"));
		assertEquals("PID2 parent", "PID0", map.getContainingPid("PID2"));
	}
	
	@Test
	public void testUnnormalizedCIDRs3() throws UnknownHostException
	{
		NetworkMap map = new NetworkMap("DUMMY");
		map.addCIDRs("PID1", "10.0.0.0/16 10.1.0.0/16");
		map.addCIDRs("PID2", "10.0.0.0/15");
		map.addCIDRs("PID0", "0.0.0.0/0");
		map.freeze();
		assertEquals("PID0 parent", null, map.getContainingPid("PID0"));
		assertEquals("PID1 parent", "PID2", map.getContainingPid("PID1"));
		assertEquals("PID2 parent", "PID0", map.getContainingPid("PID2"));
	}
	
	@Test
	public void testUnnormalizedCIDRs4() throws UnknownHostException
	{
		NetworkMap map = new NetworkMap("DUMMY");
		map.addCIDRs("PID1", "10.0.0.0/16 10.1.0.0/16 10.2.0.0/16 10.3.0.0/16");
		map.addCIDRs("PID2", "10.0.0.0/15 10.1.0.0/15 11.0.0.0/8");
		map.addCIDRs("PID0", "0.0.0.0/0");
		map.freeze();
		assertEquals("PID0 parent", null, map.getContainingPid("PID0"));
		assertEquals("PID1 parent", null, map.getContainingPid("PID1"));
		assertEquals("PID2 parent", "PID0", map.getContainingPid("PID2"));
	}
	
	@Test
	public void testUnnormalizedCIDRs5() throws UnknownHostException
	{
		NetworkMap map = new NetworkMap("DUMMY");
		map.addCIDRs("PID1", "10.0.0.0/16 128.0.0.0/16");
		map.addCIDRs("PID2", "10.0.0.0/15");
		map.addCIDRs("PID0a", "0.0.0.0/0");
		map.addCIDRs("PID0b", "128.0.0.0/1");
		map.freeze();
		assertEquals("PID0a parent", null, map.getContainingPid("PID0a"));
		assertEquals("PID0b parent", "PID0a", map.getContainingPid("PID0b"));
		assertEquals("PID1 parent", null, map.getContainingPid("PID1"));
		assertEquals("PID2 parent", "PID0a", map.getContainingPid("PID2"));
	}
	
	@Test
	public void testUnnormalizedCIDRs6() throws UnknownHostException
	{
		NetworkMap map = new NetworkMap("DUMMY");
		map.addCIDRs("PID1", "10.0.0.0/16 128.0.0.0/16");
		map.addCIDRs("PID2", "10.0.0.0/15");
		map.addCIDRs("PID0a", "0.0.0.0/0");
		map.addCIDRs("PID0b", "128.0.0.0/1");
		map.addCIDRs("PID4", "10.0.0.0/24 128.0.0.0/24");
		map.addCIDRs("PID5", "10.0.0.4/30");
		map.addCIDRs("PID6", "128.0.0.4/30");
		map.freeze();
		assertEquals("PID0a parent", null, map.getContainingPid("PID0a"));
		assertEquals("PID0b parent", "PID0a", map.getContainingPid("PID0b"));
		assertEquals("PID1 parent", null, map.getContainingPid("PID1"));
		assertEquals("PID2 parent", "PID0a", map.getContainingPid("PID2"));
		assertEquals("PID4 parent", "PID1", map.getContainingPid("PID4"));
		assertEquals("PID5 parent", "PID4", map.getContainingPid("PID5"));
		assertEquals("PID6 parent", "PID4", map.getContainingPid("PID6"));
	}
	
	@Test
	public void testNormalizedCIDRs() throws UnknownHostException
	{
		NetworkMap map = new NetworkMap("DUMMY");
		map.addCIDRs("PID1", "10.0.0.0/14");
		map.addCIDRs("PID2", "10.0.0.0/15");
		map.addCIDRs("PID0", "0.0.0.0/0");
		map.freeze();
		assertArrayEquals("getPidNames", new String[] {"PID0", "PID1", "PID2"},
				map.getPidNames());
		assertEquals("10.0.0.0", "PID2", map.getPID(new EndpointAddress("10.0.0.0")));
		assertEquals("PID1 parent", "PID0", map.getContainingPid("PID1"));
		assertEquals("PID2 parent", "PID1", map.getContainingPid("PID2"));
	}
	
	@Test
	public void testUnknownAddrTypes() throws UnknownHostException, JSONException
	{
		String json =
				  "{"
				+ "  \"meta\": {"
				+ "    \"initial-cost\": 12,"
				+ "    \"vtag\": {"
				+ "      \"resource-id\": \"map\","
				+ "      \"tag\": \"12349876\""
				+ "    }"
				+ "  },"
				+ "  \"network-map\": {"
				+ "    \"PID1\": {\"ipv4\": []},"
				+ "    \"PID2\": {},"
				+ "    \"PID3\": {\"ipvx\": [\"1.2.3.4.5\"]},"
				+ "    \"PID0\": {"
				+ "      \"ipv4\": [\"0.0.0.0/0\"],"
				+ "      \"ipv6\": [\"::0/0\"],"
				+ "      \"foo\": [\"xyz\"],"
				+ "      \"gok\": [\"xyz/0\"]"
				+ "    }"
				+ "  }"
				+ "}";
		// First, make sure the c'tor creates the new map
		// without throwing UnknownHostException or UnknownAddressTypeException.
		NetworkMap map = new NetworkMap(new AltoResp_NetworkMap(json));

		assertArrayEquals("getPidNames",
							new String[] {"PID0", "PID1", "PID2", "PID3"},
							map.getPidNames());

		assertEquals("getCIDRs(PID1)", new CIDRSet(),
							map.getCIDRs("PID1"));		
		assertEquals("getCIDRs(PID2)", new CIDRSet(),
							map.getCIDRs("PID2"));		
		assertEquals("getCIDRs(PID3)", new CIDRSet(),
							map.getCIDRs("PID3"));		
		assertEquals("getCIDRs(PID0)", new CIDRSet("ipv4:0.0.0.0/0 ipv6:::0/0"),
							map.getCIDRs("PID0"));
	}
	
	public static void main(String[] args) throws UnknownHostException
	{
		NetworkMap map;
		
//		map = makeMap(true);
//		for (String pid: map.allPids()) {
//			System.out.println(pid + " => " + map.getContainingPid(pid));
//		}
		
		map = new NetworkMap("TEST");
		map.addCIDRs("PID0", "0.0.0.0/0, ::0/0");
		for (int i = 0; i < 2; i++) {
			map.addCIDRs("PIDa-" + i, i + ".0.0.0/8");
		}
		map.addCIDRs("PIDc-0", "0.0.0.0/24, 1.0.0.0/24");
		map.freeze();
		System.out.println(map);
		System.out.println("PIDc-0: " + map.getContainingPid("PIDc-0"));
	}
}
