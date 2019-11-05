package test.junit.altomsgs;

import java.util.Arrays;

import com.wdroome.json.JSONException;
import com.wdroome.altomsgs.*;
import com.wdroome.json.JSONValue_Array;
import com.wdroome.json.JSONValue_Object;

import org.junit.*;

import static org.junit.Assert.*;

/**
 * @author wdr
 */
public class AltoResp_InfoResourceDir_Test extends CommonTestMethods
{
	@Test
	public void testInfoResourceDir() throws JSONException
	{
		AltoResp_InfoResourceDir map0 = new AltoResp_InfoResourceDir();
		map0.addResource("id1", "uri1",
						"ret-type1",
						null,
						new String[] {"req0"},
						null);
		map0.setDefaultNetworkMapId("id1");
		map0.addResource("id2", "uri2",
						"ret-type3",
						"req-type4",
						null,
						null);
		JSONValue_Object cap3 = new JSONValue_Object();
		cap3.put("cost-modes", new JSONValue_Array(new String[] {"numerical", "ordinal"}));
		cap3.put("cost-types", new JSONValue_Array(new String[] {"routingcost"}));
		cap3.put("cost-constraints", true);
		map0.addResource("id3", "uri3",
				"ret-type5",
				"req-type1",
				null,
				cap3);
		map0.addResourceOpcode("id4", "uri4",
				"ret-type6", "req-type6", new String[] {"req0"}, null, "SET_SOMETHING", null);
		map0.addCostTypeName("num-rtg", "routingcost", "numerical", "some description");
		map0.addCostTypeName("ord-rtg", "routingcost", "ordinal", null);
		
		String actual = map0.toString();
		
		String expected =
				  "{\n"
				+ "  \"meta\": {\n"
				+ "    \"cost-types\": {\n"
				+ "      \"num-rtg\": {\n"
				+ "        \"cost-metric\": \"routingcost\",\n"
				+ "        \"cost-mode\": \"numerical\",\n"
				+ "        \"description\": \"some description\"\n"
				+ "      },\n"
				+ "      \"ord-rtg\": {\n"
				+ "        \"cost-metric\": \"routingcost\",\n"
				+ "        \"cost-mode\": \"ordinal\"\n"
				+ "      }\n"
				+ "    },\n"
				+ "    \"default-alto-network-map\": \"id1\"\n"
				+ "  },\n"
				+ "  \"resources\": {\n"
				+ "    \"id1\": {\n"
				+ "      \"media-type\": \"ret-type1\",\n"
				+ "      \"uri\": \"uri1\",\n"
				+ "      \"uses\": [\"req0\"]\n"
				+ "    },\n"
				+ "    \"id2\": {\n"
				+ "      \"accepts\": \"req-type4\",\n"
				+ "      \"media-type\": \"ret-type3\",\n"
				+ "      \"uri\": \"uri2\"\n"
				+ "    },\n"
				+ "    \"id3\": {\n"
				+ "      \"accepts\": \"req-type1\",\n"
				+ "      \"capabilities\": {\n"
				+ "        \"cost-constraints\": true,\n"
				+ "        \"cost-modes\": [\n"
				+ "          \"numerical\",\n"
				+ "          \"ordinal\"\n"
				+ "        ],\n"
				+ "        \"cost-types\": [\"routingcost\"]\n"
				+ "      },\n"
				+ "      \"media-type\": \"ret-type5\",\n"
				+ "      \"uri\": \"uri3\"\n"
				+ "    },\n"
				+ "    \"id4\": {\n"
				+ "      \"accepts\": \"req-type6\",\n"
				+ "      \"media-type\": \"ret-type6\",\n"
				+ "      \"priv:alu-opcode\": \"SET_SOMETHING\",\n"
				+ "      \"uri\": \"uri4\",\n"
				+ "      \"uses\": [\"req0\"]\n"
				+ "    }\n"
				+ "  }\n"
				+ "}";
		assertEquals("JSON", expected.replaceAll("[ \n]",""), actual.replaceAll("[ \n]",""));
		
		AltoResp_InfoResourceDir map1 = new AltoResp_InfoResourceDir(actual);
		assertEquals("size", 4, map1.size());
		assertEquals("default id", "id1", map1.getDefaultNetworkMapId());
		
		assertEquals("uri[0]", "uri1", map1.getURI("id1"));
		assertEquals("uri[1]", "uri2", map1.getURI("id2"));
		assertEquals("uri[2]", "uri3", map1.getURI("id3"));
		assertEquals("uri[3]", "uri4", map1.getURI("id4"));
		
		assertEquals("media-types[0]", "ret-type1", map1.getMediaType("id1"));
		assertEquals("media-types[1]", "ret-type3", map1.getMediaType("id2"));
		assertEquals("media-types[2]", "ret-type5", map1.getMediaType("id3"));
		assertEquals("media-types[3]", "ret-type6", map1.getMediaType("id4"));

		assertTrue("accepts[0] null", map1.getAccepts("id1") == null);
		assertEquals("accepts[1]", "req-type4", map1.getAccepts("id2"));
		assertEquals("accepts[2]", "req-type1", map1.getAccepts("id3"));
		assertEquals("accepts[3]", "req-type6", map1.getAccepts("id4"));
		
		assertTrue("cap[0] null", map1.getCapabilities("id1") == null);
		assertTrue("cap[1] null", map1.getCapabilities("id2") == null);
		assertTrue("cap[3] null", map1.getCapabilities("id4") == null);
		JSONValue_Object cap2 = map1.getCapabilities("id3");
		assertEquals("cap[2] keys",
					"String[cost-constraints,cost-modes,cost-types]",
					catArray(sort(cap2.keyArray())));
		assertTrue("cap[2] cost-constraints", cap2.getBoolean("cost-constraints"));
		assertEquals("cap[2] cost-modes", "String[numerical,ordinal]",
					catArray(sort(AltoResp_InfoResourceDir.getStringArray(cap2.getArray("cost-modes")))));
		assertEquals("cap[2] cost-types", "String[routingcost]",
				catArray(sort(AltoResp_InfoResourceDir.getStringArray(cap2.getArray("cost-types")))));

		assertTrue("opcode[0] null", map1.getOpcode("id1") == null);
		assertTrue("opcode[1] null", map1.getOpcode("id2") == null);
		assertTrue("opcode[2] null", map1.getOpcode("id3") == null);
		assertEquals("opcode[3]", "SET_SOMETHING", map1.getOpcode("id4"));
		
		assertEquals("findCostType num-rtg", "num-rtg", map1.findCostTypeName("routingcost", "numerical"));
		assertEquals("findCostType num-rtg", "ord-rtg", map1.findCostTypeName("routingcost", "ordinal"));
	}

	public static void main(String[] args) throws JSONException
	{
		AltoResp_InfoResourceDir map0 = new AltoResp_InfoResourceDir();
		map0.addCostTypeName("num-rtg", "routingcost", "numerical", "some description");
		map0.addCostTypeName("ord-rtg", "routingcost", "ordinal", null);
		map0.addResource("id1", "uri1",
				"ret-type1",
				null,
				new String[] {"req0"},
				null);
		map0.setDefaultNetworkMapId("id1");
		map0.addResource("id2", "uri2",
				"ret-type3",
				"req-type4",
				null,
				null);
		JSONValue_Object cap3 = new JSONValue_Object();
		cap3.put("cost-modes", new JSONValue_Array(new String[] {"numerical", "ordinal"}));
		cap3.put("cost-types", new JSONValue_Array(new String[] {"routingcost"}));
		cap3.put("cost-constraints", true);
		map0.addResource("id3", "uri3",
				"ret-type5",
				"req-type1",
				null,
				cap3);
		map0.addResourceOpcode("id4", "uri4",
				"ret-type6", "req-type6", new String[] {"req0"}, null, "SET_SOMETHING", null);
		
		System.out.println(map0.toString());
	}
}
