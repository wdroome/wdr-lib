package com.wdroome.apps.eos2qlab;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import com.wdroome.osc.eos.EOSCueInfo;
import com.wdroome.osc.eos.EOSCueNumber;

import com.wdroome.osc.qlab.QLabUtil;

import com.wdroome.json.JSONValue;
import com.wdroome.json.JSONValueTypeException;
import com.wdroome.json.JSONValue_Object;
import com.wdroome.json.JSONValue_String;
import com.wdroome.json.JSONValue_Array;
import com.wdroome.json.JSONValue_ObjectArray;
import com.wdroome.json.JSONValue_Boolean;
import com.wdroome.json.JSONValue_Number;
import com.wdroome.json.JSONValue_Null;
import com.wdroome.json.JSONParser;

import com.wdroome.json.JSONLexan;
import com.wdroome.json.JSONParseException;

public class Config
{
	public static final String CONFIG_FILE_RESOURCE_NAME = "default-config.json";

	public static final String FLD_QLabAddrPort = "QLabAddrPort";
	public static final String FLD_EOSAddrPort = "EOSAddrPort";
	public static final String FLD_QLabNetworkPatch = "QLabNetworkPatch";
	public static final String FLD_QLabDefaultCueList = "QLabDefaultCueList";
	public static final String FLD_CueColor = "CueColor";
	public static final String FLD_CueFlagged = "CueFlagged";
	public static final String FLD_CueNumberFmt = "CueNumberFmt";
	public static final String FLD_CueNameFmt = "CueNameFmt";
	public static final String FLD_CueNameSceneSuffixFmt = "CueNameSceneSuffixFmt";
	public static final String FLD_CueNameSceneEndSuffix = "CueNameSceneEndSuffix";
	public static final String FLD_CueLabelPrefix = "CueLabelPrefix";

	public String[] m_QLabAddrPorts = {"127.0.0.1:53000"};
	public String[] m_EOSAddrPorts = {"192.168.0.113:8192", "127.0.0.1:8192"};
	
	public int m_newCueNetworkPatch = 1;
	public String m_defaultQLabCuelist = QLabUtil.DEFAULT_CUELIST_NAME;
	public String[] m_newCueNumberFmt = {"q%number%", "q%list%-%number%"};
	public String[] m_newCueNameFmt = {"Light cue %number%",
										"Light cue %list%/%number%"};
	public String m_newCueNameSceneSuffixFmt = " ***** Scene %scene%";
	public String m_newCueNameSceneEndSuffix = " ***** Scene End";
	public QLabUtil.ColorName m_newCueColor = QLabUtil.ColorName.RED;
	public String m_newCueLabelPrefix = "";
	public boolean m_newCueFlag = true;
	
	private boolean m_isSingleEOSCuelist = false;
	
	public Config(String[] args)
	{
		InputStream configInput;
		int iStart = 0;
		if (args.length >= 1 && args[0].endsWith(".json")) {
			try {
				configInput = new FileInputStream(args[0]);
			} catch (FileNotFoundException e) {
				throw new IllegalArgumentException("Cannot open EOS2QLab config file '" + args[0]
														+ "': " + e);
			}
			iStart = 1;
		} else {
			configInput = getClass().getResourceAsStream(CONFIG_FILE_RESOURCE_NAME);
		}
		JSONValue_Object jsonConfig;
		if (configInput != null) {
			try {
				jsonConfig = JSONParser.parseObject(new JSONLexan(configInput), true);
			} catch (Exception e) {
				throw new IllegalArgumentException("JSON error in EOS2QLab config file: " + e);
			}
		} else {
			jsonConfig = new JSONValue_Object();
		}
		for (int iArg = iStart; iArg < args.length; iArg++) {
			int iSep = args[iArg].indexOf('=');
			if (iSep < 1) {
				throw new IllegalArgumentException("Invalid override '" + args[iArg]
										+ "'; must be var=value");
			}
			jsonConfig.put(args[iArg].substring(0,iSep), args[iArg].substring(iSep+1));
		}
		for (Map.Entry<String, JSONValue> ent: jsonConfig.entrySet()) {
			String name = ent.getKey().trim();
			JSONValue value = ent.getValue();
			if (name.startsWith("#")) {
				continue;
			}
			if (name.equals(FLD_QLabAddrPort)) {
				m_QLabAddrPorts = jsonToStringArr(value, name);
			} else if (name.equals(FLD_EOSAddrPort)) {
				m_EOSAddrPorts = jsonToStringArr(value, name);
			} else if (name.equals(FLD_QLabNetworkPatch)) {
				m_newCueNetworkPatch = jsonToInt(value, name);
			} else if (name.equals(FLD_QLabDefaultCueList)) {
				m_defaultQLabCuelist = jsonToString(value, name);
			} else if (name.equals(FLD_CueNumberFmt)) {
				m_newCueNumberFmt = jsonToStringArr(value, name);
			} else if (name.equals(FLD_CueNameFmt)) {
				m_newCueNameFmt = jsonToStringArr(value, name);
			} else if (name.equals(FLD_CueNameSceneSuffixFmt)) {
				m_newCueNameSceneSuffixFmt = jsonToString(value, name);
			} else if (name.equals(FLD_CueNameSceneEndSuffix)) {
				m_newCueNameSceneEndSuffix = jsonToString(value, name);
			} else if (name.equals(FLD_CueLabelPrefix)) {
				m_newCueLabelPrefix = jsonToString(value, name);
			} else if (name.equals(FLD_CueColor)) {
				m_newCueColor = QLabUtil.ColorName.fromQLab(jsonToString(value, name).trim());
			} else if (name.equals(FLD_CueFlagged)) {
				m_newCueFlag = jsonToBoolean(value, name);
			} else {
				throw new IllegalArgumentException("Error in config file or overrides:"
						+ " unknown field '" + name + "'");
			}
		}
	}
	
	private String[] jsonToStringArr(JSONValue val, String key)
	{
		if (val instanceof JSONValue_String) {
			return new String[] {((JSONValue_String)val).m_value};
		} else if (val instanceof JSONValue_Array) {
			JSONValue_Array jsonArr = (JSONValue_Array)val;
			ArrayList<String> strArr = new ArrayList<>();
			for (JSONValue arrVal: jsonArr) {
				if (arrVal instanceof JSONValue_String) {
					strArr.add(((JSONValue_String)arrVal).m_value);
				} else {
					throw new IllegalArgumentException("Error in config file: " + key
													+ " isn't a string or a string array.");
				}
			}
			if (jsonArr.size() == 0) {
				return new String[] {""};
			} else {
				return strArr.toArray(new String[jsonArr.size()]);
			}
		} else if (val instanceof JSONValue_Null) {
			return new String[] {""};
		} else {
			throw new IllegalArgumentException("Error in config file: " + key
					+ " isn't a string or a string array.");			
		}
	}
	
	private int jsonToInt(JSONValue val, String key)
	{
		if (val instanceof JSONValue_Number) {
			return (int)((JSONValue_Number)val).m_value;
		} else if (val instanceof JSONValue_String) {
			try {
				return Integer.parseInt(((JSONValue_String)val).m_value.trim());
			} catch (Exception e) {
				throw new IllegalArgumentException("Error in config file: " + key
						+ " isn't a number.");			
			}
		} else {
			throw new IllegalArgumentException("Error in config file: " + key
					+ " isn't a number.");			
		}
	}
	
	private String jsonToString(JSONValue val, String key)
	{
		if (val instanceof JSONValue_String) {
			return ((JSONValue_String)val).m_value;
		} else if (val instanceof JSONValue_Null) {
			return "";
		} else {
			throw new IllegalArgumentException("Error in config file: " + key
					+ " isn't a string.");			
		}
	}
	
	private boolean jsonToBoolean(JSONValue val, String key)
	{
		if (val instanceof JSONValue_Boolean) {
			return ((JSONValue_Boolean)val).m_value;
		} else if (val instanceof JSONValue_String) {
			String v = ((JSONValue_String)val).m_value.trim().toLowerCase();
			return v.startsWith("y") || v.startsWith("t");
		} else if (val instanceof JSONValue_Number) {
			return ((JSONValue_Number)val).m_value != 0;
		} else {
			throw new IllegalArgumentException("Error in config file: " + key
								+ " isn't true or false.");			
		}
	}
	
	public String replaceVars(String fmt, EOSCueInfo cue)
	{
		return fmt
				.replace("%cuelist%", "" + cue.getCueNumber().getCuelist())
				.replace("%list%", "" + cue.getCueNumber().getCuelist())
				.replace("%cuenumber%", cue.getCueNumber().getCueNumber())
				.replace("%number%", cue.getCueNumber().getCueNumber())
				.replace("%scene%", cue.getScene())
				;
	}
	
	public void setSingleEOSCuelist(boolean isSingleEOSCuelist)
	{
		m_isSingleEOSCuelist = isSingleEOSCuelist;
		EOSCueNumber.setShowDefaultCuelist(!isSingleEOSCuelist);
	}
	
	public String makeNewCueNumber(EOSCueInfo cue)
	{
		return replaceVars(m_newCueNumberFmt[m_isSingleEOSCuelist ? 0 : 1], cue);
	}
	
	public String makeNewCueName(EOSCueInfo cue)
	{
		String name;
		String label = cue.getLabel();
		if (label != null && !label.isBlank()) {
			name = (m_newCueLabelPrefix != null ? m_newCueLabelPrefix : "") + label;
		} else {
			name = replaceVars(m_newCueNameFmt[m_isSingleEOSCuelist ? 0 : 1], cue);
		}
		String scene = cue.getScene();
		if (scene != null && !scene.isBlank()
				&& m_newCueNameSceneSuffixFmt != null
				&& !m_newCueNameSceneSuffixFmt.isBlank()) {
			name += replaceVars(m_newCueNameSceneSuffixFmt, cue);
		} else if (cue.isSceneEnd()
				&& m_newCueNameSceneEndSuffix != null
				&& !m_newCueNameSceneEndSuffix.isBlank()) {
			name += m_newCueNameSceneEndSuffix;
		}
		return name;
	}
	
	public static void main(String[] args) throws JSONParseException, JSONValueTypeException
	{
		Config config = new Config(args);
	}
}
