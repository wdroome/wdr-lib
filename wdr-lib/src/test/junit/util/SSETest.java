package test.junit.util;

import static org.junit.Assert.*;

import org.junit.Test;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import com.wdroome.util.SSEEvent;
import com.wdroome.util.SSESender;
import com.wdroome.util.SSEReader;
import com.wdroome.util.ArrayToList;

/**
 * @author wdr
 */
public class SSETest
{
	private SSEEvent[] m_events1 = new SSEEvent[] {
		new SSEEvent("event 0", "id 0", null),
		new SSEEvent("event 1", null,
				new ArrayToList<String>(new String[] {"data item 1a", "data item 1b"})),
		new SSEEvent("event 2", null,
				new ArrayToList<String>(new String[] {"data item 2a", "data item 2b", "data item 2c"})),
		new SSEEvent("event 3", null, null),
	};
	
	private SSEEvent[] m_events3 = new SSEEvent[] {
		new SSEEvent("event 2a", null,
				new ArrayToList<String>(new String[] {"data item 3a xxx", "data item 3b yyyy", "data item 3c zzzzz"})),
		new SSEEvent("event 2b", null,
				new ArrayToList<String>(new String[] {"data item 3a xxx data item 3b yyyy data item 3c zzzzz"})),
	};
	
	private static class EventHandler implements SSEReader.EventCB
	{
		private final String m_name;
		private final SSEEvent[] m_expectedEvents;
		private int m_nevents = 0;
		private boolean m_gotEOF = false;
		
		public EventHandler(String name, SSEEvent[] expectedEvents)
		{
			m_name = name;
			m_expectedEvents = expectedEvents;
		}
		
		@Override
		public void newSSE(SSEEvent event)
		{
			if (m_nevents > m_expectedEvents.length) {
				fail(m_name + ": Too many events received");
			}
			SSEEvent expected = m_expectedEvents[m_nevents];
			assertEquals(m_name + ": Event " + m_nevents + " event", expected.m_event, event.m_event);
			assertEquals(m_name + ": Event " + m_nevents + " id", expected.m_id, event.m_id);
			assertEquals(m_name + ": Event " + m_nevents + " data", expected.m_dataLines, event.m_dataLines);
			m_nevents++;
		}

		@Override
		public void eofSSE()
		{
			m_gotEOF = true;
			if (m_nevents != m_expectedEvents.length) {
				fail(m_name + ": Unexpected EOF after " + m_nevents + " events");
			}
		}

		@Override
		public void errorSSE(IOException e)
		{
			System.err.println(m_name + ": SSE Error: " + e);
		}
		
		public void verifyEOF()
		{
			if (!m_gotEOF) {
				fail(m_name + ": Failed to get EOF; only got " + m_nevents + " events");
			}
		}
	}

	@Test
	public void test1() throws IOException
	{
		PipedOutputStream pout = new PipedOutputStream();
		PipedInputStream pin = new PipedInputStream(pout);
		
		EventHandler handler = new EventHandler("test1", m_events1);
		new SSEReader(pin, handler);
		SSESender sender = new SSESender(pout);
		for (SSEEvent event: m_events1) {
			sender.sendEvent(event);
			try { Thread.sleep(2000); } catch (Exception e) {}
		}
		pout.close();
		try { Thread.sleep(2000); } catch (Exception e) {}
		handler.verifyEOF();
	}

	@Test
	public void test2() throws IOException
	{
		PipedOutputStream pout = new PipedOutputStream();
		PipedInputStream pin = new PipedInputStream(pout);
		
		EventHandler handler = new EventHandler("test2", m_events1);
		new SSEReader(pin, handler);
		SSESender sender = new SSESender(pout);
		for (SSEEvent event: m_events1) {
			if (!event.m_dataLines.isEmpty()) {
				StringBuilder b = new StringBuilder();
				for (String line: event.m_dataLines) {
					b.append(line);
					b.append("\n");
				}
				sender.bufferData(b.toString());
			}
			sender.sendEvent(event.m_event, event.m_id);
			try { Thread.sleep(2000); } catch (Exception e) {}
		}
		pout.close();
		try { Thread.sleep(2000); } catch (Exception e) {}
		handler.verifyEOF();
	}

	@Test
	public void test3() throws IOException
	{
		PipedOutputStream pout = new PipedOutputStream();
		PipedInputStream pin = new PipedInputStream(pout);
		
		EventHandler handler = new EventHandler("test3", m_events3);
		new SSEReader(pin, handler);
		SSESender sender = new SSESender(pout);
		for (SSEEvent event: m_events3) {
			if (!event.m_dataLines.isEmpty()) {
				StringBuilder b = new StringBuilder();
				for (String line: event.m_dataLines) {
					b.append(line);
					b.append("\n");
				}
				// sender.bufferData(b.toString());
				byte[] bytes = b.toString().getBytes();
				int sendLen = 5;
				for (int i = 0; i < bytes.length; i+= sendLen) {
					sender.streamDataBlock(bytes, i, Math.min(sendLen, (bytes.length - i)));
				}
				sender.streamDataEnd();
			}
			sender.sendEvent(event.m_event, event.m_id);
			try { Thread.sleep(2000); } catch (Exception e) {}
		}
		pout.close();
		try { Thread.sleep(2000); } catch (Exception e) {}
		handler.verifyEOF();
	}
}
