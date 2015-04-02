package com.wdroome.altodata;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.wdroome.util.inet.CIDRAddress;

/**
 * Read and parse a XML network-map specification file.
 * <br/>
<pre>
  The root element is &lt;network-maps&gt;, with a &lt;network-map&gt; child element
  for each network map. If there is only one network map,
  you can use its &lt;network-map&gt; as the root element.  

  &lt;network-map&gt;:
      The &lt;pid&gt; tags inside a &lt;network-map&gt; specify the PIDs and their CIDRs.
      The "name" attribute is the PID name, and the text body is a
      white-space-separated list of CIDRs in that PID. CIDRs may start with an
      address type prefix (e.g., "ipv4:" or "ipv6:"). If a CIDR doesn't, we deduce
      the address type from the format.
      
      Several &lt;pid&gt; tags can have the same "name" attribute.
      If so, the PID is the union of the CIDRs defined in those tags.
      
      The &lt;network-map&gt; tag has two attributes.
      
      The "network-id" attribute gives the resource id of this network map. If
      there is only one network map, this attribute is optional. Otherwise it is
      required, and must match the resource id of a network map defined by the
      IRD.
      
      The optional "vtag" attribute specifies the version tag for the map. If
      omitted, we calculate a vtag based on a secure hash of a cannonical
      representation of the map. This means two network maps with the same sets
      of PIDs and CIDRs will have the same default vtag.

  Abbreviations:
      You can abbreviate &lt;pid&gt; as &lt;p&gt;, and name= as n=.
 </pre>
 *
 * @author wdr
 */
public class NetworkMapReader
{
	public static final String TN_NETWORK_MAPS = "network-maps";
	public static final String TN_NETWORK_MAP = "network-map";
	public static final String TN_PID = "pid";
	public static final String TN_PID_ABBR = "p";
	
	public static final String AN_NETWORK_ID = "network-id";
	public static final String AN_VTAG = "vtag";
	public static final String AN_NAME = "name";
	public static final String AN_NAME_ABBR = "n";

	/**
	 * A SAX handler for the XML network map specification.
	 * We use the SAX model, rather than the DOM model,
	 * because network maps can be large and the XML tree
	 * might not fit in memory.
	 * 
	 * @author wdr
	 */
	private static class NetworkMapScanner extends DefaultHandler
	{
		private final AltoData m_altoData;
		private final String m_defaultMapId;
		private final String m_makeVtagPrefix;
		private Locator m_locator = null;
		
		private Stack<String> m_path = new Stack<String>();
		private NetworkMap m_map = null;
		private String m_vtag = null;
		private String m_pid = null;
		private boolean m_usedDefaultId = false;
		private StringBuilder m_cidrList = null;
		
		private enum STATE {ROOT, NETWORK_MAP, PID, CIDRS};
		private STATE m_state = STATE.ROOT;
		
		public NetworkMapScanner(AltoData altoData,
								 String defaultMapId,
								 String makeVtagPrefix)
		{
			m_altoData = altoData;
			m_defaultMapId = defaultMapId;
			m_makeVtagPrefix = makeVtagPrefix;
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
		 * @see org.xml.sax.helpers.DefaultHandler#startElement(String, String, String, org.xml.sax.Attributes)
		 */
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attrs)
				throws SAXException
		{
			m_path.push(qName);
			if (TN_NETWORK_MAPS.equals(qName)) {
				if (m_state != STATE.ROOT) {
					throw new SAXParseException("<" + qName + "> at wrong level", m_locator);
				}
				m_state = STATE.NETWORK_MAP;
			} else if (TN_NETWORK_MAP.equals(qName)) {
				if (m_state != STATE.NETWORK_MAP && m_state != STATE.ROOT) {
					throw new SAXParseException("<" + qName + "> at wrong level", m_locator);
				}
				String mapId = getAttr(attrs, AN_NETWORK_ID, null);
				if (mapId == null && !m_usedDefaultId) {
					mapId = m_defaultMapId;
					m_usedDefaultId = true;
				}
				if (mapId == null || mapId.equals("")) {
					throw new SAXParseException("<" + qName + ">: missing "
											+ AN_NETWORK_ID + " attribute", m_locator);
				}
				if (m_altoData.getNetworkMap(mapId) != null) {
					throw new SAXParseException("<" + qName + ">: "
							+ "A map has already been defined for resource id '" + mapId + "'",
							m_locator);					
				}
				m_map = new NetworkMap(mapId);
				m_vtag = getAttr(attrs, AN_VTAG, null);
				m_state = STATE.PID;
			} else if (TN_PID.equals(qName) || TN_PID_ABBR.equals(qName)) {
				if (m_state != STATE.PID) {
					throw new SAXParseException("<" + qName + "> at wrong level", m_locator);
				}
				m_pid = getAttr(attrs, AN_NAME, AN_NAME_ABBR);
				if (m_pid == null || m_pid.equals("")) {
					throw new SAXParseException("<" + qName + ">: missing "
											+ AN_NAME + " attribute", m_locator);
				}
				m_cidrList = new StringBuilder();
				m_state = STATE.CIDRS;
			} else {
				fatalError(new SAXParseException("Unknown tag <" + qName + ">", m_locator));					
			}
		}

		/* (non-Javadoc)
		 * @see org.xml.sax.helpers.DefaultHandler#endElement(String, String, String)
		 */
		@Override
		public void endElement(String uri, String localName, String qName)
				throws SAXException
		{
			m_path.pop();
			if (TN_NETWORK_MAPS.equals(qName)) {
				m_state = STATE.ROOT;
			} else if (TN_NETWORK_MAP.equals(qName)) {
				m_map.freeze(m_vtag, m_makeVtagPrefix);
				try {
					m_altoData.setNetworkMap(m_map);
				} catch (Exception e) {
					throw new SAXParseException("<" + qName + ">: "
							+ "Error adding Network Map: " + e.getMessage(),
							m_locator);										
				}
				m_map = null;
				m_state = STATE.NETWORK_MAP;
			} else if (TN_PID.equals(qName) || TN_PID_ABBR.equals(qName)) {
				try {
					m_map.addCIDRs(m_pid, m_cidrList.toString());
				} catch (UnknownHostException e) {
					throw new SAXParseException("PID '" + m_pid + "': " + e.getMessage(), m_locator);
				}
				m_cidrList = null;
				m_pid = null;
				m_state = STATE.PID;
			}
		}

		/* (non-Javadoc)
		 * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
		 */
		@Override
		public void characters(char[] ch, int start, int length)
				throws SAXException
		{
			if (m_state == STATE.CIDRS) {
				m_cidrList.append(ch, start, length);
			} else if (!isWhiteSpace(ch, start, length)) {
				throw new SAXParseException(
								"In " + getPath() + ": illegal text string '" + new String(ch, start, length) + "'",
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
		
		/**
		 * Return the path of the current XML element.
		 * @return The path of the current XML element.
		 */
		private String getPath()
		{
			StringBuilder b = new StringBuilder();
			String sep = "";
			for (String s: m_path) {
				b.append(sep);
				b.append(s);
				sep = ".";
			}
			if (b.length() == 0) {
				b.append("(root)");
			}
			return b.toString();
		}
	}
	
	/**
	 * Read an XML network map specification, and return a NetworkMap object
	 * for each defined map.
	 * 
	 * @param istr
	 * 		The InputStream with the XML specification.
	 * @param altoData
	 * 		Where we save the Network Maps we create.
	 * @param defaultMapId
	 * 		The default network map resource id.
	 * 		If not null, use this if one network-map specification
	 * 		does not declare a map resource id.
	 * @param makeVtagPrefix
	 * 		If not null, and if a network map specification does
	 *		not give an explicit vtag, prepend this string
	 *		to the automatically generated vtag.
	 * @throws ParserConfigurationException
	 * 		Something went wrong creating the XML parser.
	 * 		Shouldn't happen if JVM is configured properly.
	 * @throws SAXException
	 * 		An error in the XML specification.
	 * @throws IOException
	 * 		An error while reading the file.
	 */
	public static void createNetworkMaps(InputStream istr,
									    AltoData altoData,
		      							String defaultMapId,
		      							String makeVtagPrefix)
		    throws ParserConfigurationException, SAXException, IOException
	{
		SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
		NetworkMapScanner handler = new NetworkMapScanner(altoData, defaultMapId, makeVtagPrefix);
		parser.parse(istr, handler);
	}

	/**
	 * For testing, read, parse and print network map specification files.
	 * @param args
	 * 		The names of files to read.
	 * @throws ParserConfigurationException
	 * 		Something went wrong creating the XML parser.
	 * 		Shouldn't happen if JVM is configured properly.
	 */
	public static void main(String[] args)
			throws ParserConfigurationException
	{
		AltoData altoData = new AltoData(null);
		for (String fname: args) {
			System.out.println(fname + ":");
			try {
				createNetworkMaps(new FileInputStream(fname), altoData, "default-map", null);
				for (String mapId: altoData.networkMapIds()) {
					NetworkMap map = altoData.getNetworkMap(mapId);
					System.out.println("Map " + map.getMapId() + ", vtag " + map.getVtag() + ":");
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
			} catch (SAXException e) {
				System.err.println(e);
			} catch (IOException e) {
				System.err.println(e);
			}
		}
	}
}
