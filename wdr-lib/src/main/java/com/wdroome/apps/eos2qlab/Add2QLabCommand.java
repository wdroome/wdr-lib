package com.wdroome.apps.eos2qlab;

import java.io.IOException;

import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

import com.wdroome.osc.eos.EOSCueInfo;
import com.wdroome.osc.eos.EOSCuelistInfo;
import com.wdroome.osc.eos.EOSUtil;
import com.wdroome.osc.eos.EOSCueNumber;

import com.wdroome.osc.qlab.QueryQLab;
import com.wdroome.osc.qlab.QLabCue;
import com.wdroome.osc.qlab.QLabCuelistCue;
import com.wdroome.osc.qlab.QLabGroupCue;
import com.wdroome.osc.qlab.QLabNetworkCue;
import com.wdroome.osc.qlab.QLabUtil;
import com.wdroome.osc.qlab.QLabCueType;
import com.wdroome.osc.qlab.QLabWorkspaceInfo;
import com.wdroome.osc.qlab.QLabNetworkPatchInfo;

public class Add2QLabCommand
{
	private final Config m_config;
	private final QueryQLab m_queryQLab;
	private final ReadResponse m_response;
	private final List<QLabCuelistCue> m_qlabCuelists;
	private final TreeMap<EOSCueNumber, QLabNetworkCue> m_eosCuesInQLab;
	private final EOSCuesNotInQLab m_notInQLab;
	
	public Add2QLabCommand(Config config,
						QueryQLab queryQLab,
						ReadResponse response,
						List<QLabCuelistCue> qlabCuelists,
						TreeMap<EOSCueNumber, QLabNetworkCue> eosCuesInQLab,
						EOSCuesNotInQLab notInQLab)
						// XXX TreeMap<EOSCueNumber,EOSCueInfo> missingCuesByNumber)
	{
		m_config = config;
		m_queryQLab = queryQLab;
		m_response = response;
		m_qlabCuelists = qlabCuelists;
		m_eosCuesInQLab = eosCuesInQLab;
		m_notInQLab = notInQLab;
	}
	
	private class NewCueOptions
	{
		private QLabCuelistCue m_targetCuelist;
		private boolean m_flagged = m_config.getNewCueFlag();
		private QLabUtil.ColorName m_color = m_config.getNewCueColor();
		private int m_patchNumber;
		private QLabNetworkPatchInfo.PatchType m_patchType;
		TreeMap<EOSCueNumber, EOSCueInfo> m_cuesToAdd;
		
		private NewCueOptions() throws IOException
		{
			if (m_qlabCuelists.isEmpty()) {
				throw new IllegalStateException("QLab does not have a cuelist.");
			}
			setDefaultTargetCuelist();
			setDefaultNetworkPatch();
			m_cuesToAdd = new TreeMap<EOSCueNumber, EOSCueInfo> ();
			m_cuesToAdd.putAll(m_notInQLab.m_missingCuesByNumber);
		}
		
		private void setDefaultTargetCuelist()
		{
			if (m_qlabCuelists.size() == 1) {
				m_targetCuelist = m_qlabCuelists.get(0);
			} else {
				for (QLabCuelistCue cuelist: m_qlabCuelists) {
					if (cuelist.getName().equals(m_config.getDefaultQLabCuelist())) {
						m_targetCuelist = cuelist;
						return;
					}
				}
				m_targetCuelist = m_qlabCuelists.get(0);
			}
		}
		
		private void setDefaultNetworkPatch() throws IOException
		{
			m_patchNumber = m_config.getNewCueNetworkPatch();
			m_patchType = m_queryQLab.getNetworkPatchType(m_patchNumber,
														QLabNetworkPatchInfo.PatchType.OSC_MESSAGE);
			switch (m_patchType) {
			case OSC_MESSAGE:
			case ETC_EOS_FAMILY:
				break;
			default:
				for (QLabNetworkPatchInfo patchInfo: m_queryQLab.getNetworkPatches()) {
					switch (patchInfo.m_patchType) {
					case OSC_MESSAGE:
					case ETC_EOS_FAMILY:
						m_patchNumber = patchInfo.m_number;
						m_patchType = patchInfo.m_patchType;
						break;
					default:
						m_patchNumber = 1;
						m_patchType = QLabNetworkPatchInfo.PatchType.OSC_MESSAGE;
					}
				}
			}
		}
		
		private boolean confirm() throws IOException
		{
			int nTries = 0;
			while (true) {
				if (++nTries > 1) {
					m_response.println();
				}
				m_response.println("Summary of add operation:");
				m_response.println("  1. Target QLab cuelist: " + m_targetCuelist.getName());
				m_response.println("  2. Network patch number: " + m_patchNumber);
				m_response.println("  3. Cue Color: " + m_color.toQLab());
				m_response.println("  4. Flag cues: " + m_flagged);
				m_response.print(  "  5. EOS Cues:");
				TreeMap<Integer, ACount> cntByList = cueCountByList(m_cuesToAdd.keySet());
				String sep = " ";
				for (Map.Entry<Integer, ACount> ent : cntByList.entrySet()) {
					m_response.print(sep + ent.getValue().count + " cues in list " + ent.getKey());
					sep = ", ";
				}
				m_response.println("");
				while (true) {
					String reply = m_response.getResponse(
							"Enter y to add the cues, n to cancel, or 1-5 to change an option: ");
					if (reply == null) {
						return false;
					} else if (reply.toLowerCase().startsWith("y")) {
						return true;
					} if (reply.toLowerCase().startsWith("n")
							|| reply.toLowerCase().startsWith("q")) {
						return false;
					} else {
						Integer changeOpt = getIntValue(reply, 1, 5);
						if (changeOpt == null) {
							continue;
						}
						switch (changeOpt) {
						case 1:
							if (!pickQLabCuelist()) {
								return false;
							}
							break;
						case 2:
							if (!pickNetworkPatch()) {
								return false;
							}
							break;
						case 3:
							if (!pickColor()) {
								return false;
							}
							break;
						case 4:
							if (!pickFlagged()) {
								return false;
							}
							break;
						case 5:
							m_cuesToAdd = selectCuesToAdd();
							if (m_cuesToAdd == null) {
								return false;
							}
							break;
						}
						break;
					}
				} 
			}
		}
		
		private Integer getIntValue(String reply, int min, int max)
		{
			try {
				int v = Integer.parseInt(reply);
				return (v >= min && v <= max) ? Integer.valueOf(v) : null;
			} catch (Exception e) {
				return null;
			}
		}
		
		private boolean pickQLabCuelist()
		{
			ArrayList<QLabCuelistCue> cuelists = new ArrayList<>();
			m_response.println("Select target QLab cue list:");
			int iCuelist = 0;
			int iDefault = -1;
			for (QLabCuelistCue cuelist: m_qlabCuelists) {
				String defaultLabel = "";
				if (m_config.getDefaultQLabCuelist().equals(cuelist.getName()) && iDefault < 0) {
					iDefault = iCuelist;
					defaultLabel = " (default)";
				}
				m_response.println("   " + (iCuelist+1) + ": \"" + cuelist.getName()
								+ "\""+ defaultLabel);
				cuelists.add(cuelist);
				iCuelist++;
			}
			Integer iResp = m_response.getIntResponse("Enter cuelist number, " + Commands.QUIT_CMD[0]
									+ (iDefault >= 0 ? ", or return for default" : "") + ":",
									iDefault+1, 1, cuelists.size());
			if (iResp == null) {
				return false;
			} else {
				m_targetCuelist = cuelists.get(iResp-1);
				return true;
			}
		}
		
		private boolean pickNetworkPatch() throws IOException
		{
			Integer patchNumber = null;
			if (m_queryQLab.isQLab5()) {
				m_response.println("Available Network Patches:");
				for (QLabNetworkPatchInfo patch: m_queryQLab.getNetworkPatches()) {
					m_response.println("  " + patch.m_number + ": " + patch.m_typeAndName);
				}
			}
			patchNumber = m_response.getIntResponse("Enter QLab network patch: ",
										m_config.getNewCueNetworkPatch(),
										1, QLabUtil.MAX_NETWORK_PATCHES);			
			if (patchNumber == null) {
				return false;
			}
			QLabNetworkPatchInfo.PatchType patchType = m_queryQLab.getNetworkPatchType(patchNumber,
												QLabNetworkPatchInfo.PatchType.OSC_MESSAGE);
			switch (patchType) {
			case OSC_MESSAGE: break;
			case ETC_EOS_FAMILY: break;
			default:
				m_response.println("*** WARNING: Network Patch " + patchNumber
						+ " has an unsupported type " + patchType + ".");
				// Punt, and pretend it's a vanilla OSC message.
				patchType = QLabNetworkPatchInfo.PatchType.OSC_MESSAGE;
			}
			m_patchNumber = patchNumber;
			m_patchType = patchType;
			return true;
		}
		
		private boolean pickColor()
		{
			String resp = m_response.getResponse("Enter a color: ");
			while (true) {
				if (resp == null || Commands.isCmd(resp, Commands.QUIT_CMD)) {
					return false;
				} else {
					resp = resp.trim();
					QLabUtil.ColorName newColor =
									QLabUtil.ColorName.valueOf(resp.toUpperCase(), null);
					if (newColor != null) {
						m_color = newColor;
						return true;
					}
					m_response.println("The legal colors are: " + QLabUtil.allQLabColors(" "));
					resp = m_response.getResponse(
							"Enter a value QLab cue color, or 'q' to cancel: ");
				}
			}
		}

		private boolean pickFlagged()
		{
			while (true) {
				String resp = m_response.getResponse("Flag added cues? (Enter y, n or q) ");
				if (resp == null || Commands.isCmd(resp, Commands.QUIT_CMD)) {
					return false;
				} else if (!resp.isBlank()) {
					resp = resp.trim().toLowerCase();
					if (resp.startsWith("y") || resp.startsWith("f")) {
						m_flagged = true;
						return true;
					} else if (resp.startsWith("n") || resp.startsWith("u")) {
						m_flagged = false;
						return true;
					}
				} 
			}
		}
	}
 
	public int addCues() throws IOException
	{
		NewCueOptions options;
		try {
			options = new NewCueOptions();
		} catch (IllegalStateException e) {
			m_response.println(e.getMessage());
			return 0;
		} catch (Exception e) {
			m_response.println(e.toString());
			return 0;
		}
		if (!options.confirm()) {
			return 0;
		}
		int nAdded = 0;
		String targetCuelistId = options.m_targetCuelist.m_uniqueId;	// means add at end
		for (EOSCueInfo eosCue: options.m_cuesToAdd.descendingMap().values()) {
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
						m_response.println("Error moving " + eosNumber + " to head.");
					}
				}
				QLabCue newCue = QLabCueType.insertNewCue(qlabCueId, m_qlabCuelists, m_queryQLab);
				if (newCue == null) {
					m_response.println("Error adding EOS cue " + eosNumber + ": insertNewCue failed");
					break;
				}
				if (newCue instanceof QLabNetworkCue) {
					m_eosCuesInQLab.put(eosNumber, (QLabNetworkCue)newCue);
				} else {
					System.out.println("Error: Added EOS cue " + eosNumber
									+ " but it's type " + newCue.m_type);
				}
				m_queryQLab.setPatchNumber(qlabCueId, options.m_patchNumber);
				if (options.m_color != QLabUtil.ColorName.NONE) {
					m_queryQLab.setColorName(qlabCueId, options.m_color);					
				}
				if (options.m_flagged) { 
					m_queryQLab.setFlagged(qlabCueId, options.m_flagged);
				}
				String notes = eosCue.getNotes();
				if (notes != null && !notes.isBlank()) {
					m_queryQLab.setNotes(qlabCueId, eosCue.getNotes());
				}
				if (!m_queryQLab.isQLab5()) {
					m_queryQLab.setNetworkMessageType(qlabCueId, QLabUtil.NetworkMessageType.OSC);
					m_queryQLab.setCustomString(qlabCueId, EOSUtil.makeCueFireRequest(eosNumber));
				} else {
					switch (options.m_patchType) {
					case OSC_MESSAGE:
						m_queryQLab.setCustomString(qlabCueId, EOSUtil.makeCueFireRequest(eosNumber));
						break;
					case ETC_EOS_FAMILY:
						List<String> params = QLabUtil.getEosFireCueParam(eosNumber.toFullString());
						m_queryQLab.setParameterValues(qlabCueId, params);
						break;
					default:
						break;	// Shouldn't happen; we excluded others earlier in method.
					}
				}
				if (nAdded > 0 && nAdded%60 == 0) {
					m_response.println();
				}
				m_response.print(".");
			} catch (IOException e) {
				m_response.println("Error adding cue \"" + eosCue.getCueNumber() + "\"");
				break;
			}
		}
		if (nAdded > 0) {
			m_response.println();
		}
		return nAdded;
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
		insertPoint = insertPoint.getCueSequenceStart();
		insertPoint = insertPoint.getPrevCue();
		if (insertPoint == null) {
			return new InsertPoint(cuelistId, true);
		}
		return new InsertPoint(insertPoint.m_uniqueId, false);
	}
	
	private TreeMap<EOSCueNumber, EOSCueInfo> selectCuesToAdd()
	{
		m_notInQLab.prtCuesNotInQLab(m_response.getOut(), true, false);
		TreeMap<EOSCueNumber, EOSCueInfo> cuesToAdd = new TreeMap<>();
		m_response.println("Enter the cues, or cue ranges, to add.");
		m_response.println("Use \"*\" to select all missing cues, or \"" + Commands.QUIT_CMD[0]
							+ "\" to cancel.");
		m_response.println("Enter a blank line when done.");
		while (true) {
			String resp = m_response.getResponse("> ");
			if (resp == null || Commands.isCmd(resp, Commands.QUIT_CMD)) {
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
									m_response.println("Cue number \"" + cueNumber.toFullString()
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
			m_response.println("Ignoring invalid cue number \"" + num + "\"");
			return null;
		}
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
}