package test.junit.util.inet;

import static org.junit.Assert.*;

import java.net.UnknownHostException;
import java.util.Arrays;

import org.junit.Test;

import com.wdroome.util.inet.EndpointAddress;
import com.wdroome.util.inet.NamedEndpointAddress;

/**
 * @author wdr
 *
 */
public class NamedEndpointAddressTest
{
	@Test
	public void testClone() throws UnknownHostException
	{
		String name = "localhost";
		double cost = 42.0;
		NamedEndpointAddress a1 = new NamedEndpointAddress("127.0.0.1");
		a1.setName(name);
		a1.setCost(cost);
		assertEquals("original name", name, a1.getName());
		assertEquals("original cost", cost, a1.getCost(), .0001);
		
		NamedEndpointAddress a2 = (NamedEndpointAddress)a1.clone();
		assertEquals("clone name", name, a2.getName());
		assertEquals("clone cost", cost, a2.getCost(), .0001);
	}
	
	@Test
	public void testSortByName() throws UnknownHostException
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
		NamedEndpointAddress[] addrs = new NamedEndpointAddress[addrStrs.length];
		for (int i = 0; i < addrStrs.length; i++) {
			addrs[i] = new NamedEndpointAddress(addrStrs[i]);
			addrs[i].setName(addrStrs[i]);
			addrs[i].setCost(addrStrs[i].hashCode());
		}
		String[] actualSortedAddrStrs = new String[addrStrs.length];
		
		Arrays.sort(addrs, new NamedEndpointAddress.NameComparator());
		for (int i = 0; i < addrStrs.length; i++)
			actualSortedAddrStrs[i] = addrs[i].toString();
		if (false) {
			for (String s:actualSortedAddrStrs)
				System.out.println("\"" + s + "\",");
		}
		
		String[] expectedSortedAddrStrs = new String[] {
				"0.1.2.3[0.1.2.3]",
				"0:0:0:1:0:0:0:0[0:0:0:1::]",
				"0:0:0:1:2:0:0:0[::1:2:0:0:0]",
				"0:0:0::0[::]",
				"1.1.1.1[1.1.1.1]",
				"1.2.3.1[1.2.3.1]",
				"1.2.3.4[1.2.3.4]",
				"1234:0:0[1234::]",
				"127.0.0.1[127.0.0.1]",
				"1:2:0:0:3:0:0:0[1:2:0:0:3::]",
				"1:2:0:0:3:0:0:1[1:2::3:0:0:1]",
				"1:2:3:4:5:6:0:0[1:2:3:4:5:6::]",
				"1:2:3:4:5:6:7.8.9.10[1:2:3:4:5:6:708:90A]",
				"1:2:3:4:5:6:7:0[1:2:3:4:5:6:7:0]",
				"1:2:3:4:5:6:7:8[1:2:3:4:5:6:7:8]",
				"1:2:3:4:5::0[1:2:3:4:5::]",
				"1:2:3:4:5::8[1:2:3:4:5::8]",
				"1:2:3:4::0:8[1:2:3:4::8]",
				"1:2:3:4::1:0[1:2:3:4::1:0]",
				"4.3.2.1[4.3.2.1]",
				"::[::]",
				"::0[::]",
				"FFEE:1234:4567::[FFEE:1234:4567::]",
				"FFEE:1234:4567::0[FFEE:1234:4567::]",
				"FFEE::[FFEE::]",
				"FFEE::0[FFEE::]",
				"FFEE::1[FFEE::1]",
				"FFEE::1.2.3.4[FFEE::102:304]",
				"FFEE::6:12.34.56.78[FFEE::6:C22:384E]",
				"ipv4:1.2.3.4[1.2.3.4]",
				"ipv6:::0[::]",
				"ipv6:ABCD::[ABCD::]",
			};
		assertArrayEquals(expectedSortedAddrStrs, actualSortedAddrStrs);
	}
	
	@Test
	public void testSortByCost() throws UnknownHostException
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
		NamedEndpointAddress[] addrs = new NamedEndpointAddress[addrStrs.length];
		for (int i = 0; i < addrStrs.length; i++) {
			addrs[i] = new NamedEndpointAddress(addrStrs[i]);
			addrs[i].setName(addrStrs[i]);
			addrs[i].setCost(addrStrs[i].hashCode());
		}
		String[] actualSortedAddrStrs = new String[addrStrs.length];
		
		Arrays.sort(addrs, new NamedEndpointAddress.CostComparator());
		for (int i = 0; i < addrStrs.length; i++)
			actualSortedAddrStrs[i] = (int)(addrs[i].getCost()) + "/" + addrs[i].toString();
		if (false) {
			for (String s:actualSortedAddrStrs)
				System.out.println("\"" + s + "\",");
		}
		
		String[] expectedSortedAddrStrs = new String[] {
				"-1937415971/ipv6:::0[::]",
				"-1934225799/1:2:3:4:5::0[1:2:3:4:5::]",
				"-1934225791/1:2:3:4:5::8[1:2:3:4:5::8]",
				"-1934086446/1:2:3:4::0:8[1:2:3:4::8]",
				"-1934085493/1:2:3:4::1:0[1:2:3:4::1:0]",
				"-1861210066/1234:0:0[1234::]",
				"-1580242955/ipv6:ABCD::[ABCD::]",
				"-1242883073/1:2:3:4:5:6:0:0[1:2:3:4:5:6::]",
				"-1242876346/1:2:3:4:5:6:7:0[1:2:3:4:5:6:7:0]",
				"-1242876338/1:2:3:4:5:6:7:8[1:2:3:4:5:6:7:8]",
				"-1191571766/FFEE::6:12.34.56.78[FFEE::6:C22:384E]",
				"-879592876/0:0:0::0[::]",
				"-630235824/FFEE:1234:4567::0[FFEE:1234:4567::]",
				"-574454560/FFEE::1.2.3.4[FFEE::102:304]",
				"-229375088/FFEE::0[FFEE::]",
				"-229375087/FFEE::1[FFEE::1]",
				"-88438229/0:0:0:1:0:0:0:0[0:0:0:1::]",
				"-32084757/ipv4:1.2.3.4[1.2.3.4]",
				"1856/::[::]",
				"57584/::0[::]",
				"271011584/4.3.2.1[4.3.2.1]",
				"1007305776/1:2:0:0:3:0:0:0[1:2:0:0:3::]",
				"1007305777/1:2:0:0:3:0:0:1[1:2::3:0:0:1]",
				"1014117116/0.1.2.3[0.1.2.3]",
				"1503690464/FFEE:1234:4567::[FFEE:1234:4567::]",
				"1505998205/127.0.0.1[127.0.0.1]",
				"1686569133/0:0:0:1:2:0:0:0[::1:2:0:0:0]",
				"1901619834/1.1.1.1[1.1.1.1]",
				"1902545277/1.2.3.1[1.2.3.1]",
				"1902545280/1.2.3.4[1.2.3.4]",
				"2006489224/1:2:3:4:5:6:7.8.9.10[1:2:3:4:5:6:708:90A]",
				"2070810784/FFEE::[FFEE::]",
			};
		assertArrayEquals(expectedSortedAddrStrs, actualSortedAddrStrs);
	}
}
