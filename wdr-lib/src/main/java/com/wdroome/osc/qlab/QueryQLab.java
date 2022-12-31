package com.wdroome.osc.qlab;

import java.io.IOException;
import java.io.PrintStream;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.wdroome.util.MiscUtil;
import com.wdroome.osc.OSCConnection;
import com.wdroome.osc.OSCMessage;
import com.wdroome.osc.OSCUtil;
import com.wdroome.json.JSONParseException;
import com.wdroome.json.JSONValue_String;
import com.wdroome.json.JSONValue_Object;
import com.wdroome.json.JSONValue_Array;
import com.wdroome.json.JSONValue_ObjectArray;
import com.wdroome.json.JSONValueTypeException;

/**
 * Manage a connection to QLab. Send commands and return responses.
 * @author wdr
 *
 */
public class QueryQLab extends OSCConnection
{
	public static final long DEF_MIN_TIME_BETWEEN_REPEAT_MSGS = 200;
	public static final int DEF_TIMEOUT = 2000;
	
	private static final String UNKNOWN_VERSION = "0.0.0";
	private static final int UNKNOWN_MAJOR_VERSION = 0;
	
	private long m_timeoutMS = DEF_TIMEOUT;
	private String m_passcode = "";
	private String m_lastReplyWorkspaceId = "";
	private int m_numDenied = 0;
	private PrintStream m_errOut = System.err;
	private String m_version = UNKNOWN_VERSION;
	private int m_majorVersion = UNKNOWN_MAJOR_VERSION;
	private String m_targetWorkspace = "";

		// Only valid for QLab 5.
	private List<QLabNetworkPatchInfo> m_networkPatchList = null;
	private long m_networkPatchListTS = 0;
	private static final long NETWORK_PATCH_LIST_REFRESH_MS = 10000;

	/**
	 * Create connection to query a QLab server.
	 * @param addr The server's inet socket address.
	 */
	public QueryQLab(InetSocketAddress addr)
	{
		super(addr);
	}

	/**
	 * Create connection to query a QLab server.
	 * @param addr The server's inet address.
	 * @param port The server's port.
	 */
	public QueryQLab(String addr, int port) throws IllegalArgumentException
	{
		super(addr, port);
		setMinTimeBetweenRepeatMsgs(DEF_MIN_TIME_BETWEEN_REPEAT_MSGS);
	}

	/**
	 * Create connection to query a QLab server.
	 * @param addr An ipaddr:port string with the server's inet socket address,
	 * 			with an optional [/passcode] suffix.
	 */
	public QueryQLab(String addrPortPasscode) throws IllegalArgumentException
	{
		super(extractAddrPort(addrPortPasscode));
		m_passcode = extractPasscode(addrPortPasscode);
		setMinTimeBetweenRepeatMsgs(DEF_MIN_TIME_BETWEEN_REPEAT_MSGS);
	}
	
	private static String extractAddrPort(String addrPortPasscode)
	{
		String[] parse = addrPortPasscode.split("/");
		return parse.length == 2 ? parse[0] : addrPortPasscode;
	}
	
	private static String extractPasscode(String addrPortPasscode)
	{
		String[] parse = addrPortPasscode.split("/");
		return parse.length == 2 ? parse[1] : "";
	}
	
	public void setPasscode(String passcode)
	{
		m_passcode = passcode != null ? passcode : "";
	}
	
	/**
	 * Create a connection to a QLab instance. Try a list of addresses,
	 * and use the first one that responds to QLab requests.
	 * @param addrPorts A list of IP addresses and ports (addr:port).
	 * @param connectTimeoutMS Timeout for establishing the connection, in milliseconds.
	 * 				If 0, use a default timeout.
	 * @return A QueryQLab for the first address that responds,
	 * 		or null if none do.
	 */
	public static QueryQLab makeQueryQLab(String[] addrPortPasscodes, int connectTimeoutMS)
	{
		if (connectTimeoutMS <= 50) {
			connectTimeoutMS = DEF_TIMEOUT;
		}
		for (String addrPortPasscode: addrPortPasscodes) {
			try {
				QueryQLab queryQLab = new QueryQLab(addrPortPasscode);
				queryQLab.setConnectTimeout(connectTimeoutMS);
				queryQLab.connect();
				if (queryQLab.isQLab()) {
					return queryQLab;
				}
			} catch (Exception e) {
				// skip, try next address.
			}
		}
		return null;
	}
	
	/**
	 * Get the workspace id for the most recent request.
	 * @return The unique id of the workspace of the most recent request.
	 */
	public String getLastReplyWorkspaceId()
	{
		return m_lastReplyWorkspaceId;
	}
	
	public long getTimeoutMS() {
		return m_timeoutMS;
	}

	public void setTimeoutMS(long timeoutMS) {
		this.m_timeoutMS = timeoutMS;
	}
	
	/**
	 * Connect to QLab. Set alwaysReply, get version, set passcode, check for permissions.
	 */
	@Override
	public void connect() throws IOException
	{
		m_numDenied = 0;
		super.connect();
		super.sendMessage(new OSCMessage(QLabUtil.ALWAYS_REPLY_REQ, new Object[] {"1"}));
		QLabReply reply;

		// Get & save QLab version.
		try {
			reply = sendQLabReqNoConn(QLabUtil.VERSION_REQ, null);
			m_version = reply != null ? reply.getString(UNKNOWN_VERSION) : UNKNOWN_VERSION;
		} catch (Exception e) {
			m_version = UNKNOWN_VERSION;
		}
		try {
			if (m_version != null && !m_version.isBlank()) {
				m_majorVersion = Integer.parseInt(m_version.replaceAll("\\..*$", ""));
			}
		} catch (Exception e) {
			m_majorVersion = UNKNOWN_MAJOR_VERSION;
		}

		// Connect if passcode is specified.
		if (!m_passcode.isEmpty()) {
			reply = sendQLabReqNoConn(QLabUtil.CONNECT_REQ, new Object[] {m_passcode});
			String connData;
			if (reply == null || (connData = reply.getString(null)) == null || !connData.startsWith("ok")) {
				logError("***** QueryQLab: " + QLabUtil.CONNECT_REQ + " failed. Check passcode.");
			}
		}
		reply = sendQLabReqNoConn(QLabUtil.BASE_PATH_REQ, null);
		
		// Check for whether we have permission to access QLab.
		reply = sendQLabReqNoConn(QLabUtil.BASE_PATH_REQ, null);
		if (reply == null || !reply.isOk()) {
			accessDeniedError(QLabUtil.BASE_PATH_REQ);
		}
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
		return sendQLabReqNoConn(requestMethod, args);
	}
	
	/**
	 * Send a request to QLab and return the reply.
	 * @param requestMethod The request method.
	 * @param args The argument. May be null.
	 * @return Information about the reply.
	 * @throws IOException If an IO error occurs.
	 */
	private QLabReply sendQLabReqNoConn(String requestMethod, Object[] args) throws IOException
	{
		OSCMessage req = new OSCMessage(requestMethod, args);
		final ArrayBlockingQueue<OSCMessage> replyQueue = new ArrayBlockingQueue<>(10);
		ReplyHandler replyHandler;
		replyHandler = sendMessage1Reply(req,
					QLabUtil.REPLY_PREFIX + QLabUtil.WORKSPACE_REPLY_PREFIX_PAT + requestMethod,
					replyQueue);
		OSCMessage replyMsg = null;
		try {
			replyMsg = replyQueue.poll(m_timeoutMS, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			// Usually this is timeout on the poll().
		}
		boolean dropped = dropReplyHandler(replyHandler);
		if (replyMsg == null) {
			return null;
		}
		try {
			QLabReply reply = new QLabReply(replyMsg);
			if (!reply.m_workspaceId.isBlank()) {
				m_lastReplyWorkspaceId = reply.m_workspaceId;
			}
			if (reply.isDenied()) {
				if (m_numDenied <= 2) {
					accessDeniedError(requestMethod);
				}
				m_numDenied++;
			}
			return reply;
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
	
	protected void accessDeniedError(String method)
	{
		logError("");
		logError("**********************************************");
		logError("**** QLab Denied Access for " + method + ".");
		logError("**** Check passcode.");
		logError("**********************************************");
		logError("");
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
			if (!isConnected()) {
				connect();
			}
			return m_version != null && !m_version.equals(UNKNOWN_VERSION);
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Get the QLab version.
	 * @return The QLab version, or UNKNOWN_VERSION if we cannot get the version.
	 * @throws IOException If an IO error occurs.
	 */
	public String getVersion()
	{
		if (!isConnected()) {
			try {
				connect();
			} catch (IOException e) {
				return UNKNOWN_VERSION;
			}
		}
		return m_version;
	}
	
	/**
	 * Return the QLab major version as an integer (eg, 4 or 5).
	 * @return The QLab major version as an integer.
	 * 		Return UNKNOWN_MAJOR_VERSION if there is no connection
	 * 		or the version cannot be determined.
	 */
	public int getMajorVersion()
	{
		if (!isConnected()) {
			try {
				connect();
			} catch (IOException e) {
				return UNKNOWN_MAJOR_VERSION;
			}
		}
		return m_majorVersion;
	}
	
	/**
	 * Test if this is QLab5 or later.
	 * @return True if this is QLab 5 or later.
	 */
	public boolean isQLab5()
	{
		return getMajorVersion() >= 5;
	}
	
	/**
	 * Get information about the workspaces in QLab.
	 * @return A list of the workspaces. List is never null, but may be empty.
	 * @throws IOException If an IO error occurs.
	 */
	public List<QLabWorkspaceInfo> getWorkspaces() throws IOException
	{
		QLabReply reply = sendQLabReq(QLabUtil.WORKSPACES_REQ);
		List<QLabWorkspaceInfo> workspaces = new ArrayList<>();
		JSONValue_ObjectArray arr = reply.getJSONObjectArray(null);
		if (arr != null) {
			for (JSONValue_Object jsonWorkspace: arr) {
				workspaces.add(new QLabWorkspaceInfo(jsonWorkspace));
			}
		}
		return workspaces;
	}
	
	/**
	 * Get information about the network patches in QLab.
	 * This only works in QLab5 and up.
	 * @return A list of the patches. List is never null, but may be empty.
	 * @throws IOException If an IO error occurs.
	 */
	public List<QLabNetworkPatchInfo> getNetworkPatches() throws IOException
	{
		if (!isQLab5()) {
			return List.of();
		}
		if (m_networkPatchList == null
					|| System.currentTimeMillis() - m_networkPatchListTS >= NETWORK_PATCH_LIST_REFRESH_MS) {
			QLabReply reply = sendQLabReq(addTargetWS(QLabUtil.NETWORK_PATCH_LIST_REQ));
			List<QLabNetworkPatchInfo> networkPatches = new ArrayList<>();
			JSONValue_ObjectArray arr = reply.getJSONObjectArray(null);
			if (arr != null) {
				int patchNumber = 1;
				for (JSONValue_Object jsonPatch: arr) {
					networkPatches.add(new QLabNetworkPatchInfo(patchNumber, jsonPatch));
					patchNumber++;
				}
			}
			m_networkPatchList = networkPatches;
		}
		return m_networkPatchList;
	}
	
	/**
	 * Get the network patch info for a patch number.
	 * Only works for QLab5 and up.
	 * @param patchNumber The patch number (starting with 1).
	 * @return The patch info, or null if not available.
	 * @throws IOException If an IO error occurs.
	 */
	public QLabNetworkPatchInfo getNetworkPatchInfo(int patchNumber) throws IOException
	{
		List<QLabNetworkPatchInfo> patches = getNetworkPatches();
		if (patchNumber >= 1 && patchNumber <= patches.size()) {
			return patches.get(patchNumber-1);
		} else {
			return null;
		}
	}
	
	/**
	 * Select a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @throws IOException If an IO error occurs.
	 */
	public void selectCue(String idOrNumber) throws IOException
	{
		sendQLabReq(addTargetWS(String.format(
				QLabUtil.isCueId(idOrNumber) ? QLabUtil.SELECT_CUE_ID : QLabUtil.SELECT_CUE_NUMBER,
				idOrNumber)));
	}
	
	/**
	 * Test if a cue is broken.
	 * @param idOrNumber The cue unique id or number.
	 * @return True if the cue is broken.
	 * @throws IOException If an IO error occurs.
	 */
	public boolean getIsBroken(String idOrNumber) throws IOException
	{
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.IS_BROKEN_CUE_REQ));
		return reply != null ? reply.getBool(false) : false;
	}
	
	/**
	 * Get the type of a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @return The type the cue. If there's an error, return QLabCueType.UNKNOWN, rather than null.
	 * @throws IOException If an IO error occurs.
	 */
	public QLabCueType getType(String idOrNumber) throws IOException
	{
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.TYPE_CUE_REQ));
		QLabCueType type = reply != null
				? QLabCueType.fromQLab(reply.getString(""))
				: QLabCueType.UNKNOWN;
		return type;
	}

	/**
	 * For testing, send a "get type" request with an argument.
	 * QLab seems to ignore the argument.
	 * @param idOrNumber
	 * @param arg
	 * @return
	 * @throws IOException
	 */
	public QLabCueType getType(String idOrNumber, String arg) throws IOException
	{
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.TYPE_CUE_REQ),
						(arg != null && !arg.isBlank()) ? new Object[] {arg} : null);
		QLabCueType type = reply != null
				? QLabCueType.fromQLab(reply.getString(""))
				: QLabCueType.UNKNOWN;
		return type;
	}

	/**
	 * Get the number for a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @return The number for the cue.
	 * @throws IOException If an IO error occurs.
	 */
	public String getNumber(String idOrNumber) throws IOException
	{
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.NUMBER_CUE_REQ));
		return reply != null ? reply.getString("") : "";
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
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.NUMBER_CUE_REQ),
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
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.NAME_CUE_REQ));
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
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.NAME_CUE_REQ),
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
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.NOTES_CUE_REQ));
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
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.NOTES_CUE_REQ),
									new Object[] {notes});
		return reply != null && reply.isOk();
	}
	
	/**
	 * Get the file target field for a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @return The file target field for the cue.
	 * @throws IOException If an IO error occurs.
	 */
	public String getFileTarget(String idOrNumber) throws IOException
	{
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.FILE_TARGET_CUE_REQ));
		return reply != null ? reply.getString("") : "";
	}
	
	/**
	 * Get the cue target ID field for a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @return The cue target ID field for the cue.
	 * @throws IOException If an IO error occurs.
	 */
	public String getCueTargetId(String idOrNumber) throws IOException
	{
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.CUE_TARGET_ID_CUE_REQ));
		return reply != null ? reply.getString("") : "";
	}
	
	/**
	 * Get the list name field for a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @return The list name field for the cue.
	 * @throws IOException If an IO error occurs.
	 */
	public String getListName(String idOrNumber) throws IOException
	{
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.LIST_NAME_CUE_REQ));
		return reply != null ? reply.getString("") : "";
	}

	/**
	 * Get the duration field for a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @return The duration field for the cue.
	 * @throws IOException If an IO error occurs.
	 */
	public double getDuration(String idOrNumber) throws IOException
	{
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.DURATION_CUE_REQ));
		return reply != null ? reply.getDouble(0) : 0;
	}
	
	/**
	 * Set the duration field for a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @param duration The new duration.
	 * @return True if successful.
	 * @throws IOException If an IO error occurs.
	 */
	public boolean setDuration(String idOrNumber, double duration) throws IOException
	{
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.DURATION_CUE_REQ),
									new Object[] {Double.valueOf(duration)});
		return reply != null && reply.isOk();
	}

	/**
	 * Get the prewait field for a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @return The prewait field for the cue.
	 * @throws IOException If an IO error occurs.
	 */
	public double getPrewait(String idOrNumber) throws IOException
	{
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.PREWAIT_CUE_REQ));
		return reply != null ? reply.getDouble(-1) : -1;
	}
	
	/**
	 * Set the prewait field for a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @param prewait The new prewait.
	 * @return True if successful.
	 * @throws IOException If an IO error occurs.
	 */
	public boolean setPrewait(String idOrNumber, double prewait) throws IOException
	{
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.PREWAIT_CUE_REQ),
									new Object[] {Double.valueOf(prewait)});
		return reply != null && reply.isOk();
	}

	/**
	 * Get the postwait field for a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @return The postwait field for the cue.
	 * @throws IOException If an IO error occurs.
	 */
	public double getPostwait(String idOrNumber) throws IOException
	{
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.POSTWAIT_CUE_REQ));
		return reply != null ? reply.getDouble(0) : 0;
	}
	
	/**
	 * Set the postwait field for a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @param postwait The new postwait.
	 * @return True if successful.
	 * @throws IOException If an IO error occurs.
	 */
	public boolean setPostwait(String idOrNumber, double postwait) throws IOException
	{
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.POSTWAIT_CUE_REQ),
									new Object[] {Double.valueOf(postwait)});
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
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.CONTINUE_MODE_CUE_REQ));
		return reply != null
				? QLabUtil.ContinueMode.fromQLab((int)reply.getLong(QLabUtil.ContinueMode.NO_CONTINUE.ordinal()))
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
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.CONTINUE_MODE_CUE_REQ),
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
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.COLOR_NAME_CUE_REQ));
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
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.COLOR_NAME_CUE_REQ),
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
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.FLAGGED_CUE_REQ));
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
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.FLAGGED_CUE_REQ),
									new Object[] {Integer.valueOf(flagged ? 1 : 0)});
		return reply != null && reply.isOk();
	}
	
	/**
	 * Get the armed attribute for a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @return True if the cue is armed.
	 * @throws IOException If an IO error occurs.
	 */
	public boolean getArmed(String idOrNumber) throws IOException
	{
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.ARMED_CUE_REQ));
		return reply != null ? reply.getBool(false) : false;
	}
	
	/**
	 * Set the armed attribute for a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @param flagged The new armed attribute.
	 * @return True if successful.
	 * @throws IOException If an IO error occurs.
	 */
	public boolean setArmed(String idOrNumber, boolean flagged) throws IOException
	{
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.ARMED_CUE_REQ),
									new Object[] {Integer.valueOf(flagged ? 1 : 0)});
		return reply != null && reply.isOk();
	}
	
	/**
	 * Get the mode of a group cue.
	 * @param idOrNumber The cue unique id or number.
	 * @return The mode.
	 * @throws IOException If an IO error occurs.
	 */
	public QLabUtil.GroupMode getGroupMode(String idOrNumber) throws IOException
	{
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.MODE_CUE_REQ));
		return reply != null
				? QLabUtil.GroupMode.fromQLab((int)reply.getLong(QLabUtil.GroupMode.START_AND_ENTER.ordinal()))
				: QLabUtil.GroupMode.START_AND_ENTER;
	}
	
	/**
	 * Get the patch number for a Network cue.
	 * @param idOrNumber The cue unique id or number.
	 * @return The patch number for the cue.
	 * @throws IOException If an IO error occurs.
	 */
	public int getPatchNumber(String idOrNumber) throws IOException
	{
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber,
						isQLab5() ? QLabUtil.NETWORK_PATCH_NUMBER_REQ : QLabUtil.PATCH_CUE_REQ));
		return reply != null ? (int)reply.getLong(1) : 1;
	}
	
	/**
	 * Set the network patch number for a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @param patch The new patch number.
	 * @return True if successful.
	 * @throws IOException If an IO error occurs.
	 */
	public boolean setPatchNumber(String idOrNumber, int patchNumber) throws IOException
	{
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber,
								isQLab5() ? QLabUtil.NETWORK_PATCH_NUMBER_REQ : QLabUtil.PATCH_CUE_REQ),
								new Object[] {Integer.valueOf(patchNumber)});
		return reply != null && reply.isOk();
	}
	
	/**
	 * Get the network message type for a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @return The network message type for the cue.
	 * @throws IOException If an IO error occurs.
	 */
	public QLabUtil.NetworkMessageType getNetworkMessageType(String idOrNumber) throws IOException
	{
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.MESSAGE_TYPE_CUE_REQ));
		return reply != null
				? QLabUtil.NetworkMessageType.fromQLab(
						(int)reply.getLong(QLabUtil.NetworkMessageType.UNKNOWN.ordinal()))
				: QLabUtil.NetworkMessageType.UNKNOWN;
	}
	
	/**
	 * Set the network message type field for a network cue.
	 * @param idOrNumber The cue unique id or number.
	 * @param type The new message type.
	 * @return True if successful.
	 * @throws IOException If an IO error occurs.
	 */
	public boolean setNetworkMessageType(String idOrNumber, QLabUtil.NetworkMessageType type) throws IOException
	{
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.MESSAGE_TYPE_CUE_REQ),
									new Object[] {Integer.valueOf(type.toQLab())});
		return reply != null && reply.isOk();
	}
	
	/**
	 * Get the parameter value array for a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @return The parameter value array for this cue. Never null, but may be empty.
	 * @throws IOException If an IO error occurs.
	 */
	public List<String> getParameterValues(String idOrNumber) throws IOException
	{
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.PARAMETER_VALUES_REQ));
		List<String> list = null;
		if (reply != null) {
			list = reply.getStringList(null);
		}
		return list != null ? list : List.of();
	}
	
	/**
	 * Set the parameter value array for a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @param values The new parameter value array for this cue.
	 * @return True if successful.
	 * @throws IOException If an IO error occurs.
	 */
	public boolean setParameterValues(String idOrNumber, List<String> values) throws IOException
	{
		if (values == null) {
			values = List.of();
		}
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.PARAMETER_VALUES_REQ),
									values.toArray(new String[values.size()]));
		return reply != null && reply.isOk();
	}
	
	/**
	 * Get the custom string field for a network cue.
	 * @param idOrNumber The cue unique id or number.
	 * @return The custom string field for the cue.
	 * @throws IOException If an IO error occurs.
	 */
	public String getCustomString(String idOrNumber) throws IOException
	{
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.CUSTOM_STRING_CUE_REQ));
		return reply != null ? reply.getString("") : "";
	}
	
	/**
	 * Set the custom string field for a network cue.
	 * @param idOrNumber The cue unique id or number.
	 * @param customString The new custom string.
	 * @return True if successful.
	 * @throws IOException If an IO error occurs.
	 */
	public boolean setCustomString(String idOrNumber, String customString) throws IOException
	{
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.CUSTOM_STRING_CUE_REQ),
									new Object[] {customString});
		return reply != null && reply.isOk();
	}
	
	/**
	 * Get the unique ID of the cue which contains a cue.
	 * @param idOrNumber The child cue.
	 * @return The unique ID of the cue, or "" if it cannot be determined.
	 * @throws IOException If an IO error occurs.
	 */
	public String getParent(String idOrNumber) throws IOException
	{
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber, QLabUtil.PARENT_CUE_REQ), null);
		String parentId = reply != null ? reply.getString("") : "";
		if (QLabUtil.CUELIST_CUE_PARENT_ID.equals(parentId)) {
			parentId = "";
		}
		return parentId;
	}
	
	/**
	 * Get the unique IDs of the immediate children of a container cue.
	 * @param idOrNumber The container cue.
	 * @return A List of the unique IDs of the immediate children of the container cue.
	 * 			If it's not a container cue, return an empty list.
	 * @throws IOException If an IO error occurs.
	 */
	public List<String> getChildrenIds(String idOrNumber) throws IOException
	{
		QLabReply reply = sendQLabReq(getCueReq(idOrNumber,
										QLabUtil.CHILDREN_UNIQUEIDS_SHALLOW_CUE_REQ), null);
		ArrayList<String> childIds = new ArrayList<>();
		JSONValue_ObjectArray data;
		if (reply != null && (data = reply.getJSONObjectArray(null)) != null) {
			for (JSONValue_Object childObj: data) {
				String childId = childObj.getString(QLabUtil.FLD_UNIQUE_ID, "");
				if (childId != null && !childId.isBlank()) {
					childIds.add(childId);
				}
			}
		}
		return childIds;
	}
	
	/**
	 * Get the index of a cue in its containing cue.
	 * @param idOrNumber The cue.
	 * @return The index of the cue in its container, or -1 if we cannot determine it.
	 * @throws IOException If an IO error occurs.
	 */
	public int getIndexInParent(String idOrNumber) throws IOException
	{
		int index = -1;
		String parentId = getParent(idOrNumber);
		if (parentId != null && !parentId.isBlank()) {
			long startTS = System.currentTimeMillis();
			List<String> siblings = getChildrenIds(parentId);
			if (parentId.isBlank()) {
				System.out.println("XXX child of blank parent: " + siblings);
			}
			if (siblings != null) {
				index = siblings.indexOf(idOrNumber);
			}
		}
		return index;
	}
	
	/**
	 * Create a new cue.
	 * @param type The new cue type.
	 * @param afterCueId Place it after this cue. If that's a cuelist, add to end of cuelist.
	 * @param number The new cue's cue number.
	 * @param name The new cue's name.
	 * @return The unique ID of the new cue, or null if there was an error.
	 * @throws IOException If an IO error occurs.
	 */
	public String newCue(QLabCueType type, String afterCueId, String number, String name) throws IOException
	{
		ArrayList<Object> args = new ArrayList<>();
		args.add(type.toQLab());
		if (afterCueId != null && !afterCueId.isBlank()) {
			args.add(afterCueId);
		}
		QLabReply reply = sendQLabReq(addTargetWS(QLabUtil.NEW_CUE_REQ), args.toArray());
		if (reply == null || !reply.isOk()) {
			return null;
		}
		String newCueId = reply.getString("");
		if (newCueId == null) {
			logError("QueryQLab: no id in new-cue reply.");
			return null;
		}
		if (number != null && !number.isBlank()) {
			setNumber(newCueId, number);
		}
		if (name != null && !name.isBlank()) {
			setName(newCueId, name);
		}
		return newCueId;
	}
	
	/**
	 * Move a cue.
	 * @param cueId The unique ID of the cue to move.
	 * @param newIndex The new index for that cue in its parent. 0 to N, where N means "at end".
	 * @param newParentId The unique ID of the new parent, or null to move it within the same parent.
	 * @return True if successful, false if not.
	 * @throws IOException If an IO error occurs.
	 */
	public boolean moveCue(String cueId, int newIndex, String newParentId) throws IOException
	{
		ArrayList<Object> args = new ArrayList<>();
		args.add(Integer.valueOf(newIndex));
		if (newParentId != null && !newParentId.isBlank()) {
			args.add(newParentId);
		}
		QLabReply reply = sendQLabReq(addTargetWS(String.format(QLabUtil.MOVE_CUE_REQ, cueId)),
										args.toArray());
		return reply != null && reply.isOk();
	}
	
	/**
	 * Get all cue lists and all contained cues.
	 * @return A list of cue lists.
	 * @throws IOException If an IO error occurs.
	 */
	public List<QLabCuelistCue> getAllCueLists() throws IOException
	{
		QLabReply reply = sendQLabReq(addTargetWS(QLabUtil.CUELISTS_REQ));
		if (reply == null) {
			return null;
		}
		List<QLabCuelistCue> cuelists = new ArrayList<>();
		for (QLabCue cue: QLabCueType.getCueArray(reply.getJSONArray(null), null, false, this)) {
			if (cue instanceof QLabCuelistCue) {
				cuelists.add((QLabCuelistCue)cue);
			} else if (cue instanceof QLabCartCue) {
				// ignore carts
			} else {
				logError("QueryQLab.getAllCuelists(): non-cuelist/cuecart at top level: " + cue);
			}
		}
		return cuelists;
	}
	
	/**
	 * Get all cue carts and all contained cues.
	 * @return A list of cue carts.
	 * @throws IOException If an IO error occurs.
	 */
	public List<QLabCartCue> getAllCueCarts() throws IOException
	{
		QLabReply reply = sendQLabReq(addTargetWS(QLabUtil.CUELISTS_REQ));
		if (reply == null) {
			return null;
		}
		List<QLabCartCue> cuecarts = new ArrayList<>();
		for (QLabCue cue: QLabCueType.getCueArray(reply.getJSONArray(null), null, false, this)) {
			if (cue instanceof QLabCartCue) {
				cuecarts.add((QLabCartCue)cue);
			} else if (cue instanceof QLabCuelistCue) {
				// ignore cue lists
			} else {
				logError("QueryQLab.getAllCueCarts(): non-cuelist/cuecart at top level: " + cue);
			}
		}
		return cuecarts;
	}
	
	/**
	 * Return the method for a request for a specific cue.
	 * @param numOrId The cue number or cue id. Note that if it's a cue number,
	 * 		it must be acceptable in an OSC method: no blanks or illegal characters.
	 * @param request The request part.
	 * @return The full request, with the appropriate prefix
	 * 			with the cue number or id.
	 */
	public String getCueReq(String numOrId, String request)
	{
		return addTargetWS(QLabUtil.getCueReq(numOrId, request));
	}
	
	/**
	 * Add the target workspace ID to a command.
	 * @param req A QLab OSC command.
	 * @return If there is a target workspace, return "/workspace/TARGET-ID/req."
	 * 		If not, just return "req,"
	 */
	public String addTargetWS(String req)
	{
		if (m_targetWorkspace != null && !m_targetWorkspace.isBlank()) {
			return String.format(QLabUtil.WORKSPACE_REQ_PREFIX, m_targetWorkspace) + req;
		} else {
			return req;
		}
	}
	
	/**
	 * Set the target workspace. If set, add this workspace's prefix to the appropriate commands.
	 * @param id The ID of the target workspace. If null or blank, remove the target workspace.
	 * @return The ID of the previous target workspace, or "".
	 */
	public String setTargetWorkspace(String id)
	{
		if (id == null) {
			id = "";
		}
		String prev = m_targetWorkspace;
		m_targetWorkspace = id;
		return prev;
	}
	
	/**
	 * Get the current target workspace.
	 * @return The ID of the target workspace, or "".
	 */
	public String getTargetWorkspace()
	{
		return m_targetWorkspace;
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
	
	private static String mkIndent(int depth)
	{
		StringBuilder b = new StringBuilder();
		for (int n = 0; n < depth; n++) {
			b.append("   ");
		}
		return b.toString();
	}
	
	@Override
	public void logError(String err)
	{
		m_errOut.println(err);
	}
	
	/**
	 * Set the error output stream.
	 * @param errOut The error output stream. If null, use System.err.
	 */
	public void setErrOut(PrintStream errOut)
	{
		m_errOut = errOut != null ? errOut : System.err;
	}

	public static void main(String[] args) throws IOException
	{
		try (QueryQLab queryQLab = new QueryQLab(args[0])) {
			long startTS = System.currentTimeMillis();
			System.out.println("Version: " + queryQLab.getMajorVersion()
						+ " (" + queryQLab.getVersion() + ")");
			System.out.println("IsQLab: " + queryQLab.isQLab());
			System.out.println("Workspaces:");
			for (QLabWorkspaceInfo ws: queryQLab.getWorkspaces()) {
				System.out.println("  " + ws);
			}
			if (queryQLab.isQLab5()) {
				System.out.println("Patches:");
				for (QLabNetworkPatchInfo patch : queryQLab.getNetworkPatches()) {
					System.out.println("  " + patch);
				} 
			}
			List<QLabCuelistCue> allCues = queryQLab.getAllCueLists();

			int nCues = 0;
			for (QLabCuelistCue cuelist: allCues) {
				nCues += cuelist.walkCues((cue, path) -> { return true; });
			}
			System.out.println("All Cues: Got " + nCues
							+ " cues in " + (System.currentTimeMillis() - startTS)/1000.0 + " sec");

			for (QLabCue cue: allCues) {
				System.out.println();
				cue.printCue(System.out, "", "   ");
			}
			for (QLabCuelistCue cuelist: allCues) {
				System.out.println();
				System.out.println("Using walkCues() on \"" + cuelist.getName() + "\":");
				int n = cuelist.walkCues((cue, path) -> {
										System.out.println(mkIndent(path.size())
												+ cue.m_type + " \""
												+ cue.m_number + "\" \"" + cue.getName() + "\"");
										return true;
									});
				System.out.println(n + " cues in cuelist");
			}
			System.out.println("Using walkCues() on all cuelists:");
			ArrayList<String> allCueIds = new ArrayList<>();
			int n = QLabCue.walkCues(allCues, (cue, path) -> {
						if (cue instanceof QLabCuelistCue) {
							System.out.println();
						}
						System.out.println(mkIndent(1+path.size())
								+ cue.m_type + " \""
								+ cue.m_number + "\" \"" + cue.getName() + "\"");
						allCueIds.add(cue.m_uniqueId);
						int indexInParent;
						try {
							indexInParent = queryQLab.getIndexInParent(cue.m_uniqueId);
							cue.getIndexInParent();
							if (indexInParent != cue.getIndexInParent()) {
								System.out.println(mkIndent(1+path.size()) + "**** indexInParent err: "
										+ indexInParent + " != " + cue.getIndexInParent());
							}
						} catch (IOException e) {
							System.out.println("**** getIndexInParent err: " + e);
						}
						return true;
			});
			System.out.println(n + " cues in all cuelists.");
			System.out.println();
			System.out.println("Get parent & childrent for container cues:");
			QLabCue.walkCues(allCues, (cue, path) -> {
						if (cue instanceof QLabCuelistCue || cue instanceof QLabGroupCue) {
							try {
								System.out.println();
								System.out.println(cue.m_type + " " + cue.m_uniqueId
												+ ": parent=" + queryQLab.getParent(cue.m_uniqueId)
												+ "\n   children=" + queryQLab.getChildrenIds(cue.m_uniqueId));
							} catch (IOException e) {
								System.out.println("  *** IOException " + e);
							}
						}
						return true;
					});
			System.out.println();
			System.out.println("Check findCuePath for all cues:");
			int nFound = 0;
			for (String cueId: allCueIds) {
				List<QLabCue> path = QLabCue.findCuePath(cueId, allCues);
				if (path == null || path.isEmpty() || !path.get(0).m_uniqueId.equals(cueId)) {
					System.out.println("  *** cannot find cue id " + cueId);
				} else {
					nFound++;
					System.out.print("  ");
					for (QLabCue cue: path) {
						System.out.print(" " + cue.m_uniqueId);
					}
					System.out.println();
				}
			}

			System.out.println("  Found " + nFound + " cues out of " + allCueIds.size());
			QLabCue.walkCues(allCues, (cue,path) -> {
				try {
					long t0 = System.currentTimeMillis();
					QLabCueType type0 = queryQLab.getType(cue.m_uniqueId);
					// QLabUtil.ColorName color0 = queryQLab.getColorName(cue.m_uniqueId);
					long t1 = System.currentTimeMillis();
					MiscUtil.sleep(0);
					// QLabUtil.ColorName color1 = queryQLab.getColorName(cue.m_uniqueId);
					QLabCueType type1 = queryQLab.getType(cue.m_uniqueId);
					long t2 = System.currentTimeMillis();
					System.out.println(" " + cue.m_uniqueId + "/" + cue.m_type
								+ ": t1-t0: " + (t1-t0) + " t2-t1: " + (t2-t1));
					if (!cue.m_type.equals(type0)) {
						System.out.println(" *** " + cue.m_uniqueId + " getType0 " + type0 + " != " + cue.m_type);
					}
					if (!cue.m_type.equals(type1)) {
						System.out.println(" *** " + cue.m_uniqueId + " getType1 " + type1 + " != " + cue.m_type);
					}
				} catch (IOException e) {
					System.out.println("Err: " + e);
					return false;
				}
				return true;
			});
		} catch (IllegalArgumentException e) {
			System.err.println(e);
		}
	}
}
