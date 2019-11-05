package test.junit.util.inet;

import java.net.*;
import java.util.*;

import com.wdroome.util.inet.CIDRAddress;
import com.wdroome.util.inet.EndpointAddress;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Test methods for {@link CIDRAddress}.
 * @author wdr
 */
public class CIDRAddressTest
{

	@Test(expected=java.net.UnknownHostException.class)
	public void testCIDRAddress_Err1() throws UnknownHostException
	{
		new CIDRAddress("1.2.3.4");
	}

	@Test(expected=java.net.UnknownHostException.class)
	public void testCIDRAddress_Err2() throws UnknownHostException
	{
		new CIDRAddress("1.2.3.4/12a");
	}

	@Test(expected=java.net.UnknownHostException.class)
	public void testCIDRAddress_Err3() throws UnknownHostException
	{
		new CIDRAddress("1.2.3.4/33");
	}

	@Test(expected=java.net.UnknownHostException.class)
	public void testCIDRAddress_Err4() throws UnknownHostException
	{
		new CIDRAddress("1.2.3.4/-1");
	}

	@Test(expected=java.net.UnknownHostException.class)
	public void testCIDRAddress_Err5() throws UnknownHostException
	{
		new CIDRAddress("localhost/32");
	}

	@Test(expected=java.net.UnknownHostException.class)
	public void testCIDRAddress_Err6() throws UnknownHostException
	{
		new CIDRAddress("1.2.3.4 5/12");
	}

	@Test(expected=java.net.UnknownHostException.class)
	public void testCIDRAddress_Err7() throws UnknownHostException
	{
		new CIDRAddress("1.2.3.4\\/12");
	}

	/**
	 * Test method for {@link CIDRAddress#CIDRAddress(java.lang.String)}.
	 * @throws UnknownHostException 
	 */
	@Test
	public void testCIDRAddress() throws UnknownHostException
	{
		String[] addrs = new String[] {"255.255.255.255", "0.0.0.0", "ffee::"};
		String[] actualDetails = new String[addrs.length*33];
		String[] actualHash = new String[addrs.length*33];
		int i = 0;
		for (String addr: addrs) {
			for (int len = 0; len <= 32; len++) {
				CIDRAddress cidr = new CIDRAddress(addr + "/" + len);
				actualDetails[i] = cidr.toDetailedString();
				actualHash[i] = cidr.toString() + " hash=" + Integer.toHexString(cidr.hashCode());
				i++;
			}
		}
		String[] expectedDetails = new String[] {
				"0.0.0.0/0 bytes=[0,0,0,0] mask=[]",
				"128.0.0.0/1 bytes=[80,0,0,0] mask=[80]",
				"192.0.0.0/2 bytes=[c0,0,0,0] mask=[c0]",
				"224.0.0.0/3 bytes=[e0,0,0,0] mask=[e0]",
				"240.0.0.0/4 bytes=[f0,0,0,0] mask=[f0]",
				"248.0.0.0/5 bytes=[f8,0,0,0] mask=[f8]",
				"252.0.0.0/6 bytes=[fc,0,0,0] mask=[fc]",
				"254.0.0.0/7 bytes=[fe,0,0,0] mask=[fe]",
				"255.0.0.0/8 bytes=[ff,0,0,0] mask=[ff]",
				"255.128.0.0/9 bytes=[ff,80,0,0] mask=[ff,80]",
				"255.192.0.0/10 bytes=[ff,c0,0,0] mask=[ff,c0]",
				"255.224.0.0/11 bytes=[ff,e0,0,0] mask=[ff,e0]",
				"255.240.0.0/12 bytes=[ff,f0,0,0] mask=[ff,f0]",
				"255.248.0.0/13 bytes=[ff,f8,0,0] mask=[ff,f8]",
				"255.252.0.0/14 bytes=[ff,fc,0,0] mask=[ff,fc]",
				"255.254.0.0/15 bytes=[ff,fe,0,0] mask=[ff,fe]",
				"255.255.0.0/16 bytes=[ff,ff,0,0] mask=[ff,ff]",
				"255.255.128.0/17 bytes=[ff,ff,80,0] mask=[ff,ff,80]",
				"255.255.192.0/18 bytes=[ff,ff,c0,0] mask=[ff,ff,c0]",
				"255.255.224.0/19 bytes=[ff,ff,e0,0] mask=[ff,ff,e0]",
				"255.255.240.0/20 bytes=[ff,ff,f0,0] mask=[ff,ff,f0]",
				"255.255.248.0/21 bytes=[ff,ff,f8,0] mask=[ff,ff,f8]",
				"255.255.252.0/22 bytes=[ff,ff,fc,0] mask=[ff,ff,fc]",
				"255.255.254.0/23 bytes=[ff,ff,fe,0] mask=[ff,ff,fe]",
				"255.255.255.0/24 bytes=[ff,ff,ff,0] mask=[ff,ff,ff]",
				"255.255.255.128/25 bytes=[ff,ff,ff,80] mask=[ff,ff,ff,80]",
				"255.255.255.192/26 bytes=[ff,ff,ff,c0] mask=[ff,ff,ff,c0]",
				"255.255.255.224/27 bytes=[ff,ff,ff,e0] mask=[ff,ff,ff,e0]",
				"255.255.255.240/28 bytes=[ff,ff,ff,f0] mask=[ff,ff,ff,f0]",
				"255.255.255.248/29 bytes=[ff,ff,ff,f8] mask=[ff,ff,ff,f8]",
				"255.255.255.252/30 bytes=[ff,ff,ff,fc] mask=[ff,ff,ff,fc]",
				"255.255.255.254/31 bytes=[ff,ff,ff,fe] mask=[ff,ff,ff,fe]",
				"255.255.255.255/32 bytes=[ff,ff,ff,ff] mask=[ff,ff,ff,ff]",
				"0.0.0.0/0 bytes=[0,0,0,0] mask=[]",
				"0.0.0.0/1 bytes=[0,0,0,0] mask=[80]",
				"0.0.0.0/2 bytes=[0,0,0,0] mask=[c0]",
				"0.0.0.0/3 bytes=[0,0,0,0] mask=[e0]",
				"0.0.0.0/4 bytes=[0,0,0,0] mask=[f0]",
				"0.0.0.0/5 bytes=[0,0,0,0] mask=[f8]",
				"0.0.0.0/6 bytes=[0,0,0,0] mask=[fc]",
				"0.0.0.0/7 bytes=[0,0,0,0] mask=[fe]",
				"0.0.0.0/8 bytes=[0,0,0,0] mask=[ff]",
				"0.0.0.0/9 bytes=[0,0,0,0] mask=[ff,80]",
				"0.0.0.0/10 bytes=[0,0,0,0] mask=[ff,c0]",
				"0.0.0.0/11 bytes=[0,0,0,0] mask=[ff,e0]",
				"0.0.0.0/12 bytes=[0,0,0,0] mask=[ff,f0]",
				"0.0.0.0/13 bytes=[0,0,0,0] mask=[ff,f8]",
				"0.0.0.0/14 bytes=[0,0,0,0] mask=[ff,fc]",
				"0.0.0.0/15 bytes=[0,0,0,0] mask=[ff,fe]",
				"0.0.0.0/16 bytes=[0,0,0,0] mask=[ff,ff]",
				"0.0.0.0/17 bytes=[0,0,0,0] mask=[ff,ff,80]",
				"0.0.0.0/18 bytes=[0,0,0,0] mask=[ff,ff,c0]",
				"0.0.0.0/19 bytes=[0,0,0,0] mask=[ff,ff,e0]",
				"0.0.0.0/20 bytes=[0,0,0,0] mask=[ff,ff,f0]",
				"0.0.0.0/21 bytes=[0,0,0,0] mask=[ff,ff,f8]",
				"0.0.0.0/22 bytes=[0,0,0,0] mask=[ff,ff,fc]",
				"0.0.0.0/23 bytes=[0,0,0,0] mask=[ff,ff,fe]",
				"0.0.0.0/24 bytes=[0,0,0,0] mask=[ff,ff,ff]",
				"0.0.0.0/25 bytes=[0,0,0,0] mask=[ff,ff,ff,80]",
				"0.0.0.0/26 bytes=[0,0,0,0] mask=[ff,ff,ff,c0]",
				"0.0.0.0/27 bytes=[0,0,0,0] mask=[ff,ff,ff,e0]",
				"0.0.0.0/28 bytes=[0,0,0,0] mask=[ff,ff,ff,f0]",
				"0.0.0.0/29 bytes=[0,0,0,0] mask=[ff,ff,ff,f8]",
				"0.0.0.0/30 bytes=[0,0,0,0] mask=[ff,ff,ff,fc]",
				"0.0.0.0/31 bytes=[0,0,0,0] mask=[ff,ff,ff,fe]",
				"0.0.0.0/32 bytes=[0,0,0,0] mask=[ff,ff,ff,ff]",
				"::/0 bytes=[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0] mask=[]",
				"8000::/1 bytes=[80,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0] mask=[80]",
				"c000::/2 bytes=[c0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0] mask=[c0]",
				"e000::/3 bytes=[e0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0] mask=[e0]",
				"f000::/4 bytes=[f0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0] mask=[f0]",
				"f800::/5 bytes=[f8,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0] mask=[f8]",
				"fc00::/6 bytes=[fc,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0] mask=[fc]",
				"fe00::/7 bytes=[fe,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0] mask=[fe]",
				"ff00::/8 bytes=[ff,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0] mask=[ff]",
				"ff80::/9 bytes=[ff,80,0,0,0,0,0,0,0,0,0,0,0,0,0,0] mask=[ff,80]",
				"ffc0::/10 bytes=[ff,c0,0,0,0,0,0,0,0,0,0,0,0,0,0,0] mask=[ff,c0]",
				"ffe0::/11 bytes=[ff,e0,0,0,0,0,0,0,0,0,0,0,0,0,0,0] mask=[ff,e0]",
				"ffe0::/12 bytes=[ff,e0,0,0,0,0,0,0,0,0,0,0,0,0,0,0] mask=[ff,f0]",
				"ffe8::/13 bytes=[ff,e8,0,0,0,0,0,0,0,0,0,0,0,0,0,0] mask=[ff,f8]",
				"ffec::/14 bytes=[ff,ec,0,0,0,0,0,0,0,0,0,0,0,0,0,0] mask=[ff,fc]",
				"ffee::/15 bytes=[ff,ee,0,0,0,0,0,0,0,0,0,0,0,0,0,0] mask=[ff,fe]",
				"ffee::/16 bytes=[ff,ee,0,0,0,0,0,0,0,0,0,0,0,0,0,0] mask=[ff,ff]",
				"ffee::/17 bytes=[ff,ee,0,0,0,0,0,0,0,0,0,0,0,0,0,0] mask=[ff,ff,80]",
				"ffee::/18 bytes=[ff,ee,0,0,0,0,0,0,0,0,0,0,0,0,0,0] mask=[ff,ff,c0]",
				"ffee::/19 bytes=[ff,ee,0,0,0,0,0,0,0,0,0,0,0,0,0,0] mask=[ff,ff,e0]",
				"ffee::/20 bytes=[ff,ee,0,0,0,0,0,0,0,0,0,0,0,0,0,0] mask=[ff,ff,f0]",
				"ffee::/21 bytes=[ff,ee,0,0,0,0,0,0,0,0,0,0,0,0,0,0] mask=[ff,ff,f8]",
				"ffee::/22 bytes=[ff,ee,0,0,0,0,0,0,0,0,0,0,0,0,0,0] mask=[ff,ff,fc]",
				"ffee::/23 bytes=[ff,ee,0,0,0,0,0,0,0,0,0,0,0,0,0,0] mask=[ff,ff,fe]",
				"ffee::/24 bytes=[ff,ee,0,0,0,0,0,0,0,0,0,0,0,0,0,0] mask=[ff,ff,ff]",
				"ffee::/25 bytes=[ff,ee,0,0,0,0,0,0,0,0,0,0,0,0,0,0] mask=[ff,ff,ff,80]",
				"ffee::/26 bytes=[ff,ee,0,0,0,0,0,0,0,0,0,0,0,0,0,0] mask=[ff,ff,ff,c0]",
				"ffee::/27 bytes=[ff,ee,0,0,0,0,0,0,0,0,0,0,0,0,0,0] mask=[ff,ff,ff,e0]",
				"ffee::/28 bytes=[ff,ee,0,0,0,0,0,0,0,0,0,0,0,0,0,0] mask=[ff,ff,ff,f0]",
				"ffee::/29 bytes=[ff,ee,0,0,0,0,0,0,0,0,0,0,0,0,0,0] mask=[ff,ff,ff,f8]",
				"ffee::/30 bytes=[ff,ee,0,0,0,0,0,0,0,0,0,0,0,0,0,0] mask=[ff,ff,ff,fc]",
				"ffee::/31 bytes=[ff,ee,0,0,0,0,0,0,0,0,0,0,0,0,0,0] mask=[ff,ff,ff,fe]",
				"ffee::/32 bytes=[ff,ee,0,0,0,0,0,0,0,0,0,0,0,0,0,0] mask=[ff,ff,ff,ff]",
			};
		String[] expectedHash = new String[] {
				"0.0.0.0/0 hash=1b4dc60",
				"128.0.0.0/1 hash=faa91be1",
				"192.0.0.0/2 hash=fe2efc22",
				"224.0.0.0/3 hash=fff1ec43",
				"240.0.0.0/4 hash=d36454",
				"248.0.0.0/5 hash=144205d",
				"252.0.0.0/6 hash=17c7e62",
				"254.0.0.0/7 hash=198ad65",
				"255.0.0.0/8 hash=1a6c4e7",
				"255.128.0.0/9 hash=16c9568",
				"255.192.0.0/10 hash=189ad29",
				"255.224.0.0/11 hash=198390a",
				"255.240.0.0/12 hash=19f7efb",
				"255.248.0.0/13 hash=1a321f4",
				"255.252.0.0/14 hash=1a4f371",
				"255.254.0.0/15 hash=1a5dc30",
				"255.255.0.0/16 hash=1a65090",
				"255.255.128.0/17 hash=1a47011",
				"255.255.192.0/18 hash=1a56052",
				"255.255.224.0/19 hash=1a5d873",
				"255.255.240.0/20 hash=1a61484",
				"255.255.248.0/21 hash=1a6328d",
				"255.255.252.0/22 hash=1a64192",
				"255.255.254.0/23 hash=1a64915",
				"255.255.255.0/24 hash=1a64cd7",
				"255.255.255.128/25 hash=1a63d58",
				"255.255.255.192/26 hash=1a64519",
				"255.255.255.224/27 hash=1a648fa",
				"255.255.255.240/28 hash=1a64aeb",
				"255.255.255.248/29 hash=1a64be4",
				"255.255.255.252/30 hash=1a64c61",
				"255.255.255.254/31 hash=1a64ca0",
				"255.255.255.255/32 hash=1a64cc0",
				"0.0.0.0/0 hash=1b4dc60",
				"0.0.0.0/1 hash=1b4dc61",
				"0.0.0.0/2 hash=1b4dc62",
				"0.0.0.0/3 hash=1b4dc63",
				"0.0.0.0/4 hash=1b4dc64",
				"0.0.0.0/5 hash=1b4dc65",
				"0.0.0.0/6 hash=1b4dc66",
				"0.0.0.0/7 hash=1b4dc67",
				"0.0.0.0/8 hash=1b4dc68",
				"0.0.0.0/9 hash=1b4dc69",
				"0.0.0.0/10 hash=1b4dc6a",
				"0.0.0.0/11 hash=1b4dc6b",
				"0.0.0.0/12 hash=1b4dc6c",
				"0.0.0.0/13 hash=1b4dc6d",
				"0.0.0.0/14 hash=1b4dc6e",
				"0.0.0.0/15 hash=1b4dc6f",
				"0.0.0.0/16 hash=1b4dc70",
				"0.0.0.0/17 hash=1b4dc71",
				"0.0.0.0/18 hash=1b4dc72",
				"0.0.0.0/19 hash=1b4dc73",
				"0.0.0.0/20 hash=1b4dc74",
				"0.0.0.0/21 hash=1b4dc75",
				"0.0.0.0/22 hash=1b4dc76",
				"0.0.0.0/23 hash=1b4dc77",
				"0.0.0.0/24 hash=1b4dc78",
				"0.0.0.0/25 hash=1b4dc79",
				"0.0.0.0/26 hash=1b4dc7a",
				"0.0.0.0/27 hash=1b4dc7b",
				"0.0.0.0/28 hash=1b4dc7c",
				"0.0.0.0/29 hash=1b4dc7d",
				"0.0.0.0/30 hash=1b4dc7e",
				"0.0.0.0/31 hash=1b4dc7f",
				"0.0.0.0/32 hash=1b4dc80",
				"::/0 hash=c491e5e0",
				"8000::/1 hash=6fa2e561",
				"c000::/2 hash=9a1a65a2",
				"e000::/3 hash=af5625c3",
				"f000::/4 hash=b9f405d4",
				"f800::/5 hash=3f42f5dd",
				"fc00::/6 hash=81ea6de2",
				"fe00::/7 hash=233e29e5",
				"ff00::/8 hash=73e807e7",
				"ff80::/9 hash=aaf91868",
				"ffc0::/10 hash=f709029",
				"ffe0::/11 hash=41ac4c0a",
				"ffe0::/12 hash=41ac4c0b",
				"ffe8::/13 hash=4e3b3b04",
				"ffec::/14 hash=d482b281",
				"ffee::/15 hash=97a66e40",
				"ffee::/16 hash=97a66e41",
				"ffee::/17 hash=97a66e42",
				"ffee::/18 hash=97a66e43",
				"ffee::/19 hash=97a66e44",
				"ffee::/20 hash=97a66e45",
				"ffee::/21 hash=97a66e46",
				"ffee::/22 hash=97a66e47",
				"ffee::/23 hash=97a66e48",
				"ffee::/24 hash=97a66e49",
				"ffee::/25 hash=97a66e4a",
				"ffee::/26 hash=97a66e4b",
				"ffee::/27 hash=97a66e4c",
				"ffee::/28 hash=97a66e4d",
				"ffee::/29 hash=97a66e4e",
				"ffee::/30 hash=97a66e4f",
				"ffee::/31 hash=97a66e50",
				"ffee::/32 hash=97a66e51",
			};
		assertArrayEquals("IPV4 details", expectedDetails, actualDetails);
		assertArrayEquals("IPV4 hash", expectedHash, actualHash);
	}

	/**
	 * Test method for {@link CIDRAddress(byte[], int)}.
	 * @throws UnknownHostException 
	 */
	@Test
	public void testFromInetAddress() throws UnknownHostException
	{
		byte[] addrBytes = {(byte)127, (byte)0, (byte)0, (byte)1};
		InetAddress inetAddr = InetAddress.getByAddress(addrBytes);
		CIDRAddress cidr = new CIDRAddress(inetAddr, 8);
		assertEquals("getAddr", "127.0.0.0", cidr.getAddr());
		assertEquals("toString", "127.0.0.0/8", cidr.toString());
		assertEquals("maskLen", 8, cidr.getMaskLen());
	}

	/**
	 * Test method for {@link CIDRAddress(String)}.
	 * @throws UnknownHostException 
	 */
	@Test
	public void testFromString() throws UnknownHostException
	{
		CIDRAddress cidr = new CIDRAddress("127.0.0.1/8");
		assertEquals("getAddr", "127.0.0.0", cidr.getAddr());
		assertEquals("toString", "127.0.0.0/8", cidr.toString());
		assertEquals("maskLen", 8, cidr.getMaskLen());
	}

	/**
	 * Test method for {@link CIDRAddress#equals(java.lang.Object)}.
	 * @throws UnknownHostException 
	 */
	@Test
	public void testEqualsObject() throws UnknownHostException
	{
		CIDRAddress a0 = new CIDRAddress("255.255.255.255/0");
		CIDRAddress a1 = new CIDRAddress("0.0.0.0/0");
		CIDRAddress a2 = new CIDRAddress("ffee::/0");
		CIDRAddress a3 = new CIDRAddress("0000::/0");
		assertEquals("IPV4/0a", true, a0.equals(a1));
		assertEquals("IPV4/0b", true, a1.equals(a0));
		assertEquals("IPV6/0a", true, a2.equals(a3));
		assertEquals("IPV6/0b", true, a3.equals(a2));
		assertEquals("IPV4/0 != IPV6/0a", false, a0.equals(a2));
		assertEquals("IPV4/0 != IPV6/0b", false, a2.equals(a0));
		
		CIDRAddress a4 = new CIDRAddress("1.2.128.0/24");
		CIDRAddress a5 = new CIDRAddress("1.2.128.4/24");
		CIDRAddress a6 = new CIDRAddress("1.2.128.0/23");
		assertEquals("1.2.128.0/24 == 1.2.128.4/24", true, a4.equals(a5));
		assertEquals("1.2.128.0/24 != 1.2.128.0/23", false, a4.equals(a6));
	}
	
	/**
	 * Test method for {@link CIDRAddress#contains(java.net.InetAddress)}.
	 * @throws UnknownHostException 
	 */
	@Test
	public void testMatches() throws UnknownHostException
	{
		EndpointAddress[] leadingOnes = new EndpointAddress[33];
		EndpointAddress[] leadingZeros = new EndpointAddress[33];
		for (int i = 0; i < 33; i++) {
			int x = (i == 0) ? 0 : (0x80000000 >> (i-1));
			leadingOnes[i] = new EndpointAddress(
									InetAddress.getByAddress(new byte[] {
										(byte)((x >> 24) & 0xff),
										(byte)((x >> 16) & 0xff),
										(byte)((x >>  8) & 0xff),
										(byte)((x      ) & 0xff)}
									));
			x = ~x;
			leadingZeros[i] = new EndpointAddress(
									InetAddress.getByAddress(new byte[] {
										(byte)((x >> 24) & 0xff),
										(byte)((x >> 16) & 0xff),
										(byte)((x >>  8) & 0xff),
										(byte)((x      ) & 0xff)}
									));
			// System.out.println(leadingOnes[i].toString() + " " + leadingZeros[i].toString());
		}
		
		for (int maskLen = 0; maskLen <= 32; maskLen++) {
			CIDRAddress cidr = new CIDRAddress("255.255.255.255/" + maskLen);
			for (int i = 0; i < 33; i++) {
				assertEquals("A: " + leadingOnes[i].toString() + " vs " + cidr.toString(),
									(i >= maskLen), leadingOnes[i].isContainedIn(cidr));
				assertEquals("B: " + leadingZeros[i].toString() + " vs " + cidr.toString(),
									(maskLen == 0 || i == 0), leadingZeros[i].isContainedIn(cidr));
			}
			cidr = new CIDRAddress("0.0.0.0/" + maskLen);
			for (int i = 0; i < 33; i++) {
				assertEquals("C: " + leadingZeros[i].toString() + " vs " + cidr.toString(),
									(i >= maskLen), leadingZeros[i].isContainedIn(cidr));
				assertEquals("D: " + leadingOnes[i].toString() + " vs " + cidr.toString(),
									(maskLen == 0 || i == 0), leadingOnes[i].isContainedIn(cidr));
			}
		}
	}
	
	/**
	 * Test method for {@link CIDRAddress#covers(CIDRAddress)}.
	 * @throws UnknownHostException 
	 */
	@Test
	public void testCovers() throws UnknownHostException
	{
		CIDRAddress[] leadingOnes = new CIDRAddress[33];
		CIDRAddress[] leadingZeros = new CIDRAddress[33];
		for (int i = 0; i < 33; i++) {
			int x = (i == 0) ? 0 : (0x80000000 >> (i-1));
			leadingOnes[i] = new CIDRAddress(
									new byte[] {
										(byte)((x >> 24) & 0xff),
										(byte)((x >> 16) & 0xff),
										(byte)((x >>  8) & 0xff),
										(byte)((x      ) & 0xff)}
									, i);
			x = ~x;
			leadingZeros[i] = new CIDRAddress(
									new byte[] {
										(byte)((x >> 24) & 0xff),
										(byte)((x >> 16) & 0xff),
										(byte)((x >>  8) & 0xff),
										(byte)((x      ) & 0xff)}
									, i);
			// System.out.println(leadingOnes[i].toString() + " " + leadingZeros[i].toString());
		}
		
		for (int maskLen = 0; maskLen <= 32; maskLen++) {
			CIDRAddress cidr = new CIDRAddress("255.255.255.255/" + maskLen);
			for (int i = 0; i < 33; i++) {
				assertEquals("A: " + leadingOnes[i].toString() + " vs " + cidr.toString(),
									(i <= maskLen), leadingOnes[i].covers(cidr));
				assertEquals("B: " + leadingZeros[i].toString() + " vs " + cidr.toString(),
									(i == 0), leadingZeros[i].covers(cidr));
			}
			cidr = new CIDRAddress("0.0.0.0/" + maskLen);
			for (int i = 0; i < 33; i++) {
				assertEquals("C: " + leadingZeros[i].toString() + " vs " + cidr.toString(),
									(i <= maskLen), leadingZeros[i].covers(cidr));
				assertEquals("D: " + leadingOnes[i].toString() + " vs " + cidr.toString(),
									(i == 0), leadingOnes[i].covers(cidr));
			}
		}
	}
	
	@Test
	public void testHashCode() throws UnknownHostException
	{
		String[] addrs = new String[] {"255.255.255.255", "0.0.0.0", "ffee::", "0000::"};
		HashSet<Integer> hashCodes = new HashSet<Integer>();
		for (String addr: addrs) {
			for (int len = 1; len <= 32; len++) {
				CIDRAddress cidr = new CIDRAddress(addr + "/" + len);
				if (!hashCodes.add(cidr.hashCode())) {
					fail("Duplicate hashCode " + cidr.hashCode()
							+ " for " + addr + "/" + len);
				}
			}
		}
		CIDRAddress a0 = new CIDRAddress("255.255.255.255/0");
		CIDRAddress a1 = new CIDRAddress("0.0.0.0/0");
		CIDRAddress a2 = new CIDRAddress("ffee::/0");
		CIDRAddress a3 = new CIDRAddress("ff80::/0");
		assertEquals("IPV4/0", a0.hashCode(), a1.hashCode());
		assertEquals("IPV6/0", a2.hashCode(), a3.hashCode());
		assertFalse("IPV4/0 != IPV6/0", a0.hashCode() == a2.hashCode());
	}

	@Test
	public void testComparable() throws UnknownHostException
	{
		String[] addrs = new String[] {"255.255.255.255", "0.0.0.0", "ffee::", "0000::"};
		CIDRAddress[] cidrs = new CIDRAddress[addrs.length*33];
		int i = 0;
		for (String addr: addrs) {
			for (int len = 0; len <= 32; len++) {
				cidrs[i++] = new CIDRAddress(addr + "/" + len);
			}
		}
		Arrays.sort(cidrs);
		String[] actual = new String[cidrs.length];
		for (i = 0; i < cidrs.length; i++) {
			actual[i] = cidrs[i].toString();
			// System.out.println(actual[i]);
		}
		String[] expected = {
				"0.0.0.0/32",
				"255.255.255.255/32",
				"::/32",
				"ffee::/32",
				"0.0.0.0/31",
				"255.255.255.254/31",
				"::/31",
				"ffee::/31",
				"0.0.0.0/30",
				"255.255.255.252/30",
				"::/30",
				"ffee::/30",
				"0.0.0.0/29",
				"255.255.255.248/29",
				"::/29",
				"ffee::/29",
				"0.0.0.0/28",
				"255.255.255.240/28",
				"::/28",
				"ffee::/28",
				"0.0.0.0/27",
				"255.255.255.224/27",
				"::/27",
				"ffee::/27",
				"0.0.0.0/26",
				"255.255.255.192/26",
				"::/26",
				"ffee::/26",
				"0.0.0.0/25",
				"255.255.255.128/25",
				"::/25",
				"ffee::/25",
				"0.0.0.0/24",
				"255.255.255.0/24",
				"::/24",
				"ffee::/24",
				"0.0.0.0/23",
				"255.255.254.0/23",
				"::/23",
				"ffee::/23",
				"0.0.0.0/22",
				"255.255.252.0/22",
				"::/22",
				"ffee::/22",
				"0.0.0.0/21",
				"255.255.248.0/21",
				"::/21",
				"ffee::/21",
				"0.0.0.0/20",
				"255.255.240.0/20",
				"::/20",
				"ffee::/20",
				"0.0.0.0/19",
				"255.255.224.0/19",
				"::/19",
				"ffee::/19",
				"0.0.0.0/18",
				"255.255.192.0/18",
				"::/18",
				"ffee::/18",
				"0.0.0.0/17",
				"255.255.128.0/17",
				"::/17",
				"ffee::/17",
				"0.0.0.0/16",
				"255.255.0.0/16",
				"::/16",
				"ffee::/16",
				"0.0.0.0/15",
				"255.254.0.0/15",
				"::/15",
				"ffee::/15",
				"0.0.0.0/14",
				"255.252.0.0/14",
				"::/14",
				"ffec::/14",
				"0.0.0.0/13",
				"255.248.0.0/13",
				"::/13",
				"ffe8::/13",
				"0.0.0.0/12",
				"255.240.0.0/12",
				"::/12",
				"ffe0::/12",
				"0.0.0.0/11",
				"255.224.0.0/11",
				"::/11",
				"ffe0::/11",
				"0.0.0.0/10",
				"255.192.0.0/10",
				"::/10",
				"ffc0::/10",
				"0.0.0.0/9",
				"255.128.0.0/9",
				"::/9",
				"ff80::/9",
				"0.0.0.0/8",
				"255.0.0.0/8",
				"::/8",
				"ff00::/8",
				"0.0.0.0/7",
				"254.0.0.0/7",
				"::/7",
				"fe00::/7",
				"0.0.0.0/6",
				"252.0.0.0/6",
				"::/6",
				"fc00::/6",
				"0.0.0.0/5",
				"248.0.0.0/5",
				"::/5",
				"f800::/5",
				"0.0.0.0/4",
				"240.0.0.0/4",
				"::/4",
				"f000::/4",
				"0.0.0.0/3",
				"224.0.0.0/3",
				"::/3",
				"e000::/3",
				"0.0.0.0/2",
				"192.0.0.0/2",
				"::/2",
				"c000::/2",
				"0.0.0.0/1",
				"128.0.0.0/1",
				"::/1",
				"8000::/1",
				"0.0.0.0/0",
				"0.0.0.0/0",
				"::/0",
				"::/0",
				};
		assertArrayEquals(expected, actual);
	}

	public static void main(String[] args) throws Exception
	{
		TreeSet<CIDRAddress> cidrSet = new TreeSet<CIDRAddress>();
		String[] addrs = new String[] {"255.255.255.255", "0.0.0.0", "ffee::", "0000::"};
		// addrs = new String[] {"192.128.2.4", "255.255.255.255", "192.1.2.4", "ffee::"};
		for (String addr: addrs) {
			for (int len = 0; len <= 32; len++) {
				CIDRAddress cidr = new CIDRAddress(addr + "/" + len);
				cidrSet.add(cidr);
				System.out.println(cidr.toDetailedString()
						+ " hash=" + Integer.toHexString(cidr.hashCode()));
			}
		}
		System.out.println("Addr set size = " + cidrSet.size());
		for (CIDRAddress cidr:cidrSet) {
			System.out.println("  " + cidr.toString());
		}
		
		CIDRAddress cidr = new CIDRAddress("1.2.3.0/20");
		for (String addr: new String[] {"192.1.2.4", "1.2.2.2", "1.2.3.4", "192.1.2.4", "ffee::"}) {
			System.out.println(addr + " matches " + cidr.toString() + ": "
								+ cidr.contains(addr));
		}
	}
}
