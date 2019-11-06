package com.wdroome.util;

import java.io.InputStreamReader;
import java.io.BufferedReader;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *	MD5 hash utility functions and standalone encoder program.
 *	See main().
 */
public class MD5Encoder
{
	private static final MessageDigest g_md5Digest;
	static {
		MessageDigest md5Digest = null;
		try {
			md5Digest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// Opps!
		}
		g_md5Digest = md5Digest;
	}
	
	/**
	 * Return the MD5 hash of the concatenated arguments, as a byte array.
	 * @param src The byte arrays to hash. Ignore null arguments,
	 * @return The MD5 hash of the concatenated arguments,
	 * 		or null if there is no MD5 encoder in this JVM.
	 */
	public static byte[] encodeToBytes(byte[]... src)
	{
		if (g_md5Digest == null) {
			return null;
		}
		synchronized (g_md5Digest) {
			g_md5Digest.reset();
			for (byte[] b: src) {
				if (b != null) {
					g_md5Digest.update(b);
				}
			}
			return g_md5Digest.digest();
		}
	}
	
	/**
	 * Return the MD5 hash of the concatenated arguments, as lower-case hex string.
	 * @param src The byte arrays to hash.
	 * @return The MD5 hash of the concatenated arguments,
	 * 		or null if there is no MD5 encoder in this JVM.
	 */
	public static String encodeToHex(byte[]...src)
	{
		byte[] hash = encodeToBytes(src);
		return hash != null ? MiscUtil.bytesToHex(hash) : null;
	}
	
	/**
	 * Return the MD5 hash of the concatenated arguments, as a byte array.
	 * @param src The strings to hash. Ignore null arguments.
	 * @return The MD5 hash of the concatenated arguments,
	 * 		or null if there is no MD5 encoder in this JVM.
	 */
	public static byte[] encodeToBytes(String... src)
	{
		if (g_md5Digest == null) {
			return null;
		}
		synchronized (g_md5Digest) {
			g_md5Digest.reset();
			for (String s: src) {
				if (s != null) {
					g_md5Digest.update(s.getBytes());
				}
			}
			return g_md5Digest.digest();
		}
		
	}
	
	/**
	 * Return the MD5 hash of the concatenated arguments, as lower-case hex string.
	 * @param src The strings to hash.
	 * @return The MD5 hash of the concatenated arguments,
	 * 		or null if there is no MD5 encoder in this JVM.
	 */
	public static String encodeToHex(String... src)
	{
		byte[] hash = encodeToBytes(src);
		return hash != null ? MiscUtil.bytesToHex(hash) : null;
	}
	
	/**
	 *	Print the MD5 hex encoding of each argument.
	 *	If no arguments, print the MD5 hex encoding
	 *	of each (trimmed) line from standard input.
	 */
	public static void main(String args[])
	{
		try {
			if (args.length <= 0
			  || (args.length == 1 && args[0].equals("-"))) {
				BufferedReader rdr = new BufferedReader(new InputStreamReader(System.in));
				System.out.println("Enter strings to hash, one per line."
							+ " Leading and trailing whitespace will be ignored.");
				String line;
				while ((line = rdr.readLine()) != null) {
					System.out.println(encodeToHex(line.trim()));
				}
			} else {
				for (int i = 0; i < args.length; i++) {
					System.out.println(encodeToHex(args[i]));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
