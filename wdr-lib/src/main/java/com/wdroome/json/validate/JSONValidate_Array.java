package com.wdroome.json.validate;

import java.util.List;

import com.wdroome.json.*;

/**
 * Verify that a JSON value is an array,
 * with optional tests on the values in the array.
 * @author wdr
 */
public class JSONValidate_Array extends JSONValidate
{
	private final JSONValidate m_elementSpec;
	private final int m_minLength;
	private final int m_maxLength;
	
	/**
	 * Verify that the JSON value is an array,
	 * and optionally specify the type of elements in the array.
	 * @param elementSpec
	 * 		If not null, all elements must satisfy this validator.
	 * 		If null, the array can have elements of any type.
	 */
	public JSONValidate_Array(JSONValidate elementSpec)
	{
		this(elementSpec, 0, Integer.MAX_VALUE, false);
	}
	
	/**
	 * Verify that the JSON value is an array,
	 * and optionally specify the type of elements in the array
	 * and the minimum length.
	 * @param elementSpec
	 * 		If not null, all elements must satisfy this validator.
	 * 		If null, the array can have elements of any type.
	 * @param minLength The minimum number of elements in the array.
	 */
	public JSONValidate_Array(JSONValidate elementSpec, int minLength)
	{
		this(elementSpec, minLength, Integer.MAX_VALUE, false);
	}
	
	/**
	 * Verify that the JSON value is an array,
	 * and optionally specify the type of elements in the array
	 * and the minimum and maximum lengths.
	 * @param elementSpec
	 * 		If not null, all elements must satisfy this validator.
	 * 		If null, the array can have elements of any type.
	 * @param minLength The minimum number of elements in the array.
	 * @param maxLength The maximum number of elements in the array.
	 */
	public JSONValidate_Array(JSONValidate elementSpec, int minLength, int maxLength)
	{
		this(elementSpec, minLength, maxLength, false);
	}
	
	/**
	 * Verify that the JSON value is an array,
	 * and optionally specify the type of elements in the array
	 * and the minimum and maximum lengths.
	 * @param elementSpec
	 * 		If not null, all elements must satisfy this validator.
	 * 		If null, the array can have elements of any type.
	 * @param minLength The minimum number of elements in the array.
	 * @param maxLength The maximum number of elements in the array.
	 * @param nullAllowed If this JSON value can be null.
	 */
	public JSONValidate_Array(JSONValidate elementSpec, int minLength, int maxLength, boolean nullAllowed)
	{
		super(JSONValue_Array.class, nullAllowed);
		m_elementSpec = elementSpec;
		m_minLength = minLength;
		m_maxLength = maxLength;
	}
	
	/**
	 * Validate a JSON value.
	 * See {@link JSONValidate#validate(JSONValue,String)}.
	 */
	@Override
	public boolean validate(JSONValue value, String path) throws JSONValidationException
	{
		if (!super.validate(value, path)) {
			return false;
		}
		boolean valid = true;
		// super() verifies that next instanceof test is always true,
		// but java doesn't know that.
		if (m_elementSpec != null && value instanceof JSONValue_Array) {
			JSONValue_Array array = (JSONValue_Array)value;
			int n = array.size();
			if (n < m_minLength) {
				handleValidationError("Array len " + n + " < " + m_minLength + atPath(path));
				valid = false;
			} else if (n > m_maxLength) {
				handleValidationError("Array len " + n + " > " + m_maxLength + atPath(path));				
				valid = false;
			}
			for (int i = 0; i < n; i++) {
				if (!m_elementSpec.validate(array.get(i), newPath(path, i))) {
					valid = false;
				}
			}
		}
		return valid;
	}
	
	/**
	 * Collect errors in the specified list.
	 * This method invokes collectErrors()
	 * on the JSONValidate object passed to the constructor.
	 * See {@link JSONValidate#collectErrors(List)}.
	 */
	@Override
	public void collectErrors(List<String> errors)
	{
		super.collectErrors(errors);
		if (m_elementSpec != null) {
			m_elementSpec.collectErrors(errors);
		}
	}
}
