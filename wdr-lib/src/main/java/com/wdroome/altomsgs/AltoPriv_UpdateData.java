package com.wdroome.altomsgs;

import java.util.Iterator;
import java.io.Reader;

import com.wdroome.json.IJSONLexan;
import com.wdroome.json.JSONLexan;
import com.wdroome.json.JSONException;
import com.wdroome.json.JSONParseException;
import com.wdroome.json.JSONValueTypeException;
import com.wdroome.json.JSONValue_Array;
import com.wdroome.json.JSONValue_Object;
import com.wdroome.util.inet.EndpointAddress;
import com.wdroome.util.String2;

/**
 * A class representing an ALTO update request message.
 * Although it's a request message, we extend the response message type,
 * because, as in a response message, this request may have a vtag resource id.
 * The message has two data components: an array of strings,
 * and a dictionary of objects. Both components are optional.
 *  * <p>
 * This is a private message type, and is not defined in the ALTO protocol.
 * @author wdr
 */
public class AltoPriv_UpdateData extends AltoResp_Base
{
	public static final String MEDIA_TYPE = MEDIA_TYPE_PREFIX + "updatedata" + MEDIA_TYPE_SUFFIX;
	
	private static final String FN_OBJECT_DATA = "object-data";
	private static final String FN_STRINGS = "strings";

	/**
	 * Create an empty response object.
	 * Used by server to construct a response to send to a client.
	 */
	public AltoPriv_UpdateData()
	{
		super();
	}
	
	/**
	 * Create an object from a JSON parser.
	 * Used to decode a received message.
	 * @param lexan The JSON input parser.
	 * @throws JSONParseException If jsonSrc isn't a valid JSON string.
	 * @throws JSONValueTypeException If the input isn't a JSON Object dictionary.
	 */
	public AltoPriv_UpdateData(IJSONLexan lexan)
		throws JSONParseException, JSONValueTypeException
	{
		super(lexan);
	}
	
	/**
	 * Create a response object from a JSON string.
	 * Used by client to decode a response from a server.
	 * @param jsonSrc The encoded JSON response.
	 * @throws JSONParseException If jsonSrc isn't a valid JSON string.
	 * @throws JSONValueTypeException If the input isn't a JSON Object dictionary.
	 */
	public AltoPriv_UpdateData(String jsonSrc)
		throws JSONParseException, JSONValueTypeException
	{
		this(new JSONLexan(jsonSrc));
	}
	
	/**
	 * Create a response object from a JSON Reader.
	 * Used by client to decode a response from a server.
	 * @param jsonSrc The encoded JSON response.
	 * @throws JSONParseException If jsonSrc isn't a valid JSON string.
	 * @throws JSONValueTypeException If the input isn't a JSON Object dictionary.
	 */
	public AltoPriv_UpdateData(Reader jsonSrc)
		throws JSONParseException, JSONValueTypeException
	{
		this(new JSONLexan(jsonSrc));
	}
	
	@Override
	public String[] getMapNames()
	{
		return new String[] { FN_OBJECT_DATA };
	}
	
	@Override
	public String[] getArrayNames()
	{
		return new String[] { FN_STRINGS };
	}
	
	@Override
	public String getMediaType()
	{
		return MEDIA_TYPE;
	}

	@Override
	protected boolean needPathNames()
	{
		return true;
	}

	/**
	 * Add a string to the array.
	 * @param value The string to add. Ignored if null.
	 */
	public void addString(String value)
	{
		if (value != null) {
			m_array.add(value);
		}
	}

	/**
	 * Add a set of strings to the array..
	 * @param values An array of strings.
	 */
	public void addStrings(String[] values)
	{
		for (String value: values)
			addString(value);
	}

	/**
	 * Add a set of strings to the array..
	 * @param values An collection of strings.
	 */
	public void addStrings(Iterable<String> values)
	{
		for (String s:values)
			addString(s);
	}
	
	/**
	 * Return the number of strings in the array.
	 * @return The number of strings in the array.
	 */
	public int nStrings()
	{
		return m_array.size();
	}
	
	/**
	 * Return the strings as a java array.
	 * @return A java array. Never null, but may be 0-length.
	 */
	public String[] getStrings()
	{
		return getStringArray(m_array);
	}
	
	/**
	 * Return the i'th string.
	 * @param i The index (starts with 0).
	 * @return The i'th string.
	 * @throws JSONException If i isn't a valid index.
	 */
	public String getString(int i) throws JSONException
	{
		return m_array.getString(i);
	}

	public void optString(String name)
	{
		m_map.getString(name, null);
	}

	public void optJSONObject(String name)
	{
		m_map.getObject(name, null);
	}

	public void optJSONArray(String name)
	{
		m_map.getArray(name, null);
	}
}
