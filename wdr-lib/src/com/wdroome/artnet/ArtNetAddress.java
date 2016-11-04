package com.wdroome.artnet;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.wdroome.util.StringUtils;
import com.wdroome.util.ByteAOL;
import com.wdroome.util.HexDump;

/**
 * An Art-Net Address message.
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
	public int m_swVideo = 0;
	public int m_command = 0;

	/**
	 * Create a message with the default field values.
	 */
	public ArtNetAddress()
	{
		super(ArtNetOpcode.OpAddress);
	}
	
	/**
	 * Create a message from bytes received from a message.
	 * @param buff The message buffer.
	 * @param off The starting offset of the data within buff.
	 * @param length The length of the data.
	 */
	public ArtNetAddress(byte[] buff, int off, int length)
	{
		super(ArtNetOpcode.OpAddress);
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
		copyBytes(m_swIn, 0, buff, off, 4);
		off += 4;
		copyBytes(m_swOut, 0, buff, off, 4);
		off += 4;
		m_subNetAddr = buff[off++] & 0xff;
		m_swVideo = buff[off++] & 0xff;
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
				+ 1		// swVideo
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
		putString(buff, off, 18, m_shortName);
		off += 18;
		putString(buff, off, 64, m_longName);
		off += 64;
		copyBytes(buff, off, m_swIn, 0, 4);
		off += 4;
		copyBytes(buff, off, m_swOut, 0, 4);
		off += 4;
		buff[off++] = (byte)m_subNetAddr;
		buff[off++] = (byte)m_swVideo;
		buff[off++]= (byte)m_command;
		return off;
	}
	
	@Override
	public String toString()
	{
		StringBuilder b = new StringBuilder(300);
		b.append("ArtNetAddress{");
		appendHex(b, "command", m_command);
		append(b, "protoVers", m_protoVers);
		appendHex(b, "netAddr", m_netAddr);
		appendHex(b, "subNetAddr", m_subNetAddr);
		append(b, "shortName", m_shortName);
		append(b, "longName", m_longName);
		append(b, "swIn", m_swIn);
		append(b, "swOut", m_swOut);
		appendHex(b, "swVideo", m_swVideo);
		append(b, "bindIndex", m_bindIndex);
		b.append('}');
		return b.toString();
	}
}
