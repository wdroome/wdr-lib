package com.wdroome.apps.eos2qlab;

import java.io.InputStream;
import java.io.PrintStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
import com.wdroome.json.JSONWriter;
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
	public static final String FLD_CueNumber = "CueNumber";
	public static final String FLD_CueNameNoLabel = "CueNameNoLabel";
	public static final String FLD_CueNameFromLabel = "CueNameFromLabel";
	public static final String FLD_ConnectTimeoutMS = "ConnectTimeoutMS";

	private String[] m_QLabAddrPorts = {"127.0.0.1:53000"};
	private String[] m_EOSAddrPorts = {"192.168.0.113:8192", "127.0.0.1:8192"};
	
	private int m_newCueNetworkPatch = 1;
	private String m_defaultQLabCuelist = QLabUtil.DEFAULT_CUELIST_NAME;
	private String m_newCueNumber = "q%{list}-%%number%";
	private String m_newCueNameNoLabel =
						"Light cue %{list}/%%number%% ***** scene-end{sceneend}%% ***** {scene}%";
	private String m_newCueNameFromLabel =
						"Cue %{list}/%%number%: %label%% ***** scene-end{sceneend}%% ***** {scene}%";
	private QLabUtil.ColorName m_newCueColor = QLabUtil.ColorName.RED;
	private boolean m_newCueFlag = true;
	private int m_connectTimeoutMS = 2000;
	
	private static final String VAR_BASE_PATTERN = "%([^%]*)%";
	private static final String VAR_FULL_PATTERN = "^(.*)\\{([A-Za-z0-9]+)\\}(.*)$";
	
	private boolean m_isSingleEOSCuelist = false;
	private JSONValue_Object m_jsonConfig;
	
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
		if (configInput != null) {
			try {
				m_jsonConfig = JSONParser.parseObject(new JSONLexan(configInput), true);
			} catch (Exception e) {
				throw new IllegalArgumentException("JSON error in EOS2QLab config file: " + e);
			}
		} else {
			m_jsonConfig = new JSONValue_Object();
		}
		for (int iArg = iStart; iArg < args.length; iArg++) {
			int iSep = args[iArg].indexOf('=');
			if (iSep < 1) {
				throw new IllegalArgumentException("Invalid override '" + args[iArg]
										+ "'; must be var=value");
			}
			m_jsonConfig.put(args[iArg].substring(0,iSep), args[iArg].substring(iSep+1));
		}
		for (Map.Entry<String, JSONValue> ent: m_jsonConfig.entrySet()) {
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
				m_newCueNetworkPatch = jsonToInt(value, name, 1, 16);
			} else if (name.equals(FLD_QLabDefaultCueList)) {
				m_defaultQLabCuelist = jsonToString(value, name);
			} else if (name.equals(FLD_CueNumber)) {
				m_newCueNumber = jsonToString(value, name);
			} else if (name.equals(FLD_CueNameNoLabel)) {
				m_newCueNameNoLabel = jsonToString(value, name);
			} else if (name.equals(FLD_CueNameFromLabel)) {
				m_newCueNameFromLabel = jsonToString(value, name);
			} else if (name.equals(FLD_CueColor)) {
				m_newCueColor = QLabUtil.ColorName.fromQLab(jsonToString(value, name).trim());
			} else if (name.equals(FLD_CueFlagged)) {
				m_newCueFlag = jsonToBoolean(value, name);
			} else if (name.equals(FLD_ConnectTimeoutMS)) {
				m_connectTimeoutMS = jsonToInt(value, name, 25, 30000);
			} else {
				throw new IllegalArgumentException("Error in config file or overrides:"
						+ " unknown field '" + name + "'");
			}
		}
	}
	
	public String[] getQLabAddrPorts() {
		return m_QLabAddrPorts;
	}

	public String[] getEOSAddrPorts() {
		return m_EOSAddrPorts;
	}



	public int getNewCueNetworkPatch() {
		return m_newCueNetworkPatch;
	}

	public String getDefaultQLabCuelist() {
		return m_defaultQLabCuelist;
	}

	public QLabUtil.ColorName getNewCueColor() {
		return m_newCueColor;
	}

	public boolean getNewCueFlag() {
		return m_newCueFlag;
	}

	public int getConnectTimeoutMS() {
		return m_connectTimeoutMS;
	}

	public boolean isSingleEOSCuelist() {
		return m_isSingleEOSCuelist;
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
	
	private int jsonToInt(JSONValue val, String key, int min, int max)
	{
		int intVal;
		if (val instanceof JSONValue_Number) {
			intVal = (int)((JSONValue_Number)val).m_value;
		} else if (val instanceof JSONValue_String) {
			try {
				intVal = Integer.parseInt(((JSONValue_String)val).m_value.trim());
			} catch (Exception e) {
				throw new IllegalArgumentException("Error in config file: " + key
						+ " isn't a number.");			
			}
		} else {
			throw new IllegalArgumentException("Error in config file: " + key
					+ " isn't a number.");			
		}
		if (!(intVal >= min && intVal <= max)) {
			throw new IllegalArgumentException("Error in config file: " + key
					+ " must be between " + min + " and " + max + ".");						
		}
		return intVal;
	}
	
	private long jsonToLong(JSONValue val, String key)
	{
		if (val instanceof JSONValue_Number) {
			return (long)((JSONValue_Number)val).m_value;
		} else if (val instanceof JSONValue_String) {
			try {
				return Long.parseLong(((JSONValue_String)val).m_value.trim());
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
	
	public String replaceVars(String src, EOSCueInfo cue)
	{
		Pattern basePat = Pattern.compile(VAR_BASE_PATTERN);
		Pattern fullPat = Pattern.compile(VAR_FULL_PATTERN);
		Matcher baseMatcher = basePat.matcher(src);
		StringBuffer result = new StringBuffer();
		int start = 0;
		while (baseMatcher.find() && baseMatcher.groupCount() == 1) {
			int findStart = baseMatcher.start();
			if (findStart > start) {
				result.append(src.substring(start, findStart));
			}
			start = baseMatcher.end();
			String fullVar = baseMatcher.group(1);
			Matcher varMatcher = fullPat.matcher(fullVar);
			if (varMatcher.find() && varMatcher.groupCount() == 3) {
				GetVarResult varInfo = getVar(varMatcher.group(2), cue);
				if (varInfo == null) {
					result.append(baseMatcher.group(0));
				} else if (varInfo.m_needed) {
					result.append(varMatcher.group(1));
					result.append(varInfo.m_value);
					result.append(varMatcher.group(3));
				}
			} else {
				String var = baseMatcher.group(1);
				if (var.isBlank()) {
					result.append("%");
				} else {
					GetVarResult varInfo = getVar(baseMatcher.group(1), cue);
					if (varInfo == null) {
						result.append(baseMatcher.group(0));
					} else {
						result.append(varInfo.m_value);
					}
				}
			}
		}
		result.append(src.substring(start));
		return result.toString();
	}
	
	private static class GetVarResult
	{
		private final String m_value;
		private final boolean m_needed;
		
		private GetVarResult(String value)
		{
			m_value = value;
			m_needed = m_value != null && !m_value.isBlank();
		}
		
		private GetVarResult(String value, boolean needed)
		{
			m_value = value;
			m_needed = needed;
		}
	}
	
	private GetVarResult getVar(String var, EOSCueInfo cue)
	{
		if (var.equalsIgnoreCase("list")) {
			return new GetVarResult(cue.getCueNumber().getCuelist() + "", !m_isSingleEOSCuelist);
		} else if (var.equalsIgnoreCase("number")) {
			return new GetVarResult(cue.getCueNumber().getCueNumber());
		} else if (var.equalsIgnoreCase("label")) {
			return new GetVarResult(cue.getLabel());
		} else if (var.equalsIgnoreCase("scene")) {
			return new GetVarResult(cue.getScene());
		} else if (var.equalsIgnoreCase("sceneend")) {
			return new GetVarResult("", cue.isSceneEnd());
		} else {
			return null;
		}
	}
	
	public void setSingleEOSCuelist(boolean isSingleEOSCuelist)
	{
		m_isSingleEOSCuelist = isSingleEOSCuelist;
		EOSCueNumber.setShowDefaultCuelist(!isSingleEOSCuelist);
	}
	
	public String makeNewCueNumber(EOSCueInfo cue)
	{
		return replaceVars(m_newCueNumber, cue);
	}
	
	public String makeNewCueName(EOSCueInfo cue)
	{
		String label = cue.getLabel();
		String nameFmt = (label != null && !label.isBlank()) ?
							m_newCueNameFromLabel : m_newCueNameNoLabel; 
		return replaceVars(nameFmt, cue);
	}
	
	/**
	 * Print the json config file.
	 * @param out
	 * @throws IOException
	 */
	public void prtConfigFile(PrintStream out, String lineIndent) throws IOException
	{
		if (lineIndent == null) {
			lineIndent = "   ";
		}
		JSONWriter writer = new JSONWriter(out);
		writer.setSorted(true);
		writer.setIndents(lineIndent, "   ");
		m_jsonConfig.writeJSON(writer);
		writer.writeNewline();		
	}
	
	public static void main(String[] args) throws JSONParseException, JSONValueTypeException
	{
		Config config = new Config(args);
	}
}
