package com.wdroome.artnet;

import static org.junit.Assert.*;

import org.junit.Test;
import com.wdroome.artnet.ArtNetUniv;

/**
 * @author wdr
 */
public class ArtNetUniv_Test
{
	@Test
	public void test1()
	{
		ArtNetUniv p = new ArtNetUniv("1.2.3");
		assertEquals("net", 1, p.m_net);
		assertEquals("subNet", 2, p.m_subNet);
		assertEquals("universe", 3, p.m_universe);
		assertEquals("subUniv", 0x23, p.subUniv());
	}

	@Test
	public void test2()
	{
		ArtNetUniv p = new ArtNetUniv("1.2");
		assertEquals("net", 0, p.m_net);
		assertEquals("subNet", 1, p.m_subNet);
		assertEquals("universe", 2, p.m_universe);
		assertEquals("subUniv", 0x12, p.subUniv());
	}

	@Test
	public void test3()
	{
		ArtNetUniv p = new ArtNetUniv(1, 0x23);
		assertEquals("net", 1, p.m_net);
		assertEquals("subNet", 2, p.m_subNet);
		assertEquals("universe", 3, p.m_universe);
		assertEquals("subUniv", 0x23, p.subUniv());
	}
	
	@Test
	public void testEq()
	{
		ArtNetUniv p1 = new ArtNetUniv("1.2.3");
		ArtNetUniv p2 = new ArtNetUniv("1.2.3");
		ArtNetUniv p3 = new ArtNetUniv("1.1.2");
		
		assertTrue("p1==p2", p1.equals(p2));
		assertTrue("p1!=p3", !p1.equals(p3));
		
		assertEquals("p1.hash", 0x123, p1.hashCode());
		assertEquals("p2.hash", 0x123, p2.hashCode());
		assertEquals("p3.hash", 0x112, p3.hashCode());
	}
}
