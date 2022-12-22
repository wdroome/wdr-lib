package com.wdroome.artnet;

import java.net.InetSocketAddress;

/**
 * Information about an Art-Net node, as discovered by polling.
 * This is based on the node's poll response(s).
 * Art-Net (TM) Designed by and Copyright Artistic License Holdings Ltd.
 * @author wdr
 */
public class ArtNetNode implements Comparable<ArtNetNode>
{
	/** The node's ArtNetPoll reply message. */
	public final ArtNetPollReply m_reply;
	
	/** Node's response time, in milliseconds. */
	public final long m_responseMS;
	
	/** Remote node's address and port. */
	public final InetSocketAddress m_sender;
	
	/**
	 * Create an ArtNetNode.
	 * @param reply The poll reply.
	 * @param responseMS The node's response time, in millisec.
	 * @param sender The node's socket address.
	 */
	public ArtNetNode(ArtNetPollReply reply, long responseMS, InetSocketAddress sender)
	{
		m_sender = sender;
		m_responseMS = responseMS;
		m_reply = reply;
	}
	
	/**
	 * Return a nicely formatted multi-line summary of this node.
	 */
	@Override
	public String toString()
	{
		StringBuilder b = new StringBuilder();
		b.append("Node " + m_reply.m_nodeAddr + " time: " + m_responseMS + "ms\n");
		return m_reply.toFmtString(b, "  ");
	}

	/**
	 * Compare based on the NodeAddr's, namely the root-address and bind index.
	 */
	@Override
	public int compareTo(ArtNetNode o)
	{
		if (o == null) {
			return -1;
		}
		return m_reply.m_nodeAddr.compareTo(o.m_reply.m_nodeAddr);
	}
	
	@Override
	public int hashCode() 
	{
		return m_reply.m_nodeAddr.hashCode();
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) {
			return true;
		}
		if (obj == null || !(obj instanceof ArtNetNode)) {
			return false;
		}
		return m_reply.m_nodeAddr.equals(((ArtNetNode)obj).m_reply.m_nodeAddr);
	}

}
