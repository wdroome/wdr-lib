package com.wdroome.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * Miscellaneous static String utility functions.
 * @author wdr
 */
public class StringUtils
{
	/**
	 * Split "src" on the 'splitChar', and return the tokens as a String array.
	 * This always returns a non-empty array; if the "src" is empty or empty, 
	 * it returns an array with a single empty string.
	 * Handle backslash escapes in "src". That is, when "src" has a backslash,
	 * remove the backslash and treat the following character as part of a token,
	 * rather than a split character.
	 * @param src A string to be broken into tokens.
	 * @param splitChar The token separator character.
	 * @return A String array with the tokens in "src".
	 */
	public static String[] split(String src, char splitChar)
	{
		ArrayList<String> arr = new ArrayList<String>();
		int srcLen = (src != null) ? src.length() : 0;
		StringBuilder curStr = new StringBuilder();
		for (int iSrc = 0; iSrc < srcLen; iSrc++) {
			char c = src.charAt(iSrc);
			if (c == '\\' && iSrc+1 < srcLen) {
				curStr.append(src.charAt(iSrc+1));
				iSrc++;
			} else if (c == splitChar) {
				arr.add(curStr.toString());
				curStr = new StringBuilder();
			} else {
				curStr.append(c);
			}
		}
		arr.add(curStr.toString());
		return arr.toArray(new String[arr.size()]);
	}
	
	/**
	 * Split "src" on commas, and return the tokens as a String array.
	 * This always returns an array; if the "src" is empty, it returns an
	 * array with a single empty string.
	 * Handle backslash escapes in "src". That is, when "src" has a backslash,
	 * remove the backslash and treat the following character as part of a token,
	 * rather than a split character.
	 * @param src A string to be broken into tokens.
	 * @return A String array with the tokens in "src".
	 * @see #split(String,char)
	 */
	public static String[] split(String src)
	{
		return split(src, ',');
	}
	
	/**
	 * Remove any backslashes in src.
	 * Specifically, if src has a backslash, remove that backslash and retain the following character.
	 * If src does NOT contain backslashes, return src rather than a new String.
	 * Thus "\," becomes "," "\\" becomes "\",  "\\," becomes "\,", and so on.
	 * Note that this method JUST removes backslashes.
	 * It does NOT convert "\b" to a backspace, "\t" to a tab, or "\n" to a new-line.
	 * @param src A string possibly containing backslashes.
	 * @return If src has backslashes, return a new string without the backslashes.
	 * 		Otherwise return src.
	 */
	public static String removeBackslashes(String src)
	{
		if (src == null) {
			return src;
		}
		if (!src.contains("\\")) {
			return src;
		}
		int srcLen = src.length();
		StringBuilder b = new StringBuilder(srcLen);
		for (int iSrc = 0; iSrc < srcLen; iSrc++) {
			char c = src.charAt(iSrc);
			if (c == '\\' && iSrc+1 < srcLen) {
				b.append(src.charAt(iSrc+1));
				iSrc++;
			} else {
				b.append(c);
			}
		}
		return b.toString();
	}
	
	/**
	 * Split a string into three parts.
	 * Stop when we hit an unescaped instance of a character in stopChars.
	 * @param src The source string.
	 * @param stopChars A list of stop characters.
	 * @return An array of one or three strings.
	 * 		If the method returns one string, src did not contain an unescaped
	 *		of a character in stopChars, and the one string is "src".
	 * 		If the method returns three strings,
	 *		the first is src up to the first unescaped stop character,
	 *		the second is the stop character (as a string),
	 * 		and the third is the remainder of src, not including the stop char.
	 */
	public static String[] escapedSplit(String src, char[] stopChars)
	{
		int srcLen = src.length();
		if (stopChars == null) {
			stopChars = new char[0];
		}
		int stopCharsLen = stopChars.length;
		StringBuilder b = new StringBuilder();
		for (int iSrc = 0; iSrc < srcLen; iSrc++) {
			char c = src.charAt(iSrc);
			if (c == '\\' && iSrc+1 < srcLen) {
				b.append('\\');
				b.append(src.charAt(iSrc+1));
				iSrc++;
			} else {
				for (int iStopChar = 0; iStopChar < stopCharsLen; iStopChar++) {
					if (c == stopChars[iStopChar]) {
						return new String[] {b.toString(),
											 new String(stopChars, iStopChar, 1),
											 src.substring(iSrc+1)};
					}
				}
				b.append(c);
			}
		}
		return new String[] {b.toString()};
	}

	/**
	 * Escape double-quotes or backslashes in src.
	 * We assume that src does not have any other escapable characters
	 * (e.g., backspace, new-line, etc).
	 * Note that we optimize for the case where src does
	 * not contain any escapable characters.
	 * @param src A string to escape.
	 * @return src with " replaced by \" and \ replaced by \\.
	 * 		If src has neither character, just return s.
	 */
	public static String escapeSimpleJSONString(String src)
	{
		if (src == null) {
			return src;
		}
		if (!src.contains("\\") && !src.contains("\"")) {
			return src;
		}
		int srcLen = src.length();
		StringBuilder b = new StringBuilder(srcLen);
		for (int iSrc = 0; iSrc < srcLen; iSrc++) {
			char c = src.charAt(iSrc);
			if (c == '\\') {
				b.append("\\\\");
			} else if (c == '\"') {
				b.append("\\\"");
			} else {
				b.append(c);
			}
		}
		return b.toString();
	}
	
	/**
	 * Return true if "value" is in "list."
	 * @param list A list of strings. May be null.
	 * @param value The value to test. May be null.
	 * @return True iff "value" matches a string in "list."
	 *		If "list" is null, always return true.
	 *		If "list" is zero-length, always return false.
	 *		That is, a null array contains "all strings",
	 *		while a zero-length array contains "no strings."
	 */
	public static boolean contains(String[] list, String value)
	{
		if (list == null) {
			return true;
		}
		for (String s:list) { 
			if (value.equals(s)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Add the strings in an array to a set.
	 * @param arr The strings to add. Ignored if null.
	 * @param set The set to which the strings are added.
	 * 		If null, create a new {@link HashSet}.
	 * @return The set "set", or the new set if "set" is null.
	 */
	public static Set<String> addToSet(String[] arr, Set<String> set)
	{
		if (set == null) {
			set = new HashSet<String>((arr != null) ? arr.length : 5);
		}
		if (arr != null) {
			for (String s: arr) {
				set.add(s);
			}
		}
		return set;
	}
	
	/**
	 * Create a new Set and add the strings in an array.
	 * @param arr The strings to add. Ignored if null.
	 * @return The new set.
	 */
	public static Set<String> makeSet(String[] arr)
	{
		return addToSet(arr, null);
	}
	
	/**
	 * Return a type-safe {@link List} of Strings from an arbitrary List.
	 * Returns a new ArrayList with the {@link Object#toString} value
	 * of each object in the source list.
	 * @param src A list of objects. May be null.
	 * @return A List guaranteed to contain the String representation
	 * 		of each item in the source list.
	 * 		Never null; if src is null, return an empty List.
	 */
	public static List<String> makeStringList(List src)
	{
		if (src == null) {
			return new ArrayList<String>();
		}
		ArrayList<String> strList = new ArrayList<String>(src.size());
		for (Object elem: src) {
			if (elem == null) {
				strList.add("null");
			} else {
				strList.add(elem.toString());
			}
		}
		return strList;
	}
	
	/**
	 * Return a type-safe {@link Set} of Strings from an arbitrary Set.
	 * Returns a new HashSet with the {@link Object#toString} value
	 * of each object in the source set.
	 * @param src A set of objects. May be null.
	 * @return A Set guaranteed to contain the String representation
	 * 		of each item in the source set.
	 * 		Never null; if src is null, return an empty Set.
	 */
	public static Set<String> makeStringSet(Set src)
	{
		if (src == null) {
			return new HashSet<String>();
		}
		HashSet<String> strSet = new HashSet<String>(src.size());
		for (Object elem: src) {
			if (elem == null) {
				strSet.add("null");
			} else {
				strSet.add(elem.toString());
			}
		}
		return strSet;
	}
}
