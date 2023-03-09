package com.wdroome.artnet.msgs;

import java.util.Arrays;

import com.wdroome.artnet.ACN_UID;

import com.wdroome.util.HexDump;
import com.wdroome.util.ByteAOL;

/**
 * An RDM packet in an ArtNetRdm message.
 * Art-Net (TM) Designed by and Copyright Artistic License Holdings Ltd.
 * @author wdr
 */
public class RdmPacket
{
	public static final int RDM_START_CODE = 204;
	public static final int RDM_SUB_START_CODE = 1;
	
	public static final int RESP_ACK = 0x00;
	public static final int RESP_ACK_TIMER = 0x01;
	public static final int RESP_NACK_REASON = 0x02;
	
	public static final int CMD_GET = 0x20;
	public static final int CMD_GET_RESP = 0x21;
	public static final int CMD_SET = 0x30;
	public static final int CMD_SET_RESP = 0x31;
	
	public int m_subStartCode = RDM_SUB_START_CODE;
	private int m_msgLen = 0;
	public ACN_UID m_destUid = null;
	public ACN_UID m_srcUid = null;
	public int m_transNum = 0;
	public int m_portOrRespType = 0;
	public int m_msgCount = 0;
	public int m_subDevice = 0;
	public int m_command = CMD_GET;
	public int m_paramIdCode = 0;
	public int m_paramDataLen = 0;
	public byte[] m_paramData = null;
	private int m_checkSum = 0;
	
	private static final int PACKET_HDR_LEN = 24;	// includes the RDM start code

	/**
	 * Create a packet with the default values.
	 */
	public RdmPacket()
	{
	}
	
	/**
	 * Create a packet to be sent to an ArtNet node.
	 * @param destUid The destination.
	 * @param cmd The command.
	 * @param paramId The parameter ID.
	 * @param paramData The parameter data. May be null.
	 */
	public RdmPacket(ACN_UID destUid, int cmd, int paramId, byte[] paramData)
	{
		m_destUid = destUid;
		m_srcUid = new ACN_UID();
		m_command = cmd;
		m_portOrRespType = 1;	// default "port number"
		m_paramIdCode = paramId;
		m_paramDataLen = paramData != null ? paramData.length : 0;
		m_paramData = paramData;
		setMsgLen();
	}
	
	/**
	 * Create a packet to be sent to an ArtNet node.
	 * @param destUid The destination.
	 * @param cmd The command.
	 * @param paramId The parameter ID.
	 * @param paramData The parameter data. May be null.
	 */
	public RdmPacket(ACN_UID destUid, int cmd, RdmParamId paramId, byte[] paramData)
	{
		this(destUid, cmd, paramId.getCode(), paramData);
	}
	
	/**
	 * Create a packet with a device's reply to an RDM request.
	 * @param rdmReq The request this is replying to.
	 * @param paramData The reply data.
	 */
	public RdmPacket(RdmPacket rdmReq, byte[] paramData)
	{
		m_destUid = rdmReq.m_srcUid;
		m_srcUid = rdmReq.m_destUid;
		m_transNum = rdmReq.m_transNum;
		m_command = rdmReq.m_command == CMD_SET ? CMD_SET_RESP : CMD_GET_RESP;
		m_paramIdCode = rdmReq.m_paramIdCode;
		m_paramDataLen = paramData != null ? paramData.length : 0;
		m_paramData = paramData;
		setMsgLen();
	}
	
	/**
	 * Create an RDM packet from bytes read from a node.
	 * @param buff The bytes sent by the node.
	 * @param off The offset in buff.
	 * @param length The length of buff.
	 * @throws IllegalArgumentException If the incoming bytes are invalid or the checksum is wrong.
	 */
	public RdmPacket(byte[] buff, int off, int length)
	{
		int startOffset = off;
		if (length < PACKET_HDR_LEN) {
			throw new IllegalArgumentException("RdmPacket: short msg " + length);
		}
		m_subStartCode = buff[off++] & 0xff;
		if (m_subStartCode != RDM_SUB_START_CODE) {
			throw new IllegalArgumentException("RdmPacket: wrong RDM substart code " + m_subStartCode);
		}
		m_msgLen = buff[off++] & 0xff;
		if (length <= m_msgLen) {
			throw new IllegalArgumentException("RdmPacket: bad msglen " + m_msgLen + " " + length);
		}
		m_destUid = new ACN_UID(buff, off);
		off += ACN_UID.SACN_UID_LENGTH;
		m_srcUid = new ACN_UID(buff, off);
		off += ACN_UID.SACN_UID_LENGTH;
		m_transNum = buff[off++] & 0xff;
		m_portOrRespType = buff[off++] & 0xff;
		m_msgCount = buff[off++] & 0xff;
		m_subDevice = ArtNetMsgUtil.getBigEndInt16(buff, off);
		off += 2;
		m_command = buff[off++] & 0xff;
		m_paramIdCode = ArtNetMsgUtil.getBigEndInt16(buff, off);
		off += 2;
		m_paramDataLen = buff[off++] & 0xff;
		if (m_paramDataLen >= length - PACKET_HDR_LEN) {
			throw new IllegalArgumentException("RdmPacket: bad data len " + m_paramDataLen + " " + length);			
		}
		m_paramData = new byte[m_paramDataLen];
		ArtNetMsgUtil.copyBytes(m_paramData, 0, buff, off, m_paramDataLen);
		off += m_paramDataLen;
		int msgCheckSum = calcCheckSum(buff, startOffset, off);
		m_checkSum = ArtNetMsgUtil.getBigEndInt16(buff, off);
		if (m_checkSum != msgCheckSum) {
			throw new IllegalArgumentException("RdmPacket: bad checksum " + msgCheckSum + " " + m_checkSum);						
		}
	}
	
	/**
	 * Get the parameter id as an enum member.
	 * @return
	 */
	public RdmParamId getParamId()
	{
		return RdmParamId.getParamId(m_paramIdCode);
	}
	
	/**
	 * Get the parameter name, if known.
	 * @return The parameter name, if known.
	 */
	public String getParamName()
	{
		return RdmParamId.getName(m_paramIdCode);
	}
	
	/**
	 * Set the parameter id.
	 * @param paramId The parameter.
	 */
	public void setParam(RdmParamId paramId)
	{
		m_paramIdCode = paramId != null ? paramId.getCode() : 0;
	}
	
	/**
	 * Test if this packet is a response.
	 * @param reqCmd If CMD_GET or CMD_SET, return true iff this packet
	 * 			is a CMD_GET_RESP or CMD_SET_RESP, respectively.
	 * 			If neither, return true iff this packet is either response type.
	 * @return True iff this packet is a response for a "reqCmd" request.
	 */
	public boolean isReply(int reqCmd)
	{
		switch (reqCmd) {
		case CMD_GET:
			return m_command == CMD_GET_RESP;
		case CMD_SET:
			return m_command == CMD_SET_RESP;
		default:
			return m_command == CMD_GET_RESP || m_command == CMD_SET_RESP;
		}
	}
	
	/**
	 * Test if this packet is a response.
	 * @return True is this packet is a response.
	 */
	public boolean isReply()
	{
		return m_command == CMD_GET_RESP || m_command == CMD_SET_RESP;
	}
	
	/**
	 * Test if this packet is a successful response.
	 * @return true if this packet is a successful response.
	 */
	public boolean isRespAck()
	{
		return isReply() && m_portOrRespType == RESP_ACK;
	}
	
	/**
	 * Calculate the checksum from the bytes of an RDM packet.
	 * @param buff A byte[] with the packet data.
	 * @param start The starting offset of the RDM packet in buff.
	 * @param end The ending offset (exclusive) of the RDM packet.
	 * @return The checksum.
	 */
	protected static int calcCheckSum(byte[] buff, int start, int end)
	{
		int sum = RDM_START_CODE;
		for (int off = start; off < end; off++) {
			sum += buff[off] & 0xff;
		}
		return sum;
	}

	/**
	 * Set the m_msgLen field from the parameter data length.
	 */
	public void setMsgLen()
	{
		m_msgLen = PACKET_HDR_LEN + m_paramDataLen;
	}
	
	/**
	 * Get the total size of this RDM packet, including the checksum.
	 * @return The byte length of this RDM packet.
	 */
	public int getBufferSize()
	{
		setMsgLen();
		return m_msgLen - 1 + 2;	// message length, minus startcode, plus checksum
	}
	
	/**
	 * Return a byte[] with the RDM packet data, including the checksum.
	 * @return A byte[] with the RDM packet.
	 */
	public byte[] getBytes()
	{
		setMsgLen();
		if (m_destUid == null) {
			m_destUid = new ACN_UID();
		}
		if (m_srcUid == null) {
			m_srcUid = new ACN_UID();
		}
		
		byte[] buff = new byte[getBufferSize()];
		int off = 0;
		buff[off++] = (byte)m_subStartCode;
		buff[off++] = (byte)m_msgLen;
		off = m_destUid.putUid(buff, off);
		off = m_srcUid.putUid(buff, off);
		buff[off++] = (byte)m_transNum;
		buff[off++] = (byte)m_portOrRespType;
		buff[off++] = (byte)m_msgCount;
		off = ArtNetMsgUtil.putBigEndInt16(buff, off, m_subDevice);
		buff[off++] = (byte)m_command;
		off = ArtNetMsgUtil.putBigEndInt16(buff, off, m_paramIdCode);
		buff[off++] = (byte)m_paramDataLen;
		ArtNetMsgUtil.copyBytes(buff, off, m_paramData, 0, m_paramDataLen);
		off += m_paramDataLen;
		off = ArtNetMsgUtil.putBigEndInt16(buff, off, calcCheckSum(buff, 0, off));
		return buff;
	}
	
	/**
	 * Return a nicely formatted string with the most important fields in the message.
	 * @param linePrefix A prefix for each line. If null, assume "".
	 * @param buff Append the formatted string to this buffer.
	 * 			If null, create a new buffer.
	 * @return The formatted string. Specifically, buff.toString().
	 */
	public String toFmtString(StringBuilder buff, String linePrefix)
	{
		if (linePrefix == null) {
			linePrefix = "";
		}
		if (buff == null) {
			buff = new StringBuilder();
		}
		if (buff.length() > 10) {
			buff.append("\n" + linePrefix);
		}
		buff.append("RdmPacket(");
		buff.append(m_srcUid + "->" + m_destUid);
		buff.append(",cmd=0x" + Integer.toHexString(m_command));
		buff.append(",PID=" + getParamName());
		buff.append(",trans=" + m_transNum);
		buff.append(",resp=" + m_portOrRespType);
		if (m_msgCount > 0) { buff.append(",#msgs=" + m_msgCount); }
		if (m_paramDataLen > 0) {
			buff.append(",data=" + m_paramDataLen);
			if (m_paramData != null) {
				buff.append("/");
				new ByteAOL(m_paramData).appendHex(buff);
			} else {
				buff.append("/null");
			}
		}
		buff.append(")");
		return buff.toString();
	}
	
	@Override
	public String toString()
	{
		StringBuilder b = new StringBuilder(300);
		b.append("RdmPacket{");
		b.append("subStart=" + m_subStartCode);
		b.append(",msgLen=" + m_msgLen);
		b.append(",uids=" + m_srcUid + "->" + m_destUid);
		b.append(",transNum=" + m_transNum);
		b.append(",port=" + m_portOrRespType);
		b.append(",msgCount=" + m_msgCount);
		b.append(",subDev=" + m_subDevice);
		b.append(",cmd=0x" + Integer.toHexString(m_command));
		b.append(",PID=" + getParamName());
		b.append(",dataLen=" + m_paramDataLen);
		if (m_paramDataLen > 0 && m_paramData != null) {
			b.append(",data=");
			new ByteAOL(m_paramData).appendHex(b);
		}
		return b.toString();
	}
	
	/**
	 * Test.
	 */
	public static void main(String[] args)
	{
		// This is the sample packet from "Light Bytes".
		ACN_UID destUid = new ACN_UID(0x414c, 0x01020304);
		ACN_UID srcUid = new ACN_UID(0x414c, 0);
		RdmParamId paramId = RdmParamId.DEVICE_INFO;
		RdmPacket m = new RdmPacket(destUid, CMD_GET, paramId, null);
		m.m_srcUid = srcUid;
		m.m_transNum = 1;
		m.m_portOrRespType = 1;
		System.out.println(m.toFmtString(null, null));
		
		byte[] buff = m.getBytes();
		System.out.println("Packet length: " + buff.length);
		new HexDump().dump(buff);
		byte[] correctBuff = new byte[] {
				0x01, 0x18,								// subStartCode, msgLen
				0x41, 0x4c, 0x01, 0x02, 0x03, 0x04,		// dest uid
				0x41, 0x4c, 0x00, 0x00, 0x00, 0x00,		// src uid
				0x01, 0x01, 0x00,						// transNum, portOrRespType, msgCount
				0x00, 0x00,								// subDevice
				0x20,									// command (GET)
				0x00, 0x60,								// paramId
				0x00,									// paramDataLen
				0x02, (byte)0x8b,						// checkSum
		};
		if (Arrays.compare(buff, correctBuff) == 0) {
			System.out.println("Correct");
		} else {
			System.out.println("*** INCORRECT ***");
		}
		
		System.out.println("Make packet from those bytes:");
		RdmPacket m2 = new RdmPacket(buff, 0, buff.length);
		byte[] buff2 = m2.getBytes();
		new HexDump().dump(buff2);
		if (Arrays.compare(buff2, correctBuff) == 0) {
			System.out.println("Correct");
		} else {
			System.out.println("*** INCORRECT ***");
		}
		
		System.out.println("Try again with param data:");
		byte[] paramData = new byte[] {1, 2, 3, 4};
		m = new RdmPacket(destUid, CMD_SET, paramId, paramData);
		m.m_srcUid = srcUid;
		m.m_transNum = 1;
		m.m_portOrRespType = 1;
		buff = m.getBytes();
		System.out.println(m.toFmtString(null, null));
		System.out.println("Packet length: " + buff.length);
		new HexDump().dump(buff);
		System.out.println("Make packet from those bytes:");
		m2 = new RdmPacket(buff, 0, buff.length);
		buff2 = m2.getBytes();
		new HexDump().dump(buff2);
	}
}
