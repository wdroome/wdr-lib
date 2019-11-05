package com.wdroome.json.validate;

import com.wdroome.json.*;

/**
 * Validate a JSON boolean.
 * @author wdr
 */
public class JSONValidate_Boolean extends JSONValidate
{
	/** A simple static JSON Boolean validator for public use. */
	public final static JSONValidate BOOLEAN = new JSONValidate_Boolean();
	
	/**
	 * Verify that this JSON value is a Boolean.
	 */
	public JSONValidate_Boolean()
	{
		this(false);
	}

	/**
	 * Verify that this JSON value is a Boolean.
	 * @param nullAllowed If this JSON value can be null.
	 */
	JSONValidate_Boolean(boolean nullAllowed)
	{
		super(JSONValue_Boolean.class, nullAllowed);
	}
}
