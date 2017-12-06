package com.wdroome.json;

import java.io.IOException;
import java.util.Iterator;
import java.util.ListIterator;
import java.math.BigInteger;

/**
 * A JSON array which is limited to string values.
 * @author wdr
 */
public class JSONValue_StringArray implements JSONValue, Iterable<String>
{
	private final JSONValue_Array m_array;

	/**
	 * Create a new empty array.
	 */
	public JSONValue_StringArray()
	{
		m_array = new JSONValue_Array();
	}

	/**
	 * Create a new String array from an existing JSON array.
	 * @param array The existing JSON array.
	 * @param convert If true, replace number &amp; boolean elements with
	 * 		the equivalent string elements, and delete all other elements.
	 *		If false, delete all non-string elements.
	 */
	public JSONValue_StringArray(JSONValue_Array array, boolean convert)
	{
		m_array = array;
		for (ListIterator<JSONValue> iter = array.listIterator(); iter.hasNext(); ) {
			JSONValue v = iter.next();
			if (v instanceof JSONValue_String) {
				// ok!
			} else if (!convert) {
				iter.remove();
			} else if (v instanceof JSONValue_Number) {
				iter.set(new JSONValue_String(Double.toString(((JSONValue_Number)v).m_value)));
			} else if (v instanceof JSONValue_BigInt) {
				iter.set(new JSONValue_String((((JSONValue_BigInt)v).m_value).toString()));
			} else if (v instanceof JSONValue_Boolean) {
				iter.set(new JSONValue_String(((JSONValue_Boolean)v).m_value ? "true" : "false"));
			} else {
				iter.remove();
			}
		}
	}
	
	/**
	 * Return the underlying JSON array.
	 * @return The underlying JSON array.
	 */
	public JSONValue_Array getJSONArray()
	{
		return m_array;
	}
	
	/**
	 * Return a String-valued element.
	 * @param index The element's index.
	 * @return The string value of the element.
	 * 		Return null if it's not a JSON string.
	 * @throws IndexOutOfBoundsException If index is invalid.
	 */
	public String get(int index)
	{
		JSONValue value = m_array.get(index);
		return (value instanceof JSONValue_String) ? ((JSONValue_String)value).m_value : null;
	}
	
	/**
	 * Add a string value to the array.
	 * Create a {@link JSONValue_String} from the argument.
	 * @param value The value.
	 */
	public void add(String value)
	{
		m_array.add(new JSONValue_String(value));
	}
	
	/**
	 * Replace an array element.
	 * Create a {@link JSONValue_String} from the argument.
	 * @param index The element's index.
	 * @param value The value.
	 * @throws IndexOutOfBoundsException If index is invalid.
	 */
	public String set(int index, String value)
	{
		String prev = get(index);
		m_array.set(index, new JSONValue_String(value));
		return prev;
	}
	
	/**
	 * Remove an array element.
	 * @param index The element's index.
	 * @return The previous value.
	 * @throws IndexOutOfBoundsException If index is invalid.
	 */
	public String remove(int index)
	{
		String prev = get(index);
		m_array.remove(index);
		return prev;
	}
	
	/**
	 * Return the size of the array.
	 * @return The size of the array.
	 */
	public int size()
	{
		return m_array.size();
	}

	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<String> iterator()
	{
		return new MyIterator();
	}
	
	private class MyIterator implements Iterator<String>
	{
		private final Iterator<JSONValue> m_arrayIter;
		
		private MyIterator()
		{
			m_arrayIter = m_array.iterator();
		}
		
		public String next()
		{
			JSONValue value = m_arrayIter.next();
			return (value instanceof JSONValue_String) ? ((JSONValue_String)value).m_value : null;		
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#hasNext()
		 */
		@Override
		public boolean hasNext()
		{
			return m_arrayIter.hasNext();
		}
	}

	/* (non-Javadoc)
	 * @see JSONValue#isSimple()
	 */
	@Override
	public boolean isSimple()
	{
		return false;
	}

	/* (non-Javadoc)
	 * @see JSONValue#jsonType()
	 */
	@Override
	public String jsonType()
	{
		return "StringArray";
	}

	/* (non-Javadoc)
	 * @see JSONValue#writeJSON(JSONWriter)
	 */
	@Override
	public void writeJSON(JSONWriter writer) throws IOException
	{
		m_array.writeJSON(writer);
	}

	/**
	 * Return a compact JSON encoding of this array.
	 */
	@Override
	public String toString()
	{
		return JSONUtil.toJSONString(this, false);
	}
}
