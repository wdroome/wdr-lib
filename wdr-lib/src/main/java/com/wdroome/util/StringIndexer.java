package com.wdroome.util;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Map a set of Strings to integer indexes, and vice versa.
 * Indexes run from 0 to N-1 -- eg, array indexes.
 * Strings can be added dynamically, but they can't be deleted.
 * @author wdr
 */
public class StringIndexer
{
	// Map string to index.
	private final Map<String,Integer> m_stringToIndex = new HashMap<String,Integer>();
	
	// Map index to string.
	private final ArrayList<String> m_indexToString = new ArrayList<String>();
	
	// Array of strings, in index order. Created when requested.
	private String[] m_strings = null;
	
	/** Create an empty indexer. */
	public StringIndexer() {}
	
	/**
	 * Return the number of strings in this table.
	 * @return The number of strings in this table.
	 */
	public int size()
	{
		return m_indexToString.size();
	}
	
	/**
	 * Return the index of a string.
	 * @param s The string.
	 * @return The index of s (0 to N-1), or -1 if s isn't in the table.
	 */
	public int getIndex(String s)
	{
		Integer i = m_stringToIndex.get(s);
		return (i != null) ? i : -1;
	}
	
	/**
	 * Return the index of a string, adding the string if it's not in the table.
	 * @param s The string.
	 * @return The index of the string (0 to N-1).
	 */
	public int makeIndex(String s)
	{
		Integer i = m_stringToIndex.get(s);
		if (i != null) {
			return i;
		} else {
			int x = m_indexToString.size();
			m_stringToIndex.put(s, x);
			m_indexToString.add(s);
			m_strings = null;
			return x;
		}
	}
	
	/**
	 * Return the string for an index.
	 * @param index The index.
	 * @return The string with that index, or null if index is out of range.
	 */
	public String getString(int index)
	{
		if (index >= 0 && index < m_indexToString.size())
			return m_indexToString.get(index);
		else
			return null;
	}
	
	/**
	 * Return an array with the string names in index order.
	 * CAVEAT: For efficiency, this method returns the same array for each call.
	 * So clients should NOT change elements of the array.
	 * @return An array with the string names in index order.
	 */
	public String[] toArray()
	{
		if (m_strings == null) {
			m_strings = m_indexToString.toArray(new String[m_indexToString.size()]);
		}
		return m_strings;
	}
}
