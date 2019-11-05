package com.wdroome.util;

import java.util.HashSet;
import java.util.Arrays;

/**
 * Convert a set of double values to ordinals.
 * To use, create an instance, call {@link #addValue(double)} for each value,
 * and then call {@link #doneAdding()}.
 * After that, you can call {@link #getOrdinal(double)} to get the ordinal for a value.
 * NOTE: To allow for a potentially large number of distinct values,
 * this class actually converts the doubles to floats,
 * and ordinalizes the float values.
 * @author wdr
 */
public class Ordinalizer
{
	private HashSet<Float> m_accumulatingValues = new HashSet<Float>();
	private float[] m_finalValues = null;
	
	/**
	 * Add a new value. Ignore NaN values.
	 * @param value The value to add.
	 * @throws IllegalStateException If called after {@link #doneAdding()}.
	 */
	public void addValue(double value)
	{
		if (m_accumulatingValues == null)
			throw new IllegalStateException("Ordinalizer.addValue() called after finishing.");
		if (!Double.isNaN(value))
			m_accumulatingValues.add((float)value);
	}
	
	/**
	 * All values have been added.
	 */
	public void doneAdding()
	{
		if (m_finalValues != null)
			return;
		m_finalValues = new float[m_accumulatingValues.size()];
		int i = 0;
		for (Float v:m_accumulatingValues) {
			m_finalValues[i++] = v;
		}
		m_accumulatingValues = null; // give it to garbage collector
		Arrays.sort(m_finalValues);
	}
	
	/**
	 * Return the ordinal for a value, from 1 to the number of distinct values.
	 * @param value The value for which we want the ordinal.
	 * @return The ordinal for value, or -1 if value was never added to the set.
	 * @throws IllegalStateException If called before {@link #doneAdding()}.
	 */
	public int getOrdinal(double value)
	{
		if (m_finalValues == null)
			throw new IllegalStateException("getOrdinal() called before finishing.");
		int index = Arrays.binarySearch(m_finalValues, (float)value);
		return (index >= 0) ? (index + 1) : -1;
	}
	
	/**
	 * Return the maximum ordinal value. That is, the number of distinct values.
	 * @throws IllegalStateException If called before {@link #doneAdding()}.
	 */
	public int getMaxOrdinal()
	{
		if (m_finalValues == null)
			throw new IllegalStateException("getMaxOrdinal() called before finishing.");
		return m_finalValues.length;
	}
	
	/**
	 * Return the value for an ordinal. Note that the first ordinal is 1.
	 * @param ordinal The ordinal, starting with 1.
	 * @return The value for that ordinal.
	 * @throws IllegalStateException If called before {@link #doneAdding()}.
	 * @throws IllegalArgumentException If ordinal isn't between 1 and the maximum ordinal.
	 */
	public double getValue(int ordinal)
	{
		if (m_finalValues == null)
			throw new IllegalStateException("getOrdinal() called before finishing.");
		if (!(ordinal >= 1 && ordinal <= m_finalValues.length))
			throw new IllegalArgumentException(ordinal + " is not a legal ordinal.");
		return m_finalValues[ordinal-1];
	}
}
