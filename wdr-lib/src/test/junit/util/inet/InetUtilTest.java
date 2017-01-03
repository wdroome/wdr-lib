package test.junit.util.inet;

import static org.junit.Assert.*;

import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import com.wdroome.util.inet.InetUtil;

/**
 * @author wdr
 *
 */
public class InetUtilTest
{
	@Test
	public void testAddrCmp() throws UnknownHostException
	{
		InetAddress a = InetAddress.getByName("1.2.3.4");
		InetAddress b = InetAddress.getByName("1.2.3.5");
		assertEquals(a.toString() + "<>" + b.toString(), -1, InetUtil.compare(a,b));
		assertEquals(b.toString() + "<>" + a.toString(),  1, InetUtil.compare(b,a));
		
		b = InetAddress.getByName("1.2.3.4");
		assertEquals(a.toString() + "<>" + b.toString(), 0, InetUtil.compare(a,b));
		
		a = InetAddress.getByName("255.0.0.0");
		b = InetAddress.getByName("0.0.0.0");
		assertEquals(a.toString() + "<>" + b.toString(), 1, InetUtil.compare(a,b));
		
		a = InetAddress.getByName("::0");
		assertEquals(a.toString() + "<>" + b.toString(), 1, InetUtil.compare(a,b));
	}

	@Test
	public void testSockAddrCmp() throws UnknownHostException
	{
		InetSocketAddress a = new InetSocketAddress("1.2.3.4", 80);
		InetSocketAddress b = new InetSocketAddress("1.2.3.5", 80);
		InetSocketAddress c = new InetSocketAddress("1.2.3.4", 81);
		assertEquals(a.toString() + "<>" + b.toString(), -1, InetUtil.compare(a,b));
		assertEquals(b.toString() + "<>" + a.toString(),  1, InetUtil.compare(b,a));
		assertEquals(a.toString() + "<>" + c.toString(), -1, InetUtil.compare(a,c));
		assertEquals(b.toString() + "<>" + c.toString(),  1, InetUtil.compare(b,c));
		
		b = new InetSocketAddress("1.2.3.4", 80);
		assertEquals(a.toString() + "<>" + b.toString(), 0, InetUtil.compare(a,b));
	}

	@Test
	public void testParseAddrPort() throws NumberFormatException, UnknownHostException
	{
		InetSocketAddress a = new InetSocketAddress("1.2.3.4", 80);
		InetSocketAddress b = InetUtil.parseAddrPort("1.2.3.4:80");
		InetSocketAddress c = InetUtil.parseAddrPort("1.2.3.4", 80);
		assertEquals(a, b);
		assertEquals(a, c);
		
		a = new InetSocketAddress("1::2", 80);
		b = InetUtil.parseAddrPort("1::2:80");
		assertEquals(a, b);
	}
	
	@Test
	public void testToAddrPort()
	{
		assertEquals("1.2.3.4:80", InetUtil.toAddrPort(new InetSocketAddress("1.2.3.4", 80)));		
		assertEquals("0:0:0:0:0:0:0:0:80", InetUtil.toAddrPort(new InetSocketAddress("::0", 80)));		
	}
}
