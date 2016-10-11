package test.junit.util;

import com.wdroome.util.StringUtils;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/**
 * @author wdr
 *
 */
public class StringUtilsTest
{

	@Test
	public void escapeSimpleJSONString_test1()
	{
		assertEquals("1", "abcd", StringUtils.escapeSimpleJSONString("abcd"));
		assertEquals("2", "\\\"ba", StringUtils.escapeSimpleJSONString("\"ba"));
		assertEquals("3", "\\\"ba\\\\", StringUtils.escapeSimpleJSONString("\"ba\\"));
		assertEquals("4", "\\\"ba\\\\\\\\", StringUtils.escapeSimpleJSONString("\"ba\\\\"));
	}

	@Test
	public void removeBackslashes_test1()
	{
		assertEquals("1", "\\", StringUtils.removeBackslashes("\\\\"));
		assertEquals("2", "\"", StringUtils.removeBackslashes("\\\""));
		assertEquals("3", "abc\"def\\\"", StringUtils.removeBackslashes("abc\\\"def\\\\\\\""));
	}

	@Test
	public void removeBackslashes_test2()
	{
		String s = "abcd";
		assertTrue("4", s == StringUtils.removeBackslashes(s));
		s = "127.0.0.0\\/24";
		assertFalse("5", s == StringUtils.removeBackslashes(s));
	}

	@Test
	public void split_test1()
	{
		assertArrayEquals("1", new String[] {"abc", "def", "0123"}, StringUtils.split("abc,def,0123"));
		assertArrayEquals("2", new String[] {""}, StringUtils.split(""));
		assertArrayEquals("3", new String[] {"",""}, StringUtils.split(","));
		assertArrayEquals("4", new String[] {"a,bc","d,ef","0,"}, StringUtils.split("a\\,bc,d\\,ef,0\\,"));
		assertArrayEquals("5", new String[] {"a,bc",""}, StringUtils.split("a\\,bc,"));
		assertArrayEquals("6", new String[] {"a,bc"}, StringUtils.split("a\\,bc"));
		assertArrayEquals("7", new String[] {"abc"}, StringUtils.split("abc"));
		assertArrayEquals("8", new String[] {"abc\\"}, StringUtils.split("abc\\"));
		assertArrayEquals("10", new String[] {"abc\\,","def"}, StringUtils.split("abc\\\\\\,,def"));
		assertArrayEquals("11", new String[] {"abc\\", "def"}, StringUtils.split("abc\\\\,def"));
	}
	
	@Test
	public void removeBackslashes_test3()
	{
		char[] stopChars = new char[] {'/', '='};
		assertArrayEquals("1", new String[] {"abcd"}, StringUtils.escapedSplit("abcd", stopChars));
		assertArrayEquals("2", new String[] {"a\\bc\\d\\/"}, StringUtils.escapedSplit("a\\bc\\d\\/", stopChars));
		assertArrayEquals("3", new String[] {"abcd", "/", "xyz"}, StringUtils.escapedSplit("abcd/xyz", stopChars));
		assertArrayEquals("4", new String[] {"ab\\/cd", "/", "xyz"}, StringUtils.escapedSplit("ab\\/cd/xyz", stopChars));
		assertArrayEquals("5", new String[] {"ab\\/\\=cd", "=", ""}, StringUtils.escapedSplit("ab\\/\\=cd=", stopChars));
	}
	
	@Test
	public void makeStringList_test1()
	{
		List<Object> src = new ArrayList<Object>();
		src.add("Element 1");
		src.add(2);
		src.add("Element 3");
		src.add(null);
		List<String> res = StringUtils.makeStringList(src);
		assertEquals("size", src.size(), res.size());
		for (int i = 0; i < src.size(); i++) {
			String s = (src.get(i) != null) ? src.get(i).toString() : "null";
			assertEquals("["+i+"]", s, res.get(i));
		}
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void makeStringList_test2()
	{
		List src = new ArrayList();
		src.add("Element 1");
		src.add(2);
		src.add("Element 3");
		src.add(null);
		List<String> res = StringUtils.makeStringList(src);
		assertEquals("size", src.size(), res.size());
		for (int i = 0; i < src.size(); i++) {
			String s = (src.get(i) != null) ? src.get(i).toString() : "null";
			assertEquals("["+i+"]", s, res.get(i));
		}
	}
	
	@Test
	public void makeStringSet_test1()
	{
		Set<Object> src = new HashSet<Object>();
		src.add("Element 1");
		src.add(2);
		src.add("Element 3");
		src.add(null);
		Set<String> res = StringUtils.makeStringSet(src);
		assertEquals("size", src.size(), res.size());
		for (Object o: src) {
			String s = (o != null) ? o.toString() : "null";
			assertTrue("'" + s + "':", res.contains(s));
		}
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void makeStringSet_test2()
	{
		Set src = new HashSet<Object>();
		src.add("Element 1");
		src.add(2);
		src.add("Element 3");
		src.add(null);
		Set<String> res = StringUtils.makeStringSet(src);
		assertEquals("size", src.size(), res.size());
		for (Object o: src) {
			String s = (o != null) ? o.toString() : "null";
			assertTrue("'" + s + "':", res.contains(s));
		}
	}
	
	@Test
	public void makeStringTest()
	{
		byte[] buff = new byte[] {'a', 'b', 'c', 0, 0, 'd'};
		assertEquals("0,3", "abc", StringUtils.makeString(buff, 0, 3));
		assertEquals("0,4", "abc", StringUtils.makeString(buff, 0, 4));
		assertEquals("1,3", "bc", StringUtils.makeString(buff, 1, 3));
		assertEquals("1,4", "bc", StringUtils.makeString(buff, 1, 4));
		assertEquals("5,1", "d", StringUtils.makeString(buff, 5, 1));
		assertEquals("3,2", "", StringUtils.makeString(buff, 3, 2));
		assertEquals("1,0", "", StringUtils.makeString(buff, 1, 0));
	}
	
	public static void main(String[] args)
	{
		printArr(StringUtils.split("abc,def,0123"));
		printArr(StringUtils.split("abc\\\\\\,,def"));
		printArr(StringUtils.split("very-funny-name!@#$%^&*()_=<>?\\,.:\";'[]{}|\\\\,mypid1,27"));
	}
	
	public static void printArr(String[] arr)
	{
		for (String s:arr) {
			System.out.print("  \"" + s + "\"");
		}
		System.out.println();
	}
}
