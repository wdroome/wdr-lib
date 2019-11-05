package com.wdroome.json;

import java.io.IOException;

/**
 * A JSON String.
 * For simplicity, the string value is a read-only public member.
 * <p>
 * It would be nice if this could extend java.lang.String, but that's final.
 * @author wdr
 */
public class JSONValue_String implements JSONValue
{
	/** The read-only string value. */
	public final String m_value;
	
	/**
	 * Create a new JSON string.
	 * @param value The string value. If null, use "".
	 */
	public JSONValue_String(String value)
	{
		m_value = value != null ? value : "";
	}
	
	/**
	 * Return a compact JSON encoding of this string.
	 */
	@Override
	public String toString()
	{
		return quotedString(m_value);
	}
	
	/**
	 * @see JSONValue#writeJSON(JSONWriter)
	 */
	@Override
	public void writeJSON(JSONWriter writer) throws IOException
	{
		writer.write(quotedString(m_value));
	}

	/**
	 * @see JSONValue#isSimple()
	 */
	@Override
	public boolean isSimple()
	{
		return true;
	}
	
	/**
	 * @see JSONValue#jsonType()
	 */
	@Override
	public String jsonType()
	{
		return "String";
	}
	
	private static final char[] HEX_DIGITS = {
				'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
				'a', 'b', 'c', 'd', 'e', 'f'		
			};
	
	/**
	 * Return a string as a quoted JSON string constant.
	 * Surround with quotes, and escape appropriate characters.
	 * <p>
	 * Note that this class generates escapes, but does not read them.
	 * That is, it converts an internal string to an escaped external string.
	 * The method that recognizes escapes and
	 * converts an external escaped string into an internal string
	 * is in the {@link JSONLexan} class.
	 * 
	 * @param value The string.
	 * @return value as a quoted, escaped string.
	 */
	public static String quotedString(String value)
	{
		boolean needsEscape = false;
		int len = value.length();
		for (int i = 0; i < len && !needsEscape; i++) {
			char c = value.charAt(i);
			if (c <= 0x1f || c >= 0x7f || c == '\\' || c == '/' || c == '"') {
				needsEscape = true;
			}
		}
		if (!needsEscape) {
			StringBuilder buff = new StringBuilder(2 +len);
			buff.append('"');
			buff.append(value);
			buff.append('"');
			return buff.toString();
		} else {
			StringBuilder buff = new StringBuilder(2*len);
			buff.append('"');
			for (int i = 0; i < len; i++) {
				char c = value.charAt(i);
				switch (c) {
					case '"':	buff.append("\\\""); break;
					case '\\':	buff.append("\\\\"); break;
					case '\t':	buff.append("\\t"); break;
					case '\r':	buff.append("\\r"); break;
					case '\n':	buff.append("\\n"); break;
					case '\b':	buff.append("\\b"); break;
					case '\f':	buff.append("\\f"); break;
					default:
						if (c <= 0x1f || c >= 0x7e) {
							buff.append("\\u");
							buff.append(HEX_DIGITS[(c & 0xf000) >> 12]);
							buff.append(HEX_DIGITS[(c & 0x0f00) >>  8]);
							buff.append(HEX_DIGITS[(c & 0x00f0) >>  4]);
							buff.append(HEX_DIGITS[(c & 0x000f)      ]);
						} else {
							buff.append(c);
						}
				}
			}
			buff.append('"');
			return buff.toString();
		}
	}

	/**
	 * Return the hashcode of the string value.
	 */
	@Override
	public int hashCode()
	{
		return m_value.hashCode();
	}

	/**
	 * Return true iff the strings are equal.
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		return m_value.equals(((JSONValue_String)obj).m_value);
	}
}
