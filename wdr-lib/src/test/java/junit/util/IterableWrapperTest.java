package test.junit.util;

import java.util.ArrayList;
import java.util.Iterator;

import static org.junit.Assert.*;

import org.junit.Test;

import com.wdroome.util.IterableWrapper;

/**
 * @author wdr
 *
 */
public class IterableWrapperTest
{
	
	private static ArrayList<String> list = new ArrayList<String>();
	static {
		list.add("String1");
		list.add("String2");
	}

	@Test
	public void test1()
	{
		int i = 0;
		for (String actual:new IterableWrapper<String>(list.iterator())) {
			assertEquals("test1[" + i + "]", list.get(i), actual);
			i++;
		}
		assertEquals("test1 end", i, list.size());
		assertEquals("test1 toString",
					 new IterableWrapper<String>(list.iterator()).toString(),
					 "[String1,String2]");
	}

	@Test
	public void test2()
	{
		int i = 0;
		for (String actual:new IterableWrapper<String>(list)) {
			assertEquals("test2[" + i + "]", list.get(i), actual);
			i++;
		}
		assertEquals("test2 end", i, list.size());
		assertEquals("test2 toString",
				 new IterableWrapper<String>(list).toString(),
				 "[String1,String2]");
	}
	
	@Test
	public void test3()
	{
		int i = 0;
		for (@SuppressWarnings("unused")
				String actual: new IterableWrapper<String>((ArrayList<String>)null)) {
			i++;
		}
		assertEquals("test3 end", i, 0);
		assertEquals("test3 toString",
				 new IterableWrapper<String>((ArrayList<String>)null).toString(),
				 "[]");
	}
	
	@Test
	public void test4()
	{
		int i = 0;
		for (@SuppressWarnings("unused")
				String actual: new IterableWrapper<String>((Iterator<String>)null)) {
			i++;
		}
		assertEquals("test3 end", i, 0);
		assertEquals("test3 toString",
				 new IterableWrapper<String>((Iterator<String>)null).toString(),
				 "[]");
	}

	public static void main(String[] args)
	{
		System.out.println("iterator toString: " + new IterableWrapper<String>(list.iterator()).toString());
		System.out.println("iterable toString: " + new IterableWrapper<String>(list).toString());
		System.out.println("iterable toString: " + new IterableWrapper<String>((ArrayList<String>)null).toString());
		System.out.println("iterable toString: " + new IterableWrapper<String>((Iterator<String>)null).toString());
	}
}
