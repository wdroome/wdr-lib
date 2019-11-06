package com.wdroome.util;

/**
 *	Represent tag-length-value triple
 *	as an integer tag and a ByteAOL segment.
 *	Classes can access data members directly.
 */
public class TagAOL implements java.io.Serializable, Cloneable
{
	private static final long serialVersionUID = -3887326650617258900L;

	/** The tag value. */
	public final int tag;

	/** The length and byte[] value. */
	public final ByteAOL value;

	/** Create a new TagAOL from the indicated tag and ByteAOL. */
	public TagAOL(int tag, ByteAOL value)
	{
		this.tag = tag;
		this.value = value;
	}

	/** Return a string representation of the tag, length, and value. */
	public String toString()
	{
		return tag + "/" + value.toString();
	}

	/**
	 *	Return a shallow clone. The clone has the same offset and length
	 *	as the source, and shares a reference to the array.
	 *<p>
	 *	This is equivalent to new TagAOL(tag,value).
	 */
	public Object clone()
	{
		return new TagAOL(tag, value);
	}

	/**
	 *	Return a deep clone. The deep clone's value will be
	 *	a new ByteAOL with a copy of the bytes used by this object.
	 *<p>
	 *	This is equivalent to new TagAOL(tag, value.deepCopy()).
	 */
	public TagAOL deepCopy()
	{
		return new TagAOL(tag, value.deepCopy());
	}
}
