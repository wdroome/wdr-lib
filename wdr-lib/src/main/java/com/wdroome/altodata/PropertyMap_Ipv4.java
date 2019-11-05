package com.wdroome.altodata;

import java.util.HashSet;
import java.util.Set;

import com.wdroome.util.inet.EndpointAddress;
import com.wdroome.util.inet.CIDRAddress;

/**
 * Implement a property map for IPV4 Endpoint Addresses.
 * The map is optionally coupled with a CIDR property map,
 * and endpoints inherit properties from CIDRs if needed.
 * @author wdr
 */
public class PropertyMap_Ipv4 extends PropertyMap<EndpointAddress>
{
	private final PropertyMap_CIDRv4 m_cidrMap;
	
	/**
	 * Create a new Endpoint Property Map.
	 * @param cidrMap
	 * 		A CIDR property map. An endpoint can inherit properties
	 * 		from the cidr that covers it.
	 */
	public PropertyMap_Ipv4(PropertyMap_CIDRv4 cidrMap)
	{
		super(EntityMakers.ENT_TYPE_IPV4, EntityMakers.g_makeEndAddr, false);
		m_cidrMap = cidrMap;
	}
	
	/* (non-Javadoc)
	 * @see PropertyMap#getProp(java.lang.Object, java.lang.String)
	 */
	@Override
	public synchronized String getProp(EndpointAddress addr, String propName)
	{
		if (!addr.isIPV4()) {
			throw new IllegalArgumentException("PropertyMap_Ipv4.getProp(): addr is not v4");
		}
		String value = super.getProp(addr, propName);
		if (value != null || super.propExists(addr, propName)) {
			return value;
		}
		
		byte[] rawBits = addr.getAddress();
		for (int len = rawBits.length; len >= 0; --len) {
			try {
				CIDRAddress cidr = new CIDRAddress(rawBits, len);
				value = m_cidrMap.getProp(cidr, propName);
				if (value != null || m_cidrMap.propExists(cidr, propName)) {
					return value;
				}
			} catch (Exception e) {
				// Shouldn't happen ...
				return null;
			}
		}
		
		return null;
	}

	/* (non-Javadoc)
	 * @see cPropertyMap#getPropNames()
	 */
	@Override
	public synchronized Set<String> getPropNames()
	{
		Set<String> addrProps = super.getPropNames();
		Set<String> cidrProps = (m_cidrMap != null) ? m_cidrMap.getPropNames() : null;
		if (cidrProps == null || cidrProps.isEmpty()) {
			return addrProps;
		} else if (cidrProps != null && addrProps.isEmpty()) {
			return cidrProps;
		} else {
			HashSet<String> names = new HashSet<String>(addrProps.size() + cidrProps.size());
			names.addAll(addrProps);
			names.addAll(cidrProps);
			return names;
		}
	}

	/* (non-Javadoc)
	 * @see PropertyMap#setProp(Object, String, String)
	 */
	@Override
	public synchronized String setProp(EndpointAddress addr, String propName, String value)
	{
		if (!addr.isIPV4()) {
			throw new IllegalArgumentException("PropertyMap_Ipv4.setProp(): addr is not v4");
		}
		return super.setProp(addr, propName, value);
	}
}
