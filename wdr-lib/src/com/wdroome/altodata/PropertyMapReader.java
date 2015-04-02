package com.wdroome.altodata;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.UnknownHostException;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.wdroome.xmltools.XMLParser;
import com.wdroome.util.IErrorLogger;
import com.wdroome.util.SystemErrorLogger;
import com.wdroome.util.String3;
import com.wdroome.util.inet.CIDRAddress;

/**
 * Read and set initial properties.
 * @author wdr
 */
public class PropertyMapReader
{
	/**
	 * Read initial properties from an XML file.
	 * <br/>
<pre>
  Specify the endpoint and PID properties.
  
  This XNL document is a tree of nested &lt;props&gt; elements. &lt;props&gt; elements have
  four optional attributes: "name", "value", "addr", and "pid", for property
  name, property value, endpoint address and PID name, respectively. These
  attributes can be abbreviated as "n", "v", "a" and "p".
 
  Do not use this document to define the built-in property "pid." The ALTO
  server automatically defines that property for each network map, and ignores
  any attempt to set that property.

  Endpoint properties:
    While walking the tree of elements, whenever we encounter a &lt;props&gt; element
    for which the "name", "value" and "addr" attribute are specified in that
    element or in one of its parents, we set that property to that value for that
    endpoint address.

    Whenever we encounter a "name" attribute, we add that property name to the
    list of recognized properties, even if the element tree does not define any
    values for that property.
    
    As defined in Section 10.8 of RFC 7285, resource-specific property names must
    be prefixed with the resource id of their network map. Property names without
    resource prefixes are global properties.
  
    An "addr" attribute may be a CIDR as well as an endpoint address, to set that
    property for all endpoints covered by that CIDR. Addresses and CIDRs may have
    type prefixes (e.g., "ipv4:" or "ipv6:"). If not, we infer the type from the
    address format.
  
  PID properties:
    Whenever we encounter a &lt;props&gt; element for which the "name", "value"
    and "pid" attributes are specified, either in that element or in one of
    its parent tags, we set that property to that value for that PID.
    
    For resource-specific properties (e.g., "name" is of the form
    "resource-id.prop-name"), the "pid" attribute must be the name of a PID in
    the network map with that resource id. For global properties, the "pid"
    attribute must be the name of a PID in the default network map. Thus the PID
    name in the "pid" attribute is never qualified with a resource id.
    
  Relationship between PID properties and endpoint properties:
    PID properties define default values for endpoint properties. Specifically,
    for a resource-specific property, the default value for an endpoint is the
    value of the PID property for that endpoint's PID in that network map. For a
    global property, the default value for an endpoint is the the value of the
    PID property for that endpoint's PID in the default network map.
</pre>
	 *
	 * @param errorLogger Called whenever there is an error in the file.
	 * @param fname The name of the XML file.
	 * @param altoData The structures defining this ALTO server.
	 * @param grumble If true, log an error if the file doesn't exist.
	 * 		If false, quietly ignore a missing file.
	 * 		But if the file does exist, always log errors in the file.
	 */
	public static void readXML(IErrorLogger errorLogger,
							   String fname,
							   AltoData altoData,
							   boolean grumble)
	{
		if (errorLogger == null) {
			errorLogger = new SystemErrorLogger();
		}
		try {
			walkElements(errorLogger, altoData, new XMLParser().parseFile(fname),
						null, null, null, null);
		} catch (SAXException e) {
			errorLogger.logError("XML error parsing property file \"" + fname
						+ "\": " + e.getMessage());
		} catch (ParserConfigurationException e) {
			errorLogger.logError("Error reading property file \"" + fname
						+ "\": " + e.getMessage());
		} catch (IOException e) {
			if (grumble) {
				errorLogger.logError("Error reading property file \"" + fname
							+ "\": " + e.getMessage());
			}
		}
	}
	
	private static void walkElements(IErrorLogger errorLogger,
									 AltoData serverData,
									 Node node,
									 String name,
									 String value, 
									 String addr, 
									 String pid)
	{
		if (node == null)
			return;
		if (!(node instanceof Element))
			return;
		Element elem = (Element)node;
		name = getAttr(elem, "name", name);
		value = getAttr(elem, "value", value);
		addr = getAttr(elem, "addr", addr);
		pid = getAttr(elem, "pid", pid);
		if (name != null && !name.equals("")) {
			serverData.addPropName(name);
			if (value != null) {
				if (pid != null && !pid.equals("")) {
					serverData.setPidProp(pid, name, value);
				}
				if (addr != null && !addr.equals("")) {
					try {
						serverData.setEndpointProp(addr, name, value);
					} catch (UnknownHostException e) {
						errorLogger.logError("Error in properties file: bad address \"" + addr + "\"");
					}
				}
			}
		}
		for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
			walkElements(errorLogger, serverData, child, name, value, addr, pid);
		}
	}
	
	private static String getAttr(Element elem, String name, String value)
	{
		if (elem.hasAttribute(name)) {
			return elem.getAttribute(name);
		} else {
			String abbr = name.substring(0, 1);
			if (elem.hasAttribute(abbr)) {
				return elem.getAttribute(abbr);
			} else {
				return value;
			}
		}
	}
	
	/**
	 * For testing, read, parse and print network and cost map specification files.
	 * @param args
	 * 		arg[0] is the name of a network-map XML file (@see NetworkMapReader);
	 * 		arg[1] is the name of a property-map XML file.
	 * 		arg[2] (optional) is the default network-map resource id.
	 * @throws ParserConfigurationException
	 * 		Something went wrong creating the XML parser.
	 * 		Shouldn't happen if JVM is configured properly.
	 */
	public static void main(String[] args)
			throws ParserConfigurationException
	{
		if (args.length != 2 && args.length != 3) {
			System.err.println("Usage: network-map-xml-file prop-map-xml-file [default-network-id]");
			System.exit(1);
		}
		String netFile = args[0];
		String propFile = args[1];
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
		
		// Read property map file.
		System.out.println(propFile + ":");
		try {
			PropertyMapReader.readXML(null, propFile, altoData, true);
		} catch (Exception e) {
			e.printStackTrace();
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
		
		// Print global endpoint props.
		System.out.println();
		System.out.println("Global Endpoint Properties:");
		for (EndpointPropertyTable.PropValue prop: altoData.getEndpointProps()) {
			if (prop.m_addr != null) {
				System.out.println("  " + prop.m_addr.toIPAddrWithPrefix()
									+ " " + prop.m_name + " = '" + prop.m_value + "'");
			} else if (prop.m_cidr != null) {
				System.out.println("  " + prop.m_cidr.toIPAddrWithPrefix()
						+ " " + prop.m_name + " = '" + prop.m_value + "'");
			}
		}

		// Print resource-specific endpoint & pid props.
		for (String mapId: altoData.networkMapIds()) {
			System.out.println();
			System.out.println("Properties for " + mapId + ":");
			for (EndpointPropertyTable.PropValue prop: altoData.getEndpointProps(mapId)) {
				if (prop.m_addr != null) {
					System.out.println("  " + prop.m_addr.toIPAddrWithPrefix()
										+ " " + prop.m_name + " = '" + prop.m_value + "'");
				} else if (prop.m_cidr != null) {
					System.out.println("  " + prop.m_cidr.toIPAddrWithPrefix()
										+ " " + prop.m_name + " = '" + prop.m_value + "'");
				}
			}
			for (String3 prop: altoData.getPidProps(mapId)) {
				System.out.println("  " + prop.m_str1 + " " + prop.m_str2
									+ " = '" + prop.m_str3 + "'");				
			}
		}
	}
}
