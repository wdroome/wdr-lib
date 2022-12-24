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
		int v = getBigEndInt16(buff, off);
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
		return putBigEndInt16(buff, off, protoVers);
	}
	
	/**
	 * Return a String representation of an Art-Net port.
	 * @param net The network number, 0-32,767.
	 * @param subnet The subnet number, 0-15.
	 * @param univ The universe number, 0-15.
	 * @return An Art-Net port number in the form net.subnet.univ.
	 */
	public static String toPortString(int net, int subnet, int univ)
	{
		return net + "." + subnet + "." + univ;
	}
	
	/**
	 * Return a String representation of an Art-Net port.
	 * @param net The network number, 0-32,768.
	 * @param subUniv The subnet and universe numbers.
	 * 		Subnet is bits 0xf0, univ is bits 0x0f.
	 * @return An Art-Net port number in the form net.subnet.univ.
	 */
	public static String toPortString(int net, int subUniv)
	{
		return net + "." + ((subUniv & 0xf0) >> 4) + "." + (subUniv & 0x0f);
	}
	
	/**
	 * Parse an IP address-port string of the form addr:port
	 * and return the corresponding InetSocketAddress.
	 * If the port is omitted, use the default Art-Net port.
	 * @param addrport A string of the form ipaddr[:port].
	 * @return The address as an InetSocketAddress.
	 * @throws UnknownHostException
	 * 		If the ipaddr part is not a valid IP address.
	 * @throws NumberFormatException
	 * 		If the :port part is not a number.
	 * @throws IllegalArgumentException
	 * 		If the :port part (or defPort, if used) is not a legal port number.
	 */
	public static InetSocketAddress makeSocketAddress(String addrport)
			throws UnknownHostException, NumberFormatException, IllegalArgumentException
	{
		return InetUtil.parseAddrPort(addrport, ArtNetConst.ARTNET_PORT);
	}
	
	protected static int getBigEndInt16(byte[] buff, int off)
	{
		return ((buff[off] & 0xff) << 8) | (buff[off+1] & 0xff);
	}
	
	protected static int getLittleEndInt16(byte[] buff, int off)
	{
		return ((buff[off+1] & 0xff) << 8) | (buff[off] & 0xff);
	}
	
	protected static int putBigEndInt16(byte[] buff, int off, int v)
	{
		buff[off++] = (byte)((v >> 8) & 0xff);
		buff[off++] = (byte)((v     ) & 0xff);
		return off;
	}
	
	protected static int putLittleEndInt16(byte[] buff, int off, int v)
	{
		buff[off++] = (byte)((v     ) & 0xff);
		buff[off++] = (byte)((v >> 8) & 0xff);
		return off;
	}
	
	protected static Inet4Address getIpAddr(byte[] buff, int off)
	{
		byte[] addr = new byte[4];
		addr[0] = buff[off];
		addr[1] = buff[off+1];
		addr[2] = buff[off+2];
		addr[3] = buff[off+3];
		try {
			return (Inet4Address)(InetAddress.getByAddress(addr));
		} catch (Exception e) {
			return null;	// Should not happen.
		}
	}
	
	public static Inet4Address getZeroIpAddr()
	{
		byte[] addr = new byte[4];
		try {
			return (Inet4Address)(InetAddress.getByAddress(addr));
		} catch (Exception e) {
			return null;	// Should not happen.
		}		
	}
	
	public static boolean isZeroIpAddr(Inet4Address addr)
	{
		if (addr != null) {
			for (byte b: addr.getAddress()) {
				if (b != 0) {
					return false;
				}
			}
		}
		return true;
	}
	
	protected static int putIpAddr(byte[] buff, int off, Inet4Address ipaddr)
	{
		if (ipaddr == null) {
			buff[off++] = 0;
			buff[off++] = 0;
			buff[off++] = 0;
			buff[off++] = 0;
		} else {
			byte[] addr = ipaddr.getAddress();
			buff[off++] = addr[0];
			buff[off++] = addr[1];
			buff[off++] = addr[2];
			buff[off++] = addr[3];
		}
		return off;
	}
	
	protected static void copyBytes(byte[] dest, int destOff, byte[] src, int srcOff, int length)
	{
		for (int i = 0; i < length; i++) {
			dest[destOff+i] = src[srcOff+i];
		}
	}
	
	protected static void zeroBytes(byte[] buff, int off, int length)
	{
		for (int i = 0; i < length; i++) {
			buff[off+i] = 0;
		}
	}
	
	protected static void putString(byte[] buff, int off, int length, String src)
	{
		int srclen = (src != null) ? src.length() : 0;
		if (srclen > length) {
			srclen = length;
		}
		for (int i = 0; i < srclen; i++) {
			buff[off+i] = (byte)src.charAt(i);
		}
		for (int i = srclen; i < length; i++) {
			buff[off+i] = 0;
		}
	}
	
	protected static void append(StringBuilder b, String name, int value)
	{
		b.append(name);
		b.append(':');
		b.append(value);
		b.append(',');
	}
	
	protected static void appendHex(StringBuilder b, String name, int value)
	{
		b.append(name);
		b.append(":x");
		b.append(Integer.toHexString(value));
		b.append(',');
	}
	
	protected static void append(StringBuilder b, String name, String value)
	{
		b.append(name);
		b.append(":\"");
		if (value != null) {
			b.append(value);
		}
		b.append("\",");
	}
	
	protected static void append(StringBuilder b, String name, byte[] buff)
	{
		b.append(name);
		b.append(':');
		char sep = 'x';
		for (int i = 0; i < buff.length; i++) {
			b.append(sep);
			b.append(Integer.toHexString(buff[i] & 0xff));
			sep = '.';
		}
		b.append(',');
	}
	
	protected static void append(StringBuilder b, String name, Inet4Address ipAddr)
	{
		if (ipAddr != null) {
			b.append(name);
			b.append(':');
			b.append(ipAddr.getHostAddress());
			b.append(',');
		}
	}
	
	protected static void appendHex(StringBuilder b, String name, byte[] data, int dataLen)
	{
		if (data != null && dataLen > 0) {
			b.append(name);
			b.append(':');
			for (int i = 0; i < dataLen; i++) {
				b.append(Integer.toHexString(data[i] & 0xff));
				b.append(',');
			}
			b.append(',');
		}
	}
	
	protected static void appendUInt(StringBuilder b, String name, byte[] data, int dataLen)
	{
		if (data != null && dataLen > 0) {
			b.append(name);
			b.append(':');
			for (int i = 0; i < dataLen; i++) {
				b.append(data[i] & 0xff);
				b.append(',');
			}
			b.append(',');
		}
	}
	
	protected static void append(StringBuilder b, String name, ArtNetPort port)
	{
		if (port != null) {
			b.append(name);
			b.append(':');
			b.append(port.toString());
			b.append(',');
		}		
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
