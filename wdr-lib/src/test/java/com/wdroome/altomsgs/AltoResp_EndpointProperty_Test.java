package com.wdroome.altomsgs;

import java.util.Arrays;

import com.wdroome.json.JSONException;
import com.wdroome.altomsgs.*;
import com.wdroome.util.String2;

import org.junit.*;

import static org.junit.Assert.*;

/**
 * @author wdr
 */
public class AltoResp_EndpointProperty_Test extends CommonTestMethods
{
	@Test
	public void testEndpointProperty() throws JSONException
	{
		AltoResp_EndpointProp map0 = new AltoResp_EndpointProp();
		map0.setProperty("192.0.2.2", "map.pid", "PID1");
		map0.setProperty("192.0.2.2", "foo", "foo-value");
		map0.setProperty("128.0.2.2", "map.pid", "PID2");
		map0.setProperty("128.0.2.2", "foo", "foo-value");
		map0.setProperty("pid:pid1", "foo", "pid1-foo-value");
		map0.setProperty("128.0.2.2/32", "foo", "cidr-foo-value");
		map0.setDependentVtags(Arrays.asList(new String2("map", "12345")));
		String actual = map0.toString();
		
		String expected =
			  "{\n"
			+ "\"endpoint-properties\": {\n"
			+ "  \"ipv4:128.0.2.2\": {\"foo\": \"foo-value\", \"map.pid\": \"PID2\"}, \n"
			+ "  \"ipv4:128.0.2.2/32\": {\"foo\": \"cidr-foo-value\"}, \n"
			+ "  \"ipv4:192.0.2.2\": {\"foo\": \"foo-value\", \"map.pid\": \"PID1\"}, \n"
			+ "  \"pid:pid1\": {\"foo\": \"pid1-foo-value\"}\n"
			+ "}, \n"
			+ "\"meta\": {\n"
			+ "  \"dependent-vtags\": [\n"
			+ "    {\"resource-id\": \"map\", \"tag\": \"12345\"}\n"
			+ "  ]\n"
			+ "}\n"
			+ "}";
		assertEquals("JSON", expected, actual);
		
		AltoResp_EndpointProp map1 = new AltoResp_EndpointProp(actual);
		assertEquals("endpoint-addrs", "String[ipv4:128.0.2.2,ipv4:128.0.2.2/32,ipv4:192.0.2.2,pid:pid1]",
				catArray(sort(map1.getEndpointAddrs())));
		assertEquals("prop-names", "String[foo,map.pid]",
				catArray(sort(map1.getPropertyNames("192.0.2.2"))));
		assertEquals("ipv4:192.0.2.2/pid", "PID1", map1.getProperty("192.0.2.2", "map.pid", "???"));
		assertEquals("ipv4:192.0.2.2/foo", "foo-value", map1.getProperty("192.0.2.2", "foo", "???"));
		assertEquals("ipv4:128.0.2.2/pid", "PID2", map1.getProperty("128.0.2.2", "map.pid", "???"));
		assertEquals("ipv4:128.0.2.2/foo", "foo-value", map1.getProperty("128.0.2.2", "foo", "???"));
		assertEquals("pid:pid1/foo", "pid1-foo-value", map1.getProperty("pid:pid1", "foo", "???"));
		assertEquals("ipv4:128.0.2.2/foo", "cidr-foo-value", map1.getProperty("128.0.2.2/32", "foo", "???"));
		assertEquals("map-id", "map", map1.getDependentResourceId());
		assertEquals("map-vtag", "12345", map1.getDependentTag());
	}
	
	@Test
	public void testEndpointProperty2() throws JSONException
	{
		AltoResp_EndpointProp map0 = new AltoResp_EndpointProp();
		map0.setProperty("192.0.2.2", "xpid", "PID1");
		map0.setProperty("192.0.2.2", "foo", "foo-value");
		map0.setProperty("128.0.2.2", "xpid", "PID2");
		map0.setProperty("128.0.2.2", "foo", "foo-value");
		String actual = map0.toString();
		
		String expected =
			  "{\n"
			+ "\"endpoint-properties\": {\n"
			+ "  \"ipv4:128.0.2.2\": {\"foo\": \"foo-value\", \"xpid\": \"PID2\"}, \n"
			+ "  \"ipv4:192.0.2.2\": {\"foo\": \"foo-value\", \"xpid\": \"PID1\"}\n"
			+ "}, \n"
			+ "\"meta\": {}\n"
			+ "}";
		assertEquals("JSON", expected, actual);
		
		AltoResp_EndpointProp map1 = new AltoResp_EndpointProp(actual);
		assertEquals("endpoint-addrs", "String[ipv4:128.0.2.2,ipv4:192.0.2.2]",
				catArray(sort(map1.getEndpointAddrs())));
		assertEquals("prop-names", "String[foo,xpid]",
				catArray(sort(map1.getPropertyNames("192.0.2.2"))));
		assertEquals("ipv4:192.0.2.2/xpid", "PID1", map1.getProperty("192.0.2.2", "xpid", "???"));
		assertEquals("ipv4:192.0.2.2/foo", "foo-value", map1.getProperty("192.0.2.2", "foo", "???"));
		assertEquals("ipv4:128.0.2.2/xpid", "PID2", map1.getProperty("128.0.2.2", "xpid", "???"));
		assertEquals("ipv4:128.0.2.2/foo", "foo-value", map1.getProperty("128.0.2.2", "foo", "???"));
		assertEquals("map-vtag", null, map1.getDependentTag());
	}
	
	public static void main(String[] args) throws JSONException
	{
		AltoResp_EndpointProp map0 = new AltoResp_EndpointProp();
		map0.setProperty("192.0.2.2", "map.pid", "PID1");
		map0.setProperty("192.0.2.2", "foo", "foo-value");
		map0.setProperty("128.0.2.2", "map.pid", "PID2");
		map0.setDependentVtags(Arrays.asList(new String2("map", "12345")));
		map0.setProperty("128.0.2.2", "foo", "foo-value");
		
		System.out.println(map0.toString());
		AltoResp_EndpointProp map1 = new AltoResp_EndpointProp(map0.getJSON());
		String[] addrs = map1.getEndpointAddrs();
		Arrays.sort(addrs);
		System.out.println("Endpoint Addrs: " + catArray(addrs));
		String[] propNames = map1.getPropertyNames(addrs[0]);
		for (String addr:addrs) {
			for (String prop:propNames) {
				System.out.println("  " + addr + "/" + prop + ": " + map1.getProperty(addr,prop,"???"));
			}
		}
	}
}
