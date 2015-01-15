package com.wdroome.json;

import java.io.IOException;

/**
 * The JSON constant "null".
 * @author wdr
 */
public class JSONValue_Null implements JSONValue
{
	private static final String VALUE = "null";
	
	/** A public constant. */
	public static final JSONValue_Null NULL = new JSONValue_Null();
	
	/**
	 * Return "null".
	 */
	@Override
	public String toString()
	{
		return VALUE;
	}
	
	/**
	 * @see JSONValue#writeJSON(JSONWriter)
	 */
	@Override
	public void writeJSON(JSONWriter writer) throws IOException
	{
		writer.write(VALUE);
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
	 * Return the same hashcode for all NULL objects,
	 * namely the hashcode of "null".
	 * @return The hash code.
	 */
	@Override
	public int hashCode()
	{
		return VALUE.hashCode();
	}

	/**
	 * Return true iff obj is also of type JSONValue_Null.
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
		return true;
	}
}
