package com.wdroome.apps.eos2qlab;

import java.io.PrintStream;

import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;

import com.wdroome.osc.eos.EOSCueInfo;
import com.wdroome.osc.eos.EOSCuelistInfo;
import com.wdroome.osc.eos.EOSUtil;
import com.wdroome.osc.eos.EOSCueNumber;

import com.wdroome.osc.qlab.QueryQLab;
import com.wdroome.osc.qlab.QLabNetworkCue;

public class EOSCuesNotInQLab
{
	/** EOS cues not in QLab, grouped by EOS cuelist number. */
	public final TreeMap<Integer, List<EOSCueInfo>> m_missingCuesByList;

	/** EOS cues not in QLab, by EOS cue number. */
	public final TreeMap<EOSCueNumber,EOSCueInfo> m_missingCuesByNumber;

	/** EOS cue ranges not in QLab. */
	public final TreeMap<Integer, List<EOSCueRange>> m_missingCueRanges;

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
		
	public EOSCuesNotInQLab(TreeMap<Integer, TreeMap<EOSCueNumber, EOSCueInfo>> eosCuesByList,
								TreeMap<EOSCueNumber, QLabNetworkCue> eosCuesInQLab)
	{
		m_missingCuesByList = new TreeMap<>();
		m_missingCueRanges = new TreeMap<>();
		for (Map.Entry<Integer, TreeMap<EOSCueNumber, EOSCueInfo>> ent: eosCuesByList.entrySet()) {
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
				if (eosCuesInQLab.get(eosCueNum) == null) {
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
				m_missingCuesByList.put(cuelistNum, cuesForCuelist);
				m_missingCueRanges.put(cuelistNum, rangesForCuelist);
			}
		}
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

	public void prtCuesNotInQLab(PrintStream out, boolean prtRanges, boolean prtCues)
	{
		if (isEmpty()) {
			out.println("All EOS Cues are in QLab");
		} else {
			String indent = "   ";
			for (Map.Entry<Integer, List<EOSCueInfo>> ent
								: m_missingCuesByList.entrySet()) {
				out.println("EOS cuelist " + ent.getKey() + ": "
											+ ent.getValue().size() + " cue(s) not in QLab");
				if (prtCues) {
					int nCues = 0;
					out.print(indent + (prtRanges ? "Cues: ": ""));
					for (EOSCueInfo eosCue: ent.getValue()) {
						if ((nCues % 10) != 0) {
							out.print(", ");
						} else if (nCues > 0) {
							out.println();
							out.print(indent);
						}
						out.print(eosCue.getCueNumber());
						nCues++;
					}
					out.println();
				}
				if (prtRanges) {
					int nRanges = 0;
					out.print(indent + (prtCues ? "Ranges: ": ""));
					for (EOSCueRange range : m_missingCueRanges.get(ent.getKey())) {
						if ((nRanges % 5) != 0) {
							out.print(",  ");
						} else if (nRanges > 0) {
							out.println();
							out.print(indent);
						}
						out.print(range.m_start.getCueNumber() + "-"
										+ range.m_end.getCueNumber()
										+ " [" + range.m_nCues + "]");
						nRanges++;
					} 
					out.println();					
				}
			}
		}
	}
}
