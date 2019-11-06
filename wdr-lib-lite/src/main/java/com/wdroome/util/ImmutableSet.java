package com.wdroome.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

/**
 * A read-only interface to an existing set.
 * This is useful when a class wants to return a set to a client,
 * but doesn't want the client to modify that set,
 * and doesn't want the overhead of cloning the set.
 * The underlying set is an argument to the c'tor.
 * This class implements all the Set methods.
 * The "read" methods just call the corresponding methods in the underlying set,
 * while the "modify" methods throw an UnsupportedOperationException.
 * @author wdr
 */
public class ImmutableSet<E> implements Set<E>
{
	private final Set<E> m_set;
	
	/**
	 * Create a read-only interface to an existing set.
	 * @param set The underlying set.
	 */
	public ImmutableSet(Set<E> set)
	{
		if (set == null) {
			set = new HashSet<E>();
		}
		m_set = set;
	}
	
	/* (non-Javadoc)
	 * @see java.util.Set#add(java.lang.Object)
	 */
	@Override
	public boolean add(E arg)
	{
		throw new UnsupportedOperationException(
				"ImmutableSet<E> does not support add()");
	}

	/* (non-Javadoc)
	 * @see java.util.Set#addAll(java.util.Collection)
	 */
	@Override
	public boolean addAll(Collection<? extends E> arg)
	{
		throw new UnsupportedOperationException(
				"ImmutableSet<E> does not support addAll()");
	}

	/* (non-Javadoc)
	 * @see java.util.Set#clear()
	 */
	@Override
	public void clear()
	{
		throw new UnsupportedOperationException(
				"ImmutableSet<E> does not support clear()");
	}

	/* (non-Javadoc)
	 * @see java.util.Set#contains(java.lang.Object)
	 */
	@Override
	public boolean contains(Object arg)
	{
		return m_set.contains(arg);
	}

	/* (non-Javadoc)
	 * @see java.util.Set#containsAll(java.util.Collection)
	 */
	@Override
	public boolean containsAll(Collection<?> arg)
	{
		return m_set.containsAll(arg);
	}

	/* (non-Javadoc)
	 * @see java.util.Set#isEmpty()
	 */
	@Override
	public boolean isEmpty()
	{
		return m_set.isEmpty();
	}

	/* (non-Javadoc)
	 * @see java.util.Set#iterator()
	 */
	@Override
	public Iterator<E> iterator()
	{
		return new ImmutableIterator<E>(m_set.iterator());
	}

	/* (non-Javadoc)
	 * @see java.util.Set#remove(java.lang.Object)
	 */
	@Override
	public boolean remove(Object arg)
	{
		throw new UnsupportedOperationException(
				"ImmutableSet<E> does not support remove()");
	}

	/* (non-Javadoc)
	 * @see java.util.Set#removeAll(java.util.Collection)
	 */
	@Override
	public boolean removeAll(Collection<?> arg)
	{
		throw new UnsupportedOperationException(
				"ImmutableSet<E> does not support removeAll()");
	}

	/* (non-Javadoc)
	 * @see java.util.Set#retainAll(java.util.Collection)
	 */
	@Override
	public boolean retainAll(Collection<?> arg)
	{
		throw new UnsupportedOperationException(
				"ImmutableSet<E> does not support retainAll()");
	}

	/* (non-Javadoc)
	 * @see java.util.Set#size()
	 */
	@Override
	public int size()
	{
		return m_set.size();
	}

	/* (non-Javadoc)
	 * @see java.util.Set#toArray()
	 */
	@Override
	public Object[] toArray()
	{
		return m_set.toArray();
	}

	/* (non-Javadoc)
	 * @see java.util.Set#toArray(T[])
	 */
	@Override
	public <T> T[] toArray(T[] arg)
	{
		return m_set.toArray(arg);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return m_set.toString();
	}
	
	@Override
	public boolean equals(Object other)
	{
		return m_set.equals(other);
	}
	
	@Override
	public int hashCode()
	{
		return m_set.hashCode();
	}
}
