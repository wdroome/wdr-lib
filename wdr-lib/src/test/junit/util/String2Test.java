package test.junit.util;

import static org.junit.Assert.*;

import org.junit.Test;

import com.wdroome.util.String2;

/**
 * @author wdr
 */
public class String2Test
{

	@Test
	public void test()
	{
		String2 a1 = new String2("a", "b");
		String2 a2 = new String2("a", "b");
		String2 b1 = new String2("a", "a");
		String2 b2 = new String2("b", "b");

		assertTrue("equals() a1 a2", a1.equals(a2));
		assertTrue("equals() a2 a1", a2.equals(a1));
		assertTrue("hashCode() a1 a2", a1.hashCode() == a2.hashCode());
		
		assertTrue("!equals() a1 b1", !a1.equals(b1));
		assertTrue("!equals() a1 b2", !a1.equals(b2));
		assertTrue("!equals() a2 b1", !a2.equals(b1));
		assertTrue("!equals() a2 b2", !a2.equals(b2));

		assertTrue("!hashCode() a1 b1", a1.hashCode() != b1.hashCode());
		assertTrue("!hashCode() a1 b2", a1.hashCode() != b2.hashCode());
		
		String2 na1 = new String2("a", null);
		String2 na2 = new String2("a", null);
		String2 nb1 = new String2(null, "b");
		String2 nb2 = new String2(null, "b");
		String2 nc1 = new String2(null, null);
		String2 nc2 = new String2(null, null);
		
		assertTrue("equals() na1 na2", na1.equals(na2));
		assertTrue("equals() nb1 nb2", nb1.equals(nb2));
		assertTrue("equals() nc1 nc2", nc1.equals(nc2));
		assertTrue("!equals() na1 a1", !na1.equals(a1));
		assertTrue("!equals() a1 na1", !a1.equals(na1));

		assertTrue("hashCode() na1 na2", na1.hashCode() == na2.hashCode());
		assertTrue("hashCode() nb1 nc2", nb1.hashCode() == nb2.hashCode());
		assertTrue("hashCode() nc1 nc2", nc1.hashCode() == nc2.hashCode());
		assertTrue("!hashCode() na1 a1", na1.hashCode() != a1.hashCode());
		assertTrue("!hashCode() na1 nb1", na1.hashCode() != nb1.hashCode());

		assertEquals("na1.toString", "<a,null>", na1.toString());
		assertEquals("nb1.toString", "<null,b>", nb1.toString());
		assertEquals("nc1.toString", "<null,null>", nc1.toString());
	}
}
