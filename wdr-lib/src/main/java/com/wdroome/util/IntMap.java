package com.wdroome.util;

import java.util.HashMap;

/**
 * A Map from keys to primitive ints, with a method to increment the int value.
 * Unmapped keys have a default value, zero unless overriden in the constructor.
 * @param <K> The key type.
 * @author wdr
 */
public class IntMap<K> extends HashMap<K,Integer>
{
	// The standard default value.
	public static final int DEF_VALUE = 0;
	
	// This Map's default value.
	private int m_defValue = DEF_VALUE;
	
	/**
	 * Create a new map with an initial capacity and a specific default value.
	 * @param initCapacity The initial capacity.
	 * @param defValue THe default value.
	 */
	public IntMap(int initSize, int defValue)
	{
		super(initSize);
		m_defValue = defValue;
	}
	
	/**
	 * Create a new map with an initial capacity and the standard default value.
	 * @param initCapacity The initial capacity.
	 */
	public IntMap(int initCapacity)
	{
		this(initCapacity, DEF_VALUE);
	}

	/**
	 * Create a new map with the standard default value.
	 */
	public IntMap()
	{
		super();
	}
	
	/**
	 * Get the current value or the default.
	 * @param key The key.
	 * @return The current value, or the default if not set.
	 */
	public int iget(K key)
	{
		return get(key, m_defValue);
	}
	
	/**
	 * Get the current value or the default.
	 * @param key The key.
	 * @return The current value, or the default if not set.
	 */
	@Override
	public Integer get(Object key)
	{
		Integer v = super.get(key);
		return v != null ? v : m_defValue;
	}
	
	/**
	 * Get the current value, or "def" if not set.
	 * @param key The key.
	 * @param def The default it there is no value for key.
	 * @return The current value, or "def" if not set.
	 */
	public int get(K key, int def)
	{
		Integer v = super.get(key);
		return v != null ? v : m_defValue;
	}
	
	/**
	 * Replace the current value.
	 * @param key The key.
	 * @param v THe new value.
	 * @return The previous value.
	 */
	public int put(K key, int v)
	{
		Integer prev = super.get(key);
		super.put(key, v);
		return prev != null ? prev : m_defValue;
	}
	
	/**
	 * Add to an existing value.
	 * @param key The key.
	 * @param delta The change.
	 * @return New value: previous value plus delta.
	 */
	public int add(K key, int delta)
	{
		int v = iget(key) + delta;
		put(key, v);
		return v;
	}
}
