package com.wdroome.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.util.List;
import java.util.ArrayList;

/**
 * Send a stream of Server Sent Events.
 * @author wdr
 */
public class SSESender
{
	/** The end-of-line character we use. */
	public static final char EOL_CHAR = '\n';
	
	private static final byte[] DATA_FIELD_BYTES
				= (SSEEvent.DATA_FIELD_NAME + SSEEvent.FIELD_SEP_CHAR + " ").getBytes();
	private static final byte[] ID_FIELD_BYTES
				= (SSEEvent.ID_FIELD_NAME + SSEEvent.FIELD_SEP_CHAR + " ").getBytes();
	private static final byte[] EVENT_FIELD_BYTES
				= (SSEEvent.EVENT_FIELD_NAME + SSEEvent.FIELD_SEP_CHAR + " ").getBytes();
	
	private final OutputStream m_outStream;

	/**
	 * Create a new SSE stream.
	 * @param outStream
	 * 		The output stream. If it is not an instance of {@link BufferedOutputStream},
	 * 		and doNotuffer is false, create a BufferedOutputStream based on it.
	 * @param doNotBuffer
	 * 		If true, do not create a BufferedOutputStream layer;
	 * 		write data directly to outStream.
	 */
	public SSESender(OutputStream outStream, boolean doNotBuffer)
	{
		if (!doNotBuffer && !(outStream instanceof BufferedOutputStream)) {
			outStream = new BufferedOutputStream(outStream, 8192);
		}
		m_outStream = outStream;
	}
	
	/**
	 * Create a new SSE stream.
	 * @param outStream
	 * 		The output stream. If it is not an instance of {@link BufferedOutputStream},
	 * 		create a BufferedOutputStream based on it.
	 */
	public SSESender(OutputStream outStream)
	{
		this(outStream, false);
	}

	/**
	 * Send a data field. The data is written to the output stream,
	 * which may or may not be buffered, but the client will not
	 * treat the data as an event until you call
	 * {@link #sendEvent(String)} or {@link #sendEvent(String, String)}.
	 * @param dataLine
	 * 		The data. It is recommended that dataLine NOT contain
	 * 		any new-lines or carriage-returns. If dataLine does contain
	 * 		CRs or NLs, it will be split into multiple SSE data fields.
	 *		Note that trailing NLs and CRs are ignored, and a sequence
	 *		of NL's and CR's within the string will be treated as one NL.
	 *		If you want a data field with a new line, set dataLine to "".
	 * @throws IOException
	 * 		If a write error occurs.
	 */
	public void bufferData(String dataLine)
			throws IOException
	{
		if (dataLine == null) {
			return;
		}
		if (!hasNLorCR(dataLine)) {
			m_outStream.write(DATA_FIELD_BYTES);
			m_outStream.write(dataLine.getBytes());
			m_outStream.write(EOL_CHAR);
		} else {
			String[] lines = dataLine.split("[\r\n]+");
			for (String line: lines) {
				m_outStream.write(DATA_FIELD_BYTES);
				m_outStream.write(line.getBytes());
				m_outStream.write(EOL_CHAR);
			}
		}
	}
	
	/**
	 * Return true iff a string contains a new-line or a carriage return.
	 * @param s The string
	 * @return True iff s contains a new-line or a carriage return.
	 */
	private boolean hasNLorCR(String s)
	{
		return s.indexOf('\n') > 0 || s.indexOf('\r') > 0;
	}
	
	/**
	 * Send an event, including all data previously written via
	 * {@link #bufferData(String)}. This method completes the SSE
	 * by writing a blank line and flushing the output stream.
	 * @param event
	 * 		The event field, or null. May not contain new-lines or carriage returns.
	 * @param id
	 * 		The id field, or null. May not contain new-lines or carriage returns.
	 * @throws IOException
	 * 		If an I/O error occurs while writing the event.
	 * @throws IllegalArgumentException
	 * 		If event or id contain an NL or a CR.
	 */
	public void sendEvent(String event, String id)
			throws IOException
	{
		if (event != null && hasNLorCR(event)) {
			throw new IllegalArgumentException("SSESender.sendEvent(): event cannot have NL or CR");
		}
		if (id != null && hasNLorCR(id)) {
			throw new IllegalArgumentException("SSESender.sendEvent(): id cannot have NL or CR");
		}
		if (event != null) {
			m_outStream.write(EVENT_FIELD_BYTES);
			m_outStream.write(event.getBytes());
			m_outStream.write(EOL_CHAR);
		}
		if (id != null) {
			m_outStream.write(ID_FIELD_BYTES);
			m_outStream.write(id.getBytes());
			m_outStream.write(EOL_CHAR);
		}
		m_outStream.write(EOL_CHAR);
		m_outStream.flush();
	}

	/**
	 * Send an event, including all data previously written via
	 * {@link #bufferData(String)}. This method completes the SSE
	 * by writing a blank line and flushing the output stream.
	 * @param event
	 * 		The event field, or null. May not contain new-lines or carriage returns.
	 * @throws IOException
	 * 		If an I/O error occurs while writing the event.
	 * @throws IllegalArgumentException
	 * 		If event contains an NL or a CR.
	 */
	public void sendEvent(String event)
			throws IOException
	{
		sendEvent(event, null);
	}
	
	/**
	 * Send an event, including the data, event type and id.
	 * This method completes the SSE by writing a blank line
	 * and flushing the output stream.
	 * @param event
	 * 		The event to send.
	 * @throws IOException
	 * 		If an I/O error occurs while writing the event.
	 * @throws IllegalArgumentException
	 * 		If event.m_event or event.m_id contain an NL or a CR.
	 */
	public void sendEvent(SSEEvent event)
			throws IOException
	{
		if (event == null) {
			return;
		}
		if (event.m_dataLines != null) {
			for (String dataLine: event.m_dataLines) {
				bufferData(dataLine);
			}
		}
		sendEvent(event.m_event, event.m_id);
	}
}
