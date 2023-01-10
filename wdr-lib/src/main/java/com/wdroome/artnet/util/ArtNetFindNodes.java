package com.wdroome.artnet.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.wdroome.util.MiscUtil;
import com.wdroome.util.IErrorLogger;
import com.wdroome.util.SystemErrorLogger;
import com.wdroome.util.inet.InetInterface;
import com.wdroome.util.inet.InetUtil;

import com.wdroome.artnet.ArtNetConst;
import com.wdroome.artnet.ArtNetPort;
import com.wdroome.artnet.ArtNetNode;
import com.wdroome.artnet.ArtNetNodeAddr;
import com.wdroome.artnet.ArtNetOpcode;
import com.wdroome.artnet.ArtNetChannel;

import com.wdroome.artnet.msgs.ArtNetMsg;
import com.wdroome.artnet.msgs.ArtNetPoll;
import com.wdroome.artnet.msgs.ArtNetPollReply;

/**
 * Send ArtNet Poll Messages to find the nodes in the network.
 * Art-Net (TM) Designed by and Copyright Artistic License Holdings Ltd.
 * @author wdr
 */
public class ArtNetFindNodes implements ArtNetChannel.Receiver
{
	public static final long MIN_REPLY_WAIT_MS = 50;
	public static final long MAX_REPLY_WAIT_MS = 15000;
	
	/**
	 * The cooked results of polling.
	 */
	public static class Results
	{
		/** All replies. Unordered, may have duplicates. */
		public final List<ArtNetNode> m_allNodes;
		
		/** Unique nodes, sorted. */
		public final Set<ArtNetNode> m_uniqueNodes;
		
		/** Map from ArtNet Ports to Nodes. */
		public final Map<ArtNetPort, Set<ArtNetNode>> m_portsToNodes;
		
		public Results(List<ArtNetNode> allNodes)
		{
			m_allNodes = allNodes;
			m_uniqueNodes = ArtNetNode.getUniqueNodes(allNodes);
			m_portsToNodes = ArtNetNode.getDmxPort2NodeMap(allNodes);
		}
	}
	
	private long m_replyWaitMS = ArtNetConst.MAX_POLL_REPLY_MS;
	private IErrorLogger m_errorLogger = new SystemErrorLogger();
	
	private long m_sendTS = 0;
	
	private List<Integer> m_listenPorts = null;
	private List<Integer> m_sendPorts = null;
	private List<InetAddress> m_sendInetAddrs = null;
	private List<InetSocketAddress> m_sendSockAddrs = null;
	
	private ArtNetChannel m_sharedChannel = null;
	
	// Shared between poll() and Receiver methods.
	private final AtomicReference<List<ArtNetNode>> m_nodes = new AtomicReference<>();

	/**
	 * Create the poller.
	 */
	public ArtNetFindNodes()
	{
		this(null);
	}
	
	/**
	 * Create the poller using the caller's ArtNetChannel.
	 * @param sharedChannel The channel to use. If null, create and close a channel.
	 */
	public ArtNetFindNodes(ArtNetChannel sharedChannel)
	{
		m_sharedChannel = sharedChannel;
	}
	
	/**
	 * Setup m_sendInetAddrs, if not already setup.
	 */
	private void setupParam()
	{
		if (m_listenPorts == null || m_listenPorts.isEmpty()) {
			m_listenPorts = List.of(ArtNetConst.ARTNET_PORT);
		}
		if (m_sendSockAddrs != null && !m_sendSockAddrs.isEmpty()) {
			return;
		}
		if (m_sendPorts == null || m_sendPorts.isEmpty()) {
			m_sendPorts = List.of(ArtNetConst.ARTNET_PORT);
		}
		if (m_sendInetAddrs == null || m_sendInetAddrs.isEmpty()) {
			m_sendInetAddrs = new ArrayList<>();
			for (InetInterface iface: InetInterface.getBcastInterfaces()) {
				if (!iface.m_isLoopback && iface.m_broadcast instanceof Inet4Address) {
					m_sendInetAddrs.add(iface.m_broadcast);
				}
			}
		}
		m_sendSockAddrs = new ArrayList<>();
		for (InetAddress addr: m_sendInetAddrs) {
			for (int port: m_sendPorts) {
				m_sendSockAddrs.add(new InetSocketAddress(addr, port));
			}
		}
	}
	
	/**
	 * Set the error logger. The default is {@link SystemErrorLogger}.
	 * @param errorLogger New error logger.
	 * 		If null, use the previous logger -- we must have a logger.
	 * @return The previous error logger.
	 */
	public IErrorLogger setErrorLogger(IErrorLogger errorLogger)
	{
		IErrorLogger prevLogger = m_errorLogger;
		if (errorLogger != null) {
			m_errorLogger = errorLogger;
		}
		return prevLogger;
	}
	
	/**
	 * Set the time to wait for all replies. Default is the time specified in the ArtNet protocol.
	 * @param replyWaitMS The wait time, in milliseconds. Values outside of MIN and MAX are ignored.
	 * @return The previous wait time.
	 */
	public long setReplyWaitMS(long replyWaitMS)
	{
		long prevWait = m_replyWaitMS;
		if (replyWaitMS >= MIN_REPLY_WAIT_MS && replyWaitMS <= MAX_REPLY_WAIT_MS) {
			m_replyWaitMS = replyWaitMS;
		}
		return prevWait;
	}
	
	/**
	 * Set the ports on which poll messages will be sent.
	 * @param ports The ports. The class copies the list.
	 */
	public void setPorts(List<Integer> ports)
	{
		m_sendPorts = List.copyOf(ports);
		m_sendSockAddrs = null;
	}
	
	/**
	 * Set the inet addresses on which poll messages will be sent.
	 * It will be send to each port in the port list.
	 * @param addrs The addresses. The class copies the list.
	 */
	public void setInetAddrs(List<InetAddress> addrs)
	{
		m_sendInetAddrs = List.copyOf(addrs);
		m_sendSockAddrs = null;
	}
	
	/**
	 * Set the socket addresses on which poll() will send ArtNetPoll messages.
	 * This overrides the addresses and ports
	 * set by {@link #setInetAddrs(List)} and {@link #setPorts(List)}.
	 * @param addrs The socket addresses. The class makes a shallow copy of this list.
	 */
	public void setSockAddrs(List<InetSocketAddress> addrs)
	{
		m_sendSockAddrs = List.copyOf(addrs);
	}
	
	/**
	 * Return the socket addresses to which poll() will send ArtNetPoll messages.
	 * @return The socket addresses to which poll() will send ArtNetPoll messages.
	 */
	public List<InetSocketAddress> getSockAddrs()
	{
		setupParam();
		return List.copyOf(m_sendSockAddrs);
	}
	
	/**
	 * Send ArtNet poll messages to the specified addresses,
	 * wait for the replies, and return all of them.
	 * @return The replies, or null if there was an error.
	 */
	public Results poll()
	{
		m_nodes.set(new ArrayList<ArtNetNode>());
		setupParam();
		boolean closeChan;
		ArtNetChannel chan;
		if (m_sharedChannel != null) {
			chan = m_sharedChannel;
			closeChan = false;
			chan.addReceiver(this);
		} else {
			try {
				chan = new ArtNetChannel(this, m_listenPorts);
			} catch (IOException e) {
				m_errorLogger.logError("ArtNetFindNodes: exception creating channel listenPorts="
						+ m_listenPorts + ": " + e);
				return null;
			}
			closeChan = true;
		}
		try {
			m_sendTS = System.currentTimeMillis();
			for (InetSocketAddress addr : m_sendSockAddrs) {
				ArtNetPoll msg = new ArtNetPoll();
				msg.m_talkToMe |= ArtNetPoll.FLAGS_SEND_REPLY_ON_CHANGE;
				// System.out.println("XXX: poll msg: " + msg);
				try {
					if (!chan.send(msg, addr)) {
						m_errorLogger.logError("ArtNetFindNodes/" + addr + ": send failed.");
					}
				} catch (IOException e) {
					m_errorLogger.logError("ArtNetFindNodes/" + addr + ": Exception sending Poll: ");
				}
			} 
			MiscUtil.sleep(m_replyWaitMS);
		} finally {
			if (chan != null) {
				if (closeChan) {
					chan.shutdown();
				} else {
					chan.dropReceiver(this);
				}
			}
		}
		return new Results(m_nodes.getAndSet(null));
	}

	@Override
	public void msgArrived(ArtNetChannel chan, ArtNetMsg msg,
					InetSocketAddress sender, InetSocketAddress receiver)
	{
		if (msg instanceof ArtNetPollReply) {
			// System.out.println("XXX: msg from " + sender);
			ArtNetNode nodeInfo = new ArtNetNode((ArtNetPollReply)msg, System.currentTimeMillis() - m_sendTS,
								sender);
			List<ArtNetNode> replies = m_nodes.get();
			if (replies != null) {
				replies.add(nodeInfo);
			}
		}
	}

	@Override
	public void msgArrived(ArtNetChannel chan, ArtNetOpcode opcode, byte[] buff, int len, InetSocketAddress sender,
			InetSocketAddress receiver)
	{
		// ignore
	}

	@Override
	public void msgArrived(ArtNetChannel chan, byte[] msg, int len, InetSocketAddress sender,
			InetSocketAddress receiver)
	{
		// ignore
	}
	
	/**
	 * Stand-alone main. Arguments are a list of internet addresses or socket addresses
	 * to which the method sends poll messages.
	 * If the port is omitted, we use the ArtNet port.
	 * A summary of the poll replies is printed on standard output.
	 * @param args Internet or socket addresses to send polls to.
	 * @throws IOException If an I/O error occurs.
	 */
	public static void main(String[] args) throws IOException
	{
		int nRepeats = 1;	// repeats are useful for testing.
		boolean prtAllReplies = false;
		boolean prtRawReplies = false;
		ArtNetFindNodes poller = new ArtNetFindNodes();
		Results results;
		for (String arg: args) {
			if (arg.startsWith("-a")) {
				prtAllReplies = true;
			} else if (arg.startsWith("-r")) {
				prtRawReplies = true;
			}

		}
		if (args.length > 0) {
			List<InetSocketAddress> sockAddrs = new ArrayList<>();
			for (String arg: args) {
				if (!arg.startsWith("-")) {
					sockAddrs.add(InetUtil.parseAddrPort(arg, ArtNetConst.ARTNET_PORT));
				}
			}
			poller.setSockAddrs(sockAddrs);
		}
		for (int i = 0; i < nRepeats; i++) {
			if (i > 0) {
				System.out.println();
				MiscUtil.sleep(1);
			}
			System.out.print("Sending polls to");
			for (InetSocketAddress sockAddr: poller.getSockAddrs()) {
				System.out.print(" " + InetUtil.toAddrPort(sockAddr));
			}
			System.out.println(" ....");
			System.out.flush();
			results = poller.poll();
			if (prtAllReplies) {
				Collections.sort(results.m_allNodes);
				System.out.println(results.m_allNodes.size() + " replies:");
				for (ArtNetNode ni : results.m_allNodes) {
					System.out.println(ni.toString());
					// ni.m_reply.print(System.out, "");
					if (prtRawReplies) {
						System.out.println("  " + ni.m_reply.toString());
					}
				}
				System.out.println();
			}
			System.out.println(results.m_uniqueNodes.size() + " unique nodes:");
			for (ArtNetNode ni: results.m_uniqueNodes) {
				System.out.println(ni.toString());
			}
			System.out.println();
			
			System.out.println(results.m_portsToNodes.keySet().size() + " DMX Output Ports: ");
			for (Map.Entry<ArtNetPort, Set<ArtNetNode>> ent: results.m_portsToNodes.entrySet()) {
				System.out.print("  " + ent.getKey() + ":");
				for (ArtNetNode ni: ent.getValue()) {
					System.out.print(" " + ni.m_reply.m_nodeAddr);
				}
				System.out.println();
			}
		}
		System.out.println("DONE");
	}
}
