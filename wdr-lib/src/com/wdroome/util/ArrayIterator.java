package com.wdroome.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator over the elements of an array.
 * The iterator is also Iterable, so it can be used in a foreach statement.
 * If the standard java library has a class that does this,
 * I can't find it.
 * @author wdr
 */
public class ArrayIterator<T> implements Iterator<T>, Iterable<T>, IteratorWithPosition<T>
{
	private final T[] m_array;				// Underlying array. May be null.
	private final int m_length;				// Length of m_array. 0 if m_array is null.
	private final boolean m_skipNulls;		// If true, skip nulls in array.
	
	private int m_nextIndex = 0;			// Index of next element to return.
	private int m_lastReturnedIndex = -1;	// Index of last element returned by next(),
											// or -1 at start and m_length at end.
	
	/**
	 * Create a new iterator.
	 * @param array The array. If null, assume a 0-length array.
	 * @param skipNulls If true, next() skips null elements in the array.
	 */
	public ArrayIterator(T[] array, boolean skipNulls)
	{
		m_array = array;
		m_length = (array != null) ? array.length : 0;
		m_skipNulls = skipNulls;
		if (m_skipNulls) {
			skipNulls();
		}
	}

	/**
	 * Create a new iterator.
	 * Equivalent to ArrayIterator(array,false).
	 * @param array The array. If null, assume a 0-length array.
	 */
	public ArrayIterator(T[] array)
	{
		this(array, false);
	}

	/**
	 * Return true iff there are more elements (Iterator<T> interface).
	 */
	@Override
	public boolean hasNext()
	{
		if (m_nextIndex < m_length) {
			return true;
		} else {
			m_lastReturnedIndex = m_length;
			return false;
		}
	}

	/**
	 * Return the next element (Iterator<T> interface).
	 * This may or may not return null elements in the array,
	 * depending on the flag given to the c'tor.
	 * @throws NoSuchElementException If there are no more elements.
	 */
	@Override
	public T next()
	{
		if (m_nextIndex >= m_length) {
			m_lastReturnedIndex = m_length;
			throw new NoSuchElementException("ArrayIterator<T> over end");
		}
		m_lastReturnedIndex = m_nextIndex;
		T ret = m_array[m_nextIndex++];
		if (m_skipNulls) {
			skipNulls();
		}
		return ret;
	}

	/**
	 * Not implemented; cannot remove items from array (Iterator<T> interface).
	 * @throws UnsupportedOperationException Always.
	 */
	@Override
	public void remove()
	{
		throw new UnsupportedOperationException(
					"ArrayIterator<T> does not support remove()");
	}
	
	/**
	 * Advance m_nextIndex until m_array[m_nextIndex] != null,
	 * or m_nextIndex is over the end of the array.
	 */
	private void skipNulls()
	{
		while (m_nextIndex < m_length && m_array[m_nextIndex] == null) {
			m_nextIndex++;
		}
	}

	/**
	 * Return this object as an Iterator,
	 * so the object can be used in a foreach statement (Iterable<T> interface).
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<T> iterator()
	{
		return this;
	}

	/**
	 * Return a description of the index of the most recently returned element
	 * (IteratorWithPosition<T> interface).
	 * This returns "item[##]", where ## is the index of the item
	 * in the underlying array.
	 * @see IteratorWithPosition#getPositionDescription()
	 */
	@Override
	public String getPositionDescription()
	{
		return "item[" + m_lastReturnedIndex + "]";
	}
}
