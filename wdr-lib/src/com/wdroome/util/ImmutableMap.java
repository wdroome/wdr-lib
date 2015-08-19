package com.wdroome.util;

import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.HashMap;

/**
 * A read-only interface to an existing {@link Map}.
 * This is useful when a class wants to return a map to a client,
 * but doesn't want the client to modify that map,
 * and doesn't want the overhead of cloning the map.
 * The underlying map is an argument to the c'tor.
 * This class implements all the Map methods.
 * The "read" methods just call the corresponding methods in the underlying map,
 * while the "modify" methods throw an UnsupportedOperationException.
 * @author wdr
 */
public class ImmutableMap<K,V> implements Map<K,V>
{
	private final Map<K,V> m_map;

	/**
	 * Create a new Immutable Map from an existing map.
	 */
	public ImmutableMap(Map<K,V> map)
	{
		m_map = map != null ? map : new HashMap<K,V>();
	}

	/* (non-Javadoc)
	 * @see Map#size()
	 */
	@Override
	public int size()
	{
		return m_map.size();
	}

	/* (non-Javadoc)
	 * @see Map#isEmpty()
	 */
	@Override
	public boolean isEmpty()
	{
		return m_map.isEmpty();
	}

	/* (non-Javadoc)
	 * @see Map#containsKey(java.lang.Object)
	 */
	@Override
	public boolean containsKey(Object key)
	{
		return m_map.containsKey(key);
	}

	/* (non-Javadoc)
	 * @see Map#containsValue(java.lang.Object)
	 */
	@Override
	public boolean containsValue(Object value)
	{
		return m_map.containsValue(value);
	}

	/* (non-Javadoc)
	 * @see Map#get(java.lang.Object)
	 */
	@Override
	public V get(Object key)
	{
		return m_map.get(key);
	}

	/* (non-Javadoc)
	 * @see Map#keySet()
	 */
	@Override
	public Set<K> keySet()
	{
		return new ImmutableSet<K>(m_map.keySet());
	}

	/* (non-Javadoc)
	 * @see Map#values()
	 */
	@Override
	public Collection<V> values()
	{
		return new ImmutableCollection<V>(m_map.values());
	}

	/* (non-Javadoc)
	 * @see Map#entrySet()
	 */
	@Override
	public Set<Entry<K, V>> entrySet()
	{
		return new ImmutableSet<Map.Entry<K,V>>(m_map.entrySet());
	}

	/**
	 * @see Map#put(Object, Object)
	 * @throws UnsupportedOperationException
	 */
	@Override
	public V put(K key, V value)
	{
		throw new UnsupportedOperationException(
				"ImmutableMap<K,V> does not support put()");
	}

	/**
	 * @see Map#putAll(Map)
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void putAll(Map<? extends K, ? extends V> m)
	{
		throw new UnsupportedOperationException(
				"ImmutableMap<K,V> does not support putAll()");
	}

	/**
	 * @see Map#remove(Object)
	 * @throws UnsupportedOperationException
	 */
	@Override
	public V remove(Object key)
	{
		throw new UnsupportedOperationException(
				"ImmutableMap<K,V> does not support remove()");
	}

	/**
	 * @see Map#clear()
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void clear()
	{
		throw new UnsupportedOperationException(
				"ImmutableMap<K,V> does not support clear()");
	}
}
