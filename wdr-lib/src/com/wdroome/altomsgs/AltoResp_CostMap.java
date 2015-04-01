package com.wdroome.altomsgs;

import java.util.Iterator;
import java.util.HashSet;
import java.util.List;
import java.io.Reader;

import com.wdroome.json.JSONException;
import com.wdroome.json.IJSONLexan;
import com.wdroome.json.JSONFieldMissingException;
import com.wdroome.json.JSONLexan;
import com.wdroome.json.JSONParseException;
import com.wdroome.json.JSONValueTypeException;
import com.wdroome.json.JSONValue;
import com.wdroome.json.JSONValue_Number;
import com.wdroome.json.JSONValue_Array;
import com.wdroome.json.JSONValue_Object;
import com.wdroome.json.JSONValue_String;
import com.wdroome.json.validate.JSONValidate;
import com.wdroome.json.validate.JSONValidate_Number;
import com.wdroome.json.validate.JSONValidate_Invalid;
import com.wdroome.json.validate.JSONValidate_Object;
import com.wdroome.json.validate.JSONValidate_Object.FieldSpec;
import com.wdroome.json.validate.JSONValidate_Object.RegexKey;
import com.wdroome.json.validate.JSONValidate_Object.SimpleKey;

/**
 * A class representing an ALTO Cost-Map response message.
 * Warning: This class will run out of memory for large response messages (1000 pids and up).
 * {@link AltoResp_IndexedCostMap} is more efficient, and will handle responses
 * with 5,000 pids and 25 million cost points.
 * @see AltoResp_IndexedCostMap
 * @author wdr
 */
public class AltoResp_CostMap extends AltoResp_Base implements Iterable<String>
{
	public static final String MEDIA_TYPE = MEDIA_TYPE_PREFIX + "costmap" + MEDIA_TYPE_SUFFIX;

	public static final String COST_METRIC_ROUTINGCOST	= "routingcost";
	public static final String COST_METRIC_HOPCOUNT		= "hopcount";
	public static final String COST_MODE_NUMERICAL		= "numerical";
	public static final String COST_MODE_ORDINAL		= "ordinal";
	
	public static final String CAPABILITY_COST_CONSTRAINTS	= "cost-constraints";
	public static final String CAPABILITY_COST_TYPE_NAMES	= "cost-type-names";
	public static final String CAPABILITY_COST_TYPES_METRIC = "cost-metric";
	public static final String CAPABILITY_COST_TYPES_MODE	= "cost-mode";
	public static final String CAPABILITY_COST_TYPES_DESCRIPTION	= "description";
	
	private static final String FN_COST_MAP = "cost-map";
	
	private static final String FN_DEFAULT_COST = "default-cost";	// ALU private extension
	
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
			return "Full Cost Map";
		} else if (accepts.equals(AltoReq_FilteredCostMap.MEDIA_TYPE)) {
			return "Filtered Cost Map";
		} else {
			return null;
		}
	}
			
	/**
	 * Create an empty object.
	 * Used by server to construct a message to send.
	 */
	public AltoResp_CostMap()
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
	public AltoResp_CostMap(IJSONLexan lexan)
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
	public AltoResp_CostMap(String jsonSrc)
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
	public AltoResp_CostMap(Reader jsonSrc)
			throws JSONParseException, JSONValueTypeException
	{
		this(new JSONLexan(jsonSrc));
	}
	
	/**
	 * Return an approximate limit on the size of JSON input
	 * for which this representation class is suitable.
	 * For this class, return 1,000,000.
	 */
	public static long inputSizeLimit()
	{
		return 10000000;
	}
	
	@Override
	public String getMediaType()
	{
		return MEDIA_TYPE;
	}
	
	@Override
	public String[] getMapNames()
	{
		return new String[] {FN_COST_MAP};
	}
	
	public double getDefaultCost()
	{
		return getMetaDouble(FN_DEFAULT_COST, Double.NaN);
	}
	
	public void setDefaultCost(double value)
	{
		if (Double.isNaN(value))
			setMetaValue(FN_DEFAULT_COST, "NaN");
		else
			setMetaValue(FN_DEFAULT_COST, value);
	}
	
	/**
	 * Return an iterator over the source PIDs.
	 */
	public Iterator<String> iterator()
	{
		return m_map.keys();
	}
	
	/**
	 * Return an array with the source PIDs.
	 * Not sorted.
	 * @return An array with the source PIDs.
	 */
	public String[] getSrcPIDs()
	{
		return m_map.keyArray();
	}
	
	/**
	 * Return the number of source pids.
	 */
	public int getNumSrcPids()
	{
		return m_map.size();
	}
	
	/**
	 * Return the destination PIDs for a source PID.
	 * @param src The source PID.
	 * @return An (unsorted) array with the destination PIDs for "src."
	 * @throws JSONException If "src" doesn't exist.
	 */
	public String[] getDestPIDs(String src) throws JSONException
	{
		return m_map.getObject(src).keyArray();
	}
	
	/**
	 * Return an array with all destination PIDs referenced in this message.
	 * @return An array with all destination PIDs referenced in this message.
	 */
	public String[] getDestPIDs()
	{
		String[] srcPids = getSrcPIDs();
		HashSet<String> destPids = new HashSet<String>(srcPids.length);
		for (String src:srcPids) {
			try {
				for (String dest:getDestPIDs(src)) {
					destPids.add(dest);
				}
			} catch (JSONException e) {
				// Ignore -- shouldn't happen
			}
		}
		return destPids.toArray(new String[destPids.size()]);
	}
	
	/**
	 * Return the cost between two pids.
	 * @param src The source PID.
	 * @param dest The destination PID.
	 * @return The cost.
	 * @throws JSONException
	 *		If src or dest aren't valid PIDs,
	 *		or if there's no cost defined from src to dest.
	 *		Note: If the default cost is non-negative,
	 *		we return the default cost instead of throwing an exception.
	 */
	public double getCost(String src, String dest) throws JSONException
	{
		try {
			return m_map.getObject(src).getNumber(dest);
		} catch (JSONException e) {
			double defCost = getDefaultCost();
			if (defCost >= 0) {
				return defCost;
			} else {
				throw e;
			}
		}
	}

	/**
	 * Set the cost between two pids.
	 * @param src The source PID.
	 * @param dest The destination PID.
	 * @param cost The cost.
	 */
	public void setCost(String src, String dest, double cost)
	{
		JSONValue_Object srcMap = m_map.getObject(src, null);
		if (srcMap == null) {
			srcMap = new JSONValue_Object();
			m_map.put(src, srcMap);
		}
		if (Double.isNaN(cost))
			srcMap.put(dest, "NaN");
		else
			srcMap.put(dest, cost);
	}
	
	/** FieldSpecs to validate Cost Map messages. */
	private static final FieldSpec[] COST_MAP_FIELD_SPECS = new FieldSpec[] {
		
			// meta:
			AltoValidators.META_COST_TYPE_VTAGS,
		
			// cost-map:
			new FieldSpec(
				new SimpleKey(FN_COST_MAP, true),
				new JSONValidate_Object(new FieldSpec[] {
					new FieldSpec(
						new RegexKey(AltoValidators.PID_NAME_PAT),
						new JSONValidate_Object(new FieldSpec[] {
							new FieldSpec(
								new RegexKey(AltoValidators.PID_NAME_PAT),
								new JSONValidate_Number(0)),
							new FieldSpec(
								new RegexKey(".*"),
								new JSONValidate_Invalid("Invalid Destination PID Name")),
						})
					),
					new FieldSpec(
						new RegexKey(".*"),
						new JSONValidate_Invalid("Invalid Source PID Name")
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
		return new JSONValidate_Object(COST_MAP_FIELD_SPECS);
	}
	
	/**
	 * Validate a JSON object as a CostMap message.
	 * @param json The JSON Object to validate.
	 * @return A list of errors, or null if the message passed validation.
	 */
	public static List<String> validate(JSONValue_Object json)
	{
		return validate(json, new JSONValidate_Object(COST_MAP_FIELD_SPECS));
	}
}
