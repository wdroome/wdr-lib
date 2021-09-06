package com.wdroome.osc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import com.wdroome.json.JSONLexan;
import com.wdroome.json.JSONParseException;
import com.wdroome.json.JSONParser;
import com.wdroome.json.JSONUtil;
import com.wdroome.json.JSONValue;
import com.wdroome.json.JSONValueTypeException;
import com.wdroome.json.JSONValue_Object;
import com.wdroome.json.JSONValue_Array;
import com.wdroome.json.JSONValue_String;

import com.wdroome.util.MiscUtil;

/**
 * Manage a connection to the QLab server.
 * @author wdr
 */
public class OSCConnection
{
	/**
	 * Callback for response messages.
	 * @author wdr
	 */
	public interface ResponseHandler
	{
		/**
		 * Called when a response message arrives from the OSC target.
		 * @param msg The message: [0] is the method, [1] gives the argument types,
		 * 		and [2:end] are the typed arguments. If there are no arguments,
		 * 		the length is 1.
		 */
		void handleOscResponse(List<Object> msg);
	}
	
	public static final int DEF_CONNECT_TIMEOUT = 30 * 1000;

	private final InetSocketAddress m_oscIpAddr;
	
	// Set by connect():
	private	Listener m_listener = null;
	private Socket m_oscSocket = null;
	private OutputStream m_oscOutputStream;
	private InputStream m_oscInputStream;

	private ResponseHandler m_responseHandler = null;

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
		System.out.println("XXX connect " + addr + " " + port + " " + m_oscIpAddr);
	}
	
	/**
	 * Create a TP connection to the OSC server.
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
		
		System.out.println("XXX connecting to " + m_oscIpAddr);
		m_oscSocket.connect(m_oscIpAddr, m_connectTimeout);
		try {
			m_oscOutputStream = m_oscSocket.getOutputStream();
			m_oscInputStream = m_oscSocket.getInputStream();
		} catch (IOException e) {
			try { disconnect(); } catch (Exception ex) {}
			throw e;
		}
		System.out.println("XXX connected to " + m_oscIpAddr);
		
		m_listener = new Listener();
	}

	/**
	 * Disconnect from the QLab server.
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
	 * Send a request to the OSC server and return.
	 * @param req The request.
	 * @throws IOException
	 * 		If an error occurs while sending the request.
	 * 		In this case, we will NOT call one of reply() methods in req.
	 * @throws IllegalArgumentException
	 * 		Internal error: this method does not recognize req's argument type.
	 */
	public void sendRequest(OSCRequest req) throws IOException
	{
		String method = req.getMethod();
		List<byte[]> byteArrays = req.getOSCBytes(null);
		if (false) {
			System.out.println("XXX: Sending:"); hexDump(byteArrays);
		}
		synchronized (m_oscOutputStream) {
			try {
				OSCUtil.writeSlipMsg(m_oscOutputStream, byteArrays);
			} catch (IOException e) {
				throw e;
			}
		}
	}
	
	/**
	 * Set the callback for asynchronous response messages from the OSC server.
	 * There can only be one update handler;
	 * setting a new one removes the previous one.
	 * Furthermore, you must set the update handler BEFORE calling {@link #connect()}.
	 * @param updateHandler The new update handler, or null.
	 * @throws IllegalStateException
	 * 		If we are currently connected to a server.
	 */
	public void setResponseHandler(ResponseHandler responseHandler)
	{
		if (m_listener != null) {
			throw new IllegalStateException("OSCConnection.setResponseHandler(): already connected");
		}
		m_responseHandler = responseHandler;
	}
	
	/**
	 * Return IP address of QLab server.
	 * @return IP address of QLab server.
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
		logError("OSCConnection: " + err);
	}
	
	/**
	 * A daemon thread that listens for messages from the OSC server.
	 */
	private class Listener extends Thread
	{
		private boolean m_running = true;
		private final InputStream m_listenerInputStream;
		
		private Listener()
		{
			setDaemon(true);
			setName("OSCConnection.Listener-" + m_oscIpAddr);
			m_listenerInputStream = m_oscInputStream;
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
				ArrayList<Object> msg = readOscMessage();
				if (msg == null) {
					break;
				}
				if (m_responseHandler != null) {
					m_responseHandler.handleOscResponse(msg);
				}
			}
		}
		
		/**
		 * Read and return the next message sent by the OSC server
		 * .
		 * @return
		 * 		An ArrayList with the next message.
		 * 		The first element is the String method, and is never null.
		 * 		The remaining elements are the typed arguments.
		 * 		Return null on EOF or permanent I/O error.
		 */
		private ArrayList<Object> readOscMessage()
		{
			List<Byte> reply;
			try {
				synchronized (m_listenerInputStream) {
					reply = OSCUtil.readSlipMsg(m_listenerInputStream);
				}
			} catch (IOException e) {
				if (m_running) {
					logError("Listener: read IOException: " + e);
				}
				return null;
			}
			if (reply == null) {
				return null;
			}
			ArrayList<Object> ret = new ArrayList<>();
			Iterator<Byte> iter = reply.iterator();
			ret.add(OSCUtil.getOSCString(iter));
			String argFmt = OSCUtil.getOSCString(iter);
			int argCnt = argFmt.length();
			for (int iArg = 0; iArg < argCnt; iArg++) {
				char c = argFmt.charAt(iArg);
				if (iArg == 0 && c == OSCUtil.OSC_ARG_FMT_HEADER.charAt(0)) {
					continue;
				} else if (c == OSCUtil.OSC_STR_ARG_FMT.charAt(0)) {
					ret.add(OSCUtil.getOSCString(iter));
				} else if (c == OSCUtil.OSC_INT32_ARG_FMT.charAt(0)) {
					ret.add(OSCUtil.getOSCInt32(iter));
				} else if (c == OSCUtil.OSC_FLOAT_ARG_FMT.charAt(0)) {
					ret.add(OSCUtil.getOSCFloat32(iter));
				} else if (c == OSCUtil.OSC_INT64_ARG_FMT.charAt(0)) {
					ret.add(OSCUtil.getOSCInt64(iter));
				} else if (c == OSCUtil.OSC_BLOB_ARG_FMT.charAt(0)) {
					ret.add(OSCUtil.getOSCBlob(iter));
				} else if (c == OSCUtil.OSC_TIME_TAG_ARG_FMT.charAt(0)) {
					ret.add(OSCUtil.getOSCInt64(iter));
				} else {
					logError("Listener: unexpected OSC arg format '" + c
									+ "' in '" + argFmt + "'");
				}
			}
			return ret;
		}
	
		private void shutdown()
		{
			m_running = false;
			interrupt();
		}
	};
	
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
