package com.wdroome.artnet.msgs;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import com.wdroome.artnet.ArtNetConst;
import com.wdroome.artnet.ArtNetNodeAddr;
import com.wdroome.artnet.ArtNetOpcode;
import com.wdroome.artnet.ArtNetUniv;
import com.wdroome.artnet.ACN_UID;
import com.wdroome.util.ByteAOL;
import com.wdroome.util.inet.InetUtil;

/**
 * An Art-Net RDM message.
 * Art-Net (TM) Designed by and Copyright Artistic License Holdings Ltd.
 * @author wdr
 */
public class ArtNetRdm extends ArtNetMsg
{
	public static final int COMMAND_AT_PROCESS = 0x00;
	
	public int m_protoVers = ArtNetConst.PROTO_VERS;
	public int m_rdmVers = ArtNetTodRequest.RDM_VERS;
	public int m_net = 0;
	public int m_command = COMMAND_AT_PROCESS;
	public int m_subnetUniv = 0;
	public RdmPacket m_rdmPacket = null;

	/**
	 * Create a message with the default field values.
	 */
	public ArtNetRdm()
	{
		super(ArtNetOpcode.OpRdm, null);
	}
	
	/**
	 * Create a message from bytes received from a message.
	 * @param buff The message buffer.
	 * @param off The starting offset of the data within buff.
	 * @param length The length of the data.
	 */
	public ArtNetRdm(byte[] buff, int off, int length, Inet4Address fromAddr)
	{
		super(ArtNetOpcode.OpRdm, fromAddr);
		if (length < size()) {
			throw new IllegalArgumentException("ArtNetRdm: short msg " + length);
		}
		ArtNetOpcode opcode = getOpcode(buff, off, length);
		if (opcode != ArtNetOpcode.OpRdm) {
			throw new IllegalArgumentException("ArtNetRdm: wrong opcode " + opcode);
		}
		off += ArtNetConst.HDR_OPCODE_LENGTH;
		m_protoVers = getProtoVers(buff, off);
		off += ArtNetConst.PROTO_VERS_LENGTH;
		m_rdmVers = buff[off++] & 0xff;
		if (m_rdmVers != ArtNetTodRequest.RDM_VERS) {
			throw new IllegalArgumentException("ArtNetRdm: wrong RDM vers " + m_rdmVers);
		}
		off += 1 + 7;	// filler + spare
		m_net = buff[off++] & 0xff;
		m_command = buff[off++] & 0xff;
		m_subnetUniv = buff[off++] & 0xff;
		m_rdmPacket = new RdmPacket(buff, off, length);
		off = length;
	}
	
	/**
	 * Return the minimum length of an ArtNetRdm message.
	 * @return The minimum length of an ArtNetRdm message.
	 */
	public static int size()
	{
		return ArtNetConst.HDR_OPCODE_LENGTH
				+ ArtNetConst.PROTO_VERS_LENGTH		// protoVers
				+ 1			// rdmVer
				+ 1			// filler
				+ 7			// spare
				+ 1			// net
				+ 1			// command
				+ 1;		// subnetUniv
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
		buff[off++] = (byte)m_rdmVers;
		ArtNetMsgUtil.zeroBytes(buff, off, 1 + 7);
		off += 1 + 7;
		buff[off++] = (byte)m_net;
		buff[off++] = (byte)m_command;
		buff[off++] = (byte)m_subnetUniv;
		if (m_rdmPacket != null) {
			byte[] rdmBytes = m_rdmPacket.getBytes();
			ArtNetMsgUtil.copyBytes(buff, off, rdmBytes, 0, rdmBytes.length);
			off += rdmBytes.length;
		}
		return off;
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
		Inet4Address fromAddr = getFromAddr();
		buff.append(linePrefix
					+ (fromAddr != null ? "from:" + fromAddr.getHostAddress() : "")
					+ " cmd:" + Integer.toHexString(m_command)
					+ " ANPort:" + new ArtNetUniv(m_net, m_subnetUniv));
		if (m_rdmPacket != null) {
			// buff.append("\n" + linePrefix);
			buff.append(m_rdmPacket.toFmtString(null, linePrefix));
		}
		buff.append("\n");
		return buff.toString();
	}
	
	@Override
	public String toString()
	{
		StringBuilder b = new StringBuilder(300);
		b.append("ArtNetRdm{");
		ArtNetMsgUtil.append(b, "from", getFromAddr());
		ArtNetMsgUtil.append(b, "rdmVers", m_rdmVers);
		ArtNetMsgUtil.append(b, "net", m_net);
		ArtNetMsgUtil.appendHex(b, "command", m_command);
		ArtNetMsgUtil.appendHex(b, "subnetUniv", m_subnetUniv);
		if (m_rdmPacket != null) {
			b.append(m_rdmPacket.toString());
		}
		b.append('}');
		return b.toString();
	}
	
	public static void main(String[] args) throws IOException
	{
		ArtNetRdm m = new ArtNetRdm();
		m.m_command = COMMAND_AT_PROCESS;
		m.m_net = 1;
		m.m_subnetUniv = (3 << 4) + 5;
		m.m_rdmPacket = new RdmPacket();
		System.out.println("min size: " + ArtNetRdm.size());
		m.print(System.out, "");
		byte[] buff = new byte[1024];
		int msgLen = m.putData(buff, 0);
		String x = new ByteAOL(buff, 0, msgLen).toHex();
		System.out.println(x);
		ArtNetRdm m2 = new ArtNetRdm(buff, 0, msgLen, null);
		m2.print(System.out, "");
		m2.fmtPrint(System.out, null);
		
		if (args.length > 0) {
			System.out.println("Sending ...");
			m.sendMsg(args);
			System.out.println("Sent");
		}
	}
}
