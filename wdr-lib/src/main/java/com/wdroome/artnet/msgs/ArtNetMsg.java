package com.wdroome.artnet.msgs;

import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import com.wdroome.util.inet.InetUtil;
import com.wdroome.artnet.ArtNetOpcode;
import com.wdroome.artnet.ArtNetConst;
import com.wdroome.artnet.ArtNetPort;

/**
 * Base class for Art-Net messages.
 * Art-Net (TM) Designed by and Copyright Artistic Licence Holdings Ltd.
 * @author wdr
 */
public abstract class ArtNetMsg
{
	/**
	 * Decode an incoming message and return the appropriate message subtype.
	 * @param buff The message buffer.
	 * @param off The offset of the message within buff.
	 * @param length The length of the message.
	 * @param fromAddr The sender's IP address. May be null.
	 * @return The message, or null if it is not a valid ArtNet message.
	 */
	@FunctionalInterface
	public interface MsgMaker
	{
		public ArtNetMsg make(byte[] buff, int off, int length, Inet4Address fromAddr);
	}

	
	/** The Art-Net op code. Required. */
	public final ArtNetOpcode m_opcode;
	
	/** The address of the device that sent this message. May be null. */
	private final Inet4Address m_fromAddr;

	/**
	 * Create a new message.
	 * @param opcode The opcode for the message.
	 * @param fromAddr For incoming messages, the address of the sender. Null for outgoing messages.
	 */
	public ArtNetMsg(ArtNetOpcode opcode, Inet4Address fromAddr)
	{
		m_opcode = opcode;
		m_fromAddr = fromAddr;
	}
	
	/**
	 * Get the sender's address.
	 * @return The sender's address, or null for locally created messages.
	 */
	public Inet4Address getFromAddr() { return m_fromAddr; }
	
	/**
	 * Copy the data for the message into a buffer.
	 * @param buff The buffer.
	 * @param off The offset in buff. Must have space for at least size() bytes.
	 * @return The length of the message.
	 */
	public abstract int putData(byte[] buff, int off);
	
	/**
	 * Print all fields in the message, one per line.
	 * @param out The output stream.
	 * @param linePrefix A prefix for each line.
	 * @see #fmtPrint(PrintStream, String)
	 */
	public void print(PrintStream out, String linePrefix)
	{
		if (linePrefix == null) {
			linePrefix = "";
		}
		out.println(linePrefix
				+ toString().replace(",}", "}").replace(",", "\n" + linePrefix + "  "));
	}
	
	/**
	 * Pretty-print the most import fields in the message.
	 * @param out The output stream.
	 * @param linePrefix A prefix for each line.
	 * @see #toFmtString(StringBuilder, String)
	 */
	public void fmtPrint(PrintStream out, String linePrefix)
	{
		out.println(toFmtString(null, linePrefix));
	}
	
	/**
	 * Pretty-print the most import fields in the message.
	 * The base class prints all fields, one per line.
	 * Child classes can override to select the important fields.
	 * @param linePrefix A prefix for each line. If null, assume "".
	 * @param buff Append the formatted string to this buffer.
	 * 			If null, create a new buffer.
	 * @return The formatted string. Specifically, buff.toString().
	 */
	public String toFmtString(StringBuilder buff, String linePrefix)
	{
		if (linePrefix == null) {
			linePrefix = "";
		}
		if (buff == null) {
			buff = new StringBuilder();
		}
		buff.append(linePrefix
				+ toString().replace(",}", "}").replace(",", "\n" + linePrefix + "  "));
		return buff.toString();
	}
	
	/**
	 * If a buffer starts with the ArtNet header string and an opcode,
	 * return the opcode. Otherwise return Invalid.
	 * @param buff The message buffer.
	 * @param off The offset within the buffer.
	 * @param length The length of the message.
	 * @return The opcode, or Invalid.
	 */
	public static ArtNetOpcode getOpcode(byte[] buff, int off, int length)
	{
		if (length < ArtNetConst.HDR_OPCODE_LENGTH) {
			return ArtNetOpcode.Invalid;
		}
		for (int i = 0; i < ArtNetConst.HEADER_STRING.length; off++, i++) {
			if (buff[off] != ArtNetConst.HEADER_STRING[i]) {
				return ArtNetOpcode.Invalid;
			}
		}
		return ArtNetOpcode.fromBytes(buff, off);
	}
	
	/**
	 * Decode an incoming message and return the appropriate message subtype.
	 * @param buff The message buffer.
	 * @param off The offset of the message within buff.
	 * @param length The length of the message.
	 * @param sender The sender's IP address. May be null.
	 * @return The message, or null if it is not a valid or supported ArtNet message.
	 * @see ArtNetOpcode#makeMsg(byte[], int, int, Inet4Address)
	 */
	public static ArtNetMsg make(byte[] buff, int off, int length, InetSocketAddress sender)
	{
		try {
			ArtNetOpcode opcode = getOpcode(buff, off, length);
			return opcode.makeMsg(buff, off, length, getInet4Address(sender));
		} catch (Exception e) {
			// Message was too short or other error.
			return null;
		}
	}
	
	private static Inet4Address getInet4Address(InetSocketAddress sockAddr)
	{
		if (sockAddr == null) {
			return null;
		}
		InetAddress addr = sockAddr.getAddress();
		if (addr instanceof Inet4Address) {
			return (Inet4Address)addr;
		} else {
			return null;
		}
	}
	
	/**
	 * Interpret the next PROTO_VERS_LENGTH bytes of a buffer as a protocol number
	 * and return it.
	 * @param buff The buffer.
	 * @param off The offset of the data in buffer.
	 * @return The protocol number.
	 * @throws IllegalArgumentException If the version number is less than PROTO_VERS.
	 */
	public static int getProtoVers(byte[] buff, int off)
	{
		int v = ArtNetMsgUtil.getBigEndInt16(buff, off);
		if (v < ArtNetConst.PROTO_VERS) {
			throw new IllegalArgumentException("ArtNetMsg: bad version " + v);
		}
		return v;
	}

	/**
	 * Copy the header and opcode to a buffer.
	 * @param buff The buffer.
	 * @param off The offset in buff.
	 * @return The offset of the next byte after the opcode.
	 */
	protected int putHeader(byte[] buff, int off)
	{
		for (int i = 0; i < ArtNetConst.HEADER_STRING.length; off++, i++) {
			buff[off] = ArtNetConst.HEADER_STRING[i];
		}
		return m_opcode.putBytes(buff, off);
	}
	
	/**
	 * Copy the header, opcode and protocol version to a buffer.
	 * @param buff The buffer.
	 * @param off The offset in buff.
	 * @param protoVers The protocol version.
	 * @return The offset of the next byte after the opcode.
	 */
	protected int putHeader(byte[] buff, int off, int protoVers)
	{
		off = putHeader(buff, off);
		return ArtNetMsgUtil.putBigEndInt16(buff, off, protoVers);
	}	
	
	/**
	 * Send this message.
	 * @param args The destination IP address and optional port, as strings.
	 * @throws IOException
	 */
	public void sendMsg(String[] args) throws IOException
	{
		if (args.length == 0) {
			System.err.println("Usage: to-ipaddr [to-port]");
		}
		InetAddress toAddr = InetAddress.getByName(args[0]);
		int toPort = ArtNetConst.ARTNET_PORT;
		if (args.length >= 2) {
			toPort = Integer.parseInt(args[1]);
		}
		sendMsg(toAddr, toPort);
	}
	
	/**
	 * Send this message.
	 * @param toAddr The destination address.
	 * @param toPort The destination port.
	 * @throws IOException
	 */
	public void sendMsg(InetAddress toAddr, int toPort) throws IOException
	{
		try (DatagramSocket socket = new DatagramSocket()) {
			byte[] msgBuff = new byte[ArtNetConst.MAX_MSG_LEN];
			int msgLen = putData(msgBuff, 0);
			 
			DatagramPacket request = new DatagramPacket(msgBuff, msgLen, toAddr, toPort);
			socket.send(request);
		} catch (IOException e) {
			System.out.println("ArtNetMsg.send " + "->" + toAddr.getHostAddress() + ":" + toPort
								+ " ERR:" + e);
			throw e;
		}			
	}
}
