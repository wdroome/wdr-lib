package com.wdroome.util.inet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.Comparator;
import java.net.UnknownHostException;
import java.net.InetAddress;
import java.security.MessageDigest;

/**
 * An internet address (IPV4 or IPV6) and mask. The mask is limited to a prefix-match in CIDR format.
 * Objects are idempotent: the address and mask are specified in the c'tor,
 * and cannot be changed.
 * @author wdr
 */
public class CIDRAddress implements Comparable<CIDRAddress>
{
	private final String m_addr;	// The base address, in canonical string format.

	private final int m_maskLen;	// The mask length, in bits.

	private final byte[] m_bytes;	// The bits in the raw base address, as a byte array.
									// The array has the appropriate number of bytes
									// for the address type: 4 bytes/32 bits for ipv4, 16 bytes/128 bits for ipv6).
									// The first maskLen bits define the address;
									// the remaining bits are always zero.

	private final byte[] m_mask;	// A bitmap with the first m_maskLen bits on.
									// Trailing zero bytes are omitted,
									// so the m_mask array may be shorter than the m_bytes array.
	
	private final String m_type; // The type of the base address -- "ipv4" or "ipv6."
	
	private final boolean m_ipv4;
	private final boolean m_ipv6;
	
	/**
	 * A table for creating the bytes in a prefix mask (e.g., m_mask).
	 * Suppose the first M bits of the mask are on.
	 * Then the mask has (M+7)/8 bytes.
	 * The last byte of the mask has the value g_lastMaskByte[M%8],
	 * and the previous bytes of the mask are all 0xff.
	 * So g_lastMaskByte[k] is a byte with the high-order k bits on,
	 * unless k is 0, in which case all bits are on.
	 */
	private static final byte[] g_lastMaskByte = new byte[] {
			(byte)0xff,
			(byte)0x80, (byte)0xc0, (byte)0xe0, (byte)0xf0,
			(byte)0xf8, (byte)0xfc, (byte)0xfe,
		};
	
	/**
	 * A cache of CIDR masks. The index is the mask length.
	 * Masks are created when needed; a null entry means a mask
	 * has not yet been created for that mask length.
	 * @see #createMask(int)
	 */
	private static final ArrayList<byte[]> g_masks = new ArrayList<byte[]>(129);

	/**
	 * Create a CIDR from a string. The string must be a valid IPV4 or IPV6 address
	 * followed by "/n", where n is the mask length.
	 * @param cidr The address and mask length.
	 * @throws UnknownAddressTypeException
	 *		If src has an unrecognized address type prefix.
	 * @throws UnknownHostException
	 * 		If cidr isn't of the form addr/len, or if the mask length is invalid, etc.
	 * @see #CIDRAddress(String,String)
	 */
	public CIDRAddress(String cidr) throws UnknownHostException
	{
		this(cidr, null);
	}

	/**
	 * Create a CIDR from a string. The string must be a valid IPV4 or IPV6 address
	 * followed by "/n", where n is the mask length.
	 * @param cidr The address and mask length.
	 * @param addrType The default address type for the address. If null, guess.
	 * @throws UnknownAddressTypeException
	 *		If src has an unrecognized address type prefix,
	 *		or if addrType isn't recognized.
	 * @throws UnknownHostException
	 * 		If cidr isn't of the form addr/len, or if the mask length is invalid, etc.
	 */
	public CIDRAddress(String cidr, String addrType)
			throws UnknownHostException
	{
		// Split cidr into the address and mask parts.
		int slashIndex = cidr.lastIndexOf("/");
		if (slashIndex <= 0) {
			throw new UnknownHostException("CIDR \"" + cidr + "\" does not end in /##");
		}
		String addrStr = cidr.substring(0, slashIndex);
		
		// Get the address byte array.
		try {
			m_bytes = EndpointAddress.ipAddrStrToBytes(addrStr, addrType);
		} catch (UnknownAddressTypeException e) {
			throw e;
		} catch (UnknownHostException e) {
			throw new UnknownHostException("CIDR \"" + cidr + "\" must be a numeric ip address " + e.getMessage());
		}
		
		try {
			m_maskLen = Integer.parseInt(cidr.substring(slashIndex+1));
		} catch (Exception e) {
			throw new UnknownHostException("CIDR \"" + cidr + "\" does not end in /##");
		}
		if (m_maskLen > (8*m_bytes.length) || m_maskLen < 0) {
			throw new UnknownHostException("CIDR \"" + cidr + "\" mask length is too long");
		}
		
		// Create the mask and clear all address bits not in the mask.
		m_mask = createMask(m_maskLen);
		clearUnmaskedBits(m_bytes, m_mask);

		// Regenerate the canonical address string from the address bits.
		EndpointAddress baseAddr = new EndpointAddress(m_bytes);
		m_addr = baseAddr.toIPAddr();
		m_type = baseAddr.getType();
		m_ipv4 = baseAddr.isIPV4();
		m_ipv6 = baseAddr.isIPV6();
	}
	
	/**
	 * Create a CIDRAddress from a byte array and a mask length.
	 * @param bytes The mask bytes. The array must be 4 or 16 bytes. We copy this array.
	 * @param maskLen The number of bits in the mask.
	 * @throws UnknownHostException Internal error; shouldn't happen.
	 * @throws IllegalArgumentException Bad length for bytes or maskLen.
	 */
	public CIDRAddress(byte[] bytes, int maskLen)
		throws UnknownHostException, IllegalArgumentException
	{
		if (bytes.length != 4 && bytes.length != 16) {
			throw new IllegalArgumentException("CIDR illegal byte array len " + bytes.length);
		} else if (maskLen > 8*bytes.length) {
			throw new IllegalArgumentException("CIDR mask len " + maskLen
											+ " > byte array bit len " + bytes.length);
		}
		
		// Copy the address bytes.
		m_maskLen = maskLen;
		m_bytes = new byte[bytes.length];
		for (int i = 0; i < bytes.length; i++) {
			m_bytes[i] = bytes[i];
		}
		
		// Create the mask and clear all address bits not in the mask.
		m_mask = createMask(m_maskLen);
		clearUnmaskedBits(m_bytes, m_mask);

		// Regenerate the canonical address string from the address bits.
		EndpointAddress baseAddr = new EndpointAddress(m_bytes);
		m_addr = baseAddr.toIPAddr();
		m_type = baseAddr.getType();
		m_ipv4 = baseAddr.isIPV4();
		m_ipv6 = baseAddr.isIPV6();
	}

	/**
	 * Create a CIDRAddress from an InetAddress and a mask length.
	 * @param addr An interface address.
	 * @param maskLen The mask length. Should be in [0,addr.length].
	 * 		If not, it will be quietly truncated to that range.
	 */
	public CIDRAddress(InetAddress addr, int maskLen)
	{
		// Copy the address bytes.
		byte[] addrBytes = addr.getAddress();
		m_bytes = new byte[addrBytes.length];
		for (int i = 0; i < addrBytes.length; i++) {
			m_bytes[i] = addrBytes[i];
		}

		// Create the mask and clear all address bits not in the mask.
		if (maskLen < 0) {
			maskLen = 0;
		} else if (maskLen > 8*addrBytes.length) {
			maskLen = 8*addrBytes.length;
		}
		m_maskLen = maskLen;
		m_mask = createMask(m_maskLen);
		clearUnmaskedBits(m_bytes, m_mask);

		// Regenerate the canonical address string from the address bits.
		EndpointAddress baseAddr = new EndpointAddress(addr, m_mask);
		m_addr = baseAddr.toIPAddr();
		m_type = baseAddr.getType();
		m_ipv4 = baseAddr.isIPV4();
		m_ipv6 = baseAddr.isIPV6();
	}

	/**
	 * Create a CIDRAddress from an EndpointAddress and a mask length.
	 * @param endpoint The base address.
	 * @param maskLen The number of bits in the mask.
	 * @throws UnknownHostException Internal error; shouldn't happen.
	 * @throws IllegalArgumentException Bad length for bytes or maskLen.
	 */
	public CIDRAddress(EndpointAddress endpoint, int maskLen)
		throws UnknownHostException, IllegalArgumentException
	{
		this(endpoint.getAddress(), maskLen);
	}

	/**
	 * Create a CIDRAddress from an EndpointAddress, using the full address.
	 * Eg, the CIDR is addr/32 or addr/128.
	 * @param endpoint The base address.
	 * @throws UnknownHostException Internal error; shouldn't happen.
	 * @throws IllegalArgumentException Bad length for bytes or maskLen.
	 */
	public CIDRAddress(EndpointAddress endpoint)
		throws UnknownHostException, IllegalArgumentException
	{
		this(endpoint.getAddress(), 8*endpoint.size());
	}
	
	/**
	 * Create a CIDRAddress from the leading bits of an existing CIDR.
	 * @param src The source CIDR.
	 * @param maskLen The number of bits in the mask. Cannot be longer than src's mask.
	 * @throws UnknownHostException Internal error; shouldn't happen.
	 * @throws IllegalArgumentException Invalid maskLen.
	 */
	public CIDRAddress(CIDRAddress src, int maskLen)
		throws UnknownHostException, IllegalArgumentException
	{
		if (maskLen > src.m_maskLen) {
			throw new IllegalArgumentException("CIDR mask len " + maskLen
											+ " > src mask len" + src.m_maskLen);
		}
		
		// Copy the address bytes.
		m_maskLen = maskLen;
		m_bytes = new byte[src.m_bytes.length];
		for (int i = 0; i < src.m_bytes.length; i++) {
			m_bytes[i] = src.m_bytes[i];
		}
		
		// Create the mask and clear all address bits not in the mask.
		m_mask = createMask(m_maskLen);
		clearUnmaskedBits(m_bytes, m_mask);

		// Regenerate the canonical address string from the address bits.
		EndpointAddress baseAddr = new EndpointAddress(m_bytes);
		m_addr = baseAddr.toIPAddr();
		m_type = baseAddr.getType();
		m_ipv4 = baseAddr.isIPV4();
		m_ipv6 = baseAddr.isIPV6();
	}

	/**
	 * Return a byte array with the first maskLen bits on.
	 * Cache the masks, and when possible, return a previously created mask.
	 * Hence clients must not modify the returned masks!
	 * @param maskLen The number of leading 1 bits.
	 * @return An array with the maskLen leading 1 bits.
	 */
	private byte[] createMask(int maskLen)
	{
		byte[] mask;
		synchronized (g_masks) {
			if (maskLen >= g_masks.size()) {
				g_masks.ensureCapacity(maskLen+1);
				for (int i = g_masks.size(); i < maskLen+1; i++) {
					g_masks.add(null);
				}
			}
			mask = g_masks.get(maskLen);
			if (mask == null) {
				mask = new byte[(maskLen + 7)/8];
				for (int i = 0; i < mask.length - 1; i++) {
					mask[i] = (byte)0xff;
				}
				if (maskLen > 0) {
					mask[mask.length-1] = g_lastMaskByte[maskLen % 8];
				}
				g_masks.set(maskLen, mask);
			}
		}
		return mask;
	}
	
	/**
	 * Clear any bits in "bytes" that are not covered by the bits in "mask".
	 * @param bytes An array of bits. Must be at least as long as "mask."
	 * @param mask A mask array with leading bits on.
	 */
	private void clearUnmaskedBits(byte[] bytes, byte[] mask)
	{
		for (int i = 0; i < bytes.length; i++) {
			if (i < mask.length) {
				bytes[i] &= mask[i];
			} else {
				bytes[i] = 0;
			}
		}
	}
	
	// These two fields are sacred to hashCode(). Since the object is idempotent,
	// the first time hashCode() is called, it calculates the hash,
	// saves it in m_hash, and sets m_hashCalculated to true.
	// Subsequent calls just return m_hash.
	private boolean m_hashCalculated = false;
	private int m_hash;

	/**
	 * Return a hashcode for this CIDR.
	 * The hash includes the mask length as well as the bytes,
	 * so CIDRs with the same bytes but different mask lengths
	 * have different hash codes.
	 */
	@Override
	public int hashCode()
	{
		if (!m_hashCalculated) {
			// Thread considerations:
			// We're assuming that int-assignment (eg, m_hash=hash) is atomic.
			final int prime = 31;
			int hash = 1;
			hash = prime * hash + Arrays.hashCode(m_bytes);
			hash = prime * hash + m_maskLen;
			m_hash = hash;
			m_hashCalculated = true;
		}
		return m_hash;
	}

	/**
	 * Return true if this CIDR equals another CIDR.
	 * For equality, the mask lengths and the underlying byte arrays
	 * must be identical.
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
		CIDRAddress other = (CIDRAddress) obj;
		if (m_maskLen != other.m_maskLen) {
			return false;
		} else if (!Arrays.equals(m_bytes, other.m_bytes)) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Compare two CIDRs.
	 * First we compare on mask length. The CIDR with the longer mask compares LOWER,
	 * so sort puts longer masks to the front of the list.
	 * If the mask lengths are the same, next we compare on address length,
	 * with IPv4 addresses being lower than IPv6 addresses.
	 * Finally, within the same type, we compare the addresses as unsigned bytes.
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(CIDRAddress other)
	{
		if (m_maskLen < other.m_maskLen) {
			return +1;
		} else if (m_maskLen > other.m_maskLen) {
			return -1;
		}
		int addrLen = m_bytes.length;
		if (addrLen < other.m_bytes.length) {
			return -1;
		} else if (addrLen > other.m_bytes.length) {
			return +1;
		}
		for (int i = 0; i < addrLen; i++) {
			int x = m_bytes[i] & 0xff;
			int y = other.m_bytes[i] & 0xff;
			if (x < y) {
				return -1;
			} else if (x > y) {
				return +1;
			}
		}
		return 0;
	}

	/**
	 * Return the address part of this CIDR, in canonical String format.
	 * Note that the bits after the mask prefix will be zero,
	 * regardless of what address you gave to the c'tor.
	 */
	public String getAddr()
	{
		return m_addr;
	}
	
	/**
	 * Return the address type: ipv4 or ipv6.
	 * @return The address type: ipv4 or ipv6.
	 */
	public String getType()
	{
		return m_type;
	}
	
	/**
	 * Return true iff this is an IPV4 address.
	 * @return True iff this is an IPV4 address.
	 */
	public boolean isIPV4()
	{
		return m_ipv4;
	}
	
	/**
	 * Return true iff this is an IPV6 address.
	 * @return True iff this is an IPV6 address.
	 */
	public boolean isIPV6()
	{
		return m_ipv6;
	}

	/**
	 * Return the address length of this CIDR, in bytes.
	 * @return The address length in bytes.
	 */
	public int getAddressLen()
	{
		return m_bytes.length;
	}

	/**
	 * Return the mask length of this CIDR.
	 * @return The mask length.
	 */
	public int getMaskLen()
	{
		return m_maskLen;
	}
	
	/**
	 * Test whether an IP address is contained within this CIDR.
	 * @param addr The address to test.
	 * @return True iff addr matches this CIDR under the mask.
	 */
	public boolean contains(InetAddress addr)
	{
		return contains(addr.getAddress());
	}
	
	/**
	 * Test whether an IP address is contained within this CIDR.
	 * @param bytes The raw bytes of the address to test.
	 * @return True iff "bytes" matches this CIDR under the mask.
	 */
	public boolean contains(byte[] bytes)
	{
		if (bytes.length != m_bytes.length) {
			return false;
		}
		for (int i = 0; i < m_mask.length; i++) {
			if ((bytes[i] & m_mask[i]) != m_bytes[i]) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Test whether this CIDR covers another CIDR.
	 * That is, whether every address contained in the other CIDR
	 * is also contained in this CIDR.
	 * @param other The other CIDR.
	 * @return True iff this CIDR completely covers the "other" CIDR.
	 *		Returns true if the CIDRs are identical,
	 *		and false if they are different address families.
	 */
	public boolean covers(CIDRAddress other)
	{
		if (this.m_bytes.length != other.m_bytes.length) {
			return false;
		} else if (this.m_maskLen > other.m_maskLen) {
			return false;
		} else {
			return contains(other.m_bytes);
		}
	}
	
	/**
	 * Test whether an IP address is contained within this CIDR.
	 * @param addr The address to test, as a dotted dec or colon hex string.
	 * @return True iff addr matches this CIDR under the mask.
	 * @throws UnknownHostException "Addr" is not a valid IP address.
	 * @see EndpointAddress#isContainedIn(CIDRAddress)
	 */
	public boolean contains(String addr) throws UnknownHostException
	{
		return new EndpointAddress(addr).isContainedIn(this);
	}

	/**
	 * Return "address/length".
	 */
	@Override
	public String toString()
	{
		return m_addr + "/" + m_maskLen;
	}
	
	/**
	 * Return a very detailed String representation, with internal fields.
	 */
	public String toDetailedString()
	{
		return toString()
				+ " bytes=" + catArray(m_bytes)
				+ " mask=" + catArray(m_mask);
	}

	/**
	 * Like {@link #toString()}, but with the address type prefix.
	 * @return The CIDR in "type:address/length" format.
	 */
	public String toIPAddrWithPrefix()
	{
		if (m_type != null) {
			return m_type + EndpointAddress.PREFIX_SEP + toString();
		} else {
			return toString();
		}
	}
	
	/**
	 * Update a message digest with the "raw bytes" of this CIDR.
	 * The format doesn't matter, as long as it's repeatable;
	 * the goal is to add a unique, consistent representation
	 * of this CIDR to the message digest. Using the toString() representation
	 * would accomplish the same goal, but this is more efficient.
	 * @param digest A message digest,
	 */
	public void update(MessageDigest digest)
	{
		digest.update(m_bytes);
		digest.update((byte)'/');
		digest.update(m_mask);
	}

	/**
	 * Return true if an address string looks like a CIDR
	 * rather than a single address.
	 * @param addr The address string.
	 * @return True iff s ends with "/##".
	 */
	public static boolean isCIDRFormat(String addr)
	{
		// Split cidr into the address and mask parts.
		int slashIndex = addr.lastIndexOf("/");
		if (slashIndex <= 0) {
			return false;
		}
		try {
			return Integer.parseInt(addr.substring(slashIndex+1)) >= 0;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Return a square-bracket-wrapped, comma-separated string
	 * with the hex values of the bytes in an array.
	 * This is primarily for debugging and testing.
	 */
	private static String catArray(byte[] arr)
	{
		StringBuilder b = new StringBuilder();
		b.append("[");
		String sep = "";
		for (byte x: arr) {
			b.append(sep);
			b.append(Integer.toHexString(x & 0xff));
			sep = ",";
		}
		b.append("]");
		return b.toString();
	}
	
	/**
	 * Return true if the first nbits of this CIDR's address match the first nbits of "other".
	 * @param other Another CIDR.
	 * @param nbits The number of bits in the address to test.
	 * @return True if the first nbits of this CIDR's address match the first nbits of "other".
	 */
	public boolean addrBitsMatch(CIDRAddress other, int nbits)
	{
		if (nbits == 0) {
			return true;
		} else if (nbits > m_maskLen || nbits > other.m_maskLen) {
			return false;
		}
		byte[] mask = createMask(nbits);
		for (int i = 0; i < mask.length; i++) {
			if ((other.m_bytes[i] & mask[i]) != m_bytes[i]) {
				return false;
			}
		}
		return true;	
	}

	/**
	 * An alternate comparator for CIDRs.
	 * First compare on the address type; IPV4 CIDRs are lower than IPV6 CIDRs.
	 * Then compare on the address bytes, as unsigned binary numbers.
	 * If the addresses are equal, compare the mask lengths (shorter means lower).
	 */
	public static class AddressByteComparator implements Comparator<CIDRAddress>
	{
		@Override
		public int compare(CIDRAddress o1, CIDRAddress o2)
		{
			if (o1.m_bytes.length < o2.m_bytes.length) {
				return -1;
			} else if (o1.m_bytes.length > o2.m_bytes.length) {
				return 1;
			}
			for (int i = 0; i < o1.m_bytes.length; i++) {
				int b1 = o1.m_bytes[i] & 0xff;
				int b2 = o2.m_bytes[i] & 0xff;
				if (b1 < b2) {
					return -1;
				} else if (b1 > b2) {
					return 1;
				}
			}
			if (o1.m_maskLen < o2.m_maskLen) {
				return -1;
			} else if (o1.m_maskLen > o2.m_maskLen) {
				return 1;
			} else {
				return 0;
			}
		}
	}
	
	/**
	 * Return true iff two arrays of CIDRAddresses are set-equivalent.
	 * That is, if you regard the arrays as sets, return true iff the sets are equal.
	 * Unlike Arrays.equals() in java.util, equivalent arrays can have
	 * different number of elements, and the elements can be in different order.
	 * <p>
	 * Caveat: This uses a simple n-squared algorithm to test for set equality.
	 * That's fine for small arrays, which is what we expect.
	 * But it will be slow for arrays with 100's of addresses.
	 * If large arrays become common, make shallow copies of the arrays,
	 * sort the copies, and then compare the copies in sequence, skipping duplicates.
	 * 
	 * @param arr1 An array of addresses.
	 * @param arr2 An array of addresses.
	 * @return True iff every address in arr1 is equal to an address in arr2,
	 * 		and every address in arr2 is equal to an addresses in arr1.
	 */
	public static boolean equivalentArrays(CIDRAddress[] arr1, CIDRAddress[] arr2)
	{
		if (arr1 == null) {
			arr1 = new CIDRAddress[0];
		} else if (arr2 == null) {
			arr2 = new CIDRAddress[0];
		}
		for (CIDRAddress a1:arr1) {
			boolean found = false;
			for (CIDRAddress a2:arr2) {
				if (a1.equals(a2)) {
					found = true;
					break;
				}
			}	
			if (!found) {
				return false;
			}
		}
		for (CIDRAddress a2:arr2) {
			boolean found = false;
			for (CIDRAddress a1:arr1) {
				if (a2.equals(a1)) {
					found = true;
					break;
				}
			}	
			if (!found) {
				return false;
			}
		}
		return true;
	}
}
