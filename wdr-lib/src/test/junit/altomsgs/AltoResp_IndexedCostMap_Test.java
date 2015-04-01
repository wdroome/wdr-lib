package test.junit.altomsgs;

import java.io.*;
import java.util.Arrays;

import com.wdroome.json.JSONException;
import com.wdroome.json.JSONFieldMissingException;
import com.wdroome.json.JSONParseException;
import com.wdroome.json.JSONValueTypeException;
import com.wdroome.altomsgs.*;
import com.wdroome.json.JSONValue_Object;

import org.junit.*;

import static org.junit.Assert.*;

/**
 * @author wdr
 */
public class AltoResp_IndexedCostMap_Test extends CommonTestMethods
{
	@Test
	public void testIndexedCostMap() throws JSONException, IOException
	{
		AltoResp_IndexedCostMap map0 = new AltoResp_IndexedCostMap();
		map0.setDependentVtag("id", "12349876");
		map0.setCostMode("numerical");
		map0.setCostMetric("routingcost");
		map0.setCost("PID1", "PID1", 1);
		map0.setCost("PID1", "PID2", 5);
		map0.setCost("PID1", "PID3", 10);
		map0.setCost("PID2", "PID1", 5);
		map0.setCost("PID2", "PID2", 1);
		map0.setCost("PID2", "PID3", 15);
		map0.setCost("PID3", "PID1", 20);
		map0.setCost("PID3", "PID2", 15);
		map0.setCost("PID3", "PID3", 1);
		map0.setCost("PID4", "PID1", 12);
		assertTrue("Def-cost/0", Double.isNaN(map0.getDefaultCost()));
 		assertTrue("PID4=>PID2", Double.isNaN(map0.getCost("PID4", "PID2")));
		String actual = map0.toString();

		String expectedCostIds =
				  "{\"meta\":{\n"
				+ "  \"dependent-vtags\":[{\"resource-id\":\"id\",\"tag\":\"12349876\"}],\n"
				+ "  \"cost-type\":{\"cost-metric\":\"routingcost\",\"cost-mode\":\"numerical\"}\n"
				+ "  },\n"
				+ "\"cost-map\": {\n"
				+ "  \"PID1\": {\n"
				+ "    \"PID1\":1.0, \"PID2\":5.0, \"PID3\":10.0  \n"
				+ "  },\n"
				+ "  \"PID2\": {\n"
				+ "    \"PID1\":5.0, \"PID2\":1.0, \"PID3\":15.0  \n"
				+ "  },\n"
				+ "  \"PID3\": {\n"
				+ "    \"PID1\":20.0, \"PID2\":15.0, \"PID3\":1.0  \n"
				+ "  },\n"
				+ "  \"PID4\": {\n"
				+ "    \"PID1\":12.0\n"
				+ "}}}\n";
		assertEquals("JSON", expectedCostIds.replaceAll("[ \n]",""), actual.replaceAll("[ \n]",""));
		
		AltoResp_IndexedCostMap map1 = new AltoResp_IndexedCostMap(actual);
		assertEquals("map-id", "id", map1.getDependentResourceId());
		assertEquals("map-vtag", "12349876", map1.getDependentTag());
		assertEquals("cost-mode", "numerical", map1.getCostMode());
		assertEquals("cost-metric", "routingcost", map1.getCostMetric());
		assertEquals("src-PIDs", "String[PID1,PID2,PID3,PID4]",
				catArray(sort(map1.getSrcPIDs())));
		assertEquals("dest-PIDs", "String[PID1,PID2,PID3]",
				catArray(sort(map1.getDestPIDs())));
		assertEquals("PID1=>PID1",  1.0, map1.getCost("PID1", "PID1"), .001);
		assertEquals("PID1=>PID2",  5.0, map1.getCost("PID1", "PID2"), .001);
		assertEquals("PID1=>PID3", 10.0, map1.getCost("PID1", "PID3"), .001);
		assertEquals("PID2=>PID1",  5.0, map1.getCost("PID2", "PID1"), .001);
		assertEquals("PID2=>PID2",  1.0, map1.getCost("PID2", "PID2"), .001);
		assertEquals("PID2=>PID3", 15.0, map1.getCost("PID2", "PID3"), .001);
		assertEquals("PID3=>PID1", 20.0, map1.getCost("PID3", "PID1"), .001);
		assertEquals("PID3=>PID2", 15.0, map1.getCost("PID3", "PID2"), .001);
		assertEquals("PID3=>PID3",  1.0, map1.getCost("PID3", "PID3"), .001);
		assertEquals("PID4=>PID1", 12.0, map1.getCost("PID4", "PID1"), .001);
		
		// assertEquals("Def-cost", -1, map1.getDefaultCost(), 0.001);
		assertTrue("Def-cost", Double.isNaN(map1.getDefaultCost()));
		assertTrue("PID1=>PID4", Double.isNaN(map1.getCost("PID1", "PID4")));
		assertTrue("PID2=>PID4", Double.isNaN(map1.getCost("PID2", "PID4")));
		assertTrue("PID3=>PID4", Double.isNaN(map1.getCost("PID3", "PID4")));
		assertTrue("PID4=>PID2", Double.isNaN(map1.getCost("PID4", "PID2")));
		assertTrue("PID4=>PID3", Double.isNaN(map1.getCost("PID4", "PID3")));
		assertTrue("PID4=>PID4", Double.isNaN(map1.getCost("PID4", "PID4")));
	}
	
	@Test
	public void testIndexedCostMap2() throws JSONException
	{
		AltoResp_IndexedCostMap map0 = new AltoResp_IndexedCostMap();
		map0.setDependentVtag("id", "12349876");
		map0.setCostMode("numerical");
		map0.setCostMetric("routingcost");
		map0.setCost("PID1", "PID1", 1);
		map0.setCost("PID1", "PID2", 5);
		map0.setCost("PID1", "PID3", 10);
		map0.setCost("PID2", "PID1", 5);
		map0.setCost("PID2", "PID2", 1);
		map0.setCost("PID2", "PID3", 15);
		map0.setCost("PID3", "PID1", 20);
		map0.setCost("PID3", "PID2", 15);
		map0.setCost("PID3", "PID3", 1);
		map0.setCost("PID4", "PID1", 12);
		assertTrue("Def-cost/0", Double.isNaN(map0.getDefaultCost()));
 		assertTrue("PID4=>PID2", Double.isNaN(map0.getCost("PID4", "PID2")));
		String actual = map0.toString();
		
		AltoResp_CostMap map1 = new AltoResp_CostMap(actual);
		assertEquals("map-id", "id", map1.getDependentResourceId());
		assertEquals("map-vtag", "12349876", map1.getDependentTag());
		assertEquals("cost-mode", "numerical", map1.getCostMode());
		assertEquals("cost-metric", "routingcost", map1.getCostMetric());
		assertEquals("src-PIDs", "String[PID1,PID2,PID3,PID4]",
				catArray(sort(map1.getSrcPIDs())));
		assertEquals("dest-PIDs", "String[PID1,PID2,PID3]",
				catArray(sort(map1.getDestPIDs())));
		assertEquals("PID1=>PID1",  1.0, map1.getCost("PID1", "PID1"), .001);
		assertEquals("PID1=>PID2",  5.0, map1.getCost("PID1", "PID2"), .001);
		assertEquals("PID1=>PID3", 10.0, map1.getCost("PID1", "PID3"), .001);
		assertEquals("PID2=>PID1",  5.0, map1.getCost("PID2", "PID1"), .001);
		assertEquals("PID2=>PID2",  1.0, map1.getCost("PID2", "PID2"), .001);
		assertEquals("PID2=>PID3", 15.0, map1.getCost("PID2", "PID3"), .001);
		assertEquals("PID3=>PID1", 20.0, map1.getCost("PID3", "PID1"), .001);
		assertEquals("PID3=>PID2", 15.0, map1.getCost("PID3", "PID2"), .001);
		assertEquals("PID3=>PID3",  1.0, map1.getCost("PID3", "PID3"), .001);
		assertEquals("PID4=>PID1", 12.0, map1.getCost("PID4", "PID1"), .001);
		
		// assertEquals("Def-cost", -1, map1.getDefaultCost(), 0.001);
		assertTrue("Def-cost", Double.isNaN(map1.getDefaultCost()));
//		assertTrue("PID1=>PID4", Double.isNaN(map1.getCost("PID1", "PID4")));
//		assertTrue("PID2=>PID4", Double.isNaN(map1.getCost("PID2", "PID4")));
//		assertTrue("PID3=>PID4", Double.isNaN(map1.getCost("PID3", "PID4")));
//		assertTrue("PID4=>PID2", Double.isNaN(map1.getCost("PID4", "PID2")));
//		assertTrue("PID4=>PID3", Double.isNaN(map1.getCost("PID4", "PID3")));
//		assertTrue("PID4=>PID4", Double.isNaN(map1.getCost("PID4", "PID4")));
	}
	
	@Test
	public void testIndexedCostMap3() throws JSONException, IOException
	{
		AltoResp_CostMap map0 = new AltoResp_CostMap();
		map0.setDependentVtag("id", "12349876");
		map0.setCostMode("numerical");
		map0.setCostMetric("routingcost");
		map0.setCost("PID1", "PID1", 1);
		map0.setCost("PID1", "PID2", 5);
		map0.setCost("PID1", "PID3", 10);
		map0.setCost("PID2", "PID1", 5);
		map0.setCost("PID2", "PID2", 1);
		map0.setCost("PID2", "PID3", 15);
		map0.setCost("PID3", "PID1", 20);
		map0.setCost("PID3", "PID2", 15);
		map0.setCost("PID3", "PID3", 1);
		map0.setCost("PID4", "PID1", 12);
		assertTrue("Def-cost/0", Double.isNaN(map0.getDefaultCost()));
		String actual = map0.toString();
		
		AltoResp_IndexedCostMap map1 = new AltoResp_IndexedCostMap(actual);
		assertEquals("map-id", "id", map1.getDependentResourceId());
		assertEquals("map-vtag", "12349876", map1.getDependentTag());
		assertEquals("cost-mode", "numerical", map1.getCostMode());
		assertEquals("cost-metric", "routingcost", map1.getCostMetric());
		assertEquals("src-PIDs", "String[PID1,PID2,PID3,PID4]",
				catArray(sort(map1.getSrcPIDs())));
		assertEquals("dest-PIDs", "String[PID1,PID2,PID3]",
				catArray(sort(map1.getDestPIDs())));
		assertEquals("PID1=>PID1",  1.0, map1.getCost("PID1", "PID1"), .001);
		assertEquals("PID1=>PID2",  5.0, map1.getCost("PID1", "PID2"), .001);
		assertEquals("PID1=>PID3", 10.0, map1.getCost("PID1", "PID3"), .001);
		assertEquals("PID2=>PID1",  5.0, map1.getCost("PID2", "PID1"), .001);
		assertEquals("PID2=>PID2",  1.0, map1.getCost("PID2", "PID2"), .001);
		assertEquals("PID2=>PID3", 15.0, map1.getCost("PID2", "PID3"), .001);
		assertEquals("PID3=>PID1", 20.0, map1.getCost("PID3", "PID1"), .001);
		assertEquals("PID3=>PID2", 15.0, map1.getCost("PID3", "PID2"), .001);
		assertEquals("PID3=>PID3",  1.0, map1.getCost("PID3", "PID3"), .001);
		assertEquals("PID4=>PID1", 12.0, map1.getCost("PID4", "PID1"), .001);
		
		// assertEquals("Def-cost", -1, map1.getDefaultCost(), 0.001);
		assertTrue("Def-cost", Double.isNaN(map1.getDefaultCost()));
		assertTrue("PID1=>PID4", Double.isNaN(map1.getCost("PID1", "PID4")));
		assertTrue("PID2=>PID4", Double.isNaN(map1.getCost("PID2", "PID4")));
		assertTrue("PID3=>PID4", Double.isNaN(map1.getCost("PID3", "PID4")));
		assertTrue("PID4=>PID2", Double.isNaN(map1.getCost("PID4", "PID2")));
		assertTrue("PID4=>PID3", Double.isNaN(map1.getCost("PID4", "PID3")));
		assertTrue("PID4=>PID4", Double.isNaN(map1.getCost("PID4", "PID4")));
	}

	@Test
	public void testCostMapDef() throws JSONException, IOException
	{
		AltoResp_IndexedCostMap map0 = new AltoResp_IndexedCostMap();
		map0.setDependentVtag("id", "12349876");
		map0.setCostMode("numerical");
		map0.setCostMetric("routingcost");
		map0.setCost("PID1", "PID1", 1);
		map0.setCost("PID1", "PID2", 5);
		map0.setCost("PID1", "PID3", 10);
		map0.setCost("PID2", "PID1", 5);
		map0.setCost("PID2", "PID2", 1);
		map0.setCost("PID2", "PID3", 15);
		map0.setCost("PID3", "PID1", 20);
		map0.setCost("PID3", "PID2", 15);
		map0.setCost("PID3", "PID3", 1);
		map0.setDefaultCost(42);
		String actual = map0.toString();
	
		String expectedCostIds =
				  "{\"meta\":{\n"
				+ "  \"default-cost\":42.0,\n"
				+ "  \"dependent-vtags\":[{\"resource-id\":\"id\",\"tag\":\"12349876\"}],\n"
				+ "  \"cost-type\":{\"cost-metric\":\"routingcost\",\"cost-mode\":\"numerical\"}\n"
				+ "  },\n"
				+ "\"cost-map\": {\n"
				+ "  \"PID1\": {\n"
				+ "    \"PID1\":1.0, \"PID2\":5.0, \"PID3\":10.0  \n"
				+ "  },\n"
				+ "  \"PID2\": {\n"
				+ "    \"PID1\":5.0, \"PID2\":1.0, \"PID3\":15.0  \n"
				+ "  },\n"
				+ "  \"PID3\": {\n"
				+ "    \"PID1\":20.0, \"PID2\":15.0, \"PID3\":1.0\n"
				+ "}}}\n";
		//System.out.println(actual);
		//System.out.println(expectedCostIds);
	
		assertEquals("JSON", expectedCostIds, actual);
		
		AltoResp_IndexedCostMap map1 = new AltoResp_IndexedCostMap(actual);
		assertEquals("map-vtag", "12349876", map1.getDependentTag());
		assertEquals("cost-mode", "numerical", map1.getCostMode());
		assertEquals("cost-metric", "routingcost", map1.getCostMetric());
		assertEquals("src-PIDs", "String[PID1,PID2,PID3]",
				catArray(sort(map1.getSrcPIDs())));
		assertEquals("dest-PIDs", "String[PID1,PID2,PID3]",
				catArray(sort(map1.getDestPIDs())));
		assertEquals("PID1=>PID1",  1.0, map1.getCost("PID1", "PID1"), .001);
		assertEquals("PID1=>PID2",  5.0, map1.getCost("PID1", "PID2"), .001);
		assertEquals("PID1=>PID3", 10.0, map1.getCost("PID1", "PID3"), .001);
		assertEquals("PID2=>PID1",  5.0, map1.getCost("PID2", "PID1"), .001);
		assertEquals("PID2=>PID2",  1.0, map1.getCost("PID2", "PID2"), .001);
		assertEquals("PID2=>PID3", 15.0, map1.getCost("PID2", "PID3"), .001);
		assertEquals("PID3=>PID1", 20.0, map1.getCost("PID3", "PID1"), .001);
		assertEquals("PID3=>PID2", 15.0, map1.getCost("PID3", "PID2"), .001);
		assertEquals("PID3=>PID3",  1.0, map1.getCost("PID3", "PID3"), .001);
		
		assertEquals("Def-cost", 42, map1.getDefaultCost(), 0.001);
	}

	@Test
	public void testCostMapDefNaN() throws JSONException, IOException
	{
		AltoResp_IndexedCostMap map0 = new AltoResp_IndexedCostMap();
		map0.setDependentVtag("id", "12349876");
		map0.setCostMode("numerical");
		map0.setCostMetric("routingcost");
		map0.setCost("PID1", "PID1", 1);
		map0.setCost("PID1", "PID2", 5);
		map0.setCost("PID1", "PID3", 10);
		map0.setCost("PID2", "PID1", 5);
		map0.setCost("PID2", "PID2", 1);
		map0.setCost("PID2", "PID3", 15);
		map0.setCost("PID3", "PID1", 20);
		map0.setCost("PID3", "PID2", 15);
		map0.setCost("PID3", "PID3", 1);
		map0.setDefaultCost(Double.NaN);
		String actual = map0.toString();

		String expectedCostIds =
				  "{\"meta\":{\n"
				+ "  \"default-cost\":\"NaN\",\n"
				+ "  \"dependent-vtags\":[{\"resource-id\":\"id\",\"tag\":\"12349876\"}],\n"
				+ "  \"cost-type\":{\"cost-metric\":\"routingcost\",\"cost-mode\":\"numerical\"}\n"
				+ "  },\n"
				+ "\"cost-map\": {\n"
				+ "  \"PID1\": {\n"
				+ "    \"PID1\":1.0, \"PID2\":5.0, \"PID3\":10.0  \n"
				+ "  },\n"
				+ "  \"PID2\": {\n"
				+ "    \"PID1\":5.0, \"PID2\":1.0, \"PID3\":15.0  \n"
				+ "  },\n"
				+ "  \"PID3\": {\n"
				+ "    \"PID1\":20.0, \"PID2\":15.0, \"PID3\":1.0\n"
				+ "}}}\n";

		assertEquals("JSON", expectedCostIds, actual);
		
		AltoResp_IndexedCostMap map1 = new AltoResp_IndexedCostMap(actual);
		assertEquals("map-vtag", "12349876", map1.getDependentTag());
		assertEquals("map-id", "id", map1.getDependentResourceId());
		assertEquals("cost-mode", "numerical", map1.getCostMode());
		assertEquals("cost-metric", "routingcost", map1.getCostMetric());
		assertEquals("src-PIDs", "String[PID1,PID2,PID3]",
				catArray(sort(map1.getSrcPIDs())));
		assertEquals("dest-PIDs", "String[PID1,PID2,PID3]",
				catArray(sort(map1.getDestPIDs())));
		assertEquals("PID1=>PID1",  1.0, map1.getCost("PID1", "PID1"), .001);
		assertEquals("PID1=>PID2",  5.0, map1.getCost("PID1", "PID2"), .001);
		assertEquals("PID1=>PID3", 10.0, map1.getCost("PID1", "PID3"), .001);
		assertEquals("PID2=>PID1",  5.0, map1.getCost("PID2", "PID1"), .001);
		assertEquals("PID2=>PID2",  1.0, map1.getCost("PID2", "PID2"), .001);
		assertEquals("PID2=>PID3", 15.0, map1.getCost("PID2", "PID3"), .001);
		assertEquals("PID3=>PID1", 20.0, map1.getCost("PID3", "PID1"), .001);
		assertEquals("PID3=>PID2", 15.0, map1.getCost("PID3", "PID2"), .001);
		assertEquals("PID3=>PID3",  1.0, map1.getCost("PID3", "PID3"), .001);
		
		assertEquals("Def-cost", Double.NaN, map1.getDefaultCost(), 0.001);
	}
	
	@Test
	public void nullTest1() throws JSONException, IOException
	{
		AltoResp_IndexedCostMap map1 = getCostMap();
		assertEquals("PID1=>PID1",  1.0, map1.getCost("PID1", "PID1"), .001);
		assertEquals("PID1=>PID2",  5.0, map1.getCost("PID1", "PID2"), .001);
		assertTrue("PID1=>PID3", Double.isNaN(map1.getCost("PID1", "PID3")));
		assertTrue("PID2=>PID3", Double.isNaN(map1.getCost("PID2", "PID3")));
		assertTrue("PID1=>PID4", Double.isNaN(map1.getCost("PID1", "PID4")));
		assertTrue("PID4=>PID1", Double.isNaN(map1.getCost("PID4", "PID1")));
	}
	
	public static AltoResp_IndexedCostMap getCostMap()
			throws JSONParseException, JSONValueTypeException, IOException
	{
		String jsonSrc =
				  "{\n"
				+ "  \"cost-map\": {\n"
				+ "    \"PID1\": {\n"
				+ "      \"PID1\": 1,\n"
				+ "      \"PID2\": 5,\n"
				+ "      \"PID3\": null\n"
				+ "    },\n"
				+ "    \"PID2\": {\n"
				+ "      \"PID1\": 5,\n"
				+ "      \"PID2\": 1,\n"
				+ "      \"PID3\": null\n"
				+ "    },\n"
				+ "    \"PID3\": {\n"
				+ "      \"PID1\": 20,\n"
				+ "      \"PID2\": 15,\n"
				+ "      \"PID3\": 1\n"
				+ "    }\n"
				+ "  },\n"
				+ "  \"meta\": {\n"
				+ "    \"cost-type\": {\n"
				+ "      \"cost-metric\": \"routingcost\",\n"
				+ "      \"cost-mode\": \"numerical\"\n"
				+ "    },\n"
				+ "    \"dependent-vtags\": [{\n"
				+ "      \"resource-id\": \"map\",\n"
				+ "      \"tag\": \"12349876\"\n"
				+ "    }]\n"
				+ "  }\n"
				+ "}";
		return new AltoResp_IndexedCostMap(jsonSrc);
	}

	@Test
	public void testCostMapBig() throws JSONException, IOException
	{
		int nPids = 2000;
		String[] pids = new String[nPids];
		for (int i = 0; i < nPids; i++) {
			pids[i] = "PID_" + i;
		}
		AltoResp_IndexedCostMap map0 = new AltoResp_IndexedCostMap();
		map0.setDependentVtag("id", "12349876");
		map0.setCostMode("numerical");
		map0.setCostMetric("routingcost");
		for (int iSrc = 0; iSrc < nPids; iSrc++) {
			float cost0 = nPids *iSrc;
			for (int iDest = 0; iDest < nPids; iDest++) {
				map0.setCost(pids[iSrc], pids[iDest], cost0 + iDest);
			}
		}
		
		File cacheFile = File.createTempFile("indexedCostMapTest", ".txt");
		cacheFile.deleteOnExit();
		PrintStream out = new PrintStream(
							new BufferedOutputStream(
								new FileOutputStream(cacheFile)));
		map0.writeJSON(out);
		out.close();
		out = null;
		map0 = null;
		
		InputStream in = new BufferedInputStream(new FileInputStream(cacheFile));
		AltoResp_IndexedCostMap map1 = new AltoResp_IndexedCostMap(in, -1);
		in.close();
		in = null;
		assertEquals("map-vtag", "12349876", map1.getDependentTag());
		assertEquals("map-id", "id", map1.getDependentResourceId());
		assertEquals("cost-mode", "numerical", map1.getCostMode());
		assertEquals("cost-metric", "routingcost", map1.getCostMetric());
		for (int iSrc = 0; iSrc < nPids; iSrc++) {
			float cost0 = nPids *iSrc;
			for (int iDest = 0; iDest < nPids; iDest++) {
				assertEquals(iSrc + "=>" + iDest, cost0 + iDest, map1.getCost(pids[iSrc], pids[iDest]), .001);
			}
		}
	}

	private static class TestAltoCostMapInfo implements AltoCostMapInfo
	{
		private final int m_nPids;
		private final String[] m_pids;
			
		public TestAltoCostMapInfo(int nPids)
		{
			m_nPids = nPids;
			m_pids = new String[nPids];
			for (int i = 0; i < nPids; i++) {
				m_pids[i] = "PID_" + i;
			}
		}
		
		@Override
		public String getCostMetric()
		{
			return "routingcost";
		}
		
		@Override
		public String getMapId()
		{
			return "id";
		}

		@Override
		public String getMapVtag()
		{
			return "123456789";
		}

		@Override
		public int getNumPids()
		{
			return m_nPids;
		}

		@Override
		public String indexToPid(int index)
		{
			return m_pids[index];
		}

		@Override
		public int pidToIndex(String pid)
		{
			for (int i = 0; i < m_nPids; i++) {
				if (pid.equals(m_pids[i])) {
					return i;
				}
			}
			return -1;
		}

		@Override
		public double getCost(int iSrc, int iDest)
		{
			return iSrc * m_nPids + iDest;
		}

		@Override
		public double getCost(String srcPid, String destPid)
		{
			return getCost(pidToIndex(srcPid), pidToIndex(destPid));
		}
	}
	
	@Test
	public void testCostMapBig2() throws JSONException, IOException
	{
		int nPids = 2000;
		TestAltoCostMapInfo cminfo = new TestAltoCostMapInfo(nPids);
		
		File cacheFile = File.createTempFile("indexedCostMapTest2", ".txt");
		cacheFile.deleteOnExit();
		PrintStream out = new PrintStream(
							new BufferedOutputStream(
								new FileOutputStream(cacheFile)));
		AltoResp_IndexedCostMap.writeCostMap(out, cminfo, "numerical", true);
		out.close();
		out = null;
		
		InputStream in = new BufferedInputStream(new FileInputStream(cacheFile));
		AltoResp_IndexedCostMap map1 = new AltoResp_IndexedCostMap(in, -1);
		in.close();
		in = null;
		assertEquals("map-vtag", cminfo.getMapVtag(), map1.getDependentTag());
		assertEquals("map-id", cminfo.getMapId(), map1.getDependentResourceId());
		assertEquals("cost-mode", "numerical", map1.getCostMode());
		assertEquals("cost-metric", cminfo.getCostMetric(), map1.getCostMetric());
		for (int iSrc = 0; iSrc < nPids; iSrc++) {
			for (int iDest = 0; iDest < nPids; iDest++) {
				assertEquals(iSrc + "=>" + iDest, cminfo.getCost(iSrc, iDest), map1.getCost(iSrc, iDest), .001);
			}
		}
	}
	
	public static void main(String[] args) throws JSONException, IOException
	{
		AltoResp_IndexedCostMap map0 = new AltoResp_IndexedCostMap();
		map0.setDependentVtag("id", "12349876");
		map0.setCostMode("numerical");
		map0.setCostMetric("routingcost");
		map0.setCost("PID1", "PID1", 1);
		map0.setCost("PID1", "PID2", 5);
		map0.setCost("PID1", "PID3", 10);
		map0.setCost("PID2", "PID1", 5);
		map0.setCost("PID2", "PID2", 1);
		map0.setCost("PID2", "PID3", 15);
		map0.setCost("PID3", "PID1", 20);
		map0.setCost("PID3", "PID2", 15);
		map0.setCost("PID3", "PID3", 1);
		map0.setCost("PID4", "PID4", 12);
		
		String x = map0.toString();
		System.out.println(map0.toString());
		AltoResp_IndexedCostMap map1 = new AltoResp_IndexedCostMap(x);
		System.out.println("map-vtag: " + map1.getDependentTag());
		String[] keys = map1.getSrcPIDs();
		Arrays.sort(keys);
		System.out.println("Src PIDs: " + catArray(keys));
		for (String src:keys) {
			for (String dest:keys) {
				System.out.println("  " + src + "->" + dest + ": " + map1.getCost(src,dest));
			}
		}
	}
}
