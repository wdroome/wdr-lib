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
public class EOSCuelistInfo implements Comparable<EOSCuelistInfo>
{
	public static final String GET_CUELIST_INFO_METHOD = "/eos/get/cuelist/index/%d";	// arg is cuelist index, 0-N-1
	public static final String GET_CUELIST_INFO_REPLY_PAT
			= "/eos/out/get/cuelist/[^/]+(/links)?/list/[0-9]+/[0-9]+";
	
	public static final int GET_CUELIST_INFO_REPLY_LIST_NUMBER = 4;
	public static final int GET_CUELIST_INFO_REPLY_TYPE = 5;
	public static final String GET_CUELIST_INFO_REPLY_TYPE_LIST = "list";
	public static final String GET_CUELIST_INFO_REPLY_TYPE_LINKS = "links";
	
	public static final int GET_CUELIST_INFO_REPLY_FLD_INDEX = 0;
	public static final int GET_CUELIST_INFO_REPLY_FLD_UUID = 1;
	public static final int GET_CUELIST_INFO_REPLY_FLD_LABEL = 2;
	public static final int GET_CUELIST_INFO_REPLY_FLD_PLAYBACK_MODE = 3;
	public static final int GET_CUELIST_INFO_REPLY_FLD_FADER_MODE = 4;
	
	private int m_cuelistIndex = -1;
	private String m_cuelistNumber = "";
	private int m_cueCount = -1;
	private String m_uuid = "";
	private String m_label = "";
	private String m_playbackMode = "";
	private String m_faderMode = "";

	/**
	 * Get the information for a cue list from the EOS server.
	 * @param cuelistIndex The index of the cue list (0 to N-1).
	 * @param oscConn A connection to the EOS server.
	 * @param timeoutMS The timeout to wait for replies.
	 * @throws IOException If an IO error occurs.
	 */
	public EOSCuelistInfo(int cuelistIndex, OSCConnection oscConn, long timeoutMS) throws IOException
	{
		m_cuelistIndex = cuelistIndex;
		String requestMethod = String.format(GET_CUELIST_INFO_METHOD, cuelistIndex);
		String replyMethod = GET_CUELIST_INFO_REPLY_PAT;
		final ArrayBlockingQueue<OSCMessage> replies = new ArrayBlockingQueue<>(10);
		ReplyHandler replyHandler = oscConn.sendMessage(new OSCMessage(requestMethod), replyMethod, replies);
		boolean gotListReply = false;
		boolean gotLinksReply = false;
		while (true) {
			try {
				OSCMessage msg = replies.poll(timeoutMS, TimeUnit.MILLISECONDS);
				int indexArg = (int)msg.getLong(GET_CUELIST_INFO_REPLY_FLD_INDEX, -1);
				if (indexArg != cuelistIndex) {
					continue;
				}
				List<String> cmdTokens = OSCUtil.parseMethod(msg.getMethod(), "",
							GET_CUELIST_INFO_REPLY_LIST_NUMBER,
							GET_CUELIST_INFO_REPLY_TYPE);
				if (cmdTokens.get(1).equals(GET_CUELIST_INFO_REPLY_TYPE_LIST)) {
					m_cuelistNumber = cmdTokens.get(0);
					try {
						m_cueCount = Integer.parseInt(cmdTokens.get(2));
					} catch (Exception e) {
						// ignore
					}
					m_uuid = msg.getString(GET_CUELIST_INFO_REPLY_FLD_UUID, m_label);
					m_label = msg.getString(GET_CUELIST_INFO_REPLY_FLD_LABEL, m_label);
					m_playbackMode = msg.getString(GET_CUELIST_INFO_REPLY_FLD_PLAYBACK_MODE, m_playbackMode);
					m_faderMode = msg.getString(GET_CUELIST_INFO_REPLY_FLD_FADER_MODE, m_faderMode);
					gotListReply = true;
				} else if (cmdTokens.get(1).equals(GET_CUELIST_INFO_REPLY_TYPE_LINKS)) {
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
		if (isValid()) {
			m_cueCount = oscConn.getIntReply(
								String.format(QueryEOS.GET_CUE_COUNT_METHOD, m_cuelistNumber),
								String.format(QueryEOS.GET_CUE_COUNT_REPLY, m_cuelistNumber),
								timeoutMS);
		}
	}

	public boolean isValid()
	{
		return !m_cuelistNumber.equals("");
	}
	
	public static String getMethod(int cuelistIndex)
	{
		return String.format(GET_CUELIST_INFO_METHOD, cuelistIndex);
	}
	
	public static String getReplyPat(int cuelistIndex)
	{
		return GET_CUELIST_INFO_REPLY_PAT;
	}

	public int getCuelistIndex() {
		return m_cuelistIndex;
	}

	public String getCuelistNumber() {
		return m_cuelistNumber;
	}

	public int getCueCount() {
		return m_cueCount;
	}

	public String getLabel() {
		return m_label;
	}

	public String getPlaybackMode() {
		return m_playbackMode;
	}

	public String getFaderMode() {
		return m_faderMode;
	}
	
	/**
	 * Compare based on the cuelist number, as a String.
	 */
	@Override
	public int compareTo(EOSCuelistInfo o)
	{
		return m_cuelistNumber.compareTo(o.m_cuelistNumber);
	}

	@Override
	public String toString() {
		return "EOSCuelistInfo[index=" + m_cuelistIndex + ",number=" + m_cuelistNumber
				+ ",cueCount=" + m_cueCount + ",label=" + m_label + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + m_cuelistIndex;
		result = prime * result + ((m_cuelistNumber == null) ? 0 : m_cuelistNumber.hashCode());
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
		EOSCuelistInfo other = (EOSCuelistInfo) obj;
		if (m_cuelistIndex != other.m_cuelistIndex)
			return false;
		if (m_cuelistNumber == null) {
			if (other.m_cuelistNumber != null)
				return false;
		} else if (!m_cuelistNumber.equals(other.m_cuelistNumber))
			return false;
		if (m_uuid == null) {
			if (other.m_uuid != null)
				return false;
		} else if (!m_uuid.equals(other.m_uuid))
			return false;
		return true;
	}
}
