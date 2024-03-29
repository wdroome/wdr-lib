package com.wdroome.artnet;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.wdroome.util.inet.InetUtil;

import com.wdroome.artnet.msgs.ArtNetMsgUtil;

/*
 * The logical identifier for a node: the bind IP address, bind index
 * and UDP address to send requests to. The unique key is the combination
 * of "bind address" and "bind index."
 * Art-Net (TM) Designed by and Copyright Artistic License Holdings Ltd.
 */
public class ArtNetNodeAddr implements Comparable<ArtNetNodeAddr>
{
	public final Inet4Address m_rootAddr;	// Address of root node.
	public final int m_index;				// Index of this node. Starts with 1. Never 0.
	public final InetSocketAddress m_nodeAddr;	// IP address of this node, for sending messages.
												// Never null.	
	
	/**
	 * Create a new logical node address from fields in a reply message from the node.
	 * @param bindAddr The node's "bind" address. If 0 or null, use pollAddr or fromAddr.
	 * @param index The node's "bind" index. If 0, use 1.
	 * @param pollIpAddr The address in the ArtNetPollReply msg. May be all zero.
	 * @param port The port on which the sender listens. If 0, use the Art-Net port.
	 * @param fromAddr The node's "send" socket address. Never null.
	 */
	public ArtNetNodeAddr(Inet4Address bindAddr, int index, Inet4Address pollIpAddr,
							int port, Inet4Address fromAddr)
	{
		if (ArtNetMsgUtil.isZeroIpAddr(bindAddr)) {
			bindAddr = pollIpAddr;
			if (ArtNetMsgUtil.isZeroIpAddr(bindAddr)) {
				bindAddr = fromAddr;
			}
		}
		if (ArtNetMsgUtil.isZeroIpAddr(bindAddr)) {
			throw new IllegalArgumentException("ArtNetNodeAddr(): No 'bind' address.");			
		}
		m_rootAddr = bindAddr;
		m_index = index > 0 ? index : 1;
		InetAddress sendAddr = pollIpAddr;
		if (ArtNetMsgUtil.isZeroIpAddr(pollIpAddr)) {
			sendAddr = fromAddr;
		}
		m_nodeAddr = new InetSocketAddress(sendAddr, port != 0 ? port : ArtNetConst.ARTNET_PORT);
	}
	
	/**
	 * Create a new logical address from just and address and a bind index.
	 * Use the default ArtNet port.
	 * @param addr The IP address.
	 * @param index The bind index. If 0, use 1.
	 */
	public ArtNetNodeAddr(Inet4Address addr, int index)
	{
		m_rootAddr = addr;
		m_index = index > 0 ? index : 1;
		m_nodeAddr = new InetSocketAddress(addr, ArtNetConst.ARTNET_PORT);
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
		return m_rootAddr.getHostAddress() + "[" + m_index + "]";
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
