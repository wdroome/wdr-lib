package test.junit.util;

import static org.junit.Assert.*;
import org.junit.*;

import com.wdroome.util.Ordinalizer;

/**
 * @author wdr
 */
public class OrdinalizerTest
{
	@Test
	public void testGetOrdinal()
	{
		Ordinalizer ord = new Ordinalizer();
		
		double[] values = new double[] {
				15, 10, 9, 9.99999, 10, 15, 16, 1, 0, 3.14159, 42, 99999, 100, Double.NaN
		};
		
		double[] unordinalizedValues = {11, 27, Double.NaN};
		
		int[] expectedOrdinals = new int[] {
				7, 6, 4, 5, 6, 7, 8, 2, 1, 3, 9, 11, 10, -1		
		};
		
		for (double v:values)
			ord.addValue(v);
		ord.doneAdding();
		
		assertEquals("getMaxOrdinal()", 11, ord.getMaxOrdinal());
		
		int[] actualOrdinals = new int[values.length];
		for (int i = 0; i < values.length; i++)
			actualOrdinals[i] = ord.getOrdinal(values[i]);
		assertArrayEquals("getOrdinal()", expectedOrdinals, actualOrdinals);
		
		for (double v:unordinalizedValues) {
			assertEquals("unordinalized value " + v, -1, ord.getOrdinal(v));
		}
	}
}
