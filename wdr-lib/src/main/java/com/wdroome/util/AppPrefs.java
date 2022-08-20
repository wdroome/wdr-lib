package com.wdroome.util;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.List;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Set;
import java.util.HashSet;

/**
 * Access the application preferences via the Java Preference package.
 * @author wdr
 */
public class AppPrefs
{
	// Node for all app preferences. Never null.
	private final Preferences m_appPrefs;

	/**
	 * Create the preference node for the application.
	 * @param appPathName The root pathname for the application's preferences.
	 * 		E.g., /com/mydomain/myapp.
	 */
	public AppPrefs(String appPathName)
	{
		m_appPrefs = Preferences.userRoot().node(appPathName);
	}
	
	/**
	 * Remove all preferences for this app.
	 */
	public void clear()
	{
		try {
			m_appPrefs.clear();
			flush();
		} catch (BackingStoreException e) {
			// What else?
		}
	}

	/**
	 * Return the value of a String preference.
	 * @param name The preference name.
	 * @param def The default value if the property does not exist.
	 * @return The preference value, or def if it does not exist.
	 */
	public String get(String name, String def)
	{
		return get(name, null, def);
	}

	/**
	 * Return the value of a String preference.
	 * @param name The preference name.
	 * @param propName The name of a system property with the default value, or null.
	 * @param def The default value if the property does not exist.
	 * @return The preference value, or def if it does not exist.
	 */
	public String get(String name, String propName, String def)
	{
		if (propName != null && !propName.equals("")) {
			def = System.getProperty(propName, def);
		}
		return m_appPrefs.get(name, def);
	}

	/**
	 * Set a String preference value.
	 * @param name The preference name.
	 * @param value The new value, or null or "" to delete the current value.
	 */
	public void put(String name, String value)
	{
		if (value != null && !value.equals("")) {
			m_appPrefs.put(name, value);
		} else {
			m_appPrefs.remove(name);
		}
		flush();
	}

	/**
	 * Return the value of a boolean preference.
	 * @param name The preference name.
	 * @param def The default return value.
	 * @return The preference value, or def if it does not exist.
	 */
	public boolean getBoolean(String name, boolean def)
	{
		return getBoolean(name, null, def);
	}

	/**
	 * Return the value of a boolean preference.
	 * @param name The preference name.
	 * @param propName The name of a system property with the default value, or null.
	 * @param def The default return value.
	 * @return The preference value, or def if it does not exist.
	 */
	public boolean getBoolean(String name, String propName, boolean def)
	{
		if (propName != null && !propName.equals("")) {
			String pv = System.getProperty(propName, null);
			if (pv != null && !pv.trim().equals("")) {
				if (pv.trim().toLowerCase().startsWith("t")) {
					def = true;
				} else if (pv.trim().toLowerCase().startsWith("f")) {
					def = false;
				}
			}
		}
		return m_appPrefs.getBoolean(name, def);
	}

	/**
	 * Set a boolean preference value.
	 * @param name The preference name.
	 * @param value The new value.
	 */
	public void putBoolean(String name, boolean value)
	{
		m_appPrefs.putBoolean(name, value);
		flush();
	}

	/**
	 * Return the value of an int preference.
	 * @param name The preference name.
	 * @param def The default return value.
	 * @return The preference value, or def if it does not exist.
	 */
	public int getInt(String name, int def)
	{
		return m_appPrefs.getInt(name, def);
	}

	/**
	 * Return the value of an int preference.
	 * @param name The preference name.
	 * @param propName The name of a system property with the default value, or null.
	 * @param def The default return value.
	 * @return The preference value, or def if it does not exist.
	 */
	public int getInt(String name, String propName, int def)
	{
		if (propName != null && !propName.equals("")) {
			String pv = System.getProperty(propName, null);
			if (pv != null && !pv.trim().equals("")) {
				try {
					def = Integer.parseInt(pv.trim());
				} catch (Exception e) {
					// Ignore
				}
			}
		}
		return m_appPrefs.getInt(name, def);
	}

	/**
	 * Set an int preference value.
	 * @param name The preference name.
	 * @param value The new value.
	 */
	public void putInt(String name, int value)
	{
		m_appPrefs.putInt(name, value);
		flush();
	}

	/**
	 * Return the value of a long preference.
	 * @param name The preference name.
	 * @param def The default return value.
	 * @return The preference value, or def if it does not exist.
	 */
	public long getLong(String name, long def)
	{
		return m_appPrefs.getLong(name, def);
	}

	/**
	 * Return the value of a long preference.
	 * @param name The preference name.
	 * @param propName The name of a system property with the default value, or null.
	 * @param def The default return value.
	 * @return The preference value, or def if it does not exist.
	 */
	public long getLong(String name, String propName, long def)
	{
		if (propName != null && !propName.equals("")) {
			String pv = System.getProperty(propName, null);
			if (pv != null && !pv.trim().equals("")) {
				try {
					def = Long.parseLong(pv.trim());
				} catch (Exception e) {
					// Ignore
				}
			}
		}
		return m_appPrefs.getLong(name, def);
	}

	/**
	 * Set a long preference value.
	 * @param name The preference name.
	 * @param value The new value.
	 */
	public void putLong(String name, long value)
	{
		m_appPrefs.putLong(name, value);
		flush();
	}
	
	/**
	 * Return the names of the preferences in this set.
	 * Any item whose name ends with .## is assumed to be a list preference.
	 * @return The names of the available preference items.
	 */
	public Set<String> prefNames()
	{
		HashSet<String> names = new HashSet<String>();
		try {
			for (String s: m_appPrefs.keys()) {
				names.add(s.replaceAll("\\.[0-9]+$", "."));
			}
		} catch (BackingStoreException e) {
			// What else?
		}
		return names;
	}
	
	/**
	 * Get a list-valued preference.
	 * The Preferences class does not support Lists (as far as I can tell),
	 * so a list-valued preference is a set of scalar preferences
	 * named foo.0, foo.1, foo.2, etc.
	 * @param namePrefix The prefix for this preference list.
	 * 		Normally ends in ".".
	 * @return A List with the values. If none, return an empty list.
	 * @see #putList(String, List)
	 */
	public List<String> getList(String namePrefix)
	{
		return getList(namePrefix, null, null, new ArrayList<String>());
	}
	
	/**
	 * Get a list-valued preference.
	 * The Preferences class does not support Lists (as far as I can tell),
	 * so a list-valued preference is a set of scalar preferences
	 * named foo.0, foo.1, foo.2, etc.
	 * @param namePrefix The prefix for this preference list.
	 * 		Normally ends in ".".
	 * @param propName The name of a system property with the default value, or null.
	 * @param splitRegex The regular expression used to split the property
	 * 		into list elements. If null or "", split on whitepace, commas and semi-colons.
	 * @param def The default value if the property does not exist.
	 * @return A List with the values. If none, return the property list or def.
	 * @see #putList(String, List)
	 */
	public List<String> getList(String namePrefix, String propName, String splitRegex, List<String> def)
	{
		ArrayList<String> values = new ArrayList<>();
		TreeMap<Integer,String> indexes = new TreeMap<>();
		try {
			for (String key: m_appPrefs.keys()) {
				if (key.startsWith(namePrefix)) {
					try {
						indexes.put(Integer.parseInt(key.substring(namePrefix.length())), key);
					} catch (Exception e) {
						// Ignore that key.
					}
				}
			}
		} catch (BackingStoreException e) {
			// What else can we do??
		}
		for (String key: indexes.values()) {
			String v = m_appPrefs.get(key,  null);
			if (v != null && !v.equals("")) {
				values.add(v);
			}
		}
		
		if (values.isEmpty()) {
			if (propName != null && !propName.equals("")) {
				String pv = System.getProperty(propName);
				if (pv != null && !pv.equals("")) {
					if (splitRegex == null || splitRegex.equals("")) {
						splitRegex = "[ \t\n\r,;]+";
					}
					try {
						for (String s: pv.split(splitRegex)) {
							values.add(s);
						}
					} catch (Exception e) {
						// Ignore errors in regex.
					}
				}
			}
		}
		
		return !values.isEmpty() ? values : def;
	}
	
	/**
	 * Set a list-valued preference. Discards all elements in the current list.
	 * @param namePrefix The prefix for this preference list.
	 * 		Normally ends in ".".
	 * @param values The new values. If null or empty, just delete the current list.
	 * @see #getList(String)
	 */
	public void putList(String namePrefix, List<String> values)
	{
		try {
			for (String key: m_appPrefs.keys()) {
				if (key.startsWith(namePrefix)) {
					m_appPrefs.remove(key);
				}
			}
		} catch (BackingStoreException e) {
		}
		if (values != null) {
			for (int i = 0; i < values.size(); i++) {
				m_appPrefs.put(namePrefix + i, values.get(i));
			} 
		}
		flush();
	}
	
	private void flush()
	{
		try {
			m_appPrefs.flush();
		} catch (BackingStoreException e) {
			// Ignore??
		}		
	}
}