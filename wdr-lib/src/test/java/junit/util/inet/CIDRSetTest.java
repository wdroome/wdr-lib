package test.junit.util.inet;

import java.net.*;
import java.util.*;

import com.wdroome.util.inet.CIDRAddress;
import com.wdroome.util.inet.CIDRSet;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author wdr
 */
public class CIDRSetTest
{

	/**
	 * Test method for {@link CIDRSet#CIDRSet(java.lang.String)}.
	 * @throws UnknownHostException 
	 */
	@Test
	public void testCIDRSetString() throws UnknownHostException
	{
		CIDRSet cidrs = new CIDRSet("  1.2.3.0/24 1.2.92.0/24  1.2.128.0/24,1.3.1.1/24 ");
		String[] expected = new String[] {
				"1.2.128.0/24",
				"1.2.3.0/24",
				"1.2.92.0/24",
				"1.3.1.0/24"
		};
		int i = 0;
		String[] actual = new String[4];
		for (CIDRAddress cidr:cidrs) {
			actual[i++] = cidr.toString();
		}
		Arrays.sort(actual);
		assertArrayEquals("iterator", expected, actual);
	}

	/**
	 * Test method for {@link CIDRSet#addCidrs(java.lang.String)}.
	 * @throws UnknownHostException 
	 */
	@Test
	public void testAddCidrsString() throws UnknownHostException
	{
		CIDRSet cidrs = new CIDRSet();
		cidrs.addCidrs("  1.2.3.0/24 1.2.92.0/24  1.2.128.0/24,1.3.1.1/24 ");
		String[] actual = cidrs.toStringArray();
		Arrays.sort(actual);
		String[] expected = new String[] {
				"1.2.128.0/24",
				"1.2.3.0/24",
				"1.2.92.0/24",
				"1.3.1.0/24"
		};
		assertArrayEquals("add1", expected, actual);
		cidrs.addCidrs("  1.2.3.0/24 1.0.24.128/25  1.2.128.0/24,1.3.1.1/24 ");
		actual = cidrs.toStringArray();
		Arrays.sort(actual);
		expected = new String[] {
				"1.0.24.128/25",
				"1.2.128.0/24",
				"1.2.3.0/24",
				"1.2.92.0/24",
				"1.3.1.0/24"
		};
		assertArrayEquals("add2", expected, actual);
	}

	/**
	 * Test method for {@link CIDRSet#toStringArray()}.
	 * @throws UnknownHostException 
	 */
	@Test
	public void testToStringArray() throws UnknownHostException
	{
		CIDRSet cidrs = new CIDRSet("  1.2.3.0/24 1.2.92.0/24  1.2.128.0/24,1.3.1.1/24 ");
		String[] actual = cidrs.toStringArray();
		Arrays.sort(actual);
		String[] expected = new String[] {
				"1.2.128.0/24",
				"1.2.3.0/24",
				"1.2.92.0/24",
				"1.3.1.0/24"
		};
		assertArrayEquals("toStringArray", expected, actual);
	}

	/**
	 * Test method for {@link CIDRSet#toString()}.
	 * @throws UnknownHostException 
	 */
	@Test
	public void testToString() throws UnknownHostException
	{
		CIDRSet cidrs = new CIDRSet("  1.2.3.0/24 1.2.92.0/24  1.2.128.0/24,1.3.1.1/24 ");
		String[] actual = cidrs.toString()
								.replaceAll("^\\[", "")
								.replaceAll("\\]$", "")
								.split(" ");
		Arrays.sort(actual);
		String[] expected = new String[] {
				"1.2.128.0/24",
				"1.2.3.0/24",
				"1.2.92.0/24",
				"1.3.1.0/24"
		};
		assertArrayEquals("toString", expected, actual);
	}

	@Test
	public void testCoalesce1() throws Exception
	{
		CIDRSet cidrs = new CIDRSet();
		for (int i = 0; i < 256; i++) {
			cidrs.add(new CIDRAddress("0.0.0." + i + "/32"));
			cidrs.add(new CIDRAddress("0.1.0." + i + "/32"));
			// System.out.println("XXX " + i + ": " + "0.32." + (i>>4) + "." + ((i&0xf) << 4) + "/28");
			cidrs.add(new CIDRAddress("0.32." + (i>>4) + "." + ((i&0xf) << 4) + "/28"));
		}
		cidrs.freeze(true);
		assertEquals(cidrs.toSortedString(), "[0.0.0.0/24 0.1.0.0/24 0.32.0.0/20]");
	}

	@Test
	public void testCoalesce2() throws Exception
	{
		CIDRSet cidrs = new CIDRSet();
		for (int i = 0; i < 256; i++) {
			cidrs.add(new CIDRAddress("0.0.0." + i + "/32"));
			cidrs.add(new CIDRAddress("0.1.0." + i + "/32"));
			cidrs.add(new CIDRAddress("0.32." + (i>>4) + "." + ((i&0xf) << 4) + "/28"));
		}
		cidrs.add("0.32.2.0/24");
		cidrs.freeze(true);
		// System.out.println(cidrs.toSortedString());
		assertEquals(cidrs.toSortedString(), "[0.0.0.0/24 0.1.0.0/24 0.32.0.0/20]");
		assertTrue("contains 0.0.0.0/24", cidrs.contains(new CIDRAddress("0.0.0.0/24")));
		assertTrue("contains 0.1.0.0/24", cidrs.contains(new CIDRAddress("0.1.0.0/24")));
		assertTrue("contains 0.32.0.0/20", cidrs.contains(new CIDRAddress("0.32.0.0/20")));
		assertFalse("contains 0.0.0.0.20", cidrs.contains(new CIDRAddress("0.0.0.0/20")));
	}

	@Test
	public void testCoalesce3() throws Exception
	{
		CIDRSet cidrs = new CIDRSet();
		for (int i = 0; i < 256; i++) {
			cidrs.add(new CIDRAddress("0.0.0." + i + "/32"));
			cidrs.add(new CIDRAddress("0.1.0." + i + "/32"));
			cidrs.add(new CIDRAddress("0.32." + (i>>4) + "." + ((i&0xf) << 4) + "/28"));
		}
		cidrs.add("0.32.2.0/24");
		cidrs.add("::/0");
		cidrs.freeze(true);
		// System.out.println(cidrs.toSortedString());
		assertEquals(cidrs.toSortedString(), "[0.0.0.0/24 0.1.0.0/24 0.32.0.0/20 ::/0]");
		assertTrue("contains 0.0.0.0/24", cidrs.contains(new CIDRAddress("0.0.0.0/24")));
		assertTrue("contains 0.1.0.0/24", cidrs.contains(new CIDRAddress("0.1.0.0/24")));
		assertTrue("contains 0.32.0.0/20", cidrs.contains(new CIDRAddress("0.32.0.0/20")));
		assertTrue("contains ::/0", cidrs.contains(new CIDRAddress("::/0")));
		assertFalse("contains 0.0.0.0/20", cidrs.contains(new CIDRAddress("0.0.0.0/20")));
	}
	
	@Test
	public void testEquals1() throws Exception
	{
		CIDRSet s1 = new CIDRSet();
		CIDRSet s2 = new CIDRSet();
		int nCidr = 16;
		for (int i = 0; i < nCidr; i++) {
			// System.out.println("s1.hash=" + s1.hashCode() + " s2.hash=" + s2.hashCode());	// XXXX
			if (i == 0) {
				assertTrue("Equals/empty-set/1", s1.equals(s2));
				assertTrue("Equals/empty-set/2", s2.equals(s1));
				assertEquals("Hashcode/empty-set", s1.hashCode(), s2.hashCode());
			} else {
				assertFalse("Equals/partial-set/1", s1.equals(s2));
				assertFalse("Equals/partial-set/2", s2.equals(s1));
				assertNotSame("Hashcode/partial-set", s1.hashCode(), s2.hashCode());
			}
			s1.add(i + ".0.0.0/8");
			s2.add((nCidr - 1 - i) + ".0.0.0/8");
		}
		// System.out.println("s1.hash=" + s1.hashCode() + " s2.hash=" + s2.hashCode());	// XXXX
		// System.out.println("s1: " + s1.toSortedString());
		// System.out.println("s2: " + s2.toSortedString());
		assertTrue("Equals/full-set-pre-freeze/1", s1.equals(s2));
		assertTrue("Equals/full-set-pre-freeze/2", s2.equals(s1));
		int h1 = s1.hashCode();
		int h2 = s2.hashCode();
		assertEquals("Pre-freeze hash codes", h1, h2);
		s1.freeze();
		s2.freeze();
		assertTrue("Equals/full-set-post-freeze/1", s1.equals(s2));
		assertTrue("Equals/full-set-post-freeze/2", s2.equals(s1));
		assertEquals("Post-freeze s1 hash code", h1, s1.hashCode());
		assertEquals("Post-freeze s2 hash code", h2, s2.hashCode());
	}
	
	@Test
	public void testEquals2() throws Exception
	{
		CIDRSet s1 = new CIDRSet();
		CIDRSet s2 = new CIDRSet();
		int nCidr = 16;
		for (int i = 0; i < nCidr; i++) {
			// System.out.println("s1.hash=" + s1.hashCode() + " s2.hash=" + s2.hashCode());	// XXXX
			assertTrue("Equals/partial-set/1", s1.equals(s2));
			assertTrue("Equals/partial-set/2", s2.equals(s1));
			assertEquals("Hashcode/partial-set", s1.hashCode(), s2.hashCode());
			s1.add(i + ".0.0.0/8");
			s2.add(i + ".0.0.0/8");
		}
		// System.out.println("s1.hash=" + s1.hashCode() + " s2.hash=" + s2.hashCode());	// XXXX
		assertTrue("Equals/full-set-pre-freeze/1", s1.equals(s2));
		assertTrue("Equals/full-set-pre-freeze/2", s2.equals(s1));
		int h1 = s1.hashCode();
		int h2 = s2.hashCode();
		assertEquals("Pre-freeze hash codes", h1, h2);
		s1.freeze();
		s2.freeze();
		assertTrue("Equals/full-set-post-freeze/1", s1.equals(s2));
		assertTrue("Equals/full-set-post-freeze/2", s2.equals(s1));
		assertEquals("Post-freeze s1 hash code", h1, s1.hashCode());
		assertEquals("Post-freeze s2 hash code", h2, s2.hashCode());
	}
	
	@Test
	public void testEquals3() throws Exception
	{
		CIDRSet s1 = new CIDRSet();
		CIDRSet s2 = new CIDRSet();
		int nCidr = 256;
		for (int i = 0; i < nCidr; i++) {
			s1.add(i + ".0.0.0/8");
			s2.add((nCidr - 1 - i) + ".0.0.0/8");
		}
		// System.out.println("s1.hash=" + s1.hashCode() + " s2.hash=" + s2.hashCode());	// XXXX
		assertTrue("Equals/full-set-pre-freeze/1", s1.equals(s2));
		assertTrue("Equals/full-set-pre-freeze/2", s2.equals(s1));
		int h1 = s1.hashCode();
		int h2 = s2.hashCode();
		assertEquals("Pre-freeze hash codes", h1, h2);
		s1.freeze();
		s2.freeze();
		assertTrue("Equals/full-set-post-freeze/1", s1.equals(s2));
		assertTrue("Equals/full-set-post-freeze/2", s2.equals(s1));
		assertEquals("Post-freeze s1 hash code", h1, s1.hashCode());
		assertEquals("Post-freeze s2 hash code", h2, s2.hashCode());
	}
	
	@Test
	public void testCovers1() throws Exception
	{
		CIDRSet s1 = new CIDRSet("1.0.0.0/8 3.0.0.0/8");
		CIDRSet s2 = new CIDRSet("1.0.0.0/16 1.2.0.0/16 3.4.0.0/16");
		CIDRSet s3 = new CIDRSet("1.0.0.0/16 4.2.0.0/16 3.4.0.0/16");
		assertTrue("s1 fully covers s2", s1.fullyCovers(s2));
		assertFalse("!s1 fully covers s1", s1.fullyCovers(s1));
		assertFalse("!s1 fully covers s2", s1.fullyCovers(s3));
		assertTrue("s1 partially covers s2", s1.partiallyCovers(s2));
		assertTrue("s1 partially covers s3", s1.partiallyCovers(s3));
		assertFalse("!s2 fully covers s3", s2.fullyCovers(s3));
		assertFalse("!s2 partially covers s3", s2.partiallyCovers(s3));
	}
}
