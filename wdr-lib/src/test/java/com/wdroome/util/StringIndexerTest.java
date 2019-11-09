package com.wdroome.util;

import static org.junit.Assert.*;

import org.junit.Test;

import com.wdroome.util.StringIndexer;

/**
 * @author wdr
 */
public class StringIndexerTest
{
	@Test
	public void testMakeIndex()
	{
		StringIndexer ndxr = new StringIndexer();
		assertEquals("add 0", 0, ndxr.makeIndex("hello"));
		assertEquals("add 1", 1, ndxr.makeIndex("there"));
		assertEquals("add 2", 2, ndxr.makeIndex("y'all"));
		assertEquals("add 0", 0, ndxr.makeIndex("hello"));
		assertEquals("add 1", 1, ndxr.makeIndex("there"));
		assertEquals("add 2", 2, ndxr.makeIndex("y'all"));
		assertEquals("size 3", 3, ndxr.size());
		assertEquals("get 0", "hello", ndxr.getString(0));
		assertEquals("get 1", "there", ndxr.getString(1));
		assertEquals("get 2", "y'all", ndxr.getString(2));
		assertEquals("get 3", null, ndxr.getString(3));
		assertEquals("get hello", 0, ndxr.getIndex("hello"));
		assertEquals("get there", 1, ndxr.getIndex("there"));
		assertEquals("get y'all", 2, ndxr.getIndex("y'all"));
		assertEquals("get foobar", -1, ndxr.getIndex("foobar"));
		assertArrayEquals("toArray",
					new String[] {"hello", "there", "y'all"},
					ndxr.toArray());
	}
}
