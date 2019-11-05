package com.wdroome.json.validate;

import java.util.Set;
import java.util.HashSet;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.wdroome.json.*;

/**
 * Validate a JSON string.
 * The various constructors specify different requirements on the string value.
 * @author wdr
 */
public class JSONValidate_String extends JSONValidate
{
	/** A simple static JSON String validator for public use. */
	public final static JSONValidate STRING = new JSONValidate_String();
	
	private final Set<String> m_validStrings;
	private final Pattern m_pattern;
	private final String m_errMsgFmt;
	
	/**
	 * Verify that this JSON value is a String.
	 * Any string is acceptable.
	 */
	public JSONValidate_String()
	{
		this((String)null, false, null);
	}
	
	/**
	 * Verify that this JSON value is a String
	 * that matches a regular expression.
	 * @param regex The regular expression.
	 * @throws PatternSyntaxException If regex is not a valid regular expression.
	 */
	public JSONValidate_String(String regex)
	{
		this(regex, false, null);
	}
	
	/**
	 * Verify that this JSON value is one of a set of Strings.
	 * @param validStrings The acceptable strings.
	 */
	public JSONValidate_String(String[] validStrings)
	{
		this(validStrings, false, null);
	}
	
	/**
	 * Verify that this JSON value is one of a set of Strings.
	 * @param validStrings The acceptable strings.
	 */
	public JSONValidate_String(Set<String> validStrings)
	{
		this(validStrings, false);
	}

	/**
	 * Verify that this JSON value is a String,
	 * and optionally matches a regular expression.
	 * @param regex A regular expression, or null.
	 * @param nullAllowed If this JSON value can be null.
	 * @param errMsgFmt The error message to display
	 * 		if a string does not match the regular expression.
	 * 		"%s" will be replaced by the invalid string.
	 * 		If null or blank, use "String %s does not match (regex)".
	 * @throws PatternSyntaxException
	 *		If regex is not null and is not a valid regular expression.
	 */
	public JSONValidate_String(String regex, boolean nullAllowed, String errMsgFmt)
			throws PatternSyntaxException
	{
		super(JSONValue_String.class, nullAllowed);
		m_pattern = (regex != null) ? Pattern.compile(regex) : null;
		m_validStrings = null;
		if (errMsgFmt == null || errMsgFmt.equals("")) {
			errMsgFmt = "String \"%s\" does not match \"" + regex + "\"";
		}
		m_errMsgFmt = errMsgFmt;
	}

	/**
	 * Verify that this JSON value is a String,
	 * and optionally matches one of a set of strings.
	 * @param validStrings A list of valid strings, or null to allow any string.
	 * @param errMsgFmt The error message to display
	 * 		if a string does not match the regular expression.
	 * 		"%s" will be replaced by the invalid string.
	 * 		If null or blank, use "Illegal string %s".
	 * @param nullAllowed If this JSON value can be null.
	 */
	public JSONValidate_String(String[] validStrings, boolean nullAllowed, String errMsgFmt)
	{
		super(JSONValue_String.class, nullAllowed);
		if (validStrings != null) {
			m_validStrings = new HashSet<String>();
			for (String s : validStrings) {
				m_validStrings.add(s);
			}
		} else {
			m_validStrings = null;
		}
		m_pattern = null;
		if (errMsgFmt == null || errMsgFmt.equals("")) {
			errMsgFmt = "Illegal string \"%s\"";
		}
		m_errMsgFmt = errMsgFmt;
	}

	/**
	 * Verify that this JSON value is a String,
	 * and optionally matches one of a set of strings.
	 * @param validStrings A set of valid strings, or null to allow any string.
	 * @param nullAllowed If this JSON value can be null.
	 */
	public JSONValidate_String(Set<String> validStrings, boolean nullAllowed)
	{
		super(JSONValue_String.class, nullAllowed);
		m_validStrings = validStrings;
		m_pattern = null;
		m_errMsgFmt = "Illegal string \"%s\"";
	}
	
	/**
	 * See {@link JSONValidate#validate(JSONValue,String)}.
	 */
	@Override
	public boolean validate(JSONValue value, String path) throws JSONValidationException
	{
		if (!super.validate(value, path)) {
			return false;
		}
		boolean valid = true;
		// super() verifies that next test is always true,
		// but java doesn't know that.
		if (value instanceof JSONValue_String) {
			String svalue = ((JSONValue_String)value).m_value;
			if (m_validStrings != null) {
				if (!m_validStrings.contains(svalue)) {
					handleValidationError(String.format(m_errMsgFmt, svalue) + atPath(path));
					valid = false;
				}
			} else if (m_pattern != null) {
				if (!m_pattern.matcher(svalue).matches()) {
					handleValidationError(String.format(m_errMsgFmt, svalue) + atPath(path));
					valid = false;
				}
			}
		}
		return valid;
	}
}
