package com.wdroome.util;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.ByteArrayOutputStream;

/**
 * Produce hex-dumps in the classic IBM style.
 * @author wdr
 */
public class HexDump
{
	private int m_quadsPerLine = 5;
	private String m_addrFormat = "%6d";
	
	private PrintStream m_printStream = null;
	private PrintWriter m_printWriter = null;
	
	/**
	 * Set the number of 4-byte quads per line. Default is 5.
	 * @param quadsPerLine
	 * 		The number of 4-byte quads per line.
	 * 		Ignored if not positive.
	 */
	public void setQuadsPerLine(int quadsPerLine)
	{
		if (quadsPerLine > 0) {
			m_quadsPerLine = quadsPerLine;
		}
	}
	
	/**
	 * Set the address format. Default is "%6d".
	 * @param addrFormat
	 * 		The address format.  If null, do not display the address.
	 */
	public void setAddrFormat(String addrFormat)
	{
		m_addrFormat = addrFormat;
	}
	
	/**
	 * Specify a PrintStream for output.
	 * @param printStream the printStream to set
	 */
	public void setPrintStream(PrintStream printStream)
	{
		m_printStream = printStream;
		m_printWriter = null;
	}

	/**
	 * Specify a PrintWriter for output.
	 * @param printWriter the printWriter to set
	 */
	public void setPrintWriter(PrintWriter printWriter)
	{
		m_printWriter = printWriter;
		m_printStream = null;
	}

	/**
	 * Dump a byte array to the specified output stream or writer.
	 * If none specified, use System.out.
	 * @param array The byte array.
	 * @param offset The starting offset.
	 * @param len The number of bytes to dump.
	 */
	public void dump(byte[] array, int offset, int len)
	{
		if (m_printStream == null && m_printWriter == null) {
			m_printStream = System.out;
		}
		int bytesPerLine = 4*m_quadsPerLine;
		char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7',
							'8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
		StringBuilder hexPart = new StringBuilder();
		StringBuilder charPart = new StringBuilder();
		int longestHexPartLen = 0;
		for (int i = 0; i < len; i++) {
			if (i%bytesPerLine == 0) {
				if (i > 0) {
					int hexPartLen = hexPart.length();
					if (hexPartLen > longestHexPartLen) {
						longestHexPartLen = hexPartLen;
					}
					for (int k = hexPartLen; k < longestHexPartLen; k++) {
						hexPart.append(' ');
					}
					outputLine(hexPart.toString(), charPart.toString());
					hexPart = new StringBuilder();
					charPart = new StringBuilder();
				}
				if (m_addrFormat != null) {
					hexPart.append(String.format(m_addrFormat, i+offset));
				}
			}
			if ((i%bytesPerLine)%4 == 0) {
				hexPart.append(' ');
			}
			byte b = array[i+offset];
			hexPart.append(hexDigits[(b >> 4) & 0xf]);
			hexPart.append(hexDigits[(b     ) & 0xf]);
			charPart.append((b >= 0x20 && b <= 0x7e) ? ((char)b) : '.');
		}
		for (int k = hexPart.length(); k < longestHexPartLen; k++) {
			hexPart.append(' ');
		}
		outputLine(hexPart.toString(), charPart.toString());
	}
	
	/**
	 * Output a line. The base class version writes
	 * to the specified output stream. Child classes may override if needed.
	 * 
	 * @param hexPart The address and hex part of the line.
	 * @param charPart
	 * 		The character equivalent for the hex bytes.
	 * 		charPart does not include a "*" prefix or suffix;
	 * 		this method adds those.
	 */
	protected void outputLine(String hexPart, String charPart)
	{
		String charPrefix = "  *";
		String charSuffix = "*";
		if (m_printStream != null) {
			m_printStream.print(hexPart);
			m_printStream.print(charPrefix);
			m_printStream.print(charPart);
			m_printStream.println(charSuffix);
		} else if (m_printWriter != null) {
			m_printWriter.print(hexPart);
			m_printWriter.print(charPrefix);
			m_printWriter.print(charPart);
			m_printWriter.println(charSuffix);
		}
	}
	
	/**
	 * Dump a byte array to the specified output stream or writer.
	 * If none specified, use System.out.
	 * @param array The byte array.
	 */
	public void dump(byte[] array)
	{
		dump(array, 0, array.length);
	}
	
	/**
	 * Return a String with the hex dump of an array.
	 * @param array The byte array.
	 * @param offset The starting offset.
	 * @param len The number of bytes to dump.
	 * @return A String with the full dump.
	 */
	public static String dumpToString(byte[] array, int offset, int len)
	{
		HexDump hexDump = new HexDump();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		PrintStream printStream = new PrintStream(outputStream);
		hexDump.setPrintStream(printStream);
		hexDump.dump(array, offset, len);
		printStream.flush();
		return outputStream.toString();
	}
	
	/**
	 * Return a String with the hex dump of an array.
	 * @param array The byte array.
	 * @return A String with the full dump.
	 */
	public static String dumpToString(byte[] array)
	{
		return dumpToString(array, 0, array.length);
	}
	
	/**
	 * Dump several strings and print to System.out.
	 * @param args Not used.
	 */
	public static void main(String[] args)
	{
		String s = "1234567890-=qwertyuiop[]asdfghjkl;'zxcvbnm,./";
		String d = dumpToString(s.getBytes());
		System.out.println(d);
		s = "!@#$%^&*()";
		d = dumpToString(s.getBytes());
		System.out.println(d);
	}
}

