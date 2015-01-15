package com.wdroome.json;


/**
 * Represent the error of JSON field having the wrong type.
 * @author wdr
 */
@SuppressWarnings("serial")
public class JSONValueTypeException extends JSONException
{
	private final String m_field;

	/**
	 * Create a new exception.
	 * @param msg A description of the error.
	 * @param field The field name (possibly null).
	 */
	public JSONValueTypeException(String msg, String field)
	{
		super(msg);
		m_field = field;
	}
	
	/**
	 * @return The name of the missing field. May be null.
	 */
	public String getField()
	{
		return m_field;
	}
}
