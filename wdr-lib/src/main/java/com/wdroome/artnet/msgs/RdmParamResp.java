package com.wdroome.artnet.msgs;

import java.util.List;
import java.util.ArrayList;

import com.wdroome.util.ByteAOL;

public class RdmParamResp
{
	public static class DeviceInfo extends RdmParamData
	{
		public final int m_protoVersMajor;
		public final int m_protoVersMinor;
		public final int m_model;
		public final int m_category;
		public final int m_softwareVersion;
		public final int m_dmxFootprint;
		public final int m_currentPersonality;
		public final int m_nPersonalities;
		public final int m_startAddr;
		public final int m_numSubDevs;
		public final int m_numSensors;
		
		public DeviceInfo(RdmPacket rdmPacket)
		{
			super(RdmParamId.DEVICE_INFO, rdmPacket.m_paramData);
			int off = 0;
			if (off + rdmPacket.m_paramDataLen < 19) {
				throw new IllegalArgumentException("RDM DeviceInfo resp: short data "
							+ rdmPacket.m_paramDataLen);
			}
			m_protoVersMajor = rdmPacket.m_paramData[off++] & 0xff;
			m_protoVersMinor = rdmPacket.m_paramData[off++] & 0xff;
			m_model = ArtNetMsg.getBigEndInt16(rdmPacket.m_paramData, off);
			off += 2;
			m_category = ArtNetMsg.getBigEndInt16(rdmPacket.m_paramData, off);
			off += 2;
			m_softwareVersion = ArtNetMsg.getBigEndInt32(rdmPacket.m_paramData, off);
			off += 4;
			m_dmxFootprint = ArtNetMsg.getBigEndInt16(rdmPacket.m_paramData, off);
			off += 2;
			m_currentPersonality = rdmPacket.m_paramData[off++] & 0xff;
			m_nPersonalities = rdmPacket.m_paramData[off++] & 0xff;
			m_startAddr = ArtNetMsg.getBigEndInt16(rdmPacket.m_paramData, off);
			off += 2;
			m_numSubDevs = ArtNetMsg.getBigEndInt16(rdmPacket.m_paramData, off);
			off += 2;
			m_numSensors = rdmPacket.m_paramData[off++] & 0xff;
		}
		
		@Override
		public String toString()
		{
			return "DeviceInfo("
					+ "vers=" + m_protoVersMajor + "." + m_protoVersMinor
					+ ",model=" + m_model
					+ ",cat=0x" + Integer.toHexString(m_category)
					+ ",swVers=" + m_softwareVersion
					+ ",addr=" + m_startAddr + "-" + (m_startAddr + m_dmxFootprint - 1)
					+ (m_numSubDevs > 0 ? (",#sub=" + m_numSubDevs) : "")
					+ (m_numSensors > 0 ? (",#sensor=" + m_numSensors) : "")
					+ ")";
		}
	}
	
	public static class PidList extends RdmParamData
	{
		public final List<RdmParamId> m_stdPids;
		public final List<Integer> m_otherPids;
		
		public PidList(RdmPacket rdmPacket)
		{
			super(rdmPacket.m_paramIdCode, rdmPacket.m_paramData);
			m_stdPids = new ArrayList<>();
			m_otherPids = new ArrayList<>();
			for (int off = 0; off+1 < rdmPacket.m_paramDataLen; off += 2) {
				int paramIdCode = ArtNetMsg.getBigEndInt16(rdmPacket.m_paramData, off);
				RdmParamId paramId = RdmParamId.getParamId(paramIdCode);
				if (paramId != RdmParamId.UNKNOWN_PARAM_ID) {
					m_stdPids.add(paramId);
				} else {
					m_otherPids.add(paramIdCode);
				}
			}
		}
		
		@Override
		public String toString()
		{
			StringBuilder buff = new StringBuilder();
			buff.append(paramNameOrCode() + "[");
			String sep = "";
			for (RdmParamId pid: m_stdPids) {
				buff.append(sep + pid);
				sep = ",";
			}
			for (Integer code: m_otherPids) {
				buff.append(sep + "0x" + Integer.toHexString(code));
				sep = ",";
			}
			buff.append("]");
			return buff.toString();
		}
	}
	
	public static class StringReply extends RdmParamData
	{
		public final String m_string;
		
		public StringReply(RdmPacket rdmPacket)
		{
			super(rdmPacket.m_paramIdCode, rdmPacket.m_paramData);
			StringBuilder buff = new StringBuilder();
			if (rdmPacket.m_paramData != null) {
				for (byte b: rdmPacket.m_paramData) {
					if (b == 0) {
						break;
					}
					buff.append((char) b);
				} 
			}
			m_string = buff.toString();
		}
		
		@Override
		public String toString()
		{
			return paramNameOrCode() + "(" + m_string + ")";
		}
	}
	
	public static class PersonalityDesc extends RdmParamData
	{
		public final int m_personalityNumber;
		public final int m_nSlots;
		public final String m_desc;
		
		public PersonalityDesc(RdmPacket rdmPacket)
		{
			super(rdmPacket.m_paramIdCode, rdmPacket.m_paramData);
			// System.out.println("XXX pers/desc " + new ByteAOL(rdmPacket.m_paramData).toHex());
			if (rdmPacket.m_paramDataLen < 3) {
				throw new IllegalArgumentException("RDM DeviceInfo resp: short data "
						+ rdmPacket.m_paramDataLen);				
			}
			int off = 0;
			m_personalityNumber = rdmPacket.m_paramData[off++] & 0xff;
			m_nSlots = ArtNetMsg.getBigEndInt16(rdmPacket.m_paramData, off);
			off += 2;
			StringBuilder buff = new StringBuilder();
			if (rdmPacket.m_paramData != null) {
				for (; off < rdmPacket.m_paramDataLen; off++) {
					byte b = rdmPacket.m_paramData[off];
					if (b == 0) {
						break;
					}
					buff.append((char) b);
				} 
			}
			m_desc = buff.toString();
		}
		
		public PersonalityDesc(int number, int numChannels, String desc)
		{
			super(RdmParamId.DMX_PERSONALITY_DESCRIPTION, new byte[0]);
			m_personalityNumber = number;
			m_nSlots = numChannels;
			m_desc = desc;
		}
		
		@Override
		public String toString()
		{
			return paramNameOrCode() + "(" + m_personalityNumber + ":" + m_desc
						+ ",#slots=" + m_nSlots + ")";
		}
	}
}
