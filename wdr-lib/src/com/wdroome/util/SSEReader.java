package com.wdroome.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;

/**
 * A thread that reads Server Sent Events from a stream.
 * To get SSEs, create an instance of this class,
 * and give the c'tor the input stream and a callback object
 * which implements the {@link EventCB} interface.
 * The thread calls methods of that object when new SSEs arrive,
 * or when an error occurs or when the stream is closed.
 * @author wdr
 */
public class SSEReader extends Thread
{

	/**
	 * Callback interface for Server Sent Event arrival.
	 */
	public interface EventCB
	{
		/**
		 * Called when a new SSE arrives.
		 * @param event The new event.
		 */
		public void newSSE(SSEEvent event);
		
		/**
		 * Called when the stream is closed.
		 */
		public void eofSSE();
		
		/**
		 * Called when a permanent I/O error occurs on the SSE stream.
		 * {@link #eofSSE()} is called after this method returns.
		 */
		public void errorSSE(IOException e);
	}
	
	/** The stream on which SSE's arrive. */
	private final InputStream m_inStream;
	
	/** The method to call when an event arrives. */
	private final EventCB m_callback;

	/**
	 * Create a new SSE reader, and start the thread.
	 * @param inStream The input stream for reading SSE events.
	 * @param callback The event callback method.
	 */
	public SSEReader(InputStream inStream, EventCB callback)
	{
		m_inStream = inStream;
		m_callback = callback;
		start();
	}
	
	/**
	 * Read events from the input stream, and call the callback method
	 * when new SSEs arrive. Close the stream when done.
	 */
	@Override
	public void run()
	{
		Line line;
		String eventType = null;
		String eventId = null;
		List<String> dataLines = new ArrayList<String>();
		try {
			while ((line = readLine(false)) != null) {
				if (line.isEventEnd()) {
					if (eventType != null || eventId != null || !dataLines.isEmpty()) {
						m_callback.newSSE(new SSEEvent(eventType, eventId, dataLines));
					}
					eventType = null;
					eventId = null;
					dataLines = new ArrayList<String>();
				} else if (line.m_field == null) {
					// skip comment
				} else if (line.m_field.equals(SSEEvent.EVENT_FIELD_NAME)) {
					eventType = line.m_value;
				} else if (line.m_field.equals(SSEEvent.ID_FIELD_NAME)) {
					eventId = line.m_value;
				} else if (line.m_field.equals(SSEEvent.DATA_FIELD_NAME)) {
					dataLines.add(line.m_value);
				}
			}
		} catch (IOException e) {
			m_callback.errorSSE(e);
		}
		m_callback.eofSSE();
		try { m_inStream.close(); } catch (Exception e) {}
	}

	/**
	 * An event line.
	 */
	private static class Line
	{
		private final String m_field;
		private final String m_value;
		
		private Line(String field, String value)
		{
			m_field = field;
			m_value = value;
		}
		
		private boolean isEventEnd()
		{
			return m_field == null && m_value == null;
		}
		
		private boolean isComment()
		{
			return m_field == null && m_value != null;
		}
	}
	
	private static enum LineState {AT_START, IN_FIELD, AT_VALUE, IN_VALUE, EOL};
	
	/**
	 * Read and return the next event line. Block until a line is available.
	 * @param returnComments If true, return comment lines. If false, ignore comments.
	 * @return The next event line, or null on EOF.
	 * @throws IOException If an I/O error occurs.
	 */
	private Line readLine(boolean returnComments) throws IOException
	{
		while (true) {
			StringBuilder field = new StringBuilder();
			StringBuilder value = new StringBuilder();
			LineState state = LineState.AT_START;
			boolean badLine = false;
			int c;
			while (state != LineState.EOL && (c = nextChar()) >= 0) {
				if (state == LineState.AT_START && (c == '\n' || c == '\r')) {
					return new Line(null, null);
				} else if (state == LineState.AT_START || state == LineState.IN_FIELD) {
					if (c == SSEEvent.FIELD_SEP_CHAR) {
						state = LineState.AT_VALUE;
					} else if (c != '\r' && c != '\n') {
						field.append((char)c);
						state = LineState.IN_FIELD;
					} else {
						// Oops -- sender protocol error.
						badLine = true;
						break;
					}
				} else if (state == LineState.AT_VALUE && c == ' ') {
					state = LineState.IN_VALUE;
				} else if (c == '\n' || c == '\r') {
					state = LineState.EOL;
				} else {
					value.append((char)c);
					state = LineState.IN_VALUE;
				}
			}
			if (badLine) {
				// continue
			} else if (state == LineState.AT_START) {
				return null;
			} else if (field.length() > 0) {
				// System.out.println("XXX: Line " + field.toString() + ": " + value.toString());
				return new Line(field.toString(), value.toString());
			} else if (returnComments) {
				// System.out.println("XXX: Comment " + value.toString());
				return new Line(null, value.toString());
			} else {
				// Comment: instead of returning the comment,
				// just continue the outer loop,
				// so the method does not return until it
				// has an actual line.
			}
		}
	}
	
	private static final byte[] BOM = new byte[] {(byte)0xef, (byte)0xbb, (byte)0xbf};	
	private boolean m_initBOMScan = true;
	private int m_initNonBomChar = -1;
	private int m_echoInitBomCount = 0;
	private int m_echoInitBomIndex = -1;
	
	/**
	 * Like {@link #nextByte()}, but skip initial BOM if present.
	 * Warning: Kludgy code!
	 * @return The next character, or -1 if at EOF.
	 * @throws IOException If an I/O error occurs while reading.
	 */
	private int nextChar() throws IOException
	{
		if (!m_initBOMScan) {
			return nextByte();
		}
		if (m_echoInitBomIndex < 0) {
			int c;
			for (m_echoInitBomCount = 0;
						(c = nextByte()) >= 0
								&& m_echoInitBomCount < BOM.length
								&& c == (BOM[m_echoInitBomCount] & 0xff);
						m_echoInitBomCount++) {
			}
			if (m_echoInitBomCount == 0 || m_echoInitBomCount >= BOM.length) {
				m_initBOMScan = false;
				return c;
			}
			m_initNonBomChar = c;
			m_echoInitBomIndex = 1;
			return BOM[0] & 0xff;
		} else if (m_echoInitBomIndex < m_echoInitBomCount) {
			return BOM[m_echoInitBomIndex++] & 0xff;
		} else {
			m_initBOMScan = false;
			return m_initNonBomChar;
		}
	}
	
	private Chunk m_curChunk = null;
	private boolean m_ignoreNextNL = false;
	private boolean m_eof = false;
	
	/**
	 * Return the next byte, or block until available.
	 * if the stream has a CR-LF, this method returns the CR,
	 * but ignores the following NL.
	 * @return The next character, or -1 if at EOF.
	 * @throws IOException If an I/O error occurs while reading.
	 */
	private int nextByte() throws IOException
	{
		while (true) {
			if (m_eof) {
				return -1;
			}
			if (m_curChunk == null) {
				m_curChunk = readChunk();
				if (m_curChunk == null) {
					m_eof = true;
					return -1;
				}
			}
			int c = m_curChunk.next();
			if (c < 0) {
				m_curChunk = null;
			} else if (c == '\n' && m_ignoreNextNL) {
				m_ignoreNextNL = false;
			} else {
				if (c == '\r') {
					m_ignoreNextNL = true;
				}
				return c;
			}
		}
	}
	
	/**
	 * A block of data read from the input stream,
	 * with a next() method to iterate over the bytes
	 * in the block.
	 */
	private static class Chunk
	{
		private final int m_size;
		private int m_next;
		private final byte[] m_chunk;
		
		private Chunk(int n, byte[] chunk)
		{
			m_size = n;
			m_chunk = chunk;
			m_next = 0;
		}
		
		/**
		 * Return the next character, or -1 if we have returned
		 * all bytes in the block.
		 */
		private int next()
		{
			if (m_next < m_size) {
				return m_chunk[m_next++] & 0xff;
			} else {
				return -1;
			}
		}
	}
	
	/**
	 * Read and return data from the input stream.
	 * @return A block of data, or null on EOF.
	 * @throws IOException If an error occurs while reading.
	 */
	private Chunk readChunk() throws IOException
	{
		int c;
		c = m_inStream.read();
		if (c < 0) {
			return null;
		}
    	int nReady = m_inStream.available();
       	if (nReady < 0) {
           	nReady = 0;
        }
        byte[] buff = new byte[1+nReady];
        buff[0] = (byte)c;
        int n = (nReady > 0) ? 1 + m_inStream.read(buff, 1, buff.length-1) : 1;
        return new Chunk(n, buff);
	}
}
