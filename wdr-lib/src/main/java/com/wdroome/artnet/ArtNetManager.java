package com.wdroome.artnet;

import java.io.IOException;
import java.io.Closeable;
import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
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
import com.wdroome.artnet.msgs.RdmParamId;
import com.wdroome.artnet.msgs.ArtNetTodData;
import com.wdroome.artnet.msgs.ArtNetTodControl;
import com.wdroome.artnet.msgs.RdmPacket;
import com.wdroome.artnet.msgs.RdmParamResp;
import com.wdroome.artnet.util.ArtNetTestNode;
import com.wdroome.util.IErrorLogger;
import com.wdroome.util.SystemErrorLogger;
import com.wdroome.util.ImmutableMap;
import com.wdroome.util.ImmutableSet;
import com.wdroome.util.inet.InetInterface;
import com.wdroome.util.inet.InetUtil;

/**
 * Poll the Art-Net network to discover the nodes and RDM devices.
 * Art-Net (TM) Designed by and Copyright Artistic License Holdings Ltd.
 * @author wdr
 */
public class ArtNetManager implements Closeable
{
	public static final long MIN_POLL_REPLY_MS = 500;
	public static final long MAX_POLL_REPLY_MS = 15000;
	public static final long DEF_POLL_REPLY_MS = ArtNetConst.MAX_POLL_REPLY_MS;
	
	public static final long MIN_TOD_DATA_MS = 1000;
	public static final long MAX_TOD_DATA_MS = 120000;
	public static final long DEF_TOD_DATA_MS = 10000;
	
	private final MonitorSync m_monitorSync = new MonitorSync();
	private ArtNetRdmRequest m_rdmRequest = null;
	private final MonitorThread m_monitorThread;
	
	private final ArtNetChannel m_channel;
	private final boolean m_isSharedChannel;
	
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

	/**
	 * Create a new manager. This c'tor creates and destroys an ArtNetChannel as needed.
	 * @throws IOException 
	 */
	public ArtNetManager() throws IOException
	{
		this(null);
	}
	
	/**
	 * Create a new manager using an existing ArtNetChannel.
	 * @param sharedChannel The channel to use. If null, create and destroy a new channel as needed.
	 * @throws IOException 
	 */
	public ArtNetManager(ArtNetChannel sharedChannel) throws IOException
	{
		// System.out.println("XXX: ArtNetManager c'tor");
		if (sharedChannel != null) {
			m_channel = sharedChannel;
			m_isSharedChannel = true;
		} else {
			m_channel = new ArtNetChannel();
			m_isSharedChannel = false;
		}
		m_monitorThread = new MonitorThread();
		m_channel.addReceiver(m_monitorThread);
	}
	
	/**
	 * Close the manager. If it's using a shared ArtNetChannel, disconnect from that channel.
	 * If the manager created it's own ArtNetChannel, close it.
	 */
	@Override
	public void close() throws IOException
	{
		// System.out.println("ArtNetManager: close XXX");
		m_monitorSync.shutdown();
		if (!m_isSharedChannel) {
			m_channel.shutdown();
		}
	}

	/**
	 * Send ArtNetPoll and ArtNetTodControl messages to discover the nodes and devices.
	 * This method blocks until discovery is complete.
	 * Discovery is timeout-based: the method waits a fixed time,
	 * and assumes all nodes will reply within that time.
	 * @see #setPollReplyWaitMS(long)
	 * @see #setTodDataWaitMS(long)
	 * @return True if discovery was successful.
	 */
	public boolean refresh()
	{
		return m_monitorSync.refresh();
	}
	
	/**
	 * Return an immutable List of all nodes. This may include duplicates.
	 * @return An immutable List of all nodes, possibly including duplicates.
	 */
	public List<ArtNetNode> getAllNodes()
	{
		return m_monitorSync.getAllNodes();
	}

	/**
	 * Return a sorted immutable Set of all unique nodes.
	 * @return A sorted immutable Set of all unique nodes.
	 */
	public Set<ArtNetNode> getUniqueNodes()
	{
		return m_monitorSync.getUniqueNodes();
	}

	/**
	 * Return a sorted immutable Set of all unique merged nodes.
	 * @return A sorted immutable Set of all unique merged nodes.
	 */
	public Set<MergedArtNetNode> getMergedNodes()
	{
		return m_monitorSync.getMergedNodes();
	}

	/**
	 * Return an immutable Map of universes to the nodes handling that universe.
	 * The map is sorted by universe.
	 * @return An immutable sorted Map of universes to nodes.
	 */
	public Map<ArtNetUniv, Set<ArtNetNode>> getUnivsToNodes()
	{
		return m_monitorSync.getUnivsToNodes();
	}

	/**
	 * Return a sorted immutable sorted List of the ArtNet ports handled by some node.
	 * @return A sorted immutable sorted Set of the ArtNet ports handled by some node.
	 */
	public List<ArtNetUniv> getAllPorts()
	{
		return m_monitorSync.getAllUnivs();
	}

	/**
	 * Return an immutable sorted list of all Universe/IP-Addrs.
	 * @return An immutable sorted list of all Universe/IP-Addrs.
	 */
	public List<ArtNetUnivAddr> getAllUnivAddrs()
	{
		return m_monitorSync.getAlUnivAddrs();
	}

	/**
	 * Return an immutable Map from every ArtNetUnivAddr
	 * to the UIDs of the RDM devices on that port of that node.
	 * @return An immutable sorted Map from ArtNetUnivAddrs to UIDs on that node's universe.
	 */
	public Map<ArtNetUnivAddr, Set<ACN_UID>> getUnivAddrsToUids()
	{
		return m_monitorSync.getUnivAddrsToUids();
	}
	
	/**
	 * Return an immutable Map from every ArtNetUniv
	 * to the unique IP addresses of the nodes which respond to that universe.
	 * If a node has several outputs with the same ArtNetUniv,
	 * the set has the node's address once.
	 * @return An immutable Map from ArtNetUnivs to the unique IP address
	 * 		of the nodes which handle that universe.
	 */
	public Map<ArtNetUniv, Set<InetSocketAddress>> getUnivsToIpAddrs()
	{
		return m_monitorSync.getUnivsToIpAddrs();
	}
	
	/**
	 * Return an immutable Map from every ArtNetUniv which supports RDM
	 * to the unique IP addresses of the nodes which respond to that universe.
	 * If a node has several RDM outputs with the same ArtNetUniv,
	 * the set has the node's address once.
	 * @return An immutable Map from RDM ArtNetUnivs to the unique IP address
	 * 		of the nodes which handle that universe.
	 */
	public Map<ArtNetUniv, Set<InetSocketAddress>> getRdmUnivsToIpAddrs()
	{
		return m_monitorSync.getRdmUnivsToIpAddrs();
	}
	
	/**
	 * Return an immutable Map from every RDM device UID to the ArtNetUnivAddr for that device.
	 * @return An immutable Map from every RDM device UID to the ArtNetUnivAddr for that device.
	 */
	public Map<ACN_UID, ArtNetUnivAddr> getUidsToUnivAddrs()
	{
		return m_monitorSync.getUidsToUnivAddrs();
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
	 * Set the time to wait for ArtNet POLL_REPLY messages.
	 * Default is the time specified in the ArtNet protocol (3 seconds).
	 * @param replyWaitMS The wait time, in milliseconds. Values outside of MIN and MAX are ignored.
	 * @return The previous PollReply wait time.
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
	 * Set the time to wait for ArtNet TOD_DATA messages.
	 * The manager sends ArtNetTodControl messages to the nodes
	 * to force the nodes to redo DMX RDM discovery. This could take  minutes
	 * if there are a lot of devices on the DMX chain.
	 * We force discovery because some ArtNet nodes only do RDM device discovery
	 * when they power up. Unless we use TodControl, those nodes
	 * will not detect a device that has disconnected after the node powered up.
	 * @param replyWaitMS The wait time, in milliseconds. Values outside of MIN and MAX are ignored.
	 * @return The previous TodData wait time.
	 */
	public long setTodDataWaitMS(long replyWaitMS)
	{
		long prevWait = m_todDataWaitMS;
		if (replyWaitMS >= MIN_POLL_REPLY_MS && replyWaitMS <= MAX_POLL_REPLY_MS) {
			m_todDataWaitMS = replyWaitMS;
		}
		return prevWait;
	}
	
	/**
	 * Set the UDP ports on which ArtNetPoll messages will be sent.
	 * @param ports The ports. The class copies the list.
	 */
	public void setPorts(List<Integer> ports)
	{
		m_pollPorts = List.copyOf(ports);
		m_pollSockAddrs = null;
	}
	
	/**
	 * Set the UDP addresses on which ArtNetPoll messages will be sent.
	 * It will be send to each port in the UDP port list.
	 * @param addrs The addresses. The class copies the list.
	 */
	public void setInetAddrs(List<InetAddress> addrs)
	{
		m_pollInetAddrs = List.copyOf(addrs);
		m_pollSockAddrs = null;
	}
	
	/**
	 * Set the UDP socket addresses on which ArtNetPoll messages will be sent.
	 * This overrides the addresses and UDP ports
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
	 * Return an ArtNetRdmRequest, which can be used to send RDM requests.
	 * @return
	 * @throws IOException 
	 */
	public ArtNetRdmRequest getRdmRequest() throws IOException
	{
		if (m_rdmRequest == null) {
			m_rdmRequest = new ArtNetRdmRequest(m_channel, getUidsToUnivAddrs());
		}
		return m_rdmRequest;
	}
	
	/**
	 * Send an RDM request to a device and return the response.
	 * This uses the UID map ({@link #getUidsToUnivAddrs()} to find the node address and port for the UID.
	 * @param destUid The device UID.
	 * @param isSet True if this is a SET request, false if it's a GET.
	 * @param paramId The RMD parameter id.
	 * @param requestData The request data. May be null.
	 * @return The RdmPacket with the device's reply, or null if the request timed out
	 * 			or the UID map does not have the node for destUID.
	 * @throws IOException If an IO error occurs when sending the request.
	 */
	public RdmPacket sendRdmRequest(ACN_UID destUid, boolean isSet, RdmParamId paramId, byte[] requestData)
			throws IOException
	{
		return  getRdmRequest().sendRequest(destUid, isSet, paramId, requestData);
	}
		
	/**
	 * Get the device information for all RDM devices.
	 * Note: This method does not cache the results;
	 * each time it's called it gets fresh information for each device.
	 * @param errors If errors occur, append a message to this list for each error.
	 * 				If the list is null, ignore errors.  
	 * @return A sorted map from UIDs to RdmDevice descriptions.
	 * @throws IOException 
	 */
	public Map<ACN_UID, RdmDevice> getDeviceMap(List<String> errors) throws IOException
	{
		Map<ACN_UID, RdmDevice> deviceInfoMap = new TreeMap<>();
		Set<ACN_UID> uids = getUidsToUnivAddrs().keySet();
		// uids = new TreeSet<>(uids);
		for (ACN_UID uid: uids) {
			try {
				// System.err.println("XXX: pre getDeviceInfo: " + uid);
				RdmDevice info = new RdmDevice(uid, getUidsToUnivAddrs().get(uid), getRdmRequest());
				deviceInfoMap.put(uid, info);
			} catch (Exception e) {
				if (errors != null) {
					errors.add("ArtNetManger: Exception getting UID " + uid + ": " + e);
				}
			}
		}
		return deviceInfoMap;
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
		List<InetSocketAddress> pollSockAddrs = new ArrayList<>();
		for (InetAddress addr: m_pollInetAddrs) {
			for (int port: m_pollPorts) {
				pollSockAddrs.add(new InetSocketAddress(addr, port));
			}
		}
		m_pollSockAddrs = List.copyOf(pollSockAddrs);
	}
	
	/**
	 * A class used to synchronize with the MonitorThread.
	 * This object holds the results of the last discovery -- the lists, maps and sets.
	 * refresh() signals the MonitorThread to initiate discovery,
	 * and blocks until discovery is complete.
	 * When complete, the MonitorThread calls done() with the new data.
	 * The "getters" return the last list or map, and normally return immediately.
	 * But they will block if discovery has never been done.
	 * @author wdr
	 */
	private class MonitorSync
	{
		private boolean m_isValid = false;
		
		private List<ArtNetNode> m_allNodes = null;
		private Set<ArtNetNode> m_uniqueNodes = null;
		private Set<MergedArtNetNode> m_mergedNodes = null;
		private Map<ArtNetUniv, Set<ArtNetNode>> m_portsToNodes = null;
		private List<ArtNetUniv> m_allUnivs = null;
		private List<ArtNetUnivAddr> m_allUnivAddrs = null;
		private Map<ArtNetUnivAddr, Set<ACN_UID>> m_univAddrsToUids = null;
		private Map<ACN_UID, ArtNetUnivAddr> m_uidsToUnivAddrs = null;
		private Map<ArtNetUniv, Set<InetSocketAddress>> m_univsToIpAddrs = null;
		private Map<ArtNetUniv, Set<InetSocketAddress>> m_rdmUnivsToIpAddrs = null;
		
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

		private synchronized Set<MergedArtNetNode> getMergedNodes()
		{
			if (!m_isValid) {
				refresh();
			}
			return m_mergedNodes;
		}

		private synchronized Map<ArtNetUniv, Set<ArtNetNode>> getUnivsToNodes()
		{
			if (!m_isValid) {
				refresh();
			}
			return m_portsToNodes;
		}

		private synchronized List<ArtNetUniv> getAllUnivs()
		{
			if (!m_isValid) {
				refresh();
			}
			return m_allUnivs;
		}

		private synchronized List<ArtNetUnivAddr> getAlUnivAddrs()
		{
			if (!m_isValid) {
				refresh();
			}
			return m_allUnivAddrs;
		}

		private synchronized Map<ArtNetUnivAddr, Set<ACN_UID>> getUnivAddrsToUids()
		{
			if (!m_isValid) {
				refresh();
			}
			return m_univAddrsToUids;
		}

		private synchronized Map<ACN_UID, ArtNetUnivAddr> getUidsToUnivAddrs()
		{
			if (!m_isValid) {
				refresh();
			}
			return m_uidsToUnivAddrs;
		}

		private synchronized Map<ArtNetUniv, Set<InetSocketAddress>> getUnivsToIpAddrs()
		{
			if (!m_isValid) {
				refresh();
			}
			return m_univsToIpAddrs;
		}

		private synchronized Map<ArtNetUniv, Set<InetSocketAddress>> getRdmUnivsToIpAddrs()
		{
			if (!m_isValid) {
				refresh();
			}
			return m_rdmUnivsToIpAddrs;
		}
		
		/**
		 * Send a "refresh" command to the MonitorThread, and wait for that
		 * thread to call done() with the results.
		 * @return
		 */
		private synchronized boolean refresh() 
		{
			try {
				setupParam();
				m_monitorCmds.put(MonitorCmd.Refresh);
				wait();
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	
		/**
		 * Called by the MonitorThread when discovery is complete.
		 * This method saves the latest results, and wakes up the waiting refresh() call.
		 */
		private synchronized void done(
						List<ArtNetNode> allNodes,
						Set<ArtNetNode> uniqueNodes,
						Set<MergedArtNetNode> mergedNodes,
						Map<ArtNetUniv, Set<ArtNetNode>> portsToNodes,
						List<ArtNetUniv> allUnivs,
						List<ArtNetUnivAddr> allUnivAddrs,
						Map<ArtNetUnivAddr, Set<ACN_UID>> univAddrsToUids,
						Map<ACN_UID, ArtNetUnivAddr> uidsToUnivAddrs,
						Map<ArtNetUniv, Set<InetSocketAddress>> univsToIpAddrs,
						Map<ArtNetUniv, Set<InetSocketAddress>> rdmUnivsToIpAddrs
						)
		{
			m_allNodes = allNodes;
			m_uniqueNodes = uniqueNodes;
			m_mergedNodes = mergedNodes;
			m_portsToNodes = portsToNodes;
			m_allUnivs = allUnivs;
			m_allUnivAddrs = allUnivAddrs;
			m_univAddrsToUids = univAddrsToUids;
			m_uidsToUnivAddrs = uidsToUnivAddrs;
			m_univsToIpAddrs = univsToIpAddrs;
			m_rdmUnivsToIpAddrs = rdmUnivsToIpAddrs;
			m_isValid = true;
			notifyAll();
		}
		
		/**
		 * Tell the MonitorThread to stop.
		 */
		private synchronized void shutdown()
		{
			try {
				m_monitorCmds.put(MonitorCmd.Shutdown);
			} catch (Exception e) {
				// ignore.
			}		
		}
	}
	
	/**
	 * A thread that sends the ArtNetPoll and ArtNetTodControl request messages
	 * processes the replies from the nodes, and creates the lists, maps and sets
	 * defining the network of nodes and devices.
	 * The run() method loops on taking items from a concurrent blocking queue.
	 * The items are ArtNetPollReply messages, ArtNetTodData messages,
	 * or MonitorCmd enums. The enums trigger discovery (refresh) or shutdown.
	 * Methods in the parent class put those commands on the queue, as instructed
	 * by the client thread. The msgArrived() method puts the ArtNet reply messages
	 * on the queue. That method is called by the "listener" thread in the ArtNetChannel.
	 * It should do very little processing. Hence that method "forwards" the message
	 * to the MonitorThread, so run() can handle it.
	 */
	private class MonitorThread extends Thread implements ArtNetChannel.Receiver
	{
		// Working versions of the lists and maps for the current discovery process.
		private List<ArtNetNode> m_allNodes = null;
		private Set<ArtNetNode> m_uniqueNodes = null;
		private Map<ArtNetUnivAddr, Set<ACN_UID>> m_portAddrsToUids = null;
		private Map<ACN_UID, ArtNetUnivAddr> m_uidsToUnivAddrs = null;
		private Map<ArtNetUniv, Set<InetSocketAddress>> m_univsToIpAddrs = null;
		private Map<ArtNetUniv, Set<InetSocketAddress>> m_rdmUnivsToIpAddrs = null;
		
		private boolean m_polling = false;
		private long m_startPollTS = 0;
		private long m_pollEndTS = 0;

		/**
		 * Create and start the thread.
		 */
		private MonitorThread()
		{
			setName("ArtNetManager.MonitorThread");
			setDaemon(true);
			start();
		}

		/**
		 * Get the next item from the blocking queue, and handle it.
		 */
		public void run()
		{
			boolean running = true;
			try {
				while (running) {
					Object cmd = null;
					/*
					 * Minor sleaze alert! When discovery starts, m_pollEndTS is set to
					 * the time when discovery is complete. That is, start time plus the
					 * poll and tod reply wait times. Instead of getting cleaver about
					 * calculating the wait time to get the next item in the queue,
					 * we just wait 1 second and check if we're after m_pollEndTS.
					 * This is slightly wasteful, but simple and reliable.
					 */
					try {
						cmd = m_monitorCmds.poll(1000, TimeUnit.MILLISECONDS);
						// System.out.println("XXX: Mgr MonitorThread got " + cmd);
					} catch (InterruptedException e) {
						m_errorLogger.logError("ArtNetManager.MonitorThread interrupted: " + e);
						running = false;
						break;
					}
					if (cmd == null) {
						if (m_polling && System.currentTimeMillis() > m_pollEndTS) {
							stopPolling();
						}
					} else if (cmd instanceof MonitorCmd) {
						switch ((MonitorCmd) cmd) {
						case Refresh:
							startPolling();
							break;
						case Shutdown:
							running = false;
							break;
						}
					} else if (cmd instanceof ArtNetPollReply) {
						handlePollReply((ArtNetPollReply) cmd);
					} else if (cmd instanceof ArtNetTodData) {
						handleTodData((ArtNetTodData) cmd);
					}
				} 
			} finally {
				m_polling = false;
				// System.out.println("XXX: ArtNetManager.MonitorThread exiting.");
			}
		}
		
		private void startPolling()
		{
			if (m_polling) {
				return;
			}
			m_allNodes = new ArrayList<>();
			m_uniqueNodes = new TreeSet<>();
			m_portAddrsToUids = new TreeMap<>();
			m_uidsToUnivAddrs = new HashMap<>();
			m_univsToIpAddrs = new HashMap<>();
			m_rdmUnivsToIpAddrs = new HashMap<>();
			m_startPollTS = System.currentTimeMillis();
			m_pollEndTS = m_startPollTS + m_pollReplyWaitMS + (m_findRdmUids ? m_todDataWaitMS : 0);
			m_polling = true;
			for (InetSocketAddress addr: getSockAddrs()) {
				ArtNetPoll msg = new ArtNetPoll();
				msg.m_talkToMe |= ArtNetPoll.FLAGS_SEND_REPLY_ON_CHANGE;
				// System.out.println("XXX: poll msg: " + msg);
				try {
					if (!m_channel.send(msg, addr)) {
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
				// System.out.println("XXX: Stop polling.");
				Map<ArtNetUniv, Set<ArtNetNode>> portsToNodes
								= ArtNetNode.getDmxPort2NodeMap(m_allNodes);
				m_monitorSync.done(
						List.copyOf(m_allNodes),
						new ImmutableSet<ArtNetNode>(ArtNetNode.getUniqueNodes(m_allNodes)),
						new ImmutableSet<MergedArtNetNode>(MergedArtNetNode.makeMergedNodes(m_allNodes)),
						new ImmutableMap<ArtNetUniv, Set<ArtNetNode>>(portsToNodes),
						List.copyOf(portsToNodes.keySet()),
						List.copyOf(ArtNetNode.getUnivAddrs(m_allNodes)),
						new ImmutableMap<ArtNetUnivAddr, Set<ACN_UID>>(m_portAddrsToUids),
						new ImmutableMap<ACN_UID, ArtNetUnivAddr>(m_uidsToUnivAddrs),
						new ImmutableMap<ArtNetUniv, Set<InetSocketAddress>>(m_univsToIpAddrs),
						new ImmutableMap<ArtNetUniv, Set<InetSocketAddress>>(m_rdmUnivsToIpAddrs)
						);
				m_polling = false;
			}
		}
		
		/**
		 * Process an ArtNetPollReply from a node.
		 * If this is the first time we've seen this node,
		 * send it an ArtNetTodControl message to initiate RDM discovery.
		 * The node will send ArtNetTodData replies when done.
		 * @param msg The reply message.
		 */
		private void handlePollReply(ArtNetPollReply msg)
		{
			if (!m_polling) {
				return;
			}
			if (m_prtReplies) {
				System.out.println("PollReply: from=" + msg.getFromAddr().getHostAddress()
							+ " time=" + (System.currentTimeMillis() - m_startPollTS) + "ms");
				System.out.println("   " + msg.toFmtString(null, "   "));
			}
			ArtNetNode nodeInfo = new ArtNetNode((ArtNetPollReply)msg,
								System.currentTimeMillis() - m_startPollTS);
			m_allNodes.add(nodeInfo);
			
			m_uniqueNodes.add(nodeInfo);
			InetSocketAddress nodeAddr = nodeInfo.getNodeAddr().m_nodeAddr;
			for (ArtNetUniv dmxUniv: nodeInfo.m_dmxOutputUnivs) {
				Set<InetSocketAddress> addrs = m_univsToIpAddrs.get(dmxUniv);
				if (addrs == null) {
					addrs = new HashSet<>();
					m_univsToIpAddrs.put(dmxUniv, addrs);
				}
				addrs.add(nodeAddr);
			}
			if (m_findRdmUids) {
				for (ArtNetUniv rdmUniv: nodeInfo.m_dmxRdmUnivs) {
					Set<InetSocketAddress> addrs = m_rdmUnivsToIpAddrs.get(rdmUniv);
					if (addrs == null) {
						addrs = new HashSet<>();
						m_rdmUnivsToIpAddrs.put(rdmUniv, addrs);
					}
					if (addrs.add(nodeAddr)) {
						// System.out.println("XXX: Added rdm addr " + nodeAddr + " to " + addrs);
						ArtNetTodControl todCtlReq = new ArtNetTodControl();
						todCtlReq.m_net = rdmUniv.m_net;
						todCtlReq.m_command = ArtNetTodControl.COMMAND_ATC_FLUSH;
						todCtlReq.m_subnetUniv = rdmUniv.subUniv();
						try {
							if (false) {	// XXX
								System.out.println("XXX: Send TodControl to " + nodeAddr + " for " + rdmUniv);
							}
							if (!m_channel.send(todCtlReq, nodeAddr)) {
								m_errorLogger.logError("ArtNetManager: send TODControl failed.");
							}
						} catch (IOException e1) {
							m_errorLogger.logError("ArtNetManager: Exception sending TODControl: " + e1);
						} 
					}
				}
			}
			
			// XXX
			/*XXX
			if (m_uniqueNodes.add(nodeInfo) && m_findRdmUids) {
				for (ArtNetUniv univ: nodeInfo.m_dmxRdmPorts) {
					ArtNetTodControl todCtlReq = new ArtNetTodControl();
					todCtlReq.m_net = univ.m_net;
					todCtlReq.m_command = ArtNetTodControl.COMMAND_ATC_FLUSH;
					todCtlReq.m_subnetUniv = univ.subUniv();
					try {
						System.out.println("XXX: Control to " + nodeInfo.getNodeAddr().m_nodeAddr + " for " + univ);
						if (!m_channel.send(todCtlReq, nodeInfo.getNodeAddr().m_nodeAddr)) {
							m_errorLogger.logError("ArtNetManager: send TODControl failed.");
						}
					} catch (IOException e1) {
						m_errorLogger.logError("ArtNetManager: Exception sending TODControl: " + e1);
					} 
				}
			}
			XXX */
		}
		
		private void handleTodData(ArtNetTodData msg)
		{
			if (!m_polling) {
				return;
			}
			ArtNetTodData todData = (ArtNetTodData)msg;
			InetAddress fromAddr = msg.getFromAddr();
			if (todData.m_rdmVers != ArtNetTodRequest.RDM_VERS) {
				System.out.println("ArtNetManager: Illegal RDM version " + todData.m_rdmVers
							+ " from " + fromAddr);
				return;
			}
			if (!(fromAddr instanceof Inet4Address)) {
				System.out.println("ArtNetManager: Non-IP4 sender " + fromAddr);
				return;
			}
			ArtNetNodeAddr nodeAddr = new ArtNetNodeAddr((Inet4Address)fromAddr, todData.m_bindIndex);
			ArtNetUniv anUniv = new ArtNetUniv(todData.m_net, todData.m_subnetUniv);
			ArtNetUnivAddr univAddr = new ArtNetUnivAddr(nodeAddr, anUniv);
			if (todData.m_command == ArtNetTodRequest.COMMAND_TOD_NAK) {
				System.out.println("ArtNetManager: NAK from " + univAddr + " #uids=" + todData.m_numUids);
			}
			if (m_prtReplies) {
				System.out.println("TODData: from=" + fromAddr.getHostAddress()
							+ " time=" + (System.currentTimeMillis() - m_startPollTS) + "ms");
				System.out.println("   " + todData.toFmtString(null, "   "));
			}
			Set<ACN_UID> uids = m_portAddrsToUids.get(univAddr);
			if (uids == null) {
				uids = new HashSet<>();
				m_portAddrsToUids.put(univAddr, uids);
			}
			for (int i = 0; i < todData.m_numUids; i++) {
				uids.add(todData.m_uids[i]);
				m_uidsToUnivAddrs.put(todData.m_uids[i], univAddr);
			}
		}

		/**
		 * Get an incoming message.
		 * Note that the ArtNetChannel's listener thread calls this method,
		 * not MonitorThread.
		 */
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
	
	/**
	 * Initiate node and device discovery, and print the results.
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception
	{
		List<String> argList = new ArrayList<>();
		if (args != null) {
			for (String arg: args) {
				argList.add(arg);
			}
		}

		ArtNetChannel chan = null;
		boolean useTestNode = false;
		File testNodeParamFile = null;
		for (ListIterator<String> iter = argList.listIterator(); iter.hasNext(); ) {
			String arg = iter.next();
			if (arg.startsWith("-testnode")) {
				arg = arg.substring("-testnode".length());
				if (arg.startsWith("=")) {
					testNodeParamFile = new File(arg.substring(1));
				}
				useTestNode = true;
				iter.remove();
			}
		}
		ArtNetTestNode testNode = null;
		if (useTestNode) {
			chan = new ArtNetChannel();
			testNode = new ArtNetTestNode(chan, null, testNodeParamFile, testNodeParamFile);
		}
		
		boolean prtAllReplies = false;
		long nRepeats = 1;
		try (ArtNetManager mgr = new ArtNetManager(chan)) {
			Long longVal;
			for (String arg: argList) {
				if (arg.equals("-a")) {
					prtAllReplies = true;
				} else if (arg.equals("-r")) {
					mgr.setPrtReplies(true);
				} else if ((longVal = parseNumValueArg("-repeat=", arg)) != null) {
					nRepeats = longVal > 0 ? longVal : 1;
				} else if ((longVal = parseNumValueArg("-poll=", arg)) != null) {
					mgr.setPollReplyWaitMS(longVal);
				} else if ((longVal = parseNumValueArg("-tod=", arg)) != null) {
					mgr.setTodDataWaitMS(longVal);
				} else if (arg.startsWith("-")) {
					System.out.println("Unknown flag argument '" + arg + "'");
				}
			}
			if (args.length > 0) {
				List<InetSocketAddress> sockAddrs = new ArrayList<>();
				for (String arg: args) {
					if (!arg.startsWith("-")) {
						sockAddrs.add(InetUtil.parseAddrPort(arg, ArtNetConst.ARTNET_PORT));
					}
				}
				mgr.setSockAddrs(sockAddrs);
			}
			
			for (int iRepeat = 1; iRepeat <= nRepeats; iRepeat++) {
				if (iRepeat > 1) {
					System.out.println();
					Thread.sleep(2000);
					System.out.println("Starting discovery number " + iRepeat);
				}
				System.out.print("Sending polls to");
				for (InetSocketAddress sockAddr : mgr.getSockAddrs()) {
					System.out.print(" " + InetUtil.toAddrPort(sockAddr));
				}
				System.out.println(" ....");
				System.out.flush();
				long ts0 = System.currentTimeMillis();
				mgr.refresh();
				System.out.println("Discovery time: " + (System.currentTimeMillis() - ts0)/1000.0 + " sec");
				String indent = "    ";
				if (prtAllReplies) {
					List<ArtNetNode> allNodes = new ArrayList<>(mgr.getAllNodes());
					Collections.sort(allNodes);
					System.out.println(allNodes.size() + " Replies:");
					for (ArtNetNode node : allNodes) {
						System.out.println(indent + node.toString().replaceAll("\n", "\n" + indent));
						// node.m_reply.print(System.out, "");
					}
					System.out.println();
				}
				
				Set<ArtNetNode> uniqueNodes = mgr.getUniqueNodes();
				System.out.println(uniqueNodes.size() + " Unique Nodes:");
				for (ArtNetNode node : uniqueNodes) {
					System.out.println(indent + node.toString().replaceAll("\n", "\n" + indent));
					// XXX System.out.println(indent + node.m_reply.m_nodeReport);
				}
				System.out.println();
				
				Set<MergedArtNetNode> mergedNodes = mgr.getMergedNodes();
				System.out.println(mergedNodes.size() + " Merged Nodes:");
				for (MergedArtNetNode node : mergedNodes) {
					System.out.println(indent + node.toFmtString(null).replaceAll("\n", "\n" + indent));
				}
				System.out.println();
				
				List<ArtNetUniv> allUnivs = mgr.getAllPorts();
				System.out.println(allUnivs.size() + " ArtNet Universes:");
				String sep = indent;
				for (ArtNetUniv univ: allUnivs) {
					System.out.print(sep + univ);
					sep = "  ";
				}
				System.out.println();
				System.out.println();
				
				int maxPerLine = 5;
				String indent2 = "          ";
				Map<ArtNetUniv, Set<ArtNetNode>> portsToNodes = mgr.getUnivsToNodes();
				System.out.println(portsToNodes.keySet().size() + " DMX Output Universes: ");
				for (Map.Entry<ArtNetUniv, Set<ArtNetNode>> ent : portsToNodes.entrySet()) {
					System.out.print(indent + ent.getKey() + ":");
					int nOnLine = 0;
					for (ArtNetNode node : ent.getValue()) {
						if (nOnLine >= maxPerLine) {
							System.out.println();
							System.out.print(indent2);
							nOnLine = 0;
						}
						System.out.print(" " + node.m_reply.m_nodeAddr);
						nOnLine++;
					}
					System.out.println();
				}
				System.out.println();
				
				Map<ArtNetUniv, Set<InetSocketAddress>> portsToIpAddrs = mgr.getUnivsToIpAddrs();
				System.out.println(portsToIpAddrs.keySet().size() + " DMX Output Universes to node IP addrs: ");
				for (Map.Entry<ArtNetUniv, Set<InetSocketAddress>> ent : portsToIpAddrs.entrySet()) {
					System.out.print(indent + ent.getKey() + ":");
					int nOnLine = 0;
					for (InetSocketAddress addr: ent.getValue()) {
						if (nOnLine >= maxPerLine) {
							System.out.println();
							System.out.print(indent2);
							nOnLine = 0;
						}
						System.out.print(" " + addr.getHostString());
						nOnLine++;
					}
					System.out.println();
				}
				System.out.println();
				
				Map<ArtNetUniv, Set<InetSocketAddress>> rdmUnivsToIpAddrs = mgr.getRdmUnivsToIpAddrs();
				System.out.println(rdmUnivsToIpAddrs.keySet().size() + " RDM DMX Output Universes to node IP addrs: ");
				for (Map.Entry<ArtNetUniv, Set<InetSocketAddress>> ent : rdmUnivsToIpAddrs.entrySet()) {
					System.out.print(indent + ent.getKey() + ":");
					for (InetSocketAddress addr: ent.getValue()) {
						System.out.print(" " + addr.getHostString());
					}
					System.out.println();
				}
				System.out.println();
				
				Map<ArtNetUnivAddr, Set<ACN_UID>> univAddrsToUids = mgr.getUnivAddrsToUids();
				System.out.println("RDM Device UIDs, by ArtNet Universe:");
				for (Map.Entry<ArtNetUnivAddr, Set<ACN_UID>> ent : univAddrsToUids.entrySet()) {
					if (!ent.getValue().isEmpty()) {
						System.out.print(indent + ent.getKey() + ":");
						int nOnLine = 0;
						for (ACN_UID uid: ent.getValue()) {
							if (nOnLine >= maxPerLine) {
								System.out.println();
								System.out.print(indent2);
								nOnLine = 0;
							}
							System.out.print(" " + ent.getValue());
							nOnLine++;
						}
						System.out.println();
					}
				}
				System.out.println();
				
				Map<ACN_UID, ArtNetUnivAddr> uidsToUnivAddrs = mgr.getUidsToUnivAddrs();
				System.out.println("UID to universe/ip-addr map:");
				for (Map.Entry<ACN_UID, ArtNetUnivAddr> ent : uidsToUnivAddrs.entrySet()) {
					System.out.println(indent + ent.getKey() + ": " + ent.getValue());
				}
				System.out.println();
				
				List<String> errors = new ArrayList<>();
				Map<ACN_UID, RdmDevice> map = mgr.getDeviceMap(errors);
				if (!errors.isEmpty()) {
					System.out.println("RdmDevice2 errors: " + errors);
				}
				for (RdmDevice rdmDev: map.values()) {
					System.out.println("Device:\n   " + rdmDev);
				}
			}
		}
	}
	
	private static Long parseNumValueArg(String prefix, String arg)
	{
		if (arg != null && arg.startsWith(prefix)) {
			return Long.parseLong(arg.substring(prefix.length()));
		} else {
			return null;
		}
	}
}
