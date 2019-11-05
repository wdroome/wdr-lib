package com.wdroome.util;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * An {@link IErrorLogger} that writes messages to a PrintStream.
 * The default constructor uses System.err.
 * Other c'tors can use different streams,
 * and can define an optional time-stamp for each message.
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
	 * as a separate line, without a time stamp, to {@link System#err}.
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
	 * Log an error message using {@link PrintStream#println(String)}.
	 * The constructor specifies the PrintStream
	 * and the format of the optional time-stamp.
	 * This method does not call {@link PrintStream#flush()};
	 * it assumes println() does that.
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
}
