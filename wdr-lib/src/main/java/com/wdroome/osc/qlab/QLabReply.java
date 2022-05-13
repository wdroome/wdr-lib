package com.wdroome.osc.qlab;

import com.wdroome.json.JSONValue;
import com.wdroome.json.JSONValueTypeException;
import com.wdroome.json.JSONValue_Object;
import com.wdroome.json.JSONValue_Array;
import com.wdroome.json.JSONParser;

import com.wdroome.json.JSONLexan;
import com.wdroome.json.JSONParseException;

import com.wdroome.osc.OSCMessage;

public class QLabReply
{
	/**
	 * Status in a QLab response.
	 */
	public static enum Status
	{
		OK, ERROR;
		
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
	public final JSONValue m_rawData;
	
	/** m_rawData cast as a JSON dictionary, or null if m_rawData isn't a dictionary. */
	public final JSONValue_Object m_dataObj;
	
	/** m_rawData cast as a JSON array, or null if m_rawData isn't an array. */
	public final JSONValue_Array m_dataArr;
	
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
		m_rawData = reply.get(QLabUtil.FLD_DATA, null);
		m_dataObj = (m_rawData instanceof JSONValue_Object) ? (JSONValue_Object)m_rawData : null;
		m_dataArr = (m_rawData instanceof JSONValue_Array) ? (JSONValue_Array)m_rawData : null;
	}
	
	/**
	 * Test if the request was successful.
	 * @return True iff the status is "OK".
	 */
	public boolean isOk()
	{
		return m_status == Status.OK;
	}

	@Override
	public String toString() {
		return "QLabReply[workspace=" + m_workspaceId + ", addr=" + m_address + ", status=" + m_status
				+ ", data=" + m_rawData + "]";
	}
}
