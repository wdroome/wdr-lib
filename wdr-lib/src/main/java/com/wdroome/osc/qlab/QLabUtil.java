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

	public static final String FLD_NUMBER = "number";
	public static final String FLD_LIST_NAME = "listName";
	public static final String FLD_NAME = "name";
	public static final String FLD_TYPE = "type";
	public static final String FLD_COLOR_NAME = "colorName";
	public static final String FLD_FLAGGED = "flagged";
	public static final String FLD_ARMED = "armed";
	public static final String FLD_CUES = "cues";
	
	public static final String VALUE_COLOR_NAME_NONE = "none";
	
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


}
