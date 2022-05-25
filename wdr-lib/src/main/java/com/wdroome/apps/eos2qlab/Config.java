package com.wdroome.apps.eos2qlab;

import java.util.Map;
import java.util.HashMap;

import com.wdroome.osc.eos.EOSCueNumber;

import com.wdroome.osc.qlab.QLabUtil;

public class Config
{
	public String m_QLabAddrPort = "127.0.0.1:53000";
	public String m_EOSAddrPort = "127.0.0.1:8192";
	
	public int m_newCueNetworkPatch = 1;
	public String[] m_newCueNumberFmt = {"q%cuenumber%", "q%cuelist%-%cuenumber%"};
	public String[] m_newCueNameFmt = {"Light cue %cuenumber%", "Light cue %cuelist%/%cuenumber%"};
	public QLabUtil.ColorName m_newCueColor = QLabUtil.ColorName.RED;
	public boolean m_newCueFlag = true;
	
	private boolean m_isSingleEOSCuelist = false;
	
	public String replaceVars(String fmt, EOSCueNumber cue)
	{
		return fmt
				.replace("%cuelist%", "" + cue.getCuelist())
				.replace("%list%", "" + cue.getCuelist())
				.replace("%cuenumber%", cue.getCueNumber())
				.replace("%number%", cue.getCueNumber())
				;
	}
	
	public void setSingleEOSCuelist(boolean isSingleEOSCuelist)
	{
		m_isSingleEOSCuelist = isSingleEOSCuelist;
	}
	
	public String makeNewCueNumber(EOSCueNumber eosCueNumber)
	{
		return replaceVars(m_newCueNumberFmt[m_isSingleEOSCuelist ? 0 : 1], eosCueNumber);
	}
	
	public String makeNewCueName(String eosName, EOSCueNumber eosCueNumber)
	{
		if (eosName != null && !eosName.isBlank()) {
			return eosName;
		} else {
			return replaceVars(m_newCueNameFmt[m_isSingleEOSCuelist ? 0 : 1], eosCueNumber);
		}
	}
}
