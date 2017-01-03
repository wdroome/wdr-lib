package com.wdroome.util.inet;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * @author wdr
 */
public class InetUtil
{
	/**
	 * Compare two internet addresses based on the raw address bytes.
	 * Shorter addresses compare less than longer addresses.
	 * @param a The first address.
	 * @param b The second address.
	 * @return -1 if a &lt; b, +1 if a &gt; b, or 0 if a == b.
	 */
	public static int compare(InetAddress a, InetAddress b)
	{
		byte[] ab = a.getAddress();
		byte[] bb = b.getAddress();
		if (ab.length < bb.length) {
			return -1;
		} else if (ab.length > bb.length) {
			return 1;
		} else {
			for (int i = 0; i < ab.length; i++) {
				if ((ab[i] & 0xff) < (bb[i] & 0xff)) {
					return -1;
				} else if ((ab[i] & 0xff) > (bb[i] & 0xff)) {
					return 1;
				}
			}
		}
		return 0;
	}
	
	/**
	 * Compare two socket addresses based on the raw address bytes,
	 * and if those are equal, the port numbers.
	 * Shorter addresses compare less than longer addresses.
	 * @param a The first address.
	 * @param b The second address.
	 * @return -1 if a &lt; b, +1 if a &gt; b, or 0 if a == b.
	 */
	public static int compare(InetSocketAddress a, InetSocketAddress b)
	{
		int c = compare(a.getAddress(), b.getAddress());
		if (c != 0) {
			return c;
		}
		int ap = a.getPort();
		int bp = b.getPort();
		if (ap < bp) {
			return -1;
		} else if (ap > bp) {
			return 1;
		} else {
			return 0;
		}
	}
	
	/**
	 * Return a socket address as an addr:port string.
	 * @param addr The socket address.
	 * @return The address as an addr:port string.
	 */
	public static String toAddrPort(InetSocketAddress addr)
	{
		return addr.getAddress().getHostAddress() + ":" + addr.getPort();
	}
	
	/**
	 * Parse an IP address-port string of the form addr:port
	 * and return the corresponding InetSocketAddress.
	 * @param addrport A string of the form ipaddr[:port].
	 * @return The address as an InetSocketAddress.
	 * @throws UnknownHostException
	 * 		If the ipaddr part is not a valid IP address.
	 * @throws NumberFormatException
	 * 		If the :port part is missing or is not a number.
	 */
	public static InetSocketAddress parseAddrPort(String addrport)
			throws UnknownHostException, NumberFormatException
	{
		int port = 0;
		int iColon = addrport.lastIndexOf(':');
		if (iColon > 0) {
			port = Integer.parseInt(addrport.substring(iColon+1));
		} else {
			throw new NumberFormatException("Missing :port");
		}
		return new InetSocketAddress(InetAddress.getByName(addrport.substring(0, iColon)), port);
	}
	
	/**
	 * Parse an Inet4 address-port string of the form addr[:port]
	 * and return the corresponding InetSocketAddress.
	 * Note that the port cannot be omitted for an IPv6 address.
	 * @param addrport A string of the form ipaddr[:port].
	 * @param defPort The default port number (only for IPv4 addresses).
	 * @return The address as an InetSocketAddress.
	 * @throws UnknownHostException
	 * 		If the ipaddr part is not a valid IP address.
	 * @throws NumberFormatException
	 * 		If the :port part is not a number.
	 */
	public static InetSocketAddress parseAddrPort(String addrport, int defPort)
			throws UnknownHostException, NumberFormatException
	{
		int port = defPort;
		int iColon = addrport.lastIndexOf(':');
		if (iColon > 0) {
			port = Integer.parseInt(addrport.substring(iColon+1));
		} else {
			iColon = addrport.length();
		}
		return new InetSocketAddress(InetAddress.getByName(addrport.substring(0, iColon)), port);
	}
}
