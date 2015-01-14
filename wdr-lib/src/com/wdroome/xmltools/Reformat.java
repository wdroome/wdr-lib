package com.wdroome.xmltools;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.InputStream;

import org.w3c.dom.*;

import javax.xml.parsers.*;

import org.xml.sax.*;

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

/**
 * Reformat and "pretty print" an XML file.
 * To use, create an instance and specify the output stream.
 * Then call one or more of the "print" methods.
 * @author wdr
 */
public class Reformat
{
	private PrintStream m_out = System.out;
	private int m_level = 0;
	
	/**
	 * Create a new object and specify the output stream.
	 * @param out The output stream. If null, use System.out.
	 */
	public Reformat(PrintStream out)
	{
		if (out != null)
			m_out = out;
	}
	
	/**
	 * Create a new object using System.out.
	 */
	public Reformat()
	{
	}
	
	private void printNameValue(String name, String value)
	{
		if (value != null) {
			m_out.print(name);
			m_out.print("=\"");
			m_out.print(value
						.replace("&", "&amp;")
						.replace("<", "&lt;")
						.replace(">", "&gt;")
						.replace("\"", "&quot;")
					);
			m_out.print("\"");
		}
	}
	
	/**
	 * Print a Document. This prints the xml header and then the root Element.
	 * @param doc
	 */
	public void print(Document doc)
	{
		m_out.print("<?xml");
		m_out.print(" ");
		printNameValue("version", doc.getXmlVersion());
		m_out.print(" ");
		printNameValue("encoding", doc.getXmlEncoding());
		m_out.print(" ");
		printNameValue("standalone", doc.getXmlStandalone() ? "yes" : "no");
		m_out.println("?>");
		
		print(doc.getDocumentElement());
	}
	
	/**
	 * Read XML from istream, and print the formatted Document on the output stream.
	 * @param istream The XML input stream.
	 * @throws SAXException If the input XML isn't valid.
	 * @throws ParserConfigurationException An internal error.
	 * @throws IOException If an I/O error occurs while reading istream.
	 */
	public void print(InputStream istream)
		throws SAXException, ParserConfigurationException, IOException
	{
		XMLParser parser = new XMLParser(true);
		parser.parse(istream);
		Document doc = parser.getDocument();
		print(doc);
	}
	
	/**
	 * Read XML from a file, and print the formatted Document on the output stream.
	 * @param fileName The XML input file.
	 * @throws SAXException If the input XML isn't valid.
	 * @throws ParserConfigurationException An internal error.
	 * @throws IOException If fileName doesn't exist or an I/O error occurs while reading it.
	 */
	public void printFile(String fileName)
		throws SAXException, ParserConfigurationException, IOException
	{
		XMLParser parser = new XMLParser(true);
		parser.parseFile(fileName);
		Document doc = parser.getDocument();
		print(doc);
	}
	
	private void printIndent()
	{
		printIndent(0);
	}
	
	private void printIndent(int extra)
	{
		for (int i = 0; i < m_level + extra; i++)
			m_out.print("  ");
	}
	
	private void print(Node node)
	{
		if (node instanceof Element) {
			print((Element)node);
		} else if (node instanceof Text) {
			print((Text)node);
		} else if (node instanceof CDATASection) {
			print((CDATASection)node);
		} else if (node instanceof Comment) {
			print((Comment)node);
		} else {
			printIndent();
			m_out.println("UNKNOWN NODE TYPE " + node.getNodeName());
		}
	}
	
	/**
	 * Print a formatted tree under an Element.
	 * @param elem
	 */
	public void print(Element elem)
	{
		printIndent();
		m_out.print("<");
		m_out.print(elem.getNodeName());
		NamedNodeMap attrMap = elem.getAttributes();
		int nAttrs = (attrMap != null) ? attrMap.getLength() : 0;
		if (nAttrs > 0) {
			for (int i = 0; i < nAttrs; i++) {
				Node item = attrMap.item(i);
				if (item instanceof Attr) {
					if (nAttrs > 1) {
						m_out.println();
						printIndent(3);
					} else {
						m_out.print(" ");
					}
					Attr attr = (Attr)item;
					printNameValue(attr.getNodeName(), attr.getNodeValue());
				} else {
					m_out.println();
					printIndent(3);
					m_out.print("UNKNOWN NODE TYPE " + item.getNodeName() + " IN ATTR");
				}
			}
		}
		NodeArrayList kids = new NodeArrayList(elem);
		if (kids.size() > 0) {
			m_out.println(">");
			m_level++;
			for (Node kid: kids) {
				print(kid);
			}
			--m_level;
			printIndent();
			m_out.println("</" + elem.getNodeName() + ">");
		} else {
			m_out.println("/>");
		}
	}
	
	/**
	 * Print a formatted tree under an Element on standard output.
	 * @param desc Text to print first.
	 * @param elem The element to print.
	 */
	public static void print(String desc, Element elem)
	{
		PrintStream out = System.out;
		out.println(desc + ":");
		if (elem == null) {
			out.println("  null element!");
		} else {
			Reformat r = new Reformat(out);
			r.print(elem);
		}
	}
	
	private void print(Text text)
	{
		String value = text.getNodeValue();
		if (value.trim().equals(""))
			return;
		// XXX: Next needs work!
		printIndent();  // ????
		StringBuilder escval = new StringBuilder(value);
		int len = escval.length();
		for (int i = 0; i < len; i++) {
			char c = escval.charAt(i);
			String x = null;
			if (c == '&') {
				x = "&amp;";
			} else if (c == '<') {
				x = "&lt;";
			} else if (c == '>') {
				x = "&gt;";
			} else if (c < 20 || c > 0x7e) {
				x = "&#" + Integer.toHexString(c) + ";";
			}
			if (x != null) {
				int xlen = x.length();
				escval.replace(i, i+1, x);
				len += xlen - 1;
				i += xlen - 1;
			}
		}
		m_out.println(escval.toString());
	}
	
	private void print(CDATASection cdata)
	{
		printIndent();
		m_out.println("<![CDATA[");
		printIndent(1);
		m_out.println(cdata.getNodeValue());
		printIndent();
		m_out.println("]]>");
	}
	
	private void print(Comment comment)
	{
		printIndent();
		m_out.println("<!--");
		printIndent(1);
		m_out.println(comment.getNodeValue());
		printIndent();
		m_out.println("-->");
	}
	
	/**
	 * Output the reformatted versions of one or more XML files to standard output.
	 * @param args A list of XML file names.
	 */
	public static void main(String[] args)
	{
		for (int i = 0; i < args.length; i++) {
			try {
				System.out.println();
				System.out.println(args[i]);
				System.out.println();
				new Reformat(System.out).printFile(args[i]);
			} catch (Exception e) {
				System.out.println(" *** parser threw exception: " + e.getMessage());
			}
		}
	}

}
