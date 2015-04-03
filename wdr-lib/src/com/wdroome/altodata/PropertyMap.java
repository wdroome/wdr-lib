package com.wdroome.altodata;

import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import com.wdroome.util.ImmutableSet;

/**
 * A generalized Property Map for an entity class.
 * This class is fully synchronized.
 * 
 * @author wdr
 *
 * @param <E> The entity-name type.
 * @param <V> The value type.
 */
public class PropertyMap<E,V>
{
	/**
	 * The master property table.
	 * Keys are property names, values are maps from entity names to property values.
	 * If we assume there are a small number of distinct property names,
	 * and a large number of entities, this two-level map-of-maps approach
	 * is more efficient than a single-level map from (entity-name,prop-name)
	 * pairs to property values.
	 */
	private final Map<String, Map<E,V>> m_propNameMaps = new HashMap<String, Map<E,V>>();
	
	// If true, use TreeMaps for the secondary entity->value maps in m_propNameMaps,
	// so they iterate over entity names in ascending order. If false, use HashMaps.
	private final boolean m_sortEntityNames;
	
	// The name of the entity type (ipv4, pid, etc).
	private final String m_entityType;
	
	// The distinct property names. That is, the keys for m_propNameMaps.
	// If null, getPropNames() sets it to a clone of m_propNameMaps.keyset(),
	// and then returns it to the caller.
	// Whenever we add or remove a property name from m_propNameMaps,
	// we set m_propNames to null.
	// Thus we can give m_propNames to clients without worrying
	// about the property name set changing while the client
	// iterates over the set. Remember we expect the set
	// of property names to be relatively small,
	// and we expect it to change very rarely,
	// so this should be very efficient.
	private ImmutableSet<String> m_propNames = null;
	
	private final ArrayList<E> g_emptyEntityList = new ArrayList<E>();
	
	/**
	 * Create a new Property Map.
	 * @param entityType
	 * 		The entity type name, for descriptive purposes.
	 * @param sortEntityNames
	 * 		If true, the methods that iterate over entity names
	 * 		will return the entities in ascending order,
	 * 		using the entity class's natural ordering.
	 * 		If false, the methods will return entity names in any order.
	 */
	public PropertyMap(String entityType, boolean sortEntityNames)
	{
		m_entityType = entityType;
		m_sortEntityNames = sortEntityNames;
	}
	
	/**
	 * Return the entity type name.
	 * @return The entity type name.
	 */
	public String getEntityType()
	{
		return m_entityType;
	}
	
	/**
	 * Return a property value.
	 * @param entityName The entity name. Cannot be null.
	 * @param propName The property name. Cannot be null.
	 * @return
	 * 		The value for that property for that entity,
	 * 		or null if no value has been set.
	 * 		Note that null may also mean the property value
	 * 		was set to null.
	 * @see #valueExists(Object, String)
	 */
	public synchronized V getProp(E entityName, String propName)
	{
		Map<E,V> entMap = m_propNameMaps.get(propName);
		if (entMap != null) {
			return entMap.get(entityName);
		}
		return null;
	}
	
	/**
	 * Return true iff a value (possibly null) has been set for a property.
	 * @param entityName The entity name. Cannot be null.
	 * @param propName The property name. Cannot be null.
	 * @return
	 * 		True iff a (possibly null) value has been set
	 * 		for that property for that entity.
	 * 		If not, return false.
	 */
	public synchronized boolean valueExists(E entityName, String propName)
	{
		Map<E,V> entMap = m_propNameMaps.get(propName);
		if (entMap != null) {
			return entMap.containsKey(entityName);
		}
		return false;
	}	
	
	/**
	 * Set a property value.
	 * @param entityName The entity name. Cannot be null.
	 * @param propName The property name. Cannot be null.
	 * @param value
	 * 		The new value. If null, the property will exist,
	 * 		but {@link #getProp(Object, String)} will return null.
	 * 		Use {@link #valueExists(Object, String)} to determine
	 * 		if the property has been set to null.
	 * @return The previous value of this property, or null.
	 */
	public synchronized V setProp(E entityName, String propName, V value)
	{
		Map<E,V> entMap = m_propNameMaps.get(propName);
		if (entMap == null) {
			entMap = m_sortEntityNames ? new TreeMap<E,V>() : new HashMap<E,V>();
			m_propNameMaps.put(propName, entMap);
			m_propNames = null;
		}
		return entMap.put(entityName, value);
	}

	/**
	 * Remove a property value.
	 * @param entityName The entity name. Cannot be null.
	 * @param propName The property name. Cannot be null.
	 * @return The previous value, or null.
	 */
	public synchronized V removeProp(E entityName, String propName)
	{
		Map<E,V> entMap = m_propNameMaps.get(propName);
		if (entMap == null) {
			return null;
		}
		V prevValue = entMap.remove(entityName);
		if (entMap.isEmpty()) {
			m_propNameMaps.remove(propName);
			m_propNames = null;
		}
		return prevValue;
	}
	
	/**
	 * Return the total number of property-value entries in this map.
	 * @return
	 * 		The total number of property-value entries in this map.
	 */
	public synchronized int size()
	{
		int size = 0;
		for (Map<E,V> entMap: m_propNameMaps.values()) {
			size += entMap.size();
		}
		return size;
	}
	
	/**
	 * Return the property names.  This method returns an immutable clone
	 * of the set of property names when the method was called,
	 * so the returned set will NOT reflect any subsequent changes.
	 * @return
	 * 		The property names.
	 */
	public synchronized Set<String> getPropNames()
	{
		if (m_propNames == null) {
			m_propNames = new ImmutableSet<String>(
							new HashSet<String>(m_propNameMaps.keySet()));
		}
		return m_propNames;
	}
	
	/**
	 * Return the entity names which have values for a property.
	 * This method returns an immutable clone
	 * of the set of entity names when the method was called,
	 * so the returned set will NOT reflect any subsequent changes.
	 * @param propName The property name. Cannot be null.
	 * @return
	 * 		The entity names.
	 */
	public synchronized List<E> getEntityNames(String propName)
	{
		Map<E,V> entMap = m_propNameMaps.get(propName);
		if (entMap != null) {
			return new ArrayList<E>(entMap.keySet());
		} else {
			return g_emptyEntityList;
		}
	}
	
	/**
	 * A callback interface for the methods that iterate
	 * over property values.
	 *
	 * @param <E> The entity-name type.
	 * @param <V> The value type.
	 */
	public interface PropValueCB<E,V>
	{
		/**
		 * Called to present a new property value to the client.
		 * Note that the iterator calls this method while holding
		 * a synchronization lock on the property map.
		 * Hence this method MUST NOT change properties,
		 * and SHOULD return quickly.
		 * 
		 * @param entityName The entity name.
		 * @param propName The property name.
		 * @param value The property value.
		 * 
		 * @return
		 * 		True to continue iterating over the properties,
		 * 		or false to stop.
		 */
		public boolean propValue(E entityName, String propName, V value);
	};
	
	/**
	 * Iterate over all (entity-name, prop-name, value) triples
	 * for a specific property name. If the property table
	 * is sorted (see {@link #PropertyMap(String, boolean)},
	 * the iterator presents the property values in ascending order,
	 * using the entity-name class's natural ordering.
	 * Otherwise the order is unpredictable.
	 * @param propValueCB
	 * 		The prop-value callback. This method calls propValueCB.propValue()
	 * 		on each property value. If propValue() returns false,
	 * 		the iterator stops.
	 * @param propName The property name. Must not be null.
	 * @return
	 * 		True if the iterator presented all property values to the client,
	 * 		or false if the iterator terminated early because
	 *		the callback returned false.
	 */
	public synchronized boolean getProperties(PropValueCB<E,V> propValueCB, String propName)
	{
		Map<E,V> entMap = m_propNameMaps.get(propName);
		if (entMap != null) {
			for (Map.Entry<E,V> entValue: entMap.entrySet()) {
				if (!propValueCB.propValue(entValue.getKey(), propName, entValue.getValue())) {
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * Iterate over all (entity-name, prop-name, value) triples.
	 * This method does NOT guarantee the order in which
	 * the property values are returned. If order is important,
	 * use {@link #getProperties(PropValueCB, String)}.
	 * @param propValueCB
	 * 		The prop-value callback. This method calls propValueCB.propValue()
	 * 		on each property value. If propValue() returns false,
	 * 		the iterator stops.
	 * @return
	 * 		True if the iterator presented all property values to the client,
	 * 		or false if the iterator terminated early because
	 *		the callback returned false.
	 */
	public synchronized boolean getProperties(PropValueCB<E,V> propValueCB)
	{
		for (String propName: m_propNameMaps.keySet()) {
			if (!getProperties(propValueCB, propName)) {
				return false;
			}
		}
		return true;
	}
}
