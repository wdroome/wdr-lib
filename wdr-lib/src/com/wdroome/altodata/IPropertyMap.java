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
 * An interface for a generalized Property Map for an entity class.
 * 
 * @author wdr
 *
 * @param <E>
 *  	The class for entity names.
 * 		Objects will be used as hash table keys,
 * 		so the class must implement hashCode() & equals() properly.

 */
public interface IPropertyMap<E>
{
	/**
	 * Interface to create an Entity from a String.
	 * The {@link #makeEntity(String, String)} method
	 * uses an object of this type to create an entity.
	 *  
	 * @author wdr
	 *
	 * @param <E>
	 *  	The class for entity names.
	 */
	public interface MakeEntity<E>
	{
		public E makeEntity(String entity, String type);
	}

	/**
	 * The entity type name (without the ':' suffix).
	 * @return The entity type name.
	 */
	public String getEntityType();
	
	/**
	 * Create an object of class E from a String.
	 * @param str The string
	 * @return An instance of the entity name type.
	 * @throws IllegalArgumentException
	 * 		If str is not a valid string representation
	 * 		of an entity of class E.
	 */
	public E makeEntity(String str) throws IllegalArgumentException;
	
	/**
	 * Return an entity name as a string with the appropriate type prefix.
	 * @param entity An entity.
	 * @return
	 * 		The entity name as a typed string.
	 * 		E.g., {@link #getEntityType()} + ":" + entity.toString().
	 */
	public String getTypedName(E entity);
	
	/**
	 * Return a property value.
	 * @param entityName The entity name. Cannot be null.
	 * @param propName The property name. Cannot be null.
	 * 
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
	public String getProp(E entityName, String propName);
	
	/**
	 * Return true iff this property has been set.
	 * If propExists() returns true, but getProp() returns null,
	 * then then property is explicitly defined to have
	 * no value for this entity.
	 * <p>
	 * In general, this method does NOT consider inherited values.
	 * That is, it only returns true if a value was explicitly
	 * set for this particular entity. If {@link IPropertyMap#getProp(Object, String)}
	 * returns a value inherited from another entity, this method
	 * usually returns false.
	 * 
	 * @param entityName The entity name. Cannot be null.
	 * @param propName The property name. Cannot be null.
	 * @return
	 * 		True iff a value has been set
	 * 		for that property for that entity.
	 * 		Note that the value may be null.
	 * 		If not, return false.
	 */
	public boolean propExists(E entityName, String propName);
	
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
	public String setProp(E entityName, String propName, String value);

	/**
	 * Remove a property value.
	 * @param entityName The entity name. Cannot be null.
	 * @param propName The property name. Cannot be null.
	 * @return The previous value, or null.
	 */
	public  String removeProp(E entityName, String propName);
	
	/**
	 * Return the property names.  This method returns an immutable clone
	 * of the set of property names when the method was called,
	 * so the returned set will NOT reflect any subsequent changes.
	 * @return
	 * 		The property names.
	 */
	public Set<String> getPropNames();
	
	/**
	 * Return the entity names which may have values for a property.
	 * The list must include all entities with values for that property,
	 * but might include entities that do not.
	 * This method returns an immutable clone
	 * of the set of entity names when the method was called,
	 * so the returned set will NOT reflect any subsequent changes.
	 * <p>
	 * In general, this method does NOT consider inherited values.
	 * That is, it only returns entities with explicitly
	 * set values. If {@link IPropertyMap#getProp(Object, String)}
	 * returns an inherited value for an entity, the returned
	 * list will not necessarily include that entity.
	 * 
	 * @param propName The property name. Cannot be null.
	 * @return
	 * 		The entity names.
	 */
	public List<E> getEntityNames(String propName);
	
	/**
	 * A callback interface for the methods that iterate
	 * over property values.
	 *
	 * @param <E> The entity-name type.
	 */
	public interface PropValueCB<E>
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
		 * @param value The property value. If null,
		 * 			the property is defined as having no value
		 * 			for this entity, so the client should
		 * 			not use heiarchy rules to impute a value.
		 * 
		 * @return
		 * 		True to continue iterating over the properties,
		 * 		or false to stop.
		 */
		public boolean propValue(E entityName, String propName, String value);
	};
	
	/**
	 * Iterate over all (entity-name, prop-name, value) triples
	 * for a specific property name.
	 * If the property table is sorted,
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
	public boolean getProperties(PropValueCB<E> propValueCB, String propName);
	
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
	public boolean getProperties(PropValueCB<E> propValueCB);
}
