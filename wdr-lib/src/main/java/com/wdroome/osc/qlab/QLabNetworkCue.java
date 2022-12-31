package com.wdroome.osc.qlab;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.ArrayList;

import com.wdroome.json.JSONValue_Object;
import com.wdroome.osc.eos.EOSUtil;
import com.wdroome.osc.eos.EOSCueNumber;

/**
 * Base class for a QLab Network cue.
 * Network cues changed from QLab4 to 5. Version-specific subclasses handle the differences.
 * @author wdr
 */
public abstract class QLabNetworkCue extends QLabCue
{
	public final int m_patchNumber;
	
		// The target EOS cue of the fire-cue command,
		// or null if this isn't an EOS fire-cue command.
	protected EOSCueNumber m_eosCueNumber;
	
	public QLabNetworkCue(JSONValue_Object jsonCue, QLabCue parent, int parentIndex,
							boolean isAuto, QueryQLab queryQLab)
	{
		super(jsonCue, parent, parentIndex, isAuto, queryQLab);
		int patchNumber = 1;
		if (queryQLab != null) {
			try {
				patchNumber = queryQLab.getPatchNumber(m_uniqueId);
			} catch (IOException e) {
				// ignore
			}
		}
		m_patchNumber = patchNumber;
	}
	
	protected QLabNetworkCue(String uniqueId, QueryQLab queryQLab)
	{
		super(uniqueId, QLabCueType.NETWORK, queryQLab);
		int patchNumber = 1;
		if (queryQLab != null) {
			try {
				patchNumber = queryQLab.getPatchNumber(m_uniqueId);
			} catch (IOException e) {
				// ignore
			}
		}
		m_patchNumber = patchNumber;
	}
	
	/**
	 * Get the EOS cue number, or null if it's not an EOS fire-cue command.
	 * @return
	 */
	public EOSCueNumber getEosCueNumber()
	{
		return m_eosCueNumber;
	}
	
	@Override
	public String toString()
	{
		return super.toString() + " patch=" + m_patchNumber
					+ (m_eosCueNumber != null ? (" eosCue=" + m_eosCueNumber.toFullString()) : "");
	}
	
	@Override
	public void printCue(PrintStream out, String indent, String indentIncr)
	{
		if (out == null) {
			out = System.out;
		}
		super.printCue(out, indent, indentIncr);
		out.println(indent + indentIncr + indentIncr + "patch=" + m_patchNumber
				+ (m_eosCueNumber != null ? (" eosCue=" + m_eosCueNumber.toFullString()) : ""));
	}
}
