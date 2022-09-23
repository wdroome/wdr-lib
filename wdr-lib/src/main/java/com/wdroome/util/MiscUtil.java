package com.wdroome.util;

import java.io.IOException;
import java.io.FileReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 *	Miscellaneous static utility methods.
 */
public class MiscUtil
{
	/**
	 *	Convenience shortcut: the line separator string for this platform.
	 */
	public static final String LINE_SEP = System.getProperty("line.separator", "\n");

	/**
	 *	Convenience shortcut: the file separator string for this platform.
	 */
	public static final String FILE_SEP = System.getProperty("file.separator", "/");

	/**
	 *	Convenience shortcut: the path separator string for this platform.
	 */
	public static final String PATH_SEP = System.getProperty("path.separator", ":");

	/**
	 *	If the system property propName exists, and it's numeric,
	 *	return its value as an int.  If not, return def.
	 *<p>
	 *	@return The integer value of the property, or def.
	 *
	 *	@param propName The property name.
	 *	@param def The default value.
	 */
	public static int getIntProperty(String propName, int def)
	{
		try {
			String s = System.getProperty(propName);
			if (s != null) {
				return Integer.parseInt(s.trim());
			}
		} catch (Exception e) {
		}
		return def;
	}

	/**
	 *	If the system property propName exists, and it's numeric,
	 *	return its value as an long.  If not, return def.
	 *<p>
	 *	@return The long value of the property, or def.
	 *
	 *	@param propName The property name.
	 *	@param def The default value.
	 */
	public static long getLongProperty(String propName, long def)
	{
		try {
			String s = System.getProperty(propName);
			if (s != null) {
				return Long.parseLong(s.trim());
			}
		} catch (Exception e) {
		}
		return def;
	}

	/**
	 *	If the system property propName exists, and it's a boolean,
	 *	return its value as an boolean.  If not, return def.
	 *<p>
	 *	@return The boolean value of the property, or def.
	 *
	 *	@param propName The property name.
	 *	@param def The default value.
	 */
	public static boolean getBooleanProperty(String propName, boolean def)
	{
		try {
			String s = System.getProperty(propName);
			if (s != null) {
				return Boolean.valueOf(s.trim()).booleanValue();
			}
		} catch (Exception e) {
		}
		return def;
	}

	/**
	 *	Return the "leaf" part of the classname of an object.
	 */
	public static String getLeafClassName(Object o)
	{
		String cname = o.getClass().getName();
		if (cname == null) {
			return "???";
		}
		int lastPeriod = cname.lastIndexOf('.');
		if (lastPeriod < 0) {
			return cname;
		}
		return cname.substring(lastPeriod+1);
	}

	/**
	 *	Return the package name for an object's class.
	 */
	public static String getPackageName(Object o)
	{
		Package p = o.getClass().getPackage();
		if (p == null) {
			return "???";
		}
		return p.getName();
	}

	/**
	 *	Return the value of a byte[] as String of 2-digit hex numbers.
	 *	Uses lower case a-f.
	 */
	public static String bytesToHex(byte[] bytes)
	{
		if (bytes == null)
			return "";
		char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7',
							'8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
		char buff[] = new char[2*bytes.length];
		for (int i = 0; i < bytes.length; i++) {
			byte b = bytes[i];
			buff[2*i  ] = hexDigits[(b >> 4) & 0xf];
			buff[2*i+1] = hexDigits[(b     ) & 0xf];
		}
		return new String(buff);
	}

	/**
	 *	Given a String of hex digits, return the equivalent byte[].
	 *<p>
	 *	Caveats:
	 *	We treat any character that isn't a valid hex digit as zero.
	 *	The hex string should have an even number of digits. If not,
	 *	we logically append a zero at the end.
	 */
	public static byte[] hexToBytes(String s)
	{
		if (s == null) {
			return new byte[0];
		}
		int n = s.length();
		byte[] ret = new byte[(n+1)/2];
		for (int i = 0; i < n; i += 2) {
			byte b = 0;
			switch (s.charAt(i)) {
				case '1':	b += 0x10;	break;
				case '2':	b += 0x20;	break;
				case '3':	b += 0x30;	break;
				case '4':	b += 0x40;	break;
				case '5':	b += 0x50;	break;
				case '6':	b += 0x60;	break;
				case '7':	b += 0x70;	break;
				case '8':	b += 0x80;	break;
				case '9':	b += 0x90;	break;
				case 'a': case 'A':	b += 0xa0;	break;
				case 'b': case 'B':	b += 0xb0;	break;
				case 'c': case 'C':	b += 0xc0;	break;
				case 'd': case 'D':	b += 0xd0;	break;
				case 'e': case 'E':	b += 0xe0;	break;
				case 'f': case 'F':	b += 0xf0;	break;
			}
			if (i+1 < n) {
				switch (s.charAt(i+1)) {
					case '1':	b += 0x1;	break;
					case '2':	b += 0x2;	break;
					case '3':	b += 0x3;	break;
					case '4':	b += 0x4;	break;
					case '5':	b += 0x5;	break;
					case '6':	b += 0x6;	break;
					case '7':	b += 0x7;	break;
					case '8':	b += 0x8;	break;
					case '9':	b += 0x9;	break;
					case 'a': case 'A':	b += 0xa;	break;
					case 'b': case 'B':	b += 0xb;	break;
					case 'c': case 'C':	b += 0xc;	break;
					case 'd': case 'D':	b += 0xd;	break;
					case 'e': case 'E':	b += 0xe;	break;
					case 'f': case 'F':	b += 0xf;	break;
				}
			}
			ret[i/2] = b;
		}
		return ret;
	}

	/**
	 *	Return true iff Class "testClass" is an instance of "isaClassName".
	 *	isaClassName may be a superclass or an interface.
	 */
	public static boolean classIsa(Class testClass, String isaClassName)
	{
		if (testClass != null && isaClassName != null) {
			for (Class c = testClass; c != null; c = c.getSuperclass()) {
				if (isaClassName.equals(c.getName())) {
					return true;
				}
			}
			Class[] interfaces = testClass.getInterfaces();
			if (interfaces != null) {
				for (int i = 0; i < interfaces.length; i++) {
					if (classIsa(interfaces[i], isaClassName)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 *	Return true if cname is limited to the characters in a
	 *	fully qualified class name: letters, numbers, underbar, or period.
	 *	Return false if cname is null or empty.
	 */
	public static boolean isClassName(String cname)
	{
		if (cname == null) {
			return false;
		}
		int n = cname.length();
		if (n <= 0)
			return false;
		for (int i = 0; i < n; i++) {
			char c = cname.charAt(i);
			if (!Character.isLetterOrDigit(c)
			  && c != '_'
			  && c != '.') {
				return false;
			}
		}
		return true;
	}

	/**
	 *	Return a string representing an integer, with a minimum width.
	 *
	 *	@param val The value.
	 *	@param minWidth The minimum width of the returned string.
	 *	@param pad The padding character (on the left).
	 *
	 *	@return The String representation of val.
	 */
	public static String itoa(int val, int minWidth, char pad)
	{
		String s = Integer.toString(val);
		int slen = s.length();
		if (slen < minWidth) {
			StringBuilder b = new StringBuilder(minWidth);
			for (int i = 0; i < minWidth-slen; i++) {
				b.append(pad);
			}
			b.append(s);
			s = b.toString();
		}
		return s;
	}
	
	/**
	 *	Return a string representing an integer, with a minimum width.
	 *	Add blanks on the left as padding.
	 *
	 *	@param val The value.
	 *	@param minWidth The minimum width of the returned string.
	 *
	 *	@return The String representation of val.
	 */
	public static String itoa(int val, int minWidth)
	{
		return itoa(val, minWidth, ' ');
	}
	
	/**
	 *	Return a string representing a long, with a minimum width.
	 *
	 *	@param val The value.
	 *	@param minWidth The minimum width of the returned string.
	 *	@param pad The padding character (on the left).
	 *
	 *	@return The String representation of val.
	 */
	public static String itoa(long val, int minWidth, char pad)
	{
		String s = Long.toString(val);
		int slen = s.length();
		if (slen < minWidth) {
			StringBuilder b = new StringBuilder(minWidth);
			for (int i = 0; i < minWidth-slen; i++) {
				b.append(pad);
			}
			b.append(s);
			s = b.toString();
		}
		return s;
	}
	
	/**
	 *	Return a string representing a long, with a minimum width.
	 *	Add blanks on the left as padding.
	 *
	 *	@param val The value.
	 *	@param minWidth The minimum width of the returned string.
	 *
	 *	@return The String representation of val.
	 */
	public static String itoa(long val, int minWidth)
	{
		return itoa(val, minWidth, ' ');
	}
	
	/**
	 *	Read and return the next line from an InputStream, as a String.
	 *	The returned line will not include the new line character,
	 *	or the preceding carriage-return, if any.
	 *	Return null on EOF.
	 */
	public static String readLine(InputStream s)
	{
		//
		// Read a char at a time (ugh) and save in cb array.
		// If the line won't fit in cb, spool into StringBuilder sb.
		// ncb is the number of valid characters in the cb array.
		//
		char[] cb = new char[1024];
		int ncb = 0;
		StringBuilder sb = null;
		boolean gotSomething = false;
		
		// Read until we get EOF or end of line.
		try {
			int c;
			while ((c = s.read()) >= 0) {
				gotSomething = true;
				if (c == '\n') {
					break;
				}
				if (ncb >= cb.length) {
					if (sb == null) {
						sb = new StringBuilder(2*ncb);
					}
					sb.append(cb, 0, ncb);
					ncb = 0;
				}
				cb[ncb++] = (char)(c & 0xff);
			}
		} catch (IOException e) {
			// ignore; treat errors as EOF.
		}
		
		// If there was a CR before the NL, remove the CR.
		// It would have to be in the cb array.
		if (ncb > 0 && cb[ncb-1] == '\r') {
			ncb--;
		}

		// Return result: sb plus first ncb char in cb.
		String res = null;
		if (sb != null) {
			if (ncb > 0) {
				sb.append(cb, 0, ncb);
			}
			res = sb.toString();
		} else if (gotSomething) {
			res = new String(cb, 0, ncb);
		}
		return res;
	}

	/**
	 * Read a stream into str. Stop at EOF or after maxLen bytes.
	 * @param istr The stream to read.
	 * @param maxLen Number of bytes to read. If -1, read to EOF.
	 * @param str Append the data to this buffer.
	 * @throws IOException
	 */
	public static void readInputStream(InputStream istr, int maxLen, StringBuilder str)
			throws IOException
	{
		if (maxLen < 0) {
			maxLen = Integer.MAX_VALUE;
		}
		if (istr != null) {
			int n;
			byte[] buff = new byte[8192];
			int nx = buff.length;
			if (nx > maxLen) {
				nx = maxLen;
			}
			while ((n = istr.read(buff, 0, nx)) > 0) {
				str.append(new String(buff, 0, n));
				maxLen -= n;
			}
		}
	}

	/**
	 *	Read and return the contents of a file.
	 *	@param fileName The name of the file.
	 *	@return The file contents.
	 *	@throws IOException If we cannot open or read the file.
	 */
	public static String readFile(String fileName)
		throws IOException
	{
		StringBuilder fileBuff = new StringBuilder();
		int n;
		char[] buff = new char[4096];
		FileReader reader = new FileReader(fileName);
		try {
			while ((n = reader.read(buff, 0, buff.length)) > 0) {
				fileBuff.append(buff, 0, n);
			}
		} finally {
			try { reader.close(); } catch (Exception e) {}
		}
		return fileBuff.toString();
	}

	/**
	 *	Read and return the contents of a file.
	 *	@param file The file.
	 *	@return The file contents.
	 *	@throws IOException If we cannot open or read the file.
	 */
	public static String readFile(File file)
		throws IOException
	{
		StringBuilder fileBuff = new StringBuilder();
		int n;
		char[] buff = new char[4096];
		FileReader reader = new FileReader(file);
		try {
			while ((n = reader.read(buff, 0, buff.length)) > 0) {
				fileBuff.append(buff, 0, n);
			}
		} finally {
			try { reader.close(); } catch (Exception e) {}
		}
		return fileBuff.toString();
	}
	
	/**
	 *	Given a string that may be a URL or a file name,
	 *	if it can be opened and read, return an InputStream for it.
	 *
	 *	@param name
	 *		A URL, or the name of a local file.
	 *	@return
	 *		An InputStream. Returns null if name is null or blank.
	 *	@throws java.io.IOException
	 *		If we cannot create an InputStream for this name.
	 */
	public static InputStream getInputStreamForFileOrURL(String name)
		throws java.io.IOException
	{
		if (name == null || name.equals("")) {
			return null;
		}
		try {
			return new URL(name).openStream();
		} catch (MalformedURLException e) {
			// fall thru to try as file.
		} catch (UnknownHostException e) {
			// fall thru to try as file.
		}
		return new FileInputStream(name);
	}

	/**
	 *	Given a string that may be a URL or a file name,
	 *	if it can be opened and read, return a BufferedReader for it.
	 *
	 *	@param name
	 *		A URL, or the name of a local file.
	 *	@return
	 *		A BufferedReader. Returns null if name is null or blank.
	 *	@throws java.io.IOException
	 *		If we cannot create a BufferedReader for this name.
	 */
	public static BufferedReader getBufferedReaderForFileOrURL(String name)
		throws java.io.IOException
	{
		InputStream is = getInputStreamForFileOrURL(name);
		if (is == null) {
			return null;
		} else {
			return new BufferedReader(new InputStreamReader(is));
		}
	}

	/**
	 *	Sleeps for no less than sleepFor milliseconds.
	 *	Unlike Thread.sleep(), guarantees minimum sleep time.
	 *
	 *	@param sleepFor the number of milliseconds to wait
	 */
	public static void sleep(long sleepFor)
	{
		sleep(sleepFor, null);
	}

	/**
	 *	Sleeps for no less than sleepFor milliseconds.
	 *	Unlike Thread.sleep(), guarantees minimum sleep time.
	 *
	 *	@param sleepFor the number of milliseconds to wait
	 *	@param ignoreInterrupts
	 *		Determines whether to return when interrupted.
	 *		If false, return when interrupted.
	 *		If true (or null), ignore interrupts and wait the remaining time.
	 *		Typically this is a Thread's "isRunning" flag.
	 */
	public static void sleep(long sleepFor, AtomicBoolean ignoreInterrupts)
	{
		long startTime = System.currentTimeMillis();
		long haveBeenSleeping = 0;
		while (haveBeenSleeping < sleepFor) {
			try {
				Thread.sleep(sleepFor - haveBeenSleeping);
			} catch (InterruptedException ex) {
				if (ignoreInterrupts != null && !ignoreInterrupts.get()) {
					return;
				}
			}
			haveBeenSleeping = (System.currentTimeMillis() - startTime);
		}
	}

	/**
	 *	Given an Enumeration of Strings, return the Strings
	 *	as a sorted array.
	 */
	public static String[] strEnumToSortedArray(Enumeration<String> e)
	{
		ArrayList<String> rawList = new ArrayList<String>(100);
		if (e != null) {
			while (e.hasMoreElements()) {
				rawList.add(e.nextElement());
			}
		}
		String[] sortedList = rawList.toArray(new String[rawList.size()]);
		Arrays.sort(sortedList);
		return sortedList;
	}

	/**
	 *	Return true iff s matches a String in an array.
	 *
	 *	@param s The String to test.
	 *	@param arr An array of Strings.
	 *	@return True iff s equals one of the strings in arr.
	 */
	public static boolean strOnList(String s, String[] arr)
	{
		if (s != null && arr != null) {
			for (int i = arr.length; --i >= 0; ) {
				if (s.equals(arr[i])) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 *	Return true if two internet addresses and ports are identical.
	 *	If necessary, hostnames are resolved to IP addresses,
	 *	and the resulting IP addresses are compared.
	 *	Thus this method says that "localhost" and "127.0.0.1" are identical.
	 *<p>
	 *	The addresses can be specified as strings or InetAddresses, or both.
	 *	The strings may be symbolic hostnames or numeric IP addresses.
	 *	This method uses the most efficient test possible given
	 *	the information supplied.
	 *<p>
	 *	For efficiency, if both addresses are passed
 	 *	as numeric IP address strings, we compare the strings.
	 *	Thus we assume that numeric IP address strings are always in
	 *	cannonical format.
	 *<p>
	 *	To compare addresses without comparing ports, pass 0 for both ports (duh).
	 *
	 *	@param a1
	 *		String version of first address
	 *		(hostname or numeric IP address), or null.
	 *	@param a1inet
	 *		InetAddress for first address, or null.
	 *		a1 and a1inet cannot both be null.
	 *	@param p1
	 *		Port number for first address.
	 *	@param a2
	 *		String version of second address
	 *		(hostname or numeric IP address), or null.
	 *	@param a2inet
	 *		InetAddress for second address, or null.
	 *		a2 and a2inet cannot both be null.
	 *	@param p2
	 *		Port number for second address.
	 *	@return
	 *		True iff the two addresses and ports match.
	 *	@see #addrsMatch(String[],InetAddress[],int[],String,InetAddress,int)
	 *	@see #anyAddrsMatch
	 */
	public static boolean addrsMatch(
				String a1, 
				InetAddress a1inet, 
				int p1, 
				String a2, 
				InetAddress a2inet,
				int p2)
	{
		if (p1 != p2) {
			return false;
		}

		if (a1 != null && a2 != null && a1.equalsIgnoreCase(a2)) {
			return true;
		}
	
		try {
			if (a1inet == null) {
				a1inet = InetAddress.getByName(a1);
			}
			if (a2inet == null) {
				a2inet = InetAddress.getByName(a2);
			}
			return a1inet.equals(a2inet);
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 *	Return true if two internet addresses and ports match.
	 *	If necessary, hostnames are resolved to IP addresses,
	 *	and the resulting IP addresses are compared.
	 *	Thus this method says that "localhost" and "127.0.0.1" are identical.
	 *<p>
	 *	In addition, if the second address is specified as a hostname
	 *	which is mapped to multiple IP addresses, then they match if
	 *	any of those IP addresses matches the first IP address.
	 *<p>
	 *	The addresses can be specified as strings or InetAddresses, or both.
	 *	The strings may be symbolic hostnames or numeric IP addresses.
	 *	This method uses the most efficient test possible given
	 *	the information supplied.
	 *<p>
	 *	For efficiency, if both addresses are passed
 	 *	as numeric IP address strings, we compare the strings.
	 *	Thus we assume that numeric IP address strings are always in
	 *	cannonical format.
	 *<p>
	 *	To compare addresses without comparing ports, pass 0 for both ports (duh).
	 *
	 *	@param a1
	 *		String version of first address
	 *		(hostname or numeric IP address), or null.
	 *	@param a1inet
	 *		InetAddress for first address, or null.
	 *		a1 and a1inet cannot both be null.
	 *	@param p1
	 *		Port number for first address.
	 *	@param a2
	 *		String version of second address
	 *		(hostname or numeric IP address), or null.
	 *	@param a2inet
	 *		The InetAddresses assigned to the second address, or null.
	 *		a2 and a2inet cannot both be null.
	 *	@param p2
	 *		Port number for second address.
	 *	@return
	 *		True iff the ports match, and if any address assigned to a2 matches a1.
	 *	@see #addrsMatch
	 *	@see #anyAddrsMatch(String[],InetAddress[],int[],String,InetAddress[],int)
	 *	@see InetAddress#getAllByName(String)
	 */
	public static boolean anyAddrsMatch(
				String a1,
				InetAddress a1inet,
				int p1,
				String a2,
				InetAddress[] a2inet,
				int p2)
	{
		if (p1 != p2) {
			return false;
		}

		if (a1 != null && a2 != null && a1.equalsIgnoreCase(a2)) {
			return true;
		}

		try {
			if (a1inet == null) {
				a1inet = InetAddress.getByName(a1);
			}
			if (a2inet == null) {
				a2inet = InetAddress.getAllByName(a2);
			}
			for (int i = 0; i < a2inet.length; i++) {
				if (a1inet.equals(a2inet[i])) {
					return true;
				}
			}
			return false;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 *	Return true if an internet address and port
	 *	matches one of a set of addresses and ports.
	 *	If necessary, hostnames are resolved to IP addresses,
	 *	and the resulting IP addresses are compared.
	 *	Thus this method says that "localhost" and "127.0.0.1" are identical.
	 *<p>
	 *	The addresses can be specified as strings or InetAddresses, or both.
	 *	The strings may be symbolic hostnames or numeric IP addresses.
	 *	This method uses the most efficient test possible given
	 *	the information supplied.
	 *<p>
	 *	For efficiency, if both addresses are passed
 	 *	as numeric IP address strings, we compare the strings.
	 *	Thus we assume that numeric IP address strings are always in
	 *	cannonical format.
	 *<p>
	 *	To compare addresses without comparing ports, pass null for p1arr.
	 *
	 *	@param a1arr
	 *		Array of String versions of first addresses
	 *		(hostname or numeric IP address), or null.
	 *	@param a1inetArr
	 *		Array of InetAddresses for first addresses, or null.
	 *		a1arr and a1inetArr cannot both be null.
	 *	@param p1arr
	 *		An array of port numbers for first address.
	 *		If null, ignore the port number test.
	 *	@param a2
	 *		String version of second address
	 *		(hostname or numeric IP address), or null.
	 *	@param a2inet
	 *		InetAddress for second address, or null.
	 *		a2 and a2inet cannot both be null.
	 *	@param p2
	 *		Port number for second address.
	 *	@return
	 *		True iff p2 matches a port in p1arr,
	 *		and a2 matches an address in a1arr/a1inetArr.
	 *	@see #addrsMatch(String,InetAddress,int,String,InetAddress,int)
	 *	@see #anyAddrsMatch
	 */
	public static boolean addrsMatch(
				String[] a1arr, 
				InetAddress[] a1inetArr, 
				int[] p1arr, 
				String a2, 
				InetAddress a2inet,
				int p2)
	{
		if (p1arr != null) {
			boolean portMatch = false;
			for (int i = p1arr.length; --i >= 0; ) {
				if (p2 == p1arr[i]) {
					portMatch = true;
					break;
				}
			}
			if (!portMatch) {
				return false;
			}
		}

		int n;
		if (a1arr != null) {
			n = a1arr.length;
		} else if (a1inetArr != null) {
			n = a1inetArr.length;
		} else {
			return false;
		}

		if (a1arr != null && a2 != null) {
			for (int i = 0; i < n; i++) {
				String a1 = a1arr[i];
				if (a1 != null && a1.equalsIgnoreCase(a2)) {
					return true;
				}
			}
		}

		for (int i = 0; i < n; i++) {
			String a1 = (a1arr != null) ? a1arr[i] : null;
			InetAddress a1inet = (a1inetArr != null) ? a1inetArr[i] : null;
			try {
				if (a1inet == null) {
					a1inet = InetAddress.getByName(a1);
				}
				if (a2inet == null) {
					a2inet = InetAddress.getByName(a2);
				}
				if (a1inet.equals(a2inet)) {
					return true;
				}
			} catch (Exception e) {
			}
		}
		return false;
	}

	/**
	 *	Return true if an internet address and port
	 *	matches one of a set of addresses and ports.
	 *	If necessary, hostnames are resolved to IP addresses,
	 *	and the resulting IP addresses are compared.
	 *	Thus this method says that "localhost" and "127.0.0.1" are identical.
	 *<p>
	 *	In addition, if the second address is specified as a hostname
	 *	which is mapped to multiple IP addresses, then they match if
	 *	any of those IP addresses match any of the addresses in the first set.
	 *<p>
	 *	The addresses can be specified as strings or InetAddresses, or both.
	 *	The strings may be symbolic hostnames or numeric IP addresses.
	 *	This method uses the most efficient test possible given
	 *	the information supplied.
	 *<p>
	 *	For efficiency, if both addresses are passed
 	 *	as numeric IP address strings, we compare the strings.
	 *	Thus we assume that numeric IP address strings are always in
	 *	cannonical format.
	 *<p>
	 *	To compare addresses without comparing ports, pass null for p1arr.
	 *
	 *	@param a1arr
	 *		Array of String versions of first addresses
	 *		(hostname or numeric IP address), or null.
	 *	@param a1inetArr
	 *		Array of InetAddresses for first addresses, or null.
	 *		a1arr and a1inetArr cannot both be null.
	 *	@param p1arr
	 *		An array of port numbers for first address.
	 *		If null, ignore the port number test.
	 *	@param a2
	 *		String version of second address
	 *		(hostname or numeric IP address), or null.
	 *	@param a2inet
	 *		InetAddress for second address, or null.
	 *		a2 and a2inet cannot both be null.
	 *	@param p2
	 *		Port number for second address.
	 *	@return
	 *		True iff p2 matches a port in p1arr, and if any address
	 *		assigned to a2 matches an address in a1arr/a1inetArr.
	 *	@see #anyAddrsMatch(String,InetAddress,int,String,InetAddress[],int)
	 *	@see #addrsMatch
	 *	@see InetAddress#getAllByName(String)
	 */
	public static boolean anyAddrsMatch(
				String[] a1arr, 
				InetAddress[] a1inetArr, 
				int[] p1arr, 
				String a2, 
				InetAddress[] a2inet,
				int p2)
	{
		if (p1arr != null) {
			boolean portMatch = false;
			for (int i = p1arr.length; --i >= 0; ) {
				if (p2 == p1arr[i]) {
					portMatch = true;
					break;
				}
			}
			if (!portMatch)
				return false;
		}

		int n;
		if (a1arr != null) {
			n = a1arr.length;
		} else if (a1inetArr != null) {
			n = a1inetArr.length;
		} else {
			return false;
		}

		for (int i = 0; i < n; i++) {
			String a1 = (a1arr != null) ? a1arr[i] : null;
			InetAddress a1inet = (a1inetArr != null) ? a1inetArr[i] : null;
			if (a1 != null && a2 != null && a1.equalsIgnoreCase(a2)) {
				return true;
			}
	
			try {
				if (a1inet == null) {
					a1inet = InetAddress.getByName(a1);
				}
				if (a2inet == null) {
					a2inet = InetAddress.getAllByName(a2);
				}
				for (int j = 0; j < a2inet.length; j++) {
					if (a1inet.equals(a2inet[j])) {
						return true;
					}
				}
			} catch (Exception e) {
			}
		}
		return false;
	}

	/**
	 *	Return the InetAddress for the symbolic address "host."
	 *	Equivalent to getByName(String) in java.net.InetAddress,
	 *	except that instead of throwing an exception,
	 *	this method returns null if "host" cannot
	 *	be resolved to an IP address.
	 *
	 *	@param addr	A hostname or symbolic IP address.
	 *	@return An InetAddress for host, or null.
	 */
	public static InetAddress getInetAddrByName(String addr)
	{
		try {
			return InetAddress.getByName(addr);
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * Return a list of all entries in a jar file.
	 * @param jarPath The name of the jar file.
	 * @param content If not null, append names to this list and return it.
	 * 		If null, create and return a new List.
	 * @return A List of the entries.
	 */
	public static List<String> getJarContent(String jarPath, List<String> content)
	{
		JarFile jarFile = null;
		if (content == null) {
			content = new ArrayList<>();
		}
		try {
			jarFile = new JarFile(jarPath);
			Enumeration<JarEntry> e = jarFile.entries();
			while (e.hasMoreElements()) {
				JarEntry entry = (JarEntry) e.nextElement();
				String name = entry.getName();
				content.add(name);
			}
			return content;
		} catch (Exception e) {
			return content;
		} finally {
			if (jarFile != null) {
				try {jarFile.close();} catch (Exception e) {}
			}
		}
	}
	
	/**
	 * Return the names of all items in the class path jars.
	 * @return A list with the names of all items in the classpath jars.
	 */
	public static List<String> getClassPathFileNames()
	{
		List<String> content = new ArrayList<>();
		for (String jar: System.getProperty("java.class.path", "")
				.split(System.getProperty("path.separator", ";"))) {
			if (jar.endsWith(".jar")) {
				getJarContent(jar, content);
			}
		}
		return content;
	}
	
	/**
	 * Simple method to run a set of tasks in parallel and wait for all to complete.
	 * The tasks cannot return values, and
	 * new threads are created for each invocation,
	 * If you need more control, or need return values, use the thread pool tools
	 * in java.util.concurrent.
	 * @param tasks The tasks to run.
	 * @param taskName A base name for the threads. Ignored if null or blank.
	 */
	public static void runTasksAndWait(List<Runnable> tasks, String taskName)
	{
		if (tasks == null || tasks.isEmpty()) {
			return;
		}
		if (tasks.size() == 1) {
			// Optimize if there's only one task.
			tasks.get(0).run();
		} else {
			Thread[] workers = new Thread[tasks.size()];
			for (int iTask = 0; iTask < tasks.size(); iTask++) {
				Thread t = new Thread(null, tasks.get(iTask),
							(taskName != null && !taskName.isBlank() ? (taskName + "-" + iTask) : null));
				t.start();
				workers[iTask] = t;
			}
			for (Thread t: workers) {
				while (t.isAlive()) {
					try {
						t.join();
					} catch (InterruptedException e) {
						// Ignore interrupts to calling thread.
					}		
				}
			}
		}
	}

	/**
	 *	Test driver: for each argument, call hexToBytes(),
	 *	and then call bytesToHex() on the result to verify
	 *	we get the original back.
	 */
	public static void main(String[] args)
	{
		for (int i = 0; i < args.length; i++) {
			System.out.print(args[i] + ":");
			byte[] b = hexToBytes(args[i]);
			String s = bytesToHex(b);
			if (s.equals(args[i])) {
				System.out.println(" ok");
			} else {
				System.out.println(" mismatch\n   "
						+ b.length + " " + s.length() + " \"" + s + "\"");
			}
		}
		String line = null;
		while ((line = readLine(System.in)) != null) {
			byte[] b = line.getBytes();
			System.out.println("got " + line.length() + "/" + b.length + " bytes");
		}
	}
}
