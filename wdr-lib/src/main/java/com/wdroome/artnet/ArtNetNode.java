package com.wdroome.artnet;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Collection;
import java.util.Collections;

import com.wdroome.artnet.msgs.ArtNetPollReply;

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
	
	/** Node's ArtNet DMX output universes, if any. */
	public final List<ArtNetUniv> m_dmxOutputUnivs;
	
	/**
	 * Node's ArtNet universes which support RDM, if any.
	 * This doesn't mean the universe actually any RDM devices.
	 */
	public final Set<ArtNetUniv> m_dmxRdmUnivs;
	
	/**
	 * Create an ArtNetNode.
	 * @param reply The poll reply.
	 * @param responseMS The node's response time, in millisec.
	 * @param sender The node's socket address.
	 */
	public ArtNetNode(ArtNetPollReply reply, long responseMS)
	{
		m_responseMS = responseMS;
		m_reply = reply;
		m_dmxOutputUnivs = new ArrayList<>();
		m_dmxRdmUnivs = new HashSet<>();
		for (int i = 0; i < m_reply.m_numPorts; i++) {
			int portType = m_reply.m_portTypes[i];
			ArtNetUniv port = m_reply.getOutputPort(i);
			if (((portType & ArtNetPollReply.PORT_TYPE_OUTPUT) != 0)
					&& ((portType & ArtNetPollReply.PORT_TYPE_PROTO_MASK)
									== ArtNetPollReply.PORT_TYPE_PROTO_DMX512)) {
				m_dmxOutputUnivs.add(port);
				if ((m_reply.m_status2 & ArtNetPollReply.STATUS2_ARTNET_3OR4) != 0
						&& (m_reply.m_goodOutputB[i] & ArtNetPollReply.GOOD_OUTPUTB_RDM_DISABLED) == 0) {
					m_dmxRdmUnivs.add(port);
				}
			}
		}
	}
	
	/**
	 * Return this node's NodeAddress. Yes, it's in m_reply, but it's buried there.
	 * @return
	 */
	public ArtNetNodeAddr getNodeAddr()
	{
		return m_reply.m_nodeAddr;
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
	
	/**
	 * Given a list of possibly duplicate nodes, return the unique nodes as a sorted set.
	 * @param nodes The nodes.
	 * @return A sorted set with the unique nodes.
	 */
	public static Set<ArtNetNode> getUniqueNodes(Collection<ArtNetNode> nodes)
	{
		TreeSet<ArtNetNode> uniqueNodes = new TreeSet<>();
		if (nodes != null) {
			for (ArtNetNode ni: nodes) {
				uniqueNodes.add(ni);
			} 
		}
		return uniqueNodes;
	}
	
	/**
	 * Given a list of discovered nodes, return a sorted map from each distinct ArtNet port
	 * to the set of the nodes which can output DMX on that port.
	 * @param nodes The nodes.
	 * @return A map from each ArtNetUniv to a set of nodes that can output DMX on that port.
	 * 		The map is only defined for ports supported by some node.
	 */
	public static Map<ArtNetUniv, Set<ArtNetNode>> getDmxPort2NodeMap(Collection<ArtNetNode> nodes)
	{
		Map<ArtNetUniv, Set<ArtNetNode>> map = new TreeMap<>();
		if (nodes != null) {
			for (ArtNetNode ni: nodes) {
				for (ArtNetUniv port: ni.m_dmxOutputUnivs) {
					Set<ArtNetNode> portNodes = map.get(port);
					if (portNodes == null) {
						portNodes = new HashSet<ArtNetNode>();
						map.put(port, portNodes);
					}
					portNodes.add(ni);					
				}
			}
		}
		return map;
	}
	
	/**
	 * Return a sorted list of all unique universe/node-ip-addr pairs.
	 * @param nodes The nodes.
	 * @return A sorted list of all unique universe/node-ip-addr pairs.
	 */
	public static List<ArtNetUnivAddr> getUnivAddrs(Collection<ArtNetNode> nodes)
	{
		ArrayList<ArtNetUnivAddr> univAddrs = new ArrayList<>();
		if (nodes != null) {
			for (ArtNetNode ni: nodes) {
				for (ArtNetUniv univ: ni.m_dmxOutputUnivs) {
					ArtNetUnivAddr univAddr = new ArtNetUnivAddr(ni.m_reply.m_nodeAddr, univ);
					if (!univAddrs.contains(univAddr)) {
						univAddrs.add(univAddr);
					}					
				}
			}
		}
		Collections.sort(univAddrs);
		return univAddrs;
	}
}
