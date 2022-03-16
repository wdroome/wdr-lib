package com.wdroome.artnet.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.wdroome.util.MiscUtil;
import com.wdroome.util.IErrorLogger;
import com.wdroome.util.SystemErrorLogger;
import com.wdroome.util.inet.InetInterface;
import com.wdroome.util.inet.InetUtil;

import com.wdroome.artnet.ArtNetConst;
import com.wdroome.artnet.ArtNetPort;
import com.wdroome.artnet.ArtNetMsg;
import com.wdroome.artnet.ArtNetOpcode;
import com.wdroome.artnet.ArtNetPoll;
import com.wdroome.artnet.ArtNetPollReply;
import com.wdroome.artnet.ArtNetChannel;

/**
 * Send ArtNet Poll Messages to discover the nodes in the network.
 * Art-Net (TM) Designed by and Copyright Artistic License Holdings Ltd.
 * @author wdr
 */
public class ArtNetPoller implements ArtNetChannel.Receiver
{
	public static final long MIN_REPLY_WAIT_MS = 50;
	public static final long MAX_REPLY_WAIT_MS = 15000;
	
	private long m_replyWaitMS = ArtNetConst.MAX_POLL_REPLY_MS;
	private IErrorLogger m_errorLogger = new SystemErrorLogger();
	
	private long m_sendTS = 0;
	
	private List<Integer> m_listenPorts = null;
	private List<Integer> m_sendPorts = null;
	private List<InetAddress> m_sendInetAddrs = null;
	private List<InetSocketAddress> m_sendSockAddrs = null;
	
	/**
	 * Information about a discovered node.
	 */
	public static class NodeInfo
	{
		/** The node's ArtNetPoll reply message. */
		public final ArtNetPollReply m_reply;
		
		/** Node's response time, in milliseconds. */
		public final long m_responseMS;
		
		/** Remote node's address and port. */
		public final InetSocketAddress m_sender;
		
		/** Address and port to which the remote node sent the reply. Note the address may be 0.0.0.0. */
		public final InetSocketAddress m_receiver;
		
		/**
		 * Create a NodeInfo.
		 * @param reply The poll reply.
		 * @param responseMS The node's response time, in millisec.
		 * @param sender The node's socket address.
		 * @param receiver The local socket address to which the response was sent.
		 */
		public NodeInfo(ArtNetPollReply reply, long responseMS,
						InetSocketAddress sender, InetSocketAddress receiver)
		{
			m_sender = sender;
			m_responseMS = responseMS;
			m_reply = reply;
			m_receiver = receiver;
		}
		
		/**
		 * Return a nicely formatted multi-line summary of this node.
		 */
		@Override
		public String toString()
		{
			StringBuilder b = new StringBuilder();
			b.append("Reply src: " + InetUtil.toAddrPort(m_sender) + " time: " + m_responseMS + "ms\n");
			return m_reply.toFmtString(b, "  ");
		}
	}

	// Shared between poll() and Receiver methods.
	private final AtomicReference<List<NodeInfo>> m_pollReplies = new AtomicReference<>();

	/**
	 * Create the poller. The c'tor doesn't do anything, but I like to define them anyway.
	 */
	public ArtNetPoller()
	{
		// Just in case we need something ...
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
				m_sendInetAddrs.add(iface.m_broadcast);
			}
		}
		m_sendSockAddrs = new ArrayList<>();
		for (InetAddress addr: m_sendInetAddrs) {
			for (int port: m_sendPorts) {
				m_sendSockAddrs.add(new InetSocketAddress(addr, port));
			}
		}
		MiscUtil.sleep(ArtNetConst.MAX_POLL_REPLY_MS);
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
	public List<NodeInfo> poll()
	{
		m_pollReplies.set(new ArrayList<NodeInfo>());
		setupParam();
		ArtNetChannel chan = null;
		try {
			try {
				chan = new ArtNetChannel(this, m_listenPorts);
			} catch (IOException e1) {
				m_errorLogger.logError("ArtNetPoller: exception creating channel listenPorts="
									+ m_listenPorts + ": " + e1);
				return null;
			}
			m_sendTS = System.currentTimeMillis();
			for (InetSocketAddress addr : m_sendSockAddrs) {
				ArtNetPoll msg = new ArtNetPoll();
				try {
					if (!chan.send(msg, addr)) {
						m_errorLogger.logError("ArtNetPoller/" + addr + ": send failed.");
					}
				} catch (IOException e) {
					m_errorLogger.logError("ArtNetPoller/" + addr + ": Exception sending Poll: ");
				}
			} 
			MiscUtil.sleep(m_replyWaitMS);
		} finally {
			if (chan != null) {
				chan.shutdown();
			}
		}
		return m_pollReplies.getAndSet(null);
	}

	@Override
	public void msgArrived(ArtNetChannel chan, ArtNetMsg msg,
					InetSocketAddress sender, InetSocketAddress receiver) {
		if (msg instanceof ArtNetPollReply) {
			// System.out.println("XXX: msg from " + sender);
			NodeInfo nodeInfo = new NodeInfo((ArtNetPollReply)msg, System.currentTimeMillis() - m_sendTS,
								sender, receiver);
			List<NodeInfo> replies = m_pollReplies.get();
			if (replies != null) {
				replies.add(nodeInfo);
			}
		}
	}

	@Override
	public void msgArrived(ArtNetChannel chan, ArtNetOpcode opcode, byte[] buff, int len, InetSocketAddress sender,
			InetSocketAddress receiver) {
		// ignore
	}

	@Override
	public void msgArrived(ArtNetChannel chan, byte[] msg, int len, InetSocketAddress sender,
			InetSocketAddress receiver) {
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
		ArtNetPoller poller = new ArtNetPoller();
		List<NodeInfo> replies;
		if (args.length > 0) {
			List<InetSocketAddress> sockAddrs = new ArrayList<>();
			for (String arg: args) {
				sockAddrs.add(InetUtil.parseAddrPort(arg, ArtNetConst.ARTNET_PORT));
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
			replies = poller.poll();
			System.out.println(replies.size() + " replies:");
			for (NodeInfo ni : replies) {
				System.out.println(ni.toString());
				// ni.m_reply.print(System.out, "");
			}
		}
		System.out.println("DONE");
	}
}
