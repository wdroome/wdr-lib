package com.wdroome.osc.qlab;

import java.util.List;
import java.util.ArrayList;

import com.wdroome.json.JSONValue;
import com.wdroome.json.JSONValueTypeException;
import com.wdroome.json.JSONValue_Object;
import com.wdroome.json.JSONValue_String;
import com.wdroome.json.JSONValue_Array;
import com.wdroome.json.JSONValue_BigInt;
import com.wdroome.json.JSONValue_ObjectArray;
import com.wdroome.json.JSONValue_Boolean;
import com.wdroome.json.JSONValue_Number;
import com.wdroome.json.JSONParser;

import com.wdroome.json.JSONLexan;
import com.wdroome.json.JSONParseException;

import com.wdroome.osc.OSCMessage;

public class QLabReply
{
	/**
	 * Status in a QLab response.
	 * DENIED is new in QLab5.
	 */
	public static enum Status
	{
		OK, ERROR, DENIED;
		
		public static Status fromQLab(String fromQLab)
		{
			if (fromQLab != null) {
				for (Status status : Status.values()) {
					if (status.toString().equalsIgnoreCase(fromQLab)) {
						return status;
					}
				} 
			}
			return ERROR;
		}
	};

	/** Workspace id. Never null, but may be "". */
	public final String m_workspaceId;
	
	/** Address. Never null. */
	public final String m_address;
	
	/** Status. See {@link #isOk()}. */
	public final Status m_status;
	
	/** Generic JSON data from QLab. May be null, but only if error. */
	private final JSONValue m_data;
	
	/**
	 * Create a reply object from a QLab reply message.
	 * @param msg The QLab reply message.
	 * @throws JSONParseException If the reply argument isn't valid JSON.
	 * @throws JSONValueTypeException If the reply argument isn't valid JSON.
	 */
	public QLabReply(OSCMessage msg) throws JSONParseException, JSONValueTypeException
	{
		String resp = msg.getString(0, "");
		JSONValue_Object reply = JSONParser.parseObject(new JSONLexan(resp), true);
		m_workspaceId = reply.getString(QLabUtil.FLD_WORKSPACE_ID, "");
		m_address = reply.getString(QLabUtil.FLD_ADDRESS, "");
		m_status = Status.fromQLab(reply.getString(QLabUtil.FLD_STATUS, Status.ERROR.toString()));
		m_data = reply.get(QLabUtil.FLD_DATA, null);
	}
	
	/**
	 * Test if the request was successful.
	 * @return True iff the status is "OK".
	 */
	public boolean isOk()
	{
		return m_status == Status.OK;
	}
	
	/**
	 * Test if the request failed because the requester didn't have the necessary permissions.
	 * @return True if the request was denied because the client has not connected with a valid passcode.
	 */
	public boolean isDenied()
	{
		return m_status == Status.DENIED;
	}

	@Override
	public String toString() {
		return "QLabReply[workspace=" + m_workspaceId + ", addr=" + m_address + ", status=" + m_status
				+ ", data=" + m_data + "]";
	}
	
	/**
	 * Get the data field in the reply as a generic JSONValue.
	 * @param def The default value.
	 * @return The data field as a generic JSONValue, or def if null.
	 */
	public JSONValue getData(JSONValue def)
	{
		return m_data != null ? m_data : def;
	}
	
	/**
	 * Get the data field in the reply as a JSON dictionary.
	 * @param def The default value.
	 * @return The data field as a JSON dictionary, or def if it isn't a dictionary.
	 */
	public JSONValue_Object getJSONObject(JSONValue_Object def)
	{
		if (m_data == null) {
			return def;
		} else if (m_data instanceof JSONValue_Object) {
			return (JSONValue_Object)m_data;
		} else {
			return def;
		}
	}
	
	/**
	 * Get the data field in the reply as a JSON array.
	 * @param def The default value.
	 * @return The data field as a JSON array, or def if it isn't a array.
	 */
	public JSONValue_Array getJSONArray(JSONValue_Array def)
	{
		if (m_data == null) {
			return def;
		} else if (m_data instanceof JSONValue_Array) {
			return (JSONValue_Array)m_data;
		} else {
			return def;
		}
	}
	
	/**
	 * Get the data field in the reply as a JSON array of JSON dictionaries.
	 * @param def The default value.
	 * @return The data field as a JSON array of dictionaries, or def if it isn't an array.
	 */
	public JSONValue_ObjectArray getJSONObjectArray(JSONValue_ObjectArray def)
	{
		if (m_data == null) {
			return def;
		} else if (m_data instanceof JSONValue_Array) {
			return new JSONValue_ObjectArray((JSONValue_Array)m_data);
		} else {
			return def;
		}
	}
	
	/**
	 * Return the reply data as a single boolean value.
	 * @param def The default value.
	 * @return The boolean value of the m_data field, or def.
	 */
	public boolean getBool(boolean def)
	{
		if (m_data == null) {
			return def;
		} else if (m_data instanceof JSONValue_String) {
			return !((JSONValue_String)m_data).m_value.equals("0");
		} else if (m_data instanceof JSONValue_Number) {
			return ((JSONValue_Number)m_data).m_value != 0;
		} else if (m_data instanceof JSONValue_Boolean) {
			return ((JSONValue_Boolean)m_data).m_value;
		} else {
			return def;
		}
	}
	
	/**
	 * Return the reply data as a single boolean value.
	 * @param def The default value.
	 * @return The boolean value of the m_data field, or def.
	 */
	public String getString(String def)
	{
		if (m_data == null) {
			return def;
		} else if (m_data instanceof JSONValue_String) {
			return ((JSONValue_String)m_data).m_value;
		} else if (m_data instanceof JSONValue_Number) {
			return ((JSONValue_Number)m_data).m_value + "";
		} else if (m_data instanceof JSONValue_Boolean) {
			return ((JSONValue_Boolean)m_data).m_value ? "true" : "false";
		} else {
			return def;
		}
	}
	
	/**
	 * Return the reply data as a long value.
	 * @param def The default value.
	 * @return The long value of the m_data field, or def.
	 */
	public long getLong(long def)
	{
		if (m_data == null) {
			return def;
		} else if (m_data instanceof JSONValue_Number) {
			return (long)(((JSONValue_Number)m_data).m_value);
		} else if (m_data instanceof JSONValue_String) {
			try { return (long)Double.parseDouble(((JSONValue_String)m_data).m_value); }
			catch (Exception e) { return def; }
		} else if (m_data instanceof JSONValue_Boolean) {
			return ((JSONValue_Boolean)m_data).m_value ? 1 : 0;
		} else {
			return def;
		}
	}
	
	/**
	 * Return the reply data as a long value.
	 * @param def The default value.
	 * @return The long value of the m_data field, or def.
	 */
	public double getDouble(double def)
	{
		if (m_data == null) {
			return def;
		} else if (m_data instanceof JSONValue_Number) {
			return ((JSONValue_Number)m_data).m_value;
		} else if (m_data instanceof JSONValue_String) {
			try { return Double.parseDouble(((JSONValue_String)m_data).m_value); }
			catch (Exception e) { return def; }
		} else if (m_data instanceof JSONValue_Boolean) {
			return ((JSONValue_Boolean)m_data).m_value ? 1 : 0;
		} else {
			return def;
		}
	}
	
	/**
	 * Return the reply data as a List of Strings.
	 * @param def The default value.
	 * @return The reply data as a String List, or def.
	 */
	public List<String> getStringList(List<String> def)
	{
		if (m_data == null) {
			return def;
		} else if (m_data instanceof JSONValue_String) {
			return List.of(((JSONValue_String)m_data).m_value);
		} else if (m_data instanceof JSONValue_Array) {
			List<String> list = new ArrayList<>();
			for (JSONValue val: (JSONValue_Array)m_data) {
				if (val instanceof JSONValue_String) {
					list.add(((JSONValue_String)val).m_value);
				} else if (val instanceof JSONValue_Number) {
					list.add(Double.toString(((JSONValue_Number)val).m_value));
				} else if (val instanceof JSONValue_Boolean) {
					list.add(((JSONValue_Boolean)val).m_value ? "true" : "false");
				}
			}
			return list;
		} else {
			return def;
		}
	}
}
