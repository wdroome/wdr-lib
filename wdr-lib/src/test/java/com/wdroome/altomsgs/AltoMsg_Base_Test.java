package com.wdroome.altomsgs;

import static org.junit.Assert.*;

import org.junit.Test;

import com.wdroome.json.JSONException;
import com.wdroome.json.JSONFieldMissingException;
import com.wdroome.json.JSONLexan;
import com.wdroome.json.JSONValue_Object;
import com.wdroome.json.JSONValueTypeException;
import com.wdroome.altomsgs.AltoMsg_Base;

/**
 * @author wdr
 *
 */
public class AltoMsg_Base_Test
{
	public static class TestMsg1 extends AltoMsg_Base
	{
		public final JSONValue_Object m_fld1;
		public final JSONValue_Object m_fld1_flda;
		public final JSONValue_Object m_fld2;
		
		@Override
		public String getMediaType() { return "media-type"; }
		
		@Override
		protected boolean needPathNames() { return true; }
		
		public TestMsg1() throws JSONException
		{
			super();
			m_json.put("fld1", m_fld1 = new JSONValue_Object());
			m_json.put("fld2", m_fld2 = new JSONValue_Object());
			m_fld1.put("flda", m_fld1_flda = new JSONValue_Object());
		}
		
		public TestMsg1(String src) throws JSONException
		{
			super(new JSONLexan(src));
			m_json.put("fld1", m_fld1 = new JSONValue_Object());
			m_json.put("fld2", m_fld2 = new JSONValue_Object());
			m_fld1.put("flda", m_fld1_flda = new JSONValue_Object());
		}
	}
	
	public static class TestMsg2 extends AltoMsg_Base
	{
		public final JSONValue_Object m_fld1;
		public final JSONValue_Object m_fld1_flda;
		public final JSONValue_Object m_fld2;
		
		@Override
		public String getMediaType() { return "media-type"; }
		
		public TestMsg2() throws JSONException
		{
			super();
			m_json.put("fld1", m_fld1 = new JSONValue_Object());
			m_json.put("fld2", m_fld2 = new JSONValue_Object());
			m_fld1.put("flda", m_fld1_flda = new JSONValue_Object());
		}
		
		public TestMsg2(String src) throws JSONException
		{
			super(new JSONLexan(src));
			m_json.put("fld1", m_fld1 = new JSONValue_Object());
			m_json.put("fld2", m_fld2 = new JSONValue_Object());
			m_fld1.put("flda", m_fld1_flda = new JSONValue_Object());
		}
	}

	@Test
	public void test1a() throws JSONException
	{
		TestMsg1 msg0 = new TestMsg1();
		assertEquals("fld1", "/fld1", msg0.m_fld1.getPathName());
		assertEquals("fld2", "/fld2", msg0.m_fld2.getPathName());
		assertEquals("fld1_flda", "/fld1/flda", msg0.m_fld1_flda.getPathName());
		
		TestMsg1 msg1 = new TestMsg1(msg0.toString());
		assertEquals("fld1", "/fld1", msg1.m_fld1.getPathName());
		assertEquals("fld2", "/fld2", msg1.m_fld2.getPathName());
		assertEquals("fld1_flda", "/fld1/flda", msg1.m_fld1_flda.getPathName());
	}

	@Test
	public void test1b() throws JSONException
	{
		TestMsg1 msg0 = new TestMsg1();
		try {
			msg0.m_fld1.getString("foo");
			fail("getString(foo) didn't throw exception");
		} catch (JSONFieldMissingException e) {
			assertEquals("fld1.foo", "/fld1/foo", e.getField());
		}

		msg0.m_fld1_flda.put("str", "String value");
		msg0.m_fld1_flda.put("num", 42);
		assertEquals("fld1.flda.str as string", "String value", msg0.m_fld1_flda.getString("str"));
		assertEquals("fld1.flda.str as num", 42.0, msg0.m_fld1_flda.getNumber("num"), .0001);
		try {
			msg0.m_fld1_flda.getString("num");
			fail("getString(num) didn't throw exception");
		} catch (JSONValueTypeException e) {
			assertEquals("fld1.flda.num", "/fld1/flda/num", e.getField());
		}
		try {
			msg0.m_fld1_flda.getNumber("str");
			fail("getNumber(str) didn't throw exception");
		} catch (JSONValueTypeException e) {
			assertEquals("fld1.flda.str", "/fld1/flda/str", e.getField());
		}
	}

	@Test
	public void test2a() throws JSONException
	{
		TestMsg2 msg0 = new TestMsg2();
		assertTrue("fld1", null == msg0.m_fld1.getPathName());
		assertTrue("fld2", null == msg0.m_fld2.getPathName());
		assertTrue("fld1_flda", null == msg0.m_fld1_flda.getPathName());
		
		TestMsg2 msg1 = new TestMsg2(msg0.toString());
		assertTrue("fld1", null == msg1.m_fld1.getPathName());
		assertTrue("fld2", null == msg1.m_fld2.getPathName());
		assertTrue("fld1_flda", null == msg1.m_fld1_flda.getPathName());
	}

	@Test
	public void test2b() throws JSONException
	{
		TestMsg2 msg0 = new TestMsg2();
		try {
			msg0.m_fld1.getString("foo");
			fail("getString(foo) didn't throw exception");
		} catch (JSONFieldMissingException e) {
			assertTrue("fld1.foo", null == e.getField());
		}

		msg0.m_fld1_flda.put("str", "String value");
		msg0.m_fld1_flda.put("num", 42);
		assertEquals("fld1.flda.str as string", "String value", msg0.m_fld1_flda.getString("str"));
		assertEquals("fld1.flda.str as num", 42, msg0.m_fld1_flda.getNumber("num"), 0.0001);
		try {
			msg0.m_fld1_flda.getString("num");
			fail("getString(num) didn't throw exception");
		} catch (JSONValueTypeException e) {
			assertTrue("fld1.flda.num", null == e.getField());
		}
		try {
			msg0.m_fld1_flda.getNumber("str");
			fail("getNumber(str) didn't throw exception");
		} catch (JSONValueTypeException e) {
			assertTrue("fld1.flda.str", null == e.getField());
		}
	}

	public static void main(String[] args) throws JSONException
	{
		TestMsg1 msg0 = new TestMsg1();
		System.out.println(msg0.m_fld1.getPathName());
		System.out.println(msg0.m_fld2.getPathName());
		System.out.println(msg0.m_fld1_flda.getPathName());
		System.out.println(msg0.toString());
	}
}
