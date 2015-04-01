package com.wdroome.altomsgs;

import java.util.Iterator;
import java.util.List;
import java.io.Reader;

import com.wdroome.json.JSONException;
import com.wdroome.util.inet.EndpointAddress;
import com.wdroome.util.String2;
import com.wdroome.json.IJSONLexan;
import com.wdroome.json.JSONLexan;
import com.wdroome.json.JSONParseException;
import com.wdroome.json.JSONValueTypeException;
import com.wdroome.json.JSONValue_Array;
import com.wdroome.json.JSONValue_Object;
import com.wdroome.json.validate.JSONValidate;
import com.wdroome.json.validate.JSONValidate_Invalid;
import com.wdroome.json.validate.JSONValidate_Number;
import com.wdroome.json.validate.JSONValidate_String;
import com.wdroome.json.validate.JSONValidate_Object;
import com.wdroome.json.validate.JSONValidate_Object.FieldSpec;
import com.wdroome.json.validate.JSONValidate_Object.RegexKey;
import com.wdroome.json.validate.JSONValidate_Object.SimpleKey;

/**
 * A class representing an ALTO Endpoint-Property response message.
 * @author wdr
 */
public class AltoResp_EndpointProp extends AltoResp_Base implements Iterable<String>
{
	public static final String MEDIA_TYPE = MEDIA_TYPE_PREFIX + "endpointprop" + MEDIA_TYPE_SUFFIX;

	public static final String CAPABILITY_PROP_TYPES	= "prop-types";
	
	public static final String PROPERTY_TYPE_PID		= "pid";

	private static final String FN_ENDPOINT_PROPERTIES = "endpoint-properties";
	
	/**
	 * True iff the "pid" property name MUST be qualified with a network map id. 
	 */
	public static final boolean MUST_QUALIFY_PID_PROPERTY = true;

	private boolean m_addrPrefixRequired = true;
	
	/**
	 * Return the name of the ALTO service that returns response messages
	 * of this type.
	 * @param accepts If null, return name of GET-mode resource.
	 * 		If not null, return name of POST-mode service that accepts this type.
	 * @return The name of the ALTO service, or null if this service
	 * 		does not accept POST-mode requests of that type.
	 */
	public static String getServiceName(String accepts)
	{
		if (accepts != null && accepts.equals(AltoReq_EndpointPropParams.MEDIA_TYPE)) {
			return "Endpoint Property Service";
		} else {
			return null;
		}
	}

	/**
	 * Create an empty response object.
	 * Used by server to construct a response to send to a client.
	 */
	public AltoResp_EndpointProp()
	{
		super();
	}
	
	/**
	 * Create an object from a JSON parser.
	 * Used to decode a received message.
	 * @param lexan The JSON input parser.
	 * @throws JSONParseException If jsonSrc isn't a valid JSON string.
	 * @throws JSONValueTypeException If the input isn't a JSON Object dictionary.
	 */
	public AltoResp_EndpointProp(IJSONLexan lexan)
		throws JSONParseException, JSONValueTypeException
	{
		super(lexan);
	}
	
	/**
	 * Create a response object from a JSON string.
	 * Used by client to decode a response from a server.
	 * @param jsonSrc The encoded JSON response.
	 * @throws JSONParseException If jsonSrc isn't a valid JSON string.
	 * @throws JSONValueTypeException If the input isn't a JSON Object dictionary.
	 */
	public AltoResp_EndpointProp(String jsonSrc)
		throws JSONParseException, JSONValueTypeException
	{
		this(new JSONLexan(jsonSrc));
	}
	
	/**
	 * Create a response object from a JSON Reader.
	 * Used by client to decode a response from a server.
	 * @param jsonSrc The encoded JSON response.
	 * @throws JSONParseException If jsonSrc isn't a valid JSON string.
	 * @throws JSONValueTypeException If the input isn't a JSON Object dictionary.
	 */
	public AltoResp_EndpointProp(Reader jsonSrc)
		throws JSONParseException, JSONValueTypeException
	{
		this(new JSONLexan(jsonSrc));
	}
	
	@Override
	public String getMediaType()
	{
		return MEDIA_TYPE;
	}
	
	@Override
	public String[] getMapNames()
	{
		return new String[] {FN_ENDPOINT_PROPERTIES};
	}
	
	/**
	 * Set whether or not an ipv4/ipv6 address prefix is required.
	 * If required, the "get" and "set" methods will add a prefix if needed.
	 * If not required, they won't.
	 * Default is "required."
	 * @param addrPrefixRequired True if prefixes are required.
	 */
	public void setAddrPrefixRequired(boolean addrPrefixRequired)
	{
		m_addrPrefixRequired = addrPrefixRequired;
	}
	
	/**
	 * Return true iff address prefixes are required.
	 * If required, the "get" and "set" methods will add them.
	 * @return True iff address prefixes are required.
	 */
	public boolean isAddrPrefixRequired()
	{
		return m_addrPrefixRequired;
	}
	
	/**
	 * Return an iterator over the endpoint IP addrs.
	 */
	public Iterator<String> iterator()
	{
		return m_map.keys();
	}
	
	/**
	 * Return an array with the endpoint IP addrs.
	 * Not sorted.
	 * @return An array with the endpoint IP addrs.
	 */
	public String[] getEndpointAddrs()
	{
		return m_map.keyArray();
	}
	
	/**
	 * Return the property names for an IP addr.
	 * @param addr The IP addr.
	 * @return An (unsorted) array with the property names for "src."
	 * @throws JSONException If "src" doesn't exist.
	 */
	public String[] getPropertyNames(String addr) throws JSONException
	{
		if (m_addrPrefixRequired)
			addr = EndpointAddress.addPrefix(addr);
		return m_map.getObject(addr).keyArray();
	}
	
	/**
	 * Return the value of a property for an endpoint.
	 * @param addr The endpoint IP addr.
	 * @param prop The property name.
	 * @param def The value to return if src exists, but does not have this property.
	 * @return The property value.
	 * @throws JSONException If src isn't in the map.
	 */
	public String getProperty(String addr, String prop, String def) throws JSONException
	{
		if (m_addrPrefixRequired)
			addr = EndpointAddress.addPrefix(addr);
		return m_map.getObject(addr).getString(prop, def);
	}

	/**
	 * Set the value of a property for an endpoint.
	 * @param addr The endpoint IP addr.
	 * @param prop The property name.
	 * @param value The property value.
	 */
	public void setProperty(String addr, String prop, String value)
	{
		if (m_addrPrefixRequired)
			addr = EndpointAddress.addPrefix(addr);
		JSONValue_Object endpointMap = m_map.getObject(addr, null);
		if (endpointMap == null) {
			endpointMap = new JSONValue_Object();
			m_map.put(addr, endpointMap);
		}
		endpointMap.put(prop, value);
	}
	
	/**
	 * Split a property name of the form "map-id.name"
	 * into its two components.
	 * @param fullName A property name of the form "name" or "map-id.name".
	 * @return If fullName is of the form "map-id.name", a String pair <map-id,name>.
	 * 		If fullName is a simple, unqualified property name, return null.
	 */
	public static String2 splitPropName(String fullName)
	{
		int iPeriod = fullName.lastIndexOf('.');
		if (iPeriod <= 0 || iPeriod >= fullName.length()-1)
			return null;
		return new String2(fullName.substring(0, iPeriod), fullName.substring(iPeriod+1));
	}
	
	/**
	 * Create a fully-qualified property name of the form "map-id.prop-name".
	 * @param mapId A network map resource id. May be null.
	 * @param leaf The property's leaf name.
	 * @return mapId.leaf, or just leaf if mapId is null.
	 */
	public static String makePropName(String mapId, String leaf)
	{
		if (mapId != null && !mapId.equals(""))
			return mapId + "." + leaf;
		else
			return leaf;
	}
	
	/** FieldSpecs to validate Endpoint Property Map messages. */
	private static final FieldSpec[] ENDPOINT_PROP_MAP_FIELD_SPECS = new FieldSpec[] {
		
			// meta:
			AltoValidators.META_VTAGS_OPTIONAL,
		
			// endpoint-properties:
			new FieldSpec(
				new SimpleKey(FN_ENDPOINT_PROPERTIES, true),
				new JSONValidate_Object(new FieldSpec[] {
					new FieldSpec(
						AltoValidators.VALID_IP_ADDR_KEY,
						new JSONValidate_Object(new FieldSpec[] {
							new FieldSpec(
								new RegexKey(AltoValidators.PROPERTY_NAME_PAT),
								JSONValidate_String.STRING),
							new FieldSpec(
								new RegexKey(".*"),
								new JSONValidate_Invalid("Invalid Property Name")
							),
						})
					),
					new FieldSpec(
						new RegexKey(".*"),
						new JSONValidate_Invalid("Invalid IP Address")),
				})
			)
		};

	/**
	 * Return a new validator for Endpoint Property Map messages.
	 */
	@Override
	protected JSONValidate getValidator()
	{
		return new JSONValidate_Object(ENDPOINT_PROP_MAP_FIELD_SPECS);
	}
	
	/**
	 * Validate a JSON object as an Endpoint Property Map message.
	 * @param json The JSON Object to validate.
	 * @return A list of errors, or null if the message passed validation.
	 */
	public static List<String> validate(JSONValue_Object json)
	{
		return validate(json, new JSONValidate_Object(ENDPOINT_PROP_MAP_FIELD_SPECS));
	}
}
