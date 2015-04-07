package com.wdroome.altodata;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

import com.wdroome.util.inet.CIDRAddress;
import com.wdroome.util.inet.CIDRSet;

/**
 * @author wdr
 */
public class ProperyMap_pid implements IPropertyMap<String>
{
	private final NetworkMap m_networkMap;
	private final PropertyMap_CIDRv4 m_cidr4Props;
	private final PropertyMap_CIDRv6 m_cidr6Props;
	
	public ProperyMap_pid(NetworkMap networkMap,
						  PropertyMap_CIDRv4 cidr4Props,
						  PropertyMap_CIDRv6 cidr6Props)
	{
		m_networkMap = networkMap;
		if (cidr4Props == null) {
			cidr4Props = new PropertyMap_CIDRv4();
		}
		m_cidr4Props = cidr4Props;
		if (cidr6Props == null) {
			cidr6Props = new PropertyMap_CIDRv6();
		}
		m_cidr6Props = cidr6Props;
	}
	
	/* (non-Javadoc)
	 * @see IPropertyMap#getEntityType()
	 */
	@Override
	public String getEntityType()
	{
		return EntityMakers.ENT_TYPE_PID;
	}

	/* (non-Javadoc)
	 * @see IPropertyMap#makeEntity(java.lang.String)
	 */
	@Override
	public String makeEntity(String str) throws IllegalArgumentException
	{
		return str;
	}

	/* (non-Javadoc)
	 * @see IPropertyMap#getTypedName(java.lang.Object)
	 */
	@Override
	public String getTypedName(String entity)
	{
		return getEntityType() + ":" + entity.toString();
	}

	/* (non-Javadoc)
	 * @see IPropertyMap#getProp(java.lang.Object, java.lang.String)
	 */
	@Override
	public String getProp(String pid, String propName)
	{
		String value = null;
		boolean first = true;
		CIDRSet cidrs = m_networkMap.getCIDRs(pid);
		if (cidrs != null) {
			for (CIDRAddress cidr : cidrs) {
				IPropertyMap<CIDRAddress> cidrMap = cidr.isIPV6() ? m_cidr6Props : m_cidr4Props;
				String tvalue = cidrMap.getProp(cidr, propName);
				if (first) {
					value = tvalue;
					first = false;
				} else if (value == null && tvalue == null) {
					continue;
				} else if (!value.equals(tvalue)) {
					return null;
				}
			}
		}
		return value;
	}

	/* (non-Javadoc)
	 * @see IPropertyMap#propExists(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean propExists(String pid, String propName)
	{
		return getProp(pid, propName) != null;
	}

	/* (non-Javadoc)
	 * @see IPropertyMap#setProp(java.lang.Object, java.lang.String, java.lang.String)
	 */
	@Override
	public String setProp(String pid, String propName, String value)
	{
		String prevValue = null;
		boolean first = true;
		CIDRSet cidrs = m_networkMap.getCIDRs(pid);
		if (cidrs != null) {
			for (CIDRAddress cidr : cidrs) {
				IPropertyMap<CIDRAddress> cidrMap = cidr.isIPV6() ? m_cidr6Props : m_cidr4Props;
				String tvalue = cidrMap.getProp(cidr, propName);
				if (first) {
					prevValue = tvalue;
					first = false;
				} else if (prevValue == null && tvalue == null) {
					continue;
				} else if (!prevValue.equals(tvalue)) {
					prevValue = null;
					break;
				}
			}
			for (CIDRAddress cidr : cidrs) {
				IPropertyMap<CIDRAddress> cidrMap = cidr.isIPV6() ? m_cidr6Props : m_cidr4Props;		
				cidrMap.setProp(cidr, propName, value);
			}
		}
		return prevValue;
	}

	/* (non-Javadoc)
	 * @see IPropertyMap#removeProp(java.lang.Object, java.lang.String)
	 */
	@Override
	public String removeProp(String pid, String propName)
	{
		String prevValue = null;
		boolean first = true;
		CIDRSet cidrs = m_networkMap.getCIDRs(pid);
		if (cidrs != null) {
			for (CIDRAddress cidr : cidrs) {
				IPropertyMap<CIDRAddress> cidrMap = cidr.isIPV6() ? m_cidr6Props : m_cidr4Props;
				String tvalue = cidrMap.getProp(cidr, propName);
				if (first) {
					prevValue = tvalue;
					first = false;
				} else if (prevValue == null && tvalue == null) {
					continue;
				} else if (!prevValue.equals(tvalue)) {
					prevValue = null;
					break;
				}
			}
			for (CIDRAddress cidr : cidrs) {
				IPropertyMap<CIDRAddress> cidrMap = cidr.isIPV6() ? m_cidr6Props : m_cidr4Props;		
				cidrMap.removeProp(cidr, propName);
			}
		}
		return prevValue;
	}

	/* (non-Javadoc)
	 * @see IPropertyMap#getPropNames()
	 */
	@Override
	public Set<String> getPropNames()
	{
		Set<String> v4Props = m_cidr4Props.getPropNames();
		Set<String> v6Props = m_cidr6Props.getPropNames();
		if (v6Props.isEmpty()) {
			return v4Props;
		} else if (v4Props.isEmpty()) {
			return v6Props;
		} else {
			HashSet<String> names = new HashSet<String>(v4Props.size() + v6Props.size());
			names.addAll(v4Props);
			names.addAll(v6Props);
			return names;
		}
	}

	/* (non-Javadoc)
	 * @see IPropertyMap#getEntityNames(java.lang.String)
	 */
	@Override
	public List<String> getEntityNames(String propName)
	{
		HashSet<String> pids = new HashSet<String>();
		for (CIDRAddress cidr : m_cidr4Props.getEntityNames(propName)) {
			String pid = m_networkMap.getPID(cidr);
			if (pid != null) {
				pids.add(pid);
			}
		}
		for (CIDRAddress cidr : m_cidr6Props.getEntityNames(propName)) {
			String pid = m_networkMap.getPID(cidr);
			if (pid != null) {
				pids.add(pid);
			}
		}
		return new ArrayList<String>(pids);
	}

	/* (non-Javadoc)
	 * @see IPropertyMap#getProperties(IPropertyMap.PropValueCB, java.lang.String)
	 */
	@Override
	public boolean getProperties(PropValueCB<String> propValueCB,
			String propName)
	{
		// First create "pids" as a Set of pids that MIGHT have this property.
		// The assumption is that "pids" will be much smaller than
		// the set of all pids.
		HashSet<String> pids = new HashSet<String>();
		for (CIDRAddress cidr : m_cidr4Props.getEntityNames(propName)) {
			String pid = m_networkMap.getPID(cidr);
			if (pid != null) {
				pids.add(pid);
			}
		}
		for (CIDRAddress cidr : m_cidr6Props.getEntityNames(propName)) {
			String pid = m_networkMap.getPID(cidr);
			if (pid != null) {
				pids.add(pid);
			}
		}
		
		// Now test those PIDs.
		for (String pid: pids) {
			String value = getProp(pid, propName);
			if (value != null) {
				if (!propValueCB.propValue(pid, propName, value)) {
					return false;
				}
			}
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see IPropertyMap#getProperties(IPropertyMap.PropValueCB)
	 */
	@Override
	public boolean getProperties(PropValueCB<String> propValueCB)
	{
		for (String propName: getPropNames()) {
			if (!getProperties(propValueCB, propName)) {
				return false;
			}
		}
		return true;
	}
}
