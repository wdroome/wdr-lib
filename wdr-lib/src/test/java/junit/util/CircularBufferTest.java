package test.junit.util;

import static org.junit.Assert.*;

import org.junit.Test;

import com.wdroome.util.CircularBuffer;

/**
 * @author wdr
 *
 */
public class CircularBufferTest
{

	@Test
	public void test()
	{
		int limit = 5;
		CircularBuffer<String> b = new CircularBuffer<String>(limit);
		for (int i = 1; i <= limit; i++) {
			b.add("s" + i);
			assertEquals("size, add phase 1", i, b.size());
		}
		
		String[] arr = b.toArray(new String[0]);
		assertArrayEquals("array, phase 1", new String[]{"s1", "s2", "s3", "s4", "s5"}, arr);
		
		int iGet = 0;
		for (String s: b) {
			iGet++;
			assertEquals("get, phase 1 #" + iGet, "s" + iGet, s);
		}
		assertEquals("get, phase 1 $", b.limit(), iGet);
		
		int nOver = 2;
		for (int i = limit+1; i <= limit+nOver; i++) {
			b.add("s" + i);
			assertEquals("size, add phase 2", limit, b.size());
		}
		
		arr = b.toArray(new String[0]);
		assertArrayEquals("array, phase 2", new String[]{"s3", "s4", "s5", "s6", "s7"}, arr);
		
		iGet = 0;
		for (String s: b) {
			iGet++;
			assertEquals("get, phase 2 #" + iGet, "s" + (iGet + nOver), s);
		}
		assertEquals("get, phase 2 $", b.limit(), iGet);
	}

	/**
	 *	Test driver: add some strings, then get the array.
	 */
	public static void main(String[] args)
	{
		CircularBuffer<String> b = new CircularBuffer<String>(5);
		int n = 6;
		System.out.println("Add " + n + " strings to a buffer of length 5.");
		for (int i = 1; i <= n; i++) {
			b.add("s" + i);
		}
		System.out.println("Dump array:");
		String[] arr = b.toArray(new String[0]);
		for (int i = 0; i < arr.length; i++) {
			System.out.println("[" + i + "]: \"" + arr[i] + "\"");
		}
	}
}
