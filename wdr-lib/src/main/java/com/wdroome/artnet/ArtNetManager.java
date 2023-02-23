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
	 * Return an immutable Map of ports to the nodes handling that port.
	 * The map is sorted by port.
	 * @return An immutable sorted Map of ports to nodes.
	 */
	public Map<ArtNetPort, Set<ArtNetNode>> getPortsToNodes()
	{
		return m_monitorSync.getPortsToNodes();
	}

	/**
	 * Return a sorted immutable sorted List of the ArtNet ports handled by some node.
	 * @return A sorted immutable sorted Set of the ArtNet ports handled by some node.
	 */
	public List<ArtNetPort> getAllPorts()
	{
		return m_monitorSync.getAllPorts();
	}

	/**
	 * Return an immutable sorted list of all NodePorts.
	 * @return An immutable sorted list of all NodePorts.
	 */
	public List<ArtNetPortAddr> getAllNodePorts()
	{
		return m_monitorSync.getAllNodePorts();
	}

	/**
	 * Return an immutable Map from every ArtNetPortAddr
	 * to the UIDs of the RDM devices on that port of that node.
	 * @return An immutable sorted Map from ArtNetPortAddrs to UIDs on that node's port.
	 */
	public Map<ArtNetPortAddr, Set<ACN_UID>> getPortAddrsToUids()
	{
		return m_monitorSync.getPortAddrsToUids();
	}
	
	/**
	 * Return an immutable Map from every RDM device UID to the ArtNetPortAddr for that device.
	 * @return An immutable Map from every RDM device UID to the ArtNetPortAddr for that device.
	 */
	public Map<ACN_UID, ArtNetPortAddr> getUidsToPortAddrs()
	{
		return m_monitorSync.getUidsToPortAddrs();
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
	 * Send an RDM request to a device and return the response.
	 * This uses the UID map ({@link #getUidsToPortAddrs()} to find the node address and port for the UID.
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
		if (m_rdmRequest == null) {
			m_rdmRequest = new ArtNetRdmRequest(m_channel, getUidsToPortAddrs());
		}
		return m_rdmRequest.sendRequest(destUid, isSet, paramId, requestData);
	}
	
	/**
	 * Get the standard information for all RDM devices.
	 * Note: This method does not cache the results;
	 * each time it's called it gets fresh information for each device.
	 * @param errors If errors occur, append a message to this list for each error.
	 * 				If the list is null, ignore errors.  
	 * @return A sorted map from UIDs to RdmDevice descriptions.
	 */
	public Map<ACN_UID, RdmDevice> getDeviceMap(List<String> errors)
	{
		Map<ACN_UID, RdmDevice> deviceInfoMap = new TreeMap<>();
		for (ACN_UID uid: getUidsToPortAddrs().keySet()) {
			try {
				RdmDevice info = getDevice(uid);
				if (info != null) {
					deviceInfoMap.put(uid, info);
				} else if (errors != null) {
					errors.add("Cannot get DeviceInfo for " + uid);
				}
			} catch (Exception e) {
				if (errors != null) {
					errors.add("Exception getting UID " + uid + ": " + e);
				}
			}
		}
		return deviceInfoMap;
	}

	/**
	 * Get the standard information for an RDM device.
	 * @param uid The device UID.
	 * @return The device information, or null if the TOD requests did not
	 * 			give the node with that UID, or the device does not reply.
	 * @throws IOException If an I//O error occurs.
	 */
	public RdmDevice getDevice(ACN_UID uid) throws IOException
	{
		ArtNetPortAddr portAddr = getUidsToPortAddrs().get(uid);
		if (portAddr == null) {
			return null;
		}
		RdmPacket devInfoReply = sendRdmRequest(uid, false, RdmParamId.DEVICE_INFO, null);
		if (devInfoReply == null || !devInfoReply.isRespAck()) {
			return null;
		}
		RdmParamResp.DeviceInfo devInfo = new RdmParamResp.DeviceInfo(devInfoReply);
		
		RdmPacket supportedPidsReply = sendRdmRequest(uid, false, RdmParamId.SUPPORTED_PARAMETERS, null);
		RdmParamResp.PidList supportedPids = new RdmParamResp.PidList(supportedPidsReply);
		
		RdmPacket swVerLabelReply = sendRdmRequest(uid, false, RdmParamId.SOFTWARE_VERSION_LABEL, null);
		String swVerLabel = "[" + devInfo.m_softwareVersion + "]";
		if (swVerLabelReply != null) {
			swVerLabel = new RdmParamResp.StringReply(swVerLabelReply).m_string;
		}
		
		String manufacturer = String.format("0x%04x", uid.getManufacturer());
		if (isParamSupported(RdmParamId.MANUFACTURER_LABEL, supportedPids)) {
			RdmPacket manufacturerReply = sendRdmRequest(uid, false, RdmParamId.MANUFACTURER_LABEL, null);
			if (manufacturerReply != null && manufacturerReply.isRespAck()) {
				manufacturer = new RdmParamResp.StringReply(manufacturerReply).m_string;
			} 
		}
		
		String model = String.format("0x%04x", devInfo.m_model);
		if (isParamSupported(RdmParamId.DEVICE_MODEL_DESCRIPTION, supportedPids)) {
			RdmPacket modelReply = sendRdmRequest(uid, false, RdmParamId.DEVICE_MODEL_DESCRIPTION, null);
			if (modelReply != null && modelReply.isRespAck()) {
				model = new RdmParamResp.StringReply(modelReply).m_string;
			} 
		}
		
		return new RdmDevice(uid, portAddr, devInfo,
							getPersonalities(uid, devInfo.m_nPersonalities, supportedPids),
							getSlotDescs(uid, devInfo.m_dmxFootprint, supportedPids),
							manufacturer, model, swVerLabel, supportedPids);
	}
	
	
	private TreeMap<Integer,RdmParamResp.PersonalityDesc> getPersonalities(ACN_UID uid, int nPersonalities,
													 RdmParamResp.PidList supportedPids) throws IOException
	{
		TreeMap<Integer,RdmParamResp.PersonalityDesc> personalities = new TreeMap<>();
		boolean ok = isParamSupported(RdmParamId.DMX_PERSONALITY_DESCRIPTION, supportedPids);
		if (ok) {
			for (int iPersonality = 1; iPersonality <= nPersonalities; iPersonality++) {
				RdmPacket personalityDescReply = null;
				if (ok) {
					personalityDescReply = sendRdmRequest(uid, false,
														RdmParamId.DMX_PERSONALITY_DESCRIPTION,
														new byte[] { (byte) iPersonality });
					if (personalityDescReply == null) {
						ok = false;
					}
				}
				RdmParamResp.PersonalityDesc desc;
				if (personalityDescReply != null && personalityDescReply.isRespAck()) {
					desc = new RdmParamResp.PersonalityDesc(personalityDescReply);
				} else {
					desc = new RdmParamResp.PersonalityDesc(iPersonality, 1, RdmDevice.UNKNOWN_DESC);
				}
				personalities.put(iPersonality, desc);
			} 
		}
		return personalities;
	}
	
	private TreeMap<Integer,String> getSlotDescs(ACN_UID uid, int nSlots,
												 RdmParamResp.PidList supportedPids) throws IOException
	{
		TreeMap<Integer,String> slotDescs = new TreeMap<>();
		boolean ok = isParamSupported(RdmParamId.SLOT_DESCRIPTION, supportedPids);
		if (ok) {
			for (int iSlot = 0; iSlot < nSlots; iSlot++) {
				slotDescs.put(iSlot, RdmDevice.UNKNOWN_DESC);
			}
			for (int iSlot = 0; iSlot < nSlots; iSlot++) {
				if (ok) {
					byte[] iSlotAsBytes = new byte[] {(byte)((iSlot >> 8) & 0xff), (byte)(iSlot & 0xff)};
					RdmPacket slotDescReply = sendRdmRequest(uid, false,
													RdmParamId.SLOT_DESCRIPTION, iSlotAsBytes);
					if (slotDescReply != null && slotDescReply.isRespAck()) {
						RdmParamResp.SlotDesc slotDesc = new RdmParamResp.SlotDesc(slotDescReply);
						slotDescs.put(slotDesc.m_number, slotDesc.m_desc);
					} else {
						ok = false;
					}
				}
			} 
		}
		return slotDescs;
	}
	
	private boolean isParamSupported(RdmParamId paramId, RdmParamResp.PidList supportedPids)
	{
		return paramId.isRequired() || (supportedPids != null && supportedPids.isSupported(paramId));
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
		private Map<ArtNetPort, Set<ArtNetNode>> m_portsToNodes = null;
		private List<ArtNetPort> m_allPorts = null;
		private List<ArtNetPortAddr> m_allNodePorts = null;
		private Map<ArtNetPortAddr, Set<ACN_UID>> m_portAddrsToUids = null;
		private Map<ACN_UID, ArtNetPortAddr> m_uidsToPortAddrs = null;
		
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

		private synchronized List<ArtNetPort> getAllPorts()
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

		private synchronized Map<ArtNetPortAddr, Set<ACN_UID>> getPortAddrsToUids()
		{
			if (!m_isValid) {
				refresh();
			}
			return m_portAddrsToUids;
		}

		private synchronized Map<ACN_UID, ArtNetPortAddr> getUidsToPortAddrs()
		{
			if (!m_isValid) {
				refresh();
			}
			return m_uidsToPortAddrs;
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
						Map<ArtNetPort, Set<ArtNetNode>> portsToNodes,
						List<ArtNetPort> allPorts,
						List<ArtNetPortAddr> allNodePorts,
						Map<ArtNetPortAddr, Set<ACN_UID>> portAddrsToUids,
						Map<ACN_UID, ArtNetPortAddr> uidsToPortAddrs
						)
		{
			m_allNodes = allNodes;
			m_uniqueNodes = uniqueNodes;
			m_portsToNodes = portsToNodes;
			m_allPorts = allPorts;
			m_allNodePorts = allNodePorts;
			m_portAddrsToUids = portAddrsToUids;
			m_uidsToPortAddrs = uidsToPortAddrs;
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
		private Map<ArtNetPortAddr, Set<ACN_UID>> m_portAddrsToUids = null;
		private Map<ACN_UID, ArtNetPortAddr> m_uidsToPortAddrs = null;
		
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
			m_uidsToPortAddrs = new HashMap<>();
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
				Map<ArtNetPort, Set<ArtNetNode>> portsToNodes
								= ArtNetNode.getDmxPort2NodeMap(m_allNodes);
				m_monitorSync.done(
						List.copyOf(m_allNodes),
						new ImmutableSet<ArtNetNode>(ArtNetNode.getUniqueNodes(m_allNodes)),
						new ImmutableMap<ArtNetPort, Set<ArtNetNode>>(portsToNodes),
						List.copyOf(portsToNodes.keySet()),
						List.copyOf(ArtNetNode.getNodePorts(m_allNodes)),
						new ImmutableMap<ArtNetPortAddr, Set<ACN_UID>>(m_portAddrsToUids),
						new ImmutableMap<ACN_UID, ArtNetPortAddr>(m_uidsToPortAddrs));
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
			if (m_uniqueNodes.add(nodeInfo) && m_findRdmUids) {
				for (ArtNetPort port: nodeInfo.m_dmxOutputPorts) {
					ArtNetTodControl todCtlReq = new ArtNetTodControl();
					todCtlReq.m_net = port.m_net;
					todCtlReq.m_command = ArtNetTodControl.COMMAND_ATC_FLUSH;
					todCtlReq.m_subnetUniv = port.subUniv();
					try {
						if (!m_channel.send(todCtlReq, nodeInfo.getNodeAddr().m_nodeAddr)) {
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
			Set<ACN_UID> uids = m_portAddrsToUids.get(nodePort);
			if (uids == null) {
				uids = new HashSet<>();
				m_portAddrsToUids.put(nodePort, uids);
			}
			for (int i = 0; i < todData.m_numUids; i++) {
				uids.add(todData.m_uids[i]);
				m_uidsToPortAddrs.put(todData.m_uids[i], nodePort);
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
				}
				System.out.println();
				
				List<ArtNetPort> allPorts = mgr.getAllPorts();
				System.out.println(allPorts.size() + " ArtNet Ports:");
				String sep = indent;
				for (ArtNetPort port: allPorts) {
					System.out.print(sep + port);
					sep = "  ";
				}
				System.out.println();
				System.out.println();
				
				Map<ArtNetPort, Set<ArtNetNode>> portsToNodes = mgr.getPortsToNodes();
				System.out.println(portsToNodes.keySet().size() + " DMX Output Ports: ");
				for (Map.Entry<ArtNetPort, Set<ArtNetNode>> ent : portsToNodes.entrySet()) {
					System.out.print(indent + ent.getKey() + ":");
					for (ArtNetNode node : ent.getValue()) {
						System.out.print(" " + node.m_reply.m_nodeAddr);
					}
					System.out.println();
				}
				System.out.println();
				
				Map<ArtNetPortAddr, Set<ACN_UID>> portAddrsToUids = mgr.getPortAddrsToUids();
				System.out.println("RDM Device UIDs, by ArtNet Port:");
				for (Map.Entry<ArtNetPortAddr, Set<ACN_UID>> ent : portAddrsToUids.entrySet()) {
					System.out.println(indent + ent.getKey() + ": " + ent.getValue());
				}
				System.out.println();
				
				Map<ACN_UID, ArtNetPortAddr> uidsToPortAddrs = mgr.getUidsToPortAddrs();
				System.out.println("UID to port map:");
				for (Map.Entry<ACN_UID, ArtNetPortAddr> ent : uidsToPortAddrs.entrySet()) {
					System.out.println(indent + ent.getKey() + ": " + ent.getValue());
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
