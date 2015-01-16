package com.wdroome.util;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * An iterator over the elements of an array.
 * The iterator is also Iterable, so it can be used in a foreach statement.
 * If the standard java library has a class that does this,
 * I can't find it.
 * @author wdr
 */
public class ArrayIterator<T>
	implements ListIterator<T>, Iterator<T>, Iterable<T>, IteratorWithPosition<T>
{
	private final T[] m_array;				// Underlying array. May be null.
	private final int m_start;				// Index of first element in subset. 0 if m_array is null.
	private final int m_length;				// Length of subset. 0 if m_array is null.
	private final boolean m_skipNulls;		// If true, skip nulls in array.
	
	private int m_lastReturnedIndex = -1;	// Index of last element returned by next() or previous(),
											// relative to m_start.
											// -1 at start of subset and m_length at end.

	/**
	 * Create a new iterator.
	 * Equivalent to ArrayIterator(array,false).
	 * @param array The array. If null, assume a 0-length array.
	 */
	public ArrayIterator(T[] array)
	{
		this(array, 0, -1, false);
	}

	/**
	 * Create a new iterator for a subset of an array.
	 * @param array The array. If null, assume a 0-length array.
	 * @param start Start index of subset of array.
	 * @param length Length of subset of array. If -1, use remainder of array.
	 * @throws IllegalArgumentException
	 * 			If the start index is not valid.
	 */
	public ArrayIterator(T[] array, int start, int length)
	{
		this(array, start, length, false);
	}
	
	/**
	 * Create a new iterator for a subset of an array.
	 * @param array The array. If null, assume a 0-length array.
	 * @param start Start index of subset of array.
	 * @param length Length of subset of array. If -1, use remainder of array.
	 * @param skipNulls If true, next() skips null elements in the array.
	 * @throws IllegalArgumentException
	 * 			If the start index is not valid.
	 */
	public ArrayIterator(T[] array, int start, int length, boolean skipNulls)
	{
		m_array = array;
		if (array == null) {
			m_start = 0;
			m_length = 0;
		} else {
			if (!(start >= 0 && start <= array.length)) {
				throw new IllegalArgumentException(this.getClass().getName()
										+ ": illegal start index " + start
										+ " for array length " + array.length);
			}
			m_start = start;
			if (length >= 0 && start + length < array.length) {
				m_length = length;
			} else {
				m_length = array.length - m_start;
			}
		}
		m_skipNulls = skipNulls;
	}

	/**
	 * Return true iff there are more elements (Iterator<T> interface).
	 */
	@Override
	public boolean hasNext()
	{
		return nextIndex() < m_length;
	}
	
	/**
	 * Return true iff there are more elements (ListIterator<T> interface).
	 */
	@Override
	public boolean hasPrevious()
	{
		return previousIndex() >= 0;
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
		m_lastReturnedIndex = nextIndex();
		if (m_lastReturnedIndex >= m_length) {
			throw new NoSuchElementException(this.getClass().getName() + ".next(): over end");
		}
		return m_array[m_start + m_lastReturnedIndex];
	}

	/**
	 * Return the previous element (ListIterator<T> interface).
	 * This may or may not return null elements in the array,
	 * depending on the flag given to the c'tor.
	 * @throws NoSuchElementException If there are no previous elements.
	 */
	@Override
	public T previous()
	{
		m_lastReturnedIndex = previousIndex();
		if (m_lastReturnedIndex < 0) {
			throw new NoSuchElementException(this.getClass().getName() + ".prev(): at start");
		}
		return m_array[m_start + m_lastReturnedIndex];
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
	 * Return the index in the subset of the next element,
	 * or m_length if none.  Skip null elements if so configured.
	 * Implements {@link ListIterator#nextIndex()}.
	 * The returned index is relative to the subset.
	 */
	@Override
	public int nextIndex()
	{
		int i = m_lastReturnedIndex;
		if (!m_skipNulls) {
			return i < m_length ? i+1 : m_length;
		} else {
			while (++i < m_length) {
				if (m_array[m_start + i] != null) {
					return i;
				}
			}
			return m_length;
		}
	}

	/**
	 * Return the index in the subset of the previous element,
	 * or m_length if none.  Skip null elements if so configured.
	 * Implements {@link ListIterator#previousIndex()}.
	 * The returned index is relative to the subset.
	 */
	@Override
	public int previousIndex()
	{
		int i = m_lastReturnedIndex;
		if (!m_skipNulls) {
			return i >= 0 ? i-1 : -1;
		} else {
			while (--i >= 0) {
				if (m_array[m_start + i] != null) {
					return i;
				}
			}
			return -1;
		}
	}

	/**
	 * Return a description of the index of the most recently returned element
	 * (IteratorWithPosition<T> interface).
	 * This returns "item[##]", where ## is the index of the item
	 * in the array subset.
	 * @see IteratorWithPosition#getPositionDescription()
	 */
	@Override
	public String getPositionDescription()
	{
		return "item[" + m_lastReturnedIndex + "]";
	}

	/**
	 * Unsupported; cannot remove items from array (Iterator<T> interface).
	 * @throws UnsupportedOperationException Always.
	 */
	@Override
	public void remove()
	{
		throw new UnsupportedOperationException(this.getClass().getName() + ".remove()");
	}

	/**
	 * Unsupported; cannot add items to array (ListIterator<T> interface).
	 * @throws UnsupportedOperationException Always.
	 */
	@Override
	public void add(T o)
	{
		throw new UnsupportedOperationException(this.getClass().getName() + ".add()");
	}

	/**
	 * Unsupported; cannot replace items in array (ListIterator<T> interface).
	 * @throws UnsupportedOperationException Always.
	 */
	@Override
	public void set(T o)
	{
		throw new UnsupportedOperationException(this.getClass().getName() + ".set()");
	}
}
