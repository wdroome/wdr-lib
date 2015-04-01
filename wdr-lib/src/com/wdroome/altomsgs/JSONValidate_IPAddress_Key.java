package com.wdroome.altomsgs;

import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.wdroome.json.JSONValue_String;
import com.wdroome.json.validate.JSONValidate_Object.IKeyTest;
import com.wdroome.util.inet.EndpointAddress;
import com.wdroome.util.StringUtils;

/**
 * A IP Address test on the key name.
 * @author wdr
 */
public class JSONValidate_IPAddress_Key implements IKeyTest
{
	private final Set<String> m_requiredPrefixes;
	
	private static final Set<String> g_ipv4ipv6 = new HashSet<String>();
	static {
			g_ipv4ipv6.add(EndpointAddress.IPV4_PREFIX);
			g_ipv4ipv6.add(EndpointAddress.IPV6_PREFIX);
	}

	/**
	 * Verify that this JSON key is an IPV4 or IPV6 address, with a prefix.
	 */
	public JSONValidate_IPAddress_Key()
	{
		this(g_ipv4ipv6);
	}

	/**
	 * Verify that this JSON key is an IP address,
	 * and optionally restrict the address type.
	 * @param requiredPrefixes A list of IP address type prefixes
	 * 		(see {@link EndpointAddress#IPV4_PREFIX}, etc).
	 * 		If not null, the address must start	with one of these prefixes.
	 * 		If null, accept any IP address type,
	 * 		and if the prefix is missing, infer the address type.
	 * @see EndpointAddress
	 */
	public JSONValidate_IPAddress_Key(String[] requiredPrefixes)
	{
		this(StringUtils.makeSet(requiredPrefixes));
	}

	/**
	 * Verify that this JSON key is an IP address.
	 * @param requiredPrefixes A set of IP address type prefixes
	 * 		(see {@link EndpointAddress#IPV4_PREFIX}, etc).
	 * 		If not null, the address must start	with one of these prefixes.
	 * 		If null, accept any IP address type,
	 * 		and if the prefix is missing, infer the address type.
	 * @see EndpointAddress
	 */
	public JSONValidate_IPAddress_Key(Set<String> requiredPrefixes)
	{
		m_requiredPrefixes = requiredPrefixes;
	}
	
	/** @see IKeyTest#keyMatches(String) */
	public boolean keyMatches(String key)
	{
		try {
			new EndpointAddress(key, m_requiredPrefixes);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	/** @see IKeyTest#requiredKeys() */
	public List<String> requiredKeys()
	{
		return null;
	}
}
