package com.wdroome.artnet.util;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import java.io.OutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;

import java.net.InetSocketAddress;

import com.wdroome.artnet.ArtNetChannel;
import com.wdroome.artnet.ArtNetConst;
import com.wdroome.artnet.ArtNetOpcode;
import com.wdroome.artnet.ArtNetUniv;
import com.wdroome.artnet.ACN_UID;

import com.wdroome.artnet.msgs.ArtNetMsg;
import com.wdroome.artnet.msgs.ArtNetMsgUtil;
import com.wdroome.artnet.msgs.ArtNetPoll;
import com.wdroome.artnet.msgs.ArtNetPollReply;
import com.wdroome.artnet.msgs.ArtNetDmx;
import com.wdroome.artnet.msgs.ArtNetTodRequest;
import com.wdroome.artnet.msgs.ArtNetTodControl;
import com.wdroome.artnet.msgs.ArtNetTodData;
import com.wdroome.artnet.msgs.ArtNetRdm;
import com.wdroome.artnet.msgs.RdmPacket;
import com.wdroome.artnet.msgs.RdmParamData;
import com.wdroome.artnet.msgs.RdmParamId;
import com.wdroome.artnet.msgs.RdmParamResp;
import com.wdroome.artnet.msgs.RdmProductCategories;

import com.wdroome.json.JSONValue;
import com.wdroome.json.JSONValueTypeException;
import com.wdroome.json.JSONValue_Object;
import com.wdroome.json.JSONValue_Array;
import com.wdroome.json.JSONValue_String;
import com.wdroome.json.JSONValue_Number;
import com.wdroome.json.JSONLexan;
import com.wdroome.json.JSONParseException;
import com.wdroome.json.JSONParser;
import com.wdroome.json.JSONUtil;

import com.wdroome.util.IErrorLogger;
import com.wdroome.util.SystemErrorLogger;

/**
 * A simulated Art-Net DMX-output node. Reply to Poll requests giving the configured ports.
 * Save DMX levels set for each port, so a client can access them. Respond to TOD requests
 * for simulated devices defined by a configuration file.
 * This simulator simulates multiple physical ports and multiple ArtNet ports,
 * but the physical ports are 1-1 with the ArtNet ports.
 * That is, this class does not allow two physical ports to have the same ArtNet port.
 * Art-Net (TM) Designed by and Copyright Artistic License Holdings Ltd.
 * @author wdr
 */
public class ArtNetTestNode implements ArtNetChannel.Receiver, Closeable
{
	@FunctionalInterface
	public interface PollHandler
	{
		public void handlePoll(ArtNetPoll poll);
	}
	
	@FunctionalInterface
	public interface PollReplyHandler
	{
		public void handlePollReply(ArtNetPollReply pollReply);
	}
	
	@FunctionalInterface
	public interface DmxHandler
	{
		public void handleDmx(ArtNetDmx dmx);
	}
	
	@FunctionalInterface
	public interface IdentifyDeviceHandler
	{
		// Called when an IDENTIFY_DEVICE request arrives, either turning it on or off.
		public void handleIdentifyDevice(ACN_UID uid, ArtNetUniv anPort, int dmxAddr, boolean isOn);
	}
	
    /** A struct with a DMX message, the time it was received, and the number of DMX msgs received. */
    public static class DmxMsgTS
    {
    	public final long m_ts;
    	public final ArtNetDmx m_msg;
    	public final long m_numMsgs;
    	
    	public DmxMsgTS(ArtNetDmx msg, long numMsgs)
    	{
    		m_msg = msg;
    		m_ts = System.currentTimeMillis();
    		m_numMsgs = numMsgs;
    	}
    }
    
	public static final String PN_NODE_SHORT_NAME = "shortName";
	public static final String PN_NODE_LONG_NAME = "longName";
	
	// Fields for the RDM information for each device:
	public static final String PN_TYPE = "type";	// CS-SPOT, etc
	public static final String PN_SERIAL = "serial";	// device serial number
	public static final String PN_DMX = "dmx";		// start address
	public static final String PN_CONFIG = "config";
	public static final String PN_LABEL = "label";
	public static final String PN_FREQ = "freq";	// 0 or 1

	private final ArtNetChannel m_channel;
	private final boolean m_isSharedChannel;
	private IErrorLogger m_logger = new SystemErrorLogger();
	
	private final JSONValue_Object m_nodeParam;
	private final File m_outputParamFile;
	
	// ArtNet DMX output ports for this node.
	private final List<ArtNetUniv> m_anPorts;
	
	// Map from ArtNet ports to RDM devices on that port.
	private final TreeMap<ArtNetUniv, List<Device>> m_deviceMap;
	
	// Devices with IDENTIFY_DEVICE set to true.
	private final Set<ACN_UID> m_identifyingDevices = new HashSet<>();
	
	// ArtNetPollReply message describing this node.
	private final ArtNetPollReply m_pollReply;
	private final List<ArtNetPollReply> m_pollReplies;
	
	// Most recent DMX message for each supported DMX output port.
	private final Map<ArtNetUniv, DmxMsgTS> m_lastDmxMsg = new ConcurrentHashMap<>();
	
	private final Map<ArtNetUniv, AtomicLong> m_numDmxMsgs = new HashMap<>();
	private final AtomicLong m_numBadDmxMsgs = new AtomicLong(0);
	private final AtomicLong m_numPollMsgs = new AtomicLong(0);
	
	private final Set<PollHandler> m_pollHandlers = new HashSet<>();
	private final Set<PollReplyHandler> m_pollReplyHandlers = new HashSet<>();
	private final Set<DmxHandler> m_dmxHandlers = new HashSet<>();
	private final Set<IdentifyDeviceHandler> m_identifyDeviceHandlers = new HashSet<>();
	
	public ArtNetTestNode(ArtNetChannel channel, IErrorLogger logger,
							File inputParamFile, File outputParamFile)
			throws IOException, JSONParseException, JSONValueTypeException
	{
		if (logger != null) {
			m_logger = logger;
		}
		if (inputParamFile != null) {
			m_nodeParam = JSONParser.parseObject(new JSONLexan(inputParamFile), true);
		} else {
			m_nodeParam = new JSONValue_Object();
		}
		m_outputParamFile = outputParamFile;
		if (channel != null) {
			m_channel = channel;
			m_isSharedChannel = true;
		} else {
			m_channel = new ArtNetChannel();
			m_isSharedChannel = false;
		}
		m_deviceMap = readConfig();
		m_anPorts = new ArrayList<>(m_deviceMap.keySet());
		for (ArtNetUniv port: m_anPorts) {
			m_numDmxMsgs.put(port, new AtomicLong(0));
		}
		m_pollReply = makePollReply();
		m_pollReplies = makePollReplies();
		new SendMsgThread();
		m_channel.addReceiver(this);
	}
	
	private TreeMap<ArtNetUniv, List<Device>> readConfig()
	{
		TreeMap<ArtNetUniv, List<Device>> map = new TreeMap<>();
		int nextDevSerial = 1;
		for (String anPortKey: new TreeSet<String>(m_nodeParam.keySet())) {
			ArtNetUniv anPort;
			try {
				anPort = new ArtNetUniv(anPortKey);
			} catch (Exception e) {
				// ignore
				continue;
			}
			List<Device> devices = new ArrayList<>();
			map.put(anPort, devices);
			JSONValue_Object devList = m_nodeParam.getObject(anPortKey, null);
			if (devList == null) {
				continue;
			}
			for (Map.Entry<String, JSONValue> ent: devList.entrySet()) {
				JSONValue jval = ent.getValue();
				if (!(jval instanceof JSONValue_Object)) {
					continue;
				}
				int serial = myParseInt(ent.getKey(), nextDevSerial);
				Device device = new Device(anPort, (JSONValue_Object)jval, serial);
				nextDevSerial = device.m_serial + 1;
				devices.add(device);
			}
		}
		if (map.isEmpty()) {
			for (String port: new String[] {"0.0.0", "0.0.1"}) {
				map.put(new ArtNetUniv(port), new ArrayList<>());
			}
		}
		return map;
	}
	
	private List<ArtNetPollReply> makePollReplies()
	{
		List<ArtNetPollReply> replies = new ArrayList<>();
		int iPort = 1;
		for (ArtNetUniv anPort: m_anPorts) {
			ArtNetPollReply reply = new ArtNetPollReply();
			replies.add(reply);
			reply.m_shortName = getParam(PN_NODE_SHORT_NAME, "ArtNetTestNode");
			reply.m_longName = getParam(PN_NODE_LONG_NAME, "ArtNetTestNode");
			reply.m_style = ArtNetConst.StNode;
			reply.m_status2 = 0x0e;	// Supports 15-bit node addresses & DHCP.
			reply.m_numPorts = 1;
			reply.m_netAddr = anPort.m_net;
			reply.m_subNetAddr = anPort.m_subNet;
			reply.m_portTypes[0] = (byte)0x80;
			reply.m_goodInput[0] = (byte)0x00;
			reply.m_goodOutput[0] = (byte)0x80;
			reply.m_swOut[0] = (byte)anPort.m_universe;
			reply.m_bindIndex = iPort;
			iPort++;
		}
		return replies;
	}
	
	private ArtNetPollReply makePollReply()
	{
		ArtNetPollReply reply = new ArtNetPollReply();
		reply = new ArtNetPollReply();
		reply.m_shortName = getParam(PN_NODE_SHORT_NAME, "ArtNetTestNode");
		reply.m_longName = getParam(PN_NODE_LONG_NAME, "ArtNetTestNode");
		reply.m_style = ArtNetConst.StNode;
		reply.m_status2 = 0x0e;	// Supports 15-bit node addresses & DHCP.
		reply.m_numPorts = m_anPorts.size();
		for (int i = 0; i < m_anPorts.size(); i++) {
			reply.m_netAddr = m_anPorts.get(i).m_net;
			reply.m_subNetAddr = m_anPorts.get(i).m_subNet;
			reply.m_portTypes[i] = (byte)0x80;
			reply.m_goodInput[i] = (byte)0x00;
			reply.m_goodOutput[i] = (byte)0x80;
			reply.m_swOut[i] = (byte)m_anPorts.get(i).m_universe;
		}
		return reply;
	}
	
	private String getParam(String name, String def)
	{
		JSONValue val = m_nodeParam.getPath(name, null);
		if (val instanceof JSONValue_String) {
			String s = ((JSONValue_String)val).m_value;
			if (!s.isBlank()) {
				return s;
			}
		} else if (val instanceof JSONValue_Number) {
			return "" + ((JSONValue_Number)val).m_value;
		}
		return def;
	}
	
	public boolean addHandler(PollHandler handler)
	{
		synchronized (m_pollHandlers) {
			return m_pollHandlers.add(handler);
		}
	}
	
	public boolean addHandler(PollReplyHandler handler)
	{
		synchronized (m_pollReplyHandlers) {
			return m_pollReplyHandlers.add(handler);
		}
	}
	
	public boolean addHandler(DmxHandler handler)
	{
		synchronized (m_dmxHandlers) {
			return m_dmxHandlers.add(handler);
		}
	}
	
	public boolean addHandler(IdentifyDeviceHandler handler)
	{
		synchronized (m_identifyDeviceHandlers) {
			return m_identifyDeviceHandlers.add(handler);
		}
	}
	
	@Override
	public void close()
	{
		if (m_isSharedChannel) {
			m_channel.dropReceiver(this);
		} else {
			m_channel.shutdown();
		}
	}

	public IErrorLogger getLogger() {
		return m_logger;
	}

	public void setLogger(IErrorLogger logger) {
		this.m_logger = logger;
	}
	
	public DmxMsgTS getDmxLevels(ArtNetUniv port)
	{
		return m_lastDmxMsg.get(port);
	}
	
	private class SendMsgReq
	{
		private final InetSocketAddress m_destAddr;	// If null, broadcast the message.
		private final ArtNetMsg m_msg;
		
		private SendMsgReq(InetSocketAddress destAddr, ArtNetMsg msg)
		{
			m_destAddr = destAddr;
			m_msg = msg;
		}
	}
	
	private final ArrayBlockingQueue<SendMsgReq> m_sendQueue = new ArrayBlockingQueue<>(50);
	
	private class SendMsgThread extends Thread
	{
		private SendMsgThread()
		{
			setName("ArtNetTestNode.SendThread");
			setDaemon(true);
			start();
		}

		@Override
		public void run()
		{
			SendMsgReq req;
			try {
				while ((req = m_sendQueue.take()) != null) {
					if (req.m_destAddr == null) {
						m_channel.broadcast(req.m_msg);
					} else {
						m_channel.send(req.m_msg, req.m_destAddr);
					}
				}
			} catch (Exception e) {
				System.err.println("ArtNetTestNode.SendMsgThread: " + e);
			}
		}
	}

	@Override
	public void msgArrived(ArtNetChannel chan, ArtNetMsg msg, InetSocketAddress sender,
							InetSocketAddress receiver)
	{
		if (msg instanceof ArtNetPollReply) {
			synchronized (m_pollReplyHandlers) {
				for (PollReplyHandler handler: m_pollReplyHandlers) {
					try {
						handler.handlePollReply((ArtNetPollReply)msg);
					} catch (Exception e) {
						m_logger.logError("ArtNetTestNode: exception in pollReply handler: " + e);
					}
				}
			}
		} else if (msg instanceof ArtNetPoll) {
			synchronized (m_pollHandlers) {
				for (PollHandler handler: m_pollHandlers) {
					try {
						handler.handlePoll((ArtNetPoll)msg);
					} catch (Exception e) {
						m_logger.logError("ArtNetTestNode: exception in poll handler: " + e);
					}
				}
			}
			m_numPollMsgs.incrementAndGet();
			for (ArtNetPollReply reply: m_pollReplies) {
				try {
					// m_channel.broadcast(m_pollReply);
					// m_channel.broadcast(reply);
					m_sendQueue.add(new SendMsgReq(null, reply));
				} catch (Exception e) {
					m_logger.logError("ArtNetTestNode: Poll Reply Send Error",
							60000, ": " + e);
				}
			}
		} else if (msg instanceof ArtNetDmx) {
			ArtNetDmx dmx = (ArtNetDmx)msg;
			synchronized (m_dmxHandlers) {
				for (DmxHandler handler: m_dmxHandlers) {
					try {
						handler.handleDmx(dmx);
					} catch (Exception e) {
						m_logger.logError("ArtNetTestNode: exception in dmx handler: " + e);
					}
				}
			}
			boolean ours = false;
			for (int i = 0; i < m_anPorts.size(); i++) {
				ArtNetUniv anPort = m_anPorts.get(i);
				if (dmx.m_subUni == anPort.subUniv() && dmx.m_net == anPort.m_net) {
					long numMsgs = m_numDmxMsgs.get(anPort).incrementAndGet();
					DmxMsgTS prev = m_lastDmxMsg.put(anPort, new DmxMsgTS(dmx, numMsgs));
					ours = true;
					if (prev != null
							&& dmx.m_sequence != 0
							&& prev.m_msg.m_sequence != 0
							&& ((dmx.m_sequence - prev.m_msg.m_sequence + 256) % 256) > 200) {
						m_logger.logError("ArtNetTestNode: out of sequence dmx msg", 60000,
									dmx.m_sequence + " " + prev.m_msg.m_sequence);
					}
					break;
				}
			}
			if (!ours) {
				m_numBadDmxMsgs.incrementAndGet();
				m_logger.logError("ArtNetMonitorWindow: Incorrect ANPort " +
								new ArtNetUniv(dmx.m_net, dmx.m_subUni).toString(), 60000, "");
			}
		} else if (msg instanceof ArtNetTodRequest) {
			ArtNetTodRequest todReq = (ArtNetTodRequest)msg;
			int numSubnetUnivs = Math.min(todReq.m_numSubnetUnivs, todReq.m_subnetUnivs.length);
			if (todReq.m_command == ArtNetTodRequest.COMMAND_TOD_FULL) {
				for (int iUniv = 0; iUniv < numSubnetUnivs; iUniv++) {
					ArtNetUniv anPort = new ArtNetUniv(todReq.m_net, todReq.m_subnetUnivs[iUniv]);
					sendTodData(anPort);
				} 
			}
		} else if (msg instanceof ArtNetTodControl) {
			ArtNetTodControl todCtl = (ArtNetTodControl)msg;
			sendTodData(new ArtNetUniv(todCtl.m_net,todCtl.m_subnetUniv));		
		} else if (msg instanceof ArtNetRdm) {
			doRdmMsg((ArtNetRdm)msg, sender);
		}
	}
	
	private void sendTodData(ArtNetUniv anPort)
	{
		ArtNetTodData todReply = new ArtNetTodData();
		List<Device> devices = m_deviceMap.get(anPort);
		if (devices != null) {
			todReply.m_port = m_anPorts.indexOf(anPort) + 1;
			todReply.m_bindIndex = todReply.m_port;
			todReply.m_net = anPort.m_net;
			todReply.m_command = ArtNetTodRequest.COMMAND_TOD_FULL;
			todReply.m_subnetUniv = anPort.subUniv();
			todReply.m_numUidsTotal = devices.size();
			todReply.m_numUids = todReply.m_numUidsTotal;
			todReply.m_uids = new ACN_UID[todReply.m_numUids];
			for (int iDev = 0; iDev < todReply.m_numUids; iDev++) {
				todReply.m_uids[iDev] = devices.get(iDev).m_uid;
			}
			if (false) {
				System.err.println("XXX: b'cast " + todReply);
			}
			try {
				// m_channel.broadcast(todReply);
				m_sendQueue.add(new SendMsgReq(null, todReply));
			} catch (Exception e) {
				m_logger.logError("ArtNetTestNode.sendTodData: " + e);
			}
		}
	}
	
	private void doRdmMsg(ArtNetRdm req, InetSocketAddress sender)
	{
		// System.err.println("XXX RDM " + req);
		ArtNetUniv anPort = new ArtNetUniv(req.m_net, req.m_subnetUniv);
		List<Device> devices = m_deviceMap.get(anPort);
		if (devices == null) {
			return;
		}
		RdmPacket rdmReq = req.m_rdmPacket;
		for (Device device: devices) {
			if (device.m_uid.matches(rdmReq.m_destUid)) {
				ArtNetRdm reply = new ArtNetRdm();
				reply.m_net = anPort.m_net;
				reply.m_subnetUniv = anPort.subUniv();
				byte[] replyRdmData = null;
				switch (rdmReq.getParamId()) {
				case DEVICE_INFO:
					replyRdmData = getDeviceInfoData(device);
					break;
				case SOFTWARE_VERSION_LABEL:
					replyRdmData = ETC_SW_VERSION_LABEL.getBytes();
					break;
				case MANUFACTURER_LABEL:
					replyRdmData = device.m_type.m_makerName.getBytes();
					break;
				case DEVICE_MODEL_DESCRIPTION:
					replyRdmData = device.m_type.m_modelName.getBytes();
					break;
				case SUPPORTED_PARAMETERS:
					replyRdmData = getSupportedParamIds(device);
					break;
				case DMX_PERSONALITY_DESCRIPTION:
					replyRdmData = getPersonalityDescr(device, rdmReq);
					break;
				case DMX_START_ADDRESS:
					replyRdmData = getDmxStartAddr(device, rdmReq);
					break;					
				case DMX_PERSONALITY:
					replyRdmData = getDmxPersonality(device, rdmReq);
					break;					
				case IDENTIFY_DEVICE:
					replyRdmData = getIdentifyDevice(device, rdmReq);
					break;
				case DEVICE_LABEL:
					replyRdmData = getDeviceLabel(device, rdmReq);
					break;
				default:
					break;
				}
				RdmPacket rdmResp = new RdmPacket(rdmReq, replyRdmData);
				rdmResp.m_srcUid = device.m_uid;
				reply.m_rdmPacket = rdmResp;
				// System.out.println("XXX: RDM " + rdmReq.getParamId() + reply);
				try {
					// m_channel.broadcast(reply);
					m_sendQueue.add(new SendMsgReq(sender, reply));
				} catch (Exception e) {
					m_logger.logError("ArtNetTestNode.sendRdm "
							+ RdmParamId.getName(rdmReq.m_paramIdCode) + ": " + e);
				}
			}
		}
	}
	
	private byte[] getDeviceInfoData(Device device)
	{
		byte[] data = new byte[19];
		int personality = device.m_config;
		int footprint;
		if (personality >= 1 && personality <= device.m_type.m_personalities.length) {
			footprint = device.m_type.m_personalities[personality - 1].m_nSlots;
		} else {
			personality = 1;
			footprint = 1;
		}
		int off = 0;
		off = ArtNetMsgUtil.putBigEndInt16(data, off, 0x0100);	// RDM major/minor protocol version
		off = ArtNetMsgUtil.putBigEndInt16(data, off, device.m_type.m_modelId);
		off = ArtNetMsgUtil.putBigEndInt16(data, off, device.m_type.m_productCategory);
		off = ArtNetMsgUtil.putBigEndInt32(data, off, ETC_SW_VERSION_NUMBER);
		off = ArtNetMsgUtil.putBigEndInt16(data, off, footprint);	
		data[off++] = (byte)personality;
		data[off++] = (byte)device.m_type.m_personalities.length;
		off = ArtNetMsgUtil.putBigEndInt16(data, off, device.m_dmx);
		off = ArtNetMsgUtil.putBigEndInt16(data, off, 0);	// Subdevice count
		data[off++] = 0;	// Sensor count
		return data;
	}
	
	private byte[] getSupportedParamIds(Device device)
	{
		byte[] data = new byte[2*ETC_SUPPORTED_PARAMETERS.length];
		int off = 0;
		for (RdmParamId paramId: ETC_SUPPORTED_PARAMETERS) {
			off = ArtNetMsgUtil.putBigEndInt16(data, off, paramId.getCode());
		}
		return data;
	}
	
	private byte[] getPersonalityDescr(Device device, RdmPacket rdmReq)
	{
		byte[] data = null;
		if (rdmReq.m_paramDataLen >= 1) {
			int personality = rdmReq.m_paramData[0] & 0xff;
			if (personality >= 1 && personality <= device.m_type.m_personalities.length) {
				DevicePersonality personalityDescr = device.m_type.m_personalities[personality-1];
				data = new byte[2 + 2 + personalityDescr.m_name.length()];
				int off = 0;
				data[off++] = (byte)personality;
				data[off++] = 0;
				off = ArtNetMsgUtil.putBigEndInt16(data, off, personalityDescr.m_nSlots);
				off = ArtNetMsgUtil.putString(data, off, personalityDescr.m_name);
			}
		}
		return data;
	}
	
	private byte[] getDeviceLabel(Device device, RdmPacket rdmReq)
	{
		byte[] data = null;
		if (rdmReq.m_command == RdmPacket.CMD_SET) {
			if (rdmReq.m_paramDataLen > 0 && rdmReq.m_paramData != null) {
				device.m_label = new String(rdmReq.m_paramData);
			}
		} else {
			data = device.m_label != null ? device.m_label.getBytes() : new byte[0];
		}
		return data;
	}
	
	private byte[] getDmxStartAddr(Device device, RdmPacket rdmReq)
	{
		byte[] data = null;
		if (rdmReq.m_command == RdmPacket.CMD_SET) {
			if (rdmReq.m_paramDataLen >= 2 && rdmReq.m_paramData != null) {
				int dmxAddr = ArtNetMsgUtil.getBigEndInt16(rdmReq.m_paramData, 0);
				if (dmxAddr >= 1 && dmxAddr <= 512) {
					device.m_dmx = dmxAddr;
				}
			}
		} else {
			data = new byte[2];
			ArtNetMsgUtil.putBigEndInt16(data, 0, device.m_dmx);
		}
		return data;
	}
	
	private byte[] getDmxPersonality(Device device, RdmPacket rdmReq)
	{
		byte[] data = null;
		if (rdmReq.m_command == RdmPacket.CMD_SET) {
			if (rdmReq.m_paramDataLen >= 1 && rdmReq.m_paramData != null) {
				int personality = rdmReq.m_paramData[0] & 0xff;
				if (personality >= 1 && personality <= device.m_type.m_personalities.length) {
					device.m_config = personality;
				}
			}
		} else {
			data = new byte[] {(byte)device.m_config, (byte)device.m_type.m_personalities.length};
		}
		return data;
	}
	
	private byte[] getIdentifyDevice(Device device, RdmPacket rdmReq)
	{
		byte[] data = null;
		if (rdmReq.m_command == RdmPacket.CMD_SET) {
			if (rdmReq.m_paramDataLen >= 1 && rdmReq.m_paramData != null) {
				boolean isOn = (rdmReq.m_paramData[0] & 0xff) != 0;
				if (isOn) {
					m_identifyingDevices.add(device.m_uid);
				} else {
					m_identifyingDevices.remove(device.m_uid);
				}
				synchronized (m_identifyDeviceHandlers) {
					if (m_identifyDeviceHandlers.isEmpty()) {
						m_logger.logError("IDENTIFY " + (isOn ? "ON" : "OFF") + " uid=" + device.m_uid + " univ="
								+ device.m_anPort + " dmx=" + device.m_dmx);
					} else {
						for (IdentifyDeviceHandler handler: m_identifyDeviceHandlers) {
							try {
								handler.handleIdentifyDevice(device.m_uid, device.m_anPort, device.m_dmx, isOn);
							} catch (Exception e) {
								m_logger.logError("ArtNetTestNode: exception in identify handler: " + e);
							}
						}
					}
				}
			}
		} else {
			data = new byte[] {(byte)(m_identifyingDevices.contains(device.m_uid) ? 1 : 0)};
		}
		return data;
	}

	@Override
	public void msgArrived(ArtNetChannel chan, ArtNetOpcode opcode, byte[] buff,
							int len, InetSocketAddress sender,
			InetSocketAddress receiver) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void msgArrived(ArtNetChannel chan, byte[] msg, int len, InetSocketAddress sender,
			InetSocketAddress receiver) {
		// TODO Auto-generated method stub
		
	}
	
	private static class DevicePersonality
	{
		private final int m_nSlots;
		private final String m_name;
		
		private DevicePersonality(int nSlots, String name)
		{
			m_nSlots = nSlots;
			m_name = name;
		}
	}
	
	private static DevicePersonality[] ETC_CS_PERSONALITIES = {
			new DevicePersonality(5, "5 Channel"),
			new DevicePersonality(6, "Direct"),
			new DevicePersonality(1, "1 Channel"),
			new DevicePersonality(3, "RGB"),
	};
	
	private static final int ETC_PRODUCT_CATEGORY = 0x0509;	// DIMMER_CS_LED
	private static final int ETC_SW_VERSION_NUMBER = 0x00010203;
	private static final String ETC_SW_VERSION_LABEL = "1.2.3";
	private static final RdmParamId[] ETC_SUPPORTED_PARAMETERS = new RdmParamId[] {
			RdmParamId.SOFTWARE_VERSION_LABEL,
			RdmParamId.MANUFACTURER_LABEL,
			RdmParamId.DEVICE_MODEL_DESCRIPTION,
			RdmParamId.DEVICE_LABEL,
			RdmParamId.DMX_PERSONALITY_DESCRIPTION,
			RdmParamId.DMX_PERSONALITY,
	};
	
	private static enum DeviceType
	{
		CS_PAR(0x6574, "ETC", 0x201, "CS PAR", ETC_CS_PERSONALITIES, ETC_PRODUCT_CATEGORY),
		CS_PAR_DB(0x6574, "ETC", 0x202, "CS PAR DB", ETC_CS_PERSONALITIES, ETC_PRODUCT_CATEGORY),
		CS_SPOT(0x6574, "ETC", 0x205, "CS SPOT", ETC_CS_PERSONALITIES, ETC_PRODUCT_CATEGORY),
		CS_SPOT_DB(0x6574, "ETC", 0x206, "CS SPOT DB", ETC_CS_PERSONALITIES, ETC_PRODUCT_CATEGORY),
		CS_SPOT_JR(0x6574, "ETC", 0x219, "CS SPOT JR", ETC_CS_PERSONALITIES, ETC_PRODUCT_CATEGORY),
		CS_SPOT_JR_DB(0x6574, "ETC", 0x21A, "CS SPOT JR DB",
										ETC_CS_PERSONALITIES, ETC_PRODUCT_CATEGORY),
		;
		
		private final int m_makerId;
		private final String m_makerName;
		private final int m_modelId;
		private final String m_modelName;
		private final int m_productCategory;
		private final DevicePersonality[] m_personalities;
		
		private DeviceType(int makerId, String makerName, int modelId, String modelName,
							DevicePersonality[] personalities, int productCategory)
		{
			m_makerId = makerId;
			m_makerName = makerName;
			m_modelId = modelId;
			m_modelName = modelName;
			m_personalities = personalities;
			m_productCategory = productCategory;
		}
	}
	
	private static class Device
	{
		private final ArtNetUniv m_anPort;
		private final DeviceType m_type;
		private final int m_serial;
		private final ACN_UID m_uid;
		
		private int m_dmx = 1;
		private int m_config = 1;
		private String m_label = "";
		private int m_freq = 0;
		
		private Device(ArtNetUniv anPort, DeviceType type, int serial)
		{
			m_anPort = anPort;
			m_type = type;
			m_serial = serial;
			m_uid = new ACN_UID(m_type.m_makerId, m_serial);
		}
		
		private Device(ArtNetUniv anPort, JSONValue_Object devInfo, int serial)
		{
			m_anPort = anPort;
			DeviceType type;
			try {
				type = DeviceType.valueOf(devInfo.getString(PN_TYPE, DeviceType.CS_PAR.name()));
			} catch (Exception e) {
				type = DeviceType.CS_PAR;
			}
			m_type = type;
			m_serial = (int)devInfo.getNumber(PN_SERIAL, serial);
			m_uid = new ACN_UID(m_type.m_makerId, m_serial);
			
			m_dmx = getParam(devInfo, PN_DMX, m_dmx);
			m_config = getParam(devInfo, PN_CONFIG, m_config);
			m_label = devInfo.getString(PN_CONFIG, m_label);
			m_freq = getParam(devInfo, PN_FREQ, m_freq);
		}
		
		private void updateJSON(JSONValue_Object params)
		{
			JSONValue_Object anPortDevList = params.getObject(m_anPort.toString(), null);
			if (anPortDevList == null) {
				anPortDevList = new JSONValue_Object();
				params.put(m_anPort.toString(), anPortDevList);
			}
			JSONValue_Object devInfo = new JSONValue_Object();
			devInfo.put(PN_TYPE, m_type.toString());
			devInfo.put(PN_DMX, m_dmx);
			devInfo.put(PN_CONFIG, m_config);
			devInfo.put(PN_LABEL, m_label);
			devInfo.put(PN_FREQ, m_freq);
			anPortDevList.put(m_serial+"", devInfo);
		}
	}
	
	private static int getParam(JSONValue_Object dictionary, String field, int def)
	{
		JSONValue val = dictionary.get(field, null);
		if (val == null) {
			return def;
		} else if (val instanceof JSONValue_Number) {
			return (int)((JSONValue_Number)val).m_value;
		} else if (val instanceof JSONValue_String) {
			return myParseInt(((JSONValue_String)val).m_value, def);
		} else {
			return def;
		}
	}
	
	private static int myParseInt(String sval, int def)
	{
		try {
			int radix = 10;
			if (sval.startsWith("0x")) {
				radix = 16;
				sval = sval.substring(2);
			}
			return Integer.parseInt(sval, radix);
		} catch (Exception e) {
			return def;
		}		
	}
}
