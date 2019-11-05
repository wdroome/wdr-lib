package com.wdroome.json;

import java.io.IOException;
import java.util.Iterator;
import java.util.ListIterator;

/**
 * A JSON array which is limited to Object values.
 * @author wdr
 */
public class JSONValue_ObjectArray implements JSONValue, Iterable<JSONValue_Object>
{
	private final JSONValue_Array m_array;

	/**
	 * Create a new empty array.
	 */
	public JSONValue_ObjectArray()
	{
		m_array = new JSONValue_Array();
	}

	/**
	 * Create a new String array from an existing JSON array.
	 * Ignore all non-object elements from the array.
	 * This class creates a shallow clone of the existing array;
	 * it does not change that array.
	 * @param array The existing JSON array.
	 */
	public JSONValue_ObjectArray(JSONValue_Array array)
	{
		m_array = array != null ? new JSONValue_Array(array) : new JSONValue_Array();
		for (ListIterator<JSONValue> iter = m_array.listIterator(); iter.hasNext(); ) {
			JSONValue v = iter.next();
			if (!(v instanceof JSONValue_Object)) {
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
	 * Return an element.
	 * @param index The element's index.
	 * @return The object value of the element.
	 * 		Return null if it's not a JSON object.
	 * @throws IndexOutOfBoundsException If index is invalid.
	 */
	public JSONValue_Object get(int index)
	{
		JSONValue value = m_array.get(index);
		return (value instanceof JSONValue_Object) ? (JSONValue_Object)value : null;
	}
	
	/**
	 * Add a object value to the array.
	 * @param value The value.
	 */
	public void add(JSONValue_Object value)
	{
		m_array.add(value);
	}
	
	/**
	 * Replace an array element.
	 * @param index The element's index.
	 * @param value The value.
	 */
	public JSONValue_Object set(int index, JSONValue_Object value)
	{
		JSONValue_Object prev = get(index);
		m_array.set(index, value);
		return prev;
	}
	
	/**
	 * Remove an array element.
	 * @param index The element's index.
	 * @return The previous value.
	 * @throws IndexOutOfBoundsException If index is invalid.
	 */
	public JSONValue_Object remove(int index)
	{
		JSONValue_Object prev = get(index);
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
	public Iterator<JSONValue_Object> iterator()
	{
		return new MyIterator();
	}
	
	private class MyIterator implements Iterator<JSONValue_Object>
	{
		private final Iterator<JSONValue> m_arrayIter;
		
		private MyIterator()
		{
			m_arrayIter = m_array.iterator();
		}
		
		public JSONValue_Object next()
		{
			JSONValue value = m_arrayIter.next();
			return (value instanceof JSONValue_Object) ? (JSONValue_Object)value : null;		
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
		return "ObjectArray";
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
