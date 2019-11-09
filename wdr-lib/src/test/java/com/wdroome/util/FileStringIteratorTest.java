package com.wdroome.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.io.CharArrayReader;

import org.junit.Test;

import com.wdroome.util.FileStringIterator;

/**
 * @author wdr
 *
 */
public class FileStringIteratorTest
{

	/**
	 * Test method for {@link FileStringIterator#next()}.
	 */
	@Test
	public void testNext()
	{
		String src = " str0   str1  str2\tstr3\nstr4 #comment\nstr5";
		String fname = "file-name";
		String suff = " of file " + fname;
		FileStringIterator iter = new FileStringIterator(
									new CharArrayReader(src.toCharArray()), fname);
		assertEquals("beginning" + suff, iter.getPositionDescription());
		assertEquals("str0", iter.next());
		assertEquals("line 1" + suff, iter.getPositionDescription());
		assertEquals("str1", iter.next());
		assertEquals("line 1" + suff, iter.getPositionDescription());
		assertEquals("str2", iter.next());
		assertEquals("line 1" + suff, iter.getPositionDescription());
		assertEquals("str3", iter.next());
		assertEquals("line 1" + suff, iter.getPositionDescription());
		assertEquals("str4", iter.next());
		assertEquals("line 2" + suff, iter.getPositionDescription());
		assertEquals("str5", iter.next());
		assertEquals("line 3" + suff, iter.getPositionDescription());
		assertTrue(!iter.hasNext());
		assertEquals("end" + suff, iter.getPositionDescription());
	}

}
