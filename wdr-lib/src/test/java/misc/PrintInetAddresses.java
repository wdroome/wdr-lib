/**
 * 
 */
package test.misc;

import com.wdroome.util.inet.InetInterface;

/**
 * Print all InetAddresses on this host, including the MAC addrs.
 * @author wdr
 */
public class PrintInetAddresses
{	
	/**
	 * Print all interfaces on this host.
	 * @param args
	 */
	public static void main(String[] args)
	{
		System.out.println("All Interfaces:");
		for (InetInterface iface: InetInterface.getAllInterfaces()) {
			System.out.println("  " + iface + " " + byteArrToStr(iface.m_hardwareAddress));
		}
		System.out.println("Broadcast Interfaces:");
		for (InetInterface iface: InetInterface.getBcastInterfaces()) {
			System.out.println("  " + iface.m_broadcast.getHostAddress() + " " + iface.m_cidr
					+ " " + byteArrToStr(iface.m_hardwareAddress));
		}
	}
	
	private static String byteArrToStr(byte[] bytes) {
		StringBuffer str = new StringBuffer(100);
		if (bytes != null) {
			for (int i = 0; i < bytes.length; i++) {
				str.append(String.format("%02x", bytes[i] & 0xff));
			}
		}
		return str.toString();
	}
}
