package com.wdroome.util;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/**
 * Find the Enum constant that matches a string. This allows unique prefix matching
 * and alternate names for enum members.
 * @author wdr
 *
 * @param <E> The Enum on which this EnumFinder operates.
 */
public class EnumFinder<E extends Enum<E>>
{
	/**
	 * An interface which the Enum E can implement to define alternative values
	 * for some or all of the enum constants.
	 * 
	 * Note: The alternate names should be unique!
	 */
	@FunctionalInterface
	public interface AltNames
	{
		/**
		 * Return the alternate names, if any, for this enum constant.
		 * @return The alternate names, if any, for this enum constant. This can return null.
		 */
		public String[] altNames();
	}
	
	private final E[] m_values;
	private final boolean m_ignoreCase;
	
	/**
	 * Create a new finder for an enum.
	 * @param values The values of the enum, that is, E.values(). As far as I can tell,
	 * 		at least as of Java 17, a generic class cannot determine the values of an Enum. 
	 * @param ignoreCase If true, ignore case when matching. If false, matching is case sensitive.
	 */
	public EnumFinder(E[] values, boolean ignoreCase)
	{
		if (values == null) {
			throw new IllegalArgumentException("EnumFinder.c'tor: values area is required.");
		}
		m_values = values;
		m_ignoreCase = ignoreCase;
	}
	
	/**
	 * Create a new case-insensitive finder for an enum.
	 * @param values The values of the enum, that is, E.values(). As far as I can tell,
	 * 		at least as of Java 17, a generic class cannot determine the values of an Enum. 
	 */
	public EnumFinder(E[] values)
	{
		this(values, true);
	}
	
	/**
	 * Find the best match for a string. This is either an exact match of the enum constant
	 * (ignoring case if configured in the c'tor), or an exact match of an alternative name.
	 * Otherwise if the string prefix-matches only one enum constant or alternative,
	 * return that enum. If there is no match, the base class calls {@link #noMatch(String)}
	 * and returns the value it returns (default is null).
	 * @param findNameArg The string to match against the enum constants.
	 * @return The best match, or null (or the value returned by {@link #noMatch(String)}
	 * 		if there isn't a match.
	 */
	public E find(String findNameArg)
	{
		E prefixMatch = null;
		int numPrefixMatches = 0;
		if (findNameArg != null && !findNameArg.isBlank()) {
			String findName = normalizeCase(findNameArg);
			for (E val: m_values) {
				String valName = normalizeCase(val.name());
				String[] altNames = (val instanceof AltNames)
								? ((AltNames)val).altNames() : null;
				if (valName.equals(findName)) {
					return val;
				}
				if (altNames != null) {
					for (String altName: altNames) {
						if (altName != null) {
							if (normalizeCase(altName).equals(findName)) {
								return val;
							}
						}
					}
				}
				if (valName.startsWith(findName)) {
					prefixMatch = val;
					numPrefixMatches++;
				} else if (altNames != null) {
					for (String altName: altNames) {
						if (altName != null) {
							if (normalizeCase(altName).startsWith(findName)) {
								prefixMatch = val;
								numPrefixMatches++;
								break;
							}
						}
					}
				}
			}
		}
		if (numPrefixMatches == 1) {
			return prefixMatch;
		} else {
			return noMatch(findNameArg);
		}
	}
	
	/**
	 * Find all enum constants which match a string, including prefix matches.
	 * @param findNameArg The string to match against the enum constants.
	 * @return All enum constants which match or prefix match the string.
	 * 		This never returns null; if there are no matches, this returns an empty set.
	 */
	public Set<E> findMatches(String findName)
	{
		Set<E> matches = new HashSet<>();
		if (findName != null && !findName.isBlank()) {
			findName = normalizeCase(findName);
			for (E val: m_values) {
				String valName = normalizeCase(val.name());
				if (valName.startsWith(findName)) {
					matches.add(val);
				} else if (val instanceof AltNames) {
					String[] altNames = ((AltNames)val).altNames();
					if (altNames != null) {
						for (String altName: altNames) {
							if (altName != null) {
								altName = normalizeCase(altName);
								if (altName.startsWith(findName)) {
									matches.add(val);
									break;
								}
							}
						}
					}
				}
			}
		}
		return matches;
	}
	
	private String normalizeCase(String name)
	{
		return (name != null && m_ignoreCase) ? name.toLowerCase() : name;
	}
	
	/**
	 * {@ #find(String)} calls this method if no enum constant matches the string,
	 * and returns the value returned by this method.
	 * The base class version returns null.
	 * A child class can override this to print an error message
	 * and/or return a different no-match default.
	 * @param findName The name which doesn't match an enum constant.
	 * @return The constant to return, or null. The base class returns null.
	 */
	public E noMatch(String findName)
	{
		return null;
	}
}
