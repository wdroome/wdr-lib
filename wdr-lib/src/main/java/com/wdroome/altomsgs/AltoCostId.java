package com.wdroome.altomsgs;

import java.util.List;
import java.util.ArrayList;

import com.wdroome.json.JSONValue_Object;
import com.wdroome.json.JSONValue_Array;

/**
 * A cost-id, combining a "cost metric" and a "cost mode",
 * plus an optional name, description and class name.
 * @author wdr
 */
public class AltoCostId
{
	private final String m_metric;
	private final String m_mode;
	private final String m_name;
	private final String m_description;
	private final String m_clazz;
	
	/**
	 * Create a named new cost id with a single metric/mode pair,
	 * and freeze it.
	 * @param name The name of this cost-id, if known. May be null.
	 * @param metric The metric for the new cost id (not null).
	 * @param mode The mode for the new cost id (not null).
	 * @param description The optional description. May be null.
	 */
	public AltoCostId(String metric, String mode, String name, String description, String clazz)
	{
		m_metric = metric;
		m_mode = mode;
		m_name = name;
		m_description = description;
		m_clazz = clazz;
	}
	
	/**
	 * Create an unnamed cost id with a single metric/mode pair,
	 * and freeze it.
	 * @param metric The metric for the new cost id (not null).
	 * @param mode The mode for the new cost id (not null).
	 */
	public AltoCostId(String metric, String mode)
	{
		this(metric, mode, null, null, null);
	}
	
	/**
	 * Return the name of this cost id. May be null.
	 */
	public String getName()
	{
		return m_name;
	}
	
	/**
	 * Return the description for this cost id, or null.
	 */
	public String getDescription()
	{
		return m_description;
	}
	
	/**
	 * Return the class name for this cost id, or null.
	 */
	public String getClazz()
	{
		return m_clazz;
	}

	/**
	 * If this is a simple cost-id, return its one and only cost metric.
	 * @return The one and only cost metric in this cost id.
	 * @throws IllegalStateException
	 *		If this cost-id has more than one type/mode pair.
	 */
	public String getMetric()
	{
		return m_metric;
	}
	
	/**
	 * If this is a simple cost-id, return its one and only cost mode.
	 * @return The one and only cost mode in this cost id.
	 * @throws IllegalStateException
	 *		If this cost-id has more than one type/mode pair.
	 */
	public String getMode()
	{
		return m_mode;
	}
	
	/**
	 * Return a string with the metric/mode in this cost id.
	 */
	@Override
	public String toString()
	{
		StringBuilder b = new StringBuilder();
		if (m_name != null) {
			b.append(m_name);
			b.append(":");
		}
		b.append(m_metric);
		b.append("/");
		b.append(m_mode);
		return b.toString();
	}

	/**
	 * Test if this cost id exactly matches another cost id.
	 * @param costId The cost id to test. Must not be null.
	 * @return True iff this cost id has a single metric/mode pair,
	 * 			and it matches costId's "metric" and "mode".
	 */
	public boolean exactMatch(AltoCostId costId)
	{
		return costId != null
				&& m_metric.equals(costId.m_metric)
				&& m_mode.equals(costId.m_mode);
	}

	/**
	 * Test if this cost id exactly matches a metric/mode pair.
	 * @param metric The cost metric to test. Must not be null.
	 * @param mode The cost mode to test. Must not be null.
	 * @return True iff this cost id has a single metric/mode pair,
	 * 			and it matches the "metric" and "mode" parameters.
	 */
	public boolean exactMatch(String metric, String mode)
	{
		return m_metric.equals(metric) && m_mode.equals(mode);
	}
	
	/**
	 * Test if this cost id matches a metric/mode pair,
	 * allowing for null as a wild card.
	 * @param metric The cost metric to test. Null always matches.
	 * @param mode The cost mode to test. Null always matches.
	 * @return True iff this cost id has a single metric/mode pair,
	 * 			and it matches the non-null "metric" and "mode" parameters.
	 */
	public boolean wildcardMatch(String metric, String mode)
	{
		if (metric != null && !metric.equals(m_metric))
			return false;
		if (mode != null && !mode.equals(m_mode))
			return false;
		return true;
	}
	
	/**
	 * Return true if this cost id matches an item in a list of ids.
	 * @param ids The list to test. If null, always return false.
	 * @return True iff this id matches some item in "ids".
	 */
	public boolean inList(List<AltoCostId> ids)
	{
		if (ids != null) {
			for (AltoCostId xid: ids) {
				if (exactMatch(xid)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Given an array of cost ids, find the first one that exactly matches
	 * a metric/mode pairs. If none exactly match, find the first wildcard match.
	 * @param metric A cost metric (or null).
	 * @param mode A cost mode (or null).
	 * @param costIds An array of cost ids.
	 * @return The first element of costIds[] to exactly match metric/mode.
	 * 		If there are no exact matches, return the first wildcard match.
	 */
	public static AltoCostId find(String metric, String mode, AltoCostId[] costIds)
	{
		boolean isWildcardTest = metric == null || mode == null;
		
		for (AltoCostId costId:costIds) {
			if (!isWildcardTest && costId.exactMatch(metric, mode))
				return costId;
			if (isWildcardTest && costId.wildcardMatch(metric, mode)) {
				return costId;
			}
		}
		return null;
	}
	
	/**
	 * Return true if this CostId matches a CostId in a Resource Directory capability list.
	 * @param capability The "capability" object in an Resource Directory entry.
	 * 			If null, always return false.
	 * @return True iff this CostId matches a CostId in a Resource Directory capability list.
	 */
	public boolean inCapability(JSONValue_Object capability, AltoResp_InfoResourceDir dir)
	{
		if (capability == null)
			return false;
		
		// New style, with list of named cost types.
		// For now, only works with simple types.
		if (dir.hasNamedCostTypes()) {
			String costTypeName = dir.findCostTypeName(m_metric, m_mode);
			if (costTypeName != null) {
				JSONValue_Array costTypeNames
						= capability.getArray(AltoResp_CostMap.CAPABILITY_COST_TYPE_NAMES, null);
				if (costTypeNames != null) {
					int n = costTypeNames.size();
					for (int i = 0; i < n; i++) {
						String s = costTypeNames.getString(i);
						if (s != null && s.equals(costTypeName)) {
							return true;
						}
					}
				}
			}
			return false;
		}
		
		return false;
	}
}
