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
 * It stores values for entities, with no additional semantics
 * or inheritance rules.
 * This class is fully synchronized.
 * Child classes may extend this class to add inheritance.
 * 
 * @author wdr
 *
 * @param <E>
 *		The class for entity names.
 * 		Objects will be used as hash table keys,
 * 		so the class must implement hashCode() & equals() properly.
 */
public class PropertyMap<E> implements IPropertyMap<E>
{
	/**
	 * The master property table.
	 * Keys are property names, values are maps from entity names to property values.
	 * If we assume there are a small number of distinct property names,
	 * and a large number of entities, this two-level map-of-maps approach
	 * is more efficient than a single-level map from (entity-name,prop-name)
	 * pairs to property values.
	 */
	private final Map<String, Map<E,String>> m_propNameMaps = new HashMap<String, Map<E,String>>();
	
	// If true, use TreeMaps for the secondary entity->value maps in m_propNameMaps,
	// so they iterate over entity names in ascending order. If false, use HashMaps.
	private final boolean m_sortEntityNames;
	
	// The name of the entity type (ipv4, pid, etc).
	private final String m_entityType;
	
	private final MakeEntity<E> m_entityMaker;
	
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
	 * 		The entity type name (without the ':' suffix).
	 * @param entityMaker
	 * 		An object that creates a new E object from a string.
	 * 		If null, E must be String.
	 * @param sortEntityNames
	 * 		If true, the methods that iterate over entity names
	 * 		will return the entities in ascending order,
	 * 		using the entity class's natural ordering.
	 * 		If false, the methods will return entity names in any order.
	 */
	public PropertyMap(String entityType, MakeEntity<E> entityMaker, boolean sortEntityNames)
	{
		m_entityType = entityType;
		m_entityMaker = entityMaker;
		m_sortEntityNames = sortEntityNames;
	}
	
	/**
	 * Return the entity type prefix.
	 * @return The entity type prefix (without the ':').
	 */
	@Override
	public String getEntityType()
	{
		return m_entityType;
	}
	
	/**
	 * Create an object of class E from a String.
	 * @param str The string
	 * @return An instance of the entity name type.
	 * @throws IllegalArgumentException
	 * 		If str is not a valid string representation
	 * 		of an entity of class E.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public E makeEntity(String str)
	{
		if (m_entityMaker != null) {
			return m_entityMaker.makeEntity(str, getEntityType());
		} else {
			// In this case, E should be String.
			return (E)str;
		}
	}
	
	/**
	 * Return an entity name as a string with the appropriate type prefix.
	 * @param entity An entity.
	 * @return
	 * 		The entity name as a typed string.
	 * 		E.g., {@link #getEntityType()} + ":" + entity.toString().
	 */
	@Override
	public String getTypedName(E entity)
	{
		return getEntityType() + ":" + entity.toString();
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
	 * 		If getProp() returns null, but propExists() returns true,
	 * 		the property is defined to no value for this entity.
	 * 		In this case the client should not use inheritance rules
	 * 		to impute a value.
	 * @see #propExists(Object, String)
	 */
	@Override
	public synchronized String getProp(E entityName, String propName)
	{
		Map<E,String> entMap = m_propNameMaps.get(propName);
		if (entMap != null) {
			return entMap.get(entityName);
		}
		return null;
	}
	
	/**
	 * Return true iff this property has been set.
	 * If propExists() returns true, but getProp() returns null,
	 * then then property is explicitly defined to have
	 * no value for this entity.
	 * 
	 * @param entityName The entity name. Cannot be null.
	 * @param propName The property name. Cannot be null.
	 * @return
	 * 		True iff a value has been set
	 * 		for that property for that entity.
	 * 		Note that the value may be null.
	 * 		If not, return false.
	 */
	@Override
	public synchronized boolean propExists(E entityName, String propName)
	{
		Map<E,String> entMap = m_propNameMaps.get(propName);
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
	 * 		Use {@link #propExists(Object, String)} to determine
	 * 		if the property has been set to null.
	 * @return The previous value of this property, or null.
	 */
	@Override
	public synchronized String setProp(E entityName, String propName, String value)
	{
		Map<E,String> entMap = m_propNameMaps.get(propName);
		if (entMap == null) {
			entMap = m_sortEntityNames ? new TreeMap<E,String>() : new HashMap<E,String>();
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
	@Override
	public synchronized String removeProp(E entityName, String propName)
	{
		Map<E,String> entMap = m_propNameMaps.get(propName);
		if (entMap == null) {
			return null;
		}
		String prevValue = entMap.remove(entityName);
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
		for (Map<E,String> entMap: m_propNameMaps.values()) {
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
	@Override
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
	@Override
	public synchronized List<E> getEntityNames(String propName)
	{
		Map<E,String> entMap = m_propNameMaps.get(propName);
		if (entMap != null) {
			return new ArrayList<E>(entMap.keySet());
		} else {
			return g_emptyEntityList;
		}
	}
	
	/**
	 * Iterate over all (entity-name, prop-name, value) triples
	 * for a specific property name. If the property table
	 * is sorted (see {@link #PropertyMap(String, IPropertyMap.MakeEntity, boolean)},
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
	@Override
	public synchronized boolean getProperties(PropValueCB<E> propValueCB, String propName)
	{
		Map<E,String> entMap = m_propNameMaps.get(propName);
		if (entMap != null) {
			for (Map.Entry<E,String> entValue: entMap.entrySet()) {
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
	@Override
	public synchronized boolean getProperties(PropValueCB<E> propValueCB)
	{
		for (String propName: m_propNameMaps.keySet()) {
			if (!getProperties(propValueCB, propName)) {
				return false;
			}
		}
		return true;
	}
}
