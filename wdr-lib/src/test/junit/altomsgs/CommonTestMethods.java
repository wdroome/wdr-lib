package test.junit.altomsgs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Common methods for junit tests for ALTO message classes.
 * @author wdr
 */
public class CommonTestMethods
{
	
	/**
	 * Return a square-bracket-wrapped, comma-separated string
	 * with the String representations of the items in an array.
	 */
	public static String catArray(Object[] arr)
	{
		StringBuilder b = new StringBuilder();
		if (arr == null)
			return "null[]";
		String cname = arr.getClass().getName();
		cname = cname.replaceAll("^\\[+L{0,1}", "");
		cname = cname.replaceAll(";$", "");
		int n = cname.lastIndexOf(".");
		if (n >= 0)
			cname = cname.substring(n+1);
		b.append(cname);
		b.append("[");
		String sep = "";
		for (Object o: arr) {
			b.append(sep);
			b.append(o.toString());
			sep = ",";
		}
		b.append("]");
		return b.toString();
	}
	
	/**
	 * Return an array with the Strings in an iteration.
	 * @param iter
	 * @return
	 */
	public static String[] iterToArray(Iterator<String> iter)
	{
		ArrayList<String> list = new ArrayList<String>();
		while (iter.hasNext())
			list.add(iter.next());
		return list.toArray(new String[list.size()]);
	}
	
	/**
	 * Sort and return an array.
	 * @param arr The array.
	 * @return The array "arr", sorted.
	 */
	public static String[] sort(String[] arr)
	{
		Arrays.sort(arr);
		return arr;
	}
}
