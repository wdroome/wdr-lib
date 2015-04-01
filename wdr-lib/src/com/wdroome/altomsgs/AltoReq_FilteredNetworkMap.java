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

/**
 * Represent a request message for Filtered Network Map request.
 * The message is a JSON dictionary with one element, "pids",
 * which is an array of PID names.
 * @author wdr
 */
public class AltoReq_FilteredNetworkMap extends AltoMsg_Base
{
	public static final String MEDIA_TYPE = MEDIA_TYPE_PREFIX + "networkmapfilter" + MEDIA_TYPE_SUFFIX;

	private final JSONValue_Array m_pids;
	private final JSONValue_Array m_addressTypes;

	private static final String FN_PIDS = "pids";
	private static final String FN_ADDRESS_TYPES = "address-types";

	/**
	 * Create an empty request object.
	 * Used by server to construct a request to send to a server.
	 */
	public AltoReq_FilteredNetworkMap()
	{
		super();
		m_json.put(FN_PIDS, m_pids = new JSONValue_Array());
		m_json.put(FN_ADDRESS_TYPES, m_addressTypes = new JSONValue_Array());
	}

	/**
	 * Create an object from a JSON parser.
	 * Used to decode a received message.
	 * @param lexan The JSON input parser.
	 * @throws JSONParseException If jsonSrc isn't a valid JSON string.
	 * @throws JSONValueTypeException If the input isn't a JSON Object dictionary.
	 * @throws JSONFieldMissingException If a required field is missing.
	 */
	public AltoReq_FilteredNetworkMap(IJSONLexan lexan)
		throws JSONParseException, JSONValueTypeException, JSONFieldMissingException
	{
		super(lexan);
		if (!m_json.has(FN_PIDS))
			m_json.put(FN_PIDS, new JSONValue_Array());
		m_pids = m_json.getArray(FN_PIDS);
		if (!m_json.has(FN_ADDRESS_TYPES))
			m_json.put(FN_ADDRESS_TYPES, new JSONValue_Array());
		m_addressTypes = m_json.getArray(FN_ADDRESS_TYPES);
	}
	
	/**
	 * Create a request object from a JSON string.
	 * Used by server to decode a request from a client.
	 * @param jsonSrc The encoded JSON request.
	 * @throws JSONParseException If jsonSrc isn't a valid JSON string.
	 * @throws JSONValueTypeException If the input isn't a JSON Object dictionary.
	 * @throws JSONFieldMissingException If a required field is missing.
	 */
	public AltoReq_FilteredNetworkMap(String jsonSrc)
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
	public AltoReq_FilteredNetworkMap(Reader jsonSrc)
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
	 * Add a PID to the request.
	 * @param pid The pid. Quietly ignore if pid is null, blank or "-".
	 */
	public void addPID(String pid)
	{
		if (pid != null && !pid.equals("") && !pid.equals("-"))
			m_pids.add(pid);
	}

	/**
	 * Add a set of pids to the request.
	 * @param pids An array of pids. Quietly ignore any that are null, blank or "-".
	 */
	public void addPIDs(String[] pids)
	{
		if (pids != null) {
			for (String pid: pids) {
				addPID(pid);
			}
		}
	}
	
	/**
	 * Return an array with the pids in the request.
	 * Never returns null; returns a 0-length array if there are no pids.
	 */
	public String[] getPIDs()
	{
		return getStringArray(m_pids);
	}
	
	/**
	 * Return the number of PIDs in the request.
	 * @return The number of PIDs in the request.
	 */
	public int getNumPIDs()
	{
		return m_pids.size();
	}

	/**
	 * Add an address type to the request.
	 * @param addressType The address type. Quietly ignore if null, blank or "-".
	 */
	public void addAddressType(String addressType)
	{
		if (addressType != null && !addressType.equals("") && !addressType.equals("-"))
			m_addressTypes.add(addressType);
	}

	/**
	 * Add a set of address types to the request.
	 * @param addressTypes An array of address types. Quietly ignore any that are null, blank or "-".
	 */
	public void addAddressTypes(String[] addressTypes)
	{
		if (addressTypes != null) {
			for (String addressType: addressTypes) {
				addAddressType(addressType);
			}
		}
	}
	
	/**
	 * Return an array with the address types in the request.
	 * Never returns null; returns a 0-length array if there are no address types.
	 */
	public String[] getAddressTypes()
	{
		return getStringArray(m_addressTypes);
	}
	
	/**
	 * Return the number of address types in the request.
	 * @return The number of address types in the request.
	 */
	public int getNumAddressTypes()
	{
		return m_addressTypes.size();
	}
	
	/** FieldSpecs to validate Filtered Network Param messages. */
	private static final FieldSpec[] FILTERED_NETWORK_PARAM_FIELD_SPECS = new FieldSpec[] {
		
			// PIDs:
			new FieldSpec(
				new SimpleKey(FN_PIDS, true),
					AltoValidators.PID_NAME_ARRAY
				),

			// address-types:
			new FieldSpec(
				new SimpleKey(FN_ADDRESS_TYPES, false),
					AltoValidators.IP_ADDR_PREFIX_ARRAY
				),
		};

	/**
	 * Return a new validator for Filtered Network Param messages.
	 */
	@Override
	protected JSONValidate getValidator()
	{
		return new JSONValidate_Object(FILTERED_NETWORK_PARAM_FIELD_SPECS);
	}
	
	/**
	 * Validate a JSON object as a Filtered Network Param message.
	 * @param json The JSON Object to validate.
	 * @return A list of errors, or null if the message passed validation.
	 */
	public static List<String> validate(JSONValue_Object json)
	{
		return validate(json, new JSONValidate_Object(FILTERED_NETWORK_PARAM_FIELD_SPECS));
	}
}
