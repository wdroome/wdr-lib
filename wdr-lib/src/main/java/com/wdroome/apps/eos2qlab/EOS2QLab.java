package com.wdroome.apps.eos2qlab;

import java.io.IOException;
import java.io.Closeable;
import java.io.PrintStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.Stack;

import java.util.function.BiPredicate;

import com.wdroome.util.MiscUtil;

import com.wdroome.osc.eos.QueryEOS;
import com.wdroome.osc.eos.EOSCueInfo;
import com.wdroome.osc.eos.EOSCuelistInfo;
import com.wdroome.osc.eos.EOSUtil;
import com.wdroome.osc.eos.EOSCueNumber;

import com.wdroome.osc.qlab.QueryQLab;
import com.wdroome.osc.qlab.QLabReply;
import com.wdroome.osc.qlab.QLabCue;
import com.wdroome.osc.qlab.QLabCuelistCue;
import com.wdroome.osc.qlab.QLabGroupCue;
import com.wdroome.osc.qlab.QLabNetworkCue;
import com.wdroome.osc.qlab.QLabUtil;
import com.wdroome.osc.qlab.QLabCueType;
import com.wdroome.osc.qlab.QLabWorkspaceInfo;
import com.wdroome.osc.qlab.QLabNetworkPatchInfo;

public class EOS2QLab implements Closeable
{
	private Config m_config = null;
	private final PrintStream m_out;
	private final ReadResponse m_response;
	
	private QueryEOS m_queryEOS = null;
	private QueryQLab m_queryQLab = null;
	private String m_eosShowName = null;
	private String m_qlabWorkspaceName = null;
	private VersionInfo m_versionInfo = null;
	private QLabWorkspaces m_qlabWorkspaces = null;
	private String m_selectedWorkspaceID = null;
	
	// EOS cuelist number => cuelist information.
	private TreeMap<Integer, EOSCuelistInfo> m_eosCuelists = null;
	
	// EOS cuelist number => map of all cues in cuelist
	private TreeMap<Integer, TreeMap<EOSCueNumber, EOSCueInfo>> m_eosCuesByList = null;
	
	// EOS cue numbers to cue for all lists.
	private TreeMap<EOSCueNumber, EOSCueInfo> m_eosCuesByNumber= null;
	
	// QLab cuelists. QLabCuelistCue contains the tree of cues in that list.
	private List<QLabCuelistCue> m_qlabCuelists = null;
	
	// QLab network cues with EOS fire commands, as a map from EOS cue number to the QLab cue data.
	private TreeMap<EOSCueNumber, QLabNetworkCue> m_eosCuesInQLab = null;
	
	private EOSCuesNotInQLab m_notInQLab = null;
	
	// QLab network cues with EOS fire commands for cues NOT in EOS.
	// The key is the name of a QLab cuelist, the value is the invalid cues in that list.
	// If all  network cues in a cuelist are valid, there's no entry for that cuelist.
	private TreeMap<String, List<QLabNetworkCue>> m_notInEOS = null;
	
	// QLab network cues not in EOS cue order.
	// That is, the network cue's EOS cue number is less than
	// the previous network cue in the QLab list.
	// The key is the name of a QLab cuelist, the value is the misordered cues in that list.
	// If all network cues in a cuelist are in order, there's no entry for that cuelist.
	private TreeMap<String, List<QLabNetworkCue>> m_misorderedNetworkCues = null;
	
	public EOS2QLab(String[] args, PrintStream out, InputStream in)
			throws IOException, IllegalArgumentException
	{
		m_out = (out != null) ? out : System.out;
		m_response = new ReadResponse((in != null) ? in : System.in, m_out);
		m_config = new Config(args);
		
		m_out.println("Connecting to EOS & QLab ...");
		MiscUtil.runTasksAndWait(List.of(
				() -> m_queryEOS = QueryEOS.makeQueryEOS(m_config.getEOSAddrPorts(),
										m_config.getConnectTimeoutMS()),
				() -> m_queryQLab = QueryQLab.makeQueryQLab(m_config.getQLabAddrPorts(),
										m_config.getConnectTimeoutMS())),
				"Connect");
		m_versionInfo = new VersionInfo(m_queryEOS, m_queryQLab, m_out);

		if (m_queryEOS != null && m_queryQLab != null) {
			m_out.println("Connected to EOS at " + m_queryEOS.getIpAddrString()
						+ " and QLab at " + m_queryQLab.getIpAddrString() + ".");
		} else if (m_queryEOS != null && m_queryQLab == null) {
			m_out.println("Connected to EOS at " + m_queryEOS.getIpAddrString()
						+ " but not to QLab.");
		} else if (m_queryEOS == null && m_queryQLab != null) {
			m_out.println("Connected to QLab at " + m_queryQLab.getIpAddrString()
						+ " but not to EOS.");
		} else {
			m_out.println("Not connected to EOS or QLab.");
		}
		
		m_qlabWorkspaces = new QLabWorkspaces(m_queryQLab);
		if (m_queryQLab != null) {
			String id = m_qlabWorkspaces.getSelectedID(m_response);
			if (id != null) {
				m_selectedWorkspaceID = id;
				m_queryQLab.setTargetWorkspace(id);
			}
		}
		
		getEOSAndQLabCues(true);
	}
	
	public EOS2QLab(String[] args) throws IOException, IllegalArgumentException
	{
		this(args, null, null);
	}
	
	private boolean m_eosOk = false;
	private boolean m_qlabOk = false;
	
	public boolean getEOSAndQLabCues(boolean alwaysGet)
	{
		m_notInQLab = null;
		m_notInEOS = null;
		ByteArrayOutputStream eosBuff = new ByteArrayOutputStream();
		PrintStream eosOut = new PrintStream(eosBuff);
		m_eosOk = true;
		ByteArrayOutputStream qlabBuff = new ByteArrayOutputStream();
		PrintStream qlabOut = new PrintStream(qlabBuff);
		m_qlabOk = true;
		ArrayList<Runnable> tasks = new ArrayList<>();
		if (alwaysGet || (m_eosCuelists == null || m_eosCuesByNumber == null)) {
			tasks.add(() -> m_eosOk = getEOSCues(eosOut));
		}
		if (alwaysGet || (m_qlabCuelists == null)) {
			tasks.add(() -> m_qlabOk = getQLabCues(qlabOut));
		}
		if (!tasks.isEmpty()) {
			m_out.println("Getting cue list(s) ...");
		}
		MiscUtil.runTasksAndWait(tasks, "GetCues");
		prtOutBuff(eosOut, eosBuff);
		prtOutBuff(qlabOut, qlabBuff);
		return m_eosOk && m_qlabOk;
	}
	
	private void prtOutBuff(PrintStream stream, ByteArrayOutputStream buff)
	{
		stream.flush();
		String v = buff.toString();
		if (!v.isBlank()) {
			m_out.print(v);
			if (!v.endsWith("\n")) {
				m_out.println();
			}
		}
	}
	
	public boolean getEOSCues()
	{
		m_notInQLab = null;
		m_notInEOS = null;
		return getEOSCues(m_out);
	}
	
	private boolean getEOSCues(PrintStream out)
	{
		m_notInQLab = null;
		m_notInEOS = null;
		if (m_queryEOS == null) {
			out.println("  *** Not connected to EOS controller.");
			return false;
		}
		try {
			out.println("EOS Show:");
			out.println("  Address: " + m_queryEOS.getIpAddrString());
			m_eosShowName = m_queryEOS.getShowName();
			out.println("  \"" + m_eosShowName + "\"  version: " + m_queryEOS.getVersion());

			m_eosCuelists = m_queryEOS.getCuelists();
			m_eosCuesByList = new TreeMap<>();
			m_eosCuesByNumber = new TreeMap<>();
			int nEOSCues = 0;
			for (EOSCuelistInfo cuelist: m_eosCuelists.values()) {
				int cuelistNum = cuelist.getCuelistNumber();
				TreeMap<EOSCueNumber, EOSCueInfo> cuesInList = m_queryEOS.getCues(cuelistNum);
				// System.out.println("XXX: EOS cuelist " + cuelistNum + ": " + cuesInList);
				m_eosCuesByList.put(cuelistNum, cuesInList);
				nEOSCues += cuesInList.size();
				m_eosCuesByNumber.putAll(cuesInList);
			}
			out.println("  " + nEOSCues + " cues in " + m_eosCuelists.size() + " cuelist(s).");
			m_config.setSingleEOSCuelist(m_eosCuelists.size() == 1);
			return true;
		} catch (IOException e) {
			out.println("Error connecting to EOS at " + m_queryEOS.getIpAddrString() + ": " + e);
			return false;
		}
	}
	
	public boolean getQLabCues()
	{
		m_notInQLab = null;
		m_notInEOS = null;
		return getQLabCues(m_out);
	}
	
	private boolean getQLabCues(PrintStream out)
	{
		if (m_queryQLab == null) {
			out.println("  *** Not connected to QLab controller.");
			return false;
		}
		m_queryQLab.setErrOut(out);
		try {
			out.println("QLab Workspaces:");
			out.println("  Address: " + m_queryQLab.getIpAddrString());
			List<QLabWorkspaceInfo> qlabWorkspaces = m_queryQLab.getWorkspaces();
			for (QLabWorkspaceInfo ws: qlabWorkspaces) {
				out.println("  \"" + ws.m_displayName + "\"  version: " + ws.m_version
						+ (ws.m_uniqueId.equals(m_selectedWorkspaceID) ? " *** selected ***" : ""));
			}
			int nQLabCues = refreshQLabCuelists();
			String activeWS = m_queryQLab.getLastReplyWorkspaceId();
			if (activeWS != null) {
				for (QLabWorkspaceInfo ws: qlabWorkspaces) {
					if (ws.m_uniqueId != null && ws.m_displayName != null
								&& activeWS.equals(ws.m_uniqueId)) {
						m_qlabWorkspaceName = ws.m_displayName;
						break;
					}
				}
			}
			out.println("  " + nQLabCues + " cues in " + m_qlabCuelists.size() + " cuelist(s)"
						+ (m_qlabWorkspaceName != null ? (" in \"" + m_qlabWorkspaceName + "\"") : "")
						+ ".");
			return true;
		} catch (IOException e) {
			out.println("Error connecting to QLab at " + m_queryQLab.getIpAddrString() + ": " + e);
			return false;
		} finally {
			if (m_queryQLab != null) {
				m_queryQLab.setErrOut(null);
			}
		}
	}
	
	private int refreshQLabCuelists() throws IOException
	{
		if (m_queryQLab == null) {
			m_out.println("  *** Not connected to QLab controller.");
			return 0;
		}
		m_qlabCuelists = m_queryQLab.getAllCueLists();
		m_eosCuesInQLab = new TreeMap<>();
		m_misorderedNetworkCues = new TreeMap<>();
		int nQLabCues = 0;
		for (QLabCuelistCue cuelist: m_qlabCuelists) {
			nQLabCues += cuelist.walkCues(new QLabCueHandler(cuelist.getName()));
		}
		return nQLabCues;
	}
	
	private class QLabCueHandler implements BiPredicate<QLabCue, Stack<? extends QLabCue>>
	{
		private EOSCueNumber m_prevCueNum = null;
		private final String m_qlabCuelistName;
		
		private QLabCueHandler(String qlabCuelistName)
		{
			m_qlabCuelistName = qlabCuelistName;
		}
		
		@Override
		public boolean test(QLabCue testCue, Stack<? extends QLabCue> path) {
			if (testCue instanceof QLabNetworkCue) {
				EOSCueNumber eosCueNum = ((QLabNetworkCue)testCue).getEosCueNumber();
				if (eosCueNum != null) {
					m_eosCuesInQLab.put(eosCueNum, (QLabNetworkCue)testCue);
					if (m_prevCueNum != null && eosCueNum.compareTo(m_prevCueNum) < 0) {
						List<QLabNetworkCue> list = m_misorderedNetworkCues.get(m_qlabCuelistName);
						if (list == null) {
							list = new ArrayList<>();
							m_misorderedNetworkCues.put(m_qlabCuelistName, list);
						}
						list.add((QLabNetworkCue)testCue);
					}
					m_prevCueNum = eosCueNum;
				}
			}
			return true;
		}
		
	}
	
	@Override
	public void close()
	{
		if (m_queryEOS != null) {
			try {m_queryEOS.close();} catch (Exception e) {}
			m_queryEOS = null;
		}
		if (m_queryQLab != null) {
			try {m_queryQLab.close();} catch (Exception e) {}
			m_queryQLab = null;
		}
	}
	
	/**
	 * Set m_notInQLab to the EOS cues that aren't in QLab.
	 * @return true if successful, false if we cannot connect to the servers.
	 */

	public boolean notInQLab()
	{
		if (!getEOSAndQLabCues(false)) {
			return false;
		}
		m_notInQLab = new EOSCuesNotInQLab(m_eosCuesByList, m_eosCuesInQLab);
		return true;
	}
	
	/**
	 * Get the QLab network cues which are not in EOS.
	 */
	public void notInEOS()
	{
		if (!getEOSAndQLabCues(false)) {
			return;
		}
		TreeMap<String, List<QLabNetworkCue>> notInEOS2 = new TreeMap<>();
		for (QLabCuelistCue cuelistCue: m_qlabCuelists) {
			cuelistCue.walkCues(
					(testCue, path) -> {
					if (testCue instanceof QLabNetworkCue) {
						EOSCueNumber eosCueNum = ((QLabNetworkCue)testCue).getEosCueNumber();
						if (eosCueNum != null && !m_eosCuesByNumber.containsKey(eosCueNum)) {
							String cuelistName = cuelistCue.getName();
							if (cuelistName == null || cuelistName.isBlank()) {
								cuelistName = QLabUtil.DEFAULT_CUELIST_NAME;
							}
							List<QLabNetworkCue> inCuelist = notInEOS2.get(cuelistName);
							if (inCuelist == null) {
								inCuelist = new ArrayList<>();
								notInEOS2.put(cuelistName, inCuelist);
							}
							inCuelist.add((QLabNetworkCue)testCue);
						}
					}
					return true;
				});
		}
	
		m_notInEOS = notInEOS2;
	}
	
	/**
	 * Find an EOS cue.
	 * @param cueNum The cue number.
	 * @return The EOS cue, or null if there is no such cue.
	 */
	public EOSCueInfo getEOSCue(EOSCueNumber cueNum)
	{
		if (cueNum == null || m_eosCuesByNumber == null) {
			return null;
		}
		return m_eosCuesByNumber.get(cueNum);
	}
	
	public void prtEOSCueSummary()
	{
		if (m_eosCuelists == null) {
			if (!getEOSCues()) {
				return;
			}
		}
		for (EOSCuelistInfo cuelist: m_eosCuelists.values()) {
			cuelist.getCueCount();
			int cuelistNum = cuelist.getCuelistNumber();
			String label = cuelist.getLabel();
			if (label != null && !label.isBlank()) {
				label = " \"" + label + "\"";
			} else {
				label = "";
			}
			int nNonAutoCues = 0;
			int nCues = 0;
			TreeMap<EOSCueNumber, EOSCueInfo> cuesInList = m_eosCuesByList.get(cuelistNum);
			if (cuesInList != null) {
				nCues = cuesInList.size();
				for (EOSCueInfo cue : cuesInList.values()) {
					if (!cue.isAutoCue() && !cue.getCueNumber().isPart()) {
						nNonAutoCues++;
					}
				} 
			}
			m_out.println("EOS Cuelist " + cuelistNum + ": "
								+ nNonAutoCues + " non-auto cues, "
								+ nCues + " total cues" + label);
		}
	}
	
	public void prtQLabCueSummary()
	{
		if (m_qlabCuelists == null) {
			if (!getQLabCues()) {
				return;
			}
		}
		for (QLabCuelistCue cuelist: m_qlabCuelists) {
			List<QLabCue> cues = cuelist.getChildren();
			if (cues == null) {
				cues = new ArrayList<>();
			}
			String indent = "   ";
			String name = cuelist.getName();
			QLabCueStats cueStats = qlabCueStats(cues, null);			
			m_out.println("QLab cuelist \"" + name + "\": "
							+ cueStats.m_nNonAutoCues + " non-auto cues"
							+ ", " + cueStats.m_nCues + " total cues"
							+ ", " + cueStats.m_nBrokenCues + " broken cues.");
			if (!cueStats.m_counts.isEmpty()) {
				String sep = "";
				int nTypes = 0;
				for (Map.Entry<QLabCueType, ACount> entry : cueStats.m_counts.entrySet()) {
					if (nTypes % 7 == 0) {
						if (nTypes > 0) {
							m_out.println();
						}
						m_out.print(indent + indent);
						sep = "";
					}
					m_out.print(sep + entry.getValue().count + " " + entry.getKey().toString());
					sep = ", ";
					nTypes++;
				}
				m_out.println();
			}
		}
	}
	
	public void prtCuesNotInQLab(boolean prtRanges, boolean prtCues)
	{
		if (m_notInQLab == null && !notInQLab()) {
			m_out.println("Cannot connect to QLab or EOS.");
			return;
		}
		if (m_notInQLab.isEmpty()) {
			m_out.println("All EOS Cues are in QLab");
		} else {
			String indent = "   ";
			for (Map.Entry<Integer, List<EOSCueInfo>> ent
								: m_notInQLab.m_missingCuesByList.entrySet()) {
				m_out.println("EOS cuelist " + ent.getKey() + ": "
											+ ent.getValue().size() + " cue(s) not in QLab");
				if (prtCues) {
					int nCues = 0;
					m_out.print(indent + (prtRanges ? "Cues: ": ""));
					for (EOSCueInfo eosCue: ent.getValue()) {
						if ((nCues % 10) != 0) {
							m_out.print(", ");
						} else if (nCues > 0) {
							m_out.println();
							m_out.print(indent);
						}
						m_out.print(eosCue.getCueNumber());
						nCues++;
					}
					m_out.println();
				}
				if (prtRanges) {
					int nRanges = 0;
					m_out.print(indent + (prtCues ? "Ranges: ": ""));
					for (EOSCuesNotInQLab.EOSCueRange range:
										m_notInQLab.m_missingCueRanges.get(ent.getKey())) {
						if ((nRanges % 5) != 0) {
							m_out.print(",  ");
						} else if (nRanges > 0) {
							m_out.println();
							m_out.print(indent);
						}
						m_out.print(range.m_start.getCueNumber() + "-"
										+ range.m_end.getCueNumber()
										+ " [" + range.m_nCues + "]");
						nRanges++;
					} 
					m_out.println();					
				}
			}
		}
	}
	
	public void prtCuesNotInEOS()
	{
		if (m_notInEOS == null) {
			notInEOS();
			if (m_notInEOS == null) {
				m_out.println("Cannot connect to EOS.");
				return;
			}
		}
		if (m_misorderedNetworkCues != null && !m_misorderedNetworkCues.isEmpty()) {
			m_out.println(m_misorderedNetworkCues.size() + " QLab Network cues may be out of order.");
		}
		if (m_notInEOS.isEmpty()) {
			m_out.println("All QLab network cues are in EOS.");
		} else {
			String indent = "   ";
			int nTotalCues = 0;
			for (List<QLabNetworkCue> cueSet: m_notInEOS.values()) {
				nTotalCues += cueSet.size();
			}
			m_out.println(nTotalCues + " QLab network cue(s) are not in EOS (QLab# => EOS#):");
			for (Map.Entry<String, List<QLabNetworkCue>> ent: m_notInEOS.entrySet()) {
				int nCues = 0;
				m_out.println(indent + "Cuelist " + ent.getKey() + ":");
				m_out.print(indent + indent);
				for (QLabNetworkCue cue: ent.getValue()) {
					if ((nCues % 10) != 0) {
						m_out.print("  ");
					} else if (nCues > 0) {
						m_out.println();
						m_out.print(indent + indent);
					}
					String number = cue.m_number;
					if (number.isBlank()) {
						number = "()";
					}
					m_out.print(number + " => " + cue.getEosCueNumber());
					nCues++;
				}
				m_out.println();
			}
		}
	}
	
	public void selectCuesNotInEOS() throws IOException
	{
		if (m_queryQLab == null) {
			m_out.println("  *** Not connected to QLab controller.");
			return;
		}
		if (m_notInEOS == null) {
			notInEOS();
		}
		if (m_notInEOS.isEmpty()) {
			m_out.println("All QLab network cues are in EOS.");
		} else {
			String cuelistName = pickCuelist(m_notInEOS);
			if (cuelistName == null) {
				return;
			}
			StringBuffer cueIds = new StringBuffer();
			String sep = "";
			for (QLabCue cue: m_notInEOS.get(cuelistName)) {
				cueIds.append(sep + cue.m_uniqueId);
				sep = ",";
			}
			if (cueIds.isEmpty()) {
				m_out.println("All QLab network cues in that list are in EOS.");
				return;
			}
			QLabReply reply = m_queryQLab.sendQLabReq(
							String.format(QLabUtil.SELECT_CUE_ID, cueIds.toString()));
			if (reply.isOk()) {
				m_out.println("Selected " + m_notInEOS.get(cuelistName).size() + " cues.");
			} else {
				m_out.println("Select request failed.");
			}
		}
	}
	
	public void selectMisorderedCues() throws IOException
	{
		if (m_queryQLab == null) {
			m_out.println("  *** Not connected to QLab controller.");
			return;
		}
		if (m_misorderedNetworkCues == null || m_misorderedNetworkCues.isEmpty()) {
			m_out.println("All QLab network cues in order.");
		} else {
			String cuelistName = pickCuelist(m_misorderedNetworkCues);
			if (cuelistName == null) {
				return;
			}
			StringBuffer cueIds = new StringBuffer();
			String sep = "";
			for (QLabCue cue: m_misorderedNetworkCues.get(cuelistName)) {
				cueIds.append(sep + cue.m_uniqueId);
				sep = ",";
			}
			if (cueIds.isEmpty()) {
				m_out.println("All QLab network cues in that list are in EOS order.");
				return;
			}
			QLabReply reply = m_queryQLab.sendQLabReq(
							String.format(QLabUtil.SELECT_CUE_ID, cueIds.toString()));
			if (reply.isOk()) {
				m_out.println("Selected " + m_misorderedNetworkCues.get(cuelistName).size()
											+ " cues.");
			} else {
				m_out.println("Select request failed.");
			}
		}
	}
	
	private String pickCuelist(TreeMap<String, List<QLabNetworkCue>> cuelistMap)
	{
		Set<String> cuelistNames = cuelistMap.keySet();
		if (cuelistNames.size() == 0) {
			return null;
		} else if (cuelistNames.size() == 1) {
			for (String cuelistName: cuelistNames) {
				return cuelistName;
			}
		}
		ArrayList<String> choices = new ArrayList<>();
		m_out.println("Pick QLab cue list -- you can only select one:");
		int iCuelist = 0;
		int iDefault = -1;
		for (Map.Entry<String, List<QLabNetworkCue>> ent: cuelistMap.entrySet()) {
			String defaultLabel = "";
			if (m_config.getDefaultQLabCuelist().equals(ent.getKey()) && iDefault < 0) {
				iDefault = iCuelist;
				defaultLabel = " (default)";
			}
			m_out.println("   " + (iCuelist+1) + ": \"" + ent.getKey() + "\": "
								+ ent.getValue().size() + " cues" + defaultLabel);
			choices.add(ent.getKey());
			iCuelist++;
		}
		Integer iResp = m_response.getIntResponse("Enter cuelist number, " + Commands.QUIT_CMD[0]
								+ (iDefault >= 0 ? ", or return for default" : "") + ":",
								iDefault+1, 1, choices.size());
		if (iResp == null) {
			return null;
		} else {
			return choices.get(iResp-1);
		}
	}
	
	public boolean add2QLab() throws IOException
	{
		if (m_notInQLab == null && !notInQLab()) {
			m_out.println("Cannot connect to QLab or EOS.");
			return false;
		}
		if (m_notInQLab.isEmpty()) {
			m_out.println("All EOS Cues are in QLab");
			return false;
		} else if (m_queryQLab == null) {
			m_out.println("  Not connected to QLab server.");
			return false;
		}
		Add2QLabCommand addCmd = new Add2QLabCommand(m_config,
													 m_queryQLab,
													 m_response,
													 m_qlabCuelists,
													 m_eosCuesInQLab,
													 m_notInQLab);
		int nAdded = addCmd.addCues();
 		m_out.println("Added " + nAdded + " cues to QLab.");
		if (nAdded > 0) {
			getQLabCues();
			m_notInQLab = null;
		}
		return nAdded > 0;
	}
	
	public void prtConfig(String lineIndent) throws IOException
	{
		m_config.prtConfigFile(m_out, lineIndent);
	}
	
	private static class ACount
	{
		private int count = 0;
		private void incr() { count++; }
	}
	
	private static class QLabCueStats
	{
		private TreeMap<QLabCueType,ACount> m_counts = new TreeMap<>();
		private int m_nNonAutoCues = 0;
		private int m_nBrokenCues = 0;
		private int m_nCues;
	}
	
	private static QLabCueStats qlabCueStats(List<QLabCue> cues, QLabCueStats cueStats)
	{
		if (cueStats == null) {
			cueStats = new QLabCueStats();
		}
		if (cues != null) {
			for (QLabCue cue : cues) {
				cueStats.m_nCues++;
				if (cue.isAuto()) {
					cueStats.m_nNonAutoCues++;
				}
				if (cue.m_isBroken) {
					cueStats.m_nBrokenCues++;
				}
				ACount cnt = cueStats.m_counts.get(cue.m_type);
				if (cnt == null) {
					cnt = new ACount();
					cueStats.m_counts.put(cue.m_type, cnt);
				}
				cnt.incr();
				qlabCueStats(cue.getChildren(), cueStats);
			} 
		}
		return cueStats;
	}
	
	public void testCueNum()
	{
		EOSCueNumber cue = new EOSCueNumber("1/0.1");
		for (Map.Entry<EOSCueNumber, EOSCueInfo> ent:
									m_eosCuesByNumber.entrySet()) {
			m_out.println("Key: " + ent.getKey().toFullString() + " equals: "
						+ (ent.getKey().equals(cue)));
		}
		m_out.println("Get: " + m_eosCuesByNumber.get(cue));
		m_out.println("Get2: " + getEOSCue(cue));
	}
	
	public void testReplace(String eosCue, String[] testStrs)
	{
		try {
			EOSCueNumber cueNumber = new EOSCueNumber(eosCue);
			EOSCueInfo cue = getEOSCue(cueNumber);
			if (cue == null) {
				throw new IllegalArgumentException("Invalid cue number");
			}
			for (String testStr: testStrs) {
				m_out.println(testStr + ": " + m_config.replaceVars(testStr, cue));
			}
		} catch (IllegalArgumentException e) {
			m_out.println("Invalid cue number '" + eosCue + "'");
			e.printStackTrace();
		}
	}
}
