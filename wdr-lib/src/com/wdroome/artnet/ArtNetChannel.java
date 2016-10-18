package com.wdroome.artnet;

import java.util.List;
import java.util.ArrayList;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

/**
 * @author wdr
 */
public class ArtNetChannel
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
		 * @param msg The message.
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
	
	private final List<DatagramChannel> m_channel = new ArrayList<DatagramChannel>();

	/**
	 * 
	 */
	public ArtNetChannel()
	{
		// XXX -- needed?
		// TODO Auto-generated constructor stub
	}
	
	public void bind(int port)
	{
		// XXX
	}
	
	public void send(ArtNetMsg m, SocketAddress target)
	{
		setupDefaultPort();
		// XXX: create buffer with bytes
		// XXX: send
	}
	
	/**
	 * If no ports have been bound, use the default Art-Net port.
	 */
	private void setupDefaultPort()
	{
		if (m_channel.isEmpty()) {
			bind(ArtNetConst.ARTNET_PORT);
		}
	}
}
