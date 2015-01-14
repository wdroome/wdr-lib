package com.wdroome.util;

/**
 * A simple interface for logging errors.
 * @see SystemErrorLogger
 * @author wdr
 */
public interface IErrorLogger
{
	/**
	 * Log an error message to the appropriate place.
	 * The message should not end with a new line;
	 * the logger will supply that as needed.
	 * The logger may also supply a time stamp prefix.
	 * @param msg The error message.
	 */
	public void logError(String msg);
}
