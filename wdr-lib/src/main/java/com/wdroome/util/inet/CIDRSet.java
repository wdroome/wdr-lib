package com.wdroome.util.inet;

import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;
import java.util.Iterator;

import com.wdroome.util.ArrayIterator;

/**
 * A set of CIDRAddress. Initially, the set is mutable:
 * the CIDRs are stored in a Set, and you can add and remove CIDRs.
 * The set can be frozen, or rendered immutable.
 * At that point we copy the CIDRs to a simple array,
 * and you can no longer add or remove CIDRs.
 * Frozen CIDRSets are thread-safe. Mutable CIDRSets are not.
 * We assume the client provides synchronization for mutable CIRDSets.
 * @author wdr
 */
public class CIDRSet implements Iterable<CIDRAddress>
{
	// When the set is "mutable", m_array is null and m_set has the CIDRs.
	// When the set is "frozen", we set m_array to the CIDRs and remove m_set.
	private CIDRAddress[] m_array = null;
	private Set<CIDRAddress> m_set = new HashSet<CIDRAddress>();
	
	// When the set is frozen, this is the final hash code from m_set.
	private int m_hashCode = 0;
	
	/**
	 * Create a new, empty mutable set.
	 */
	public CIDRSet()
	{
	}
	
	/**
	 * Create a new set from list of CIDR specs.
	 * @param src
	 * 		A list of CIDR specifications, separated by commas or white-space.
	 * @throws UnknownHostException
	 *		If any substring is not valid a valid CIDR specification.
	 */
	public CIDRSet(String src) throws UnknownHostException
	{
		String[] arr = src.split("[ \t\n\r,]+");
		for (String s:arr) {
			if (!s.equals("")) {
				add(new CIDRAddress(s));
			}
		}
	}
	
	/**
	 * Create a new set from an array of CIDR specs.
	 * @param cidrs
	 * 		An array of CIDR specifications.
	 * @throws UnknownHostException
	 *		If any element of the array is not valid a valid CIDR specification.
	 */
	public CIDRSet(String[] cidrs) throws UnknownHostException
	{
		for (String s:cidrs) {
			if (s != null && !s.equals("")) {
				add(new CIDRAddress(s));
			}
		}
	}

	/**
	 * Create a new set from an array of CIDRs.
	 * @param cidrs The source CIDRAddresses.
	 */
	public CIDRSet(CIDRAddress[] cidrs)
	{
		for (CIDRAddress cidr:cidrs) {
			add(cidr);
		}
	}
	
	/**
	 * Create a new set from a list of CIDRs.
	 * @param cidrs A list of CIDRs.
	 */
	public CIDRSet(Iterable<CIDRAddress> cidrs)
	{
		for (CIDRAddress cidr:cidrs) {
			add(cidr);
		}
	}
	
	/**
	 * Create a new, unfrozen set with the CIDRs in an existing set.
	 * @param src The source CIDR set.
	 */
	public CIDRSet(CIDRSet src)
	{
		for (CIDRAddress cidr:src) {
			m_set.add(cidr);
		}
	}
	
	/**
	 * Return the number of CIDRs in the set.
	 */
	public int size()
	{
		return (m_array != null) ? m_array.length : m_set.size();
	}
	
	/**
	 * Test if a CIDRSet is empty.
	 * @return True iff this set has no CIDRs.
	 */
	public boolean isEmpty()
	{
		return (m_array != null) ? (m_array.length == 0) : m_set.isEmpty();
	}
	
	/**
	 * Return an Iterator over the CIDRs in the set.
	 */
	@Override
	public Iterator<CIDRAddress> iterator()
	{
		if (m_array != null) {
			return new ArrayIterator<CIDRAddress>(m_array);
		} else {
			return m_set.iterator();
		}
	}
	
	/**
	 * Return an array with the CIDRs. The array is a copy; clients can change the array.
	 */
	public CIDRAddress[] getCIDRs()
	{
		if (m_array == null) {
			return m_set.toArray(new CIDRAddress[m_set.size()]);
		} else {
			CIDRAddress[] ret = new CIDRAddress[m_array.length];
			for (int i = 0; i < ret.length; i++) {
				ret[i] = m_array[i];
			}
			return ret;
		}
	}
	
	/**
	 * Return true if a CIDR is in the set.
	 * @param addr The CIDR to test.
	 * @return True iff "addr" is equal to a CIDR in the set.
	 */
	public boolean contains(CIDRAddress addr)
	{
		if (addr == null) {
			return false;
		} else if (m_array != null) {
			for (CIDRAddress a:m_array) {
				if (a.equals(addr)) {
					return true;
				}
			}
			return false;
		} else {
			return m_set.contains(addr);
		}
	}
	
	/**
	 * Return true if a CIDR is covered by a CIDR in this set.
	 * Consider that a CIDRAddress covers itself.
	 * @param addr The CIDR to test.
	 * @return True if some CIDR in this set covers "addr".
	 */
	public boolean covers(CIDRAddress addr)
	{
		return covers(addr, true);
	}
	
	/**
	 * Return true if a CIDR is covered by a CIDR in this set.
	 * @param addr The CIDR to test.
	 * @param allowEqualCIDRs If true, consider that a CIDRAddress covers itself.
	 * 			If false, CIDRs do not cover themselves.
	 * @return True if some CIDR in this set covers "addr".
	 */
	public boolean covers(CIDRAddress addr, boolean allowEqualCIDRs)
	{
		if (addr == null) {
			return false;
		} else if (m_array != null) {
			for (CIDRAddress a:m_array) {
				if (a.covers(addr) && (allowEqualCIDRs || !a.equals(addr))) {
					return true;
				}
			}
			return false;
		} else {
			for (CIDRAddress a:m_set) {
				if (a.covers(addr) && (allowEqualCIDRs || !a.equals(addr))) {
					return true;
				}
			}
			return false;
		}
	}
	
	/**
	 * Test if this set completely covers another set.
	 * This tests for strict coverage -- a CIDR does not cover itself.
	 * @param cidrs Another CIDRSet.
	 * @return True iff every CIDR in "cidrs" is covered
	 * 		by a CIDR in this set.
	 */
	public boolean fullyCovers(CIDRSet cidrs)
	{
		for (CIDRAddress cidr: cidrs) {
			if (!covers(cidr, false)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Test if this set partially covers another set.
	 * This tests for strict coverage -- a CIDR does not cover itself.
	 * @param cidrs Another CIDRSet.
	 * @return True iff every CIDR in "cidrs" is covered
	 * 		by a CIDR in this set.
	 */
	public boolean partiallyCovers(CIDRSet cidrs)
	{
		for (CIDRAddress cidr: cidrs) {
			if (covers(cidr, false)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Add a new CIDR to the set.
	 * @param cidr The CIDR.
	 * @throws IllegalStateException If the set is frozen.
	 */
	public void add(CIDRAddress cidr)
	{
		if (m_array != null) {
			throw new IllegalStateException("Cannot add CIDRs to a frozen CIDRSet");
		}
		m_set.add(cidr);
	}
	
	/**
	 * Add a CIDR to the set.
	 * @param cidrSpec The string representation of the CIDR.
	 * @throws UnknownHostException If the string isn't a valid CIDR.
	 * @throws IllegalStateException If the set is frozen.
	 */
	public void add(String cidrSpec) throws UnknownHostException
	{
		add(new CIDRAddress(cidrSpec));
	}

	/**
	 * Add a list of CIDR specs to the set.
	 * @param src
	 * 		A list of CIDR specifications, separated by commas or white-space.
	 * @throws UnknownHostException
	 *		If any substring is not valid a valid CIDR specification.
	 * @throws IllegalStateException If the set is frozen.
	 */
	public void addCidrs(String src) throws UnknownHostException
	{
		String[] arr = src.split("[ \t\n\r,]+");
		for (String s:arr) {
			if (!s.equals("")) {
				add(new CIDRAddress(s));
			}
		}
	}
	
	/**
	 * Remove a CIDR from the set.
	 * @param cidr The CIDR to remove.
	 * @return True iff the set contained the CIDR.
	 * @throws IllegalStateException If the set is frozen.
	 */
	public boolean remove(CIDRAddress cidr)
	{
		if (m_array != null) {
			throw new IllegalStateException("Cannot remove CIDRs from a frozen CIDRSet");
		}
		return m_set.remove(cidr);
	}
	
	/**
	 * Freeze the set, but without coalescing CIDRs. This optimizes iterations and storage,
	 * but you can no longer add CIDRs.
	 * If the set is already frozen, quietly do nothing.
	 * @see #freeze(boolean)
	 */
	public void freeze()
	{
		freeze(false);
	}
	
	/**
	 * Freeze the set. This optimizes iterations and storage,
	 * but you can no longer add CIDRs.
	 * If the set is already frozen, quietly do nothing.
	 * @param coalesce If true, remove overlapped CIDRs and combine adjacent CIDRs.
	 * 		This will speed up searches and save space.
	 * 		However, this can be time-consuming, and in many cases it's not necessary,
	 * 		so it's an option.
	 * @return If coalesce is true, return true iff the set was not already frozen
	 * 		and we actually removed or combined CIDRs.
	 * 		If coalesce is false, always return false. 
	 */
	public boolean freeze(boolean coalesce)
	{
		if (m_array != null) {
			return false;
		}
		int nCIDRs = m_set.size();
		if (!coalesce) {
			
			// Simple case, just make the array.
			m_array = m_set.toArray(new CIDRAddress[nCIDRs]);
			m_hashCode = m_set.hashCode();
			m_set = null;
			return false;
		} else {
			
			// Squeeze out any redundancy.
			int nOriginalCIDRs = nCIDRs;
			CIDRAddress[] src = m_set.toArray(new CIDRAddress[nCIDRs]);
			CIDRAddress[] dest = new CIDRAddress[nCIDRs];
			Arrays.sort(src, new CIDRAddress.AddressByteComparator());
			
			// Copy src to dest, skipping any CIDRs that are covered.
			int iDest = 0;			
			for (int iSrc = 0; iSrc < nCIDRs; iSrc++) {
				dest[iDest] = src[iSrc];
				for (iSrc++; iSrc < nCIDRs && dest[iDest].covers(src[iSrc]); iSrc++) {
					;
				}
				iSrc--;
				iDest++;
			}
			// System.out.println("XXX: deleted " + (nCIDRs - iDest) + "/" + nCIDRs + " covered cidrs");
			
			// Combine adjacent CIDRs when possible. Repeat until no changes.
			while (true) {
				CIDRAddress[] tmp;
				tmp = src;
				src = dest;
				dest = tmp;
				nCIDRs = iDest;
				iDest = 0;
				for (int iSrc = 0; iSrc < nCIDRs; iSrc++) {
					CIDRAddress cidr = src[iSrc];
					if (iSrc + 1 < nCIDRs
							&& cidr.getAddressLen() == src[iSrc+1].getAddressLen()
							&& cidr.getMaskLen() > 0
							&& cidr.getMaskLen() == src[iSrc+1].getMaskLen()
							&& cidr.addrBitsMatch(src[iSrc+1], cidr.getMaskLen()-1)) {
						try {
							// System.out.println("XXX: merging " + cidr + " and " + src[iSrc+1]);
							cidr = new CIDRAddress(cidr, cidr.getMaskLen() - 1);
							iSrc++;
						} catch (Exception e) {
							// Shouldn't happen.
						}
					}
					dest[iDest++] = cidr;
				}
				if (iDest == nCIDRs) {
					break;
				}
				nCIDRs = iDest;
			}
			
			// Finish up.
			m_array = new CIDRAddress[nCIDRs];
			for (int i = 0; i < nCIDRs; i++) {
				m_array[i] = dest[i];
			}
			m_hashCode = m_array.hashCode();
			m_set = null;
			return nCIDRs != nOriginalCIDRs;
		}
	}
	
	/**
	 * Return true if the set is frozen.
	 */
	public boolean isFrozen()
	{
		return m_array != null;
	}

	/**
	 * Return a consistent hashcode. If the CIDRSet isn't frozen,
	 * then return the current hashcode of the underlying Set of CIDRs.
	 * If the CIDRSet is frozen, return the hashcode of the Set
	 * at the time the CIDRSet was frozen.
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		if (m_array != null) {
			return m_hashCode;
		} else {
			return m_set.hashCode();
		}
	}

	/**
	 * Return true iff two CIDRSets have the same CIDRs.
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) {
			return true;
		} else if (obj == null) {
			return false;
		} else if (!getClass().isInstance(obj)) {
			return false;
		}
		CIDRSet other = (CIDRSet) obj;
		if (m_array == null && other.m_array == null) {
			return m_set.equals(other.m_set);
		}
		CIDRAddress[] arr = m_array != null
					? m_array : m_set.toArray(new CIDRAddress[m_set.size()]);
		CIDRAddress[] otherArr = other.m_array != null
					? other.m_array : other.m_set.toArray(new CIDRAddress[other.m_set.size()]);
		return CIDRAddress.equivalentArrays(arr, otherArr);
	}

	/**
	 * Return a String[] with the string representations of the CIDRs in this set.
	 * @return
	 * 	A String[] with the string representations of the CIDRs in this set.
	 *	It has one element per CIDR. If the set is empty, it returns a 0-length array.
	 */
	public String[] toStringArray()
	{
		String[] ret = new String[size()];
		int i = 0;
		for (CIDRAddress cidr:this) {
			ret[i++] = cidr.toString();
		}
		return ret;
	}
	
	/**
	 * Return a String with a square-bracket-wrapped, blank-sep list of the CIDRs in this set.
	 */
	@Override
	public String toString()
	{
		StringBuilder b = new StringBuilder();
		b.append('[');
		String sep = "";
		for (CIDRAddress cidr: this) {
			b.append(sep);
			b.append(cidr.toString());
			sep = " ";
		}
		b.append(']');
		return b.toString();
	}
	
	/**
	 * Like {@link #toString()}, but gives the CIDRs in sorted order.
	 */
	public String toSortedString()
	{
		CIDRAddress[] cidrs = (m_array != null)
						? m_array.clone() : m_set.toArray(new CIDRAddress[size()]);
		Arrays.sort(cidrs);
		StringBuilder b = new StringBuilder();
		b.append('[');
		String sep = "";
		for (CIDRAddress cidr:cidrs) {
			b.append(sep);
			b.append(cidr.toString());
			sep = " ";
		}
		b.append(']');
		return b.toString();
	}
}
