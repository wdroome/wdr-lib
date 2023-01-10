package com.wdroome.util;

import java.net.UnknownHostException;

/**
 *	A segment of a byte array:
 *	the array, the starting offset, and the length.
 *<p>
 *	ByteAOLs are immutable: once created, you cannot
 *	change the offset or the length.
 *<p>
 *	Gotcha: It is possible to change the bytes in the array
 *	after a ByteAOL has been created.
 *	However, unless you're using a Filler, we do not recommend that.
 *<p>
 *	Tip: To convert a byte[] b to a hex string, use new ByteAOL(b).toHex().
 *<p>
 *	Another tip: To convert a string of hex digits to a ByteAOL, use new ByteAOL(s).
 *<p>
 *	Note on serialization: This class is serializable.
 *	However, to save space, we do NOT serialize the entire source array.
 *	Instead, we just serialize the "len" bytes
 *	that this object actually uses. A reconstituted ByteAOL
 *	will always have off==0 and len==arr.length. This also means
 *	that if you have several different ByteAOLs, all of which refer to hunks
 *	of the same underlying byte array, when you serialize and deserialize them,
 *	each of the reconstituted ByteAOLs will have its own array;
 *	they will not share a byte array.
 */
public class ByteAOL implements java.io.Serializable, Cloneable, Comparable<ByteAOL>
{
	private static final long serialVersionUID = 7896321688160835529L;

	/** The (read-only) array. Never null. */
	private final byte[] m_arr;

	/** The (read-only) starting offset. */
	private final int m_off;

	/** The (read-only) length. May be 0. */
	private final int m_len;
	
	/** Whether numeric values are big-endian or little-endian. */
	private boolean littleEndian = false;

	/** Hex digits when converting to/from Strings. */
	private final static char[] hexDigits = {
						'0', '1', '2', '3', '4', '5', '6', '7',
						'8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	/**
	 *	Construct a new segment.
	 *	@param arr
	 *		The byte array.  We keep a reference to this array,
	 *		so we assume it does not change.
	 *	@param off
	 *		The starting offset of the segment within arr.
	 *	@param len
	 *		The length of the segment. May be zero.
	 *	@throws IllegalArgumentException
	 *		If arr is null, or if off is negative,
	 *		or if off+len is over the end of arr.
	 */
	public ByteAOL(byte[] arr, int off, int len)
	{
		if (arr == null) {
			throw new IllegalArgumentException("new ByteAOL(): null byte[]");
		}
		if (len > 0) {
			if (off < 0 || off + len > arr.length) {
				throw new IllegalArgumentException("new ByteAOL(): invalid offset/len "
								+ off + "/" + len);
			}
		} else if (len < 0) {
			len = 0;
		}
		this.m_arr = arr;
		this.m_off = off;
		this.m_len = len;
	}

	/**
	 *	Construct a new segment with the full contents of an array.
	 *	@param arr The byte array.
	 */
	public ByteAOL(byte[] arr)
	{
		this(arr, 0, arr.length);
	}

	/**
	 *	Construct a subsegment of an existing ByteAOL.
	 *	@param src
	 *		The source ByteAOL.  We keep a reference to the underlying byte array,
	 *		so we assume it does not change.
	 *	@param off
	 *		The starting offset of the segment within the source ByteAOL.
	 *	@param len
	 *		The length of the segment. May be zero.
	 *	@throws IllegalArgumentException
	 *		If arr is null, or if off is negative,
	 *		or if off+len is over the end of source segment.
	 */
	public ByteAOL(ByteAOL src, int off, int len)
	{
		if (src == null) {
			throw new IllegalArgumentException("new ByteAOL(): null ByteAOL");
		}
		if (len > 0) {
			if (off < 0 || off + len > src.m_len) {
				throw new IllegalArgumentException("new ByteAOL(): invalid offset/len "
								+ off + "/" + len);
			}
		} else if (len < 0) {
			len = 0;
		}
		this.m_arr = src.m_arr;
		this.m_off = src.m_off + off;
		this.m_len = len;
		this.littleEndian = src.littleEndian;
	}
	

	/**
	 *	Construct a subsegment of an existing ByteAOL.
	 *	@param src
	 *		The source ByteAOL.  We keep a reference to the underlying byte array,
	 *		so we assume it does not change.
	 *	@param off
	 *		The starting offset of the segment within the source ByteAOL.
	 *		The segment runs to the end of src.
	 *	@throws IllegalArgumentException
	 *		If arr is null, or if off is negative.
	 */
	public ByteAOL(ByteAOL src, int off)
	{
		this(src, off, src.m_len - off);
	}

	/**
	 *	Convert a string of hex digits to a byte array,
	 *	and return it as a ByteAOL.
	 *	We ignore any white space or periods or colons in the middle of the string.
	 *	Thus "01 02", "01.02" and "01:02" are equivalent to "0102".
	 *	However, note that "0:1" means "01", not "0001".
	 *<p>
	 *	If the string has an odd number of hex digits,
	 *	add a zero pad at the end, if it's 3 digits or longer.
	 *	or at the beginning, if it's only one digit.
	 *	That is, "1" is equivalent to "01",
	 *	while "123" is equivalent to "1230".
	 *
	 *	@param hexSrc
	 *		A string of hex digits, upper or lower case.
	 *		May start with "0x".
	 *	@throws NumberFormatException
	 *		If a character is not valid hex digit (or white space or a period).
	 */
	public ByteAOL(String hexSrc)
		throws NumberFormatException
	{
		this(hexSrc, HEX_STR);
	}
	
	/** Hex-string format. See {@link #ByteAOL(String,int)}. */
	public static final int HEX_STR = 1;
	
	/** Dotted-decimal-string format. See {@link #ByteAOL(String,int)}. */
	public static final int DOTTED_DEC_STR = 2;
	
	/** IP-address-string format. See {@link #ByteAOL(String,int)}. */
	public static final int IPADDR_STR = 3;
	
	/** Colon-hexadecimal-string format. See {@link #ByteAOL(String,int)}. */
	public static final int COLON_HEX_STR = 4;
	
	/** Hex-or-decimal-string format. See {@link #ByteAOL(String,int)}. */
	public static final int HEX_OR_DEC_STR = 5;

	/**
	 *	Create a byte array from a string. strType describes the string format:
	 *<p>
	 *	{@link #HEX_STR}:
	 *	A string of hex digits.
	 *	We ignore any white space or periods or colons in the middle of the string.
	 *	Thus "01 02", "01.02" and "01:02" are equivalent to "0102".
	 *	However, note that "0:1" means "01", not "0001".
	 *	If the string has an odd number of hex digits,
	 *	add a zero pad at the end, if it's 3 digits or longer.
	 *	or at the beginning, if it's only one digit.
	 *	That is, "1" is equivalent to "01",
	 *	while "123" is equivalent to "1230".
	 *	The hex string may start with the "0x" prefix.
	 *<p>
	 *	{@link #DOTTED_DEC_STR}:
	 *	A string of decimal numbers, separated by dots,
	 *	one number per byte.
	 *<p>
	 *	{@link #COLON_HEX_STR}:
	 *	A string of hexadecimal numbers, separated by colons,
	 *	one number per byte.
	 *<p>
	 *	{@link #HEX_OR_DEC_STR}:
	 *	A string of numbers, one per byte. If the numbers are separated by colons,
	 *	they're hex; if they're separated by periods, they're decimal.
	 *	That is, if the string contains a colon, this is equivalent to
	 *	{@link #COLON_HEX_STR}. If not, this is equivalent to {@link #DOTTED_DEC_STR}.
	 *<p>
	 *	{@link #IPADDR_STR}:
	 *	A v4 or v6 IP address. If the string starts with '[' or contains a ':',
	 *	we assume it's a colon-separated 16-byte IPV6 address.
	 *	Otherwise we assume it's a 4-byte dotted-decimal IPV4 address.
	 *
	 *	@param src
	 *		The source string.
	 *	@param strType
	 *		The string type. {@link #HEX_STR}, {@link #DOTTED_DEC_STR}, or {@link #IPADDR_STR}.
	 *	@throws NumberFormatException
	 *		If a character is not valid digit.
	 */
	public ByteAOL(String src, int strType)
			throws NumberFormatException
	{
		if (src == null) {
			this.m_arr = new byte[0];
			this.m_off = 0;
			this.m_len = 0;
			return;
		}
		int srcLen = src.length();
		int start = 0;
		while (start < srcLen && Character.isWhitespace(src.charAt(start))) {
			start++;
		}
		while (srcLen > start && Character.isWhitespace(src.charAt(srcLen-1))) {
			srcLen--;
		}
		if (strType == HEX_OR_DEC_STR) {
			strType = (src.indexOf(':') >= 0) ? COLON_HEX_STR : DOTTED_DEC_STR;
		}
		switch (strType) {
		  case HEX_STR:
		  default:
		  	{
				if (src.startsWith("0x")) {
					start += 2;
				}
				int nDigits = srcLen - start;
				for (int i = start; i < srcLen; i++) {
					char c = src.charAt(i);
					if (c == '.' || c == ':' || Character.isWhitespace(c)) {
						nDigits -= 1;
					}
				}
				int nBytes = (nDigits+1)/2;
				this.m_arr = new byte[nBytes];
				if (nDigits == 1) {
					m_arr[0] = (byte)hexDigitValue(src.charAt(start));
				} else {
					int iByte = 0;
					int upperNibble = -1;
					for (int i = start; i < srcLen; i++) {
						char c = src.charAt(i);
						if (c != '.' && c != ':' && !Character.isWhitespace(c)) {
							int nibble = hexDigitValue(src.charAt(i));
							if (upperNibble >= 0) {
								m_arr[iByte++] = (byte)(upperNibble | nibble);
								upperNibble = -1;
							} else {
								upperNibble = nibble << 4;
							}
						}
					}
					if (upperNibble >= 0) {
						m_arr[iByte] = (byte)upperNibble;
					}
				}
			}
			break;
		  case DOTTED_DEC_STR:
		  	{
		  		String[] numbers = src.substring(start, srcLen).split("\\.");
		  		this.m_arr = new byte[numbers.length];
		  		for (int i = 0; i < numbers.length; i++) {
		  			String s = numbers[i];
		  			if (s.equals("")) {
		  				this.m_arr[i] = 0;
		  			} else {
						this.m_arr[i] = (byte)Integer.parseInt(numbers[i]);
		  			}
		  		}
		  	}
		  	break;
		  case COLON_HEX_STR:
		  	{
		  		String[] numbers = src.substring(start, srcLen).split(":");
		  		this.m_arr = new byte[numbers.length];
		  		for (int i = 0; i < numbers.length; i++) {
		  			String s = numbers[i];
		  			if (s.equals("")) {
		  				this.m_arr[i] = 0;
		  			} else {
						this.m_arr[i] = (byte)Integer.parseInt(numbers[i], 16);
		  			}
		  		}
		  	}
		  	break;
		  case IPADDR_STR:
			{
				this.m_arr = ipAddrStrToBytes(src, start, srcLen);
			}
			break;
		}
		this.m_off = 0;
		this.m_len = this.m_arr.length;
	}

	/**
	 * Equivalent to ByteAOL(src, IPADDR_STR), except that this c'tor
	 * throws an UnknownHostException if src isn't a valid IP address.
	 *
	 * @param src
	 * 		An IP address in dotted decimal or colon hex format.
	 * @param marker
	 * 		A flag argument to select this c'tor.
	 * 		The value is ignored, so pass (UnknownHostException)null.
	 * @throws UnknownHostException
	 * 		If src isn't a valid IP address.
	 */
	public ByteAOL(String src, UnknownHostException marker)
			throws UnknownHostException
	{
		if (src == null || src.equals("")) {
			throw new UnknownHostException("IP Address is null or blank");
		}
		int srcLen = src.length();
		int start = 0;
		while (start < srcLen && Character.isWhitespace(src.charAt(start))) {
			start++;
		}
		while (srcLen > start && Character.isWhitespace(src.charAt(srcLen-1))) {
			srcLen--;
		}
		try {
			this.m_arr = ipAddrStrToBytes(src, start, srcLen);
		} catch (NumberFormatException e) {
			throw new UnknownHostException(e.getMessage());
		}
		this.m_off = 0;
		this.m_len = this.m_arr.length;
	}

	/**
	 * Parse src as an IP address, and return the address as a byte array.
	 * @param src The source address.
	 * @param start Start with this index into src.
	 * @param srcLen Use this many bytes from src.
	 * @return A byte[4] or byte[16] array with the address bits.
	 * @throws NumberFormatException If src isn't a valid IP address.
	 */
	private static byte[] ipAddrStrToBytes(String src, int start, int srcLen)
			throws NumberFormatException
	{
		boolean v6 = false;
		if (src.charAt(start) == '[') {
			v6 = true;
			start++;
			if (src.charAt(srcLen-1) == ']') {
				--srcLen;
			}
		} else if (src.indexOf(':') >= 0) {
			v6 = true;
		}
		byte[] arr;
		if (!v6) {
			String[] numbers = src.substring(start, srcLen).split("\\.");
			if (numbers.length != 4) {
				throw new NumberFormatException(src.substring(start, srcLen)
												+ " is not a proper IPV4 address");
			}
			arr = new byte[4];
			for (int i = 0; i < numbers.length && i < arr.length; i++) {
				String s = numbers[i];
				if (s.equals("")) {
					arr[i] = 0;
				} else {
					arr[i] = (byte)Integer.parseInt(numbers[i]);
				}
			}
  		} else {
  			String[] numbers = src.substring(start, srcLen).split(":", -1);
			if (numbers.length <= 2 || numbers.length > 8) {
				throw new NumberFormatException(src.substring(start, srcLen)
												+ " is not a proper IPV6 address");
			}
 			arr = new byte[16];
 			int arrLen = 16;
 			int numbersLength = numbers.length;
			String lastHunk = numbers[numbersLength-1];
			if (lastHunk.indexOf('.') > 0) {
				byte[] last4 = ipAddrStrToBytes(lastHunk, 0, lastHunk.length());
				arr[12] = last4[0];
				arr[13] = last4[1];
				arr[14] = last4[2];
				arr[15] = last4[3];
				arrLen = 12;
				numbersLength -= 1;
				if (numbersLength > 6) {
					throw new NumberFormatException(src.substring(start, srcLen)
													+ " is not a proper IPV6 address");
				}
			}
  			int iByte = 0;
  			for (int iNum = 0; iNum < numbersLength; iNum++) {
  				String num = numbers[iNum];
  				if (!num.equals("")) {
  					if (iByte+1 < arrLen) {
						int v = Integer.parseInt(num, 16);
						arr[iByte++] = (byte)(v >> 8);
						arr[iByte++] = (byte)v;
					}
				} else {
					if (iNum == 0 && iNum+1 < numbersLength && numbers[iNum+1].equals("")) {
						++iNum;
					}
  					int zerofill = arrLen - iByte - 2*(numbersLength - iNum - 1);
  					for (int iz = 0; iz < zerofill; iz++) {
  						arr[iByte++] = 0;
  					}
  				}
  			}		  					
  		}
		return arr;
	}

	/**
	 *	Return a new ByteAOL that has the bytes of this one concatenated
	 *	with the bytes of another. The new ByteAOL has the endian status
	 *	of this ByteAOL.
	 *	@param add A ByteAOL to append to the end of this one.
	 *	@return A new ByteAOL with this one's array followed by add's array.
	 */
	public ByteAOL concat(ByteAOL add)
	{
		byte[] newBytes = new byte[this.m_len + add.m_len];
		for (int i = 0; i < this.m_len; i++) {
			newBytes[i] = this.m_arr[this.m_off+i];
		}
		for (int i = 0; i < add.m_len; i++) {
			newBytes[this.m_len+i] = add.m_arr[add.m_off + i];
		}
		ByteAOL newObj = new ByteAOL(newBytes);
		newObj.littleEndian = this.littleEndian;
		return newObj;
	}
	
	/**
	 *	Return true iff multi-byte numbers
	 *	are to be treated as little-endian.
	 */
	public boolean isLittleEndian() { return littleEndian; }
	
	/**
	 *	Set whether multi-byte numbers are to be treated as little-endian.
	 *	@param littleEndian
	 *		New status; true means numbers are little-endian.
	 *		Default is false.
	 *	@return Previous status.
	 */
	public boolean setLittleEndian(boolean littleEndian)
	{
		boolean prev = this.littleEndian;
		this.littleEndian = littleEndian;
		return prev;
	}

	/**
	 *	Return the numeric value of the hex digit "c".
	 *	If "c" is not a valid hex digit (upper or lower case),
	 *	throw a NumberFormatExcetion.
	 */
	private int hexDigitValue(char c)
		throws NumberFormatException
	{
		int v = Character.digit(c, 16);
		if (v >= 0) {
			return v;
		} else {
			throw new NumberFormatException("Illegal hex digit " + c);
		}
	}

	/**
	 *	Return the length of the segment, in bytes.
	 */
	public int size() { return m_len; }

	/**
	 *	Return the offset of the start of the segment, in bytes.
	 */
	public int getOffset() { return m_off; }
	
	/**
	 *	Return the underlying byte array.
	 */
	public byte[] getArray() { return m_arr; }

	/**
	 *	Return the i'th byte of the segment.
	 *	@param i
	 *		Index within segment (0 gives first byte).
	 *	@throws IndexOutOfBoundsException
	 *		If i is not in the range [0,len).
	 */
	public byte get(int i)
	{
		if (i < 0 || i >= m_len) {
			throw new IndexOutOfBoundsException(
						"ByteAOL.get(): bad index " + i + " len=" + m_len);
		}
		return m_arr[m_off+i];
	}

	/**
	 *	Return a shallow clone. The clone has the same offset and length
	 *	as the source, and shares a reference to the array.
	 *<p>
	 *	This is equivalent to new ByteAOL(arr,off,len).
	 */
	public Object clone()
	{
		ByteAOL newObj = new ByteAOL(m_arr, m_off, m_len);
		newObj.setLittleEndian(isLittleEndian());
		return newObj;
	}

	/**
	 *	Return a deep clone. The deep clone's array will be
	 *	a new byte[] with a copy of the len bytes used by this object.
	 *	In the returned clone, "off" will be 0, "arr" will be "len" bytes long.
	 *<p>
	 *	This is equivalent to new ByteAOL(getBytes(true)).
	 */
	public ByteAOL deepCopy()
	{
		ByteAOL newObj = new ByteAOL(getBytes(true));
		newObj.setLittleEndian(isLittleEndian());
		return newObj;
	}

	/**
	 *	Copy the bytes in this segment to an array.
	 *	@param dest The destination array.
	 *	@param destOff The starting offset within dest.
	 */
	public void copyBytes(byte[] dest, int destOff)
	{
		for (int i = 0; i < m_len; i++) {
			dest[destOff + i] = m_arr[m_off + i];
		}
	}

	/**
	 *	Return a byte[] with the bytes in this segment.
	 *	If the segment covers the full array
	 *	-- that is, the segment offset is 0
	 *	and the segment length is the array length --
	 *	then we return the underlying array.
	 *	Otherwise, we allocate a new byte array
	 *	and copy the bytes in the segment into it.
	 *	
	 *	@return A byte array with the value of this segment.
	 */
	public byte[] getBytes()
	{
		return getBytes(false);
	}

	/**
	 *	Return a byte[] with the bytes in this segment.
	 *	If the segment covers the full array
	 *	-- that is, the segment offset is 0
	 *	and the segment length is the array length --
	 *	then we return the underlying array.
	 *	Otherwise, we allocate a new byte array
	 *	and copy the bytes in the segment into it.
	 *	
	 *	@param forceCopy
	 *		If true, always create a new byte array,
	 *		even if the segment covers the full array.
	 *	@return A byte array with the value of this segment.
	 */
	public byte[] getBytes(boolean forceCopy)
	{
		if (m_off == 0 && m_len == m_arr.length && !forceCopy) {
			return m_arr;
		} else {
			byte[] ret = new byte[m_len];
			for (int i = 0; i < m_len; i++)
				ret[i] = m_arr[m_off+i];
			return ret;
		}
	}

	/**
	 *	Create and return a new String from the bytes in this segment.
	 */
	public String getString()
	{
		return new String(m_arr, m_off, m_len);
	}

	/**
	 *	Create and return a new String from a subset of the bytes in this segment.
	 */
	public String getString(int start, int len)
	{
		return new String(m_arr, m_off+start, len);
	}

	/**
	 *	If the value fits in an int, return the value as an (unsigned) int.
	 *	The number is normally big-endian, unless the little-endian flag is set.
	 *
	 *	@throws IllegalStateException If the length is greater than 4.
	 */
	public int getInt()
	{
		if (m_len > 4) {
			throw new IllegalStateException("ByteAOL.getInt() called on a "
								+ m_len + " byte value.");
		}
		if (!littleEndian) {
			switch (m_len) {
				default: return 0;
	
				case 1: return m_arr[m_off] & 0xff;
	
				case 2: return    ((m_arr[m_off  ] & 0xff) <<  8)
								|  (m_arr[m_off+1] & 0xff);
	
				case 3: return	  ((m_arr[m_off  ] & 0xff) << 16)
								| ((m_arr[m_off+1] & 0xff) <<  8)
								|  (m_arr[m_off+2] & 0xff);
	
				case 4: return	  ((m_arr[m_off  ] & 0xff) << 24)
								| ((m_arr[m_off+1] & 0xff) << 16)
								| ((m_arr[m_off+2] & 0xff) <<  8)
								|  (m_arr[m_off+3] & 0xff);
			}
		} else {
			switch (m_len) {
				default: return 0;
	
				case 1: return m_arr[m_off] & 0xff;
	
				case 2: return    ((m_arr[m_off+1] & 0xff) <<  8)
								|  (m_arr[m_off  ] & 0xff);
	
				case 3: return	  ((m_arr[m_off+2] & 0xff) << 16)
								| ((m_arr[m_off+1] & 0xff) <<  8)
								|  (m_arr[m_off  ] & 0xff);
	
				case 4: return	  ((m_arr[m_off+3] & 0xff) << 24)
								| ((m_arr[m_off+2] & 0xff) << 16)
								| ((m_arr[m_off+1] & 0xff) <<  8)
								|  (m_arr[m_off  ] & 0xff);
			}
		}
	}

	/**
	 *	If the value fits in a long, return the value as an (unsigned) long.
	 *	The number is normally big-endian, unless the little-endian flag is set.
	 *
	 *	@throws IllegalStateException If the length is greater than 8.
	 */
	public long getLong()
	{
		if (m_len > 8) {
			throw new IllegalStateException("ByteAOL.getLong() called on a "
								+ m_len + " byte value.");
		}
		long v = 0;
		int endOff = m_off + m_len;
		if (!littleEndian) {
			for (int i = m_off; i < endOff; i++) {
				v <<= 8;
				v |= m_arr[i] & 0xff;
			}
		} else {
			for (int i = endOff-1; i >= 0; i--) {
				v <<= 8;
				v |= m_arr[i] & 0xff;
			}
		}
		return v;
	}
	
	/**
	 *	Return the two bytes starting at "i" as an unsigned (big-endian) int.
	 *	The number is normally big-endian, unless the little-endian flag is set.
	 *	@throws IndexOutOfBoundsException If i and i+1 aren't in range.
	 */
	public int get2ByteUInt(int i)
		throws IndexOutOfBoundsException
	{
		if (i < 0 || i+1 >= m_len) {
			throw new IndexOutOfBoundsException(
						"ByteAOL.get2ByteUInt(): bad index " + i + " len=" + m_len);
		}
		i += m_off;
		int v;
		if (!littleEndian) {
			v  = (m_arr[i] & 0xff) << 8;
			v |= m_arr[i+1] & 0xff;
		} else {
			v  = (m_arr[i+1] & 0xff) << 8;
			v |= m_arr[i] & 0xff;
		}
		return v;
	}
	
	/**
	 *	Return the four bytes starting at "i" as an unsigned (big-endian) long.
	 *	The number is normally big-endian, unless the little-endian flag is set.
	 *	@throws IndexOutOfBoundsException If i and i+3 aren't in range.
	 */
	public long get4ByteUInt(int i)
		throws IndexOutOfBoundsException
	{
		if (i < 0 || i+3 >= m_len)
			throw new IndexOutOfBoundsException(
						"ByteAOL.get4ByteUInt(): bad index " + i + " len=" + m_len);
		i += m_off;
		long v;
		if (!littleEndian) {
			v  = (long)(m_arr[i] & 0xff) << 24;
			v |= (m_arr[i+1] & 0xff) << 16;
			v |= (m_arr[i+2] & 0xff) << 8;
			v |= m_arr[i+3] & 0xff;
		} else {
			v  = (long)(m_arr[i+3] & 0xff) << 24;
			v |= (m_arr[i+2] & 0xff) << 16;
			v |= (m_arr[i+1] & 0xff) << 8;
			v |= m_arr[i] & 0xff;
		}
		return v;
	}

	/**
	 *	Append the bytes to "buff" as 2-digit hex numbers.
	 *	Use lower case digits (a-f).
	 */
	public void appendHex(StringBuffer buff)
	{
		int endOff = m_off + m_len;
		for (int i = m_off; i < endOff; i++) {
		  	byte b = m_arr[i];
			buff.append(hexDigits[(b >> 4) & 0xf]);
		   	buff.append(hexDigits[(b     ) & 0xf]);
		}
	}

	/**
	 *	Append the bytes to "buff" as dot-separated decimal numbers.
	 */
	public void appendDottedDecimal(StringBuffer buff)
	{
		int endOff = m_off + m_len;
		for (int i = m_off; i < endOff; i++) {
		  	byte b = m_arr[i];
		  	if (i != m_off) {
				buff.append('.');
		  	}
		  	buff.append(b & 0xff);
		}
	}

	/**
	 *	Append the bytes to "buff" as colon-separated hexadecimal numbers.
	 */
	public void appendColonHex(StringBuffer buff)
	{
		int endOff = m_off + m_len;
		for (int i = m_off; i < endOff; i++) {
		  	int b = m_arr[i] & 0xff;
		  	if (i != m_off) {
				buff.append(':');
		  	}
			if (b >= 0x10) {
				buff.append(hexDigits[b >> 4]);
			}
		  	buff.append(hexDigits[b & 0xf]);
		}
	}

	/**
	 *	Append the bytes to "buff" as 2-digit hex numbers.
	 *	Use lower case digits (a-f).
	 */
	public void appendHex(StringBuilder buff)
	{
		int endOff = m_off + m_len;
		for (int i = m_off; i < endOff; i++) {
		  	byte b = m_arr[i];
			buff.append(hexDigits[(b >> 4) & 0xf]);
		   	buff.append(hexDigits[(b     ) & 0xf]);
		}
	}

	/**
	 *	Append the bytes to "buff" as dot-separated decimal numbers.
	 */
	public void appendDottedDecimal(StringBuilder buff)
	{
		int endOff = m_off + m_len;
		for (int i = m_off; i < endOff; i++) {
		  	byte b = m_arr[i];
		  	if (i != m_off) {
				buff.append('.');
		  	}
		  	buff.append(b & 0xff);
		}
	}

	/**
	 *	Append the bytes to "buff" as colon-separated hexadecimal numbers.
	 */
	public void appendColonHex(StringBuilder buff)
	{
		int endOff = m_off + m_len;
		for (int i = m_off; i < endOff; i++) {
		  	int b = m_arr[i] & 0xff;
		  	if (i != m_off) {
				buff.append(':');
		  	}
			if (b >= 0x10) {
				buff.append(hexDigits[b >> 4]);
			}
		  	buff.append(hexDigits[b & 0xf]);
		}
	}

	/**
	 *	Return the bytes as a String of 2-digit hex numbers.
	 *	Use lower case digits (a-f).
	 */
	public String toHex()
	{
		StringBuilder b = new StringBuilder(2*m_len);
		appendHex(b);
		return b.toString();
	}

	/**
	 *	Return the bytes as a String of dot-separated decimal numbers.
	 */
	public String toDottedDecimal()
	{
		StringBuilder b = new StringBuilder(5*m_len);
		appendDottedDecimal(b);
		return b.toString();
	}

	/**
	 *	Return the bytes as a String of colon-separated hexadecimal numbers.
	 */
	public String toColonHex()
	{
		StringBuilder b = new StringBuilder(3*m_len);
		appendColonHex(b);
		return b.toString();
	}
	
	/**
	 *	Return the bytes in "IP address" format.
	 *	If the length is 16, use IPV6 format (without the enclosing []).
	 *	Otherwise, use dotted decimal.
	 */
	public String toIPAddr()
	{
		StringBuilder b = new StringBuilder(5*m_len);
		if (m_len != 16) {
			appendDottedDecimal(b);
		} else {
			int values[] = new int[8];
			for (int i = 0; i < values.length; i++) {
				values[i] = get2ByteUInt(2*i);
			}
			if (values[0] == 0 && values[1] == 0) {
				b.append(':');
				int i = 0;
				for (; i < values.length-1 && values[i] == 0; i++) {
					;
				}
				for (; i < values.length; i++) {
					b.append(':');
					b.append(Integer.toHexString(values[i]));
				}
			} else {
				boolean needSep = false;
				boolean usedDoubleColon = false;
				for (int i = 0; i < values.length; i++) {
					if (!usedDoubleColon
					  && values[i] == 0
					  && i+1 < values.length-1
					  && values[i+1] == 0) {
						b.append("::");
						needSep = false;
						usedDoubleColon = true;
						for (i++; i < values.length && values[i] == 0; i++) {
							;
						}
						if (i < values.length) {
							--i;
						}
					} else {
						if (needSep) {
							b.append(':');
						}
						b.append(Integer.toHexString(values[i]));
						needSep = true;
					}
				}
			}
		}
		return b.toString();
	}

	/**
	 *	Return a string representation: len/bytes-in-hex.
	 *	To convert the bytes to a String, use {@link #getString()}.
	 */
	@Override
	public String toString()
	{
		StringBuilder b = new StringBuilder(6+ 2*m_len);
		b.append(m_len);
		b.append('/');
		if (m_len > 0) {
			appendHex(b);
		} else {
			b.append('-');
		}
		return b.toString();
	}

	/**
	 *	Append a "formatted hex" dump of xlen bytes starting at xoff to "buff".
	 *	The format is
	 *<pre>
	 *     000010  xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx  |ASCII char or ..|
	 *</pre>
	 *	For example, the following prints a ByteAOL to standard output
	 *	with 16 bytes per line:
	 *<pre>
	 *     ByteAOL data;
	 *     for (int i = 0; i &lt; data.size(); i += 16)
	 *        System.out.println(data.appendFormattedHex(null,i,16));
	 *</pre>
	 *
	 *	@param buff
	 *		The buffer for the formatted hex dump.
	 *		If null, we create a new StringBuilder.
	 *	@param xoff
	 *		Starting offset of bytes to dump within this ByteAOL.
	 *	@param xlen
	 *		Number of bytes to dump. If xoff+xlen is over the end
	 *		of the segment, just dump the remaining bytes.
	 *	@return
	 *		The parameter buff. If null, return the new StringBuilder we created.
	 */
	public StringBuilder appendFormattedHex(StringBuilder buff, int xoff, int xlen)
	{
		if (buff == null) {
			buff = new StringBuilder();
		}
		buff.append(String.format("%06x ", xoff));
		char[] ascii = new char[(xoff+xlen > m_len) ? m_len-xoff : xlen];
		for (int i = 0; i < xlen; i++) {
			if ((i%4) == 0) {
				buff.append(" ");
			}
			if (xoff + i >= m_len) {
				buff.append("  ");
			} else {
				byte b = get(xoff + i);
				buff.append(hexDigits[(b >> 4) & 0xf]);
				buff.append(hexDigits[(b     ) & 0xf]);
				if ( (b >= 'a' && b <= 'z')
				  || (b >= 'A' && b <= 'Z')
				  || (b >= '0' && b <= '9')) {
					ascii[i] = (char)b;
				} else {
					switch (b) {
						case '~':
						case '`':
						case '!':
						case '@':
						case '#':
						case '$':
						case '%':
						case '^':
						case '&':
						case '*':
						case '(':
						case ')':
						case '_':
						case '-':
						case '+':
						case '=':
						case '{':
						case '[':
						case '}':
						case ']':
						case '|':
						case ':':
						case ';':
						case '"':
						case '\'':
						case '<':
						case ',':
						case '>':
						case '.':
						case '?':
						case '/':
						case ' ':
							ascii[i] = (char)b;
							break;
						default:
							ascii[i] = '.';
							break;
					}
				}
		   	}
		}
		buff.append("  |");
		buff.append(ascii);
		buff.append('|');
		return buff;
	}

	/**
	 *	Return true iff this ByteAOL's content is equal
	 *	to that of another ByteAOL.  The arrays in the two objects
	 *	can be different, as long as their segments are the same length.
	 *	and the bytes in their respective segments are identical.
	 */
	public boolean matches(ByteAOL other)
	{
		if (other == null) {
			return false;
		}
		if (m_len != other.m_len) {
			return false;
		}
		for (int i = 0; i < m_len; i++) {
			if (m_arr[m_off+i] != other.m_arr[other.m_off+i]) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 *	Return the sum of the bytes as the object's hashcode.
	 */
	@Override
	public int hashCode()
	{
		int hash = 0;
		int endOff = m_off + m_len;
		for (int i = m_off; i < endOff; i++) {
			hash += m_arr[i];
		}
		return hash;
	}

	/**
	 *	Return true iff the bytes of this object are identical to the bytes of another.
	 *	Equivalent to {@link #matches(ByteAOL)}.
	 */
	@Override
	public boolean equals(Object o)
	{
		if (o instanceof ByteAOL) {
			return matches((ByteAOL)o);
		} else {
			return false;
		}
	}

	/**
	 * Compare this object to another, and return -1, 0 or +1.
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(ByteAOL other)
	{
		for (int i = 0; i < m_len; i++) {
			if (i >= other.m_len) {
				return -1;
			}
			int b1 = m_arr[m_off+i] & 0xff;
			int b2 = other.m_arr[other.m_off+i] & 0xff;
			if (b1 < b2) {
				return -1;
			}
			if (b1 > b2) {
				return +1;
			}
		}
		return 0;
	}

	/**
	 *	A zero-length ByteAOL object.
	 *	Useful as an alternative to returning null.
	 */
	public static final ByteAOL EMPTY = new ByteAOL(new byte[0]);

	/**
	 *	For java.io.Serializable interface:
	 *	Designate a different object to serialize.
	 *	Specifically, we create a clone of this object, and serialize that.
	 *	This ensures that the serialized stream has only the "len"
	 *	bytes that this object actually uses, rather than the entire source array.
	 */
	Object writeReplace()
	{
		return new ByteAOL(getBytes(false));
	}

	/**
	 *	Scan a ByteAOL, cracking off components as we go.
	 *	The scanner has a "current offset", which most methods
	 *	increment when they extract the data item.
	 *<p>
	 *	The Scanner class is not thread-safe; we assume the caller
	 *	provides mutual exclusion on a Scanner.
	 *	However, each Scanner has its own scan pointer,
	 *	and a Scanner does not modify the associated ByteAOL,
	 *	so you may safely create many Scanners for the same ByteAOL.
	 *<p>
	 *	Most methods throw an AOLOverflowException if they run out of bytes.
	 *<p>
	 *	Example -- scan a ByteAOL which consists of a sequence of tag-length-value
	 *	triples, where the tags and lengths are two bytes:
	 *<pre>
	 *      ByteAOL segment;
	 *      ByteAOL.Scanner scanner = segment.new Scanner();
	 *      try {
	 *          while (!scanner.atEnd()) {
     *             int tag = scanner.get2ByteUInt();
	 *             int len = scanner.get2ByteUInt();
	 *             byte[] value = scanner.getBytes(len);
	 *             // use the triple ...
	 *          }
	 *      } catch (AOLException e) {
	 *         System.err.println("OOPS -- Invalid TLV triple");
	 *      }
	 *</pre>
	 */
	public class Scanner
	{
		private final int end;
		private int curOff;
		private boolean littleEndian = ByteAOL.this.littleEndian;
	
		/**
		 *	Create a new scanner for a ByteAOL.
		 */
		public Scanner()
		{
			this.curOff = m_off;
			this.end = m_off + m_len;
		}
	
		/**
		 *	Return true iff multi-byte numbers in this Scanner
		 *	are to be treated as little-endian.
		 */
		public boolean isLittleEndian() { return littleEndian; }
		
		/**
		 *	Set whether multi-byte numbers in this Scanner are to be treated as little-endian.
		 *	@param littleEndian
		 *		New status; true means numbers are little-endian.
		 *		Default is the little-endian status of the underlying ByteAOL
		 *		at the time when the Scanner was created.
		 *	@return Previous status.
		 */
		public boolean setLittleEndian(boolean littleEndian)
		{
			boolean prev = this.littleEndian;
			this.littleEndian = littleEndian;
			return prev;
		}

		/**
		 *	Return the number of unscanned bytes.
		 */
		public int bytesLeft()
		{
			return end - curOff;
		}
	
		/**
		 *	Return true iff we're scanned to end.
		 */
		public boolean atEnd()
		{
			return curOff >= end;
		}
	
		/**
		 *	Return the offset of the current scan pointer.
		 */
		public int getScanOffset()
		{
			return curOff;
		}
	
		/**
		 *	Skip the next n bytes.
		 *	@param nSkip Number of bytes to skip. If negative, backup.
		 *	@throws AOLOverflowException
		 *		If there are less than nSkip bytes left (for positive nSkip),
		 *		or, if nSkip is negative, we can't backup that far.
		 */
		public void skip(int nSkip)
			throws AOLOverflowException
		{
			int newOff = curOff + nSkip;
			if (newOff >= end || newOff < m_off) {
				throw new AOLOverflowException();
			}
			curOff = newOff;
		}
	
		/**
		 *	Return the next byte as a byte, and advance the scan pointer.
		 *	@throws AOLOverflowException If there are no more bytes.
		 */
		public byte get1Byte()
			throws AOLOverflowException
		{
			if (curOff >= end) {
				throw new AOLOverflowException();
			}
			return m_arr[curOff++];
		}
	
		/**
		 *	Return the next byte as an unsigned int,
		 *	and advance the scan pointer.
		 *	@throws AOLOverflowException If there are no more bytes.
		 */
		public int get1ByteUInt()
			throws AOLOverflowException
		{
			return get1Byte() & 0xff;
		}
	
		/**
		 *	Return the next two bytes as an unsigned int,
		 *	and advance the scan pointer.
		 *	The number is big-endian unless the little-endian flag is set for this Scanner.
		 *	@throws AOLOverflowException If there aren't two more bytes.
		 */
		public int get2ByteUInt()
			throws AOLOverflowException
		{
			if (curOff+1 >= end) {
				throw new AOLOverflowException();
			}
			int v;
			if (!littleEndian) {
				v  = (m_arr[curOff++] & 0xff) << 8;
				v |= (m_arr[curOff++] & 0xff);
			} else {
				v  = (m_arr[curOff++] & 0xff);
				v |= (m_arr[curOff++] & 0xff) << 8;
			}
			return v;
		}
	
		/**
		 *	Return the next three bytes as an unsigned int,
		 *	and advance the scan pointer.
		 *	The number is big-endian unless the little-endian flag is set for this Scanner.
		 *	@throws AOLOverflowException If there aren't four more bytes.
		 */
		public int get3ByteUInt()
			throws AOLOverflowException
		{
			if (curOff+2 >= end) {
				throw new AOLOverflowException();
			}
			int v;
			if (!littleEndian) {
				v  = (m_arr[curOff++] & 0xff) << 16;
				v |= (m_arr[curOff++] & 0xff) << 8;
				v |= (m_arr[curOff++] & 0xff);
			} else {
				v  = (m_arr[curOff++] & 0xff);
				v |= (m_arr[curOff++] & 0xff) << 8;
				v |= (m_arr[curOff++] & 0xff) << 16;
			}
			return v;
		}
	
		/**
		 *	Return the next four bytes as an unsigned long,
		 *	and advance the scan pointer.
		 *	The number is big-endian unless the little-endian flag is set for this Scanner.
		 *	@throws AOLOverflowException If there aren't four more bytes.
		 */
		public long get4ByteUInt()
			throws AOLOverflowException
		{
			if (curOff+3 >= end) {
				throw new AOLOverflowException();
			}
			long v;
			if (!littleEndian) {
				v  = ((long)(m_arr[curOff++] & 0xff)) << 24;
				v |= (m_arr[curOff++] & 0xff) << 16;
				v |= (m_arr[curOff++] & 0xff) << 8;
				v |= (m_arr[curOff++] & 0xff);
			} else {
				v  = (m_arr[curOff++] & 0xff);
				v |= (m_arr[curOff++] & 0xff) << 8;
				v |= (m_arr[curOff++] & 0xff) << 16;
				v |= ((long)(m_arr[curOff++] & 0xff)) << 24;
			}
			return v;
		}
	
		/**
		 *	Return the next 8 bytes as an unsigned long,
		 *	and advance the scan pointer.
		 *	The number is big-endian unless the little-endian flag is set for this Scanner.
		 *	@throws AOLOverflowException If there aren't four more bytes.
		 */
		public long get8ByteUInt()
			throws AOLOverflowException
		{
			if (curOff+7 >= end) {
				throw new AOLOverflowException();
			}
			long v;
			if (!littleEndian) {
				v  = ((long)(m_arr[curOff++] & 0xff)) << 56;
				v |= ((long)(m_arr[curOff++] & 0xff)) << 48;
				v |= ((long)(m_arr[curOff++] & 0xff)) << 40;
				v |= ((long)(m_arr[curOff++] & 0xff)) << 32;
				v |= ((long)(m_arr[curOff++] & 0xff)) << 24;
				v |= (m_arr[curOff++] & 0xff) << 16;
				v |= (m_arr[curOff++] & 0xff) << 8;
				v |= (m_arr[curOff++] & 0xff);
			} else {
				v  = (m_arr[curOff++] & 0xff);
				v |= (m_arr[curOff++] & 0xff) << 8;
				v |= (m_arr[curOff++] & 0xff) << 16;
				v |= ((long)(m_arr[curOff++] & 0xff)) << 24;
				v |= ((long)(m_arr[curOff++] & 0xff)) << 32;
				v |= ((long)(m_arr[curOff++] & 0xff)) << 40;
				v |= ((long)(m_arr[curOff++] & 0xff)) << 48;
				v |= ((long)(m_arr[curOff++] & 0xff)) << 56;
			}
			return v;
		}
	
		/**
		 *	Return a ByteAOL with the next n bytes,
		 *	and advance the scan pointer.
		 *	@param n Number of bytes to take.
		 *	@throws AOLOverflowException If there aren't n more bytes.
		 */
		public ByteAOL getAOL(int n)
			throws AOLOverflowException
		{
			if (curOff + n - 1 >= end) {
				throw new AOLOverflowException();
			}
			ByteAOL v = new ByteAOL(m_arr, curOff, n);
			curOff += n;
			return v;
		}
	
		/**
		 *	Return a byte[] with the next n bytes,
		 *	and advance the scan pointer.
		 *	@param n Number of bytes to take.
		 *	@throws AOLOverflowException If there aren't n more bytes.
		 */
		public byte[] getBytes(int n)
			throws AOLOverflowException
		{
			if (curOff + n - 1 >= end) {
				throw new AOLOverflowException();
			}
			byte[] v = new byte[n];
			for (int i = 0; i < n; i++) {
				v[i] = m_arr[curOff++];
			}
			return v;
		}
	
		/**
		 *	Return a byte[] with the remaining bytes,
		 *	and advance the scan pointer to the end.
		 */
		public byte[] getRemainingBytes()
		{
			try {
				return getBytes(bytesLeft());
			} catch (Exception e) {
				return new byte[0];
			}
		}
	
		/**
		 *	Return the remaining bytes as a ByteAOL.
		 *	Advance the scan pointer to the end of the buffer.
		 */
		public ByteAOL getRemaining()
		{
			ByteAOL v = new ByteAOL(m_arr, curOff, end-curOff);
			curOff = end;
			return v;
		}
		/**
		 *	Return a (possibly mult-byte) Q.773 Tag field as a integer.
		 *	Advance the scan pointer to the next byte after the encoded tag.
		 *<p>
		 *	We represent a multi-byte tag
		 *	as an int whose value is the encoded bytes of the tag
		 *	represented as a single int.
		 *	Eg, for the two-byte tag "9f 34", this method returns 0x9f34.
		 *
		 *	@throws AOLOverflowException If there aren't enough bytes left.
		 */
		public int getQ773Tag()
			throws AOLOverflowException
		{
			int tag = get1Byte() & 0xff;
			if ((tag & 0x1f) == 0x1f) {
				while (true) {
					tag <<= 8;
					int ext = get1Byte() & 0xff;
					tag |= ext;
					if ((ext & 0x80) == 0) {
						break;
					}
				}
			}
			return tag;
		}
	
		/**
		 *	Return a Q.773 length code as an integer,
		 *	and advance the scan pointer to the next byte after the encoded length.
		 *	If this is an indefinate form length, the returned
		 *	length will include the terminating EOC TLV field.
		 *
		 *	@throws AOLOverflowException
		 *		If there aren't enough bytes in the length code,
		 *		or if we run off the end looking for the EOC.
		 */
		public int getQ773Length()
			throws AOLOverflowException
		{
			int count = get1Byte() & 0xff;
			if (count == 0x80) {
				// Indefinate length form.  Assume contents are TLVs.
				// Save start point, recursively scan, then reset
				// and return total length.
				int start = curOff;
				while (curOff < end) {
					TagAOL tlv = getQ773TLV();
					if (tlv.tag == 0 && tlv.value.m_len == 0) {
						break;
					}
				}
				int len = curOff - start;
				curOff = start;
				return len;
			} else if ((count & 0x80) == 0) {
				return count;	// Short form -- 1 byte.
			} else {
				int len = 0;
				count &= 0x7f;
				while (--count >= 0) {
					len <<= 8;
					len |= get1Byte() & 0xff;
				}
				return len;
			}
		}
	
		/**
		 *	Return a Q.773 TLV field as a TagAOL,
		 *	and advance the scan pointer to the next byte after the value.
		 *	The tag value is represented as described in {@link #getQ773Tag()}.
		 *	If this TLV has an indefinate form length, the returned
		 *	value will include the terminating EOC TLV field.
		 *
		 *	@throws AOLOverflowException If there aren't enough bytes left.
		 */
		public TagAOL getQ773TLV()
			throws AOLOverflowException
		{
			int tag = getQ773Tag();
			int len = getQ773Length();
			ByteAOL val = getAOL(len);
			return new TagAOL(tag, val);
		}
	}

	/**
	 *	Fill a ByteAOL, one component at a time.
	 *	This object has a "current offset", which most methods
	 *	increment when they add a data item.
	 *<p>
	 *	This class is not thread-safe;
	 *	we assume the caller provides mutual exclusion.
	 *	And because the class is modifying the array in the underlying ByteAOL,
	 *	it does not make sense to have more than one Filler operating on a ByteAOL.
	 *<p>
	 *	Most methods throw an AOLOverflowException
	 *	if the array runs out of space.
	 *<p>
	 *	Example -- fill a ByteAOL with a sequence of tag-length-value
	 *	triples, where the tags and lengths are two bytes:
	 *<pre>
	 *      ByteAOL segment = new ByteAOL(new byte[...]);
	 *      ByteAOL.Filler filler = segment.new Filler();
	 *      try {
	 *          filler.put2ByteUInt(26);          // first field: tag
	 *          filler.put2ByteUInt(val1.length); // first field: length
	 *          filler.put(val1);                 // first field: byte[] value
	 *          filler.put2ByteUInt(4);           // second field: tag
	 *          filler.put2ByteUInt(val2.length); // second field: length
	 *          filler.put(val2);                 // second field: byte[] value
	 *      } catch (AOLOverflowException e) {
	 *          System.err.println("OOPS -- we miscalculated length");
	 *      }
	 *</pre>
	 */
	public class Filler
	{
		private int curOff;
		private int end;
		private boolean littleEndian = ByteAOL.this.littleEndian;
	
		/**
		 *	Create a new filler for a ByteAOL.
		 */
		public Filler()
		{
			this.curOff = m_off;
			this.end = m_off + m_len;
		}
	
		/**
		 *	Return true iff multi-byte numbers in this Filler
		 *	are to be treated as little-endian.
		 */
		public boolean isLittleEndian() { return littleEndian; }
		
		/**
		 *	Set whether multi-byte numbers in this Filler are to be treated as little-endian.
		 *	@param littleEndian
		 *		New status; true means numbers are little-endian.
		 *		Default is the little-endian status of the underlying ByteAOL
		 *		at the time when the Filler was created.
		 *	@return Previous status.
		 */
		public boolean setLittleEndian(boolean littleEndian)
		{
			boolean prev = this.littleEndian;
			this.littleEndian = littleEndian;
			return prev;
		}
	
		/**
		 *	Return the number of unfilled bytes.
		 */
		public int bytesLeft()
		{
			return end - curOff;
		}
	
		/**
		 *	Return the offset of the current scan pointer.
		 */
		public int getPutOffset()
		{
			return curOff;
		}
	
		/**
		 *	Skip the next n bytes.
		 *	@param nSkip Number of bytes to skip.
		 *	@throws AOLOverflowException If there isn't enough space left.
		 */
		public void skip(int nSkip)
			throws AOLOverflowException
		{
			if (curOff + nSkip >= end) {
				throw new AOLOverflowException();
			}
			curOff += nSkip;
		}
	
		/**
		 *	Append a byte, and advance the scan pointer.
		 *	@param v Byte to put.
		 *	@throws AOLOverflowException If there isn't enough space left.
		 */
		public void put(byte v)
			throws AOLOverflowException
		{
			if (curOff >= end) {
				throw new AOLOverflowException();
			}
			m_arr[curOff++] = v;
		}
	
		/**
		 *	Append an int as one byte, and advance the scan pointer.
		 *	@param v Value to put.
		 *	@throws AOLOverflowException If there isn't enough space left.
		 */
		public void put1ByteUInt(int v)
			throws AOLOverflowException
		{
			put((byte)(v & 0xff));
		}
	
		/**
		 *	Append an int as a two byte value, and advance the scan pointer.
		 *	The number is big-endian unless the little-endian flag is set for this Filler.
		 *	@param v Value to put.
		 *	@throws AOLOverflowException If there isn't enough space left.
		 */
		public void put2ByteUInt(int v)
			throws AOLOverflowException
		{
			if (curOff+1 >= end) {
				throw new AOLOverflowException();
			}
			if (!littleEndian) {
				m_arr[curOff++] = (byte)((v >> 8) & 0xff);
				m_arr[curOff++] = (byte)((v     ) & 0xff);
			} else {
				m_arr[curOff++] = (byte)((v     ) & 0xff);
				m_arr[curOff++] = (byte)((v >> 8) & 0xff);
			}
		}
	
		/**
		 *	Append an int as a three byte value, and advance the scan pointer.
		 *	The number is big-endian unless the little-endian flag is set for this Filler.
		 *	@param v Value to put.
		 *	@throws AOLOverflowException If there isn't enough space left.
		 */
		public void put3ByteUInt(int v)
			throws AOLOverflowException
		{
			if (curOff+2 >= end) {
				throw new AOLOverflowException();
			}
			if (!littleEndian) {
				m_arr[curOff++] = (byte)((v >> 16) & 0xff);
				m_arr[curOff++] = (byte)((v >>  8) & 0xff);
				m_arr[curOff++] = (byte)((v      ) & 0xff);
			} else {
				m_arr[curOff++] = (byte)((v      ) & 0xff);
				m_arr[curOff++] = (byte)((v >>  8) & 0xff);
				m_arr[curOff++] = (byte)((v >> 16) & 0xff);
			}
		}
	
		/**
		 *	Append an int as a four byte value, and advance the scan pointer.
		 *	The number is big-endian unless the little-endian flag is set for this Filler.
		 *	@param v Value to put.
		 *	@throws AOLOverflowException If there isn't enough space left.
		 */
		public void put4ByteUInt(int v)
			throws AOLOverflowException
		{
			if (curOff+3 >= end) {
				throw new AOLOverflowException();
			}
			if (!littleEndian) {
				m_arr[curOff++] = (byte)((v >> 24) & 0xff);
				m_arr[curOff++] = (byte)((v >> 16) & 0xff);
				m_arr[curOff++] = (byte)((v >>  8) & 0xff);
				m_arr[curOff++] = (byte)((v      ) & 0xff);
			} else {
				m_arr[curOff++] = (byte)((v      ) & 0xff);
				m_arr[curOff++] = (byte)((v >>  8) & 0xff);
				m_arr[curOff++] = (byte)((v >> 16) & 0xff);
				m_arr[curOff++] = (byte)((v >> 24) & 0xff);
			}
		}
	
		/**
		 *	Append a long as an eight byte value, and advance the scan pointer.
		 *	The number is big-endian unless the little-endian flag is set for this Filler.
		 *	@param v Value to put.
		 *	@throws AOLOverflowException If there isn't enough space left.
		 */
		public void put8ByteUInt(long v)
			throws AOLOverflowException
		{
			if (curOff+7 >= end) {
				throw new AOLOverflowException();
			}
			if (!littleEndian) {
				m_arr[curOff++] = (byte)((v >> 56) & 0xff);
				m_arr[curOff++] = (byte)((v >> 48) & 0xff);
				m_arr[curOff++] = (byte)((v >> 40) & 0xff);
				m_arr[curOff++] = (byte)((v >> 32) & 0xff);
				m_arr[curOff++] = (byte)((v >> 24) & 0xff);
				m_arr[curOff++] = (byte)((v >> 16) & 0xff);
				m_arr[curOff++] = (byte)((v >>  8) & 0xff);
				m_arr[curOff++] = (byte)((v      ) & 0xff);
			} else {
				m_arr[curOff++] = (byte)((v      ) & 0xff);
				m_arr[curOff++] = (byte)((v >>  8) & 0xff);
				m_arr[curOff++] = (byte)((v >> 16) & 0xff);
				m_arr[curOff++] = (byte)((v >> 24) & 0xff);
				m_arr[curOff++] = (byte)((v >> 32) & 0xff);
				m_arr[curOff++] = (byte)((v >> 40) & 0xff);
				m_arr[curOff++] = (byte)((v >> 48) & 0xff);
				m_arr[curOff++] = (byte)((v >> 56) & 0xff);
			}
		}
	
		/**
		 *	Append the bytes in a ByteAOL and advance the scan pointer.
		 *	@param v Value to put.
		 *	@throws AOLOverflowException If there isn't enough space left.
		 */
		public void put(ByteAOL v)
			throws AOLOverflowException
		{
			if (v == null) {
				return;
			}
			if (curOff + v.m_len - 1 >= end) {
				throw new AOLOverflowException();
			}
			v.copyBytes(m_arr, curOff);
			curOff += v.m_len;
		}
	
		/**
		 *	Append a byte array and advance the scan pointer.
		 *	@param v Value to put.
		 *	@throws AOLOverflowException If there isn't enough space left.
		 */
		public void put(byte[] v)
			throws AOLOverflowException
		{
			put(v, 0, v.length);
		}
		
		/**
		 *	Append a portion of a byte array and advance the scan pointer.
		 *	@param v Value to put.
		 *	@param offset Starting offset in v.
		 *	@param len Number of bytes to put.
		 *	@throws AOLOverflowException If there isn't enough space left.
		 */
		public void put(byte[] v, int offset, int len)
			throws AOLOverflowException
		{
			if (v == null) {
				return;
			}
			if (curOff + len - 1 >= end) {
				throw new AOLOverflowException();
			}
			for (int i = 0; i < len; i++) {
				m_arr[curOff++] = v[offset + i];
			}
		}
	
		/**
		 *	Append a Q773-encoded tag and advance the scan pointer.
		 *	We assume the tag is encoded as a single int,
		 *	with the appropriate marker bits.
		 *	E.g., we will output 1, 2, 3 or 4 bytes
		 *	depending on whether the tag's upper 3, 2, 1, or 0
		 *	bytes are 0.
		 *	@param tag Tag to put.
		 *	@throws AOLOverflowException If there isn't enough space left.
		 */
		public void putQ773Tag(int tag)
			throws AOLOverflowException
		{
			if (tag <= 0xff) {
				put1ByteUInt(tag);
			} else if (tag <= 0xffff) {
				put2ByteUInt(tag);
			} else if (tag <= 0xffffff) {
				put3ByteUInt(tag);
			} else {
				put4ByteUInt(tag);
			}
		}
	
		/**
		 *	Append a Q773-encoded length and advance the scan pointer.
		 *	@param len Length value to put.
		 *	@throws AOLOverflowException If there isn't enough space left.
		 */
		public void putQ773Len(int len)
			throws AOLOverflowException
		{
			if (len <= 0x7f) {
				put1ByteUInt(len);
			} else if (len <= 0xff) {
				put((byte)0x81);
				put1ByteUInt(len);
			} else if (len <= 0xffff) {
				put((byte)0x82);
				put2ByteUInt(len);
			} else if (len <= 0xffffff) {
				put((byte)0x83);
				put3ByteUInt(len);
			} else {
				put((byte)0x84);
				put4ByteUInt(len);
			}
		}
	
		/**
		 *	Append a Q773-encoded TLV and advance the scan pointer.
		 *	@param tlv
		 *		Tag, length, and value to put.
		 *		The tag must be encoded as described in {@link #putQ773Tag(int)}.
		 *	@throws AOLOverflowException If there isn't enough space left.
		 */
		public void putQ773TLV(TagAOL tlv)
			throws AOLOverflowException
		{
			putQ773Tag(tlv.tag);
			putQ773Len(tlv.value.m_len);
			put(tlv.value);
		}
	}
}
