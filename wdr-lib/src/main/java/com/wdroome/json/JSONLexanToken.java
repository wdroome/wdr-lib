package com.wdroome.json;

import java.math.BigInteger;

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
			/** Tokens with a fractional part, or integer tokens which fit in a double. */
			NUMBER,
			/** Integer tokens which do not fit into a double without loss of precision. */
			BIGINT,
			UNKNOWN,
		};
	
	/** The type of this token. */
	public final Token m_token;
	
	private final String m_string;
	private final double m_number;
	private final BigInteger m_bigInt;
	
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
		case BIGINT:
			throw new IllegalArgumentException(
					"Must use JSONScannerToken(BigInteger) c'tor to create BIGINT token");
		case UNKNOWN:
			throw new IllegalArgumentException(
					"Must use JSONScannerToken(Token,String) c'tor to create UNKNOWN token");
			
		default:
			m_token = token;
			m_string = null;
			m_number = Double.NaN;
			m_bigInt = null;
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
		m_bigInt = null;
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
		m_bigInt = null;
	}
	
	/**
	 * Create a BIGINT token.
	 * @param value The value.
	 */
	public JSONLexanToken(BigInteger value)
	{
		// System.out.println("XXX: BigInt token " + value);
		m_token = Token.BIGINT;
		m_string = null;
		m_number = Double.NaN;
		m_bigInt = value;
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
			m_bigInt = null;
			break;

		default:
			throw new IllegalArgumentException(
					"Can only use JSONScannerToken(Token,String) c'tor to create STRING or UNKNOWN tokens");
		}
	}
	
	/**
	 * For STRING or UNKNOWN tokens, return the associated string.
	 * @return The associated String.
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
	 * @return The number as a double.
	 */
	public double getNumber()
	{
		switch (m_token) {
		case NUMBER:
			return m_number;
		case BIGINT:
			throw new IllegalStateException(
					"JSONScannerToken.getNumber() called on BIGINT token"
					+ " instead of NUMBER token");
		default:
			throw new IllegalStateException(
					"JSONScannerToken.getNumber() called on " + m_token.toString()
					+ " instead of NUMBER token");
		}
	}
	
	/**
	 * For BIGINT tokens, return the number as a BigInteger.
	 * @return The number as a BigInteger.
	 */
	public BigInteger getBigInt()
	{
		switch (m_token) {
		case BIGINT:
			return m_bigInt;
		case NUMBER:
			throw new IllegalStateException(
					"JSONScannerToken.getBigInt() called on NUMBER token"
					+ " instead of BIGINT token");
		default:
			throw new IllegalStateException(
					"JSONScannerToken.getBigInt() called on " + m_token.toString()
					+ " instead of BIGINT token");
		}
	}
	
	@Override
	public String toString()
	{
		switch (m_token) {
		case STRING:	return "\"" + m_string + "\"";
		case NUMBER:	return Double.toString(m_number);
		case BIGINT:	return m_bigInt.toString();
		default:		return m_token.toString();
		}	
	}
}
