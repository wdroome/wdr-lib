package com.wdroome.altodata;

import java.io.File;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

import org.xml.sax.SAXParseException;

import com.wdroome.altomsgs.AltoResp_EndpointProp;
import com.wdroome.util.inet.EndpointAddress;
import com.wdroome.util.String2;
import com.wdroome.util.String3;

/**
 * The repository of the Network Maps, Cost Maps and Property Maps
 * for an ALTO server.
 * @author wdr
 */
public class AltoData
{
	/**
	 * If true, use the PID property values defined in the default network map
	 * as defaults for unqualified property references.
	 */
	public static final boolean USE_DEFAULT_NETWORK_MAP_PID_PROPERTIES = true;
	
	/**
	 * Private struct defining one network map and the cost and other data
	 * associated with it.
	 */
	private class NetworkGroup
	{
		/** The network map. Always frozen, never null. */
		private NetworkMap m_networkMap;
		
		/** The cost maps. The key is the cost metric. */
		private final Map<String, CostMap>	m_costMaps = new HashMap<String, CostMap>();
		
		/** The per-PID properties. The key is <pid-name, prop-leaf-name>. */
		private final Map<String2, String> m_pidProps = new HashMap<String2, String>();
		
		/**
		 * The property values for specific endpoints (and CIDRs).
		 * We use the "leaf" name of the property -- not qualified by "map-id.".
		 * If there is no value in this table, check m_pidProps for pid-wide defaults.
		 * Note that we do not use this table the the "pid" property;
		 * the NetworkMap object gives the pid for an endpoint.
		 */
		private EndpointPropertyTable m_endpointProps = new EndpointPropertyTable();

		private NetworkGroup(NetworkMap networkMap)
		{
			if (!networkMap.isFrozen()) {
				networkMap.freeze();
			}
			m_networkMap = networkMap;
		}
		
		private synchronized NetworkMap getNetworkMap()
		{
			return m_networkMap;
		}
		
		private synchronized void changeNetworkMap(NetworkMap networkMap)
		{
			m_networkMap = networkMap;
			for (CostMap costMap: m_costMaps.values()) {
				costMap.setNetworkMap(networkMap);
			}
		}
		
		private synchronized CostMap getCostMap(String costMetric)
		{
			return m_costMaps.get(costMetric);
		}
		
		private synchronized void setCostMap(CostMap costMap)
		{
			m_costMaps.put(costMap.getCostMetric(), costMap);
		}
		
		private synchronized Set<String> getCostMetrics()
		{
			// Return copy of key set, to avoid synchronization problems.
			return new HashSet<String>(m_costMaps.keySet());
		}
		
		private synchronized List<EndpointPropertyTable.PropValue> getEndpointProps()
		{
			return m_endpointProps.getAllProps(m_networkMap.getMapId());
		}
		
		private synchronized List<String3> getPidProps()
		{
			List<String3> props = new ArrayList<String3>();
			String id = m_networkMap.getMapId();
			for (Map.Entry<String2, String> entry : m_pidProps.entrySet()) {
				String2 key = entry.getKey();
				props.add(new String3(
							EndpointAddress.PID_PREFIX + EndpointAddress.PREFIX_SEP + key.m_str1,
							AltoResp_EndpointProp.makePropName(id, key.m_str2),
							entry.getValue()));
			}
			return props;
		}
	}
	
	// If not null, the valid Network Map resource ids.
	private final Set<String> m_validNetworkMapIds;
	
	// Network Map table. Key is resource id of a network map.
	private final Map<String, NetworkGroup> m_networkGroups = new HashMap<String, NetworkGroup>();
	
	// Global endpoint properties.
	private final EndpointPropertyTable m_endpointProps = new EndpointPropertyTable();
	
	// List of all distinct property names,
	// including resource-specific as well as global names.
	private final HashSet<String> m_propNames = new HashSet<String>();
	
	private String m_defaultNetworkMapId = null;
	
	/**
	 * Create a new AltoData.
	 * @param validNetworkMapIds
	 * 		If not null, refuse to add any Network Map
	 * 		whose resource id is not in this set,
	 */
	public AltoData(Set<String> validNetworkMapIds)
	{
		m_validNetworkMapIds = validNetworkMapIds;
	}
	
	/**
	 * Return the Network Map for a resource id.
	 * @param id The resource id of a Network Map.
	 * @return The Network Map with the resource id, or null.
	 */
	public NetworkMap getNetworkMap(String id)
	{
		NetworkGroup group = m_networkGroups.get(id);
		if (group == null) {
			return null;
		}
		return group.m_networkMap;
	}
	
	/**
	 * Create or change a Network Map.
	 * @param networkMap
	 * 		The new Network Map.
	 * @throws IllegalArgumentException
	 * 		If the resource id is not in the set of valid Network Map ids
	 * 		(see {@link #AltoData(Set)}).
	 */
	public synchronized void setNetworkMap(NetworkMap networkMap)
	{
		String id = networkMap.getMapId();
		if (m_validNetworkMapIds != null && !m_validNetworkMapIds.contains(id)) {
			throw new IllegalArgumentException("'" + id
								+ "' is not a valid network map resource id");					
		}
		if (m_networkGroups.containsKey(id)) {
			m_networkGroups.get(id).changeNetworkMap(networkMap);
		} else {
			m_networkGroups.put(id, new NetworkGroup(networkMap));
		}
	}
	
	/**
	 * Set the resource id of the default Network Map.
	 * 
	 * @param networkMapId
	 * 		The resource id of the default Network Map.
	 * 		If null, there is no default.
	 */
	public synchronized void setDefaultNetworkMapId(String networkMapId)
	{
		m_defaultNetworkMapId = networkMapId;
	}
	
	/**
	 * Return the resource ids of all Network Maps.
	 * @return A Set with the Network Map resource ids.
	 */
	public synchronized Set<String> networkMapIds()
	{
		// Return copy of key set, to avoid synchronization problems.
		return new HashSet<String>(m_networkGroups.keySet());
	}
	
	/**
	 * Return the resource id of the default Network Map.
	 * Note that this may be null, for no default Network Map,
	 * and there might not be a Network Map with this id.
	 * @return The resource id of the default Network Map, or null.
	 */
	public String getDefaultNetworkMapId()
	{
		return m_defaultNetworkMapId;
	}
	
	/**
	 * Return the NetworkGroup for a Network Map resource id.
	 * @param networkMapId
	 * 		The resource id of the Network Map.
	 * 		If null or "", use the default Network Map, if it exists.
	 * @return
	 * 		The NetworkGroup for networkMapId, or null if undefined.
	 */
	private synchronized NetworkGroup getGroup(String networkMapId)
	{
		if (networkMapId == null || networkMapId.equals("")) {
			networkMapId = m_defaultNetworkMapId;
		}
		if (networkMapId == null) {
			return null;
		}
		return m_networkGroups.get(networkMapId);
	}
	
	/**
	 * Return the Cost Map for a Network Map and cost metric.
	 * @param networkMapId
	 * 		The resource id of the Network Map.
	 * 		If null or "", use the default Network Map.
	 * @param costMetric
	 * 		The cost metric. Must not be null.
	 * @return
	 * 		The CostMap for that cost metric, or null if not available.
	 */
	public CostMap getCostMap(String networkMapId, String costMetric)
	{
		NetworkGroup group = getGroup(networkMapId);
		if (group == null) {
			return null;
		}
		return group.getCostMap(costMetric);
	}
	
	/**
	 * Add or replace a Cost Map for a Network Map.
	 * @param costMap The new Cost Map.
	 * @throws IllegalArgumentException
	 * 		If there is no Network Map with the mapId in the Cost Map.
	 */
	public void setCostMap(CostMap costMap)
	{
		String networkMapId = costMap.getMapId();
		NetworkGroup group = getGroup(networkMapId);
		if (group == null) {
			throw new IllegalArgumentException("AltoData.addCostMap: network map resource id '"
						+ networkMapId + "' is not defined");
		}
		group.setCostMap(costMap);
	}
	
	/**
	 * Return the cost metrics for all CostMaps associated with a Network Map.
	 * @param networkMapId
	 * 		The resource id of the Network Map.
	 * 		If null or "", use the default Network Map.
	 * @return
	 * 		A new Set with the cost metrics for the available Cost Maps.
	 * @throws IllegalArgumentException
	 * 		If there is no Network Map with resource id networkMapId.
	 */
	public Set<String> getCostMetrics(String networkMapId)
	{
		NetworkGroup group = getGroup(networkMapId);
		if (group == null) {
			throw new IllegalArgumentException("AltoData.getCostMetrics: network map resource id '"
						+ networkMapId + "' is not defined");
		}
		return group.getCostMetrics();
	}
	
	/**
	 * Declare a property name as valid, without setting a value for a specific entity.
	 * @param prop The new property name.
	 * 		Quietly do nothing if the name already has been declared.
	 */
	public void addPropName(String prop)
	{
		synchronized (m_propNames) {
			m_propNames.add(prop);
		}
	}
	
	/**
	 * Return all valid property names.
	 * This includes resource-specific names as well as global property names.
	 * However, it does not include the "pid" property.
	 * @return
	 * 		The valid property names, except for the "pid" property.
	 */
	public synchronized Set<String> getPropNames()
	{
		return new HashSet(m_propNames);
	}
	
	/**
	 * Return the value of a property for an endpoint.
	 * First check the endpoint-property table.
	 * If not found there, use the default for the endpoint's pid.
	 * @param addr The endpoint's IP address.
	 * 		This may be an encoded PID name.
	 * @param prop The property name, possibly qualified with a map id.
	 * @return The property value, or null if not set.
	 */
	public String getEndpointProp(EndpointAddress addr, String prop)
	{
		if (addr == null || prop == null) {
			return null;
		}
		String2 pair = AltoResp_EndpointProp.splitPropName(prop);
		String leaf = prop;
		NetworkGroup group = null;
		if (pair != null) {
			leaf = pair.m_str2;
			group = getGroup(pair.m_str1);
		}
		return getEndpointProperty(group, addr, leaf);
	}
	
	private String getEndpointProperty(NetworkGroup group, EndpointAddress addr, String leaf)
	{
		// network-map-id.pid property: always return PID name.
		if (group != null && leaf.equals(AltoResp_EndpointProp.PROPERTY_TYPE_PID)) {
			return group.getNetworkMap().getPID(addr);
		}
		
		// If property is defined for endpoint, return that value.
		EndpointPropertyTable endpointTable = (group != null) ? group.m_endpointProps : m_endpointProps;
		synchronized (endpointTable) {
			String value = endpointTable.getProp(addr, leaf);
			if (value != null && !value.equals("")) {
				return value;
			}	
		}
		
		// If property is defined for endpoint's pid, return that value. 
		if (group == null && USE_DEFAULT_NETWORK_MAP_PID_PROPERTIES) {
			group = getGroup(null);
		}
		if (group != null) {
			String pid = group.getNetworkMap().getPID(addr);
			if (pid != null) {
				String value = getPidProp(group, pid, leaf);
				if (value != null) {
					return value;
				}
			}
		}
		
		// No joy.
		return null;
	}
	
	/**
	 * Return the value of a property for a PID.
	 * @param pid The name of the PID.
	 * @param prop The property name, possibly qualified with a map id.
	 * @return The property value, or null if not set.
	 */
	public String getPidProp(String pid, String prop)
	{
		if (pid == null || prop == null) {
			return null;
		}
		String2 pair = AltoResp_EndpointProp.splitPropName(prop);
		String leaf = prop;
		NetworkGroup group = null;
		if (pair != null) {
			leaf = pair.m_str2;
			group = getGroup(pair.m_str1);
		}
		return getPidProp(group, pid, leaf);
	}
	
	/**
	 * Return the property assigned to a PID.
	 * If the PID doesn't have the property,
	 * go up the "enclosing PID" chain until
	 * we find a parent PID with the property.
	 * @param group The network group. If null, return null.
	 * @param pid The pid name.
	 * @param leaf The leaf part of the property name.
	 * @return The property value, or null.
	 */
	private String getPidProp(NetworkGroup group, String pid, String leaf)
	{
		if (leaf.equals(AltoResp_EndpointProp.PROPERTY_TYPE_PID)) {
			return pid;
		} else if (group == null) {
			return null;
		} else {
			synchronized (group.m_pidProps) {
				while (pid != null) {
					String2 pidKey = new String2(pid, leaf);
					String value = group.m_pidProps.get(pidKey);
					if (value != null) {
						return value;
					}
					pid = group.getNetworkMap().getContainingPid(pid);
				}
			}
			return null;
		}
	}
	
	/**
	 * Set the value of a property for an endpoint or a PID.
	 * @param addr The endpoint's IP address, or a pid name prefixed by "pid:".
	 * @param prop The property name. "id.name" sets a property in the map "id".
	 * 		"name" sets a property in the default map.
	 * @param value The new value. Use null or "" to remove the property.
	 * @throws UnknownHostException If addr isn't a valid IP address or "pid:name".
	 */
	public void setEndpointProp(String addr, String prop, String value)
			throws UnknownHostException
	{
		if (EndpointAddress.isPID(addr)) {
			EndpointAddress pid = new EndpointAddress(addr, true);
			setPidProp(pid.getName(), prop, value);
			return;
		}
		
		NetworkGroup group = null;
		String leaf = prop;
		String2 pair = AltoResp_EndpointProp.splitPropName(prop);
		if (pair != null) {
			group = getGroup(pair.m_str1);
			leaf = pair.m_str2;
		}
		if (leaf.equals(AltoResp_EndpointProp.PROPERTY_TYPE_PID)) {
			return;
		}
		EndpointPropertyTable propTable = (group != null) ? group.m_endpointProps : m_endpointProps;
		synchronized (propTable) {
			propTable.setProp(addr, leaf, value);
		}		
		synchronized (m_propNames) {
			m_propNames.add(prop);
		}
	}
	
	/**
	 * Set the default value of a property for all endpoints in a pid.
	 * @param pid The pid.
	 * @param prop The property name. If fully qualified, use that map.
	 * 		Otherwise use the default map.
	 * @param value The new value. Use null or "" to remove the property.
	 */
	public void setPidProp(String pid, String prop, String value)
	{
		String leaf = prop;
		String2 pair = AltoResp_EndpointProp.splitPropName(prop);
		
		NetworkGroup group = null;
		if (pair != null) {
			group = getGroup(pair.m_str1);
			leaf = pair.m_str2;
		} else {
			group = getGroup(null);
			leaf = prop;
		}
		if (group == null || leaf.equals(AltoResp_EndpointProp.PROPERTY_TYPE_PID)) {
			return;
		}
		
		// Update the property table.
		String2 key = new String2(pid, leaf);
		synchronized (group.m_pidProps) {
			if (value == null || value.equals("")) {
				group.m_pidProps.remove(key);
			} else {
				group.m_pidProps.put(key, value);
			}
		}
		synchronized (m_propNames) {
			m_propNames.add(prop);
		}
	}
	
	/**
	 * Return the global endpoint properties in this ALTO server.
	 * @return
	 * 		The global endpoint properties in this ALTO server.
	 * 		If none, returns an empty List instead of null.
	 */
	public List<EndpointPropertyTable.PropValue> getEndpointProps()
	{
		return m_endpointProps.getAllProps(null);
	}

	/**
	 * Return the endpoint properties for a Network Map.
	 * @param networkMapId
	 * 		The resource id of a Network Map. If null, use the default map.
	 * @return
	 * 		The endpoint properties for that Network Map.
	 * 		If none, returns an empty List instead of null.
	 */
	public List<EndpointPropertyTable.PropValue> getEndpointProps(String networkMapId)
	{
		NetworkGroup group = getGroup(networkMapId);
		if (group == null) {
			return new ArrayList<EndpointPropertyTable.PropValue>();
		}
		return group.getEndpointProps();
	}
	
	/**
	 * Return the pid properties for a Network Map.
	 * @param networkMapId
	 * 		The resource id of a Network Map. If null, use the default map.
	 * @return
	 * 		The pid properties for that Network Map, as a List of
	 * 		(pid-name, prop-name, prop-value) triples.
	 * 		The pid-names are prefixed with "pid:",
	 * 		and the property names are prefixed
	 * 		with the resource id of this Network Map.
	 * 		If none, returns an empty List instead of null.
	 */
	public List<String3> getPidProps(String networkMapId)
	{
		NetworkGroup group = getGroup(networkMapId);
		if (group == null) {
			return new ArrayList<String3>();
		}
		return group.getPidProps();
	}
}
