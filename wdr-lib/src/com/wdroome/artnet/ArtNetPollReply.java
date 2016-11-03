package com.wdroome.artnet;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.wdroome.util.StringUtils;
import com.wdroome.util.ByteAOL;
import com.wdroome.util.HexDump;

/**
 * An Art-Net Poll Reply message.
 * @author wdr
 */
public class ArtNetPollReply extends ArtNetMsg
{
	public Inet4Address m_ipAddr = null;
	public int m_ipPort = 0;
	public int m_firmwareVers = 0;
	public int m_netAddr = 0;
	public int m_subNetAddr = 0;
	public int m_oem = 0;
	public int m_ubea = 0;
	public int m_status1 = 0;
	public int m_esta = 0;
	public String m_shortName = "";
	public String m_longName = "";
	public String m_nodeReport = "";
	public int m_numPorts = 0;
	public byte[] m_portTypes = new byte[4];
	public byte[] m_goodInput = new byte[4];
	public byte[] m_goodOutput = new byte[4];
	public byte[] m_swIn = new byte[4];
	public byte[] m_swOut = new byte[4];
	public int m_swVideo = 0;
	public int m_swMacro = 0;
	public int m_swRemote = 0;
	public int m_style = 0;
	public byte[] m_macAddr = new byte[6];
	public Inet4Address m_bindIpAddr = null;
	public int m_bindIndex = 0;
	public int m_status2 = 0;

	/**
	 * Create a message with the default field values.
	 */
	public ArtNetPollReply()
	{
		super(ArtNetOpcode.OpPollReply);
	}
	
	/**
	 * Create a message from bytes received from a message.
	 * @param buff The message buffer.
	 * @param off The starting offset of the data within buff.
	 * @param length The length of the data.
	 */
	public ArtNetPollReply(byte[] buff, int off, int length)
	{
		super(ArtNetOpcode.OpPollReply);
		if (length < minSize()) {
			throw new IllegalArgumentException("ArtNetPollReply: short msg " + length);
		}
		ArtNetOpcode opcode = getOpcode(buff, off, length);
		if (opcode != ArtNetOpcode.OpPollReply) {
			throw new IllegalArgumentException("ArtNetPollReply: wrong opcode " + opcode);
		}
		off += ArtNetConst.HDR_OPCODE_LENGTH;
		m_ipAddr = getIpAddr(buff, off);
		off += 4;
		m_ipPort = getLittleEndInt16(buff, off);
		off += 2;
		m_firmwareVers = getBigEndInt16(buff, off);
		off += 2;
		m_netAddr = buff[off++] & 0xff;
		m_subNetAddr = buff[off++] & 0xff;
		m_oem = getBigEndInt16(buff, off);
		off += 2;
		m_ubea = buff[off++] & 0xff;
		m_status1 = buff[off++] & 0xff;
		m_esta = getLittleEndInt16(buff, off);
		off += 2;
		m_shortName = StringUtils.makeString(buff, off, 18);
		off += 18;
		m_longName = StringUtils.makeString(buff, off, 64);
		off += 64;
		m_nodeReport = StringUtils.makeString(buff, off, 64);
		off += 64;
		m_numPorts = getBigEndInt16(buff, off);
		off += 2;
		copyBytes(m_portTypes, 0, buff, off, 4);
		off += 4;
		copyBytes(m_goodInput, 0, buff, off, 4);
		off += 4;
		copyBytes(m_goodOutput, 0, buff, off, 4);
		off += 4;
		copyBytes(m_swIn, 0, buff, off, 4);
		off += 4;
		copyBytes(m_swOut, 0, buff, off, 4);
		off += 4;
		m_swVideo = buff[off++] & 0xff;
		m_swMacro = buff[off++] & 0xff;
		m_swRemote = buff[off++] & 0xff;
		off += 3;	// spare
		m_style = buff[off++] & 0xff;
		copyBytes(m_macAddr, 0, buff, off, 6);
		off += 6;
		if (length >= size()) {
			m_bindIpAddr = getIpAddr(buff, off);
			off += 4;
			m_bindIndex = buff[off++] & 0xff;		
			m_status2 = buff[off++] & 0xff;
			off += 26;	// filler
		}
	}
	
	/**
	 * Return the length of an ArtNetPollReply message.
	 * @return The length of an ArtNetPollReply message.
	 */
	public static int size()
	{
		return minSize()
				+ 4		// bindIpAddr
				+ 1		// bindIndex
				+ 1		// status2
				+ 26	// filler
			;
	}
	
	/**
	 * Return the minimum length of an ArtNetPollReply message.
	 * This is up to the MAC address.
	 * @return The minimum length of an ArtNetPollReply message.
	 */
	public static int minSize()
	{
		return ArtNetConst.HDR_OPCODE_LENGTH 
				+ 4		// ipAddr
				+ 2		// ipPort
				+ 2		// firmwareVers
				+ 1		// netAddr
				+ 1		// subNetAddr
				+ 2		// oem
				+ 1		// ubea
				+ 1		// status1
				+ 2		// esta
				+ 18	// shortName
				+ 64	// longName
				+ 64	// nodeReport
				+ 2		// numPorts
				+ 4		// portTypes
				+ 4		// goodInput
				+ 4		// goodOutput
				+ 4		// swIn
				+ 4		// swOut
				+ 1		// swVideo
				+ 1		// swMacro
				+ 1		// swRemote
				+ 3		// spare
				+ 1		// style
				+ 6		// macAddr
			;
	}
	
	/**
	 * Copy the data for the message into a buffer.
	 * @param buff The buffer.
	 * @param off The offset in buff. Must have space for size() bytes.
	 * @return The length of the message.
	 */
	public int putData(byte[] buff, int off)
	{
		off += putHeader(buff, off);
		putIpAddr(buff, off, m_ipAddr);
		off += 4;
		putLittleEndInt16(buff, off, m_ipPort);
		off += 2;
		putBigEndInt16(buff, off, m_firmwareVers);
		off += 2;
		buff[off++] = (byte)m_netAddr;
		buff[off++] = (byte)m_subNetAddr;
		putBigEndInt16(buff, off, m_oem);
		off += 2;
		buff[off++] = (byte)m_ubea;
		buff[off++] = (byte)m_status1;
		putLittleEndInt16(buff, off, m_esta);
		off += 2;
		putString(buff, off, 18, m_shortName);
		off += 18;
		putString(buff, off, 64, m_longName);
		off += 64;
		putString(buff, off, 64, m_nodeReport);
		off += 64;
		putBigEndInt16(buff, off, m_numPorts);
		off += 2;
		copyBytes(buff, off, m_portTypes, 0, 4);
		off += 4;
		copyBytes(buff, off, m_goodInput, 0, 4);
		off += 4;
		copyBytes(buff, off, m_goodOutput, 0, 4);
		off += 4;
		copyBytes(buff, off, m_swIn, 0, 4);
		off += 4;
		copyBytes(buff, off, m_swOut, 0, 4);
		off += 4;
		buff[off++] = (byte)m_swVideo;
		buff[off++]= (byte)m_swMacro;
		buff[off++]= (byte)m_swRemote;
		zeroBytes(buff, off, 3);	// spare
		off += 3;
		buff[off++] = (byte)m_style;
		copyBytes(buff, off, m_macAddr, 0, 6);
		off += 6;
		putIpAddr(buff, off, m_bindIpAddr);
		off += 4;
		buff[off++] = (byte)m_bindIndex;		
		buff[off++] = (byte)m_status2;
		zeroBytes(buff, off, 26);	// filler
		off += 26;
		return off;
	}
	
	@Override
	public String toString()
	{
		StringBuilder b = new StringBuilder(300);
		b.append("ArtNetPollReply{");
		if (m_ipAddr != null) {
			b.append("ipAddr:");
			b.append(m_ipAddr.getHostAddress());
			b.append(':');
			b.append(m_ipPort);
			b.append(',');
		} else {
			append(b, "ipPort", m_ipPort);
		}
		append(b, "firmwareVers", m_firmwareVers);
		appendHex(b, "netAddr", m_netAddr);
		appendHex(b, "subNetAddr", m_subNetAddr);
		append(b, "oem", m_oem);
		appendHex(b, "status1", m_status1);
		append(b, "esta", m_esta);
		append(b, "shortName", m_shortName);
		append(b, "longName", m_longName);
		append(b, "nodeReport", m_nodeReport);
		append(b, "numPorts", m_numPorts);
		append(b, "portTypes", m_portTypes);
		append(b, "goodInput", m_goodInput);
		append(b, "goodOutput", m_goodOutput);
		append(b, "swIn", m_swIn);
		append(b, "swOut", m_swOut);
		appendHex(b, "swVideo", m_swVideo);
		appendHex(b, "swMacro", m_swMacro);
		appendHex(b, "swRemote", m_swRemote);
		appendHex(b, "style", m_style);
		append(b, "macAddr", m_macAddr);
		if (m_bindIpAddr != null) {
			b.append("bindIpAddr:");
			b.append(m_bindIpAddr.getHostAddress());
			b.append(',');
		}
		append(b, "bindIndex", m_bindIndex);
		appendHex(b, "status2", m_status2);
		b.append('}');
		return b.toString();
	}
}
