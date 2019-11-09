package com.wdroome.misc;

import org.w3c.dom.Element;

import com.wdroome.xmltools.ElementArrayList;
import com.wdroome.xmltools.XMLParser;
import com.wdroome.xmltools.XMLParser.BufferHandler;

/**
 * @author wdr
 */
public class TestXMLParser1
{
	
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
				walk(root, 0);
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
	
	public static void walk(Element elem, int depth)
	{
		indent(depth);
		System.out.print(elem.getTagName());
		System.out.println("  path: " + XMLParser.getPath(elem));
		for (Element kid: new ElementArrayList(elem)) {
			walk(kid, depth+1);
		}
	}
	
	public static void indent(int depth)
	{
		System.out.print("    ");
		for (int i = 0; i < depth; i++)
			System.out.print("  ");
	}

}
