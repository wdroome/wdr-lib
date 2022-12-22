package com.wdroome.artnet.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.wdroome.artnet.ArtNetChannel;

/**
 * A utility program that prints all Art-Net messages that it receives.
 * The command line arguments are ports to listen for Art-Net messages.
 * If no ports are specified, listen to the default Art-Net port.
 * Art-Net (TM) Designed by and Copyright Artistic Licence Holdings Ltd.
 * @author wdr
 */
public class ArtNetMonitor extends ArtNetChannel.MsgPrinter
{
	private final ArtNetChannel m_chan;
	
	public ArtNetMonitor() throws IOException
	{
		this((List<Integer>)null);
	}
	
	/**
	 * @throws IOException 
	 */
	public ArtNetMonitor(int[] ports) throws IOException
	{
		m_chan = new ArtNetChannel(this, ports);
	}
	
	/**
	 * @throws IOException 
	 */
	public ArtNetMonitor(List<Integer> ports) throws IOException
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
	 * Print all Art-Net messages on a set of ports.
	 * @param args The ports. If empty, listen on the default Art-Net port.
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException
	{
		ArrayList<Integer> ports = new ArrayList<Integer>();
		for (String s: args) {
			ports.add(Integer.parseInt(s));
		}
		new ArtNetMonitor(ports);
	}
}
