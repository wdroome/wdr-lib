package com.wdroome.util;

import java.io.*;

/**
 *	Convenience methods for encoding and decoding bytes using
 *	the 'Base64' algorithm.  See RFC 2045, Section 6.8, for details
 *	on Base64.
 *<p>
 *	You'd think that the standard java libraries would have
 *	a Base64 utility class, but if they do, I couldn't find it.
 */
public class Base64
{
	/**
	 *	A 64-element array, indexed by all possible 6-bit numbers.
	 *	The value is the ascii character used to encode that 6-bit
	 *	number. RFC 2045 defines this table.
	 */
	private static final byte[] ENC_CHARS = new byte[] {
		'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',	//  0- 9
		'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',	// 10-19
		'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd',	// 20-29
		'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',	// 30-39
		'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x',	// 40-49
		'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7',	// 50-59
		'8', '9', '+', '/',									// 60-63
	};

	/**
	 *	The ascii character used for padding when the source
	 *	does not have a multiple of 3 bytes. See RFC 2045.
	 */
	private static final byte ENC_PAD = '=';

	/**
	 *	DEC_BYTES is a 256-element array, indexed by (ascii) character codes.
	 *	Valid base64 characters have the encoded value (0-63).
	 *	The pad character has the value DEC_BYTE_PAD,
	 *	and invalid Base64 characters have the value DEC_BYTE_INVALID.
	 *	DEC_BYTES is the inverse mapping from ENC_CHARS,
	 *	and is created from ENC_CHARS by a static startup block.
	 *<p>
	 *	The code assumes that any value >= DEC_BYTE_INVALID is invalid.
	 */
	private static final byte[] DEC_BYTES;
	private static final byte DEC_BYTE_INVALID = 0x40;	// lowest invalid value
	private static final byte DEC_BYTE_PAD = 0x41;

	/**
	 *	Startup initialization: setup the DEC_BYTES array.
	 */
	static {
		byte[] arr = new byte[256];
		for (int i = 0; i < 256; i++) {
			arr[i] = (byte)DEC_BYTE_INVALID;
		}
		for (int i = 0; i < 64; i++) {
			arr[ENC_CHARS[i]] = (byte)i;
		}
		arr[ENC_PAD] = (byte)DEC_BYTE_PAD;
		DEC_BYTES = arr;
	}

	/**
	 *	Short for encodeToBytes(src, 0, src.length).
	 */
	public static byte[] encodeToBytes(byte[] src)
	{
		return encodeToBytes(src, 0, src != null ? src.length : 0);
	}

	/**
	 *	Return the base 64 encoding of src, as a byte array.
	 *<p>
	 *	Note that this method returns a continuous array of encoded bytes:
	 *	the returned array has 4 bytes for every 3 source bytes,
	 *	plus the "=" padding at the end.
	 *	This method does NOT put newlines, returns, or other white space
	 *	into the returned byte array.
	 *
	 *	@param src The source array.
	 *	@param off Starting offset within the source array.
	 *	@param len Number of source bytes to encode.
	 *
	 *	@return The base 64 encoded bytes.
	 *		Never returns null; returns a byte[0] array instead.
	 */
	public static byte[] encodeToBytes(byte[] src, int off, int len)
	{
		if (off < 0)
			off = 0;
		if (src == null || len <= 0 || off >= src.length) {
			return new byte[0];
		}
		if (off + len > src.length) {
			len = src.length - off;
		}

		int encLen = 4 * ((len+2)/3);
		byte[] enc = new byte[encLen];
		int iSrc = off;
		int iEnc = 0;
		int nSrcLeft = len;
		for ( ; nSrcLeft > 0; nSrcLeft -= 3, iSrc += 3, iEnc += 4) {
			int src0 = src[iSrc] & 0xff;
			int src1 = (nSrcLeft > 1) ? src[iSrc+1] & 0xff : 0;
			int src2 = (nSrcLeft > 2) ? src[iSrc+2] & 0xff : 0;
			enc[iEnc] = ENC_CHARS[src0 >> 2];
			enc[iEnc+1] = ENC_CHARS[((src0 & 0x03) << 4) | (src1>>4)];
			enc[iEnc+2] = (nSrcLeft > 1) ? ENC_CHARS[((src1 & 0x0f) << 2) | (src2>>6)] : ENC_PAD;
			enc[iEnc+3] = (nSrcLeft > 2) ? ENC_CHARS[src2 & 0x3f] : ENC_PAD;
		}
		return enc;
	}

	/**
	 *	Short for decodeToBytes(enc, 0, enc.length).
	 */
	public static byte[] decodeToBytes(byte[] enc)
	{
		return decodeToBytes(enc, 0, (enc != null) ? enc.length : 0);
	}

	/**
	 *	Decode a base64 encoded byte array, and return as a byte array.
 	 *<p>
	 *	Note: This assumes the 4 characters of each base64 encoded chunk
	 *	are adjacent. It quietly ignores illegal base64 characters between
	 *	chunks, and continues until it finds the next chunk.
	 *
	 *	@param enc An array of base64 encoded bytes.
	 *	@param off Start with enc[off].
	 *	@param len Decode the next len bytes.
	 *
	 *	@return The decoded bytes.
	 *		Never returns null; returns byte[0] instead.
	 */
	public static byte[] decodeToBytes(byte[] enc, int off, int len)
	{
		if (off < 0) {
			off = 0;
		}
		if (enc == null || len <= 0 || off >= enc.length) {
			return new byte[0];
		}
		if (off + len > enc.length) {
			len = enc.length - off;
		}

		// dec[] is big enough to hold the decoded bytes.
		// If dec is too big, we'll return a truncated copy.
		byte[] dec = new byte[(3*len + 2)/4];

		int iDec = 0;
		int iEnc = off;
		int nEncLeft = len;
		while (nEncLeft >= 4) {
			// Between chunks; skip non-base64 characters.
			while (nEncLeft >= 4 && DEC_BYTES[enc[iEnc] & 0xff] >= DEC_BYTE_INVALID) {
				iEnc++;
				nEncLeft--;
			}
			if (nEncLeft < 4) {
				break;
			}
			// Decode 4-char chunk.
			byte dec0 = DEC_BYTES[enc[iEnc  ] & 0xff];
			byte dec1 = DEC_BYTES[enc[iEnc+1] & 0xff];
			byte dec2 = DEC_BYTES[enc[iEnc+2] & 0xff];
			byte dec3 = DEC_BYTES[enc[iEnc+3] & 0xff];
			if ( (dec0 >= DEC_BYTE_INVALID)
			  || (dec1 >= DEC_BYTE_INVALID)
			  || (dec2 == DEC_BYTE_INVALID)
			  || (dec3 == DEC_BYTE_INVALID)) {
				break;
			}
			dec[iDec++] = (byte)((dec0 << 2) | (dec1 >> 4));
			if (dec2 != DEC_BYTE_PAD) {
				dec[iDec++] = (byte)(((dec1 & 0x0f) << 4) | (dec2 >> 2));
				if (dec3 != DEC_BYTE_PAD) {
					dec[iDec++] = (byte)(((dec2 & 0x03) << 6) | dec3);
				}
			}
			iEnc += 4;
			nEncLeft -= 4;
		}

		// Return first iDec bytes of dec[] array.
		if (iDec == dec.length) {
			// Hey! We lucked out, dec[] is right size.
			return dec;
		} else {
			// Create & return copy of first iDec bytes in dec[].
			byte[] ret = new byte[iDec];
			for (int i = 0; i < iDec; i++) {
				ret[i] = dec[i];
			}
			return ret;
		}
	}

	/**
	 *	Test driver. Encode/decode/check each argument.
	 */
	public static void main(String[] args)
	{
		for (int i = 0; i < args.length; i++) {
			byte[] enc = encodeToBytes(args[i].getBytes());
			System.out.print("'" + args[i] + "' >= ");
			for (int j = 0; j < enc.length; j++) {
				System.out.print((char)enc[j]);
			}
			System.out.println();

			byte[] dec = decodeToBytes(enc);
			System.out.print("  Back to " + dec.length + " bytes: '");
			for (int j = 0; j < dec.length; j++) {
				System.out.print((char)dec[j]);
			}
			System.out.println("'");

			String decStr = new String(dec);
			if (!decStr.equals(args[i])) {
				System.out.println("  NOT EQUAL!!");
			}
			System.out.println();
		}
	}
}
