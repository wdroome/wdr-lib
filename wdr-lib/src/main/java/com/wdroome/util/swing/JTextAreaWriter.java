package com.wdroome.util.swing;
import java.io.IOException;
import java.io.Writer;

import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.JTextArea;

/**
 * A writer which writes lines to a Java Swing JTextArea
 * with a limit on the number of lines.
 * Note that the writers do NOT need to be in the Swing application thread.
 * @author wdr
 */
public class JTextAreaWriter extends Writer
{
	/** The default limit on the number of lines. */
	public static final int DEF_MAX_LINES = 500;
	
	private final JTextArea m_textArea;
	private final int m_maxLines;
	private int m_numLines;
	private StringBuilder m_workingLine = new StringBuilder();

	/**
	 * Create a new writer with the default line limit.
	 * @param textArea The JTextArea to which lines are written.
	 * 		Caller should set attributes as needed.
	 * 		In particular, the TextArea should not be editable.
	 */
	public JTextAreaWriter(JTextArea textArea)
	{
		this(textArea, 0);
	}

	/**
	 * Create a new writer.
	 * @param textArea The TextArea to which lines are written.
	 * 		Caller should set attributes as needed.
	 * 		In particular, the TextArea should not be editable.
	 * @param maxLines The maximum number of lines to display.
	 */
	public JTextAreaWriter(JTextArea textArea, int maxLines)
	{
		m_textArea = textArea;
		m_maxLines = maxLines > 0 ? maxLines : DEF_MAX_LINES;
	}

	/* (non-Javadoc)
	 * @see java.io.Writer#write(char[], int, int)
	 */
	@Override
	public void write(char[] cbuf, int off, int len) throws IOException
	{
		int end = off + len;
		for (int i = off; i < end; ) {
			int iNL = findNL(cbuf, i, end);
			if (iNL < 0) {
				m_workingLine.append(cbuf, i, end-i);
				i = end;
			} else {
				m_workingLine.append(cbuf, i, iNL - i + 1);
				writeWorkingLine();
				i = iNL + 2;
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see java.io.Writer#write(int)
	 */
	@Override
	public void write(int c) throws IOException
	{
		append((char)c);
	}

	/* (non-Javadoc)
	 * @see java.io.Writer#append(char)
	 */
	@Override
	public Writer append(char c) throws IOException
	{
		// System.out.println("XXX write char " + (int)c);
		m_workingLine.append(c);
		if (c == '\n') {
			writeWorkingLine();
		}
		return this;
	}

	/**
	 * Return the index of next new-line in cbuf, or -1 if none.
	 */
	private int findNL(char[] cbuf, int start, int end)
	{
		for (int i = start; i < end; i++) {
			if (cbuf[i] == '\n') {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * Write the working line to the JTextArea, and clear the working line.
	 * The working line should end with a newline.
	 * Remove any preceding carriage return.
	 */
	private void writeWorkingLine()
	{
		int lineLen = m_workingLine.length();
		if (lineLen >= 2 && m_workingLine.charAt(lineLen-2) == '\r') {
			m_workingLine.deleteCharAt(lineLen-2);
		}
		boolean deleteTopLine;
		if (m_numLines < m_maxLines) {
			m_numLines++;
			deleteTopLine = false;
		} else {
			deleteTopLine = true;
		}
		SwingUtilities.invokeLater(new Updater(m_workingLine.toString(), deleteTopLine));
		m_workingLine = new StringBuilder();
	}

	/* (non-Javadoc)
	 * @see java.io.Writer#flush()
	 */
	@Override
	public void flush() throws IOException
	{
	}

	/* (non-Javadoc)
	 * @see java.io.Writer#close()
	 */
	@Override
	public void close() throws IOException
	{
	}
	
	/**
	 * Append a new line and optionally delete the top line.
	 * Called by the Swing application thread.
	 */
	private class Updater implements Runnable
	{
		private final String m_add;
		private final boolean m_deleteTopLine;
	
		/**
		 * @param add The line to append. Never null.
		 * @param deleteTopLine If true, delete the first line.
		 */
		private Updater(String add, boolean deleteTopLine)
		{
			m_add = add;
			m_deleteTopLine = deleteTopLine;
		}
		
		@Override
		public void run()
		{
			if (m_deleteTopLine && m_textArea.getLineCount() > 0) {
				int firstLineEnd;
				try {
					firstLineEnd = m_textArea.getLineEndOffset(0);
					m_textArea.replaceRange(null,  0, firstLineEnd);
				} catch (BadLocationException e) {
					// Shouldn't happen.
				}
			}
			m_textArea.append(m_add);
		}
	}
}
