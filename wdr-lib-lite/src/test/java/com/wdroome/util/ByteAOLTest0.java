package com.wdroome.util;

import static org.junit.Assert.*;

import java.net.UnknownHostException;

import org.junit.Test;

/**
 * @author wdr
 *
 */
public class ByteAOLTest0
{

	/**
	 * Test method for {@link ByteAOL#ByteAOL(java.lang.String,java.io.UnknownHostException)}.
	 * @throws UnknownHostException 
	 */
	@Test
	public void testByteAOL_IPAddr() throws UnknownHostException
	{
		String[] addrStrs = new String[] {
			"127.0.0.1", null,
			"1.2.3.4", null,
			"::0", null,
			"::f", null,
			"::", "::0",
			"0::0", "::0",
			"0:0:0::0", "::0",
			"1::", null,
			"ffee:1234:4567::", null,
			"ffee:1234:4567::0", "ffee:1234:4567::",
			"0123:4567:89ab:cdef:fedc:ba98:7654:3210", "123:4567:89ab:cdef:fedc:ba98:7654:3210",
			"0123:4567:89AB:CDEF:FEDC:BA98:7654:3210", "123:4567:89ab:cdef:fedc:ba98:7654:3210",
			"1234:0:0", "1234::",
			"1:2:3:4:5:6:7:8", null,
			"1:2:3:4:5::8", null,
			"1:2:3:4:5::0", "1:2:3:4:5::",
			"1:2:3:4::0:8", "1:2:3:4::8",
			"1:2:3:4::1:0", null,
			"ffee::1", null,
			"ffee::0", "ffee::",
			"ffee::", null,
			"1:2:3:4:5:6:7.8.9.10", "1:2:3:4:5:6:708:90a",
			"ffee::1.2.3.4", "ffee::102:304",
			"ffee::6:12.34.56.78", "ffee::6:c22:384e",
		};
		for (int i = 0; i < addrStrs.length; i += 2) {
			ByteAOL addr = new ByteAOL(addrStrs[i], (UnknownHostException)null);
			String expected = addrStrs[i+1];
			if (expected == null)
				expected = addrStrs[i];
			assertEquals(expected, addr.toIPAddr());
			ByteAOL addr2 = new ByteAOL(addrStrs[i], ByteAOL.IPADDR_STR);
			assertEquals(addrStrs[i], addr, addr2);
		}
	}
	
	@Test(expected=java.net.UnknownHostException.class)
	public void testErrorA1() throws UnknownHostException
	{
		new ByteAOL("1.2.3.", (UnknownHostException)null);
	}
	
	@Test(expected=java.net.UnknownHostException.class)
	public void testErrorA2() throws UnknownHostException
	{
		new ByteAOL("1.2.3.4.5", (UnknownHostException)null);
	}
	
	@Test(expected=java.net.UnknownHostException.class)
	public void testErrorA3() throws UnknownHostException
	{
		new ByteAOL("1.2.3.a", (UnknownHostException)null);
	}
	
	@Test(expected=java.net.UnknownHostException.class)
	public void testErrorA4() throws UnknownHostException
	{
		new ByteAOL("f:0", (UnknownHostException)null);
	}
	
	@Test(expected=java.net.UnknownHostException.class)
	public void testErrorA5() throws UnknownHostException
	{
		new ByteAOL("0:1:2:3:4:5:6:7:8", (UnknownHostException)null);
	}
	
	@Test(expected=java.net.UnknownHostException.class)
	public void testErrorA6() throws UnknownHostException
	{
		new ByteAOL("localhost", (UnknownHostException)null);
	}
	
	@Test(expected=java.net.UnknownHostException.class)
	public void testErrorA7() throws UnknownHostException
	{
		new ByteAOL("www.yahoo.com", (UnknownHostException)null);
	}
		
	@Test(expected=java.net.UnknownHostException.class)
	public void testErrorA8() throws UnknownHostException
	{
		new ByteAOL("::g", (UnknownHostException)null);
	}

	@Test(expected=java.lang.NumberFormatException.class)
	public void testErrorB1()
	{
		new ByteAOL("1.2.3.", ByteAOL.IPADDR_STR);
	}
	
	@Test(expected=java.lang.NumberFormatException.class)
	public void testErrorB2()
	{
		new ByteAOL("1.2.3.4.5", ByteAOL.IPADDR_STR);
	}
	
	@Test(expected=java.lang.NumberFormatException.class)
	public void testErrorB3()
	{
		new ByteAOL("1.2.3.a", ByteAOL.IPADDR_STR);
	}
	
	@Test(expected=java.lang.NumberFormatException.class)
	public void testErrorB4()
	{
		new ByteAOL("f:0", ByteAOL.IPADDR_STR);
	}
	
	@Test(expected=java.lang.NumberFormatException.class)
	public void testErrorB5()
	{
		new ByteAOL("0:1:2:3:4:5:6:7:8", ByteAOL.IPADDR_STR);
	}
	
	@Test(expected=java.lang.NumberFormatException.class)
	public void testErrorB6()
	{
		new ByteAOL("localhost", ByteAOL.IPADDR_STR);
	}
	
	@Test(expected=java.lang.NumberFormatException.class)
	public void testErrorB7()
	{
		new ByteAOL("www.yahoo.com", ByteAOL.IPADDR_STR);
	}
	
	@Test(expected=java.net.UnknownHostException.class)
	public void testErrorB8() throws UnknownHostException
	{
		new ByteAOL("::g", (UnknownHostException)null);
	}

	@Test(expected=java.net.UnknownHostException.class)
	public void testErrorB9() throws UnknownHostException
	{
		new ByteAOL("1:2:3:4:5:6:7.8.9.a", (UnknownHostException)null);
	}

	@Test(expected=java.net.UnknownHostException.class)
	public void testErrorB10() throws UnknownHostException
	{
		new ByteAOL("1:2:3:4:5:6:60:7.8.9.a", (UnknownHostException)null);
	}
}
