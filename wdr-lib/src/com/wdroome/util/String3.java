package com.wdroome.util;

/**
 * A simple struct with three Strings.
 * The strings are immutable and are set by the c'tor;
 * for simplicity, clients access them as read-only public members.
 * The class implements equals() and hashCode(), so it can be used as a Map key.
 * @author wdr
 */
public class String3 implements Comparable<String3>
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
	 * The third string.
	 */
	public final String m_str3;
	
	/**
	 * The hash code -- set by c'tor.
	 */
	private final int m_hashCode;
	
	/**
	 * Create an instance.
	 * @param str1 The first string (may be null).
	 * @param str2 The second string (may be null).
	 * @param str3 The third string (may be null).
	 */
	public String3(String str1, String str2, String str3)
	{
		m_str1 = str1;
		m_str2 = str2;
		m_str3 = str3;

		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((m_str3 == null) ? 0 : m_str3.hashCode());
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
		String3 other = (String3) obj;
		
		if (m_str3 == null) {
			if (other.m_str3 != null)
				return false;
		} else if (!m_str3.equals(other.m_str3))
			return false;
		
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
	public int compareTo(String3 other)
	{
		int v = m_str1.compareTo(other.m_str1);
		if (v != 0)
			return v;
		v = m_str2.compareTo(other.m_str2);
		if (v != 0)
			return v;
		return m_str3.compareTo(other.m_str3);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "<" + m_str1 + "," + m_str2 + "," + m_str3 + ">";
	}
}
