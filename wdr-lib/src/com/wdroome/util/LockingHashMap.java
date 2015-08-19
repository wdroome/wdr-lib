package com.wdroome.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Collection;

/**
 * A {@link HashMap} which can be locked to prevent changes.
 * Once locked, the Map cannot be unlocked.
 * The key, value and entry sets are also immutable.
 * This class is not synchronized.
 * The client must provide synchronization until the map is locked;
 * once locked, synchronization is unnecessary.
 * @author wdr
 */
public class LockingHashMap<K,V> extends HashMap<K,V>
{
	private static final long serialVersionUID = 6216273809683383427L;

	private boolean m_isLocked = false;
	
	private Set<Map.Entry<K,V>> m_lockedEntrySet = null;
	private Set<K> m_lockedKeySet = null;
	private Collection<V> m_lockedValueCollection = null;

	/** @see HashMap#HashMap() */
	public LockingHashMap()
	{
	}

	/** @see HashMap#HashMap(int) */
	public LockingHashMap(int initialCapacity)
	{
		super(initialCapacity);
	}

	/** @see HashMap#HashMap(Map) */
	public LockingHashMap(Map m)
	{
		super(m);
	}

	/** @see HashMap#HashMap(int, float) */
	public LockingHashMap(int initialCapacity, float loadFactor)
	{
		super(initialCapacity, loadFactor);
	}
	
	/**
	 * Test if the map is locked.
	 * @return True iff the map is locked.
	 */
	public boolean isLocked()
	{
		return m_isLocked;
	}
	
	/**
	 * Lock the map. Once locked, the map cannot be unlocked.
	 */
	public void lock()
	{
		if (!m_isLocked) {
			m_isLocked = true;
			m_lockedEntrySet = new ImmutableSet<Map.Entry<K, V>>(super.entrySet());
			m_lockedKeySet = new ImmutableSet<K>(super.keySet());
			m_lockedValueCollection = new ImmutableCollection<V>(super.values());
		}
	}

	/* (non-Javadoc)
	 * @see HashMap#keySet()
	 */
	@Override
	public Set<K> keySet()
	{
		return m_isLocked ? m_lockedKeySet : super.keySet();
	}

	/* (non-Javadoc)
	 * @see HashMap#values()
	 */
	@Override
	public Collection<V> values()
	{
		return m_isLocked ? m_lockedValueCollection : super.values();
	}

	/* (non-Javadoc)
	 * @see HashMap#entrySet()
	 */
	@Override
	public Set<Entry<K, V>> entrySet()
	{
		return m_isLocked ? m_lockedEntrySet : super.entrySet();
	}

	/**
	 * @see HashMap#put(Object, Object)
	 * @throws IllegalStateException If map is locked.
	 */
	@Override
	public V put(K key, V value)
	{
		if (m_isLocked) {
			throw new IllegalStateException("HashMap.put(): Map is locked");
		}
		return super.put(key, value);
	}

	/**
	 * @see HashMap#putAll(Map)
	 * @throws IllegalStateException If map is locked.
	 */
	@Override
	public void putAll(Map<? extends K, ? extends V> m)
	{
		if (m_isLocked) {
			throw new IllegalStateException("HashMap.putAll(): Map is locked");
		}
		super.putAll(m);
	}

	/**
	 * @see HashMap#remove(Object)
	 * @throws IllegalStateException If map is locked.
	 */
	@Override
	public V remove(Object key)
	{
		if (m_isLocked) {
			throw new IllegalStateException("HashMap.remove(): Map is locked");
		}
		return super.remove(key);
	}

	/**
	 * @see HashMap#clear()
	 * @throws IllegalStateException If map is locked.
	 */
	@Override
	public void clear()
	{
		if (m_isLocked) {
			throw new IllegalStateException("HashMap.clear(): Map is locked");
		}
		super.clear();
	}
}
