package com.wdroome.osc.qlab;

import java.io.PrintStream;
import java.util.List;
import java.util.ArrayList;

import com.wdroome.json.JSONValue_Object;

/**
 * A QLab CueList cue.
 * @author wdr
 */
public class QLabCuelistCue extends QLabCue
{
	public final List<QLabCue> m_cues;
	
	public QLabCuelistCue(JSONValue_Object jsonCue, QLabCue parent, int parentIndex,
							boolean isAuto, QueryQLab queryQLab)
	{
		super(jsonCue, parent, parentIndex, isAuto, queryQLab);
		if (queryQLab != null) {
			m_cues = QLabCueType.getCueArray(jsonCue.getArray(QLabUtil.FLD_CUES, null), this, false, queryQLab);
		} else {
			m_cues = new ArrayList<>();
		}
	}
	
	public List<QLabCue> getCues()
	{
		return m_cues;
	}
	
	@Override
	public String toString()
	{
		return super.toString() + " cues=" + m_cues;
	}
	
	@Override
	public void printCue(PrintStream out, String indent, String indentIncr)
	{
		if (out == null) {
			out = System.out;
		}
		super.printCue(out, indent, indentIncr);
		if (m_cues != null) {
			for (QLabCue child: m_cues) {
				child.printCue(out, indent + indentIncr, indentIncr);
			}
		}
	}
}
