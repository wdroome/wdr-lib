package com.wdroome.misc;

import java.io.IOException;
import java.net.*;
import java.util.*;

import com.wdroome.artnet.*;
import com.wdroome.util.ByteAOL;
import com.wdroome.util.HexDump;
import com.wdroome.util.inet.CIDRAddress;

/**
 * @author wdr
 */
public class TestArtNetSender
{	
	private static class Rcvr extends Thread
	{
		private List<ArtNetPollReply> m_nodes = new ArrayList<ArtNetPollReply>();
		
		public void run()
		{
			DatagramSocket sock;
			try {
				sock = new DatagramSocket(ArtNetConst.ARTNET_PORT);
			} catch (SocketException e1) {
				e1.printStackTrace();
				return;
			}
			try {
				byte[] buff = new byte[1024];
				DatagramPacket pack = new DatagramPacket(buff, buff.length);
				System.out.println("Rcvr: Listening");
				while (true) {
					sock.receive(pack);
					System.out.println("Rcv: rmt: " + pack.getSocketAddress()
								+ " lcl: " + sock.getLocalSocketAddress()
								+ " len: " + pack.getLength());
					ArtNetMsg m = ArtNetMsg.make(buff, 0, pack.getLength(), null);
					if (m != null) {
						System.out.println(m.toString().replace(",", "\n  "));
						if (m instanceof ArtNetPollReply) {
							synchronized (m_nodes) {
								m_nodes.add((ArtNetPollReply)m);
							}
						}
					} else {
						HexDump dump = new HexDump();
						dump.dump(buff, 0, pack.getLength());
						// String x = new ByteAOL(buff, 0, pack.getLength()).toHex();
						// System.out.println(x);
					}
				} 
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					sock.close();
				} catch (Exception e2) {
				}
			}
		}
		
		public List<ArtNetPollReply> getNodes() { return m_nodes; }
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException
	{
		Rcvr rcvr = new Rcvr();
		rcvr.start();
		try { Thread.sleep(1000); } catch (Exception e) {}
		
		Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
		List<InetAddress> addrs = new ArrayList<InetAddress>();
		List<InetAddress> bcasts = new ArrayList<InetAddress>();
		List<CIDRAddress> cidrs = new ArrayList<CIDRAddress>();
		while (nis.hasMoreElements()) {
			NetworkInterface ni = nis.nextElement();
			System.out.println("NI: " + ni.toString());
			for (InterfaceAddress ia: ni.getInterfaceAddresses()) {
				System.out.println("  IntAddr: " + ia.getAddress()
								+ " bcst: " + ia.getBroadcast());
				addrs.add(ia.getAddress());
				bcasts.add(ia.getBroadcast());
				cidrs.add(new CIDRAddress(ia.getAddress().getAddress(),
								ia.getNetworkPrefixLength()));
			}
		}
		
		DatagramSocket sock = null;
		try {
			ArtNetPoll poll = new ArtNetPoll();
			byte[] buffPoll = new byte[1024];
			int len = poll.putData(buffPoll, 0);
			DatagramPacket packPoll = new DatagramPacket(buffPoll, len);
			ArtNetDmx dmx = new ArtNetDmx();
			dmx.m_sequence = 1;
			dmx.m_data = new byte[] {
					(byte)(0x00), (byte)(0x20), (byte)(0x40), (byte)(0x60),
					(byte)(0x80), (byte)(0xa0), (byte)(0xc0), (byte)(0xe0),
					(byte)(0xff),
			};
			dmx.m_dataLen = dmx.m_data.length;
			byte[] buffDmx = new byte[1024];
			len = dmx.putData(buffDmx, 0);
			DatagramPacket packDmx = new DatagramPacket(buffDmx, len);

			for (int i = 0; i < addrs.size(); i++) {
				InetAddress addr = addrs.get(i);
				InetAddress bcast = bcasts.get(i);
				if (bcast != null) {
					sock = new DatagramSocket(ArtNetConst.ARTNET_PORT-1, addr);
					packPoll.setAddress(bcast);
					packPoll.setPort(ArtNetConst.ARTNET_PORT);
					System.out.println();
					System.out.println("Sending poll to " + bcast);
					sock.send(packPoll);
					sock.close();
					sock = null;
				}
			}

			try { Thread.sleep(1000); } catch (Exception e) {}
			List<ArtNetPollReply> nodes = rcvr.getNodes();
			for (ArtNetPollReply node: nodes) {
				if (node.m_ipAddr != null) {
					InetAddress myAddr = null;
					for (int i = 0; i < addrs.size(); i++) {
						CIDRAddress cidr = cidrs.get(i);
						if (cidr.contains(node.m_ipAddr)) {
							myAddr = addrs.get(i);
							break;
						}
					}
					if (myAddr != null) {
						sock = new DatagramSocket(ArtNetConst.ARTNET_PORT - 1, myAddr);
						System.out.println();
						System.out.println("Sending dmx to " + node.m_ipAddr + " via " + myAddr);
						packDmx.setAddress(node.m_ipAddr);
						packDmx.setPort(node.m_ipPort);
						sock.send(packDmx);
						sock.close();
						sock = null;
					}
				}
			}

		} finally {
			try {
				if (sock != null) {
					sock.close();
				}
			} catch (Exception e2) {
			}
		}
		try { Thread.sleep(1000); } catch (Exception e) {}
		System.exit(0);
	}

}
