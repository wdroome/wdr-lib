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
	
	/**
	 * Print an error iff no error of the same category has occurred recently.
	 * The default version always prints the message, and the prepends the category if not empty.
	 * When printing, show the total number of errors of this category. 
	 * @param category The category (or type).
	 * @param intvlMS The time between printing errors of this type, in millisec.
	 * 			If 0, always show the message.
	 * @param msg The error message itself.
	 */
	default public void logError(String category, long intvlMS, String msg)
	{
		if (category == null || category.isBlank()) {
			logError(msg);
		} else {
			if (!category.endsWith(":")) {
				category += ":";
			}
			logError(category + " " + msg);
		}
	}
}
