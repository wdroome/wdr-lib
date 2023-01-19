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
import com.wdroome.artnet.RdmDevice;

import com.wdroome.artnet.msgs.ArtNetRdm;
import com.wdroome.artnet.msgs.RdmPacket;
import com.wdroome.artnet.msgs.RdmParamId;
import com.wdroome.artnet.msgs.RdmParamData;
import com.wdroome.artnet.msgs.RdmParamResp;
import com.wdroome.artnet.msgs.RdmProductCategories;

public class ArtNetGetDevices implements Closeable
{
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

	public Map<ACN_UID, RdmDevice> getDeviceMap(List<String> errors)
	{
		Map<ACN_UID, RdmDevice> deviceInfoMap = new HashMap<>();
		
		for (Set<ACN_UID> uidSet: m_uidMap.values()) {
			for (ACN_UID uid: uidSet) {
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
		}
		return deviceInfoMap;
	}
	
	public RdmDevice getDevice(ACN_UID uid) throws IOException
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
		
		RdmPacket supportedPidsReply = sendReq(nodePort.m_nodeAddr.m_nodeAddr,
												nodePort.m_port, uid,
												false, RdmParamId.SUPPORTED_PARAMETERS, null);
		RdmParamResp.PidList supportedPids = new RdmParamResp.PidList(supportedPidsReply);
		
		RdmPacket swVerLabelReply = sendReq(nodePort.m_nodeAddr.m_nodeAddr,
											nodePort.m_port, uid,
											false, RdmParamId.SOFTWARE_VERSION_LABEL, null);
		String swVerLabel = "[" + devInfo.m_softwareVersion + "]";
		if (swVerLabelReply != null) {
			swVerLabel = new RdmParamResp.StringReply(swVerLabelReply).m_string;
		}
		
		String manufacturer = String.format("0x%04x", uid.getManufacturer());
		if (isSupported(RdmParamId.MANUFACTURER_LABEL, supportedPids)) {
			RdmPacket manufacturerReply = sendReq(nodePort.m_nodeAddr.m_nodeAddr, nodePort.m_port, uid, false,
					RdmParamId.MANUFACTURER_LABEL, null);
			if (manufacturerReply != null && manufacturerReply.isRespAck()) {
				manufacturer = new RdmParamResp.StringReply(manufacturerReply).m_string;
			} 
		}
		
		String model = String.format("0x%04x", devInfo.m_model);
		if (isSupported(RdmParamId.DEVICE_MODEL_DESCRIPTION, supportedPids)) {
			RdmPacket modelReply = sendReq(nodePort.m_nodeAddr.m_nodeAddr, nodePort.m_port, uid, false,
					RdmParamId.DEVICE_MODEL_DESCRIPTION, null);
			if (modelReply != null && modelReply.isRespAck()) {
				model = new RdmParamResp.StringReply(modelReply).m_string;
			} 
		}
		
		return new RdmDevice(uid, nodePort, devInfo,
							getPersonalities(nodePort, uid, devInfo.m_nPersonalities, supportedPids),
							getSlotDescs(nodePort, uid, devInfo.m_dmxFootprint, supportedPids),
							manufacturer, model, swVerLabel, supportedPids);
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
	
	private boolean isSupported(RdmParamId paramId, RdmParamResp.PidList supportedPids)
	{
		return paramId.isRequired() || (supportedPids != null && supportedPids.isSupported(paramId));
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
													 ACN_UID uid, int nPersonalities,
													 RdmParamResp.PidList supportedPids) throws IOException
	{
		TreeMap<Integer,RdmParamResp.PersonalityDesc> personalities = new TreeMap<>();
		boolean ok = isSupported(RdmParamId.DMX_PERSONALITY_DESCRIPTION, supportedPids);
		if (ok) {
			for (int iPersonality = 1; iPersonality <= nPersonalities; iPersonality++) {
				RdmPacket personalityDescReply = null;
				if (ok) {
					personalityDescReply = sendReq(nodePort.m_nodeAddr.m_nodeAddr, nodePort.m_port, uid, false,
							RdmParamId.DMX_PERSONALITY_DESCRIPTION, new byte[] { (byte) iPersonality });
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
	
	private TreeMap<Integer,String> getSlotDescs(ArtNetNodePort nodePort,
												 ACN_UID uid, int nSlots,
												 RdmParamResp.PidList supportedPids) throws IOException
	{
		TreeMap<Integer,String> slotDescs = new TreeMap<>();
		boolean ok = isSupported(RdmParamId.SLOT_DESCRIPTION, supportedPids);
		if (ok) {
			for (int iSlot = 0; iSlot < nSlots; iSlot++) {
				String desc = RdmDevice.UNKNOWN_DESC;
				if (ok) {
					byte[] iSlotAsBytes = new byte[] { (byte) ((iSlot >> 8) & 0xff), (byte) (iSlot & 0xff) };
					RdmPacket slotDescReply = sendReq(nodePort.m_nodeAddr.m_nodeAddr, nodePort.m_port, uid, false,
							RdmParamId.SLOT_DESCRIPTION, iSlotAsBytes);
					if (slotDescReply != null && slotDescReply.isRespAck()) {
						desc = new RdmParamResp.StringReply(slotDescReply).m_string;
					} else {
						ok = false;
					}
				}
				slotDescs.put(iSlot, desc);
			} 
		}
		return slotDescs;
	}
	
	public static void main(String[] args)
	{
		try (ArtNetGetDevices devs = new ArtNetGetDevices(null, null)) {
			ArrayList<String> errors = new ArrayList<>();
			Map<ACN_UID, RdmDevice> deviceMap = devs.getDeviceMap(errors);
			if (!errors.isEmpty()) {
				System.out.println("errors: " + errors);
			}
			System.out.println(deviceMap.size() + " devices:");
			int iDev = 0;
			String indent = "    ";
			for (RdmDevice devInfo: RdmDevice.sort(deviceMap.values())) {
				iDev++;
				System.out.println();
				System.out.println("Device " + iDev + "  [" + devInfo.m_uid + "]:");
				System.out.println(indent + devInfo.m_manufacturer + "/" + devInfo.m_model + "  ("
							+ RdmProductCategories.getCategoryName(devInfo.m_deviceInfo.m_category) + ")");
				
				if (devInfo.m_deviceInfo.m_startAddr > 0 || devInfo.m_deviceInfo.m_dmxFootprint > 0) {
					System.out.println(indent + "dmx addresses: " + devInfo.m_deviceInfo.m_startAddr
								+ "-" + (devInfo.m_deviceInfo.m_startAddr
											+ devInfo.m_deviceInfo.m_dmxFootprint - 1)
								+ " univ: " + devInfo.m_nodePort);
				} else {
					System.out.println(indent + "univ: " + devInfo.m_nodePort);
				}
				System.out.println(indent + "dmx config " + devInfo.getPersonalityDesc());
				System.out.println(indent + "version: " + devInfo.m_softwareVersionLabel);
				if (!devInfo.m_stdParamIds.isEmpty() || !devInfo.m_otherParamIds.isEmpty()) {
					System.out.print(indent + "parameters:");
					int lineLen = indent.length() + 11;
					String sep = " ";
					for (RdmParamId pid: devInfo.m_stdParamIds) {
						String s = pid.toString();
						if (lineLen + s.length() > 75) {
							System.out.println();
							System.out.print(indent + indent);
							lineLen = 2*indent.length();
							sep = "";
						}
						System.out.print(sep + s);
						lineLen += s.length() + sep.length();
						sep = " ";
					}
					for (int pid: devInfo.m_otherParamIds) {
						String s = "0x" + Integer.toHexString(pid);
						if (lineLen + s.length() > 75) {
							System.out.println();
							System.out.print(indent + indent);
							lineLen = 2*indent.length();
							sep = "";
						}
						System.out.print(sep + s);
						lineLen += s.length() + sep.length();
						sep = " ";
					}
					System.out.println();
				}
				if (!devInfo.m_slotDescs.isEmpty()) {
					System.out.print(indent + "slots: ");
					int lineLen = indent.length() + 6;
					String sep = " ";
					for (Map.Entry<Integer,String> ent: devInfo.m_slotDescs.entrySet()) {
						String s = ent.getKey() + ": " + ent.getValue();
						if (lineLen + s.length() > 75) {
							System.out.println();
							System.out.print(indent + indent);
							lineLen = 2*indent.length();
							sep = "";
						}
						System.out.print(sep + s);
						lineLen += s.length() + sep.length();
						sep = " ";
					}
					System.out.println();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
}
