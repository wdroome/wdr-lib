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
import com.wdroome.osc.qlab.QLabUtil;

/**
 * An EOSConnection to get information from an EOS server.
 * @author wdr
 */
public class QueryEOS extends OSCConnection implements OSCConnection.MessageHandler
{
	public static final int DEF_TIMEOUT = 3000;
	
	private long m_timeoutMS = DEF_TIMEOUT;
	private String m_showName = "";

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
	
	/**
	 * Create a connection to an EOS controller. Try a list of addresses,
	 * and use the first one that responds to EOS requests.
	 * @param connectTimeoutMS Timeout for establishing the connection, in milliseconds.
	 * 				If 0, use a default timeout.
	 * @param addrPorts A list of IP addresses and ports (addr:port).
	 * @return A QueryEOS for the first address that responds,
	 * 		or null if none do.
	 */
	public static QueryEOS makeQueryEOS(String[] addrPorts, int connectTimeoutMS)
	{
		if (connectTimeoutMS <= 50) {
			connectTimeoutMS = DEF_TIMEOUT;
		}
		for (String addrPort: addrPorts) {
			try {
				QueryEOS queryEOS = new QueryEOS(addrPort);
				queryEOS.setConnectTimeout(connectTimeoutMS);
				queryEOS.connect();
				if (queryEOS.isEos()) {
					return queryEOS;
				}
			} catch (Exception e) {
				// skip, try next address.
			}
		}
		return null;
	}
	
	public long getTimeoutMS() {
		return m_timeoutMS;
	}

	public void setTimeoutMS(long timeoutMS) {
		this.m_timeoutMS = timeoutMS;
	}
	
	@Override
	public void connect() throws IOException
	{
		if (!isConnected()) {
			setMessageHandler(this);
			super.connect();
		}
	}
	
	@Override
	public void handleOscResponse(OSCMessage msg)
	{
		if (msg != null && msg.getMethod().equals(EOSUtil.SHOW_NAME_REPLY)
				&& msg.getArgType(0) == OSCUtil.OSC_STR_ARG_FMT_CHAR) {
			m_showName = msg.getString(0, m_showName);
		}
	}

	/**
	 * Get the EOS show name.
	 * @return The name of the show.
	 * @throws IOException  If an IO error occurs.
	 */
	public String getShowName() throws IOException
	{
		// The show name is one of the many messages
		// that EOS sends when the client connects.
		// When it arrives, the message handler saves it in m_showName.
		// So if it's not set, we first get the version, to force EOS
		// to send the initial messages.
		if (m_showName == null || m_showName.isBlank()) {
			getVersion();
		}
		return m_showName;
	}

	/**
	 * Test if the server is really an EOS controller.
	 * @return True iff the server is an EOS controller.
	 */
	public boolean isEos()
	{
		try {
			String version = getVersion();
			return version != null && !version.equals("");
		} catch (IOException e) {
			return false;
		}
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
	 * @return A TreeMap with all cue lists. The key is the cuelist number.
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
	 * Get all cues in a cuelist.
	 * @return A TreeMap with all cues. The key is the cue number.
	 * @throws IOException If an IO error occurs.
	 */
	public TreeMap<EOSCueNumber,EOSCueInfo> getCues(int cuelist) throws IOException
	{
		if (!isConnected()) {
			connect();
		}
		int nCues = getCueCount(cuelist);
		TreeMap<EOSCueNumber,EOSCueInfo> cues = new TreeMap<>();
		boolean nextIsAutoCue = false;
		for (int iCue = 0; iCue < nCues; iCue++) {
			EOSCueInfo cue = new EOSCueInfo(cuelist, iCue, nextIsAutoCue, this, m_timeoutMS);
			if (cue.isValid()) {
				cues.put(cue.getCueNumber(), cue);
				nextIsAutoCue = cue.getHangTimeMS() >= 0 || cue.getFollowTimeMS() >= 0;
				String warnings = cue.getWarnings();
				if (warnings != null) {
					System.out.println("*** WARNING: EOS cue " + cue.getCueNumber().toFullString()
							+ "[" + iCue + "]: " + warnings);
				}
			} else {
				System.out.println("*** WARNING: invalid cue index " + iCue + ": " + cue);
			}
			// XXX try {Thread.sleep(2000); } catch (Exception e) {}
			// XXX if (iCue >= 2) { break; } // XXXX
		}
		return cues;
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
			System.out.println("Show Name: " + queryEOS.getShowName());
			System.out.println("Version: " + queryEOS.getVersion());
			System.out.println("Cuelist count: " + queryEOS.getCuelistCount());
			TreeMap<Integer,EOSCuelistInfo> cuelists = queryEOS.getCuelists();
			System.out.println("Cuelists: " + cuelists);
			TreeMap<Integer, TreeMap<EOSCueNumber, EOSCueInfo>> allcues = new TreeMap<>();
			for (EOSCuelistInfo cuelist: cuelists.values()) {
				int cuelistNumber = cuelist.getCuelistNumber();
				System.out.println("Getting cues for cuelist " + cuelistNumber);
				allcues.put(cuelistNumber, queryEOS.getCues(cuelistNumber));
			}
			long endTS = System.currentTimeMillis();
		
			for (EOSCuelistInfo cuelist: cuelists.values()) {
				int cuelistNumber = cuelist.getCuelistNumber();
				TreeMap<EOSCueNumber, EOSCueInfo> cues = allcues.get(cuelistNumber);
				if (cues == null) {
					System.out.println("OOPS! no cues for cuelist " + cuelistNumber);
				}
				System.out.println();
				System.out.println("Cuelist " + cuelistNumber + ": ncues=" + cues.size());
				for (EOSCueInfo cue: cues.values()) {
					if (true) {
						System.out.println();
						System.out.println(" " + cue.toString());
					} else {
						System.out.println(" " + cue.toShortString());
					}
				}
			}
			System.out.println(String.format("Elapsed time: %.3f sec.",
						(endTS - startTS)/1000.0));
		} catch (IllegalArgumentException e) {
			System.err.println(e);
		}
	}
}
