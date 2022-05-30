package com.wdroome.osc.qlab;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.wdroome.osc.OSCConnection;
import com.wdroome.osc.OSCMessage;
import com.wdroome.osc.OSCUtil;
import com.wdroome.json.JSONParseException;
import com.wdroome.json.JSONValue_String;
import com.wdroome.json.JSONValue_Object;
import com.wdroome.json.JSONValue_Array;
import com.wdroome.json.JSONValue_ObjectArray;
import com.wdroome.json.JSONValueTypeException;

public class QueryQLab extends OSCConnection
{
	private long m_timeoutMS = 2500;
	private String m_passcode = "";
	private String m_lastReplyWorkspaceId = "";

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
	 * @param addr An ipaddr:port string with the server's inet socket address,
	 * 			with an optional [/passcode] suffix.
	 */
	public QueryQLab(String addrPortPasscode) throws IllegalArgumentException
	{
		super(extractAddrPort(addrPortPasscode));
		m_passcode = extractPasscode(addrPortPasscode);
		
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
	
	@Override
	public void connect() throws IOException
	{
		super.connect();
		super.sendMessage(new OSCMessage(QLabUtil.ALWAYS_REPLY_REQ, new Object[] {"1"}));
		if (!m_passcode.isEmpty()) {
			super.sendMessage(new OSCMessage(QLabUtil.CONNECT_REQ, new Object[] {m_passcode}));
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
			QLabReply reply = new QLabReply(replyMsg);
			if (!reply.m_workspaceId.isBlank()) {
				m_lastReplyWorkspaceId = reply.m_workspaceId;
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
	 * Get the number for a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @return The number for the cue.
	 * @throws IOException If an IO error occurs.
	 */
	public String getNumber(String idOrNumber) throws IOException
	{
		QLabReply reply = sendQLabReq(QLabUtil.getCueReq(idOrNumber, QLabUtil.NUMBER_CUE_REQ));
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
	 * Get the file target field for a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @return The file target field for the cue.
	 * @throws IOException If an IO error occurs.
	 */
	public String getFileTarget(String idOrNumber) throws IOException
	{
		QLabReply reply = sendQLabReq(QLabUtil.getCueReq(idOrNumber, QLabUtil.FILE_TARGET_CUE_REQ));
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
		QLabReply reply = sendQLabReq(QLabUtil.getCueReq(idOrNumber, QLabUtil.CUE_TARGET_ID_CUE_REQ));
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
		QLabReply reply = sendQLabReq(QLabUtil.getCueReq(idOrNumber, QLabUtil.LIST_NAME_CUE_REQ));
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
		QLabReply reply = sendQLabReq(QLabUtil.getCueReq(idOrNumber, QLabUtil.DURATION_CUE_REQ));
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
		QLabReply reply = sendQLabReq(QLabUtil.getCueReq(idOrNumber, QLabUtil.DURATION_CUE_REQ),
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
		QLabReply reply = sendQLabReq(QLabUtil.getCueReq(idOrNumber, QLabUtil.PREWAIT_CUE_REQ));
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
		QLabReply reply = sendQLabReq(QLabUtil.getCueReq(idOrNumber, QLabUtil.PREWAIT_CUE_REQ),
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
		QLabReply reply = sendQLabReq(QLabUtil.getCueReq(idOrNumber, QLabUtil.POSTWAIT_CUE_REQ));
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
		QLabReply reply = sendQLabReq(QLabUtil.getCueReq(idOrNumber, QLabUtil.POSTWAIT_CUE_REQ),
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
		QLabReply reply = sendQLabReq(QLabUtil.getCueReq(idOrNumber, QLabUtil.CONTINUE_MODE_CUE_REQ));
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
	 * Get the armed attribute for a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @return True if the cue is armed.
	 * @throws IOException If an IO error occurs.
	 */
	public boolean getArmed(String idOrNumber) throws IOException
	{
		QLabReply reply = sendQLabReq(QLabUtil.getCueReq(idOrNumber, QLabUtil.ARMED_CUE_REQ));
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
		QLabReply reply = sendQLabReq(QLabUtil.getCueReq(idOrNumber, QLabUtil.ARMED_CUE_REQ),
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
		QLabReply reply = sendQLabReq(QLabUtil.getCueReq(idOrNumber, QLabUtil.MODE_CUE_REQ));
		return reply != null
				? QLabUtil.GroupMode.fromQLab((int)reply.getLong(QLabUtil.GroupMode.START_AND_ENTER.ordinal()))
				: QLabUtil.GroupMode.START_AND_ENTER;
	}
	
	/**
	 * Get the patch number for a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @return The patch number for the cue.
	 * @throws IOException If an IO error occurs.
	 */
	public int getPatchNumber(String idOrNumber) throws IOException
	{
		QLabReply reply = sendQLabReq(QLabUtil.getCueReq(idOrNumber, QLabUtil.PATCH_CUE_REQ));
		return reply != null ? (int)reply.getLong(1) : 1;
	}
	
	/**
	 * Set the patch number for a cue.
	 * @param idOrNumber The cue unique id or number.
	 * @param patch The new patch number.
	 * @return True if successful.
	 * @throws IOException If an IO error occurs.
	 */
	public boolean setPatchNumber(String idOrNumber, int patchNumber) throws IOException
	{
		QLabReply reply = sendQLabReq(QLabUtil.getCueReq(idOrNumber, QLabUtil.PATCH_CUE_REQ),
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
		QLabReply reply = sendQLabReq(QLabUtil.getCueReq(idOrNumber, QLabUtil.MESSAGE_TYPE_CUE_REQ));
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
		QLabReply reply = sendQLabReq(QLabUtil.getCueReq(idOrNumber, QLabUtil.MESSAGE_TYPE_CUE_REQ),
									new Object[] {Integer.valueOf(type.toQLab())});
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
		QLabReply reply = sendQLabReq(QLabUtil.getCueReq(idOrNumber, QLabUtil.CUSTOM_STRING_CUE_REQ));
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
		QLabReply reply = sendQLabReq(QLabUtil.getCueReq(idOrNumber, QLabUtil.CUSTOM_STRING_CUE_REQ),
									new Object[] {customString});
		return reply != null && reply.isOk();
	}
	
	public String newCue(QLabCueType type, String afterCueId, String number, String name) throws IOException
	{
		ArrayList<Object> args = new ArrayList<>();
		args.add(type.toQLab());
		if (afterCueId != null && !afterCueId.isBlank()) {
			args.add(afterCueId);
		}
		QLabReply reply = sendQLabReq(QLabUtil.NEW_CUE_REQ, args.toArray());
		if (reply == null || !reply.isOk()) {
			return null;
		}
		String newCueId = reply.getString("");
		if (newCueId == null) {
			System.err.println("QueryQLab: no id in new-cue reply.");
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
	
	public boolean moveCue(String cueId, int newIndex, String newParentId) throws IOException
	{
		ArrayList<Object> args = new ArrayList<>();
		args.add(Integer.valueOf(newIndex));
		if (newParentId != null && !newParentId.isBlank()) {
			args.add(newParentId);
		}
		QLabReply reply = sendQLabReq(String.format(QLabUtil.MOVE_CUE_REQ, cueId),
										args.toArray());
		return reply != null && reply.isOk();
	}
	
	/**
	 * Get all cue lists and all all contained cues.
	 * @return A list of cue lists.
	 * @throws IOException If an IO error occurs.
	 */
	public List<QLabCuelistCue> getAllCueLists() throws IOException
	{
		QLabReply reply = sendQLabReq(QLabUtil.CUELISTS_REQ);
		if (reply == null) {
			return null;
		}
		List<QLabCuelistCue> cuelists = new ArrayList<>();
		for (QLabCue cue: QLabCueType.getCueArray(reply.getJSONArray(null), null, false, this)) {
			if (cue instanceof QLabCuelistCue) {
				cuelists.add((QLabCuelistCue)cue);
			} else {
				System.err.println("QueryEOS.getAllCuelists(): non-cuelist at top level: " + cue);
			}
		}
		return cuelists;
		// return QLabCueType.getCueArray(reply.getJSONArray(null), null, false, this);
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

	public static void main(String[] args) throws IOException
	{
		try (QueryQLab queryQLab = new QueryQLab(args[0])) {
			System.out.println("Version: " + queryQLab.getVersion());
			System.out.println("Workspaces:");
			for (QLabWorkspaceInfo ws: queryQLab.getWorkspaces()) {
				System.out.println("  " + ws);
			}
			List<QLabCuelistCue> allCues = queryQLab.getAllCueLists();
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
			int n = QLabCue.walkCues(allCues, (cue, path) -> {
						if (cue instanceof QLabCuelistCue) {
							System.out.println();
						}
						System.out.println(mkIndent(1+path.size())
								+ cue.m_type + " \""
								+ cue.m_number + "\" \"" + cue.getName() + "\"");
						return true;
			});
			System.out.println(n + " cues in all cuelists.");

		} catch (IllegalArgumentException e) {
			System.err.println(e);
		}
	}
}
