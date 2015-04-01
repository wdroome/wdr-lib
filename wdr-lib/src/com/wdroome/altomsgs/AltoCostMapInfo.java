package com.wdroome.altomsgs;

/**
 * An interface to retrieve the cost-map data necessary
 * to generate a response message.
 * A source of cost-map data will implement this interface,
 * and a client of this package will pass an instance
 * of that cost-map class to an ALTO message class
 * that needs the data in a cost-map.
 * <p>
 * We assume the costs are numeric-mode; if ordinal costs are required,
 * the client of this interface ordinalizes the numeric cost data.
 * <p>
 * Any cost-map class that implements this interface
 * must assign an internal index number to each PID.
 * The index numbers must run from 0 to N-1,
 * and this interface defines methods
 * to map index numbers to PID names and vice versa.
 * Typically the cost-map provider will sort the PID names
 * and use the sequence numbers as indexes, but we do NOT
 * require the PID names to be sorted.
 * 
 * @author wdr
 */
public interface AltoCostMapInfo
{
	/**
	 * Return the name of the cost-metric for this cost map.
	 * @return The name of the cost-metric for this cost map.
	 */
	public String getCostMetric();
	
	/**
	 * Return the resource ID of the network map associated with this cost map.
	 */
	public String getMapId();
	
	/**
	 * Return the version tag of the network map associated with this cost map.
	 */
	public String getMapVtag();

	/**
	 * Return the number of PIDs in this cost map.
	 */
	public int getNumPids();

	/**
	 * Return the pid name for an index.
	 * @param index The index.
	 * @return The name of the pid with that index.
	 */
	public String indexToPid(int index);
	
	/**
	 * Return the index for a PID name.
	 * @param pid The pid name.
	 * @return The index, or -1 if pid isn't valid.
	 */
	public int pidToIndex(String pid);
	
	/**
	 * Return the cost from a source pid to a destination pid.
	 * @param iSrc The index of the source pid.
	 * @param iDest The index of the destination pid.
	 * @return The cost. Return -1 if iSrc or iDest aren't valid.
	 */	
	public double getCost(int iSrc, int iDest);
	
	/**
	 * Return the cost from a source pid to a destination pid.
	 * @param srcPid The name of the source pid.
	 * @param destPid The name of the destination pid.
	 * @return The cost. Return -1 if srcPid or destPid aren't valid.
	 */
	public double getCost(String srcPid, String destPid);
}
