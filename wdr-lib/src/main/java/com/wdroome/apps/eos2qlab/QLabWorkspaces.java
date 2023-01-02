package com.wdroome.apps.eos2qlab;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import java.util.List;

import com.wdroome.osc.qlab.QLabWorkspaceInfo;
import com.wdroome.osc.qlab.QueryQLab;

public class QLabWorkspaces
{
	private List<QLabWorkspaceInfo> m_workspaces = null;
	private QLabWorkspaceInfo m_selected = null;
	
	public QLabWorkspaces(QueryQLab queryQLab) throws IOException
	{
		if (queryQLab != null) {
			m_workspaces = queryQLab.getWorkspaces();
		} else {
			m_workspaces = List.of();
		}
	}
	
	public QLabWorkspaceInfo getSelectedWS(ReadResponse response)
	{
		if (m_selected == null && m_workspaces != null && !m_workspaces.isEmpty()) {
			if (m_workspaces.size() == 1) {
				m_selected = m_workspaces.get(0);
			} else {
				response.println("Select QLab Workspace:");
				for (int i = 0; i < m_workspaces.size(); i++) {
					QLabWorkspaceInfo ws = m_workspaces.get(i);
					response.println("  " + (i+1) + ": \"" + ws.m_displayName + "\" version:" + ws.m_version);
				}
				Integer iSel = response.getIntResponse("> ", -1, 1, m_workspaces.size());
				if (iSel != null) {
					m_selected = m_workspaces.get(iSel-1);
				}
			}
		}
		if (m_selected == null) {
			response.println("No QLab workspace is selected.");
		}
		return m_selected;
	}
	
	public String getSelectedID(ReadResponse response)
	{
		QLabWorkspaceInfo ws = getSelectedWS(response);
		return ws != null ? ws.m_uniqueId : null;
	}
	
	public boolean deselectIfChanged(QueryQLab queryQLab) throws IOException
	{
		boolean changed = false;
		if (queryQLab != null) {
			List<QLabWorkspaceInfo> newWorkspaces = queryQLab.getWorkspaces();
			if (!newWorkspaces.equals(m_workspaces)) {
				m_selected = null;
				m_workspaces = newWorkspaces;
				changed = true;
			}
		}
		return changed;
	}
	
	public void deselect()
	{
		m_selected = null;
	}
}
