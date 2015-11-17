package com.wdroome.util;

import java.io.CharArrayWriter;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Produce strings with hex-dumps in the classic IBM style.
 * Extends {@link HexDump} to put output in a character buffer,
 * which the client can then access as a string.
 * @author wdr
 */
public class HexDumpToString extends HexDump
{
	private final CharArrayWriter m_outputBuff = new CharArrayWriter();
	private final PrintWriter m_writer = new PrintWriter(m_outputBuff);
	
	/**
	 * Create a new string-based HexDump object.
	 */
	public HexDumpToString()
	{
		super();
		super.setOutput(m_writer);
	}
	
	/**
	 * Return the dump output as a string.
	 */
	@Override
	public String toString()
	{
		m_writer.flush();
		return m_outputBuff.toString();
	}

	/**
	 * Clear and reset the character buffer,
	 * discarding any previously generated output.
	 */
	public void reset()
	{
		m_writer.flush();
		m_outputBuff.reset();
	}
	
	/**
	 * Return the current size of the character buffer.
	 * @return The size of the output string, in characters.
	 */
	public int size()
	{
		m_writer.flush();
		return m_outputBuff.size();
	}
	
	/**
	 * This class does not allow client to change the output stream.
	 * @throws UnsupportedOperationException Always.
	 */
	@Override
	public final HexDump setOutput(PrintStream printStream)
	{
		throw new UnsupportedOperationException("HexDumptoString does not allow setOutput()");
	}

	/* (non-Javadoc)
	 * @see HexDump#setQuadsPerLine(int)
	 */
	@Override
	public HexDumpToString setQuadsPerLine(int quadsPerLine)
	{
		super.setQuadsPerLine(quadsPerLine);
		return this;
	}

	/* (non-Javadoc)
	 * @see HexDump#setAddrFormat(java.lang.String)
	 */
	@Override
	public HexDumpToString setAddrFormat(String addrFormat)
	{
		super.setAddrFormat(addrFormat);
		return this;
	}

	/**
	 * This class does not allow client to change the output writer.
	 * @throws UnsupportedOperationException Always.
	 */
	@Override
	public HexDump setOutput(PrintWriter printWriter)
	{
		throw new UnsupportedOperationException("HexDumptoString does not allow setOutput()");
	}

	/**
	 * Dump several strings and print to System.out.
	 * @param args Not used.
	 */
	public static void main(String[] args)
	{
		HexDumpToString hexDump = new HexDumpToString().setAddrFormat("%#6x");
		String s = "1234567890-=qwertyuiop[]asdfghjkl;'zxcvbnm,./";
		hexDump.dump(s.getBytes());
		System.out.println(hexDump.toString());
		s = "!@#$%^&*()";
		hexDump.dump(s.getBytes());
		System.out.println(hexDump.toString());
		hexDump.reset();
		hexDump.dump(s.getBytes());
		System.out.println(hexDump.toString());
	}
}
