package com.wdroome.osc.eos;

import java.io.IOException;
import java.net.InetSocketAddress;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.TreeMap;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.wdroome.osc.OSCConnection;
import com.wdroome.osc.OSCMessage;
import com.wdroome.osc.OSCUtil;

/**
 * An EOSConnection to get information from an EOS server.
 * @author wdr
 */
public class QueryEOS extends OSCConnection
{
	private long m_timeoutMS = 2500;

	/**
	 * Create connection to query an EOS server.
	 * @param addr The server's inet socket address.
	 */
	public QueryEOS(InetSocketAddress addr)
	{
		super(addr);
	}

	/**
	 * Create connection to query an EOS server.
	 * @param addr The server's inet address.
	 * @param port The server's port.
	 */
	public QueryEOS(String addr, int port) throws IllegalArgumentException
	{
		super(addr, port);
	}

	/**
	 * Create connection to query an EOS server.
	 * @param addr An ipaddr:port string with the server's inet socket address.
	 */
	public QueryEOS(String addrPort) throws IllegalArgumentException
	{
		super(addrPort);
	}
	
	public long getTimeoutMS() {
		return m_timeoutMS;
	}

	public void setTimeoutMS(long timeoutMS) {
		this.m_timeoutMS = timeoutMS;
	}

	/**
	 * Get the EOS version.
	 * @return The EOS version.
	 * @throws IOException If an IO error occurs.
	 */
	public String getVersion() throws IOException
	{
		return getStringReply(EOSUtil.GET_VERSION_METHOD, EOSUtil.GET_VERSION_REPLY, m_timeoutMS);
	}
	
	/**
	 * Get all cue lists.
	 * @return A TreeMap with all cue lists. The key is the cuelist number, as a string.
	 * @throws IOException If an IO error occurs.
	 */
	public TreeMap<Integer, EOSCuelistInfo> getCuelists() throws IOException
	{
		if (!isConnected()) {
			connect();
		}
		int nCuelists = getCuelistCount();
		TreeMap<Integer, EOSCuelistInfo> cuelists = new TreeMap<>();
		for (int iCuelist = 0; iCuelist < nCuelists; iCuelist++) {
			EOSCuelistInfo cuelist = new EOSCuelistInfo(iCuelist, this, m_timeoutMS);
			if (cuelist.isValid()) {
				cuelists.put(cuelist.getCuelistNumber(), cuelist);
			}
		}
		return cuelists;
	}
	
	/**
	 * Get the number of cues in a cuelist.
	 * @param cuelistNumber The cuelist number.
	 * @return The number of cues in that list, or -1.
	 * @throws IOException If an IO error occurs.
	 */
	public int getCueCount(int cuelistNumber) throws IOException
	{
		String method = String.format(EOSUtil.GET_CUE_COUNT_METHOD, cuelistNumber);
		String replyPat = String.format(EOSUtil.GET_CUE_COUNT_REPLY, cuelistNumber);
		return getIntReply(method, replyPat, m_timeoutMS);
	}
	
	/**
	 * Get the number of cues in the default cue list.
	 * @return The number of cues in the default cue list.
	 * @throws IOException If an IO error occurs.
	 */
	public int getCueCount() throws IOException
	{
		return getCueCount(EOSUtil.DEFAULT_CUE_LIST);
	}

	/**
	 * Get the number of cuelists.
	 * @return The number of cuelists.
	 * @throws IOException If an IO error occurs.
	 */	
	public int getCuelistCount() throws IOException
	{
		return getIntReply(EOSUtil.GET_CUELIST_COUNT_METHOD, EOSUtil.GET_CUELIST_COUNT_REPLY, m_timeoutMS);
	}
	
	/**
	 * For testing, get and print cuelists and other information.
	 * @param args The EOS servers ipaddr:port.
	 * @throws IOException If an IO error occurs.
	 */
	public static void main(String[] args) throws IOException
	{
		try (QueryEOS queryEOS = new QueryEOS(args[0])) {
			queryEOS.connect();
			long startTS = System.currentTimeMillis();
			System.out.println("Version: " + queryEOS.getVersion());
			System.out.println("Cuelist count: " + queryEOS.getCuelistCount());
			System.out.println("Cuelists: " + queryEOS.getCuelists());
			System.out.println(String.format("Elapsed time: %.3f sec.",
						(System.currentTimeMillis() - startTS)/1000.0));
		} catch (IllegalArgumentException e) {
			System.err.println(e);
		}
	}
}