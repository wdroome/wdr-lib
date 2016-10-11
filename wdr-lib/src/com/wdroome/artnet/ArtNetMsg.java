package com.wdroome.artnet;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Base class for Art-Net messages.
 * @author wdr
 */
public abstract class ArtNetMsg
{	
	private final ArtNetOpcode m_opcode;

	/**
	 * Create a new message.
	 * @param opcode The opcode for the message.
	 */
	public ArtNetMsg(ArtNetOpcode opcode) { m_opcode = opcode; }
	
	/**
	 * Copy the data for the message into a buffer.
	 * @param buff The buffer.
	 * @param off The offset in buff. Must have space for at least size() bytes.
	 * @return The length of the message.
	 */
	public abstract int putData(byte[] buff, int off);

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
	 * @return The message, or null if it is not a valid ArtNet message.
	 */
	public static ArtNetMsg make(byte[] buff, int off, int length)
	{
		try {
			switch (getOpcode(buff, off, length)) {
			case OpPoll:
				return new ArtNetPoll(buff, off, length);
			case OpPollReply:
				return new ArtNetPollReply(buff, off, length);
			case OpDmx:
				return new ArtNetDmx(buff, off, length);
			case OpDiagData:
				return new ArtNetDiagData(buff, off, length);
			case Invalid:
				return null;
			default:
				return null;
			}
		} catch (Exception e) {
			// Message was too short or other error.
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
}
