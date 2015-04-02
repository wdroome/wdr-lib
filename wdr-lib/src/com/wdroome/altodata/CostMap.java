package com.wdroome.altodata;

import java.net.UnknownHostException;

import com.wdroome.json.JSONException;
import com.wdroome.util.inet.EndpointAddress;
import com.wdroome.util.IErrorLogger;
import com.wdroome.altomsgs.AltoCostMapInfo;
import com.wdroome.altomsgs.AltoResp_CostMap;
import com.wdroome.altomsgs.AltoResp_EndpointCost;

/**
 * Represent the costs from source to destination pids for a network map.
 * Note that the network map must be frozen, and hence the PID names are fixed
 * (if the network map changes, we create a new CostMap object).
 * So we store the cost map as a square array, indexed by "pid number."
 * The pid number is just the index of the pid name in a sorted array.
 * A client can access costs either by pid name or by pid index.
 * Also note that cost maps are always numeric. When an ALTO client asks
 * for ordinal costs, other classes ordinalize the CostMap values on the fly.
 * 
 * <p>This class is synchronized.
 * 
 * @author wdr
 */
public class CostMap implements AltoCostMapInfo
{
	/**
	 * The name of this cost metric.
	 */
	private final String m_costMetric;

	/**
	 * The Network Map. Child classes may access but shouldn't change.
	 */
	protected NetworkMap m_map;
	
	/**
	 * The PID names, sorted and in index order. Child classes may access but shouldn't change.
	 */
	protected String[] m_pids;
	
	private int m_nPids;
	private long m_lastModTime = System.currentTimeMillis();
	
	/**
	 * The costs, indexed by source and destination pid numbers.
	 * Because we cannot assume that assignment to floats is atomic,
	 * synch on m_costs whenever accessing this array.
	 * Child classes may access and update values, but should not replace the array.
	 */
	protected float[][] m_costs;
		
	/**
	 * Create a new cost map associated with a NetworkMap.
	 * @param map A frozen NetworkMap.
	 * @throws IllegalStateException If map isn't frozen.
	 */
	public CostMap(String costMetric, NetworkMap map, IErrorLogger errorLogger)
			throws IllegalStateException
	{
		if (!map.isFrozen()) {
			throw new IllegalStateException(getClassLeafName() + " c'tor: NetworkMap is not frozen");
		}
		m_costMetric = costMetric;
		m_map = map;
		m_nPids = map.getNumPids();
		m_costs = new float[m_nPids][];
		m_pids = new String[m_nPids];
		float finitCost = (float)1.0;
		for (int iSrc = 0; iSrc < m_nPids; iSrc++) {
			m_pids[iSrc] = map.indexToPid(iSrc);
			m_costs[iSrc] = new float[m_nPids];
			for (int iDest = 0; iDest < m_nPids; iDest++) {
				m_costs[iSrc][iDest] = finitCost;
			}
		}
	}
	
	/**
	 * Replace the network map. After creating new PID name and cost arrays,
	 * and setting all costs to NaN, for "unknown",
	 * this calls {@link #networkMapChanged(NetworkMap,String[],float[][]) networkMapChanged()}
	 * to copy over the costs for matching PID names.
	 * @param newMap The new map.
	 * @throws IllegalStateException If newMap isn't frozen.
	 */
	public synchronized void setNetworkMap(NetworkMap newMap)
		throws IllegalStateException
	{
		if (!newMap.isFrozen())
			throw new IllegalStateException(getClassLeafName()
							+ ".setNetworkMap(): NetworkMap is not frozen");
		NetworkMap oldMap = m_map;
		String[] oldPids = m_pids;
		float[][] oldCosts = m_costs;
		m_map = newMap;
		m_nPids = newMap.getNumPids();
		m_pids = newMap.getPidNames();
		float finitCost = Float.NaN;
		m_costs = new float[m_nPids][];
		for (int iSrc = 0; iSrc < m_nPids; iSrc++) {
			m_costs[iSrc] = new float[m_nPids];
			for (int iDest = 0; iDest < m_nPids; iDest++) {
				m_costs[iSrc][iDest] = finitCost;
			}
		}
		networkMapChanged(oldMap, oldPids, oldCosts);
	}
	
	/**
	 * Called by {@link #setNetworkMap(NetworkMap)} after replacing the network map.
	 * The base class copies over the costs for the matching PID names.
	 * A child class can override this method to re-create the costs
	 * via some other mechanism. The caller has synchronized on the object,
	 * so this method can use simple assignment to update the new cost array.
	 * When called, all costs in m_costs have been set to NaN.
	 * @param oldMap The old NetworkMap.
	 * @param oldPids The old list of PID names.
	 * @param oldCosts The old cost array, indexed by the old src and dest pid indexes.
	 */
	protected void networkMapChanged(NetworkMap oldMap,
									 String[] oldPids,
									 float[][] oldCosts)
	{
		for (int iOldSrc = 0; iOldSrc < oldPids.length; iOldSrc++) {
			int iNewSrc = m_map.pidToIndex(oldPids[iOldSrc]);
			if (iNewSrc >= 0) {
				for (int iOldDest = 0; iOldDest < oldPids.length; iOldDest++) {
					int iNewDest = m_map.pidToIndex(oldPids[iOldDest]);
					if (iNewDest >= 0) {
						m_costs[iNewSrc][iNewDest] = oldCosts[iOldSrc][iOldDest];
					}
				}
			}
		}
	}
	
	/**
	 * Return the name of the cost-metric for this cost map.
	 * @return The name of the cost-metric for this cost map.
	 */
	public String getCostMetric()
	{
		return m_costMetric;
	}
	
	/**
	 * Return the cost from a source pid to a destination pid.
	 * @param srcPid The name of the source pid.
	 * @param destPid The name of the destination pid.
	 * @return The cost. Return -1 if srcPid or destPid aren't valid.
	 */
	public double getCost(String srcPid, String destPid)
	{
		int iSrc = m_map.pidToIndex(srcPid);
		int iDest = m_map.pidToIndex(destPid);
		if (iSrc >= 0 && iDest >= 0) {
			synchronized (m_costs) {
				refreshCost(iSrc, iDest);
				return float2double(m_costs[iSrc][iDest]);
			}
		}
		return -1;
	}
	
	/**
	 * Return the cost from a source pid to a destination pid.
	 * @param iSrc The index of the source pid.
	 * @param destPid The name of the destination pid.
	 * @return The cost. Return -1 if iSrc or destPid aren't valid.
	 */	
	public double getCost(int iSrc, String destPid)
	{
		int iDest = m_map.pidToIndex(destPid);
		if (iSrc >= 0 && iDest >= 0) {
			synchronized (m_costs) {
				refreshCost(iSrc, iDest);
				return float2double(m_costs[iSrc][iDest]);
			}
		}
		return -1;
	}
	
	/**
	 * Return the cost from a source pid to a destination pid.
	 * @param iSrc The index of the source pid.
	 * @param iDest The index of the destination pid.
	 * @return The cost. Return -1 if iSrc or iDest aren't valid.
	 */	
	public double getCost(int iSrc, int iDest)
	{
		if (iSrc >= 0 && iDest >= 0) {
			synchronized (m_costs) {
				refreshCost(iSrc, iDest);
				return float2double(m_costs[iSrc][iDest]);
			}
		}
		return -1;
	}
	
	private double float2double(float f)
	{
		if (Float.isNaN(f))
			return Float.NaN;
		else
			return ((double)Math.round(1000000*(double)f)) / 1000000;
	}
	
	/**
	 * Return the last modification time of any cost in this cost map
	 * (system time stamp).
	 * @return The last update time.
	 */
	public long getLastModTime()
	{
		synchronized (m_costs) {
			return m_lastModTime;
		}
	}
	
	/**
	 * Called by the various getCost() methods
	 * before they read m_costs[iSrc][iDest].
	 * The base class does nothing.
	 * If needed, a child class should override this method
	 * and update m_costs[iSrc][iDest] directly.
	 * The caller has synchronized on m_paths, so the child class
	 * can use simple assignment.
	 * @param iSrc The source pid.
	 * @param iDest The destination pid.
	 */
	protected void refreshCost(int iSrc, int iDest)
	{
	}
	
	/**
	 * Set the cost from srcPid to destPid.
	 * @param srcPid The name of the source pid.
	 * @param destPid The name of the destination pid.
	 * @param cost The new cost.
	 * @param modTime The effective date of this update. If 0, use "now".
	 * 			If the caller has a number of costs to update,
	 * 			it's more efficient if the caller creates one Date object
	 * 			at the start, and passes it to each call.
	 * @return True if the cost was set.
	 * 		That is, srcPid and destPid are valid, and cost is non-negative.
	 */
	public boolean setCost(String srcPid, String destPid, double cost, long modTime)
	{
		if (modTime == 0)
			modTime = System.currentTimeMillis();
		int iSrc = m_map.pidToIndex(srcPid);
		int iDest = m_map.pidToIndex(destPid);
		if (iSrc >= 0 && iDest >= 0 && (cost >= 0 || Double.isNaN(cost))) {
			synchronized (m_costs) {
				m_costs[iSrc][iDest] = (float)cost;
				m_lastModTime = modTime;
			}
			return true;
		} else {
			return false;
		}
	}
		
	/**
	 * Update the cost map with the costs in a CostMap response message.
	 * @param data A set of costs.
	 * @return True iff all pids are valid; false if some are unknown.
	 */
	public boolean setCosts(AltoResp_CostMap data)
	{
		boolean ret = true;
		int nUpdates = 0;
		for (String srcPid:data.getSrcPIDs()) {
			int iSrc = m_map.pidToIndex(srcPid);
			if (iSrc < 0)
				continue;
			try {
				for (String destPid:data.getDestPIDs(srcPid)) {
					int iDest = m_map.pidToIndex(destPid);
					if (iDest < 0)
						continue;
					try {
						float newCost = (float)data.getCost(srcPid, destPid);
						synchronized (m_costs) {
							m_costs[iSrc][iDest] = newCost;
							nUpdates++;
						}
					} catch (JSONException e) {
						// ignore bad destPid?
						ret = false;
					}
				}
			} catch (JSONException e) {
				// ignore bad srcPid?
				ret = false;
			}
		}
		if (nUpdates > 0) {
			synchronized (m_costs) {
				m_lastModTime = System.currentTimeMillis();
			}
		}
		return ret;
	}
	
	/**
	 * Update the cost map with the costs between the endpoints in an EndpointCost response message.
	 * Map each endpoint to a PID, and set the PID-to-PID cost accordingly.
	 * @param data A set of endpoint costs.
	 * @return An RT_EndpointCost with the final pairwise costs for all endpoints in "data".
	 * 			Note the returned costs may differ from those in "data"
	 * 			if "data" (in effect) gives different costs for the same PID pair.
	 * 			Eg, if "data" sets the costs for a1:a2 to c1 and a1:a3 to c2,
	 * 			but a2 and a3 are in the same PID, then the returned cost map
	 * 			will report the same cost (either c1 or c2) for a1:a2 and a1:a3.
	 */
	public AltoResp_EndpointCost setCosts(AltoResp_EndpointCost data)
	{
		AltoResp_EndpointCost resp;
		resp = new AltoResp_EndpointCost();
		resp.setCostMetric(getCostMetric());
		resp.setCostMode(AltoResp_CostMap.COST_MODE_NUMERICAL);
		for (String srcEndpoint:data.getSrcAddrs()) {
			EndpointAddress srcAddr;
			String srcPid;
			try {
				srcAddr = new EndpointAddress(srcEndpoint);
				srcPid = m_map.getPID(srcAddr);
			} catch (Exception e) {
				// Ignore bad src address.
				continue;
			}
			int iSrc = m_map.pidToIndex(srcPid);
			if (iSrc < 0)
				continue;
			try {
				for (String destEndpoint:data.getDestAddrs(srcEndpoint)) {
					EndpointAddress destAddr;
					String destPid;
					try {
						destAddr = new EndpointAddress(destEndpoint);
						destPid = m_map.getPID(destAddr);
					} catch (Exception e) {
						// ignore bad dest address.
						continue;
					}
					int iDest = m_map.pidToIndex(destPid);
					if (iDest < 0)
						continue;
					try {
						float newCost = (float)data.getCost(srcEndpoint, destEndpoint);
						synchronized (m_costs) {
							m_costs[iSrc][iDest] = newCost;
							if (!Double.isNaN(newCost))
								resp.setCost(srcEndpoint, destEndpoint, newCost);
						}
					} catch (JSONException e) {
						// ignore bad destPid?
						continue;
					}
				}
			} catch (JSONException e) {
				// ignore bad dest addr.
				continue;
			}
		}
		return resp;
	}
	
	/**
	 * Set all costs to the same value.
	 * @param cost The new cost.
	 */
	public void setAllCosts(double cost)
	{
		float fcost = (float)cost;
		synchronized (m_costs) {
			for (int iSrc = 0; iSrc < m_nPids; iSrc++) {
				for (int iDest = 0; iDest < m_nPids; iDest++) {
					m_costs[iSrc][iDest] = fcost;
				}
			}
		}
	}
	
	/**
	 * Return the number of PIDs in this cost map.
	 */
	public int getNumPids()
	{
		return m_nPids;
	}
	
	/**
	 * Return the names of the PIDs in this cost map.
	 */
	public String[] getPids()
	{
		return m_pids;
	}
	
	/**
	 * Return the pid name for an index.
	 * @param index The index.
	 * @return The name of the pid with that index.
	 */
	public String indexToPid(int index)
	{
		return m_map.indexToPid(index);
	}
	
	/**
	 * Return the index for a PID name.
	 * @param pid The pid name.
	 * @return The index, or -1 if pid isn't valid.
	 */
	public int pidToIndex(String pid)
	{
		return m_map.pidToIndex(pid);
	}

	/**
	 * Return an array with the indexes for a set of PID names.
	 * @param pids The pid names.
	 * @return The pid indexes. -1 means that pid isn't valid.
	 */
	public int[] pidsToIndexes(String[] pids)
	{
		if (pids == null)
			return new int[0];
		int[] indexes = new int[pids.length];
		for (int i = 0; i < pids.length; i++)
			indexes[i] = m_map.pidToIndex(pids[i]);
		return indexes;
	}
	
	/**
	 * Return an array with the PID indexes for a set of endpoint addresses.
	 * @param endpoints The endpoint addresses.
	 * @return The pid indexes. -1 means we don't have a pid for that endpoint.
	 * @throws UnknownHostException If a element of endpoints isn't a valid IP address.
	 */
	public int[] endpointsToIndexes(String[] endpoints)
			throws UnknownHostException
	{
		int[] indexes = new int[endpoints.length];
		for (int i = 0; i < endpoints.length; i++) {
			indexes[i] = m_map.pidToIndex(m_map.getPID(new EndpointAddress(endpoints[i])));
		}
		return indexes;
	}
	
	/**
	 * Return the resource ID of the network map associated with this cost map.
	 */
	public String getMapId()
	{
		return m_map.getMapId();
	}
	
	/**
	 * Return the version tag of the network map associated with this cost map.
	 */
	public String getMapVtag()
	{
		return m_map.getVtag();
	}
	
	/**
	 * Return the leaf-name of this class.
	 */
	protected String getClassLeafName()
	{
		String name = getClass().getName();
		int lastDot = name.lastIndexOf('.');
		if (lastDot >= 0)
			return name.substring(lastDot+1);
		else
			return name;
	}
}
