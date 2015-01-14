package com.wdroome.xmltools;

import java.util.ArrayList;

import org.w3c.dom.*;

/**
 * An ArrayList that holds the Element children of a Node.
 * The c'tor loads the children into the underlying ArrayList.
 * For example, the following loops over all Element children of a parent node:
 * <pre>
 *      for (Element node: new IterableElementList(parent)) {
 *          ... access element ...
 *      }
 * </pre>
 * This works even if the parent has no children.
 * 
 * @author wdr
 */
@SuppressWarnings("serial")
public class ElementArrayList extends ArrayList<Element>
{
	public ElementArrayList(Node parent)
	{
		this(parent.getChildNodes());
	}
	
	/**
	 * Capture all children with a specific tagname.
	 * Note: The elements must be in the default name space.
	 * @param parent The parent node.
	 * @param name The child node tag name (in the default name space).
	 */
	public ElementArrayList(Element parent, String name)
	{
		this(parent.getChildNodes(), new String[] {name}, null);
	}
		
	/**
	 * Capture all direct children whose local names match a string in the desiredNames array.
	 * Note that this disregards namespaces; it gets tags from any namespace.
	 * If desiredNames is null, accept all children.
	 * @param parent The parent node.
	 * @param desiredNames The tag names of the desired children.
	 */
	public ElementArrayList(Element parent, String[] desiredNames)
	{
		this(parent.getChildNodes(), desiredNames, null);
	}
	
	/**
	 * Capture all direct children whose local names
	 * match a string in the desiredNames array,
	 * but exclude any children whose names match a string in the excludedNames array.
	 * Note that this disregards namespaces; it gets tags from any namespace.
	 * If desiredNames is null, accept all children.
	 * If excludedNames is null, do not exclude any children.
	 * @param parent The parent node.
	 * @param desiredNames The tag names of the desired children.
	 * @param excludedNames The tag names of undesired children.
	 */
	public ElementArrayList(Element parent, String[] desiredNames, String[] excludedNames)
	{
		this(parent.getChildNodes(), desiredNames, excludedNames);
	}
	
	/**
	 * Create an ArrayList from a NodeList.
	 * @param childNodes A NodeList.
	 */
	public ElementArrayList(NodeList childNodes)
	{
		this(childNodes, null, null);
	}

	/**
	 * Capture all Elements in childNodes whose local names
	 * match a string in the desiredNames array, and do not
	 * match a string in the excludedNames array
	 * Note that this disregards namespaces; it gets tags from any namespace.
	 * If desiredNames is null, accept all children.
	 * If excludedNames is null, do not exclude any children.
	 * @param childNodes A NodeList.
	 * @param desiredNames The tag names of the desired children.
	 * @param excludedNames The tag names of undesired children.
	 */	
	public ElementArrayList(NodeList childNodes, String[] desiredNames, String[] excludedNames)
	{
		int len = (childNodes != null) ? childNodes.getLength() : 0;
		for (int i = 0; i < len; i++) {
			Node kid = childNodes.item(i);
			if (kid instanceof Element) {
				String tagName = kid.getLocalName();
				if (inArray(tagName, desiredNames)
						&& (excludedNames == null || !inArray(tagName, excludedNames))) {
					add((Element)kid);
				}
			}
		}
	}
	
	/**
	 * Return the first Element in the list with a tag name.
	 * Note that this ignores namespaces.
	 * @param tagName A tag name.
	 * @return The first Element in the list named "tagName".
	 * 		If there are no Elements with that name, return null.
	 */
	public Element get(String tagName)
	{
		for (Element e:this) {
			if (tagName.equals(e.getTagName()))
				return e;
		}
		return null;
	}
	
	/**
	 * Return the first child Element of "elem" named "name."
	 * This ignores name spaces.
	 * @param elem An Element.
	 * @param name A tag name.
	 * @return The first child Element of "elem" named "name."
	 * 		If there are none, return null.
	 */
	public static Element getChild(Element elem, String name)
	{
		NodeList kids = elem.getChildNodes();
		if (kids == null)
			return null;
		int nKids = kids.getLength();
		for (int i = 0; i < nKids; i++) {
			Node kid = kids.item(i);
			if (kid instanceof Element) {
				String s = ((Element)kid).getTagName();
				if (s != null && s.equals(name))
					return (Element)kid;
			}
		}
		return null;
	}
	
	/**
	 * Return true iff s matches a string in arr.
	 * Return true if arr is null, but return false if arr is 0-length.
	 */
	private boolean inArray(String s, String[] arr)
	{
		if (arr == null)
			return true;
		for (String t: arr) {
			if (s.equals(t))
				return true;
		}
		return false;
	}
}
