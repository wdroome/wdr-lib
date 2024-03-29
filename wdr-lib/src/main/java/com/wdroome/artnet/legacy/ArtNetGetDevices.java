package com.wdroome.artnet.legacy;

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
import com.wdroome.artnet.ArtNetUnivAddr;
import com.wdroome.artnet.ArtNetUniv;
import com.wdroome.artnet.ArtNetRdmRequest;
import com.wdroome.artnet.RdmDevice;
import com.wdroome.artnet.ArtNetManager;

import com.wdroome.artnet.msgs.ArtNetRdm;
import com.wdroome.artnet.msgs.RdmPacket;
import com.wdroome.artnet.msgs.RdmParamId;
import com.wdroome.artnet.msgs.RdmParamData;
import com.wdroome.artnet.msgs.RdmParamResp;
import com.wdroome.artnet.msgs.RdmProductCategories;

public class ArtNetGetDevices implements Closeable
{
	private final ArtNetChannel m_channel;
	private final boolean m_isSharedChannel;
	private final ArtNetManager m_manager;
	private final Map<ACN_UID, ArtNetUnivAddr> m_uidMap;
	
	public ArtNetGetDevices(ArtNetChannel channel, Map<ACN_UID, ArtNetUnivAddr> uidMap)
						throws IOException
	{
		if (channel != null) {
			m_channel = channel;
			m_isSharedChannel = true;
		} else {
			m_channel = new ArtNetChannel();
			m_isSharedChannel = false;			
		}
		m_manager = new ArtNetManager(m_channel);
		if (uidMap == null) {
			uidMap = m_manager.getUidsToUnivAddrs();
		}
		m_uidMap = uidMap;
	}
		
	@Override
	public void close() throws IOException
	{
		m_manager.close();
		if (!m_isSharedChannel) {
			m_channel.shutdown();
		}
	}

	public Map<ACN_UID, RdmDevice> getDeviceMap(List<String> errors) throws IOException
	{
		ArtNetRdmRequest rdmRequest = new ArtNetRdmRequest(m_channel, m_manager.getUidsToUnivAddrs());

		Map<ACN_UID, RdmDevice> deviceInfoMap = new HashMap<>();
		for (ACN_UID uid: m_uidMap.keySet()) {
			try {
				RdmDevice info = new RdmDevice(uid, m_manager.getUidsToUnivAddrs().get(uid), rdmRequest);
				deviceInfoMap.put(uid, info);
			} catch (Exception e) {
				if (errors != null) {
					errors.add("Exception getting UID " + uid + ": " + e);
				}
			}
		}
		return deviceInfoMap;
	}
	
	private ArtNetUnivAddr findNodePort(ACN_UID uid)
	{
		return m_uidMap.get(uid);
	}
	
	private boolean isSupported(RdmParamId paramId, RdmParamResp.PidList supportedPids)
	{
		return paramId.isRequired() || (supportedPids != null && supportedPids.isSupported(paramId));
	}
	
	private RdmPacket sendReq(InetSocketAddress ipAddr, ArtNetUniv port, ACN_UID destUid,
									boolean isSet, RdmParamId paramId, byte[] requestData)
	{
		try {
			return m_manager.sendRdmRequest(destUid, isSet, paramId, requestData);
		} catch (IOException e) {
			return null;
		}
	}
	
	private TreeMap<Integer,RdmParamResp.PersonalityDesc> getPersonalities(ArtNetUnivAddr nodePort,
													 ACN_UID uid, int nPersonalities,
													 RdmParamResp.PidList supportedPids) throws IOException
	{
		TreeMap<Integer,RdmParamResp.PersonalityDesc> personalities = new TreeMap<>();
		boolean ok = isSupported(RdmParamId.DMX_PERSONALITY_DESCRIPTION, supportedPids);
		if (ok) {
			for (int iPersonality = 1; iPersonality <= nPersonalities; iPersonality++) {
				RdmPacket personalityDescReply = null;
				if (ok) {
					personalityDescReply = sendReq(nodePort.m_nodeAddr.m_nodeAddr, nodePort.m_univ,
									uid, false, RdmParamId.DMX_PERSONALITY_DESCRIPTION,
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
	
	private TreeMap<Integer,String> getSlotDescs(ArtNetUnivAddr nodePort,
												 ACN_UID uid, int nSlots,
												 RdmParamResp.PidList supportedPids) throws IOException
	{
		TreeMap<Integer,String> slotDescs = new TreeMap<>();
		boolean ok = isSupported(RdmParamId.SLOT_DESCRIPTION, supportedPids);
		if (ok) {
			for (int iSlot = 0; iSlot < nSlots; iSlot++) {
				slotDescs.put(iSlot, RdmDevice.UNKNOWN_DESC);
			}
			for (int iSlot = 0; iSlot < nSlots; iSlot++) {
				if (ok) {
					byte[] iSlotAsBytes = new byte[] { (byte) ((iSlot >> 8) & 0xff), (byte) (iSlot & 0xff) };
					RdmPacket slotDescReply = sendReq(nodePort.m_nodeAddr.m_nodeAddr,
													nodePort.m_univ, uid, false,
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
							+ RdmProductCategories.getCategoryName(devInfo.getDeviceInfo().m_category) + ")");
				
				if (devInfo.getDeviceInfo().m_startAddr > 0 || devInfo.getDeviceInfo().m_dmxFootprint > 0) {
					System.out.println(indent + "dmx addresses: " + devInfo.getDeviceInfo().m_startAddr
								+ "-" + (devInfo.getDeviceInfo().m_startAddr
											+ devInfo.getDeviceInfo().m_dmxFootprint - 1)
								+ " univ: " + devInfo.m_univAddr);
				} else {
					System.out.println(indent + "univ: " + devInfo.m_univAddr);
				}
				System.out.println(indent + "dmx config " + devInfo.getPersonalityDesc());
				System.out.println(indent + "version: " + devInfo.m_softwareVersionLabel);
				/*
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
				*/
				if (!devInfo.m_personalities.isEmpty()) {
					System.out.println(indent + "available configurations:");
					for (int iPers: devInfo.m_personalities.keySet()) {
						System.out.println(indent + indent + devInfo.getPersonalityDesc(iPers));
					}
				}
				if (!devInfo.m_stdParamIds.isEmpty() || !devInfo.m_otherParamIds.isEmpty()) {
					System.out.print(indent + "supported parameters:");
					int lineLen = 1000;
					String sep = "";
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
			}
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
}
