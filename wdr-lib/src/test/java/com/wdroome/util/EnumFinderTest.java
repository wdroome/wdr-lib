package com.wdroome.util;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.Set;

public class EnumFinderTest
{
	private enum TestEnum implements EnumFinder.AltNames
	{
		COMMANDA1("cmda1"),
		COMMANDB2("cmdb2"),
		UNKNOWN;
		
		private final String[] m_altNames;
		
		private TestEnum(String altName)
		{
			m_altNames = new String[] {altName};
		}
		
		private TestEnum(String[] altNames)
		{
			m_altNames = altNames;
		}
		
		private TestEnum()
		{
			m_altNames = null;
		}
		
		@Override
		public String[] altNames() { return m_altNames; }
	}

	@Test
	public void test()
	{
		EnumFinder<TestEnum> tf =
				new EnumFinder<>(TestEnum.values())
				{
					public TestEnum noMatch(String findName) {
						return TestEnum.UNKNOWN;
					}
				};
		String[] names =  new String[] {
				"COMMANDA1",
				"COMMANDB2",
				"commanda",
				"commandb",
				"cmda1",
				"Cmdb2",
				"cmda",
				"cmdb",
				"cmd",
				"command",
				"xcmd",
			};
		TestEnum[] values =  new TestEnum[] {
				TestEnum.COMMANDA1,
				TestEnum.COMMANDB2,
				TestEnum.COMMANDA1,
				TestEnum.COMMANDB2,
				TestEnum.COMMANDA1,
				TestEnum.COMMANDB2,
				TestEnum.COMMANDA1,
				TestEnum.COMMANDB2,
				TestEnum.UNKNOWN,
				TestEnum.UNKNOWN,
				TestEnum.UNKNOWN,
			};
		Set<TestEnum>[] matches = new Set[] {
				Set.of(TestEnum.COMMANDA1),
				Set.of(TestEnum.COMMANDB2),
				Set.of(TestEnum.COMMANDA1),
				Set.of(TestEnum.COMMANDB2),
				Set.of(TestEnum.COMMANDA1),
				Set.of(TestEnum.COMMANDB2),
				Set.of(TestEnum.COMMANDA1),
				Set.of(TestEnum.COMMANDB2),
				Set.of(TestEnum.COMMANDA1, TestEnum.COMMANDB2),
				Set.of(TestEnum.COMMANDA1, TestEnum.COMMANDB2),
				Set.of(),
			};
		
		for (int iName = 0; iName < names.length; iName++) {
			assertEquals("find(" + names[iName] + ")", values[iName], tf.find(names[iName]));
			assertEquals("findMatches(" + names[iName] + ")", matches[iName], tf.findMatches(names[iName]));
		}
	}
}
