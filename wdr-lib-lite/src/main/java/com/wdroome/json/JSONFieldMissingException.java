package com.wdroome.json;

/**
 * Represent a missing field error.
 * @author wdr
 */
public class JSONFieldMissingException extends JSONException
{
	private static final long serialVersionUID = -3602342507343909230L;
	
	private final String m_field;

	/**
	 * Create a new exception.
	 * @param msg A description of the error.
	 * @param field The field name (possibly null).
	 */
	public JSONFieldMissingException(String msg, String field)
	{
		super(msg);
		m_field = field;
	}

	/**
	 * Create a new exception.
	 * @param msg A description of the error.
	 */
	public JSONFieldMissingException(String msg)
	{
		this(msg, null);
	}
	
	/**
	 * @return The name of the missing field. May be null.
	 */
	public String getField()
	{
		return m_field;
	}
}
