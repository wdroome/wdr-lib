package test.junit.util;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

import com.wdroome.util.ArrayIterator;

/**
 * @author wdr
 */
public class ArrayIteratorTest
{
	@Test
	public void test1()
	{
		String[] testList = {"String1", "String2"};
		String[] expectedList = {"String1", "String2"};
		int i = 0;
		ArrayIterator<String> iter = new ArrayIterator<String>(testList);
		assertEquals("test1[-1] position", "item[-1]", iter.getPositionDescription());
		for (String actual: iter) {
			assertEquals("test1[" + i + "]", expectedList[i], actual);
			assertEquals("test1[" + i + "] position", "item[" + i + "]",
							iter.getPositionDescription());
			i++;
		}
		assertEquals("test1[$] position", "item[" + (testList.length-1) + "]", iter.getPositionDescription());
		assertEquals("test1 end", i, expectedList.length);
		
		try { iter.next(); } catch (Exception e) {}
		assertEquals("test1[$] position", "item[" + testList.length + "]", iter.getPositionDescription());		
		
		while (iter.hasPrevious()) {
			String actual = iter.previous();
			--i;
			assertEquals("test1[" + i + "]", expectedList[i], actual);
			assertEquals("test1[" + i + "] position", "item[" + i + "]",
							iter.getPositionDescription());
		}
		assertEquals("test1[0] position", "item[0]", iter.getPositionDescription());
		assertEquals("test1 end", i, 0);
	}

	@Test
	public void test2()
	{
		String[] testList = {"String1", null, null, "String2", null};
		String[] expectedList = {"String1", null, null, "String2", null};
		int i = 0;
		ArrayIterator<String> iter = new ArrayIterator<String>(testList);
		assertEquals("test2[-1] position", "item[-1]", iter.getPositionDescription());
		for (String actual: iter) {
			if (expectedList[i] == null) {
				assertTrue("test2[" + i + "]", actual == null);
			} else {
				assertEquals("test2[" + i + "]", expectedList[i], actual);
			}
			assertEquals("test2[" + i + "] position", "item[" + i + "]",
							iter.getPositionDescription());
			i++;
		}
		assertEquals("test2[$] position", "item[" + (testList.length-1) + "]", iter.getPositionDescription());
		assertEquals("test2 end", i, expectedList.length);
	}
	
	@Test
	public void test3()
	{
		String[] testList = {"String1", null, null, "String2", null};
		String[] expectedList = {"String1", "String2"};
		String[] expectedPosition = {"item[0]", "item[3]"};
		int i = 0;
		ArrayIterator<String> iter = new ArrayIterator<String>(testList, 0, -1, true);
		assertEquals("test3[-1] position", "item[-1]", iter.getPositionDescription());
		for (String actual: iter) {
			assertEquals("test3[" + i + "]", expectedList[i], actual);
			assertEquals("test3[" + i + "] position", expectedPosition[i],
							iter.getPositionDescription());
			i++;
		}
		assertEquals("test3[$] position", "item[" + 3 + "]", iter.getPositionDescription());
		assertEquals("test3 end", i, expectedList.length);
	}
	
	public void test4()
	{
		String[] expectedList = {};
		int i = 0;
		ArrayIterator<String> iter = new ArrayIterator<String>(null);
		assertEquals("test4[-1] position", "item[-1]", iter.getPositionDescription());
		for (String actual: iter) {
			assertEquals("test4[" + i + "]", expectedList[i], actual);
			assertEquals("test4[" + i + "] position", "item[" + i + "]",
							iter.getPositionDescription());
			i++;
		}
		assertEquals("test4[$] position", "item[-1]", iter.getPositionDescription());
		assertEquals("test4 end", i, expectedList.length);
	}
	
	@Test
	public void test5()
	{
		String[] testList = {};
		String[] expectedList = {};
		int i = 0;
		ArrayIterator<String> iter = new ArrayIterator<String>(testList);
		assertEquals("test5[-1] position", "item[-1]", iter.getPositionDescription());
		for (String actual: iter) {
			assertEquals("test5[" + i + "]", expectedList[i], actual);
			assertEquals("test5[" + i + "] position", "item[" + i + "]",
							iter.getPositionDescription());
			i++;
		}
		assertEquals("test5[$] position", "item[" + (testList.length-1) + "]", iter.getPositionDescription());
		assertEquals("test5 end", i, expectedList.length);
	}
	
	@Test
	public void test6()
	{
		String[] testList = {null, null};
		String[] expectedList = {};
		String[] expectedPosition = {};
		int i = 0;
		ArrayIterator<String> iter = new ArrayIterator<String>(testList, 0, -1, true);
		assertEquals("test6[-1] position", "item[-1]", iter.getPositionDescription());
		for (String actual: iter) {
			assertEquals("test6[" + i + "]", expectedList[i], actual);
			assertEquals("test6[" + i + "] position", expectedPosition[i],
							iter.getPositionDescription());
			i++;
		}
		assertEquals("test6[$] position", "item[" + -1 + "]", iter.getPositionDescription());
		assertEquals("test6 end", i, expectedList.length);
	}
}
