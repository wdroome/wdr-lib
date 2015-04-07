package com.wdroome.altodata;

import java.util.Map;
import java.util.HashMap;

import com.wdroome.util.inet.CIDRAddress;

/**
 * A Property Map for ipv6 CIDRs.
 * This extends the base Property Map by allowing
 * "child" CIDRs to inherit properties from "parent" CIDRs
 * -- that is, a CIDR inherits properties from shorter
 * CIDRs with the same prefix.
 * <p>
 * The iterator methods --
 * {@link #getEntityNames(String)},
 * {@link #getProperties(IPropertyMap.PropValueCB, String)}
 * and {@link #getProperties(IPropertyMap.PropValueCB)}
 * -- ignore inheritance, and just return explicitly set properties.
 * 
 * @author wdr
 */
public class PropertyMap_CIDRv6 extends PropertyMap<CIDRAddress>
{
	/**
	 * For each CIDR with properties, this gives its immediate parent CIDR.
	 * That is, the longest CIDR that covers it.
	 * Note that if properties are deleted, this table may contain
	 * CIDRs that no longer have properties. That doesn't hurt;
	 * it just causes a slight inefficiency.
	 */
	private final Map<CIDRAddress,CIDRAddress> m_cidrParent = new HashMap<CIDRAddress,CIDRAddress>();
	
	public static final CIDRAddress ZERO_LEN_CIDR;
	
	static {
		CIDRAddress cidr = null;
		try {
			cidr = new CIDRAddress("::0/0", "ipv6");
		} catch (Exception e) {
		}
		ZERO_LEN_CIDR = cidr;
	}
	
	public PropertyMap_CIDRv6()
	{
		super(EntityMakers.ENT_TYPE_CIDRV6, EntityMakers.g_makeCIDR, false);
	}

	/* (non-Javadoc)
	 * @see PropertyMap#getProp(java.lang.Object, java.lang.String)
	 */
	@Override
	public synchronized String getProp(CIDRAddress cidr, String propName)
	{
		if (!cidr.isIPV6()) {
			throw new IllegalArgumentException("CIDR6PropertyMap.getProp(): CIDR is not v6");
		}
		String value = super.getProp(cidr, propName);

		if (value != null || super.propExists(cidr, propName)) {
			return value;
		}
		
		CIDRAddress parent = m_cidrParent.get(cidr);
		if (parent != null) {
			return getProp(parent, propName);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see com.wdroome.altodata.PropertyMap#setProp(java.lang.Object, java.lang.String, java.lang.String)
	 */
	@Override
	public synchronized String setProp(CIDRAddress cidr, String propName, String value)
	{
		if (!cidr.isIPV6()) {
			throw new IllegalArgumentException("CIDR6PropertyMap.setProp(): CIDR is not v6");
		}
		String prevValue = super.setProp(cidr, propName, value);
		
		// New CIDR. Find its parent, and save in m_cidrParent.
		// And find all of its immediate children, and set their parent to this cidr.
		// But we do not create an entry for the zero-length CIDR;
		// it has no parent, and it is the default parent of every CIDR.
		int cidrMaskLen = cidr.getMaskLen();
		if (cidrMaskLen > 0 && !m_cidrParent.containsKey(cidr)) {
			CIDRAddress parentCidr = ZERO_LEN_CIDR;
			for (Map.Entry<CIDRAddress,CIDRAddress> entry: m_cidrParent.entrySet()) {
				CIDRAddress entCidr = entry.getKey();
				int entCidrMaskLen = entCidr.getMaskLen();
				CIDRAddress parCidr = entry.getValue();
				if (entCidrMaskLen < cidrMaskLen
						&& entCidr.covers(cidr)
						&& entCidrMaskLen > parentCidr.getMaskLen()) {
					parentCidr = parCidr;
				} else if (cidr.covers(entCidr)
						&& cidrMaskLen < entCidrMaskLen
						&& cidrMaskLen > parCidr.getMaskLen()) {
					entry.setValue(cidr);
				}
			}
			m_cidrParent.put(cidr, parentCidr);
		}
		
		return prevValue;
	}
}
