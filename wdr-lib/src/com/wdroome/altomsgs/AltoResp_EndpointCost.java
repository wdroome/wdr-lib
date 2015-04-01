package com.wdroome.altomsgs;

import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.Reader;

import com.wdroome.json.JSONException;
import com.wdroome.util.ArrayIterator;
import com.wdroome.util.inet.EndpointAddress;
import com.wdroome.util.inet.SrcDestAddrs;
import com.wdroome.json.IJSONLexan;
import com.wdroome.json.JSONLexan;
import com.wdroome.json.JSONParseException;
import com.wdroome.json.JSONValueTypeException;
import com.wdroome.json.JSONValue_Array;
import com.wdroome.json.JSONValue_Object;
import com.wdroome.json.validate.JSONValidate;
import com.wdroome.json.validate.JSONValidate_Invalid;
import com.wdroome.json.validate.JSONValidate_Number;
import com.wdroome.json.validate.JSONValidate_Object;
import com.wdroome.json.validate.JSONValidate_Object.FieldSpec;
import com.wdroome.json.validate.JSONValidate_Object.RegexKey;
import com.wdroome.json.validate.JSONValidate_Object.SimpleKey;

/**
 * A class representing an ALTO Endpoint Cost response message.
 * @author wdr
 */
public class AltoResp_EndpointCost extends AltoResp_Base implements Iterable<String>
{
	public static final String MEDIA_TYPE = MEDIA_TYPE_PREFIX + "endpointcost" + MEDIA_TYPE_SUFFIX;
	
	private static final String FN_ENDPOINT_COST_MAP = "endpoint-cost-map";
	
	private static final String FN_DEFAULT_COST = "default-cost";	// ALU private extension
		
	private final Map<SrcDestAddrs, Double> m_srcDestCosts = new HashMap<SrcDestAddrs, Double>();
	
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
		if (accepts != null && accepts.equals(AltoReq_EndpointCostParams.MEDIA_TYPE)) {
			return "Endpoint Cost Service";
		} else {
			return null;
		}
	}
	
	/**
	 * Create an empty object.
	 * Used by server to construct a message to send.
	 */
	public AltoResp_EndpointCost()
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
	public AltoResp_EndpointCost(IJSONLexan lexan)
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
	public AltoResp_EndpointCost(String jsonSrc)
			throws JSONParseException, JSONValueTypeException
	{
		this(new JSONLexan(jsonSrc));
		setSrcDestCostMapFromJSON();
	}
	
	/**
	 * Create an object from a JSON Reader.
	 * Used to decode a received message.
	 * @param jsonSrc The encoded JSON message.
	 * @throws JSONParseException If jsonSrc isn't a valid JSON string.
	 * @throws JSONValueTypeException If the input isn't a JSON Object dictionary.
	 */
	public AltoResp_EndpointCost(Reader jsonSrc)
			throws JSONParseException, JSONValueTypeException
	{
		this(new JSONLexan(jsonSrc));
		setSrcDestCostMapFromJSON();
	}
		
	private void setSrcDestCostMapFromJSON()
	{
		for (String src:m_map.keyArray()) {
			try {
				JSONValue_Object srcCosts = m_map.getObject(src);
				EndpointAddress srcAddr = new EndpointAddress(src);
				for (String dest:getDestAddrs(src)) {
					try {
						double cost = srcCosts.getNumber(dest);
						m_srcDestCosts.put(new SrcDestAddrs(srcAddr, new EndpointAddress(dest)), cost);
					} catch (UnknownHostException e) {
						// Ugh. Shouldn't happen.
					} catch (JSONException e) {
						// Ugh. Shouldn't happen.
					}
				}
			} catch (UnknownHostException e) {
				// Ugh. Shouldn't happen.
			} catch (JSONException e) {
				// Ugh. Shouldn't happen.
			}
		}
	}
	
	@Override
	public String getMediaType()
	{
		return MEDIA_TYPE;
	}
	
	@Override
	public String[] getMapNames()
	{
		return new String[] {FN_ENDPOINT_COST_MAP};
	}
	
	public double getDefaultCost() throws JSONException
	{
		return getMetaDouble(FN_DEFAULT_COST, -1);
	}
	
	public void setDefaultCost(double value) throws JSONException
	{
		if (Double.isNaN(value))
			setMetaValue(FN_DEFAULT_COST, "NaN");
		else
			setMetaValue(FN_DEFAULT_COST, value);
	}
	
	/**
	 * Return an iterator over the source addrs.
	 */
	public Iterator<String> iterator()
	{
		return m_map.keys();
	}
	
	/**
	 * Return an array with the source addrs.
	 * Not sorted.
	 * @return An array with the source addrs.
	 */
	public String[] getSrcAddrs()
	{
		return m_map.keyArray();
	}
	
	/**
	 * Return the destination addrs for a source addr.
	 * @param src The source addr.
	 * @return An (unsorted) array with the destination addrs for "src."
	 * @throws JSONException If "src" doesn't exist.
	 */
	public String[] getDestAddrs(String src) throws JSONException
	{
		return m_map.getObject(EndpointAddress.addPrefix(src)).keyArray();
	}
	
	/**
	 * Return the cost between two addrs.
	 * @param src The source addr.
	 * @param dest The destination addr.
	 * @return The cost.
	 * @throws JSONException If src or dest aren't valid addrs.
	 */
	public double getCost(String src, String dest) throws JSONException
	{
		try {
			SrcDestAddrs srcDest = new SrcDestAddrs(src, dest);
			Double cost = m_srcDestCosts.get(srcDest);
			if (cost != null)
				return cost;
		} catch (UnknownHostException e) {
			// Fall thru as backup.
		}
		return m_map.getObject(EndpointAddress.addPrefix(src))
									.getNumber(EndpointAddress.addPrefix(dest));
	}
	
	/**
	 * Return the cost between two addrs.
	 * @param src The source addr.
	 * @param dest The destination addr.
	 * @return The cost.
	 * @throws JSONException If src or dest aren't valid addrs.
	 */
	public double getCost(EndpointAddress src, EndpointAddress dest) throws JSONException
	{
		SrcDestAddrs srcDest = new SrcDestAddrs(src, dest);
		Double cost = m_srcDestCosts.get(srcDest);
		if (cost != null)
			return cost;
		return m_map.getObject(src.toIPAddrWithPrefix()).getNumber(dest.toIPAddrWithPrefix());
	}

	/**
	 * Set the cost between two addrs.
	 * @param src The source addr.
	 * @param dest The destination addr.
	 * @param cost The cost.
	 */
	public void setCost(String src, String dest, double cost)
	{
		src = EndpointAddress.addPrefix(src);
		dest = EndpointAddress.addPrefix(dest);
		JSONValue_Object srcMap = m_map.getObject(src, null);
		if (srcMap == null) {
			srcMap = new JSONValue_Object();
			m_map.put(src, srcMap);
		}
		if (Double.isNaN(cost))
			srcMap.put(dest, "NaN");
		else
			srcMap.put(dest, cost);
		try {
			m_srcDestCosts.put(new SrcDestAddrs(src, dest), cost);
		} catch (UnknownHostException e) {
			// Ignore. What can we do else???
		}
	}
	
	/** FieldSpecs to validate Endpoint Cost Map messages. */
	private static final FieldSpec[] ENDPOINT_COST_MAP_FIELD_SPECS = new FieldSpec[] {
		
			// meta:
			AltoValidators.META_COST_TYPE,
		
			// cost-map:
			new FieldSpec(
				new SimpleKey(FN_ENDPOINT_COST_MAP, true),
				new JSONValidate_Object(new FieldSpec[] {
					new FieldSpec(
						AltoValidators.VALID_IP_ADDR_KEY,
						new JSONValidate_Object(new FieldSpec[] {
							new FieldSpec(
								AltoValidators.VALID_IP_ADDR_KEY,
								new JSONValidate_Number(0)),
							new FieldSpec(
								new RegexKey(".*"),
								new JSONValidate_Invalid("Invalid Destination IP Address")),
						})
					),
					new FieldSpec(
						new RegexKey(".*"),
						new JSONValidate_Invalid("Invalid Source IP Address")),
				})
			)
		};

	/**
	 * Return a new validator for Endpoint Cost Map messages.
	 */
	@Override
	protected JSONValidate getValidator()
	{
		return new JSONValidate_Object(ENDPOINT_COST_MAP_FIELD_SPECS);
	}
	
	/**
	 * Validate a JSON object as an Endpoint Cost Map message.
	 * @param json The JSON Object to validate.
	 * @return A list of errors, or null if the message passed validation.
	 */
	public static List<String> validate(JSONValue_Object json)
	{
		return validate(json, new JSONValidate_Object(ENDPOINT_COST_MAP_FIELD_SPECS));
	}
}
