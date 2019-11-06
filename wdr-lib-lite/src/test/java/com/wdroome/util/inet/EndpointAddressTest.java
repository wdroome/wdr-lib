package com.wdroome.util.inet;

import static org.junit.Assert.*;

import java.net.UnknownHostException;
import java.net.InetAddress;
import java.util.Arrays;

import org.junit.Test;

import com.wdroome.util.inet.EndpointAddress;
import com.wdroome.util.inet.UnknownAddressTypeException;
import com.wdroome.util.ByteAOL;

/**
 * @author wdr
 *
 */
public class EndpointAddressTest
{

	/**
	 * Test method for {@link EndpointAddress#EndpointAddress(java.lang.String)}.
	 * @throws UnknownHostException 
	 */
	@Test
	public void testEndpointAddressString() throws UnknownHostException
	{
		String[] addrStrs = new String[] {
			"127.0.0.1", null,
			"1.2.3.4", null,
			"::", null,
			"::0", "::",
			"::1", null,
			"0:0:0::0", "::",
			"FFEE:1234:4567::", "ffee:1234:4567::",
			"FFEE:1234:4567::0", "ffee:1234:4567::",
			"1234:0:0", "1234::",
			"1:2:3:4:5:6:7:8", null,
			"1:2:3:4:5::8", null,
			"1:2:3:4:5::0", "1:2:3:4:5::",
			"1:2:3:4::0:8", "1:2:3:4::8",
			"1:2:3:4::1:0", null,
			"1:2:3:4:5:6:7:0", null,
			"1:2:3:4:5:6:0:0", "1:2:3:4:5:6::",
			"ffee::1", null,
			"FFEE::0", "ffee::",
			"ffee::", null,
			"1:2:3:4:5:6:7.8.9.10", "1:2:3:4:5:6:708:90a",
			"::ffff:1.2.3.4", null,
			"FFEE::1.2.3.4", "ffee::102:304",
			"FFEE::6:12.34.56.78", "ffee::6:c22:384e",
			"ipv4:1.2.3.4", "1.2.3.4",
			"ipv6:ABCD::", "abcd::",
			"ipv6:::0", "::",
			"1:2:0:0:3:0:0:0", "1:2:0:0:3::",
			"1:2:0:0:3:0:0:1", "1:2::3:0:0:1",
			"0:0:0:1:2:0:0:0", "::1:2:0:0:0",
			"0:0:0:1:0:0:0:0", "0:0:0:1::",
			"01:23:45:67:89:ab", null,
			"mac:01:23:45:67:89:ab", "01:23:45:67:89:ab",
			"01-23-45-67-89-ab", "01:23:45:67:89:ab",
			"mac:0-1-2-3-4-5", "00:01:02:03:04:05",
		};
		for (int i = 0; i < addrStrs.length; i += 2) {
			EndpointAddress addr = new EndpointAddress(addrStrs[i]);
			String expected = addrStrs[i+1];
			if (expected == null)
				expected = addrStrs[i];
			assertEquals(expected, addr.toString());
		}
		String[] v4AddrStrs = new String[] {
				"127.0.0.1", "ipv4:127.0.0.1",
				"1.2.3.4", "ipv4:1.2.3.4",
				"ipv6:::0", "ipv6:::",
				"ipv6:1::0", "ipv6:1::",
				"ipv4:4.3.2.1", "ipv4:4.3.2.1",
		};
		for (int i = 0; i < v4AddrStrs.length; i += 2) {
			EndpointAddress addr = new EndpointAddress(v4AddrStrs[i], EndpointAddress.IPV4_PREFIX);
			String expected = v4AddrStrs[i+1];
			if (expected == null)
				expected = addrStrs[i];
			assertEquals(expected, addr.toIPAddrWithPrefix());
		}
		String[] v6AddrStrs = new String[] {
				"a:b::0", "ipv6:a:b::",
				"1:2:3:4:5:6:7:8", "ipv6:1:2:3:4:5:6:7:8",
				"ipv6:::0", "ipv6:::",
				"ipv6:1::0", "ipv6:1::",
				"ipv4:4.3.2.1", "ipv4:4.3.2.1",
		};
		for (int i = 0; i < v6AddrStrs.length; i += 2) {
			EndpointAddress addr = new EndpointAddress(v6AddrStrs[i], EndpointAddress.IPV6_PREFIX);
			String expected = v6AddrStrs[i+1];
			if (expected == null)
				expected = addrStrs[i];
			assertEquals(expected, addr.toIPAddrWithPrefix());
		}
		String[] macAddrStrs = new String[] {
				"01:23:45:67:89:ab", "mac:01:23:45:67:89:ab",
				"01-23-45-67-89-ab", "mac:01:23:45:67:89:ab",
				"0-1-2-3-4-5", "mac:00:01:02:03:04:05",
				"mac:0-1-2-3-4-5", "mac:00:01:02:03:04:05",
				"mac:0:1:2:3:4:5", "mac:00:01:02:03:04:05",
		};
		for (int i = 0; i < macAddrStrs.length; i += 2) {
			EndpointAddress addr = new EndpointAddress(macAddrStrs[i], EndpointAddress.MAC_PREFIX);
			String expected = macAddrStrs[i+1];
			if (expected == null)
				expected = addrStrs[i];
			assertEquals(expected, addr.toIPAddrWithPrefix());
		}
	}
	
	@Test
	public void testSorting() throws UnknownHostException
	{
		String[] addrStrs = new String[] {
				"127.0.0.1",
				"4.3.2.1",
				"1.2.3.4",
				"::0",
				"::",
				"0:0:0::0",
				"FFEE:1234:4567::",
				"FFEE:1234:4567::0",
				"1.1.1.1",
				"1234:0:0",
				"1:2:3:4:5:6:7:8",
				"1:2:3:4:5::8",
				"1:2:3:4:5::0",
				"1.2.3.1",
				"0.1.2.3",
				"1:2:3:4::0:8",
				"1:2:3:4::1:0",
				"1:2:3:4:5:6:7:0",
				"1:2:3:4:5:6:0:0",
				"FFEE::1",
				"FFEE::0",
				"FFEE::",
				"1:2:3:4:5:6:7.8.9.10",
				"FFEE::1.2.3.4",
				"FFEE::6:12.34.56.78",
				"ipv4:1.2.3.4",
				"ipv6:ABCD::",
				"ipv6:::0",
				"1:2:0:0:3:0:0:0",
				"1:2:0:0:3:0:0:1",
				"0:0:0:1:2:0:0:0",
				"0:0:0:1:0:0:0:0",
			};
		EndpointAddress[] addrs = new EndpointAddress[addrStrs.length];
		for (int i = 0; i < addrStrs.length; i++)
			addrs[i] = new EndpointAddress(addrStrs[i]);
		Arrays.sort(addrs);
		String[] actualSortedAddrStrs = new String[addrStrs.length];
		for (int i = 0; i < addrStrs.length; i++)
			actualSortedAddrStrs[i] = addrs[i].toString();
		// for (String s:actualSortedAddrStrs)
		//	System.out.println("\"" + s + "\",");
		
		String[] expectedSortedAddrStrs = new String[] {
				"0.1.2.3",
				"1.1.1.1",
				"1.2.3.1",
				"1.2.3.4",
				"1.2.3.4",
				"4.3.2.1",
				"127.0.0.1",
				"::",
				"::",
				"::",
				"::",
				"0:0:0:1::",
				"::1:2:0:0:0",
				"1:2:0:0:3::",
				"1:2::3:0:0:1",
				"1:2:3:4::8",
				"1:2:3:4::1:0",
				"1:2:3:4:5::",
				"1:2:3:4:5::8",
				"1:2:3:4:5:6::",
				"1:2:3:4:5:6:7:0",
				"1:2:3:4:5:6:7:8",
				"1:2:3:4:5:6:708:90a",
				"1234::",
				"abcd::",
				"ffee::",
				"ffee::",
				"ffee::1",
				"ffee::102:304",
				"ffee::6:c22:384e",
				"ffee:1234:4567::",
				"ffee:1234:4567::",
			};
		assertArrayEquals(expectedSortedAddrStrs, actualSortedAddrStrs);
	}
	
	@Test
	public void testToInetAddress() throws UnknownHostException
	{
		String[] addrStrs = new String[] {
				"1.2.3.4",
				"4.3.2.1",
				"1:2:3:4:5:6:7:8",
				"f:e:d:c:b:a:9:8",
			};
		for (String a:addrStrs) {
			EndpointAddress addr = new EndpointAddress(a);
			InetAddress inet = addr.toInetAddress();
			assertEquals("/" + a, inet.toString());
		}
	}
	
	@Test(expected=java.net.UnknownHostException.class)
	public void testError1() throws UnknownHostException
	{
		new EndpointAddress("1.2.3.");
	}
	
	@Test(expected=java.net.UnknownHostException.class)
	public void testError2() throws UnknownHostException
	{
		new EndpointAddress("1.2.3.4.5");
	}
	
	@Test(expected=java.net.UnknownHostException.class)
	public void testError3() throws UnknownHostException
	{
		new EndpointAddress("1.2.3.a");
	}
	
	@Test(expected=java.net.UnknownHostException.class)
	public void testError4() throws UnknownHostException
	{
		new EndpointAddress("f:0");
	}
	
	@Test(expected=java.net.UnknownHostException.class)
	public void testError5() throws UnknownHostException
	{
		new EndpointAddress("0:1:2:3:4:5:6:7:8");
	}
	
	@Test(expected=java.net.UnknownHostException.class)
	public void testError6() throws UnknownHostException
	{
		new EndpointAddress("ipv4:0:1:2:3:4:5:6:7:8");
	}
	
	@Test(expected=java.net.UnknownHostException.class)
	public void testError7() throws UnknownHostException
	{
		new EndpointAddress("ipv6:1.2.3.4");
	}
	
	@Test(expected=UnknownAddressTypeException.class)
	public void testError8a() throws UnknownHostException
	{
		new EndpointAddress("ipvx:1.2.3.4");
	}
	
	@Test(expected=UnknownAddressTypeException.class)
	public void testError8b() throws UnknownHostException
	{
		new EndpointAddress("1.2.3.4", "ipvx");
	}

	@Test(expected=UnknownAddressTypeException.class)
	public void testError8c() throws UnknownHostException
	{
		new EndpointAddress("abcde:1.2.3.4");
	}

	@Test(expected=UnknownAddressTypeException.class)
	public void testError8d() throws UnknownHostException
	{
		new EndpointAddress("1.2.3.4", "abcd");
	}

	@Test(expected=java.net.UnknownHostException.class)
	public void testError8e() throws UnknownHostException
	{
		new EndpointAddress("abcd:1.2.3.4");
	}
	
	@Test(expected=java.net.UnknownHostException.class)
	public void testError9() throws UnknownHostException
	{
		new EndpointAddress("1.");
	}
	
	@Test(expected=java.net.UnknownHostException.class)
	public void testError10() throws UnknownHostException
	{
		new EndpointAddress("1.2.");
	}

	/**
	 * Test method for {@link EndpointAddress#compareTo(com.wdroome.util.ByteAOL)}.
	 */
	// @Test
	public void testCompareToByteAOL()
	{
		fail("Not yet implemented");
	}
}
