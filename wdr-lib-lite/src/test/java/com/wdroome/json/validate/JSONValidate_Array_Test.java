package com.wdroome.json.validate;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.ArrayList;

import com.wdroome.json.validate.*;
import com.wdroome.json.*;

/**
 * @author wdr
 */
public class JSONValidate_Array_Test
{

	@Test
	public void test1() throws JSONValidationException
	{	
		JSONValue_Array arr = new JSONValue_Array();
		arr.add(new JSONValue_String("String 0"));
		arr.add(new JSONValue_String("String 1"));
		arr.add(new JSONValue_String("String 1a"));
		arr.add(new JSONValue_String("String X"));
		arr.add(new JSONValue_String("String 2"));

		JSONValidate_String svalList = new JSONValidate_String(
				new String[] {"String 0", "String 1", "String 2"});
		JSONValidate_Array arrVal = new JSONValidate_Array(svalList);
	
		ArrayList<String> errors = new ArrayList<String>();
		arrVal.collectErrors(errors);
	
		boolean passed = arrVal.validate(arr);
		assertFalse("arr failed", passed);
		assertEquals("error count", 2, errors.size());
		assertEquals("error 0",
					"Illegal string \"String 1a\" at [2]", errors.get(0));
		assertEquals("error 1",
					"Illegal string \"String X\" at [3]", errors.get(1));
	}

	@Test
	public void test2() throws JSONValidationException
	{
		JSONValue_Array arr = new JSONValue_Array();
		arr.add(new JSONValue_Number(0));
		arr.add(new JSONValue_Number(1));
		arr.add(new JSONValue_Number(9));
		arr.add(new JSONValue_Number(10));
		arr.add(new JSONValue_Number(-1));

		JSONValidate_Number val0_9 = new JSONValidate_Number(0, 9);
		
		ArrayList<String> errors = new ArrayList<String>();
		JSONValidate_Array arrVal = new JSONValidate_Array(val0_9);
		arrVal.collectErrors(errors);
	
		boolean passed = arrVal.validate(arr);
		assertFalse("arr failed", passed);
		assertEquals("error count", 2, errors.size());
		assertEquals("error 0",
					"10.0 greater than 9.0 at [3]", errors.get(0));
		assertEquals("error 1",
					"-1.0 less than 0.0 at [4]", errors.get(1));
	}

}
