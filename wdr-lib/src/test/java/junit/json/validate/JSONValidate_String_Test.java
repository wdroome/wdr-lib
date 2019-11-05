package test.junit.json.validate;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.ArrayList;

import com.wdroome.json.validate.*;
import com.wdroome.json.*;

/**
 * @author wdr
 */
public class JSONValidate_String_Test
{

	@Test
	public void test1() throws JSONValidationException
	{
		JSONValue_String value1 = new JSONValue_String("String 1");
		JSONValue_String valuex = new JSONValue_String("String X");

		JSONValidate_String valList = new JSONValidate_String(
							new String[] {"String 0", "String 1", "String 2"});
		
		ArrayList<String> errors = new ArrayList<String>();
		valList.collectErrors(errors);
	
		boolean passed = valList.validate(value1);
		assertTrue("value 1 passed", passed);
		assertTrue("value 1 no errors", errors.isEmpty());
		
		errors.clear();
		passed = valList.validate(valuex);
		assertFalse("value x failed", passed);
		assertEquals("value x error count", 1, errors.size());
		assertEquals("value x error value",
					"Illegal string \"String X\" at (root)", errors.get(0));
	}

	@Test
	public void test2() throws JSONValidationException
	{
		JSONValue_String value1 = new JSONValue_String("String 1");
		JSONValue_String valuex = new JSONValue_String("String X");
		JSONValue_String value1a = new JSONValue_String("String 1a");

		JSONValidate_String valList = new JSONValidate_String(
							"String [0-9]");
		
		ArrayList<String> errors = new ArrayList<String>();
		valList.collectErrors(errors);
	
		boolean passed = valList.validate(value1);
		assertTrue("value 1 passed", passed);
		assertTrue("value 1 no errors", errors.isEmpty());
		
		errors.clear();
		passed = valList.validate(valuex);
		assertFalse("value x failed", passed);
		assertEquals("value x error count", 1, errors.size());
		assertEquals("value x error value",
					"String \"String X\" does not match \"String [0-9]\" at (root)",
					errors.get(0));
		
		errors.clear();
		passed = valList.validate(value1a);
		assertFalse("value 1a failed", passed);
		assertEquals("value 1a error count", 1, errors.size());
		assertEquals("value 1a error value",
				"String \"String 1a\" does not match \"String [0-9]\" at (root)",
				errors.get(0));
	}

}
