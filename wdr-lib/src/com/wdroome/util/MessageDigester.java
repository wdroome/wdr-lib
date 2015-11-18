package com.wdroome.util;

import java.io.FileNotFoundException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Standalone utility to calculate the secure hash of strings or files.
 * @author wdr
 */
public class MessageDigester
{
	/** Default hashing algorithm. */
	public static final String ALG_DEF = "SHA-256";

	/**
	 * Print the secure hash of strings or files.
	 * Usage:
	 * <pre>
	 *    MessageDigester [-alg hash-alg-name] [-files] [-strings] file-names-or-strings
	 * </pre>
	 * -files means subsequent arguments are file names,
	 * and the contents are hashed.
	 * -strings means subsequent arguments are strings to be hash.
	 * The default is -strings. If no string or file name arguments, hash standard input.
	 * @param args Command line arguments, as above.
	 */
	public static void main(String[] args)
	{
		String alg = ALG_DEF;
		boolean isString = true;
		int nHashArgs = 0;
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.equals("?") || arg.startsWith("-h")) {
				System.out.println("Usage: [-alg hash-alg-name] [-files] [-strings] file-names-or-strings");
				return;
			} else if (arg.startsWith("-a") && i+1 < args.length) {
				alg = args[i+1];
				i++;
			} else if (arg.startsWith("-f")) {
				isString = false;
			} else if (arg.startsWith("-s")) {
				isString = true;
			} else {
				if (arg.equals("-") && nHashArgs == 0 && i+1 >= args.length) {
					isString = false;
				}
				doArg(alg, isString, arg);
				nHashArgs++;
			}
		}
		if (nHashArgs == 0) {
			doArg(alg, false, "-");
		}
	}
	
	private static void doArg(String alg, boolean isString, String arg)
	{
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance(alg);
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Unknown hash algorithm '"+ alg + "'");
			return;
		}
		if (isString) {
			System.out.println("String \"" + arg + "\": "
						+ MiscUtil.bytesToHex(digest.digest(arg.getBytes())));
		} else {
			BufferedInputStream rdr;
			if (arg.equals("-")) {
				rdr = new BufferedInputStream(System.in);
			} else {
				try {
					rdr = new BufferedInputStream(new FileInputStream(arg));
				} catch (FileNotFoundException e) {
					System.err.println("Cannot read file '"+ arg + "'");
					return;
				}
			}
			byte[] buff = new byte[8192];
			int nread;
			try {
				while ((nread = rdr.read(buff)) > 0) {
					digest.update(buff, 0, nread);
				}
				System.out.println("File " + arg + ": "
						+ MiscUtil.bytesToHex(digest.digest(arg.getBytes())));
			} catch (IOException e) {
				System.err.println(arg + ": Read IOError: " + e);
			}
			try { rdr.close(); } catch (Exception e) {}
		}
	}
}
