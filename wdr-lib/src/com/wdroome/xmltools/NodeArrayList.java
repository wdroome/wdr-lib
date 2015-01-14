package com.wdroome.xmltools;

import java.util.ArrayList;

import org.w3c.dom.*;

/**
 * An ArrayList that holds the children of a Node.
 * The c'tor loads the children into the underlying ArrayList.
 * For example, the following loops over all children of a parent node:
 * <pre>
 *      for (Node node: new IterableNodeList(parent)) {
 *          ... access node ...
 *      }
 * </pre>
 * This works even if the parent has no children.
 * 
 * @author wdr
 */
@SuppressWarnings("serial")
public class NodeArrayList extends ArrayList<Node>
{
	/**
	 * Create an ArrayList with Nodes in NodeList.
	 * @param childNodes A set of Nodes.
	 */
	public NodeArrayList(NodeList childNodes)
	{
		int len = (childNodes != null) ? childNodes.getLength() : 0;
		for (int i = 0; i < len; i++) {
			add(childNodes.item(i));
		}
	}
	
	/**
	 * Create an ArrayList with the children of a Node.
	 * @param parent The parent Node.
	 */
	public NodeArrayList(Node parent)
	{
		this(parent.getChildNodes());
	}
}
