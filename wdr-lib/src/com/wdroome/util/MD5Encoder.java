package com.wdroome.util;

import java.io.*;
import java.security.*;

/**
 *	Standalone utility program: print the MD5 hex encoding
 *	of various strings.  See main().
 */
public class MD5Encoder
{
	/**
	 *	Print the MD5 hex encoding of each argument.
	 *	If no arguments, print the MD5 hex encoding
	 *	of each (trimmed) line from standard input.
	 */
	public static void main(String args[])
	{
		try {
			MessageDigest md5Digest = MessageDigest.getInstance("MD5");
			if (args.length <= 0
			  || (args.length == 1 && args[0].equals("-"))) {
				BufferedReader rdr = new BufferedReader(new InputStreamReader(System.in));
				System.out.println("Enter passwords, one per line."
							+ " Leading and trailing whitespace will be ignored.");
				String passwd;
				while ((passwd = rdr.readLine()) != null) {
					md5Digest.reset();
					System.out.println(MiscUtil.bytesToHex(md5Digest.digest(passwd.trim().getBytes())));
				}
			} else {
				for (int i = 0; i < args.length; i++) {
					md5Digest.reset();
					System.out.println(MiscUtil.bytesToHex(md5Digest.digest(args[i].getBytes())));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
