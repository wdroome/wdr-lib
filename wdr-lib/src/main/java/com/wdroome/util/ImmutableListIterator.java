package com.wdroome.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.concurrent.Future;

/**
 * A read-only list iterator wrapper class.
 * For example, suppose you want to let clients iterate
 * over the elements of a private list, but you don't want
 * clients to remove elements. To prevent that, create a new
 * ImmutableListIterator, giving the list's list iterator to the c'tor,
 * and then give the ImmutableListIterator to the client.
 * The ImmutableListIterator passes hasNext() and next() to the set's list iterator,
 * but throws an exception if the client calls add().
 * @author wdr
 */
public class ImmutableListIterator<E> extends ImmutableIterator<E>
		implements ListIterator<E>
{
	private final ListIterator<E> m_iter;
	
	/**
	 * Create a new read-only list iterator from a mutable list iterator.
	 * @param iter The mutable list iterator.
	 */
	public ImmutableListIterator(ListIterator<E> iter)
	{
		super(iter);
		if (iter == null) {
			iter = new ArrayList<E>().listIterator();
		}
		m_iter = iter;
	}

	/* (non-Javadoc)
	 * @see java.util.ListIterator#hasPrevious()
	 */
	@Override
	public boolean hasPrevious()
	{
		return m_iter.hasPrevious();
	}

	/* (non-Javadoc)
	 * @see java.util.ListIterator#previous()
	 */
	@Override
	public E previous()
	{
		return m_iter.previous();
	}

	/* (non-Javadoc)
	 * @see java.util.ListIterator#nextIndex()
	 */
	@Override
	public int nextIndex()
	{
		return m_iter.nextIndex();
	}

	/* (non-Javadoc)
	 * @see java.util.ListIterator#previousIndex()
	 */
	@Override
	public int previousIndex()
	{
		return m_iter.previousIndex();
	}

	/* (non-Javadoc)
	 * @see java.util.ListIterator#set(java.lang.Object)
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void set(E e)
	{
		throw new UnsupportedOperationException(
				"ImmutableListIterator<T> does not support set()");
	}

	/* (non-Javadoc)
	 * @see java.util.ListIterator#add(java.lang.Object)
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void add(E e)
	{
		throw new UnsupportedOperationException(
				"ImmutableListIterator<T> does not support add()");
	}
}
