package com.wdroome.apps.midi2osc;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import com.wdroome.json.JSONValue;
import com.wdroome.json.JSONValue_Array;
import com.wdroome.json.JSONValue_Number;
import com.wdroome.json.JSONValue_String;
import com.wdroome.json.JSONLexan;
import com.wdroome.json.JSONParseException;
import com.wdroome.json.JSONParser;

/**
 * An ordered list of positive integers. C'tors create the list from multiple formats.
 * Methods test if an int is in the list, and iterate over the ints.
 * @author wdr
 */
public class IntList implements Iterable<Integer>
{
	private final List<IntPair> m_pairs = new ArrayList<>();
	
	/**
	 * A static list with the ints from 0 to 127, inclusive.
	 */
	public static final IntList LIST_0_127 = new IntList("0-127");
	
	/**
	 * Create a list of positive ints from a string like "1-4 6-8 9 16-14".
	 * Note that 16-14 means the integers 16, 15 and 14, in descending order.
	 * @param str The input string.
	 * @throws IllegalArgumentException If the string cannot be parsed.
	 */
	public IntList(String str)
	{
		if (str == null) {
			return;
		}
		String[] elems = str.split("[ \t\n\r,;]+");
		for (String elem: elems) {
			if (elem.isEmpty()) {
				continue;
			}
			String[] ab = elem.split("[->:/<]");
			try {
				int a = Integer.parseInt(ab[0]);
				int b = ab.length >= 2 ? Integer.parseInt(ab[1]) : a;
				if (a < 0 || b < 0 || ab.length > 2) {
					throw new IllegalArgumentException("Incorrectly formatted int list");
				}
				m_pairs.add(new IntPair(a, b));
			} catch (Exception e) {
				throw new IllegalArgumentException("Incorrectly formatted int list");
			}
		}
		if (size() <= 0) {
			throw new IllegalArgumentException("Incorrectly formatted int list");
		}
	}
	
	/**
	 * Create a list with one int.
	 * @param jnum A JSON number with the value. Use the int part if it's a float.
	 */
	public IntList(JSONValue_Number jnum)
	{
		m_pairs.add(new IntPair((int)jnum.m_value, (int)jnum.m_value));
	}
	
	/**
	 * Create a list from the numbers in a JSON array. The list is from each even index to the next odd index.
	 * So [1, 4, 8, 7] is equivalent to "1-4 8-7".
	 * @param jarr An array of numbers. Use the int part for floats.
	 */
	public IntList(JSONValue_Array jarr)
	{
		jarr = jarr.restrictValueTypes(List.of(JSONValue_Number.class));
		for (int iarr = 0; iarr < jarr.size(); iarr += 2) {
			double dfrom = jarr.getNumber(iarr, Double.NaN);
			double dto = iarr+1 < jarr.size() ? jarr.getNumber(iarr+1, Double.NaN) : dfrom;
			if (Double.isNaN(dfrom) || Double.isNaN(dto)) {
				throw new IllegalArgumentException("Incorrectly formatted int list");			
			}
			m_pairs.add(new IntPair((int)dfrom, (int)dto));
		}
	}
	
	/**
	 * Create a list with from a JSON String.
	 * @param jstr A JSON string with the list of ints.
	 * @throws IllegalArgumentException If the string cannot be parsed.
	 * @see #IntList(String)
	 */
	public IntList(JSONValue_String jstr)
	{
		this(jstr.m_value);
	}
	
	/**
	 * Create an IntList from a JSONValue.
	 * @param jval A JSONValue.
	 * @return A new IntList, or throw an exception.
	 * @throws IllegalArgumentException If the value cannot be parsed as a list of ints.
	 * @see #IntList(JSONValue_String)
	 * @see #IntList(JSONValue_Number)
	 * @see #IntList(JSONValue_Array)
	 */
	public static IntList makeIntList(JSONValue jval)
	{
		if (jval instanceof JSONValue_String) {
			return new IntList((JSONValue_String)jval);
		} else if (jval instanceof JSONValue_Number) {
			return new IntList((JSONValue_Number)jval);
		} else if (jval instanceof JSONValue_Array) {
			return new IntList((JSONValue_Array)jval);
		} else {
			throw new IllegalArgumentException("Incorrectly formatted int list \"" + jval + "\"");
		}
	}
	
	/**
	 * Return the number of integers in this list.
	 * @return The number of integers in this list.
	 */
	public int size()
	{
		int total = 0;
		for (IntPair pair: m_pairs) {
			total += pair.size();
		}
		return total;
	}
	
	/**
	 * Test if a value is in the list.
	 * @param i The value to test.
	 * @return True iff i is in the list.
	 */
	public boolean contains(int i)
	{
		for (IntPair pair: m_pairs) {
			if (pair.contains(i)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Return a String representation of this list.
	 */
	@Override
	public String toString()
	{
		return "IntList" + m_pairs;
	}

	/**
	 * Return an iterator over the ints in this list.
	 */
	@Override
	public Iterator<Integer> iterator()
	{
		return new MyIter(m_pairs);
	}
	
	/**
	 * Private iterator over the ints in this list.
	 */
	private static class MyIter implements Iterator<Integer>
	{
		private final Iterator<IntPair> m_listIter;
		private IntPair m_currPair;
		private Iterator<Integer> m_currPairIter;
		
		private MyIter(List<IntPair> pairs)
		{
			m_listIter = pairs.iterator();
			m_currPair = m_listIter.hasNext() ? m_listIter.next() : null;
			m_currPairIter = m_currPair != null ? m_currPair.iterator() : null;
		}

		@Override
		public boolean hasNext()
		{
			return m_currPair != null;
		}

		@Override
		public Integer next()
		{
			if (!hasNext()) {
				throw new IllegalStateException();
			}
			Integer v = m_currPairIter.next();
			if (!m_currPairIter.hasNext()) {
				if (m_listIter.hasNext()) {
					m_currPair = m_listIter.next();
					m_currPairIter = m_currPair.iterator();
				} else {
					m_currPair = null;
					m_currPairIter = null;
				}
			}
			return v;
		}
	}
	
	/**
	 * A pair of ints, with size(). comtains(int), and an iterator over the ints.
	 */
	private static class IntPair
	{
		private final int m_from, m_to;
		
		private IntPair(int from, int to)
		{
			m_from = from;
			m_to = to;
		}
		
		private boolean contains(int i) 
		{
			if (m_from <= m_to) {
				return i >= m_from && i <= m_to;
			} else {
				return i >= m_to && i <= m_from;
			}
		}
		
		@Override
		public String toString()
		{
			if (m_from == m_to) {
				return m_from + "";
			} else {
				return m_from + "-" + m_to;
			}
		}
		
		public Iterator<Integer> iterator()
		{
			if (m_from <= m_to) {
				return new UpIter(m_from, m_to);
			} else {
				return new DownIter(m_from, m_to);
			}
		}
		
		private int size()
		{
			if (m_from <= m_to) {
				return m_to - m_from + 1;
			} else {
				return m_from - m_to + 1;			
			}
		}
		
		private static class UpIter implements Iterator<Integer>
		{
			private final int m_from, m_to;
			private int m_next;
			
			private UpIter(int from, int to)
			{
				m_from = from;
				m_to = to;
				m_next = from;
			}

			@Override
			public boolean hasNext()
			{
				return m_next <= m_to;
			}

			@Override
			public Integer next()
			{
				if (!hasNext()) {
					throw new IllegalStateException();
				}
				return m_next++;
			}
		}
		
		private static class DownIter implements Iterator<Integer>
		{
			private final int m_from, m_to;
			private int m_next;
			
			private DownIter(int from, int to)
			{
				m_from = from;
				m_to = to;
				m_next = from;
			}

			@Override
			public boolean hasNext()
			{
				return m_next >= m_to;
			}

			@Override
			public Integer next()
			{
				if (!hasNext()) {
					throw new IllegalStateException();
				}
				return m_next--;
			}
		}
	}
	
	/**
	 * Test main. Args[0] is a string defining the list.
	 * If the string starts with "[", parse it as a JSON array.
	 * Test the remaining int args for inclusion in the list.
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception
	{
		System.out.println("LIST_0_127: size=" + LIST_0_127.size() + " str=" + LIST_0_127);
		System.out.println("Parsing \"" + args[0] + "\"");
		IntList list;
		if (args[0].startsWith("[")) {
			list = makeIntList(JSONParser.parse(new JSONLexan(args[0]), false));
		} else {
			list = new IntList(args[0]);
		}
		System.out.println("List: " + list);
		System.out.println("Size: " + list.size());
		System.out.print("Ints:");
		for (Integer i: list) {
			System.out.print(" " + i);
		}
		System.out.println();
		for (int i = 1; i < args.length; i++) {
			int t = Integer.parseInt(args[i]);
			System.out.println(" Contains " + t + ": " + list.contains(t));
		}
	}
}
