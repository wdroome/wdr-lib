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
import com.wdroome.json.validate.JSONValidate_Array;
import com.wdroome.json.validate.JSONValidate_Object;
import com.wdroome.json.validate.JSONValidate_Object.FieldSpec;
import com.wdroome.json.validate.JSONValidate_Object.SimpleKey;
import com.wdroome.util.inet.EndpointAddress;

/**
 * Represent a request message for a Filtered Cost Map request.
 * The message is a JSON dictionary with two elements, "srcs" and "dsts",
 * which are arrays of PID names.
 * @author wdr
 */
public class AltoReq_EndpointPropParams extends AltoMsg_Base
{
	public static final String MEDIA_TYPE = MEDIA_TYPE_PREFIX + "endpointpropparams" + MEDIA_TYPE_SUFFIX;

	private final JSONValue_Array m_properties;
	private final JSONValue_Array m_endpoints;

	private static final String FN_PROPERTIES = "properties";
	private static final String FN_ENDPOINTS = "endpoints";
	
	private static final boolean FIELDS_ARE_REQUIRED = true;

	/**
	 * Create an empty request object.
	 * Used by server to construct a request to send to a server.
	 */
	public AltoReq_EndpointPropParams()
	{
		super();
		m_json.put(FN_PROPERTIES, m_properties = new JSONValue_Array());
		m_json.put(FN_ENDPOINTS, m_endpoints = new JSONValue_Array());
	}
	
	/**
	 * Create an object from a JSON parser.
	 * Used to decode a received message.
	 * @param lexan The JSON input parser.
	 * @throws JSONParseException If jsonSrc isn't a valid JSON string.
	 * @throws JSONValueTypeException If the input isn't a JSON Object dictionary.
	 * @throws JSONFieldMissingException If a required field is missing.
	 */
	public AltoReq_EndpointPropParams(IJSONLexan lexan)
		throws JSONParseException, JSONValueTypeException, JSONFieldMissingException
	{
		super(lexan);
		if (!FIELDS_ARE_REQUIRED) {
			if (!m_json.has(FN_PROPERTIES))
				m_json.put(FN_PROPERTIES, new JSONValue_Array());
			if (!m_json.has(FN_ENDPOINTS))
				m_json.put(FN_ENDPOINTS, new JSONValue_Array());
		}
		m_properties = m_json.getArray(FN_PROPERTIES);
		m_endpoints = m_json.getArray(FN_ENDPOINTS);
	}
	
	/**
	 * Create a request object from a JSON string.
	 * Used by server to decode a request from a client.
	 * @param jsonSrc The encoded JSON request.
	 * @throws JSONParseException If jsonSrc isn't a valid JSON string.
	 * @throws JSONValueTypeException If the input isn't a JSON Object dictionary.
	 * @throws JSONFieldMissingException If a required field is missing.
	 */
	public AltoReq_EndpointPropParams(String jsonSrc)
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
	public AltoReq_EndpointPropParams(Reader jsonSrc)
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

	/**
	 * Add a property name to the request.
	 * @param name The property name. Quietly ignore if null, blank or "-".
	 */
	public void addProperty(String name)
	{
		if (name != null && !name.equals("") && !name.equals("-")) {
			m_properties.add(name);
		}
	}

	/**
	 * Add a set of property names to the request.
	 * @param names An array of property names
	 */
	public void addProperties(String[] names)
	{
		for (String name: names)
			addProperty(name);
	}

	/**
	 * Return the number of property names in the properties list.
	 * @return The number of property names in the properties list.
	 */
	public int getNumProperties()
	{
		return m_properties.size();
	}
	
	/**
	 * Return an array with the property names in the request.
	 * Never returns null; returns a 0-length array if there are no entries.
	 */
	public String[] getProperties()
	{
		return getStringArray(m_properties);
	}

	/**
	 * Add an endpoint IP address to the request.
	 * @param addr The IP address. Quietly ignore if null, blank or "-".
	 */
	public void addEndpoint(String addr)
	{
		if (addr != null && !addr.equals("") && !addr.equals("-")) {
			m_endpoints.add(EndpointAddress.addPrefix(addr));
		}
	}

	/**
	 * Add a set of addresses to the request.
	 * @param addrs An array of IP addresses
	 */
	public void addEndpoints(String[] addrs)
	{
		for (String addr: addrs)
			addEndpoint(addr);
	}

	/**
	 * Return the number of endpoints in the endpoints list.
	 * @return The number of endpoints in the endpoint list.
	 */
	public int getNumEndpoints()
	{
		return m_endpoints.size();
	}
	
	/**
	 * Return an array with the source pids or IP addresses in the request.
	 * Never returns null; returns a 0-length array if there are no entries.
	 */
	public String[] getEndpoints()
	{
		return getStringArray(m_endpoints);
	}
	
	public static String getPropertiesFieldName()
	{
		return JSONValue_Object.makePathName(null, FN_PROPERTIES);
	}
	
	public static String getEndpointsFieldName()
	{
		return JSONValue_Object.makePathName(null, FN_ENDPOINTS);
	}
	
	/** FieldSpecs to validate Endpoint Property Param messages. */
	private static final FieldSpec[] ENDPOINT_PROP_PARAM_FIELD_SPECS = new FieldSpec[] {
		
			// properties:
			new FieldSpec(
				new SimpleKey(FN_PROPERTIES, true),
				AltoValidators.PROPERTY_NAME_ARRAY),
			
			// endpoints:
			new FieldSpec(
				new SimpleKey(FN_ENDPOINTS, true),
				AltoValidators.IP_ADDR_ARRAY),
		};

	/**
	 * Return a new validator for Endpoint Property Param messages.
	 */
	@Override
	protected JSONValidate getValidator()
	{
		return new JSONValidate_Object(ENDPOINT_PROP_PARAM_FIELD_SPECS);
	}
	
	/**
	 * Validate a JSON object as an Endpoint Property Param message.
	 * @param json The JSON Object to validate.
	 * @return A list of errors, or null if the message passed validation.
	 */
	public static List<String> validate(JSONValue_Object json)
	{
		return validate(json, new JSONValidate_Object(ENDPOINT_PROP_PARAM_FIELD_SPECS));
	}
}
