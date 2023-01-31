package com.wdroome.artnet.legacy;

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
import java.util.TreeMap;
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
import com.wdroome.artnet.ArtNetPortAddr;
import com.wdroome.artnet.ACN_UID;

import com.wdroome.artnet.msgs.ArtNetMsg;
import com.wdroome.artnet.msgs.ArtNetTodRequest;
import com.wdroome.artnet.msgs.ArtNetTodData;
import com.wdroome.artnet.msgs.ArtNetRdm;
import com.wdroome.artnet.msgs.ArtNetTodControl;
import com.wdroome.artnet.msgs.RdmPacket;
import com.wdroome.artnet.msgs.RdmParamId;

/**
 * Send ArtNet TOD Request Messages to find the UIDs of all RDM devices in the network.
 * Art-Net (TM) Designed by and Copyright Artistic License Holdings Ltd.
 * @author wdr
 */
public class ArtNetFindRdmUids implements ArtNetChannel.Receiver
{
	public static final long MIN_REPLY_WAIT_MS = 500;
	public static final long MAX_REPLY_WAIT_MS = 15000;
	public static final long DEF_REPLY_WAIT_MS = 3000;
	
	private long m_replyWaitMS = DEF_REPLY_WAIT_MS;
	private IErrorLogger m_errorLogger = new SystemErrorLogger();
	
	private long m_sendTS = 0;
	
	private boolean m_prtReplies = false;
	
	private ArtNetChannel m_sharedChannel = null;
	
	// Shared between poll() and Receiver methods.
	private final AtomicReference<Map<ArtNetPortAddr, Set<ACN_UID>>> m_uidMap = new AtomicReference<>();

	/**
	 * Create the poller. The c'tor doesn't do anything, but I like to define them anyway.
	 */
	public ArtNetFindRdmUids()
	{
		this(null);
	}
	
	/**
	 * Create the poller using the caller's ArtNetChannel.
	 * @param sharedChannel The channel to use. If null, create and close a channel.
	 */
	public ArtNetFindRdmUids(ArtNetChannel sharedChannel)
	{
		m_sharedChannel = sharedChannel;
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
	 * Broadcast ArtNet TodRequest messages for the UIDs,
	 * wait for the replies, and return a Map from each node/port
	 * to the UIDs of the devices on that port.
	 * @param ports The ArtNet ports provided by the nodes. 
	 * @return A Map from each node/port to device UIDs on that port,
	 * 		or null of there was an error.
	 * @see ArtNetFindNodes
	 */
	public Map<ArtNetPortAddr, Set<ACN_UID>> getUidMap(Collection<ArtNetPort> ports)
	{
		if (ports == null || ports.isEmpty()) {
			ports = getAllPorts();
			if (ports == null || ports.isEmpty()) {
				// No ports. Return empty UID map.
				return new HashMap<>();
			}
		}
		// List<ArtNetPortAddr> portAddrs = getAllPortAddrs();
		m_uidMap.set(new TreeMap<>());
		boolean closeChan;
		ArtNetChannel chan;
		if (m_sharedChannel != null) {
			chan = m_sharedChannel;
			closeChan = false;
			chan.addReceiver(this);
		} else {
			try {
				chan = new ArtNetChannel(this);
			} catch (IOException e) {
				m_errorLogger.logError("ArtNetFindNodes: exception creating channel: " + e);
				return null;
			}
			closeChan = true;
		}
		try {
			if (true) {
				m_sendTS = System.currentTimeMillis();
				for (ArtNetPort port : ports) {
					ArtNetTodControl todCtlReq = new ArtNetTodControl();
					todCtlReq.m_net = port.m_net;
					todCtlReq.m_command = ArtNetTodControl.COMMAND_ATC_FLUSH;
					todCtlReq.m_subnetUniv = port.subUniv();
					try {
						if (!chan.broadcast(todCtlReq)) {
							m_errorLogger.logError("ArtNetFindRdmUids: send TODControl failed.");
						}
					} catch (IOException e1) {
						m_errorLogger.logError("ArtNetFindRdmUids: Exception sending TODControl: " + e1);
					}
				} 
			}
			MiscUtil.sleep(m_replyWaitMS);
			
			// This was before I realized the node is supposed to send TODData messages
			// in response to TODControl requests. I've left it in, just in case.
			if (false) {
				m_sendTS = System.currentTimeMillis();
				// For simplicity, send one message per universe, rather than trying to pack
				// several universes into the request.
				for (ArtNetPort port : ports) {
					ArtNetTodRequest todReq = new ArtNetTodRequest();
					todReq.m_net = port.m_net;
					todReq.m_command = ArtNetTodRequest.COMMAND_TOD_FULL;
					todReq.m_numSubnetUnivs = 1;
					todReq.m_subnetUnivs[0] = (byte) port.subUniv();
					try {
						if (!chan.broadcast(todReq)) {
							m_errorLogger.logError("ArtNetFindRdmUids: send TODReq failed.");
						}
					} catch (IOException e1) {
						m_errorLogger.logError("ArtNetFindRdmUids: Exception sending TODReq: " + e1);
					}
				}
				MiscUtil.sleep(m_replyWaitMS);
			}
		} finally {
			if (chan != null) {
				if (closeChan) {
					chan.shutdown();
				} else {
					chan.dropReceiver(this);
				}
			}
		}
		return m_uidMap.getAndSet(null);
	}
	
	private Collection<ArtNetPort> getAllPorts()
	{
		ArtNetFindNodes.Results results = new ArtNetFindNodes(m_sharedChannel).poll();
		return results != null ? List.copyOf(results.m_portsToNodes.keySet()) : null;		
	}
	
	private List<ArtNetPortAddr> getAllPortAddrs()
	{
		ArtNetFindNodes.Results results = new ArtNetFindNodes(m_sharedChannel).poll();
		return results != null ? results.m_allNodePorts : null;				
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
			ArtNetPortAddr nodePort = new ArtNetPortAddr(nodeAddr, anPort);
			if (todData.m_command == ArtNetTodRequest.COMMAND_TOD_NAK) {
				System.out.println("ArtNetFindRdmUids: NAK from " + nodePort + " #uids=" + todData.m_numUids);
			}
			if (m_prtReplies) {
				System.out.println("TODData: sender=" + sender
							+ " time=" + (System.currentTimeMillis() - m_sendTS) + "ms");
				System.out.println("   " + todData.toFmtString(null, "   "));
			}
			Map<ArtNetPortAddr, Set<ACN_UID>> map = m_uidMap.get();
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
		} else if (msg instanceof ArtNetRdm) {
			RdmPacket rdmPacket = ((ArtNetRdm)msg).m_rdmPacket;
			if (rdmPacket != null && rdmPacket.m_command == RdmPacket.CMD_GET_RESP && m_prtReplies) {
				System.out.println();
				System.out.println(msg);
			}
		} else {
			// ignore
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
	 * Stand-alone main. Arguments are a list of ArtNet ports or internet addresses or socket addresses.
	 * Send a TODRequest message to each internet address for each of those ArtNet ports.
	 * Print the UIDs of the discovered RDM devices on standard output.
	 * @param args ArtNet ports and/or ethernet addresses.
	 * @throws IOException If an I/O error occurs.
	 */
	public static void main(String[] args) throws IOException
	{
		int nRepeats = 1;	// repeats are useful for testing.
		List<ArtNetPort> artNetPorts = new ArrayList<ArtNetPort>();
		List<InetAddress> bcastAddrs = new ArrayList<>();
		boolean prtReplies = false;
		for (String arg: args) {
			if (arg.startsWith("-a")) {
				prtReplies = true;
			} else if (arg.matches("[0-9]+\\.[0-9]+\\.[0-9]+")) {
				artNetPorts.add(new ArtNetPort(arg));
			} else {
				bcastAddrs.add(InetAddress.getByName(arg));
			}
		}
		ArtNetChannel channel = new ArtNetChannel();
		if (!bcastAddrs.isEmpty()) {
			channel.setBroadcastAddrs(bcastAddrs);
		}
		ArtNetFindRdmUids uidFinder = new ArtNetFindRdmUids(channel);
		uidFinder.setPrtReplies(prtReplies);
		Map<ArtNetPortAddr, Set<ACN_UID>> uidMap;
		System.out.print("Sending ArtNet TOD requests to");
		for (InetAddress addr: channel.getBroadcastAddrs()) {
			System.out.print(" " + addr.getHostAddress());
		}
		System.out.println(":");
		System.out.flush();
		uidMap = uidFinder.getUidMap(artNetPorts);
		for (Map.Entry<ArtNetPortAddr, Set<ACN_UID>> ent: uidMap.entrySet()) {
			System.out.println(ent.getKey() + ": " + ent.getValue());
		}
		
		/*
		 * Test code to send DEVICE_INFO requests to all UIDs.
		 * 
		channel.addReceiver(uidFinder);
		for (Map.Entry<ArtNetPortAddr, Set<ACN_UID>> ent: uidMap.entrySet()) {
			ArtNetPortAddr node = ent.getKey();
			for (ACN_UID uid: ent.getValue()) {
				System.out.println("Sending DEVICE_INFO to " + node.m_port + " " + uid + ":");
				RdmPacket rdmPacket = new RdmPacket(uid, RdmPacket.CMD_GET,
											RdmParamId.DEVICE_INFO, null);
				ArtNetRdm rdmReq = new ArtNetRdm();
				rdmReq.m_net = node.m_port.m_net;
				rdmReq.m_subnetUniv = node.m_port.subUniv();
				rdmReq.m_rdmPacket = rdmPacket;
				channel.broadcast(rdmReq);
				System.out.println("XXX: sent");
			}
		}
		MiscUtil.sleep(10000);
		System.out.println("DONE");
		*/
	}
}
