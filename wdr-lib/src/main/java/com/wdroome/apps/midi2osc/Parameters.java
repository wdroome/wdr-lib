package com.wdroome.apps.midi2osc;

import java.util.List;

import com.wdroome.json.JSONValue;
import com.wdroome.json.JSONValue_Boolean;
import com.wdroome.json.JSONValue_Number;
import com.wdroome.json.JSONValue_Object;
import com.wdroome.json.JSONValue_String;

public class Parameters
{
	private final JSONValue_Object m_params;	// Never null

	/**
	 * A name/value pair used to set or override parameters.
	 */
	public static class NameValue
	{
		public final String m_name;
		public final String m_value;
		
		public NameValue(String name, String value)
		{
			m_name = name;
			m_value = value;
		}
		
		@Override
		public String toString()
		{
			return m_name + "=" + m_value;
		}
	}

	public Parameters(JSONValue_Object params, List<NameValue> paramOverrides)
	{
		m_params = params != null ? params : new JSONValue_Object();
		if (paramOverrides != null) {
			for (NameValue nv: paramOverrides) {
				setParam(nv.m_name, nv.m_value);
			}
		}
		List<String> unknown = params.findInvalidKeys(
				List.of(Midi2OscUtil.FN_NEW_VALUE_WAIT_MS,
						Midi2OscUtil.FN_SAME_VALUE_SKIP_MS,
						Midi2OscUtil.FN_CONNECT_TIMEOUT_MS,
						Midi2OscUtil.FN_RECONNECT_WAIT_MS,
						Midi2OscUtil.FN_SHOW,
						Midi2OscUtil.FN_SHOW_RESPONSES,
						Midi2OscUtil.FN_SIM,
						Midi2OscUtil.FN_VERBOSE),
				List.of("^" + Midi2OscUtil.FN_COMMENT_PREFIX + ".*"));
		if (unknown != null) {
			System.err.println("WARNING: Unrecognized fields in \"" + Midi2OscUtil.FN_PARAMS + "\": " + unknown);
		}
	}
	
	/**
	 * Override the value of a parameter in the config file.
	 * @param name The parameter name.
	 * @param value The new value.
	 */
	public void setParam(String name, String value)
	{
		JSONValue v = null;
		if (name.equals(Midi2OscUtil.FN_VERBOSE)
				|| name.equals(Midi2OscUtil.FN_SIM)
				|| name.equals(Midi2OscUtil.FN_SHOW)
				|| name.equals(Midi2OscUtil.FN_SHOW_RESPONSES)) {
			if (value.startsWith("t") || value.startsWith("T")) {
				v = JSONValue_Boolean.TRUE;
			} else {
				v = JSONValue_Boolean.FALSE;
			}
		} else if (name.equals(Midi2OscUtil.FN_SAME_VALUE_SKIP_MS)
				|| name.equals(Midi2OscUtil.FN_NEW_VALUE_WAIT_MS)
				|| name.equals(Midi2OscUtil.FN_RECONNECT_WAIT_MS)
				|| name.equals(Midi2OscUtil.FN_CONNECT_TIMEOUT_MS)) {
			v = new JSONValue_Number(Double.parseDouble(value));
		} else {
			v = new JSONValue_String(value);
		}
		m_params.put(name, v);
	}
		
	public String getStringParam(String paramName, String def)
	{
		return m_params != null ? m_params.getString(paramName, def) : def;
	}
	
	public int getIntParam(String paramName, int def)
	{
		return m_params != null ? (int)m_params.getNumber(paramName, def) : def;
	}
	
	public boolean getBooleanParam(String paramName, boolean def)
	{
		return m_params != null ? m_params.getBoolean(paramName, def) : def;
	}
	
	public boolean isVerbose() { return getBooleanParam(Midi2OscUtil.FN_VERBOSE, true); }	// XXX def -> false

	@Override
	public String toString() {
		return "Parameters[" + m_params + "]";
	}

}
