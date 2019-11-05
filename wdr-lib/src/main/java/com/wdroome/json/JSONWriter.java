package com.wdroome.json;

import java.io.IOException;
import java.io.Writer;
import java.io.PrintWriter;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Write JSON output.
 * Optionally indent and pretty-print it.
 * The c'tor specifies the output source.
 * Note that this class does not buffer the output or close the file;
 * that's the caller's responsibility.
 * @author wdr
 */
public class JSONWriter
{
	private boolean m_sorted = false;
	private boolean m_indented = false;
	private int m_indentLevel = -1;	// Looks weird, but lets us ignore the first indent.
	private String m_incrIndent = "  ";
	private String m_lineIndent = null;
	private boolean m_atStartOfLine = true;
	private boolean m_wroteSpace = false;
	
	private final Writer m_writer;
	private final PrintStream m_ostream;
	private final StringBuilder m_buffer;
	
	/**
	 * Write the JSON output to a Writer.
	 * @param writer The Writer.
	 */
	public JSONWriter(Writer writer)
	{
		m_writer = writer;
		m_ostream = null;
		m_buffer = null;
	}
	
	/**
	 * Write the JSON output to a OutputStream.
	 * @param ostream The OutputStream.
	 */
	public JSONWriter(OutputStream ostream)
	{
		m_writer = null;
		m_ostream = ostream instanceof PrintStream
					? (PrintStream)ostream : new PrintStream(ostream);
		m_buffer = null;
	}
	
	/**
	 * Write the JSON output to a StringBuilder.
	 * @param buffer The StringBuilder.
	 */
	public JSONWriter(StringBuilder buffer)
	{
		m_writer = null;
		m_ostream = null;
		m_buffer = buffer;
	}
	
	/**
	 * Write a string as a unit.
	 * Does not add quotes or break the string apart.
	 * @param value The string to write.
	 * @throws IOException If an I/O error occurs.
	 * @see JSONValue_String#quotedString(String)
	 */
	public void write(String value) throws IOException
	{
		if (value != null) {
			conditionalIndent();
			if (m_writer != null) {
				m_writer.write(value);
			} else if (m_ostream != null) {
				m_ostream.print(value);
			} else if (m_buffer != null) {
				m_buffer.append(value);
			}
		}
	}
	
	/**
	 * Write a string as a unit.
	 * Does not add quotes or break the string apart.
	 * @param value The string to write.
	 * @throws IOException If an I/O error occurs.
	 */
	public void write(StringBuilder value) throws IOException
	{
		conditionalIndent();
		if (m_writer != null) {
			m_writer.write(value.toString());
		} else if (m_ostream != null) {
			m_ostream.print(value);
		} else if (m_buffer != null) {
			m_buffer.append(value);
		}
	}
		
	/**
	 * Write a single JSON punctuation character.
	 * If pretty-printing, add a pre or post blank for selected characters.
	 * @param value The character to write.
	 * @throws IOException If an I/O error occurs.
	 */
	public void write(char value) throws IOException
	{
		boolean preSpace = false;
		boolean postSpace = false;
		if (m_indented) {
			switch (value) {
			case ',': postSpace = true; break;
			case ':': postSpace = true; break;
			case '{': preSpace = true; break;
			case '[': preSpace = true; break;
			}
		}
		if (conditionalIndent() || m_wroteSpace) {
			preSpace = false;
		}
		if (m_writer != null) {
			if (preSpace) {
				m_writer.write(' ');
			}
			m_writer.write(value);
			if (postSpace) {
				m_writer.write(' ');
			}
		} else if (m_ostream != null) {
			if (preSpace) {
				m_ostream.write(' ');
			}
			m_ostream.write(value);
			if (postSpace) {
				m_ostream.write(' ');
			}
		} else if (m_buffer != null) {
			if (preSpace) {
				m_buffer.append(' ');
			}
			m_buffer.append(value);
			if (postSpace) {
				m_buffer.append(' ');
			}
		}
		m_wroteSpace = postSpace || value == ' ';
	}
	
	/**
	 * If we're at the start of a line, write the appropriate indent.
	 * @return True iff we wrote an indent.
	 * @throws IOException If an I/O occurs while writing the indent.
	 */
	private boolean conditionalIndent() throws IOException
	{
		if (!m_atStartOfLine) {
			return false;
		} else {
			m_atStartOfLine = false;
			write(m_lineIndent);
			for (int i = 0; i < m_indentLevel; i++) {
				write(m_incrIndent);
			}
			return true;
		}
	}
	
	/**
	 * Write a new line without changing the indentation level.
	 * Ignored if pretty-printing is not enabled.
	 * @throws IOException If an I/O error occurs.
	 */
	public void writeNewline() throws IOException
	{
		if (m_indented && !m_atStartOfLine) {
			write('\n');
			m_atStartOfLine = true;
		}
	}
	
	/**
	 * Increment or decrement the indentation level for subsequent lines.
	 * Ignored if pretty-printing is not enabled.
	 * @param indentDelta Change in indentation level.
	 */
	public void incrIndent(int indentDelta)
	{
		if (m_indented) {
			m_indentLevel += indentDelta;
			if (m_indentLevel < 0) {
				m_indentLevel = -1;
			}
		}
	}

	/**
	 * @return True if JSONObjects are to be written in key order.
	 */
	public boolean isSorted()
	{
		return m_sorted;
	}

	/**
	 * @param sorted True if JSONObjects are to be written in key order.
	 */
	public void setSorted(boolean sorted)
	{
		this.m_sorted = sorted;
	}

	/**
	 * @return True if the output is to be indented and pretty-printed.
	 */
	public boolean isIndented()
	{
		return m_indented;
	}

	/**
	 * @param indented True if the output is to be indented and pretty-printed.
	 */
	public void setIndented(boolean indented)
	{
		this.m_indented = indented;
	}
	
	/**
	 * Set the indent strings for pretty-printing,
	 * and turn on indenting.
	 * However, if both arguments are null or blank, turn off indenting.
	 * @param incrIndent The incremental indent for each level.
	 * @param lineIndent The leading indent for all lines.
	 */
	public void setIndents(String incrIndent, String lineIndent)
	{
		m_incrIndent = incrIndent;
		m_lineIndent = lineIndent;
		if ((m_incrIndent != null && !m_incrIndent.equals(""))
				|| (m_lineIndent != null && !m_lineIndent.equals(""))) {
			m_indented = true;
		} else {
			m_indented = false;
		}
	}
}
