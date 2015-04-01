package com.wdroome.altomsgs;

import com.wdroome.json.IJSONLexan;
import com.wdroome.json.JSONLexan;
import com.wdroome.json.JSONException;
import com.wdroome.json.JSONFieldMissingException;
import com.wdroome.json.JSONParseException;
import com.wdroome.json.JSONValueTypeException;
import com.wdroome.json.JSONValue_Array;
import com.wdroome.json.JSONValue_Object;

/**
 * Representation of an ALTO "general response" response message.
 * Like other responses, it has "meta" and "data" objects,
 * but it lets you get/set arbitrary items in the data dictionary.
 * However, the items in data must be JSONObjects or JSONArrays.
 * <p>
 * This is a private message type, and is not defined in the ALTO protocol.
 * @author wdr
 */
public class AltoPriv_GeneralResponse extends AltoResp_Base
{
	public static final String MEDIA_TYPE = MEDIA_TYPE_PREFIX + "priv-genresp" + MEDIA_TYPE_SUFFIX;
	
	public static final String FN_DATA = "data";
	
	/**
	 * Create an empty object.
	 * Used by server to construct a message to send.
	 */
	public AltoPriv_GeneralResponse()
	{
		super();
	}
	
	/**
	 * Create an object from a JSON parser.
	 * Used to decode a received message.
	 * @param lexan The JSON input parser.
	 * @throws JSONParseException If jsonSrc isn't a valid JSON string.
	 * @throws JSONValueTypeException If the input isn't a JSON Object dictionary.
	 * @throws JSONFieldMissingException  If a required field isn't the correct type.
	 */
	public AltoPriv_GeneralResponse(IJSONLexan lexan)
		throws JSONParseException, JSONValueTypeException, JSONFieldMissingException
	{
		super(lexan);
	}
	
	/**
	 * Create an object from a JSON string.
	 * Used to decode a received message.
	 * @param jsonSrc The encoded JSON message.
	 * @throws JSONParseException If jsonSrc isn't a valid JSON string.
	 * @throws JSONValueTypeException If the input isn't a JSON Object dictionary.
	 * @throws JSONFieldMissingException  If a required field isn't the correct type.
	 */
	public AltoPriv_GeneralResponse(String jsonSrc)
		throws JSONParseException, JSONValueTypeException, JSONFieldMissingException
	{
		this(new JSONLexan(jsonSrc));
	}
	
	@Override
	public String getMediaType()
	{
		return MEDIA_TYPE;
	}
	
	@Override
	public String[] getMapNames()
	{
		return new String[] { FN_DATA };
	}

	
	public JSONValue_Object getObject(String name)
	{
		return m_map.getObject(name, null);
	}
	
	public JSONValue_Array getArray(String name)
	{
		return m_map.getArray(name, null);
	}
	
	public void set(String name, JSONValue_Object value)
	{
		m_map.put(name, value);
	}
	
	public void set(String name, JSONValue_Array value)
	{
		m_map.put(name, value);
	}
}
