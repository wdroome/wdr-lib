package com.wdroome.osc.qlab;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.wdroome.osc.OSCConnection;
import com.wdroome.osc.OSCMessage;
import com.wdroome.osc.OSCUtil;
import com.wdroome.osc.qlab.QLabUtil.ContinueMode;
import com.wdroome.json.JSONParseException;
import com.wdroome.json.JSONValue_String;
import com.wdroome.json.JSONValue_Array;
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
		return reply != null ? reply.getString("") : "";
	}
	
	/**
	 * Select a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @throws IOException If an IO error occurs.
	 */
	public void selectCue(String idOrNumber) throws IOException
	{
		sendQLabReq(String.format(
				QLabUtil.isCueId(idOrNumber) ? QLabUtil.SELECT_CUE_ID : QLabUtil.SELECT_CUE_NUMBER,
				idOrNumber));
	}
	
	/**
	 * Test if a cue is broken.
	 * @param idOrNumber The cue unique id or number.
	 * @return True if the cue is broken.
	 * @throws IOException If an IO error occurs.
	 */
	public boolean getIsBroken(String idOrNumber) throws IOException
	{
		QLabReply reply = sendQLabReq(QLabUtil.getCueReq(idOrNumber, QLabUtil.IS_BROKEN_CUE_REQ));
		return reply != null ? reply.getBool(false) : false;
	}
	
	/**
	 * Set the cue number for a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @param number The new number.
	 * @return True if successful.
	 * @throws IOException If an IO error occurs.
	 */
	public boolean setNumber(String idOrNumber, String number) throws IOException
	{
		QLabReply reply = sendQLabReq(QLabUtil.getCueReq(idOrNumber, QLabUtil.NUMBER_CUE_REQ),
									new Object[] {number});
		return reply != null && reply.isOk();
	}
	
	/**
	 * Get the name field for a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @return The name field for the cue.
	 * @throws IOException If an IO error occurs.
	 */
	public String getName(String idOrNumber) throws IOException
	{
		QLabReply reply = sendQLabReq(QLabUtil.getCueReq(idOrNumber, QLabUtil.NAME_CUE_REQ));
		return reply != null ? reply.getString("") : "";
	}
	
	/**
	 * Set the name field for a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @param name The new name.
	 * @return True if successful.
	 * @throws IOException If an IO error occurs.
	 */
	public boolean setName(String idOrNumber, String name) throws IOException
	{
		QLabReply reply = sendQLabReq(QLabUtil.getCueReq(idOrNumber, QLabUtil.NAME_CUE_REQ),
									new Object[] {name});
		return reply != null && reply.isOk();
	}
	
	/**
	 * Get the notes field for a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @return The notes field for the cue.
	 * @throws IOException If an IO error occurs.
	 */
	public String getNotes(String idOrNumber) throws IOException
	{
		QLabReply reply = sendQLabReq(QLabUtil.getCueReq(idOrNumber, QLabUtil.NOTES_CUE_REQ));
		return reply != null ? reply.getString("") : "";
	}
	
	/**
	 * Set the notes field for a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @param notes The new notes.
	 * @return True if successful.
	 * @throws IOException If an IO error occurs.
	 */
	public boolean setNotes(String idOrNumber, String notes) throws IOException
	{
		QLabReply reply = sendQLabReq(QLabUtil.getCueReq(idOrNumber, QLabUtil.NOTES_CUE_REQ),
									new Object[] {notes});
		return reply != null && reply.isOk();
	}
	
	/**
	 * Get the continue mode for a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @return The continue mode for the cue.
	 * @throws IOException If an IO error occurs.
	 */
	public QLabUtil.ContinueMode getContinueMode(String idOrNumber) throws IOException
	{
		QLabReply reply = sendQLabReq(QLabUtil.getCueReq(idOrNumber, QLabUtil.CONTINUE_MODE_CUE_REQ));
		return reply != null
				? QLabUtil.ContinueMode.fromQLab((int)reply.getLong(0))
				: QLabUtil.ContinueMode.NO_CONTINUE;
	}
	
	/**
	 * Set the continue mode field for a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @param mode The new mode.
	 * @return True if successful.
	 * @throws IOException If an IO error occurs.
	 */
	public boolean setContinueMode(String idOrNumber, QLabUtil.ContinueMode mode) throws IOException
	{
		QLabReply reply = sendQLabReq(QLabUtil.getCueReq(idOrNumber, QLabUtil.CONTINUE_MODE_CUE_REQ),
									new Object[] {Integer.valueOf(mode.toQLab())});
		return reply != null && reply.isOk();
	}
	
	/**
	 * Get the color name for a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @return The color name for the cue.
	 * @throws IOException If an IO error occurs.
	 */
	public QLabUtil.ColorName getColorName(String idOrNumber) throws IOException
	{
		QLabReply reply = sendQLabReq(QLabUtil.getCueReq(idOrNumber, QLabUtil.COLOR_NAME_CUE_REQ));
		return reply != null
				? QLabUtil.ColorName.fromQLab(reply.getString(""))
				: QLabUtil.ColorName.NONE;
	}
	
	/**
	 * Set the color name field for a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @param colorName The new color name.
	 * @return True if successful.
	 * @throws IOException If an IO error occurs.
	 */
	public boolean setColorName(String idOrNumber, QLabUtil.ColorName colorName) throws IOException
	{
		QLabReply reply = sendQLabReq(QLabUtil.getCueReq(idOrNumber, QLabUtil.COLOR_NAME_CUE_REQ),
									new Object[] {colorName.toQLab()});
		return reply != null && reply.isOk();
	}
	
	/**
	 * Get the flagged attribute for a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @return True if the cue is flagged.
	 * @throws IOException If an IO error occurs.
	 */
	public boolean getIsFlagged(String idOrNumber) throws IOException
	{
		QLabReply reply = sendQLabReq(QLabUtil.getCueReq(idOrNumber, QLabUtil.FLAGGED_CUE_REQ));
		return reply != null ? reply.getBool(false) : false;
	}
	
	/**
	 * Set the flagged attribute for a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @param flagged The new flagged attribute.
	 * @return True if successful.
	 * @throws IOException If an IO error occurs.
	 */
	public boolean setFlagged(String idOrNumber, boolean flagged) throws IOException
	{
		QLabReply reply = sendQLabReq(QLabUtil.getCueReq(idOrNumber, QLabUtil.FLAGGED_CUE_REQ),
									new Object[] {Integer.valueOf(flagged ? 1 : 0)});
		return reply != null && reply.isOk();
	}
	
	/**
	 * Get all cue lists and all all contained cues.
	 * @return A list of cue lists.
	 * @throws IOException If an IO error occurs.
	 */
	public List<QLabCue> getAllCueLists() throws IOException
	{
		QLabReply reply = sendQLabReq(QLabUtil.CUELISTS_REQ);
		if (reply == null) {
			return null;
		}
		return QLabCue.getCueArray(reply.getJSONArray(null), null, this);
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
