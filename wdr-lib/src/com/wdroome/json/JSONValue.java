package com.wdroome.json;

import java.io.IOException;

/**
 * Marker class for JSON Value types.
 * @author wdr
 */
public interface JSONValue
{
	/**
	 * Return true if this is a simple scalar value,
	 * such as a string or a number or a keyword,
	 * as opposed to a compound value, like an array
	 * or an object dictionary.
	 */
	public boolean isSimple();
	
	/**
	 * Write JSON for this value.
	 * @param writer A JSON-specific writer.
	 * @throws IOException If an I/O error occurs.
	 */
	public void writeJSON(JSONWriter writer) throws IOException;
}
