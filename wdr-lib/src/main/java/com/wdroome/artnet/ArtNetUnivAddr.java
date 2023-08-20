package com.wdroome.artnet;

/**
 * An ArtNet Universe and the Node Address of a node which handles that universe.
 * A client can use the address to send a message to the node.
 * This object can be used as a key in Maps.
 * Art-Net (TM) Designed by and Copyright Artistic License Holdings Ltd.
 * @author wdr
 */
public class ArtNetUnivAddr implements Comparable<ArtNetUnivAddr>
{
	public final ArtNetNodeAddr m_nodeAddr;
	public final ArtNetUniv m_port;
	
	public ArtNetUnivAddr(ArtNetNodeAddr nodeAddr, ArtNetUniv univ)
	{
		m_nodeAddr = nodeAddr;
		m_port = univ;
	}

	@Override
	public int compareTo(ArtNetUnivAddr o)
	{
		int cmp = m_nodeAddr.compareTo(o.m_nodeAddr);
		if (cmp != 0) {
			return cmp;
		}
		return m_port.compareTo(o.m_port);
	}

	/**
	 * Return port @ node-address.
	 */
	@Override
	public String toString()
	{
		return m_port + "@" + m_nodeAddr;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((m_nodeAddr == null) ? 0 : m_nodeAddr.hashCode());
		result = prime * result + ((m_port == null) ? 0 : m_port.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ArtNetUnivAddr other = (ArtNetUnivAddr) obj;
		if (m_nodeAddr == null) {
			if (other.m_nodeAddr != null)
				return false;
		} else if (!m_nodeAddr.equals(other.m_nodeAddr))
			return false;
		if (m_port == null) {
			if (other.m_port != null)
				return false;
		} else if (!m_port.equals(other.m_port))
			return false;
		return true;
	}
	
}
