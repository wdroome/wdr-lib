package com.wdroome.json;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;

/**
 * A JSON Object, or a dictionary from Strings to JSON values.
 * This is simply a java HashMap that implements the JSONValue marker interface.
 * @author wdr
 */
public class JSONValue_Object extends HashMap<String,JSONValue> implements JSONValue
{
	private static final long serialVersionUID = 7951974441619152479L;
	
	/**
     * The pathname for this object, or null.
     * @see #usePathNames(boolean)
     */
	private String m_pathName = null;
	
	/**
	 * Create an empty JSON object.
	 */
	public JSONValue_Object() {}
	
	/**
	 * Create an empty JSON object with a specified initial capacity.
	 * @param initialSize The initial size of the map.
	 */
	public JSONValue_Object(int initialSize)
	{
		super(initialSize);
	}
	
	/**
	 * Create a JSON Object with the key-value pairs in another map.
	 * Use {@link JSONUtil#toValue(Object)} to convert
	 * the map's Java values to JSONValues,
	 * and use toString() to convert the map's keys to Strings. 
	 * @param src A map whose mappings are to be placed in this map.
	 */
	public JSONValue_Object(Map<? extends Object, ? extends Object> src)
	{
		super(src != null ? src.size() : 0);
		putObjects(src);
	}
	
	/**
	 * Add a key-value pair to the dictionary.
	 * Use the base class put() method,
	 * but if "value" is a JSON Object and path names are enabled,
	 * set it's path name.
	 * @param key The key.
	 * @param value The value.
	 * @return The previous value associated with "key", or null if none.
	 */
	@Override
	public JSONValue put(String key, JSONValue value)
	{
		JSONValue prevValue = super.put(key, value);
		if (m_pathName != null && value instanceof JSONValue_Object) {
			((JSONValue_Object)value).setPathName(m_pathName + "/" + key);
		}
		return prevValue;
	}
	
	/**
	 * Add a string value to the dictionary.
	 * Create a {@link JSONValue_String} from the argument.
	 * @param key The key.
	 * @param value The value.
	 * @return The previous value, or null.
	 */
	public JSONValue put(String key, String value)
	{
		return put(key, new JSONValue_String(value));
	}
	
	/**
	 * Add a numeric value to the dictionary.
	 * Create a {@link JSONValue_Number} from the argument.
	 * @param key The key.
	 * @param value The value.
	 * @return The previous value, or null.
	 */
	public JSONValue put(String key, double value)
	{
		return put(key, new JSONValue_Number(value));
	}
	
	/**
	 * Add a boolean value to the dictionary.
	 * Create a {@link JSONValue_Boolean} from the argument.
	 * @param key The key.
	 * @param value The value.
	 * @return The previous value, or null.
	 */
	public JSONValue put(String key, boolean value)
	{
		return put(key, new JSONValue_Boolean(value));
	}
	
	/**
	 * Add mappings to this JSON object.
	 * Use {@link JSONUtil#toValue(Object)} to convert
	 * the map's Java values to JSONValues,
	 * and use toString() to convert the map's keys to Strings. 
	 * @param src A map whose mappings are to be placed in this map.
	 * @return This object.
	 */
	public JSONValue_Object putObjects(Map<? extends Object, ? extends Object> src)
	{
		if (src != null) {
			for (Map.Entry<? extends Object, ? extends Object> srcEntry : src.entrySet()) {
				put(srcEntry.getKey().toString(), JSONUtil.toValue(srcEntry.getValue()));
			}
		}
		return this;
	}
	
	/**
	 * Copy String-valued mappings into this JSON object.
	 * Add each object's string value, as determined by toString().
	 * Convert the keys to Strings with toString().
	 * @param src An Object-Object map.
	 * @return This object.
	 */
	public JSONValue_Object putStrings(Map<? extends Object, ? extends Object> src)
	{
		for (Map.Entry<? extends Object,? extends Object> srcEntry: src.entrySet()) {
			put(srcEntry.getKey().toString(), new JSONValue_String(srcEntry.getValue().toString()));
		}
		return this;
	}
	
	/**
	 * Copy Number-valued mappings into this JSON object.
	 * @param src A String-Number map.
	 * @return This object.
	 */
	public JSONValue_Object putNumbers(Map<String, ? extends Number> src)
	{
		for (Map.Entry<String,? extends Number> srcEntry: src.entrySet()) {
			put(srcEntry.getKey(), new JSONValue_Number(srcEntry.getValue().doubleValue()));
		}
		return this;
	}
	
	/**
	 * Return a String-valued entry.
	 * @param key The entry's key.
	 * @return The string value of the key.
	 * @throws JSONFieldMissingException If field is missing.
	 * @throws JSONValueTypeException If field exists, but it's not a string.
	 */
	public String getString(String key) throws JSONFieldMissingException, JSONValueTypeException
	{
		JSONValue value = get(key);
		if (value == null) {
            throw new JSONFieldMissingException(
            		"JSONObject[" + JSONValue_String.quotedString(key) + "] not found.",
            		makePathName(key));

		}
		if (!(value instanceof JSONValue_String)) {
        	throw new JSONValueTypeException(
        			"JSONObject[" + JSONValue_String.quotedString(key) + "] is not a String.",
        			makePathName(key));
		}
		return ((JSONValue_String)value).m_value;
	}

	/**
	 * Return a String-valued entry.
	 * @param key The entry's key.
	 * @param def The default value.
	 * @return The string value of the key,
	 * 		or def if key doesn't exist or it's not a string.
	 */
	public String getString(String key, String def)
	{
		JSONValue value = get(key);
		return (value != null && value instanceof JSONValue_String)
						? ((JSONValue_String)value).m_value : def;
	}

	/**
	 * Return a numeric-valued entry.
	 * @param key The entry's key.
	 * @return The number value of the key.
	 * @throws JSONFieldMissingException If field is missing or null.
	 * @throws JSONValueTypeException If field exists, but it's not a number.
	 */
	public double getNumber(String key) throws JSONFieldMissingException, JSONValueTypeException
	{
		JSONValue value = get(key);
		if (value == null || (value instanceof JSONValue_Null)) {
            throw new JSONFieldMissingException(
            		"JSONObject[" + JSONValue_String.quotedString(key) + "] not found.",
            		makePathName(key));
		}
		if (!(value instanceof JSONValue_Number)) {
        	throw new JSONValueTypeException(
        			"JSONObject[" + JSONValue_String.quotedString(key) + "] is not a Number.",
        			makePathName(key));
		}
		return ((JSONValue_Number)value).m_value;
	}

	/**
	 * Return a numeric-valued entry.
	 * @param key The entry's key.
	 * @param def The default value.
	 * @return The numeric value of the key,
	 * 		or def if key doesn't exist or it's not a number.
	 */
	public double getNumber(String key, double def)
	{
		JSONValue value = get(key);
		return (value != null && value instanceof JSONValue_Number)
						? ((JSONValue_Number)value).m_value : def;
	}

	/**
	 * Return a boolean-valued entry.
	 * @param key The entry's key.
	 * @return The boolean value of the key.
	 * @throws JSONFieldMissingException If field is missing.
	 * @throws JSONValueTypeException If field exists, but it's not a string.
	 */
	public boolean getBoolean(String key) throws JSONFieldMissingException, JSONValueTypeException
	{
		JSONValue value = get(key);
		if (value == null) {
            throw new JSONFieldMissingException(
            		"JSONObject[" + JSONValue_String.quotedString(key) + "] not found.",
            		makePathName(key));

		}
		if (!(value instanceof JSONValue_Boolean)) {
        	throw new JSONValueTypeException(
        			"JSONObject[" + JSONValue_String.quotedString(key) + "] is not a Boolean.",
        			makePathName(key));
		}
		return ((JSONValue_Boolean)value).m_value;
	}

	/**
	 * Return a boolean-valued entry.
	 * @param key The entry's key.
	 * @param def The default value.
	 * @return The boolean value of the key,
	 * 		or def if key doesn't exist or it's not a boolean.
	 */
	public boolean getBoolean(String key, boolean def)
	{
		JSONValue value = get(key);
		return (value != null && value instanceof JSONValue_Boolean)
						? ((JSONValue_Boolean)value).m_value : def;
	}

	/**
	 * Return an array-valued entry.
	 * @param key The entry's key.
	 * @return The array value of the key.
	 * @throws JSONFieldMissingException If field is missing.
	 * @throws JSONValueTypeException If field exists, but it's not a string.
	 */
	public JSONValue_Array getArray(String key) throws JSONFieldMissingException, JSONValueTypeException
	{
		JSONValue value = get(key);
		if (value == null) {
            throw new JSONFieldMissingException(
            		"JSONObject[" + JSONValue_String.quotedString(key) + "] not found.",
            		makePathName(key));

		}
		if (!(value instanceof JSONValue_Array)) {
        	throw new JSONValueTypeException(
        			"JSONObject[" + JSONValue_String.quotedString(key) + "] is not an Array.",
        			makePathName(key));
		}
		return (JSONValue_Array)value;
	}

	/**
	 * Return an array-valued entry.
	 * @param key The entry's key.
	 * @param def The default value.
	 * @return The array value of the key, or def if it doesn't exist or it's not an array.
	 */
	public JSONValue_Array getArray(String key, JSONValue_Array def)
	{
		JSONValue value = get(key);
		return (value != null && value instanceof JSONValue_Array) ? (JSONValue_Array)value : def;
	}

	/**
	 * Return an object-valued entry.
	 * @param key The entry's key.
	 * @return The object value of the key.
	 * @throws JSONFieldMissingException If field is missing.
	 * @throws JSONValueTypeException If field exists, but it's not a string.
	 */
	public JSONValue_Object getObject(String key) throws JSONFieldMissingException, JSONValueTypeException
	{
		JSONValue value = get(key);
		if (value == null) {
            throw new JSONFieldMissingException(
            		"JSONObject[" + JSONValue_String.quotedString(key) + "] not found.",
            		makePathName(key));
		}
		if (!(value instanceof JSONValue_Object)) {
        	throw new JSONValueTypeException(
        			"JSONObject[" + JSONValue_String.quotedString(key) + "] is not an Object.",
        			makePathName(key));
		}
		return (JSONValue_Object)value;
	}

	/**
	 * Return an object-valued entry.
	 * @param key The entry's key.
	 * @param def The default value.
	 * @return The object value of the key, or def if it doesn't exist or it's not an object.
	 */
	public JSONValue_Object getObject(String key, JSONValue_Object def)
	{
		JSONValue value = get(key);
		return (value != null && value instanceof JSONValue_Object) ? (JSONValue_Object)value : def;
	}

	/**
	 * Return true iff the object defines a value of type "clazz" for "key".
	 * @param key The value's key.
	 * @param clazz The desired class. Ignored if null.
	 * @return True iff the object has a value for "key",
	 * 		and the value is an instance of class "clazz" (if not null).
	 */
	public boolean has(String key, Class clazz)
	{
		JSONValue value = get(key);
		if (value == null) {
			return false;
		} else if (clazz != null && !clazz.isInstance(value)) {
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Return true iff the object defines a value for "key".
	 * @param key The value's key.
	 * @return True iff the object has a value for "key".
	 */
	public boolean has(String key)
	{
		return containsKey(key);
	}
	
    /**
     * Get an enumeration of the keys of the JSONObject.
     *
     * @return An iterator of the keys.
     */
    public Iterator<String> keys()
    {
        return keySet().iterator();
    }
 
    /**
     * Return the keys as an array. The array is not sorted.
     * @return An array of key names. Returns a zero-length array if there are no keys.
     */
    public String[] keyArray()
    {
    	Set<String> keySet = keySet();
    	if (keySet == null)
    		return new String[0];
    	String[] keys = keySet.toArray(new String[keySet.size()]);
    	return keys;
    }

	/**
	 * @see JSONValue#writeJSON(JSONWriter)
	 */
	@Override
	public void writeJSON(JSONWriter writer) throws IOException
	{
		boolean indent = false;
		if (writer.isIndented()) {
			if (size() > 5) {
				indent = true;
			} else {
				for (JSONValue value: values()) {
					if (!value.isSimple()) {
						indent = true;
						break;
					}
				}
			}
		}
		if (indent) {
			writer.write('{');
			writer.writeNewline();
			writer.incrIndent(1);
		} else {
			writer.write('{');
		}
		Map<String,JSONValue> map = writer.isSorted()
					? new TreeMap<String,JSONValue>(this) : this;
		int n = 0;
		for (Map.Entry<String,JSONValue> entry: map.entrySet()) {
			if (n > 0) {
				writer.write(',');
			}
			if (indent) {
				writer.writeNewline();
			}
			writer.write(JSONValue_String.quotedString(entry.getKey()));
			writer.write(':');
			entry.getValue().writeJSON(writer);
			n++;
		}
		if (indent) {
			writer.writeNewline();
			writer.incrIndent(-1);
			writer.write('}');
		} else {
			writer.write('}');
		}
	}

	/**
	 * @see JSONValue#isSimple()
	 */
	@Override
	public boolean isSimple()
	{
		return false;
	}
	
	/**
	 * @see JSONValue#jsonType()
	 */
	@Override
	public String jsonType()
	{
		return "Object";
	}
	
	/**
	 * If this object is maintaining path names,
	 * return the JSON path name for "child" in this object.
	 * Otherwise, return null.
 	 * @param child The leaf name of the child object.
 	 * @return The fully qualified name of "child", or null.
	 */
	public String makePathName(String child)
	{
		if (m_pathName == null) {
			return null;
		} else {
			return makePathName(m_pathName, child);
		}
	}
	
 	/**
 	 * Return the JSON path name for "child" in "parent".
 	 * Note this assumes parent is a JSONObject; it doesn't handle array references.
 	 * See RFC 6901.
 	 * @param parent The full name of the parent object (may be null).
 	 * @param child The leaf name of the child object.
 	 * @return The fully qualified name of "child" in "parent".
 	 */
 	public static String makePathName(String parent, String child)
 	{
 		if (parent == null || parent.equals("")) {
 			if (child == null || child.equals("")) {
 				return "";
 			} else {
 				return "/" + child;
 			}
 		}
 		if (!parent.startsWith("/")) {
 			parent = "/" + parent;
 		}
 		if (child == null || child.equals("")) {
 			return parent;
 		} else {
 			return parent + "/" + child;
 		}
 	}
 	
 	/**
 	 * Return true iff this object maintains pathnames.
 	 * @return True iff this object maintains pathnames.
 	 * @see #usePathNames(boolean)
 	 * @see #getPathName() 
 	 * @see #setPathName(String)
 	 */
 	public boolean usePathNames()
 	{
 		return m_pathName != null;
 	}
 	
 	/**
 	 * Specify whether or not we should maintain pathnames for
 	 * this object and its children.
 	 * @param usePathNames True iff we need to maintain pathnames
 	 * 			for this object and its children.
 	 * @see #usePathNames()
 	 */
	public void usePathNames(boolean usePathNames)
	{
		if (!usePathNames) {
			m_pathName = null;
		} else if (m_pathName == null) {
			m_pathName = "";
		}
	}

	/**
	 * Return this object's path name.
	 * @return This object's path name, or null if unknown.
	 */
	public String getPathName()
	{
		return m_pathName;
	}
	
	/**
	 * Set this object's path name.
	 * @param pathName The new pathname, or null.
	 * @return This object.
	 */
	private JSONValue_Object setPathName(String pathName)
	{
		m_pathName = pathName;
		return this;
	}
	
	/**
	 * Return a compact JSON encoding of this object.
	 */
	@Override
	public String toString()
	{
		return JSONUtil.toJSONString(this, false);
	}
}
