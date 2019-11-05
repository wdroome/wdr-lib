package com.wdroome.json.validate;

import java.math.BigInteger;

import com.wdroome.json.*;

/**
 * Validate a JSON BigInt. This also allows integer-valued JSON Numbers.
 * The various constructors specify different types of validation.
 * @author wdr
 */
public class JSONValidate_BigInt extends JSONValidate
{
	/** A simple static JSON Number validator for public use. */
	public final static JSONValidate BIGINT = new JSONValidate_BigInt();
	
	private final BigInteger m_min;
	private final BigInteger m_max;

	/**
	 * Verify that this JSON value is an integer.
	 * Any number is acceptable.
	 */
	public JSONValidate_BigInt()
	{
		this(false);
	}

	/**
	 * Verify that this JSON value is an integer or optionally null.
	 * Any number is acceptable.
	 * @param nullAllowed If true, allow null as well as a Number.
	 */
	public JSONValidate_BigInt(boolean nullAllowed)
	{
		this(null, null, nullAllowed);
	}

	/**
	 * Verify that this JSON value is an integer with a minimum value.
	 * @param min If not null, the value must be greater than or equal to this.
	 */
	public JSONValidate_BigInt(BigInteger min)
	{
		this(min, null, false);
	}

	/**
	 * Verify that this JSON value is an integer within a specified range.
	 * @param min If not null, the minimum value.
	 * @param max If not null, the maximum value.
	 */
	public JSONValidate_BigInt(BigInteger min, BigInteger max)
	{
		this(min, max, false);
	}
	
	/**
	 * Verify that this JSON value is an integer within a specified range.
	 * @param min If not null, the minimum value.
	 * @param max If not null, the maximum value.
	 * @param nullAllowed If true, allow null as well as a Number.
	 */
	public JSONValidate_BigInt(BigInteger min, BigInteger max, boolean nullAllowed)
	{
		super(JSONValue_BigInt.class, nullAllowed);
		m_min = min;
		m_max = max;
	}
	
	/**
	 * See {@link JSONValidate#validate(JSONValue,String)}.
	 */
	@Override
	public boolean validate(JSONValue value, String path) throws JSONValidationException
	{
		BigInteger thisValue;
		if (value instanceof JSONValue_Number) {
			try {
				thisValue = ((JSONValue_Number) value).toBigInteger();
			} catch (Exception e) {
				handleValidationError("Non-integer " + value + atPath(path));
				return false;
			}
		} else if (!super.validate(value, path)) {
			return false;
		} else if (value instanceof JSONValue_BigInt) {
			thisValue = ((JSONValue_BigInt)value).m_value;
		} else {
			// super() verifies that the above test is always true,
			// so we will never get here, but java doesn't know that.
			return false;
		}
		String err;
		if (m_min != null && thisValue.compareTo(m_min) < 0) {
			err = thisValue + " less than " + m_min;
		} else if (m_max != null && thisValue.compareTo(m_max) > 0) {
			err = thisValue + " greater than " + m_max;
		} else {
			err = null;
		}
		if (err != null) {
			handleValidationError(err + atPath(path));
			return false;
		}
		return true;
	}
}
