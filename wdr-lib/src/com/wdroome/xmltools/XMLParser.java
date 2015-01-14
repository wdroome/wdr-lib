package com.wdroome.xmltools;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

import org.w3c.dom.*;

import javax.xml.parsers.*;
import org.xml.sax.*;

//import com.lucent.sird.alto.util.IErrorLogger;

/**
 *	Utility class to simplify reading and parsing XML files.
 *	To parse a file (or whatever), create a new XMLParser and call one
 *	of the parse() methods. The base class uses a default set of attributes
 *	to create the xml parser -- see {@link #makeDocumentBuilder()}. To change
 *	those attributes, create a subclass and override that method.
 *
 * @author wdr
 */
public class XMLParser
{
	/**
	 *	A quiet XML ErrorHandler. Ignore warnings.
	 *	For errors (regular and fatal), throw the exception,
	 *	without printing a message,
	 *	and assume the top-level caller will deal with it.
	 */
	public static class SilentHandler implements ErrorHandler
	{
		@Override public void error(SAXParseException e) throws SAXParseException {throw e;}
		@Override public void warning(SAXParseException e) throws SAXParseException {}
		@Override public void fatalError(SAXParseException e) throws SAXParseException {throw e;}
	}
	
	/**
	 *	A noisy XML ErrorHandler.
	 *	Print all warnings and errors on System.err.
	 *	Print the description of the XML source (e.g.,file name, url, etc)
	 *	and the line number as well as the error.
	 *	For fatal errors, throw the exception to terminate parsing.
	 *	For warnings and regular errors, continue without throwing an exception.
	 *	Note that this object is embedded in an XMLParser instance.
	 */
	public class NoisyHandler implements ErrorHandler
	{
		/**
		 * Create a new handler and set it as the XMLParser's error handler.
		 */
		public NoisyHandler()
		{
			setErrorHandler(this);
		}
		
		@Override
		public void error(SAXParseException e) throws SAXParseException
		{
			prt("XML Error", e);
		}

		@Override
		public void warning(SAXParseException e) throws SAXParseException
		{
			prt("XML Warning", e);
		}

		@Override
		public void fatalError(SAXParseException e) throws SAXParseException
		{
			prt("XML Fatal Error", e);
			throw e;
		}
		
		private void prt(String prefix, SAXParseException e)
		{
			StringBuilder buf = new StringBuilder();
			buf.append(prefix);
			String d = getDescription();
			if (d != null && !d.equals("")) {
				buf.append(", ");
				buf.append(d);
			}
			buf.append(", line ");
			buf.append(e.getLineNumber());
			buf.append(": ");
			buf.append(e.getMessage());
			System.err.println(buf);
		}
	}
	
	/**
	 *	An XML ErrorHandler that logs errors via an {@link IErrorLogger}
	 *	specified in the c'tor.
	 *	Print the description of the XML source (file name, url, etc)
	 *	and the line number as well as the error.
	 *	For fatal errors, throw the exception to terminate parsing.
	 *	For warnings and regular errors, continue without throwing an exception.
	 *	Note that this object is embedded in an XMLParser instance.
	 */
//	public class LoggerHandler implements ErrorHandler
//	{
//		private IErrorLogger m_errorLogger = null;
//		private boolean m_printWarnings = true;
//		
//		/**
//		 * Create a new handler and set it as the XMLParser's error handler.
//		 * @param errorLogger The error logger. If null, use System.err.
//		 * @param printWarnings If true, log warnings. If false, ignore warnings.
//		 */
//		public LoggerHandler(IErrorLogger errorLogger, boolean printWarnings)
//		{
//			m_errorLogger = errorLogger;
//			m_printWarnings = printWarnings;
//			setErrorHandler(this);
//		}
//		
//		@Override
//		public void error(SAXParseException e) throws SAXParseException
//		{
//			prt("XML Error", e);
//		}
//
//		@Override
//		public void warning(SAXParseException e) throws SAXParseException
//		{
//			if (m_printWarnings)
//				prt("XML Warning", e);
//		}
//
//		@Override
//		public void fatalError(SAXParseException e) throws SAXParseException
//		{
//			prt("XML Fatal Error", e);
//			throw e;
//		}
//		
//		private void prt(String prefix, SAXParseException e)
//		{
//			StringBuilder buf = new StringBuilder();
//			buf.append(prefix);
//			String d = getDescription();
//			if (d != null && !d.equals("")) {
//				buf.append(", ");
//				buf.append(d);
//			}
//			buf.append(", line ");
//			buf.append(e.getLineNumber());
//			buf.append(": ");
//			buf.append(e.getMessage());
//			if (m_errorLogger != null)
//				m_errorLogger.logError(buf.toString());
//			else
//				System.err.println(buf);
//		}
//	}

	/**
	 *	An XML ErrorHandler that saves errors and warnings in a buffer,
	 *	and returns the error messages on demand.
	 *	Each error includes a description of the XML source (file name, url, etc)
	 *	and the line number as well as the error.
	 *	For fatal errors, throw the exception to terminate parsing.
	 *	For warnings and regular errors, continue without throwing an exception.
	 *	Note that this object is embedded in an XMLParser instance.
	 */
	public class BufferHandler implements ErrorHandler
	{
		private ArrayList<String> m_errors = null;

		/**
		 * Create a new handler and set it as the XMLParser's error handler.
		 */
		public BufferHandler()
		{
			setErrorHandler(this);
		}

		@Override
		public void error(SAXParseException e) throws SAXException
		{
			log("XML Error", e);
		}

		@Override
		public void warning(SAXParseException e) throws SAXException
		{
			log("XML Warning", e);
		}
	
		@Override
		public void fatalError(SAXParseException e) throws SAXParseException
		{
			log("XML Fatal Error", e);
			throw e;
		}
		
		/**
		 * Return an array with the error messages generated while parsing this file,
		 * one element per error or warning. If there were no messages, return null.
		 * Note that almost all errors will be fatal, so it's unlikely there will be
		 * more than one error message.
		 */
		public String[] getErrors()
		{
			return m_errors == null ? null : m_errors.toArray(new String[m_errors.size()]);
		}
		
		private void log(String prefix, SAXParseException e)
		{
			StringBuilder msg = new StringBuilder();
			msg.append(prefix);
			String d = getDescription();
			if (d != null && !d.equals("")) {
				msg.append(", ");
				msg.append(d);
			}
			msg.append(", line ");
			msg.append(e.getLineNumber());
			msg.append(": ");
			msg.append(e.getMessage());
			msg.append("\n");
			if (m_errors == null)
				m_errors = new ArrayList<String>();
			m_errors.add(msg.toString());
		}
	}
	
	/** This object's ErrorHandler. */
	private ErrorHandler m_errorHandler = new SilentHandler();
	
	/** A description of the source of the XML being parsed. */
	private String m_description = "";
	
	/** The parsed Document. */
	private Document m_document = null;

	/**
	 *	Create a new XMLParser, using {@link SilentHandler} to handler parse errors.
	 */
	public XMLParser() { this(false); }

	/**
	 *	Create a new XMLParser.
	 *	@param useNoisyHandler
	 *		If true, use {@link NoisyHandler} to handle parse errors.
	 *		If false, use {@link SilentHandler}.
	 */
	public XMLParser(boolean useNoisyHandler)
	{
		setErrorHandler(useNoisyHandler ? new NoisyHandler() : new SilentHandler());
	}

	/**
	 *	Create and return a new DocumentBuilder.
	 *	Set the error handler to that returned by {@link #getErrorHandler()}.
	 *	The new parser has the following properties:
	 *	<br />Validating: false
	 *	<br />NamespaceAware: true
	 *	<br />ExpandEntityReferences: true
	 *	<br />IgnoringComments: false
	 *	<br />Coalescing: false
	 *	@return A new XML parser.
	 *	@throws ParserConfigurationException
	 *		If thrown by {@link DocumentBuilderFactory#newInstance()}.
	 */
	public DocumentBuilder makeDocumentBuilder()
				throws ParserConfigurationException
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setNamespaceAware(true);
		factory.setExpandEntityReferences(false);
		factory.setIgnoringComments(false);
		factory.setCoalescing(false);
		DocumentBuilder xmlParser = factory.newDocumentBuilder();
		xmlParser.setErrorHandler(getErrorHandler());
		return xmlParser;
	}

	/**
	 *	Return the error handler to use when parsing XML files.
	 *	@see #setErrorHandler(ErrorHandler)
	 *	@see #XMLParser(boolean)
	 */
	public ErrorHandler getErrorHandler() { return m_errorHandler; }
	
	/**
	 *	Set the ErrorHandler used when parsing XML input.
	 *	If null, use the XML parser's default handler.
	 *	@return This XMLParser, so you can chain calls.
	 *	@see #XMLParser(boolean)
	 */
	public XMLParser setErrorHandler(ErrorHandler errorHandler)
	{
		this.m_errorHandler = errorHandler;
		return this;
	}

	/**
	 *	Return the description of the source of the XML being parsed.
	 */
	public String getDescription() { return m_description; }
	
	/**
	 *	Set the description of the source of the XML being parsed.
	 *	@return This XMLParser, so you can chain calls.
	 */
	public XMLParser setDescription(String description)
	{
		this.m_description = description;
		return this;
	}
	
	/**
	 *	Return the parsed Document, or null.
	 */
	public Document getDocument()
	{
		return m_document;
	}
	
	/**
	 *	Create a new parser, via {@link #makeDocumentBuilder()},
	 *	and use it to parse an XML InputStream. Return the root element
	 *	of the parsed XML document.
	 *
	 *	@param iStream The XML input.
	 *	@return The document's root Element.
	 *	@throws ParserConfigurationException
	 *		If thrown by {@link #makeDocumentBuilder()}.
	 *	@throws SAXException
	 *		If thrown by {@link DocumentBuilder#parse(File)} while parsing the file.
	 *	@throws IOException
	 *		If an error occurs while reading the input.
	 *	@see #setDescription(String)
	 */
	public Element parse(InputStream iStream)
				throws SAXException, ParserConfigurationException, IOException
	{
		m_document = makeDocumentBuilder().parse(iStream);
		return m_document.getDocumentElement();
	}

	/**
	 *	Create a new parser, via {@link #makeDocumentBuilder()},
	 *	and use it to parse an XML file. Return the root element
	 *	of the parsed XML document.
	 *
	 *	@param fname The file name.
	 *	@return The document's root Element.
	 *	@throws ParserConfigurationException
	 *		If thrown by {@link #makeDocumentBuilder()}.
	 *	@throws SAXException
	 *		If thrown by {@link DocumentBuilder#parse(File)} while parsing the file.
	 *	@throws IOException
	 *		If we cannot open the file, or an error occurs while reading it.
	 */
	public Element parseFile(String fname)
				throws SAXException, ParserConfigurationException, IOException
	{
		setDescription("File \"" + fname + "\"");
		m_document = makeDocumentBuilder().parse(new File(fname));
		return m_document.getDocumentElement();
	}

	/**
	 *	Create a new parser, via {@link #makeDocumentBuilder()},
	 *	and use it to parse the XML content of a URL. Return the root element
	 *	of the parsed XML document.
	 *
	 *	@param url The URL.
	 *	@return The document's root Element.
	 *	@throws ParserConfigurationException
	 *		If thrown by {@link #makeDocumentBuilder()}.
	 *	@throws SAXException
	 *		If thrown by {@link DocumentBuilder#parse(File)} while parsing the file.
	 *	@throws IOException
	 *		If we cannot open the url, or an error occurs while reading it's contents.
	 */
	public Element parse(URL url)
				throws SAXException, ParserConfigurationException, IOException
	{
		setDescription("URL \"" + url.toString() + "\"");
		m_document = makeDocumentBuilder().parse(url.openStream());
		return m_document.getDocumentElement();
	}

	/**
	 *	Create a new parser, via {@link #makeDocumentBuilder()},
	 *	and use it to parse the XML content of a URL. Return the root element
	 *	of the parsed XML document.
	 *
	 *	@param url The URL.
	 *	@return The document's root Element.
	 *	@throws ParserConfigurationException
	 *		If thrown by {@link #makeDocumentBuilder()}.
	 *	@throws SAXException
	 *		If thrown by {@link DocumentBuilder#parse(File)} while parsing the file.
	 *	@throws IOException
	 *		If we cannot open the url, or an error occurs while reading it's contents.
	 */
	public Element parseURL(String url)
				throws SAXException, ParserConfigurationException, IOException
	{
		setDescription("URL \"" + url + "\"");
		m_document = makeDocumentBuilder().parse(new URL(url).openStream());
		return m_document.getDocumentElement();
	}

	/**
	 *	Create a new parser, via {@link #makeDocumentBuilder()},
	 *	and use it to parse an XML string. Return the root element
	 *	of the parsed XML document.
	 *
	 *	@param xml A string with an xml document.
	 *	@return The document's root Element.
	 *	@throws ParserConfigurationException
	 *		If thrown by {@link #makeDocumentBuilder()}.
	 *	@throws SAXException
	 *		If thrown by {@link DocumentBuilder#parse(File)} while parsing the file.
	 *	@throws IOException
	 *		If we cannot open the file, or an error occurs while reading it.
	 */
	public Element parseXMLString(String xml)
				throws SAXException, ParserConfigurationException, IOException
	{
		setDescription("XML String");
		m_document = makeDocumentBuilder().parse(new InputSource(new StringReader(xml)));
		return m_document.getDocumentElement();
	}
	
	/**
	 * Return the text value of a child of an element.
	 * @param parent The parent element.
	 * @param childTag The tag name of the desired child.
	 * @param def The value to return if there is no such child.
	 * @return The text value of the first child named "childTag", or "def".
	 */
	public static String getChildValue(Element parent, String childTag, String def)
	{
		NodeList children = parent.getElementsByTagName(childTag);
		if (children == null || children.getLength() < 1)
			return def;
		return children.item(0).getTextContent().trim();
	}

	/**
	 * Return an array of text values of the children of an element.
	 * @param parent The parent element.
	 * @param childTag The tag name of the desired child.
	 * @return Return an array with the text values of all children named "childTag".
	 * 		If none, return a String[0].
	 */
	public static String[] getChildValues(Element parent, String childTag)
	{
		NodeList children = parent.getElementsByTagName(childTag);
		if (children == null || children.getLength() < 1)
			return new String[0];
		String[] vals = new String[children.getLength()];
		for (int i = 0; i < vals.length; i++)
			vals[i] = children.item(i).getTextContent().trim();
		return vals;
	}
	
	/**
	 * Return the first child element named "childTag" of "parent".
	 * @param parent The parent element.
	 * @param childTag The name of the desired child.
	 * 			If "*" or null, return the first element with any name.
	 * @return Parent's first child element named childTag, or null if none.
	 */
	public static Element getFirstChildElement(Element parent, String childTag)
	{
		if (childTag == null)
			childTag = "*";
		NodeList children = parent.getElementsByTagName(childTag);
		if (children != null) {
			for (int i = 0; i < children.getLength(); i++) {
				Node node = children.item(i);
				if (node instanceof Element)
					return (Element)node;
			}
		}
		return null;
	}
	
	/**
	 * URL encode a value. For blanks, use "%20" rather than "+".
	 * @param val The value to be encoded.
	 * @return The encoded value of "val".
	 */
	public static String urlEncode(String val)
	{
		try {
			return URLEncoder.encode(val, "UTF-8").replaceAll("\\+", "%20");
		} catch (Exception e) {
			return val;
		}
	}
	
	/**
	 * Return "name[##]", where "name" is child's node name,
	 * and "##" is child's index with respect to siblings with the same name.
	 * If child is unique, just return "name".
	 * @param child A node in an XML tree.
	 * @return "name[##]" or "name".
	 */
	public static String getIndexInParent(Node child)
	{
		String nodeName = child.getNodeName();
		Node parent = child.getParentNode();
		if (parent == null || !(parent instanceof Element))
			return nodeName;
		ElementArrayList sibs = new ElementArrayList((Element)parent, nodeName);
		if (sibs.size() == 1)
			return nodeName;
		int i = 0;
		for (Element sib:sibs) {
			if (sib == child) {
				return nodeName + "[" + i + "]";
			}
			i++;
		}
		return nodeName + "[?]";
	}
	
	/**
	 * Return the path from the root to an element.
	 * If an element has several siblings with the same name,
	 * disambiguate by appending "[#]" to the element name.
	 * @param elem An element in an XML tree.
	 * @return Elem's path name, as "a[1].b[6].c[0]".
	 * 		Omit the "[##]" for unique children.
	 */
	public static String getPath(Element elem)
	{
		StringBuilder path = new StringBuilder();
		for (Node node = elem; node != null && !(node instanceof Document); node = node.getParentNode()) {
			String s = getIndexInParent(node);
			if (path.length() > 0)
				s += ".";
			path.insert(0, s);
		}
		return path.toString();
	}

	/**
	 *	Convenience method: replace any XML tag character in String s
	 *	with the XML entity.
	 *	If s does not have any such tag characters, return s.
	 *<p>
	 *	Note: This is very efficient of s doesn't have any tag characters,
	 *	but it's inefficient of s does have tags.
	 *
	 *	@param s The String to be converted.
	 *	@param doQuotes If true, replace double-quotes with entities.
	 *	@return String s with XML tag characters (and possibly double quotes)
	 *		replaced with XML entities.
	 */
	public static String tagsToEntities(String s, boolean doQuotes)
	{
		if (s == null)
			return "";
		boolean hasAmp = false;
		boolean hasLT = false;
		boolean hasGT = false;
		boolean hasQuote = false;

		for (int i = s.length(); --i >= 0; ) {
			char c = s.charAt(i);
			switch (c) {
				case '&': hasAmp = true; break;
				case '<': hasLT = true; break;
				case '>': hasGT = true; break;
				case '"': hasQuote = true; break;
			}
		}

		if (hasAmp)
			s = s.replaceAll("&", "&amp;");
		if (hasLT)
			s = s.replaceAll("<", "&lt;");
		if (hasGT)
			s = s.replaceAll(">", "&gt;");
		if (hasQuote && doQuotes)
			s =  s.replaceAll("\"", "\\&quot;");

		return s;
	}

	/**
	 *	Convenience method: replace any XML tag character in String s
	 *	with the XML entity.
	 *	If s does not have any such tag characters, return s.
	 *<p>
	 *	Note: This is very efficient of s doesn't have any tag characters,
	 *	but it's inefficient of s does have tags.
	 *
	 *	@param s The String to be converted.
	 *	@return String s with XML tag characters (and possibly double quotes)
	 *		replaced with XML entities.
	 */
	public static String tagsToEntities(String s)
	{
		return tagsToEntities(s, false);
	}
	
	/**
	 *	For testing, parse each xml file passed as an argument with a noisy XMLParser.
	 */
	public static void main(String[] args)
	{
		for (int i = 0; i < args.length; i++) {
			BufferHandler errorHandler = null;
			try {
				System.out.println();
				System.out.println(args[i]);
				XMLParser parser = new XMLParser();
				errorHandler = parser.new BufferHandler();
				Element root = parser.parseFile(args[i]);
				System.out.println(" Parse returned:");
				System.out.println("  Name=" + root.getNodeName());
				System.out.println("  Value=" + root.getNodeValue());
				System.out.println("  NumKids=" + new ElementArrayList(root).size());
			} catch (Exception e) {
				System.out.println(" *** parser threw exception: " + e.getMessage());
			}
			if (errorHandler != null) {
				String[] errors = errorHandler.getErrors();
				if (errors != null) {
					System.out.println("  " + errors.length + " Errors:");
					for (String err:errors)
						System.out.println("     " + err);
				}
			}
		}
	}
}
