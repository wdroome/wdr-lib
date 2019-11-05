package com.wdroome.util.inet;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Comparator;

/**
 * An {@link EndpointAddress} with a name and a cost.
 * The name and cost can change dynamically.
 * The name is an arbitrary symbolic name; it need not be a hostname.
 * Note that comparisons, hashes and equality tests
 * are based solely on the address, NOT on the name or cost.
 * So when using NamedEndpointAddresses in a hash table,
 * be sure that the underlying addresses are unique within that table.
 * @author wdr
 */
public class NamedEndpointAddress extends EndpointAddress
{
	private String m_name = null;
	private double m_cost = -1;
	
	/**
	 * Create an endpoint from an array of bytes.
	 * This c'tor makes a copy of the raw address bytes;
	 * it does not hold a reference to "address".
	 * @param address The raw bytes of the address.
	 * @throws UnknownHostException If address isn't 4 or 16 bytes long.
	 */
	public NamedEndpointAddress(byte[] address) throws UnknownHostException
	{
		super(address);
	}
	
	/**
	 * Create an endpoint from the raw bytes in an InetAddress.
	 * This c'tor makes a copy of the raw address bytes;
	 * it does not hold a reference to that byte array or to "src".
	 * @param src The IP address.
	 */
	public NamedEndpointAddress(InetAddress src)
	{
		super(src);
	}
	
	/**
	 * Create an endpoint from a dotted-decimal or colon-hex string.
	 * If the string starts with {@link #IPV4_PREFIX} or {@link #IPV6_PREFIX},
	 * followed by a colon, we assume it's that type. If not, we assume it's addrType.
	 * And if that's null, we guess the type.
	 * @param src The IP address.
	 * @param addrType The default address type.
	 * @throws UnknownHostException If src isn't a properly formed IP address.
	 * @see #ipAddrStrToBytes(String, String)
	 */
	public NamedEndpointAddress(String src, String addrType)
			throws UnknownHostException
	{
		super(src, addrType);
	}
	
	/**
	 * Create an endpoint from a dotted-decimal or colon-hex string.
	 * If the string starts with {@link #IPV4_PREFIX} or {@link #IPV6_PREFIX},
	 * followed by a colon, we assume it's that type. If not, we guess the type.
	 * @param src The IP address.
	 * @throws UnknownHostException If src isn't a properly formed IP address.
	 * @see #ipAddrStrToBytes(String)
	 */
	public NamedEndpointAddress(String src) throws UnknownHostException
	{
		super(src);
	}

	/**
	 *	Return a deep clone, with a copy of the address bytes used by this object.
	 */
	@Override
	public Object clone()
	{
		try {
			NamedEndpointAddress newObj = new NamedEndpointAddress(getAddress());
			newObj.m_name = m_name;
			newObj.m_cost = m_cost;
			return newObj;
		} catch (Exception e) {
			// Shouldn't happen!
			return null;
		}
	}

	/**
	 * Return the Endpoint's name, or null if not set.
	 * @return The Endpoint's name, or null if not set.
	 */
	public String getName()
	{
		return m_name;
	}

	/**
	 * Set the Endpoint's name.
	 * @param name The new name.
	 */
	public void setName(String name)
	{
		m_name = name;
	}

	/**
	 * Return the Endpoint's cost, or -1 if not set.
	 * @return The Endpoint's cost, or -1 if not set.
	 */
	public double getCost()
	{
		return m_cost;
	}

	/**
	 * Set the Endpoint's cost.
	 * @param cost The new cost.
	 */
	public void setCost(double cost)
	{
		m_cost = cost;
	}

	/**
	 * A Comparator for NamedEndpointAddresses using the cost.
	 */
	public static class CostComparator implements Comparator<NamedEndpointAddress>
	{		
		/**
		 * Return -1, 0, or +1 depending on whether o1's cost
		 * is less than, equal to, or greater o2's cost.
		 * @param o1 First item to compare.
		 * @param o2 Second item to compare.
		 * @return -1, 0, or +1.
		 */
		@Override
		public int compare(NamedEndpointAddress o1, NamedEndpointAddress o2)
		{
			double cost1 = o1.getCost();
			double cost2 = o2.getCost();
			if (cost1 < cost2) {
				return -1;
			} else if (cost1 > cost2) {
				return 1;
			} else {
				return 0;
			}
		}
	}

	/**
	 * A Comparator for NamedEndpointAddresses using the name.
	 */
	public static class NameComparator implements Comparator<NamedEndpointAddress>
	{		
		/**
		 * Return -1, 0, or +1 depending on whether o1's name
		 * is less than, equal to, or greater than o2's name.
		 * This uses the java String comparator.
		 * @param o1 First item to compare.
		 * @param o2 Second item to compare.
		 * @return -1, 0, or +1.
		 */
		@Override
		public int compare(NamedEndpointAddress o1, NamedEndpointAddress o2)
		{
			return o1.getName().compareTo(o2.getName());
		}
	}
	
	/**
	 * Return the name and address in canonical format (dotted decimal or colon hex).
	 * IPV6 addresses will use "::" to elide the longest string of zero words.
	 */
	@Override
	public String toString()
	{
		if (m_name == null || m_name.equals("")) {
			return super.toString();
		} else {
			StringBuilder b = new StringBuilder(m_name);
			b.append('[');
			appendIPAddr(b);
			b.append(']');
			return b.toString();
		}
	}
}
