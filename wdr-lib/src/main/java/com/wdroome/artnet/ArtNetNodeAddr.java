package com.wdroome.artnet;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import com.wdroome.util.inet.InetUtil;

/*
 * The logical identifier for a node: the bind IP address, bind index
 * and UDP address to send requests to. The unique key is the combination
 * of "bind address" and "bind index."
 */
public class ArtNetNodeAddr implements Comparable<ArtNetNodeAddr>
{
	public final Inet4Address m_rootAddr;	// Address of root node.
	public final int m_index;				// Index of this node. Starts with 1. Never 0.
	public final InetSocketAddress m_nodeAddr;	// IP address of this node. Never null.	
	
	/**
	 * Create a new logical node address.
	 * @param bindAddr The node's "bind" address. If 0 or null, use srcAddr.
	 * @param index The node's "bind" index. If 0, use 1.
	 * @param pollIpAddr The address in the ArtNetPollReply msg. May be all zero.
	 * @param port The port on which the sender listens. If 0, use the Art-Net port.
	 * @param sender The node's "send" socket address. Never null.
	 */
	public ArtNetNodeAddr(Inet4Address bindAddr, int index, Inet4Address pollIpAddr,
							int port, InetSocketAddress sender)
	{
		if (ArtNetMsg.isZeroIpAddr(bindAddr)) {
			bindAddr = pollIpAddr;
			if (ArtNetMsg.isZeroIpAddr(bindAddr)) {
				if (sender == null) {
					throw new IllegalArgumentException("ArtNetNodeAddr(): No sender address.");
				}
				InetAddress addr = sender.getAddress();
				if (addr instanceof Inet4Address) {
					bindAddr = (Inet4Address)addr;
				} else {
					throw new IllegalArgumentException("ArtNetNodeAddr(): sender not IPv4");
				}
			}
		}
		m_rootAddr = bindAddr;
		m_index = index > 0 ? index : 1;
		InetAddress sendAddr = pollIpAddr;
		if (ArtNetMsg.isZeroIpAddr(pollIpAddr)) {
			sendAddr = sender.getAddress();
		}
		m_nodeAddr = new InetSocketAddress(sendAddr, port != 0 ? port : ArtNetConst.ARTNET_PORT);
	}

	@Override
	public int compareTo(ArtNetNodeAddr o)
	{
		if (o == null) {
			return 1;
		}
		int cmp = InetUtil.compare(m_rootAddr, o.m_rootAddr);
		if (cmp != 0) {
			return cmp;
		}
		return Integer.compare(m_index, o.m_index);
	}

	@Override
	public String toString()
	{
		return m_rootAddr.getHostAddress() + ":" + m_nodeAddr.getPort() + "[" + m_index + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + m_index;
		result = prime * result + ((m_rootAddr == null) ? 0 : m_rootAddr.hashCode());
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
		ArtNetNodeAddr other = (ArtNetNodeAddr) obj;
		if (m_index != other.m_index)
			return false;
		if (m_rootAddr == null) {
			if (other.m_rootAddr != null)
				return false;
		} else if (!m_rootAddr.equals(other.m_rootAddr))
			return false;
		return true;
	}
}
