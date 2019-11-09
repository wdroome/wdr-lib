package com.wdroome.misc;

import java.io.IOException;
import java.net.*;

import com.wdroome.artnet.*;

/**
 * @author wdr
 */
public class TestArtNetRcvr
{
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException
	{
		DatagramSocket sock = new DatagramSocket(ArtNetConst.ARTNET_PORT);
		try {
			System.out.println("Init addr: " + sock.getLocalAddress());
			byte[] buff = new byte[1024];
			DatagramPacket pack = new DatagramPacket(buff, buff.length);
			while (true) {
				sock.receive(pack);
				System.out.println("Rcv: rmt: " + pack.getSocketAddress()
							+ " lcl: " + sock.getLocalSocketAddress()
							+ " len: " + pack.getLength());
				ArtNetMsg m = ArtNetMsg.make(buff, 0, pack.getLength());
				if (m != null) {
					System.out.println(m);
				}
			} 
		} finally {
			sock.close();
		}
	}

}
