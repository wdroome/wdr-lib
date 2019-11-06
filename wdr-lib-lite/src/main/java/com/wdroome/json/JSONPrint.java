package com.wdroome.json;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Pretty-print json input in a standard format.
 * @author wdr
 */
public class JSONPrint
{
	/**
	 * Pretty-print JSON input to an output stream.
	 * If the input cannot be parsed, write an error message to standard error.
	 * @param out The output stream.
	 * @param in The input.
	 * @param desc A description of the input, for error messages.
	 * @return True if the input could be parsed, false if not.
	 */
	public static boolean print(PrintStream out, JSONLexan in, String desc)
	{
		try {
			JSONValue value = JSONParser.parse(in, false);
			JSONWriter writer = new JSONWriter(out);
			writer.setSorted(true);
			writer.setIndented(true);
			value.writeJSON(writer);
			writer.writeNewline();
			return true;
		} catch (JSONException e) {
			System.err.println(desc + ": Parsing error: " + e);
			return false;
		} catch (IOException e) {
			System.err.println(desc + ": IO Error: " + e);
			return false;
		}
	}

	/**
	 * Pretty-print the JSON stream in standard input to an output stream.
	 * If the input cannot be parsed, write an error message to standard error.
	 * @param out The output stream.
	 * @return True if the input could be parsed, false if not.
	 */
	public static boolean printSysIn(PrintStream out)
	{
		return print(out, new JSONLexan(System.in), "standard input");
	}

	/**
	 * Read one or more files of JSON input, and pretty-print them to standard output.
	 * Arguments are file names. If no arguments, read standard input.
	 * "-" as a file name also means read standard input.
	 * If there is an error reading or parsing any file,
	 * when done, exit with status code 1.
	 * @param args File names.
	 */
	public static void main(String[] args)
	{
		if (args == null || args.length == 0) {
			args = new String[] {"-"};
		}
		int nFailed = 0;
		for (int i = 0; i < args.length; i++) {
			String fname = args[i];
			if (i > 0) {
				System.out.println();
			}
			if (args.length > 1) {
				System.out.println(fname + ":");
			}
			if (fname.equals("-")) {
				if (!printSysIn(System.out))
					nFailed++;
			} else {
				try {
					if (!print(System.out, new JSONLexan(new File(fname)), fname)) {
						nFailed++;
					}
				} catch (Exception e) {
					System.err.println(fname + ": Error opening file: " + e.toString());
					nFailed++;
				}
			}
		}
		if (nFailed > 0) {
			System.exit(1);
		}
	}
}
