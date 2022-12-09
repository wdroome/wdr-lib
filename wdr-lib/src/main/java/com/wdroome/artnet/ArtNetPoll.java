package com.wdroome.artnet;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.wdroome.util.ByteAOL;

/**
 * An Art-Net Poll message.
 * @author wdr
 */
public class ArtNetPoll extends ArtNetMsg
{	
	public int m_protoVers = ArtNetConst.PROTO_VERS;
	public int m_talkToMe = 0;
	public int m_priority = 0;

	/**
	 * Create a message with the default field values.
	 */
	public ArtNetPoll()
	{
		super(ArtNetOpcode.OpPoll);
	}
	
	/**
	 * Create a message from bytes received from a message.
	 * @param buff The message buffer.
	 * @param off The starting offset of the data within buff.
	 * @param length The length of the data.
	 */
	public ArtNetPoll(byte[] buff, int off, int length)
	{
		super(ArtNetOpcode.OpPoll);
		if (length < size()) {
			throw new IllegalArgumentException("ArtNetPoll: short msg " + length);
		}
		ArtNetOpcode opcode = getOpcode(buff, off, length);
		if (opcode != ArtNetOpcode.OpPoll) {
			throw new IllegalArgumentException("ArtNetPoll: wrong opcode " + opcode);
		}
		off += ArtNetConst.HDR_OPCODE_LENGTH;
		m_protoVers = getProtoVers(buff, off);
		off += ArtNetConst.PROTO_VERS_LENGTH;
		m_talkToMe = buff[off++] & 0xff;
		m_priority = buff[off++] & 0xff;
	}
	
	/**
	 * Return the length of an ArtNetPoll message.
	 * @return The length of an ArtNetPoll message.
	 */
	public static int size()
	{
		return ArtNetConst.HDR_OPCODE_LENGTH
				+ ArtNetConst.PROTO_VERS_LENGTH		// protoVers
				+ 1			// talkToMe
				+ 1;		// priority
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
		buff[off++] = (byte)m_talkToMe;
		buff[off++] = (byte)m_priority;
		return off;
	}
	
	@Override
	public String toString()
	{
		StringBuilder b = new StringBuilder(300);
		b.append("ArtNetPoll{");
		append(b, "protoVers", m_protoVers);
		appendHex(b, "talkToMe", m_talkToMe);
		appendHex(b, "priority", m_priority);
		b.append('}');
		return b.toString();
	}
	
	public static void main(String[] args) throws IOException
	{
		ArtNetPoll m = new ArtNetPoll();
		m.m_talkToMe = 0x02;
		m.m_priority = ArtNetConst.DpMed;
		System.out.println("size: " + ArtNetPoll.size());
		m.print(System.out, "");
		byte[] buff = new byte[ArtNetPoll.size()];
		m.putData(buff, 0);
		String x = new ByteAOL(buff, 0, buff.length).toHex();
		System.out.println(x);
		ArtNetPoll m2 = new ArtNetPoll(buff, 0, ArtNetPollReply.size());
		m2.print(System.out, "");
		
		if (args.length > 0) {
			System.out.println("Sending ...");
			m.sendMsg(args);
			System.out.println("Sent");
		}
	}
}
