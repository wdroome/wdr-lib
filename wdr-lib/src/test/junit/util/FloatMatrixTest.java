package test.junit.util;

import static org.junit.Assert.*;

import org.junit.Test;

import com.wdroome.util.FloatMatrix;

/**
 * @author wdr
 */
public class FloatMatrixTest
{

	/**
	 * Test method for {@link FloatMatrix#set(int, int, float)}.
	 */
	@Test
	public void testSet()
	{
		FloatMatrix m = new FloatMatrix(8192);
		int nrows = 6000;
		int ncols = 4000;
		
		float v = 0;
		for (int i = 0; i < nrows; i++) {
			for (int j = 0; j < ncols; j++) {
				m.set(i, j, ++v);
			}
		}
		
		v = 0;
		for (int i = 0; i < nrows; i++) {
			for (int j = 0; j < ncols; j++) {
				float mv = m.set(i, j, ++v);
				assertEquals("re-set " + i + "," + j, v, mv, 0.0001);
			}
		}
		
		v = 0;
		for (int i = 0; i < nrows; i++) {
			for (int j = 0; j < ncols; j++) {
				++v;
				float mv = m.get(i, j);
				assertEquals("get " + i + "," + j, v, mv, 0.0001);
			}
		}
		
		assertEquals("nRows", nrows, m.getNrows());
		assertEquals("nCols", ncols, m.getNcols());
	}
	
	@Test
	public void testSkip1()
	{
		FloatMatrix m = new FloatMatrix(8192);
		int nrows = 6000;
		int ncols = 4000;
		int rowSkip = 6;
		int colSkip = 5;
		float defVal = -1;
		m.setDefaultValue(defVal);
		
		float v = 0;
		for (int i = 0; i < nrows; i += rowSkip) {
			for (int j = 0; j < ncols; j += colSkip) {
				m.set(i, j, ++v);
			}
		}
		
		v = 0;
		for (int i = 0; i < nrows; i++) {
			for (int j = 0; j < ncols; j++) {
				float expected = defVal;
				if ((i % rowSkip == 0) && (j % colSkip) == 0) {
					expected = ++v;
				}
				assertEquals("scan get " + i + "," + j, expected, m.get(i, j), .0001);
				assertEquals("scan isSet " + i + "," + j, expected > 0, m.isSet(i, j));
			}
		}
		
		v = 0;
		defVal = -2;
		m.setDefaultValue(defVal);
		for (int i = 0; i < nrows; i++) {
			for (int j = 0; j < ncols; j++) {
				float expected = defVal;
				if ((i % rowSkip == 0) && (j % colSkip) == 0) {
					expected = ++v;
				}
				float prev = m.unset(i, j);
				assertEquals("unset prev " + i + "," + j, expected, prev, .0001);
				assertEquals("unset isSet " + i + "," + j, false, m.isSet(i, j));
				assertEquals("unset get " + i + "," + j, defVal, m.get(i, j), .0001);
			}
		}
	}
	
	@Test
	public void testNaN()
	{
		FloatMatrix m = new FloatMatrix();
		m.setDefaultValue(Float.NaN);
		// System.out.println("[0,0]: " + m.get(0,0));
		assertTrue("(0,0) is NaN", Float.isNaN(m.get(0,0)));
		m.set(0, 0, 0);
		// System.out.println("[0,0]: " + m.get(0,0));
		assertFalse("(0,0) is NaN", Float.isNaN(m.get(0,0)));
		// System.out.println("[0,1]: " + m.get(0,1));
		assertTrue("(0,1) is NaN", Double.isNaN(m.get(0,1)));
	}

	public static void main(String[] args)
	{
		Runtime rt = Runtime.getRuntime();
		FloatMatrix m = new FloatMatrix(6000);
		System.out.println("Heap: " + rt.totalMemory() + " free: " + rt.freeMemory()
				+ " used: " + (rt.totalMemory()-rt.freeMemory()));
		rt.gc();
		rt.gc();
		System.out.println("Heap: " + rt.totalMemory() + " free: " + rt.freeMemory()
				+ " used: " + (rt.totalMemory()-rt.freeMemory()));
		int ntry = 5000;
		int incr = 1;
		
		float v = 0;
		for (int i = 0; i < ntry; i++) {
			for (int j = 0; j < ntry; j++) {
				m.set(i*incr, j*incr, ++v);
			}
		}
		System.out.println("Heap: " + rt.totalMemory() + " free: " + rt.freeMemory()
				+ " used: " + (rt.totalMemory()-rt.freeMemory()));
		rt.gc();
		rt.gc();
		System.out.println("Heap: " + rt.totalMemory() + " free: " + rt.freeMemory()
				+ " used: " + (rt.totalMemory()-rt.freeMemory()));
		
		v = 0;
		for (int i = 0; i < ntry; i++) {
			for (int j = 0; j < ntry; j++) {
				float mv = m.set(i*incr, j*incr, ++v);
				if (v != mv) {
					System.out.println("OOPS! re-set error "
										+ i*incr + "," + j*incr
										+ " = " + mv + " != " + v);
					System.exit(1);
				}
			}
		}
		System.out.println("Heap: " + rt.totalMemory() + " free: " + rt.freeMemory()
				+ " used: " + (rt.totalMemory()-rt.freeMemory()));
		rt.gc();
		rt.gc();
		System.out.println("Heap: " + rt.totalMemory() + " free: " + rt.freeMemory()
				+ " used: " + (rt.totalMemory()-rt.freeMemory()));
		
		v = 0;
		for (int i = 0; i < ntry; i++) {
			for (int j = 0; j < ntry; j++) {
				++v;
				float mv = m.get(i*incr, j*incr);
				if (v != mv) {
					System.out.println("OOPS! get error "
										+ i*incr + "," + j*incr
										+ " = " + mv + " != " + v);
					System.exit(1);
				}
			}
		}
		System.out.println("Heap: " + rt.totalMemory() + " free: " + rt.freeMemory()
				+ " used: " + (rt.totalMemory()-rt.freeMemory()));
		rt.gc();
		rt.gc();
		System.out.println("Heap: " + rt.totalMemory() + " free: " + rt.freeMemory()
				+ " used: " + (rt.totalMemory()-rt.freeMemory()));
		System.out.println("nrows/cols: " + m.getNcols() + "x" + m.getNrows());
		System.out.println("arrays: " + m.getLvl2Arrays() + " " + m.getLvl3Arrays());
		m = null;
		rt.gc();
		rt.gc();
		System.out.println("Heap: " + rt.totalMemory() + " free: " + rt.freeMemory()
				+ " used: " + (rt.totalMemory()-rt.freeMemory()));
	}
}
