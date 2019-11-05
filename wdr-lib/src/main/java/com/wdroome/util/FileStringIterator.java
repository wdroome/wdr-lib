package com.wdroome.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.IOException;
import java.io.Reader;

/**
 * An iterator that breaks a file into tokens,
 * and provides the line number of the current token on request.
 * By default, tokens are separated by white space.
 * The iterator discards comments, which by default are
 * # to end of line, provided that the # is at the beginning of a line
 * or is immediately preceded by white space.
 * @author wdr
 */
public class FileStringIterator implements IteratorWithPosition<String>
{
	private final LineNumberReader m_rdr;
	private final String m_fname;
	private final boolean m_closeAtEOF;
	private String m_splitPattern = "[ \t\n\r]+";
	private String m_commentPattern = "(^|[ \t\r]+)#.*$";
	private String m_nextElement = null;
	private boolean m_atEOF = false;
	private int m_currentLineNumber = -1;
	private String[] m_currentLineTokens = new String[0];
	private int m_iToken = 0;
	
	/**
	 * Create a new iterator by reading a file.
	 * @param fname The name of the file.
	 * @throws IOException If the file cannot be opened for reading.
	 */
	public FileStringIterator(String fname) throws IOException
	{
		this(new FileReader(fname), fname, true);
	}
	
	/**
	 * Create a new iterator by reading an already opened file.
	 * @param reader The open file.
	 * @param fname The file name (used by {@link #getPositionDescription()}).
	 * @param closeAtEOF If true, close the file on EOF.
	 */
	public FileStringIterator(Reader reader, String fname, boolean closeAtEOF)
	{
		m_fname = fname;
		m_rdr = new LineNumberReader(reader);
		m_closeAtEOF = closeAtEOF;
	}
	
	/**
	 * Create a new iterator by reading an already opened file.
	 * The file will be closed when we reach end-of-file.
	 * @param reader The open file.
	 * @param fname The file name (used by {@link #getPositionDescription()}).
	 */
	public FileStringIterator(Reader reader, String fname)
	{
		this(reader, fname, true);
	}
	
	/**
	 * Return this object as a String Iterator,
	 * so you can use ths object as a target of a "foreach" statement.
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<String> iterator()
	{
		return this;
	}

	/**
	 * Return true iff there is another element.
	 * @see java.util.Iterator#hasNext()
	 */
	@Override
	public boolean hasNext()
	{
		return obtainNextElement();
	}

	/**
	 * Return the next element.
	 * @throws NoSuchElementException If there are no more elements.
	 * @see java.util.Iterator#next()
	 */
	@Override
	public String next() throws NoSuchElementException
	{
		if (obtainNextElement()) {
			String r = m_nextElement;
			m_nextElement = null;
			return r;
		} else {
			throw new NoSuchElementException("At end of " + m_fname);
		}
	}

	/**
	 * Ensure that m_nextToken is the next token,
	 * or null if we're at the end.
	 * @return True iff there is a next token.
	 */
	private boolean obtainNextElement()
	{
		if (m_atEOF)
			return false;
		if (m_nextElement != null)
			return true;
		while (m_iToken >= m_currentLineTokens.length) {
			try {
				String line = m_rdr.readLine();
				if (line == null) {
					m_atEOF = true;
					try { if (m_closeAtEOF) m_rdr.close(); } catch (Exception e) {}
					return false;
				}
				if (m_commentPattern != null && !m_commentPattern.equals(""))
					line = line.replaceAll(m_commentPattern, "");
				line = line.trim();
				if (!line.equals("")) {
					m_currentLineNumber = m_rdr.getLineNumber();
					m_currentLineTokens = line.split(m_splitPattern);
					m_iToken = 0;
				}
			} catch (IOException e) {
				m_atEOF = true;
				try { if (m_closeAtEOF) m_rdr.close(); } catch (Exception e2) {}
				return false;
			}
		}
		m_nextElement = m_currentLineTokens[m_iToken++];
		return true;
	}

	/**
	 * Remove the most recently returned element.
	 * Not supported.
	 * @throws UnsupportedOperationException Always thrown.
	 * @see java.util.Iterator#remove()
	 */
	@Override
	public void remove()
	{
		throw new UnsupportedOperationException(
						"FileStringIterator does not support remove()");
	}

	/**
	 * Return the line number of the current element, and the file name,
	 * as a readable string.
	 * @see IteratorWithPosition#getPositionDescription()
	 */
	@Override
	public String getPositionDescription()
	{
		if (m_atEOF) {
			return "end of file " + m_fname;
		} else if (m_currentLineNumber < 0) {
			return "beginning of file " + m_fname;
		} else {
			return "line " + m_currentLineNumber + " of file " + m_fname;
		}
	}

	/**
	 * Return the pattern used to split a line into tokens.
	 * @return The pattern used to split a line into tokens.
	 */
	public String getSplitPattern()
	{
		return m_splitPattern;
	}

	/**
	 * Set the pattern used to split a line into tokens.
	 * The default is white space: "[ \t\n\r]+".
	 * @param splitPattern The new pattern.
	 */
	public void setSplitPattern(String splitPattern)
	{
		m_splitPattern = splitPattern;
	}

	/**
	 * Return the pattern used to remove comments.
	 */
	public String getCommentPattern()
	{
		return m_commentPattern;
	}

	/**
	 * Set the pattern used to remove comments.
	 * After reading a line, if part of the line matches this pattern,
	 * that part is replaced by "".
	 * The default is "#.*$".
	 * @param commentPattern
	 * 		The new comment-removal pattern.
	 * 		To suppress comment removal, use null or "".
	 */
	public void setCommentPattern(String commentPattern)
	{
		m_commentPattern = commentPattern;
	}
}
