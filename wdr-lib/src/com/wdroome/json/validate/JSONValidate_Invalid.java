package com.wdroome.json.validate;

import com.wdroome.json.*;

/**
 * A JSON validator that always fails.
 * @author wdr
 */
public class JSONValidate_Invalid extends JSONValidate
{
	private final String m_errorMsg;
	
	/**
	 * Create a validator that always fails.
	 * @param errorMsg The validation error message.
	 */
	public JSONValidate_Invalid(String errorMsg)
	{
		super(JSONValue.class, false);
		m_errorMsg = errorMsg;
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
		handleValidationError(m_errorMsg + atPath(path));
		return false;
	}
}
