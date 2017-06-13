package com.wdroome.json;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;
import java.math.BigInteger;
import java.math.BigDecimal;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

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
	 * set its path name.
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
	 * Add a key-value pair at the end of a JSON Object path,
	 * creating objects on the path as needed.
	 * Specifically, ensure that this object has a value for key path[0],
	 * and it is another object. If the object exists, leave it;
	 * if not, replace it with a new JSON Object.
	 * Then ensure that object has a value for key path[1],
	 * and it is another object. Continue up to the last
	 * key in the path. For that key, set the value to "value".
	 * @param path The path to the value.
	 * @param value The value to be added at the end of the path.
	 */
	public void putPath(List<String> path, JSONValue value)
	{
		int n;
		if (value == null || path == null || (n = path.size()) == 0) {
			return;
		}
		JSONValue_Object curElem = this;
		for (int i = 0; i < n-1; i++) {
			String key = path.get(i);
			JSONValue_Object nextElem = curElem.getObject(key, null);
			if (nextElem == null) {
				nextElem = new JSONValue_Object();
				curElem.put(key, nextElem);
			}
			curElem = nextElem;
		}
		curElem.put(path.get(n-1), value);
	}
	
	/**
	 * Call {@link #putPath(List, JSONValue)} on a path/value pair.
	 * @param pathValue A path and a value.
	 */
	public void putPath(JSONPathValuePair pathValue)
	{
		putPath(pathValue.m_path, pathValue.m_value);
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
	 * Add a numeric value to the dictionary.
	 * Create a {@link JSONValue_Number} or a @link JSONValue_BigInt} from the argument.
	 * @param key The key.
	 * @param value The value.
	 * @return The previous value, or null.
	 */
	public JSONValue put(String key, long value)
	{
		if (JSONValue_BigInt.isBigInt(value)) {
			return put(key, new JSONValue_BigInt(value));
		} else {
			return put(key, new JSONValue_Number(value));
		}
	}
	
	/**
	 * Add a numeric value to the dictionary.
	 * Create a {@link JSONValue_BigInt} from the argument.
	 * @param key The key.
	 * @param value The value.
	 * @return The previous value, or null.
	 */
	public JSONValue put(String key, BigInteger value)
	{
		return put(key, new JSONValue_BigInt(value));
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
			Number value = srcEntry.getValue();
			if (value instanceof BigInteger) {
				put(srcEntry.getKey(), new JSONValue_BigInt(value));
			} else {
				put(srcEntry.getKey(), new JSONValue_Number(value.doubleValue()));
			}
		}
		return this;
	}
	
	/**
	 * Remove the value at the end of an object path.
	 * @param path
	 * 		A list of keys representing the object path.
	 *		See {@link #putPath(List, JSONValue)} for the definition
	 *		of the object path.
	 * @return
	 * 		The previous value at the end of the path,
	 *		or null if there was no such value or if
	 *		the path did not exist.
	 */
	public JSONValue removePath(List<String> path)
	{
		int n;
		if (path == null || (n = path.size()) == 0) {
			return null;
		}
		JSONValue_Object curElem = this;
		for (int i = 0; i < n-1; i++) {
			String key = path.get(i);
			JSONValue_Object nextElem = curElem.getObject(key, null);
			if (nextElem == null) {
				return null;
			}
			curElem = nextElem;
		}
		return curElem.remove(path.get(n-1));
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
	 * Return a numeric-valued entry as a double.
	 * If the entry is a BigInt, return it as a double, with loss of precision.
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
		if (value instanceof JSONValue_Number) {
			return ((JSONValue_Number)value).m_value;
		} else if (value instanceof JSONValue_BigInt) {
			return ((JSONValue_BigInt)value).m_value.doubleValue();
		} else {
        	throw new JSONValueTypeException(
        			"JSONObject[" + JSONValue_String.quotedString(key) + "] is not a Number or BigInt.",
        			makePathName(key));
		}
	}

	/**
	 * Return a numeric-valued entry as a double.
	 * @param key The entry's key.
	 * @param def The default value.
	 * @return The numeric value of the key,
	 * 		or def if key doesn't exist or it's not a number.
	 */
	public double getNumber(String key, double def)
	{
		JSONValue value = get(key);
		if (value == null) {
			return def;
		} else if (value instanceof JSONValue_Number) {
			return ((JSONValue_Number)value).m_value;
		} else if (value instanceof JSONValue_BigInt) {
			return ((JSONValue_BigInt)value).m_value.doubleValue();
		} else {
        	return def;
		}
	}
	
	/**
	 * Return an integer-valued entry as a BigInteger.
	 * This works for Number values as well, as long as they
	 * have an integral value.
	 * @param key The entry's key.
	 * @return The BigInteger value of the key.
	 * @throws JSONFieldMissingException If field is missing or null.
	 * @throws JSONValueTypeException If field exists, but it's not an integer.
	 */
	public BigInteger getBigInt(String key) throws JSONFieldMissingException, JSONValueTypeException
	{
		JSONValue value = get(key);
		if (value == null || (value instanceof JSONValue_Null)) {
            throw new JSONFieldMissingException(
            		"JSONObject[" + JSONValue_String.quotedString(key) + "] not found.",
            		makePathName(key));
		}
		if (value instanceof JSONValue_BigInt) {
			return ((JSONValue_BigInt)value).m_value;
		} else if (value instanceof JSONValue_Number) {
			try {
				return ((JSONValue_Number)value).toBigInteger();
			} catch (Exception e) {
				// Not integer: fall thru
			}
		}
        throw new JSONValueTypeException(
        		"JSONObject[" + JSONValue_String.quotedString(key) + "] is not a BigInt or an integer.",
        		makePathName(key));
	}

	/**
	 * Return an integer-valued entry as a BigInteger.
	 * @param key The entry's key.
	 * @param def The default value.
	 * @return The numeric value of the key,
	 * 		or def if key doesn't exist or it's not an integer.
	 */
	public BigInteger getBigInt(String key, BigInteger def)
	{
		JSONValue value = get(key);
		if (value == null) {
			return def;
		} else if (value instanceof JSONValue_BigInt) {
			return ((JSONValue_BigInt)value).m_value;
		} else if (value instanceof JSONValue_Number) {
			try {
				return ((JSONValue_Number)value).toBigInteger();
			} catch (Exception e) {
				// Not integer: fall thru
			}
		}
    	return def;
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
	 * If a value exists for a key, return it; otherwise, return an alternate value.
	 * @param key The key.
	 * @param def The alternate value.
	 * @return The value of key, if it exists, or "def", if it does not.
	 */
	public JSONValue get(String key, JSONValue def)
	{
		JSONValue value = get(key);
		return value != null ? value : def;
	}
	
	/**
	 * Return the value at the end of a path.
	 * @param path A list of keys.
	 * @param def The default value.
	 * @return
	 * 		The value at the end of the object path,
	 * 		or "def" if that path does not exist.
	 *		See {@link #putPath(List, JSONValue)} for the definition
	 *		of the object path.
	 */
	public JSONValue getPath(List<String> path, JSONValue def)
	{
		int n;
		if (path == null || (n = path.size()) == 0) {
			return def;
		}
		JSONValue_Object curElem = this;
		for (int i = 0; i < n-1; i++) {
			String key = path.get(i);
			JSONValue_Object nextElem = curElem.getObject(key, null);
			if (nextElem == null) {
				return def;
			}
			curElem = nextElem;
		}
		return curElem.get(path.get(n-1), def);
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
     * Return a new array with the keys. The array is not sorted,
     * but since each caller gets a new array, the client may sort it.
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
 			} else if (child.startsWith("/")) {
 				return child;
 			} else {
 				return "/" + child;
 			}
 		}
 		if (!parent.startsWith("/")) {
 			parent = "/" + parent;
 		}
 		if (child == null || child.equals("")) {
 			return parent;
 		} else if (child.startsWith("/")) {
 			return parent + child;
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
	
	/**
	 * Apply a JSON Merge-Patch update to this object.
	 * For details, see RFC 7386 and {@link JSONMergePatchScanner}.
	 * @param patch The Merge-Patch update.
	 */
	public void applyMergePatch(JSONValue_Object patch)
	{
		new ApplyMergePatch(patch);
	}
	
	/**
	 * Apply a Merge-Patch update to this object.
	 * Create an instance and give the c'tor the merge-patch object.
	 * The c'tor applies the update and then returns.
	 */
	private class ApplyMergePatch extends JSONMergePatchScanner
	{
		/**
		 * Apply a merge-patch update to the base object.
		 * @param patch The merge-patch update.
		 */
		private ApplyMergePatch(JSONValue_Object patch)
		{
			scan(patch);
		}

		/**
		 * Add or delete a patched value.
		 * @see JSONMergePatchScanner#newValue(List, JSONValue)
		 */
		@Override
		protected void newValue(List<String> path, JSONValue value)
		{
			if (value instanceof JSONValue_Null) {
				removePath(path);
			} else {
				putPath(path, value);
			}
		}
	}
	
	/**
	 * Copy columns from an SQL result row to fields in this dictionary.
	 * Impute the JSON types from the SQL types. The INT, FLOAT and DOUBLE types
	 * become JSON Numbers. BOOLEAN and BIT(1) become JSON Booleans.
	 * Anything else becomes a JSON String (including DECIMAL).
	 * Ignore NULL-valued SQL entries.
	 * @param rs The result of an SQL query. Copy the columns in the current row.
	 * @param colNames The column names to copy. If null, copy all columns.
	 * @param jsonNames The names of the corresponding JSON fields.
	 * 			If null, use the SQL column names.
	 * @throws SQLException If a column name is invalid,
	 * 			or the result set is closed, or some other SQL error occurs.
	 */
	public void putCols(ResultSet rs, String[] colNames, String[] jsonNames)
			throws SQLException
	{
		ResultSetMetaData meta = rs.getMetaData();
		if (colNames == null) {
			int nCol = meta.getColumnCount();
			colNames = new String[nCol];
			for (int iCol = 1; iCol <= nCol; iCol++) {
				colNames[iCol-1] = meta.getColumnName(iCol);
			}
		}
		for (int iFld = 0; iFld < colNames.length; iFld++) {
			String colName = colNames[iFld];
			String jsonName = (jsonNames != null && iFld < jsonNames.length)
							? jsonNames[iFld] : colName;
			int iCol = rs.findColumn(colName);
			switch (meta.getColumnType(iCol)) {
			case Types.INTEGER:
			case Types.SMALLINT:
			case Types.TINYINT:
			{
				if (meta.getPrecision(iCol) == 1) {
					boolean v = rs.getBoolean(iCol);
					if (!rs.wasNull()) {
						put(jsonName, v);
					}
				} else {
					long v = rs.getLong(iCol);
					if (!rs.wasNull()) {
						put(jsonName, v);
					}
				}
				break;
			}
			case Types.BIGINT:
			{
				BigDecimal v = rs.getBigDecimal(iCol);
				if (!rs.wasNull()) {
					put(jsonName, v.toBigInteger());
				}
				break;
			}
			case Types.FLOAT:
			case Types.DOUBLE:
				{
					double v = rs.getDouble(iCol);
					if (!rs.wasNull()) {
						put(jsonName, v);
					}
					break;
				}
			case Types.BOOLEAN:
			{
				boolean v = rs.getBoolean(iCol);
				if (!rs.wasNull()) {
					put(jsonName, v);
				}
			}
			case Types.BIT:
			{
				// 1-bit becomes a JSON boolean.
				// Longer bit fields become JSON strings with 0/1.
				if (meta.getPrecision(iCol) == 1) {
					boolean v = rs.getBoolean(iCol);
					if (!rs.wasNull()) {
						put(jsonName, v);
					}
				} else {
					String v = rs.getString(iCol);
					if (!rs.wasNull()) {
						put(jsonName, v);
					}				
				}
				break;
			}
			default:
				{
					String v = rs.getString(iCol);
					if (!rs.wasNull()) {
						put(jsonName, v);
					}				
				}
			}
		}
	}
	
	/**
	 * Copy columns from an SQL result row to fields in this dictionary.
	 * Impute the JSON types from the SQL types. The INT, FLOAT and DOUBLE types
	 * become JSON Numbers. BOOLEAN and BIT(1) become JSON Booleans.
	 * Anything else becomes a JSON String (including DECIMAL).
	 * Ignore NULL-valued SQL entries.
	 * @param rs The result of an SQL query. Copy the columns in the current row.
	 * @param colNames The column names to copy. If null, copy all columns.
	 * 			Use these as the JSON field names.
	 * @throws SQLException If a column name is invalid,
	 * 			or the result set is closed, or some other SQL error occurs.
	 */
	public void putCols(ResultSet rs, String[] colNames)
			throws SQLException
	{
		putCols(rs, colNames, null);
	}
	
	/**
	 * Copy all columns from an SQL result row to fields in this dictionary.
	 * Impute the JSON types from the SQL types. The INT, FLOAT and DOUBLE types
	 * become JSON Numbers. BOOLEAN and BIT(1) become JSON Booleans.
	 * Anything else becomes a JSON String (including DECIMAL).
	 * Ignore NULL-valued SQL entries.
	 * @param rs The result of an SQL query. Copy the columns in the current row.
	 * @throws SQLException If the result set is closed, or some other SQL error occurs.
	 */
	public void putCols(ResultSet rs)
			throws SQLException
	{
		putCols(rs, null, null);
	}
	
	/**
	 * Create a JSON Object from the rows in an SQL result set.
	 * Each row becomes an item in the dictionary, with the key
	 * being a value from a column in that row (e.g., an ID field).
	 * Ignore NULL-valued SQL entries, or rows with NULL-valued keys.
	 * @param rs The SQL Result Set.
	 * @param keyCol The name of the column with the key values.
	 * @param colNames The names of the SQL columns to copy to JSON.
	 * 		If null, copy all columns.
	 * @param jsonNames The names of the JSON fields. If null, use the SQL column names.
	 * @return A JSON Object with the results of the query.
	 * @throws SQLException If a column name is invalid,
	 * 			or the result set is closed, or some other SQL error occurs.
	 */
	public static JSONValue_Object makeObject(ResultSet rs, String keyCol, String[] colNames, String[] jsonNames)
			throws SQLException
	{
		JSONValue_Object jsonResults = new JSONValue_Object();
		while (rs.next()) {
			String key = rs.getString(keyCol);
			if (!rs.wasNull()) {
				JSONValue_Object jsonRow = new JSONValue_Object();
				jsonRow.putCols(rs, colNames, jsonNames);
				jsonResults.put(key, jsonRow);
			}
		}
		return jsonResults;
	}
}
