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
 * An Art-Net TOD Control message.
 * Art-Net (TM) Designed by and Copyright Artistic License Holdings Ltd.
 * @author wdr
 */
public class ArtNetTodControl extends ArtNetMsg
{
	public static final int COMMAND_ATC_NONE = 0x00;
	public static final int COMMAND_ATC_FLUSH = 0x01;

	public int m_protoVers = ArtNetConst.PROTO_VERS;
	public int m_net = 0;
	public int m_command = COMMAND_ATC_NONE;
	public int m_subnetUniv = 0;
	
	/**
	 * Create a message with the default field values.
	 */
	public ArtNetTodControl()
	{
		super(ArtNetOpcode.OpTodControl, null);
	}
	
	/**
	 * Create a message from bytes received from a message.
	 * @param buff The message buffer.
	 * @param off The starting offset of the data within buff.
	 * @param length The length of the data.
	 */
	public ArtNetTodControl(byte[] buff, int off, int length, Inet4Address fromAddr)
	{
		super(ArtNetOpcode.OpTodControl, fromAddr);
		if (length < size()) {
			throw new IllegalArgumentException("ArtNetTodControl: short msg " + length);
		}
		ArtNetOpcode opcode = getOpcode(buff, off, length);
		if (opcode != ArtNetOpcode.OpTodControl) {
			throw new IllegalArgumentException("ArtNetTodControl: wrong opcode " + opcode);
		}
		off += ArtNetConst.HDR_OPCODE_LENGTH;
		m_protoVers = getProtoVers(buff, off);
		off += ArtNetConst.PROTO_VERS_LENGTH;
		off += 2;	// filler
		off += 7;	// spare
		m_net = buff[off++] & 0xff;
		m_command = buff[off++] & 0xff;
		m_subnetUniv = buff[off++] & 0xff;
	}
	
	/**
	 * Return the length of an ArtNetTodControl message.
	 * @return The length of an ArtNetTodControl message.
	 */
	public static int size()
	{
		return ArtNetConst.HDR_OPCODE_LENGTH
				+ ArtNetConst.PROTO_VERS_LENGTH		// protoVers
				+ 2			// filler
				+ 7			// spare
				+ 1			// net
				+ 1			// command
				+ 1;			// subnetUniv
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
		zeroBytes(buff, off, 2 + 7);	// filler & spare
		off += 2 + 7;
		buff[off++] = (byte)m_net;
		buff[off++] = (byte)m_command;
		buff[off++] = (byte)m_subnetUniv;
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
		buff.append(linePrefix + "cmd:" + Integer.toHexString(m_command)
					+ " port:" + new ArtNetPort(m_net, m_subnetUniv)
					+ "\n");
		return buff.toString();
	}
	
	@Override
	public String toString()
	{
		StringBuilder b = new StringBuilder(300);
		b.append("ArtNetTodControl{");
		append(b, "protoVers", m_protoVers);
		append(b, "net", m_net);
		appendHex(b, "command", m_command);
		appendHex(b, "subnetUniv", m_subnetUniv);
		b.append('}');
		return b.toString();
	}
	
	public static void main(String[] args) throws IOException
	{
		ArtNetTodControl m = new ArtNetTodControl();
		m.m_command = COMMAND_ATC_FLUSH;
		m.m_net = 1;
		m.m_subnetUniv = (3 << 4) | 4;
		System.out.println("size: " + ArtNetTodControl.size());
		m.print(System.out, "");
		byte[] buff = new byte[ArtNetTodControl.size()];
		m.putData(buff, 0);
		String x = new ByteAOL(buff, 0, buff.length).toHex();
		System.out.println(x);
		ArtNetTodControl m2 = new ArtNetTodControl(buff, 0, ArtNetTodControl.size(), null);
		m2.print(System.out, "");
		m2.fmtPrint(System.out, null);
		
		if (args.length > 0) {
			System.out.println("Sending ...");
			m.sendMsg(args);
			System.out.println("Sent");
		}
	}
}
