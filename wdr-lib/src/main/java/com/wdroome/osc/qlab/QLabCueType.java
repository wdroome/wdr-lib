package com.wdroome.osc.qlab;

import java.util.ArrayList;
import java.util.List;

import com.wdroome.json.JSONValue_Array;
import com.wdroome.json.JSONValue_Object;
import com.wdroome.json.JSONValue_ObjectArray;

/**
 * The cue types QLab supports.
 * @author wdr
 */
public enum QLabCueType
{
	AUDIO("Audio"),
	MIC("Mic"),
	VIDEO("Video"),
	CAMERA("Camera"),
	TEXT("Text"),
	LIGHT("Light"),
	FADE("Fade)"),
	NETWORK("Network"),
	MIDI("MIDI"),
	MIDIFILE("MIDI File"),
	TIMECODE("Timecode"),
	GROUP("Group"),
	START("Start"),
	STOP("Stop"),
	PAUSE("Pause"),
	LOAD("Load"),
	RESET("Reset"),
	DEVAMP("Devamp"),
	GOTO("Goto"),
	TARGET("Target"),
	ARM("Arm"),
	DISARM("Disarm"),
	WAIT("Wait"),
	MEMO("Memo"),
	SCRIPT("Script"),
	LIST("List"),
	CUELIST("Cue List"),
	UNKNOWN("???");
	
	private final String m_toQLab;
	private final String m_fromQLab;
	
	private QLabCueType(String fromQLab)
	{
		m_toQLab = fromQLab;
		m_fromQLab = fromQLab;
	}
	
	private QLabCueType(String fromQLab, String toQLab)
	{
		m_toQLab = toQLab;
		m_fromQLab = fromQLab;
	}
	
	/**
	 * Return the QLabCueType value for a cue type code from QLab.
	 * @param fromQLab The type string from QLab.
	 * @return The type, or UNKNOWN if not recognized.
	 */
	public static QLabCueType fromQLab(String fromQLab)
	{
		if (fromQLab != null) {
			for (QLabCueType type : QLabCueType.values()) {
				if (fromQLab.equalsIgnoreCase(type.m_fromQLab)) {
					return type;
				}
			}
			fromQLab = fromQLab.replaceAll(" ", "");
			for (QLabCueType type : QLabCueType.values()) {
				if (fromQLab.equalsIgnoreCase(type.m_fromQLab.replaceAll(" ", ""))) {
					return type;
				}
			} 
		}
		return UNKNOWN;
	}
	
	/**
	 * Return the type code to send to QLab in a "create new cue" request.
	 * @return The type code to send to QLab in a "create new cue" request.
	 */
	public String toQLab()
	{
		return m_toQLab.toLowerCase();
	}
	
	public static QLabCue makeCue(JSONValue_Object jsonCue, QLabCue parent, int parentIndex, QueryQLab queryQLab)
	{
		QLabCueType type = QLabCueType.fromQLab(jsonCue.getString(QLabUtil.FLD_TYPE,
											QLabCueType.UNKNOWN.toString()));
		switch (type) {
		case GROUP: return new QLabGroupCue(jsonCue, parent, parentIndex, queryQLab);
		case CUELIST: return new QLabCuelistCue(jsonCue, parent, parentIndex, queryQLab);
		case NETWORK: return new QLabNetworkCue(jsonCue, parent, parentIndex, queryQLab);
			
		default: return new QLabCue(jsonCue, parent, parentIndex, queryQLab);
		}
	}

	public static List<QLabCue> getCueArray(JSONValue_Array arr, QLabCue parent, QueryQLab queryQLab)
	{
		List<QLabCue> cues = new ArrayList<>();
		if (arr != null) {
			for (JSONValue_Object v: new JSONValue_ObjectArray(arr)) {
				cues.add(makeCue(v, parent, cues.size(), queryQLab));
			}
		}
		return cues;
	}

	/**
	 * For testing.
	 * @param args
	 */
	public static void main(String[] args)
	{
		if (args == null || args.length == 0) {
			args = new String[] {"Audio", "audio", "Midi File", "midifile", "cuelist", "cue list", "Network"};
		}
		for (String arg: args) {
			QLabCueType t = QLabCueType.fromQLab(arg);
			System.out.println(arg + "=> " + t + " \"" + t.toQLab() + "\"");
		}
	}
}
