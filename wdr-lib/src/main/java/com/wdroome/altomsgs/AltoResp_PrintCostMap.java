package com.wdroome.altomsgs;

import java.io.PrintStream;
import java.util.Arrays;

import com.wdroome.json.JSONException;

/**
 * <p>
 * Pretty-print a Cost Map.
 * This object controls the output formatting.
 * This class can print a Cost Map in "matrix mode" or "regular mode."
 * Matrix mode is a table, n(sources) x n(destinations),
 * with one row per source and one column per destination.
 * Regular (non-matrix) mode has each source pid on a line by itself,
 * followed by one or more lines with "dest-pid=###" entries.
 * The class selects the mode (see {@link #setMaxMatrixLineLength(int)}).
 * </p>
 * <p>
 * The object itself just contains formatting parameters.
 * No data specific to a cost map is stored in the object.
 * So you can use the same object to print multiple cost maps concurrently.
 * </p>
 * @author wdr
 */
public class AltoResp_PrintCostMap
{
	private String m_linePrefix = "  ";
	private String m_lineSuffix = System.lineSeparator();
	private boolean m_equalColWidths = false;
	private String m_costSep = "  ";
	private String m_colSep = " ";
	private int m_minColWidth = 0;
	private int m_maxMatrixLineLength = 80;
	private String m_missingCost = "-";
		
	/**
	 * Create a new object.
	 */
	public AltoResp_PrintCostMap()
	{
	}
	
	/**
	 * Print a Cost Map.
	 * @param out The output stream.  If null, use stdout.
	 * @param costMap The Cost Map.
	 */
	public void print(PrintStream out, AltoResp_IndexedCostMap costMap)
	{
		printCostMap(out, new IndexedCostMapData(costMap));
	}
	
	/**
	 * Print a Cost Map.
	 * @param out The output stream.  If null, use stdout.
	 * @param costMap The Cost Map.
	 */
	public void print(PrintStream out, AltoResp_CostMap costMap)
	{
		printCostMap(out, new SimpleCostMapData(costMap));
	}

	/**
	 * Return the prefix string for all lines.
	 * @return The prefix string for all lines.
	 */
	public String getLinePrefix()
	{
		return m_linePrefix;
	}

	/**
	 * Set the prefix string for all lines. Default is two spaces.
	 * @param linePrefix The new line prefix.
	 */
	public void setLinePrefix(String linePrefix)
	{
		m_linePrefix = linePrefix != null ? linePrefix : "";
	}

	/**
	 * Return the suffix and terminator for all lines.
	 * This normally includes a new-line.
	 * @return The suffix and terminator for all lines.
	 */
	public String getLineSuffix()
	{
		return m_lineSuffix;
	}

	/**
	 * Set the suffix and terminator for all lines.
	 * This normally includes a new-line.
	 * The default is System.lineSeparator().
	 * @param lineSuffix The new line suffix and terminator.
	 */
	public void setLineSuffix(String lineSuffix)
	{
		m_lineSuffix = lineSuffix != null ? lineSuffix : "";
	}

	/**
	 * Return true iff matrix columns are forced to be uniform width.
	 * @return True iff matrix columns are forced to be uniform width.
	 */
	public boolean isEqualColWidths()
	{
		return m_equalColWidths;
	}

	/**
	 * Set whether or not matrix columns are forced to be the uniform width.
	 * @param equalColWidths If true, make all matrix columns the same width.
	 */
	public void setEqualColWidths(boolean equalColWidths)
	{
		m_equalColWidths = equalColWidths;
	}

	/**
	 * Return the matrix-mode column separator.
	 * @return the matrixColSep
	 */
	public String getColSep()
	{
		return m_colSep;
	}

	/**
	 * Set the matrix-mode column separator.
	 * The default is one space.
	 * @param colSep The new matrix-mode column separator.
	 */
	public void setColSep(String colSep)
	{
		m_colSep = colSep != null ? colSep : "";
	}

	/**
	 * Return the non-matrix-mode cost separator.
	 * @return the colSep
	 */
	public String getCostSep()
	{
		return m_costSep;
	}

	/**
	 * Set the non-matrix-mode cost separator.
	 * Default is two spaces.
	 * @param costSep The new non-matrix-mode cost separator.
	 */
	public void setCostSep(String costSep)
	{
		m_costSep = costSep;
	}

	/**
	 * Return the matrix-mode minimum column width.
	 * @return The matrix-mode minimum column width.
	 */
	public int getMinColWidth()
	{
		return m_minColWidth;
	}

	/**
	 * Set the matrix-mode minimum column width.
	 * Default is 0.
	 * @param minColWidth The new matrix-mode minimum column width.
	 */
	public void setMinColWidth(int minColWidth)
	{
		m_minColWidth = minColWidth;
	}

	/**
	 * Return the maximum line length for printing costs
	 * in matrix mode.
	 * @return The maximum line length for printing costs
	 * 		in matrix mode.
	 */
	public int getMaxMatrixLineLength()
	{
		return m_maxMatrixLineLength;
	}

	/**
	 * Set the maximum line length for printing costs
	 * in matrix mode. Use regular mode if the matrix
	 * lines would be longer than this. The maximum
	 * length is compared to the data part of the matrix:
	 * the length of the source column and the cost columns.
	 * It does not consider line prefix and suffix.
	 * The default is 80.
	 * @param maxMatrixWidth
	 * 		The new maximum line length for printing costs in matrix mode.
	 */
	public void setMaxMatrixLineLength(int maxMatrixWidth)
	{
		m_maxMatrixLineLength = maxMatrixWidth;
	}

	/**
	 * Print a cost map.
	 * @param out The output stream.
	 * @param costMap A generic cost map.
	 */
	private void printCostMap(PrintStream out, CostMapData costMap)
	{
		if (out == null) {
			out = System.out;
		}
		
		// Get source & destination PIDs.
		String[] srcPids = costMap.getSortedSrcPIDs();
		String[] destPids = costMap.getSortedDestPIDs();
		
		// Calculate column widths if we were to use matrix mode.
		int srcColWidth = getMaxLength(srcPids);
		int[] destColWidths = new int[destPids.length];
		int maxCostColWidth = 0;
		for (int iDest = 0; iDest < destPids.length; iDest++) {
			destColWidths[iDest] = destPids[iDest].length();
			for (int iSrc = 0; iSrc < srcPids.length; iSrc++) {
				int width = costMap.getStringCost(srcPids[iSrc], destPids[iDest],
												m_missingCost).length();
				if (width > destColWidths[iDest]) {
					destColWidths[iDest] = width;
				}
			}
			if (destColWidths[iDest] < m_minColWidth) {
				destColWidths[iDest] = m_minColWidth;
			}
			if (destColWidths[iDest] > maxCostColWidth) {
				maxCostColWidth = destColWidths[iDest];
			}
		}
		if (m_equalColWidths) {
			for (int i = 0; i < destColWidths.length; i++) {
				destColWidths[i] = maxCostColWidth;
			}
		}
		
		// Determine line length if we were to use matrix mode.
		int matrixLineLength = srcColWidth;
		for (int iDest = 0; iDest < destPids.length; iDest++) {
			matrixLineLength += m_colSep.length() + destColWidths[iDest];
		}
		
		// Decide whether to use regular mode or matrix mode. Finally!!
		if (matrixLineLength > m_maxMatrixLineLength) {
			
			// Regular mode.
			for (String srcPid: srcPids) {
				out.print(m_linePrefix + srcPid + " =>" + m_lineSuffix);
				int n = 0;
				for (String dstPid: destPids) {
					String cost = costMap.getStringCost(srcPid,  dstPid, null);
					if (cost != null) {
						out.print((n == 0 ? (m_linePrefix + m_costSep) : m_costSep) + dstPid + "=" + cost);
						if (++n >= 5) {
							out.print(m_lineSuffix);
							n = 0;
						}
					}
				}
				if (n > 0) {
					out.print(m_lineSuffix);
				}
			}
		} else {
			
			// Matrix (table) mode.
			out.print(m_linePrefix);
			prtRightAlign(out, "", srcColWidth);
			for (int iDst = 0; iDst < destPids.length; iDst++) {
				out.print(m_colSep);
				prtRightAlign(out, destPids[iDst], destColWidths[iDst]);
			}
			out.print(m_lineSuffix);
			
			for (String srcPid: srcPids) {
				out.print(m_linePrefix);
				prtRightAlign(out, srcPid, srcColWidth);
				for (int iDest = 0; iDest < destPids.length; iDest++) {
					String costStr = costMap.getStringCost(srcPid,  destPids[iDest],
															m_missingCost);
					out.print(m_colSep);
					prtRightAlign(out, costStr, destColWidths[iDest]);
				}
				out.print(m_lineSuffix);
			}
		}
	}
	
	/**
	 * Print a string right-aligned in a column.
	 * @param out The output stream.
	 * @param s The string to print.
	 * @param width The column width.
	 */
	private void prtRightAlign(PrintStream out, String s, int width)
	{
		int len = s.length();
		for (int i = width - len; i > 0; --i) {
			out.print(' ');
		}
		out.print(s);
	}
	
	/**
	 * Return the length of the longest string in an array.
	 * @param arr An array of strings.
	 * @return The length of the longest string in "arr".
	 */
	private int getMaxLength(String[] arr)
	{
		int maxLen = 0;
		for (String s: arr) {
			if (s != null) {
				int len = s.length();
				if (len > maxLen) {
					maxLen = len;
				}
			}
		}
		return maxLen;
	}

	/**
	 * Return a sorted copy of an array.
	 * @param in The input array.
	 * @return A sorted copy of "in". If "in" is null, return a 0-length array.
	 */
	private static String[] copyAndSort(String[] in)
	{
		if (in == null || in.length == 0) {
			return new String[0];
		}
		String[] out = Arrays.copyOf(in,  in.length);
		Arrays.sort(out);
		return out;
	}
	
	/**
	 * A generic cost map. Provides sorted PID names and string-valued costs. Period.
	 * Cost-map-specific child classes extend this base class.
	 * This should be an interface, but Java doesn't like
	 * private interfaces.
	 */
	private abstract static class CostMapData
	{
		protected abstract String[] getSortedSrcPIDs();
		protected abstract String[] getSortedDestPIDs();
		
		/**
		 * Return a string with the cost from srcPid to dstPid.
		 * If not specified in the cost map, return "def".
		 * @param srcPid The source pid.
		 * @param dstPid The destination pid.
		 * @param def The default string for omitted costs.
		 * @return The cost from srcPid to dstPid, as a string, or def.
		 */
		protected abstract String getStringCost(String srcPid, String dstPid, String def);
	};
	
	/**
	 * A CostMapData object for cost data stored in an RT_IndexedCostMap.
	 */
	private static class IndexedCostMapData extends CostMapData
	{
		private final AltoResp_IndexedCostMap m_costMap;
		
		private IndexedCostMapData(AltoResp_IndexedCostMap costMap)
		{
			m_costMap = costMap;
		}
		
		protected String[] getSortedSrcPIDs()
		{
			return copyAndSort(m_costMap.getSrcPIDs());
		}
		
		protected String[] getSortedDestPIDs()
		{
			return copyAndSort(m_costMap.getDestPIDs());
		}
		
		protected String getStringCost(String srcPid, String dstPid, String def)
		{
			double cost = m_costMap.getCost(srcPid, dstPid);
			if (!Double.isNaN(cost) && cost >= 0) {
				return Double.toString(cost);
			} else {
				return def;
			}
		}
	}
	
	/**
	 * A CostMapData object for cost data stored in an RT_CostMap.
	 */
	private static class SimpleCostMapData extends CostMapData
	{
		private final AltoResp_CostMap m_costMap;
		
		private SimpleCostMapData(AltoResp_CostMap costMap)
		{
			m_costMap = costMap;
		}
		
		protected String[] getSortedSrcPIDs()
		{
			return copyAndSort(m_costMap.getSrcPIDs());
		}
		
		protected String[] getSortedDestPIDs()
		{
			return copyAndSort(m_costMap.getDestPIDs());
		}
		
		protected String getStringCost(String srcPid, String dstPid, String def)
		{
			try {
				return Double.toString(m_costMap.getCost(srcPid, dstPid));
			} catch (JSONException e) {
				return def;
			}
		}
	}
}
