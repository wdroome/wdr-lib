package com.wdroome.osc.qlab;

import java.util.List;
import java.util.ArrayList;

import java.io.PrintStream;

import com.wdroome.json.JSONValue;
import com.wdroome.json.JSONValue_Object;
import com.wdroome.json.JSONValue_Array;
import com.wdroome.json.JSONValue_ObjectArray;

/**
 * A cue retrieved from QLab.
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
	public final String m_colorName;
	
	// Contained cues for Group & CueList cues. May be null.
	public final List<QLabCue> m_cues;
	
	/**
	 * Create a QlabCue from a QLab reply message.
	 * This includes all child cues.
	 * @param jsonCue The cue data from QLab.
	 * @param parent The parent cue, if not null.
	 * @param parentIndex The index in the parent cue, if there is one.
	 */
	public QLabCue(JSONValue_Object jsonCue, QLabCue parent, int parentIndex)
	{
		m_parent = parent;
		m_parentIndex = parentIndex;
		m_type = QLabCueType.fromQLab(jsonCue.getString(QLabUtil.FLD_TYPE, QLabCueType.UNKNOWN.toString()));
		m_listName = jsonCue.getString(QLabUtil.FLD_LIST_NAME, "");
		m_uniqueId = jsonCue.getString(QLabUtil.FLD_UNIQUE_ID, "");
		m_name = jsonCue.getString(QLabUtil.FLD_NAME, "");
		m_number = jsonCue.getString(QLabUtil.FLD_NUMBER, "");
		m_colorName = jsonCue.getString(QLabUtil.FLD_COLOR_NAME, "");
		m_armed = QLabUtil.getBool(jsonCue, QLabUtil.FLD_ARMED, true);
		m_flagged = QLabUtil.getBool(jsonCue, QLabUtil.FLD_FLAGGED, true);
		m_cues = getCueArray(jsonCue.getArray(QLabUtil.FLD_CUES, null), this);
	}

	@Override
	public String toString() {
		return "QLabCue[type=" + m_type + ",parent=" + m_parent + "[" + m_parentIndex
				+ "],listName=" + m_listName + ",name=" + m_name + ",number=" + m_number + ",uniqueId="
				+ m_uniqueId + ",armed=" + m_armed + ",flagged=" + m_flagged + ",colorName=" + m_colorName
				+ ",cues=" + m_cues + "]";
	}

	public static List<QLabCue> getCueArray(JSONValue_Array arr, QLabCue parent)
	{
		List<QLabCue> cues = null;
		if (arr != null) {
			cues = new ArrayList<>();
			for (JSONValue_Object v: new JSONValue_ObjectArray(arr)) {
				cues.add(new QLabCue(v, parent, cues.size()));
			}
		}
		return cues;
	}
	
	public void printCue(PrintStream out, String indent, String indentIncr)
	{
		if (out == null) {
			out = System.out;
		}
		out.println(indent + m_type + " num=" + m_number
				+ nameValue(" name=", m_name)
				+ nameValue(" listName=", m_listName)
				+ (m_armed ? " armed" : "") + (m_flagged ? " flag" : "")
				+ (!QLabUtil.VALUE_COLOR_NAME_NONE.equals(m_colorName) ? m_colorName : "")
				+ " id=" + m_uniqueId
				);
		if (m_parent != null) {
			out.println(indent + indentIncr + indentIncr + "parent[" + m_parentIndex + "]: "
					+ m_parent.m_type + "/" + m_parent.m_number + "/" + m_parent.m_name);
		}
		if (m_cues != null) {
			for (QLabCue child: m_cues) {
				child.printCue(out, indent + indentIncr, indentIncr);
			}
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
