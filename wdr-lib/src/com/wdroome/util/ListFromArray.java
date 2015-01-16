// tabs every 4

 //================================================================
 //  Alcatel-Lucent provides the software in this file for the
 //  purpose of obtaining feedback.  It may be copied and modified.
 // 
 //  Alcatel-Lucent is not liable for any consequence of loading
 //  or running this software or any modified version thereof.
 //================================================================

package com.wdroome.util;

import java.util.AbstractList;
import java.util.List;
import java.util.Iterator;
import java.util.ListIterator;

/**
 *	A class that creates a read-only {@link List} from an arbitrary array.
 *	For example, if you want to pass the String array "foo"
 *	to a method that expects a List of Strings, you can pass
 *	<code>new ListForArray&lt;String&gt;(foo)</code> to that method.
 *<p>
 *	This class does not modify the underlying array,
 *	so the add(), clear(), remove() and set() methods
 *	throw an UnsupportedOperationException.
 *<p>
 *	Aside: It seems like a class like this should have been added to
 *	the standard java library when they added generic types to java.
 *	But if it exists, I haven't found it.
 */
public class ListFromArray<T> extends AbstractList<T>
{
	private final T[] array;

	/** Create a List from an array. */
	public ListFromArray(T[] array) { this.array = array; }

	/** Return the i'th element of the array. */
	@Override
	public T get(int i) { return array[i]; }

	/** Return the size of the array. */
	@Override
	public int size() { return array.length; }

	/** Return an Iterator for the elements of the array. */
	@Override
	public Iterator<T> iterator() { return new ArrayIterator<T>(array); }

	/** Return a ListIterator for the elements of the array. */
	@Override
	public ListIterator<T> listIterator() { return new ArrayIterator<T>(array); }

	/** Return a ListIterator for the elements of the array, starting at index. */
	@Override
	public ListIterator<T> listIterator(int index) { return new ArrayIterator<T>(array, index, -1); }
}
