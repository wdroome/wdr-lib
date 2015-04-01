package com.wdroome.altomsgs;

import java.util.Iterator;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.PrintStream;
import java.io.Reader;

import com.wdroome.json.JSONException;
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
import com.wdroome.json.validate.JSONValidate_Array;
import com.wdroome.json.validate.JSONValidate_Object;
import com.wdroome.json.validate.JSONValidate_Object.FieldSpec;
import com.wdroome.json.validate.JSONValidate_Object.RegexKey;
import com.wdroome.json.validate.JSONValidate_Object.SimpleKey;
import com.wdroome.util.ArrayIterator;
import com.wdroome.util.inet.CIDRAddress;
import com.wdroome.util.inet.CIDRSet;
import com.wdroome.util.inet.EndpointAddress;
import com.wdroome.util.StringUtils;

/**
 * A class representing an ALTO Network-Map response message.
 * @author wdr
 */
public class AltoResp_NetworkMap extends AltoResp_Base implements Iterable<String>
{
	public static final String MEDIA_TYPE = MEDIA_TYPE_PREFIX + "networkmap" + MEDIA_TYPE_SUFFIX;
	
	private static final String FN_NETWORK_MAP = "network-map";
	
	private static final String FN_INITIAL_COST = "initial-cost";	// private to ALU ALTO server
	
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
		if (accepts == null) {
			return "Full Network Map";
		} else if (accepts.equals(AltoReq_FilteredNetworkMap.MEDIA_TYPE)) {
			return "Filtered Network Map";
		} else {
			return null;
		}
	}
		
	/**
	 * Create an empty object.
	 * Used by server to construct a message to send.
	 */
	public AltoResp_NetworkMap()
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
	public AltoResp_NetworkMap(IJSONLexan lexan)
		throws JSONParseException, JSONValueTypeException
	{
		super(lexan);
	}
	
	/**
	 * Create an object from a JSON string.
	 * Used to decode a received message.
	 * @param jsonSrc The encoded JSON message.
	 * @throws JSONParseException If jsonSrc isn't a valid JSON string.
	 * @throws JSONValueTypeException If the input isn't a JSON Object dictionary.
	 */
	public AltoResp_NetworkMap(String jsonSrc)
		throws JSONParseException, JSONValueTypeException
	{
		this(new JSONLexan(jsonSrc));
	}
	
	/**
	 * Create an object from a JSON Reader.
	 * Used to decode a received message.
	 * @param jsonSrc The encoded JSON message.
	 * @throws JSONParseException If jsonSrc isn't a valid JSON string.
	 * @throws JSONValueTypeException If the input isn't a JSON Object dictionary.
	 */
	public AltoResp_NetworkMap(Reader jsonSrc)
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
		return new String[] { FN_NETWORK_MAP };
	}
	
	@Deprecated
	public double getInitialCost()
	{
		return getMetaDouble(FN_INITIAL_COST, -1.0);
	}
	
	@Deprecated
	public void setInitialCost(double value)
	{
		setMetaValue(FN_INITIAL_COST, value);
	}
	
	/**
	 * Return an iterator over the PIDs.
	 */
	public Iterator<String> iterator()
	{
		return m_map.keys();
	}
	
	/**
	 * Return an array with the PIDs.
	 * Not sorted.
	 * @return An array with the PIDs. Never returns null.
	 */
	public String[] getPIDs()
	{
		String[] pids = m_map.keyArray();
		if (pids == null) {
			pids = new String[0];
		}
		return pids;
	}
	
	/**
	 * Return the CIDRs for a pid.
	 * @param pid The pid.
	 * @param addrType
	 * 		An address type, such as {@link EndpointAddress#IPV4_PREFIX}
	 * 		or {@link EndpointAddress#IPV6_PREFIX}.
	 * @return The CIDRs for pid of that address type.
	 * @throws JSONException If pid doesn't exist.
	 */
	public String[] getCIDRs(String pid, String addrType) throws JSONException
	{
		JSONValue_Object cidrsForPid = m_map.getObject(pid, null);
		if (cidrsForPid == null)
			return new String[0];
		return getStringArray(cidrsForPid.getArray(addrType, null));
	}
	
	/**
	 * Return all CIDRs for a pid, for all address types.
	 * Each string is prefixed with its address type.
	 * @param pid The pid.
	 * @return All cidrs for that pid, with address type prefixes.
	 * @throws JSONException If pid doesn't exist.
	 */
	public String[] getCIDRs(String pid) throws JSONException
	{
		JSONValue_Object cidrsForPid = m_map.getObject(pid, null);
		if (cidrsForPid == null)
			return new String[0];
		ArrayList<String> cidrs = new ArrayList<String>();
		for (Iterator<String> iter = cidrsForPid.keys(); iter.hasNext(); ) {
			String addrType = iter.next();
			addStringArray(cidrs, cidrsForPid.getArray(addrType, null), addrType + ":");
		}
		return cidrs.toArray(new String[cidrs.size()]);
	}
	
	/**
	 * Return the ip address types for the CIDRs in a PID.
	 * @param pid The pid.
	 * @return An iterator over the address type strings.
	 * @see #getCIDRs(String, String)
	 */
	public Iterator<String> getAddressTypes(String pid)
	{
		JSONValue_Object cidrsForPid = m_map.getObject(pid, null);
		if (cidrsForPid == null)
			return new ArrayIterator<String>(null);
		return cidrsForPid.keys();
	}
	
	private HashSet<String> m_allAddressTypes = null;
	
	/**
	 * Return an iterator with all distinct IP address types for all PIDs.
	 */
	public Iterator<String> getAllAddressTypes()
	{
		if (m_allAddressTypes == null) {
			HashSet<String> addrTypes = new HashSet<String>(4);
			for (String pid:getPIDs()) {
				for (Iterator<String> iter = getAddressTypes(pid); iter.hasNext(); ) {
					addrTypes.add(iter.next());
				}
			}
			m_allAddressTypes = addrTypes;
		}
		return m_allAddressTypes.iterator();
	}
	
	/**
	 * Add a CIDR for a pid.
	 * @param pid The pid.
	 * @param addrType The address type -- ipv4 or ipv6.
	 * @param cidr The new CIDR.
	 */
	public void addCIDR(String pid, String addrType, String cidr)
	{
		addToArray(m_map, pid, addrType, cidr);
	}
	
	/**
	 * Ensure that a PID is defined. If it does not yet exist,
	 * create an empty dictionary for it.
	 * If the PID does exist, return quietly.
	 * @param pid The name of the PID.
	 */
	public void addPID(String pid)
	{
		if (!m_map.has(pid)) {
			m_map.put(pid, new JSONValue_Object());
		}
	}
	
	/**
	 * Write a network map response in compact JSON.
	 * @param out The output stream.
	 * @param resourceId The map's resource ID.
	 * @param tag The map's vtag.
	 * @param pidEntries The CIDR set for each PID name.
	 */
	public static void writeNetworkMap(PrintStream out,
									   String resourceId,
									   String tag,
									   Iterable<Map.Entry<String,CIDRSet>> pidEntries)
	{
		out.print("{\"meta\":{\"vtag\":{\"resource-id\":\"");
		out.print(StringUtils.escapeSimpleJSONString(resourceId));
		out.print("\",\"tag\":\"");
		out.print(StringUtils.escapeSimpleJSONString(tag));
		out.print("\"}},\"network-map\":{");
		int nPids = 0;
		for (Map.Entry<String, CIDRSet> entry:pidEntries) {
			String pid = entry.getKey();
			if (++nPids > 1)
				out.print(',');
			out.print('"');
			out.print(StringUtils.escapeSimpleJSONString(pid));
			out.print("\":{");
			String sep = "\"ipv4\":[\"";
			int nIpv4 = 0;
			ArrayList<String> ipv6 = new ArrayList<String>();
			for (CIDRAddress cidr:entry.getValue()) {
				if (cidr.isIPV6()) {
					ipv6.add(cidr.toString());
				} else {
					out.print(sep);
					out.print(cidr.toString());		// We assume CIDRs do not contain escapable characters
					sep = "\",\"";
					nIpv4++;
				}
			}
			if (nIpv4 > 0) {
				out.print("\"]");
			}
			if (ipv6.size() > 0) {
				if (nIpv4 > 0)
					out.print(',');
				out.print("\"ipv6\":");
				sep = "[\"";
				for (String addr:ipv6) {
					out.print(sep);
					out.print(addr);	// We assume CIDRs do not contain escapable characters
					sep = "\",\"";
				}
				out.print("\"]");
			}
			out.print('}');
		}
		out.print("}}");
	}
	
	/** FieldSpecs to validate Network Map messages. */
	private static final FieldSpec[] NETWORK_MAP_FIELD_SPECS = new FieldSpec[] {
		
			// meta:
			AltoValidators.META_VTAG,
		
			// network-map:
			new FieldSpec(
				new SimpleKey(FN_NETWORK_MAP, true),
				new JSONValidate_Object(new FieldSpec[] {
					new FieldSpec(
						new RegexKey(AltoValidators.PID_NAME_PAT),
						new JSONValidate_Object(
							new FieldSpec[] {
								new FieldSpec(
									new SimpleKey(EndpointAddress.IPV4_PREFIX, false),
									new JSONValidate_Array(AltoValidators.VALID_IPV4_CIDR)
								),
								new FieldSpec(
									new SimpleKey(EndpointAddress.IPV6_PREFIX, false),
									new JSONValidate_Array(AltoValidators.VALID_IPV6_CIDR)
								),
						})
					),
					new FieldSpec(
						new RegexKey(".*"),
						new JSONValidate_Invalid("Invalid PID Name")
					),
				})
			)
		};

	/**
	 * Return a new validator for Cost Map messages.
	 */
	@Override
	protected JSONValidate getValidator()
	{
		return new JSONValidate_Object(NETWORK_MAP_FIELD_SPECS);
	}
	
	/**
	 * Validate a JSON object as a CostMap message.
	 * @param json The JSON Object to validate.
	 * @return A list of errors, or null if the message passed validation.
	 */
	public static List<String> validate(JSONValue_Object json)
	{
		return validate(json, new JSONValidate_Object(NETWORK_MAP_FIELD_SPECS));
	}
}
