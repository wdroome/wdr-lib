package com.wdroome.json;

import java.io.IOException;
import java.math.BigInteger;

/**
 * A JSON Number with a decimal point,
 * or an integer which fits in a double without loss of precision.
 * For simplicity, the value is a read-only public member.
 * <p>
 * It would be nice if this could extend java.lang.Number, but that's final.
 * @see JSONValue_BigInt
 * @see JSONValue_BigInt#isBigInt(long)
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
	 * Return the value as a BigInteger.
	 * @return The value as a BigInteger.
	 * @throws NumberFormatException If the value is not an integer.
	 */
	public BigInteger toBigInteger() throws NumberFormatException
	{
		if (Math.floor(m_value) != m_value) {
			throw new NumberFormatException(m_value + " is not an integer");
		}
		return new BigInteger(String.format("%.0f", m_value));
	}
	
	/**
	 * Return the string value of the number.
	 * If the value is not a valid number,
	 * return the string value of the JSON null constant.
	 */
	@Override
	public String toString()
	{
		if (Double.isNaN(m_value) || Double.isInfinite(m_value)) {
			return JSONValue_Null.NULL.toString();
		}
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
