package com.wdroome.artnet.msgs;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.wdroome.artnet.ArtNetConst;
import com.wdroome.artnet.ArtNetOpcode;

import com.wdroome.util.ByteAOL;
import com.wdroome.util.StringUtils;

/**
 * An Art-Net IP Prog Reply message.
 * Art-Net (TM) Designed by and Copyright Artistic License Holdings Ltd.
 * @author wdr
 */
public class ArtNetIpProgReply extends ArtNetMsg
{	
	public int m_protoVers = ArtNetConst.PROTO_VERS;
	public Inet4Address m_ipAddr = null;
	public Inet4Address m_ipMask = null;
	public int m_ipPort = 0;
	public int m_status = 0;
	public Inet4Address m_defGateway = null;

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
		m_ipAddr = ArtNetMsgUtil.getIpAddr(buff, off);
		off += 4;
		m_ipMask = ArtNetMsgUtil.getIpAddr(buff, off);
		off += 4;
		m_ipPort = ArtNetMsgUtil.getBigEndInt16(buff, off);
		off += 2;
		m_status = buff[off++] & 0xff;
		off += 1;		// spare
		m_defGateway = ArtNetMsgUtil.getIpAddr(buff, off);
		off += 4;
	}
	
	/**
	 * Return the length of an ArtNetIpProgReply message.
	 * @return The length of an ArtNetIpProgReply message.
	 */
	public static int size()
	{
		return ArtNetConst.HDR_OPCODE_LENGTH
				+ ArtNetConst.PROTO_VERS_LENGTH		// protoVers
				+ 4			// filler
				+ 4			// ipAddr
				+ 4			// ipMask
				+ 2			// port
				+ 1			// status
				+ 1			// spare
				+ 4			// defGateway
				+ 2;		// spare
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
		ArtNetMsgUtil.zeroBytes(buff, off, 4);
		off += 4;
		ArtNetMsgUtil.putIpAddr(buff, off, m_ipAddr);
		off += 4;
		ArtNetMsgUtil.putIpAddr(buff, off, m_ipMask);
		off += 4;
		ArtNetMsgUtil.putBigEndInt16(buff, off, m_ipPort);
		off += 2;
		buff[off++] = (byte)m_status;
		buff[off++] = 0;
		ArtNetMsgUtil.putIpAddr(buff, off, m_defGateway);
		off += 4;
		ArtNetMsgUtil.zeroBytes(buff, off, 2);
		off += 2;
		return off;
	}
	
	@Override
	public String toString()
	{
		StringBuilder b = new StringBuilder(300);
		b.append("OpIpProgReply{");
		ArtNetMsgUtil.append(b, "protoVers", m_protoVers);
		ArtNetMsgUtil.append(b, "ipAddr", m_ipAddr);
		ArtNetMsgUtil.append(b, "ipMask", m_ipMask);
		ArtNetMsgUtil.append(b, "ipPort", m_ipPort);
		ArtNetMsgUtil.appendHex(b, "status", m_status);
		ArtNetMsgUtil.append(b, "defGateway", m_defGateway);
		b.append('}');
		return b.toString();
	}
}
