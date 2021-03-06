package com.wdroome.json.validate;

/**
 * A JSON value fails validation.
 * @author wdr
 */
public class JSONValidationException extends Exception
{
	private static final long serialVersionUID = 2003751443730340570L;

	/**
	 * A validation error in a JSON value.
	 * @param msg A description of the error,
	 * with an indication of the path of the offending element.
	 */
	public JSONValidationException(String msg)
	{
		super(msg);
	}
}
