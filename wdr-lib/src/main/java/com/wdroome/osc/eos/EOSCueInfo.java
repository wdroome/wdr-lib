package com.wdroome.osc.eos;

import java.io.IOException;

import java.util.List;
import java.util.ArrayList;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.wdroome.osc.OSCConnection;
import com.wdroome.osc.OSCMessage;
import com.wdroome.osc.OSCUtil;
import com.wdroome.osc.OSCConnection.ReplyHandler;

/**
 * Information for an EOS cuelist.
 * @author wdr
 */
public class EOSCueInfo implements Comparable<EOSCueInfo>
{
		// args are cuelist number & cue index, 0-N-1
	public static final String GET_CUE_INFO_METHOD = "/eos/get/cue/%d/index/%d";
	public static final String GET_CUE_INFO_REPLY_PAT
			= "/eos/out/get/cue/%d/[.0-9]+/[0-9]+(/fx|/links|/actions)?/list/[0-9]+/[0-9]+";
		
	public static final int GET_CUE_INFO_REPLY_CUE_NUMBER = 5;
	public static final int GET_CUE_INFO_REPLY_PART_NUMBER = 6;
	public static final int GET_CUE_INFO_REPLY_TYPE = 7;
	public static final String GET_CUE_INFO_REPLY_TYPE_LIST = "list";
	public static final String GET_CUE_INFO_REPLY_TYPE_FX = "fx";
	public static final String GET_CUE_INFO_REPLY_TYPE_LINKS = "links";
	public static final String GET_CUE_INFO_REPLY_TYPE_ACTIONS = "actions";
	
	public static final int GET_CUE_INFO_REPLY_FLD_INDEX = 0;
	public static final int GET_CUE_INFO_REPLY_FLD_UUID = 1;
	public static final int GET_CUE_INFO_REPLY_FLD_LABEL = 2;
	public static final int GET_CUE_INFO_REPLY_FLD_UP_TIME = 3;
	public static final int GET_CUE_INFO_REPLY_FLD_UP_DELAY = 4;
	public static final int GET_CUE_INFO_REPLY_FLD_DOWN_TIME = 5;
	public static final int GET_CUE_INFO_REPLY_FLD_DOWN_DELAY = 6;
	public static final int GET_CUE_INFO_REPLY_FLD_FOCUS_TIME = 7;
	public static final int GET_CUE_INFO_REPLY_FLD_FOCUS_DELAY = 8;
	public static final int GET_CUE_INFO_REPLY_FLD_COLOR_TIME = 9;
	public static final int GET_CUE_INFO_REPLY_FLD_COLOR_DELAY = 10;
	public static final int GET_CUE_INFO_REPLY_FLD_BEAM_TIME = 11;
	public static final int GET_CUE_INFO_REPLY_FLD_BEAM_DELAY = 12;
	public static final int GET_CUE_INFO_REPLY_FLD_RATE = 15;
	public static final int GET_CUE_INFO_REPLY_FLD_MARK = 16;
	public static final int GET_CUE_INFO_REPLY_FLD_BLOCK = 17;;
	public static final int GET_CUE_INFO_REPLY_FLD_ASSERT = 18;
	public static final int GET_CUE_INFO_REPLY_FLD_LINK = 19;
	public static final int GET_CUE_INFO_REPLY_FLD_FOLLOW_TIME = 20;
	public static final int GET_CUE_INFO_REPLY_FLD_HANG_TIME = 21;
	public static final int GET_CUE_INFO_REPLY_FLD_ALL_FADE = 22;
	public static final int GET_CUE_INFO_REPLY_FLD_LOOP = 23;
	public static final int GET_CUE_INFO_REPLY_FLD_PART_COUNT = 26;
	public static final int GET_CUE_INFO_REPLY_FLD_NOTES = 27;
	public static final int GET_CUE_INFO_REPLY_FLD_SCENE = 28;
	public static final int GET_CUE_INFO_REPLY_FLD_SCENE_END = 29;
	public static final int GET_CUE_INFO_REPLY_FLD_PART_INDEX = 30;
	
	public static final int GET_CUE_INFO_REPLY_FLD_FX_LIST = 2;
	public static final int GET_CUE_INFO_REPLY_FLD_LINKS_CUELIST = 2;
	public static final int GET_CUE_INFO_REPLY_FLD_ACTIONS_EXTCMD = 2;
	
	private int m_cuelist;
	private int m_cueIndex = -1;
	private EOSCueNumber m_cueNumber = null;
	private String m_uuid = "";
	private String m_label = "";
	private int m_upTimeMS = -1;
	private int m_upDelayMS = -1;
	private int m_downTimeMS = -1;
	private int m_downDelayMS = -1;
	private int m_focusTimeMS = -1;
	private int m_focusDelayMS = -1;
	private int m_colorTimeMS = -1;
	private int m_colorDelayMS = -1;
	private int m_beamTimeMS = -1;
	private int m_beamDelayMS = -1;
	private int m_rate = -1;
	private String m_mark = "";
	private String m_block = "";
	private String m_assert = "";
	private String m_link = "";
	private int m_followTimeMS = -1;
	private int m_hangTimeMS = -1;
	private boolean m_allFade = false;
	private int m_loop = -1;
	private int m_partCount = -1;
	private String m_notes = "";
	private String m_scene = "";
	private boolean m_sceneEnd = false;
	private int m_partIndex = -1;
	
	private ArrayList<String> m_fxList = null;
	private ArrayList<String> m_linkedCuelists = null;
	private String m_extActions = "";
	
	private boolean m_isAutoCue = false;
	
	private String m_warnings = null;

	/**
	 * Get the information for a cue list from the EOS server.
	 * Note that the code assumes cuelist numbers are always simple positive integers
	 * -- no decimal points, no "number 0".
	 * @param cuelistIndex The index of the cue list (0 to N-1).
	 * @param oscConn A connection to the EOS server.
	 * @param timeoutMS The timeout to wait for replies.
	 * @throws IOException If an IO error occurs.
	 */
	public EOSCueInfo(int cuelist, int cueIndex, boolean isAutoCue, OSCConnection oscConn, long timeoutMS) throws IOException
	{
		m_cuelist = cuelist;
		m_cueIndex = cueIndex;
		m_isAutoCue = isAutoCue;
		String requestMethod = String.format(GET_CUE_INFO_METHOD, cuelist, cueIndex);
		String replyMethod = String.format(GET_CUE_INFO_REPLY_PAT, cuelist);
		// System.err.println("\nXXX GetCue req: " + requestMethod);
		// System.err.println("XXX GetCue resp: " + replyMethod);
		final ArrayBlockingQueue<OSCMessage> replies = new ArrayBlockingQueue<>(10);
		ReplyHandler replyHandler = oscConn.sendMessage(new OSCMessage(requestMethod), replyMethod, replies);
		int listIndex = -1;
		int fxIndex = -1;
		int linksIndex = -1;
		int actionsIndex = -1;
		EOSCueNumber fxCueNumber = null;
		EOSCueNumber linksCueNumber = null;
		EOSCueNumber actionsCueNumber = null;
		while (true) {
			try {
				OSCMessage msg = replies.poll(timeoutMS, TimeUnit.MILLISECONDS);
				if (msg == null) {
					break;
				}
				// System.out.println("XXX CueInfo Resp: " + msg);
				int indexArg = (int)msg.getLong(GET_CUE_INFO_REPLY_FLD_INDEX, -1);
				
				/* 
				 * Sometimes EOS gives the wrong cue index in the reply.
				 * Dunno why. The cue number in the reply method seems to be ok.
				 * So we ignore this error. And hope all is well.
				 */
				if (false && indexArg != cueIndex) {
					System.err.println("XXX EOSCueInfo: Bad index " + indexArg + "!=" + cueIndex);
					continue;
				}
				List<String> cmdTokens = OSCUtil.parseMethod(msg.getMethod(), "",
							GET_CUE_INFO_REPLY_CUE_NUMBER,
							GET_CUE_INFO_REPLY_PART_NUMBER,
							GET_CUE_INFO_REPLY_TYPE);
				int part;
				try {
					part = Integer.parseInt(cmdTokens.get(1));
				} catch (Exception e1) {
					continue;
				}
				EOSCueNumber msgCueNumber;
				try {
					msgCueNumber = new EOSCueNumber(m_cuelist, cmdTokens.get(0), part);
				} catch (Exception e) {
					continue;
				}
				String respType = cmdTokens.get(2);
				if (respType.equals(GET_CUE_INFO_REPLY_TYPE_LIST)) {
					m_cueNumber = msgCueNumber;
					m_uuid = msg.getString(GET_CUE_INFO_REPLY_FLD_UUID, m_label);
					m_label = msg.getString(GET_CUE_INFO_REPLY_FLD_LABEL, m_label);
					m_upTimeMS = (int)msg.getLong(GET_CUE_INFO_REPLY_FLD_UP_TIME, m_upTimeMS);
					m_upDelayMS = (int)msg.getLong(GET_CUE_INFO_REPLY_FLD_UP_DELAY, m_upDelayMS);
					m_downTimeMS = (int)msg.getLong(GET_CUE_INFO_REPLY_FLD_DOWN_TIME, m_downTimeMS);
					m_downDelayMS = (int)msg.getLong(GET_CUE_INFO_REPLY_FLD_DOWN_DELAY, m_downDelayMS);
					m_focusTimeMS = (int)msg.getLong(GET_CUE_INFO_REPLY_FLD_FOCUS_TIME, m_focusTimeMS);
					m_focusDelayMS = (int)msg.getLong(GET_CUE_INFO_REPLY_FLD_FOCUS_DELAY, m_focusDelayMS);
					m_colorTimeMS = (int)msg.getLong(GET_CUE_INFO_REPLY_FLD_COLOR_TIME, m_colorTimeMS);
					m_colorDelayMS = (int)msg.getLong(GET_CUE_INFO_REPLY_FLD_COLOR_DELAY, m_colorDelayMS);
					m_beamTimeMS = (int)msg.getLong(GET_CUE_INFO_REPLY_FLD_BEAM_TIME, m_beamTimeMS);
					m_beamDelayMS = (int)msg.getLong(GET_CUE_INFO_REPLY_FLD_BEAM_DELAY, m_beamDelayMS);
					m_rate = (int)msg.getLong(GET_CUE_INFO_REPLY_FLD_RATE, m_rate);
					m_mark = msg.getString(GET_CUE_INFO_REPLY_FLD_MARK, m_mark);
					m_block = msg.getString(GET_CUE_INFO_REPLY_FLD_BLOCK, m_block);
					m_assert = msg.getString(GET_CUE_INFO_REPLY_FLD_ASSERT, m_assert);
					m_link = msg.getString(GET_CUE_INFO_REPLY_FLD_LINK, m_link);
					m_followTimeMS = (int)msg.getLong(GET_CUE_INFO_REPLY_FLD_FOLLOW_TIME, m_followTimeMS);
					m_hangTimeMS = (int)msg.getLong(GET_CUE_INFO_REPLY_FLD_HANG_TIME, m_hangTimeMS);
					m_allFade = msg.getBoolean(GET_CUE_INFO_REPLY_FLD_ALL_FADE, m_allFade);
					m_loop = (int)msg.getLong(GET_CUE_INFO_REPLY_FLD_LOOP, m_loop);
					m_partCount = (int)msg.getLong(GET_CUE_INFO_REPLY_FLD_PART_COUNT, m_partCount);
					m_notes = msg.getString(GET_CUE_INFO_REPLY_FLD_NOTES, m_notes);
					m_scene = msg.getString(GET_CUE_INFO_REPLY_FLD_SCENE, m_scene);
					m_sceneEnd = msg.getBoolean(GET_CUE_INFO_REPLY_FLD_SCENE_END, m_sceneEnd);
					m_partIndex = (int)msg.getLong(GET_CUE_INFO_REPLY_FLD_PART_INDEX, m_partIndex);
					listIndex = indexArg;
				} else if (respType.equals(GET_CUE_INFO_REPLY_TYPE_LINKS)) {
					linksCueNumber = msgCueNumber;
					m_linkedCuelists = getRemainingStrArgs(msg, GET_CUE_INFO_REPLY_FLD_LINKS_CUELIST);
					linksIndex = indexArg;
				} else if (respType.equals(GET_CUE_INFO_REPLY_TYPE_FX)) {
					fxCueNumber = msgCueNumber;
					m_fxList = getRemainingStrArgs(msg, GET_CUE_INFO_REPLY_FLD_FX_LIST);
					fxIndex = indexArg;
				} else if (respType.equals(GET_CUE_INFO_REPLY_TYPE_ACTIONS)) {
					actionsCueNumber = msgCueNumber;
					m_extActions = msg.getString(GET_CUE_INFO_REPLY_FLD_ACTIONS_EXTCMD, m_extActions);
					actionsIndex = indexArg;
				} else {
					System.out.println("XXX: unknown type: " + respType + ": " + msg);
				}
				if (listIndex >= 0 && linksIndex >= 0 && fxIndex >= 0 && actionsIndex >= 0) {
					if (linksIndex == listIndex && fxIndex == listIndex && actionsIndex == listIndex) {
						/**
						 * This is another cue-index mismatch test (see above).
						 * We get here if the part replies all have the same index,
						 * but it doesn't match the request index. We get the cue number from
						 * the reply method. This gives a warning, rather than ignore the reply.
						 * And for now, we've disabled the warning.
						 */
						if (false && listIndex != cueIndex) {
							addWarning("EOSCue-by-index mismatch: cue=" + m_cueNumber.toFullString()
									+ " req#=" + cueIndex + " reply#=" + listIndex);
						}
						/**
						 * This verifies that the reply parts all have the same cue number
						 * in the reply method. Unlike the index mismatch, this is
						 * a serious problem.
						 */
						if (!m_cueNumber.equals(actionsCueNumber) || !m_cueNumber.equals(linksCueNumber)
								|| !m_cueNumber.equals(fxCueNumber) || !m_cueNumber.equals(actionsCueNumber)) {
							addWarning("EOSCue-by-index cue number mismatch: index=#" + cueIndex
									+ " list=" + m_cueNumber.toFullString()
									+ " links=" + linksCueNumber.toFullString()
									+ " fx=" + fxCueNumber.toFullString()
									+ " actions=" + actionsCueNumber.toFullString());
						}
					} else {
						addWarning(
								"Cuelist index disagreement: cue=" + m_cueNumber.toFullString() + " list=#" + listIndex
										+ " links=#" + linksIndex + " fx=#" + fxIndex + " actions=#" + actionsIndex);
					}
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
				break;
			}
		}
		oscConn.dropReplyHandler(replyHandler);
	}
	
	private void addWarning(String warning)
	{
		if (m_warnings == null) {
			m_warnings = warning;
		} else {
			m_warnings += "; " + warning;
		}
	}

	public boolean isValid()
	{
		return m_cueNumber != null;
	}

	public int getCueIndex() {
		return m_cueIndex;
	}

	public EOSCueNumber getCueNumber() {
		return m_cueNumber;
	}

	public String getUuid() {
		return m_uuid;
	}

	public String getLabel() {
		return m_label;
	}

	public int getUpTimeMS() {
		return m_upTimeMS;
	}

	public int getUpDelayMS() {
		return m_upDelayMS;
	}

	public int getDownTimeMS() {
		return m_downTimeMS;
	}

	public int getDownDelayMS() {
		return m_downDelayMS;
	}

	public int getFocusTimeMS() {
		return m_focusTimeMS;
	}

	public int getFocusDelayMS() {
		return m_focusDelayMS;
	}

	public int getColorTimeMS() {
		return m_colorTimeMS;
	}

	public int getColorDelayMS() {
		return m_colorDelayMS;
	}

	public int getBeamTimeMS() {
		return m_beamTimeMS;
	}

	public int getBeamDelayMS() {
		return m_beamDelayMS;
	}

	public int getRate() {
		return m_rate;
	}

	public String getMark() {
		return m_mark;
	}

	public String getBlock() {
		return m_block;
	}

	public String getAssert() {
		return m_assert;
	}

	public String getLink() {
		return m_link;
	}

	public int getFollowTimeMS() {
		return m_followTimeMS;
	}

	public int getHangTimeMS() {
		return m_hangTimeMS;
	}

	public boolean isAllFade() {
		return m_allFade;
	}

	public int getLoop() {
		return m_loop;
	}

	public int getPartCount() {
		return m_partCount;
	}

	public String getNotes() {
		return m_notes;
	}

	public String getScene() {
		return m_scene;
	}

	public boolean isSceneEnd() {
		return m_sceneEnd;
	}

	public int getPartIndex() {
		return m_partIndex;
	}

	public List<String> getFxList() {
		return m_fxList;
	}

	public List<String> getLinkedCuelists() {
		return m_linkedCuelists;
	}

	public String getExtActions() {
		return m_extActions;
	}

	public boolean isAutoCue() {
		return m_isAutoCue;
	}

	public String getWarnings() {
		return m_warnings;
	}
	
	/**
	 * Get the cue duration, in seconds.
	 * @return The cue duration, in seconds.
	 */
	public double getDuration()
	{
		int dur = 0;
		dur = maxTimeDelay(dur, m_upTimeMS, m_upDelayMS);
		dur = maxTimeDelay(dur, m_downTimeMS, m_downDelayMS);
		dur = maxTimeDelay(dur, m_focusTimeMS, m_focusDelayMS);
		dur = maxTimeDelay(dur, m_colorTimeMS, m_colorDelayMS);
		dur = maxTimeDelay(dur, m_beamTimeMS, m_beamDelayMS);
		return dur/1000.0;
	}
	
	private int maxTimeDelay(int a, int bTime, int bDelay)
	{
		return Math.max(a, Math.max(bTime,0) + Math.max(bDelay,0));
	}
	
	/**
	 * Return a List with the remaining String args in a message.
	 * @param msg
	 * @param iStart The index of the starting string arg.
	 * @return A list with arguments iArg to end, or null if none.
	 */
	private ArrayList<String> getRemainingStrArgs(OSCMessage msg, int iStart)
	{
		if (msg.getArgType(iStart) != OSCUtil.OSC_STR_ARG_FMT_CHAR) {
			return null;
		}
		ArrayList<String> list = new ArrayList<>();
		for (int iArg = iStart; iArg < msg.size(); iArg++) {
			list.add(msg.getString(iArg, GET_CUE_INFO_METHOD));
		}
		return list;
	}

	/**
	 * Compare based on the cue number.
	 */
	@Override
	public int compareTo(EOSCueInfo o)
	{
		return m_cueNumber.compareTo(o.m_cueNumber);
	}

	public String toShortString() {
		String prefix = "";
		if (m_cueNumber.isPart()) {
			prefix = "--";
		} else if (m_isAutoCue) {
			prefix = ">";
		}
		return prefix + m_cueNumber.toFullString()
				+ "," + getDuration() + "sec"
				+ (!m_label.isBlank() ? (",\"" + m_label + "\"") : "");
	}

	@Override
	public String toString() {
		return "EOSCueInfo[cueIndex=" + m_cueIndex
				+ ",cueNumber=" + (m_cueNumber != null ? m_cueNumber.toFullString() : "null")
				+ ",isAuto=" + m_isAutoCue + ",uuid=" + m_uuid
				+ ",label=" + m_label + ",upTimeMS=" + m_upTimeMS + ",upDelayMS=" + m_upDelayMS
				+ ",downTimeMS=" + m_downTimeMS + ",downDelayMS=" + m_downDelayMS + ",focusTimeMS="
				+ m_focusTimeMS + ",focusDelayMS=" + m_focusDelayMS + ",colorTimeMS=" + m_colorTimeMS
				+ ",colorDelayMS=" + m_colorDelayMS + ",beamTimeMS=" + m_beamTimeMS + ",beamDelayMS="
				+ m_beamDelayMS + ",rate=" + m_rate + ",mark=" + m_mark + ",block=" + m_block + ",assert="
				+ m_assert + ",link=" + m_link + ",followTimeMS=" + m_followTimeMS + ",hangTimeMS="
				+ m_hangTimeMS + ",allFade=" + m_allFade + ",loop=" + m_loop + ",partCount=" + m_partCount
				+ ",notes=" + m_notes + ",scene=" + m_scene + ",sceneEnd=" + m_sceneEnd + ",partIndex="
				+ m_partIndex + ",fxList=" + m_fxList + ",linkedCuelists=" + m_linkedCuelists + ",extActions="
				+ m_extActions + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + m_cueIndex;
		result = prime * result + ((m_cueNumber == null) ? 0 : m_cueNumber.hashCode());
		result = prime * result + ((m_uuid == null) ? 0 : m_uuid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EOSCueInfo other = (EOSCueInfo) obj;
		if (m_cueIndex != other.m_cueIndex)
			return false;
		if (m_cueNumber == null) {
			if (other.m_cueNumber != null)
				return false;
		} else if (!m_cueNumber.equals(other.m_cueNumber))
			return false;
		if (m_uuid == null) {
			if (other.m_uuid != null)
				return false;
		} else if (!m_uuid.equals(other.m_uuid))
			return false;
		return true;
	}
}
