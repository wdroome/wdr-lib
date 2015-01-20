package com.wdroome.json;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * Utility methods to create a JSONObject from a regular java object or a scalar value.
 * @author wdr
 */
public class JSONUtil
{
	/**
	 * Create the appropriate JSONObject from java object.
	 * Java Arrays and Collections become JSON arrays,
	 * Java Maps become JSON objects,
	 * Java Booleans become JSON booleans,
	 * Java Numbers become JSON numbers,
	 * null becomes a JSON null,
	 * and anything else becomes a JSON string with the object's toString() value.
	 * @param obj The object to be converted to a JSONValue.
	 * @return The JSONValue of "obj".
	 */
	public static JSONValue toValue(Object obj)
	{
		if (obj == null) {
			return JSONValue_Null.NULL;
		} else if (obj instanceof JSONValue) {
			return (JSONValue)obj;
		} else if (obj instanceof String) {
			return new JSONValue_String((String)obj);
		} else if (obj instanceof Number) {
			return new JSONValue_Number((Number)obj);
		} else if (obj instanceof Boolean) {
			return ((Boolean)obj) ? JSONValue_Boolean.TRUE : JSONValue_Boolean.FALSE;
		} else if (obj instanceof Object[]) {
			return new JSONValue_Array((Object[])obj);
		} else if (obj instanceof Collection<?>) {
				return new JSONValue_Array((Collection<?>)obj);
		} else if (obj instanceof Map<?,?>) {
			return new JSONValue_Object((Map<?,?>)obj);
		} else {
			return new JSONValue_String(obj.toString());
		}
	}
	
	/**
	 * Create a JSONString from a String.
	 * @param s The string value.
	 * @return The JSONValue of "obj".
	 */
	public static JSONValue toValue(String s)
	{
		return new JSONValue_String(s);
	}
	
	/**
	 * Create a JSONNumber from a double or other scalar numeric type.
	 * @param d The numeric value.
	 * @return The JSONValue of "obj".
	 */
	public static JSONValue toValue(double d)
	{
		return new JSONValue_Number(d);
	}
	
	/**
	 * Create a JSONBoolean from a scalar boolean.
	 * @param b The boolean value.
	 * @return The JSONValue of "obj".
	 */
	public static JSONValue toValue(boolean b)
	{
		return b ? JSONValue_Boolean.TRUE : JSONValue_Boolean.FALSE;
	}
	
	/**
	 * Return a nicely formated JSON encoding of a JSON value,
	 * and list all object dictionary entries in key order.
	 * @param value The JSON value.
	 * @return The JSON encoding of "value".
	 */
	public static String toJSONString(JSONValue value)
	{
		return toJSONString(value, true, true);
	}
	
	/**
	 * Return a JSON encoding of a JSON value.
	 * @param value The JSON value.
	 * @param sortAndIndent
	 * 		If true, list all object dictionary entries in key order,
	 * 		and indent the JSON for readability.
	 * @return The JSON encoding of "value".
	 */
	public static String toJSONString(JSONValue value, boolean sortAndIndent)
	{
		return toJSONString(value, sortAndIndent, sortAndIndent);
	}
	
	/**
	 * Return the JSON encoding of a JSON value.
	 * @param value The JSON value.
	 * @param indented If true, indent the JSON for readability.
	 * @param sorted If true, list object dictionary entries in key order.
	 * @return The JSON encoding of "value".
	 */
	public static String toJSONString(JSONValue value, boolean sorted, boolean indented)
	{
		StringBuilder buff = new StringBuilder();
		JSONWriter writer = new JSONWriter(buff);
		writer.setSorted(sorted);
		writer.setIndented(indented);
		try {
			value.writeJSON(writer);
		} catch (IOException e) {
			// Shouldn't happen with a StringBuilder target.
		}
		return buff.toString();
	}
	
	/**
	 * Return the JSON encoding of a JSON value.
	 * @param value The JSON value.
	 * @param incrIndent The incremental indent string.
	 * @param lineIndent The leading indent for each line.
	 * @return The JSON encoding of "value".
	 */
	public static String toJSONString(JSONValue value, String incrIndent, String lineIndent)
	{
		StringBuilder buff = new StringBuilder();
		JSONWriter writer = new JSONWriter(buff);
		writer.setSorted(true);
		writer.setIndents(incrIndent, lineIndent);
		try {
			value.writeJSON(writer);
		} catch (IOException e) {
			// Shouldn't happen with a StringBuilder target.
		}
		return buff.toString();
	}
}
