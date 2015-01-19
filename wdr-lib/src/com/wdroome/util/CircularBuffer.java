package com.wdroome.util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 *	Implement a bounded-size circular buffer.
 *	You can add object at the end, clear the buffer,
 *	and get an array of all elements,
 *	Once the buffer hits its size limit,
 *	adding a new object at the end
 *	removes the eldest object.
 *<p>
 *	This class is not synchronized.
 */
public class CircularBuffer<T> implements Iterable<T>
{
	private final LinkedList<T> m_list;
	private final int m_maxSize;
	private int m_curSize = 0;

	/**
	 *	Create an empty circular buffer.
	 *
	 *	@param maxSize The maximum size of the buffer.
	 *	@throws IllegalArgumentException If maxSize isn't positive.
	 */
	public CircularBuffer(int maxSize)
	{
		if (maxSize <= 0) {
			throw new IllegalArgumentException("CircularBuffer: maxSize must be > 0");
		}
		this.m_maxSize = maxSize;
		m_list = new LinkedList<T>();
	}

	/**
	 *	Add a new object to the end of the buffer.
	 *	If the buffer is full, discard the oldest object.
	 */
	public void add(T obj)
	{
		if (m_curSize >= m_maxSize) {
			m_list.removeFirst();
		} else {
			m_curSize++;
		}
		m_list.add(obj);
	}

	/**
	 *	Clear the list.
	 */
	public void clear()
	{
		m_list.clear();
		m_curSize = 0;
	}

	/**
	 *	Return the list as an array.
	 *	The first element is the oldest.
	 *	If a.length is greater than the current size,
	 *	copy the buffer's items into "a", padding the end with nulls,
	 *	and return "a".
	 *	If a.length is less than the current size,
	 *	create a new array and return it.
	 */
	public <T2> T[] toArray(T[] a)
	{
		return m_list.toArray(a);
	}

	/**
	 *	Return the number of items in the buffer.
	 */
	public int size()
	{
		return m_curSize;
	}

	/**
	 *	Return the maximum number of items in the buffer.
	 */
	public int limit()
	{
		return m_maxSize;
	}

	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<T> iterator()
	{
		return new IterableWrapper<T>(m_list.iterator()).iterator();
	}

	/**
	 *	Test driver: add some strings, then get the array.
	 */
	public static void main(String[] args)
	{
		CircularBuffer<String> b = new CircularBuffer<String>(5);
		int n = 6;
		System.out.println("Add " + n + " strings to a buffer of length 5.");
		for (int i = 1; i <= n; i++) {
			b.add("s" + i);
		}
		System.out.println("Dump array:");
		String[] arr = b.toArray(new String[0]);
		for (int i = 0; i < arr.length; i++) {
			System.out.println("[" + i + "]: \"" + arr[i] + "\"");
		}
	}
}
