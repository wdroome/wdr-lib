package com.wdroome.osc.qlab;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.ArrayList;

import com.wdroome.json.JSONValue_Object;

/**
 * A QLab CueList cue.
 * @author wdr
 */
public class QLabCartCue extends QLabCue
{
	public final List<QLabCue> m_cues;
	
	public QLabCartCue(JSONValue_Object jsonCue, QLabCue parent, int parentIndex,
							boolean isAuto, QueryQLab queryQLab)
	{
		super(jsonCue, parent, parentIndex, isAuto, queryQLab);
		if (queryQLab != null) {
			m_cues = QLabCueType.getCueArray(jsonCue.getArray(QLabUtil.FLD_CUES, null),
												this, false, queryQLab);
		} else {
			m_cues = new ArrayList<>();
		}
	}
	
	@Override
	public List<QLabCue> getChildren()
	{
		return m_cues;
	}
	
	@Override
	public int getIndexOfChild(String childId)
	{
		int nChildren = m_cues.size();
		for (int iChild = 0; iChild < nChildren; iChild++) {
			if (m_cues.get(iChild).m_uniqueId.equals(childId)) {
				return iChild;
			}
		}
		return -1;
	}
	
	@Override
	protected boolean insertCue(QLabCue cue, QueryQLab queryQLab)
	{
		int cueIndex;
		try {
			cueIndex = queryQLab.getIndexInParent(cue.m_uniqueId);
		} catch (IOException e) {
			return false;
		}
		if (cueIndex < 0) {
			return false;
		} else {
			m_cues.add(cueIndex, cue);
			cue.setParent(this);
			cue.setIsAuto(cueIndex > 0
					&& m_cues.get(cueIndex-1).m_continueMode != QLabUtil.ContinueMode.NO_CONTINUE);
			return true;
		}
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
