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
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;
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
import com.wdroome.artnet.ArtNetNodePort;
import com.wdroome.artnet.ArtNetNodePort;
import com.wdroome.artnet.ACN_UID;

import com.wdroome.artnet.msgs.ArtNetMsg;
import com.wdroome.artnet.msgs.ArtNetTodRequest;
import com.wdroome.artnet.msgs.ArtNetTodData;

/**
 * Send ArtNet TOD Request Messages to find the UIDs of all RDM devices in the network.
 * Art-Net (TM) Designed by and Copyright Artistic License Holdings Ltd.
 * @author wdr
 */
public class ArtNetFindRdmUids implements ArtNetChannel.Receiver
{
	public static final long MIN_REPLY_WAIT_MS = 50;
	public static final long MAX_REPLY_WAIT_MS = 15000;
	
	private long m_replyWaitMS = MAX_REPLY_WAIT_MS;
	private IErrorLogger m_errorLogger = new SystemErrorLogger();
	
	private long m_sendTS = 0;
	
	private List<Integer> m_listenPorts = null;
	private List<Integer> m_sendPorts = null;
	private List<InetAddress> m_sendInetAddrs = null;
	private List<InetSocketAddress> m_sendSockAddrs = null;
	private boolean m_prtReplies = false;
	
	// Shared between poll() and Receiver methods.
	private final AtomicReference<Map<ArtNetNodePort, Set<ACN_UID>>> m_uidMap = new AtomicReference<>();

	/**
	 * Create the poller. The c'tor doesn't do anything, but I like to define them anyway.
	 */
	public ArtNetFindRdmUids()
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
	}
	
	public boolean setPrtReplies(boolean prtReplies)
	{
		boolean prevValue = m_prtReplies;
		m_prtReplies = prtReplies;
		return prevValue;
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
	public Map<ArtNetNodePort, Set<ACN_UID>> poll(Collection<ArtNetPort> ports)
	{
		m_uidMap.set(new HashMap<>());
		setupParam();
		ArtNetChannel chan = null;
		try {
			try {
				chan = new ArtNetChannel(this, m_listenPorts);
			} catch (IOException e1) {
				m_errorLogger.logError("ArtNetFindRdmUids: exception creating channel listenPorts="
									+ m_listenPorts + ": " + e1);
				return null;
			}
			m_sendTS = System.currentTimeMillis();
			for (ArtNetPort port: ports) {
				ArtNetTodRequest todReq = new ArtNetTodRequest();
				todReq.m_net = port.m_net;
				todReq.m_command = ArtNetTodRequest.COMMAND_TOD_FULL;
				todReq.m_numSubnetUnivs = 1;
				todReq.m_subnetUnivs[0] = (byte)port.subUniv();
				for (InetSocketAddress addr : m_sendSockAddrs) {
					// System.out.println("XXX: todRequest: " + todRequest);
					try {
						if (!chan.send(todReq, addr)) {
							m_errorLogger.logError("ArtNetFindRdmUids/" + addr + ": send failed.");
						}
					} catch (IOException e) {
						m_errorLogger.logError("ArtNetFindRdmUids/" + addr + ": Exception sending Poll: ");
					}
				}
			}
			MiscUtil.sleep(m_replyWaitMS);
		} finally {
			if (chan != null) {
				chan.shutdown();
			}
		}
		return m_uidMap.getAndSet(null);
	}

	@Override
	public void msgArrived(ArtNetChannel chan, ArtNetMsg msg,
					InetSocketAddress sender, InetSocketAddress receiver)
	{
		if (msg instanceof ArtNetTodData) {
			// System.out.println("XXX: msg from " + sender);
			ArtNetTodData todData = (ArtNetTodData)msg;
			if (todData.m_rdmVers != ArtNetTodRequest.RDM_VERS) {
				System.out.println("ArtNetFindRdmUids: Illegal RDM version " + todData.m_rdmVers
							+ " from " + sender);
				return;
			}
			InetAddress fromAddr = sender.getAddress();
			if (!(fromAddr instanceof Inet4Address)) {
				System.out.println("ArtNetFindRdmUids: Non-IP4 sender " + sender);
				return;
			}
			ArtNetNodeAddr nodeAddr = new ArtNetNodeAddr((Inet4Address)fromAddr, todData.m_bindIndex);
			ArtNetPort anPort = new ArtNetPort(todData.m_net, todData.m_subnetUniv);
			ArtNetNodePort nodePort = new ArtNetNodePort(nodeAddr, anPort);
			if (todData.m_command == ArtNetTodRequest.COMMAND_TOD_NAK) {
				System.out.println("ArtNetFindRdmUids: NAK from " + nodePort + " #uids=" + todData.m_numUids);
			}
			if (m_prtReplies) {
				System.out.println("TODData: sender=" + sender
							+ " time=" + (System.currentTimeMillis() - m_sendTS) + "ms");
				System.out.println("   " + todData.toFmtString(null, "   "));
			}
			Map<ArtNetNodePort, Set<ACN_UID>> map = m_uidMap.get();
			if (map != null) {
				Set<ACN_UID> uids = map.get(nodePort);
				if (uids == null) {
					uids = new HashSet<>();
					map.put(nodePort, uids);
				}
				for (int i = 0; i < todData.m_numUids; i++) {
					uids.add(todData.m_uids[i]);
				}
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
		ArtNetFindRdmUids poller = new ArtNetFindRdmUids();
		List<ArtNetPort> artNetPorts = new ArrayList<ArtNetPort>();
		List<InetSocketAddress> sockAddrs = new ArrayList<>();
		for (String arg: args) {
			if (arg.startsWith("-a")) {
				poller.setPrtReplies(true);
			} else if (arg.matches("[0-9]+\\.[0-9]+\\.[0-9]+")) {
				artNetPorts.add(new ArtNetPort(arg));
			} else {
				sockAddrs.add(InetUtil.parseAddrPort(arg, ArtNetConst.ARTNET_PORT));
			}
		}
		if (artNetPorts.isEmpty()) {
			for (String p: new String[] {"0.0.0", "0.0.1", "1.0.0", "1.0.1"}) {
				artNetPorts.add(new ArtNetPort(p));
			}
		}
		if (!sockAddrs.isEmpty()) {
			poller.setSockAddrs(sockAddrs);
		}
		Map<ArtNetNodePort, Set<ACN_UID>> uidMap;
		for (int i = 0; i < nRepeats; i++) {
			if (i > 0) {
				System.out.println();
				MiscUtil.sleep(1);
			}
			System.out.print("Sending polls to");
			for (InetSocketAddress sockAddr: poller.getSockAddrs()) {
				System.out.print(" " + InetUtil.toAddrPort(sockAddr));
			}
			System.out.println();
			System.out.print("for ports");
			for (ArtNetPort p: artNetPorts) {
				System.out.print(" " + p);
			}
			System.out.println(" ....");
			System.out.flush();
			uidMap = poller.poll(artNetPorts);
			for (Map.Entry<ArtNetNodePort, Set<ACN_UID>> ent: uidMap.entrySet()) {
				System.out.println(ent.getKey() + ": " + ent.getValue());
			}
		}
		System.out.println("DONE");
	}
}
