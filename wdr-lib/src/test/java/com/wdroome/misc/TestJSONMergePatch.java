package com.wdroome.misc;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import com.wdroome.json.JSONMergePatchScanner;
import com.wdroome.json.JSONParseException;
import com.wdroome.json.JSONValue;
import com.wdroome.json.JSONValueTypeException;
import com.wdroome.json.JSONValue_Object;
import com.wdroome.json.JSONValue;
import com.wdroome.json.JSONParser;
import com.wdroome.json.JSONLexan;

/**
 * @author wdr
 */
public class TestJSONMergePatch extends JSONMergePatchScanner
{
	/* (non-Javadoc)
	 * @see JSONMergePatchReader#newValue(java.util.List, JSONValue)
	 */
	@Override
	protected void newValue(List<String> path, JSONValue value)
	{
		System.out.println("New value: " + path + " -> " + value);
	}

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 * @throws JSONValueTypeException 
	 * @throws JSONParseException 
	 */
	public static void main(String[] args)
			throws JSONParseException, JSONValueTypeException, FileNotFoundException
	{
		TestJSONMergePatch scanner = new TestJSONMergePatch();
		if (args.length == 0) {
			args = new String[] {"-"};
		}
		for (String f: args) {
			System.out.println();
			System.out.println(f + ":");
			JSONLexan lexan = f.equals("-") ? new JSONLexan(System.in) : new JSONLexan(new File(f));
			JSONValue_Object patch = JSONParser.parseObject(lexan, true);
			scanner.scan(patch); 
		}
	}
}
