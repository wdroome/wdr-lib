package test.misc;

import java.io.IOException;
import java.net.*;
import java.util.*;

import com.wdroome.artnet.*;
import com.wdroome.util.ByteAOL;
import com.wdroome.util.HexDump;

/**
 * @author wdr
 */
public class TestArtNetSender
{
	private static class Rcvr extends Thread
	{
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
					ArtNetMsg m = ArtNetMsg.make(buff, 0, pack.getLength());
					if (m != null) {
						System.out.println(m.toString().replace(",", "\n  "));
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
	}
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException
	{
		if (true) {
			Rcvr rcvr = new Rcvr();
			rcvr.start();
			try { Thread.sleep(1000); } catch (Exception e) {}
		}
		
		Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
		List<InetAddress> addrs = new ArrayList<InetAddress>();
		List<InetAddress> bcasts = new ArrayList<InetAddress>();
		while (nis.hasMoreElements()) {
			NetworkInterface ni = nis.nextElement();
			System.out.println("NI: " + ni.toString());
			for (InterfaceAddress ia: ni.getInterfaceAddresses()) {
				System.out.println("  IntAddr: " + ia.getAddress() + " bcst: " + ia.getBroadcast());
				addrs.add(ia.getAddress());
				bcasts.add(ia.getBroadcast());
			}
		}
		
		DatagramSocket sock = null;
		try {
			ArtNetPoll poll = new ArtNetPoll();
			byte[] buff = new byte[1024];
			int len = poll.putData(buff, 0);
			DatagramPacket pack = new DatagramPacket(buff, len);
			for (int i = 0; i < addrs.size(); i++) {
				InetAddress addr = addrs.get(i);
				InetAddress bcast = bcasts.get(i);
				if (bcast != null) {
					sock = new DatagramSocket(ArtNetConst.ARTNET_PORT-1, addr);
					pack.setAddress(bcast);
					pack.setPort(ArtNetConst.ARTNET_PORT);
					System.out.println("Sending to " + bcast);
					sock.send(pack);
					sock.close();
					sock = null;
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
	}

}
