package com.wdroome.util.swing;
import java.io.PrintStream;
import java.io.PrintWriter;

import java.text.DateFormat;

import javax.swing.JTextArea;
import javax.swing.JScrollPane;

import com.wdroome.util.IErrorLogger;
import com.wdroome.util.SystemErrorLogger;

/**
 * An IErrorLogger which writes to a JTextArea.
 * Note that the writers do NOT need to be in the Swing application thread.
 * @author wdr
 */
public class JTextAreaErrorLogger implements IErrorLogger
{
	private final IErrorLogger m_logger;
	private final JTextArea m_textArea;
	
	/**
	 * Create a new logger.
	 */
	public JTextAreaErrorLogger()
	{
		this(0, null, null);
	}
	
	/**
	 * Create a new logger.
	 * @param style The style for the TextArea (ignored if null).
	 * @param id The id for the TextArea (ignored if null).
	 */
	public JTextAreaErrorLogger(String style, String id)
	{
		this(0, style, id);
	}

	/**
	 * Create a new logger.
	 * @param maxLines The maximum number of lines to retain.
	 * 		If 0 or negative, use a default.
	 * @param style The style for the TextArea (ignored if null).
	 * @param id The id for the TextArea (ignored if null).
	 */
	public JTextAreaErrorLogger(int maxLines, String style, String id)
	{
		m_textArea = new JTextArea();
		// XXX set style
		SwingComponentMgr.g_defMgr.setId(m_textArea, id);
    	m_textArea.setEditable(false);
    	m_textArea.setLineWrap(true);
    	m_textArea.setWrapStyleWord(false);
		m_logger = new SystemErrorLogger(new PrintWriter(new JTextAreaWriter(m_textArea, maxLines)), null);
	}

	/**
	 * Create a new logger.
	 * @param maxLines The maximum number of lines to retain.
	 * 		If 0 or negative, use a default.
	 * @param style The style for the TextArea (ignored if null).
	 * @param id The id for the TextArea (ignored if null).
	 * @param useTimeStamp If not null, prepend a time stamp to all printed messages.
	 */
	public JTextAreaErrorLogger(int maxLines, String style, String id, boolean useTimeStamp)
	{
		this(maxLines, style, id, useTimeStamp ? SystemErrorLogger.stdDateFormat() : null);
	}

	/**
	 * Create a new logger.
	 * @param maxLines The maximum number of lines to retain.
	 * 		If 0 or negative, use a default.
	 * @param style The style for the TextArea (ignored if null).
	 * @param id The id for the TextArea (ignored if null).
	 * @param dateFormat If not null, prepend this time stamp to all printed messages.
	 */
	public JTextAreaErrorLogger(int maxLines, String style, String id, DateFormat dateFormat)
	{
		m_textArea = new JTextArea();
		// XXX set style
		SwingComponentMgr.g_defMgr.setId(m_textArea, id);
    	m_textArea.setEditable(false);
    	m_textArea.setLineWrap(true);
    	m_textArea.setWrapStyleWord(false);
		m_logger = new SystemErrorLogger(new PrintWriter(new JTextAreaWriter(m_textArea, maxLines)),
					dateFormat);
	}

	/* (non-Javadoc)
	 * @see IErrorLogger#log(String, long, String)
	 */
	@Override
	public void logError(String category, long intvlMS, String msg)
	{
		m_logger.logError(category, intvlMS, msg);
	}
	
	@Override
	public void logError(String msg)
	{
		m_logger.logError(msg);
	}

	/**
	 * Return the TextArea.
	 * @return The TextArea.
	 */
	public JTextArea getTextArea()
	{
		return m_textArea;
	}
	
	/**
	 * Return a JScrollPane with the textarea for this logger.
	 * @param style The style for the new JScrollPane. Ignored if null.
	 * @param id The id for the new JScrollPane. Ignored if null.
	 * @return A new scroll pane with the logger text area.
	 */
	public JScrollPane getScrollPane(String style, String id)
	{
		JScrollPane sp = new JScrollPane(m_textArea);
		// XXX set style
		SwingComponentMgr.g_defMgr.setId(sp, id);
		return sp;
	}
}
