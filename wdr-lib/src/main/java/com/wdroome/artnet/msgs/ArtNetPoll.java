package com.wdroome.artnet.msgs;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.wdroome.artnet.ArtNetConst;
import com.wdroome.artnet.ArtNetOpcode;
import com.wdroome.artnet.ArtNetUniv;
import com.wdroome.util.ByteAOL;

/**
 * An Art-Net Poll message.
 * Art-Net (TM) Designed by and Copyright Artistic License Holdings Ltd.
 * @author wdr
 */
public class ArtNetPoll extends ArtNetMsg
{
	public static final int FLAGS_TARGETED_MODE = 0x20;
	public static final int FLAGS_DISABLE_VLC = 0x10;
	public static final int FLAGS_UNICAST_DIAG = 0x08;
	public static final int FLAGS_SEND_DIAG = 0x04;
	public static final int FLAGS_SEND_REPLY_ON_CHANGE = 0x02;
	
	public int m_protoVers = ArtNetConst.PROTO_VERS;
	public int m_talkToMe = 0;	// Called "Flags" in v4df.
	public int m_priority = 0;	// Called "DiagPriority" in v4df.
	public ArtNetUniv m_topTarget = ArtNetUniv.HIGH_UNIV;
	public ArtNetUniv m_bottomTarget = ArtNetUniv.LOW_UNIV;

	/**
	 * Create a message with the default field values.
	 */
	public ArtNetPoll()
	{
		super(ArtNetOpcode.OpPoll, null);
	}
	
	/**
	 * Create a message from bytes received from a message.
	 * @param buff The message buffer.
	 * @param off The starting offset of the data within buff.
	 * @param length The length of the data.
	 */
	public ArtNetPoll(byte[] buff, int off, int length, Inet4Address fromAddr)
	{
		super(ArtNetOpcode.OpPoll, fromAddr);
		if (length < minSize()) {
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
		if (off + 4 <= length) {
			m_topTarget = new ArtNetUniv(buff, off);
			off += 2;
			m_bottomTarget = new ArtNetUniv(buff, off);
			off += 2;
		}
	}
	
	/**
	 * Return the minimum length of an ArtNetPoll message.
	 * @return The length of an ArtNetPoll message.
	 */
	public static int minSize()
	{
		return ArtNetConst.HDR_OPCODE_LENGTH
				+ ArtNetConst.PROTO_VERS_LENGTH		// protoVers
				+ 1			// talkToMe (aka flags)
				+ 1;			// priority
	}
	
	/**
	 * Return the maximum length of an ArtNetPoll message.
	 * @return The maximum length of an ArtNetPoll message.
	 */
	public static int maxSize()
	{
		return minSize() 
				+ 2			// targetPortAddrTop
				+ 2;		// targetPortAddrBot				
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
		off = m_topTarget.put(buff, off);
		off = m_bottomTarget.put(buff, off);
		return off;
	}
	
	@Override
	public String toString()
	{
		StringBuilder b = new StringBuilder(300);
		b.append("ArtNetPoll{");
		ArtNetMsgUtil.append(b, "protoVers", m_protoVers);
		ArtNetMsgUtil.appendHex(b, "flags", m_talkToMe);
		ArtNetMsgUtil.appendHex(b, "priority", m_priority);
		ArtNetMsgUtil.append(b, "target", m_topTarget.toString() + "-" + m_bottomTarget.toString());
		b.append('}');
		return b.toString();
	}
	
	public static void main(String[] args) throws IOException
	{
		ArtNetPoll m = new ArtNetPoll();
		m.m_talkToMe = 0x02;
		m.m_priority = ArtNetConst.DpMed;
		System.out.println("min/maxSize: " + ArtNetPoll.minSize() + "/" + ArtNetPoll.maxSize());
		m.print(System.out, "");
		byte[] buff = new byte[ArtNetPoll.maxSize()];
		m.putData(buff, 0);
		String x = new ByteAOL(buff, 0, buff.length).toHex();
		System.out.println(x);
		ArtNetPoll m2 = new ArtNetPoll(buff, 0, ArtNetPoll.maxSize(), null);
		m2.print(System.out, "");
		
		if (args.length > 0) {
			System.out.println("Sending ...");
			m.sendMsg(args);
			System.out.println("Sent");
		}
	}
}
