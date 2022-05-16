package com.wdroome.osc.qlab;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.ArrayList;

import com.wdroome.json.JSONValue_Object;

/**
 * A QLab Netowrk cue.
 * @author wdr
 */
public class QLabNetworkCue extends QLabCue
{
	public final int m_patchNumber;
	public final QLabUtil.NetworkMessageType m_msgType;
	public final String m_customString;
	
	public QLabNetworkCue(JSONValue_Object jsonCue, QLabCue parent, int parentIndex, QueryQLab queryQLab)
	{
		super(jsonCue, parent, parentIndex, queryQLab);
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
	}
	
	@Override
	public String toString()
	{
		return super.toString() + " msgtype=" + m_msgType + " patch=" + m_patchNumber
					+ " customString=" + m_customString;
	}
	
	@Override
	public void printCue(PrintStream out, String indent, String indentIncr)
	{
		if (out == null) {
			out = System.out;
		}
		super.printCue(out, indent, indentIncr);
		out.println(indent + indentIncr + indentIncr + m_msgType + " patch=" + m_patchNumber
				+ (!m_customString.isBlank() ? (" cmd=\"" + m_customString + "\"") : ""));
	}
}
