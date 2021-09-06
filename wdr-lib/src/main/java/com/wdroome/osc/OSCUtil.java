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
	
	public static String OSC_ARG_FMT_HEADER = ",";
	public static String OSC_STR_ARG_FMT = "s";
	public static String OSC_INT32_ARG_FMT = "i";
	public static String OSC_FLOAT_ARG_FMT = "f";
	public static String OSC_INT64_ARG_FMT = "h";
	public static String OSC_BLOB_ARG_FMT = "b";
	public static String OSC_TIME_TAG_ARG_FMT = "t";
	
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
