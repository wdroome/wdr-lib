package com.wdroome.json.validate;

import java.util.List;
import java.util.ArrayList;

import com.wdroome.json.*;

/**
 * <p>
 * Validate a JSON value against a specification.
 * This is the base class for all validation.
 * For each JSON type, there's a type-specific validator that extends
 * this class and overrides {@link #validate(JSONValue,String)}.
 * </p>
 * <p>
 * There are two models for reporting validation errors.
 * The default is {@link #validate(JSONValue,String)}
 * throws a {@link JSONValidationException} when it finds
 * the first error.
 * The alternative is to append all validation errors to a List,
 * and return false if there are any problems.
 * To use the List approach, call {@link #collectErrors(List)}.
 * </p>
 * @author wdr
 */
public abstract class JSONValidate
{
	private final Class m_requiredClass;
	private final boolean m_nullAllowed;
	
	private List<String> m_errors = null;
	
	/**
	 * Create a new validator and set the required subclass of JSONValue.
	 * @param requiredClass Valid values must be instances of this class.
	 */
	public JSONValidate(Class requiredClass)
	{
		this(requiredClass, false);
	}
	
	/**
	 * Create a new validator and set the required subclass of JSONValue.
	 * @param requiredClass Valid values must be instances of this class.
	 * @param nullAllowed If true, null values are allowed at this spot.
	 * 		The default is false.
	 */
	public JSONValidate(Class requiredClass, boolean nullAllowed)
	{
		m_requiredClass = requiredClass;
		m_nullAllowed = nullAllowed;
	}
	
	/**
	 * Append validation errors to a specified List.
	 * @param errors
	 * 		If not null, {@link #validate(JSONValue,String) validate()}
	 * 		will append errors to this List.
	 * 		The list must support the add(String) method.
	 * 		If null, validate() will throw an exception
	 * 		when it detects the first error.
	 */
	public void collectErrors(List<String> errors)
	{
		m_errors = errors;
	}
	
	/**
	 * Return the validation error list.
	 * @return The validation error list,
	 * 		or null if we are not collecting errors in a list.
	 * @see #collectErrors(List)
	 */
	public List<String> getErrors()
	{
		return m_errors;
	}
	
	/**
	 * Validate a JSONValue. If valid, return true.
	 * If not, either throw an exception,
	 * or else add the error to the List given to {@link #collectErrors(List)}.
	 * @param value The JSON value to validate against this requirement.
	 * @return True iff value passes validation.
	 * @throws JSONValidationException
	 * 		If "value" fails validation and you did not give
	 * 		an error list to {@link #collectErrors(List)} to
	 */
	public final boolean validate(JSONValue value) throws JSONValidationException
	{
		return validate(value, "");
	}
	
	/**
	 * Validate a JSONValue. If valid, return true.
	 * If not, either throw an exception,
	 * or else add the error to the List given to {@link #collectErrors(List)}.
	 * @param value The JSON value to validate against this requirement.
	 * @param path The JSON path for this value.
	 * 		Used in the exception message to identify the invalid element
	 * 		of a possibly large compound JSON object.
	 * 		Use "" if this is a root element.
	 * @return True iff "value" passes validation.
	 * @throws JSONValidationException
	 * 		If "value" fails validation and you did not give
	 * 		an error list to {@link #collectErrors(List)} to
	 */
	public boolean validate(JSONValue value, String path) throws JSONValidationException
	{
		if (m_nullAllowed && value instanceof JSONValue_Null) {
			return true;
		} else if (!m_requiredClass.isInstance(value)) {
			handleValidationError(
						"Wrong type: " + classLeafName(value)
						+ " instead of " + classLeafName(m_requiredClass)
						+ atPath(path));
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Handle a validation error.
	 * If the client has provided an error List,
	 * add the error to the List and return.
	 * Otherwise create and throw an exception.
	 * @param msg A string describing the validation error.
	 * @throws JSONValidationException If the client did not request
	 * 			all validation errors to be collected in a list.
	 */
	protected void handleValidationError(String msg) throws JSONValidationException
	{
		if (m_errors != null) {
			m_errors.add(msg);
		} else {
			throw new JSONValidationException(msg);
		}
	}
	
	/**
	 * Return a string describing the location of the error in the tree.
	 * @param path The path to the element, or null or "" for the root element.
	 * @return A string of the form " at <path>" or " at root".
	 */
	protected String atPath(String path)
	{
		return " at " + ((path == null || path.equals("")) ? "(root)" : path);
	}
	
	/**
	 * Return the pathname of a member of an object dictionary.
	 * @param path The pathname of a JSON object dictionary.
	 * @param objectMember The key of this member.
	 * @return "path.objectMember", or "objectMember" if path is null or "".
	 */
	protected static String newPath(String path, String objectMember)
	{
		if (path == null || path.equals("")) {
			return objectMember;
		} else {
			return path + "." + objectMember;
		}
	}
	
	/**
	 * Return the pathname of an item in an array.
	 * @param path The pathname of the array.
	 * @param arrayIndex The index of the array element.
	 * @return "path[##]", or "[##]" if path is null or "".
	 */
	protected static String newPath(String path, int arrayIndex)
	{
		if (path == null || path.equals("")) {
			return "[" + arrayIndex + "]";
		} else {
			return path + "[" + arrayIndex + "]";
		}
	}	
	
	/**
	 * Return the leaf name of the class of a JSON value.
	 * @param obj A JSONValue, or a Class representing a JSON value.
	 * @return If obj is a Class, the leaf name of the class.
	 * 		Otherwise, the leaf name of the class of obj.
	 */
	public static String classLeafName(Object obj)
	{
		Class<?> clazz = (obj instanceof Class) ? (Class<?>)obj : obj.getClass();
		String clazzName = clazz.getName();
		int lastPeriod = clazzName.lastIndexOf('.');
		if (lastPeriod < 0 || lastPeriod >= clazzName.length() - 1) {
			return clazzName;
		} else {
			return clazzName.substring(lastPeriod+1);
		}
	}
}
