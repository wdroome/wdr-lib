package com.wdroome.altomsgs;

import static org.junit.Assert.*;

import org.junit.Test;

import com.wdroome.json.JSONValue_Object;
import com.wdroome.json.JSONException;
import com.wdroome.altomsgs.*;

/**
 * @author wdr
 */
public class AltoResp_Error_Test
{

	@Test
	public void testError1() throws JSONException
	{
		AltoResp_Error err0 = new AltoResp_Error(AltoResp_Error.ERROR_CODE_MISSING_FIELD, "field-foo");
		
		AltoResp_Error err1 = new AltoResp_Error(err0.getJSON());
		String actual = err1.toString();
		String expected =
			  "{\n"
			+ "\"meta\": {\"code\": \"E_MISSING_FIELD\", \"field\": \"field-foo\"}\n"
			+ "}";
		assertEquals("JSON", expected, actual);
		assertEquals(AltoResp_Error.ERROR_CODE_MISSING_FIELD, err1.getCode());
		assertEquals("field-foo", err1.getField());
		assertTrue(err1.getValue() == null);
		assertTrue(err1.getSyntaxError() == null);
	}

	@Test
	public void testError2() throws JSONException
	{
		AltoResp_Error err0 = new AltoResp_Error(AltoResp_Error.ERROR_CODE_MISSING_FIELD, null);
		
		AltoResp_Error err1 = new AltoResp_Error(err0.getJSON());
		String actual = err1.toString();
		String expected =
			  "{\n"
			+ "\"meta\": {\"code\": \"E_MISSING_FIELD\"}\n"
			+ "}";
		assertEquals("JSON", expected, actual);
		assertEquals(AltoResp_Error.ERROR_CODE_MISSING_FIELD, err1.getCode());
		assertTrue(err1.getField() == null);
		assertTrue(err1.getValue() == null);
		assertTrue(err1.getSyntaxError() == null);
	}
	
	@Test
	public void testError3() throws JSONException
	{
		AltoResp_Error err0 = new AltoResp_Error(AltoResp_Error.ERROR_CODE_INVALID_FIELD_VALUE, "field-foo", "\"bad-value\"");
		
		AltoResp_Error err1 = new AltoResp_Error(err0.getJSON());
		String actual = err1.toString();
		String expected =
			  "{\n"
			+ "\"meta\": {"
			+ "\"code\": \"E_INVALID_FIELD_VALUE\","
			+ " \"field\": \"field-foo\","
			+ " \"value\": \"\\\"bad-value\\\"\"}\n"
			+ "}";
		assertEquals("JSON", expected, actual);
		assertEquals(AltoResp_Error.ERROR_CODE_INVALID_FIELD_VALUE, err1.getCode());
		assertEquals("field-foo", err1.getField());
		assertEquals("\"bad-value\"", err1.getValue());
		assertTrue(err1.getSyntaxError() == null);
	}
	
	@Test
	public void testError4() throws JSONException
	{
		AltoResp_Error err0 = new AltoResp_Error(AltoResp_Error.ERROR_CODE_SYNTAX, "syntax error on \"foo\"");
		
		AltoResp_Error err1 = new AltoResp_Error(err0.getJSON());
		String actual = err1.toString();
		String expected =
			  "{\n"
			+ "\"meta\": {"
			+ "\"code\": \"E_SYNTAX\","
			+ " \"syntax-error\": \"syntax error on \\\"foo\\\"\"}\n"
			+ "}";
		assertEquals("JSON", expected, actual);
		assertEquals(AltoResp_Error.ERROR_CODE_SYNTAX, err1.getCode());
		assertEquals("syntax error on \"foo\"", err1.getSyntaxError());
		assertTrue(err1.getField() == null);
		assertTrue(err1.getValue() == null);
	}

	public static void main(String[] args) throws JSONException
	{
		AltoResp_Error err0 = new AltoResp_Error(AltoResp_Error.ERROR_CODE_MISSING_FIELD, "field-foo");
		
		System.out.println(err0.toString());
	}
}
