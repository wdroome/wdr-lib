package test.junit.util;

import com.wdroome.util.StringUtils;

import static org.junit.Assert.*;

import org.junit.Test;

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
