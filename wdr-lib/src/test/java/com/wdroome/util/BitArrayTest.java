package com.wdroome.util;

import static org.junit.Assert.*;

import org.junit.Test;

import com.wdroome.util.BitArray;

/**
 * @author wdr
 */
public class BitArrayTest
{
	@Test
	public void test1()
	{
		BitArray bits = new BitArray(128);
		for (int i = 0; i < bits.size(); i++) {
			boolean prev = bits.set(i, true);
			assertEquals("setting prev " + i, false, prev);
			assertEquals("setting get " + i, true, bits.isSet(i));
			assertEquals("setting allClear" + i, false, bits.allClear());
		}
		for (int i = 0; i < bits.size(); i++) {
			boolean prev = bits.set(i, false);
			assertEquals("clearing prev " + i, true, prev);
			assertEquals("clearing get " + i, false, bits.isSet(i));
			assertEquals("clearing allClear" + i, i+1 >= bits.size(), bits.allClear());
		}
	}
	
	@Test
	public void test2()
	{
		BitArray bits = new BitArray(128);
		for (int i = 1; i < bits.size(); i += 2) {
			int prev = bits.set(i, 1);
			assertEquals("setting odd prev " + i, 0, prev);
			assertEquals("setting odd get " + i, 1, bits.get(i));
			assertEquals("setting odd allClear" + i, false, bits.allClear());
		}
		for (int i = 0; i < bits.size(); i++) {
			assertEquals("scanning odd " + i, i&1, bits.get(i));
		}
		for (int i = 0; i < bits.size(); i += 2) {
			int prev = bits.set(i, 1);
			assertEquals("setting even prev " + i, 0, prev);
			assertEquals("setting even get " + i, 1, bits.get(i));
			assertEquals("setting even allClear" + i, false, bits.allClear());
		}
		for (int i = 0; i < bits.size(); i++) {
			assertEquals("scanning all " + i, 1, bits.get(i));
		}
		for (int i = 1; i < bits.size(); i += 2) {
			int prev = bits.set(i, 0);
			assertEquals("clearing odd prev " + i, 1, prev);
			assertEquals("clearing odd get " + i, 0, bits.get(i));
			assertEquals("clearing odd allClear" + i, false, bits.allClear());
		}
		for (int i = 0; i < bits.size(); i++) {
			assertEquals("scanning odd " + i, ((i&1) == 0) ? 1 : 0, bits.get(i));
		}
		for (int i = 0; i < bits.size(); i += 2) {
			int prev = bits.set(i, 0);
			assertEquals("clearing even prev " + i, 1, prev);
			assertEquals("clearing even get " + i, 0, bits.get(i));
			assertEquals("clearing even allClear" + i, i+2 >= bits.size(), bits.allClear());
		}
		for (int i = 0; i < bits.size(); i++) {
			assertEquals("scanning all " + i, 0, bits.get(i));
		}
	}
}
