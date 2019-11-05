package com.wdroome.artnet.util;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;

import com.wdroome.util.MiscUtil;
import com.wdroome.util.inet.InetInterface;

import com.wdroome.artnet.ArtNetConst;
import com.wdroome.artnet.ArtNetMsg;
import com.wdroome.artnet.ArtNetPollReply;
import com.wdroome.artnet.ArtNetChannel;

/**
 * @author wdr
 */
public class ArtNetMonitor extends ArtNetChannel.MsgPrinter
{
	private final ArtNetChannel m_chan;
	
	public ArtNetMonitor() throws IOException
	{
		this(null);
	}
	
	/**
	 * @throws IOException 
	 */
	public ArtNetMonitor(int[] ports) throws IOException
	{
		m_chan = new ArtNetChannel(this, ports);
	}
	
	/* (non-Javadoc)
	 * @see com.wdroome.artnet.ArtNetChannel.MsgPrinter#msgPrefix()
	 */
	@Override
	public String msgPrefix()
	{
		return "ArtNetMonitor ";
	}
	
	public void shutdown() { m_chan.shutdown(); }

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException
	{
		ArrayList<Integer> ports = new ArrayList<Integer>();
		for (String s: args) {
			ports.add(Integer.parseInt(s));
		}
		int[] arr = new int[ports.size()];
		int i = 0;
		for (int p: ports) {
			arr[i++] = p;
		}
		new ArtNetMonitor(arr);
	}
}
