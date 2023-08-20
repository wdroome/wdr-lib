package com.wdroome.artnet.msgs;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import com.wdroome.artnet.ArtNetConst;
import com.wdroome.artnet.ArtNetUniv;
import com.wdroome.util.inet.InetUtil;

/**
 * Static utility methods for manipulating bytes in a message.
 * @author wdr
 */
public class ArtNetMsgUtil
{
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
	
	public static int getBigEndInt16(byte[] buff, int off)
	{
		return ((buff[off] & 0xff) << 8) | (buff[off+1] & 0xff);
	}
	
	public static int getBigEndInt32(byte[] buff, int off)
	{
		return    ((buff[off  ] & 0xff) << 24)
				| ((buff[off+1] & 0xff) << 16)
				| ((buff[off+2] & 0xff) <<  8)
				| ((buff[off+3] & 0xff)      );
	}
	
	public static int getLittleEndInt16(byte[] buff, int off)
	{
		return ((buff[off+1] & 0xff) << 8) | (buff[off] & 0xff);
	}
	
	public static int getLittleEndInt32(byte[] buff, int off)
	{
		return    ((buff[off+3] & 0xff) << 24)
				| ((buff[off+2] & 0xff) << 16)
				| ((buff[off+1] & 0xff) <<  8)
				| ((buff[off  ] & 0xff)      );
	}
	
	public static int putBigEndInt16(byte[] buff, int off, int v)
	{
		buff[off++] = (byte)((v >> 8) & 0xff);
		buff[off++] = (byte)((v     ) & 0xff);
		return off;
	}
	
	public static int putBigEndInt32(byte[] buff, int off, int v)
	{
		buff[off++] = (byte)((v >> 24) & 0xff);
		buff[off++] = (byte)((v >> 16) & 0xff);
		buff[off++] = (byte)((v >>  8) & 0xff);
		buff[off++] = (byte)((v      ) & 0xff);
		return off;
	}
	
	public static int putLittleEndInt16(byte[] buff, int off, int v)
	{
		buff[off++] = (byte)((v     ) & 0xff);
		buff[off++] = (byte)((v >> 8) & 0xff);
		return off;
	}
	
	public static Inet4Address getIpAddr(byte[] buff, int off)
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
	
	public static int putIpAddr(byte[] buff, int off, Inet4Address ipaddr)
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
	
	public static void copyBytes(byte[] dest, int destOff, byte[] src, int srcOff, int length)
	{
		for (int i = 0; i < length; i++) {
			dest[destOff+i] = src[srcOff+i];
		}
	}
	
	public static void zeroBytes(byte[] buff, int off, int length)
	{
		for (int i = 0; i < length; i++) {
			buff[off+i] = 0;
		}
	}
	
	public static void putString(byte[] buff, int off, int length, String src)
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
	
	public static int putString(byte[] buff, int off, String src)
	{
		if (src != null) {
			for (byte b: src.getBytes()) {
				buff[off++] = b;
			}
		}
		return off;
	}
	
	public static void append(StringBuilder b, String name, int value)
	{
		b.append(name);
		b.append(':');
		b.append(value);
		b.append(',');
	}
	
	public static void append(StringBuilder b, String name, long value)
	{
		b.append(name);
		b.append(':');
		b.append(value);
		b.append(',');
	}
	
	public static void appendHex(StringBuilder b, String name, int value)
	{
		b.append(name);
		b.append(":x");
		b.append(Integer.toHexString(value));
		b.append(',');
	}
	
	public static void append(StringBuilder b, String name, boolean value)
	{
		b.append(name);
		b.append(':');
		b.append(value ? "T" : "F");
		b.append(',');
	}
	
	public static void append(StringBuilder b, String name, String value)
	{
		b.append(name);
		b.append(":\"");
		if (value != null) {
			b.append(value);
		}
		b.append("\",");
	}
	
	public static void append(StringBuilder b, String name, byte[] buff)
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
	
	public static void append(StringBuilder b, String name, Inet4Address ipAddr)
	{
		if (ipAddr != null) {
			b.append(name);
			b.append(':');
			b.append(ipAddr.getHostAddress());
			b.append(',');
		}
	}
	
	public static void appendHex(StringBuilder b, String name, byte[] data, int dataLen)
	{
		if (data != null && dataLen > 0) {
			b.append(name);
			b.append(':');
			for (int i = 0; i < dataLen; i++) {
				if (i > 0) {
					b.append(',');
				}
				b.append(Integer.toHexString(data[i] & 0xff));
			}
		}
	}
	
	public static void appendUInt(StringBuilder b, String name, byte[] data, int dataLen)
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
	
	public static void append(StringBuilder b, String name, ArtNetUniv port)
	{
		if (port != null) {
			b.append(name);
			b.append(':');
			b.append(port.toString());
			b.append(',');
		}		
	}
}
