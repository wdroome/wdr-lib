package com.wdroome.artnet;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import com.wdroome.util.inet.InetInterface;

/**
 * Simple class to send Art-Net messages.
 * The caller is blocked until the message is sent.
 * This class sends messages from a random port, rather than the Art-Net port.
 * Art-Net (TM) Designed by and Copyright Artistic License Holdings Ltd.
 * @author wdr
 */
public class ArtNetSender implements AutoCloseable
{
	private DatagramSocket m_sendSocket = null;
	
	/**
	 * Create the sender.
	 * @throws SocketException If for some bizarre reason we cannot create a datagram socket.
	 */
	public ArtNetSender() throws SocketException
	{
		m_sendSocket = new DatagramSocket();
	}
	
	/**
	 * Send an Art-Net message.
	 * @param msg The message.
	 * @param target The target address and port.
	 * @throws IOException If the send fails.
	 */
	public void send(ArtNetMsg msg, InetSocketAddress target) throws IOException
	{
		if (m_sendSocket == null) {
			m_sendSocket = new DatagramSocket();
		}
		try {
			byte[] msgBuff = new byte[ArtNetConst.MAX_MSG_LEN];
			int msgLen = msg.putData(msgBuff, 0);		 
			DatagramPacket request = new DatagramPacket(msgBuff, msgLen, target);
			m_sendSocket.send(request);
		} catch (IOException e) {
			System.out.println("ArtNetSender.send " + "->" + target + " ERR:" + e);
			throw e;
		}			
	}
	
	/**
	 * Send an Art-Net message to the default Art-Net port.
	 * @param msg The message.
	 * @param target The target address.
	 * @throws IOException If the send fails.
	 */
	public void send(ArtNetMsg msg, InetAddress addr) throws IOException
	{
		send(msg, new InetSocketAddress(addr, ArtNetConst.ARTNET_PORT));
	}
	
	/**
	 * Close & disconnect the local datagram socket.
	 */
	@Override
	public void close()
	{
		try {
			m_sendSocket.close();
		} catch (Exception e) {
			m_sendSocket = null;
		}
	}
	
	/**
	 * For testing, send Art-Net poll message(s).
	 * @param args The inet addresses to send the polls to.
	 * 		If omitted, send to the broadcast address for all interfaces.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, InterruptedException
	{
		ArtNetPoll poll = new ArtNetPoll();
		List<InetAddress> dests = new ArrayList<>();
		for (String arg: args) {
			dests.add(InetAddress.getByName(arg));
		}
		if (dests.isEmpty()) {
			for (InetInterface iface: InetInterface.getBcastInterfaces()) {
				dests.add(iface.m_broadcast);
			}
		}
		try (ArtNetSender sender = new ArtNetSender()) {
			for (InetAddress addr: dests) {
				System.out.println("Sending poll to " + addr.getHostAddress());
				sender.send(poll, addr);
				Thread.sleep(2000);
			}
			System.out.println("Done.");
		}
	}
}
