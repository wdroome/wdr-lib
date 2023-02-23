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
import com.wdroome.artnet.ArtNetPort;
import com.wdroome.artnet.ACN_UID;

import com.wdroome.artnet.msgs.ArtNetMsg;
import com.wdroome.artnet.msgs.ArtNetPoll;
import com.wdroome.artnet.msgs.ArtNetPollReply;
import com.wdroome.artnet.msgs.ArtNetDmx;
import com.wdroome.artnet.msgs.ArtNetTodRequest;
import com.wdroome.artnet.msgs.ArtNetTodControl;
import com.wdroome.artnet.msgs.ArtNetTodData;
import com.wdroome.artnet.msgs.ArtNetRdm;

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
	private final List<ArtNetPort> m_anPorts;
	
	// Map from ArtNet ports to RDM devices on that port.
	private final TreeMap<ArtNetPort, List<Device>> m_deviceMap;
	
	// ArtNetPollReply message describing this node.
	private final ArtNetPollReply m_pollReply;
	private final List<ArtNetPollReply> m_pollReplies;
	
	// Most recent DMX message for each supported DMX output port.
	private final Map<ArtNetPort, DmxMsgTS> m_lastDmxMsg = new ConcurrentHashMap<>();
	
	private final Map<ArtNetPort, AtomicLong> m_numDmxMsgs = new HashMap<>();
	private final AtomicLong m_numBadDmxMsgs = new AtomicLong(0);
	private final AtomicLong m_numPollMsgs = new AtomicLong(0);
	
	private final Set<PollHandler> m_pollHandlers = new HashSet<>();
	private final Set<PollReplyHandler> m_pollReplyHandlers = new HashSet<>();
	private final Set<DmxHandler> m_dmxHandlers = new HashSet<>();
	
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
		for (ArtNetPort port: m_anPorts) {
			m_numDmxMsgs.put(port, new AtomicLong(0));
		}
		m_pollReply = makePollReply();
		m_pollReplies = makePollReplies();
		m_channel.addReceiver(this);
	}
	
	private TreeMap<ArtNetPort, List<Device>> readConfig()
	{
		TreeMap<ArtNetPort, List<Device>> map = new TreeMap<>();
		int nextDevSerial = 1;
		for (String anPortKey: new TreeSet<String>(m_nodeParam.keySet())) {
			ArtNetPort anPort;
			try {
				anPort = new ArtNetPort(anPortKey);
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
				map.put(new ArtNetPort(port), new ArrayList<>());
			}
		}
		return map;
	}
	
	private List<ArtNetPollReply> makePollReplies()
	{
		List<ArtNetPollReply> replies = new ArrayList<>();
		int iPort = 1;
		for (ArtNetPort anPort: m_anPorts) {
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
	
	public DmxMsgTS getDmxLevels(ArtNetPort port)
	{
		return m_lastDmxMsg.get(port);
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
					m_channel.broadcast(reply);
				} catch (IOException e) {
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
				ArtNetPort anPort = m_anPorts.get(i);
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
								new ArtNetPort(dmx.m_net, dmx.m_subUni).toString(), 60000, "");
			}
		}
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
	
	private static enum DeviceType
	{
		CS_PAR(0x6574, "ETC", 0x201, "CS PAR", ETC_CS_PERSONALITIES),
		CS_PAR_DB(0x6574, "ETC", 0x202, "CS PAR DB", ETC_CS_PERSONALITIES),
		CS_SPOT(0x6574, "ETC", 0x205, "CS SPOT", ETC_CS_PERSONALITIES),
		CS_SPOT_DB(0x6574, "ETC", 0x206, "CS SPOT DB", ETC_CS_PERSONALITIES),
		CS_SPOT_JR(0x6574, "ETC", 0x219, "CS SPOT JR", ETC_CS_PERSONALITIES),
		CS_SPOT_JR_DB(0x6574, "ETC", 0x21A, "CS SPOT JR DB", ETC_CS_PERSONALITIES),
		;
		
		private final int m_makerId;
		private final String m_makerName;
		private final int m_modelId;
		private final String m_modelName;
		private final DevicePersonality[] m_personalities;
		
		private DeviceType(int makerId, String makerName, int modelId, String modelName,
							DevicePersonality[] personalities)
		{
			m_makerId = makerId;
			m_makerName = makerName;
			m_modelId = modelId;
			m_modelName = modelName;
			m_personalities = personalities;
		}
	}
	
	private static class Device
	{
		private final ArtNetPort m_anPort;
		private final DeviceType m_type;
		private final int m_serial;
		private final ACN_UID m_uid;
		
		private int m_dmx = 1;
		private int m_config = 1;
		private String m_label = "";
		private int m_freq = 0;
		
		private Device(ArtNetPort anPort, DeviceType type, int serial)
		{
			m_anPort = anPort;
			m_type = type;
			m_serial = serial;
			m_uid = new ACN_UID(m_type.m_makerId, m_serial);
		}
		
		private Device(ArtNetPort anPort, JSONValue_Object devInfo, int serial)
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
