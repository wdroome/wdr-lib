/**
 * 
 */
package test.junit.util;

import static org.junit.Assert.*;

import org.junit.Test;

import com.wdroome.util.String3;

/**
 * @author wdr
 */
public class String3Test
{

	@Test
	public void test()
	{
		String3 a1 = new String3("a", "b", "c");
		String3 a2 = new String3("a", "b", "c");
		String3 b1 = new String3("a", "a", "a");
		String3 b2 = new String3("b", "b", "b");

		assertTrue("equals() a1 a2", a1.equals(a2));
		assertTrue("equals() a2 a1", a2.equals(a1));
		assertTrue("hashCode() a1 a2", a1.hashCode() == a2.hashCode());
		
		assertTrue("!equals() a1 b1", !a1.equals(b1));
		assertTrue("!equals() a1 b2", !a1.equals(b2));
		assertTrue("!equals() a2 b1", !a2.equals(b1));
		assertTrue("!equals() a2 b2", !a2.equals(b2));

		assertTrue("!hashCode() a1 b1", a1.hashCode() != b1.hashCode());
		assertTrue("!hashCode() a1 b2", a1.hashCode() != b2.hashCode());
		
		String3 na1 = new String3("a", null, null);
		String3 na2 = new String3("a", null, null);
		String3 nb1 = new String3(null, "b", null);
		String3 nb2 = new String3(null, "b", null);
		String3 nc1 = new String3(null, null, "c");
		String3 nc2 = new String3(null, null, "c");
		String3 nd1 = new String3(null, null, null);
		String3 nd2 = new String3(null, null, null);
		
		assertTrue("equals() na1 na2", na1.equals(na2));
		assertTrue("equals() nb1 nb2", nb1.equals(nb2));
		assertTrue("equals() nc1 nc2", nc1.equals(nc2));
		assertTrue("equals() nd1 nd2", nd1.equals(nd2));
		assertTrue("!equals() na1 a1", !na1.equals(a1));
		assertTrue("!equals() a1 na1", !a1.equals(na1));

		assertTrue("hashCode() na1 na2", na1.hashCode() == na2.hashCode());
		assertTrue("hashCode() nb1 nb2", nb1.hashCode() == nb2.hashCode());
		assertTrue("hashCode() nc1 nc2", nc1.hashCode() == nc2.hashCode());
		assertTrue("hashCode() nd1 nd2", nd1.hashCode() == nd2.hashCode());
		assertTrue("!hashCode() na1 a1", na1.hashCode() != a1.hashCode());
		assertTrue("!hashCode() na1 nb1", na1.hashCode() != nb1.hashCode());

		assertEquals("na1.toString", "<a,null,null>", na1.toString());
		assertEquals("nb1.toString", "<null,b,null>", nb1.toString());
		assertEquals("nc1.toString", "<null,null,c>", nc1.toString());
		assertEquals("nd1.toString", "<null,null,null>", nd1.toString());
	}

}
