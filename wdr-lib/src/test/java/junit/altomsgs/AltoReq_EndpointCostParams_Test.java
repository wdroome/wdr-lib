package test.junit.altomsgs;

import static org.junit.Assert.*;

import org.junit.Test;

import com.wdroome.json.JSONException;
import com.wdroome.altomsgs.*;

/**
 * @author wdr
 */
public class AltoReq_EndpointCostParams_Test extends CommonTestMethods
{

	@Test
	public void testReqNetworkMap() throws JSONException
	{
		AltoReq_EndpointCostParams req0 = new AltoReq_EndpointCostParams();
		req0.addSource("1.1.1.1");
		req0.addSources(new String[] {"1.1.1.3", "1.1.1.2"});
		req0.addDestination("1.1.1.6");
		req0.addDestinations(new String[] {"1.1.1.7", "1.1.1.5"});
		req0.setCostMode("numerical");
		req0.setCostMetric("routingcost");
		
		AltoReq_EndpointCostParams req1 = new AltoReq_EndpointCostParams(req0.getJSON());
		String actual = req1.toString();
		String expected =
			  "{\n"
			+ "\"cost-type\": {\"cost-metric\": \"routingcost\", \"cost-mode\": \"numerical\"}, \n"
			+ "\"endpoints\": {\n"
			+ "  \"dsts\": [\"ipv4:1.1.1.6\", \"ipv4:1.1.1.7\", \"ipv4:1.1.1.5\"], \n"
			+ "  \"srcs\": [\"ipv4:1.1.1.1\", \"ipv4:1.1.1.3\", \"ipv4:1.1.1.2\"]\n"
			+ "}\n"
			+ "}";
		assertEquals("JSON", expected, actual);
		assertEquals("getSources", "String[ipv4:1.1.1.1,ipv4:1.1.1.3,ipv4:1.1.1.2]",
						catArray(req1.getSources()));
		assertEquals("getDestinations", "String[ipv4:1.1.1.6,ipv4:1.1.1.7,ipv4:1.1.1.5]",
						catArray(req1.getDestinations()));
	}

	public static void main(String[] args) throws JSONException
	{
		AltoReq_EndpointCostParams req0 = new AltoReq_EndpointCostParams();
		req0.addSource("1.1.1.1");
		req0.addSources(new String[] {"1.1.1.3", "1.1.1.2"});
		req0.addDestination("1.1.1.6");
		req0.addDestinations(new String[] {"1.1.1.7", "1.1.1.5"});
		req0.setCostMode("numerical");
		req0.setCostMetric("routingcost");
		
		System.out.println(req0.toString());
		System.out.println("Sources: " + catArray(req0.getSources()));
		System.out.println("Destinations: " + catArray(req0.getDestinations()));
	}
}
