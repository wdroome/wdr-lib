package com.wdroome.json;

/**
 * A JSON parsing error, including the offending token
 * and input location (if known).
 * @author wdr
 */
public class JSONParseException extends JSONException
{
	private static final long serialVersionUID = 1448430568260827076L;

	/** The invalid token, or null if at EOF. */
	public final JSONLexanToken m_token;
	
	/** The location of the error, or null if unknown. */
	public final String m_location;
	
	/**
	 * Create a new parse error.
	 * @param msg Description of the error.
	 * @param token The offending token (may be null).
	 * @param location The location of the token (may be null).
	 */
	public JSONParseException(String msg, JSONLexanToken token, String location)
	{
		super(msg);
		m_token = token;
		m_location = location;
	}
	
	/**
	 * Return the class name, error message,
	 * offending token, and location in the input.
	 */
	@Override
	public String toString()
	{
		StringBuilder msg = new StringBuilder();
		msg.append("JSONParseException:");
		String s = getLocalizedMessage();
		String sep = "";
		if (s != null && !s.equals("")) {
			msg.append(" ");
			msg.append(s);
			if (!s.endsWith(".")) {
				sep = ".";
			}
		}
		if (m_token != null) {
			msg.append(sep);
			msg.append(" Last token: ");
			msg.append(m_token.toString());
			sep = "";
		}
		if (m_location != null && !m_location.equals("")) {
			msg.append(sep);
			msg.append(" at ");
			msg.append(m_location);
		}
		return msg.toString();
	}

}
