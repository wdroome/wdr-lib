package com.wdroome.altomsgs;

import java.io.Reader;

import com.wdroome.json.JSONException;
import com.wdroome.json.IJSONLexan;
import com.wdroome.json.JSONFieldMissingException;
import com.wdroome.json.JSONLexan;
import com.wdroome.json.JSONParseException;
import com.wdroome.json.JSONValueTypeException;
import com.wdroome.json.JSONValue_Array;
import com.wdroome.json.JSONValue_Object;
import com.wdroome.json.validate.JSONValidate;
import com.wdroome.json.validate.JSONValidate_Array;
import com.wdroome.json.validate.JSONValidate_Object;
import com.wdroome.json.validate.JSONValidate_Object.FieldSpec;
import com.wdroome.json.validate.JSONValidate_Object.SimpleKey;
import com.wdroome.util.inet.EndpointAddress;

import java.net.InetAddress;
import java.util.List;

/**
 * Represent a request message for a Filtered Cost Map request.
 * The message is a JSON dictionary with two elements, "srcs" and "dsts",
 * which are arrays of PID names.
 * @author wdr
 */
public class AltoReq_FilteredCostMap extends AltoMsg_Base
{
	public static final String MEDIA_TYPE = MEDIA_TYPE_PREFIX + "costmapfilter" + MEDIA_TYPE_SUFFIX;

	private final JSONValue_Object m_pids;
	private final JSONValue_Array m_srcs;
	private final JSONValue_Array m_dsts;

	private static final String FN_COST_MODE = "cost-mode";
	private static final String FN_COST_METRIC = "cost-metric";
	private static final String FN_COST_TYPE = "cost-type";
	private static final String FN_CONSTRAINTS = "constraints";
	private static final String FN_PIDS = "pids";

	private static final String FN_SRCS = "srcs";
	private static final String FN_DSTS = "dsts";

	/**
	 * Create an empty request object.
	 * Used by server to construct a request to send to a server.
	 */
	public AltoReq_FilteredCostMap()
	{
		super();
		m_json.put(FN_PIDS, m_pids = new JSONValue_Object());
		m_pids.put(FN_SRCS, m_srcs = new JSONValue_Array());
		m_pids.put(FN_DSTS, m_dsts = new JSONValue_Array());
	}
	
	/**
	 * Create an object from a JSON parser.
	 * Used to decode a received message.
	 * @param lexan The JSON input parser.
	 * @throws JSONParseException If jsonSrc isn't a valid JSON string.
	 * @throws JSONValueTypeException If the input isn't a JSON Object dictionary.
	 * @throws JSONFieldMissingException If a required field is missing.
	 */
	public AltoReq_FilteredCostMap(IJSONLexan lexan)
		throws JSONParseException, JSONValueTypeException, JSONFieldMissingException
	{
		super(lexan);
		if (!m_json.has(FN_PIDS)) {
			m_json.put(FN_PIDS, new JSONValue_Object());
		}
		m_pids = m_json.getObject(FN_PIDS);
		if (!m_pids.has(FN_SRCS)) {
			m_pids.put(FN_SRCS, new JSONValue_Array());
		}
		if (!m_pids.has(FN_DSTS)) {
			m_pids.put(FN_DSTS, new JSONValue_Array());
		}
		m_srcs = m_pids.getArray(FN_SRCS);
		m_dsts = m_pids.getArray(FN_DSTS);
	}

	/**
	 * Create a request object from a JSON string.
	 * Used by server to decode a request from a client.
	 * @param jsonSrc The encoded JSON request.
	 * @throws JSONParseException If jsonSrc isn't a valid JSON string.
	 * @throws JSONValueTypeException If the input isn't a JSON Object dictionary.
	 * @throws JSONFieldMissingException If a required field is missing.
	 */
	public AltoReq_FilteredCostMap(String jsonSrc)
		throws JSONParseException, JSONValueTypeException, JSONFieldMissingException
	{
		this(new JSONLexan(jsonSrc));
	}
	
	/**
	 * Create a request object from a JSON Reader.
	 * Used by server to decode a request from a client.
	 * @param jsonSrc The encoded JSON request.
	 * @throws JSONParseException If jsonSrc isn't a valid JSON string.
	 * @throws JSONValueTypeException If the input isn't a JSON Object dictionary.
	 * @throws JSONFieldMissingException If a required field is missing.
	 */
	public AltoReq_FilteredCostMap(Reader jsonSrc)
		throws JSONParseException, JSONValueTypeException, JSONFieldMissingException
	{
		this(new JSONLexan(jsonSrc));
	}
	
	@Override
	public String getMediaType()
	{
		return MEDIA_TYPE;
	}

	@Override
	protected boolean needPathNames()
	{
		return true;
	}
	
	public String getCostMetric()
	{
		JSONValue_Object costId = m_json.getObject(FN_COST_TYPE, null);
		if (costId != null) {
			return costId.getString(FN_COST_METRIC, null);
		} else {
			return null;
		}
	}
	
	public void setCostMetric(String value)
	{
		JSONValue_Object costId = m_json.getObject(FN_COST_TYPE, null);
		if (costId == null) {
			costId = new JSONValue_Object();
			m_json.put(FN_COST_TYPE, costId);
		}
		costId.put(FN_COST_METRIC, value);
	}
	
	public String getCostMode()
	{
		JSONValue_Object costId = m_json.getObject(FN_COST_TYPE, null);
		if (costId != null) {
			return costId.getString(FN_COST_MODE, null);
		} else {
			return null;
		}
	}
	
	public void setCostMode(String value)
	{
		JSONValue_Object costId = m_json.getObject(FN_COST_TYPE, null);
		if (costId == null) {
			costId = new JSONValue_Object();
			m_json.put(FN_COST_TYPE, costId);
		}
		costId.put(FN_COST_MODE, value);
	}
	
	public String[] getConstraints()
	{
		return getStringArray(m_json.getArray(FN_CONSTRAINTS, null));
	}
	
	public void setConstraints(String[] value)
	{
		m_json.put(FN_CONSTRAINTS, new JSONValue_Array(value));
	}

	/**
	 * Add a source pid to the request.
	 * @param src The pid. Quietly ignore if null, blank or "-".
	 */
	public void addSource(String src)
	{
		if (src != null && !src.equals("") && !src.equals("-")) {
			m_srcs.add(src);
		}
	}

	/**
	 * Add a set of source pids to the request.
	 * @param srcs An array of pids
	 */
	public void addSources(String[] srcs)
	{
		for (String src: srcs) {
			addSource(src);
		}
	}
	
	/**
	 * Return an array with the source pids in the request.
	 * Never returns null; returns a 0-length array if there are no entries.
	 */
	public String[] getSources()
	{
		return getStringArray(m_srcs);
	}
	
	/**
	 * Return the number of source pids in the request.
	 * @return The number of source pids in the request.
	 */
	public int getNumSources()
	{
		return m_srcs.size();
	}

	/**
	 * Add a destination pid to the request.
	 * @param dest The pid. Quietly ignore if null, blank or "-".
	 */
	public void addDestination(String dest)
	{
		if (dest != null && !dest.equals("") && !dest.equals("-")) {
			m_dsts.add(dest);
		}
	}

	/**
	 * Add a set of destination pids to the request.
	 * @param dests An array of pids.
	 */
	public void addDestinations(String[] dests)
	{
		for (String dest: dests) {
			addDestination(dest);
		}
	}
	
	/**
	 * Return an array with the destination pids in the request.
	 * Never returns null; returns a 0-length array if there are no entries.
	 */
	public String[] getDestinations()
	{
		return getStringArray(m_dsts);
	}
	
	/**
	 * Return the number of destination pids in the request.
	 * @return The number of destination pids in the request.
	 */
	public int getNumDestinations()
	{
		return m_dsts.size();
	}
	
	public static String getCostMetricFieldName()
	{
		return JSONValue_Object.makePathName(FN_COST_TYPE, FN_COST_METRIC);
	}
	
	public static String getCostModeFieldName()
	{
		return JSONValue_Object.makePathName(FN_COST_TYPE, FN_COST_MODE);
	}
	
	public static String getConstraintsFieldName()
	{
		return JSONValue_Object.makePathName(null, FN_CONSTRAINTS);
	}
	
	public static String getSourcesFieldName()
	{
		return JSONValue_Object.makePathName(FN_PIDS, FN_SRCS);
	}
	
	public static String getDestinationsFieldName()
	{
		return JSONValue_Object.makePathName(FN_PIDS, FN_DSTS);
	}
	
	/** FieldSpecs to validate Filtered Cost messages. */
	private static final FieldSpec[] FILTERED_COST_PARAM_FIELD_SPECS = new FieldSpec[] {
		
			// cost-type:
			new FieldSpec(
				new SimpleKey(FN_COST_TYPE, true), AltoValidators.COST_TYPE),
			
			// constraints:
			new FieldSpec(
				new SimpleKey(FN_CONSTRAINTS, false), AltoValidators.COST_CONSTRAINT_ARRAY),
		
			// endpoints:
			new FieldSpec(
				new SimpleKey(FN_PIDS, true),
				new JSONValidate_Object(new FieldSpec[] {
						new FieldSpec(
							new SimpleKey(FN_SRCS, true),
							AltoValidators.PID_NAME_ARRAY
						),
						new FieldSpec(
							new SimpleKey(FN_DSTS, true),
							AltoValidators.PID_NAME_ARRAY
						),
				})
			)
		};

	/**
	 * Return a new validator for Filtered Cost Param messages.
	 */
	@Override
	protected JSONValidate getValidator()
	{
		return new JSONValidate_Object(FILTERED_COST_PARAM_FIELD_SPECS);
	}
	
	/**
	 * Validate a JSON object as a Filtered Cost Param message.
	 * @param json The JSON Object to validate.
	 * @return A list of errors, or null if the message passed validation.
	 */
	public static List<String> validate(JSONValue_Object json)
	{
		return validate(json, new JSONValidate_Object(FILTERED_COST_PARAM_FIELD_SPECS));
	}
}
