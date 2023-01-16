package com.wdroome.artnet.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.io.Closeable;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;

import com.wdroome.artnet.ACN_UID;
import com.wdroome.artnet.ArtNetChannel;
import com.wdroome.artnet.ArtNetNode;
import com.wdroome.artnet.ArtNetNodePort;
import com.wdroome.artnet.ArtNetPort;
import com.wdroome.artnet.ArtNetRdmRequest;

import com.wdroome.artnet.msgs.ArtNetRdm;
import com.wdroome.artnet.msgs.RdmPacket;
import com.wdroome.artnet.msgs.RdmParamId;
import com.wdroome.artnet.msgs.RdmParamData;
import com.wdroome.artnet.msgs.RdmParamResp;

public class ArtNetGetDevices implements Closeable
{
	public static final String UNKNOWN_DESC = "???";
	
	public static class DeviceInfo
	{
		public final ACN_UID m_uid;
		public final ArtNetNodePort m_nodePort;
		public final RdmParamResp.DeviceInfo m_deviceInfo;
		public final String m_manufacturer;
		public final String m_model;
		public final TreeMap<Integer, RdmParamResp.PersonalityDesc> m_personalities;
		public final List<RdmParamId> m_stdParamIds;
		public final List<Integer> m_otherParamIds;
		
		public DeviceInfo(ACN_UID uid,
						ArtNetNodePort nodePort,
						RdmParamResp.DeviceInfo deviceInfo,
						TreeMap<Integer, RdmParamResp.PersonalityDesc> personalities,
						String manufacturer,
						String model,
						RdmParamResp.PidList supportedPids)
		{
			m_uid = uid;
			m_nodePort = nodePort;
			m_deviceInfo = deviceInfo;
			m_personalities = personalities != null ? personalities : new TreeMap<>();
			m_manufacturer = manufacturer;
			m_model = model;
			m_stdParamIds = supportedPids != null ? supportedPids.m_stdPids : List.of();
			m_otherParamIds = supportedPids != null ? supportedPids.m_otherPids : List.of();
		}
		
		@Override
		public String toString()
		{
			return	"RdmDevice(" + m_uid
					+ "@" + m_nodePort
					+ getManModel(",")
					+ ",dmx=" + m_deviceInfo.m_startAddr + "-"
							+ (m_deviceInfo.m_startAddr + m_deviceInfo.m_dmxFootprint - 1)
					+ ",config=" + m_deviceInfo.m_currentPersonality + "/"
							+ m_deviceInfo.m_nPersonalities
					+ ",model=" + m_deviceInfo.m_model
					+ ",cat=0x" + Integer.toHexString(m_deviceInfo.m_category)
					+ (m_deviceInfo.m_numSubDevs > 0 ? (",#sub=" + m_deviceInfo.m_numSubDevs) : "")
					+ ",pids=" + m_stdParamIds
					+ ",personalities=" + m_personalities
					+ (!m_otherParamIds.isEmpty() ? (",xpids=" + m_otherParamIds) : "")
					+ ")";
		}
		
		private String getManModel(String prefix)
		{
			if (!isUnknownDesc(m_manufacturer) && !isUnknownDesc(m_model)) {
				return prefix + m_manufacturer + "/" + m_model;
			} else if (!isUnknownDesc(m_manufacturer) && isUnknownDesc(m_model)) {
				return prefix + m_manufacturer + "/" + UNKNOWN_DESC;
			} else if (isUnknownDesc(m_manufacturer) && !isUnknownDesc(m_model)) {
				return prefix + UNKNOWN_DESC + "/" + m_model;
			} else {
				return "";
			}
		}
		
		public boolean isUnknownDesc(String name)
		{
			return name == null || name.isEmpty() || name.equals(UNKNOWN_DESC);
		}
		
		public String supportedPids()
		{
			StringBuilder buff = new StringBuilder();
			String sep = "";
			for (RdmParamId pid: m_stdParamIds) {
				buff.append(sep + pid);
				sep = ",";
			}
			for (Integer code: m_otherParamIds) {
				buff.append(sep + "0x" + Integer.toHexString(code));
				sep = ",";
			}
			buff.append("]");
			return buff.toString();
		}
	}
	
	private final ArtNetChannel m_channel;
	private final boolean m_sharedChannel;
	private Map<ArtNetNodePort, Set<ACN_UID>> m_uidMap = null;
	private final ArtNetRdmRequest m_rdmReq;
	
	public ArtNetGetDevices(ArtNetChannel channel, Map<ArtNetNodePort, Set<ACN_UID>> uidMap)
						throws IOException
	{
		if (channel != null) {
			m_channel = channel;
			m_sharedChannel = true;
		} else {
			m_channel = new ArtNetChannel();
			m_sharedChannel = false;			
		}
		if (uidMap == null) {
			uidMap = new ArtNetFindRdmUids(m_channel).getUidMap(null);
		}
		m_uidMap = uidMap;
		m_rdmReq = new ArtNetRdmRequest(m_channel, m_uidMap);
	}
		
	@Override
	public void close() throws IOException
	{
		if (!m_sharedChannel) {
			m_channel.shutdown();
		}
	}

	public Map<ACN_UID, DeviceInfo> getDeviceMap(List<String> errors)
	{
		Map<ACN_UID, DeviceInfo> deviceInfoMap = new HashMap<>();
		
		for (Set<ACN_UID> uidSet: m_uidMap.values()) {
			for (ACN_UID uid: uidSet) {
				try {
					DeviceInfo info = getDevice(uid);
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
		}
		return deviceInfoMap;
	}
	
	public DeviceInfo getDevice(ACN_UID uid) throws IOException
	{
		ArtNetNodePort nodePort = findNodePort(uid);
		if (nodePort == null) {
			return null;
		}
		RdmPacket devInfoReply = m_rdmReq.sendRequest(nodePort.m_nodeAddr.m_nodeAddr,
													nodePort.m_port, uid,
													false, RdmParamId.DEVICE_INFO, null);
		if (devInfoReply == null || !devInfoReply.isRespAck()) {
			return null;
		}
		RdmParamResp.DeviceInfo devInfo = new RdmParamResp.DeviceInfo(devInfoReply);
		
		RdmPacket manufacturerReply = sendReq(nodePort.m_nodeAddr.m_nodeAddr,
													nodePort.m_port, uid,
													false, RdmParamId.MANUFACTURER_LABEL, null);
		String manufacturer = UNKNOWN_DESC;
		if (manufacturerReply != null && manufacturerReply.isRespAck()) {
			manufacturer = new RdmParamResp.StringReply(manufacturerReply).m_string;
		}
		
		RdmPacket modelReply = sendReq(nodePort.m_nodeAddr.m_nodeAddr,
													nodePort.m_port, uid,
													false, RdmParamId.DEVICE_MODEL_DESCRIPTION, null);
		String model = UNKNOWN_DESC;
		if (modelReply != null && modelReply.isRespAck()) {
			model = new RdmParamResp.StringReply(modelReply).m_string;
		}
		
		RdmPacket supportedPidsReply = sendReq(nodePort.m_nodeAddr.m_nodeAddr,
													nodePort.m_port, uid,
													false, RdmParamId.SUPPORTED_PARAMETERS, null);
		RdmParamResp.PidList supportedPids = new RdmParamResp.PidList(supportedPidsReply);
				
		return new DeviceInfo(uid, nodePort, devInfo,
							getPersonalities(nodePort, uid, devInfo.m_nPersonalities),
							manufacturer, model, supportedPids);
	}
	
	private ArtNetNodePort findNodePort(ACN_UID uid)
	{
		for (Map.Entry<ArtNetNodePort, Set<ACN_UID>> ent: m_uidMap.entrySet()) {
			if (ent.getValue().contains(uid)) {
				return ent.getKey();
			}
		}
		return null;
	}
	
	private RdmPacket sendReq(InetSocketAddress ipAddr, ArtNetPort port, ACN_UID destUid,
									boolean isSet, RdmParamId paramId, byte[] requestData)
	{
		try {
			return m_rdmReq.sendRequest(ipAddr, port, destUid, isSet, paramId, requestData);
		} catch (IOException e) {
			return null;
		}
	}
	
	private TreeMap<Integer,RdmParamResp.PersonalityDesc> getPersonalities(ArtNetNodePort nodePort,
													 ACN_UID uid, int nPersonalities) throws IOException
	{
		TreeMap<Integer,RdmParamResp.PersonalityDesc> personalities = new TreeMap<>();
		for (int iPersonality = 1; iPersonality <= nPersonalities; iPersonality++) {
			RdmPacket personalityDescReply = sendReq(nodePort.m_nodeAddr.m_nodeAddr,
													nodePort.m_port, uid,
													false, RdmParamId.DMX_PERSONALITY_DESCRIPTION,
													new byte[] {(byte)iPersonality});
			RdmParamResp.PersonalityDesc desc;
			if (personalityDescReply != null) {
				desc = new RdmParamResp.PersonalityDesc(personalityDescReply);
			} else {
				desc = new RdmParamResp.PersonalityDesc(iPersonality, 1, UNKNOWN_DESC);
			}
			personalities.put(iPersonality, desc);
		}
		return personalities;
	}
	
	public static void main(String[] args)
	{
		try (ArtNetGetDevices devs = new ArtNetGetDevices(null, null)) {
			ArrayList<String> errors = new ArrayList<>();
			Map<ACN_UID, DeviceInfo> deviceMap = devs.getDeviceMap(errors);
			if (!errors.isEmpty()) {
				System.out.println("errors: " + errors);
			}
			System.out.println(deviceMap.size() + " devices:");
			for (DeviceInfo devInfo: deviceMap.values()) {
				System.out.println(devInfo);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
}
