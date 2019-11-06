package com.wdroome.json.validate;

import com.wdroome.json.*;

/**
 * Validate a JSON number.
 * The various constructors specify different types of validation.
 * @author wdr
 */
public class JSONValidate_Number extends JSONValidate
{
	/** A simple static JSON Number validator for public use. */
	public final static JSONValidate NUMBER = new JSONValidate_Number();
	
	private final double m_min;
	private final double m_max;

	/**
	 * Verify that this JSON value is a Number.
	 * Any number is acceptable.
	 */
	public JSONValidate_Number()
	{
		this(false);
	}

	/**
	 * Verify that this JSON value is a Number or optionally null.
	 * Any number is acceptable.
	 * @param nullAllowed If true, allow null as well as a Number.
	 */
	public JSONValidate_Number(boolean nullAllowed)
	{
		this(Double.MIN_VALUE, Double.MAX_VALUE, nullAllowed);
	}

	/**
	 * Verify that this JSON value is a Number with a minimum value.
	 * @param min The value must be greater than or equal to this.
	 */
	public JSONValidate_Number(double min)
	{
		this(min, Double.MAX_VALUE, false);
	}

	/**
	 * Verify that this JSON value is a Number within a specified range.
	 * @param min The minimum value.
	 * @param max The maximum value.
	 */
	public JSONValidate_Number(double min, double max)
	{
		this(min, max, false);
	}
	
	/**
	 * Verify that this JSON value is a Number within a specified range.
	 * @param min The minimum value.
	 * @param max The maximum value.
	 * @param nullAllowed If true, allow null as well as a Number.
	 */
	public JSONValidate_Number(double min, double max, boolean nullAllowed)
	{
		super(JSONValue_Number.class, nullAllowed);
		m_min = min;
		m_max = max;
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
		if (value instanceof JSONValue_Number) {
			double d = ((JSONValue_Number)value).m_value;
			String err;
			if (d < m_min) {
				err = d + " less than " + m_min;
			} else if (d > m_max) {
				err = d + " greater than " + m_max;
			} else {
				err = null;
			}
			if (err != null) {
				handleValidationError(err + atPath(path));
				valid = false;
			}
		}
		return valid;
	}
}
