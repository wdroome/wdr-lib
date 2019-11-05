package com.wdroome.altomsgs;

import java.util.Set;
import java.util.HashSet;

import java.net.UnknownHostException;

import com.wdroome.json.*;
import com.wdroome.json.validate.*;
import com.wdroome.util.inet.EndpointAddress;
import com.wdroome.util.StringUtils;

/**
 * Validate a JSON string that contains an IP address.
 * @author wdr
 */
public class JSONValidate_IPAddress_String extends JSONValidate
{
	private final Set<String> m_requiredPrefixes;
	
	private static final Set<String> g_ipv4ipv6 = new HashSet<String>();
	static {
			g_ipv4ipv6.add(EndpointAddress.IPV4_PREFIX);
			g_ipv4ipv6.add(EndpointAddress.IPV6_PREFIX);
	}
	
	/**
	 * Verify that this JSON value is an IPV4 or IPV6 address, with a prefix.
	 */
	public JSONValidate_IPAddress_String()
	{
		this(g_ipv4ipv6);
	}

	/**
	 * Verify that this JSON value is an IP address,
	 * and optionally restrict the address type.
	 * @param requiredPrefixes A list of IP address type prefixes
	 * 		(see {@link EndpointAddress#IPV4_PREFIX}, etc).
	 * 		If not null, the address must start	with one of these prefixes.
	 * 		If null, accept any IP address type,
	 * 		and if the prefix is missing, infer the address type.
	 * @see EndpointAddress
	 */
	public JSONValidate_IPAddress_String(String[] requiredPrefixes)
	{
		this(StringUtils.makeSet(requiredPrefixes));
	}

	/**
	 * Verify that this JSON value is an IP address.
	 * @param requiredPrefixes A set of IP address type prefixes
	 * 		(see {@link EndpointAddress#IPV4_PREFIX}, etc).
	 * 		If not null, the address must start	with one of these prefixes.
	 * 		If null, accept any IP address type,
	 * 		and if the prefix is missing, infer the address type.
	 * @see EndpointAddress
	 */
	public JSONValidate_IPAddress_String(Set<String> requiredPrefixes)
	{
		super(JSONValue_String.class, false);
		m_requiredPrefixes = requiredPrefixes;
	}
	
	/**
	 * See {@link JSONValidate#validate(JSONValue,String)}.
	 */
	@Override
	public boolean validate(JSONValue value, String path) throws JSONValidationException
	{
		if (!super.validate(value, path)) {
			return false;
		}
		boolean valid = true;
		
		// super() verifies that next test is always true,
		// but java doesn't know that.
		if (value instanceof JSONValue_String) {
			String svalue = ((JSONValue_String)value).m_value;
			try {
				new EndpointAddress(svalue, m_requiredPrefixes);
			} catch (Exception e) {
				handleValidationError("Invalid IP address '" + svalue + "'" + atPath(path));
				valid = false;
			}
		}
		return valid;
	}
}
