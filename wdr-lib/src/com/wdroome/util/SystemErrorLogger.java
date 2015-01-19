package com.wdroome.util;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * An {@link IErrorLogger} that logs messages to System.err.
 * @author wdr
 */
public class SystemErrorLogger implements IErrorLogger
{
	/**
	 * A {@link SimpleDateFormat} string specifying a standard time stamp prefix.
	 */
	public static final String STANDARD_TIME_STAMP = "yyyy-MM-dd HH:mm:ss.SSS ";
	
	private final PrintStream m_ostream;	// Output stream. Never null.
	private final DateFormat m_dateFormat;	// If != null, format for time stamp prefix.
	
	/**
	 * Create a new logger that writes each message to {@link System#err}
	 * as a separate line, without a time stamp.
	 */
	public SystemErrorLogger()
	{
		this(null, null);
	}
	
	/**
	 * Create a new logger that writes each message to {@link System#err}
	 * as a separate line, with an optional time stamp.
	 * @param useTimeStamp If true, prepend {@link #STANDARD_TIME_STAMP} to each message.
	 */
	public SystemErrorLogger(boolean useTimeStamp)
	{
		this(null, useTimeStamp ? null : new SimpleDateFormat(STANDARD_TIME_STAMP));
	}
	
	/**
	 * Create a new logger that writes each message to a specified stream
	 * as a separate line, with an optional time stamp.
	 * @param out The output stream. If null, use {@link System#err}.
	 * @param dateFormat If not null, prepend a time stamp in this format
	 * 		to each message.
	 */
	public SystemErrorLogger(PrintStream out, DateFormat dateFormat)
	{
		m_ostream = out != null ? out : System.err;
		m_dateFormat = dateFormat;
	}
	
	/**
	 * Log an error message to System.err.
	 * This does not supply a time stamp; it simply calls System.err.println(msg).
	 * @see IErrorLogger#logError(java.lang.String)
	 */
	@Override
	public void logError(String msg)
	{
		if (m_dateFormat != null) {
			m_ostream.print(m_dateFormat.format(new Date()));
		}
		m_ostream.println(msg);
	}

	/**
	 *	Append the current time stamp, in the specified date format,
	 *	to a buffer.
	 *
	 *	@param b The buffer. If null, create a new StringBuilder.
	 *	@param dateFormat The format for the timestamp.
	 *	@return The parameter b, or, if null, a new StringBuilder.
	 *	@see #appendDate(StringBuilder)
	 */
	public static StringBuilder appendDate(StringBuilder b, DateFormat dateFormat)
	{
		if (b == null)
			b = new StringBuilder();
		b.append(dateFormat.format(new Date()));
		return b;
	}

	/**
	 *	Append the current time stamp to a buffer.
	 *	Use the date format "MM/dd/yyyy HH:mm:ss.SSS ".
	 *
	 *	@param b The buffer. If null, create a new StringBuilder.
	 *	@return The parameter b, or, if null, a new StringBuilder.
	 *	@see #appendDate(StringBuilder,DateFormat)
	 *	@see SimpleDateFormat
	 */
	public static StringBuilder appendDate(StringBuilder b)
	{
		return appendDate(b, new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS "));
	}
}
