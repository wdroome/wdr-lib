package com.wdroome.json;

import java.io.IOException;

/**
 * A JSON Number.
 * All numbers are doubles, even integer values.
 * For simplicity, the value is a read-only public member.
 * <p>
 * It would be nice if this could extend java.lang.Number, but that's final.
 * @author wdr
 */
public class JSONValue_Number implements JSONValue
{
	/** The read-only numeric value. */
	public final double m_value;
	
	/**
	 * Create a new JSON number.
	 * @param value The numeric value.
	 */
	public JSONValue_Number(double value)
	{
		m_value = value;
	}
	
	/**
	 * Create a new JSON number.
	 * @param value The numeric value.
	 */
	public JSONValue_Number(Number value)
	{
		m_value = value.doubleValue();
	}
	
	/**
	 * Return the string value of the number.
	 */
	@Override
	public String toString()
	{
		String s = Double.toString(m_value);
		if (s.endsWith(".0")) {
			s = s.substring(0, s.length()-2);
		}
		return s;
	}
	
	/**
	 * @see JSONValue#writeJSON(JSONWriter)
	 */
	@Override
	public void writeJSON(JSONWriter writer) throws IOException
	{
		writer.write(toString());
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
		return "Number";
	}

	/**
	 * Return the hashcode for the double value.
	 */
	@Override
	public int hashCode()
	{
		long temp;
		temp = Double.doubleToLongBits(m_value);
		return (int) (temp ^ (temp >>> 32));
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
		return m_value == ((JSONValue_Number)obj).m_value;
	}
}
