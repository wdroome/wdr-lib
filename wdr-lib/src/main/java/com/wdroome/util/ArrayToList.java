package com.wdroome.util;

import java.util.ArrayList;

/**
 * Create an {@link ArrayList} object from an array.
 * If ArrayList has a constructor that does this,
 * or if an equivalent class exists in the standard library,
 * I cannot find it.
 * @author wdr
 */
public class ArrayToList<T> extends ArrayList<T>
{
	private static final long serialVersionUID = 2570793486096324413L;

	/**
	 * Create a new ArrayList from an array of items.
	 * @param array The items for the new list.
	 * 		If null, the new list will be empty.
	 */
	public ArrayToList(T[] array)
	{
		super(array != null ? array.length : 0);
		if (array != null) {
			for (T elem: array) {
				add(elem);
			}
		}
	}
	
	/**
	 * Create an {@link ArrayList} of Integers from an int array.
	 * @param array The items for the new list.
	 * @return A list with the items in the array.
	 * 		If the array is null, return a 0-length list.
	 */
	public static ArrayList<Integer> toList(int[] array)
	{
		ArrayList<Integer> list = new ArrayList<>(array != null ? array.length : 0);
		if (array != null) {
			for (int v : array) {
				list.add(v);
			} 
		}
		return list;
	}
	
	/**
	 * Create an {@link ArrayList} of Longs from a long array.
	 * @param array The items for the new list.
	 * @return A list with the items in the array.
	 * 		If the array is null, return a 0-length list.
	 */
	public static ArrayList<Long> toList(long[] array)
	{
		ArrayList<Long> list = new ArrayList<>(array != null ? array.length : 0);
		if (array != null) {
			for (long v : array) {
				list.add(v);
			} 
		}
		return list;
	}
	
	/**
	 * Create an {@link ArrayList} of Bytes from a byte array.
	 * @param array The items for the new list.
	 * @return A list with the items in the array.
	 * 		If the array is null, return a 0-length list.
	 */
	public static ArrayList<Byte> toList(byte[] array)
	{
		ArrayList<Byte> list = new ArrayList<>(array != null ? array.length : 0);
		if (array != null) {
			for (byte v : array) {
				list.add(v);
			} 
		}
		return list;
	}
	
	/**
	 * Create an {@link ArrayList} of Characters from a char array.
	 * @param array The items for the new list.
	 * @return A list with the items in the array.
	 * 		If the array is null, return a 0-length list.
	 */
	public static ArrayList<Character> toList(char[] array)
	{
		ArrayList<Character> list = new ArrayList<>(array != null ? array.length : 0);
		if (array != null) {
			for (char v : array) {
				list.add(v);
			} 
		}
		return list;
	}
	
	/**
	 * Create an {@link ArrayList} of Doubles from a double array.
	 * @param array The items for the new list.
	 * @return A list with the items in the array.
	 * 		If the array is null, return a 0-length list.
	 */
	public static ArrayList<Double> toList(double[] array)
	{
		ArrayList<Double> list = new ArrayList<>(array != null ? array.length : 0);
		if (array != null) {
			for (double v : array) {
				list.add(v);
			} 
		}
		return list;
	}
	
	/**
	 * Create an {@link ArrayList} of Floats from a float array.
	 * @param array The items for the new list.
	 * @return A list with the items in the array.
	 * 		If the array is null, return a 0-length list.
	 */
	public static ArrayList<Float> toList(float[] array)
	{
		ArrayList<Float> list = new ArrayList<>(array != null ? array.length : 0);
		if (array != null) {
			for (float v : array) {
				list.add(v);
			} 
		}
		return list;
	}
}
