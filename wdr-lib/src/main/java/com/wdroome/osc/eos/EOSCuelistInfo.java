package com.wdroome.osc.eos;

import java.util.List;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.wdroome.osc.OSCConnection;
import com.wdroome.osc.OSCMessage;
import com.wdroome.osc.OSCUtil;

public class EOSCuelistInfo
{
	public static final String GET_CUELIST_INFO_CMD = "/eos/get/cuelist/index/%d";	// arg is cuelist index, 0-N-1
	public static final String GET_CUELIST_INFO_REPLY_PAT
			= "/eos/out/get/cuelist/[^/]+(/links)?/list/%d/[0-9]+";
	
	public static final int GET_CUELIST_INFO_REPLY_LIST_NUMBER = 4;
	public static final int GET_CUELIST_INFO_REPLY_TYPE = 5;
	public static final int GET_CUELIST_INFO_REPLY_LIST_COUNT = 7;
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

	public EOSCuelistInfo(int cuelistIndex, BlockingQueue<OSCMessage> replies,
							OSCConnection oscConn,
							OSCConnection.ReplyHandler replyHandler, long timeoutMS)
	{
		m_cuelistIndex = cuelistIndex;
		boolean gotListReply = false;
		boolean gotLinksReply = false;
		while (true) {
			try {
				OSCMessage msg = replies.poll(timeoutMS, TimeUnit.MILLISECONDS);
				System.out.println("XXX reply: " + msg);
				List<String> cmdTokens = OSCUtil.parseCmd(msg.getMethod(), "",
							GET_CUELIST_INFO_REPLY_LIST_NUMBER,
							GET_CUELIST_INFO_REPLY_TYPE,
							GET_CUELIST_INFO_REPLY_LIST_COUNT);
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
					System.out.println("XXX: cuelist index arg: "
							+ msg.getLong(GET_CUELIST_INFO_REPLY_FLD_INDEX, -2));
					System.out.println("XXX: cuelist HTP arg: "
							+ msg.getBoolean(6, true));
					gotListReply = true;
				} else if (cmdTokens.get(1).equals(GET_CUELIST_INFO_REPLY_TYPE_LINKS)) {
					gotLinksReply = true;
				}
				if (gotListReply && gotLinksReply) {
					System.out.println("XXX: got Cuelist " + m_cuelistNumber);
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
		return !m_cuelistNumber.equals("");
	}
	
	public static String getMethod(int cuelistIndex)
	{
		return String.format(GET_CUELIST_INFO_CMD, cuelistIndex);
	}
	
	public static String getReplyPat(int cuelistIndex)
	{
		return String.format(GET_CUELIST_INFO_REPLY_PAT, cuelistIndex);
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
