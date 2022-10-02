package com.wdroome.osc.qlab;

import com.wdroome.json.JSONValue_Object;

/**
 * Information about a QLab workspace.
 * @author wdr
 */
public class QLabWorkspaceInfo
{
	public final String m_uniqueId;
	public final String m_displayName;
	public final String m_version;
	
	public final boolean m_hasPasscode;	// only valid in QLab4
	
	public final int m_port;			// only valid in QLab5
	public final int m_udpReplyPort;	// only valid in QLab5
	
	public QLabWorkspaceInfo(JSONValue_Object jsonWorkspace)
	{
		m_uniqueId = jsonWorkspace.getString(QLabUtil.FLD_UNIQUE_ID, "");
		m_displayName = jsonWorkspace.getString(QLabUtil.FLD_DISPLAY_NAME, "");
		m_hasPasscode = jsonWorkspace.getBoolean(QLabUtil.FLD_HAS_PASSCODE, false);
		m_version = jsonWorkspace.getString(QLabUtil.FLD_VERSION, "");
		m_port = (int)jsonWorkspace.getNumber(QLabUtil.FLD_PORT, 0);
		m_udpReplyPort = (int)jsonWorkspace.getNumber(QLabUtil.FLD_UDP_REPLY_PORT, 0);
	}
	
	@Override
	public String toString() {
		return "QLabWorkspaceInfo["
				+ "id=" + m_uniqueId
				+ ", displayName=" + m_displayName
				+ ", hasPasscode=" + m_hasPasscode
				+ ", version=" + m_version
				+ ", port/udpPort=" + m_port + "/" + m_udpReplyPort
				+ "]";
	}
}
