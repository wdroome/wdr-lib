package com.wdroome.osc.qlab;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import java.util.function.Predicate;

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
	FADE("Fade"),
	NETWORK((jsonCue, parent, parentIndex, isAuto, queryQLab)
						-> new QLabNetworkCue(jsonCue, parent, parentIndex, isAuto, queryQLab),
			(id, type, queryQLab) -> new QLabNetworkCue(id, queryQLab),
			"Network", null),
	MIDI("MIDI"),
	MIDIFILE("MIDI File"),
	TIMECODE("Timecode"),
	GROUP((jsonCue, parent, parentIndex, isAuto, queryQLab)
					-> new QLabGroupCue(jsonCue, parent, parentIndex, isAuto, queryQLab),
			null, "Group", null),
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
	CUELIST((jsonCue, parent, parentIndex, isAuto, queryQLab)
						-> new QLabCuelistCue(jsonCue, parent, parentIndex, isAuto, queryQLab),
			null, "Cue List", null),
	UNKNOWN("???");
	
	private final String m_toQLab;
	private final String m_fromQLab;
	private final JsonCueMaker m_jsonCueMaker;
	private final IdCueMaker m_IdCueMaker;
	
	private QLabCueType(String fromQLab)
	{
		this(null, null, fromQLab, null);
	}
	
	private QLabCueType(JsonCueMaker cueMaker, IdCueMaker cueInserter, String toQLab, String fromQLab)
	{
		m_jsonCueMaker = cueMaker != null ? cueMaker : new DefaultJsonCueMaker();
		m_IdCueMaker = cueInserter != null ? cueInserter : new DefaultIdCueMaker();
		m_toQLab = toQLab;
		m_fromQLab = fromQLab != null ? fromQLab : toQLab;
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
	
	@FunctionalInterface
	private interface JsonCueMaker
	{
		QLabCue makeCue(JSONValue_Object jsonCue, QLabCue parent, int parentIndex,
											boolean isAuto, QueryQLab queryQLab);
	}
	
	@FunctionalInterface
	private interface IdCueMaker
	{
		QLabCue makeCue(String uniqueId, QLabCueType type, QueryQLab queryQLab);
	}
	
	private static class DefaultJsonCueMaker implements JsonCueMaker
	{
		public QLabCue makeCue(JSONValue_Object jsonCue, QLabCue parent, int parentIndex,
										boolean isAuto, QueryQLab queryQLab)
		{
			return new QLabCue(jsonCue, parent, parentIndex, isAuto, queryQLab);
		}
	}
	
	private static class DefaultIdCueMaker implements IdCueMaker
	{
		public QLabCue makeCue(String uniqueId, QLabCueType type, QueryQLab queryQLab)
		{
			return new QLabCue(uniqueId, type, queryQLab);
		}
	}
	
	/**
	 * Create a new QLabCue from the json returned by a /cueLists request.
	 * @param jsonCue The JSON cue information.
	 * @param parent The parent cue.
	 * @param parentIndex The index in the parent.
	 * @param isAuto True iff this is an automatic cue.
	 * @param queryQLab The connection to QLab.
	 * @return The new QLabCue object.
	 */
	public static QLabCue makeCue(JSONValue_Object jsonCue, QLabCue parent, int parentIndex,
									boolean isAuto, QueryQLab queryQLab)
	{
		QLabCueType type = QLabCueType.fromQLab(jsonCue.getString(QLabUtil.FLD_TYPE,
											QLabCueType.UNKNOWN.toString()));
		return type.m_jsonCueMaker.makeCue(jsonCue, parent, parentIndex, isAuto, queryQLab);
	}
	
	/**
	 * Create a QLabCue object for a cue that has been added to QLab,
	 * and place that object in a cuelists tree.
	 * @param uniqueId The id of the newly added cue.
	 * @param cuelists A list of cuelists, as returned by {@link QueryQLab#getAllCueLists()}.
	 * @param queryQLab The QLab connection.
	 * @return The new QLabCue object, or null if it cannot be created.
	 * @throws IOException If an IO error occurs.
	 */
	public static QLabCue insertNewCue(String uniqueId, List<? extends QLabCue> cuelists, QueryQLab queryQLab)
								throws IOException
	{
		QLabCue newCue = null;
		String parentId = queryQLab.getParent(uniqueId);
		if (parentId == null || parentId.isBlank()) {
			System.out.println("QLabCueType.insertNewCue: cannot find parent ID of " + uniqueId);
			return null;
		}
		QLabCue parentCue = QLabCue.findCue(parentId, cuelists);
		if (parentCue == null) {
			System.out.println("QLabCueType.insertNewCue: cannot find parent cue for ID " + uniqueId);
			return null;
		}
		QLabCueType newType = queryQLab.getType(uniqueId);
		newCue = newType.m_IdCueMaker.makeCue(uniqueId, newType, queryQLab);
		if (!parentCue.insertCue(newCue, queryQLab)) {
			System.err.println("QLabCueType.insertCue: Cannot insert into type "
								+ parentCue.m_type);
		}
		return newCue;
	}

	public static List<QLabCue> getCueArray(JSONValue_Array jsonCues, QLabCue parent,
							boolean isAuto, QueryQLab queryQLab)
	{
		List<QLabCue> cues = new ArrayList<>();
		if (jsonCues != null) {
			for (JSONValue_Object v: new JSONValue_ObjectArray(jsonCues)) {
				QLabCue cue = makeCue(v, parent, cues.size(), isAuto, queryQLab);
				cues.add(cue);
				isAuto = cue.m_continueMode == QLabUtil.ContinueMode.AUTO_CONTINUE
							|| cue.m_continueMode == QLabUtil.ContinueMode.AUTO_FOLLOW;
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
		for (QLabCueType type: QLabCueType.values()) {
			System.out.println(type + ": " + type.m_toQLab + " " + type.m_fromQLab);
		}
		for (String arg: args) {
			QLabCueType t = QLabCueType.fromQLab(arg);
			System.out.println(arg + "=> " + t + " \"" + t.toQLab() + "\"");
		}
	}
}
