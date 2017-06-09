package com.wdroome.altodata;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.wdroome.util.inet.CIDRAddress;
import com.wdroome.util.String2;
import com.wdroome.util.IErrorLogger;
import com.wdroome.util.SystemErrorLogger;

/**
 * Read and parse a XML cost map  specification file.
 * <br>
<pre>
  The root element is &lt;cost-maps&gt;, with a &lt;cost-map&gt; child tag
  for each distinct cost map. If there is only one cost map,
  you can use its &lt;cost-map&gt; tag as the root element.  

  &lt;cost-map&gt;:
      The &lt;cost-map&gt; tag has three attributes.
      
      The "metric" attribute is the name of the cost metric, and is required.
      
      The "network-id" attribute is the resource id of the associated network map.
      If the server provides only one network map, this attribute is optional.
      Otherwise it is required.
      
      Note that this means there is only one cost map for each
      (network-map,cost-metric) pair, and it has the numerical-mode values.
      If the IRD specifies an ordinal-mode cost map resource,
      the ALTO server creates ordinal values from the numerical values
      defined in this file. This means you cannot specify ordinal-mode
      values directly.
      
      The optional "default" attribute gives a default cost value
      for any (src,dst) pair for which a cost is not explicitly defined.
      
  &lt;cost&gt;:
      The &lt;cost&gt; tag specifies the cost from a source PID to a destination PID,
      and must appear inside a &lt;cost-map&gt; tag. &lt;cost-map&gt; has four attributes:
      "src", "dst", "value" and "default". "src" and "dst" are the names of
      source and destination PIDs. "value" and "default" are cost values, and
      cannot appear in the same &lt;cost&gt; tag.
      
      &lt;cost&gt; tags can nest to factor out common elements, such as source or
      destination PIDs. To determine the full cost matrix, we do a depth-first
      walk of the tree of &lt;cost&gt; tags. If we reach a &lt;cost&gt; tag where that tag,
      or its parents, define "src", "dst" and "value" attributes, we set the
      cost from "src" to "dst" to "value".
      
      Otherwise, when we encounter a &lt;cost&gt; tag with a "default" attribute, we
      search up the chain of &lt;cost&gt; tags to find a "src" or "dst" attribute. If
      there is a "src" but no "dst", we set the cost from "src" to all
      destinations to the "default" value. If there is a "dst" but no "src", we
      set the cost to "dst" from all sources to the "default" value. If there is
      both a "src" and a "dst", we set the cost from "src" to "dst" to the
      "default" value. However, that is pointless; you should use the "value"
      attribute instead.
      
      &lt;cost&gt; tags are processed depth-first, in the order presented
      in the file. A &lt;cost&gt; tag may override a previously set cost.
      
      The string "NaN" (Not A Number) is legal as a cost value, and means "this
      cost point does not exist." The ALTO server will delete any NaN-valued
      costs from the cost maps it presents to the clients.
      
  &lt;symmetric&gt;:
      A &lt;symmetric&gt; tag must also appear inside a &lt;cost-map&gt; tag. Any &lt;cost&gt;
      tags nested inside a &lt;symmetric&gt; tag set costs symmetrically. That is,
      when a &lt;cost&gt; tag sets the cost from pid1 to pid2, it also sets the cost
      from pid2 to pid1 to the same value.

  Abbreviations:
      You can abbreviate the &lt;cost&gt; tag as &lt;c&gt;.
      
      You can abbreviate the "src", "dst", "value" and "default" attributes
      as "s", "d", "v" and "def", respectively.
</pre>
 *
 * @author wdr
 */
public class CostMapReader
{
	public static final String TN_COST_MAPS = "cost-maps";
	public static final String TN_COST_MAP = "cost-map";
	public static final String TN_COST = "cost";
	public static final String TN_COST_ABBR = "c";
	public static final String TN_SYMMETRIC = "symmetric";
	
	public static final String AN_NETWORK_ID = "network-id";
	public static final String AN_METRIC = "metric";
	public static final String AN_SRC = "src";
	public static final String AN_SRC_ABBR = "s";
	public static final String AN_DST = "dst";
	public static final String AN_DST_ABBR = "d";
	public static final String AN_VALUE = "value";
	public static final String AN_VALUE_ABBR = "v";
	public static final String AN_DEFAULT = "default";
	public static final String AN_DEFAULT_ABBR = "def";
	
	/**
	 * A (src-pid,dst-pid,cost-value) triple.  All three items are optional.
	 * Because the data components are simple and final,
	 * we used public members instead of defining getter methods.
	 * Note that the cost value is a Double object, not a primitive double;
	 * if there is no cost value, m_value is null.
	 * @author wdr
	 */
	private static class CostElem
	{
		public final String m_src;
		public final String m_dst;
		public final Double m_value;
	
		public CostElem(String src, String dst, Double value)
		{
			m_src = src;
			m_dst = dst;
			m_value = value;
		}
	}

	/**
	 * A SAX handler for the XML cost map specification.
	 * We use the SAX model, rather than the DOM model,
	 * because network maps can be large and the XML tree
	 * might not fit in memory.
	 * 
	 * @author wdr
	 */
	private static class CostMapScanner extends DefaultHandler
	{
		private final AltoData m_altoData;
		private final String m_defaultMapId;
		private final IErrorLogger m_errorLogger;
		private final long m_currentTS = System.currentTimeMillis();
		private Locator m_locator = null;
		
		private NetworkMap m_networkMap = null;
		private CostMap m_costMap = null;
		private String m_metric = null;
		private Stack<CostElem> m_costStack = new Stack<CostElem>();
		private int m_symmetricCount = 0;
		
		private enum STATE {ROOT, COST_MAP, COST};
		private STATE m_state = STATE.ROOT;
		
		public CostMapScanner(AltoData altoData,
							  String defaultMapId,
							  IErrorLogger errorLogger)
		{
			m_defaultMapId = defaultMapId;
			m_altoData = altoData;
			if (errorLogger == null)
				errorLogger = new SystemErrorLogger();
			m_errorLogger = errorLogger;
		}

		/* (non-Javadoc)
		 * @see org.xml.sax.helpers.DefaultHandler#setDocumentLocator(org.xml.sax.Locator)
		 */
		@Override
		public void setDocumentLocator(Locator locator)
		{
			m_locator = locator;
		}
		
		/* (non-Javadoc)
		 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attrs)
					throws SAXException
		{
			if (TN_COST_MAPS.equals(qName)) {
				if (m_state != STATE.ROOT) {
					throw new SAXParseException("<" + qName + "> at wrong level", m_locator);
				}
				m_state = STATE.COST_MAP;
			} else if (TN_COST_MAP.equals(qName)) {
				if (m_state != STATE.COST_MAP && m_state != STATE.ROOT) {
					throw new SAXParseException("<" + qName + "> at wrong level", m_locator);
				}
				String mapId = getAttr(attrs, AN_NETWORK_ID, null);
				if (mapId == null) {
					mapId = m_defaultMapId;
				}
				if (mapId == null || mapId.equals("")) {
					throw new SAXParseException("<" + qName + ">: missing "
											+ AN_NETWORK_ID + " attribute", m_locator);
				}
				m_networkMap = m_altoData.getNetworkMap(mapId);
				if (m_networkMap == null) {
					throw new SAXParseException("<" + qName + ">: '" + mapId
							+ "' is not a valid network map resource id", m_locator);					
				}
				m_metric = getAttr(attrs, AN_METRIC, null);
				m_costMap = m_altoData.getCostMap(mapId, m_metric);
				if (m_costMap == null) {
					m_costMap = new CostMap(m_metric, m_networkMap, m_errorLogger);
					m_altoData.setCostMap(m_costMap);
				}
				Double def = getDoubleAttr(attrs, AN_DEFAULT, AN_DEFAULT_ABBR);
				if (def != null) {
					m_costMap.setAllCosts(def);
				}
				m_costStack.clear();
				m_symmetricCount = 0;
				m_state = STATE.COST;
			} else if (TN_COST.equals(qName) || TN_COST_ABBR.equals(qName)) {
				if (m_state != STATE.COST) {
					throw new SAXParseException("<" + qName + "> at wrong level", m_locator);
				}
				String src = getAttr(attrs, AN_SRC, AN_SRC_ABBR);
				String dst = getAttr(attrs, AN_DST, AN_DST_ABBR);
				if (checkPid(src) && checkPid(dst)) {
					Double value = getDoubleAttr(attrs, AN_VALUE, AN_VALUE_ABBR);
					Double def = getDoubleAttr(attrs, AN_DEFAULT, AN_DEFAULT_ABBR);
					CostElem costElem = new CostElem(src, dst, value);
					m_costStack.push(costElem);
					String topSrc = null;
					String topDst = null;
					Double topValue = null;
					for (CostElem ce: m_costStack) {
						if (ce.m_src != null) {
							topSrc = ce.m_src;
						}
						if (ce.m_dst != null) {
							topDst = ce.m_dst;
						}
						if (ce.m_value != null) {
							topValue = ce.m_value;
						}
					}
					if (def != null) {
						double d = def;
						if (topSrc != null && topDst == null) {
							for (String pid: m_networkMap.allPids()) {
								m_costMap.setCost(topSrc, pid, d, m_currentTS);
								if (m_symmetricCount > 0) {
									m_costMap.setCost(pid, topSrc, d, m_currentTS);
								}
							}
						} else if (topSrc == null && topDst != null) {
							for (String pid: m_networkMap.allPids()) {
								m_costMap.setCost(pid, topDst, d, m_currentTS);
								if (m_symmetricCount > 0) {
									m_costMap.setCost(topSrc, pid, d, m_currentTS);
								}
							}
						} else if (topSrc != null && topDst != null) {
							if (m_errorLogger != null) {
								StringBuilder b = new StringBuilder();
								b.append("Illegal " + AN_DEFAULT + " attribute '" + def + "'");
								if (m_locator != null) {
									b.append(" line " + m_locator.getLineNumber() + " col "
											+ m_locator.getColumnNumber());
								}
								m_errorLogger.logError(b.toString());
							}
						}
					}
					if (topSrc != null && topDst != null && topValue != null) {
						m_costMap.setCost(topSrc, topDst, topValue, m_currentTS);
						if (m_symmetricCount > 0) {
							m_costMap.setCost(topDst, topSrc, topValue, m_currentTS);
						}
					}
				}
			} else if (TN_SYMMETRIC.equals(qName)) {
				if (m_state != STATE.COST) {
					throw new SAXParseException("<" + qName + "> at wrong level", m_locator);
				}
				m_symmetricCount++;
			} else {
				fatalError(new SAXParseException("Unknown tag <" + qName + ">", m_locator));					
			}
		}

		/* (non-Javadoc)
		 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
		 */
		@Override
		public void endElement(String uri, String localName, String qName)
				throws SAXException
		{
			if (TN_COST_MAPS.equals(qName)) {
				m_state = STATE.ROOT;
			} else if (TN_COST_MAP.equals(qName)) {
				m_state = STATE.COST_MAP;
			} else if (TN_COST.equals(qName) || TN_COST_ABBR.equals(qName)) {
				m_costStack.pop();
			} else if (TN_SYMMETRIC.equals(qName)) {
				m_symmetricCount--;
			}
		}

		/* (non-Javadoc)
		 * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
		 */
		@Override
		public void characters(char[] ch, int start, int length)
				throws SAXException
		{
			if (!isWhiteSpace(ch, start, length)) {
				throw new SAXParseException(
								"Illegal text string '" + new String(ch, start, length) + "'",
								m_locator);
			}
		}
		
		/**
		 * Return the value of an attribute or an alternate abbreviation.
		 * @param attrs The set of attributes.
		 * @param qName1 The attribute name.
		 * @param qName2 An alternate name for the attribute, or null.
		 * @return The value of attribute qName1, or the value of qName2
		 * 			if qName1 is not defined.
		 * 			If neither attribute exists, return null.
		 */
		private String getAttr(Attributes attrs, String qName1, String qName2)
		{
			String value = null;
			if (attrs != null) {
				int i = -1;
				if (qName1 != null) {
					i = attrs.getIndex(qName1);
				}
				if (i < 0 && qName2 != null) {
					i = attrs.getIndex(qName2);
				}
				if (i >= 0) {
					value = attrs.getValue(i);
				}
			}
			return value;
		}
		
		/**
		 * Return the value of a numeric attribute or an alternate abbreviation.
		 * @param attrs The set of attributes.
		 * @param qName1 The attribute name.
		 * @param qName2 An alternate name for the attribute, or null.
		 * @return The numeric value of attribute qName1, or the value of qName2
		 * 			if qName1 is not defined.
		 * 			If neither attribute exists, return null.
		 * 			If the attribute exists but is not a valid number (or NaN),
		 * 			log an error and return null.
		 */
		private Double getDoubleAttr(Attributes attrs, String qName1, String qName2)
		{
			String s = getAttr(attrs, qName1, qName2);
			if (s == null) {
				return null;
			}
			try {
				return new Double(s);
			} catch (NumberFormatException e) {
				if (m_errorLogger != null) {
					StringBuilder b = new StringBuilder();
					b.append("Non-numeric " + qName1 + " attribute '" + s + "'");
					if (m_locator != null) {
						b.append(" line " + m_locator.getLineNumber() + " col "
								+ m_locator.getColumnNumber());
					}
					m_errorLogger.logError(b.toString());
				}
				return null;
			}
		}
		
		private boolean checkPid(String pid)
		{
			if (pid == null || m_networkMap.pidToIndex(pid) >= 0) {
				return true;
			}
			if (m_errorLogger != null) {
				StringBuilder b = new StringBuilder();
				b.append("Invalid PID '" + pid + "'");
				if (m_locator != null) {
					b.append(" line " + m_locator.getLineNumber() + " col "
							+ m_locator.getColumnNumber());
				}
				m_errorLogger.logError(b.toString());
			}
			return false;
		}
		
		/**
		 * Return true if a character substring is just white space.
		 * @param ch An array of characters.
		 * @param start The starting index in ch[].
		 * @param len The length of the substring to test.
		 * @return True iff the "len" characters starting at ch[start]
		 * 		are all white space.
		 */
		private boolean isWhiteSpace(char ch[], int start, int len)
		{
			for (int i = 0; i < len; i++) {
				if (!Character.isWhitespace(ch[start + i])) {
					return false;
				}
			}
			return true;
		}
	}
	
	/**
	 * Read an XML cost map specification, and return a CostMap object
	 * for each defined map.
	 * 
	 * @param istr
	 * 		The InputStream with the XML specification.
	 * @param altoData
	 * 		Where we save the Cost Maps we create.
	 * @param defaultMapId
	 * 		The default network map resource id, for cost maps
	 *		which do not specify a network map id.
	 * 		May be null.
	 * @param errorLogger
	 * 		If not null, an error logger for non-fatal errors.
	 * @throws ParserConfigurationException
	 * 		Something went wrong creating the XML parser.
	 * 		Shouldn't happen if JVM is configured properly.
	 * @throws SAXException
	 * 		An error in the XML specification.
	 * @throws IOException
	 * 		An error while reading the file.
	 */
	public static void createCostMaps(InputStream istr,
									  AltoData altoData,
									  String defaultMapId,
									  IErrorLogger errorLogger)
		    throws ParserConfigurationException, SAXException, IOException
	{
		SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
		CostMapScanner handler = new CostMapScanner(altoData, defaultMapId, errorLogger);
		parser.parse(istr, handler);
	}
	
	/**
	 * For testing, read, parse and print network and cost map specification files.
	 * @param args
	 * 		arg[0] is the name of a network-map XML file (@see NetworkMapReader);
	 * 		arg[1] is the name of a cost-map XML file.
	 * 		arg[2] (optional) is the default network-map resource id.
	 * @throws ParserConfigurationException
	 * 		Something went wrong creating the XML parser.
	 * 		Shouldn't happen if JVM is configured properly.
	 */
	public static void main(String[] args)
			throws ParserConfigurationException
	{
		if (args.length != 2 && args.length != 3) {
			System.err.println("Usage: network-map-xml-file cost-map-xml-file [default-network-id]");
			System.exit(1);
		}
		String netFile = args[0];
		String costFile = args[1];
		String defaultId = args.length >= 3 ? args[2] : null;
		AltoData altoData = new AltoData(null);
		altoData.setDefaultNetworkMapId(defaultId);
	
		// Read network map file.
		System.out.println(netFile + ":");
		boolean okay = false;
		try {
			NetworkMapReader.createNetworkMaps(
						new FileInputStream(netFile), altoData, defaultId, null);
			okay = true;
		} catch (SAXException e) {
			System.err.println(e);
		} catch (IOException e) {
			System.err.println(e);
		}
		if (!okay) {
			System.exit(1);;
		}
		
		// Read cost map file.
		System.out.println(costFile + ":");
		okay = false;
		try {
			CostMapReader.createCostMaps(
										new FileInputStream(costFile),
										altoData, defaultId, null);
			okay = true;
		} catch (SAXException e) {
			System.err.println(e);
		} catch (IOException e) {
			System.err.println(e);
		}
		if (!okay) {
			System.exit(1);;
		}
		
		// Print network maps.
		for (String mapId: altoData.networkMapIds()) {
			NetworkMap map = altoData.getNetworkMap(mapId);
			System.out.println();
			System.out.println("Network Map: network-id: " + map.getMapId()
								+ "  vtag: " + map.getVtag() + ":");
			for (String pid: map.allPids()) {
				System.out.print("  " + pid + ":");
				int n = 0;
				for (CIDRAddress cidr: map.getCIDRs(pid)) {
					if (++n > 5) {
						System.out.println();
						System.out.print("        ");
						n = 0;
					}
					System.out.print(" " + cidr);
				}
				System.out.println();
			}
		}

		// Print cost maps.
		for (String mapId: altoData.networkMapIds()) {
			for (String costMetric: altoData.getCostMetrics(mapId)) {
				CostMap costMap = altoData.getCostMap(mapId, costMetric);
				System.out.println();
				System.out.println("Cost Map: network-id: " + costMap.getMapId()
									+ "  metric: " + costMap.getCostMetric());
				int nPids = costMap.getNumPids();
				for (int iSrc = 0; iSrc < nPids; iSrc++) {
					System.out.print("  " + costMap.indexToPid(iSrc) + " to");
					int nPrt = 0;
					for (int iDst = 0; iDst < nPids; iDst++) {
						double cost = costMap.getCost(iSrc, iDst);
						if (!Double.isNaN(cost) && cost >= 0) {
							if (nPrt++ % 5 == 0) {
								System.out.println();
								System.out.print("    ");
							}
							System.out.print("  " + costMap.indexToPid(iDst) + "="
									+ costMap.getCost(iSrc, iDst));
						}
					}
					System.out.println();
				}
			}
		}
	}
}
