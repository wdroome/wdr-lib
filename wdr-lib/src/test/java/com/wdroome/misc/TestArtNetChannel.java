package com.wdroome.misc;

import java.io.IOException;

import java.net.InetSocketAddress;

import com.wdroome.artnet.ArtNetConst;
import com.wdroome.artnet.msgs.ArtNetPoll;
import com.wdroome.artnet.ArtNetChannel;

import com.wdroome.util.inet.InetInterface;

/**
 * @author wdr
 */
public class TestArtNetChannel
{

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException
	{
		ArtNetChannel defChan = new ArtNetChannel(new ArtNetChannel.MsgPrinter(),
												new int[] {ArtNetConst.ARTNET_PORT});
		
		ArtNetChannel altChan1 = new ArtNetChannel(new ArtNetChannel.MsgPrinter(),
												new int[] {8001});
		ArtNetChannel altChan2 = new ArtNetChannel(new ArtNetChannel.MsgPrinter(),
												new int[] {8002});
		
		for (int i = 0; i < 2; i++) {
			System.out.println();
			defChan.send(new ArtNetPoll(),
					new InetSocketAddress("127.0.0.1", 8001));
			defChan.send(new ArtNetPoll(),
					new InetSocketAddress("127.0.0.1", 8002));
			for (InetInterface iface: InetInterface.getBcastInterfaces()) {
				defChan.send(new ArtNetPoll(),
						new InetSocketAddress(iface.m_broadcast, 8001));
				defChan.send(new ArtNetPoll(),
						new InetSocketAddress(iface.m_broadcast, 8002));
			}
			try {Thread.sleep(1000);} catch (Exception e) {}
		}
		
		defChan.shutdown();
		altChan1.shutdown();
		altChan2.shutdown();
	}
}
