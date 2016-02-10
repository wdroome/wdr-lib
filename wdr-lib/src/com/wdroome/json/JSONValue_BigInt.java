package com.wdroome.json;

import java.io.IOException;
import java.math.BigInteger;

/**
 * A JSON integer value (e.g, no fractional part and no exponent)
 * which does not fit into a double without loss of precision.
 * For simplicity, the value is a read-only public member.
 * <p>
 * It would be nice if this could extend java.lang.Number, but that's final.
 * @see JSONValue_Number
 * @author wdr
 */
public class JSONValue_BigInt implements JSONValue
{
	/** The read-only numeric value. */
	public final BigInteger m_value;
	
	/**
	 * Create a new JSON BigInt.
	 * @param value The numeric value.
	 */
	public JSONValue_BigInt(BigInteger value)
	{
		m_value = value;
	}
	
	/**
	 * Create a new JSON BigInt.
	 * @param value The numeric value.
	 * @throws NumberFormatException If value is not an integer.
	 */
	public JSONValue_BigInt(Number value)
	{
		if (value instanceof BigInteger) {
			m_value = (BigInteger)value;
		} else if (value instanceof Double) {
			double d = ((Double)value).doubleValue();
			if (Math.floor(d) != d) {
				throw new NumberFormatException(value + " is not an integer");
			}
			m_value = new BigInteger(String.format("%.0f", d));
		} else if (value instanceof Float) {
			double d = ((Float)value).doubleValue();
			if (Math.floor(d) != d) {
				throw new NumberFormatException(value + " is not an integer");
			}
			m_value = new BigInteger(String.format("%.0f", d));
		} else {
			m_value = new BigInteger(value.toString());
		}
	}
	
	/**
	 * Create a new JSON BigInt.
	 * @param value The numeric value.
	 */
	public JSONValue_BigInt(long value)
	{
		m_value = new BigInteger(Long.toString(value));
	}
	
	/**
	 * Return the string value of the number.
	 */
	@Override
	public String toString()
	{
		return m_value.toString();
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
		return "BigInt";
	}

	/**
	 * Return the hashcode for the double value.
	 */
	@Override
	public int hashCode()
	{
		return m_value.hashCode();
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
		return m_value == ((JSONValue_BigInt)obj).m_value;
	}
}
