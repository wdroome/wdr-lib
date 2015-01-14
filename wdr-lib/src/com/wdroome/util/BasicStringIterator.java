package com.wdroome.util;

import java.util.Iterator;
import java.util.ArrayList;

/**
 * An immutable iterator over a collection of Strings
 * that provides the index of the current element on request.
 * @author wdr
 */
public class BasicStringIterator implements IteratorWithPosition<String>
{
	private final Iterator<String> m_iterator;
	private int m_index = 0;
	
	/**
	 * Create a new iterator from an existing string iterator.
	 * @param base
	 */
	public BasicStringIterator(Iterable<String> base)
	{
		if (base == null) {
			base = new ArrayList<String>();
		}
		m_iterator = base.iterator();
	}
	
	/**
	 * Create a new iterator by splitting a source string into tokens.
	 * @param source The source string.
	 * @param splitPattern The split pattern. If null, use whitespace.
	 */
	public BasicStringIterator(String source, String splitPattern)
	{
		if (splitPattern == null || splitPattern.equals("")) {
			splitPattern = "[ \t\n\r]+";
		}
		m_iterator = new ArrayIterator<String>(
					(source != null) ? source.split(splitPattern) : new String[0]);
	}
	
	/**
	 * Create a new iterator from the white-space-separated tokens
	 * in a source string. This is equivalent to
	 * {@link #BasicStringIterator(String,String) BasicStringIterator(source,null)}.
	 * @param source The source string.
	 */
	public BasicStringIterator(String source)
	{
		this(source, null);
	}
	
	/**
	 * Return this object.
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<String> iterator()
	{
		return this;
	}

	/**
	 * Return true if there is a next element.
	 * @see java.util.Iterator#hasNext()
	 */
	@Override
	public boolean hasNext()
	{
		return m_iterator.hasNext();
	}

	/**
	 * Return the next element, or else throw NoSuchElementException.
	 * @see java.util.Iterator#next()
	 */
	@Override
	public String next()
	{
		String r = m_iterator.next();
		m_index++;
		return r;
	}

	/**
	 * Unsupported. Always throws UnsupportedOperationException.
	 * @see java.util.Iterator#remove()
	 */
	@Override
	public void remove()
	{
		throw new UnsupportedOperationException(
					"BasicStringIterator does not support remove()");
	}

	/**
	 * Return a string with the index of the most recently returned element.
	 * @see IteratorWithPosition#getPositionDescription()
	 */
	@Override
	public String getPositionDescription()
	{
		return "item[" + (m_index-1) + "]";
	}
}
