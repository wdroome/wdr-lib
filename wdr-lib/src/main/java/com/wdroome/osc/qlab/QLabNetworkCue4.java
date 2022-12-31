package com.wdroome.osc.qlab;

import java.util.List;
import java.io.IOException;
import java.io.PrintStream;

import com.wdroome.json.JSONValue_Object;
import com.wdroome.osc.eos.EOSUtil;

/**
 * A QLab Network Cue in QLab 4, but not QLab 5.
 * @author wdr
 */
public class QLabNetworkCue4 extends QLabNetworkCue
{
	public final QLabUtil.NetworkMessageType m_msgType;
	public final String m_customString;
	
	public QLabNetworkCue4(JSONValue_Object jsonCue, QLabCue parent, int parentIndex,
			boolean isAuto, QueryQLab queryQLab)
	{
		super(jsonCue, parent, parentIndex, isAuto, queryQLab);
		QLabUtil.NetworkMessageType msgType = QLabUtil.NetworkMessageType.OSC;
		String customString = "";
		if (queryQLab != null) {
			try {
				msgType = queryQLab.getNetworkMessageType(m_uniqueId);
				customString = queryQLab.getCustomString(m_uniqueId);
			} catch (IOException e) {
				// ignore
			}
		}
		m_msgType = msgType;
		m_customString = customString;
		m_eosCueNumber = EOSUtil.getCueInFireRequest(m_customString);
	}
	
	protected QLabNetworkCue4(String uniqueId, QueryQLab queryQLab)
	{
		super(uniqueId, queryQLab);
		QLabUtil.NetworkMessageType msgType = QLabUtil.NetworkMessageType.OSC;
		String customString = "";
		if (queryQLab != null) {
			try {
				msgType = queryQLab.getNetworkMessageType(uniqueId);
				customString = queryQLab.getCustomString(uniqueId);
			} catch (IOException e) {
				// ignore
			}
		}
		m_msgType = msgType;
		m_customString = customString;
		m_eosCueNumber = EOSUtil.getCueInFireRequest(m_customString);
	}
	
	@Override
	public String toString()
	{
		return super.toString() + " msgtype=" + m_msgType + " customString=" + m_customString;
	}
	
	@Override
	public void printCue(PrintStream out, String indent, String indentIncr)
	{
		if (out == null) {
			out = System.out;
		}
		super.printCue(out, indent, indentIncr);
		out.println(indent + indentIncr + indentIncr + m_msgType
				+ (!m_customString.isBlank() ? (" cmd=\"" + m_customString + "\"") : ""));
	}
}
