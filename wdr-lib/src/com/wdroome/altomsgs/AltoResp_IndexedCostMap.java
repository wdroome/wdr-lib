package com.wdroome.altomsgs;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Stack;


// import com.wdroome.json.*;
import com.wdroome.json.JSONException;
import com.wdroome.json.JSONParseException;
import com.wdroome.json.IJSONLexan;
import com.wdroome.json.JSONLexan;
import com.wdroome.json.GenericJSONScanner;
import com.wdroome.json.JSONValue;
import com.wdroome.json.JSONValue_Object;
import com.wdroome.json.JSONValue_Array;
import com.wdroome.json.JSONValue_Number;
import com.wdroome.json.JSONValue_String;
import com.wdroome.json.JSONValue_Null;
import com.wdroome.json.JSONWriter;
import com.wdroome.util.Ordinalizer;
import com.wdroome.util.StringIndexer;
import com.wdroome.util.FloatMatrix;
import com.wdroome.util.StringUtils;

/**
 * A class that represents a COST-MAP response in a space-efficient manor.
 * This class is functional equivalent to {@link AltoResp_CostMap},
 * but is practical for cost maps with thousands of PIDs.
 * One difference: In AltoResp_CostMap, getCost(src,dst) throws an exception
 * if there's no cost defined from src to dst.
 * In AltoResp_IndexedCostMap, getCost(src,dst) returns the default cost.
 * Note that we create a sparse matrix, and unless you call {@link #setDefaultCost(double)},
 * the default cost is 'NaN'.
 * @see AltoResp_CostMap
 * @author wdr
 */
public class AltoResp_IndexedCostMap extends AltoResp_Base
{
	public static final String MEDIA_TYPE = MEDIA_TYPE_PREFIX + "costmap" + MEDIA_TYPE_SUFFIX;

	private static final String FN_COST_MAP = "cost-map";
	private static final String FN_COST_TYPE = "cost-type";
	private static final String FN_COST_TYPE_METRIC = "cost-metric";
	private static final String FN_COST_TYPE_MODE = "cost-mode";
	private static final String FN_DEPENDENT_VTAGS = "dependent-vtags";
	private static final String FN_RESOURCE_ID = "resource-id";
	private static final String FN_TAG = "tag";

	private static final String FN_DEFAULT_COST = "default-cost";	// ALU private extension

	private final StringIndexer m_srcPids = new StringIndexer();
	
	private final StringIndexer m_destPids = new StringIndexer();
	
	private final FloatMatrix m_costs = new FloatMatrix();
	
	private float m_defaultCost = Float.NaN;
	private boolean m_clientSetDefaultCost = false;
	
	/**
	 * Create an empty message.
	 * @throws JSONException
	 */
	public AltoResp_IndexedCostMap() throws JSONException
	{
		super();
		m_costs.setDefaultValue(m_defaultCost);
	}
	
	/**
	 * Create an object from a JSON parser.
	 * Used to decode a received message.
	 * @param lexan The JSON input parser.
	 * @throws JSONParseException If jsonSrc isn't a valid JSON string.
	 * @throws IOException If lexan throws it while reading input.
	 */
	public AltoResp_IndexedCostMap(IJSONLexan lexan)
		throws JSONParseException, IOException
	{
		super();
		m_costs.setDefaultValue(m_defaultCost);
		if (lexan != null) {
			new MyJSONScanner(lexan).scan();
		}
	}
	
	/**
	 * Read a cost-map response from an input stream,
	 * and save the cost data. This is space-efficient,
	 * and does not retain the JSON input.
	 * @param istr An input streak with a JSON-encoded cost-map response message.
	 * @param contentLength The maximum number of bytes to read. If -1, read to EOF.
	 * @throws JSONParseException If jsonSrc isn't a valid JSON string.
	 * @throws IOException If an error occurs while reading input.
	 */
	public AltoResp_IndexedCostMap(InputStream istr, int contentLength)
		throws JSONParseException, IOException
	{
		this(new JSONLexan(istr, null, contentLength));
	}
	
	/**
	 * Create a cost-map from in-memory JSON data.
	 * @param json A JSON-encoded cost-map response message.
	 * @throws JSONParseException If jsonSrc isn't a valid JSON string.
	 * @throws IOException Should happen.
	 */
	public AltoResp_IndexedCostMap(String json)
		throws JSONParseException, IOException
	{
		this(new JSONLexan(json));
	}

	@Override
	public String getMediaType()
	{
		return MEDIA_TYPE;
	}
	
	@Override
	public String[] getMapNames()
	{
		return new String[] {FN_COST_MAP};
	}
	
	/**
	 * Return the default cost, or NaN if never set.
	 * @return The default cost, or NaN if never set.
	 */
	public double getDefaultCost()
	{
		return m_defaultCost;
	}

	/**
	 * Set the default cost.
	 * You should call this before setting any costs
	 * or reading any input.
	 * If you set the default cost to anything other than NaN,
	 * when generating a JSON encoding for this message,
	 * we will include a non-standard 'defaultCost' entry.
	 * @param defaultCost The default cost.
	 */
	public void setDefaultCost(double defaultCost)
	{
		m_defaultCost = (float)defaultCost;
		m_clientSetDefaultCost = true;
		m_costs.setDefaultValue(m_defaultCost);
	}
	
	public int getSrcPidIndex(String pid)
	{
		return m_srcPids.getIndex(pid);
	}
	
	public int getDestPidIndex(String pid)
	{
		return m_destPids.getIndex(pid);
	}
	
	public int getNumSrcPids()
	{
		return m_srcPids.size();
	}
	
	public int getNumDestPids()
	{
		return m_destPids.size();
	}
	
	public String getSrcPidName(int index)
	{
		return m_srcPids.getString(index);
	}
	
	public String getDestPidName(int index)
	{
		return m_destPids.getString(index);
	}
	
	/**
	 * Return an array with the source pid names, in index order.
	 * CAVEAT: Do not change the array elements.
	 * @return An array with the source pid names, in index order.
	 */
	public String[] getSrcPIDs()
	{
		return m_srcPids.toArray();
	}
	
	/**
	 * Return an array with the destination pid names, in index order.
	 * CAVEAT: Do not change the array elements.
	 * @return An array with the destination pid names, in index order.
	 */
	public String[] getDestPIDs()
	{
		return m_destPids.toArray();
	}
	
	/**
	 * Return an array with the destination pid names, in index order.
	 * CAVEAT: Do not change the array elements.
	 * @param src A specific source pid name.
	 * 		Ignored; this method returns all destination pids,
	 * 		not just those for "src". This method exists
	 * 		for compatibility with {@link AltoResp_CostMap}.
	 * @return An array with the destination pid names, in index order.
	 */
	public String[] getDestPIDs(String src)
	{
		return m_destPids.toArray();
	}
	
	/**
	 * Set a cost.
	 * @param srcPid The source PID.
	 * @param destPid The destination PID.
	 * @param cost The new cost.
	 */
	public void setCost(String srcPid, String destPid, double cost)
	{
		int srcIndex = m_srcPids.makeIndex(srcPid);
		int destIndex = m_destPids.makeIndex(destPid);
		m_costs.set(srcIndex, destIndex, (float)cost);
	}
	
	/**
	 * Return the cost for a source/dest pair.
	 * @param srcPid Name of source pid.
	 * @param destPid Name of destination pid.
	 * @return The cost for src to dest.
	 * 		Return the default cost if srcPid or destPid aren't valid.
	 */
	public double getCost(String srcPid, String destPid)
	{
		return getCost(m_srcPids.getIndex(srcPid), m_destPids.getIndex(destPid));
	}
	
	/**
	 * Return the cost for a source/dest pair.
	 * @param srcIndex Index of source pid.
	 * @param destIndex Index of destination pid.
	 * @return The cost for src to dest.
	 * 		Return the default cost if srcIndex or destIndex are out of range.
	 */
	public double getCost(int srcIndex, int destIndex)
	{
		if (srcIndex < 0 || srcIndex >= m_srcPids.size()
				|| destIndex < 0 || destIndex >= m_destPids.size()) {
			return m_defaultCost;
		} else {
			return m_costs.getDouble(srcIndex, destIndex);
		}
	}

	private class MyJSONScanner extends GenericJSONScanner
	{
		private static final String FPATH_META = FN_META;
		private static final String FPATH_COST_MAP = FN_COST_MAP;
		private static final String FPATH_DEPENDENT_RESOURCE_ID = FN_META + "." + FN_DEPENDENT_VTAGS + "." + FN_RESOURCE_ID;
		private static final String FPATH_DEPENDENT_TAG = FN_META + "." + FN_DEPENDENT_VTAGS + "." + FN_TAG;
		private static final String FPATH_COST_METRIC = FN_META + "." + FN_COST_TYPE + "." + FN_COST_TYPE_METRIC;
		private static final String FPATH_COST_MODE = FN_META + "." + FN_COST_TYPE + "." + FN_COST_TYPE_MODE;
		private static final String FPATH_DEFAULT_COST = FN_META + "." + FN_DEFAULT_COST;

		MyJSONScanner(IJSONLexan lexan)
		{
			super(lexan);
		}
		
		private String m_srcPid = null;
		private Stack<JSONValue> m_metaStack = new Stack<JSONValue>();
		
		@Override
		public void enterDictionary() throws JSONParseException
		{
			JSONValue topMeta = !m_metaStack.empty() ? m_metaStack.peek() : null;
			if (topMeta != null
					|| (inDictionary() && getDictionaryKeyPath().equals(FPATH_META))) {
				JSONValue_Object newDict = (topMeta != null) ? new JSONValue_Object(): getMeta();
				m_metaStack.push(newDict);
				if (topMeta instanceof JSONValue_Object) {
					((JSONValue_Object) topMeta).put(getTopDictionaryKey(), newDict);
				} else if (topMeta instanceof JSONValue_Array) {
					((JSONValue_Array) topMeta).add(newDict);
				}
			}
		}

		@Override
		public void leaveDictionary() throws JSONParseException
		{
			if (!m_metaStack.empty()) {
				m_metaStack.pop();
			} else {
				m_srcPid = null;
			}
		}

		@Override
		public void gotDictionaryValue(JSONValue value) throws JSONParseException
		{
			if (m_metaStack.empty()) {
				if (value instanceof JSONValue_Number) {
					double dvalue = ((JSONValue_Number)value).m_value;
					if (getDictionaryKeyPath().equals(FPATH_DEFAULT_COST)) {
						setDefaultCost(dvalue);
					} else if (getNestedDictionaryDepth() == 3 && m_srcPid != null) {
						setCost(m_srcPid, getTopDictionaryKey(), dvalue);
					}
				} else if (value instanceof JSONValue_Null) {
					// Just ignore nulls.
				} else {
					// OOPS -- value is something other than a number or null.
					// Ignore for now?
				}
			} else {
				Object topMeta = m_metaStack.peek();
				if (topMeta instanceof JSONValue_Object) {
					((JSONValue_Object)topMeta).put(getTopDictionaryKey(), value);
				} else if (topMeta instanceof JSONValue_Array) {
					((JSONValue_Array)topMeta).add(value);
				}
				if (value instanceof JSONValue_Number) {
					double dvalue = ((JSONValue_Number)value).m_value;
					if (getDictionaryKeyPath().equals(FPATH_DEFAULT_COST)) {
						setDefaultCost(dvalue);
					}
				} else if (value instanceof JSONValue_String) {
					String svalue = ((JSONValue_String)value).m_value;
					String path = getDictionaryKeyPath();
					if (path.equals(FPATH_DEPENDENT_RESOURCE_ID)) {
						setDependentVtag(svalue, getDependentTag());
					} else if (path.equals(FPATH_DEPENDENT_TAG)) {
						setDependentVtag(getDependentResourceId(), svalue);
					} else if (path.equals(FPATH_COST_METRIC)) {
						setCostMetric(svalue);
					} else if (path.equals(FPATH_COST_MODE)) {
						setCostMode(svalue);
					}
				}
			}
		}

		@Override
		public void gotDictionaryKey(String key) throws JSONParseException
		{
			if (getParentDictionaryKeyPath().equals(FPATH_COST_MAP)) {
				m_srcPid = key;
			}
		}
		
		@Override
		public void enterArray() throws JSONParseException
		{
			Object topMeta = !m_metaStack.empty() ? m_metaStack.peek() : null;
			if (topMeta != null) {
				JSONValue_Array newArray = new JSONValue_Array();
				m_metaStack.push(newArray);
				if (topMeta instanceof JSONValue_Object) {
					((JSONValue_Object) topMeta).put(getTopDictionaryKey(), newArray);
				} else if (topMeta instanceof JSONValue_Array) {
					((JSONValue_Array) topMeta).add(newArray);
				}
			}
		}

		@Override
		public void leaveArray() throws JSONParseException
		{
			if (!m_metaStack.empty()) {
				m_metaStack.pop();
			} else {
				m_srcPid = null;
			}
		}

		@Override
		public void gotArrayValue(JSONValue value) throws JSONParseException
		{
			if (!m_metaStack.empty()) {
				Object topMeta = m_metaStack.peek();
				if (topMeta instanceof JSONValue_Array) {
					((JSONValue_Array)topMeta).add(value);
				}
			}
		}
	}
	
	/**
	 * Write the JSON message using client-specified formatting.
	 * Because this class does NOT store the costs in the m_json field,
	 * we must override the base-class method.
	 * @param writer A JSON writer provided by the client.
	 * @throws IOException If writer throws an I/O error.
	 * @see RT_Base#writeJSON(JSONWriter)
	 */
	@Override
	public void writeJSON(JSONWriter writer) throws IOException
	{
		boolean indent = writer.isIndented();
		writer.write('{');
		if (indent) {
			writer.writeNewline();
			writer.incrIndent(1);
		}
		
		writer.write("\"" + FN_COST_MAP + "\": {");
		writer.incrIndent(1);

		int nSrc = m_srcPids.size();
		int nDest = m_destPids.size();
		for (int iSrc = 0; iSrc < nSrc; iSrc++) {
			if (indent) {
				writer.writeNewline();
			};
			writer.write('"');
			writer.write(StringUtils.escapeSimpleJSONString(m_srcPids.getString(iSrc)));
			writer.write("\": {");
			if (indent) {
				writer.writeNewline();
				writer.incrIndent(1);
			}
			int nEntries = 0;
			for (int iDest = 0; iDest < nDest; iDest++) {
				float cost = m_costs.get(iSrc, iDest);
				if (!Float.isNaN(cost) && cost != m_defaultCost) {
					if (nEntries > 0) {
						writer.write(',');
					}
					if (indent && ((nEntries) % 4) == 0) {
						writer.writeNewline();
					} else {
						writer.write(' ');
					}
					writer.write("\"");
					writer.write(StringUtils.escapeSimpleJSONString(m_destPids.getString(iDest)));
					writer.write("\": ");
					writer.write(Float.toString(cost));
					nEntries++;
				}
			}
			if (indent) {
				writer.writeNewline();
			}
			writer.write('}');
			if (iSrc+1 < nSrc) {
				writer.write(',');
			}
			writer.incrIndent(-1);
		}
		if (indent) {
			writer.writeNewline();
		};
		
		writer.write("},");
		if (indent) {
			writer.writeNewline();
			writer.incrIndent(-1);
		}
		
		writer.write("\"" + FN_META + "\": ");
		getMeta().writeJSON(writer);
		
		if (indent) {
			writer.writeNewline();
			writer.incrIndent(-1);
		}
		writer.write('}');
	}
			
	/**
	 * Write the cost-map as compact json-encoded data.
	 * @param out The stream to write on.
	 * @throws JSONException 
	 */
	public void writeJSON(PrintStream out)
	{
		out.print("{\"" + FN_META + "\":{");
		try {
			if (m_clientSetDefaultCost)
				writeKeyValue(out, FN_DEFAULT_COST, getDefaultCost(), ",");
			writeVtag(out, getDependentResourceId(), getDependentTag(), ",");			
			writeCostMetricMode(out, getCostMetric(), getCostMode(), null);
			out.println("}");
		} catch (Exception e) {
			out.print("}");
		}
		out.print(",\"" + FN_COST_MAP + "\":{");
		
		int nSrc = m_srcPids.size();
		int nDest = m_destPids.size();
		String srcSep = "\"";
		for (int iSrc = 0; iSrc < nSrc; iSrc++) {
			out.print(srcSep);
			out.print(StringUtils.escapeSimpleJSONString(m_srcPids.getString(iSrc)));
			String destSep = "\":{";
			int nEntries = 0;
			for (int iDest = 0; iDest < nDest; iDest++) {
				float cost = m_costs.get(iSrc, iDest);
				if (!Float.isNaN(cost) && cost != m_defaultCost) {
					out.print(destSep);
					out.print("\"");
					out.print(StringUtils.escapeSimpleJSONString(m_destPids.getString(iDest)));
					out.print("\":");
					out.print(cost);
					nEntries++;
					destSep = ",";
				}
			}
			if (nEntries == 0)
				out.print(destSep);
			srcSep = "},\"";
		}
		out.print("}}}");
		out.flush();
	}
	
	/**
	 * For testing, write the cost-map in an alternate, more efficient json format.
	 * NOTE: This is NOT defined by the ALTO protocol spec.
	 * @param out The stream to write on.
	 * @throws JSONException 
	 */
	public void altWriteJSON(PrintStream out)
	{
		out.print("{\"" + FN_META + "\":{");
		try {
			if (m_clientSetDefaultCost)
				writeKeyValue(out, FN_DEFAULT_COST, getDefaultCost(), ",");
			writeVtag(out, getDependentResourceId(), getDependentTag(), ",");			
			writeCostMetricMode(out, getCostMetric(), getCostMode(), null);
			out.println("}");
		} catch (Exception e) {
			out.print("}");
		}
		out.print(",\"" + FN_COST_MAP + "\":{");
		
		int nSrc = m_srcPids.size();
		out.print("\"srcs\":[");
		for (int i = 0; i < nSrc; i++) {
			if (i > 0)
				out.print("\",\"");
			else
				out.print('\"');
			out.print(m_srcPids.getString(i));
		}
		if (nSrc > 0)
			out.print('\"');
		out.print("],");
			
		int nDest = m_destPids.size();
		out.print("\"dests\":[");
		for (int i = 0; i < nDest; i++) {
			if (i > 0)
				out.print("\",\"");
			else
				out.print('\"');
			out.print(m_destPids.getString(i));
		}
		if (nDest > 0)
			out.print('\"');
		out.print("],");
		
		out.print("\"costs\":[");
		for (int iSrc = 0; iSrc < nSrc; iSrc++) {
			if (iSrc > 0)
				out.print(",[");
			else
				out.print('[');
			for (int iDest = 0; iDest < nDest; iDest++) {
				if (iSrc > 0 || iDest > 0)
					out.print(",");
				out.print(m_costs.get(iSrc, iDest));
			}
			out.print(']');
		}
		out.print("]}}}");
		out.flush();
	}
	
	/**
	 * Return the formatted json-encoded cost-map message.
	 * WARNING: If there are a lot of PIDs, this will run out of memory.
	 */
	@Override
	public String toString()
	{
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		PrintStream pout = new PrintStream(bout);
		writeJSON(pout, "  ");
		pout.flush();
		return bout.toString();
	}
	
	/**
	 * Write the json-encoded cost-map message in a more readable format.
	 * @param out The stream to write on.
	 * @param indent The number of characters to indent for each level.
	 */
	public void writeJSON(PrintStream out, String indent)
	{
		String indent2 = indent + indent;
		String newLineIndent = "\n" + indent;
		String commaNewLineIndent = ",\n" + indent;

		out.print("{\"" + FN_META + "\":{" + newLineIndent);
		try {
			if (m_clientSetDefaultCost)
				writeKeyValue(out, FN_DEFAULT_COST, getDefaultCost(), commaNewLineIndent);
			writeVtag(out, getDependentResourceId(), getDependentTag(), commaNewLineIndent);			
			writeCostMetricMode(out, getCostMetric(), getCostMode(), newLineIndent);
			out.println("},");
		} catch (Exception e) {
			out.println("},");
		}
		out.println("\"" + FN_COST_MAP + "\": {");
		
		int nSrc = m_srcPids.size();
		int nDest = m_destPids.size();
		String srcSep = "\"";
		for (int iSrc = 0; iSrc < nSrc; iSrc++) {
			out.print(indent);
			out.print(srcSep);
			out.print(StringUtils.escapeSimpleJSONString(m_srcPids.getString(iSrc)));
			String destSep = "\": {\n" + indent2;
			int nEntries = 0;
			for (int iDest = 0; iDest < nDest; iDest++) {
				float cost = m_costs.get(iSrc, iDest);
				if (!Float.isNaN(cost) && cost != m_defaultCost) {
					out.print(destSep);
					out.print("\"");
					out.print(StringUtils.escapeSimpleJSONString(m_destPids.getString(iDest)));
					out.print("\":");
					out.print(cost);
					nEntries++;
					if (((nEntries) % 5) == 0)
						destSep = ",\n" + indent2;
					else
						destSep = ", ";
				}
			}
			if (nEntries == 0)
				out.print(destSep);
			srcSep = "\n" + indent + "},\n" + indent + "\"";
		}
		out.println("\n}}}");
		out.flush();
	}
	
	private void writeVtag(PrintStream out, String id, String tag, String suffix)
	{
		out.print("\"");
		out.print(FN_DEPENDENT_VTAGS);
		out.print("\":[{\"");
		out.print(FN_RESOURCE_ID);
		out.print("\":\"");
		out.print(StringUtils.escapeSimpleJSONString(id));
		out.print("\",\"");
		out.print(FN_TAG);
		out.print("\":\"");
		out.print(StringUtils.escapeSimpleJSONString(tag));
		out.print("\"}]");
		if (suffix != null)
			out.print(suffix);
	}
	
	private void writeCostMetricMode(PrintStream out, String metric, String mode, String suffix)
	{
		out.print("\"");
		out.print(FN_COST_TYPE);
		out.print("\":{\"");
		out.print(FN_COST_TYPE_METRIC);
		out.print("\":\"");
		out.print(metric);
		out.print("\",\"");
		out.print(FN_COST_TYPE_MODE);
		out.print("\":\"");
		out.print(mode);
		out.print("\"}");
		if (suffix != null)
			out.print(suffix);
	}
	
	@SuppressWarnings("unused")
	private void writeKeyValue(PrintStream out, String key, String value, String suffix)
	{
		if (value != null) {
			out.print("\"");
			out.print(StringUtils.escapeSimpleJSONString(key));
			out.print("\":\"");
			out.print(StringUtils.escapeSimpleJSONString(value));
			out.print("\"");
			if (suffix != null)
				out.print(suffix);
		}
	}
	
	private void writeKeyValue(PrintStream out, String key, double value, String suffix)
	{
		out.print("\"");
		out.print(StringUtils.escapeSimpleJSONString(key));
		out.print("\":");
		if (Double.isNaN(value))
			out.print("\"NaN\"");
		else
			out.print(value);
		if (suffix != null)
			out.print(suffix);
	}
	
	/**
	 * Write compact JSON for a cost-map response message.
	 * @param out The output stream.
	 * @param costMap The source of cost map data.
	 * @param costMode The mode to give in the message.
	 * @param fullOrdinal If true and costMode is ordinal,
	 * 		ordinalize the costs into 1, 2, 3, etc.
	 * 		If false and costMode is ordinal, round the costs
	 * 		to the nearest integer.
	 */
	public static void writeCostMap(PrintStream out,
							 AltoCostMapInfo costMap,
							 String costMode,
							 boolean fullOrdinal)
	{
		out.print("{\"meta\":{\"dependent-vtags\":[{\"resource-id\":\"");
		out.print(StringUtils.escapeSimpleJSONString(costMap.getMapId()));
		out.print("\",\"tag\":\"");
		out.print(StringUtils.escapeSimpleJSONString(costMap.getMapVtag()));
		out.print("\"}],");
		out.print("\"cost-type\":{\"cost-metric\":\"");
		out.print(StringUtils.escapeSimpleJSONString(costMap.getCostMetric()));
		out.print("\",\"cost-mode\":\"");
		out.print(StringUtils.escapeSimpleJSONString(costMode));
		out.print("\"}}");
		out.print(",\"cost-map\":{");
		
		int nPids = costMap.getNumPids();
		Ordinalizer ord = null;
		boolean intCosts = false;
		if (AltoResp_CostMap.COST_MODE_ORDINAL.equals(costMode)) {
			if (fullOrdinal) {
				ord = new Ordinalizer();
				for (int iSrc = 0; iSrc < nPids; iSrc++) {
					for (int iDest = 0; iDest < nPids; iDest++) {
						double cost = costMap.getCost(iSrc, iDest);
						if (cost >= 0.0)
							ord.addValue(cost);
					}
				}
				ord.doneAdding();
			} else {
				intCosts = true;
			}
		}
		String srcSep = "\"";
		for (int iSrc = 0; iSrc < nPids; iSrc++) {
			out.print(srcSep);
			out.print(StringUtils.escapeSimpleJSONString(costMap.indexToPid(iSrc)));
			String destSep = "\":{";
			int nEntries = 0;
			for (int iDest = 0; iDest < nPids; iDest++) {
				double cost = costMap.getCost(iSrc, iDest);
				if (cost >= 0.0) {
					if (ord != null)
						cost = ord.getOrdinal(cost);
					else if (intCosts)
						cost = Math.round(cost);
					out.print(destSep);
					out.print("\"");
					out.print(StringUtils.escapeSimpleJSONString(costMap.indexToPid(iDest)));
					out.print("\":");
					out.print(cost);
					destSep = ",";
					nEntries++;				}
			}
			if (nEntries == 0)
				out.print(destSep);
			srcSep = "},\"";
		}
		out.print("}}}");
	}
}
