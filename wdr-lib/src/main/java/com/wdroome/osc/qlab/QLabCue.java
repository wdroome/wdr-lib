package com.wdroome.osc.qlab;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.PrintStream;

import com.wdroome.json.JSONValue;
import com.wdroome.json.JSONValue_Object;
import com.wdroome.json.JSONValue_Array;
import com.wdroome.json.JSONValue_ObjectArray;
import com.wdroome.osc.qlab.QLabUtil.ColorName;

/**
 * The base class for cues retrieved from QLab.
 * @author wdr
 */
public class QLabCue
{
	public final QLabCueType m_type;
	
	// May be null.
	public final QLabCue m_parent;
	
	// If m_parent isn't null, the index in m_parent.
	public final int m_parentIndex;
	
	public final String m_listName;
	public final String m_name;
	public final String m_number;
	public final String m_uniqueId;
	public final boolean m_armed;
	public final boolean m_flagged;
	public final QLabUtil.ColorName m_colorName;
	public final boolean m_isBroken;
	public final QLabUtil.ContinueMode m_continueMode;
	public final String m_notes;
	public final double m_duration;
	public final double m_prewait;
	public final double m_postwait;
	public final String m_cueTargetId;
	public final String m_fileTarget;
	public final boolean m_isAuto;
	
	/**
	 * Create a QlabCue from a QLab reply message.
	 * This includes all child cues.
	 * @param jsonCue The cue data from QLab.
	 * @param parent The parent cue, if not null.
	 * @param parentIndex The index in the parent cue, if there is one.
	 */
	public QLabCue(JSONValue_Object jsonCue, QLabCue parent, int parentIndex, boolean isAuto, QueryQLab queryQLab)
	{
		m_parent = parent;
		m_parentIndex = parentIndex;
		m_isAuto = isAuto || autoFromParent(parent);
		m_type = QLabCueType.fromQLab(jsonCue.getString(QLabUtil.FLD_TYPE, QLabCueType.UNKNOWN.toString()));
		m_listName = jsonCue.getString(QLabUtil.FLD_LIST_NAME, "");
		m_uniqueId = jsonCue.getString(QLabUtil.FLD_UNIQUE_ID, "");
		m_name = jsonCue.getString(QLabUtil.FLD_NAME, "");
		m_number = jsonCue.getString(QLabUtil.FLD_NUMBER, "");
		m_colorName = ColorName.fromQLab(jsonCue.getString(QLabUtil.FLD_COLOR_NAME, ""));
		m_armed = QLabUtil.getBool(jsonCue, QLabUtil.FLD_ARMED, true);
		m_flagged = QLabUtil.getBool(jsonCue, QLabUtil.FLD_FLAGGED, true);
		
		boolean isBroken = false;
		double duration = 0;
		double prewait = 0;
		double postwait = 0;
		String cueTargetId = "";
		String fileTarget = "";
		QLabUtil.ContinueMode continueMode = QLabUtil.ContinueMode.NO_CONTINUE;
		String notes = "";
		if (queryQLab != null) {
			try {
				isBroken = queryQLab.getIsBroken(m_uniqueId);
				continueMode = queryQLab.getContinueMode(m_uniqueId);
				notes = queryQLab.getNotes(m_uniqueId);
				duration = queryQLab.getDuration(m_uniqueId);
				prewait = queryQLab.getPrewait(m_uniqueId);
				postwait = queryQLab.getPostwait(m_uniqueId);
				cueTargetId = queryQLab.getCueTargetId(m_uniqueId);
				fileTarget = queryQLab.getFileTarget(m_uniqueId);
			} catch (IOException e) {
				// Skip ??
			}
		}
		m_isBroken = isBroken;
		m_continueMode = continueMode;
		m_notes = notes;
		m_duration = duration;
		m_prewait = prewait;
		m_postwait = postwait;
		m_cueTargetId = cueTargetId;
		m_fileTarget = fileTarget;
	}
	
	private boolean autoFromParent(QLabCue parent)
	{
		if (parent != null && parent instanceof QLabGroupCue) {
			return ((QLabGroupCue)parent).m_groupMode != QLabUtil.GroupMode.START_AND_ENTER;
		} else {
			return false;
		}
	}
	
	/**
	 * Return the non-blank name field.
	 * @return The non-blank name field.
	 */
	public String getName()
	{
		if (m_name != null && !m_name.isBlank()) {
			return m_name;
		} else if (m_listName != null && !m_listName.isBlank()) {
			return m_listName;
		} else {
			return "(unnamed)";
		}
	}
	
	/**
	 * Test if a cue is a child of a cuelist, rather than embedded in a group.
	 * @return True iff this is a top-level cue directly in a cuelist.
	 * 		Also return true if this cue is a cuelist.
	 */
	public boolean isTopLevelCue()
	{
		return m_parent == null || m_parent instanceof QLabCuelistCue;
	}
	
	/**
	 * Find the top-level parent of this cue.
	 * Return "this" if this is a top-level cue.
	 * @return The top-level cue which contains this cue.
	 */
	public QLabCue getTopLevelCue()
	{
		QLabCue cue = this;
		for (; !cue.isTopLevelCue(); cue = cue.m_parent) {
			// nothing here
		}
		return cue;
	}
	
	/**
	 * Get the unique id of the cuelist containing this cue.
	 * @return The unique id of the cuelist containing this cue.
	 * 		Never returns null.
	 */
	public String getCuelistId()
	{
		QLabCue cue;
		for (cue = this; cue.m_parent != null; cue = cue.m_parent) {
		}
		return cue.m_parent != null ? cue.m_parent.m_uniqueId : cue.m_uniqueId;
	}
	
	/**
	 * Return the previous cue in this cue's containing cuelist or group.
	 * @return The previous cue, or null.
	 */
	public QLabCue getPrevCue()
	{
		if (m_parent == null) {
			return null;
		}
		List<QLabCue> siblings = m_parent.getChildren();
		int iPrev = m_parentIndex - 1;
		if (iPrev >= 0 && iPrev < siblings.size()) {
			return siblings.get(iPrev);
		} else {
			return null;
		}
	}
	
	/**
	 * Return the nexf cue in this cue's containing cuelist or group.
	 * @return The next cue, or null.
	 */
	public QLabCue getNextCue()
	{
		if (m_parent == null) {
			return null;
		}
		List<QLabCue> siblings = m_parent.getChildren();
		int iNext = m_parentIndex + 1;
		if (iNext >= 0 && iNext < siblings.size()) {
			return siblings.get(iNext);
		} else {
			return null;
		}
	}

	/**
	 * Return a list of the "child cues" contained in this cue,
	 * or null if this isn't a container cue.
	 * The base class returns null.
	 * Container cues must override this.
	 * @return The "child cues" in this cue, or null;
	 */
	public List<QLabCue> getChildren()
	{
		return null;
	}

	@Override
	public String toString() {
		return "QLabCue[type=" + m_type + ",parent=" + m_parent + "[" + m_parentIndex
				+ "],listName=" + m_listName + ",name=" + m_name + ",number=" + m_number
				+ ",uniqueId=" + m_uniqueId + ",armed=" + m_armed + ",flagged=" + m_flagged
				+ ",colorName=" + m_colorName
				+ (m_isAuto ? ",auto" : "")
				+ ",pre/dur/post=" + QLabUtil.fmt3Times(m_prewait, m_duration, m_postwait)
				+ (!m_cueTargetId.isBlank() ? (",target=" + m_cueTargetId) : "")
				+ (!m_fileTarget.isBlank() ? (",file=" + m_fileTarget) : "")			
				;
	}
	
	public void printCue(PrintStream out, String indent, String indentIncr)
	{
		if (out == null) {
			out = System.out;
		}
		out.println(indent
				+ (m_isBroken ? "*** " : "")
				+ (m_isAuto ? ">" : "")
				+ m_type
				+ " num=" + m_number
				+ nameValue(" name=", m_name)
				+ (!m_listName.equals(m_name) ? nameValue(" listName=", m_listName) : "")
				+ " pre/dur/post=" + QLabUtil.fmt3Times(m_prewait, m_duration, m_postwait)
				+ (m_armed ? " armed" : "") + (m_flagged ? " flag" : "")
				+ (m_colorName != QLabUtil.ColorName.NONE ? (" " + m_colorName) : "")
				+ (!m_cueTargetId.isBlank() ? (" target=" + m_cueTargetId) : "")
				+ (m_continueMode != QLabUtil.ContinueMode.NO_CONTINUE ? (" " + m_continueMode) : "")
				+ " id=" + m_uniqueId
				);
		if (m_parent != null) {
			out.println(indent + indentIncr + indentIncr + "parent[" + m_parentIndex + "]: "
					+ m_parent.m_type + "/" + m_parent.m_number + "/" + m_parent.m_name);
		}
	}
	
	private String nameValue(String name, String value)
	{
		if (value == null || value.isBlank()) {
			return "";
		} else {
			return name + "\"" + value + "\"";
		}
	}
}
