package com.wdroome.util.inet;

import java.net.UnknownHostException;

/**
 * A simple struct with two items, a CIDR and a String.
 * The items are immutable and are set by the c'tor;
 * for simplicity, clients access them as read-only public members.
 * The class implements equals() and hashCode(), so it can be used as a Map key.
 * @author wdr
 */
public class CIDRString implements Comparable<CIDRString>
{
	/**
	 * The endpoint.
	 */
	public final CIDRAddress m_cidr;
	
	/**
	 * The string.
	 */
	public final String m_string;
	
	/**
	 * The hash code -- set by c'tor.
	 */
	private final int m_hashCode;
	
	/**
	 * Create an instance.
	 * @param addr A string representation of the CIDR.
	 * @param string The string value.
	 * @throws UnknownHostException If addr isn't a valid internet address.
	 */
	public CIDRString(String addr, String string) throws UnknownHostException
	{
		this(new CIDRAddress(addr), string);
	}
	
	/**
	 * Create an instance.
	 * @param addr The CIDR address.
	 * @param string The string value.
	 */
	public CIDRString(CIDRAddress addr, String string)
	{
		m_cidr = addr;
		m_string = string;

		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((m_string == null) ? 0 : m_string.hashCode());
		result = prime * result
				+ ((m_cidr == null) ? 0 : m_cidr.hashCode());
		m_hashCode = result;
	}
	
	@Override
	public int hashCode()
	{
		return m_hashCode;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!getClass().isInstance(obj))
			return false;
		CIDRString other = (CIDRString) obj;
		if (m_string == null) {
			if (other.m_string != null)
				return false;
		} else if (!m_string.equals(other.m_string))
			return false;
		if (m_cidr == null) {
			if (other.m_cidr != null)
				return false;
		} else if (!m_cidr.equals(other.m_cidr))
			return false;
		return true;
	}

	/**
	 * Compare first on CIDR address, then on the string.
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(CIDRString other)
	{
		int v = m_cidr.compareTo(other.m_cidr);
		if (v != 0)
			return v;
		return m_string.compareTo(other.m_string);
	}
}
