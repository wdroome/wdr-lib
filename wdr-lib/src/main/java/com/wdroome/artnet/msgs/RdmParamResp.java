package com.wdroome.artnet.msgs;

import java.util.List;
import java.util.ArrayList;

import com.wdroome.util.ByteAOL;

/**
 * Parameter-specific data for an RDM response message from an RDM device.
 * TO cut down on the number of files, the classes are bundled into this one class.
 * @author wdr
 */
public class RdmParamResp
{
	/**
	 * Response for a RDM DEVICE_INFO request.
	 */
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
			m_model = ArtNetMsgUtil.getBigEndInt16(rdmPacket.m_paramData, off);
			off += 2;
			m_category = ArtNetMsgUtil.getBigEndInt16(rdmPacket.m_paramData, off);
			off += 2;
			m_softwareVersion = ArtNetMsgUtil.getBigEndInt32(rdmPacket.m_paramData, off);
			off += 4;
			m_dmxFootprint = ArtNetMsgUtil.getBigEndInt16(rdmPacket.m_paramData, off);
			off += 2;
			m_currentPersonality = rdmPacket.m_paramData[off++] & 0xff;
			m_nPersonalities = rdmPacket.m_paramData[off++] & 0xff;
			m_startAddr = ArtNetMsgUtil.getBigEndInt16(rdmPacket.m_paramData, off);
			off += 2;
			m_numSubDevs = ArtNetMsgUtil.getBigEndInt16(rdmPacket.m_paramData, off);
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
						+ "/" + RdmProductCategories.getCategoryName(m_category)
					+ ",swVers=" + m_softwareVersion
					+ ",addr=" + m_startAddr + "-" + (m_startAddr + m_dmxFootprint - 1)
					+ (m_numSubDevs > 0 ? (",#sub=" + m_numSubDevs) : "")
					+ (m_numSensors > 0 ? (",#sensor=" + m_numSensors) : "")
					+ ")";
		}
	}
	
	/**
	 * Response that consists of an array of parameter ids.
	 */
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
				int paramIdCode = ArtNetMsgUtil.getBigEndInt16(rdmPacket.m_paramData, off);
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
		
		public boolean isSupported(RdmParamId paramId)
		{
			return m_stdPids.contains(paramId);
		}
		
		public boolean isSupported(int code)
		{
			return m_otherPids.contains(code)
					|| m_stdPids.contains(RdmParamId.getParamId(code));
		}
	}
	
	/*
	 * Response that consists of a single ASCII string.
	 */
	public static class StringReply extends RdmParamData
	{
		public final String m_string;
		
		public StringReply(RdmPacket rdmPacket)
		{
			super(rdmPacket.m_paramIdCode, rdmPacket.m_paramData);
			m_string = getStringRemainder(rdmPacket.m_paramData, 0, rdmPacket.m_paramDataLen);
		}
		
		@Override
		public String toString()
		{
			return paramNameOrCode() + "(" + m_string + ")";
		}
	}
	
	/**
	 * Response for a DMX_PERSONALITY_DESCRIPTION request.
	 */
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
				throw new IllegalArgumentException("RDM PersonalityDesc resp: short data "
						+ rdmPacket.m_paramDataLen);				
			}
			int off = 0;
			m_personalityNumber = rdmPacket.m_paramData[off++] & 0xff;
			m_nSlots = ArtNetMsgUtil.getBigEndInt16(rdmPacket.m_paramData, off);
			off += 2;
			m_desc = getStringRemainder(rdmPacket.m_paramData, off, rdmPacket.m_paramDataLen);
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
	
	/**
	 * Response for a SLOT_DESCRIPTION request.
	 */
	public static class SlotDesc extends RdmParamData
	{
		public final int m_number;
		public final String m_desc;
	
		public SlotDesc(RdmPacket rdmPacket)
		{
			super(rdmPacket.m_paramIdCode, rdmPacket.m_paramData);
			if (rdmPacket.m_paramDataLen < 2) {
				throw new IllegalArgumentException("RDM SlotDesc resp: short data "
						+ rdmPacket.m_paramDataLen);				
			}
			int off = 0;
			m_number = ArtNetMsgUtil.getBigEndInt16(rdmPacket.m_paramData, off);
			off += 2;
			m_desc = getStringRemainder(rdmPacket.m_paramData, off, rdmPacket.m_paramDataLen);
		}
	}
	
	/**
	 * Return the rest of the data as a String.
	 * @param data The response data.
	 * @param off The starting offset of the string.
	 * @param len The length of the data.
	 * @return The rest of the data as a String.
	 */
	private static String getStringRemainder(byte[] data, int off, int len)
	{
		StringBuilder buff = new StringBuilder();
		if (data != null) {
			for (; off < len; off++) {
				byte b = data[off];
				if (b == 0) {
					break;
				}
				buff.append((char) b);
			} 
		}
		return buff.toString();
	}
}
