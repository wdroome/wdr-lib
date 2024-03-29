package com.wdroome.artnet.msgs;

import java.net.Inet4Address;

import com.wdroome.util.StringUtils;
import com.wdroome.util.ByteAOL;
import com.wdroome.util.HexDump;

import com.wdroome.artnet.ArtNetConst;
import com.wdroome.artnet.ArtNetOpcode;

/**
 * An Art-Net Address message.
 * Art-Net (TM) Designed by and Copyright Artistic License Holdings Ltd.
 * @author wdr
 */
public class ArtNetAddress extends ArtNetMsg
{
	public int m_protoVers = ArtNetConst.PROTO_VERS;
	public int m_netAddr = 0;
	public int m_subNetAddr = 0;
	public int m_bindIndex = 0;
	public String m_shortName = "";
	public String m_longName = "";
	public byte[] m_swIn = new byte[4];
	public byte[] m_swOut = new byte[4];
	public int m_acnPriority = 0;		// ArtNet 4. Was swVideo before.
	public int m_command = 0;

	/**
	 * Create a message with the default field values.
	 */
	public ArtNetAddress()
	{
		super(ArtNetOpcode.OpAddress, null);
	}
	
	/**
	 * Create a message from bytes received from a message.
	 * @param buff The message buffer.
	 * @param off The starting offset of the data within buff.
	 * @param length The length of the data.
	 * @param fromAddr The sender's IP address.
	 */
	public ArtNetAddress(byte[] buff, int off, int length, Inet4Address fromAddr)
	{
		super(ArtNetOpcode.OpAddress, fromAddr);
		if (length < size()) {
			throw new IllegalArgumentException("ArtNetAddress: short msg " + length);
		}
		ArtNetOpcode opcode = getOpcode(buff, off, length);
		if (opcode != ArtNetOpcode.OpAddress) {
			throw new IllegalArgumentException("ArtNetAddress: wrong opcode " + opcode);
		}
		off += ArtNetConst.HDR_OPCODE_LENGTH;
		m_protoVers = getProtoVers(buff, off);
		off += ArtNetConst.PROTO_VERS_LENGTH;
		m_netAddr = buff[off++] & 0xff;
		m_bindIndex = buff[off++] & 0xff;
		m_shortName = StringUtils.makeString(buff, off, 18);
		off += 18;
		m_longName = StringUtils.makeString(buff, off, 64);
		off += 64;
		ArtNetMsgUtil.copyBytes(m_swIn, 0, buff, off, 4);
		off += 4;
		ArtNetMsgUtil.copyBytes(m_swOut, 0, buff, off, 4);
		off += 4;
		m_subNetAddr = buff[off++] & 0xff;
		m_acnPriority = buff[off++] & 0xff;
		m_command = buff[off++] & 0xff;
	}
	
	/**
	 * Return the length of an ArtNetAddress message.
	 * @return The length of an ArtNetAddress message.
	 */
	public static int size()
	{
		return ArtNetConst.HDR_OPCODE_LENGTH 
				+ ArtNetConst.PROTO_VERS_LENGTH		// protoVers
				+ 1		// netAddr
				+ 1		// bindIndex
				+ 18	// shortName
				+ 64	// longName
				+ 4		// swIn
				+ 4		// swOut
				+ 1		// subNetAddr
				+ 1		// acnPriority
				+ 1;	// command
	}
	
	/**
	 * Copy the data for the message into a buffer.
	 * @param buff The buffer.
	 * @param off The offset in buff. Must have space for size() bytes.
	 * @return The length of the message.
	 */
	public int putData(byte[] buff, int off)
	{
		off += putHeader(buff, off, m_protoVers);
		buff[off++] = (byte)m_netAddr;
		buff[off++] = (byte)m_bindIndex;
		ArtNetMsgUtil.putString(buff, off, 18, m_shortName);
		off += 18;
		ArtNetMsgUtil.putString(buff, off, 64, m_longName);
		off += 64;
		ArtNetMsgUtil.copyBytes(buff, off, m_swIn, 0, 4);
		off += 4;
		ArtNetMsgUtil.copyBytes(buff, off, m_swOut, 0, 4);
		off += 4;
		buff[off++] = (byte)m_subNetAddr;
		buff[off++] = (byte)m_acnPriority;
		buff[off++]= (byte)m_command;
		return off;
	}
	
	@Override
	public String toString()
	{
		StringBuilder b = new StringBuilder(300);
		b.append("ArtNetAddress{");
		ArtNetMsgUtil.appendHex(b, "command", m_command);
		ArtNetMsgUtil.append(b, "protoVers", m_protoVers);
		ArtNetMsgUtil.appendHex(b, "netAddr", m_netAddr);
		ArtNetMsgUtil.appendHex(b, "subNetAddr", m_subNetAddr);
		ArtNetMsgUtil.append(b, "shortName", m_shortName);
		ArtNetMsgUtil.append(b, "longName", m_longName);
		ArtNetMsgUtil.append(b, "swIn", m_swIn);
		ArtNetMsgUtil.append(b, "swOut", m_swOut);
		ArtNetMsgUtil.appendHex(b, "acnPriority", m_acnPriority);
		ArtNetMsgUtil.append(b, "bindIndex", m_bindIndex);
		b.append('}');
		return b.toString();
	}
}
