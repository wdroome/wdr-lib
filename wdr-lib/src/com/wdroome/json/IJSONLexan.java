package com.wdroome.json;

import java.io.IOException;

/**
 * Interface for a JSON lexical analyzer.
 * @author wdr
 */
public interface IJSONLexan
{
	/**
	 * Return the next token, or null if no more.
	 */
	public JSONLexanToken nextToken() throws IOException;

	/**
	 * Return the next token, or null if no more,
	 * but push it back so that the next {@link #nextToken()}
	 * call will return it.
	 */
	public JSONLexanToken peekToken() throws IOException;
	
	/**
	 * Return an estimate of the input size, if possible.
	 * @return An estimate of the input size, in bytes.
	 * 		If the size is unknown, return 0 or -1.
	 */
	public long estimatedSize();
	
	/**
	 * Return a string describing the location of the last token returned by {@link #nextToken()}.
	 * Normally this is of the form "LINE.OFFSET in FILE-NAME".
	 * May return null.
	 */
	public String lastTokenLocation();
}
