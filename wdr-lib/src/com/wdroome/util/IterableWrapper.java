package com.wdroome.util;

import java.util.Iterator;
import java.util.HashSet;

/**
 * A wrapper class that lets a client access the Iterable interface
 * of an object, but nothing else.
 * For example, suppose your class has private Set.
 * You want to let clients iterate over the members of the Set.
 * But if you give clients the Set, they can use put() and remove()
 * to change it. However, if you give clients a new IterableWrapper(mySet),
 * the clients can iterate over the elements, but cannot change the set.
 */
public class IterableWrapper<T> implements Iterable<T>
{
	private final Iterable<T> m_iterable;
	
	private final Iterator<T> m_iterator;
	
	/**
	 * Create a wrapper for an Iterable object.
	 * @param iter The wrapped object.
	 */
	public IterableWrapper(Iterable<T> iter)
	{
		m_iterable = iter;
		m_iterator = null;
	}
	
	/**
	 * Create a wrapper from an Iterator object.
	 * @param iter The iterator.
	 */
	public IterableWrapper(Iterator<T> iter)
	{
		m_iterator = iter;
		m_iterable = null;
	}
	
	/**
	 * Iterable&lt;T&gt; Interface: Return a new iterator for the wrapped object.
	 * The returned iterator is immutable: it rejects remove() requests.
	 */
	@Override
	public Iterator<T> iterator()
	{
		if (m_iterator != null) {
			return new ImmutableIterator<T>(m_iterator);
		} else if (m_iterable != null) {
			return new ImmutableIterator<T>(m_iterable.iterator());
		} else {
			return new ImmutableIterator<T>(new HashSet<T>().iterator());
		}
	}
	
	/**
	 * Return a String with the objects in this set.
	 */
	@Override
	public String toString()
	{
		StringBuilder b = new StringBuilder();
		b.append('[');
		int n = 0;
		for (T object: this) {
			if (n > 0) {
				b.append(',');
			}
			b.append(object);
			n++;
		}
		b.append(']');
		return b.toString();
	}
}