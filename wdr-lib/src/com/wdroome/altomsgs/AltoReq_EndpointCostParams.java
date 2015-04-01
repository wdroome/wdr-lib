package com.wdroome.altomsgs;

import java.io.Reader;
import java.util.List;

import com.wdroome.json.JSONException;
import com.wdroome.json.IJSONLexan;
import com.wdroome.json.JSONFieldMissingException;
import com.wdroome.json.JSONLexan;
import com.wdroome.json.JSONParseException;
import com.wdroome.json.JSONValueTypeException;
import com.wdroome.json.JSONValue_Array;
import com.wdroome.json.JSONValue_Object;
import com.wdroome.json.validate.JSONValidate;
import com.wdroome.json.validate.JSONValidate_Number;
import com.wdroome.json.validate.JSONValidate_Array;
import com.wdroome.json.validate.JSONValidate_Object;
import com.wdroome.json.validate.JSONValidate_Object.FieldSpec;
import com.wdroome.json.validate.JSONValidate_Object.RegexKey;
import com.wdroome.json.validate.JSONValidate_Object.SimpleKey;
import com.wdroome.util.inet.EndpointAddress;

/**
 * Represent a request message for a Filtered Cost Map request.
 * The message is a JSON dictionary with two elements, "srcs" and "dsts",
 * which are arrays of PID names.
 * @author wdr
 */
public class AltoReq_EndpointCostParams extends AltoMsg_Base
{
	public static final String MEDIA_TYPE = MEDIA_TYPE_PREFIX + "endpointcostparams" + MEDIA_TYPE_SUFFIX;

	private final JSONValue_Object m_endpoints;
	private final JSONValue_Array m_srcs;
	private final JSONValue_Array m_dsts;

	private static final String FN_COST_MODE = "cost-mode";
	private static final String FN_COST_METRIC = "cost-metric";
	private static final String FN_COST_TYPE = "cost-type";
	private static final String FN_CONSTRAINTS = "constraints";
	private static final String FN_ENDPOINTS = "endpoints";

	private static final String FN_SRCS = "srcs";
	private static final String FN_DSTS = "dsts";

	/**
	 * Create an empty request object.
	 * Used by server to construct a request to send to a server.
	 */
	public AltoReq_EndpointCostParams()
	{
		super();
		m_json.put(FN_ENDPOINTS, m_endpoints = new JSONValue_Object());
		m_endpoints.put(FN_SRCS, m_srcs = new JSONValue_Array());
		m_endpoints.put(FN_DSTS, m_dsts = new JSONValue_Array());
	}
	
	/**
	 * Create an object from a JSON parser.
	 * Used to decode a received message.
	 * @param lexan The JSON input parser.
	 * @throws JSONParseException If jsonSrc isn't a valid JSON string.
	 * @throws JSONValueTypeException If the input isn't a JSON Object dictionary.
	 * @throws JSONFieldMissingException If a required field is missing.
	 */
	public AltoReq_EndpointCostParams(IJSONLexan lexan)
		throws JSONParseException, JSONValueTypeException, JSONFieldMissingException
	{
		super(lexan);
		if (!m_json.has(FN_ENDPOINTS)) {
			m_json.put(FN_ENDPOINTS, new JSONValue_Object());
		}
		m_endpoints = m_json.getObject(FN_ENDPOINTS);
		if (!m_endpoints.has(FN_SRCS)) {
			m_endpoints.put(FN_SRCS, new JSONValue_Array());
		}
		if (!m_endpoints.has(FN_DSTS)) {
			m_endpoints.put(FN_DSTS, new JSONValue_Array());
		}
		m_srcs = m_endpoints.getArray(FN_SRCS);
		m_dsts = m_endpoints.getArray(FN_DSTS);
	}
	
	/**
	 * Create a request object from a JSON string.
	 * Used by server to decode a request from a client.
	 * @param jsonSrc The encoded JSON request.
	 * @throws JSONParseException If jsonSrc isn't a valid JSON string.
	 * @throws JSONValueTypeException If the input isn't a JSON Object dictionary.
	 * @throws JSONFieldMissingException If a required field is missing.
	 */
	public AltoReq_EndpointCostParams(String jsonSrc)
		throws JSONParseException, JSONValueTypeException, JSONFieldMissingException
	{
		this(new JSONLexan(jsonSrc));
	}
	
	/**
	 * Create a request object from a JSON reader.
	 * Used by server to decode a request from a client.
	 * @param jsonSrc The encoded JSON request.
	 * @throws JSONParseException If jsonSrc isn't a valid JSON string.
	 * @throws JSONValueTypeException If the input isn't a JSON Object dictionary.
	 * @throws JSONFieldMissingException If a required field is missing.
	 */
	public AltoReq_EndpointCostParams(Reader jsonSrc)
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
	 * Add a source endpoint to the request.
	 * @param src The endpoint. Quietly ignore if null, blank or "-".
	 */
	public void addSource(String src)
	{
		if (src != null && !src.equals("") && !src.equals("-")) {
			m_srcs.add(EndpointAddress.addPrefix(src));
		}
	}

	/**
	 * Add a set of source endpoints to the request.
	 * @param srcs An array of endpoints
	 */
	public void addSources(String[] srcs)
	{
		for (String src: srcs) {
			addSource(src);
		}
	}
	
	/**
	 * Return the number of sources in the source list.
	 * @return The number of sources in the source list.
	 */
	public int getNumSources()
	{
		return m_srcs.size();
	}
	
	/**
	 * Return an array with the source endpoints in the request.
	 * Never returns null; returns a 0-length array if there are no entries.
	 */
	public String[] getSources()
	{
		return getStringArray(m_srcs);
	}

	/**
	 * Add a destination endpoint to the request.
	 * @param dest The endpoint. Quietly ignore if null, blank or "-".
	 */
	public void addDestination(String dest)
	{
		if (dest != null && !dest.equals("") && !dest.equals("-")) {
			m_dsts.add(EndpointAddress.addPrefix(dest));
		}
	}

	/**
	 * Add a set of destination endpoints to the request.
	 * @param dests An array of endpoints.
	 */
	public void addDestinations(String[] dests)
	{
		for (String dest: dests) {
			addDestination(dest);
		}
	}

	/**
	 * Return the number of destinations in the destination list.
	 * @return The number of destinations in the destination list.
	 */
	public int getNumDestinations()
	{
		return m_dsts.size();
	}
	
	/**
	 * Return an array with the destination endpoints in the request.
	 * Never returns null; returns a 0-length array if there are no entries.
	 */
	public String[] getDestinations()
	{
		return getStringArray(m_dsts);
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
		return JSONValue_Object.makePathName(FN_ENDPOINTS, FN_SRCS);
	}
	
	public static String getDestinationsFieldName()
	{
		return JSONValue_Object.makePathName(FN_ENDPOINTS, FN_DSTS);
	}
	
	/** FieldSpecs to validate Endpoint Cost Param messages. */
	private static final FieldSpec[] ENDPOINT_COST_PARAM_FIELD_SPECS = new FieldSpec[] {
		
			// cost-type:
			new FieldSpec(
				new SimpleKey(FN_COST_TYPE, true), AltoValidators.COST_TYPE),
			
			// constraints:
			new FieldSpec(
				new SimpleKey(FN_CONSTRAINTS, false), AltoValidators.COST_CONSTRAINT_ARRAY),
		
			// endpoints:
			new FieldSpec(
				new SimpleKey(FN_ENDPOINTS, true),
				new JSONValidate_Object(new FieldSpec[] {
						new FieldSpec(
							new SimpleKey(FN_SRCS, false),
							AltoValidators.IP_ADDR_ARRAY
						),
						new FieldSpec(
							new SimpleKey(FN_DSTS, false),
							AltoValidators.IP_ADDR_ARRAY
						),
				})
			)
		};

	/**
	 * Return a new validator for Endpoint Cost Param messages.
	 */
	@Override
	protected JSONValidate getValidator()
	{
		return new JSONValidate_Object(ENDPOINT_COST_PARAM_FIELD_SPECS);
	}
	
	/**
	 * Validate a JSON object as an Endpoint Cost Param message.
	 * @param json The JSON Object to validate.
	 * @return A list of errors, or null if the message passed validation.
	 */
	public static List<String> validate(JSONValue_Object json)
	{
		return validate(json, new JSONValidate_Object(ENDPOINT_COST_PARAM_FIELD_SPECS));
	}
}
