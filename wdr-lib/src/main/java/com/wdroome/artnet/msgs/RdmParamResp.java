package com.wdroome.artnet.msgs;

import java.util.List;
import java.util.ArrayList;

import com.wdroome.util.ByteAOL;

/**
 * Parameter-specific data for an RDM response message from an RDM device.
 * To cut down on the number of files, the classes are bundled into this one class.
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
	 * Response for a SENSOR_DEFINITION parameter.
	 */
	public static class SensorDef extends RdmParamData
	{
		public final int m_sensorNum;	// Starts with 0
		public final int m_type;
		public final int m_unit;
		public final int m_prefix;
		public final int m_min;
		public final int m_max;
		public final int m_normMin;
		public final int m_normMax;
		public final int m_recordedValueSupport;
		public final String m_desc;
		
		public SensorDef(RdmPacket rdmPacket)
		{
			super(rdmPacket.m_paramIdCode, rdmPacket.m_paramData);
			if (rdmPacket.m_paramDataLen < 13) {
				throw new IllegalArgumentException("RDM SensorDesc resp: short data "
						+ rdmPacket.m_paramDataLen);								
			}
			int off = 0;
			m_sensorNum = rdmPacket.m_paramData[off++] & 0xff;
			m_type = rdmPacket.m_paramData[off++] & 0xff;
			m_unit = rdmPacket.m_paramData[off++] & 0xff;
			m_prefix = rdmPacket.m_paramData[off++] & 0xff;
			m_min = ArtNetMsgUtil.getBigEndInt16(rdmPacket.m_paramData, off);
			off += 2;
			m_max = ArtNetMsgUtil.getBigEndInt16(rdmPacket.m_paramData, off);
			off += 2;
			m_normMin = ArtNetMsgUtil.getBigEndInt16(rdmPacket.m_paramData, off);
			off += 2;
			m_normMax = ArtNetMsgUtil.getBigEndInt16(rdmPacket.m_paramData, off);
			off += 2;
			m_recordedValueSupport = rdmPacket.m_paramData[off++] & 0xff;
			m_desc = getStringRemainder(rdmPacket.m_paramData, off, rdmPacket.m_paramDataLen);
		}
		
		@Override
		public String toString()
		{
			return "SensorDef("
					+ m_sensorNum
					+ ",\"" + m_desc + "\""
					+ ",type=" + m_type
					+ ",unit=" + m_unit + ",prefix=" + m_prefix
					+ ",range=" + m_min + "-" + m_max
					+ ",norm=" + m_normMin + "-" + m_normMax
					+ ")";			
		}
	}
	
	/**
	 * Response for a SENSOR_VALUE parameter.
	 */
	public static class SensorValue extends RdmParamData
	{
		public final int m_sensorNum;
		public final int m_value;
		public final int m_lowValue;
		public final int m_highValue;
		public final int m_recordedValue;
		public final String m_optName;
		
		public SensorValue(RdmPacket rdmPacket, String optName)
		{
			super(rdmPacket.m_paramIdCode, rdmPacket.m_paramData);
			if (rdmPacket.m_paramDataLen < 9) {
				throw new IllegalArgumentException("RDM SensorValue resp: short data "
						+ rdmPacket.m_paramDataLen);								
			}
			int off = 0;
			m_sensorNum = rdmPacket.m_paramData[off++] & 0xff;
			m_value = ArtNetMsgUtil.getBigEndInt16(rdmPacket.m_paramData, off);
			off += 2;
			m_lowValue = ArtNetMsgUtil.getBigEndInt16(rdmPacket.m_paramData, off);
			off += 2;
			m_highValue = ArtNetMsgUtil.getBigEndInt16(rdmPacket.m_paramData, off);
			off += 2;
			m_recordedValue = ArtNetMsgUtil.getBigEndInt16(rdmPacket.m_paramData, off);
			off += 2;
			m_optName = optName != null && !optName.isBlank() ? optName.trim() : null;
		}
		
		@Override
		public String toString()
		{
			return "SensorValue("
					+ (m_optName != null ? m_optName : ("#" + m_sensorNum))
					+ "=" + m_value
					+ ",low/hi/rec=" + m_lowValue + "/" + m_highValue + "/" + m_recordedValue
					+ ")";
			}
	}
	
	/**
	 * Response that consists of a single unsigned big-endian 32-bit int.
	 */
	public static long bigEndInt32(RdmPacket rdmPacket, long def)
	{
		if (rdmPacket.m_paramDataLen >= 4) {
			return ArtNetMsgUtil.getBigEndInt32(rdmPacket.m_paramData, 0) & 0xffffffffL;
		} else {
			return def;
		}
	}
	
	/**
	 * Response that consists of a single unsigned little-endian 32-bit int.
	 */
	public static long littleEndInt32(RdmPacket rdmPacket, long def)
	{
		if (rdmPacket.m_paramDataLen >= 4) {
			return ArtNetMsgUtil.getLittleEndInt32(rdmPacket.m_paramData, 0) & 0xffffffffL;
		} else {
			return def;
		}
	}
	
	/**
	 * Response that consists of a single unsigned 32-bit int which can be big- or little-endian.
	 * If the first two bytes are 0, assume it's big-endian.
	 * Otherwise assume it's little endian.
	 */
	public static long unknownEnd32Int(RdmPacket rdmPacket, long def)
	{
		if (rdmPacket.m_paramDataLen < 4) {
			return def;
		} else if (rdmPacket.m_paramData[0] == 0 && rdmPacket.m_paramData[1] == 0) {
			return ArtNetMsgUtil.getBigEndInt32(rdmPacket.m_paramData, 0) & 0xffffffffL;
		} else {
			return ArtNetMsgUtil.getLittleEndInt32(rdmPacket.m_paramData, 0) & 0xffffffffL;
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
