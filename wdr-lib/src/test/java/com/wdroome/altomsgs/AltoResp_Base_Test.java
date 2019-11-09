package com.wdroome.altomsgs;

import static org.junit.Assert.*;

import org.junit.Test;

import com.wdroome.json.JSONException;
import com.wdroome.json.JSONLexan;
import com.wdroome.json.JSONValue_Object;
import com.wdroome.altomsgs.AltoResp_Base;

/**
 * @author wdr
 *
 */
public class AltoResp_Base_Test
{
	private static class TestBaseResp1 extends AltoResp_Base
	{
		public TestBaseResp1() throws JSONException { super(); }
		public TestBaseResp1(String jsonSrc) throws JSONException { super(new JSONLexan(jsonSrc)); }
		@Override public String[] getMapNames() { return new String[] {"test-map"}; }
		@Override public String getMediaType() { return "test1"; }
	}
	
	private static class TestBaseResp2 extends AltoResp_Base
	{
		public TestBaseResp2() throws JSONException { super(); }
		public TestBaseResp2(String jsonSrc) throws JSONException { super(new JSONLexan(jsonSrc)); }
		@Override public String[] getMapNames() { return new String[] {"test-map"}; }
		@Override public String[] getArrayNames() { return new String[] {"test-array"}; }
		@Override public String getMediaType() { return "test2"; }
		
		public void putArrayItem(String value) { m_array.add(value); }
		public void putMapItem(String name, String value) throws JSONException { m_map.put(name, value); }
		
		public String catStringItems()
		{
			StringBuilder b = new StringBuilder();
			String sep = "";
			for (Object o: m_array) {
				b.append(sep);
				b.append(o.toString());
				sep = ",";
			}
			return b.toString();
		}
		
		public String getMapString(String name)
		{
			return m_map.getString(name, null);
		}
	}

	@Test
	public void test() throws JSONException
	{
		TestBaseResp1 resp = new TestBaseResp1();
		resp.setThisVtag("myid", "mytag");
		resp.setDependentVtag("otherid", "othertag");
		resp.setCostMetric("routingcost");
		resp.setCostMode("numeric");
		resp.setMetaValue("def-cost", 24.2);
		
		String expected =
						  "{\n"
						+ "  \"meta\": {\n"
						+ "    \"cost-type\": {\n"
						+ "      \"cost-metric\": \"routingcost\",\n"
						+ "      \"cost-mode\": \"numeric\"\n"
						+ "    },\n"
						+ "    \"def-cost\": 24.2,\n"
						+ "    \"dependent-vtags\": [{\n"
						+ "      \"resource-id\": \"otherid\",\n"
						+ "      \"tag\": \"othertag\"\n"
						+ "    }],\n"
						+ "    \"vtag\": {\n"
						+ "      \"resource-id\": \"myid\",\n"
						+ "      \"tag\": \"mytag\"\n"
						+ "    }\n"
						+ "  },\n"
						+ "  \"test-map\": {}\n"
						+ "}";
		TestBaseResp1 respx = new TestBaseResp1(resp.toString());
		assertEquals("JSON", expected.replaceAll("[ \n]",""), respx.toString().replaceAll("[ \n]",""));
		assertEquals("this-id", "myid", respx.getThisResourceId());
		assertEquals("this-tag", "mytag", respx.getThisTag());
		assertEquals("dependent-id", "otherid", respx.getDependentResourceId());
		assertEquals("dependent-tag", "othertag", respx.getDependentTag());
	}
	
	@Test
	public void test2() throws JSONException
	{
		TestBaseResp2 resp2 = new TestBaseResp2();
		resp2.putArrayItem("String 1");
		resp2.putArrayItem("String 2");
		resp2.putMapItem("pid1", "pid1 value");
		resp2.putMapItem("pid2", "pid2 value");
		
		String expected =
						  "{\n"
						+ "  \"meta\": {},\n"
						+ "  \"test-array\": [\n"
						+ "    \"String 1\",\n"
						+ "    \"String 2\"\n"
						+ "  ],\n"
						+ "  \"test-map\": {\n"
						+ "    \"pid1\": \"pid1 value\",\n"
						+ "    \"pid2\": \"pid2 value\"\n"
						+ "  }\n"
						+ "}";
		TestBaseResp2 resp2b = new TestBaseResp2(resp2.toString());
		assertEquals("JSON", expected.replaceAll("[ \n]",""), resp2.toString().replaceAll("[ \n]",""));
		assertEquals("Strings", "\"String 1\",\"String 2\"", resp2b.catStringItems());
		assertEquals("pid1", "pid1 value", resp2.getMapString("pid1"));
		assertEquals("pid2", "pid2 value", resp2.getMapString("pid2"));
		assertTrue("this-id", resp2.getThisResourceId() == null);
		assertTrue("this-tag", resp2.getThisTag() == null);
		assertTrue("dependent-id", resp2.getDependentResourceId() == null);
		assertTrue("dependent-tag", resp2.getDependentTag() == null);
	}

	public static void main(String[] args) throws JSONException
	{
		TestBaseResp1 resp1 = new TestBaseResp1();
		resp1.setThisVtag("myid", "mytag");
		resp1.setDependentVtag("otherid", "othertag");
		resp1.setCostMetric("routingcost");
		resp1.setCostMode("numeric");
		resp1.setMetaValue("def-cost", 24.2);
		String respStr = resp1.toString();
		System.out.println(respStr);
		
		TestBaseResp1 resp1b = new TestBaseResp1(respStr);
		String resp2Str = resp1b.toString();
		if (!respStr.equals(resp2Str)) {
			System.out.println("NOT EQUAL!!");
			System.out.println(resp2Str);
		}
		
		TestBaseResp2 resp2 = new TestBaseResp2();
		resp2.putArrayItem("String 1");
		resp2.putArrayItem("String 2");
		resp2.putMapItem("pid1", "pid1 value");
		resp2.putMapItem("pid2", "pid2 value");
		System.out.println(resp2.toString());
	}
}
