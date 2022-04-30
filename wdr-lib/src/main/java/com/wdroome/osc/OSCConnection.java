package com.wdroome.osc;

import java.io.IOException;
import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Iterator;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Predicate;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Manage a connection to an OSC server.
 * This only supports OSC version 1.1, via TCP and SLIP.
 * @author wdr
 */
public class OSCConnection implements Closeable
{
	/**
	 * Callback for response messages.
	 * This is a legacy; new code should implement a Consumer&lt;OSCMessage&gt;.
	 * @author wdr
	 */
	public interface MessageHandler
	{
		/**
		 * Called when a response message arrives from the OSC target.
		 * @param msg The message. If null, the OSC server disconnected
		 * 			or an unrecoverable error occurred,
		 * 			and no more messages will be arriving.
		 */
		void handleOscResponse(OSCMessage msg);
	}
	
	/**
	 * An opaque class representing a handler for reply messages
	 * that match a pattern. When the client declares a reply handler,
	 * the base class creates an instance and returns it to the client.
	 * The client can use that instance to cancel the reply handler.
	 * @author wdr
	 */
	public class ReplyHandler
	{
		private final Pattern m_methodPattern;
		private final Predicate<OSCMessage> m_replyHandler;
		
		private ReplyHandler(String methodRegex, Predicate<OSCMessage> replyHandler)
		{
			m_methodPattern = Pattern.compile(methodRegex);
			m_replyHandler = replyHandler;
		}
		
		@Override
		public String toString()
		{
			return "OSCConnection.ReplyHandler(" + m_methodPattern.toString() + ")";
		}
	}

	public static final int DEF_CONNECT_TIMEOUT = 30 * 1000;

	private final InetSocketAddress m_oscIpAddr;
	
	// Set by connect():
	private	Listener m_listener = null;
	private Socket m_oscSocket = null;
	private OutputStream m_oscOutputStream;
	private InputStream m_oscInputStream;	// Shared with Listener thread. Non-Listener methods
											// only use this when Listener isn't active.

	private MessageHandler m_messageHandler = null;
	private Consumer<OSCMessage> m_messageConsumer = null;
	
	// m_replyHandlers is shared by the Listener thread and the clients.
	// Must synchronize for all access.
	private final List<ReplyHandler> m_replyHandlers = new LinkedList<ReplyHandler>();

	private int m_connectTimeout = DEF_CONNECT_TIMEOUT;
	
	/**
	 * Create a new connection to OSC server.
	 * Note: You must also call {@link #connect()} to establish the connection.
	 * @param addr
	 * 		Server's IP address or host name.
	 * @param port
	 * 		Server's port.
	 * @throws IllegalArgumentException
	 * 		If addr is an invalid IP address or an unknown host name.
	 */
	public OSCConnection(String addr, int port) throws IllegalArgumentException
	{
		if (port <= 0) {
			throw new IllegalArgumentException("OSCConnection: invalid port '" + port + "'");
		}
		m_oscIpAddr = new InetSocketAddress(addr, port);
		if (m_oscIpAddr.isUnresolved()) {
			throw new IllegalArgumentException("OSCConnection: invalid or unknown addr '"
						+ addr + "'");
		}
	}
	
	/**
	 * Create a new connection to OSC server.
	 * Note: You must also call {@link #connect()} to establish the connection.
	 * @param addrPort
	 * 		Server's IP address or host name and port, as addr:port.
	 * @throws IllegalArgumentException
	 * 		If addr is an invalid IP address or an unknown host name.
	 */
	public OSCConnection(String addrPort) throws IllegalArgumentException
	{
		if (addrPort == null || addrPort.isBlank()) {
			throw new IllegalArgumentException("OSCConnection: invalid addr:port '"
						+ addrPort + "'");
		}
		int iColon = addrPort.indexOf(':');
		if (iColon <= 0) {
			throw new IllegalArgumentException("OSCConnection: invalid addr:port '"
						+ addrPort + "'");			
		}
		String addr;
		int port;
		try {
			port = Integer.parseInt(addrPort.substring(iColon+1));
			addr = addrPort.substring(0, iColon);
		} catch (Exception e) {
			throw new IllegalArgumentException("OSCConnection: invalid addr:port '" + addrPort + "'");
		}
		m_oscIpAddr = new InetSocketAddress(addr, port);
		if (m_oscIpAddr.isUnresolved()) {
			throw new IllegalArgumentException("OSCConnection: invalid or unknown addr '" + addr + "'");
		}
	}
	
	/**
	 * Create a new connection to OSC server.
	 * Note: You must also call {@link #connect()} to establish the connection.
	 * @param addr The server's internet address.
	 * @param port The server's port.
	 */
	public OSCConnection(InetAddress addr, int port)
	{
		m_oscIpAddr = new InetSocketAddress(addr, port);
		if (m_oscIpAddr.isUnresolved()) {
			throw new IllegalArgumentException("OSCConnection: invalid or unknown addr '" + addr + "'");
		}		
	}
	
	/**
	 * Create a new connection to OSC server.
	 * Note: You must also call {@link #connect()} to establish the connection.
	 * @param addr The server's internet address & port.
	 */
	public OSCConnection(InetSocketAddress addr)
	{
		m_oscIpAddr = addr;
		if (m_oscIpAddr.isUnresolved()) {
			throw new IllegalArgumentException("OSCConnection: invalid or unknown addr '" + addr + "'");
		}		
	}
	
	/**
	 * Open a TCP connection to the OSC server.
	 * @throws IOException
	 * 		If we cannot connect to OSC server,
	 * 		or the connection was refused, etc.
	 */
	public void connect() throws IOException
	{
		if (m_listener != null) {
			throw new IllegalStateException("OSCConnection.connect(): already connected");
		}
		
		m_oscSocket = new Socket();
		m_oscSocket.setSoTimeout(0);
		
		// System.out.println("XXX connecting to " + m_oscIpAddr);
		m_oscSocket.connect(m_oscIpAddr, m_connectTimeout);
		try {
			m_oscOutputStream = m_oscSocket.getOutputStream();
			m_oscInputStream = m_oscSocket.getInputStream();
		} catch (IOException e) {
			try { disconnect(); } catch (Exception ex) {}
			throw e;
		}
		// System.out.println("XXX connected to " + m_oscIpAddr);
		
		m_listener = new Listener();
	}

	/**
	 * Disconnect from the OSC server. Same as {@link #close()}.
	 */
	public void disconnect()
	{
		if (m_listener != null) {
			m_listener.shutdown();
			m_listener = null;
		}
		if (m_oscSocket != null) {
			try { m_oscOutputStream.close(); } catch (Exception e) {}
			try { m_oscInputStream.close(); } catch (Exception e) {}
			try { m_oscSocket.close(); } catch (Exception e) {}
			m_oscSocket = null;
			m_oscOutputStream = null;
			m_oscInputStream = null;
		}
	}
	
	/**
	 * Close the connection to the OSC server. Same as {@link #disconnect()}.
	 */
	@Override
	public void close()
	{
		disconnect();
	}
	
	/**
	 * Test if we are connected.
	 * @return True iff we are connected to the OSC server.
	 */
	public boolean isConnected()
	{
		return m_listener != null;
	}
	
	/**
	 * Send a message to the OSC server and return.
	 * @param msg The message.
	 * @throws IOException
	 * 		If an error occurs while sending the message.
	 */
	public void sendMessage(OSCMessage msg) throws IOException
	{
		List<byte[]> byteArrays = msg.getOSCBytes(null);
		synchronized (m_oscOutputStream) {
			try {
				OSCUtil.writeSlipMsg(m_oscOutputStream, byteArrays);
			} catch (IOException e) {
				throw e;
			}
		}
	}
	
	/**
	 * Send a message to the server and call a handler on the reply messages sent by the server,
	 * @param msg The message to send.
	 * @param replyMethodRegex A regular expression that matches the method
	 * 			in the reply messages expected for "msg".
	 * @param handler The handler. If it returns false, remove the handler for that regex
	 * 		(e.g., the server will not send any more replies for this request.
	 * @return A ReplyHandler object for the registered reply handler.
	 * 		The caller can use this to drop the reply handler if needed.
	 * @throws IOException
	 * 		If an error occurs while sending the message.
	 */
	public ReplyHandler sendMessage(OSCMessage msg, String replyMethodRegex, Predicate<OSCMessage> handler)
			throws IOException
	{
		ReplyHandler replyHandler = addReplyHandler(replyMethodRegex, handler);
		try {
			sendMessage(msg);
			return replyHandler;
		} catch (IOException e) {
			dropReplyHandler(replyHandler);
			throw e;
		}
	}
	
	/**
	 * Set the callback for asynchronous response messages from the OSC server.
	 * There can only be one handler;
	 * setting a new one removes the previous one.
	 * Furthermore, you must set the handler BEFORE calling {@link #connect()}.
	 * This is deprecated; use (@link {@link #setMessageConsumer(Consumer) instead.
	 * @param messageHandler The new update handler, or null.
	 * @throws IllegalStateException
	 * 		If we are currently connected to a server.
	 */
	public void setMessageHandler(MessageHandler messageHandler)
	{
		if (m_listener != null) {
			throw new IllegalStateException("OSCConnection.setMessageHandler(): already connected");
		}
		m_messageHandler = messageHandler;
		m_messageConsumer = null;
	}
	
	/**
	 * Set the callback for asynchronous response messages from the OSC server.
	 * These are messages that do NOT match a registered reply handler.
	 * There can only be one handler;
	 * setting a new one removes the previous one.
	 * Furthermore, you must set the handler BEFORE calling {@link #connect()}.
	 * @param messageConsumer The new response handler, or null.
	 * 		The argument is the incoming message. If null, a fatal error occurred
	 * 		and the class disconnected from the OSC server.
	 * 		No more messages will arrive.
	 * @throws IllegalStateException
	 * 		If we are currently connected to a server.
	 */
	public void setMessageConsumer(Consumer<OSCMessage> messageConsumer)
	{
		if (m_listener != null) {
			throw new IllegalStateException("OSCConnection.setMessageCosumer(): already connected");
		}
		m_messageConsumer = messageConsumer;
		m_messageHandler = null;
	}
	
	/**
	 * Register a new reply handler.
	 * @param methodRegex A regular expression. The handler will be called for all incoming messages
	 * 		whose method matches this regex.
	 * @param replyHandler The reply handler code.
	 * 		If it returns true, the reply handler will remain active and will be called
	 * 		if another response matches the regex. If it returns false, the client does not
	 * 		expect any more responses that match this regex, and the handler will be removed.
	 * 		NOTE: If the client adds multiple handlers for the same regex, they will all
	 * 		be called when a matching message arrives.
	 * @return An opaque object which the client can use to remove this handler
	 * 		(see {@link #dropReplyHandler(ReplyHandler)}).
	 */
	public ReplyHandler addReplyHandler(String methodRegex, Predicate<OSCMessage> replyHandler)
	{
		ReplyHandler rep = new ReplyHandler(methodRegex, replyHandler);
		synchronized (m_replyHandlers) {
			m_replyHandlers.add(rep);
		}
		return rep;
	}
	
	/**
	 * Remove a previously registered reply handler.
	 * @param replyHandler The replay handler to remove.
	 * @return True if the reply handler was removed. False means it had already been removed.
	 */
	public boolean dropReplyHandler(ReplyHandler replyHandler)
	{
		synchronized (m_replyHandlers) {
			return m_replyHandlers.remove(replyHandler);
		}
	}
	
	/**
	 * Return IP address of OSC server.
	 * @return IP address of OSC server.
	 */
	public InetSocketAddress getIpAddress()
	{
		return m_oscIpAddr;
	}

	/**
	 * Return the connect timeout.
	 * @return The connect timeout (in milliseconds).
	 */
	public int getConnectTimeout()
	{
		return m_connectTimeout;
	}

	/**
	 * Set the connect timeout.
	 * You must set the timeout BEFORE calling {@link #connect()}.
	 * @param connectTimeout The new connect timeout (in milliseconds). 0 means no timeout.
	 * @throws IllegalStateException
	 * 		If called after the connection has been established.
	 */
	public void setConnectTimeout(int connectTimeout)
	{
		if (m_listener != null) {
			throw new IllegalStateException(
						"OSCConnection.setConnectTimeout(): Must call before connecting");
		}
		if (connectTimeout > 0) {
			m_connectTimeout = connectTimeout;
		} else {
			m_connectTimeout = 0;
		}
	}
	
	/**
	 * Called to print an error message. Child class may override.
	 * The base class prints the message on std err.
	 * @param err The error message.
	 */
	public void logError(String err)
	{
		System.err.println("OSCConnection: " + err);
	}
	
	/**
	 * A daemon thread that listens for messages from the OSC server.
	 * There is only one Listener per instance.
	 * Note that this thread uses m_oscInputStream in the main instance
	 * without synchronization.
	 */
	private class Listener extends Thread
	{
		private boolean m_running = true;
		
		private Listener()
		{
			setDaemon(true);
			setName("OSCConnection.Listener-" + m_oscIpAddr);
			start();
		}
	
		/**
		 * Listen for messages from the OSC server, and when one arrives,
		 * call the appropriate handler method.
		 */
		@Override
		public void run()
		{
			while (m_running) {
				ArrayList<ReplyHandler> matchingHandlers = new ArrayList<>();
				try {
					matchingHandlers.clear();
					OSCMessage msg = readOscMessage();
					// Save matching reply handlers, but do NOT call the handler in the synch block.
					synchronized (m_replyHandlers) {
						for (ReplyHandler replyHandler: m_replyHandlers) {
							if (replyHandler.m_methodPattern.matcher(msg.getMethod()).matches()) {
								matchingHandlers.add(replyHandler);
							}
						}
					}
					// Now call the matching reply handlers. If none, call the backup handler.
					if (!matchingHandlers.isEmpty()) {
						for (ReplyHandler replyHandler: matchingHandlers) {
							boolean keep;
							try {
								keep = replyHandler.m_replyHandler.test(msg);
							} catch (Exception e) {
								logError("Exception in ReplyHandler for \"" + replyHandler.m_methodPattern
											+ "\": " + e);
								keep = false;
							}
							if (!keep) {
								dropReplyHandler(replyHandler);
							}
						}
					} else if (m_messageConsumer != null) {
						m_messageConsumer.accept(msg);
					} else if (m_messageHandler != null) {
						m_messageHandler.handleOscResponse(msg);
					}
					if (msg == null) {
						break;
					}
				} catch (Exception e) {
					logError("Error parsing OSC response message: " + e);
				}
			}
		}
		
		private OSCMessage readOscMessage()
		{
			List<Byte> reply;
			try {
				reply = OSCUtil.readSlipMsg(m_oscInputStream);
			} catch (IOException e) {
				if (m_running) {
					logError("Listener: read IOException: " + e);
				}
				return null;
			}
			if (reply == null || reply.isEmpty()) {
				return null;
			}
			return new OSCMessage(reply.iterator(), (msg) -> logError(msg));
		}
	
		private void shutdown()
		{
			m_running = false;
			interrupt();
		}
	}
	
	private static void hexDump(byte[] bytes)
	{
		hexDump(bytes, 0, bytes.length);
	}
	
	private static void hexDump(List<byte[]> byteArrays)
	{
		for (byte[] bytes: byteArrays) {
			hexDump(bytes);
		}
	}

	private static void hexDump(byte[] bytes, int iStart, int iEnd)
	{
		for (int i = iStart; i < iEnd; i++) {
			if (i > 0 && i%4 == 0) {
				System.out.println();
			}
			byte c = bytes[i];
			System.out.print("  0x" + Integer.toHexString(c & 0xff));
			if (c >= 0x20 && c <= 0x7e) {
				System.out.print("/('" + (char)c + "')");
			}
		}
		if (iEnd > iStart) {
			System.out.println();
		}
	}
}
