package com.wdroome.util.inet;

import java.net.UnknownHostException;

/**
 * A CIDRSet with a name. The name is fixed when the object is created.
 * @author wdr
 */
public class NamedCIDRSet extends CIDRSet
{
	/** The set's name. */
	public final String m_name;
	
	/**
	 * Create a new named set.
	 * @param name The set's name.
	 */
	public NamedCIDRSet(String name)
	{
		m_name = name;
	}
	
	/**
	 * Create a new named set from an array of CIDR specs.
	 * @param name The set's name.
	 * @param cidrs An array of CIDR specifications.
	 * @throws UnknownHostException
	 *		If any element of the array is not valid a valid CIDR specification.
	 */
	public NamedCIDRSet(String name, String[] cidrs) throws UnknownHostException
	{
		super(cidrs);
		m_name = name;
	}

	/**
	 * Create a new named set from an array of CIDRs.
	 * @param name The set's name.
	 * @param cidrs The source CIDRAddresses.
	 */
	public NamedCIDRSet(String name, CIDRAddress[] cidrs)
	{
		super(cidrs);
		m_name = name;
	}
	
	/**
	 * Create a new named set from an list of CIDRs.
	 * @param name The set's name.
	 * @param cidrs The source CIDRAddresses.
	 */
	public NamedCIDRSet(String name, Iterable<CIDRAddress> cidrs)
	{
		super(cidrs);
		m_name = name;
	}
	
	/**
	 * Return a string with the set's name and (sorted) CIDRs.
	 */
	@Override
	public String toString()
	{
		return m_name + super.toSortedString();
	}

	/**
	 * Return a hash code, using the set's name and the CIDRs in the set.
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((m_name == null) ? 0 : m_name.hashCode());
		return result;
	}

	/**
	 * Return true if this set has the same name and the same CIDRs as another named set.
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) {
			return true;
		} else if (!super.equals(obj)) {
			return false;
		} else if (getClass() != obj.getClass()) {
			return false;
		}
		NamedCIDRSet other = (NamedCIDRSet) obj;
		if (m_name == null) {
			if (other.m_name != null) {
				return false;
			}
		} else if (!m_name.equals(other.m_name)) {
			return false;
		}
		return true;
	}
}
