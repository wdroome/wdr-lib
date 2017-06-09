package com.wdroome.json;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Collection;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A JSON array.
 * This is simply a java ArrayList that implements the JSONValue marker interface.
 * @author wdr
 */
public class JSONValue_Array extends ArrayList<JSONValue> implements JSONValue
{
	private static final long serialVersionUID = 3869583752810778476L;

	/**
	 * Create an empty JSON array.
	 */
	public JSONValue_Array() {}
	
	/**
	 * Create an empty JSON array with a specified initial capacity.
	 * @param initialSize The initial size of the array.
	 */
	public JSONValue_Array(int initialSize)
	{
		super(initialSize);
	}
	
	/**
	 * Create a JSON Object with the values in another list.
	 * We use {@link JSONUtil#toValue(Object)} to convert
	 * the Java Objects to JSON values.
	 * @param src A list of values to be placed in this array.
	 */
	public JSONValue_Array(Collection<? extends Object> src)
	{
		super(src != null ? src.size() : 0);
		addObjects(src);
	}
	
	/**
	 * Create a JSONArray from an array of JSONValues.
	 * @param src The source for the new array.
	 */
	public JSONValue_Array(JSONValue[] src)
	{
		super(src != null ? src.length : 0);
		addJSONValues(src);
	}
	
	/**
	 * Create a JSONArray from an array of Java objects.
	 * We use {@link JSONUtil#toValue(Object)} to convert
	 * the Java Objects to JSON values.
	 * @param src The source for the new array.
	 */
	public JSONValue_Array(Object[] src)
	{
		super(src != null ? src.length : 0);
		if (src != null) {
			for (Object obj: src) {
				add(JSONUtil.toValue(obj));
			}
		}
	}
	
	/**
	 * Add a string value to the array.
	 * Create a {@link JSONValue_String} from the argument.
	 * @param value The value.
	 */
	public void add(String value)
	{
		add(new JSONValue_String(value));
	}
	
	/**
	 * Add a numeric value to the array.
	 * Create a {@link JSONValue_Number} from the argument.
	 * @param value The value.
	 */
	public void add(double value)
	{
		add(new JSONValue_Number(value));
	}
	
	/**
	 * Add a numeric value to the array.
	 * Create a {@link JSONValue_BigInt} from the argument.
	 * @param value The value.
	 */
	public void add(BigInteger value)
	{
		add(new JSONValue_BigInt(value));
	}
	
	/**
	 * Add a boolean value to the array.
	 * Create a {@link JSONValue_Boolean} from the argument.
	 * @param value The value.
	 */
	public void add(boolean value)
	{
		add(new JSONValue_Boolean(value));
	}

	/**
	 * Add an array of Objects to this array.
	 * We use {@link JSONUtil#toValue(Object)} to convert
	 * the Java objects to JSONValues. 
	 * @param src The values to be added.
	 * @return This object.
	 */
	public JSONValue_Array addObjects(Object[] src)
	{
		if (src != null) {
			for (Object value:src) {
				add(JSONUtil.toValue(value));
			}
		}
		return this;
	}

	/**
	 * Add a Collection of Objects to this array.
	 * We use {@link JSONUtil#toValue(Object)} to convert
	 * the Java objects to JSONValues. 
	 * @param src The values to be added.
	 * @return This object.
	 */
	public JSONValue_Array addObjects(Collection<? extends Object> src)
	{
		if (src != null) {
			for (Object value:src) {
				add(JSONUtil.toValue(value));
			}
		}
		return this;
	}

	/**
	 * Add an array of JSONValue values to this array.
	 * @param src The values to be added.
	 * @return This object.
	 */
	public JSONValue_Array addJSONValues(JSONValue[] src)
	{
		if (src != null) {
			for (JSONValue value:src) {
				add(value);
			}
		}
		return this;
	}

	/**
	 * Add a sequence of String values to this array.
	 * @param src The values to be added.
	 * 		We add each object's String value, as returned by toString().
	 * @return This object.
	 */
	public JSONValue_Array addStrings(Collection<? extends Object> src)
	{
		if (src != null) {
			for (Object value:src) {
				add(new JSONValue_String(value.toString()));
			}
		}
		return this;
	}
	
	/**
	 * Add an array of Strings to this array.
	 * @param src The Strings to be added.
	 * @return This object.
	 */
	public JSONValue_Array addStrings(String[] src)
	{
		if (src != null) {
			for (String value : src) {
				add(new JSONValue_String(value));
			}
		}
		return this;
	}
	
	/**
	 * Add a list of Numbers to this array.
	 * @param src The Numbers to be added.
	 * 		BigIntegers are added as JSON BigInt values.
	 * 		Other values are added as JSON Number values (e.g., doubles).
	 * @return This object.
	 */
	public JSONValue_Array addNumbers(Collection<? extends Number> src)
	{
		if (src != null) {
			for (Number value : src) {
				if (value instanceof BigInteger) {
					add(new JSONValue_BigInt((BigInteger)value));
				} else {
					add(new JSONValue_Number(value.doubleValue()));
				}
			}
		}
		return this;
	}
	
	/**
	 * Add an array of doubles to this array.
	 * @param src The doubles to be added.
	 * @return This object.
	 */
	public JSONValue_Array addNumbers(double[] src)
	{
		if (src != null) {
			for (double value : src) {
				add(new JSONValue_Number(value));
			}
		}
		return this;
	}

	/**
	 * Add an array of ints to this array.
	 * @param src The ints to be added.
	 * @return This object.
	 */
	public JSONValue_Array addNumbers(int[] src)
	{
		if (src != null) {
			for (double value : src) {
				add(new JSONValue_Number(value));
			}
		}
		return this;
	}
	
	/**
	 * Return a String-valued element.
	 * @param index The element's index.
	 * @return The string value of the element.
	 * 		Return null if it's not a JSON string.
	 * @throws IndexOutOfBoundsException If index is invalid.
	 */
	public String getString(int index)
	{
		JSONValue value = get(index);
		return (value instanceof JSONValue_String) ? ((JSONValue_String)value).m_value : null;
	}

	/**
	 * Return a numeric-valued element as a double.
	 * @param index The element's index.
	 * @param def The default value.
	 * @return The numeric value of the element.
	 * 		Return def if it's not a JSON number.
	 * @throws IndexOutOfBoundsException If index is invalid.
	 */
	public double getNumber(int index, double def)
	{
		JSONValue value = get(index);
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
	 * Return an integer-valued element as a BigInteger.
	 * @param index The element's index.
	 * @param def The default value.
	 * @return The numeric value of the element.
	 * 		Return def if the value is not a JSON number,
	 * 		or if it's not an integer,
	 * @throws IndexOutOfBoundsException If index is invalid.
	 */
	public BigInteger getBigInt(int index, BigInteger def)
	{
		JSONValue value = get(index);
		if (value == null) {
			return def;
		} else if (value instanceof JSONValue_BigInt) {
			return ((JSONValue_BigInt)value).m_value;
		} else if (value instanceof JSONValue_Number) {
			try {
				return ((JSONValue_Number) value).toBigInteger();
			} catch (Exception e) {
				return def;
			}
		} else {
			return def;
		}
	}

	/**
	 * Return a boolean-valued element.
	 * @param index The element's index.
	 * @param def The default value.
	 * @return The boolean value of the element.
	 * 		Return def if it's not a JSON boolean.
	 * @throws IndexOutOfBoundsException If index is invalid.
	 */
	public boolean getBoolean(int index, boolean def)
	{
		JSONValue value = get(index);
		return (value instanceof JSONValue_Boolean) ? ((JSONValue_Boolean)value).m_value : def;
	}

	/**
	 * Return an array-valued element.
	 * @param index The element's index.
	 * @return The array value of the element.
	 * 		Return null if it's not a JSON array.
	 * @throws IndexOutOfBoundsException If index is invalid.
	 */
	public JSONValue_Array getArray(int index)
	{
		JSONValue value = get(index);
		return (value instanceof JSONValue_Array) ? (JSONValue_Array)value : null;
	}

	/**
	 * Return an object-valued element.
	 * @param index The element's index.
	 * @return The object value of the element.
	 * 		Return null if it's not a JSON object.
	 * @throws IndexOutOfBoundsException If index is invalid.
	 */
	public JSONValue_Object getObject(int index)
	{
		JSONValue value = get(index);
		return (value instanceof JSONValue_Object) ? (JSONValue_Object)value : null;
	}
	
	/**
	 * Return true iff the element's value is of type "clazz".
	 * @param index The element's index.
	 * @param clazz The desired class. Ignored if null.
	 * @return True iff index is valid, and the element's value
	 * 		is an instance of class "clazz" (if not null).
	 */
	public boolean exists(int index, Class clazz)
	{
		JSONValue value;
		try {
			value = get(index);
		} catch (Exception e) {
			return false;
		}
		if (value == null) {
			return false;
		} else if (clazz != null && !clazz.isInstance(value)) {
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * @see JSONValue#writeJSON(JSONWriter)
	 */
	@Override
	public void writeJSON(JSONWriter writer) throws IOException
	{
		int maxPerLine = Integer.MAX_VALUE;
		boolean indent = false;
		if (writer.isIndented()) {
			maxPerLine = 5;
			for (JSONValue value: this) {
				if (!value.isSimple()) {
					maxPerLine = 1;
					indent = true;
					break;
				}
			}
			if (!indent && size() > maxPerLine) {
				indent = true;
			}
		}
		if (indent) {
			writer.write('[');
			writer.writeNewline();
			writer.incrIndent(1);
		} else {
			writer.write('[');
		}
		int n = 0;
		for (JSONValue value: this) {
			if (n > 0) {
				writer.write(',');
			}
			if (n >= maxPerLine) {
				writer.writeNewline();
				n = 0;
			}
			value.writeJSON(writer);
			n++;
		}
		if (indent) {
			writer.writeNewline();
			writer.incrIndent(-1);
			writer.write(']');
		} else {
			writer.write(']');			
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
		return "Array";
	}

	/**
	 * Return a compact JSON encoding of this array.
	 */
	@Override
	public String toString()
	{
		return JSONUtil.toJSONString(this, false);
	}
	
	/**
	 * Create a JSON Array with the rows in an SQL result set.
	 * Ignore NULL-valued SQL entries.
	 * @param rs The SQL Result Set.
	 * @param colNames The names of the SQL columns to copy to JSON.
	 * 		If null, copy all columns.
	 * @param jsonNames The names of the JSON fields. If null, use the SQL column names.
	 * @return A JSON Array with the results of the query.
	 * @throws SQLException If a column name is invalid,
	 * 			or the result set is closed, or some other SQL error occurs.
	 */
	public static JSONValue_Array makeArray(ResultSet rs, String[] colNames, String[] jsonNames)
			throws SQLException
	{
		JSONValue_Array jsonResults = new JSONValue_Array();
		while (rs.next()) {
			JSONValue_Object jsonRow = new JSONValue_Object();
			jsonRow.putCols(rs, colNames, jsonNames);
			jsonResults.add(jsonRow);
		}
		return jsonResults;
	}
}
