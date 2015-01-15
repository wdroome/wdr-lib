package com.wdroome.json;

import java.io.InputStream;

/**
 * A JSONScanner with no-op versions of all abstract methods.
 * You can extend this class and just override the methods you need.
 * @author wdr
 */
public abstract class GenericJSONScanner extends JSONScanner
{
	public GenericJSONScanner(IJSONLexan lexan)
	{
		super(lexan);
	}

	/* (non-Javadoc)
	 * @see JSONScanner#enterDictionary()
	 */
	@Override
	public void enterDictionary() throws JSONParseException
	{
	}

	/* (non-Javadoc)
	 * @see JSONScanner#leaveDictionary()
	 */
	@Override
	public void leaveDictionary() throws JSONParseException
	{
	}

	/* (non-Javadoc)
	 * @see JSONScanner#gotDictionaryKey(java.lang.String)
	 */
	@Override
	public void gotDictionaryKey(String key) throws JSONParseException
	{
	}

	/* (non-Javadoc)
	 * @see JSONScanner#gotDictionaryValue(JSONValue)
	 */
	@Override
	public void gotDictionaryValue(JSONValue value) throws JSONParseException
	{
	}

	/* (non-Javadoc)
	 * @see JSONScanner#enterArray()
	 */
	@Override
	public void enterArray() throws JSONParseException
	{
	}

	/* (non-Javadoc)
	 * @see JSONScanner#leaveArray()
	 */
	@Override
	public void leaveArray() throws JSONParseException
	{
	}

	/* (non-Javadoc)
	 * @see cJSONScanner#gotArrayValue(JSONValue)
	 */
	@Override
	public void gotArrayValue(JSONValue value) throws JSONParseException
	{
	}

	/* (non-Javadoc)
	 * @see cJSONScanner#gotTopLevelValue(JSONValue)
	 */
	@Override
	public void gotTopLevelValue(JSONValue value) throws JSONParseException
	{
	}
}
