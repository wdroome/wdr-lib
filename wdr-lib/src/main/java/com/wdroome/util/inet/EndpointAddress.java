package com.wdroome.util.inet;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Set;

/**
 * An IPV4, IPV6 or MAC address constructed from a numeric string.
 * Unlike InetAddress, this class rejects host names.
 * This object may also hold a "pid" name; see {#EndpointAddress(String,boolean)}.
 * @see NamedEndpointAddress
 * @author wdr
 */
public class EndpointAddress implements Cloneable, Comparable<EndpointAddress>
{
	/**
	 * If this property is "true", use "::" short-cut in IPV6 addresses.
	 * The default is "true".
	 */
	public static final String USE_DOUBLE_COLON_ENV
									= EndpointAddress.class.getName() + ".USE_DOUBLE_COLON";

	/**
	 * If this property is "true", use upper-case rather than lower-case hex in IPV6 addresses.
	 * The default is "false".
	 */
	public static final String USE_UPPER_CASE_ENV
									= EndpointAddress.class.getName() + ".USE_UPPER_CASE";

	private static final boolean g_useDoubleColon;
	private static final boolean g_useUpperCase;
	static {
		String val = System.getProperty(USE_DOUBLE_COLON_ENV);
		if (val == null) {
			g_useDoubleColon = true;
		} else if (val.startsWith("t") || val.startsWith("T") || val.equals("1")) {
			g_useDoubleColon = true;
		} else {
			g_useDoubleColon = false;
		}
		val = System.getProperty(USE_UPPER_CASE_ENV);
		if (val == null) {
			g_useUpperCase = false;
		} else if (val.startsWith("t") || val.startsWith("T") || val.equals("1")) {
			g_useUpperCase = true;
		} else {
			g_useUpperCase = false;
		}
	}
	
	public static final String IPV4_PREFIX = "ipv4";
	
	public static final String IPV6_PREFIX = "ipv6";
	
	public static final String MAC_PREFIX = "mac";
	
	public static final String PID_PREFIX = "pid";
	
	public static final String PREFIX_SEP = ":";
	
	public static final char PREFIX_SEP_CHAR = PREFIX_SEP.charAt(0);
	
	/**
	 * The prefix for IPv4-mapped IPv6 addresses.
	 */
	public static final String IPV4_MAPPED_IPV6_ADDRS = IPV6_PREFIX + PREFIX_SEP +  "::ffff:0:0/96";
	
	/**
	 * The CIDRAddress for {@link #IPV4_MAPPED_IPV6_ADDRS}.
	 */
	public static final CIDRAddress IPV4_MAPPED_IPV6_CIDR;
	static {
		CIDRAddress cidr = null;
		try {
			cidr = new CIDRAddress(IPV4_MAPPED_IPV6_ADDRS);
		} catch (UnknownHostException e) {
			// Shouldn't happen ....
		}
		IPV4_MAPPED_IPV6_CIDR = cidr;
	}
	
	private static String[] g_addrTypes = new String[] {
					IPV4_PREFIX,
					IPV6_PREFIX, 
					MAC_PREFIX, 
					PID_PREFIX
					};
	
	// For IP & MAC addresses, m_address is valid,
	// and m_strvalue and m_strprefix are null.
	// For PID addresses, m_strvalue is the pid name,
	// m_strprefix is the PID prefix, and m_address is null.
	private final byte[] m_address;
	private final String m_strprefix;
	private final String m_strvalue;
	
	// The hashcode of m_address or m_strprefix:m_strvalue.
	private final int m_hashCode;
	
	private InetAddress m_inetAddress = null;
	
	/**
	 * Map each byte into the byte with bits reversed.
	 * So g_reversedBits[0x1] == 0x80, g_reversedBits[0x5] = 0xa0, etc.
	 */
	private static byte[] g_reversedBits = new byte[256];
	
	static {
		for (int i = 0; i < 256; i++) {
			int b = 0;
			for (int x = 0x01, y = 0x80; y != 0; x <<= 1, y >>= 1) {
				if ((i & x) != 0) {
					b |= y;
				}
			}
			g_reversedBits[i] = (byte)b;
		}
		//	for (int i = 0; i < 256; i++) {
		//		System.out.println(byteToBinary(i) + " => "
		//						+ byteToBinary(g_reversedBits[i] & 0xff));
		//	}
	}
	
	@SuppressWarnings("unused")
	private static String byteToBinary(int v)
	{
		StringBuilder b = new StringBuilder(8);
		for (int m = 0x80; m != 0; m >>= 1) {
			b.append((v & m) != 0 ? '1' : '0');
		}
		return b.toString();
	}
	
	private static String g_hexDigits[] = new String[] {
			"0", "1", "2", "3", "4", "5", "6", "7",
			"8", "9", "a", "b", "c", "d", "e", "f",
		};
	
	/**
	 * Create an endpoint from a dotted-decimal or colon-hex string.
	 * If the string starts with {@link #IPV4_PREFIX} or {@link #IPV6_PREFIX},
	 * followed by a colon, we assume it's that type. If not, we guess the type.
	 * @param src The IP address.
	 * @throws UnknownAddressTypeException
	 *		If src has an unrecognized address type prefix.
	 * @throws UnknownHostException If src isn't a properly formed IP address.
	 * @see #ipAddrStrToBytes(String)
	 */
	public EndpointAddress(String src)
		throws UnknownHostException
	{
		m_address = ipAddrStrToBytes(src, null);
		m_strprefix = null;
		m_strvalue = null;
		m_hashCode = Arrays.hashCode(m_address);
	}
	
	/**
	 * Create an endpoint from a dotted-decimal or colon-hex string.
	 * If the string starts with {@link #IPV4_PREFIX}, {@link #IPV6_PREFIX} or {@link #MAC_PREFIX},
	 * followed by a colon, we assume it's that type. If not, we assume it's addrType.
	 * And if that's null, we guess the type.
	 * @param src The IP address.
	 * @param defaultAddrType The default address type.
	 * @throws UnknownAddressTypeException If addrType isn't a recognized address type.
	 * @throws UnknownHostException If src isn't a properly formed IP address.
	 * @see #ipAddrStrToBytes(String, String)
	 */
	public EndpointAddress(String src, String defaultAddrType)
		throws UnknownHostException
	{
		m_address = ipAddrStrToBytes(src, defaultAddrType);
		m_strprefix = null;
		m_strvalue = null;
		m_hashCode = Arrays.hashCode(m_address);
	}
	
	/**
	 * Create an endpoint from a dotted-decimal or colon-hex string
	 * with an explicit type prefix.
	 * @param src The IP address.
	 * @param requiredAddrPrefixes The address must start with one of these
	 * 		address type prefixes. If null, allow all prefixes.
	 * 		Also, if not null, a prefix is required.
	 * 		If null, the prefix is optional.
	 * @throws UnknownHostException If src isn't a properly formed IP address,
	 * 		or if it does not start with one of the required prefixes.
	 */
	public EndpointAddress(String src, Set<String> requiredAddrPrefixes)
		throws UnknownHostException
	{
		String prefix = null;
		if (requiredAddrPrefixes != null) {
			for (String p: requiredAddrPrefixes) {
				if (src.startsWith(p + PREFIX_SEP)) {
					prefix = p;
					break;
				}
			}
			if (prefix == null) {
				throw new UnknownHostException("'" + src
						+ "' does not have the correct IP address prefix");
			}
		}
		m_address = ipAddrStrToBytes(src, prefix);
		m_strprefix = null;
		m_strvalue = null;
		m_hashCode = Arrays.hashCode(m_address);
	}
	
	/**
	 * Create an endpoint from the raw bytes in an InetAddress.
	 * @param src The IP address.
	 */
	public EndpointAddress(InetAddress src)
	{
		this(src, null);
	}
	
	/**
	 * Create an endpoint from the raw bytes in an InetAddress,
	 * ignoring any address bits not covered by bits in a mask.
	 * @param src The IP address.
	 * @param mask A bit mask for the raw bytes of the address.
	 */
	public EndpointAddress(InetAddress src, byte[] mask)
	{
		m_address = src.getAddress();
		if (mask != null) {
			for (int i = 0; i < m_address.length; i++) {
				if (i < mask.length) {
					m_address[i] &= mask[i];
				} else {
					m_address[i] = 0;
				}
			} 
		}
		m_strprefix = null;
		m_strvalue = null;
		m_hashCode = Arrays.hashCode(m_address);
	}
	
	/**
	 * Create an endpoint from an array of bytes.
	 * This c'tor makes a copy of the raw address bytes;
	 * it does not hold a reference to "address".
	 * @param address The raw bytes of the address.
	 * @throws UnknownHostException If address isn't 4, 6 or 16 bytes long.
	 */
	public EndpointAddress(byte[] address)
		throws UnknownHostException
	{
		if (!(address.length == 4 || address.length == 16 || address.length == 6)) {
			throw new UnknownHostException("byte[" + address.length + "] is not a valid address");
		}
		m_address = new byte[address.length];
		m_strprefix = null;
		m_strvalue = null;
		for (int i = 0; i < address.length; i++) {
			m_address[i] = address[i];
		}
		m_hashCode = Arrays.hashCode(m_address);
	}
	
	/**
	 * Create an "extended" endpoint address, which could be a PID name
	 * as well as a numerical address.
	 * For pid addresses, allowPids must be true and "src" must start with
	 * the PID prefix, {@link #PID_PREFIX}.
	 * @param src The address string.
	 * @param allowPids True iff pid:pid-name addresses are allowed.
	 * @throws UnknownAddressTypeException
	 *		If src has an unrecognized address type prefix.
	 * @throws UnknownHostException
	 * 		If src isn't a PID name or a properly formed IP address.
	 */
	public EndpointAddress(String src, boolean allowPids)
		throws UnknownHostException
	{
		if (allowPids && src.startsWith(PID_PREFIX + PREFIX_SEP)) {
			m_address = null;
			m_strprefix = PID_PREFIX;
			m_strvalue = src.substring(PID_PREFIX.length() + 1);
			m_hashCode = (m_strprefix + PREFIX_SEP + m_strvalue).hashCode();
		} else {
			m_address = ipAddrStrToBytes(src, null);
			m_strprefix = null;
			m_strvalue = null;
			m_hashCode = Arrays.hashCode(m_address);
		}
	}
	
	/**
	 * Convert an array of IP address strings into an array of EndpointAddresses.
	 * @param srcs The IP address strings. If they start with "ipv4:" or "ipv6:",
	 * 			the address must be of that type. If not, we guess the type.
	 * @return An array with the corresponding EndpointAddresses.
	 * @throws UnknownAddressTypeException
	 *		If any IP address has an unrecognized address type prefix.
	 * @throws UnknownHostException If any IP address string is invalid.
	 */
	public static EndpointAddress[] getEndpointAddresses(String[] srcs)
		throws UnknownHostException
	{
		int nAddrs = srcs.length;
		EndpointAddress[] ret = new EndpointAddress[nAddrs];
		for (int iAddr = 0; iAddr < nAddrs; iAddr++) {
			ret[iAddr] = new EndpointAddress(srcs[iAddr]);
		}
		return ret;
	}
	
	/**
	 * Return the length of the raw address, in bytes.
	 * Return 0 for pid addresses.
	 */
	public int size()
	{
		return m_address != null ? m_address.length : 0;
	}
	
	/**
	 * Return a copy of the raw bytes of the address.
	 * Note that for MAC addresses, the bits are reversed in the hex-string format.
	 * The raw bytes are not reversed. So for a MAC address whose string representation
	 * is "01:02:03:04:05:06", the raw bytes will be [80 40 c0 20 a0 60].
	 * @throws IllegalStateException If the address is a pid name.
	 */
	public byte[] getAddress()
	{
		if (m_address == null) {
			throw new IllegalStateException("Endpoint.getAddress() is undefined for pid names.");
		}
		byte[] v = new byte[m_address.length];
		for (int i = 0; i < m_address.length; i++) {
			v[i] = m_address[i];
		}
		return v;
	}
	
	/**
	 * If this is a PID name, return the name.
	 * @return The PID name in the address.
	 * @throws IllegalStateException If the address isn't a PID name.
	 */
	public String getName()
	{
		if (m_strvalue == null) {
			throw new IllegalStateException("Endpoint.getName() is undefined for numeric addresses.");
		}
		return m_strvalue;
	}
	
	/**
	 * Test whether this endpoint is contained within a CIDR.
	 * @param cidr The CIDR to test.
	 * @return True iff this endpoint matches the CIDR under the mask.
	 * @throws IllegalStateException If the address is a pid name.
	 */
	public boolean isContainedIn(CIDRAddress cidr)
	{
		if (m_address == null) {
			throw new IllegalStateException("Endpoint.getAddress() is undefined for pid names.");
		}
		return cidr.contains(m_address);
	}
	
	/**
	 * Return the type prefix (without the ending colon) for this address.
	 */
	public String getType()
	{
		if (m_strprefix != null) {
			return m_strprefix;
		} else if (m_address == null) {
			return "??";	// Shouldn't happen, but just in case ....
		}
		switch (m_address.length) {
			case 4: return IPV4_PREFIX;
			case 6: return MAC_PREFIX;
			case 16: return IPV6_PREFIX;
			default: return "IP??";
		}
	}
	
	/**
	 * Return true iff this is an IPV4 address.
	 * @return True iff this is an IPV4 address.
	 */
	public boolean isIPV4()
	{
		return m_address != null && m_address.length == 4;
	}
	
	/**
	 * Return true iff this is an IPV6 address.
	 * @return True iff this is an IPV6 address.
	 */
	public boolean isIPV6()
	{
		return m_address != null && m_address.length == 16;
	}
	
	/**
	 * Return true iff this is a MAC address.
	 * @return True iff this is a MAC address.
	 */
	public boolean isMAC()
	{
		return m_address != null && m_address.length == 6;
	}
	
	/**
	 * Return true iff this is a PID name.
	 * @return True iff this is a PID name.
	 */
	public boolean isPID()
	{
		return m_strprefix != null && m_strprefix.equals(PID_PREFIX);
	}
	
	/**
	 * Test if a string looks like a PID specification.
	 * @param s The string to test.
	 * @return True iff "s" starts with the prefix for a PID specification.
	 */
	public static boolean isPID(String s)
	{
		return s.startsWith(PID_PREFIX + PREFIX_SEP);
	}
	
	/**
	 *	Return a deep clone, with a copy of the address bytes used by this object.
	 */
	public Object clone()
	{
		try {
			if (m_address != null) {
				return new EndpointAddress(m_address);
			} else {
				return new EndpointAddress(m_strprefix + PREFIX_SEP + m_strvalue, true);
			}
		} catch (Exception e) {
			// Shouldn't happen!
			return null;
		}
	}
	
	/**
	 * Return the IP address string.
	 * Use dotted decimal for IPV4, and colon hex for IPV6 and MAC.
	 * @return A string in "IP address" format.
	 * @throws IllegalStateException If the address is a pid name.
	 */
	public String toIPAddr()
	{
		return appendIPAddr(null).toString();
	}

	/**
	 * Return an InetAddress for this endpoint address.
	 * Repeated calls return the same InetAddress object.
	 * @return An InetAddress for this endpoint address.
	 * @throws UnknownHostException If this endpoint is a MAC address.
	 * @throws IllegalStateException If the address is a pid name.
	 */
	public InetAddress toInetAddress() throws UnknownHostException
	{
		if (m_address == null) {
			throw new IllegalStateException("Endpoint.getAddress() is undefined for pid names.");
		}
		if (m_inetAddress == null) {
			// No need to synchronize. If two threads get here at the same time,
			// and both create InetAddress objects, one will stick and the
			// other will be garbage collected.
			m_inetAddress = InetAddress.getByAddress(m_address);
		}
		return m_inetAddress;
	}
	
	/**
	 * Return this address with the appropriate prefix.
	 * Use dotted decimal for IPV4, colon hex for IPV6 or MAC,
	 * or a string name for a PID.
	 * @return A string in "IP address" format.
	 */
	public String toIPAddrWithPrefix()
	{
		if (m_strprefix != null) {
			return m_strprefix + PREFIX_SEP + m_strvalue;
		} else {
			StringBuilder buff = new StringBuilder(10 + 5*m_address.length);
			buff.append(getType());
			buff.append(':');
			return appendIPAddr(buff).toString();
		}
	}

	/**
	 *	Append the address in "IP address" format to a buffer.
	 *	Use dotted decimal for IPV4, or colon hex for IPV6 or MAC.
	 *	@param buff The buffer. If null, create a new one.
	 *	@return The buffer "buff", or the new one that was allocated.
	 *	@throws IllegalStateException If the address is a pid name.
	 */
	public StringBuilder appendIPAddr(StringBuilder buff)
	{
		if (m_address == null) {
			throw new IllegalStateException("Endpoint.getAddress() is undefined for pid names.");
		}
		if (buff == null) {
			buff = new StringBuilder(5*m_address.length);
		}
		if (m_address.length == 4 || (m_address.length & 1) != 0) {
			appendDottedDecimal(buff, m_address, 0, m_address.length);
		} else if (m_address.length == 6) {
			for (int i = 0; i < m_address.length; i++) {
				if (i > 0) {
					buff.append(PREFIX_SEP);
				}
				int b = g_reversedBits[m_address[i] & 0xff] & 0xff;
				buff.append(g_hexDigits[b >> 4]);
				buff.append(g_hexDigits[b & 0xf]);
			}
		} else if (isIPV6()
				&& IPV4_MAPPED_IPV6_CIDR != null
				&& isContainedIn(IPV4_MAPPED_IPV6_CIDR)) {
			int prefixLen = IPV4_MAPPED_IPV6_CIDR.getMaskLen()/8;
			appendColonHex(buff, m_address, 0, prefixLen);
			buff.append(':');
			appendDottedDecimal(buff, m_address, prefixLen, m_address.length - prefixLen);
		} else {
			appendColonHex(buff, m_address, 0, m_address.length);
		}
		return buff;
	}
	
	/**
	 * Append dotted-decimal format bytes to a buffer.
	 * @param buff The string buffer.
	 * @param addr The bytes.
	 * @param start The starting index in "addr".
	 * @param len The number of bytes to use.
	 */
	private static void appendDottedDecimal(StringBuilder buff,
											byte[] addr,
											int start,
											int len)
	{
		for (int i = 0; i < len; i++) {
		  	byte b = addr[start + i];
		  	if (i > 0) {
				buff.append('.');
		  	}
		  	buff.append(b & 0xff);
		}
	}
	
	/**
	 * Append colon-hex format bytes to a buffer.
	 * @param buff The string buffer.
	 * @param addr The bytes.
	 * @param start The starting index in "addr".
	 * @param len The number of bytes to use.
	 */
	private static void appendColonHex(StringBuilder buff,
									   byte[] addr,
									   int start,
									   int len)
	{
		int values[] = new int[len/2];
		for (int i = 0; i < values.length; i++) {
			values[i] = ((addr[start + 2*i] & 0xff) << 8) | (addr[start + 2*i+1] & 0xff);
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
				String hex = Integer.toHexString(values[i]);
				buff.append(g_useUpperCase ? hex.toUpperCase() : hex);
				needSep = true;
			}
		}
		// Special case for all-zero address.
		// if (doubleColonStartIndex == 0 && !needSep)
		//	buff.append('0');
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
		if (!g_useDoubleColon) {
			return -1;
		}
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

	/**
	 * Return the address in canonical format (dotted decimal or colon hex).
	 * IPV6 addresses will use "::" to elide the longest string of zero words.
	 * PID names will start with the "pid" prefix; IP/MAC addresses will not.
	 */
	@Override
	public String toString()
	{
		if (m_strprefix != null) {
			return m_strprefix + PREFIX_SEP + m_strvalue;
		} else {
			return toIPAddr();
		}
	}

	/**
	 * Return -1, 0 or +1, depending on whether this address
	 * is less than, equal to, or greater than another.
	 * If the addresses have different lengths (e.g., different types),
	 * then the shorter address always compares less.
	 * So IPV4 addresses are less than MAC addresses are less than IPV6 addresses.
	 * For PID addresses, compare the pid names.
	 * When comparing PIDs to numerical addresses, PIDs are always greater.
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(EndpointAddress other)
	{
		if (m_strprefix != null && other.m_strprefix != null) {
			int v = m_strprefix.compareTo(other.m_strprefix);
			if (v != 0) {
				return v;
			} else {
				return m_strvalue.compareTo(other.m_strvalue);
			}
		} else if (m_address != null && other.m_address != null) {
			int len = m_address.length;
			int otherLen = other.m_address.length;
			if (len < otherLen) {
				return -1;
			} else if (len > otherLen) {
				return +1;
			}
			for (int i = 0; i < len; i++) {
				int b1 = m_address[i] & 0xff;
				int b2 = other.m_address[i] & 0xff;
				if (b1 < b2) {
					return -1;
				} else if (b1 > b2) {
					return +1;
				}
			}
			return 0;
		} else if (m_strprefix != null) {
			// String-valued addresses compare higher than numeric addresses.
			return 1;
		} else {
			return -1;
		}
	}

	/**
	 * Return a hashcode for this address.
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		return m_hashCode;
	}

	/**
	 * Return true iff this address is equal to another.
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) {
			return true;
		} else if (obj == null) {
			return false;
		} else if (!getClass().isInstance(obj)) {
			return false;
		}
		EndpointAddress other = (EndpointAddress)obj;
		if (m_address != null && other.m_address != null) {
			return Arrays.equals(m_address, ((EndpointAddress)obj).m_address);
		} else if (m_strprefix != null && other.m_strprefix != null) {
			return m_strprefix.equals(other.m_strprefix)
					&& m_strvalue.equals(other.m_strvalue);
		} else {
			return false;
		}
	}

	/**
	 * Parse src as an IP address, and return the address as a byte array.
	 * @param src The source address. If the string starts with {@link #IPV4_PREFIX}
	 * or {@link #IPV6_PREFIX} followed by a colon, the rest of the string must
	 * be in that address format. If there's no prefix, guess as to the type.
	 * 
	 * @return A byte[4] or byte[16] array with the address bits.
	 * @throws UnknownAddressTypeException
	 *		If src has an unrecognized address type prefix.
	 * @throws UnknownHostException If src isn't a valid IP address.
	 */
	public static byte[] ipAddrStrToBytes(String src)
			throws UnknownHostException
	{
		return ipAddrStrToBytes(src, null);
	}
	
	/**
	 * Parse src as an IP address, and return the address as a byte array.
	 * @param src The source address. If the string starts with {@link #IPV4_PREFIX},
	 *		{@link #IPV6_PREFIX} or {@link #MAC_PREFIX} followed by a colon, the rest of the string must
	 *		be in that address format. If there's no prefix, guess as to the type.
	 * @param addrType The default address type, {@link #IPV4_PREFIX},
	 *		{@link #IPV6_PREFIX} or {@link #MAC_PREFIX}.
	 *		If src starts with an address type prefix,
	 *		we honor that. If not, we assume src is of type addrType.
	 *		And if addrType is null or "", we guess	the address type from src.
	 * 
	 * @return A byte[4], byte[6] or byte[16] array with the address bits.
	 * @throws UnknownAddressTypeException
	 *		If src has an unrecognized address type prefix,
	 *		or if addrType isn't recognized.
	 * @throws UnknownHostException If src isn't a valid IP address.
	 */
	public static byte[] ipAddrStrToBytes(String src, String addrType)
			throws UnknownHostException
	{
		String origSrc = src;
		if (src.startsWith(IPV4_PREFIX + PREFIX_SEP)) {
			src = src.substring(IPV4_PREFIX.length() + 1);
			addrType = IPV4_PREFIX;
		} else if (src.startsWith(IPV6_PREFIX + PREFIX_SEP)) {
			src = src.substring(IPV6_PREFIX.length() + 1);
			addrType = IPV6_PREFIX;
		} else if (src.startsWith(MAC_PREFIX + PREFIX_SEP)) {
			src = src.substring(MAC_PREFIX.length() + 1);
			addrType = MAC_PREFIX;
		} else if (addrType == null || addrType.equals("-")) {
			if (src.length() == 17 && src.matches(
								"[0-9a-f][0-9a-f][-:][0-9a-f][0-9a-f][-:][0-9a-f][0-9a-f][-:]"
							  + "[0-9a-f][0-9a-f][-:][0-9a-f][0-9a-f][-:][0-9a-f][0-9a-f]")) {
				addrType = MAC_PREFIX;
			} else if (hasAddrTypePrefix(src)) {
				throw new UnknownAddressTypeException(null, origSrc);
			} else if (charOccursTwice(src, ':')) {
				addrType = IPV6_PREFIX;
			} else if (charOccursTwice(src, '.')) {
				addrType = IPV4_PREFIX;
			} else {
				throw new UnknownHostException(origSrc);
			}
		} else if (!addrType.equals(IPV4_PREFIX)
				&& !addrType.equals(IPV6_PREFIX)
				&& !addrType.equals(MAC_PREFIX)) {
			throw new UnknownAddressTypeException(addrType, origSrc);
		}
		byte[] arr;
		if (addrType.equals(MAC_PREFIX)) {
			String[] numbers = src.split("[-:]");
			if (numbers.length != 6) {
				throw new UnknownHostException(origSrc);
			}
			arr = new byte[6];
			for (int i = 0; i < numbers.length; i++) {
				arr[i] = g_reversedBits[myParseInt(numbers[i], 16, 0xff, origSrc) & 0xff];
			}
		} else if (addrType.equals(IPV4_PREFIX)) {
			String[] numbers = src.split("\\.");
			if (numbers.length != 4) {
				throw new UnknownHostException(origSrc);
			}
			arr = new byte[4];
			for (int i = 0; i < numbers.length && i < arr.length; i++) {
				String s = numbers[i];
				if (s.equals("")) {
					arr[i] = 0;
				} else {
					arr[i] = (byte)myParseInt(numbers[i], 10, 0xff, origSrc);
				}
			}
  		} else {
  			String[] numbers = src.split(":", -1);
			if (numbers.length <= 2 || numbers.length > 8) {
				throw new UnknownHostException(origSrc);
			}
 			arr = new byte[16];
 			int arrLen = 16;
 			int numbersLength = numbers.length;
			String lastHunk = numbers[numbersLength-1];
			if (lastHunk.indexOf('.') > 0) {
				byte[] last4 = ipAddrStrToBytes(lastHunk);
				arr[12] = last4[0];
				arr[13] = last4[1];
				arr[14] = last4[2];
				arr[15] = last4[3];
				arrLen = 12;
				numbersLength -= 1;
				if (numbersLength > 6) {
					throw new UnknownHostException(origSrc);
				}
			}
  			int iByte = 0;
  			for (int iNum = 0; iNum < numbersLength; iNum++) {
  				String num = numbers[iNum];
  				if (!num.equals("")) {
  					if (iByte+1 < arrLen) {
						int v = myParseInt(num, 16, 0xffff, origSrc);
						arr[iByte++] = (byte)(v >> 8);
						arr[iByte++] = (byte)v;
					}
				} else {
					if (iNum == 0 && iNum+1 < numbersLength && numbers[iNum+1].equals("")) {
						++iNum;
					}
  					int zerofill = arrLen - iByte - 2*(numbersLength - iNum - 1);
  					for (int iz = 0; iz < zerofill; iz++) {
  						arr[iByte++] = 0;
  					}
  				}
  			}		  					
  		}
		return arr;
	}
		
	/**
	 * Return true if src appears to have a (possibly unknown) address type prefix.
	 * That is, the string before the first ":" is alphanumeric
	 * and is not a valid 16-bit hex or decimal number.
	 */
	private static boolean hasAddrTypePrefix(String src)
	{
		int colonNdx = src.indexOf(':');
		if (colonNdx <= 0) {
			return false;
		}
		for (int i = 0; i < colonNdx; i++) {
			if (!Character.isLetterOrDigit(src.charAt(i))) {
				return false;
			}
		}
		boolean ret = true;
		if (colonNdx <= 4) {
			ret = false;
			for (int i = 0; i < colonNdx; i++) {
				char c = src.charAt(i);
				if ((c >= 'g' && c <= 'z') || (c >= 'G' && c <= 'Z')) {
					ret = true;
					break;
				}
			}
		}
		return ret;
	}

	/**
	 * Prepend an ipv4 or ipv6 or mac prefix to an IP address string.
	 * @param addr An ipv4 or ipv6 or mac address.
	 * @return
	 * 		The "addr" parameter with the appropriate address type prefix.
	 * 		If "addr" already starts with a prefix, just return "addr".
	 */
	public static String addPrefix(String addr)
	{
		if (addr.startsWith(IPV4_PREFIX + PREFIX_SEP)) {
			return addr;
		} else if (addr.startsWith(IPV6_PREFIX + PREFIX_SEP)) {
			return addr;
		} else if (addr.startsWith(MAC_PREFIX + PREFIX_SEP)) {
			return addr;
		} else if (addr.startsWith(PID_PREFIX + PREFIX_SEP)) {
			return addr;
		} else if (addr.length() == 17 && addr.matches(
					"[0-9a-f][0-9a-f][-:][0-9a-f][0-9a-f][-:][0-9a-f][0-9a-f][-:]"
				  + "[0-9a-f][0-9a-f][-:][0-9a-f][0-9a-f][-:][0-9a-f][0-9a-f]")) {
			return MAC_PREFIX + addr;
		} else if (charOccursTwice(addr, ':')) {
			return IPV6_PREFIX + PREFIX_SEP + addr;
		} else {
			return IPV4_PREFIX + PREFIX_SEP + addr;
		}
	}
	
	/**
	 * If addr ends in a % scopeid substring, strip the substring.
	 * If not, just return addr.
	 * @param addr An IP address.
	 * @return The address minus any scope substring.
	 */
	public static String stripSuffix(String addr)
	{
		int i = addr.indexOf('%');
		if (i > 0) {
			addr = addr.substring(0, i);
		}
		return addr;
	}
	
	/**
	 * Test if an address type is recognized.
	 * @param addrType The address type to test (without the ":").
	 * @return True iff this class recognizes addrType.
	 */
	public static boolean isKnownAddressType(String addrType)
	{
		for (String t: g_addrTypes) {
			if (t.equals(addrType)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Convert a numeric string to a positive base b integer.
	 * @param s The numeric string.
	 * @param b The desired base.
	 * @param max The maximum value.
	 * @param addr The full address string, used if we throw an UnknownHostException.
	 * @return The integer value of s in base b.
	 * @throws UnknownHostException If s isn't a valid base b number, or negative, or too large,
	 */
	private static int myParseInt(String s, int b, int max, String addr) throws UnknownHostException
	{
		try {
			int v = Integer.parseInt(s, b);
			if (!(v >= 0 && v <= max)) {
				throw new UnknownHostException(addr);
			}
			return v;
		} catch (NumberFormatException e) {
			throw new UnknownHostException(addr);
		}
	}

	/**
	 * Return true if s has at least two instances of the character c.
	 */
	private static boolean charOccursTwice(String s, char c)
	{
		int first = s.indexOf(c);
		if (first < 0) {
			return false;
		}
		return s.indexOf(c, first+1) > first;
	}
	
	/**
	 * Print tests on standard output.
	 */
	public static void main(String[] args)
	{
		System.out.println(findLongestZeroString(new int[] {0, 0, 0, 0, 0, 0}));
		System.out.println(findLongestZeroString(new int[] {0, 0, 1, 0, 0, 0}));
		System.out.println(findLongestZeroString(new int[] {1, 2, 0, 0, 0, 1, 0, 0, 0}));
		System.out.println(findLongestZeroString(new int[] {1, 2, 0, 0, 0, 1, 0, 0, 0, 0, 1}));		
		System.out.println(findLongestZeroString(new int[] {1, 2, 3, 4, 5}));		
		System.out.println(findLongestZeroString(new int[] {1, 0, 2, 0, 3, 0, 4, 0, 5, 0}));		
		System.out.println(findLongestZeroString(new int[] {1, 0, 2, 0, 3, 0, 0, 4, 0}));
		System.out.println(findLongestZeroString(new int[] {0, 0, 0, 1, 0, 0, 0, 0}));
	}
}
