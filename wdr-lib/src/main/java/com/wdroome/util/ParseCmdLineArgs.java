// Tab == 4 spaces.

 //================================================================
 //  Alcatel-Lucent provides the software in this file for the
 //  purpose of obtaining feedback.  It may be copied and modified.
 //
 //  Alcatel-Lucent is not liable for any consequence of loading
 //  or running this software or any modified version thereof.
 //================================================================

package com.wdroome.util;

import java.util.ArrayList;

/**
 *	Parse a single string into tokens separated by white space.
 *	Unlike {@link String#split(String)}, this class considers
 *	quoted strings to be single tokens. The class recognizes
 *	single or double quotes, and accepts backslash-quote as
 *	an escape for an embedded quote.
 */
public class ParseCmdLineArgs
{
	private final String m_line;
	private int m_curOffset = 0;
	private final int m_length;

	/**
	 *	Create a new parser for the indicated string.
	 */
	public ParseCmdLineArgs(String line)
	{
		if (line == null) {
			line = "";
		}
		this.m_line = line;
		this.m_length = line.length();
	}

	/**
	 *	Return a String[] with the remaining arguments.
	 *	That is, call {@link #nextArg()} until it returns null,
	 *	collect the returned Strings in an array, and return the array.
	 *	If called before calling {@link #nextArg()}, this method
	 *	returns a String[] for all the tokens in the string given to the c'tor.
	 */
	public String[] getRemainingArgs()
	{
		ArrayList<String> argList = new ArrayList<String>(20);
		String s;
		while ((s = nextArg()) != null) {
			argList.add(s);
		}
		return (String[])argList.toArray(new String[argList.size()]);
	}

	/**
	 *	Return the next argument. If quoted, strip the quotes.
	 *	If no more arguments, return null.
	 */
	public String nextArg()
	{
		for (; m_curOffset < m_length; m_curOffset++) {
			char c = m_line.charAt(m_curOffset);
			if (!isWhitespace(c)) {
				break;
			}
		}
		if (m_curOffset >= m_length) {
			return null;
		}
		StringBuilder arg = new StringBuilder(100);
		boolean inQuote = false;
		char startQuote = 0;
		for (; m_curOffset < m_length; m_curOffset++) {
			char c = m_line.charAt(m_curOffset);
			if (isEscape(c) && m_curOffset+1 < m_length) {
				m_curOffset++;
				arg.append(m_line.charAt(m_curOffset));
			} else if (inQuote) {
				if (isEndQuote(c, startQuote)) {
					inQuote = false;
				} else {
					arg.append(c);
				}
			} else if (isStartQuote(c)) {
				inQuote = true;
				startQuote = c;
			} else if (isWhitespace(c)) {
				break;
			} else {
				arg.append(c);
			}
		}
		return arg.toString();
	}

	/**
	 *	Return true if c is a whitespace character.
	 *	Base class version returns Character.isWhitespace(c).
	 *	Subclass can override.
	 */
	protected boolean isWhitespace(char c)
	{
		return Character.isWhitespace(c);
	}

	/**
	 *	Return true if c is a start-of-quote character.
	 *	Base class version returns true if c is single or double quote.
	 *	Subclass can override.
	 */
	protected boolean isStartQuote(char c)
	{
		return c == '\"' || c == '\'';
	}

	/**
	 *	Return true iff c is a end-of-quote character for a quote
	 *	started by the startQuote character.
	 *	Base class version returns true if c == startQuote.
	 *	Subclass can override.
	 */
	protected boolean isEndQuote(char c, char startQuote)
	{
		return c == startQuote;
	}

	/**
	 *	Return true if c is an escape character.
	 *	Base class version returns true if c is backslash.
	 *	Subclass can override.
	 */
	protected boolean isEscape(char c)
	{
		return c == '\\';
	}

	/**
	 *	For testing, create a new ParseCmdLineArgs object for each argument,
	 *	and parse the argument string into sub-arguments.
	 */
	public static void main(String[] args)
	{
		for (int i = 0; i < args.length; i++) {
			System.out.println(args[i]);
			String[] subargs = new ParseCmdLineArgs(args[i]).getRemainingArgs();
			System.out.println("  " + subargs.length + " args:");
			for (int j = 0; j < subargs.length; j++) {
				System.out.println("  [" + j + "] \"" + subargs[j] + "\"");
			}
		}
	}
}
