package com.wdroome.artnet.util;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import com.wdroome.util.MiscUtil;
import com.wdroome.util.inet.InetInterface;

import com.wdroome.artnet.ArtNetConst;
import com.wdroome.artnet.ArtNetMsg;
import com.wdroome.artnet.ArtNetPoll;
import com.wdroome.artnet.ArtNetChannel;
import com.wdroome.artnet.ArtNetDmx;

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
	
	private class Chaser extends Thread
	{
		private int m_chaseRate = 40;
		
		private boolean m_running;
		private InetSocketAddress m_nodeAddr;
		private int m_fromChan;
		private int m_toChan;
		private int m_level;
		private long m_periodMS;
		
		private Chaser(InetSocketAddress addr, int from, int to, int lvl, long periodMS)
		{
			m_nodeAddr = addr;
			m_fromChan = from;
			m_toChan = to;
			m_level = lvl;
			m_periodMS = periodMS;
			
			if (m_fromChan < 1) {
				m_fromChan = 1;
			} else if (m_fromChan > 512) {
				m_fromChan = 512;
			}
			if (m_toChan < 1) {
				m_toChan = 1;
			} else if (m_toChan > 512) {
				m_toChan = 512;
			}
			if (m_fromChan > m_toChan) {
				int x = m_fromChan;
				m_fromChan = m_toChan;
				m_toChan = x;
			}
			if (m_level < 0) {
				m_level = 0;
			} else if (m_level > 255) {
				m_level = 255;
			}
			if (periodMS < 250) {
				periodMS = 250;
			}
			
			m_running = true;
			start();
		}
		
		@Override
		public void run()
		{
			int chan = m_fromChan;
			int lvl = 0;
			long waitMS = (long)(1000.0/m_chaseRate);
			byte[] dmxLvls = new byte[m_toChan];
			for (int i = 0; i < dmxLvls.length; i++) {
				dmxLvls[i] = 0;
			}
			
			boolean up = true;
			long m_startTS = System.currentTimeMillis();
			long m_endTS = m_startTS + m_periodMS/2;
			
			ArtNetDmx msg = new ArtNetDmx();
			msg.m_data = dmxLvls;
			msg.m_dataLen = dmxLvls.length;
			boolean mustSend = true;
			
			while (m_running) {
				long curTS = System.currentTimeMillis();
				long msFromStart = curTS - m_startTS;
				lvl = (int) ((double)m_level * msFromStart / (m_periodMS/2));
				if (!up) {
					lvl = m_level - lvl;
				}
				if (lvl >= m_level) {
					lvl = m_level;
				} else if (lvl <= 0) {
					lvl = 0;
				}
				int cur = dmxLvls[chan-1] & 0xff;
				if (cur != lvl || mustSend) {
					msg.incrSeqn();
					dmxLvls[chan-1] = (byte)lvl;
					try {
						// System.out.println("Send: " + chan + " " + cur + " " + lvl + " " + msg);
						send(msg, m_nodeAddr);
					} catch (Exception e) {
						e.printStackTrace();
						m_running = false;
					}
					mustSend = false;
				}
				if (curTS >= m_endTS) {
					m_startTS = curTS;
					m_endTS = m_startTS + m_periodMS/2;
					mustSend = true;
					up = !up;
					if (up) {
						chan++;
						if (chan > m_toChan) {
							chan = m_fromChan;
						}
					}
				}
				try {Thread.sleep(waitMS);} catch (Exception e) {}
			}
			System.out.println("Chaser stopped");
		}
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
		Chaser chaser = null;
				
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
			} else if (cmd.equals("chase")) {
				String chaseUsage = "Usage: chase addr from-chan [to-chan max-level sec]";
				if (chaser != null) {
					System.out.println("You must stop previous chaser first");
					continue;
				}
				if (!(cmdArgs.length >= 3)) {
					System.out.println(chaseUsage);
					continue;
				}
				InetSocketAddress addr = getAddr(cmdArgs[1]);
				if (addr == null) {
					continue;
				}
				try {
					int from = Integer.parseInt(cmdArgs[2]);
					int to = from;
					int level = 255;
					double sec = 1.0;
					if (cmdArgs.length >= 4) {
						to = Integer.parseInt(cmdArgs[3]);
					}
					if (cmdArgs.length >= 5) {
						level = Integer.parseInt(cmdArgs[4]);
					}
					if (cmdArgs.length >= 6) {
						sec = Double.parseDouble(cmdArgs[5]);
					}
					chaser = monitor.new Chaser(addr, from, to, level, (long)(1000 * sec));
				} catch (NumberFormatException e) {
					System.out.println(chaseUsage);
					continue;
				}
			} else if (cmd.equals("stop")) {
				if (chaser != null) {
					chaser.m_running = false;
					chaser = null;
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
	
	private static InetSocketAddress getAddr(String s)
	{
		try {
			int port;
			InetAddress addr;
			int iColon = s.lastIndexOf(":");
			if (iColon >= 0) {
				port = Integer.parseInt(s.substring(iColon+1));
				addr = InetAddress.getByName(s.substring(0, iColon));
			} else {
				port = ArtNetConst.ARTNET_PORT;
				addr = InetAddress.getByName(s);
			}
			return new InetSocketAddress(addr, port);
		} catch (Exception e) {
			System.out.println("Invalid addr:port \"" + s + "\"");
			return null;
		}
	}
}
