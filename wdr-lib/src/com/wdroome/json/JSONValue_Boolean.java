package com.wdroome.json;

import java.io.IOException;

/**
 * A JSON boolean value -- true or false.
 * For simplicity, the value is a read-only public member.
 * <p>
 * It would be nice if this could extend java.lang.Boolean, but that's final.
 * @author wdr
 */
public class JSONValue_Boolean implements JSONValue
{
	private static final String S_TRUE = "true";
	private static final String S_FALSE = "false";

	/** A public constant for "true". */
	public static final JSONValue_Boolean TRUE = new JSONValue_Boolean(true);

	/** A public constant for "false". */
	public static final JSONValue_Boolean FALSE = new JSONValue_Boolean(false);
	
	/** The read-only boolean value. */
	public final boolean m_value;
	
	/**
	 * Create a new JSON boolean.
	 * @param value The boolean value.
	 */
	public JSONValue_Boolean(boolean value)
	{
		m_value = value;
	}
	
	/**
	 * Return the string value of the boolean.
	 */
	@Override
	public String toString()
	{
		return m_value ? S_TRUE : S_FALSE;
	}
	
	/**
	 * @see JSONValue#writeJSON(JSONWriter)
	 */
	@Override
	public void writeJSON(JSONWriter writer) throws IOException
	{
		writer.write(m_value ? S_TRUE : S_FALSE);
	}
	
	/**
	 * @see JSONValue#isSimple()
	 */
	@Override
	public boolean isSimple()
	{
		return true;
	}
	
	/**
	 * @see JSONValue#jsonType()
	 */
	@Override
	public String jsonType()
	{
		return "Boolean";
	}

	/**
	 * Return the hashcode for the boolean value.
	 */
	@Override
	public int hashCode()
	{
		return m_value ? Boolean.TRUE.hashCode() : Boolean.FALSE.hashCode();
	}

	/**
	 * Return true iff the values are equal.
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		return m_value == ((JSONValue_Boolean)obj).m_value;
	}
}
