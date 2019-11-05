package com.wdroome.altomsgs;

import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import com.wdroome.util.IterableWrapper;
import com.wdroome.util.String2;
import com.wdroome.json.JSONException;
import com.wdroome.json.IJSONLexan;
import com.wdroome.json.JSONLexan;
import com.wdroome.json.JSONFieldMissingException;
import com.wdroome.json.JSONParseException;
import com.wdroome.json.JSONValue;
import com.wdroome.json.JSONValueTypeException;
import com.wdroome.json.JSONValue_String;
import com.wdroome.json.JSONValue_Array;
import com.wdroome.json.JSONValue_Object;
import com.wdroome.json.validate.JSONValidate;
import com.wdroome.json.validate.JSONValidate_Invalid;
import com.wdroome.json.validate.JSONValidate_String;
import com.wdroome.json.validate.JSONValidate_Boolean;
import com.wdroome.json.validate.JSONValidate_Array;
import com.wdroome.json.validate.JSONValidate_Object;
import com.wdroome.json.validate.JSONValidate_Object.FieldSpec;
import com.wdroome.json.validate.JSONValidate_Object.RegexKey;
import com.wdroome.json.validate.JSONValidate_Object.SimpleKey;

/**
 * Represent an Information Resource Directory response from an ALTO server.
 * @author wdr
 */
public class AltoResp_InfoResourceDir extends AltoResp_Base
{
	public static final String MEDIA_TYPE = MEDIA_TYPE_PREFIX + "directory" + MEDIA_TYPE_SUFFIX;
	
	private static final String FN_RESOURCES = "resources";
	
	// Resource fields:
	public static final String FN_URI = "uri";
	public static final String FN_MEDIA_TYPE = "media-type";
	public static final String FN_ACCEPTS = "accepts";
	public static final String FN_USES = "uses";
	public static final String FN_CAPABILITIES = "capabilities";
	
	// Meta fields:
	public static final String FN_COST_TYPES = "cost-types";
	public static final String FN_DEFAULT_NETWORK_MAP = "default-alto-network-map";
	
	// The "opcode" for private, extended resources:
	private static final String	FN_OPCODE = "priv:alu-opcode";
	private static final String	FN_CMD_CODE = "priv:alu-cmd-code";
	
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
			return "Information Resource Directory";
		} else {
			return null;
		}
	}
		
	/**
	 * Create an empty object.
	 * Used by server to construct a message to send.
	 */
	public AltoResp_InfoResourceDir()
	{
		super();
	}
	
	/**
	 * Create an object from a JSON parser.
	 * Used to decode a received message.
	 * @param lexan The JSON input parser.
	 * @throws JSONParseException If jsonSrc isn't a valid JSON string.
	 * @throws JSONValueTypeException If the input isn't a JSON Object dictionary.
	 * @throws JSONFieldMissingException  If a required field isn't the correct type.
	 */
	public AltoResp_InfoResourceDir(IJSONLexan lexan)
		throws JSONParseException, JSONValueTypeException, JSONFieldMissingException
	{
		super(lexan);
	}

	/**
	 * Create an object from a JSON string.
	 * Used to decode a received message.
	 * @param jsonSrc The encoded JSON message.
	 * @throws JSONParseException If jsonSrc isn't a valid JSON string.
	 * @throws JSONValueTypeException If the input isn't a JSON Object dictionary.
	 * @throws JSONFieldMissingException  If a required field isn't the correct type.
	 */
	public AltoResp_InfoResourceDir(String jsonSrc)
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
	public String[] getMapNames()
	{
		return new String[] { FN_RESOURCES };
	}

	/**
	 * Add a new cost-type name to this IRD's meta data.
	 * @param name The cost-type name.
	 * @param metric The metric name.
	 * @param mode The mode.
	 * @param description If not null, a short description.
	 */
	public void addCostTypeName(String name, String metric, String mode, String description)
	{
		JSONValue_Object entry = new JSONValue_Object();
		entry.put(AltoResp_CostMap.CAPABILITY_COST_TYPES_METRIC, metric);
		entry.put(AltoResp_CostMap.CAPABILITY_COST_TYPES_MODE, mode);
		if (description != null && !description.equals(""))
			entry.put(AltoResp_CostMap.CAPABILITY_COST_TYPES_DESCRIPTION, description);
		JSONValue_Object costTypes = getMetaJSONObject(FN_COST_TYPES);
		if (costTypes == null) {
			costTypes = new JSONValue_Object();
			setMetaValue(FN_COST_TYPES, costTypes);
		}
		costTypes.put(name, entry);
	}
	
	/**
	 * Return true iff this IRD has named cost types in its metadata.
	 */
	public boolean hasNamedCostTypes()
	{
		return getMetaJSONObject(FN_COST_TYPES) != null;
	}
	
	/**
	 * Search the IRD cost-type metadata for an entry with "metric" and "mode",
	 * and return that cost-type name.
	 * @param metric The desired cost metric.
	 * @param mode The desired cost mode.
	 * @return The name of that cost type, or null if not found.
	 */
	public String findCostTypeName(String metric, String mode)
	{
		JSONValue_Object costTypes = getMetaJSONObject(FN_COST_TYPES);
		if (costTypes == null)
			return null;
		for (Map.Entry<String,JSONValue> nct: costTypes.entrySet()) {
			JSONValue ov = nct.getValue();
			if (ov instanceof JSONValue_Object) {
				JSONValue_Object v = (JSONValue_Object)ov;
				if (metric.equals(v.getString(AltoResp_CostMap.CAPABILITY_COST_TYPES_METRIC, null))
						&& mode.equals(v.getString(AltoResp_CostMap.CAPABILITY_COST_TYPES_MODE, null))) {
					return nct.getKey();
				}
			}
		}
		return null;
	}
	
	/**
	 * Return the named cost types in this IRD.
	 * @return A table with the named cost types in this IRD.
	 * 		Return an empty map if there are no named cost types.
	 */
	public Map<String,AltoCostId> getCostTypes()
	{
		Map<String, AltoCostId> result = new HashMap<String, AltoCostId>();
		JSONValue_Object costTypes = getMetaJSONObject(FN_COST_TYPES);
		if (costTypes != null) {
			for (Map.Entry<String, JSONValue> nct : costTypes.entrySet()) {
				JSONValue ov = nct.getValue();
				if (ov instanceof JSONValue_Object) {
					String name = nct.getKey();
					JSONValue_Object v = (JSONValue_Object) ov;
					String metric = v
							.getString(AltoResp_CostMap.CAPABILITY_COST_TYPES_METRIC, null);
					String mode = v
							.getString(AltoResp_CostMap.CAPABILITY_COST_TYPES_MODE, null);
					if (metric != null && !metric.equals("") && mode != null
							&& !mode.equals("")) {
						result.put(name, new AltoCostId(metric, mode, name,
										v.getString(AltoResp_CostMap.CAPABILITY_COST_TYPES_DESCRIPTION, null),
										null));
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * Add a new resource to the directory.
	 * @param uri The URI. Required.
	 * @param mediaType The media type. Required.
	 * @param accepts The accept types. May be null.
	 * @param uses The resource id this resource depends on. May be null.
	 * @param capabilities The capabilities, as a JSON dictionary. May be null.
	 */
	public void addResource(String id,
							String uri,
							String mediaType,
							String accepts,
							String[] uses,
							JSONValue_Object capabilities)
	{
		JSONValue_Object r = new JSONValue_Object();
		r.put(FN_URI, uri);
		r.put(FN_MEDIA_TYPE, mediaType);
		if (accepts != null) {
			r.put(FN_ACCEPTS, accepts);
		}
		if (uses != null && uses.length > 0) {	
			r.put(FN_USES, new JSONValue_Array(uses));
		}
		if (capabilities != null && capabilities.size() > 0) {
			r.put(FN_CAPABILITIES, capabilities);
		}
		m_map.put(id, r);
	}

	/**
	 * Add a new resource to the directory.
	 * @param uri The URI. Required.
	 * @param mediaType The media type this resource returns. Required.
	 * @param accept The media type this resource accepts. May be null.
	 * @param uses The resource id this resource depends on. May be null.
	 * @param capabilities The capabilities, as a JSON dictionary. May be null.
	 * @param opcode Opcode for extended resources. Null for standard resources.
	 * @param cmdCode The operation for custom costmap commands.
	 */
	public void addResourceOpcode(String id,
								  String uri,
								  String mediaType,
								  String accept,
								  String[] uses,
								  JSONValue_Object capabilities,
								  String opcode,
								  String cmdCode)
	{
		JSONValue_Object r = new JSONValue_Object();
		r.put(FN_URI, uri);
		r.put(FN_MEDIA_TYPE, mediaType);
		if (accept != null) {
			r.put(FN_ACCEPTS, accept);
		}
		if (uses != null && uses.length > 0) {
			r.put(FN_USES, new JSONValue_Array(uses));
		}
		if (capabilities != null && capabilities.size() > 0) {
			r.put(FN_CAPABILITIES, capabilities);
		}
		if (opcode != null) {
			r.put(FN_OPCODE, opcode);
		}
		if (cmdCode != null) {
			r.put(FN_CMD_CODE, cmdCode);
		}
		m_map.put(id, r);
	}
	
	/**
	 * Add an arbitrary JSON resource.
	 * @param resource The JSON resource to add.
	 */
	public void addResource(String id, JSONValue_Object resource)
	{
		m_map.put(id, resource);
	}
	
	/**
	 * Set the default network map ID.
	 * @param mapId The default ID.
	 */
	public void setDefaultNetworkMapId(String mapId)
	{
		setMetaValue(FN_DEFAULT_NETWORK_MAP, mapId);
	}
	
	/**
	 * Return the resource ID of the default network map,
	 * or null if no default map is declared in this IRD.
	 */
	public String getDefaultNetworkMapId()
	{
		return getMetaString(FN_DEFAULT_NETWORK_MAP);
	}
		
	/**
	 * @return The number of resources in this directory.
	 */
	public int size()
	{
		return m_map.size();
	}
	
	/**
	 * Return an Iterable over the resource IDs in this IRD.
	 */
	public Iterable<String> getIds()
	{
		return new IterableWrapper<String>(m_map.keys());
	}
	
	/**
	 * Return the URI for a resource.
	 * @param id The ID of the resource.
	 * @return The URI for id.
	 * @throws JSONException If id doesn't exist or it doesn't have a uri.
	 */
	public String getURI(String id) throws JSONException
	{
		return m_map.getObject(id).getString(FN_URI);
	}
	
	/**
	 * Return the media type for a resource.
	 * @param id The ID of the resource.
	 * @return The media type for id, or null if none specified.
	 */
	public String getMediaType(String id)
	{
		JSONValue_Object r = m_map.getObject(id, null);
		return r != null ? r.getString(FN_MEDIA_TYPE, null) : null;
	}
	
	/**
	 * Return the accept types for a resource.
	 * @param id The ID of the resource.
	 * @return The accept type for id, or null if unspecified.
	 */	
	public String getAccepts(String id)
	{
		JSONValue_Object r = m_map.getObject(id, null);
		return r != null ? r.getString(FN_ACCEPTS, null) : null;
	}
	
	/**
	 * Return the ids of any resources this resource depends on.
	 * @param id The ID of the resource.
	 * @return The resources that id depends on, or null if none.
	 */	
	public Set<String> getUses(String id)
	{
		JSONValue_Object r = m_map.getObject(id, null);
		if (r == null) {
			return null;
		}
		JSONValue_Array uses = r.getArray(FN_USES, null);
		if (uses == null || uses.size() == 0) {
			return null;
		}
		Set<String> strs = new HashSet<String>(uses.size());
		for (JSONValue obj: uses) {
			if (obj != null && obj instanceof JSONValue_String) {
				strs.add(((JSONValue_String)obj).m_value);
			}
		}
		return strs;
	}
	
	/**
	 * Return the capabilities for a resource.
	 * @param id The ID of the resource.
	 * @return The capabilities for iResource, or null if unspecified.
	 */	
	public JSONValue_Object getCapabilities(String id)
	{
		JSONValue_Object r = m_map.getObject(id, null);
		return (r != null && r.has(FN_CAPABILITIES)) ? r.getObject(FN_CAPABILITIES, null) : null;
	}
	
	/**
	 * Return the extended opcode for a resource.
	 * @param id The ID of the resource.
	 * @return The extended opcode for iResource, or null if not present.
	 */
	public String getOpcode(String id)
	{
		JSONValue_Object r = m_map.getObject(id, null);
		return r != null ? r.getString(FN_OPCODE, null) : null;
	}
	
	/**
	 * Return the costmap command code for an extended opcode resource.
	 * @param id The ID of the resource.
	 * @return The costmap command code for iResource, or null if not present.
	 */
	public String getCostmapCmd(String id)
	{
		JSONValue_Object r = m_map.getObject(id, null);
		return r != null ? r.getString(FN_CMD_CODE, null) : null;
	}
	
	/** FieldSpecs to validate IRD messages. */
	private static final FieldSpec[] IRD_FIELD_SPECS = new FieldSpec[] {
		
		// meta:
		new FieldSpec(
			new SimpleKey(FN_META, false),
			new JSONValidate_Object(new FieldSpec[] {
				new FieldSpec(
					new SimpleKey(FN_COST_TYPES, false),
					new JSONValidate_Object(new FieldSpec[] {
						new FieldSpec(
							new RegexKey(AltoValidators.COST_TYPE_NAME_PAT),
							AltoValidators.EXTENDED_COST_TYPE),
						new FieldSpec(
							new RegexKey(".*"),
							new JSONValidate_Invalid("Invalid Cost Type Name")),
					})),
				new FieldSpec(
					new SimpleKey(FN_DEFAULT_NETWORK_MAP, false),
					AltoValidators.VALID_RESOURCE_ID),
			})
		),
	
		// resources:
		new FieldSpec(
			new SimpleKey(FN_RESOURCES, true),
			new JSONValidate_Object(new FieldSpec[] {
				new FieldSpec(
					new RegexKey(AltoValidators.RESOURCE_ID_PAT),
					new JSONValidate_Object(new FieldSpec[] {
						new FieldSpec(
							new SimpleKey(FN_URI, true),
							JSONValidate_String.STRING),
						new FieldSpec(
							new SimpleKey(FN_MEDIA_TYPE, true),
							new JSONValidate_String(AltoValidators.RESPONSE_MEDIA_TYPES,
													false, "%s is not a valid response media type")),
						new FieldSpec(
							new SimpleKey(FN_ACCEPTS, false),
							new JSONValidate_String(AltoValidators.REQUEST_MEDIA_TYPES,
													false, "%s is not a valid request media type")),
						new FieldSpec(
							new SimpleKey(FN_USES, false),
							AltoValidators.RESOURCE_ID_ARRAY),
						new FieldSpec(
							new SimpleKey(FN_CAPABILITIES, false),
							new JSONValidate_Object(new FieldSpec[] {
								new FieldSpec(
									new SimpleKey(AltoResp_CostMap.CAPABILITY_COST_TYPE_NAMES, false),
									AltoValidators.COST_TYPE_NAME_ARRAY),
								new FieldSpec(
									new SimpleKey(AltoResp_CostMap.CAPABILITY_COST_CONSTRAINTS, false),
									JSONValidate_Boolean.BOOLEAN),
								new FieldSpec(
									new SimpleKey(AltoResp_EndpointProp.CAPABILITY_PROP_TYPES, false),
									AltoValidators.PROPERTY_NAME_ARRAY),
							})
						)
					})
				),
				new FieldSpec(
					new RegexKey(".*"),
					new JSONValidate_Invalid("Invalid Resource ID")
				),
			})
		)
	};

	/**
	 * Return a new validator for IRD messages.
	 */
	@Override
	protected JSONValidate getValidator()
	{
		return new JSONValidate_Object(IRD_FIELD_SPECS);
	}
	
	private final static Map<String,String[]> OPTIONAL_ACCEPTS = new HashMap<String,String[]>();
	private final static Map<String,String[]> REQUIRED_ACCEPTS = new HashMap<String,String[]>();
	private final static Map<String,String[]> OPTIONAL_USES = new HashMap<String,String[]>();
	private final static Map<String2,String[]> CONDITIONAL_USES = new HashMap<String2,String[]>();
	private final static Map<String,String[]> REQUIRED_USES = new HashMap<String,String[]>();
	private final static Map<String,String[]> OPTIONAL_CAPS = new HashMap<String,String[]>();
	private final static Map<String,String[]> REQUIRED_CAPS = new HashMap<String,String[]>();
	
	static {
		OPTIONAL_ACCEPTS.put(AltoResp_NetworkMap.MEDIA_TYPE, new String[] {AltoReq_FilteredNetworkMap.MEDIA_TYPE});
		CONDITIONAL_USES.put(new String2(AltoResp_NetworkMap.MEDIA_TYPE, AltoReq_FilteredNetworkMap.MEDIA_TYPE),
						new String[] {AltoResp_NetworkMap.MEDIA_TYPE});

		OPTIONAL_ACCEPTS.put(AltoResp_CostMap.MEDIA_TYPE, new String[] {AltoReq_FilteredCostMap.MEDIA_TYPE});
		REQUIRED_USES.put(AltoResp_CostMap.MEDIA_TYPE, new String[] {AltoResp_NetworkMap.MEDIA_TYPE});
		REQUIRED_CAPS.put(AltoResp_CostMap.MEDIA_TYPE, new String[] {AltoResp_CostMap.CAPABILITY_COST_TYPE_NAMES});
		OPTIONAL_CAPS.put(AltoResp_CostMap.MEDIA_TYPE, new String[] {AltoResp_CostMap.CAPABILITY_COST_CONSTRAINTS});
		
		REQUIRED_ACCEPTS.put(AltoResp_EndpointCost.MEDIA_TYPE, new String[] {AltoReq_EndpointCostParams.MEDIA_TYPE});
		REQUIRED_CAPS.put(AltoResp_EndpointCost.MEDIA_TYPE, new String[] {AltoResp_CostMap.CAPABILITY_COST_TYPE_NAMES});
		OPTIONAL_CAPS.put(AltoResp_EndpointCost.MEDIA_TYPE, new String[] {AltoResp_CostMap.CAPABILITY_COST_CONSTRAINTS});

		REQUIRED_ACCEPTS.put(AltoResp_EndpointProp.MEDIA_TYPE, new String[] {AltoReq_EndpointPropParams.MEDIA_TYPE});
		REQUIRED_CAPS.put(AltoResp_EndpointProp.MEDIA_TYPE, new String[] {AltoResp_EndpointProp.CAPABILITY_PROP_TYPES});
	}
	
	/**
	 * Validate the JSON object using the validator supplied by the child class.
	 * @return A list of errors, or null if the message passed validation.
	 */
	@Override
	public List<String> validate()
	{
		return validate(m_json);
	}
	
	/**
	 * Validate the JSON object using the validator supplied by the child class.
	 * @param isRootIRD If true, validate this as a Root IRD.
	 * @return A list of errors, or null if the message passed validation.
	 */
	public List<String> validate(boolean isRootIRD)
	{
		return validate(m_json, isRootIRD);
	}
	
	/**
	 * Validate a JSON object as a IRD message.
	 * @param json The JSON Object to validate.
	 * @return A list of errors, or null if the message passed validation.
	 */
	public static List<String> validate(JSONValue_Object json)
	{
		return validate(json, false);
	}
	
	/**
	 * Validate a JSON object as a IRD message.
	 * @param json The JSON Object to validate.
	 * @param isRootIRD If true, validate this as a Root IRD.
	 * @return A list of errors, or null if the message passed validation.
	 */
	public static List<String> validate(JSONValue_Object json, boolean isRootIRD)
	{
		List<String> errors = validate(json, new JSONValidate_Object(IRD_FIELD_SPECS));
		if (errors == null) {
			errors = new ArrayList<String>();
		}
		
		// Additional validations: Verify all cost type names used in capabilities
		// are defined in meta, and verify that accepts, uses and capabilities
		// are legal for the mediaType.
		JSONValue_Object meta = json.getObject(FN_META, null);
		JSONValue_Object resources = json.getObject(FN_RESOURCES, null);
		
		String defNetworkMapId = (meta != null)
						? meta.getString(FN_DEFAULT_NETWORK_MAP, null) : null;
		JSONValue_Object defNetworkMap = (defNetworkMapId != null && resources != null)
						? resources.getObject(defNetworkMapId, null) : null;
		if (defNetworkMapId == null) {
			if (isRootIRD) {
				errors.add("IRD does not define a Default Network Map ID");
			}
		} else if (defNetworkMap == null) {
			errors.add("Default Network Map resource '" + defNetworkMapId
								+ "' does not exist in this IRD");
		} else if (!AltoResp_NetworkMap.MEDIA_TYPE.equals(defNetworkMap.getString(FN_MEDIA_TYPE, ""))) {
			errors.add("Default Network Map resource '" + defNetworkMapId
								+ "' is not a Network Map service");
		} else if (defNetworkMap.getString(FN_ACCEPTS, null) != null) {
			errors.add("Default Network Map resource '" + defNetworkMapId
								+ "' is not a Full Network Map service");
		}

		if (resources != null) {
			JSONValue_Object costTypeNameDictionary =
					(meta != null) ? meta.getObject(FN_COST_TYPES, null) : null;
			boolean gaveMissingCostTypesError = false;
			for (Map.Entry<String, JSONValue> resourceEntry: resources.entrySet()) {
				JSONValue resourceValue = resourceEntry.getValue();
				if (!(resourceValue instanceof JSONValue_Object)) {
					// The validator reported this error.
					continue;
				}
				String resourceId = resourceEntry.getKey();
				JSONValue_Object resource = (JSONValue_Object)resourceValue;
				String mediaType = resource.getString(FN_MEDIA_TYPE, "");
				String accepts = resource.getString(FN_ACCEPTS, null);
				JSONValue_Array uses = resource.getArray(FN_USES, null);
				JSONValue_Object caps = resource.getObject(FN_CAPABILITIES, null);
				
				testValue(errors, resourceId, FN_ACCEPTS,
							(accepts != null ? new String[] {accepts} : null),
							REQUIRED_ACCEPTS.get(mediaType), OPTIONAL_ACCEPTS.get(mediaType));
				
				if (uses == null || uses.size() == 0) {
					if (REQUIRED_USES.get(mediaType) != null) {
						errors.add("resourceId " + resourceId + ": missing 'uses' attribute(s)");
					}
				} else {
					String[] requiredTypes = REQUIRED_USES.get(mediaType);
					if (requiredTypes == null) {
						requiredTypes = g_emptyArray;
					}
					String[] conditionalTypes = null;
					if (accepts != null) {
						conditionalTypes = CONDITIONAL_USES.get(new String2(mediaType, accepts));
					}
					if (conditionalTypes == null) {
						conditionalTypes = g_emptyArray;
					}
					String[] optionalTypes = OPTIONAL_USES.get(mediaType);
					if (optionalTypes == null) {
						optionalTypes = g_emptyArray;
					}
					boolean usesAllowed = requiredTypes.length > 0 || conditionalTypes.length > 0 || optionalTypes.length > 0;
					for (JSONValue xval: uses) {
						if (xval instanceof JSONValue_String) {
							String id = ((JSONValue_String)xval).m_value;
							JSONValue_Object xres = resources.getObject(id, null);
							if (!usesAllowed) {
								errors.add("resourceId " + resourceId + ": illegal 'uses' resource " + id);
							} else if (xres != null) {
								String xtype = xres.getString(FN_MEDIA_TYPE, "");
								if (!(inArray(xtype, requiredTypes)
											|| inArray(xtype, conditionalTypes)
											|| inArray(xtype, optionalTypes))) {
									errors.add("resourceId " + resourceId + ": 'uses' resource " + id
											+ " has incorrect mediaType for this service");
								}
							}
						}
					}
					if (uses.size() == 0 && (requiredTypes.length > 0 || conditionalTypes.length > 0)) {
						errors.add("resourceId " + resourceId + ": missing 'uses' attribute");
					}
				}

				testValue(errors, resourceId, FN_CAPABILITIES,
						(caps != null ? caps.keyArray() : null),
						REQUIRED_CAPS.get(mediaType), OPTIONAL_CAPS.get(mediaType));
				
				if (caps != null) {
					JSONValue_Array costTypeNames
							= caps.getArray(AltoResp_CostMap.CAPABILITY_COST_TYPE_NAMES, null);
					if (costTypeNames != null) {
						for (JSONValue value: costTypeNames) {
							if (value instanceof JSONValue_String) {
								String costTypeName = ((JSONValue_String)value).m_value;
								if (costTypeNameDictionary == null) {
									if (!gaveMissingCostTypesError) {
										errors.add("Missing field: " + FN_COST_TYPES + " at " + FN_META);
										gaveMissingCostTypesError = true;
									}
								} else if (costTypeNameDictionary.getObject(costTypeName, null) == null) {
									errors.add("resourceId " + resourceId
											+ " has undefined cost-type-name '" + costTypeName + "'");
								}
							}
						}
					}
				}
			}
		}	
		
		return !errors.isEmpty() ? errors : null;
	}
	
	private static String[] g_emptyArray = new String[0];
	
	/**
	 * If the strings in "required" are not in "actual",
	 * or if some string in "actual" is not in either "required" or "optional",
	 * add an error message to "errors".
	 * "actual", "required" and "optional" may be null.
	 */
	private static void testValue(List<String> errors,
								  String resourceId,
								  String attribute,
								  String[] actual,
								  String[] required,
								  String[] optional)
	{
		if (required == null) {
			required = g_emptyArray;
		}
		if (optional == null) {
			optional = g_emptyArray;
		}
		for (String s: required) {
			if (actual == null || !inArray(s, actual)) {
				errors.add("resourceId " + resourceId
						+ ": missing '" + attribute + "' " + s);
			}
		}
		if (actual != null) {
			for (String s: actual) {
				if (!(inArray(s, required) || inArray(s, optional))) {
					errors.add("resourceId " + resourceId
							+ ": illegal '" + attribute + "' " + s);	
				}
			}
		}
	}
	
	/**
	 * Return true iff test is in arr. Test and arr must not be null.
	 */
	private static boolean inArray(String test, String[] arr)
	{
		for (String s: arr) {
			if (test.equals(s)) {
				return true;
			}
		}
		return false;
	}
}
