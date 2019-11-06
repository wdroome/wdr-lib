package com.wdroome.json;

import java.util.Map;
import java.util.List;
import java.util.Stack;

/**
 * A scanner for the JSON Merge-Patch representation
 * of changes to a JSON data structure, as described
 * in RFC 7386. To use this, create a subclass,
 * override {@link #newValue(List, JSONValue)},
 * and then call {@link #scan(JSONValue_Object)}.
 * The base class is stateless and re-entrent,
 * and hence is thread safe.
 * @author wdr
 */
public abstract class JSONMergePatchScanner
{
	/**
	 * The registered media-type for JSON Merge-Patch.
	 */
	public static final String MERGE_PATCH_MEDIA_TYPE = "application/merge-patch+json";
	
	/**
	 * Scan a JSON Merge-Patch object and call
	 * {@link #newValue(List, JSONValue)} for each modified value.
	 * @param patch A Merge-Patch object.
	 */
	public void scan(JSONValue_Object patch)
	{
		scan(null, patch);
	}
	
	/**
	 * Scan a JSON Merge-Patch object and call
	 * {@link #newValue(List, JSONValue)} for each modified value.
	 * @param path
	 * 		The names of the keys leading to "object".
	 * 		If null, "object" is the root.
	 * @param object
	 * 		A sub-tree of a Merge-Patch object.
	 */
	private void scan(Stack<String> path, JSONValue_Object object)
	{
		if (path == null) {
			path = new Stack<String>();
		}
		enterObject(path, object);
		for (Map.Entry<String,JSONValue> ent: object.entrySet()) {
			String name = ent.getKey();
			JSONValue value = ent.getValue();
			path.push(name);
			if (value instanceof JSONValue_Object) {
				scan(path, (JSONValue_Object)value);
			} else {
				newValue(path, value);
			}
			path.pop();
		}
		leaveObject(path, object);
	}
	
	/**
	 * Called when Merge-Patch has a new value for an element.
	 * The client must override this method, and update the
	 * appropriate data structures.
	 * @param path
	 * 		The path to the new value, as a list of keys.
	 * 		The list is never empty, and the last element
	 * 		is the key for this value.
	 *		The method must not modify this list.
	 * @param value
	 * 		The new value. May be any type except {@link JSONValue_Object}.
	 * 		JSONValue_Null means "delete this item".
	 */
	protected abstract void newValue(List<String> path, JSONValue value);
	
	/**
	 * Called when the algorithm enters a JSON Object in the patch tree,
	 * before calling {@link #newValue(List, JSONValue)} for any items
	 * in that object. The base class does nothing; the client may override if needed.
	 * @param path
	 * 		The path to this object, as a list of keys.
	 * 		The list is empty if object is the root Merge-Patch object.
	 *		The method must not modify this list.
	 * @param object
	 * 		The object itself.
	 */
	protected void enterObject(List<String> path, JSONValue_Object object) {}
	
	/**
	 * Called when the algorithm leaves a JSON Object in the patch tree.
	 * after calling {@link #newValue(List, JSONValue)} for all items
	 * in that object. The base class does nothing; the client may override if needed.
	 * @param path
	 * 		The path to this object, as a list of keys.
	 * 		The list is empty if object is the root Merge-Patch object.
	 *		The method must not modify this list.
	 * @param object
	 * 		The object itself.
	 */
	protected void leaveObject(List<String> path, JSONValue_Object object) {}
}
