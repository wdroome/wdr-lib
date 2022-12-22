package com.wdroome.artnet;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.io.PrintStream;
import java.io.PrintWriter;

import com.wdroome.util.StringUtils;
import com.wdroome.util.ByteAOL;
import com.wdroome.util.HexDump;
import com.wdroome.util.inet.InetUtil;

/**
 * An Art-Net Poll Reply message.
 * Note: In early versions of the protocol, the sub-net number
 * was in the high nibble of swIn and swOut.
 * @author wdr
 */
public class ArtNetPollReply extends ArtNetMsg
{
	public static final int PORT_TYPE_OUTPUT = 0x80;
	public static final int PORT_TYPE_INPUT = 0x40;
	public static final int PORT_TYPE_PROTO_MASK = 0x1f;
	public static final int PORT_TYPE_PROTO_DMX512 = 0x00;
	public static final int PORT_TYPE_PROTO_MIDI = 0x01;
	public static final int PORT_TYPE_PROTO_AVAB = 0x02;
	public static final int PORT_TYPE_PROTO_COLORTRAN_CMX = 0x03;
	public static final int PORT_TYPE_PROTO_ADB_625 = 0x04;
	public static final int PORT_TYPE_PROTO_ARTNET = 0x05;

	public static final int STATUS1_RDM = 0x02;
	
	public static final int STATUS2_RDM_SWITCH = 0x80;		// ArtNet 4
	public static final int STATUS2_STYLE_SWITCH = 0x40;	// ArtNet 4
	public static final int STATUS2_SQUAWKING = 0x20;		// ArtNet 4
	public static final int STATUS2_SADN_SWITCH = 0x10;
	public static final int STATUS2_ARTNET_3OR4 = 0x08;
	public static final int STATUS2_DHCP_CAPABLE = 0x04;
	public static final int STATUS2_SET_BY_DHCP = 0x02;
	public static final int STATUS2_BROWSER_CONFIG = 0x01;
	
			// Art-Net 4
	public static final int STATUS3_FAILSAFE_MASK = 0xc0;
	public static final int STATUS3_FAILSAFE_HOLD = 0x00;
	public static final int STATUS3_FAILSAFE_ZERO = 0x40;
	public static final int STATUS3_FAILSAFE_FULL = 0x80;
	public static final int STATUS3_FAILOVER = 0x20;
	public static final int STATUS3_LLRP = 0x10;
	public static final int STATUS3_IN_OUT_SWITCHING = 0x80;

	public static final int GOOD_INPUT_ACTIVE = 0x80;
	
	public static final int GOOD_OUTPUT_ACTIVE = 0x80;
	public static final int GOOD_OUTPUT_MERGE = 0x08;
	public static final int GOOD_OUTPUT_LTP = 0x02;
	public static final int GOOD_OUTPUT_SACN = 0x01;
	
	public static final int GOOD_OUTPUTB_RDM = 0x80;
	public static final int GOOD_OUTPUTB_CONTINUOUS = 0x40;

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
	public byte[] m_portTypes = new byte[ArtNetConst.MAX_PORTS_PER_NODE];
	public byte[] m_goodInput = new byte[ArtNetConst.MAX_PORTS_PER_NODE];
	public byte[] m_goodOutput = new byte[ArtNetConst.MAX_PORTS_PER_NODE];
	public byte[] m_swIn = new byte[ArtNetConst.MAX_PORTS_PER_NODE];
	public byte[] m_swOut = new byte[ArtNetConst.MAX_PORTS_PER_NODE];
	public int m_acnPriority = 0;	// Art-Net 4; was SwVideo in Art-Net 3
	public int m_swMacro = 0;
	public int m_swRemote = 0;
	public int m_style = 0;
	public byte[] m_macAddr = new byte[6];
	public Inet4Address m_bindIpAddr = getZeroIpAddr();
	public int m_bindIndex = 0;
	public int m_status2 = 0;
		// Added in Art-Net 4
	public byte[] m_goodOutputB = new byte[ArtNetConst.MAX_PORTS_PER_NODE];
	public int m_status3 = 0;
	public ACN_UID m_defRespUID = new ACN_UID();
	
		/** Output and input ArtNet ports for this device's ports. */
	public ArtNetPort[] m_inPorts = new ArtNetPort[4];
	public ArtNetPort[] m_outPorts = new ArtNetPort[4];
	
	/** For incoming messages, the node's unique address. null for outgoing messages. */
	public final ArtNetNodeAddr m_nodeAddr;

	/**
	 * Create a message with the default field values.
	 */
	public ArtNetPollReply()
	{
		super(ArtNetOpcode.OpPollReply, null);
		m_nodeAddr = null;
	}
	
	/**
	 * Create a message from bytes received from a message.
	 * @param buff The message buffer.
	 * @param off The starting offset of the data within buff.
	 * @param length The length of the data.
	 * @param fromAddr The sender's IP address.
	 */
	public ArtNetPollReply(byte[] buff, int off, int length, Inet4Address fromAddr)
	{
		super(ArtNetOpcode.OpPollReply, fromAddr);
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
		m_acnPriority = buff[off++] & 0xff;
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
			copyBytes(m_goodOutputB, 0, buff, off, 4);
			off += 4;
			m_status3 = buff[off++] & 0xff;
			m_defRespUID = new ACN_UID(buff, off);
			off += 15;	// filler
		}
		
		ArtNetPort defPort = new ArtNetPort(0,0,0);
		for (int i = 0; i < 4; i++) {
			if (i < m_numPorts) {
				m_inPorts[i] = new ArtNetPort(m_netAddr, m_subNetAddr, m_swIn[i]);
				m_outPorts[i] = new ArtNetPort(m_netAddr, m_subNetAddr, m_swOut[i]);
			} else {
				m_inPorts[i] = defPort;
				m_outPorts[i] = defPort;
			}
		}
		
		m_nodeAddr = new ArtNetNodeAddr(m_bindIpAddr, m_bindIndex, m_ipAddr, m_ipPort, getFromAddr());
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
				+ 4		// goodOutputB
				+ 1		// status3
				+ 6		// defaultRespUID
				+ 15	// filler
			;
	}
	
	/**
	 * Return the minimum length of an ArtNetPollReply message.
	 * This is up to the MAC address.
	 * Apparently version 2 of the protocol had a shorter message. 
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
		buff[off++] = (byte)m_acnPriority;
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
	
	/**
	 * Return the Art-Net port for an output port of this node.
	 * @param iPort The physical port number, starting with 0.
	 * @return The Art-Net port fields, or null if there is no such output port.
	 */
	public ArtNetPort getOutputPort(int iPort)
	{
		if (iPort >= 0
				&& iPort < ArtNetConst.MAX_PORTS_PER_NODE
				&& iPort <= m_numPorts
				&& (m_portTypes[iPort] & 0x80) == 0x80) {
			if (m_subNetAddr == 0 && (m_swOut[iPort] & 0xf0) != 0) {
				return new ArtNetPort(m_netAddr, m_swOut[iPort]);
			} else {
				return new ArtNetPort(m_netAddr, m_subNetAddr, m_swOut[iPort]);
			}
		} else {
			return null;
		}
	}
	
	/**
	 * Return the Art-Net port for an input port of this node.
	 * @param iPort The physical port number, starting with 0.
	 * @return The Art-Net port fields, or null if there is no such input port.
	 */
	public ArtNetPort getInputPort(int iPort)
	{
		if (iPort >= 0
				&& iPort < ArtNetConst.MAX_PORTS_PER_NODE
				&& iPort <= m_numPorts
				&& (m_portTypes[iPort] & 0x40) == 0x40) {
			if (m_subNetAddr == 0 && (m_swIn[iPort] & 0xf0) != 0) {
				return new ArtNetPort(m_netAddr, m_swIn[iPort]);
			} else {
				return new ArtNetPort(m_netAddr, m_subNetAddr, m_swIn[iPort]);
			}
		} else {
			return null;
		}
	}
	
	/**
	 * A string with all the fields in the message.
	 */
	@Override
	public String toString()
	{
		StringBuilder b = new StringBuilder(300);
		b.append("ArtNetPollReply{");
		append(b, "ipAddr", m_ipAddr);
		append(b, "ipPort", m_ipPort);
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
		appendHex(b, "acnPrty", m_acnPriority);
		appendHex(b, "swMacro", m_swMacro);
		appendHex(b, "swRemote", m_swRemote);
		appendHex(b, "style", m_style);
		append(b, "macAddr", m_macAddr);
		append(b, "bindIpAddr", m_bindIpAddr);
		append(b, "bindIndex", m_bindIndex);
		appendHex(b, "status2", m_status2);
		append(b, "goodOutputB", m_goodOutputB);
		appendHex(b, "status3", m_status3);
		append(b, "defUID", m_defRespUID.toString());
		b.append('}');
		return b.toString();
	}
	
	/**
	 * Return a nicely formatted string with the most important fields in the message.
	 * @param linePrefix A prefix for each line. If null, assume "".
	 * @param buff Append the formatted string to this buffer.
	 * 			If null, create a new buffer.
	 * @return The formatted string. Specifically, buff.toString().
	 */
	@Override
	public String toFmtString(StringBuilder buff, String linePrefix)
	{
		if (linePrefix == null) {
			linePrefix = "";
		}
		if (buff == null) {
			buff = new StringBuilder();
		}
		String indent = "   ";
		buff.append(linePrefix
				+ "names: \"" + m_shortName + "\""
				+ (m_longName.isBlank() ? "" : (" \"" + m_longName + "\""))
				+ " addr: "
					+ (m_nodeAddr != null ? InetUtil.toAddrPort(m_nodeAddr.m_nodeAddr) : "(none)")
				+ "\n");
		buff.append(linePrefix
				+ (((m_status2 & STATUS2_BROWSER_CONFIG) != 0) ? "web-configurable: yes " : "")
				+ "ArtNet-version: " + (((m_status2 & STATUS2_ARTNET_3OR4) != 0) ? "3/4 " : "1/2 ")
				);
		buff.append("addr-config: ");
		switch (m_status2 & (STATUS2_SET_BY_DHCP | STATUS2_DHCP_CAPABLE)) {
		case 0:	buff.append("manual"); break;
		case STATUS2_DHCP_CAPABLE: buff.append("manual/dhcp-capable"); break;
		case STATUS2_SET_BY_DHCP | STATUS2_DHCP_CAPABLE: buff.append("dhcp"); break;
		}
		for (int i = 0; i < m_numPorts; i++) {
			buff.append("\n");
			buff.append(linePrefix + "Port " + i + ":");
			switch (m_portTypes[i] & PORT_TYPE_PROTO_MASK) {
			case PORT_TYPE_PROTO_DMX512: buff.append(" dmx"); break;
			case PORT_TYPE_PROTO_MIDI: buff.append(" midi"); break;
			case PORT_TYPE_PROTO_AVAB: buff.append(" avab"); break;
			case PORT_TYPE_PROTO_COLORTRAN_CMX: buff.append(" colortran"); break;
			case PORT_TYPE_PROTO_ADB_625: buff.append(" adb"); break;
			case PORT_TYPE_PROTO_ARTNET: buff.append(" artnet"); break;
			}
			if ((m_portTypes[i] & PORT_TYPE_OUTPUT) != 0) {
				buff.append("\n");
				buff.append(linePrefix + indent + m_outPorts[i] + ": out");
				if ((m_goodOutput[i] & GOOD_OUTPUT_ACTIVE) != 0) {
					buff.append("/active");
				}
				if ((m_goodOutput[i] & GOOD_OUTPUT_SACN) != 0) {
					buff.append("/sACN");
				}
				if ((m_goodOutput[i] & GOOD_OUTPUT_MERGE) != 0) {
					buff.append("/merge");
				}
				buff.append((m_goodOutput[i] & GOOD_OUTPUT_LTP) != 0 ? "/ltp" : "/htp");
			}
			if ((m_portTypes[i] & PORT_TYPE_INPUT) != 0) {
				buff.append("\n");
				buff.append(linePrefix + indent + m_inPorts[i] + ": in");
				if ((m_goodOutput[i] & GOOD_INPUT_ACTIVE) != 0) {
					buff.append("/active");
				}
			}
		}
		return buff.toString();
	}
	
	public void printCommon(PrintStream out, String linePrefix)
	{
		
	}
}
