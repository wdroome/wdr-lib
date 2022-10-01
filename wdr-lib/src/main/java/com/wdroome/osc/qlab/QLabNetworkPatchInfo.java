package com.wdroome.osc.qlab;

import com.wdroome.json.JSONValue_Object;

/**
 * Information about a Network Patch. This is new in QLab 5.
 * @author wdr
 */
public class QLabNetworkPatchInfo
{
	public final int m_number;
	public final String m_uniqueID;
	public final String m_name;
	
	public QLabNetworkPatchInfo(int number, String uniqueID, String name)
	{
		m_number = number;
		m_uniqueID = uniqueID;
		m_name = name;
	}
	
	public QLabNetworkPatchInfo(int number, JSONValue_Object json)
	{
		m_number = number;
		m_uniqueID = json.getString(QLabUtil.FLD_UNIQUE_ID, "");
		m_name = json.getString(QLabUtil.FLD_NAME, "");
	}
	
	@Override
	public String toString()
	{
		return "NetworkPatch(" + m_number + ":" + m_name + "/" + m_uniqueID + ")";
	}

}
