package com.wdroome.artnet;

/**
 * A simple struct with the components of an Art-Net Port Number.
 * Note that the constructors do not verify that the components are in range.
 * @author wdr
 */
public class ArtNetPort implements Comparable<ArtNetPort>
{
	public static final ArtNetPort LOW_PORT = new ArtNetPort(0,0,0);
	public static final ArtNetPort HIGH_PORT = new ArtNetPort(127,15,15);
	
	/** The network number. */
	public final int m_net;
	
	/** The sub-net number. */
	public final int m_subNet;
	
	/** The universe number. */
	public final int m_universe;
	
	/**
	 * Create an ArtNetPort.
	 * @param net The network number.
	 * @param subNet The sub-net number.
	 * @param universe The universe number.
	 */
	public ArtNetPort(int net, int subNet, int universe)
	{
		m_net = net;
		m_subNet = subNet;
		m_universe = universe;
	}
	
	/**
	 * Create an ArtNetPort.
	 * @param net The network number.
	 * @param subNetUniv The sub-net number (bits 0xf0)
	 * 		and universe number (bits 0x0f).
	 */
	public ArtNetPort(int net, int subNetUniv)
	{
		m_net = net;
		m_subNet = (subNetUniv & 0xf0) >> 4;
		m_universe = subNetUniv & 0x0f;
	}
	
	/**
	 * Copy an ArtNetPort.
	 * @param src The source port.
	 */
	public ArtNetPort(ArtNetPort src)
	{
		m_net = src.m_net;
		m_subNet = src.m_subNet;
		m_universe = src.m_universe;
	}
	
	/**
	 * Create an ArtNetPort from a string of the form
	 * net.subNet.univ or subNet.univ.
	 * @param portStr An Art-Net port string.
	 * @throws IllegalArgumentException
	 * 		If portStr is not in the correct format.
	 */
	public ArtNetPort(String portStr)
	{
		String[] parts = portStr.split("\\.");
		try {
			switch (parts.length) {
			case 3:
				m_net = Integer.parseInt(parts[0]);
				m_subNet = Integer.parseInt(parts[1]);
				m_universe = Integer.parseInt(parts[2]);
				return;
			case 2:
				m_net = 0;
				m_subNet = Integer.parseInt(parts[0]);
				m_universe = Integer.parseInt(parts[1]);
				return;
			}
		} catch (Exception e) {
			// number format exception -- fall thru
		}
		throw new IllegalArgumentException("\"" + portStr
							+ "\" is not a valid Art-Net port string.");
	}
	
	/**
	 * Create an ArtNetPort from 2 bytes in a buffer. First byte is the network,
	 * second is the subNet and universe.
	 * @param buff The byte buffer.
	 * @param offset The offset of the first byte of the port address.
	 */
	public ArtNetPort(byte[] buff, int offset)
	{
		if (offset + 2 > buff.length) {
			throw new IllegalArgumentException("ArtNetPort(byte[]): bad offset="
								+ offset + " len=" + buff.length);
		}
		m_net = buff[offset] & 0x7f;
		m_subNet = (buff[offset+1] & 0xf0) >> 4;
		m_universe = buff[offset+1] & 0x0f;
	}
	
	/**
	 * Return the bottom 8 bits of the port adddress.
	 * That is, the sub-net and universe parts.
	 * @return The sub-net and universe in the top and bottom nibble
	 * 		of the low-order byte.
	 */
	public int subUniv()
	{
		return (m_subNet << 4) + (m_universe);
	}
	
	/**
	 * Put the ArtNetPort into a byte buffer as a 16-bit int.
	 * @param buff The buffer.
	 * @param off The offset in buff.
	 * @return The offset of the next byte after the port address.
	 */
	public int put(byte[] buff, int off)
	{
		buff[off] = (byte)m_net;
		buff[off+1] = (byte) ((m_subNet << 4) | m_universe);
		return off+2;
	}
	
	/**
	 * Return a string of the form net.subnet.univ.
	 */
	@Override
	public String toString()
	{
		return m_net + "." + m_subNet + "." + m_universe;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		return (m_net << 8) + (m_subNet << 4) + (m_universe);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ArtNetPort other = (ArtNetPort) obj;
		return m_net == other.m_net
					&& m_subNet == other.m_subNet
					&& m_universe == other.m_universe;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(ArtNetPort o)
	{
		if (this.m_net < o.m_net) {
			return -1;
		} else if (this.m_net > o.m_net) {
			return 1;
		} else if (this.m_subNet < o.m_subNet) {
			return -1;
		} else if (this.m_subNet > o.m_subNet) {
			return 1;
		} else if (this.m_universe < o.m_universe) {
			return -1;
		} else if (this.m_universe > o.m_universe) {
			return 1;
		} else {
			return 0;
		}
	}
}