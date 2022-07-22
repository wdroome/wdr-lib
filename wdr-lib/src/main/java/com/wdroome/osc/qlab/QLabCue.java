package com.wdroome.osc.qlab;

import java.util.List;
import java.util.ArrayList;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.function.BiPredicate;

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
	private QLabCue m_parent = null;
	
	// If m_parent isn't null, the index in m_parent.
	protected final int m_parentIndex;
	
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
	
	private boolean m_isAuto;
	
	/**
	 * Create a QlabCue from a QLab reply message.
	 * This includes all child cues.
	 * @param jsonCue The cue data from QLab.
	 * @param parent The parent cue, if not null.
	 * @param parentIndex The index in the parent cue, if there is one.
	 */
	public QLabCue(JSONValue_Object jsonCue, QLabCue parent, int parentIndex,
							boolean isAuto, QueryQLab queryQLab)
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
	
	protected QLabCue(String uniqueId, QLabCueType type, QueryQLab queryQLab)
	{
		long t0 = System.currentTimeMillis();
		m_uniqueId = uniqueId;
		m_type = type;
		m_parent = null;
		m_parentIndex = -1;
		m_isAuto = false;
		
		String number = "";
		String listName = "";
		String name = "";
		QLabUtil.ColorName colorName = QLabUtil.ColorName.NONE;
		boolean armed = true;
		boolean flagged = false;
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
				number = queryQLab.getNumber(m_uniqueId);
				listName = queryQLab.getListName(m_uniqueId);
				name = queryQLab.getName(m_uniqueId);
				colorName = queryQLab.getColorName(m_uniqueId);
				flagged = queryQLab.getIsFlagged(m_uniqueId);
				armed = queryQLab.getArmed(m_uniqueId);
				name = queryQLab.getName(m_uniqueId);
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
		m_number = number;
		m_listName = listName;
		m_name = name;
		m_colorName = colorName;
		m_armed = armed;
		m_flagged = flagged;
		m_isBroken = isBroken;
		m_continueMode = continueMode;
		m_notes = notes;
		m_duration = duration;
		m_prewait = prewait;
		m_postwait = postwait;
		m_cueTargetId = cueTargetId;
		m_fileTarget = fileTarget;
	}
	
	/**
	 * Get the cue or list which contains this cue.
	 * @return The containing cue, or null if none.
	 */
	public QLabCue getParent()
	{
		return m_parent;
	}
	
	/**
	 * Return the unique ID of the cue or list which contains this cue,
	 * or "" if there isn't a parent.
	 * @return The parent's unique ID, or "" if there isn't a parent.
	 */
	public String getParentId()
	{
		return m_parent != null ? m_parent.m_uniqueId : "";
	}
	
	protected void setParent(QLabCue parent)
	{
		m_parent = parent;
	}
	
	public boolean isAuto()
	{
		return m_isAuto;
	}
	
	protected void setIsAuto(boolean isAuto)
	{
		m_isAuto = isAuto;
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
	 * Get the cuelist containing this cue.
	 * @return The cuelist cue containing this cue, or null if we cannot determine it.
	 */
	public QLabCuelistCue getCuelist()
	{
		if (m_parent == null) {
			return (this instanceof QLabCuelistCue) ? (QLabCuelistCue)this : null;
		}
		QLabCue cue;
		for (cue = this; cue.m_parent != null; cue = cue.m_parent) {
		}
		QLabCue root = cue.m_parent;
		return (cue instanceof QLabCuelistCue) ? (QLabCuelistCue)root : null;
	}
	
	/**
	 * Return the index of this cue in it's container cue,
	 * of -1 if it's not in a cuelist or a group cue.
	 * @return
	 */
	public int getIndexInParent()
	{
		return m_parent != null ? m_parent.getIndexOfChild(m_uniqueId) : -1;
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
		int iPrev = getIndexInParent() - 1;
		if (iPrev >= 0 && iPrev < siblings.size()) {
			return siblings.get(iPrev);
		} else {
			return null;
		}
	}
	
	/**
	 * Return the next cue in this cue's containing cuelist or group.
	 * @return The next cue, or null.
	 */
	public QLabCue getNextCue()
	{
		if (m_parent == null) {
			return null;
		}
		List<QLabCue> siblings = m_parent.getChildren();
		int iNext = getIndexInParent() + 1;
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
	
	/**
	 * Get the index of a child in this list,
	 * or -1 if it's not in this list or this isn't a container cue.
	 * The base class returns -1; container cue classes should override.
	 * @param childId The unique ID of a cue.
	 * @return The index of childId in this container cue, or -1.
	 */
	public int getIndexOfChild(String childId)
	{
		return -1;
	}
	
	/**
	 * Insert a cue into a container cue.
	 * @param cue The cue to add. It should be in QLab.
	 * @param cue the cue to insert.
	 * @return True iff successful. The base class always returns false.
	 */
	protected boolean insertCue(QLabCue cue, QueryQLab queryQLab)
	{
		return false;
	}
	
	/**
	 * Call a function on this cue and on all contained cues.
	 * That is, "walk the cue tree."
	 * @param handleCue The function to be called. The first argument is the cue being processed.
	 * 			The second argument is the stack of cues which contain the cue.
	 * 			This stack only goes up to the cue on which walkCues() was invoked.
	 * 			If the predicate returns false, stop walking the cue tree.
	 * @return The number of cues walked.
	 */
	public int walkCues(BiPredicate<QLabCue, Stack<? extends QLabCue>> handleCue)
	{
		WalkState walkState = myWalkCues(null, handleCue);
		return walkState.m_nWalked;
	}
	
	/**
	 * Call a function on all cues in a list and on all contained cues.
	 * That is, "walk the cue tree(s)."
	 * @param handleCue The function to be called. The first argument is the cue being processed.
	 * 			The second argument is the stack of cues which contain the cue.
	 * 			This stack only goes up to the cue in the list on which walkCues() was invoked.
	 * 			If the predicate returns false, stop walking the cue tree.
	 * @return The number of cues walked.
	 */
	public static int walkCues(List<? extends QLabCue> cues,
								BiPredicate<QLabCue, Stack<? extends QLabCue>> handleCue)
	{
		WalkState walkState = new WalkState();
		if (cues != null) {
			for (QLabCue cue: cues) {
				cue.myWalkCues(walkState, handleCue);
				if (!walkState.m_walking) {
					break;
				}
			} 
		}
		return walkState.m_nWalked;
	}
	
	private static class WalkState
	{
		private boolean m_walking = true;
		private int m_nWalked = 0;
		Stack<QLabCue> m_path = new Stack<>();
	}
	
	private WalkState myWalkCues(WalkState walkState, BiPredicate<QLabCue, Stack<? extends QLabCue>> handleCue)
	{
		if (walkState == null) {
			walkState = new WalkState();
		}
		if (walkState.m_walking) {
			walkState.m_nWalked++;
			if (!handleCue.test(this, walkState.m_path)) {
				walkState.m_walking = false;
			} else {
				List<? extends QLabCue> children = getChildren();
				if (children != null) {
					walkState.m_path.push(this);
					for (QLabCue child: children) {
						child.myWalkCues(walkState, handleCue);
						if (!walkState.m_walking) {
							break;
						}
					}
					walkState.m_path.pop();
				}
			}
		}
		return walkState;
	}
	
	/**
	 * Find the cue in a cuelist tree with a unique ID.
	 * @param cueId The id to find.
	 * @param cues A list of cues (usually a list of cuelist cues).
	 * @return The cue with cueId, or null if there is no such cue.
	 */
	public static QLabCue findCue(String cueId, List<? extends QLabCue> cues)
	{
		List<QLabCue> revPath = findCuePath(cueId, cues);
		return (revPath != null && !revPath.isEmpty()) ? revPath.get(0) : null;
	}
		
	/**
	 * Find the path to the cue in a cuelist tree with a unique ID.
	 * @param cueId The id to find.
	 * @param cues A list of cues (usually a list of cuelists).
	 * @return A list with the found cue and it's containing cues.
	 * 			[0] is the found cue, [1] is it's container,
	 * 			[2] is the containing cue's container, etc.
	 * 			If not found, return an empty list.
	 */
	public static List<QLabCue> findCuePath(String cueId, List<? extends QLabCue> cues)
	{
		final ArrayList<List<QLabCue>> foundListHolder = new ArrayList<>();
		walkCues(cues, (cue,path) -> {
					if (cueId.equals(cue.m_uniqueId)) {
						ArrayList<QLabCue> retList = new ArrayList<>();
						retList.add(cue);
						for (int i = path.size()-1; i >= 0; --i) {
							retList.add(path.get(i));
						}
						foundListHolder.add(retList);
						return false;
					} else {
						return true;
					}
				});
		return !foundListHolder.isEmpty() ? foundListHolder.get(0) : List.of();
	}

	@Override
	public String toString() {
		return "QLabCue[type=" + m_type + ",parent=" + getParentId() + "[" + m_parentIndex
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
