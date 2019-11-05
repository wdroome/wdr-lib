package com.wdroome.json;

/**
 * Represent an invalid field value error.
 * @author wdr
 */
public class JSONFieldValueException extends JSONException
{	
	private static final long serialVersionUID = 9049220875990763488L;
	
	private final String m_field;
	private final String m_value;

	/**
	 * Create a new exception.
	 * @param msg A description of the error.
	 * @param field The field name (possibly null).
	 * @param value The value, as a string,
	 * 		or null if not practical to convert to string.
	 */
	public JSONFieldValueException(String msg, String field, String value)
	{
		super(msg);
		m_field = field;
		m_value = value;
	}

	/**
	 * Create a new exception.
	 * @param msg A description of the error.
	 */
	public JSONFieldValueException(String msg)
	{
		this(msg, null, null);
	}
	
	/**
	 * @return The name of the missing field. May be null.
	 */
	public String getField()
	{
		return m_field;
	}
	
	/**
	 * @return The invalid value. May be null.
	 */
	public String getValue()
	{
		return m_value;
	}
}
