package com.wdroome.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 *	Bounded-size LRU Cache utility class.
 *<p>
 *	Specify the maximum number of entries in the constructor,
 *	and (optionally) the initial size and load factor.
 *	After that, client uses just like a LinkedHashMap:
 *	e.g., client calls get() & put().
 *	put() and putAll() add elements to the head of the list,
 *	and if the list is too large, they remove the oldest
 *	items from the tail.  get() moves the returned element
 *	the head of the list.  Iterators do not change the LRU order.
 *<p>
 *	If client needs to cleanup the deleted entry,
 *	create a callback object that implements LRUCache.RemoveCallback,
 *	and call setRemoveCB on that object.  Then we will call that
 *	object whenever we automatically remove an item from the cache.
 */

public class LRUCache<K,V> extends LinkedHashMap<K,V>
{
	private static final long serialVersionUID = 6407140558132004364L;

	public interface RemoveCallback
	{
		void removeEldestEntry(Map.Entry eldest);
	}

	private static final float DEF_LOAD_FACTOR = (float).75;
	private int m_maxSize;
	private RemoveCallback m_removeCB = null;

	public LRUCache(int maxSize)
	{
		super((int)(maxSize/DEF_LOAD_FACTOR), DEF_LOAD_FACTOR, true);
		this.m_maxSize = maxSize;
	}

	public LRUCache(int maxSize, int initSize)
	{
		super(initSize, DEF_LOAD_FACTOR, true);
		this.m_maxSize = maxSize;
	}

	public LRUCache(int maxSize, int initSize, float loadFactor)
	{
		super(initSize, loadFactor, true);
		this.m_maxSize = maxSize;
	}

	public LRUCache(int maxSize, float loadFactor)
	{
		super((int)(maxSize/DEF_LOAD_FACTOR), loadFactor, true);
		this.m_maxSize = maxSize;
	}

	public void setRemoveCB(RemoveCallback removeCB)
	{
		this.m_removeCB = removeCB;
	}
	
	public int maxSize()
	{
		return m_maxSize;
	}

	protected boolean removeEldestEntry(Map.Entry eldest)
	{
		if (size() > m_maxSize) {
			if (m_removeCB != null)
				m_removeCB.removeEldestEntry(eldest);
			return true;
		} else {
			return false;
		}
	}

	/**
	 *	Test driver: create cache, add entries,
	 *	check size bound, check deletion is LRU, etc.
	 */
	public static void main(String[] args)
	{
		LRUCache<String,String> c = new LRUCache<String,String>(5);

		c.put("1", "v1");
		c.put("2", "v2");
		c.put("3", "v3");
		c.put("4", "v4");
		c.put("5", "v5");
		c.put("6", "v6");
		System.out.println("Should have 5 entries, 2-6:\n  " + c);

		c.setRemoveCB(new TestCB());
		c.get("2");
		c.put("7", "v7");
		System.out.println("Accessed '2', added '7'; should remove 3:\n  " + c);

		c.put("8", "v8");
		c.put("9", "v9");
		c.put("10", "v10");
		c.put("11", "v11");
		System.out.println("Added 8-11, should have 7-11:\n  " + c);

		System.out.println("Clearing cache; does it call RemoveCallbacks?");
		c.clear();
		System.out.println("Final cache: " + c);
	}

	private static class TestCB implements LRUCache.RemoveCallback
	{
		public void removeEldestEntry(Map.Entry eldest)
		{
			System.out.println("   Removing '" + eldest.getKey()
					+ "'='" + eldest.getValue() + "'");
		}
	}
}
