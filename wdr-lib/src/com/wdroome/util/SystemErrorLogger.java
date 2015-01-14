package com.wdroome.util;

/**
 * An {@link IErrorLogger} that log messages to System.err.
 * @author wdr
 */
public class SystemErrorLogger implements IErrorLogger
{
	/**
	 * Log an error message to System.err.
	 * This does not supply a time stamp; it simply calls System.err.println(msg).
	 * @see IErrorLogger#logError(java.lang.String)
	 */
	@Override
	public void logError(String msg)
	{
		System.err.println(msg);
	}

}
