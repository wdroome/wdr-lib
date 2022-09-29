package com.wdroome.apps.midi2osc;

import java.util.List;

import com.wdroome.json.JSONValue_Array;

/**
 * The read-only definition of an OSC action,
 * including the trigger conditions.
 * @author wdr
 */
public class ActionSpec
{
	public final ServerInfo m_oscServer;
	public final TypeChanKey m_tck;
	public final String m_encodedTCK;
	public final IntList m_data2List;
	public final List<JSONValue_Array> m_cmdTemplate;	// May have unresolved variables, like ${datas2}.
	public final int m_newValueWaitMS;
	public final int m_sameValueSkipMS;
	
	
	public ActionSpec(ServerInfo oscServer, TypeChanKey tck, IntList data2List,
						List<JSONValue_Array> cmdTemplate, int newValueWaitMS, int sameValueSkipMS)
	{
		m_oscServer = oscServer;
		m_tck = tck;
		m_data2List = data2List;
		m_cmdTemplate = cmdTemplate;
		m_encodedTCK = m_tck.m_encoded;
		m_newValueWaitMS = newValueWaitMS;
		m_sameValueSkipMS = sameValueSkipMS;
	}
	
	public boolean matchesData2(int data2)
	{
		return m_data2List.contains(data2);
	}

	@Override
	public String toString()
	{
		return "ActionSpec[oscServer=\"" + m_oscServer.m_name + "\", tck=" + m_tck
				+ ", data2List=" + m_data2List + ", cmds=" + m_cmdTemplate
				+ ", new/sameValueMS=" + m_newValueWaitMS + "/" + m_sameValueSkipMS + "]";
	}
}
