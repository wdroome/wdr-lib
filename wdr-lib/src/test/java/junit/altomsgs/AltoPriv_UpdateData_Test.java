package test.junit.altomsgs;

import static org.junit.Assert.*;

import org.junit.Test;

import com.wdroome.json.JSONException;
import com.wdroome.altomsgs.*;
import com.wdroome.json.JSONValue_Object;
import com.wdroome.altomsgs.AltoPriv_UpdateData;

/**
 * @author wdr
 */
public class AltoPriv_UpdateData_Test extends CommonTestMethods
{

	@Test
	public void test_StringArray() throws JSONException
	{
		AltoPriv_UpdateData req0 = new AltoPriv_UpdateData();
		req0.addString("1.2.3.4");
		req0.addStrings(new String[] {"4.3.2.1", "192.0.0.1"});
		req0.setDependentVtag("mapid", null);
		
		AltoPriv_UpdateData req1 = new AltoPriv_UpdateData(req0.getJSON());
		String actual = req1.toString();
		String expected =
						  "{\n"
						+ "  \"meta\": {\"dependent-vtags\": [{\n"
						+ "    \"resource-id\": \"mapid\",\n"
						+ "    \"tag\": \"\"\n"
						+ "  }]},\n"
						+ "  \"object-data\": {},\n"
						+ "  \"strings\": [\n"
						+ "    \"1.2.3.4\",\n"
						+ "    \"4.3.2.1\",\n"
						+ "    \"192.0.0.1\"\n"
						+ "  ]\n"
						+ "}";
		assertEquals("JSON", expected.replaceAll("[ \n]",""), actual.replaceAll("[ \n]",""));
		assertEquals("getStrings", "String[1.2.3.4,4.3.2.1,192.0.0.1]",
						catArray(req1.getStrings()));
		assertEquals("mapId", "mapid", req1.getDependentResourceId());
	}

	public static void main(String[] args) throws JSONException
	{
		AltoPriv_UpdateData req0 = new AltoPriv_UpdateData();
		req0.addString("1.2.3.4");
		req0.addStrings(new String[] {"4.3.2.1", "192.0.0.1"});
		req0.setDependentVtag("mapid", null);
		
		System.out.println(req0.toString());
		System.out.println("getStrings: " + catArray(req0.getStrings()));
	}
}
