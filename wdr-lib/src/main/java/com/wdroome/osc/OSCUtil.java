package com.wdroome.osc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Static utility methods for communicating via OSC and SLIP.
 * @author wdr
 */
public class OSCUtil
{
	private static final byte SLIP_END_BYTE = (byte)0xC0;		// 0300
	private static final byte SLIP_ESC_BYTE = (byte)0xDB;		// 0333
	private static final byte SLIP_ESC_END_BYTE = (byte)0xDC;	// 0334
	private static final byte SLIP_ESC_ESC_BYTE = (byte)0xDD;	// 0335
	
	public static final char OSC_ARG_FMT_HEADER_CHAR = ',';
	public static final String OSC_ARG_FMT_HEADER
					= Character.valueOf(OSC_ARG_FMT_HEADER_CHAR).toString();

	public static final char OSC_STR_ARG_FMT_CHAR = 's';
	public static final String OSC_STR_ARG_FMT
					= Character.valueOf(OSC_STR_ARG_FMT_CHAR).toString();

	public static final char OSC_INT32_ARG_FMT_CHAR = 'i';
	public static final String OSC_INT32_ARG_FMT
					= Character.valueOf(OSC_INT32_ARG_FMT_CHAR).toString();

	public static final char OSC_FLOAT_ARG_FMT_CHAR = 'f';
	public static final String OSC_FLOAT_ARG_FMT
					= Character.valueOf(OSC_FLOAT_ARG_FMT_CHAR).toString();

	public static final char OSC_INT64_ARG_FMT_CHAR = 'h';
	public static final String OSC_INT64_ARG_FMT
					= Character.valueOf(OSC_INT64_ARG_FMT_CHAR).toString();

	public static final char OSC_BLOB_ARG_FMT_CHAR = 'b';
	public static final String OSC_BLOB_ARG_FMT
					= Character.valueOf(OSC_BLOB_ARG_FMT_CHAR).toString();

	public static final char OSC_TIME_TAG_ARG_FMT_CHAR = 't';
	public static final String OSC_TIME_TAG_ARG_FMT
					= Character.valueOf(OSC_TIME_TAG_ARG_FMT_CHAR).toString();

	public static final char OSC_TRUE_ARG_FMT_CHAR = 'T';
	public static final String OSC_TRUE_ARG_FMT
					= Character.valueOf(OSC_TRUE_ARG_FMT_CHAR).toString();

	public static final char OSC_FALSE_ARG_FMT_CHAR = 'F';
	public static final String OSC_FALSE_ARG_FMT
					= Character.valueOf(OSC_FALSE_ARG_FMT_CHAR).toString();

	public static final char OSC_IMPULSE_ARG_FMT_CHAR = 'I';
	public static final String OSC_IMPULSE_ARG_FMT
					= Character.valueOf(OSC_IMPULSE_ARG_FMT_CHAR).toString();

	public static final char OSC_NULL_ARG_FMT_CHAR = 'N';
	public static final String OSC_NULL_ARG_FMT
					= Character.valueOf(OSC_NULL_ARG_FMT_CHAR).toString();
	
	private static final byte[] g_noByteArr = new byte[0];

	/**
	 * Return the format spec string (OSC_STR_ARG_FMT, etc) for an OSC argument.
	 * Note this does NOT work for IMPULSE or TIME TAG arguments -- the client must handle those directly.
	 * @param arg The argument.
	 * @return The type string.
	 * @throws IllegalArgumentException If arg isn't an acceptable type.
	 */
	public static String getArgFormatSpec(Object arg)
	{
		if (arg == null) {
			return OSC_NULL_ARG_FMT;
		} else if (arg instanceof String) {
			return OSC_STR_ARG_FMT;
		} else if (arg instanceof Integer) {
			return OSC_INT32_ARG_FMT;
		} else if (arg instanceof Float) {
			return OSC_FLOAT_ARG_FMT;
		} else if (arg instanceof Long) {
			return OSC_INT64_ARG_FMT;
		} else if (arg instanceof byte[]) {
			return OSC_BLOB_ARG_FMT;
		} else if (arg instanceof Boolean) {
			return ((Boolean)arg).booleanValue() ? OSC_TRUE_ARG_FMT : OSC_FALSE_ARG_FMT;
		} else {
			// Shouldn't happen: c'tor should ensure everything is valid.
			throw new IllegalArgumentException(
						"OSCUtil.getArgType(): Unknown argument class "
						+ arg.getClass().getName());
		}
	}
	
	/**
	 * Convert an OSC argument value to an OSC byte array.
	 * @param argFmt The argument format specifier -- OSC_STR_ARG_FMT_CHAR, etc.
	 * @param arg An Object with the argument value.
	 * @return A byte[] representing the argument. May be a 0-length array. 
	 * @throws ClassCastException
	 * 		If the arg type does not match argFmt. Normally the caller verifies
	 * 		that the type is correct.
	 */
	public static byte[] getArgByteArray(char argFmt, Object arg)
	{
		switch (argFmt) {
		case OSC_NULL_ARG_FMT_CHAR:
		case OSC_TRUE_ARG_FMT_CHAR:
		case OSC_FALSE_ARG_FMT_CHAR:
		case OSC_IMPULSE_ARG_FMT_CHAR:
			return g_noByteArr;
			
		case OSC_STR_ARG_FMT_CHAR:
			return toOSCBytes((String)arg);
		case OSC_INT32_ARG_FMT_CHAR:
			return toOSCBytes(((Integer)arg));
		case OSC_FLOAT_ARG_FMT_CHAR:
			return toOSCBytes((Float)arg);
		case OSC_INT64_ARG_FMT_CHAR:
			return toOSCBytes((Long)arg);
		case OSC_BLOB_ARG_FMT_CHAR:
			return (byte[])arg;
		case OSC_TIME_TAG_ARG_FMT_CHAR:
			return toOSCBytes((Long)arg);
		default:
			throw new IllegalArgumentException(
					"OSCUtil.getArgByteArray(): Unknown argument type '" + argFmt + "'");
		}
	}
	
	/**
	 * Convert a java String into a byte array
	 * for an OSC (Open Sound Control) string data item.
	 * Specifically, a 0-terminated byte[] array
	 * whose length zero-padded to a multiple of 4.
	 * @param s The input string.
	 * @return "s" as a zero-terminated, zero-padded,
	 * byte array of length 4x.
	 */
	public static byte[] toOSCBytes(String s)
	{
		byte[] strBytes = s.getBytes();
		int tlen = (strBytes.length + 4) & (~0x3);
		byte[] oscBytes = new byte[tlen];
		for (int i = 0; i < strBytes.length; i++) {
			oscBytes[i] = strBytes[i];
		}
		return oscBytes;
	}
	
	/**
	 * Convert a java integer into a byte array
	 * for an OSC (Open Sound Control) int32 data item.
	 * Specifically, a big-endian byte[4] array.
	 * @param i The input integer.
	 * @return "i" as a byte[4].
	 */
	public static byte[] toOSCBytes(int i)
	{
		byte[] v = new byte[4];
		v[0] = (byte)((i >> 24) & 0xff);
		v[1] = (byte)((i >> 16) & 0xff);
		v[2] = (byte)((i >>  8) & 0xff);
		v[3] = (byte)((i      ) & 0xff);
		return v;
	}
	
	/**
	 * Convert a java long into a byte array
	 * for an OSC (Open Sound Control) int64 or OSC Time Tag data item.
	 * Specifically, a big-endian byte[8] array.
	 * @param i The input integer.
	 * @return "i" as a byte[8].
	 */
	public static byte[] toOSCBytes(long i)
	{
		byte[] v = new byte[8];
		v[0] = (byte)((i >> 56) & 0xff);
		v[1] = (byte)((i >> 48) & 0xff);
		v[2] = (byte)((i >> 40) & 0xff);
		v[3] = (byte)((i >> 32) & 0xff);
		v[4] = (byte)((i >> 24) & 0xff);
		v[5] = (byte)((i >> 16) & 0xff);
		v[6] = (byte)((i >>  8) & 0xff);
		v[7] = (byte)((i      ) & 0xff);
		return v;
	}

	/**
	 * Convert a java float into a byte array
	 * for an OSC (Open Sound Control) float data item.
	 * Specifically, a big-endian byte[4] array.
	 * @param f The input float.
	 * @return "f" as a byte array of length 4x.
	 */
	public static byte[] toOSCBytes(float f)
	{
		return toOSCBytes(Float.floatToIntBits(f));
	}
	
	/**
	 * Parse a string-valued OSC argument.
	 * @param iter
	 * 		A iterator for the bytes in an OSC message.
	 * 		The next byte should be the start of an OSC string.
	 * 		Read the OSC string and return it as a Java String.
	 * 		Leave the iterator pointing to the next byte
	 * 		after the OSC string.
	 * @return
	 * 		A String with the OSC string parameter.
	 */
	public static String getOSCString(Iterator<Byte> iter)
	{
		StringBuilder str = new StringBuilder();
		byte b;
		while (iter.hasNext() && (b = iter.next()) != 0) {
			str.append((char)b);
		}
		int pad = (4 - ((str.length() + 1) & 0x3)) & 0x3;
		while (iter.hasNext() && --pad >= 0) {
			iter.next();
		}
		return str.toString();
	}
	
	/**
	 * Parse an int32-valued OSC argument.
	 * @param iter
	 * 		A iterator for the bytes in an OSC message.
	 * 		The next byte should be the start of int32 argument.
	 * 		Read the raw bytes and return them as a Integer.
	 * 		Leave the iterator pointing to the next byte
	 * 		after the OSC argument.
	 * @return
	 * 		An Integer with the OSC int32 parameter.
	 */
	public static int getOSCInt32(Iterator<Byte> iter)
	{
		int ret = 0;
		for (int n = 0; n < 4; n++) {
			if (iter.hasNext()) {
				ret = ret << 8 | iter.next();
			}
		}
		return ret;
	}
	
	/**
	 * Parse an int64-valued OSC argument.
	 * @param iter
	 * 		A iterator for the bytes in an OSC message.
	 * 		The next byte should be the start of int32 argument.
	 * 		Read the raw bytes and return them as a Integer.
	 * 		Leave the iterator pointing to the next byte
	 * 		after the OSC argument.
	 * @return
	 * 		An Integer with the OSC int32 parameter.
	 */
	public static long getOSCInt64(Iterator<Byte> iter)
	{
		long ret = 0;
		for (int n = 0; n < 8; n++) {
			if (iter.hasNext()) {
				ret = ret << 8 | iter.next();
			}
		}
		return ret;
	}
	
	/**
	 * Parse an float32-valued OSC argument.
	 * @param iter
	 * 		A iterator for the bytes in an OSC message.
	 * 		The next byte should be the start of float32 argument.
	 * 		Read the raw bytes and return them as a Float.
	 * 		Leave the iterator pointing to the next byte
	 * 		after the OSC argument.
	 * @return
	 * 		An Float with the OSC float32 parameter.
	 */
	public static float getOSCFloat32(Iterator<Byte> iter)
	{
		return Float.intBitsToFloat(getOSCInt32(iter));
	}
	
	/**
	 * Parse a blob-valued OSC argument.
	 * @param iter
	 * 		A iterator for the bytes in an OSC message.
	 * 		The next byte should be the start of blob argument.
	 * 		Read the raw bytes and return them as a Float.
	 * 		Leave the iterator pointing to the next byte
	 * 		after the OSC argument.
	 * @return
	 * 		An byte[] with the OSC blob parameter.
	 */
	public static byte[] getOSCBlob(Iterator<Byte> iter)
	{
		int len = getOSCInt32(iter);
		byte[] ret = new byte[len];
		for (int n = 0; n < len; n++) {
			if (iter.hasNext()) {
				ret[n] = iter.next();
			}
		}
		return ret;
	}
	
	/**
	 * Read the next SLIP message from a stream.
	 * Handle escaped bytes. Waits for next message.
	 * Skips empty messages. Returns a 0-length message
	 * only on end-of-file.
	 * @param in The input stream.
	 * @return The bytes in the message. Return empty list on EOF.
	 * @throws IOException If thrown by in.read().
	 */
	public static List<Byte> readSlipMsg(InputStream in) throws IOException
	{
		ArrayList<Byte> msg = new ArrayList<Byte>();
		int c;
		while ((c = in.read()) >= 0) {
			byte b = (byte)c;
			if (b == SLIP_ESC_BYTE) {
				c = in.read();
				if (c < 0) {
					// Protocol error.
					break;
				}
				if (c == SLIP_ESC_END_BYTE) {
					b = SLIP_END_BYTE;
				} else if (c == SLIP_ESC_ESC_BYTE) {
					b = SLIP_ESC_BYTE;
				} else {
					// Protocol error, but what the heck.
					b = (byte)c;
				}
				msg.add(b);
			} else if (b == SLIP_END_BYTE) {
				if (!msg.isEmpty()) {
					break;
				}
			} else {
				msg.add(b);
			}
		}
		return msg;
	}
	
	/**
	 * Write a double-ENDed SLIP message and flush the stream when done.
	 * Add escapes as needed.
	 * @param out The output stream.
	 * @param byteArrays An iterator with the bytes to write.
	 * @throws IOException If write() throws an error.
	 */
	public static void writeSlipMsg(OutputStream out, List<byte[]> byteArrays) throws IOException
	{
		out.write(SLIP_END_BYTE);
		for (byte[] bytes: byteArrays) {
			for (byte b: bytes) {
				if (b == SLIP_END_BYTE) {
					out.write(SLIP_ESC_BYTE);
					out.write(SLIP_ESC_END_BYTE);
				} else if (b == SLIP_ESC_BYTE) {
					out.write(SLIP_ESC_BYTE);
					out.write(SLIP_ESC_ESC_BYTE);
				} else {
					out.write(b);
				}
			}
		}
		out.write(SLIP_END_BYTE);
		out.flush();
	}
}
