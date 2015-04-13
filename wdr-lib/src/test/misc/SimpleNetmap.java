package test.misc;

import java.net.UnknownHostException;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author wdr
 */
public class SimpleNetmap
{
	/**
	 * Simple class to store a CIDR.
	 * @author wdr
	 */
	public static class CIDRAddress implements Comparable<CIDRAddress>
	{
		public final byte[] m_bytes;
		public final byte[] m_mask;
		public final int m_maskLen;
		
		/**
		 * Create a CIDRAddress from an ipaddr/## string.
		 * @param cidr
		 * @throws UnknownHostException
		 */
		public CIDRAddress(String cidr) throws UnknownHostException
		{
			// Delete type prefix if present. We infer type from format.
			for (String prefix: new String[] {"ipv4:", "ipv6:"}) {
				if (cidr.startsWith(prefix)) {
					cidr = cidr.substring(prefix.length());
					break;
				}
			}
			
			// Split cidr into the address and mask parts.
			int slashIndex = cidr.lastIndexOf("/");
			if (slashIndex <= 0) {
				throw new UnknownHostException("CIDR \"" + cidr + "\" does not end in /##");
			}
			InetAddress inetAddr = InetAddress.getByName(cidr.substring(0, slashIndex));
			m_bytes = inetAddr.getAddress();

			try {
				m_maskLen = Integer.parseInt(cidr.substring(slashIndex+1));
			} catch (Exception e) {
				throw new UnknownHostException("CIDR \"" + cidr + "\" does not end in /##");
			}
			if (m_maskLen > (8*m_bytes.length) || m_maskLen < 0) {
				throw new UnknownHostException("CIDR \"" + cidr + "\" mask length is too long");
			}
			
			m_mask = createMask(m_maskLen);
			clearUnmaskedBits(m_bytes, m_mask);
		}
		
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
					for (int i = 0; i < mask.length - 1; i++)
						mask[i] = (byte)0xff;
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
				if (i < mask.length)
					bytes[i] &= mask[i];
				else
					bytes[i] = 0;
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

		@Override
		public String toString()
		{
			String addr;
			try {
				addr = InetAddress.getByAddress(m_bytes).getHostAddress();
			} catch (UnknownHostException e) {
				addr = "????";
			}
			return (m_bytes.length == 16 ? "ipv6:" : "ipv4:")
							+ addr + "/" + m_maskLen;
		}
	}

	/**
	 * A Map from each CIDR to the name of its pid.
	 * The map is sorted by the CIDRAddress class's native ordering
	 * (longest-mask-first).
	 */
	private Map<CIDRAddress,String> m_cidr2pid = new TreeMap<CIDRAddress, String>();
	
	/**
	 * Add a CIDR to the CIDR-to-PID map.
	 * @param pid The pid name.
	 * @param cidr The CIDR (with or without type prefix).
	 * @throws UnknownHostException Invalid CIDR.
	 */
	public void addCIDR(String pid, String cidr) throws UnknownHostException
	{
		m_cidr2pid.put(new CIDRAddress(cidr), pid);
	}
	
	/**
	 * Return an Iterable object with the CIDR-to-PID entries.
	 */
	public Iterable<Map.Entry<CIDRAddress, String>> getCIDRs()
	{
		return m_cidr2pid.entrySet();
	}
	
	/**
	 *	Return an ascii string with the MD5 digest of a canonical
	 *	representation of the pids & cidrs in this map.
	 *	If we cannot do MD5 digests at all, return the current time stamp as a string.
	 *	@param prefix
	 *		If not null, prefix the returned vtag with this string.
	 *	@return
	 *		A cannonical vtag for the network map.
	 */
	public String makeVtag(String prefix)
	{
		StringBuilder hashStr = new StringBuilder();
		if (prefix != null) {
			hashStr.append(prefix);
		}
		
		MessageDigest md5Digest = null;
		try {
			md5Digest = MessageDigest.getInstance("MD5");
		} catch (java.security.NoSuchAlgorithmException e) {
			md5Digest = null;
		}
		if (md5Digest == null) {
			hashStr.append(Long.toString(System.currentTimeMillis(), Character.MAX_RADIX));
			return hashStr.toString();
		}
		
		// NOTE: Because m_cidr2pid is a TreeMap, entrySet() returns
		// the CIDRs in predictable, repeatable order.
		for (Map.Entry<CIDRAddress, String> ent:m_cidr2pid.entrySet()) {
			CIDRAddress cidr = ent.getKey();
			md5Digest.update(cidr.m_bytes);
			md5Digest.update((byte)'/');
			md5Digest.update(cidr.m_mask);			
			md5Digest.update((byte)' ');
			md5Digest.update(ent.getValue().getBytes());
			md5Digest.update((byte)'\n');
		}
		byte[] hash = md5Digest.digest();
		String[] hex = {"0", "1", "2", "3", "4", "5", "6", "7",
						"8", "9", "a", "b", "c", "d", "e", "f"};
		for (byte b:hash) {
			hashStr.append(hex[(b>>4) & 0xf]);
			hashStr.append(hex[(b   ) & 0xf]);
		}
		return hashStr.toString();
	}

	/**
	 * Create a network map and calculate a vtag.
	 * @param args
	 * 		A series of pid names and CIDRs.
	 * 		A CIDR has a "/", a PID does not.
	 * 		The first argument must be a PID name.
	 * @throws UnknownHostException
	 * 		If a CIDR is invalid.
	 */
	public static void main(String[] args) throws UnknownHostException
	{
		SimpleNetmap netmap = new SimpleNetmap();
		String pid = null;
		for (int i = 0; i < args.length; i++) {
			if (!args[i].contains("/")) {
				pid = args[i];
			} else if (pid != null) {
				netmap.addCIDR(pid, args[i]);
			} else {
				System.out.println("Usage: pid cidr cidr ... pid cidr ....");
			}
		}
		for (Map.Entry<CIDRAddress, String> ent:netmap.getCIDRs()) {
			System.out.println("  " + ent.getKey() + " => " + ent.getValue());
		}
			
		System.out.println("vtag: " + netmap.makeVtag(null));
	}
}
