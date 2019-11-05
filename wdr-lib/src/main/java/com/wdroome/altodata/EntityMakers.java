package com.wdroome.altodata;

import com.wdroome.util.inet.EndpointAddress;
import com.wdroome.util.inet.CIDRAddress;

/**
 * @author wdr
 */
public class EntityMakers
{
	public static final String ENT_TYPE_IPV4 = "ipv4";
	public static final String ENT_TYPE_IPV6 = "ipv6";
	public static final String ENT_TYPE_CIDRV4 = "cidrv4";
	public static final String ENT_TYPE_CIDRV6 = "cidrv6";
	public static final String ENT_TYPE_PID = "pid";
	public static final String ENT_TYPE_ANE = "ane";
	
	public static class MakeEndAddr implements IPropertyMap.MakeEntity<EndpointAddress>
	{
		public EndpointAddress makeEntity(String entity, String type)
		{
			try {
				return new EndpointAddress(entity, type.equals(ENT_TYPE_IPV6)
											? EndpointAddress.IPV6_PREFIX
											: EndpointAddress.IPV4_PREFIX);
			} catch (Exception e) {
				throw new IllegalArgumentException("Invalid " + type + ": '"
													+ entity + "'", e);
			}
		}
	}

	public static class MakeCIDR implements IPropertyMap.MakeEntity<CIDRAddress>
	{
		public CIDRAddress makeEntity(String entity, String type)
		{
			try {
				return new CIDRAddress(entity, type.equals(ENT_TYPE_CIDRV6)
											? EndpointAddress.IPV6_PREFIX
											: EndpointAddress.IPV4_PREFIX);
			} catch (Exception e) {
				throw new IllegalArgumentException("Invalid " + type + ": '"
													+ entity + "'", e);
			}
		}
	}
	
	public static final MakeEndAddr g_makeEndAddr = new MakeEndAddr();
	public static final MakeCIDR g_makeCIDR = new MakeCIDR();
}
