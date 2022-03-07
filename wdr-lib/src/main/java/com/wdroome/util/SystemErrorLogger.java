package com.wdroome.util;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * An {@link IErrorLogger} that writes messages to a PrintStream or a PrintWriter.
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
	
	private final PrintStream m_ostream;	// One of these is never null.
	private final PrintWriter m_writer;
	
	private final DateFormat m_dateFormat;	// If != null, format for time stamp prefix.
	
	private static class Count
	{
		private int m_count;
		private long m_lastPrtTS;
	}
	private Map<String, Count> m_counts = new HashMap<String, Count>();
	
	/**
	 * Return a DateFormat for the class's standard date format.
	 * @return A DateFormat for the class's standard date format.
	 */
	public static DateFormat stdDateFormat()
	{
		return new SimpleDateFormat(STANDARD_TIME_STAMP);
	}
	
	/**
	 * Create a new logger that writes each message to {@link System#err}
	 * as a separate line, without a time stamp, to {@link System#err}.
	 */
	public SystemErrorLogger()
	{
		this(System.err, null);
	}
	
	/**
	 * Create a new logger that writes each message to {@link System#err}
	 * as a separate line, with an optional time stamp.
	 * @param useTimeStamp If true, prepend {@link #STANDARD_TIME_STAMP} to each message.
	 */
	public SystemErrorLogger(boolean useTimeStamp)
	{
		this(System.err, useTimeStamp ? null : new SimpleDateFormat(STANDARD_TIME_STAMP));
	}
	
	/**
	 * Create a new logger that writes each message to a specified stream
	 * as a separate line, with an optional time stamp.
	 * @param out The output stream. If null, no messages will be written.
	 * @param useTimeStamp If true, prepend {@link #STANDARD_TIME_STAMP} to each message.
	 */
	public SystemErrorLogger(PrintStream out, boolean useTimeStamp)
	{
		this(out, useTimeStamp ? null : new SimpleDateFormat(STANDARD_TIME_STAMP));
	}
	
	/**
	 * Create a new logger that writes each message to a specified stream
	 * as a separate line, with an optional time stamp.
	 * @param out The output stream. If null, no messages will be written..
	 * @param dateFormat If not null, prepend a time stamp in this format
	 * 		to each message.
	 */
	public SystemErrorLogger(PrintStream out, DateFormat dateFormat)
	{
		m_ostream = out;
		m_writer = null;
		m_dateFormat = dateFormat;
	}
	
	/**
	 * Create a new logger that writes each message to a specified writer
	 * as a separate line, with an optional time stamp.
	 * @param writer The output stream. If null, no messages will be written.
	 * @param useTimeStamp If true, prepend {@link #STANDARD_TIME_STAMP} to each message.
	 */
	public SystemErrorLogger(PrintWriter writer, boolean useTimeStamp)
	{
		this(writer, useTimeStamp ? null : new SimpleDateFormat(STANDARD_TIME_STAMP));
	}
	
	/**
	 * Create a new logger for a writer.
	 * @param writer
	 * 		The writer for error messages.
	 * 		If null, no messages will be written.
	 */
	public SystemErrorLogger(PrintWriter writer, DateFormat dateFormat)
	{
		m_ostream = null;
		m_writer = writer;
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
		if (msg.endsWith("\n")) {
			msg = msg.replaceAll("[\\r\\n]+$", "");
		}
		if (m_dateFormat != null) {
			msg = m_dateFormat.format(new Date()) + msg;
		}
		if (m_ostream != null) {
			m_ostream.println(msg);
		} else {
			m_writer.println(msg);
		}
	}
	
	/**
	 * Print an error iff no error of the same category has occurred recently.
	 * When printing, show the total number of errors of this category. 
	 * @param category The category (or type).
	 * @param intvlMS The time between printing errors of this type, in millisec.
	 * 			If 0, always show the message.
	 * @param msg The error message itself.
	 */
	@Override
	public void logError(String category, long intvlMS, String msg)
	{
		// If no category, always print.
		if (category == null || category.isBlank()) {
			logError(msg);
			return;
		}
		
		// Increment category count & last time, print if appropriate.
		boolean prt = false;
		int n;
		synchronized (m_counts) {
			Count count = m_counts.get(category);
			if (count == null) {
				count = new Count();
				count.m_count = 0;
				count.m_lastPrtTS = 0;
				m_counts.put(category, count);
			}
			long curTS = System.currentTimeMillis();
			if ((curTS - count.m_lastPrtTS) >= intvlMS) {
				prt = true;
				count.m_lastPrtTS = curTS;
			}
			count.m_count += 1;
			n = count.m_count;
		}
		if (prt) {
			logError(category + "[" + n + "]: " + msg);
		}
	}
}
