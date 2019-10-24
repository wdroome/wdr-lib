package com.wdroome.util.inet;

import java.net.InetAddress;
import java.net.Inet6Address;
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
	 * @return The address as an addr:port string,
	 * 		for ipv4, or [addr]:port, for ipv6.
	 */
	public static String toAddrPort(InetSocketAddress addr)
	{
		InetAddress addrPart = addr.getAddress();
		if (addrPart instanceof Inet6Address) {
			return "[" + addrPart.getHostAddress() + "]:" + addr.getPort();
		} else {
			return addr.getAddress().getHostAddress() + ":" + addr.getPort();
		}
	}
	
	/**
	 * Parse an IP address-port string of the form addr:port
	 * and return the corresponding InetSocketAddress.
	 * @param addrport A string of the form ipaddr:port.
	 * @return The address as an InetSocketAddress.
	 * @throws UnknownHostException
	 * 		If the ipaddr part is not a valid IP address.
	 * @throws NumberFormatException
	 * 		If the :port part is not a number.
	 * @throws IllegalArgumentException
	 * 		If the :port part is missing or it's not a legal port number.
	 */
	public static InetSocketAddress parseAddrPort(String addrport)
			throws UnknownHostException, NumberFormatException, IllegalArgumentException
	{
		return parseAddrPort(addrport, -1);
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
	 * @throws IllegalArgumentException
	 * 		If the :port part (or defPort, if used) is not a legal port number.
	 */
	public static InetSocketAddress parseAddrPort(String addrport, int defPort)
			throws UnknownHostException, NumberFormatException, IllegalArgumentException
	{
		int port = defPort;
		if (addrport.startsWith("[")) {
			// [addr]:port format
			int iCloseBracket = addrport.lastIndexOf(']');
			if (iCloseBracket > 0) {
				InetAddress addr = InetAddress.getByName(addrport.substring(1, iCloseBracket));
				if (iCloseBracket+2 < addrport.length()) {
					switch (addrport.charAt(iCloseBracket+1)) {
					case ':':
					case '.':
					case '#':
						port = Integer.parseInt(addrport.substring(iCloseBracket+2));
					}
				}
				return new InetSocketAddress(addr, port);
			}
		}
		
		int iLastColon = addrport.lastIndexOf(':');
		if (iLastColon < 0) {
			// addr format -- no port.
			return new InetSocketAddress(InetAddress.getByName(addrport), port);
		}
		
		int nColons = numColons(addrport);
		if (nColons == 1 || nColons == 8) {
			// ipv4-addr:port or ipv6-addr:port format
			if (iLastColon+1 < addrport.length()) {
				port = Integer.parseInt(addrport.substring(iLastColon+1));
			}
			return new InetSocketAddress(
						InetAddress.getByName(addrport.substring(0, iLastColon)), port);
		}
		
		// Must be ipv6. 
		int iPortSep = lastNonDigit(addrport);
		if (iPortSep > 0 && (addrport.charAt(iPortSep) == '.' || addrport.charAt(iPortSep) == '#')) {
			// ipv6.port or ipv6#port format
			port = Integer.parseInt(addrport.substring(iPortSep+1));
			return new InetSocketAddress(InetAddress.getByName(addrport.substring(0, iPortSep)), port);
		} else {
			// ipv6 format
			return new InetSocketAddress(InetAddress.getByName(addrport), port);
		}
	}
	
	/**
	 * If a string ends with a decimal digit, return the index
	 * of the last non-digit character. If not, return -1.
	 * @param s The string.
	 * @return The index of the last non digit, or -1.
	 */
	private static int lastNonDigit(String s)
	{
		int n = s.length();
		if (n > 0 && Character.isDigit(s.charAt(n-1))) {
			for (int i = n-1; --i >= 0; ) {
				if (!Character.isDigit(s.charAt(i))) {
					return i;
				}
			}
		}
		return -1;
	}
	
	private static int numColons(String s)
	{
		int nColons = 0;
		for (int i = s.length(); --i >= 0; ) {
			if (s.charAt(i) == ':') {
				nColons++;
			}
		}
		return nColons;
	}
	
	/**
	 * Return the canonical string format for an IP address.
	 * For IPv6, this is colon-hex with the longest 0-sequence replaced by "::".
	 * @param addr The address.
	 * @return
	 */
	public static String getCanonicalInetAddr(InetAddress addr)
	{
		if (addr == null) {
			return "0.0.0.0";
		}
		if (addr instanceof Inet6Address) {
			return getColonHex(addr.getAddress());
		}
		return addr.getHostAddress();
	}
	
	/**
	 * Return colon-hex format bytes.
	 * @param addr The bytes.
	 */
	 private static String getColonHex(byte[] addr)
	{
		StringBuilder buff = new StringBuilder();
		int values[] = new int[addr.length/2];
		for (int i = 0; i < values.length; i++) {
			values[i] = ((addr[2*i] & 0xff) << 8) | (addr[2*i+1] & 0xff);
		}
		boolean needSep = false;
		int doubleColonStartIndex = findLongestZeroString(values);
		for (int i = 0; i < values.length; i++) {
			if (i == doubleColonStartIndex) {
				buff.append("::");
				needSep = false;
				for (i++; i < values.length && values[i] == 0; i++) {
					;
				}
				if (i < values.length) {
					--i;
				}
			} else {
				if (needSep) {
					buff.append(':');
				}
				buff.append(Integer.toHexString(values[i]));
				needSep = true;
			}
		}
		return buff.toString();
	}
		
	/**
	 * If values has a string of two or more zeros,
	 * return the starting index of the longest such string.
	 * If several strings have the same length, return the first one.
	 * If there are no such strings of zeros, return -1.
	 * @param values An array of integers.
	 * @return The starting index of the longest string of zeros, or -1.
	 */
	private static int findLongestZeroString(int[] values)
	{
		int[] zeroCounts = new int[values.length];
		for (int i = 0; i < values.length; i++) {
			if (values[i] == 0) {
				int iend;
				for (iend = i+1; iend < values.length && values[iend] == 0; iend++) {
					;
				}
				zeroCounts[i] = iend - i;
				i = iend - 1;
			}
		}
		int max = 1;
		int imax = -1;
		for (int i = 0; i < values.length; i++) {
			if (zeroCounts[i] > max) {
				imax = i;
				max = zeroCounts[i];
			}
		}
		return imax;
	}
}
