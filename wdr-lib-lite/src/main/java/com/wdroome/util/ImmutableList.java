package com.wdroome.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A read-only interface to an existing list.
 * This is useful when a class wants to return a list to a client,
 * but doesn't want the client to modify that list,
 * and doesn't want the overhead of cloning the list.
 * The underlying list is an argument to the c'tor.
 * This class implements all the List methods.
 * The "read" methods just call the corresponding methods in the underlying list,
 * while the "modify" methods throw an UnsupportedOperationException.
 * @author wdr
 */
public class ImmutableList<E> implements List<E>
{
	private final List<E> m_list;

	/**
	 * Create a read-only interface to an existing list.
	 * @param list The underlying list.
	 */
	public ImmutableList(List<E> list)
	{
		m_list = list;
	}

	/* (non-Javadoc)
	 * @see java.util.List#size()
	 */
	@Override
	public int size()
	{
		return m_list.size();
	}

	/* (non-Javadoc)
	 * @see java.util.List#isEmpty()
	 */
	@Override
	public boolean isEmpty()
	{
		return m_list.isEmpty();
	}

	/* (non-Javadoc)
	 * @see java.util.List#contains(java.lang.Object)
	 */
	@Override
	public boolean contains(Object o)
	{
		return m_list.contains(o);
	}

	/* (non-Javadoc)
	 * @see java.util.List#iterator()
	 */
	@Override
	public Iterator<E> iterator()
	{
		return new ImmutableIterator<E>(m_list.iterator());
	}

	/* (non-Javadoc)
	 * @see java.util.List#toArray()
	 */
	@Override
	public Object[] toArray()
	{
		return m_list.toArray();
	}

	/* (non-Javadoc)
	 * @see java.util.List#toArray(java.lang.Object[])
	 */
	@Override
	public <T> T[] toArray(T[] a)
	{
		return m_list.toArray(a);
	}

	/* (non-Javadoc)
	 * @see java.util.List#add(java.lang.Object)
	 * @throws UnsupportedOperationException
	 */
	@Override
	public boolean add(E e)
	{
		throw new UnsupportedOperationException(
				"ImmutableList<E> does not support add()");
	}

	/* (non-Javadoc)
	 * @see java.util.List#remove(java.lang.Object)
	 * @throws UnsupportedOperationException
	 */
	@Override
	public boolean remove(Object o)
	{
		throw new UnsupportedOperationException(
				"ImmutableList<E> does not support remove()");
	}

	/* (non-Javadoc)
	 * @see java.util.List#containsAll(java.util.Collection)
	 */
	@Override
	public boolean containsAll(Collection<?> c)
	{
		return m_list.containsAll(c);
	}

	/* (non-Javadoc)
	 * @see java.util.List#addAll(java.util.Collection)
	 * @throws UnsupportedOperationException
	 */
	@Override
	public boolean addAll(Collection<? extends E> c)
	{
		throw new UnsupportedOperationException(
				"ImmutableList<E> does not support addAll()");
	}

	/* (non-Javadoc)
	 * @see java.util.List#addAll(int, java.util.Collection)
	 * @throws UnsupportedOperationException
	 */
	@Override
	public boolean addAll(int index, Collection<? extends E> c)
	{
		throw new UnsupportedOperationException(
				"ImmutableList<E> does not support addAll()");
	}

	/* (non-Javadoc)
	 * @see java.util.List#removeAll(java.util.Collection)
	 * @throws UnsupportedOperationException
	 */
	@Override
	public boolean removeAll(Collection<?> c)
	{
		throw new UnsupportedOperationException(
				"ImmutableList<E> does not support removeAll()");
	}

	/* (non-Javadoc)
	 * @see java.util.List#retainAll(java.util.Collection)
	 * @throws UnsupportedOperationException
	 */
	@Override
	public boolean retainAll(Collection<?> c)
	{
		throw new UnsupportedOperationException(
				"ImmutableList<E> does not support retainAll()");
	}

	/* (non-Javadoc)
	 * @see java.util.List#clear()
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void clear()
	{
		throw new UnsupportedOperationException(
				"ImmutableList<E> does not support clear()");
	}

	/* (non-Javadoc)
	 * @see java.util.List#get(int)
	 */
	@Override
	public E get(int index)
	{
		return m_list.get(index);
	}

	/* (non-Javadoc)
	 * @see java.util.List#set(int, java.lang.Object)
	 * @throws UnsupportedOperationException
	 */
	@Override
	public E set(int index, E element)
	{
		throw new UnsupportedOperationException(
				"ImmutableList<E> does not support set(int,E)");
	}

	/* (non-Javadoc)
	 * @see java.util.List#add(int, java.lang.Object)
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void add(int index, E element)
	{
		throw new UnsupportedOperationException(
				"ImmutableList<E> does not support add(int,E)");
	}

	/* (non-Javadoc)
	 * @see java.util.List#remove(int)
	 * @throws UnsupportedOperationException
	 */
	@Override
	public E remove(int index)
	{
		throw new UnsupportedOperationException(
				"ImmutableList<E> does not support remove(int)");
	}

	/* (non-Javadoc)
	 * @see java.util.List#indexOf(java.lang.Object)
	 */
	@Override
	public int indexOf(Object o)
	{
		return m_list.indexOf(o);
	}

	/* (non-Javadoc)
	 * @see java.util.List#lastIndexOf(java.lang.Object)
	 */
	@Override
	public int lastIndexOf(Object o)
	{
		return m_list.lastIndexOf(o);
	}

	/* (non-Javadoc)
	 * @see java.util.List#listIterator()
	 */
	@Override
	public ListIterator<E> listIterator()
	{
		return new ImmutableListIterator<E>(m_list.listIterator());
	}

	/* (non-Javadoc)
	 * @see java.util.List#listIterator(int)
	 */
	@Override
	public ListIterator<E> listIterator(int index)
	{
		return new ImmutableListIterator<E>(m_list.listIterator(index));
	}

	/* (non-Javadoc)
	 * @see java.util.List#subList(int, int)
	 */
	@Override
	public List<E> subList(int fromIndex, int toIndex)
	{
		return new ImmutableList<E>(m_list.subList(fromIndex, toIndex));
	}
}
