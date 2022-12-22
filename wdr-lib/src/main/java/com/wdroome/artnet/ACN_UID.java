package com.wdroome.artnet;

import java.util.Arrays;

/*
 * Represent a ACN UID.
 */
public class ACN_UID
{
	public static final int SACN_UID_LENGTH = 6;
	
	public final byte[] m_bytes;
	
	public ACN_UID()
	{
		this(null, 0);
	}
	
	public ACN_UID(byte[] src)
	{
		this(src, 0);
	}
	
	public ACN_UID(byte[] src, int offset)
	{
		if (src == null) {
			m_bytes = new byte[SACN_UID_LENGTH];
		} else if (offset + SACN_UID_LENGTH <= src.length) {
			m_bytes = Arrays.copyOfRange(src, offset, offset + SACN_UID_LENGTH - 1);
		} else {
			throw new IllegalArgumentException("ACN_UID incorrect len " + src.length);
		}
	}
	
	@Override
	public String toString()
	{
		StringBuilder buff = new StringBuilder();
		String sep = "";
		for (byte b: m_bytes) {
			buff.append(sep);
			buff.append(Integer.toHexString(b & 0xff));
			sep = ":";
		}
		return buff.toString();
	}
}
