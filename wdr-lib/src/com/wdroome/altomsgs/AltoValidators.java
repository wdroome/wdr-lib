package com.wdroome.altomsgs;

import java.util.HashSet;
import java.util.Set;

import com.wdroome.util.inet.EndpointAddress;
import com.wdroome.json.validate.JSONValidate;
import com.wdroome.json.validate.JSONValidate_Array;
import com.wdroome.json.validate.JSONValidate_Object;
import com.wdroome.json.validate.JSONValidate_String;
import com.wdroome.json.validate.JSONValidate_Object.FieldSpec;
import com.wdroome.json.validate.JSONValidate_Object.IKeyTest;
import com.wdroome.json.validate.JSONValidate_Object.SimpleKey;
import com.wdroome.json.validate.JSONValidate_Object.RegexKey;

/**
 * Common validation specifications for all ALTO message classes.
 * @author wdr
 */
public class AltoValidators
{
	/** A list of legal cost modes. */
	public static final String[] COST_MODE_LIST	= new String[] {"numerical", "ordinal"};
	
	/** A list of IP address prefixes allowed in ALTO IP addresses. */
	public static final String[] IP_ADDR_PREFIX_LIST = new String[] {
		EndpointAddress.IPV4_PREFIX,
		EndpointAddress.IPV6_PREFIX,
	};
	
	/** A list of legal response media types. */
	public static final String[] RESPONSE_MEDIA_TYPES = new String[] {
		AltoResp_CostMap.MEDIA_TYPE,
		AltoResp_EndpointCost.MEDIA_TYPE,
		AltoResp_EndpointProp.MEDIA_TYPE,
		AltoResp_Error.MEDIA_TYPE,
		AltoResp_InfoResourceDir.MEDIA_TYPE,
		AltoResp_NetworkMap.MEDIA_TYPE,
	};
	
	/** A list of legal request media types. */
	public static final String[] REQUEST_MEDIA_TYPES = new String[] {
		AltoReq_EndpointCostParams.MEDIA_TYPE,
		AltoReq_EndpointPropParams.MEDIA_TYPE,
		AltoReq_FilteredCostMap.MEDIA_TYPE,
		AltoReq_FilteredNetworkMap.MEDIA_TYPE,
	};

	/** A regex pattern for legal cost-metric names. */
	public static final String COST_METRIC_PAT = "[-_:.a-zA-Z0-9]{1,32}";
	
	/** A regex pattern for legal PID names. */
	public static final String PID_NAME_PAT = "[-:@_.a-zA-Z0-9]{1,64}";

	/** A regex pattern for legal Resource ID names. */
	public static final String RESOURCE_ID_PAT = "[-:@_.a-zA-Z0-9]{1,64}";

	/** A regex pattern for legal Property names. */
	public static final String PROPERTY_NAME_PAT = "([-:@_.a-zA-Z0-9]{1,64}\\.)?[-:_a-zA-Z0-9]{1,64}";
	
	/** A regex pattern for printable, non-blank ascii strings, 1-64 charactors long. */
	public static final String NON_BLANK_ASCII_PAT_64 = "[\u0021-\u007e]{1,64}";
	
	/** A regex pattern for ipv4 addresses, with ipv4 prefix. */
	public static final String IPV4_ADDR_PAT = "ipv4:[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}";
	
	/** A regex pattern for ipv4 CIDRs. */
	public static final String IPV4_CIDR_PAT = "[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}/[0-9]{1,2}";
	
	/** A regex pattern for ipv6 addresses. Note this accepts some invalid ipv6 address strings. */
	public static final String IPV6_ADDR_PAT = "ipv6:[0-9a-fA-F:.]+";
	
	/** A regex pattern for ipv6 CIDR. Note this accepts some invalid ipv6 address strings. */
	public static final String IPV6_CIDR_PAT = "[0-9a-fA-F:.]+/[0-9]{1,3}";
	
	/** A regex pattern for an ipv4 or ipv6 addr string. The ipv4/ipv6 prefix is required. */
	public static final String IP_ADDR_PAT = "(" + IPV4_ADDR_PAT + ")|(" + IPV6_ADDR_PAT + ")";
	
	/** A regex pattern for cost constraint. */
	public static final String COST_CONSTRAINT_PAT = "(eq|le|lt|ge|gt)[ \t]+[0-9.]+";
	
	/** A validator for cost-mode names. */
	public static final JSONValidate VALID_COST_MODE
				= new JSONValidate_String(COST_MODE_LIST, false,
						"\"%s\" is not a valid cost-mode name");
	
	/** A validator for cost-metric names. */
	public static final JSONValidate VALID_COST_METRIC
				= new JSONValidate_String(COST_METRIC_PAT, false,
						"\"%s\" is not a valid cost-metric name");
	
	/** A validator for PID names. */
	public static final JSONValidate VALID_PID_NAME
				= new JSONValidate_String(PID_NAME_PAT, false,
						"\"%s\" is not a valid PID name");
	
	/** A validator for resource ids. */
	public static final JSONValidate VALID_RESOURCE_ID
				= new JSONValidate_String(RESOURCE_ID_PAT, false,
						"\"%s\" is not a legal resource-id");
	
	/** A validator for cost type names. */
	public static final JSONValidate VALID_COST_TYPE_NAME
				= new JSONValidate_String(RESOURCE_ID_PAT, false,
						"\"%s\" is not a legal cost-type name");
	
	/** A validator for tag strings. */
	public static final JSONValidate VALID_TAG
				= new JSONValidate_String(NON_BLANK_ASCII_PAT_64, false,
						"\"%s\" is not a valid tag");
	
	/** A validator for ipv4 CIDRs */
	public static final JSONValidate VALID_IPV4_CIDR
				= new JSONValidate_String(IPV4_CIDR_PAT, false,
						"\"%s\" is not a valid ipv4 CIDR");
	
	/** A validator for ipv6 CIDRs. */
	public static final JSONValidate VALID_IPV6_CIDR
				= new JSONValidate_String(IPV6_CIDR_PAT, false,
						"\"%s\" is not a valid ipv6 CIDR");
	
	/** A validator for ipv4 or ipv6 IP address strings (prefix required). */
	public static final JSONValidate VALID_IP_ADDR
				= new JSONValidate_IPAddress_String();
	
	/** A validator for ipv4 or ipv6 IP address keys (prefix required). */
	public static final IKeyTest VALID_IP_ADDR_KEY
				= new JSONValidate_IPAddress_Key(IP_ADDR_PREFIX_LIST);
	
	/** A validator for IP address prefixes. */
	public static final JSONValidate VALID_IP_ADDR_PREFIX
				= new JSONValidate_String(IP_ADDR_PREFIX_LIST, false,
						"\"%s\" is not a valid IP address prefix");

	/** A validator for an array of PID names. */
	public static final JSONValidate PID_NAME_ARRAY
				= new JSONValidate_Array(VALID_PID_NAME);

	/** A validator for an array of resource ids. */
	public static final JSONValidate RESOURCE_ID_ARRAY
				= new JSONValidate_Array(VALID_RESOURCE_ID);

	/** A validator for an array of cost type names. */
	public static final JSONValidate COST_TYPE_NAME_ARRAY
				= new JSONValidate_Array(VALID_COST_TYPE_NAME);
	
	/** A validator for an array of cost constraints. */
	public static final JSONValidate IP_ADDR_PREFIX_ARRAY
				= new JSONValidate_Array(VALID_IP_ADDR_PREFIX);
		
	/** A validator for an array of prefixed IPV4 or IPV6 addresses. */
	public static final JSONValidate IP_ADDR_ARRAY
				= new JSONValidate_Array(VALID_IP_ADDR);

	/** A validator for an array of property names. */
	public static final JSONValidate PROPERTY_NAME_ARRAY
				= new JSONValidate_Array(new JSONValidate_String(AltoValidators.PROPERTY_NAME_PAT));
	
	/** A validator for an array of IP address prefixes. */
	public static final JSONValidate COST_CONSTRAINT_ARRAY
				= new JSONValidate_Array(
						new JSONValidate_String(COST_CONSTRAINT_PAT, false,
								"\"%s\" is not a valid cost constraint"));

	/**
	 * Validator for a vtag: a dictionary with resource-id and tag fields.
	 */
	public static final JSONValidate VTAG
		= new JSONValidate_Object(new FieldSpec[] {
			new FieldSpec(
				new SimpleKey(AltoResp_Base.FN_RESOURCE_ID, true),
				VALID_RESOURCE_ID),
			new FieldSpec(
				new SimpleKey(AltoResp_Base.FN_TAG, true),
				VALID_TAG),
		});

	/**
	 * A validator for a cost-type: a dictionary with cost-mode and cost-metric fields.
	 */
	public static final JSONValidate COST_TYPE
		= new JSONValidate_Object(new FieldSpec[] {
			new FieldSpec(
				new SimpleKey(AltoResp_Base.FN_COST_MODE, true),
				VALID_COST_MODE),
			new FieldSpec(
				new SimpleKey(AltoResp_Base.FN_COST_METRIC, true),
				VALID_COST_METRIC),
		});

	/**
	 * A validator for an extended cost-type in an IRD:
	 * a dictionary with cost-mode and cost-metric fields,
	 * and an optional description field.
	 */
	public static final JSONValidate EXTENDED_COST_TYPE
		= new JSONValidate_Object(new FieldSpec[] {
			new FieldSpec(
				new SimpleKey(AltoResp_Base.FN_COST_MODE, true),
				VALID_COST_MODE),
			new FieldSpec(
				new SimpleKey(AltoResp_Base.FN_COST_METRIC, true),
				VALID_COST_METRIC),
			new FieldSpec(
				new SimpleKey("description", false),
				JSONValidate_String.STRING),
		});
		
	/**
	 * FieldSpec for a required "meta" with a vtag field.
	 */
	public static final FieldSpec META_VTAG
		= new FieldSpec(
			new SimpleKey(AltoResp_Base.FN_META, true),
			new JSONValidate_Object(new FieldSpec[] {
				new FieldSpec(
					new SimpleKey(AltoResp_Base.FN_VTAG, true),
					AltoValidators.VTAG),
			})
		);
	
	/**
	 * FieldSpec for an optional "meta" with a dependent-vtags field.
	 */
	public static final FieldSpec META_VTAGS_OPTIONAL
		= new FieldSpec(
			new SimpleKey(AltoResp_Base.FN_META, false),
			new JSONValidate_Object(new FieldSpec[] {
				new FieldSpec(
					new SimpleKey(AltoResp_Base.FN_DEPENDENT_VTAGS, false),
					new JSONValidate_Array(VTAG, 1)),
			})
		);
	
	/**
	 * FieldSpec for "meta" with a cost-type field.
	 */
	public static final FieldSpec META_COST_TYPE
		= new FieldSpec(
			new SimpleKey(AltoResp_Base.FN_META, true),
			new JSONValidate_Object(new FieldSpec[] {
				new FieldSpec(
					new SimpleKey(AltoResp_Base.FN_COST_TYPE, true),
					AltoValidators.COST_TYPE),
			})
		);
	
	/**
	 * FieldSpec for "meta" with cost-type and dependent-vtags fields.
	 */
	public static final FieldSpec META_COST_TYPE_VTAGS
		= new FieldSpec(
			new SimpleKey(AltoResp_Base.FN_META, true),
			new JSONValidate_Object(new FieldSpec[] {
				new FieldSpec(
					new SimpleKey(AltoResp_Base.FN_COST_TYPE, true),
					AltoValidators.COST_TYPE),
				new FieldSpec(
					new SimpleKey(AltoResp_Base.FN_DEPENDENT_VTAGS, true),
					new JSONValidate_Array(VTAG, 1)),
			})
		);
}
