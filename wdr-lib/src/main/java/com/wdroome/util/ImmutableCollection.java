package com.wdroome.util;

import java.util.Collection;
import java.util.Iterator;

/**
 * A read-only interface to an existing collection.
 * This is useful when a class wants to return a collection to a client,
 * but doesn't want the client to modify that collection,
 * and doesn't want the overhead of cloning the collection.
 * The underlying collection is an argument to the c'tor.
 * This class implements all the Set methods.
 * The "read" methods just call the corresponding methods in the underlying collection,
 * while the "modify" methods throw an UnsupportedOperationException.
 * @author wdr
 */
public class ImmutableCollection<E> implements Collection<E>
{
	private final Collection<E> m_collection;
	
	/**
	 * Create a read-only interface to an existing set.
	 * @param collection The underlying collection.
	 */
	public ImmutableCollection(Collection<E> collection)
	{
		m_collection = collection;
	}

	/* (non-Javadoc)
	 * @see Collection#size()
	 */
	@Override
	public int size()
	{
		return m_collection.size();
	}

	/* (non-Javadoc)
	 * @see Collection#isEmpty()
	 */
	@Override
	public boolean isEmpty()
	{
		return m_collection.isEmpty();
	}

	/* (non-Javadoc)
	 * @see Collection#contains(Object)
	 */
	@Override
	public boolean contains(Object o)
	{
		return m_collection.contains(o);
	}

	/* (non-Javadoc)
	 * @see Collection#iterator()
	 */
	@Override
	public Iterator<E> iterator()
	{
		return new ImmutableIterator<E>(m_collection.iterator());
	}

	/* (non-Javadoc)
	 * @see Collection#toArray()
	 */
	@Override
	public Object[] toArray()
	{
		return m_collection.toArray();
	}

	/* (non-Javadoc)
	 * @see Collection#toArray(Object[])
	 */
	@Override
	public <T> T[] toArray(T[] a)
	{
		return m_collection.toArray(a);
	}

	/* (non-Javadoc)
	 * @see Collection#add(Object)
	 */
	@Override
	public boolean add(E e)
	{
		throw new UnsupportedOperationException(
				"ImmutableCollection<E> does not support add()");
	}

	/* (non-Javadoc)
	 * @see Collection#remove(Object)
	 */
	@Override
	public boolean remove(Object o)
	{
		throw new UnsupportedOperationException(
				"ImmutableCollection<E> does not support remove()");
	}

	/* (non-Javadoc)
	 * @see Collection#containsAll(Collection)
	 */
	@Override
	public boolean containsAll(Collection<?> c)
	{
		return m_collection.containsAll(c);
	}

	/* (non-Javadoc)
	 * @see Collection#addAll(Collection)
	 */
	@Override
	public boolean addAll(Collection<? extends E> c)
	{
		throw new UnsupportedOperationException(
				"ImmutableCollection<E> does not support addAll()");
	}

	/* (non-Javadoc)
	 * @see Collection#removeAll(Collection)
	 */
	@Override
	public boolean removeAll(Collection<?> c)
	{
		throw new UnsupportedOperationException(
				"ImmutableCollection<E> does not support removeAll()");
	}

	/* (non-Javadoc)
	 * @see Collection#retainAll(Collection)
	 */
	@Override
	public boolean retainAll(Collection<?> c)
	{
		throw new UnsupportedOperationException(
				"ImmutableCollection<E> does not support retainAll()");
	}

	/* (non-Javadoc)
	 * @see Collection#clear()
	 */
	@Override
	public void clear()
	{
		throw new UnsupportedOperationException(
				"ImmutableCollection<E> does not support clear()");
	}
}
