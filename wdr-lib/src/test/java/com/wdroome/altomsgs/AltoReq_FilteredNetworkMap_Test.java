package com.wdroome.altomsgs;

import static org.junit.Assert.*;

import org.junit.Test;

import com.wdroome.json.JSONException;
import com.wdroome.altomsgs.*;
import com.wdroome.json.JSONValue_Object;

/**
 * @author wdr
 */
public class AltoReq_FilteredNetworkMap_Test extends CommonTestMethods
{

	@Test
	public void testAltoReq_FilteredNetworkMap() throws JSONException
	{
		AltoReq_FilteredNetworkMap req0 = new AltoReq_FilteredNetworkMap();
		req0.addPID("PID1");
		req0.addPIDs(new String[] {"PID3", "PID2"});
		
		AltoReq_FilteredNetworkMap req1 = new AltoReq_FilteredNetworkMap(req0.getJSON());
		String actual = req1.toString();
		String expected =
			  "{\n"
				+ "  \"address-types\": [],\n"
				+ "  \"pids\": [\n"
				+ "    \"PID1\",\n"
				+ "    \"PID3\",\n"
				+ "    \"PID2\"\n"
				+ "  ]\n"
				+ "}";
		assertEquals("JSON", expected.replaceAll("[ \n]",""), actual.replaceAll("[ \n]",""));
		assertEquals("getPIDs", "String[PID1,PID3,PID2]", catArray(req1.getPIDs()));
	}

	@Test
	public void test2AltoReq_FilteredNetworkMap() throws JSONException
	{
		AltoReq_FilteredNetworkMap req0 = new AltoReq_FilteredNetworkMap();
		req0.addPID("PID1");
		req0.addAddressTypes(new String[] {"ipv4", "ipv6"});
		
		AltoReq_FilteredNetworkMap req1 = new AltoReq_FilteredNetworkMap(req0.getJSON());
		String actual = req1.toString();
		String expected =
			  "{\n"
				+ "  \"address-types\": [\n"
				+ "    \"ipv4\",\n"
				+ "    \"ipv6\"\n"
				+ "  ],\n"
				+ "  \"pids\": [\"PID1\"]\n"
				+ "}";
		assertEquals("JSON", expected.replaceAll("[ \n]",""), actual.replaceAll("[ \n]",""));
		assertEquals("getPIDs", "String[PID1]", catArray(req1.getPIDs()));
		assertEquals("getAddressTypes", "String[ipv4,ipv6]", catArray(req1.getAddressTypes()));
	}

	public static void main(String[] args) throws JSONException
	{
		AltoReq_FilteredNetworkMap req0 = new AltoReq_FilteredNetworkMap();
		req0.addPID("PID1");
		req0.addPIDs(new String[] {"PID3", "PID2"});
		
		System.out.println(req0.toString());
		System.out.println("PIDS: " + catArray(req0.getPIDs()));

		req0 = new AltoReq_FilteredNetworkMap();
		req0.addPID("PID1");
		req0.addAddressTypes(new String[] {"ipv4", "ipv6"});
		
		System.out.println(req0.toString());
		System.out.println("PIDS: " + catArray(req0.getPIDs()));
		System.out.println("AddressTypes: " + catArray(req0.getAddressTypes()));
	}
}
