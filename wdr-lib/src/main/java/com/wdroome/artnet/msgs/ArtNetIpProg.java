package com.wdroome.artnet.msgs;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.wdroome.artnet.ArtNetConst;
import com.wdroome.artnet.ArtNetOpcode;

import com.wdroome.util.ByteAOL;
import com.wdroome.util.StringUtils;

/**
 * An Art-Net IP Prog message.
 * Art-Net (TM) Designed by and Copyright Artistic License Holdings Ltd.
 * @author wdr
 */
public class ArtNetIpProg extends ArtNetMsg
{	
	public int m_protoVers = ArtNetConst.PROTO_VERS;
	public int m_command = 0;
	public Inet4Address m_ipAddr = null;
	public Inet4Address m_ipMask = null;
	public int m_ipPort = 0;
	public Inet4Address m_defGateway = null;

	/**
	 * Create a message with the default field values.
	 */
	public ArtNetIpProg()
	{
		super(ArtNetOpcode.OpIpProg, null);
	}
	
	/**
	 * Create a message from bytes received from a message.
	 * @param buff The message buffer.
	 * @param off The starting offset of the data within buff.
	 * @param length The length of the data.
	 * @param fromAddr The sender's IP address.
	 */
	public ArtNetIpProg(byte[] buff, int off, int length, Inet4Address fromAddr)
	{
		super(ArtNetOpcode.OpIpProg, fromAddr);
		if (length < size()) {
			throw new IllegalArgumentException("ArtNetIpProg: short msg " + length);
		}
		ArtNetOpcode opcode = getOpcode(buff, off, length);
		if (opcode != ArtNetOpcode.OpIpProg) {
			throw new IllegalArgumentException("ArtNetIpProg: wrong opcode " + opcode);
		}
		off += ArtNetConst.HDR_OPCODE_LENGTH;
		m_protoVers = getProtoVers(buff, off);
		off += ArtNetConst.PROTO_VERS_LENGTH;
		off += 2;		// filler
		m_command = buff[off++] & 0xff;
		off += 1;		// filler
		m_ipAddr = getIpAddr(buff, off);
		off += 4;
		m_ipMask = getIpAddr(buff, off);
		off += 4;
		m_ipPort = getBigEndInt16(buff, off);
		off += 2;
		m_defGateway = getIpAddr(buff, off);
		off += 4;
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
				+ 4			// defaultGateway
				+ 4;		// spare
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
		buff[off++] = 0;	// filler
		buff[off++] = 0;	// filler
		buff[off++] = (byte)m_command;
		buff[off++] = 0;	// filler
		putIpAddr(buff, off, m_ipAddr);
		off += 4;
		putIpAddr(buff, off, m_ipMask);
		off += 4;
		putBigEndInt16(buff, off, m_ipPort);
		off += 2;
		putIpAddr(buff, off, m_defGateway);
		off += 4;
		zeroBytes(buff, off, 4);
		off += 4;
		return off;
	}
	
	@Override
	public String toString()
	{
		StringBuilder b = new StringBuilder(300);
		b.append("ArtNetIpProg{");
		append(b, "protoVers", m_protoVers);
		appendHex(b, "command", m_command);
		append(b, "ipAddr", m_ipAddr);
		append(b, "ipMask", m_ipMask);
		append(b, "ipPort", m_ipPort);
		b.append('}');
		return b.toString();
	}
}
