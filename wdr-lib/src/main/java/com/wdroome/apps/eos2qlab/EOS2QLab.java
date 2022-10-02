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
import java.util.Collection;
import java.util.Set;
import java.util.Stack;

import java.util.function.BiPredicate;

import com.wdroome.util.MiscUtil;
import com.wdroome.util.StringUtils;
import com.wdroome.util.HashCounter;

import com.wdroome.json.JSONParseException;
import com.wdroome.json.JSONValueTypeException;

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

public class EOS2QLab implements Closeable
{
	public final static String[] CHECK_CMD = {"check", "chk", "compare", "cmp"};
	public final static String[] PRINT_CMD = {"print", "prt"};
	public final static String[] QUIT_CMD = {"quit", "q", "exit"};
	public final static String[] REFRESH_CMD = {"refresh"};
	public final static String[] ADD_CMD = {"add", "add-cues"};
	public final static String[] SELECT_CMD = {"select", "sel"};
	public final static String[] HELP_CMD = {"help", "?"};
	
	public final static String[] EOS_ARG = {"eos"};
	public final static String[] QLAB_ARG = {"qlab"};
	public final static String[] MISSING_ARG = {"missing", "miss"};
	public final static String[] ORDER_ARG = {"order", "seqn"};
	public final static String[] CONFIG_ARG = {"config"};
	
	public final static String[] HELP_RESP = {
				"refresh: Get the cue information from EOS & QLab.",
				"check: Find the EOS cues not in QLab, and the QLab cues not in EOS.",
				"add: Add missing EOS cues to QLab.",
				"select missing: Select QLab network cues not in EOS.",
				"select order: Select QLab network cues not in EOS cue order.",
				"print eos: Print a summary of the EOS cues.",
				"print qlab: Print a summary of the QLab cues.",
				"print missing: Print the EOS cues not in QLab and the QLab cues not in EOS.",
				"print config: Print the JSON configuration file.",
				"print: Print all of the above.",
				"quit: Quit.",
	};
	
	private Config m_config = null;
	private final PrintStream m_out;
	private final InputStream m_in;
	
	private QueryEOS m_queryEOS = null;
	private QueryQLab m_queryQLab = null;
	private String m_eosShowName = null;
	private String m_qlabWorkspaceName = null;
	
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
	
	private NotInQLabResults m_notInQLab = null;
	
	// QLab network cues with EOS fire commands for cues NOT in EOS.
	// The key is the name of a QLab cuelist, the value is the invalid cues in that list.
	// If all  network cues in a cuelist are valid, there's no entry for that cuelist.
	private TreeMap<String, List<QLabNetworkCue>> m_notInEOS = null;
	
	// QLab network cues not in EOS cue order.
	// That is, the network cue's EOS cue number is less than the previous metwork cue in the QLab list.
	// The key is the name of a QLab cuelist, the value is the misordered cues in that list.
	// If all network cues in a cuelist are in order, there's no entry for that cuelist.
	private TreeMap<String, List<QLabNetworkCue>> m_misorderedNetworkCues = null;
	
	public EOS2QLab(String[] args, PrintStream out, InputStream in)
			throws IOException, IllegalArgumentException
	{
		m_out = (out != null) ? out : System.out;
		m_in = (in != null) ? in : System.in;
		m_config = new Config(args);
		
		m_out.println("Connecting to EOS & QLab ...");
		MiscUtil.runTasksAndWait(List.of(
				() -> m_queryEOS = QueryEOS.makeQueryEOS(m_config.getEOSAddrPorts(),
										m_config.getConnectTimeoutMS()),
				() -> m_queryQLab = QueryQLab.makeQueryQLab(m_config.getQLabAddrPorts(),
										m_config.getConnectTimeoutMS())),
				"Connect");

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
				out.println("  \"" + ws.m_displayName + "\"  version: " + ws.m_version);
			}
			int nQLabCues = refreshQLabCuelists();
			String activeWS = m_queryQLab.getLastReplyWorkspaceId();
			if (activeWS != null) {
				for (QLabWorkspaceInfo ws: qlabWorkspaces) {
					if (ws.m_uniqueId != null && ws.m_displayName != null && activeWS.equals(ws.m_uniqueId)) {
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
				EOSCueNumber eosCueNum = ((QLabNetworkCue)testCue).m_eosCueNumber;
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
		public final TreeMap<Integer, List<EOSCueInfo>> m_missingCuesByList;

		/** EOS cues not in QLab, by EOS cue number. */
		public final TreeMap<EOSCueNumber,EOSCueInfo> m_missingCuesByNumber;
	
		/** EOS cue ranges not in QLab. */
		public final TreeMap<Integer, List<EOSCueRange>> m_missingCueRanges;
		
		public NotInQLabResults(TreeMap<Integer, List<EOSCueInfo>> missingCues,
								TreeMap<Integer, List<EOSCueRange>> missingCueRanges)
		{
			m_missingCuesByList = missingCues;
			m_missingCueRanges = missingCueRanges;
			m_missingCuesByNumber = new TreeMap<>();
			for (List<EOSCueInfo> cuelist: m_missingCuesByList.values()) {
				for (EOSCueInfo cue: cuelist) {
					m_missingCuesByNumber.put(cue.getCueNumber(), cue);
				}
			}
		}
		
		public boolean isEmpty()
		{
			return m_missingCuesByList.isEmpty();
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
		TreeMap<Integer, List<EOSCueInfo>> missingCues = new TreeMap<>();
		TreeMap<Integer, List<EOSCueRange>> missingRanges = new TreeMap<>();
		for (Map.Entry<Integer, TreeMap<EOSCueNumber, EOSCueInfo>> ent: m_eosCuesByList.entrySet()) {
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
						EOSCueNumber eosCueNum = ((QLabNetworkCue)testCue).m_eosCueNumber;
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
		if (m_notInQLab == null) {
			if (!notInQLab()) {
				m_out.println("Cannot connect to QLab or EOS.");
			}
		}
		if (m_notInQLab.isEmpty()) {
			m_out.println("All EOS Cues are in QLab");
		} else {
			String indent = "   ";
			for (Map.Entry<Integer, List<EOSCueInfo>> ent : m_notInQLab.m_missingCuesByList.entrySet()) {
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
					for (EOSCueRange range : m_notInQLab.m_missingCueRanges.get(ent.getKey())) {
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
					m_out.print(number + " => " + cue.m_eosCueNumber);
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
				m_out.println("Selected " + m_misorderedNetworkCues.get(cuelistName).size() + " cues.");
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
		Integer iResp = getIntResponse("Enter cuelist number, " + QUIT_CMD[0]
								+ (iDefault >= 0 ? ", or return for default" : "") + ":",
								iDefault+1, 1, choices.size());
		if (iResp == null) {
			return null;
		} else {
			return choices.get(iResp-1);
		}
	}
	
	public boolean add2QLab()
	{
		if (m_notInQLab == null) {
			notInQLab();
			if (m_notInQLab.isEmpty()) {
				return false;
			}
		} else if (m_notInQLab.isEmpty()) {
			m_out.println("All EOS Cues are in QLab");
			return false;
		} else if (m_queryQLab == null) {
			m_out.println("  Not connected to QLab server.");
			return false;
		}
		QLabCuelistCue targetQLabCuelist = selectQLabCuelist();
		if (targetQLabCuelist == null) {
			return false;
		}
		Integer networkPatch = getIntResponse("Enter QLab network patch: (default is "
						+ m_config.getNewCueNetworkPatch() + ")", m_config.getNewCueNetworkPatch(),
						1, 16);
		if (networkPatch == null) {
			return false;
		}
		NewCueMarking newCueMarking = selectNewCueMarking();
		if (newCueMarking == null) {
			return false;
		}
		TreeMap<EOSCueNumber, EOSCueInfo> cuesToAdd = selectCuesToAdd();
		if (cuesToAdd == null) {
			return false;
		}
		m_out.println("Summary:");
		TreeMap<Integer,ACount> cntByList = cueCountByList(cuesToAdd.keySet());
		m_out.print(  "  Source:");
		String sep = " ";
		for (Map.Entry<Integer,ACount> ent: cntByList.entrySet()) {
			m_out.print(sep + ent.getValue().count + " cues in list " + ent.getKey());
			sep = ", ";
		}
		m_out.println();
		m_out.println("  Destination: QLab cuelist \"" + targetQLabCuelist.getName() + "\"");
		m_out.println("  Cue marking:" + (newCueMarking.m_flag ? " flagged" : "")
							+ " " + newCueMarking.m_color.toQLab());
		String prompt = "Ok? [y or n]";
		while (true) {
			String resp = getResponse(prompt);
			if (resp.toLowerCase().startsWith("y")) {
				break;
			} else if (resp.toLowerCase().startsWith("n") || resp.toLowerCase().startsWith("q")) {
				m_out.println("No changes made.");
				return false;
			} else {
				prompt = "Please enter y or n:";
			}
		}
		int nAdded = 0;
		String targetCuelistId = targetQLabCuelist.m_uniqueId;	// means add at end
		for (EOSCueInfo eosCue: cuesToAdd.descendingMap().values()) {
			try {
				EOSCueNumber eosNumber = eosCue.getCueNumber();
				InsertPoint insertPoint = findCueInsertPoint(eosNumber, targetCuelistId);
				String qlabCueId = m_queryQLab.newCue(QLabCueType.NETWORK,
										insertPoint.m_afterId,
										m_config.makeNewCueNumber(eosCue),
										m_config.makeNewCueName(eosCue));
				if (qlabCueId == null) {
					break;
				}
				nAdded++;
				if (insertPoint.m_moveToHead) {
					if (!m_queryQLab.moveCue(qlabCueId, 0, null)) {
						m_out.println("Error moving " + eosNumber + " to head.");
					}
				}
				QLabCue newCue = QLabCueType.insertNewCue(qlabCueId, m_qlabCuelists, m_queryQLab);
				if (newCue == null) {
					m_out.println("Error adding EOS cue " + eosNumber + ": insertNewCue failed");
					break;
				}
				if (newCue instanceof QLabNetworkCue) {
					m_eosCuesInQLab.put(eosNumber, (QLabNetworkCue)newCue);
				} else {
					System.out.println("Error: Added EOS cue " + eosNumber + " but it's type " + newCue.m_type);
				}
				m_queryQLab.setNetworkMessageType(qlabCueId, QLabUtil.NetworkMessageType.OSC);
				m_queryQLab.setCustomString(qlabCueId, EOSUtil.makeCueFireRequest(eosNumber));
				m_queryQLab.setPatchNumber(qlabCueId, networkPatch);
				m_queryQLab.setColorName(qlabCueId, newCueMarking.m_color);
				m_queryQLab.setFlagged(qlabCueId, newCueMarking.m_flag);
				m_queryQLab.setNotes(qlabCueId, eosCue.getNotes());
				if (nAdded > 0 && nAdded%60 == 0) {
					m_out.println();
				}
				m_out.print(".");
			} catch (IOException e) {
				m_out.println("Error adding cue \"" + eosCue.getCueNumber() + "\"");
				break;
			}
		}
		if (nAdded > 0) {
			m_out.println();
		}
		m_out.println("Added " + nAdded + " cues to QLab.");
		if (nAdded > 0) {
			getQLabCues();
			m_notInQLab = null;
		}
		return nAdded > 0;
	}
	
	private class InsertPoint
	{
		private final String m_afterId;
		private final boolean m_moveToHead;
		private InsertPoint(String afterId, boolean moveToHead)
		{
			m_afterId = afterId;
			m_moveToHead = moveToHead;
		}
	}
	
	/**
	 * Get the ID of the QLab cue after which the network cue for a missing EOS
	 * should be inserted. This is the cue before the existing network cue
	 * for the lowest EOS cue after the given cue.  The existing network cue
	 * must be in the QLab cuelist "cuelistId"; ignore network cues in other cuelists.
	 * If QLab does not have such a cue, return cuelistId to add the cue at the end
	 * of that cuelist.
	 * @param eosNum An EOS cue number.
	 * @param cuelistId The ID to return if there 
	 * @return The QLab cue after which the EOS network cue should be added.
	 */
	private InsertPoint findCueInsertPoint(EOSCueNumber eosNum, String cuelistId)
	{
		Map.Entry<EOSCueNumber, QLabNetworkCue> nextCue;
		for (nextCue = m_eosCuesInQLab.higherEntry(eosNum);
				nextCue != null && !nextCue.getValue().getCuelistId().equals(cuelistId);
				nextCue = m_eosCuesInQLab.higherEntry(nextCue.getKey())) {
		}
		if (nextCue == null) {
			return new InsertPoint(cuelistId, false);
		}
		QLabCue insertPoint = nextCue.getValue().getTopLevelCue();
		if (insertPoint == null) {
			return new InsertPoint(cuelistId, false);
		}
		insertPoint = insertPoint.getPrevCue();
		if (insertPoint == null) {
			return new InsertPoint(cuelistId, true);
		}
		return new InsertPoint(insertPoint.m_uniqueId, false);
	}
	
	private QLabCuelistCue selectQLabCuelist()
	{
		if (m_qlabCuelists.size() == 0) {
			m_out.println("Oops -- QLab doesn't have a cuelist!");
			return null;
		} else if (m_qlabCuelists.size() == 1) {
			return m_qlabCuelists.get(0);
		} else {
			ArrayList<QLabCuelistCue> cuelists = new ArrayList<>();
			m_out.println("Select target QLab cue list:");
			int iCuelist = 0;
			int iDefault = -1;
			for (QLabCuelistCue cuelist: m_qlabCuelists) {
				String defaultLabel = "";
				if (m_config.getDefaultQLabCuelist().equals(cuelist.getName()) && iDefault < 0) {
					iDefault = iCuelist;
					defaultLabel = " (default)";
				}
				m_out.println("   " + (iCuelist+1) + ": \"" + cuelist.getName()
								+ "\""+ defaultLabel);
				cuelists.add(cuelist);
				iCuelist++;
			}
			Integer iResp = getIntResponse("Enter cuelist number, " + QUIT_CMD[0]
									+ (iDefault >= 0 ? ", or return for default" : "") + ":",
									iDefault+1, 1, cuelists.size());
			if (iResp == null) {
				return null;
			} else {
				return cuelists.get(iResp-1);
			}
		}
	}
	
	private class NewCueMarking
	{
		private boolean m_flag = m_config.getNewCueFlag();
		private QLabUtil.ColorName m_color = m_config.getNewCueColor();
	}
	
	private NewCueMarking selectNewCueMarking()
	{
		NewCueMarking marking = new NewCueMarking();
		boolean responseOk;
		do {
			responseOk = true;
			m_out.println("New cues will be color " + marking.m_color.toQLab() + " and "
					+ (marking.m_flag ? "flagged" : "not flagged"));
			String resp = getResponse("Enter return if ok, or flagged, unflagged, and/or a color to change:");
			if (resp == null || isCmd(resp, QUIT_CMD)) {
				return null;
			} else if (!resp.isBlank()) {
				String[] tokens = resp.split("[ \t,;]+");
				QLabUtil.ColorName newColor = null;
				for (String token : tokens) {
					if (token.startsWith("flag")) {
						marking.m_flag = true;
					} else if (token.startsWith("unflag")) {
						marking.m_flag = false;
					} else if ((newColor = QLabUtil.ColorName.valueOf(resp.toUpperCase(), null)) != null) {
						marking.m_color = newColor;
					} else {
						m_out.println("   \"" + token + "\" is not flagged, unflagged, or a QLab color.");
						responseOk = false;
					}
				}
			} 
		} while (!responseOk);
		return marking;
	}
	
	private String getResponse(String msg)
	{
		if (msg!= null && !msg.isBlank()) {
			m_out.print(msg);
			if (!msg.endsWith(" ")) {
				m_out.print(" ");
			} 
		}
		String line = MiscUtil.readLine(m_in);
		if (line == null) {
			return null;
		}
		return line.trim();
	}
	
	private Integer getIntResponse(String msg, int def, int min, int max)
	{
		while (true) {
			String respStr = getResponse(msg);
			if (respStr == null) {
				return null;
			} else if (respStr.isBlank()) {
				return def;
			} else if (isCmd(respStr, QUIT_CMD)) {
				return null;
			} else {
				try {
					int resp = Integer.parseInt(respStr);
					if (resp >= min && resp <= max) {
						return resp;
					}
					m_out.println("Enter a number between " + min + " and " + max);
				} catch (Exception e) {
					m_out.println("Enter a number between " + min + " and " + max);
				}
			}
		}
	}
	
	private TreeMap<EOSCueNumber, EOSCueInfo> selectCuesToAdd()
	{
		prtCuesNotInQLab(true, false);
		TreeMap<EOSCueNumber, EOSCueInfo> cuesToAdd = new TreeMap<>();
		m_out.println("Enter the cues, or cue ranges, to add.");
		m_out.println("Use \"*\" to select all missing cues, or \"" + QUIT_CMD[0] + "\" to cancel.");
		m_out.println("Enter a blank line when done.");
		while (true) {
			String resp = getResponse("> ");
			if (resp == null || isCmd(resp, QUIT_CMD)) {
				return null;
			} else if (resp.isBlank()) {
				break;
			} else {
				String[] tokens = resp.split("[ \t,;]+");
				for (String token: tokens) {
					if (token.equals("*")) {
						cuesToAdd.putAll(m_notInQLab.m_missingCuesByNumber);
					} else if (token.matches("[0-9]+/\\*")) {
						String[] parts = token.split("/");
						addCuesInEOSCuelist(cuesToAdd, parts[0]);
					} else {
						String[] range = token.split("-");
						if (range.length == 2) {
							EOSCueNumber start = parseEOSCueNumber(range[0]);
							EOSCueNumber end = parseEOSCueNumber(range[1]);
							if (start != null && end != null) {
								for (Map.Entry<EOSCueNumber, EOSCueInfo> ent:
									m_notInQLab.m_missingCuesByNumber.subMap(start, true,
																			end, true).entrySet()) {
									cuesToAdd.put(ent.getKey(), ent.getValue());
								}
							}
						} else {
							EOSCueNumber cueNumber = parseEOSCueNumber(token);
							if (cueNumber != null) {
								EOSCueInfo cueInfo = m_notInQLab.m_missingCuesByNumber.get(cueNumber);
								if (cueInfo != null) {
									cuesToAdd.put(cueNumber, cueInfo);
								} else {
									m_out.println("Cue number \"" + cueNumber.toFullString()
												+ "\" isn't missing.");
								}
							}
						}
					}
				}
			}	
		}
		return cuesToAdd;
	}
	
	private void addCuesInEOSCuelist(TreeMap<EOSCueNumber, EOSCueInfo> cuesToAdd, String cuelistStr)
	{
		try {
			int cuelist = Integer.parseInt(cuelistStr);
			List<EOSCueInfo> cuesInList = m_notInQLab.m_missingCuesByList.get(cuelist);
			if (cuesInList != null) {
				for (EOSCueInfo cue: cuesInList) {
					cuesToAdd.put(cue.getCueNumber(), cue);
				}
			}
		} catch (Exception e) {
			// shouldn't happen -- caller should check.
		}
	}
	
	private EOSCueNumber parseEOSCueNumber(String num)
	{
		try {
			return new EOSCueNumber(num);
		} catch (Exception e) {
			m_out.println("Ignoring invalid cue number \"" + num + "\"");
			return null;
		}
	}
	
	public void prtConfig(String lineIndent) throws IOException
	{
		m_config.prtConfigFile(m_out, lineIndent);
	}
		
	public static void main(String[] args)
	{
		boolean running = true;
		PrintStream out = System.out;
		InputStream in = System.in;

		while (running) {
			try (EOS2QLab eos2QLab = new EOS2QLab(args)) {
				eos2QLab.prtEOSCueSummary();
				eos2QLab.prtQLabCueSummary();
				while (true) {
					out.print("* ");
					String line = MiscUtil.readLine(in);
					if (line == null) {
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
					if (isCmd(cmd[0], REFRESH_CMD)) {
						break;
					} else if (isCmd(cmd[0], QUIT_CMD)) {
						running = false;
						break;
					} else if (isCmd(cmd[0], PRINT_CMD)) {
						if (cmd.length >= 2 && isCmd(cmd[1], EOS_ARG)) {
							eos2QLab.prtEOSCueSummary();							
						} else if (cmd.length >= 2 && isCmd(cmd[1], QLAB_ARG)) {
							eos2QLab.prtQLabCueSummary();							
						} else if (cmd.length >= 2 && isCmd(cmd[1], MISSING_ARG)) {
							eos2QLab.prtCuesNotInQLab(true, true);
							eos2QLab.prtCuesNotInEOS();
						} else if (cmd.length == 2 && isCmd(cmd[1], CONFIG_ARG)) {
							eos2QLab.prtConfig("   ");
						} else if (cmd.length >= 2) {
							out.print("Unknown print command.");
						} else {
							eos2QLab.prtEOSCueSummary();							
							eos2QLab.prtCuesNotInQLab(true, true);
							eos2QLab.prtQLabCueSummary();							
							eos2QLab.prtCuesNotInEOS();
						}
					} else if (isCmd(cmd[0], CHECK_CMD)) {
						eos2QLab.notInQLab();
						eos2QLab.notInEOS();
						eos2QLab.prtCuesNotInQLab(true, false);							
						eos2QLab.prtCuesNotInEOS();					
					} else if (isCmd(cmd[0], ADD_CMD)) {
						eos2QLab.add2QLab();
					} else if (isCmd(cmd[0], SELECT_CMD)) {
						if (cmd.length >= 2 && isCmd(cmd[1], MISSING_ARG)) {
							eos2QLab.selectCuesNotInEOS();							
						} else if (cmd.length >= 2 && isCmd(cmd[1], ORDER_ARG)) {
							eos2QLab.selectMisorderedCues();							
						} else {
							out.println("Usage: " + SELECT_CMD[0] + " {" + MISSING_ARG[0]
																+ " | " + ORDER_ARG[0] + "}");
						}
					} else if (isCmd(cmd[0], HELP_CMD)) {
						for (String s: HELP_RESP) {
							out.println(s);
						}
					} else if (isCmd(cmd[0], new String[] {"test-replace"})) {
						if (cmd.length < 3) {
							out.println("Usage: test-var eos-cue-number string");
						} else {
							try {
								EOSCueNumber cueNumber = new EOSCueNumber(cmd[1]);
								EOSCueInfo cue = eos2QLab.getEOSCue(cueNumber);
								if (cue == null) {
									throw new IllegalArgumentException("Invalid cue number");
								}
								for (int iTest = 2; iTest < cmd.length; iTest++) {
									out.println(cmd[iTest] + ": "
											+ eos2QLab.m_config.replaceVars(cmd[iTest], cue));
								}
							} catch (IllegalArgumentException e) {
								out.println("Invalid cue number '" + cmd[1] + "'");
								e.printStackTrace();
							}
						}
					} else if (isCmd(cmd[0], new String[] {"test"})) {
						EOSCueNumber cue = new EOSCueNumber("1/0.1");
						for (Map.Entry<EOSCueNumber, EOSCueInfo> ent: eos2QLab.m_eosCuesByNumber.entrySet()) {
							out.println("Key: " + ent.getKey().toFullString() + " equals: "
										+ (ent.getKey().equals(cue)));
						}
						out.println("Get: " + eos2QLab.m_eosCuesByNumber.get(cue));
						out.println("Get2: " + eos2QLab.getEOSCue(cue));
					} else {
						out.println("Unknown command \"" + line + "\"");
					}
				}
			} catch (IllegalArgumentException e) {
				System.err.println(e.getMessage());
				running = false;
			} catch (IOException e) {
				System.err.println(e);
				running = false;
			}
		}
	}
	
	/**
	 * Return true iff "cmd" matches a string in "cmds". Ignore case.
	 */
	private static boolean isCmd(String cmd, String[] cmds)
	{
		for (String s: cmds) {
			if (cmd.equalsIgnoreCase(s)) {
				return true;
			}
		}
		return false;
	}
	
	private static class ACount
	{
		private int count = 0;
		private void incr() { count++; }
	}
	
	TreeMap<Integer, ACount> cueCountByList(Collection<EOSCueNumber> cueNumbers)
	{
		TreeMap<Integer, ACount> cntByList = new TreeMap<>();
		for (EOSCueNumber cueNumber: cueNumbers) {
			int list = cueNumber.getCuelist();
			ACount cnt = cntByList.get(list);
			if (cnt == null) {
				cnt = new ACount();
				cntByList.put(list, cnt);
			}
			cnt.incr();
		}
		return cntByList;
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
}
