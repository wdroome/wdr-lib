package com.wdroome.util;

import java.util.HashMap;

/**
 * An associative array of counters.
 * That is, a Map whose values are integers.
 * This class extends get(key) to return 0 if the key does not exist,
 * and adds an incr(key,delta) method to increment a counter.
 * Note that the methods are not synchronized,
 * and this class is not thread safe.
 * @author wdr
 */
public class HashCounter<K> extends HashMap<K, Integer>
{
	private static final long serialVersionUID = 4136346933298970500L;
	
	private static final Integer ZERO = new Integer(0);
	
	/**
	 * Create a new Hash Counter.
	 */
	public HashCounter()
	{
		super();
	}
	
	/**
	 * Create a new Hash Counter.
	 * @param initialCapacity The initial capacity.
	 */
	public HashCounter(int initialCapacity)
	{
		super(initialCapacity);
	}
	
	/**
	 * Create a new Hash Counter.
	 * @param initialCapacity The initial capacity.
	 * @param loadFactor The load factor.
	 */
	public HashCounter(int initialCapacity, float loadFactor)
	{
		super(initialCapacity, loadFactor);
	}
	
	/**
	 * Construct a new HashCounter with the same counts as the specified HashCounter.
	 * The HashCounter is created with default load factor (0.75)
	 * and an initial capacity sufficient to hold the mappings in the specified HashCounter. 
	 * @param src The source counters.
	 */
	public HashCounter(HashCounter<K> src)
	{
		super(src);
	}
	
	/**
	 * Return the number of occurrences of a key,
	 * or zero if there are none.
	 * @param key The key for the counter.
	 * @return The number of occurrences of key,
	 *		or zero if there are none.
	 *		This does not create a counter for key
	 *		if one does not already exist.
	 */
	@Override
	public Integer get(Object key)
	{
		Integer v = super.get(key);
		if (v != null) {
			return v;
		} else {
			return ZERO;
		}
	}
	
	/**
	 * Increment the current value of a counter.
	 * If the counter does not exist yet,
	 * initialize it to zero.
	 * @param key The key for the counter.
	 * @param delta The increment.
	 * @return The new value of the counter, after the increment.
	 */
	public Integer incr(K key, int delta)
	{
		Integer val = get(key) + delta;
		put(key, val);
		return val;
	}
}
