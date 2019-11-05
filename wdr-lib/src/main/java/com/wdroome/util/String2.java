package com.wdroome.util;

/**
 * A simple struct with two Strings.
 * The strings are immutable and are set by the c'tor;
 * for simplicity, clients access them as read-only public members.
 * The class implements equals() and hashCode(), so it can be used as a Map key.
 * @author wdr
 */
public class String2 implements Comparable<String2>
{
	/**
	 * The first string.
	 */
	public final String m_str1;
	
	/**
	 * The second string.
	 */
	public final String m_str2;
	
	/**
	 * The hash code -- set by c'tor.
	 */
	private final int m_hashCode;
	
	/**
	 * Create an instance.
	 * @param str1 The first string (may be null).
	 * @param str2 The second string (may be null).
	 */
	public String2(String str1, String str2)
	{
		m_str1 = str1;
		m_str2 = str2;

		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((m_str2 == null) ? 0 : m_str2.hashCode());
		result = prime * result
				+ ((m_str1 == null) ? 0 : m_str1.hashCode());
		m_hashCode = result;
	}
	
	@Override
	public int hashCode()
	{
		return m_hashCode;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!getClass().isInstance(obj))
			return false;
		String2 other = (String2) obj;
		
		if (m_str2 == null) {
			if (other.m_str2 != null)
				return false;
		} else if (!m_str2.equals(other.m_str2))
			return false;
		
		if (m_str1 == null) {
			if (other.m_str1 != null)
				return false;
		} else if (!m_str1.equals(other.m_str1))
			return false;
		
		return true;
	}

	/**
	 * Compare on the strings, starting with the first string.
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(String2 other)
	{
		int v = m_str1.compareTo(other.m_str1);
		if (v != 0)
			return v;
		return m_str2.compareTo(other.m_str2);
	}

	/**
	 * Return a string representation of the pair of strings.
	 * @return The two strings, as &lt;S1,S2&gt;.
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "<" + m_str1 + "," + m_str2 + ">";
	}
}
