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
	
	private int m_cuelist;
	private int m_cueIndex = -1;
	private EOSCueNumber m_cueNumber = null;
	private String m_uuid = "";
	private String m_label = "";

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
		boolean gotLinksReply = false;
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
				if (cmdTokens.get(2).equals(GET_CUE_INFO_REPLY_TYPE_LIST)) {
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
					gotListReply = true;
				} else if (cmdTokens.get(1).equals(GET_CUE_INFO_REPLY_TYPE_LINKS)) {
					gotLinksReply = true;
				}
				if (gotListReply && gotLinksReply) {
					// System.out.println("XXX: got Cuelist " + m_cuelistNumber);
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
