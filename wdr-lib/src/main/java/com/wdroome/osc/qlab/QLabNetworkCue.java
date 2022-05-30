package com.wdroome.osc.qlab;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.ArrayList;

import com.wdroome.json.JSONValue_Object;
import com.wdroome.osc.eos.EOSUtil;
import com.wdroome.osc.eos.EOSCueNumber;

/**
 * A QLab Network cue.
 * @author wdr
 */
public class QLabNetworkCue extends QLabCue
{
	public final int m_patchNumber;
	public final QLabUtil.NetworkMessageType m_msgType;
	public final String m_customString;
	
		// The target cue of the fire-cue command in m_customString,
		// or null if m_customString isn't a fire-cue command.
	public final EOSCueNumber m_eosCueNumber;
	
	public QLabNetworkCue(JSONValue_Object jsonCue, QLabCue parent, int parentIndex,
							boolean isAuto, QueryQLab queryQLab)
	{
		super(jsonCue, parent, parentIndex, isAuto, queryQLab);
		QLabUtil.NetworkMessageType msgType = QLabUtil.NetworkMessageType.OSC;
		String customString = "";
		int patchNumber = 1;
		if (queryQLab != null) {
			try {
				msgType = queryQLab.getNetworkMessageType(m_uniqueId);
				patchNumber = queryQLab.getPatchNumber(m_uniqueId);
				customString = queryQLab.getCustomString(m_uniqueId);
			} catch (IOException e) {
				// ignore
			}
		}
		m_patchNumber = patchNumber;
		m_msgType = msgType;
		m_customString = customString;
		m_eosCueNumber = EOSUtil.getCueInFireRequest(m_customString);
	}
	
	public QLabNetworkCue(String uniqueId, QLabCue parent, QueryQLab queryQLab)
	{
		super(uniqueId, QLabCueType.NETWORK, parent, queryQLab);
		int patchNumber = 1;
		QLabUtil.NetworkMessageType msgType = QLabUtil.NetworkMessageType.OSC;
		String customString = "";
		if (queryQLab != null) {
			try {
				patchNumber = queryQLab.getPatchNumber(uniqueId);
				msgType = queryQLab.getNetworkMessageType(uniqueId);
				customString = queryQLab.getCustomString(uniqueId);
			} catch (IOException e) {
				// Skip ??
			}
		}
		m_patchNumber = patchNumber;
		m_msgType = msgType;
		m_customString = customString;
		m_eosCueNumber = EOSUtil.getCueInFireRequest(m_customString);
	}
	
	@Override
	public String toString()
	{
		return super.toString() + " msgtype=" + m_msgType + " patch=" + m_patchNumber
					+ " customString=" + m_customString
					+ (m_eosCueNumber != null ? (" eosCue=" + m_eosCueNumber.toFullString()) : "");
	}
	
	@Override
	public void printCue(PrintStream out, String indent, String indentIncr)
	{
		if (out == null) {
			out = System.out;
		}
		super.printCue(out, indent, indentIncr);
		out.println(indent + indentIncr + indentIncr + m_msgType + " patch=" + m_patchNumber
				+ (m_eosCueNumber != null ? (" eosCue=" + m_eosCueNumber.toFullString()) : "")
				+ (!m_customString.isBlank() ? (" cmd=\"" + m_customString + "\"") : ""));
	}
}
