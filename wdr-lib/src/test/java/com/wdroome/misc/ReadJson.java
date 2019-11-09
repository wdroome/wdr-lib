package com.wdroome.misc;

import java.io.*;

import com.wdroome.json.*;

/**
 * @author wdr
 */
public class ReadJson
{
	public static JSONValue readJson(JSONLexan in, String desc)
	{
		try {
			JSONLexanToken firstToken = in.peekToken();
			if (firstToken.m_token == JSONLexanToken.Token.OPEN_CURLY_BRACKET) {
				return JSONParser.parse(in, true);
			} else if (firstToken.m_token == JSONLexanToken.Token.OPEN_SQUARE_BRACKET) {
				return JSONParser.parse(in, true);
			} else {
				throw new JSONException(desc + ": Input is not an OBJECT or an ARRAY");
			}
		} catch (JSONException e) {
			System.err.println(desc + ": Parsing error: " + e);
			return null;
		} catch (IOException e) {
			System.err.println(desc + ": Exception reading input: " + e);
			return null;
		}
	}

	public static void main(String[] args)
	{
		int nFailed = 0;
		for (String fname:args) {
			System.out.println(fname + ":");
			try {
				long startTS = System.currentTimeMillis();
				if (readJson(new JSONLexan(new FileInputStream(fname)), fname) == null)
					nFailed++;
				long endTS = System.currentTimeMillis();
				System.out.println("Read " + fname + " in " + (endTS - startTS) + " millisec");
			} catch (Exception e) {
				System.err.println(fname + ": Error opening file: " + e.getMessage());
				nFailed++;
			}
		}
		if (nFailed > 0)
			System.exit(1);
	}
}
