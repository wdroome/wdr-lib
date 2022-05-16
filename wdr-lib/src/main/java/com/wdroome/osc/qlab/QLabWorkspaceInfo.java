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
	public final boolean m_hasPasscode;
	public final String m_version;
	
	public QLabWorkspaceInfo(JSONValue_Object jsonWorkspace)
	{
		m_uniqueId = jsonWorkspace.getString(QLabUtil.FLD_UNIQUE_ID, "");
		m_displayName = jsonWorkspace.getString(QLabUtil.FLD_DISPLAY_NAME, "");
		m_hasPasscode = jsonWorkspace.getBoolean(QLabUtil.FLD_HAS_PASSCODE, false);
		m_version = jsonWorkspace.getString(QLabUtil.FLD_VERSION, "");
	}
	
	@Override
	public String toString() {
		return "QLabWorkspaceInfo[id=" + m_uniqueId + ", displayName=" + m_displayName + ", hasPasscode="
				+ m_hasPasscode + ", version=" + m_version + "]";
	}
}
