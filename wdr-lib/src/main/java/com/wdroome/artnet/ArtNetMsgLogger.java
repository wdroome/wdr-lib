package com.wdroome.artnet;

import java.net.InetSocketAddress;

import com.wdroome.util.CircularBuffer;
import com.wdroome.util.inet.InetUtil;

import com.wdroome.artnet.msgs.ArtNetMsg;

/**
 * Log ArtNet messages sent and received in a circular buffer.
 * This is a singleton class, logging all messages for all ArtNetChannels,
 * and is thread-safe.
 * This class is primarily for debugging; time & space efficiency are not a concern.
 * Art-Net (TM) Designed by and Copyright Artistic License Holdings Ltd.
 * @author wdr
 */

public class ArtNetMsgLogger
{
	public static final int DEF_BUFFER_SIZE = 5000;
	
	public static final ArtNetMsgLogger g_msgLogger = new ArtNetMsgLogger(DEF_BUFFER_SIZE);
	public static final long g_startTS = System.currentTimeMillis();
	
	private CircularBuffer<MsgEvent> m_msgBuffer;
	
	/**
	 * Create a message logger. Normally only one.
	 */
	private ArtNetMsgLogger(int bufferSize)
	{
		m_msgBuffer = new CircularBuffer<MsgEvent>(bufferSize);
	}
	
	/**
	 * Add an event to the end of the buffer. If buffer is full, discard oldest entry.
	 * @param msgEvent The event.
	 */
	public synchronized void addEvent(MsgEvent msgEvent)
	{
		m_msgBuffer.add(msgEvent);
	}
	
	public synchronized int size()
	{
		return m_msgBuffer.size();
	}
	
	public synchronized MsgEvent[] getEvents()
	{
		return m_msgBuffer.toArray(new MsgEvent[m_msgBuffer.size()]);
	}
	
	public synchronized MsgEvent[] getAndClear()
	{
		MsgEvent[] events = m_msgBuffer.toArray(new MsgEvent[m_msgBuffer.size()]);
		m_msgBuffer.clear();
		return events;
	}

	public synchronized void clear()
	{
		m_msgBuffer.clear();
	}
	
	public static abstract class MsgEvent
	{
		public final long m_timeMS;
		public final InetSocketAddress m_toAddr;
		public final InetSocketAddress m_fromAddr;
		
		private MsgEvent(long timeMS, InetSocketAddress toAddr, InetSocketAddress fromAddr)
		{
			if (timeMS == 0) {
				timeMS = System.currentTimeMillis();
			}
			m_timeMS = timeMS;
			m_toAddr = toAddr;
			m_fromAddr = fromAddr;
		}
		
		public String baseToString(String type)
		{
			return type + "@" + String.format("%.3f", (m_timeMS-g_startTS)/1000.0)
					+ ":to=" + InetUtil.toAddrPort(m_toAddr)
					+ ",from=" + InetUtil.toAddrPort(m_fromAddr);
		}
	}
	
	public static class SendEvent extends MsgEvent
	{
		public final ArtNetMsg m_msg;
		
		public SendEvent(ArtNetMsg msg, InetSocketAddress toAddr, InetSocketAddress fromAddr)
		{
			super(0, toAddr, fromAddr);
			m_msg = msg;
		}
		
		@Override
		public String toString()
		{
			return baseToString("Send") + " msg:"
					+ "\n  " + m_msg.toString() + "\n";
					//  + m_msg.toFmtString(null, "\n  ");
		}
	}
	
	public static class RcvEvent extends MsgEvent
	{
		public final ArtNetMsg m_msg;
		
		public RcvEvent(ArtNetMsg msg, InetSocketAddress toAddr, InetSocketAddress fromAddr)
		{
			super(0, toAddr, fromAddr);
			m_msg = msg;
		}
		
		@Override
		public String toString()
		{
			return baseToString("Rcv") + " msg:"
					+ "\n  " + m_msg.toString() + "\n";
					// + m_msg.toFmtString(null, "\n  ");
		}
	}
	
	public static class RcvUnsupportedOpcode extends MsgEvent
	{
		public final ArtNetOpcode m_opCode;
		
		public RcvUnsupportedOpcode(ArtNetOpcode opCode, InetSocketAddress toAddr, InetSocketAddress fromAddr)
		{
			super(0, toAddr, fromAddr);
			m_opCode = opCode;
		}
		
		@Override
		public String toString()
		{
			return baseToString("UnsupOpcode") + " op=" + m_opCode;
		}
	}
	
	public static class RcvUnknownBytes extends MsgEvent
	{
		public final int m_len;
		public final byte[] m_buff;
		
		public RcvUnknownBytes(byte[] buff, int len, InetSocketAddress toAddr, InetSocketAddress fromAddr)
		{
			super(0, toAddr, fromAddr);
			m_len = len;
			m_buff = buff;
		}
		
		@Override
		public String toString()
		{
			return baseToString("UnknownMsg") + " len=" + m_len;
		}
	}
}
