package com.wdroome.altomsgs;

import java.io.Reader;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;

import com.wdroome.json.JSONException;
import com.wdroome.json.IJSONLexan;
import com.wdroome.json.JSONLexan;
import com.wdroome.json.JSONFieldMissingException;
import com.wdroome.json.JSONParseException;
import com.wdroome.json.JSONValue;
import com.wdroome.json.JSONValueTypeException;
import com.wdroome.json.JSONValue_Object;
import com.wdroome.json.JSONValue_Array;
import com.wdroome.json.JSONValue_String;
import com.wdroome.json.JSONValue_Number;
import com.wdroome.util.String2;

/**
 * Base class for all ALTO response messages.
 * These messages consist of a JSON dictionary with a "meta" object
 * and one or more JSON dictionary objects ("maps").
 * This class also provides getters and setters
 * for the "meta" fields that are common to several message types,
 * such as vtags and cost type.
 * @author wdr
 */
public abstract class AltoResp_Base extends AltoMsg_Base
{
	public static final String FN_META = "meta";
		
	public static final String FN_VTAG = "vtag";
	public static final String FN_DEPENDENT_VTAGS = "dependent-vtags";
	public static final String FN_RESOURCE_ID = "resource-id";
	public static final String FN_TAG = "tag";

	public static final String FN_COST_TYPE = "cost-type";
	public static final String FN_COST_METRIC = "cost-metric";
	public static final String FN_COST_MODE = "cost-mode";

	public static final String FN_SERVER_INFO = "priv:alu-server-info";

	private final JSONValue_Object m_meta;
	private final JSONValue_Object[] m_maps;
	private final JSONValue_Array[] m_arrays;
	
	/**
	 * If this message has one and only one data map, point to it.
	 * Otherwise null.
	 * @see #getMapNames()
	 */
	protected final JSONValue_Object m_map;
	
	/**
	 * If this message has one and only one data array, point to it.
	 * Otherwise null.
	 * @see #getArrayNames()
	 */
	protected final JSONValue_Array m_array;
	
	/**
	 * Create an empty object.
	 * Used by server to construct a message to send.
	 */
	public AltoResp_Base()
	{
		super();
		m_json.put(FN_META, m_meta = new JSONValue_Object());
		String[] names = getMapNames();
		if (names == null) {
			names = new String[0];
		}
		m_maps = new JSONValue_Object[names.length];
		for (int i = 0; i < names.length; i++) {
			m_json.put(names[i], m_maps[i] = new JSONValue_Object());
		}
		m_map = (m_maps.length == 1) ? m_maps[0] : null;
		names = getArrayNames();
		if (names == null) {
			names = new String[0];
		}
		m_arrays = new JSONValue_Array[names.length];
		for (int i = 0; i < names.length; i++) {
			m_json.put(names[i], m_arrays[i] = new JSONValue_Array());
		}
		m_array = (m_arrays.length == 1) ? m_arrays[0] : null;
	}
	
	/**
	 * Create a response message from a JSON parser.
	 * @param lexan The JSON parser.
	 * @throws JSONParseException If the input isn't valid JSON.
	 * @throws JSONValueTypeException If the input isn't a JSON Object dictionary.
	 */
	public AltoResp_Base(IJSONLexan lexan)
		throws JSONParseException, JSONValueTypeException
	{
		super(lexan);
		m_meta = getObject(m_json, FN_META);
		String[] names = getMapNames();
		if (names == null) {
			names = new String[0];
		}
		m_maps = new JSONValue_Object[names.length];
		for (int i = 0; i < names.length; i++) {
			m_maps[i] = getObject(m_json, names[i]);
		}
		m_map = (m_maps.length == 1) ? m_maps[0] : null;
		names = getArrayNames();
		if (names == null) {
			names = new String[0];
		}
		m_arrays = new JSONValue_Array[names.length];
		for (int i = 0; i < names.length; i++) {
			m_arrays[i] = getArray(m_json, names[i]);
		}
		m_array = (m_arrays.length == 1) ? m_arrays[0] : null;
	}
	
	private static JSONValue_Object getObject(JSONValue_Object parent, String child)
	{
		JSONValue_Object obj = parent.getObject(child, null);
		if (obj == null) {
			obj = new JSONValue_Object();
			parent.put(child, obj);
		}
		return obj;
	}
	
	private static JSONValue_Array getArray(JSONValue_Object parent, String child)
	{
		JSONValue_Array obj = parent.getArray(child, null);
		if (obj == null) {
			obj = new JSONValue_Array();
			parent.put(child, obj);
		}
		return obj;
	}
	
	/**
	 * Return the names of the map fields in this message type.
	 * The base class returns null.
	 * @return The names of the map fields in this message type.
	 */
	public String[] getMapNames() { return null; }
	
	/**
	 * Return the names of the array fields in this message type.
	 * The base class returns null.
	 * @return The names of the array fields in this message type.
	 */
	public String[] getArrayNames() { return null; }
	
	/**
	 * Return the i'th data map.
	 * @return The i'th data map, or null if i is out of range.
	 * @see #m_map
	 */
	public JSONValue_Object getMap(int i)
	{
		if (i >= 0 && i < m_maps.length) {
			return m_maps[i];
		}
		return null;
	}
	
	/**
	 * Return the i'th data array.
	 * @return The i'th data array, or null if i is out of range.
	 * @see #m_array
	 */
	public JSONValue_Array getArray(int i)
	{
		if (i >= 0 && i < m_arrays.length) {
			return m_arrays[i];
		}
		return null;
	}
	
	/**
	 * Return the meta data dictionary for this message.
	 * @return The meta data dictionary for this message (never null).
	 */
	public JSONValue_Object getMeta()
	{
		return m_meta;
	}
	
	/**
	 * Return a dictionary-valued meta data field.
	 * @param name The field name.
	 * @return The value of "name", or null.
	 */
	public JSONValue_Object getMetaJSONObject(String name)
	{
		return m_meta.getObject(name, null);
	}
	
	/**
	 * Return a string-valued meta data field.
	 * @param name The field name.
	 * @return The value of "name", or null.
	 */
	public String getMetaString(String name)
	{
		return m_meta.getString(name, null);
	}
	
	/**
	 * Return a numeric-valued meta data field.
	 * @param name The field name.
	 * @param def The default value.
	 * @return The value of "name", or def.
	 */
	public double getMetaDouble(String name, double def)
	{
		return m_meta.getNumber(name, def);
	}
	
	/**
	 * Return a int-valued meta data field.
	 * @param name The field name.
	 * @param def The default value.
	 * @return The value of "name", or def.
	 */
	public int getMetaInt(String name, int def)
	{
		return (int)m_meta.getNumber(name, def);
	}
	
	/**
	 * Return a meta field with an unknown value type.
	 * @param name The field name.
	 * @return The value of "name", or null.
	 */
	public JSONValue getMeta(String name)
	{
		return m_meta.get(name);
	}
	
	/**
	 * Set a meta data field.
	 * @param name The field name.
	 * @param value The new value. If null, remove the field.
	 */
	public void setMetaValue(String name, String value)
	{
		if (value == null) {
			m_meta.remove(name);
		} else {
			m_meta.put(name, value);
		}
	}
	
	/**
	 * Set a meta data field.
	 * @param name The field name.
	 * @param value The new value. If null, remove the field.
	 */
	public void setMetaValue(String name, JSONValue_Object value)
	{
		if (value == null) {
			m_meta.remove(name);
		} else {
			m_meta.put(name, value);
		}
	}
	
	/**
	 * Set a meta data field.
	 * @param name The field name.
	 * @param value The new value. If NaN, remove the field.
	 */
	public void setMetaValue(String name, double value)
	{
		if (Double.isNaN(value)) {
			m_meta.remove(name);
		} else {
			m_meta.put(name, value);
		}
	}
	
	/**
	 * Return this message's resource id.
	 * @return This message's resource id, or null.
	 */
	public String getThisResourceId()
	{
		JSONValue_Object vtag = m_meta.getObject(FN_VTAG, null);
		if (vtag != null) {
			return vtag.getString(FN_RESOURCE_ID, null);
		}
		return null;
	}
	
	/**
	 * Return this message's vtag.
	 * @return This message's vtag, or null.
	 */
	public String getThisTag()
	{
		JSONValue_Object vtag = m_meta.getObject(FN_VTAG, null);
		if (vtag != null) {
			return vtag.getString(FN_TAG, null);
		}
		return null;
	}
	
	/**
	 * If this message has a map-vtag, return the resource id.
	 * If not, return null.
	 */
	public String getDependentResourceId()
	{
		return getDependentResourceId(0);
	}
	
	/**
	 * If this message has a dependent map-vtag, return the tag value.
	 * If not, return null.
	 */
	public String getDependentTag()
	{
		return getDependentTag(0);
	}
	
	/**
	 * Return the number of map-id,vtag pairs in this message.
	 */
	public int getNumVtags()
	{
		JSONValue_Array vtags = m_meta.getArray(FN_DEPENDENT_VTAGS, null);
		if (vtags != null)
			return vtags.size();
		return 0;
	}
	
	/**
	 * If this message has one or more map-id,vtag pairs, return one of the resource ids.
	 * If not, return null.
	 * @param iVtag The index (0 to n-1) of the desired pair.
	 * @return The map id of the iVtag'th pair,
	 * 			or null of there aren't at least iVtag+1 entries.
	 */
	public String getDependentResourceId(int iVtag)
	{
		JSONValue_Array vtags = m_meta.getArray(FN_DEPENDENT_VTAGS, null);
		if (vtags != null) {
			JSONValue_Object vtag = vtags.getObject(iVtag);
			if (vtag != null)
				return vtag.getString(FN_RESOURCE_ID, null);
			return null;
		}
		return null;
	}
	
	/**
	 * If this message has one or more dependent map-id,vtag pairs, return one of the tags.
	 * If not, return null.
	 * @param iVtag The index (0 to n-1) of the desired pair.
	 * @return The tag of the iVtag'th pair,
	 * 			or null of there aren't at least iVtag+1 entries.
	 */
	public String getDependentTag(int iVtag)
	{
		JSONValue_Array vtags = m_meta.getArray(FN_DEPENDENT_VTAGS, null);
		if (vtags != null) {
			JSONValue_Object vtag = vtags.getObject(iVtag);
			if (vtag != null)
				return vtag.getString(FN_TAG, null);
			return null;
		}
		return null;
	}
	
	/**
	 * Set the vtag for this message's data.
	 * @param resourceId The resource ID part.
	 * @param tag The tag part.
	 */
	public void setThisVtag(String resourceId, String tag)
	{
		if (resourceId == null)
			resourceId = "";
		if (tag == null)
			tag = "";
		if (resourceId.equals("") && tag.equals("")) {
			m_meta.remove(FN_VTAG);
			return;
		}
		JSONValue_Object vtag = new JSONValue_Object();
		m_meta.put(FN_VTAG, vtag);
		vtag.put(FN_RESOURCE_ID, new JSONValue_String(resourceId));
		vtag.put(FN_TAG, new JSONValue_String(tag));
	}
	
	/**
	 * Set the dependent vtag for this message's data.
	 * @param resourceId The resource ID part.
	 * @param tag The tag part.
	 */
	public void setDependentVtag(String resourceId, String tag)
	{
		if (resourceId == null)
			resourceId = "";
		if (tag == null)
			tag = "";
		if (resourceId.equals("") && tag.equals("")) {
			m_meta.remove(FN_DEPENDENT_VTAGS);
			return;
		}
		JSONValue_Array vtags = new JSONValue_Array();
		m_meta.put(FN_DEPENDENT_VTAGS, vtags);
		JSONValue_Object vtag = new JSONValue_Object();
		vtag.put(FN_RESOURCE_ID, new JSONValue_String(resourceId));
		vtag.put(FN_TAG, new JSONValue_String(tag));
		vtags.add(vtag);
	}
	
	/**
	 * Set this message's dependent map-vtags to an array of entries.
	 * Any previously set map-vtag entries are discarded. 
	 * @param idTagPairs The new collection of <id,tag> pairs.
	 * 		m_str1 is the resource id and m_str2 is the tag.
	 */
	public void setDependentVtags(Collection<String2> idTagPairs)
	{
		if (idTagPairs == null || idTagPairs.isEmpty()) {
			m_meta.remove(FN_DEPENDENT_VTAGS);
			return;
		}
		JSONValue_Array vtags = new JSONValue_Array();
		m_meta.put(FN_DEPENDENT_VTAGS, vtags);
		for (String2 idTag: idTagPairs) {
			JSONValue_Object vtag = new JSONValue_Object();
			vtag.put(FN_RESOURCE_ID, idTag.m_str1);
			vtag.put(FN_TAG, idTag.m_str2);
			vtags.add(vtag);
		}
	}
	
	/**
	 * Return the name of the cost-metric associated with this response.
	 * @return The name of the cost-metric associated with this response, or null.
	 */
	public String getCostMetric()
	{
		JSONValue_Object costId = m_meta.getObject(FN_COST_TYPE, null);
		if (costId != null)
			return costId.getString(FN_COST_METRIC, null);
		else
			return null;
	}
	
	/**
	 * Set the name of the cost-metric associated with this response.
	 * @param value The name of the cost-metric associated with this response.
	 */
	public void setCostMetric(String value)
	{
		JSONValue_Object costId = m_meta.getObject(FN_COST_TYPE, null);
		if (costId == null) {
			costId = new JSONValue_Object();
			m_meta.put(FN_COST_TYPE, costId);
		}
		costId.put(FN_COST_METRIC, new JSONValue_String(value));
	}
	
	/**
	 * Return the name of the cost-mode associated with this response.
	 * @return The name of the cost-mode associated with this response, or null.
	 */
	public String getCostMode()
	{
		JSONValue_Object costId = m_meta.getObject(FN_COST_TYPE, null);
		if (costId != null)
			return costId.getString(FN_COST_MODE, null);
		else
			return null;
	}
	
	/**
	 * Set the name of the cost-mode associated with this response.
	 * @param value The name of the cost-mode associated with this response.
	 */
	public void setCostMode(String value)
	{
		JSONValue_Object costId = m_meta.getObject(FN_COST_TYPE, null);
		if (costId == null) {
			costId = new JSONValue_Object();
			m_meta.put(FN_COST_TYPE, costId);
		}
		costId.put(FN_COST_MODE, new JSONValue_String(value));
	}
	
	/**
	 * Return the value of a server-info item.
	 * @return The value of the server-info item "name", or null.
	 */
	public String getServerInfoItem(String name)
	{
		JSONValue_Object serverInfo = m_meta.getObject(FN_COST_TYPE, null);
		if (serverInfo != null)
			return serverInfo.getString(name, null);
		else
			return null;
	}
	
	/**
	 * Return all server-info items in the meta data.
	 * @return A Map with all server-info items.
	 * 		Keys are item names, values are item values.
	 * 		Each invocation returns a new map.
	 * 		Never returns null; if there are no server-info items,
	 * 		return an empty map.
	 */
	public Map<String,String> getAllServerInfo()
	{
		HashMap<String,String> newMap = new HashMap<String,String>();
		JSONValue_Object serverInfo = m_meta.getObject(FN_SERVER_INFO, null);
		if (serverInfo != null) {
			for (Map.Entry<String,JSONValue> ent: serverInfo.entrySet()) {
				String item = ent.getKey();
				JSONValue value = ent.getValue();
				if (value instanceof JSONValue_String) {
					newMap.put(item, ((JSONValue_String)value).m_value);
				} else if (value instanceof JSONValue_Number) {
					newMap.put(item, ((JSONValue_String)value).toString());
				}
			}
		}
		return newMap;
	}
	
	/**
	 * Add a server-info item to the meta data. 
	 * @param name The name for this server-info item.
	 * @param value The value for that item.
	 */
	public void setServerInfoItem(String name, String value)
	{
		JSONValue_Object serverInfo = m_meta.getObject(FN_SERVER_INFO, null);
		if (serverInfo == null) {
			serverInfo = new JSONValue_Object();
			m_meta.put(FN_SERVER_INFO, serverInfo);
		}
		serverInfo.put(name, new JSONValue_String(value));
	}
}
