package com.wdroome.osc.qlab;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.wdroome.osc.OSCConnection;
import com.wdroome.osc.OSCMessage;
import com.wdroome.osc.OSCUtil;
import com.wdroome.json.JSONParseException;
import com.wdroome.json.JSONValue_String;
import com.wdroome.json.JSONValueTypeException;

public class QueryQLab extends OSCConnection
{
	private long m_timeoutMS = 2500;

	/**
	 * Create connection to query an EOS server.
	 * @param addr The server's inet socket address.
	 */
	public QueryQLab(InetSocketAddress addr)
	{
		super(addr);
	}

	/**
	 * Create connection to query an EOS server.
	 * @param addr The server's inet address.
	 * @param port The server's port.
	 */
	public QueryQLab(String addr, int port) throws IllegalArgumentException
	{
		super(addr, port);
	}

	/**
	 * Create connection to query an EOS server.
	 * @param addr An ipaddr:port string with the server's inet socket address.
	 */
	public QueryQLab(String addrPort) throws IllegalArgumentException
	{
		super(addrPort);
	}
	
	public long getTimeoutMS() {
		return m_timeoutMS;
	}

	public void setTimeoutMS(long timeoutMS) {
		this.m_timeoutMS = timeoutMS;
	}
	
	@Override
	public void connect() throws IOException
	{
		super.connect();
		sendMessage(new OSCMessage(QLabUtil.ALWAYS_REPLY_REQ, new Object[] {"1"}));
	}
	
	/**
	 * Send a request to QLab and return the reply.
	 * @param requestMethod The request method.
	 * @param args The argument. May be null.
	 * @return Information about the reply.
	 * @throws IOException If an IO error occurs.
	 */
	public QLabReply sendQLabReq(String requestMethod, Object[] args) throws IOException
	{
		if (!isConnected()) {
			connect();
		}
		OSCMessage req = new OSCMessage(requestMethod, args);
		final ArrayBlockingQueue<OSCMessage> replyQueue = new ArrayBlockingQueue<>(10);
		ReplyHandler replyHandler;
		replyHandler = sendMessage(req,
					QLabUtil.REPLY_PREFIX + QLabUtil.WORKSPACE_REPLY_PREFIX_PAT + requestMethod,
					replyQueue);
		OSCMessage replyMsg = null;
		try {
			replyMsg = replyQueue.poll(m_timeoutMS, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			// Usually this is timeout on the poll().
		}
		dropReplyHandler(replyHandler);
		if (replyMsg == null) {
			return null;
		}
		try {
			return new QLabReply(replyMsg);
		} catch (JSONParseException e) {
			logError("QueryQLab.send(" + requestMethod + "): Invalid JSON response "
						+ e + " " + replyMsg);
			return null;
		} catch (JSONValueTypeException e) {
			logError("QueryQLab.send(" + requestMethod + "): Invalid JSON response "
					+ e + " " + replyMsg);
			return null;
		}
	}

	/**
	 * Send a request to QLab and return the reply.
	 * @param requestMethod The request method.
	 * @return Information about the reply.
	 * @throws IOException If an IO error occurs.
	 */
	public QLabReply sendQLabReq(String requestMethod) throws IOException
	{
		return sendQLabReq(requestMethod, null);
	}

	/**
	 * Test if the server is really QLab.
	 * @return True iff the server is QLab.
	 */
	public boolean isQLab()
	{
		try {
			String version = getVersion();
			return version != null && !version.equals("");
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Get the QLab version.
	 * @return The QLab version.
	 * @throws IOException If an IO error occurs.
	 */
	public String getVersion() throws IOException
	{
		QLabReply reply = sendQLabReq(QLabUtil.VERSION_REQ);
		if (reply != null && reply.isOk() && reply.m_rawData instanceof JSONValue_String) {
			return ((JSONValue_String)reply.m_rawData).m_value;
		}
		return "";
	}
	
	public List<QLabCue> getAllCueLists() throws IOException
	{
		QLabReply reply = sendQLabReq(QLabUtil.CUELISTS_REQ);
		if (reply.isOk() && reply.m_dataArr != null) {
			return QLabCue.getCueArray(reply.m_dataArr, null);
		} else {
			return null;
		}
	}
	
	private boolean m_printAllMsgs = false;	// XXX
	
	@Override
	protected void logMsgSent(OSCMessage msg)
	{
		if (m_printAllMsgs) {
			System.err.println("QueryQLab: sending " + msg);
		}
	}
	
	@Override
	protected void logMsgReceived(OSCMessage msg)
	{
		if (m_printAllMsgs) {
			System.err.println("QueryQLab: recvd " + msg);
		}
	}

	public static void main(String[] args) throws IOException
	{
		try (QueryQLab queryQLab = new QueryQLab(args[0])) {
			System.out.println("Version: " + queryQLab.getVersion());
			List<QLabCue> allCues = queryQLab.getAllCueLists();
			for (QLabCue cue: allCues) {
				System.out.println();
				cue.printCue(System.out, "", "   ");
			}
		} catch (IllegalArgumentException e) {
			System.err.println(e);
		}
	}
}