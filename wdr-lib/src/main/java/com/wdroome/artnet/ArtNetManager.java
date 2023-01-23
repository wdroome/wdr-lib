package com.wdroome.artnet;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.wdroome.artnet.msgs.ArtNetMsg;
import com.wdroome.artnet.msgs.ArtNetPoll;
import com.wdroome.artnet.msgs.ArtNetPollReply;
import com.wdroome.artnet.msgs.ArtNetTodRequest;
import com.wdroome.artnet.msgs.ArtNetTodData;
import com.wdroome.artnet.msgs.ArtNetTodControl;

import com.wdroome.util.IErrorLogger;
import com.wdroome.util.MiscUtil;
import com.wdroome.util.SystemErrorLogger;
import com.wdroome.util.inet.InetInterface;

public class ArtNetManager
{
	public static final long MIN_POLL_REPLY_MS = 500;
	public static final long MAX_POLL_REPLY_MS = 15000;
	public static final long DEF_POLL_REPLY_MS = ArtNetConst.MAX_POLL_REPLY_MS;
	
	public static final long MIN_TOD_DATA_MS = 1000;
	public static final long MAX_TOD_DATA_MS = 120000;
	public static final long DEF_TOD_DATA_MS = 10000;
	
	private final MonitorSync m_monitorSync = new MonitorSync();
	
	// Use this channel if not null. If null, create & close a channel as needed.
	private ArtNetChannel m_sharedChannel = null;
	
	private long m_pollReplyWaitMS = DEF_POLL_REPLY_MS;
	private long m_todDataWaitMS = DEF_TOD_DATA_MS;
	
	private IErrorLogger m_errorLogger = new SystemErrorLogger();

	private List<Integer> m_listenPorts = null;
	
	private boolean m_findRdmUids = true;
	private boolean m_prtReplies = false;
	
	private List<Integer> m_pollPorts = null;
	private List<InetAddress> m_pollInetAddrs = null;
	private List<InetSocketAddress> m_pollSockAddrs = null;
	
	// Objects are ArtNetPollReply, ArtNetTodData, and MonitorCmd enum.
	private final ArrayBlockingQueue<Object> m_monitorCmds = new ArrayBlockingQueue<>(200);
	
	private enum MonitorCmd { Refresh, Shutdown; }

	public ArtNetManager()
	{
		this(null);
	}
	
	public ArtNetManager(ArtNetChannel sharedChannel)
	{
		m_sharedChannel = sharedChannel;
	}
	
	public boolean refresh()
	{
		return m_monitorSync.refresh();
	}
	
	public List<ArtNetNode> getAllNodes()
	{
		return m_monitorSync.getAllNodes();
	}

	public Set<ArtNetNode> getUniqueNodes()
	{
		return m_monitorSync.getUniqueNodes();
	}

	public Map<ArtNetPort, Set<ArtNetNode>> getPortsToNodes()
	{
		return m_monitorSync.getPortsToNodes();
	}

	public Set<ArtNetPort> getAllPorts()
	{
		return m_monitorSync.getAllPorts();
	}

	public List<ArtNetPortAddr> getAllNodePorts()
	{
		return m_monitorSync.getAllNodePorts();
	}

	public Map<ArtNetPortAddr, Set<ACN_UID>> getUidMap()
	{
		return m_monitorSync.getUidMap();
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
	 * Set the time to wait for ArtNet POLL_REPLY messages. Default is the time specified
	 * in the ArtNet protocol.
	 * @param replyWaitMS The wait time, in milliseconds. Values outside of MIN and MAX are ignored.
	 * @return The previous wait time.
	 */
	public long setPollReplyWaitMS(long replyWaitMS)
	{
		long prevWait = m_pollReplyWaitMS;
		if (replyWaitMS >= MIN_POLL_REPLY_MS && replyWaitMS <= MAX_POLL_REPLY_MS) {
			m_pollReplyWaitMS = replyWaitMS;
		}
		return prevWait;
	}
	
	/**
	 * Set the ports on which ArtNetPoll messages will be sent.
	 * @param ports The ports. The class copies the list.
	 */
	public void setPorts(List<Integer> ports)
	{
		m_pollPorts = List.copyOf(ports);
		m_pollSockAddrs = null;
	}
	
	/**
	 * Set the inet addresses on which ArtNetPoll messages will be sent.
	 * It will be send to each port in the port list.
	 * @param addrs The addresses. The class copies the list.
	 */
	public void setInetAddrs(List<InetAddress> addrs)
	{
		m_pollInetAddrs = List.copyOf(addrs);
		m_pollSockAddrs = null;
	}
	
	/**
	 * Set the socket addresses on which ArtNetPoll messages will be sent.
	 * This overrides the addresses and ports
	 * set by {@link #setInetAddrs(List)} and {@link #setPorts(List)}.
	 * @param addrs The socket addresses. The class makes a shallow copy of this list.
	 */
	public void setSockAddrs(List<InetSocketAddress> addrs)
	{
		m_pollSockAddrs = List.copyOf(addrs);
	}
	
	/**
	 * Return the socket addresses to which ArtNetPoll messages will be sent.
	 * @return The socket addresses to which ArtNetPoll messages will be sent.
	 */
	public List<InetSocketAddress> getSockAddrs()
	{
		setupParam();
		return List.copyOf(m_pollSockAddrs);
	}
	
	/**
	 * Test whether this manager finds RDM UIDs.
	 * @return True iff this manager finds RMD UIDs.
	 */
	public boolean isFindRdmUids()
	{
		return m_findRdmUids;
	}

	/**
	 * Control whether the manager finds UIDs for RDM devices.
	 * This can take longer than finding nodes.
	 * The default is true.
	 * @param findRdmUids Whether or not the manager should find the UIDs for all RDM devices.
	 */
	public void setFindRdmUids(boolean findRdmUids)
	{
		this.m_findRdmUids = findRdmUids;
	}

	/**
	 * Control whether the manager prints all replies as they arrive.
	 * @param prtReplies If true, print all replies.
	 * @return The previous value.
	 */
	public boolean setPrtReplies(boolean prtReplies)
	{
		boolean prevValue = m_prtReplies;
		m_prtReplies = prtReplies;
		return prevValue;
	}

	/**
	 * Setup m_pollSockAddrs, if not already setup.
	 */
	private void setupParam()
	{
		if (m_listenPorts == null || m_listenPorts.isEmpty()) {
			m_listenPorts = List.of(ArtNetConst.ARTNET_PORT);
		}
		if (m_pollSockAddrs != null && !m_pollSockAddrs.isEmpty()) {
			return;
		}
		if (m_pollPorts == null || m_pollPorts.isEmpty()) {
			m_pollPorts = List.of(ArtNetConst.ARTNET_PORT);
		}
		if (m_pollInetAddrs == null || m_pollInetAddrs.isEmpty()) {
			m_pollInetAddrs = new ArrayList<>();
			for (InetInterface iface: InetInterface.getBcastInterfaces()) {
				if (!iface.m_isLoopback && iface.m_broadcast instanceof Inet4Address) {
					m_pollInetAddrs.add(iface.m_broadcast);
				}
			}
		}
		m_pollSockAddrs = new ArrayList<>();
		for (InetAddress addr: m_pollInetAddrs) {
			for (int port: m_pollPorts) {
				m_pollSockAddrs.add(new InetSocketAddress(addr, port));
			}
		}
	}
	
	private class MonitorSync
	{
		private boolean m_isValid = false;
		
		private List<ArtNetNode> m_allNodes = null;
		private Set<ArtNetNode> m_uniqueNodes = null;
		private Map<ArtNetPort, Set<ArtNetNode>> m_portsToNodes = null;
		private Set<ArtNetPort> m_allPorts = null;
		private List<ArtNetPortAddr> m_allNodePorts = null;
		private Map<ArtNetPortAddr, Set<ACN_UID>> m_uidMap = null;
		
		private synchronized List<ArtNetNode> getAllNodes()
		{
			if (!m_isValid) {
				refresh();
			}
			return m_allNodes;
		}

		private synchronized Set<ArtNetNode> getUniqueNodes()
		{
			if (!m_isValid) {
				refresh();
			}
			return m_uniqueNodes;
		}

		private synchronized Map<ArtNetPort, Set<ArtNetNode>> getPortsToNodes()
		{
			if (!m_isValid) {
				refresh();
			}
			return m_portsToNodes;
		}

		private synchronized Set<ArtNetPort> getAllPorts()
		{
			if (!m_isValid) {
				refresh();
			}
			return m_allPorts;
		}

		private synchronized List<ArtNetPortAddr> getAllNodePorts()
		{
			if (!m_isValid) {
				refresh();
			}
			return m_allNodePorts;
		}

		private synchronized Map<ArtNetPortAddr, Set<ACN_UID>> getUidMap()
		{
			if (!m_isValid) {
				refresh();
			}
			return m_uidMap;
		}
		
		private synchronized boolean refresh() 
		{
			try {
				m_monitorCmds.put(MonitorCmd.Refresh);
				wait();
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	
		private synchronized void done(
						List<ArtNetNode> allNodes,
						Set<ArtNetNode> uniqueNodes,
						Map<ArtNetPort, Set<ArtNetNode>> portsToNodes,
						Set<ArtNetPort> allPorts,
						List<ArtNetPortAddr> allNodePorts,
						Map<ArtNetPortAddr, Set<ACN_UID>> uidMap	
						)
		{
			m_allNodes = allNodes;
			m_uniqueNodes = uniqueNodes;
			m_portsToNodes = portsToNodes;
			m_allPorts = allPorts;
			m_allNodePorts = allNodePorts;
			m_uidMap = uidMap;
			m_isValid = true;
			notifyAll();
		}
	}
	
	private class Monitor extends Thread implements ArtNetChannel.Receiver
	{
		private List<ArtNetNode> m_allNodes = null;
		private Set<ArtNetNode> m_uniqueNodes = null;
		private Map<ArtNetPortAddr, Set<ACN_UID>> m_uidMap = null;
		
		private long m_startPollTS = 0;
		private long m_pollDoneTS = 0;
		private boolean m_polling = false;
		private ArtNetChannel m_chan = m_sharedChannel;
		private boolean m_closeChan = false;

		private Monitor()
		{
			setName("ArtNetManager.MonitorThread");
			setDaemon(true);
			start();
		}

		public void run()
		{
			boolean running = true;
			while (running) {
				Object cmd = null;
				try {
					cmd = m_monitorCmds.poll(1000, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					m_errorLogger.logError("ArtNetManager.Monitor interrupted: " + e);
					running = false;
					break;
				}
				if (cmd == null) {
					if (m_polling && System.currentTimeMillis() > m_pollDoneTS) {
						stopPolling();
					}
				} else if (cmd instanceof MonitorCmd) {
					switch ((MonitorCmd)cmd) {
					case Refresh:
						startPolling();
						break;
					case Shutdown:
						running = false;
						break;
					}
				} else if (cmd instanceof ArtNetPollReply) {
					handlePollReply((ArtNetPollReply)cmd);
				} else if (cmd instanceof ArtNetTodData) {
					handleTodData((ArtNetTodData)cmd);
				}
			}
		}
		
		private void startPolling()
		{
			if (m_polling) {
				return;
			}
			m_allNodes = new ArrayList<>();
			m_uniqueNodes = new TreeSet<>();
			m_uidMap = new TreeMap<>();
			if (m_sharedChannel != null) {
				m_chan = m_sharedChannel;
				m_closeChan = false;
				m_chan.addReceiver(this);
			} else {
				try {
					m_chan = new ArtNetChannel(this, m_listenPorts);
				} catch (IOException e) {
					m_errorLogger.logError("ArtNetManager: exception creating channel listenPorts="
							+ m_listenPorts + ": " + e);
					return;
				}
				m_closeChan = true;
			}
			m_startPollTS = System.currentTimeMillis();
			m_pollDoneTS = m_startPollTS + m_pollReplyWaitMS + (m_findRdmUids ? m_todDataWaitMS : 0);
			for (InetSocketAddress addr: m_pollSockAddrs) {
				ArtNetPoll msg = new ArtNetPoll();
				msg.m_talkToMe |= ArtNetPoll.FLAGS_SEND_REPLY_ON_CHANGE;
				// System.out.println("XXX: poll msg: " + msg);
				try {
					if (!m_chan.send(msg, addr)) {
						m_errorLogger.logError("ArtNetManager/" + addr + ": send failed.");
					}
				} catch (IOException e) {
					m_errorLogger.logError("ArtNetManager/" + addr + ": Exception sending Poll: ");
				}
			}
		}
		
		private void stopPolling()
		{
			if (m_polling) {
				System.out.println("XXX: Stop polling.");
				Map<ArtNetPort, Set<ArtNetNode>> portsToNodes
								= ArtNetNode.getDmxPort2NodeMap(m_allNodes);
				m_monitorSync.done(
						m_allNodes,
						ArtNetNode.getUniqueNodes(m_allNodes),
						portsToNodes,
						portsToNodes.keySet(),
						ArtNetNode.getNodePorts(m_allNodes),
						m_uidMap);
				m_polling = false;
				if (m_closeChan) {
					m_chan.shutdown();
				} else {
					m_chan.dropReceiver(this);
				}
				m_chan = null;
			}
		}
		
		private void handlePollReply(ArtNetPollReply msg)
		{
			if (m_prtReplies) {
				System.out.println("PollReply: from=" + msg.getFromAddr().getHostAddress()
							+ " time=" + (System.currentTimeMillis() - m_startPollTS) + "ms");
				System.out.println("   " + msg.toFmtString(null, "   "));
			}
			ArtNetNode nodeInfo = new ArtNetNode((ArtNetPollReply)msg,
								System.currentTimeMillis() - m_startPollTS);
			m_allNodes.add(nodeInfo);
			if (m_uniqueNodes.add(nodeInfo) && m_findRdmUids) {
				for (ArtNetPort port: nodeInfo.m_dmxOutputPorts) {
					ArtNetTodControl todCtlReq = new ArtNetTodControl();
					todCtlReq.m_net = port.m_net;
					todCtlReq.m_command = ArtNetTodControl.COMMAND_ATC_FLUSH;
					todCtlReq.m_subnetUniv = port.subUniv();
					try {
						if (!m_chan.send(todCtlReq, nodeInfo.getNodeAddr().m_nodeAddr)) {
							m_errorLogger.logError("ArtNetManager: send TODControl failed.");
						}
					} catch (IOException e1) {
						m_errorLogger.logError("ArtNetManager: Exception sending TODControl: " + e1);
					}							
				}
			}
		}
		
		private void handleTodData(ArtNetTodData msg)
		{
			ArtNetTodData todData = (ArtNetTodData)msg;
			InetAddress fromAddr = msg.getFromAddr();
			if (todData.m_rdmVers != ArtNetTodRequest.RDM_VERS) {
				System.out.println("ArtNetManager: Illegal RDM version " + todData.m_rdmVers
							+ " from " + fromAddr);
				return;
			}
			System.out.println("XXX: TodData from " + fromAddr.getHostAddress() + " " + msg);
			if (!(fromAddr instanceof Inet4Address)) {
				System.out.println("ArtNetManager: Non-IP4 sender " + fromAddr);
				return;
			}
			ArtNetNodeAddr nodeAddr = new ArtNetNodeAddr((Inet4Address)fromAddr, todData.m_bindIndex);
			ArtNetPort anPort = new ArtNetPort(todData.m_net, todData.m_subnetUniv);
			ArtNetPortAddr nodePort = new ArtNetPortAddr(nodeAddr, anPort);
			if (todData.m_command == ArtNetTodRequest.COMMAND_TOD_NAK) {
				System.out.println("ArtNetManager: NAK from " + nodePort + " #uids=" + todData.m_numUids);
			}
			if (m_prtReplies) {
				System.out.println("TODData: from=" + fromAddr.getHostAddress()
							+ " time=" + (System.currentTimeMillis() - m_startPollTS) + "ms");
				System.out.println("   " + todData.toFmtString(null, "   "));
			}
			Set<ACN_UID> uids = m_uidMap.get(nodePort);
			if (uids == null) {
				uids = new HashSet<>();
				m_uidMap.put(nodePort, uids);
			}
			for (int i = 0; i < todData.m_numUids; i++) {
				uids.add(todData.m_uids[i]);
			}
		}

		@Override
		public void msgArrived(ArtNetChannel chan, ArtNetMsg msg, InetSocketAddress sender,
								InetSocketAddress receiver)
		{
			if (msg instanceof ArtNetPollReply || msg instanceof ArtNetTodData) {
				try {
					m_monitorCmds.put(msg);
				} catch (InterruptedException e) {
					m_errorLogger.logError("ArtNetManager.msgArrived: interrupt putting on queue.");
				}
			}
		}

		@Override
		public void msgArrived(ArtNetChannel chan, ArtNetOpcode opcode, byte[] buff, int len,
								InetSocketAddress sender, InetSocketAddress receiver)
		{
			// Ignore
		}

		@Override
		public void msgArrived(ArtNetChannel chan, byte[] msg, int len, InetSocketAddress sender,
								InetSocketAddress receiver)
		{
			// Ignore
		}
	}
}
