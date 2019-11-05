package com.wdroome.json;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

/**
 * Report the differences between two JSON values.
 * @author wdr
 */
public class JSONDiff
{
	private final PrintStream m_errorOut;
	
	/**
	 * Create new comparator.
	 * Write differences to @{link System#err}.
	 */
	public JSONDiff()
	{
		this(null);
	}
	
	/**
	 * Create new comparator.
	 * @param errorOut
	 * 		The stream to report differences. If null, use @{link System#err}.
	 */
	public JSONDiff(PrintStream errorOut)
	{
		if (errorOut == null) {
			errorOut = System.err;
		}
		m_errorOut = errorOut;
	}
	
	/**
	 * Compare two JSON values and write the differences
	 * to the error stream give to the c'tor.
	 * @param v1 The first JSON value.
	 * @param v2 The second JSON value.
	 * @return True iff the two values are the same.
	 * @see #error(String, String)
	 */
	public boolean diff(JSONValue v1, JSONValue v2)
	{
		return diff("", v1, v2);
	}
	
	/**
	 * Log a difference. The base class writes a line to the
	 * stream given to the c'tor. Child classes may override this method.
	 * @param path
	 * 		The path of the object that differs. null or "" mean
	 * 		the difference is at the root.
	 * @param msg
	 * 		A description of the difference.
	 */
	public void error(String path, String msg)
	{
		if (path == null || path.equals("")) {
			path = "ROOT";
		}
		m_errorOut.println("JSON values differ at " + path + ": " + msg);
	}
	
	/**
	 * Recursively compare two JSON values.
	 * @param path The path of these two values.
	 * @param v1 The first value.
	 * @param v2 The second value.
	 * @return True iff the two values are the same.
	 */
	private boolean diff(String path, JSONValue v1, JSONValue v2)
	{
		String t1 = v1.jsonType();
		String t2 = v2.jsonType();
		if (!t1.equals(t2)) {
			error(path, "Type mismatch: " + t1 + " vs " + t2);
			return false;
		}
		
		if (v1 instanceof JSONValue_Object) {
			boolean matches = true;
			JSONValue_Object o1 = (JSONValue_Object)v1;
			JSONValue_Object o2 = (JSONValue_Object)v2;
			for (String key: o1.keySet()) {
				JSONValue x2 = o2.get(key);
				if (x2 == null) {
					error(path, "Key \"" + key + "\" is only in first object");
					matches = false;
				} else {
					if (!diff((path != null && !path.equals("") ? (path + ".") : "") + key,
								o1.get(key), x2)) {
						matches = false;
					}
				}
			}
			for (String key: o2.keySet()) {
				JSONValue x1 = o1.get(key);
				if (x1 == null) {
					error(path, "Key \"" + key + "\" is only in second object");
					matches = false;
				}
			}
			return matches;
		}
		
		if (v1 instanceof JSONValue_Array) {
			boolean matches = true;
			JSONValue_Array a1 = (JSONValue_Array)v1;
			JSONValue_Array a2 = (JSONValue_Array)v2;
			int n1 = a1.size();
			int n2 = a2.size();
			int n = (n1 < n2) ? n1 : n2;
			for (int i = 0; i < n; i++) {
				if (!diff(path + "[" + i + "]", a1.get(i), a2.get(i))) {
					matches = false;
				}
			}
			if (n1 < n2) {
				error(path, "Second array has " + (n2-n1) + " additional element(s)");
			} else if (n1 > n2) {
				error(path, "First array has " + (n1-n2) + " additional element(s)");
			}
			return matches;
		}
		
		if (!v1.equals(v2)) {
			error(path, v1.toString() + " vs " + v2.toString());
			return false;
		}
		return true;
	}
	
	/**
	 * Read and parse a file with a JSON value.
	 * @param fname The file name. If null or "-", read standard input.
	 * @return The JSON value of the file, or null if error.
	 * 		If error, print message on System.err.
	 */
	private static JSONValue readFile(String fname)
	{
		try {
			if (fname == null || fname.equals("-") || fname.equals("")) {
				return JSONParser.parse(new JSONLexan(System.in), false);
			} else {
				return JSONParser.parse(new JSONLexan(new File(fname)), false);
			}
		} catch (JSONParseException e) {
			System.err.println("\"" + fname + "\": json parsing error: " + e.toString());
			return null;
		} catch (FileNotFoundException e) {
			System.err.println("Cannot open json file \"" + fname + "\"");
			return null;
		}
	}
	
	/**
	 * Compare two JSON files.
	 * @param args The names of the two JSON files. "-" means read standard input.
	 */
	public static void main(String[] args)
	{
		if (args.length != 2) {
			System.err.println("Usage: JSONDiff file1 file2");
			System.exit(1);
		}
		String f1 = args[0];
		String f2 = args[1];
		
		JSONValue v1 = readFile(f1);
		JSONValue v2 = readFile(f2);

		if (v1 == null || v2 == null) {
			System.exit(1);
		} else if (!new JSONDiff().diff(v1, v2)) {
			System.exit(1);
		} else {
			System.exit(0);
		}
	}
}
