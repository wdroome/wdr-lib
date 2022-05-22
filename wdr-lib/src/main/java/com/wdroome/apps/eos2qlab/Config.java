package com.wdroome.apps.eos2qlab;

import java.util.Map;
import java.util.HashMap;

import com.wdroome.osc.eos.EOSCueNumber;

import com.wdroome.osc.qlab.QLabUtil;

public class Config
{
	public String m_QLabAddrPort = "127.0.0.1:53000";
	public String m_EOSAddrPort = "127.0.0.1:8192";
	public Map<Integer, String> m_cuelistMap = Map.of(1, "Main Cue List");
	
	public int m_newCueNetworkPatch = 1;
	public String[] m_newCueNumberFmt = {"q%cuenumber%", "q%cuelist%-%cuenumber%"};
	public String[] m_newCueLabelFmt = {"Light cue %cuenumber%", "Light cue %cuelist%/%cuenumber%"};
	public String m_newCueColor = QLabUtil.ColorName.RED.toQLab();
	public boolean m_newCueFlag = true;
	
	public String replaceVars(String fmt, EOSCueNumber cue)
	{
		return fmt
				.replace("%cuelist%", "" + cue.getCuelist())
				.replace("%list%", "" + cue.getCuelist())
				.replace("%cuenumber%", cue.getCueNumber())
				.replace("%number%", cue.getCueNumber())
				;
	}
}
