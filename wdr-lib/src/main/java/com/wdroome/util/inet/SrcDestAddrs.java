package com.wdroome.util.inet;

import java.net.UnknownHostException;

/**
 * A pair of endpoint addresses.
 * The source and destination addresses are final public members,
 * so you may access them directly.
 * Implements the Comparable interface and overrides the hashCode() and equals() methods,
 * so you can use it as a Map key.
 * @author wdr
 */
public class SrcDestAddrs implements Comparable
{
	/** The source address. */
	public final EndpointAddress m_src;
	
	/** The destination address. */
	public final EndpointAddress m_dest;
	
	/**
	 * Create a new address pair.
	 * @param src The source address.
	 * @param dest The destination address.
	 */
	public SrcDestAddrs(EndpointAddress src, EndpointAddress dest)
	{
		m_src = src;
		m_dest = dest;
	}

	/**
	 * Create a new address pair.
	 * @param src A String with the source address.
	 * @param dest A String with the destination address.
	 * @throws UnknownHostException
	 * 		If src or dest cannot be converted to EndpointAddresses.
	 */
	public SrcDestAddrs(String src, String dest)
			throws UnknownHostException
	{
		this(new EndpointAddress(src), new EndpointAddress(dest));
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Object obj)
	{
		SrcDestAddrs other = (SrcDestAddrs)obj;
		int rc = m_src.compareTo(other.m_src);
		if (rc != 0) {
			return rc;
		} else {
			return m_dest.compareTo(other.m_dest);
		}
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((m_dest == null) ? 0 : m_dest.hashCode());
		result = prime * result + ((m_src == null) ? 0 : m_src.hashCode());
		return result;
	}

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
		SrcDestAddrs other = (SrcDestAddrs) obj;
		if (m_dest == null) {
			if (other.m_dest != null) {
				return false;
			}
		} else if (!m_dest.equals(other.m_dest)) {
			return false;
		}
		if (m_src == null) {
			if (other.m_src != null) {
				return false;
			}
		} else if (!m_src.equals(other.m_src)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString()
	{
		return "SrcDestAddrs[" + m_src + "," + m_dest + "]";
	}
}
