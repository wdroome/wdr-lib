package com.wdroome.json;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;

/**
 * Parse JSON and return a JSON value.
 * @author wdr
 */
public class JSONParser
{
	private boolean m_usePathNames = false;
	
	/**
	 * Parse JSON-encoded input and return the corresponding JSON value.
	 * @param lexan A lexical analyzer for the input source.
	 * @param usePathNames True iff we retain pathnames for objects.
	 * @return The JSON value represented by the input.
	 * @throws JSONParseException If the input cannot be parsed.
	 */
	public static JSONValue parse(IJSONLexan lexan, boolean usePathNames)
			throws JSONParseException
	{
		JSONParser parser = new JSONParser();
		parser.usePathNames(usePathNames);
		return parser.parse(lexan);
	}
	
	/**
	 * Parse a JSON-encoded string and return the JSON Object it describes.
	 * @param lexan A lexical analyzer for the input source.
	 * @param usePathNames True iff we retain pathnames for objects.
	 * @return The JSON object represented by the input.
	 * @throws JSONParseException
	 * 		If the input cannot be parsed.
	 * @throws JSONValueTypeException
	 * 		If the input is valid JSON, but does not define a JSON Object.
	 */
	public static JSONValue_Object parseObject(IJSONLexan lexan, boolean usePathNames)
			throws JSONParseException, JSONValueTypeException
	{
		JSONParser parser = new JSONParser();
		parser.usePathNames(usePathNames);
		JSONValue value = parser.parse(lexan);
		if (value instanceof JSONValue_Object) {
			return (JSONValue_Object)value;
		} else {
			throw new JSONValueTypeException("Top level is not a JSON Object", null);
		}
	}
	
	/**
	 * Parse JSON input and return the value contained therein.
	 * @param lexan A lexical analyzer to break the input stream into tokens.
	 * @return The JSON value defined by the input.
	 * @throws JSONParseException If the input cannot be parsed.
	 */
	public JSONValue parse(IJSONLexan lexan) throws JSONParseException
	{
		JSONLexanToken token = nextToken(lexan);
		if (token == null) {
			throw new JSONParseException("Empty JSON", null, lexan.lastTokenLocation());
		}
		JSONValue value;
		switch (token.m_token) {
			case NUMBER:	value = new JSONValue_Number(token.getNumber()); break;
			case BIGINT:	value = new JSONValue_BigInt(token.getBigInt()); break;
			case STRING:	value = new JSONValue_String(token.getString()); break;
			case TRUE:		value = JSONValue_Boolean.TRUE; break;
			case FALSE:		value = JSONValue_Boolean.FALSE; break;
			case NULL:		value = JSONValue_Null.NULL; break;
			case OPEN_CURLY_BRACKET:	value = parseObject(lexan, new JSONValue_Object()); break;
			case OPEN_SQUARE_BRACKET:	value = parseArray(lexan, new JSONValue_Array()); break;
			default:
				throw new JSONParseException("Invalid JSON input", token, lexan.lastTokenLocation());
		}
		if ((token = nextToken(lexan)) != null) {
			throw new JSONParseException("Excess characters after value", token, lexan.lastTokenLocation());
		}
		return value;
	}
	
	/**
	 * @return True iff we maintain pathnames for objects.
	 */
	public boolean usePathNames()
	{
		return m_usePathNames;
	}

	/**
	 * @param usePathNames True iff we maintain pathnames for objects.
	 */
	public void usePathNames(boolean usePathNames)
	{
		m_usePathNames = usePathNames;
	}

	private JSONValue parseObject(IJSONLexan lexan, JSONValue_Object object) throws JSONParseException
	{
		object.usePathNames(m_usePathNames);
		int nAdded = 0;
		while (true) {
			// Read key.
			JSONLexanToken token = nextToken(lexan);
			if (token == null) {
				throw new JSONParseException("Premature EOF in JSON object; expecting key", null, null);
			}
			String key;
			switch (token.m_token) {
			case STRING:
					key = token.getString();
					break;
			case CLOSE_CURLY_BRACKET:
					if (nAdded == 0) {
						return object;
					}
					// fall thru to invalid input
			default:
					throw new JSONParseException("Invalid input in JSON object; expecting key",
							token, lexan.lastTokenLocation());
			}
			nAdded++;
			
			// Read colon separator.
			token = nextToken(lexan);
			if (token == null) {
				throw new JSONParseException("Premature EOF in JSON object; expecting ':'", null, null);
			}
			switch (token.m_token) {
			case COLON:
					break;
			default:
					throw new JSONParseException("Invalid input in JSON object; expecting ':'",
							token, lexan.lastTokenLocation());
			}
			
			// Read value & add to object.
			token = nextToken(lexan);
			if (token == null) {
				throw new JSONParseException("Premature EOF in JSON object; expecting value", null, null);
			}
			switch (token.m_token) {
				case NUMBER:
					object.put(key, new JSONValue_Number(token.getNumber()));
					break;
				case BIGINT:
					object.put(key, new JSONValue_BigInt(token.getBigInt()));
					break;
				case STRING:
					object.put(key, new JSONValue_String(token.getString()));
					break;
				case TRUE:
					object.put(key, JSONValue_Boolean.TRUE);
					break;
				case FALSE:
					object.put(key, JSONValue_Boolean.FALSE);
					break;
				case NULL:
					object.put(key, JSONValue_Null.NULL);
					break;
				case OPEN_CURLY_BRACKET:
					object.put(key, parseObject(lexan, new JSONValue_Object()));
					break;
				case OPEN_SQUARE_BRACKET:
					object.put(key, parseArray(lexan, new JSONValue_Array()));
					break;
				default:
					throw new JSONParseException("Invalid input in JSON object; expecting value",
								token, lexan.lastTokenLocation());
			}
			
			// Read comma separator or square bracket to close object.
			token = nextToken(lexan);
			if (token == null) {
				throw new JSONParseException("Premature EOF in JSON object; expecting ',' or '}'", null, null);
			}
			switch (token.m_token) {
			case COMMA:
					break;
			case CLOSE_CURLY_BRACKET:
					return object;
			default:
					throw new JSONParseException("Invalid input in JSON object; expecting ',' or '}'",
								token, lexan.lastTokenLocation());
			}
		}
	}
	
	private JSONValue parseArray(IJSONLexan lexan, JSONValue_Array array) throws JSONParseException
	{
		int nAdded = 0;
		while (true) {
			// Read value and add to array.
			JSONLexanToken token = nextToken(lexan);
			if (token == null) {
				throw new JSONParseException("Premature EOF in JSON array", null, null);
			}
			switch (token.m_token) {
				case NUMBER:
					array.add(new JSONValue_Number(token.getNumber()));
					break;
				case BIGINT:
					array.add(new JSONValue_BigInt(token.getBigInt()));
					break;
				case STRING:
					array.add(new JSONValue_String(token.getString()));
					break;
				case TRUE:
					array.add(JSONValue_Boolean.TRUE);
					break;
				case FALSE:
					array.add(JSONValue_Boolean.FALSE);
					break;
				case NULL:
					array.add(JSONValue_Null.NULL);
					break;
				case OPEN_CURLY_BRACKET:
					array.add(parseObject(lexan, new JSONValue_Object()));
					break;
				case OPEN_SQUARE_BRACKET:
					array.add(parseArray(lexan, new JSONValue_Array()));
					break;
				case CLOSE_SQUARE_BRACKET:
					if (nAdded == 0) {
						return array;
					}
					// fall thru to invalid input
				default:
					throw new JSONParseException("Invalid input in JSON array; expecting value",
								token, lexan.lastTokenLocation());
			}
			nAdded++;
			
			// Read comma separator or curly bracket to close array.
			token = nextToken(lexan);
			if (token == null) {
				throw new JSONParseException("Premature EOF in JSON array; expecting ',' or ']'", null, null);
			}
			switch (token.m_token) {
			case COMMA:
					break;
			case CLOSE_SQUARE_BRACKET:
					return array;
			default:
				throw new JSONParseException("Invalid input in JSON array; expecting ',' or ']'",
								token, lexan.lastTokenLocation());
			}
		}
	}
	
	/**
	 * Return next token from the lexical analyzer.
	 * Convert any IOExceptions to ParseExceptions, with the last known location.
	 * @param lexan The input the lexical analyzer.
	 * @return The next token.
	 * @throws JSONParseException If an IO error occurs.
	 */
	private JSONLexanToken nextToken(IJSONLexan lexan) throws JSONParseException
	{
		try {
			return lexan.nextToken();
		} catch (IOException e) {
			throw new JSONParseException(e.toString(), null, lexan.lastTokenLocation());
		}
	}
	
	/**
	 * For testing, parse and print the json in each file passed as an argument.
	 * @param args File names. "-" (or no names) means read stdin.
	 * 		--fancy means pretty-print the output and write object dictionaries in key order;
	 *		this is the default. --plain means output compact json with no spaces or indenting.
	 * @throws FileNotFoundException If we cannot open a file.
	 */
	public static void main(String[] args)
	{
		JSONParser parser = new JSONParser();
		if (args.length == 0) {
			args = new String[] {"-"};
		}
		JSONWriter writer = new JSONWriter(System.out);
		writer.setIndented(true);
		writer.setSorted(true);
		for (String f:args) {
			if (f.equals("--fancy")) {
				writer.setIndented(true);
				writer.setSorted(true);
			} else if (f.equals("--plain")) {
				writer.setIndented(false);
				writer.setSorted(false);
			} else {
				System.out.println();
				System.out.println(f + ":");
				JSONValue v = null;
				try {
					JSONLexan lexan;
					if (f.equals("-")) {
						lexan = new JSONLexan(System.in, "-", -1);
					} else {
						lexan = new JSONLexan(new File(f));
					}
					v = parser.parse(lexan);
				} catch (FileNotFoundException e) {
					System.out.println("Error opening file '" + f + "': " + e.toString());
				} catch (JSONParseException e) {
					System.out.println("JSON Parse error: " + e.toString());
				}
				if (v != null) {
					try {
						v.writeJSON(writer);
						if (!writer.isIndented()) {
							System.out.println();
						}
					} catch (IOException e) {
						System.out.println("IOException writing JSON: ");
						e.printStackTrace(System.out);
					}
				}
			}
		}
	}
}
