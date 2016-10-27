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
import com.wdroome.artnet.ArtNetPoll;
import com.wdroome.artnet.ArtNetChannel;

/**
 * @author wdr
 */
public class ArtNetSender extends ArtNetChannel.MsgPrinter
{
	private final ArtNetChannel m_chan;
	
	public ArtNetSender() throws IOException
	{
		this(null);
	}
	
	/**
	 * @throws IOException 
	 */
	public ArtNetSender(int[] ports) throws IOException
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

	public boolean send(ArtNetMsg msg, InetSocketAddress target) throws IOException
	{
		return m_chan.send(msg, target);
	}

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
		int[] portArr = new int[ports.size()];
		int i = 0;
		for (int p: ports) {
			portArr[i++] = p;
		}
		if (portArr.length == 0) {
			portArr = new int[] {ArtNetConst.ARTNET_PORT};
		}
		
		ArtNetSender monitor = new ArtNetSender(portArr);
		
		while (true) {
			System.out.print("* ");
			String line = MiscUtil.readLine(System.in);
			if (line == null) {
				break;
			}
			String[] cmdArgs = line.split("[ \t]+");
			if (cmdArgs == null || cmdArgs.length == 0) {
				continue;
			}
			String cmd = cmdArgs[0].toLowerCase();
			
			if (cmd.equals("poll") || cmd.equals("p")) {
				ArrayList<InetSocketAddress> sendAddrs = new ArrayList<InetSocketAddress>();
				if (cmdArgs.length == 1) {
					for (InetInterface iface: InetInterface.getBcastInterfaces()) {
						for (int port: portArr) {
							sendAddrs.add(new InetSocketAddress(iface.m_broadcast, port));
						}
					}
				} else {
					try {
						InetAddress addr = InetAddress.getByName(cmdArgs[1]);
						int port = ArtNetConst.ARTNET_PORT;
						if (cmdArgs.length >= 3) {
							port = Integer.parseInt(cmdArgs[2]);
						}
						sendAddrs.add(new InetSocketAddress(addr, port));
					} catch (Exception e) {
						System.out.println("Usage: poll [address [port]]");
					}
				}
				for (InetSocketAddress addr: sendAddrs) {
					System.out.println("Poll " + addr);
					ArtNetPoll msg = new ArtNetPoll();
					try {
						if (!monitor.send(msg, addr)) {
							System.out.println("Send failed");
						}
					} catch (IOException e) {
						System.out.println(e);
					}
				}
			} else if (cmd.equals("quit") || cmd.equals("q")) {
				break;
			} else if (cmd.equals("")) {
				;
			} else {
				System.out.println("Unknown command");
			}
		}
		
		monitor.shutdown();
	}
}
