package com.wdroome.artnet.msgs;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.wdroome.artnet.ArtNetConst;
import com.wdroome.artnet.ArtNetOpcode;
import com.wdroome.artnet.ArtNetPort;
import com.wdroome.util.ByteAOL;

/**
 * An Art-Net TOD Request message.
 * Art-Net (TM) Designed by and Copyright Artistic License Holdings Ltd.
 * @author wdr
 */
public class ArtNetTodRequest extends ArtNetMsg
{
	public static final int MAX_SUBNET_UNIVS = 32;
	public static final int COMMAND_TOD_FULL = 0x00;
	public static final int COMMAND_TOD_NAK = 0xff;
	public static final int RDM_VERS = 1;
	
	public int m_protoVers = ArtNetConst.PROTO_VERS;
	public int m_net = 0;
	public int m_command = COMMAND_TOD_FULL;
	public int m_numSubnetUnivs = 0;
	public byte[] m_subnetUnivs = new byte[MAX_SUBNET_UNIVS];
	
	/**
	 * Create a message with the default field values.
	 */
	public ArtNetTodRequest()
	{
		super(ArtNetOpcode.OpTodRequest, null);
	}
	
	/**
	 * Create a message from bytes received from a message.
	 * @param buff The message buffer.
	 * @param off The starting offset of the data within buff.
	 * @param length The length of the data.
	 */
	public ArtNetTodRequest(byte[] buff, int off, int length, Inet4Address fromAddr)
	{
		super(ArtNetOpcode.OpTodRequest, fromAddr);
		if (length < size()) {
			throw new IllegalArgumentException("ArtNetTodRequest: short msg " + length);
		}
		ArtNetOpcode opcode = getOpcode(buff, off, length);
		if (opcode != ArtNetOpcode.OpTodRequest) {
			throw new IllegalArgumentException("ArtNetTodRequest: wrong opcode " + opcode);
		}
		off += ArtNetConst.HDR_OPCODE_LENGTH;
		m_protoVers = getProtoVers(buff, off);
		off += ArtNetConst.PROTO_VERS_LENGTH;
		off += 2;	// filler
		off += 7;	// spare
		m_net = buff[off++] & 0xff;
		m_command = buff[off++] & 0xff;
		m_numSubnetUnivs = buff[off++] & 0xff;
		ArtNetMsgUtil.zeroBytes(m_subnetUnivs, 0, MAX_SUBNET_UNIVS);
		ArtNetMsgUtil.copyBytes(m_subnetUnivs, 0, buff, off, m_numSubnetUnivs);
		off += m_numSubnetUnivs;
	}
	
	/**
	 * Return the length of an ArtNetTodRequest message.
	 * @return The length of an ArtNetTodRequest message.
	 */
	public static int size()
	{
		return ArtNetConst.HDR_OPCODE_LENGTH
				+ ArtNetConst.PROTO_VERS_LENGTH		// protoVers
				+ 2			// filler
				+ 7			// spare
				+ 1			// net
				+ 1			// command
				+ 1			// addCount
				+ MAX_SUBNET_UNIVS;	// low bytes of port addresses
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
		ArtNetMsgUtil.zeroBytes(buff, off, 2 + 7);	// filler & spare
		off += 2 + 7;
		buff[off++] = (byte)m_net;
		buff[off++] = (byte)m_command;
		buff[off++] = (byte)m_numSubnetUnivs;
		ArtNetMsgUtil.copyBytes(buff, off, m_subnetUnivs, 0, MAX_SUBNET_UNIVS);
		off += MAX_SUBNET_UNIVS;
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
		String indent = "   ";
		buff.append(linePrefix + "cmd:" + Integer.toHexString(m_command)
					+ " net:" + m_net + " nUniv:" + m_numSubnetUnivs);
		for (int i = 0; i < m_numSubnetUnivs; i++) {
			if (i % 8 == 0) {
				buff.append("\n" + linePrefix + indent);
			}
			buff.append(" ");
			buff.append(new ArtNetPort(m_net, m_subnetUnivs[i]));
		}
		buff.append("\n");
		return buff.toString();
	}
	
	@Override
	public String toString()
	{
		StringBuilder b = new StringBuilder(300);
		b.append("ArtNetTodRequest{");
		ArtNetMsgUtil.append(b, "protoVers", m_protoVers);
		ArtNetMsgUtil.append(b, "net", m_net);
		ArtNetMsgUtil.appendHex(b, "command", m_command);
		ArtNetMsgUtil.append(b, "nSubnetUniv", m_numSubnetUnivs);
		ArtNetMsgUtil.appendHex(b, "subnetUnivs", m_subnetUnivs, m_numSubnetUnivs);
		b.append('}');
		return b.toString();
	}
	
	public static void main(String[] args) throws IOException
	{
		ArtNetTodRequest m = new ArtNetTodRequest();
		m.m_command = COMMAND_TOD_FULL;
		m.m_net = 1;
		m.m_numSubnetUnivs = 2;
		m.m_subnetUnivs[0] = 2;
		m.m_subnetUnivs[1] = (3 << 4) | 4;
		System.out.println("size: " + ArtNetTodRequest.size());
		m.print(System.out, "");
		byte[] buff = new byte[ArtNetTodRequest.size()];
		m.putData(buff, 0);
		String x = new ByteAOL(buff, 0, buff.length).toHex();
		System.out.println(x);
		ArtNetTodRequest m2 = new ArtNetTodRequest(buff, 0, ArtNetTodRequest.size(), null);
		m2.print(System.out, "");
		m2.fmtPrint(System.out, null);
		
		if (args.length > 0) {
			System.out.println("Sending ...");
			m.sendMsg(args);
			System.out.println("Sent");
		}
	}
}
