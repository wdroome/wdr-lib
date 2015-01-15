package com.wdroome.json;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.io.BufferedReader;
import java.util.HashMap;

/**
 * An implementation of the {@link IJSONLexan} interface.
 * Reads JSON from a variety of input sources,
 * as determined by the constructors.
 * @author wdr
 */
public class JSONLexan implements IJSONLexan
{
	private static HashMap<String,JSONLexanToken> RESERVED_WORDS = new HashMap<String, JSONLexanToken>();
	static {
		RESERVED_WORDS.put("true", new JSONLexanToken(JSONLexanToken.Token.TRUE));
		RESERVED_WORDS.put("false", new JSONLexanToken(JSONLexanToken.Token.FALSE));
		RESERVED_WORDS.put("null", new JSONLexanToken(JSONLexanToken.Token.NULL));
	}
	
	private final InputStream m_inputStream;
	private final Reader m_reader;
	private final String m_fileName;
	private final int m_contentLength;
	private final long m_size;
	
	private int m_nread = 0;
	private int m_lineNumber = 1;
	private int m_lineOffset = 0;
	private boolean m_prevCharWasCR = false;
	private int m_pushBackChar = -1;
	private boolean m_pushBackValid = false;
	
	private JSONLexanToken m_peekToken = null;
	
	/**
	 * Read JSON from an InputStream.
	 * @param inputStream The InputStream.
	 */
	public JSONLexan(InputStream inputStream)
	{
		this(inputStream, null, -1);
	}
	
	/**
	 * Read JSON from an InputStream.
	 * @param inputStream The InputStream.
	 * @param fileName The file name, for error messages.
	 * 			Use null if unknown.
	 * @param contentLength If positive, read at most this many characters.
	 */
	public JSONLexan(InputStream inputStream, String fileName, int contentLength)
	{
		m_inputStream = inputStream;
		m_reader = null;
		if (fileName != null && fileName.equals(""))
			fileName = null;
		m_fileName = fileName;
		m_contentLength = contentLength;
		m_size = contentLength;
	}
	
	/**
	 * Read JSON from a Reader.
	 * @param reader The Reader.
	 */
	public JSONLexan(Reader reader)
	{
		this(reader, null, -1);
	}
	
	/**
	 * Read JSON from a Reader.
	 * @param reader The Reader.
	 * @param fileName The file name, for error messages.
	 * 			Use null if unknown.
	 * @param contentLength If positive, read at most this many characters.
	 */
	public JSONLexan(Reader reader, String fileName, int contentLength)
	{
		m_reader = reader;
		m_inputStream = null;
		if (fileName != null && fileName.equals(""))
			fileName = null;
		m_fileName = fileName;
		m_contentLength = contentLength;
		m_size = contentLength;
	}
	
	/**
	 * Read JSON from a file.
	 * @param file The file name.
	 * @throws FileNotFoundException If the file cannot be opened.
	 */
	public JSONLexan(File file) throws FileNotFoundException
	{
		m_reader = new BufferedReader(new FileReader(file));
		m_inputStream = null;
		m_fileName = file.toString();
		m_contentLength = -1;
		m_size = file.length();
	}
	
	/**
	 * Read a JSON string. To read a file, use the {@link #JSONLexan(File)} constructor.
	 * @param jsonSrc The JSON string.
	 */
	public JSONLexan(String jsonSrc)
	{
		m_reader = new StringReader(jsonSrc);
		m_inputStream = null;
		m_fileName = "json-src";
		m_contentLength = -1;
		m_size = jsonSrc.length();
	}

	/**
	 * @see IJSONLexan#lastTokenLocation()
	 */
	@Override
	public String lastTokenLocation()
	{
		return "offset " + m_lineOffset + " of line " + m_lineNumber
					+ (m_fileName != null ? (" in file " + m_fileName) : "");
	}
	
	/**
	 * @see IJSONLexan#estimatedSize()
	 */
	@Override
	public long estimatedSize()
	{
		return m_size;
	}

	/**
	 * @see IJSONLexan#nextToken()
	 */
	@Override
	public JSONLexanToken nextToken() throws IOException
	{
		if (m_peekToken != null) {
			JSONLexanToken ret = m_peekToken;
			m_peekToken = null;
			return ret;
		}
		while (true) {
			int c = read();
			switch (c) {
			case -1:
				return null;
			case '[':
				return new JSONLexanToken(JSONLexanToken.Token.OPEN_SQUARE_BRACKET);
			case ']':
				return new JSONLexanToken(JSONLexanToken.Token.CLOSE_SQUARE_BRACKET);
			case '{':
				return new JSONLexanToken(JSONLexanToken.Token.OPEN_CURLY_BRACKET);
			case '}':
				return new JSONLexanToken(JSONLexanToken.Token.CLOSE_CURLY_BRACKET);
			case ',':
				return new JSONLexanToken(JSONLexanToken.Token.COMMA);
			case ':':
				return new JSONLexanToken(JSONLexanToken.Token.COLON);
			case '"':
				return getString();
			case '-':
				return getNumber(c);
			}
			
			if (Character.isWhitespace(c))
				continue;
			if (Character.isDigit(c))
				return getNumber(c);
			return getWord(c);
		}
	}
	
	/**
	 * @see IJSONLexan#peekToken()
	 */
	@Override
	public JSONLexanToken peekToken() throws IOException
	{
		m_peekToken = nextToken();
		return m_peekToken;
	}
	
	/**
	 * Read the next character. Return -1 at EOF.
	 * Maintain m_lineNumber and m_lineOffset.
	 */
	private int read() throws IOException
	{
		if (m_pushBackValid) {
			m_pushBackValid = false;
			return m_pushBackChar;			
		}
		if (m_contentLength >= 0 && m_nread >= m_contentLength) {
			return -1;
		}
		int c;
		if (m_inputStream != null) {
			c = m_inputStream.read();
		} else {
			c = m_reader.read();
		}
		m_nread++;
		if (c == '\r') {
			m_prevCharWasCR = true;
			m_lineNumber++;
			m_lineOffset = 0;
		} else if (c == '\n') {
			if (!m_prevCharWasCR) {
				m_lineNumber++;
			} else {
				m_prevCharWasCR = false;
			}
			m_lineOffset = 0;
		} else {
			m_prevCharWasCR = false;
			m_lineOffset++;
		}
		return c;
	}
	
	private void pushBack(int c)
	{
		m_pushBackChar = c;
		m_pushBackValid = true;
	}
	
	/**
	 * Parse a JSON string and return a token.
	 * The caller has eaten the leading '"'.
	 */
	private JSONLexanToken getString() throws IOException
	{
		StringBuilder word = new StringBuilder();
		int c;
		while (true) {
			c = read();
			if (c == -1) {
				return new JSONLexanToken(JSONLexanToken.Token.UNKNOWN, "Premature EOF in string");
			} else if (c == '"') {
				return new JSONLexanToken(word.toString());
			} else if (Character.isISOControl(c)) {
				return new JSONLexanToken(JSONLexanToken.Token.UNKNOWN,
							"Invalid control character 0x" + Integer.toHexString(c) + " in string");
			} else if (c == '\\') {
				c = read();
				switch (c) {
					case -1:	return new JSONLexanToken(JSONLexanToken.Token.UNKNOWN,
									"Premature EOF in string");
					case '"':	word.append('"'); break;
					case '/':	word.append('/'); break;
					case '\\':	word.append('\\'); break;
					case 't':	word.append('\t'); break;
					case 'r':	word.append('\r'); break;
					case 'n':	word.append('\n'); break;
					case 'b':	word.append('\b'); break;
					case 'f':	word.append('\f'); break;
					case 'u':
						{
							StringBuilder hex = new StringBuilder();
							for (int i = 0; i < 4; i++) {
								c = read();
								if (c == -1) {
									return new JSONLexanToken(JSONLexanToken.Token.UNKNOWN,
											"Premature EOF in hex constant in string");
								}
								if (!(Character.isDigit(c)
											|| (c >= 'a' && c <= 'f')
											|| (c >= 'A' && c <= 'F'))) {
									return new JSONLexanToken(JSONLexanToken.Token.UNKNOWN,
											"Invalid hex character '" + (char)c + "\" in string");
								}
								hex.append((char)c);
							}
							word.append((char)Integer.parseInt(hex.toString(), 16));
							break;
						}
					default: return new JSONLexanToken(JSONLexanToken.Token.UNKNOWN,
									"Invalid escape charater 0x" + Integer.toHexString(c) + " in string");
				}
			} else {
				word.append((char)c);
			}
		}
	}
	
	/**
	 * Read a JSON number and return the token.
	 * @param c The first character of the number: a digit or '-'.
	 */
	private JSONLexanToken getNumber(int c) throws IOException
	{
		StringBuilder word = new StringBuilder();
		word.append((char)c);
		while ((c = read()) != -1
				&& (Character.isDigit(c) || c == '-' || c == '+'
							|| c == '.' || c == 'e' || c == 'E')) {
			word.append((char)c);
		}
		pushBack(c);
		String strWord = word.toString();
		try {
			return new JSONLexanToken(Double.parseDouble(strWord));
		} catch (Exception e) {
			return new JSONLexanToken(JSONLexanToken.Token.UNKNOWN, strWord); 
		}
	}
	
	/**
	 * Read a reserved word and return the token.
	 * @param c The first character of the word.
	 */
	private JSONLexanToken getWord(int c) throws IOException
	{
		StringBuilder word = new StringBuilder();
		word.append((char)c);
		while ((c = read()) != -1 && Character.isLetter(c)) {
			word.append((char)c);
		}
		pushBack(c);
		String strWord = word.toString();
		JSONLexanToken token = RESERVED_WORDS.get(strWord);
		if (token != null)
			return token;
		else
			return new JSONLexanToken(JSONLexanToken.Token.UNKNOWN, strWord);
	}
}
