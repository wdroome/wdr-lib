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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((m_displayName == null) ? 0 : m_displayName.hashCode());
		result = prime * result + (m_hasPasscode ? 1231 : 1237);
		result = prime * result + m_port;
		result = prime * result + m_udpReplyPort;
		result = prime * result + ((m_uniqueId == null) ? 0 : m_uniqueId.hashCode());
		result = prime * result + ((m_version == null) ? 0 : m_version.hashCode());
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
		QLabWorkspaceInfo other = (QLabWorkspaceInfo) obj;
		if (m_displayName == null) {
			if (other.m_displayName != null)
				return false;
		} else if (!m_displayName.equals(other.m_displayName))
			return false;
		if (m_hasPasscode != other.m_hasPasscode)
			return false;
		if (m_port != other.m_port)
			return false;
		if (m_udpReplyPort != other.m_udpReplyPort)
			return false;
		if (m_uniqueId == null) {
			if (other.m_uniqueId != null)
				return false;
		} else if (!m_uniqueId.equals(other.m_uniqueId))
			return false;
		if (m_version == null) {
			if (other.m_version != null)
				return false;
		} else if (!m_version.equals(other.m_version))
			return false;
		return true;
	}
	
}
