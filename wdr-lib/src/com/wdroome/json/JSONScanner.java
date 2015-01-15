package com.wdroome.json;

import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

/**
 * An "on-the-fly" parser for JSON input.
 * It reads JSON from an InputStream, and calls methods
 * when it finds a dictionary key, an array element, a new dictionary, etc.
 * To use, extend this class and implement the appropriate handler functions.
 * The class keeps a minimal amount of state, such as nesting depth in dictionaries.
 * @see GenericJSONScanner
 * @author wdr
 */
public abstract class JSONScanner
{
	private enum ScannerState {KEY, COLON, VALUE, COMMA};
	
	private ScannerState m_state = ScannerState.VALUE;
	
	private Stack<Boolean> m_isDictionaryStack = new Stack<Boolean>();
	private Stack<Boolean> m_hasElementsStack = new Stack<Boolean>();
	private Stack<String> m_dictionaryKeyStack = new Stack<String>();
	
	private final IJSONLexan m_lexan;
	
	protected int m_lineNumber = 1;
	protected int m_charInLine = 0;
	
	/**
	 * Create a new scanner. This just sets the input.
	 * You must call {@link #scan()} to start scanning.
	 * @param lexan A stream of JSON tokens.
	 */
	public JSONScanner(IJSONLexan lexan)
	{
		m_lexan = lexan;
	}
		
	/**
	 * Called when we detect an error. This creates and returns a new JSONParseException,
	 * which the caller throws. The base class appends the current line
	 * and character numbers to the message.
	 * Child classes can override if desired.
	 * @param msg A description of the error.
	 * @param token The offending token (may be null)
	 * @return A new JSONParseException for the error.
	 */
	public JSONParseException error(String msg, JSONLexanToken token)
	{
		return new JSONParseException(msg, token,
							m_lexan.lastTokenLocation());
	}
	
	/**
	 * Called when we enter a new dictionary.
	 * @throws JSONParseException If you want to abort the scan.
	 */
	public abstract void enterDictionary() throws JSONParseException;
	
	/**
	 * Called when we leave a dictionary.
	 * @throws JSONParseException If you want to abort the scan.
	 */
	public abstract void leaveDictionary() throws JSONParseException;
	
	/**
	 * Called when we get the key for a dictionary element.
	 * @param key The key.
	 * @throws JSONParseException If you want to abort the scan.
	 */
	public abstract void gotDictionaryKey(String key) throws JSONParseException;
	
	/**
	 * Called when we get a string, number or keyword value for a dictionary element.
	 * @param value The value.
	 * @throws JSONParseException If you want to abort the scan.
	 * @see #getTopDictionaryKey()
	 * @see #getDictionaryKeyPath()
	 */
	public abstract void gotDictionaryValue(JSONValue value) throws JSONParseException;
		
	/**
	 * Called when we enter a new array.
	 * @throws JSONParseException If you want to abort the scan.
	 */
	public abstract void enterArray() throws JSONParseException;
	
	/**
	 * Called when we leave an array.
	 * @throws JSONParseException If you want to abort the scan.
	 */
	public abstract void leaveArray() throws JSONParseException;
	
	/**
	 * Called when we get a string, number or keyword value for an array element.
	 * @param value The value.
	 * @throws JSONParseException If you want to abort the scan.
	 */
	public abstract void gotArrayValue(JSONValue value) throws JSONParseException;
	
	/**
	 * Called when we get a string, number or keyword value outside of an array or object.
	 * @param value The value.
	 * @throws JSONParseException If you want to abort the scan.
	 */
	public abstract void gotTopLevelValue(JSONValue value) throws JSONParseException;
	
	/**
	 * Scan the input and call the appropriate handler functions.
	 * Returns when finished.
	 * @throws JSONParseException If there's an error.
	 * @throws IOException 
	 */
	public void scan() throws JSONParseException, IOException
	{
		while (true) {
			JSONLexanToken tok = m_lexan.nextToken();
			if (tok == null) {
				if (m_isDictionaryStack.empty())
					return;
				else
					throw error("Unexpected EOF", null);
			}
			switch (tok.m_token) {
				case OPEN_CURLY_BRACKET:
					if (m_state == ScannerState.VALUE) {
						enterDictionary();
						m_isDictionaryStack.push(true);
						m_hasElementsStack.push(false);
						m_state = ScannerState.KEY;
					} else {
						throw error("Illegal '{'", tok);
					}
					break;
					
				case CLOSE_CURLY_BRACKET:
					if (m_isDictionaryStack.empty())
						throw error("Unexpected '}'", tok);
					if (m_state == ScannerState.KEY
							|| (m_state == ScannerState.COMMA && m_isDictionaryStack.peek())) {
						m_isDictionaryStack.pop();
						m_hasElementsStack.pop();
						leaveDictionary();
						if (inDictionary())
							m_dictionaryKeyStack.pop();
						m_state = ScannerState.COMMA;
						if (!m_hasElementsStack.empty() && !m_hasElementsStack.peek()) {
							m_hasElementsStack.pop();
							m_hasElementsStack.push(true);
						}
					} else {
						throw error("Unexpected '}'", tok);
					}
					break;
					
				case COLON:
					if (m_state == ScannerState.COLON) {
						m_state = ScannerState.VALUE;
					} else {
						throw error("Unexpected ':'", tok);
					}
					break;
					
				case COMMA:
					if (m_state == ScannerState.COMMA) {
						m_state = m_isDictionaryStack.peek() ? ScannerState.KEY : ScannerState.VALUE;
					} else {
						throw error("Unexpected ','", tok);
					}
					if (!m_hasElementsStack.peek()) {
						m_hasElementsStack.pop();
						m_hasElementsStack.push(true);
					}
					break;
					
				case OPEN_SQUARE_BRACKET:
					if (m_state == ScannerState.VALUE) {
						enterArray();
						m_isDictionaryStack.push(false);
						m_hasElementsStack.push(false);
						m_state = ScannerState.VALUE;
					} else {
						throw error("Unexpected '['", tok);
					}
					break;
					
				case CLOSE_SQUARE_BRACKET:
					if (m_isDictionaryStack.empty() || inDictionary())
						throw error("Unexpected ']'", tok);
					boolean hasElements = m_hasElementsStack.peek();
					if ((m_state == ScannerState.COMMA && hasElements)
							|| (m_state == ScannerState.VALUE && !hasElements)) {
						m_isDictionaryStack.pop();
						m_hasElementsStack.pop();
						leaveArray();
						if (inDictionary())
							m_dictionaryKeyStack.pop();
						m_state = ScannerState.COMMA;
						if (!m_hasElementsStack.empty() && !m_hasElementsStack.peek()) {
							m_hasElementsStack.pop();
							m_hasElementsStack.push(true);
						}
					} else {
						throw error("Unexpected ']'", tok);
					}
					break;
				
				case STRING:
				case NUMBER:
				case TRUE:
				case FALSE:
				case NULL:
					JSONValue scalarValue;
					String stringValue = null;
					switch (tok.m_token) {
						case STRING:
								stringValue = tok.getString();
								scalarValue = new JSONValue_String(stringValue);
								break;
						case NUMBER: scalarValue = new JSONValue_Number(tok.getNumber()); break;
						case TRUE: scalarValue = JSONValue_Boolean.TRUE; break;
						case FALSE: scalarValue = JSONValue_Boolean.FALSE; break;
						case NULL: scalarValue = JSONValue_Null.NULL; break;
						default: scalarValue = null; break; // shouldn't happen ....
					}
					if (m_isDictionaryStack.empty()) {
						gotTopLevelValue(scalarValue);
					} else if (!inDictionary()) {
						if (m_state != ScannerState.VALUE) {
							throw error("Unexpected string, number or keyword", tok);
						}
						gotArrayValue(scalarValue);
						m_state = ScannerState.COMMA;
						if (!m_hasElementsStack.peek()) {
							m_hasElementsStack.pop();
							m_hasElementsStack.push(true);
						}
					} else if (m_state == ScannerState.KEY && stringValue != null) {
						m_dictionaryKeyStack.push(stringValue);
						gotDictionaryKey(stringValue);
						m_state = ScannerState.COLON;
					} else if (m_isDictionaryStack.empty() || m_state != ScannerState.VALUE) {
						throw error("Unexpected string, number or keyword", tok);
					} else {
						gotDictionaryValue(scalarValue);
						m_dictionaryKeyStack.pop();
						m_state = ScannerState.COMMA;
						if (!m_hasElementsStack.peek()) {
							m_hasElementsStack.pop();
							m_hasElementsStack.push(true);
						}
					}
					break;

				default:
					throw error("Unexpected token", tok);
			}
		}
	}
	
	/**
	 * Return the current nesting depth. Includes dictionaries and arrays.
	 */
	public int getNestingDepth()
	{
		return m_isDictionaryStack.size();
	}
	
	/**
	 * Return true if we are currently in a dictionary,
	 * even if we're in an array that's a value in a dictionary.
	 */
	public boolean inDictionary()
	{
		return !m_isDictionaryStack.empty() && m_isDictionaryStack.peek();
	}
	
	/**
	 * Return the current nesting depth of dictionaries.
	 * E.g., if we're in an array that's a dictionary value,
	 * this method doesn't count the array.
	 */
	public int getNestedDictionaryDepth()
	{
		return m_dictionaryKeyStack.size();
	}
	
	/**
	 * Return the key of a dictionary element in the current dictionary nest.
	 * @param i The dictionary level. Range is 0 (outermost)
	 * 			to {@link #getNestedDictionaryDepth()}-1 (innermost).
	 * @return The key.
	 */
	public String getNestedDictionaryKey(int i)
	{
		return m_dictionaryKeyStack.get(i);
	}
	
	/**
	 * Return the current dictionary key stack as a "path."
	 * Keys are separated by periods. Eg, "key0.key1.key2".
	 * @return The current dictionary key path.
	 * @see #getParentDictionaryKeyPath()
	 */
	public String getDictionaryKeyPath()
	{
		StringBuilder str = new StringBuilder();
		String sep = "";
		for (String key:m_dictionaryKeyStack) {
			str.append(sep);
			str.append(key);
			sep = ".";
		}
		return str.toString();
	}
	
	/**
	 * Return the parent key path, that is the key path up to, but not including, the innermost key.
	 * @return The parent key path.
	 * @see #getDictionaryKeyPath()
	 */
	public String getParentDictionaryKeyPath()
	{
		StringBuilder str = new StringBuilder();
		String sep = "";
		int n = m_dictionaryKeyStack.size();
		for (int i = 0; i < n-1; i++) {
			str.append(sep);
			str.append(m_dictionaryKeyStack.get(i));
			sep = ".";
		}
		return str.toString();
	}
	
	/**
	 * Return the top (innermost) dictionary key.
	 * @return The top dictionary key, or null if we're not in a dictionary.
	 */
	public String getTopDictionaryKey()
	{
		int n = m_dictionaryKeyStack.size();
		if (n > 0)
			return m_dictionaryKeyStack.get(n-1);
		else
			return null;
	}
}
