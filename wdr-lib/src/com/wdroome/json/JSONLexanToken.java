package com.wdroome.json;

/**
 * A token returned by a JSON Lexical Analyzer.
 * @author wdr
 */
public class JSONLexanToken
{
	/** The token types. */
	public static enum Token {
			OPEN_CURLY_BRACKET,
			CLOSE_CURLY_BRACKET,
			OPEN_SQUARE_BRACKET,
			CLOSE_SQUARE_BRACKET,
			COLON,
			COMMA,
			TRUE,
			FALSE,
			NULL,
			STRING,
			NUMBER,
			UNKNOWN,
		};
	
	/** The type of this token. */
	public final Token m_token;
	
	private final String m_string;
	private final double m_number;
	
	/**
	 * Create a simple token: type only, no associated value.
	 * @param token The token type.
	 * @throws IllegalArgumentException
	 * 		If token isn't a simple type.
	 */
	public JSONLexanToken(Token token)
	{
		switch (token) {
		case STRING:
			throw new IllegalArgumentException(
					"Must use JSONScannerToken(String) c'tor to create STRING token");
		case NUMBER:
			throw new IllegalArgumentException(
					"Must use JSONScannerToken(double) c'tor to create NUMBER token");
		case UNKNOWN:
			throw new IllegalArgumentException(
					"Must use JSONScannerToken(Token,String) c'tor to create UNKNOWN token");
			
		default:
			m_token = token;
			m_string = null;
			m_number = Double.NaN;
		}
	}
	
	/**
	 * Create a STRING token.
	 * @param value The value.
	 */
	public JSONLexanToken(String value)
	{
		m_token = Token.STRING;
		m_string = value;
		m_number = Double.NaN;
	}
	
	/**
	 * Create a NUMBER token.
	 * @param value The value.
	 */
	public JSONLexanToken(double value)
	{
		m_token = Token.NUMBER;
		m_string = null;
		m_number = value;
	}
	
	/**
	 * Create a STRING or UNKNOWN token.
	 * @param token The token type.
	 * @param value The value.
	 * @throws IllegalArgumentException
	 * 		If token isn't STRING or UNKNOWN.
	 */
	public JSONLexanToken(Token token, String value)
	{
		switch (token) {
		case UNKNOWN:
		case STRING:
			m_token = token;
			m_string = value;
			m_number = Double.NaN;
			break;

		default:
			throw new IllegalArgumentException(
					"Can only use JSONScannerToken(Token,String) c'tor to create STRING or UNKNOWN tokens");
		}
	}
	
	/**
	 * For STRING or UNKNOWN tokens, return the associated string.
	 */
	public String getString()
	{
		switch (m_token) {
		case STRING:
		case UNKNOWN:
			return m_string;
		default:
			throw new IllegalStateException(
					"JSONScannerToken.getString() called on " + m_token.toString()
					+ " instead of STRING or UNKNOWN token");
		}
	}
	
	/**
	 * For NUMBER tokens, return the number as a double.
	 */
	public double getNumber()
	{
		switch (m_token) {
		case NUMBER:
			return m_number;
		default:
			throw new IllegalStateException(
					"JSONScannerToken.getNumber() called on " + m_token.toString()
					+ " instead of NUMBER token");
		}
	}
	
	@Override
	public String toString()
	{
		switch (m_token) {
		case STRING:	return "\"" + m_string + "\"";
		case NUMBER:	return Double.toString(m_number);
		default:		return m_token.toString();
		}	
	}
}
