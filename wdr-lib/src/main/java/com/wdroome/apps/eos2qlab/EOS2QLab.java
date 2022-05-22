package com.wdroome.apps.eos2qlab;

import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;
import java.io.Closeable;

import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;

import com.wdroome.util.MiscUtil;

import com.wdroome.osc.eos.QueryEOS;
import com.wdroome.osc.eos.EOSCueInfo;
import com.wdroome.osc.eos.EOSCuelistInfo;
import com.wdroome.osc.eos.EOSCueNumber;

import com.wdroome.osc.qlab.QueryQLab;
import com.wdroome.osc.qlab.QLabCue;
import com.wdroome.osc.qlab.QLabCuelistCue;
import com.wdroome.osc.qlab.QLabGroupCue;
import com.wdroome.osc.qlab.QLabNetworkCue;
import com.wdroome.osc.qlab.QLabCueType;
import com.wdroome.osc.qlab.QLabWorkspaceInfo;

public class EOS2QLab implements Closeable
{
	private Config m_config = null;
	
	private QueryEOS m_queryEOS = null;
	private QueryQLab m_queryQLab = null;
	private String m_eosShowName = null;
	private String m_qlabWorkspaceName = null;
	
	private NotInQLabResults m_notInQLab = null;
	private List<QLabNetworkCue> m_notInEOS = null;
	
	// EOS cuelist number => cuelist information.
	private TreeMap<Integer, EOSCuelistInfo> m_eosCuelists = null;
	
	// EOS cuelist number => map of all cues in cuelist
	private TreeMap<Integer, TreeMap<EOSCueNumber, EOSCueInfo>> m_eosCues = null;
	
	// QLab cuelists. QLabCuelistCue contains the tree of cues in that list.
	private List<QLabCuelistCue> m_qlabCuelists = null;
	
	// QLab network cues with EOS fire commands, as a map from EOS cue number to the QLab cue data.
	private HashMap<EOSCueNumber, QLabNetworkCue> m_eosCuesInQLab = null;
	
	public EOS2QLab(String[] args) throws IOException
	{
		// XXX -- get config name & overrides from args
		m_config = new Config();
		
		m_queryEOS = new QueryEOS(m_config.m_EOSAddrPort);
		m_queryQLab = new QueryQLab(m_config.m_QLabAddrPort);
		
		getEOSCues();
		getQLabCues();
	}
	
	public boolean getEOSCues()
	{
		try {
			System.out.println("EOS Show:");
			m_eosShowName = m_queryEOS.getShowName();
			System.out.println("  \"" + m_eosShowName + "\"  version: " + m_queryEOS.getVersion());

			m_eosCuelists = m_queryEOS.getCuelists();
			m_eosCues = new TreeMap<>();
			int nEOSCues = 0;
			for (EOSCuelistInfo cuelist: m_eosCuelists.values()) {
				int cuelistNum = cuelist.getCuelistNumber();
				TreeMap<EOSCueNumber, EOSCueInfo> cuesInList = m_queryEOS.getCues(cuelistNum);
				m_eosCues.put(cuelistNum, cuesInList);
				nEOSCues += cuesInList.size();
			}
			System.out.println("  " + nEOSCues + " cues in " + m_eosCuelists.size() + " cuelist(s).");
			return true;
		} catch (IOException e) {
			System.err.println("Error connecting to EOS at " + m_config.m_EOSAddrPort + ": " + e);
			return false;
		}
	}
	
	public boolean getQLabCues()
	{
		try {
			System.out.println("QLab Workspaces:");
			List<QLabWorkspaceInfo> qlabWorkspaces = m_queryQLab.getWorkspaces();
			for (QLabWorkspaceInfo ws: qlabWorkspaces) {
				System.out.println("  \"" + ws.m_displayName + "\"  version: " + ws.m_version);
			}
			m_qlabCuelists = m_queryQLab.getAllCueLists();
			String activeWS = m_queryQLab.getLastReplyWorkspaceId();
			if (activeWS != null) {
				for (QLabWorkspaceInfo ws: qlabWorkspaces) {
					if (ws.m_uniqueId != null && ws.m_displayName != null && activeWS.equals(ws.m_uniqueId)) {
						m_qlabWorkspaceName = ws.m_displayName;
						break;
					}
				}
			}
			m_eosCuesInQLab = new HashMap<>();
			int nQLabCues = QLabCueType.walkCues(m_qlabCuelists,
					(testCue) -> {
						if (testCue instanceof QLabNetworkCue) {
							EOSCueNumber eosCueNum = ((QLabNetworkCue)testCue).m_eosCueNumber;
							if (eosCueNum != null) {
								m_eosCuesInQLab.put(eosCueNum, (QLabNetworkCue)testCue);
							}
						}
						return true;
					});
			System.out.println("  " + nQLabCues + " cues in " + m_qlabCuelists.size() + " cuelist(s)"
						+ (m_qlabWorkspaceName != null ? (" in \"" + m_qlabWorkspaceName + "\"") : "")
						+ ".");
			return true;
		} catch (IOException e) {
			System.err.println("Error connecting to QLab at " + m_config.m_QLabAddrPort + ": " + e);
			return false;
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
	
	public class EOSCueRange
	{
		public final EOSCueNumber m_start;
		public final EOSCueNumber m_end;
		public final int m_nCues;
		
		public EOSCueRange(EOSCueNumber start, EOSCueNumber end, int nCues)
		{
			m_start = start;
			m_end = end;
			m_nCues = nCues;
		}
	}
	
	public class NotInQLabResults
	{
		/** EOS cues not in QLab, grouped by EOS cuelist number. */
		public final TreeMap<Integer, List<EOSCueInfo>> m_missingCues;
		
		/** EOS cue ranges not in QLab. */
		public final TreeMap<Integer, List<EOSCueRange>> m_missingCueRanges;
		
		public NotInQLabResults(TreeMap<Integer, List<EOSCueInfo>> missingCues,
								TreeMap<Integer, List<EOSCueRange>> missingCueRanges)
		{
			m_missingCues = missingCues;
			m_missingCueRanges = missingCueRanges;
		}
	}
	
	/**
	 * Get the EOS cues not in QLab, grouped by EOS cue list.
	 * @return A Map from the EOS cuelist numbers to the EOS cues in that list
	 * 			which are not in QLab. Never returns null, but there is no
	 * 			map entry for a cuelist whose cues are all in QLab.
	 */
	public NotInQLabResults notInQLab()
	{
		if (m_eosCuelists == null) {
			if (!getEOSCues()) {
				return null;
			}
		}
		if (m_qlabCuelists == null) {
			if (!getQLabCues()) {
				return null;
			}
		}
		TreeMap<Integer, List<EOSCueInfo>> missingCues = new TreeMap<>();
		TreeMap<Integer, List<EOSCueRange>> missingRanges = new TreeMap<>();
		for (Map.Entry<Integer, TreeMap<EOSCueNumber, EOSCueInfo>> ent: m_eosCues.entrySet()) {
			int cuelistNum = ent.getKey();
			List<EOSCueInfo> cuesForCuelist = new ArrayList<>();
			List<EOSCueRange> rangesForCuelist = new ArrayList<>();
			EOSCueNumber rangeStart = null;
			EOSCueNumber rangeEnd = null;
			int rangeCnt = 0;
			for (EOSCueInfo eosCue: ent.getValue().values()) {
				EOSCueNumber eosCueNum = eosCue.getCueNumber();
				if (eosCue.isAutoCue() || eosCueNum.isPart()) {
					continue;
				}
				if (m_eosCuesInQLab.get(eosCueNum) == null) {
					cuesForCuelist.add(eosCue);
					if (rangeStart == null) {
						rangeStart = eosCueNum;
						rangeCnt = 0;
					}
					rangeEnd = eosCueNum;
					rangeCnt++;
				} else {
					if (rangeStart != null) {
						rangesForCuelist.add(new EOSCueRange(rangeStart, rangeEnd, rangeCnt));
						rangeStart = null;
						rangeEnd = null;
						rangeCnt = 0;
					}
				}
			}
			if (rangeStart != null) {
				rangesForCuelist.add(new EOSCueRange(rangeStart, rangeEnd, rangeCnt));
			}
			if (!cuesForCuelist.isEmpty()) {
				missingCues.put(cuelistNum, cuesForCuelist);
				missingRanges.put(cuelistNum, rangesForCuelist);
			}
		}
		m_notInQLab = new NotInQLabResults(missingCues, missingRanges);
		return m_notInQLab;
	}
	
	/**
	 * Get the QLab network cues which are not in EOS.
	 * @return The QLab network cues which are not in EOS.
	 * 		The list may be empty, but is never null.
	 */
	public List<QLabNetworkCue> notInEOS()
	{
		List<QLabNetworkCue> notInEOS = new ArrayList<>();
		for (Map.Entry<EOSCueNumber,QLabNetworkCue> ent: m_eosCuesInQLab.entrySet()) {
			if (getEOSCue(ent.getKey()) == null) {
				notInEOS.add(ent.getValue());
			}
		}
		m_notInEOS = notInEOS;
		return notInEOS;
	}
	
	/**
	 * Find an EOS cue.
	 * @param cueNum The cue number.
	 * @return The EOS cue, or null if there is no such cue.
	 */
	public EOSCueInfo getEOSCue(EOSCueNumber cueNum)
	{
		if (cueNum == null) {
			return null;
		}
		TreeMap<EOSCueNumber,EOSCueInfo> cuelist = m_eosCues.get(cueNum.getCuelist());
		if (cuelist == null) {
			return null;
		} else {
			return cuelist.get(cueNum);
		}
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
			TreeMap<EOSCueNumber, EOSCueInfo> cuesInList = m_eosCues.get(cuelistNum);
			if (cuesInList != null) {
				nCues = cuesInList.size();
				for (EOSCueInfo cue : cuesInList.values()) {
					if (!cue.isAutoCue() && !cue.getCueNumber().isPart()) {
						nNonAutoCues++;
					}
				} 
			}
			System.out.println("EOS Cuelist " + cuelistNum + ": "
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
			List<QLabCue> cues = cuelist.getCues();
			if (cues == null) {
				cues = new ArrayList<>();
			}
			String indent = "   ";
			String name = (!cuelist.m_name.isBlank()) ? cuelist.m_name : cuelist.m_listName;
			QLabCueStats cueStats = qlabCueStats(cues, null);			
			System.out.println("QLab cuelist \"" + name + "\": "
							+ cueStats.m_nNonAutoCues + " non-auto cues"
							+ ", " + cueStats.m_nCues + " total cues"
							+ ", " + cueStats.m_nBrokenCues + " broken cues.");
			if (!cueStats.m_counts.isEmpty()) {
				String sep = "";
				int nTypes = 0;
				for (Map.Entry<QLabCueType, ACount> entry : cueStats.m_counts.entrySet()) {
					if (nTypes % 7 == 0) {
						if (nTypes > 0) {
							System.out.println();
						}
						System.out.print(indent + indent);
						sep = "";
					}
					System.out.print(sep + entry.getValue().count + " " + entry.getKey().toString());
					sep = ", ";
					nTypes++;
				}
				System.out.println();
			}
		}
	}
	
	public void prtCuesNotInQLab(boolean prtRanges, boolean prtCues)
	{
		if (m_notInQLab == null) {
			notInQLab();
		}
		if (m_notInQLab.m_missingCues.isEmpty()) {
			System.out.println("All EOS Cues are in QLab");
		} else {
			String indent = "   ";
			for (Map.Entry<Integer, List<EOSCueInfo>> ent : m_notInQLab.m_missingCues.entrySet()) {
				System.out.println("EOS cuelist " + ent.getKey() + ": "
											+ ent.getValue().size() + " cue(s) not in QLab");
				if (prtCues) {
					int nCues = 0;
					System.out.print(indent + (prtRanges ? "Cues: ": ""));
					for (EOSCueInfo eosCue: ent.getValue()) {
						if ((nCues % 10) != 0) {
							System.out.print(", ");
						} else if (nCues > 0) {
							System.out.println();
							System.out.print(indent);
						}
						System.out.print(eosCue.getCueNumber());
						nCues++;
					}
					System.out.println();
				}
				if (prtRanges) {
					int nRanges = 0;
					System.out.print(indent + (prtCues ? "Ranges: ": ""));
					for (EOSCueRange range : m_notInQLab.m_missingCueRanges.get(ent.getKey())) {
						if ((nRanges % 5) != 0) {
							System.out.print(",  ");
						} else if (nRanges > 0) {
							System.out.println();
							System.out.print(indent);
						}
						System.out.print(range.m_start.getCueNumber() + "-"
										+ range.m_end.getCueNumber()
										+ " [" + range.m_nCues + "]");
						nRanges++;
					} 
					System.out.println();					
				}
			}
		}
	}
	
	public void prtCuesNotInEOS()
	{
		if (m_notInEOS == null) {
			notInEOS();
		}
		if (m_notInEOS.isEmpty()) {
			System.out.println("All QLab network cues are in EOS.");
		} else {
			String indent = "   ";
			System.out.println(m_notInEOS.size() + " QLab network cue(s) are not in EOS (QLab# => EOS#):");
			int nCues = 0;
			System.out.print(indent);
			for (QLabNetworkCue cue: m_notInEOS) {
				if ((nCues % 10) != 0) {
					System.out.print("  ");
				} else if (nCues > 0) {
					System.out.println();
					System.out.print(indent);
				}
				String number = cue.m_number;
				if (number.isBlank()) {
					number = "()";
				}
				System.out.print(number + " => " + cue.m_eosCueNumber);
				nCues++;
			}
			System.out.println();
		}
	}
		
	public static void main(String[] args) throws IOException
	{
		boolean running = true;

		while (running) {
			try (EOS2QLab eos2QLab = new EOS2QLab(args)) {
				eos2QLab.prtEOSCueSummary();
				eos2QLab.prtQLabCueSummary();
				while (true) {
					System.out.print("* ");
					String line = MiscUtil.readLine(System.in);
					if (line == null || line.equals("q") || line.equals("quit") || line.equals("exit")) {
						running = false;
						break;
					}
					line = line.trim();
					if (line.isBlank()) {
						continue;
					}
					String[] cmd = line.split("[ \t]+");
					if (cmd.length == 0) {
						continue;
					}
					if (cmd[0].equals("refresh")) {
						break;
					} else if (cmd[0].equals("prt")) {
						if (cmd.length >= 2 && cmd[1].equalsIgnoreCase("eos")) {
							eos2QLab.prtEOSCueSummary();							
						} else if (cmd.length >= 2 && cmd[1].equalsIgnoreCase("qlab")) {
							eos2QLab.prtQLabCueSummary();							
						} else if (cmd.length >= 2 && cmd[1].equalsIgnoreCase("missing")) {
							eos2QLab.prtCuesNotInQLab(true, true);
							eos2QLab.prtCuesNotInEOS();
						} else {
							eos2QLab.prtEOSCueSummary();							
							eos2QLab.prtQLabCueSummary();							
						}
					} else if (cmd[0].equalsIgnoreCase("cmp") || cmd[0].equalsIgnoreCase("compare")) {
						eos2QLab.notInQLab();
						eos2QLab.notInEOS();
						eos2QLab.prtCuesNotInQLab(true, false);							
						eos2QLab.prtCuesNotInEOS();							
					}
				}
			} 
		}
	}
	
	public static void XXmain(String[] args) throws IOException
	{
		Config config = new Config();
		System.out.println("EOS Address: " + config.m_EOSAddrPort);
		System.out.println("QLab Address: " + config.m_QLabAddrPort);
		QueryEOS queryEOS = new QueryEOS(config.m_EOSAddrPort);
		QueryQLab queryQLab = new QueryQLab(config.m_QLabAddrPort);
		
		System.out.println("EOS Show: \"" + queryEOS.getShowName()
					+ "\"  version: " + queryEOS.getVersion());
		System.out.println("QLab Workspaces:");
		List<QLabWorkspaceInfo> qlabWorkspaces = queryQLab.getWorkspaces();
		for (QLabWorkspaceInfo ws: qlabWorkspaces) {
			System.out.println("  \"" + ws.m_displayName + "\"  version: " + ws.m_version);
		}
		String indent = "   ";
		
		TreeMap<Integer, EOSCuelistInfo> eosCuelists = queryEOS.getCuelists();
		System.out.println("EOS: " + eosCuelists.size() + " cuelist(s).");
		TreeMap<Integer, TreeMap<EOSCueNumber, EOSCueInfo>> cuesByList = new TreeMap<>();
		for (EOSCuelistInfo cuelist: eosCuelists.values()) {
			cuelist.getCueCount();
			int cuelistNum = cuelist.getCuelistNumber();
			String label = cuelist.getLabel();
			if (label != null && !label.isBlank()) {
				label = " \"" + label + "\"";
			} else {
				label = "";
			}
			TreeMap<EOSCueNumber, EOSCueInfo> cues = queryEOS.getCues(cuelistNum);
			cuesByList.put(cuelist.getCuelistNumber(), cues);
			if (cues.size() != cuelist.getCueCount()) {
				System.out.println(indent + "**** cuelist " + cuelistNum + " count mismatch: "
								+ cues.size() + " != " + cuelist.getCueCount());
			}
			int nNonAutoCues = 0;
			for (EOSCueInfo cue: cues.values()) {
				if (!cue.isAutoCue() && !cue.getCueNumber().isPart()) {
					nNonAutoCues++;
				}
			}
			System.out.println(indent + "Cuelist " + cuelistNum + ": "
								+ nNonAutoCues + " non-auto cues, "
								+ cues.size() + " total cues" + label);
		}
		
		List<QLabCuelistCue> qlabCuelists = queryQLab.getAllCueLists();
		System.out.println("QLab: " + qlabCuelists.size() + " cuelist(s).");
		for (QLabCuelistCue cuelist: qlabCuelists) {
			List<QLabCue> cues = cuelist.getCues();
			if (cues == null) {
				cues = new ArrayList<>();
			}
			String name = (!cuelist.m_name.isBlank()) ? cuelist.m_name : cuelist.m_listName;
			QLabCueStats cueStats = qlabCueStats(cues, null);			
			System.out.println(indent + "Cuelist \"" + name + "\": "
							+ cueStats.m_nNonAutoCues + " non-auto cues"
							+ ", " + cueStats.m_nCues + " total cues"
							+ ", " + cueStats.m_nBrokenCues + " broken cues.");
			if (!cueStats.m_counts.isEmpty()) {
				System.out.println(indent + indent + "By type: ");
				String sep = "";
				int nTypes = 0;
				for (Map.Entry<QLabCueType, ACount> entry : cueStats.m_counts.entrySet()) {
					if (nTypes % 7 == 0) {
						if (nTypes > 0) {
							System.out.println();
						}
						System.out.print(indent + indent + indent);
						sep = "";
					}
					System.out.print(sep + entry.getValue().count + " " + entry.getKey().toString());
					sep = ", ";
					nTypes++;
				}
				System.out.println();
			}
		}
		
		//  EOS connect, get version/show, get cues
		// QLab connect, get version/file, get cues
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
				if (cue.m_isAuto) {
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
				if (cue instanceof QLabCuelistCue) {
					qlabCueStats(((QLabCuelistCue) cue).getCues(), cueStats);
				} else if (cue instanceof QLabGroupCue) {
					qlabCueStats(((QLabGroupCue) cue).getCues(), cueStats);
				}
			} 
		}
		return cueStats;
	}
}
