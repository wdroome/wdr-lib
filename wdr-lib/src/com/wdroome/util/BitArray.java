package com.wdroome.util;

/**
 * A simple fixed-size bit array.
 * This class is not synchronized.
 * @author wdr
 */
public class BitArray
{
	// The array size, in bits.
	private final int m_nBits;
	
	// The bits, in big-endian order.
	private final byte[] m_bits;
	
	// The number of bits currently set.
	private int m_nSet = 0;
	
	// The obvious, just in case you didn't know.
	private static final int BITS_PER_BYTE = 8;
	
	// Big-endian masks for bits 0 through BIT_PER_BYTE-1.
	private static final byte[] BIT_MASKS = new byte[] {
			(byte)0x80,
			(byte)0x40,
			(byte)0x20,
			(byte)0x10,
			(byte)0x08,
			(byte)0x04,
			(byte)0x02,
			(byte)0x01,
		};
	
	/**
	 * Create a new bit array.
	 * @param nBits The number of bits in the array.
	 */
	public BitArray(int nBits)
	{
		m_nBits = nBits;
		m_bits = new byte[(nBits+BITS_PER_BYTE)/BITS_PER_BYTE];
	}
	
	/**
	 * Return the array size, in bits.
	 * @return The array size, in bits.
	 */
	public int size()
	{
		return m_nBits;
	}
	
	/**
	 * Set or reset a bit.
	 * @param iBit The bit index, 0 to nBits-1.
	 * @param newValue True to set, false to reset.
	 * @return The previous value for that bit.
	 * @throws IndexOutOfBoundsException
	 * 		If iBit is out of bounds.
	 */
	public boolean set(int iBit, boolean newValue)
	{
		if (iBit < 0 || iBit > m_nBits) {
			throw new IndexOutOfBoundsException("BitArray index "
							+ iBit + " out of [0," + m_nBits + "]");
		}
		int iByte = iBit/BITS_PER_BYTE;
		byte mask = BIT_MASKS[iBit % BITS_PER_BYTE];
		boolean prevValue = (m_bits[iByte] & mask) != 0 ? true : false;
		if (newValue != prevValue) {
			if (newValue) {
				m_bits[iByte] |= mask;
				m_nSet++;
			} else {
				m_bits[iByte] &= ~mask;
				m_nSet--;
			}
		}
		return prevValue;
	}
	
	/**
	 * Set or reset a bit.
	 * @param iBit The bit index, 0 to nBits-1.
	 * @param newValue 0 to clear, anything else to set.
	 * @return 1 if the bit had been set before, 0 if not.
	 * @throws IndexOutOfBoundsException
	 * 		If iBit is out of bounds.
	 */
	public int set(int iBit, int newValue)
	{
		return set(iBit, newValue != 0 ? true : false) ? 1 : 0;
	}
	
	/**
	 * Test if a bit is set.
	 * @param iBit The bit index, 0 to nBits-1.
	 * @return True if the bit is set, false if not.
	 * @throws IndexOutOfBoundsException
	 * 		If iBit is out of bounds.
	 */
	public boolean isSet(int iBit)
	{
		if (iBit < 0 || iBit > m_nBits) {
			throw new IndexOutOfBoundsException("BitArray index "
							+ iBit + " out of [0," + m_nBits + "]");
		}
		return (m_bits[iBit/BITS_PER_BYTE] & BIT_MASKS[iBit%BITS_PER_BYTE]) != 0
					? true : false;
	}
	
	/**
	 * Return the current value of a bit.
	 * @param iBit The bit index, 0 to nBits-1.
	 * @return 1 if the bit is set, 0 if not.
	 * @throws IndexOutOfBoundsException
	 * 		If iBit is out of bounds.
	 */
	public int get(int iBit)
	{
		if (iBit < 0 || iBit > m_nBits) {
			throw new IndexOutOfBoundsException("BitArray index "
							+ iBit + " out of [0," + m_nBits + "]");
		}
		return (m_bits[iBit/BITS_PER_BYTE] & BIT_MASKS[iBit%BITS_PER_BYTE]) != 0
					? 1 : 0;
	}
	
	/**
	 * Test if all bits are off.
	 * @return True iff all bits are off.
	 */
	public boolean allClear()
	{
		return m_nSet == 0;
	}
}
