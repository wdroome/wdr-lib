package com.wdroome.artnet;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import java.io.IOException;

import java.net.StandardProtocolFamily;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.net.Inet4Address;

import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;

import com.wdroome.util.inet.CIDRAddress;
import com.wdroome.util.inet.InetInterface;

/**
 * @author wdr
 */
public class ArtNetChannel extends Thread
{
	public interface Receiver
	{
		/**
		 * Called when a new Art-Net message has arrived.
		 * @param msg The message.
		 * @param sender The remote address.
		 * @param receiver The local address.
		 */
		public void msgArrived(ArtNetMsg msg,
								SocketAddress sender, SocketAddress receiver);
		
		/**
		 * Called when a new Art-Net message has arrived,
		 * but we do not have a message type for the opcode.
		 * @param opcode The art-net opcode.
		 * @param buff The raw bytes of the message.
		 * @param sender The remote address.
		 * @param receiver The local address.
		 */
		public void msgArrived(ArtNetOpcode opcode, byte[] buff,
								SocketAddress sender, SocketAddress receiver);
		
		/**
		 * Called when a non-Art-Net message has arrived.
		 * @param msg The message.
		 * @param sender The remote address.
		 * @param receiver The local address.
		 */
		public void msgArrived(byte[] msg,
								SocketAddress sender, SocketAddress receiver); 
	}
	
	private static class ChannelInfo
	{
		private final DatagramChannel m_channel;
		private final CIDRAddress m_cidr;
		
		private ChannelInfo(DatagramChannel channel, CIDRAddress cidr)
		{
			m_channel = channel;
			m_cidr = cidr;
		}
	}
	
	private final List<ChannelInfo> m_channels = new ArrayList<ChannelInfo>();
	private final Selector m_selector;
	private final Receiver m_receiver;
	private boolean running = true;

	/**
	 * @throws IOException 
	 */
	public ArtNetChannel(Receiver rcvr, int[] ports) throws IOException
	{
		m_selector = Selector.open();
		m_receiver = rcvr;
		if (ports == null | ports.length == 0) {
			ports = new int[] {ArtNetConst.ARTNET_PORT};
		}
		for (int port: ports) {
			for (InetInterface iface: InetInterface.getAllInterfaces()) {
				DatagramChannel chan = null;
				try {
					if (iface.m_address instanceof Inet4Address) {
						chan = DatagramChannel.open(StandardProtocolFamily.INET);
						chan.bind(new InetSocketAddress(iface.m_address, port));
						chan.configureBlocking(false);
						ChannelInfo chanInfo = new ChannelInfo(chan, iface.m_cidr);
						chan.register(m_selector, SelectionKey.OP_READ, chanInfo);
						m_channels.add(chanInfo);
					}
				} catch (IOException e) {
					System.err.println("ArtNetChannel(); Could not bind to "
								+ iface.m_address.getHostAddress() + ":" + port
								+ " " + e);
					if (chan != null) {
						try { chan.close(); } catch (Exception e2) {}
					}
				}
			}
		}
		start();
	}
	
	@Override
	public void run()
	{
		try {
			while (running) {
				int nsel = m_selector.select(1000);
				if (nsel > 0) {
					Set<SelectionKey> keySet = m_selector.selectedKeys();
					for (SelectionKey key: keySet) {
						keySet.remove(key);
						int ops = key.readyOps();
						Object att = key.attachment();
						if (!(att instanceof ChannelInfo)) {
							continue;
						}
						ChannelInfo chanInfo = (ChannelInfo)att;
						if ((ops & SelectionKey.OP_READ) != 0) {
							/*
							 * read into buffer.
							 * create art-net msg
							 * call m_receiver as appropriate.
							 */
						}
						if ((ops & SelectionKey.OP_WRITE) != 0) {
							// XXX: Write as needed.
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			for (ChannelInfo ci: m_channels) {
				try {ci.m_channel.close();} catch (Exception e2) {}
			}
		}
	}
	
	public void send(ArtNetMsg m, SocketAddress target)
	{
		// XXX: create buffer with bytes
		// XXX: send
	}
}
