package com.wdroome.altodata;

import java.net.UnknownHostException;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;

import com.wdroome.util.inet.EndpointAddress;
import com.wdroome.util.inet.EndpointString;
import com.wdroome.util.inet.CIDRAddress;
import com.wdroome.util.inet.CIDRString;

import com.wdroome.altomsgs.AltoResp_EndpointProp;

/**
 * A table of endpoint based properties. Lookup is by endpoint and name.
 * You set properties by endpoint and name, or by CIDR and name. 
 * Setting a property by CIDR sets it for all endpoints under that CIDR,
 * unless set by a longer CIDR or by the endpoint.
 * All operations are synchronized, so this class is thread-safe.
 * 
 * @author wdr
 */
public class EndpointPropertyTable
{
	private final Map<EndpointString, String> m_endpointProps = new HashMap<EndpointString, String>();
	private final Map<CIDRString, String> m_cidrProps = new HashMap<CIDRString, String>();
	
	private final Set<String> m_propNames = new HashSet<String>();
	private boolean m_recalcPropNames = false;
	
	private final TreeSet<CIDRAddress> m_cidrs = new TreeSet<CIDRAddress>();
	private final Map<CIDRAddress, CIDRAddress> m_cidrParents = new HashMap<CIDRAddress, CIDRAddress>();
	private boolean m_recalcCidrParents = false;

	/**
	 * Return the value for an endpoint, or if there's no value for the specific endpoint,
	 * the value for the longest CIDR that covers the endpoint.
	 * @param addr
	 * @param name
	 * @return The value for property "name" for address "addr."
	 * 		Return null if no value is specified for that property.
	 */
	public synchronized String getProp(EndpointAddress addr, String name)
	{
		String value = m_endpointProps.get(new EndpointString(addr, name));
		if (value != null)
			return value;
		
		setupCidrParents();
		
		// Set cidr to the longest cidr covering addr.
		// Because m_cidrs is a TreeSet, that's the first one that covers it.
		CIDRAddress cidr = null;
		for (CIDRAddress xcidr: m_cidrs) {
			if (addr.isContainedIn(xcidr)) {
				cidr = xcidr;
				break;
			}
		}
		
		// Now try cidr, and then walk up the cidr parent tree
		// until we find a value for this property.
		for (; cidr != null; cidr = m_cidrParents.get(cidr)) {
			if ((value = m_cidrProps.get(new CIDRString((CIDRAddress)cidr, name))) != null) {
				return value;
			}	
		}
		return null;
	}

	/**
	 * Create the m_cidrParent map if needed.
	 * The caller must synchronize on this object.
	 * Note this method may take a while to complete.
	 */
	private void setupCidrParents()
	{
		if (m_recalcCidrParents) {
			m_cidrs.clear();
			m_cidrParents.clear();
			
			// First we create a TreeSet with all distinct CIDRs.
			for (CIDRString cidrString: m_cidrProps.keySet()) {
				m_cidrs.add(cidrString.m_cidr);
			}
			
			// Now map each CIDR with a value to the longest CIDR that covers it.
			for (CIDRAddress cidr: m_cidrs) {
				CIDRAddress longestParent = null;
				for (CIDRAddress xcidr: m_cidrs) {
					if (xcidr != cidr && xcidr.covers(cidr)) {
						if (longestParent == null || xcidr.getMaskLen() > longestParent.getMaskLen()) {
							longestParent = xcidr;
						}
					}
				}
				if (longestParent != null) {
					m_cidrParents.put(cidr, longestParent);
				}
			}
			
			m_recalcCidrParents = false;
		}
	}

	/**
	 * Create the m_propNames set if needed.
	 * The caller must synchronize on this object.
	 */
	private void setupPropNames()
	{
		if (m_recalcPropNames) {
			m_propNames.clear();
			for (EndpointString endpointString: m_endpointProps.keySet()) {
				m_propNames.add(endpointString.m_string);
			}
			for (CIDRString cidrString: m_cidrProps.keySet()) {
				m_propNames.add(cidrString.m_string);
			}
			m_recalcPropNames = false;
		}
	}
	
	/**
	 * A struct with a property name, value, and an Endpoint or CIDR address.
	 */
	public static class PropValue
	{
		// The endpoint address. Null iff m_cidr != null.
		public final EndpointAddress m_addr;
		
		// The CIDR address. Null iff m_addr != null.
		public final CIDRAddress m_cidr;
		
		// The property name.
		public final String m_name;
		
		// The property value.
		public final String m_value;
		
		public PropValue(EndpointAddress addr, String name, String value)
		{
			m_addr = addr;
			m_cidr = null;
			m_name = name;
			m_value = value;
		}
		
		public PropValue(CIDRAddress cidr, String name, String value)
		{
			m_addr = null;
			m_cidr = cidr;
			m_name = name;
			m_value = value;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString()
		{
			return "PropValue[" + (m_addr != null ? m_addr : "")
						+ (m_cidr != null ? m_cidr : "")
						+ "," + m_name + "," + m_value + "]";
		}
	}
	
	/**
	 * Return a List with all property values, including values set for CIDRs.
	 * @param resourceId
	 * 		If not null, prefix every property name with this resource id.
	 * @return
	 * 		A new List with all property values.
	 */
	public synchronized List<PropValue> getAllProps(String resourceId)
	{
		ArrayList<PropValue> props = new ArrayList<PropValue>(m_endpointProps.size() + m_cidrProps.size());
		for (Map.Entry<EndpointString,String> ent: m_endpointProps.entrySet()) {
			props.add(new PropValue(ent.getKey().m_endpoint,
							AltoResp_EndpointProp.makePropName(resourceId, ent.getKey().m_string),
							ent.getValue()));
		}
		for (Map.Entry<CIDRString,String> ent: m_cidrProps.entrySet()) {
			props.add(new PropValue(ent.getKey().m_cidr,
							AltoResp_EndpointProp.makePropName(resourceId, ent.getKey().m_string),
							ent.getValue()));
		}
		return props;
	}
	
	/**
	 * Return a list of all property names.
	 */
	public synchronized List<String> getPropNames()
	{
		setupPropNames();
		return new ArrayList<String>(m_propNames); 
	}
	
	/**
	 * Return the number of entries in the table.
	 * @return The number of entries in the table.
	 */
	public synchronized int size()
	{
		return m_endpointProps.size() + m_cidrProps.size();
	}
	
	/**
	 * Set a property for either an endpoint address or a CIDR.
	 * @param addr A CIDR (if it sends in /##) or else an endpoint address.
	 * @param name The property name.
	 * @param value The value. Null removes the property value for this address.
	 * @throws UnknownHostException If addr isn't a valid CIDR or address.
	 */
	public void setProp(String addr, String name, String value)
				throws UnknownHostException
	{
		if (CIDRAddress.isCIDRFormat(addr)) {
			setProp(new CIDRAddress(addr), name, value);
		} else {
			setProp(new EndpointAddress(addr), name, value);
		}
	}
	
	/**
	 * Set a property for an endpoint address.
	 * @param addr The endpoint address.
	 * @param name The property name.
	 * @param value The value. Null removes the property value for this endpoint.
	 */
	public synchronized void setProp(EndpointAddress addr, String name, String value)
	{
		EndpointString key = new EndpointString(addr, name);
		if (value != null && !value.equals("")) {
			if (m_endpointProps.put(key, value) == null) {
				m_propNames.add(name);
				m_recalcCidrParents = true;
			}
		} else {
			if (m_endpointProps.remove(key) == null) {
				m_recalcCidrParents = true;
				m_recalcPropNames = true;
			}
		}
	}
	
	/**
	 * Set a property for a CIDR. All endpoints under the cidr
	 * will inherit this property value, unless overriden by a longer CIDR
	 * or by the endpoint itself.
	 * @param cidr The CIDR address.
	 * @param name The property name.
	 * @param value The value. Null removes the property value for this CIDR.
	 */
	public synchronized void setProp(CIDRAddress cidr, String name, String value)
	{
		CIDRString key = new CIDRString(cidr, name);
		if (value != null && !value.equals("")) {
			if (m_cidrProps.put(key, value) == null) {
				m_propNames.add(name);
				m_recalcCidrParents = true;
			}
		} else {
			if (m_cidrProps.remove(key) == null) {
				m_recalcCidrParents = true;
				m_recalcPropNames = true;
			}
		}
	}
	
	/**
	 * If you call this method after setting a group of properties,
	 * the class will do some possibly lengthy "housekeeping" operations
	 * at this time, rather than the next time you do a lookup.
	 * This is purely for optimization; the class will work perfectly
	 * if you do not call this method explicitly.
	 */
	public synchronized void finishUpdates()
	{
		setupCidrParents();
		setupPropNames();
	}
}
