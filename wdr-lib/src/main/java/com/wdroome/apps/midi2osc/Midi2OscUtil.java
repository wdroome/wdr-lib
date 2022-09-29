package com.wdroome.apps.midi2osc;

import com.wdroome.json.JSONValue;
import com.wdroome.json.JSONValue_Array;
import com.wdroome.json.JSONValue_Number;
import com.wdroome.json.JSONValue_Object;
import com.wdroome.json.JSONValue_String;

/**
 * Static utility methods for the Midi2Osc application.
 * @author wdr
 */
public class Midi2OscUtil
{
	// Names of fields in JSON config file.
	public static final String FN_TRANSFORMS = "transforms";
	public static final String FN_OSC_SERVERS = "osc-servers";
	public static final String FN_OSC_SERVER = "osc-server";
	public static final String FN_MIDI_CONTROLLER = "midi-controller";
	public static final String FN_OSC_CMDS = "osc-cmds";
	public static final String FN_OSC_CMD = "osc-cmd";
	public static final String FN_NAME = "name";
	public static final String FN_COMMENT_PREFIX = "#";
	
	public static final String FN_ACTIONS = "actions";
	public static final String FN_DATA1 = "data1";
	public static final String FN_DATA2 = "data2";

	public static final String FN_DATA1_MAP = "data1-map";
	public static final String FN_MAP = "map";
	public static final String FN_TYPE = "type";
	public static final String FN_CHAN = "chan";

	public static final String FN_PARAMS = "params";
	public static final String FN_VERBOSE = "verbose";
	public static final String FN_SAME_VALUE_SKIP_MS = "sameValueSkipMS";
	public static final String FN_NEW_VALUE_WAIT_MS = "newValueWaitMS";
	public static final String FN_SIM = "sim";
	public static final String FN_SHOW = "show";
	public static final String FN_SHOW_RESPONSES = "show-responses";
	public static final String FN_CONNECT_TIMEOUT_MS = "connectTimeoutMS";
	public static final String FN_RECONNECT_WAIT_MS = "reconnectWaitMS";
	public static final String FN_IPADDRS = "ipaddrs";
	
	public static final String FN_SCALE = "scale";
	public static final String FN_ADD = "add";
	public static final String FN_MULT = "mult";
	public static final String FN_CLIP = "clip";
	public static final String FN_FMT = "fmt";

	public static final int DEF_CONNECT_TIMEOUT_MS = 2000;
	public static final int DEF_RECONNECT_WAIT_MS = 5000;
	public static final int DEF_SAME_VALUE_SKIP_MS = 0;
	public static final int DEF_NEW_VALUE_WAIT_MS = 50;
	
	public static final int MIN_RECONNECT_WAIT_MS = 100;	// min value if > 0

	/**
	 * Get a two-number JSON array from a JSON object.
	 * @param obj The JSON object.
	 * @param name The name of the child with the two-element array.
	 * @param def The default if there is no such array.
	 * @return The child array "name" as a double[2], or "def" if not found.
	 */
	public static double[] get2Array(JSONValue_Object obj, String name, double[] def)
	{
		if (obj == null) {
			return def;
		}
		JSONValue xpair = obj.get(name);
		if (xpair == null) {
			return def;
		}
		if (xpair instanceof JSONValue_Number) {
			double v = ((JSONValue_Number)xpair).m_value;
			return new double[] {v, v};
		} else if (xpair instanceof JSONValue_Array) {
			return new double[] {
					((JSONValue_Array)xpair).getNumber(0, def[0]),
					((JSONValue_Array)xpair).getNumber(1, def[1])
			};
		} else {
			return def;
		}
	}
	
	/**
	 * If JSON object "dir" has an object named "name1",
	 * return the object named "name2" in that object.
	 * Otherwise return null.
	 * @param dir A JSON object.
	 * @param name1 Name of an object in "dir".
	 * @param name2 Name of an object in "name1".
	 * @return The object dir/name1/name2, or null if there is no object with that path.
	 */
	public static JSONValue_Object getObject(JSONValue_Object dir, String name1, String name2)
	{
		if (dir == null) {
			return null;
		}
		JSONValue_Object obj = dir.getObject(name1, null);
		if (obj == null) {
			return null;
		}
		return obj.getObject(name2, null);
	}
	
	/**
	 * If JSON object "dir" has an object named "name1",
	 * return the object named "name2" in that object.
	 * Otherwise return null.
	 * @param dir A JSON object.
	 * @param name1 Name of an object in "dir".
	 * @param name2 Name of an object in "name1".
	 * @return The object dir/name1/name2, or null if there is no object with that path.
	 */
	public static JSONValue_Object getObject(JSONValue_Object dir, String name1, String name2, String name3)
	{
		if (dir == null) {
			return null;
		}
		JSONValue_Object obj = dir.getObject(name1, null);
		if (obj == null) {
			return null;
		}
		obj = obj.getObject(name2, null);
		if (obj == null) {
			return null;
		}
		return obj.getObject(name3, null);
	}
}