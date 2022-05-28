package com.wdroome.apps.eos2qlab;

import java.util.Map;
import java.util.HashMap;

import com.wdroome.osc.eos.EOSCueInfo;
import com.wdroome.osc.eos.EOSCueNumber;

import com.wdroome.osc.qlab.QLabUtil;

public class Config
{
	public String m_QLabAddrPort = "127.0.0.1:53000";
	public String m_EOSAddrPort =
								"192.168.0.113:8192";
								// "127.0.0.1:8192";
	
	public int m_newCueNetworkPatch = 1;
	public String[] m_newCueNumberFmt = {"q%cuenumber%", "q%cuelist%-%cuenumber%"};
	public String[] m_newCueNameFmt = {"Light cue %cuenumber%",
										"Light cue %cuelist%/%cuenumber%"};
	public String m_newCueNameSceneSuffixFmt = " ***** Scene %scene%";
	public String m_newCueNameSceneEndSuffix = " ***** Scene End";
	public QLabUtil.ColorName m_newCueColor = QLabUtil.ColorName.RED;
	public boolean m_newCueFlag = true;
	
	private boolean m_isSingleEOSCuelist = false;
	
	public String replaceVars(String fmt, EOSCueInfo cue)
	{
		return fmt
				.replace("%cuelist%", "" + cue.getCueNumber().getCuelist())
				.replace("%list%", "" + cue.getCueNumber().getCuelist())
				.replace("%cuenumber%", cue.getCueNumber().getCueNumber())
				.replace("%number%", cue.getCueNumber().getCueNumber())
				.replace("%scene%", cue.getScene())
				;
	}
	
	public void setSingleEOSCuelist(boolean isSingleEOSCuelist)
	{
		m_isSingleEOSCuelist = isSingleEOSCuelist;
		EOSCueNumber.setShowDefaultCuelist(!isSingleEOSCuelist);
	}
	
	public String makeNewCueNumber(EOSCueInfo cue)
	{
		return replaceVars(m_newCueNumberFmt[m_isSingleEOSCuelist ? 0 : 1], cue);
	}
	
	public String makeNewCueName(EOSCueInfo cue)
	{
		String name;
		String label = cue.getLabel();
		if (label != null && !label.isBlank()) {
			name = label;
		} else {
			name = replaceVars(m_newCueNameFmt[m_isSingleEOSCuelist ? 0 : 1], cue);
		}
		String scene = cue.getScene();
		if (scene != null && !scene.isBlank()
				&& m_newCueNameSceneSuffixFmt != null
				&& !m_newCueNameSceneSuffixFmt.isBlank()) {
			name += replaceVars(m_newCueNameSceneSuffixFmt, cue);
		} else if (cue.isSceneEnd()
				&& m_newCueNameSceneEndSuffix != null
				&& !m_newCueNameSceneEndSuffix.isBlank()) {
			name += m_newCueNameSceneEndSuffix;
		}
		return name;
	}
}
