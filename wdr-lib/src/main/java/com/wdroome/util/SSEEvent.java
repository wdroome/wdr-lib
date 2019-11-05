package com.wdroome.util;

import java.util.List;
import java.util.ArrayList;

/**
 * A Server Sent Event (SSE), and constants related to SSEs.
 * @author wdr
 */
public class SSEEvent
{
	/** Media type for SSE streams. */
	public static final String MEDIA_TYPE = "text/event-stream";
	
	/** Name of data field. */
	public static final String DATA_FIELD_NAME = "data";
	
	/** Name of id field. */
	public static final String ID_FIELD_NAME = "id";
	
	/** Name of event field. */
	public static final String EVENT_FIELD_NAME = "event";
	
	/** Character separating a field name from its value. */
	public static final char FIELD_SEP_CHAR = ':';
	
	/** The event type, or null. */
	public final String m_event;
	
	/** The event id, or null. */
	public final String m_id;
	
	/**
	 * The event's data fields. Never null, but may be empty.
	 * The entries do NOT contain or end with new-lines.
	 */
	public final List<String> m_dataLines;
	
	/**
	 * Create a new event.
	 * @param event The event type, or null.
	 * @param id The event id, or null.
	 * @param dataLines The data lines, or null.
	 */
	public SSEEvent(String event, String id, List<String> dataLines)
	{
		m_event = event;
		m_id = id;
		m_dataLines = dataLines != null ? dataLines : new ArrayList<String>(0);
	}
	
	/**
	 * Concatenate all data field lines into a StringBuilder.
	 * @param sep
	 * 		When concatenating the data lines, use this string
	 *		to separate the lines. If null, use a new-line.
	 * @param buff
	 * 		Append the concatenated lines to this StringBuilder.
	 * 		If null, create a new StringBuilder.
	 * @return
	 * 		A StringBuilder with the concatenation of all data lines.
	 * 		If buff != null, return buff. Otherwise return a new
	 * 		StringBuilder with the concatenation.
	 */
	public StringBuilder getData(String sep, StringBuilder buff)
	{
		if (sep == null) {
			sep = "\n";
		}
		if (buff == null) {
			buff = new StringBuilder();
		}
		if (m_dataLines == null || m_dataLines.isEmpty()) {
			return buff;
		} else {
			boolean first = true;
			for (String s: m_dataLines) {
				if (first) {
					first = false;
				} else {
					buff.append(sep);
				}
				buff.append(s);
			}
			return buff;
		}
	}
	
	/**
	 * Return a string with the concatenation of all data fields.
	 * @param sep
	 * 		When concatenating the data lines, use this string
	 *		to separate the lines. If null, use a new-line.
	 * @return
	 * 		The concatenation of all data fields.
	 * 		If there are no data fields, return "".
	 */
	public String getData(String sep)
	{
		return getData(sep, null).toString();
	}
}
