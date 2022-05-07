package com.wdroome.osc.eos;

import java.io.IOException;

import java.util.List;
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
			= "/eos/out/get/cue/%d/[^/]+(/fx|/links/actions)?/list/[0-9]+/[0-9]+";
	
	public static final int GET_CUE_INFO_REPLY_CUE_NUMBER = 5;
	public static final int GET_CUE_INFO_REPLY_PART_NUMBER = 6;
	public static final int GET_CUE_INFO_REPLY_TYPE = 7;
	public static final String GET_CUE_INFO_REPLY_TYPE_LIST = "list";
	public static final String GET_CUE_INFO_REPLY_TYPE_FX = "fx";
	public static final String GET_CUE_INFO_REPLY_TYPE_LINKS = "links";
	public static final String GET_CUE_INFO_REPLY_TYPE_ACTIONS = "action";
	
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

	/**
	 * Get the information for a cue list from the EOS server.
	 * Note that the code assumes cuelist numbers are always simple positive integers
	 * -- no decimal points, no "number 0".
	 * @param cuelistIndex The index of the cue list (0 to N-1).
	 * @param oscConn A connection to the EOS server.
	 * @param timeoutMS The timeout to wait for replies.
	 * @throws IOException If an IO error occurs.
	 */
	public EOSCueInfo(int cuelist, int cueIndex, OSCConnection oscConn, long timeoutMS) throws IOException
	{
		m_cuelist = cuelist;
		m_cueIndex = cueIndex;
		String requestMethod = String.format(GET_CUE_INFO_METHOD, cuelist, cueIndex);
		String replyMethod = String.format(GET_CUE_INFO_REPLY_PAT, cuelist);
		final ArrayBlockingQueue<OSCMessage> replies = new ArrayBlockingQueue<>(10);
		ReplyHandler replyHandler = oscConn.sendMessage(new OSCMessage(requestMethod), replyMethod, replies);
		boolean gotListReply = false;
		boolean gotFxReply = false;
		boolean gotLinksReply = false;
		boolean gotActionsReply = false;
		while (true) {
			try {
				OSCMessage msg = replies.poll(timeoutMS, TimeUnit.MILLISECONDS);
				int indexArg = (int)msg.getLong(GET_CUE_INFO_REPLY_FLD_INDEX, -1);
				if (indexArg != cueIndex) {
					continue;
				}
				List<String> cmdTokens = OSCUtil.parseMethod(msg.getMethod(), "",
							GET_CUE_INFO_REPLY_CUE_NUMBER,
							GET_CUE_INFO_REPLY_PART_NUMBER,
							GET_CUE_INFO_REPLY_TYPE);
				String respType = cmdTokens.get(2);
				if (respType.equals(GET_CUE_INFO_REPLY_TYPE_LIST)) {
					int part;
					try {
						part = Integer.parseInt(cmdTokens.get(1));
					} catch (Exception e1) {
						continue;
					}
					try {
						m_cueNumber = new EOSCueNumber(m_cuelist, cmdTokens.get(0), part);
					} catch (Exception e) {
						continue;
					}
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
					gotListReply = true;
				} else if (respType.equals(GET_CUE_INFO_REPLY_TYPE_LINKS)) {
					// XXX ???
					gotLinksReply = true;
				} else if (respType.equals(GET_CUE_INFO_REPLY_TYPE_FX)) {
					// XXX ???
					gotFxReply = true;
				} else if (respType.equals(GET_CUE_INFO_REPLY_TYPE_ACTIONS)) {
					// XXX ???
					gotActionsReply = true;
				}
				if (gotListReply && gotLinksReply && gotFxReply && gotActionsReply) {
					// System.out.println("XXX: got Cue " + m_cuetNumber.toFullString());
					oscConn.dropReplyHandler(replyHandler);
					break;
				}
			} catch (Exception e) {
			// Usually this is timeout on the poll().
			break;
			}
		}
	}

	public boolean isValid()
	{
		return m_cueNumber != null;
	}
	
	public int getCuelist() {
		return m_cuelist;
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

	/**
	 * Compare based on the cue number.
	 */
	@Override
	public int compareTo(EOSCueInfo o)
	{
		return m_cueNumber.compareTo(o.m_cueNumber);
	}

	@Override
	public String toString() {
		return "EOSCuelistInfo[index=" + m_cueIndex + ",number=" + m_cueNumber.toFullString()
				+ ",label=" + m_label + "]";
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
