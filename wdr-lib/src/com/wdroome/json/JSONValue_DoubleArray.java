package com.wdroome.json;

import java.io.IOException;
import java.util.Iterator;
import java.util.ListIterator;
import java.math.BigInteger;

/**
 * A JSON array which is limited to double values.
 * @author wdr
 */
public class JSONValue_DoubleArray implements JSONValue, Iterable<Double>
{
	private final JSONValue_Array m_array;

	/**
	 * Create a new empty array.
	 */
	public JSONValue_DoubleArray()
	{
		m_array = new JSONValue_Array();
	}

	/**
	 * Create a new Double array from an existing JSON array.
	 * @param array The existing JSON array.
	 * @param convert If true, replace numeric-valued String elements
	 * 		and BigInt elements with the equivalent double element,
	 * 		and delete all other elements.
	 *		If false, delete all non-number elements.
	 */
	public JSONValue_DoubleArray(JSONValue_Array array, boolean convert)
	{
		m_array = array;
		for (ListIterator<JSONValue> iter = array.listIterator(); iter.hasNext(); ) {
			JSONValue v = iter.next();
			if (v instanceof JSONValue_Number) {
				// ok!
			} else if (!convert) {
				iter.remove();
			} else if (v instanceof JSONValue_String) {
				try {
					iter.set(new JSONValue_Number(Double.parseDouble(((JSONValue_String)v).m_value)));
				} catch (Exception e) {
					iter.remove();
				}
			} else if (v instanceof JSONValue_BigInt) {
				iter.set(new JSONValue_Number((((JSONValue_BigInt)v).m_value).doubleValue()));
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
	 * Return a double-valued element.
	 * @param index The element's index.
	 * @return The string value of the element.
	 * 		Return null if it's not a JSON string.
	 * @throws IndexOutOfBoundsException If index is invalid.
	 */
	public double get(int index)
	{
		JSONValue value = m_array.get(index);
		return (value instanceof JSONValue_Number) ? ((JSONValue_Number)value).m_value : 0;
	}
	
	/**
	 * Add a string value to the array.
	 * Create a {@link JSONValue_String} from the argument.
	 * @param value The value.
	 */
	public void add(double value)
	{
		m_array.add(new JSONValue_Number(value));
	}
	
	/**
	 * Replace an array element.
	 * Create a {@link JSONValue_Number} from the argument.
	 * @param index The element's index.
	 * @param value The value.
	 * @throws IndexOutOfBoundsException If index is invalid.
	 */
	public double set(int index, double value)
	{
		double prev = get(index);
		m_array.set(index, new JSONValue_Number(value));
		return prev;
	}
	
	/**
	 * Remove an array element.
	 * @param index The element's index.
	 * @return The previous value.
	 * @throws IndexOutOfBoundsException If index is invalid.
	 */
	public double remove(int index)
	{
		double prev = get(index);
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
	public Iterator<Double> iterator()
	{
		return new MyIterator();
	}
	
	private class MyIterator implements Iterator<Double>
	{
		private final Iterator<JSONValue> m_arrayIter;
		
		private MyIterator()
		{
			m_arrayIter = m_array.iterator();
		}
		
		public Double next()
		{
			JSONValue value = m_arrayIter.next();
			return (value instanceof JSONValue_String) ? ((JSONValue_Number)value).m_value : null;		
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
		return "DoubleArray";
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
