package com.wdroome.altodata;

import com.wdroome.json.JSONException;

/**
 * Represent an illegal PID name error.
 * That is, a PID name that does not conform to the rules of the ALTO RFC.
 * @author wdr
 */
public class IllegalPidNameException extends JSONException
{	
	private static final long serialVersionUID = -337864828194867689L;

	/** The name of the field with the invalid pid. */
	public final String m_field;
	
	/** The invalid pid. */
	public final String m_value;
	
	public IllegalPidNameException(String field, String value)
	{
		super("Invalid PID name \"" + value + "\" in field " + field);
		m_field = field;
		m_value = value;
	}
}
