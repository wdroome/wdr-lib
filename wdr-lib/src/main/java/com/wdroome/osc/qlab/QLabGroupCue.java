package com.wdroome.osc.qlab;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.ArrayList;

import com.wdroome.json.JSONValue_Object;

/**
 * A QLab Group cue.
 * @author wdr
 */
public class QLabGroupCue extends QLabCue
{
	public final QLabUtil.GroupMode m_groupMode;
	public final List<QLabCue> m_cues;
	
	public QLabGroupCue(JSONValue_Object jsonCue, QLabCue parent, int parentIndex, QueryQLab queryQLab)
	{
		super(jsonCue, parent, parentIndex, queryQLab);
		QLabUtil.GroupMode groupMode = QLabUtil.GroupMode.START_AND_ENTER;
		if (queryQLab != null) {
			try {
				groupMode = queryQLab.getGroupMode(m_uniqueId);
			} catch (IOException e) {
				// ignore
			}
		}
		m_groupMode = groupMode;
		if (queryQLab != null) {
			m_cues = QLabCueType.getCueArray(jsonCue.getArray(QLabUtil.FLD_CUES, null), this, queryQLab);
		} else {
			m_cues = new ArrayList<>();
		}
	}
	
	@Override
	public String toString()
	{
		return super.toString() + " mode=" + m_groupMode + " cues=" + m_cues;
	}
	
	@Override
	public void printCue(PrintStream out, String indent, String indentIncr)
	{
		if (out == null) {
			out = System.out;
		}
		super.printCue(out, indent, indentIncr);
		out.println(indent + indentIncr + indentIncr + "mode=" + m_groupMode);
		if (m_cues != null) {
			for (QLabCue child: m_cues) {
				child.printCue(out, indent + indentIncr, indentIncr);
			}
		}
	}
}
