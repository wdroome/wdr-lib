package com.wdroome.artnet;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.wdroome.util.ByteAOL;
import com.wdroome.util.StringUtils;

/**
 * An Art-Net IP Prog Reply message.
 * @author wdr
 */
public class ArtNetIpProgReply extends ArtNetMsg
{	
	public int m_protoVers = ArtNetConst.PROTO_VERS;
	public Inet4Address m_ipAddr = null;
	public Inet4Address m_ipMask = null;
	public int m_ipPort = 0;
	public int m_status = 0;

	/**
	 * Create a message with the default field values.
	 */
	public ArtNetIpProgReply()
	{
		super(ArtNetOpcode.OpIpProgReply, null);
	}
	
	/**
	 * Create a message from bytes received from a message.
	 * @param buff The message buffer.
	 * @param off The starting offset of the data within buff.
	 * @param length The length of the data.
	 * @param fromAddr The sender's IP address.
	 */
	public ArtNetIpProgReply(byte[] buff, int off, int length, Inet4Address fromAddr)
	{
		super(ArtNetOpcode.OpIpProgReply, fromAddr);
		if (length < size()) {
			throw new IllegalArgumentException("ArtNetIpProgReply: short msg " + length);
		}
		ArtNetOpcode opcode = getOpcode(buff, off, length);
		if (opcode != ArtNetOpcode.OpIpProgReply) {
			throw new IllegalArgumentException("ArtNetIpProgReply: wrong opcode " + opcode);
		}
		off += ArtNetConst.HDR_OPCODE_LENGTH;
		m_protoVers = getProtoVers(buff, off);
		off += ArtNetConst.PROTO_VERS_LENGTH;
		off += 4;		// filler
		m_ipAddr = getIpAddr(buff, off);
		off += 4;
		m_ipMask = getIpAddr(buff, off);
		off += 4;
		m_ipPort = getBigEndInt16(buff, off);
		off += 2;
		m_status = buff[off++] & 0xff;
	}
	
	/**
	 * Return the length of an ArtNetIpProg message.
	 * @return The length of an ArtNetIpProg message.
	 */
	public static int size()
	{
		return ArtNetConst.HDR_OPCODE_LENGTH
				+ ArtNetConst.PROTO_VERS_LENGTH		// protoVers
				+ 2			// filler
				+ 1			// command
				+ 1			// filler
				+ 4			// ipAddr
				+ 4			// ipMask
				+ 2			// ipPort
				+ 1			// status
				+ 7;		// spare
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
		zeroBytes(buff, off, 4);
		off += 4;
		putIpAddr(buff, off, m_ipAddr);
		off += 4;
		putIpAddr(buff, off, m_ipMask);
		off += 4;
		putBigEndInt16(buff, off, m_ipPort);
		off += 2;
		buff[off++] = (byte)m_status;
		zeroBytes(buff, off, 7);
		off += 7;
		return off;
	}
	
	@Override
	public String toString()
	{
		StringBuilder b = new StringBuilder(300);
		b.append("OpIpProgReply{");
		append(b, "protoVers", m_protoVers);
		append(b, "ipAddr", m_ipAddr);
		append(b, "ipMask", m_ipMask);
		append(b, "ipPort", m_ipPort);
		appendHex(b, "status", m_status);
		b.append('}');
		return b.toString();
	}
}
