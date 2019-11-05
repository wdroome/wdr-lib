package com.wdroome.altodata;

import com.wdroome.json.JSONException;

/**
 * Represent an illegal version-tag error.
 * That is, a version tag that does not follow the rules of the ALTO RFC.
 * @author wdr
 */
public class IllegalVersionTagException extends JSONException
{
	private static final long serialVersionUID = 5185284345061512005L;

	public IllegalVersionTagException(String msg)
	{
		super(msg);
	}
}
