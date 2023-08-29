package com.wdroome.artnet;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;

import java.net.InetSocketAddress;

import com.wdroome.util.inet.InetUtil;
import com.wdroome.artnet.msgs.ArtNetMsgUtil;
import com.wdroome.artnet.msgs.ArtNetPollReply;
import com.wdroome.artnet.util.ArtNetTestNode.PollReplyHandler;

/**
 * A "merged" ArtNet node. That is, all sub-nodes with the same IP address.
 * In the original ArtNet spec, a node could only describe 4 ports in a Poll Reply messages.
 * Nodes with more than 4 ports had to send several reply messages,
 * using the same IP address but different "bind addresses".
 * Later protocol versions recommended that a node send a separate
 * Poll Reply for each port. This class "merges" the Poll Reply messages
 * for a multi-port node into a single object with all of the node's ports.
 * Note that this class is primarily for nodes with DMX output ports.
 * @author wdr
 * @see ArtNetNode
 * @see ArtNetPollReply
 */
public class MergedArtNetNode implements Comparable<MergedArtNetNode>
{
	/** Node name. */
	public final String m_name;
	
	/** Node's poll response time, in milliseconds. */
	public final long m_responseMS;
	
	public final InetSocketAddress m_socketAddr;
	
	public final boolean m_artnet3or4;
	public final boolean m_usesDHCP;
	public final boolean m_browserConfig;
	
	public final List<NodePort> m_allPorts;
	public final List<DMXOutPort> m_dmxOutPorts;
	
	public final List<ArtNetUniv> m_dmxOutUnivs;
	
	public static enum MergeMode { LTP, HTP };
	
	/**
	 * A physical port (e.g., a DMX connector) for a Merged Node.
	 * @see DMXOutPort
	 */
	public class NodePort implements Comparable<NodePort>
	{
		/** Physical port number, starting with 1. */
		public final int m_portNum;
	
		/** The node's reply message. */
		public final ArtNetPollReply m_pollReply;
		
		/** This port's index (0-3) in the poll reply message. */
		public final int m_pollReplyPortIndex;
		
		private NodePort(int portNum, ArtNetPollReply pollReply, int portIndex)
		{
			m_portNum = portNum;
			m_pollReply = pollReply;
			m_pollReplyPortIndex = portIndex;
		}
		
		@Override
		public String toString()
		{
			return "Port[" + m_portNum + "]";
		}
		
		public String toFmtString(StringBuilder b)
		{
			if (b == null) {
				b = new StringBuilder();
			}
			b.append("Port[" + m_portNum + "]");
			return b.toString();
		}

		@Override
		public int compareTo(NodePort o)
		{
			return o != null ? Integer.compare(m_portNum, o.m_portNum) : -1;
		}

		@Override
		public int hashCode() {
			return m_portNum;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			return m_portNum == ((NodePort)obj).m_portNum;
		}
	}
	
	/**
	 * A subclass of NodePort for a DMX output port on a Merged Node.
	 */
	public class DMXOutPort extends NodePort
	{
		// The ArtNet universe for this port.
		public final ArtNetUniv m_univ;
		
		// Merge mode for port.
		public final MergeMode m_mergeMode;
		
		// True iff port supports RDM. Does not mean there it has RDM devices, though.
		public final boolean m_supportsRDM;
		
		private DMXOutPort(int portNum, ArtNetPollReply pollReply, int portIndex)
		{
			super(portNum, pollReply, portIndex);
			m_univ = pollReply.m_outPorts[portIndex];
			m_mergeMode = (pollReply.m_goodOutput[portIndex] & ArtNetPollReply.GOOD_OUTPUT_LTP) != 0
							? MergeMode.LTP : MergeMode.HTP;
			m_supportsRDM = (pollReply.m_status2 & ArtNetPollReply.STATUS2_ARTNET_3OR4) != 0
						&& (pollReply.m_goodOutputB[portIndex] & ArtNetPollReply.GOOD_OUTPUTB_RDM_DISABLED) == 0;
		}
		
		@Override
		public String toString()
		{
			return "OutPort[" + m_portNum + "]:" + m_univ + ","
							+ m_mergeMode.toString().toLowerCase() + (m_supportsRDM ? ",rdm" : "")
							+ "]";
		}
		
		public String toFmtString(StringBuilder b)
		{
			if (b == null) {
				b = new StringBuilder();
			}
			b.append("DmxOutPort[" + m_portNum + "]: " + m_univ
					+ " " + m_mergeMode.toString().toLowerCase()
					+ (m_supportsRDM ? " rdm" : ""));
			return b.toString();
		}
	}
	
	/**
	 * Create a Merged Node from all of node's Poll Reply response messages.
	 * @param nodes The subnodes created from the nodes replies.
	 * @see MergedArtNetNode#makeMergedNodes(Collection).
	 */
	private MergedArtNetNode(Collection<ArtNetNode> nodes)
	{
		if (nodes == null || nodes.isEmpty()) {
			throw new IllegalArgumentException("MergedArtNetNode c'tor: no nodes!");			
		}
		InetSocketAddress sockAddr = null;
		String name = null;
		long respMS = -1;
		boolean artnet3or4 = false;
		boolean usesDHCP = false;
		boolean browserConfig = false;
		List<NodePort> allPorts = new ArrayList<>();
		List<DMXOutPort> outputPorts = new ArrayList<>();
		List<ArtNetUniv> dmxOutUnivs = new ArrayList<>();
		
		for (ArtNetNode node: nodes) {
			ArtNetPollReply reply = node.m_reply;
			respMS = Math.max(respMS, node.m_responseMS);
			if (sockAddr == null) {
				sockAddr = node.getNodeAddr().m_nodeAddr;
				name = reply.m_longName;
				if (name.isBlank()) {
					name = reply.m_shortName;
				}
				artnet3or4 = (reply.m_status2 & ArtNetPollReply.STATUS2_ARTNET_3OR4) != 0;
				usesDHCP = (reply.m_status2 
								& (ArtNetPollReply.STATUS2_SET_BY_DHCP | ArtNetPollReply.STATUS2_DHCP_CAPABLE))
							== (ArtNetPollReply.STATUS2_SET_BY_DHCP | ArtNetPollReply.STATUS2_DHCP_CAPABLE);
				browserConfig = (reply.m_status2 & ArtNetPollReply.STATUS2_BROWSER_CONFIG) != 0;
			} else if (!sockAddr.equals(node.getNodeAddr().m_nodeAddr)) {
				throw new IllegalArgumentException("MergedArtNetNode c'tor: different addresses "
							+ sockAddr + " != " + node.getNodeAddr().m_nodeAddr);
			}
			for (int iPort = 0; iPort < reply.m_numPorts; iPort++) {
				int bindIndex = reply.m_bindIndex;
				if (bindIndex == 0) {
					bindIndex = 1;
				}
				if ((reply.m_portTypes[iPort] & ArtNetPollReply.PORT_TYPE_OUTPUT) != 0
					&& (reply.m_portTypes[iPort] & ArtNetPollReply.PORT_TYPE_PROTO_MASK) ==
							ArtNetPollReply.PORT_TYPE_PROTO_DMX512) {
					DMXOutPort outPort = new DMXOutPort(bindIndex + iPort, reply, iPort);
					if (!outputPorts.contains(outPort)) {
						outputPorts.add(outPort);
						allPorts.add(outPort);
						if (!dmxOutUnivs.contains(outPort.m_univ)) {
							dmxOutUnivs.add(outPort.m_univ);
						}
					}
				} else {
					NodePort nodePort = new NodePort(bindIndex + iPort, reply, iPort);
					if (!allPorts.contains(nodePort)) {
						allPorts.add(nodePort);
					}
				}
			}
		}

		allPorts.sort(null);
		outputPorts.sort(null);
		dmxOutUnivs.sort(null);

		m_socketAddr = sockAddr;
		m_responseMS = respMS;
		m_artnet3or4 = artnet3or4;
		m_usesDHCP = usesDHCP;
		m_browserConfig = browserConfig;
		allPorts.sort(null);
		m_allPorts = List.copyOf(allPorts);
		m_dmxOutPorts = List.copyOf(outputPorts);
		m_dmxOutUnivs = List.copyOf(dmxOutUnivs);
		m_name = name != null && !name.isBlank() ? name : InetUtil.toAddrPort(m_socketAddr);
	}

	/**
	 * Compare by the node's IP Socket Address.
	 */
	@Override
	public int compareTo(MergedArtNetNode o)
	{
		return o != null ? InetUtil.compare(m_socketAddr, o.m_socketAddr) : 1;
	}
	
	@Override
	public int hashCode() {
		return ((m_socketAddr == null) ? 0 : m_socketAddr.hashCode());
	}

	/**
	 * Merged Nodes are equal iff their socket addresses are equal.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MergedArtNetNode other = (MergedArtNetNode) obj;
		if (m_socketAddr == null) {
			if (other.m_socketAddr != null)
				return false;
		} else if (!m_socketAddr.equals(other.m_socketAddr))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		StringBuilder b = new StringBuilder(300);
		b.append("MergedArtNetNode{" + InetUtil.toAddrPort(m_socketAddr) + ",");
		ArtNetMsgUtil.append(b, "name", m_name);
		ArtNetMsgUtil.append(b, "respMS", m_responseMS);
		ArtNetMsgUtil.append(b, "ports", m_dmxOutPorts.size() + "/" + m_allPorts.size());
		ArtNetMsgUtil.append(b, "vers", m_artnet3or4 ? "3/4" : "1/2");
		if (m_usesDHCP) {
			b.append("dhcp,");
		}
		ArtNetMsgUtil.append(b, "dmxOutUnivs", m_dmxOutUnivs.toString());
		String sep = "";
		for (NodePort nodePort: m_allPorts) {
			b.append(sep + nodePort);
			sep = ",";
		}
		b.append("}");
		return b.toString();
	}
	
	/**
	 * Return a formatted description of this node and it's ports.
	 * @param b If not null, append the description to this StringBuilder.
	 * 			If null, append the description to a new StringBuilder
	 * @return The StringBuilder b, or the newly created one, as a String.
	 */
	public String toFmtString(StringBuilder b)
	{
		if (b == null) {
			b = new StringBuilder();
		}
		b.append((!m_name.isBlank() ? ("\"" + m_name + "\"" + " ") : "")
				+ InetUtil.toAddrPort(m_socketAddr)
				+ (m_artnet3or4 ? " artNet v3/4" : "")
				+ (m_usesDHCP ? " dhcp" : "")
				+ (m_browserConfig ? " browser-config" : "")
				+ " time: " + m_responseMS + "ms\n");
		for (NodePort port: m_allPorts) {
			b.append("    ");
			port.toFmtString(b);
			b.append("\n");
		}
		return b.toString();
	}
	
	/**
	 * Merge the nodes found by polling into a set of unique Merged Nodes.
	 * @param allNodes All nodes found by polling. There nay be duplicates.
	 * @return A set with the unique MergedNodes.
	 */
	public static Set<MergedArtNetNode> makeMergedNodes(Collection<ArtNetNode> allNodes)
	{
		Map<InetSocketAddress,Set<ArtNetNode>> nodesByAddr = new HashMap<>();
		for (ArtNetNode node: allNodes) {
			Set<ArtNetNode> nodeSet = nodesByAddr.get(node.m_reply.m_nodeAddr.m_nodeAddr);
			if (nodeSet == null) {
				nodeSet = new HashSet<>();
				nodesByAddr.put(node.m_reply.m_nodeAddr.m_nodeAddr, nodeSet);
			}
			nodeSet.add(node);
		}
		TreeSet<MergedArtNetNode> mergedNodes = new TreeSet<>();
		for (Set<ArtNetNode> nodeSet: nodesByAddr.values()) {
			mergedNodes.add(new MergedArtNetNode(nodeSet));
		}
		return mergedNodes;
	}
}
