package test.junit.util;

import static org.junit.Assert.*;

import org.junit.Test;

import com.wdroome.util.BasicStringIterator;
import java.util.ArrayList;

/**
 * @author wdr
 *
 */
public class BasicStringIteratorTest
{
	/**
	 * Test method for {@link com.lucent.sird.alto.util.BasicStringIterator#next()}.
	 */
	@Test
	public void testListCtor()
	{
		ArrayList<String> list = new ArrayList<String>();
		list.add("item0");
		list.add("item1");
		BasicStringIterator iter = new BasicStringIterator(list);
		assertEquals("item[-1]", iter.getPositionDescription());
		assertEquals("item0", iter.next());
		assertEquals("item[0]", iter.getPositionDescription());
		assertEquals("item1", iter.next());
		assertEquals("item[1]", iter.getPositionDescription());
		assertTrue(!iter.hasNext());
	}
	
	/**
	 * Test method for {@link com.lucent.sird.alto.util.BasicStringIterator#BasicStringIterator(String)}.
	 */
	@Test
	public void testStringCtor()
	{
		BasicStringIterator iter = new BasicStringIterator("item0 item1   item2 \t\n\r  item3\n");
		assertEquals("item[-1]", iter.getPositionDescription());
		assertEquals("item0", iter.next());
		assertEquals("item[0]", iter.getPositionDescription());
		assertEquals("item1", iter.next());
		assertEquals("item[1]", iter.getPositionDescription());
		assertEquals("item2", iter.next());
		assertEquals("item[2]", iter.getPositionDescription());
		assertEquals("item3", iter.next());
		assertEquals("item[3]", iter.getPositionDescription());
		assertTrue(!iter.hasNext());
	}
	
	/**
	 * Test method for {@link com.lucent.sird.alto.util.BasicStringIterator#BasicStringIterator(String)}.
	 */
	@Test
	public void testNullListCtor()
	{
		BasicStringIterator iter = new BasicStringIterator((ArrayList<String>)null);
		assertEquals("item[-1]", iter.getPositionDescription());
		assertTrue(!iter.hasNext());
	}
	
	/**
	 * Test method for {@link com.lucent.sird.alto.util.BasicStringIterator#BasicStringIterator(String)}.
	 */
	@Test
	public void testNullStringCtor()
	{
		BasicStringIterator iter = new BasicStringIterator((String)null);
		assertEquals("item[-1]", iter.getPositionDescription());
		assertTrue(!iter.hasNext());
	}
}
