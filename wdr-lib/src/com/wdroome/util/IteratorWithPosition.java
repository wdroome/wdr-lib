package com.wdroome.util;

import java.util.Iterator;

/**
 * A String iterator that also provides a readable description
 * of the position of the current element. For a file, this might
 * be the line number and file name. For an array, it might be
 * the item's index.
 * @author wdr
 */
public interface IteratorWithPosition<T>
		extends Iterable<T>, Iterator<T>
{
	/**
	 * Return a readable description of the "position"
	 * of the most recently returned element.
	 */
	public String getPositionDescription();
}
