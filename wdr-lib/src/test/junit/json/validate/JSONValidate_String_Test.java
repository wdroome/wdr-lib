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
	public void test() throws JSONValidationException
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

}
