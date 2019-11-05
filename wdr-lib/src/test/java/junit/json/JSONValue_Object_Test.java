package test.junit.json;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import com.wdroome.util.ArrayToList;
import com.wdroome.json.*;

/**
 * @author wdr
 */
public class JSONValue_Object_Test
{
	
	@Test
	public void test1() throws JSONParseException, JSONValueTypeException, JSONFieldMissingException
	{
		int nkeys = 10;
		String pkey = "key";
		JSONValue_Object jobj = new JSONValue_Object();
		StringBuffer expected = new StringBuffer();
		String prefix = "{";
		for (int i = 0; i < nkeys; i++) {
			jobj.put(pkey + i, i);
			expected.append(prefix);
			prefix = ",";
			expected.append("\"" + pkey + i + "\":" + i);
		}
		expected.append("}");
		
		String json = JSONUtil.toJSONString(jobj, true, false);
		// System.out.println(expected);
		// System.out.println(json);
		assertEquals(expected.toString(), json);
		
		JSONValue_Object jobj2 = JSONParser.parseObject(new JSONLexan(json), true);
		assertEquals("size", nkeys, jobj2.size());
		for (int i = 0; i < nkeys; i++) {
			assertEquals("key" + i, i, jobj2.getNumber("key" + i), .001);
		}
	}
	
	@Test
	public void test2() throws JSONParseException, JSONValueTypeException, JSONFieldMissingException
	{
		int nkeys = 10;
		String pkey = "key";
		String pval = "value";
		JSONValue_Object jobj = new JSONValue_Object();
		StringBuffer expected = new StringBuffer();
		String prefix = "{";
		for (int i = 0; i < nkeys; i++) {
			jobj.put(pkey + i, pval + i);
			expected.append(prefix);
			prefix = ",";
			expected.append("\"" + pkey + i + "\":\"" + pval + i + "\"");
		}
		expected.append("}");
		
		String json = JSONUtil.toJSONString(jobj, true, false);
		// System.out.println(expected);
		// System.out.println(json);
		assertEquals(expected.toString(), json);
		
		JSONValue_Object jobj2 = JSONParser.parseObject(new JSONLexan(json), true);
		assertEquals("size", nkeys, jobj2.size());
		for (int i = 0; i < nkeys; i++) {
			assertEquals(pkey + i, pval + i, jobj2.getString("key" + i));
		}
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testPutPath() throws IOException
	{
		JSONValue_Object obj = new JSONValue_Object();
		List<String>[] paths;
		paths = new List[] {
			new ArrayToList<String>(new String[] {"key0"}),
			new ArrayToList<String>(new String[] {"key0a", "key1"}),
			new ArrayToList<String>(new String[] {"key0a", "key1a", "key2a"}),
			new ArrayToList<String>(new String[] {"key0a", "key1a", "key2b"}),
			new ArrayToList<String>(new String[] {"key0b", "key1b", "key2a"}),
			new ArrayToList<String>(new String[] {"key0b", "key1c"}),
			new ArrayToList<String>(new String[] {"key0c"}),
		};
		for (List<String> path: paths) {
			obj.putPath(path, new JSONValue_String(path.toString()));
		}
		for (List<String> path: paths) {
			JSONValue actual = obj.getPath(path, JSONValue_Null.NULL);
			assertEquals("path " + path, new JSONValue_String(path.toString()), actual);
		}

		if (false) {
			StringBuilder buff = new StringBuilder();
			JSONWriter writer = new JSONWriter(buff);
			writer.setSorted(true);
			writer.setIndented(true);
			obj.writeJSON(writer);
			System.out.println(buff.toString());
		}
		
		int n = countLeaves(obj);
		assertEquals("before removal count", paths.length, n);
		for (List<String> path: paths) {
			obj.removePath(path);
			assertEquals("count after removing " + path.toString(), --n, countLeaves(obj));
		}
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testMergePatch() throws IOException
	{
		JSONValue_Object patch = new JSONValue_Object();
		List<String>[] paths;
		paths = new List[] {
			new ArrayToList<String>(new String[] {"key0"}),
			new ArrayToList<String>(new String[] {"key0a", "key1"}),
			new ArrayToList<String>(new String[] {"key0a", "key1a", "key2a"}),
			new ArrayToList<String>(new String[] {"key0a", "key1a", "key2b"}),
			new ArrayToList<String>(new String[] {"key0b", "key1b", "key2a"}),
			new ArrayToList<String>(new String[] {"key0b", "key1c"}),
			new ArrayToList<String>(new String[] {"key0c"}),
		};
		for (List<String> path: paths) {
			patch.putPath(path, new JSONValue_String(path.toString()));
		}
		
		JSONValue_Object obj = new JSONValue_Object();
		obj.applyMergePatch(patch);
		
		for (List<String> path: paths) {
			JSONValue actual = obj.getPath(path, JSONValue_Null.NULL);
			assertEquals("string patch path " + path, new JSONValue_String(path.toString()), actual);
		}

		if (false) {
			printObject(obj);
		}
		
		patch = new JSONValue_Object();
		int n = 0;
		for (List<String> path: paths) {
			patch.putPath(path, new JSONValue_Number(n));
			n++;
		}

		obj.applyMergePatch(patch);
		
		n = 0;
		for (List<String> path: paths) {
			JSONValue actual = obj.getPath(path, JSONValue_Null.NULL);
			assertEquals("number patch path " + path, new JSONValue_Number(n), actual);
			n++;
		}

		patch = new JSONValue_Object();
		for (List<String> path: paths) {
			patch.putPath(path, JSONValue_Null.NULL);
		}
		obj.applyMergePatch(patch);
		assertEquals("delete all leaf count", 0, countLeaves(obj));
		
		HashSet<List<String>> objPaths = new HashSet<List<String>>();
		for (List<String> path: paths) {
			if (path.size() > 1) {
				objPaths.add(path.subList(0, path.size()-1));
			}
		}
		if (false) {
			System.out.println(objPaths);
			printObject(obj);
		}
		assertEquals("delete all object count", objPaths.size(), countObjects(obj));
	}
	
	/**
	 * Return the total number of non-object elements in an object tree.
	 */
	private int countLeaves(JSONValue_Object obj)
	{
		int n = 0;
		for (JSONValue value: obj.values()) {
			if (value instanceof JSONValue_Object) {
				n += countLeaves((JSONValue_Object)value);
			} else {
				n++;
			}
		}
		return n;
	}
	
	/**
	 * Return the total number of object elements in an object tree,
	 * not counting "obj".
	 */
	private int countObjects(JSONValue_Object obj)
	{
		int n = 0;
		for (JSONValue value: obj.values()) {
			if (value instanceof JSONValue_Object) {
				n += 1 + countObjects((JSONValue_Object)value);
			}
		}
		return n;
	}
	
	private void printObject(JSONValue_Object obj) throws IOException
	{
		StringBuilder buff = new StringBuilder();
		JSONWriter writer = new JSONWriter(buff);
		writer.setSorted(true);
		writer.setIndented(true);
		obj.writeJSON(writer);
		System.out.println(buff.toString());
	}
}
