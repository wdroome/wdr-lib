package com.wdroome.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.ArrayList;

/**
 * A read-only iterator wrapper class.
 * For example, suppose you want to let clients iterate
 * over the elements of a private set, but you don't want
 * clients to remove elements. To prevent that, create a new
 * ImmutableIterator, giving the set's iterator to the c'tor,
 * and then give the ImmutableIterator to the client.
 * The ImmutableIterator passes hasNext() and next() to the set's iterator,
 * but throws an exception if the client calls remove().
 * @author wdr
 */
public class ImmutableIterator<T> implements Iterator<T>
{
	private final Iterator<T> m_iter;
	
	/**
	 * Create a new read-only iterator from a mutable iterator.
	 * @param iter The mutable iterator.
	 */
	public ImmutableIterator(Iterator<T> iter)
	{
		if (iter == null) {
			iter = new ArrayList<T>().iterator();
		}
		m_iter = iter;
	}

	/**
	 * Return true iff there are more elements.
	 */
	@Override
	public boolean hasNext()
	{
		return m_iter.hasNext();
	}

	/**
	 * Return the next element.
	 * @throws NoSuchElementException If there are no more elements.
	 */
	@Override
	public T next()
	{
		return m_iter.next();
	}

	/**
	 * Not implemented; cannot remove items from array.
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void remove()
	{
		throw new UnsupportedOperationException(
					"ImmutableIterator<T> does not support remove()");
	}
	
	/**
	 * Return a String with the objects in this set.
	 */
	@Override
	public String toString()
	{
		return m_iter.toString();
	}
}
