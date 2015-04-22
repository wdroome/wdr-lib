package com.wdroome.altomsgs;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.lang.reflect.Constructor;

import com.wdroome.json.*;
import com.wdroome.json.validate.*;

/**
 * Base class for all ALTO request and response messages.
 * The messages consist of a JSON dictionary;
 * child classes determine the contents of the dictionary.
 * The class also provides several static utility methods
 * for working with JSON arrays.
 * @author wdr
 */
public abstract class AltoMsg_Base
{
	/**
	 * The dictionary in this message.
	 * The base class always creates it.
	 * The base c'tor {@link #AltoMsg_Base(IJSONLexan)} sets
	 * it to the JSON object defined by the lexical analyzer.
	 * The base c'tor {@link #AltoMsg_Base()} creates an empty dictionary,
	 * and expects the child class to populate it.
	 */
	protected final JSONValue_Object m_json;
	
	/** The prefix for the media type for all ALTO messages. */
	public static final String MEDIA_TYPE_PREFIX = "application/alto-";
	
	/** The suffix for the media type for all ALTO JSON messages. */
	public static final String MEDIA_TYPE_SUFFIX = "+json";

	/**
	 * Create an empty object.
	 * Used to construct a message to send.
	 */
	public AltoMsg_Base()
	{
		m_json = new JSONValue_Object();
		m_json.usePathNames(needPathNames());
	}
	
	/**
	 * Create an object from a JSON lexical analyzer.
	 * @param lexan The lexical analyzer.
	 * @throws JSONParseException
	 * 		If the input cannot be parsed.
	 * @throws JSONValueTypeException
	 * 		If the input is valid JSON, but it's not an Object.
	 */
	public AltoMsg_Base(IJSONLexan lexan) throws JSONParseException, JSONValueTypeException
	{
		m_json = JSONParser.parseObject(lexan, needPathNames());		
	}
	
	/**
	 * Return this message's media-type string.
	 * Note: Each subclass should also define a static final member
	 * named MEDIA_TYPE with this string.
	 */
	public abstract String getMediaType();
	
	/**
	 * Return true iff we should maintain path names
	 * for JSONobjects in this message.
	 * The base class returns false.
	 * Child classes that need to maintain path names should override and return true.
	 * @return True iff we should maintain path names for JSONobjects in this message.
	 */
	protected boolean needPathNames()
	{
		return false;
	}
	
	/**
	 * Return the JSON-encoding of this message (no white space).
	 * @return The JSON encoding for the response.
	 */
	public String getJSON()
	{
		return m_json.toString();
	}

	/**
	 * Return neatly-formatted JSON string.
	 */
	@Override
	public String toString()
	{
		return JSONUtil.toJSONString(m_json, true);
	}
	
	/**
	 * Write the JSON message using client-specified formatting.
	 * @param writer A JSON writer provided by the client.
	 * @throws IOException If writer throws an I/O error.
	 */
	public void writeJSON(JSONWriter writer) throws IOException
	{
		m_json.writeJSON(writer);
	}
	
	/**
	 * Validate the JSON object using the validator supplied by the child class.
	 * @return A list of errors, or null if the message passed validation.
	 */
	public List<String> validate()
	{
		return validate(m_json, getValidator());
	}
	
	/**
	 * Return a new validator for this message class.
	 * The base class returns null.
	 * @return A new validator for this message class,
	 * 		or null if there is no validator.
	 */
	protected JSONValidate getValidator()
	{
		return null;
	}
	
	/**
	 * Validate a JSON object.
	 * @param json The JSON Object to validate. If null, quietly return null.
	 * @param validator The validator. If null, quietly return null.
	 * @return A list of errors, or null if the message passed validation.
	 */
	public static List<String> validate(JSONValue_Object json, JSONValidate validator)
	{
		ArrayList<String> errors = null;
		if (validator != null && json != null) {
			synchronized (validator) {
				validator.collectErrors(null);
				errors = new ArrayList<String>();
				validator.collectErrors(errors);
				try {
					validator.validate(json);
				} catch (Exception e) {
					// Shouldn't happen, but just in case ....
					errors.add(e.toString());
				}
				if (errors.isEmpty()) {
					errors = null;
				}
			}
		}
		return errors;
	}

	/**
	 * Return an approximate limit on the size of JSON input
	 * for which this representation class is suitable.
	 * If no limit, return 0 or -1.
	 * For this base class, return -1.
	 */
	public static long inputSizeLimit()
	{
		return -1;
	}
	
	/**
	 * Given a JSONValue_Array of Strings, return a String[] with those strings.
	 * @param jsonArray The JSON array.
	 * @return An array with the Strings in jsonArray. Returns zero-length array if no strings.
	 */
	public static String[] getStringArray(JSONValue_Array jsonArray)
	{
		if (jsonArray == null)
			return new String[0];
		String[] arr = new String[jsonArray.size()];
		for (int i = 0; i < arr.length; i++) {
			arr[i] = jsonArray.getString(i);
		}
		return arr;
	}
	
	/**
	 * Add the Strings in a JSONValue_Array to a String list.
	 * @param list An existing list of strings. Must support add().
	 * @param jsonArray An array with the Strings in jsonArray.
	 * @param prefix If not null, prepend this prefix to each added string.
	 */
	public static void addStringArray(List<String> list, JSONValue_Array jsonArray, String prefix)
	{
		if (jsonArray !=  null) {
			int n = jsonArray.size();
			for (int i = 0; i < n; i++) {
				String v = jsonArray.getString(i);
				if (v != null) {
					list.add(prefix != null ? (prefix + v) : v);
				}
			}
		}
	}
	
	/**
	 * Add a value to the end of the JSONValue_Array named key
	 * in this message's dictionary.
	 * Create the JSONValue_Array if it doesn't exist yet.
	 * @param key Name of the array field.
	 * @param value Value to append to array.
	 */
	public void addToArray(String key, Object value)
	{
		addToArray(m_json, key, value);
	}
	
	/**
	 * Add the objects in an array to the end of the JSONValue_Array named key
	 * in this message's dictionary.
	 * Create the JSONValue_Array if it doesn't exist yet.
	 * @param key Name of the array field.
	 * @param values Values to append to array.
	 */
	public void addToArray(String key, Object[] values)
	{
		addToArray(m_json, key, values);
	}
	
	/**
	 * Add the objects in an collection to the end of the JSONValue_Array named key
	 * in this message's dictionary.
	 * Create the JSONValue_Array if it doesn't exist yet.
	 * @param key Name of the array field.
	 * @param values Values to append to array.
	 */
	public void addToArray(String key, Iterable<Object> values)
	{
		addToArray(m_json, key, values);
	}
	
	/**
	 * Add a String value to the end of the JSONValue_Array named key
	 * in the JSONValue_Object "dictionary."
	 * Create the JSONValue_Array if it doesn't exist yet.
	 * @param key Name of the array field.
	 * @param value Value to append to array.
	 */
	public static void addToArray(JSONValue_Object dictionary, String key, Object value)
	{
		if (value == null)
			return;
		JSONValue_Array array = dictionary.getArray(key, null);
		if (array == null) {
			array = new JSONValue_Array();
			dictionary.put(key, array);
		}
		array.add(new JSONValue_String(value.toString()));
	}
	
	/**
	 * Add a String value to the end of the JSONValue_Array named key2
	 * in the JSONValue_Object named "key1" in the JSONValue_Object "dictionary."
	 * Create the JSONValue_Array and the key1 dictionary if they don't exist yet.
	 * @param key1 Name of a dictionary in "dictionary."
	 * @param key2 Name of the array field in the key1 dictionary.
	 * @param value Value to append to array.
	 */
	public static void addToArray(JSONValue_Object dictionary1, String key1, String key2, Object value)
	{
		if (value == null)
			return;
		JSONValue_Object dictionary2 = dictionary1.getObject(key1, null);
		if (dictionary2 == null) {
			dictionary2 = new JSONValue_Object();
			dictionary1.put(key1, dictionary2);
		}
		JSONValue_Array array = dictionary2.getArray(key2, null);
		if (array == null) {
			array = new JSONValue_Array();
			dictionary2.put(key2, array);
		}
		array.add(new JSONValue_String(value.toString()));
	}
	
	/**
	 * Add the Objects in a collection to the end of the JSONValue_Array named key
	 * in the JSONValue_Object "dictionary."
	 * Create the JSONValue_Array if it doesn't exist yet.
	 * @param key Name of the array field.
	 * @param values Values to append to array.
	 */
	public static void addToArray(JSONValue_Object dictionary, String key, Iterable<Object> values)
	{
		if (values == null)
			return;
		JSONValue_Array array = dictionary.getArray(key, null);
		if (array == null) {
			array = new JSONValue_Array();
			dictionary.put(key, array);
		}
		for (Object value:values) {
			array.add(value.toString());
		}
	}
	
	/**
	 * Add the Objects in an array to the end of the JSONValue_Array named key
	 * in the JSONValue_Object "dictionary."
	 * Create the JSONValue_Array if it doesn't exist yet.
	 * @param key Name of the array field.
	 * @param values Values to append to array.
	 */
	public static void addToArray(JSONValue_Object dictionary, String key, Object[] values)
	{
		if (values == null)
			return;
		JSONValue_Array array = dictionary.getArray(key, null);
		if (array == null) {
			array = new JSONValue_Array();
			dictionary.put(key, array);
		}
		for (Object value:values) {
			array.add(value.toString());
		}
	}
	
	/**
	 * Add the Strings in an array to the end of the JSONValue_Array named key2
	 * in the JSONValue_Object named "key1" in the JSONValue_Object "dictionary."
	 * Create the JSONValue_Array and the key1 dictionary if they don't exist yet.
	 * @param key1 Name of a dictionary in "dictionary."
	 * @param key2 Name of the array field in the key1 dictionary.
	 * @param values Values to append to array.
	 */
	public static void addToArray(JSONValue_Object dictionary1, String key1, String key2, Object[] values)
	{
		if (values == null)
			return;
		JSONValue_Object dictionary2 = dictionary1.getObject(key1, null);
		if (dictionary2 == null) {
			dictionary2 = new JSONValue_Object();
			dictionary1.put(key1, dictionary2);
		}
		JSONValue_Array array = dictionary2.getArray(key2, null);
		if (array == null) {
			array = new JSONValue_Array();
			dictionary2.put(key2, array);
		}
		for (Object value:values) {
			array.add(value.toString());
		}
	}
}
