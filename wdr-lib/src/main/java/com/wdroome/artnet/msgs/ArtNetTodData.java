package com.wdroome.artnet.msgs;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.wdroome.artnet.ArtNetConst;
import com.wdroome.artnet.ArtNetOpcode;
import com.wdroome.artnet.ArtNetPort;
import com.wdroome.artnet.ACN_UID;
import com.wdroome.util.ByteAOL;

/**
 * An Art-Net TOD Data message.
 * Art-Net (TM) Designed by and Copyright Artistic License Holdings Ltd.
 * @author wdr
 */
public class ArtNetTodData extends ArtNetMsg
{
	public int m_protoVers = ArtNetConst.PROTO_VERS;
	public int m_rdmVers = ArtNetTodRequest.RDM_VERS;
	public int m_port = 0;
	public int m_bindIndex = 0;
	public int m_net = 0;
	public int m_command = 0;
	public int m_subnetUniv = 0;
	public int m_numUidsTotal = 0;
	public int m_blockCount = 0;
	public int m_numUids = 0;
	public ACN_UID[] m_uids = null;
	
	/**
	 * Create a message with the default field values.
	 */
	public ArtNetTodData()
	{
		super(ArtNetOpcode.OpTodData, null);
	}
	
	/**
	 * Create a message from bytes received from a message.
	 * @param buff The message buffer.
	 * @param off The starting offset of the data within buff.
	 * @param length The length of the data.
	 */
	public ArtNetTodData(byte[] buff, int off, int length, Inet4Address fromAddr)
	{
		super(ArtNetOpcode.OpTodData, fromAddr);
		if (length < size()) {
			throw new IllegalArgumentException("ArtNetTodData: short msg " + length);
		}
		ArtNetOpcode opcode = getOpcode(buff, off, length);
		if (opcode != ArtNetOpcode.OpTodData) {
			throw new IllegalArgumentException("ArtNetTodData: wrong opcode " + opcode);
		}
		off += ArtNetConst.HDR_OPCODE_LENGTH;
		m_protoVers = getProtoVers(buff, off);
		off += ArtNetConst.PROTO_VERS_LENGTH;
		m_rdmVers = buff[off++] & 0xff;
		if (m_rdmVers != ArtNetTodRequest.RDM_VERS) {
			throw new IllegalArgumentException("ArtNetTodData: wrong RDM vers " + m_rdmVers);
		}
		m_port = buff[off++] & 0xff;
		off += 6;	// spare
		m_bindIndex = buff[off++] & 0xff;
		m_net = buff[off++] & 0xff;
		m_command = buff[off++] & 0xff;
		m_subnetUniv = buff[off++] & 0xff;
		m_numUidsTotal = getBigEndInt16(buff, off);
		off += 2;
		m_blockCount = buff[off++] & 0xff;
		m_numUids = buff[off++] & 0xff;
		System.out.println("XXX: numUids " + m_numUids);
		if (m_numUids > 0) {
			if (m_numUids * ACN_UID.SACN_UID_LENGTH > length - off) {
				m_numUids = (length - off)/ACN_UID.SACN_UID_LENGTH;
			}
			if (m_uids == null || m_uids.length < m_numUids*ACN_UID.SACN_UID_LENGTH) {
				m_uids = new ACN_UID[m_numUids];
			}
			m_uids = ACN_UID.getUids(buff, off, m_numUids);
			off += m_numUids*ACN_UID.SACN_UID_LENGTH;
		}
	}
	
	/**
	 * Return the minimum length of an ArtNetTodData message.
	 * @return The minimum length of an ArtNetTodData message.
	 */
	public static int size()
	{
		return ArtNetConst.HDR_OPCODE_LENGTH
				+ ArtNetConst.PROTO_VERS_LENGTH		// protoVers
				+ 1			// rdmVer
				+ 1			// port
				+ 6			// spare
				+ 1			// bindIndex				
				+ 1			// net
				+ 1			// command
				+ 1			// subnetUniv
				+ 2			// totalUidCount
				+ 1			// blockCount
				+ 1;		// uidCount
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
		buff[off++] = (byte)m_port;
		zeroBytes(buff, off, 6);
		off += 6;
		buff[off++] = (byte)m_bindIndex;
		buff[off++] = (byte)m_net;
		buff[off++] = (byte)m_command;
		buff[off++] = (byte)m_subnetUniv;
		putBigEndInt16(buff, off, m_numUidsTotal);
		off += 2;
		buff[off++] = (byte)m_blockCount;
		buff[off++] = (byte)m_numUids;
		off = ACN_UID.putUids(buff, off, m_uids, m_numUids);
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
					+ " ANPort:" + new ArtNetPort(m_net, m_subnetUniv)
					+ " bind/port:" + m_bindIndex + "/" + m_port
					+ " nUids:" + m_numUids + "/" + m_numUidsTotal + "[" + m_blockCount + "]");
		for (int i = 0; i < m_numUids; i++) {
			if ((i%4) == 0) {
				buff.append("\n" + linePrefix + indent);
			}
			buff.append(" " + m_uids[i]);
		}
		buff.append("\n");
		return buff.toString();
	}
	
	@Override
	public String toString()
	{
		StringBuilder b = new StringBuilder(300);
		b.append("ArtNetTodData{");
		append(b, "protoVers", m_protoVers);
		append(b, "rdmVers", m_rdmVers);
		append(b, "port", m_port);
		append(b, "bindIndex", m_bindIndex);
		append(b, "net", m_net);
		appendHex(b, "command", m_command);
		appendHex(b, "subnetUniv", m_subnetUniv);
		append(b, "numUidsTotal", m_numUidsTotal);
		append(b, "blockCount", m_blockCount);
		append(b, "numUids", m_numUids);
		if (m_uids != null) {
			b.append("uids:");
			for (ACN_UID uid: m_uids) {
				b.append(" " + uid);
			}
		}
		b.append('}');
		return b.toString();
	}
	
	public static void main(String[] args) throws IOException
	{
		ArtNetTodData m = new ArtNetTodData();
		m.m_command = ArtNetTodRequest.COMMAND_TOD_NAK;
		m.m_net = 1;
		m.m_port = 4;
		m.m_bindIndex = 2;
		m.m_subnetUniv = (3 << 4) + 5;
		m.m_numUidsTotal = 3;
		m.m_blockCount = 0;
		m.m_numUids = 3;
		m.m_uids = new ACN_UID[] {ACN_UID.makeTestUid(16), ACN_UID.makeTestUid(32),
				ACN_UID.makeTestUid(48)};
		System.out.println("size: " + ArtNetTodData.size());
		m.print(System.out, "");
		byte[] buff = new byte[1024];
		int msgLen = m.putData(buff, 0);
		String x = new ByteAOL(buff, 0, msgLen).toHex();
		System.out.println(x);
		ArtNetTodData m2 = new ArtNetTodData(buff, 0, buff.length, null);
		m2.print(System.out, "");
		m2.fmtPrint(System.out, null);
		
		if (args.length > 0) {
			System.out.println("Sending ...");
			m.sendMsg(args);
			System.out.println("Sent");
		}
	}
}
