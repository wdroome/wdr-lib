package com.wdroome.util.inet;

import java.net.UnknownHostException;

/**
 * A simple struct with two items, an endpoint address and a String.
 * The items are immutable and are set by the c'tor;
 * for simplicity, clients access them as read-only public members.
 * The class implements equals() and hashCode(), so it can be used as a Map key.
 * @author wdr
 */
public class EndpointString implements Comparable<EndpointString>
{
	/**
	 * The endpoint.
	 */
	public final EndpointAddress m_endpoint;
	
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
	 * @param addr A string representation of the endpoint's address.
	 * @param string The string value.
	 * @throws UnknownHostException If addr isn't a valid internet address.
	 */
	public EndpointString(String addr, String string) throws UnknownHostException
	{
		this(new EndpointAddress(addr), string);
	}
	
	/**
	 * Create an instance.
	 * @param addr The endpoint's address.
	 * @param string The string value.
	 */
	public EndpointString(EndpointAddress addr, String string)
	{
		m_endpoint = addr;
		m_string = string;

		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((m_string == null) ? 0 : m_string.hashCode());
		result = prime * result
				+ ((m_endpoint == null) ? 0 : m_endpoint.hashCode());
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
		if (this == obj) {
			return true;
		} else if (obj == null) {
			return false;
		} else if (!getClass().isInstance(obj)) {
			return false;
		}
		EndpointString other = (EndpointString) obj;
		if (m_string == null) {
			if (other.m_string != null) {
				return false;
			}
		} else if (!m_string.equals(other.m_string)) {
			return false;
		}
		if (m_endpoint == null) {
			if (other.m_endpoint != null) {
				return false;
			}
		} else if (!m_endpoint.equals(other.m_endpoint)) {
			return false;
		}
		return true;
	}

	/**
	 * Compare first on endpoint address, then on the string.
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(EndpointString other)
	{
		int v = m_endpoint.compareTo(other.m_endpoint);
		if (v != 0) {
			return v;
		} else {
			return m_string.compareTo(other.m_string);
		}
	}
}
