package com.wdroome.osc.qlab;

import com.wdroome.json.JSONValue;
import com.wdroome.json.JSONValue_Boolean;
import com.wdroome.json.JSONValue_Number;
import com.wdroome.json.JSONValue_Object;
import com.wdroome.json.JSONValue_String;

/**
 * Constants and static utility methods for communicating with QLab.
 * @author wdr
 */

public class QLabUtil
{
	public static final String DEFAULT_CUELIST_NAME = "Main Cue List";
	
	// The "unique ID" which QLab returns for the "parent ID" of cuelist cues.
	// QueryQLab.getParent() returns "" if QLab returns this ID.
	public static final String CUELIST_CUE_PARENT_ID = "[root group of cue lists]";
	
	public static final String REPLY_PREFIX = "/reply";
	
	public static final String WORKSPACE_REQ_PREFIX = "/workspace/%s";	// Arg is workspace id
	public static final String WORKSPACE_REPLY_PREFIX_PAT = "(/workspace/[-0-9A-Za-z]+)?";
	
	public static final String FLD_WORKSPACE_ID = "workspace_id";
	public static final String FLD_ADDRESS = "address";
	public static final String FLD_STATUS = "status";
	public static final String FLD_DATA = "data";
	
	public static final String VERSION_REQ = "/version";
	public static final String ALWAYS_REPLY_REQ = "/alwaysReply";
	public static final String WORKSPACES_REQ = "/workspaces";
	
	public static final String FLD_UNIQUE_ID = "uniqueID";
	public static final String FLD_DISPLAY_NAME= "displayName";
	public static final String FLD_HAS_PASSCODE = "hasPasscode";
	public static final String FLD_VERSION = "version";
	
	// These requests are optionally prefixed by WORKSPACE_REQ_PREFIX.
	// If there's no prefix, they apply to the currently selected workspace.
	// But the address in the reply always has the workspace prefix.
	
	public static final String CONNECT_REQ = "/connect";
	public static final String DISCONNECT_REQ = "/disconnect";
	
	public static final String CUELISTS_REQ = "/cueLists";
	public static final String SELECTED_CUES_REQ = "/selectedCues";
	public static final String RUNNING_CUES_REQ = "/runningCues";
	public static final String RUNNING_OR_PAUSED_CUES_REQ = "/runningOrPausedCues";
	
	public static final String SELECT_CUE_NUMBER = "/select/%s";
	public static final String SELECT_CUE_ID = "/select_id/%s";

	public static final String FLD_NUMBER = "number";
	public static final String FLD_LIST_NAME = "listName";
	public static final String FLD_NAME = "name";
	public static final String FLD_TYPE = "type";
	public static final String FLD_COLOR_NAME = "colorName";
	public static final String FLD_FLAGGED = "flagged";
	public static final String FLD_ARMED = "armed";
	public static final String FLD_CUES = "cues";
	
	public static final String CUE_NUMBER_REQ_PREFIX = "/cue/";
	public static final String CUE_ID_REQ_PREFIX = "/cue_id/";

	/*
	 * Requests whose names end in "_CUE_REQ" apply to a specific cue.
	 * The request method must be prefixed by either "/cue/cue-number"
	 * or "/cue_id/cue-unique-id".
	 */
	
	/*
	 * Requests for all cue types.
	 */
	public static final String DEFAULT_NAME_CUE_REQ = "/defaultName";	// get only
	public static final String DISPLAY_NAME_CUE_REQ = "/displayName";	// get only
	public static final String LIST_NAME_CUE_REQ = "/listName";			// get only
	public static final String NAME_CUE_REQ = "/name";					// string
	public static final String NOTES_CUE_REQ = "/notes";				// string
	public static final String NUMBER_CUE_REQ = "/number";				// string
	public static final String FLAGGED_CUE_REQ = "/flagged";			// number: 0 false, !0 true
	public static final String ARMED_CUE_REQ = "/armed";				// number: 0 false, !0 true
	public static final String IS_BROKEN_CUE_REQ = "/isBroken";			// get only
	public static final String CUE_TARGET_ID_CUE_REQ = "/cueTargetId";	// string
	public static final String CUE_TARGET_NUMBER_CUE_REQ = "/cueTargetNumber";	// string
	public static final String DURATION_CUE_REQ = "/duration";			// number
	public static final String FILE_TARGET_CUE_REQ = "/fileTarget";		// string
	public static final String PREWAIT_CUE_REQ = "/preWait";			// number
	public static final String POSTWAIT_CUE_REQ = "/postWait";			// number
	public static final String TYPE_CUE_REQ = "/type";					// get only
	public static final String PARENT_CUE_REQ = "/parent";				// get only
	public static final String CHILDREN_UNIQUEIDS_SHALLOW_CUE_REQ
									= "/children/uniqueIDs/shallow";	// get only
	
	public static final String NEW_CUE_REQ = "/new";					// cue-type [cue-id]
	public static final String MOVE_CUE_REQ = "/move/%s";				// cue-id new-index [new-parent-id]
	
	public static final String CONTINUE_MODE_CUE_REQ = "/continueMode";	// number
	public static enum ContinueMode {
					NO_CONTINUE(0), AUTO_CONTINUE(1), AUTO_FOLLOW(2);
		
					private final int m_qlab;
					private ContinueMode(int qlab) { m_qlab = qlab; }
					
					public int toQLab() { return m_qlab; }
					
					public static ContinueMode fromQLab(int v)
					{
						for (ContinueMode mode: ContinueMode.values()) {
							if (mode.ordinal() == v) {
								return mode;
							}
						}
						return NO_CONTINUE;
					}
				}
	
	public static final String COLOR_NAME_CUE_REQ = "/colorName";		// string
	public static enum ColorName {
					NONE, RED, ORANGE, GREEN, BLUE, PURPLE;
				
					public String toQLab() { return toString().toLowerCase(); }
				
					public static ColorName fromQLab(String v)
					{
						for (ColorName color: ColorName.values()) {
							if (color.toString().equalsIgnoreCase(v)) {
								return color;
							}
						}
						return NONE;
					}
				}
	
	/*
	 * Requests for Group cues.
	 */
	public static final String MODE_CUE_REQ = "/mode";					// number (group mode)
	public static enum GroupMode {
					UNKNOWN(0),	START_AND_ENTER(1), START_AND_NEXT(2), TIMELINE(3), RANDOM(4);
		
					private final int m_qlab;
					private GroupMode(int qlab) { m_qlab = qlab; }
					
					public int toQLab() { return m_qlab; }
					
					public static GroupMode fromQLab(int v)
					{
						for (GroupMode mode: GroupMode.values()) {
							if (mode.m_qlab == v) {
								return mode;
							}
						}
						return UNKNOWN;
					}
				}
	
	/*
	 * Requests for Network cues.
	 */
	public static final String PATCH_CUE_REQ = "/patch";				// number (network patch, 1-16)
	public static final String CUSTOM_STRING_CUE_REQ = "/customString";	// OSC command string
	
	public static final String MESSAGE_TYPE_CUE_REQ = "/messageType";	// number 
	public static enum NetworkMessageType {
					UNKNOWN(0), QLab(1), OSC(2), UDP(3);
			
					private final int m_qlab;
					private NetworkMessageType(int qlab) { m_qlab = qlab; }
					
					public int toQLab() { return m_qlab; }
					
					public static NetworkMessageType fromQLab(int v)
					{
						for (NetworkMessageType mode: NetworkMessageType.values()) {
							if (mode.m_qlab == v) {
								return mode;
							}
						}
						return UNKNOWN;
					}
				}
	
	private static final String HEX_DIGIT_PAT = "[A-Fa-z0-9]";
	private static final String UNIQUE_ID_PAT =
											HEX_DIGIT_PAT + "{8}"
									+ "-" + HEX_DIGIT_PAT + "{4}"
									+ "-" + HEX_DIGIT_PAT + "{4}"
									+ "-" + HEX_DIGIT_PAT + "{4}"
									+ "-" + HEX_DIGIT_PAT + "{12}"
									;
	
	/**
	 * Test if a string is a cue unique id or a cue number.
	 * @param id The possible id.
	 * @return True if id looks like a unique identifier.
	 */
	public static boolean isCueId(String id)
	{
		return id.matches(UNIQUE_ID_PAT);
	}
	
	/**
	 * Return the method for a request for a specific cue.
	 * @param numOrId The cue number or cue id. Note that if it's a cue number,
	 * 		it must be acceptable in an OSC method: no blanks or illegal characters.
	 * @param request The request part.
	 * @return The full request, with the appropriate prefix
	 * 			with the cue number or id.
	 */
	public static String getCueReq(String numOrId, String request)
	{
		return (isCueId(numOrId) ? CUE_ID_REQ_PREFIX : CUE_NUMBER_REQ_PREFIX)
									+ numOrId + request;
	}
	
	/**
	 * Return a QLab "boolean" field in a JSON object as a real boolean.
	 * @param json A json object from QLab.
	 * @param fld The field name in json.
	 * @param def The default value.
	 * @return
	 */
	public static boolean getBool(JSONValue_Object json, String fld, boolean def)
	{
		JSONValue v = json.get(fld, null);
		if (v == null) {
			return def;
		} else if (v instanceof JSONValue_String) {
			return !((JSONValue_String)v).m_value.equals("0");
		} else if (v instanceof JSONValue_Number) {
			return ((JSONValue_Number)v).m_value != 0;
		} else if (v instanceof JSONValue_Boolean) {
			return ((JSONValue_Boolean)v).m_value;
		} else {
			return def;
		}
	}
	
	public static String fmtTime(double time)
	{
		return String.format("%.2f", time);
	}
	
	public static String fmt3Times(double time1, double time2, double time3)
	{
		return String.format("%.2f/%.2f/%.2f", time1, time2, time3);
	}
}
