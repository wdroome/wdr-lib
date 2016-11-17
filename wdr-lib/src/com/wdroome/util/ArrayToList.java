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
}
