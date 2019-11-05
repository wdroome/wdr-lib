package com.wdroome.json;

import java.util.List;

/**
 * A simple struct with a path to an element in a JSON Object and a value.
 * @author wdr
 */
public class JSONPathValuePair
{
	/** The path to the element. */
	public final List<String> m_path;
	
	/** The value for the element. */
	public final JSONValue m_value;
	
	/**
	 * Create a new instance.
	 * @param path
	 * 		The path to the element.
	 * 		The caller should not change the list after calling the c'tor.
	 * @param value The value for the element,
	 */
	public JSONPathValuePair(List<String> path, JSONValue value)
	{
		m_path = path;
		m_value = value;
	}
}
