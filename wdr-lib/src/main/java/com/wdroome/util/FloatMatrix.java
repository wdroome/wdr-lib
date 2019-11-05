package com.wdroome.util;

/**
 * A large, possibly sparse, matrix of floats.
 * This is designed to handle large matrixes (e.g., tens of millions of entries)
 * and be efficient both in memory and time.
 * Hence we've hard-wired the value type as float,
 * rather than allow arbitrary objects.
 * @author wdr
 */
public class FloatMatrix
{
	public static final int DEF_MAX_INDEX = 32768;
	public static final int DEF_LVL2_ARRAY_SIZE = 1024;
	public static final int DEF_LVL3_ARRAY_SIZE = 1024;
	
	private final int m_maxIndex;
	private final int m_n1;
	private final int m_n2;
	private final int m_n3;
	
	private final float[][][] m_data;
	private final BitArray[][] m_valueSetBits;
	
	private float m_defaultValue = 0;
	
	private int m_nrows = 0;
	private int m_ncols = 0;
	
	private int m_lvl2Arrays = 0;
	private int m_lvl3Arrays = 0;
	
	/**
	 * Create a matrix and specify all tuning parameters.
	 * @param maxIndex The maximum number of rows or columns.
	 * 			If 0, use the default.
	 * @param n2 The size of the "level 2" arrays (pointers to level 3 arrays).
	 * 			If 0, use the default.
	 * @param n3 The size of the "level 3" arrays (actual floats).
	 * 			If 0, use the default.
	 */
	public FloatMatrix(int maxIndex, int n2, int n3)
	{
		m_maxIndex = maxIndex > 0 ? maxIndex : DEF_MAX_INDEX;
		m_n2 = n2 > 0 ? n2 : DEF_LVL2_ARRAY_SIZE;
		m_n3 = n3 > 0 ? n3 : DEF_LVL3_ARRAY_SIZE;
		m_n1 = (m_maxIndex*m_maxIndex + m_n2*m_n3 - 1) / (m_n2*m_n3);
		if (m_n1 <= 0) {
			throw new IllegalArgumentException("Invalid config parameters");
		}
		m_data = new float[m_n1][][];
		m_valueSetBits = new BitArray[m_n1][];
	}
	
	/**
	 * Create a matrix with the indicated maximum index.
	 * @param maxIndex The maximum number of rows or columns.
	 */
	public FloatMatrix(int maxIndex)
	{
		this(maxIndex, 0, 0);
	}
	
	/**
	 * Create a matrix with the default tuning parameters.
	 */
	public FloatMatrix()
	{
		this(0);
	}

	/**
	 * Return the default value.
	 * @return The default value.
	 */
	public float getDefaultValue()
	{
		return m_defaultValue;
	}

	/**
	 * Set the default value.
	 * @param defaultValue The default value.
	 */
	public void setDefaultValue(float defaultValue)
	{
		m_defaultValue = defaultValue;
	}
	
	/**
	 * Set a matrix element.
	 * @param i The row index.
	 * @param j The column index.
	 * @param v The new value.
	 * @return The previous value, or the default value if never set.
	 * @throws IndexOutOfBoundsException If i or j exceed the maximum allowable index.
	 */
	public float set(int i, int j, float v)
	{
		if (i < 0 || i >= m_maxIndex || j < 0 || j >= m_maxIndex) {
			throw new IndexOutOfBoundsException(i + "," + j + " out of bounds");
		}
		boolean debug = false;
		int k = i*m_maxIndex + j;
		if (debug) {
			System.out.println(i + "," + j + " -> " + k);
		}
		int x = k / (m_n2*m_n3);
		if (debug) {
			System.out.println("  x " + x);
		}
		float[][] v2 = m_data[x];
		BitArray[] bits2 = m_valueSetBits[x];
		if (v2 == null) {
			m_data[x] = v2 = new float[m_n2][];
			m_valueSetBits[x] = bits2 = new BitArray[m_n2];
			m_lvl2Arrays++;
		}
		int y = (k % (m_n2*m_n3)) / m_n3;
		if (debug) {
			System.out.println("  y " + y);
		}
		float[] v3 = v2[y];
		BitArray bits3 = bits2[y];
		if (v3 == null) {
			v2[y] = v3 = new float[m_n3];
			bits2[y] = bits3 = new BitArray(m_n3);
			m_lvl3Arrays++;
		}
		int z = k % m_n3;
		if (debug) {
			System.out.println("  z " + z);
		}
		float prev = bits3.isSet(z) ? v3[z] : m_defaultValue;
		v3[z] = v;
		bits3.set(z, true);
		if (i+1 >= m_nrows) {
			m_nrows = i+1;
		}
		if (j+1 >= m_ncols) {
			m_ncols = j+1;
		}
		return prev;
	}
	
	/**
	 * Get a matrix element.
	 * @param i The row index.
	 * @param j The column index.
	 * @return The value at [i,j], or the default value if never set.
	 * @throws IndexOutOfBoundsException If i or j exceed the maximum allowable index.
	 */
	public float get(int i, int j)
	{
		if (i < 0 || i >= m_maxIndex || j < 0 || j >= m_maxIndex) {
			throw new IndexOutOfBoundsException(i + "," + j + " out of bounds");
		}
		int k = i*m_maxIndex + j;
		int x = k / (m_n2*m_n3);
		float[][] v2 = m_data[x];
		if (v2 == null) {
			return m_defaultValue;
		}
		BitArray[] bits2 = m_valueSetBits[x];
		int y = (k % (m_n2*m_n3)) / m_n3;
		float[] v3 = v2[y];
		if (v3 == null) {
			return m_defaultValue;
		}
		BitArray bits3 = bits2[y];
		int z = k % m_n3;
		return bits3.isSet(z) ? v3[z] : m_defaultValue;
	}
	
	/**
	 * Unset a matrix element. Note that index still exists.
	 * But get(i,j) will return the default value,
	 * and (if possible) we reclaim the space the element occupied.
	 * @param i The row index.
	 * @param j The column index.
	 * @return The previous value at [i,j], or the default value if never set.
	 * @throws IndexOutOfBoundsException If i or j exceed the maximum allowable index.
	 */
	public float unset(int i, int j)
	{
		if (i < 0 || i >= m_maxIndex || j < 0 || j >= m_maxIndex) {
			throw new IndexOutOfBoundsException(i + "," + j + " out of bounds");
		}
		int k = i*m_maxIndex + j;
		int x = k / (m_n2*m_n3);
		float[][] v2 = m_data[x];
		if (v2 == null) {
			return m_defaultValue;
		}
		BitArray[] bits2 = m_valueSetBits[x];
		int y = (k % (m_n2*m_n3)) / m_n3;
		float[] v3 = v2[y];
		if (v3 == null) {
			return m_defaultValue;
		}
		BitArray bits3 = bits2[y];
		int z = k % m_n3;
		float prev = bits3.isSet(z) ? v3[z] : m_defaultValue;
		bits3.set(z, false);
		if (bits3.allClear()) {
			v2[y] = null;
			bits2[y] = null;
		}
		return prev;
	}
	
	/**
	 * Unset all elements in a row.
	 * @param i The row index.
	 * @throws IndexOutOfBoundsException If i exceeds the maximum allowable index.
	 */
	public void unsetRow(int i)
	{
		if (i < 0 || i >= m_maxIndex) {
			throw new IndexOutOfBoundsException(i + " out of bounds");
		}
		for (int j = 0; j < m_ncols; j++) {
			unset(i, j);
		}
	}
	
	/**
	 * Unset all elements in a column.
	 * @param j The column index.
	 * @throws IndexOutOfBoundsException If i exceeds the maximum allowable index.
	 */
	public void unsetCol(int j)
	{
		if (j < 0 || j >= m_maxIndex) {
			throw new IndexOutOfBoundsException(j + " out of bounds");
		}
		for (int i = 0; i < m_nrows; i++) {
			unset(i, j);
		}
	}
	
	/**
	 * Get a matrix element as a double. Round as needed.
	 * @param i The row index.
	 * @param j The column index.
	 * @return The value at [i,j], or the default value if never set.
	 * @throws IndexOutOfBoundsException If i or j exceed the maximum allowable index.
	 */
	public double getDouble(int i, int j)
	{
		float v = get(i, j);
		if (Float.isNaN(v)) {
			return Double.NaN;
		} else {
			return ((double)Math.round(1000000*(double)v)) / 1000000;
		}
	}
	
	/**
	 * Return true if a matrix element has been set.
	 * @param i The row index.
	 * @param j The column index.
	 * @return True if the value at [i,j] has been set.
	 * @throws IndexOutOfBoundsException If i or j exceed the maximum allowable index.
	 */
	public boolean isSet(int i, int j)
	{
		if (i < 0 || i >= m_maxIndex || j < 0 || j >= m_maxIndex) {
			throw new IndexOutOfBoundsException(i + "," + j + " out of bounds");
		}
		int k = i*m_maxIndex + j;
		int x = k / (m_n2*m_n3);
		BitArray[] bits2 = m_valueSetBits[x];
		if (bits2 == null) {
			return false;
		}
		int y = (k % (m_n2*m_n3)) / m_n3;
		BitArray bits3 = bits2[y];
		if (bits3 == null) {
			return false;
		}
		return bits3.isSet(k % m_n3);
	}
	
	/**
	 * Return number of rows.
	 * @return The index (plus 1) of the highest row
	 * 		with an element that has ever been set.
	 */
	public int getNrows()
	{
		return m_nrows;
	}

	/**
	 * Return the number of columns.
	 * @return The index (plus 1) of the highest column
	 * 		with an element that has ever been set.
	 */
	public int getNcols()
	{
		return m_ncols;
	}

	/**
	 * Return the limit on the number of rows or columns.
	 * @return The maximum allowable number of rows or columns.
	 * That is, the maximum index plus one.
	 */
	public int getMaxIndex()
	{
		return m_maxIndex;
	}

	/**
	 * Return the number of "level 2" arrays allocated for this matrix (for debugging and performance analysis).
	 * @return The number of "level 2" arrays allocated for this matrix.
	 */
	public int getLvl2Arrays()
	{
		return m_lvl2Arrays;
	}

	/**
	 * Return the number of "level 3" arrays allocated for this matrix (for debugging and performance analysis).
	 * @return The number of "level 3" arrays allocated for this matrix.
	 */
	public int getLvl3Arrays()
	{
		return m_lvl3Arrays;
	}
}
