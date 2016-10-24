package com.wdroome.artnet;

import java.util.ArrayList;
import java.util.Set;
import java.util.Stack;
import java.util.ArrayDeque;

import java.io.IOException;
import java.io.PrintStream;

import java.net.StandardProtocolFamily;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.net.Inet4Address;

import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;

import com.wdroome.util.HexDump;
import com.wdroome.util.inet.CIDRAddress;
import com.wdroome.util.inet.InetInterface;

/**
 * @author wdr
 */
public class ArtNetChannel extends Thread
{
	/**
	 * Methods called when an Art-Net message arrives.
	 * @author wdr
	 */
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
		 * @param len The length of the message.
		 * @param sender The remote address.
		 * @param receiver The local address.
		 */
		public void msgArrived(ArtNetOpcode opcode, byte[] buff, int len,
								SocketAddress sender, SocketAddress receiver);
		
		/**
		 * Called when a non-Art-Net message has arrived.
		 * @param msg The message.
		 * @param len The length of the message.
		 * @param sender The remote address.
		 * @param receiver The local address.
		 */
		public void msgArrived(byte[] msg, int len,
								SocketAddress sender, SocketAddress receiver); 
	}
	
	private static class SendBuffer
	{
		private final SocketAddress m_target;
		private final ByteBuffer m_buff;
		
		private SendBuffer(SocketAddress target, ByteBuffer buff)
		{
			m_target = target;
			m_buff = buff;
		}
	}
	
	private static class ChannelInfo
	{
		private final DatagramChannel m_channel;
		private final CIDRAddress m_cidr;
		
		// Must synch on m_sendBuffs when accessing.
		private ArrayDeque<SendBuffer> m_sendBuffs = new ArrayDeque<SendBuffer>();
		
		private ChannelInfo(DatagramChannel channel, CIDRAddress cidr)
		{
			m_channel = channel;
			m_cidr = cidr;
		}
	}
	
	private static final int MAX_SEND_BUFFS = 10;
	
	private final ChannelInfo[] m_channels;
	private final Selector m_selector;
	private final Receiver m_receiver;
	
	// Pre-allocated stack of free send buffers.
	// Synch on m_freeSendBuffs when accessing.
	private final Stack<ByteBuffer> m_freeSendBuffs;

	// True if the thread is running.
	private boolean m_running = true;

	/**
	 * @throws IOException 
	 */
	public ArtNetChannel(Receiver receiver, int[] ports) throws IOException
	{
		m_selector = Selector.open();
		m_receiver = receiver;
		if (ports == null | ports.length == 0) {
			ports = new int[] {ArtNetConst.ARTNET_PORT};
		}
		ArrayList<ChannelInfo> channels = new ArrayList<ChannelInfo>();
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
						channels.add(chanInfo);
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
		m_channels = channels.toArray(new ChannelInfo[channels.size()]);
		
		m_freeSendBuffs = new Stack<ByteBuffer>();
		for (int i = 0; i < MAX_SEND_BUFFS; i++) {
			m_freeSendBuffs.push(ByteBuffer.allocate(ArtNetConst.MAX_MSG_LEN));
		}
		
		start();
	}
	
	@Override
	public void run()
	{
		ByteBuffer rcvBuff = ByteBuffer.allocate(ArtNetConst.MAX_MSG_LEN);
		byte[] msgBuff = new byte[ArtNetConst.MAX_MSG_LEN];
		try {
			while (m_running) {
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
							SocketAddress sender;
							while ((sender = chanInfo.m_channel.receive(rcvBuff)) != null) {
								int msgLen = rcvBuff.remaining();
								rcvBuff.get(msgBuff, 0, msgLen);
								rcvBuff.clear();
								ArtNetMsg msg = ArtNetMsg.make(msgBuff, 0, msgLen);
								if (msg != null) {
									if (m_receiver != null) {
										m_receiver.msgArrived(msg, sender, chanInfo.m_channel.getLocalAddress());
									}
								} else {
									ArtNetOpcode opcode = ArtNetMsg.getOpcode(msgBuff, 0, msgLen);
									switch (opcode) {
									case Invalid:
										if (m_receiver != null) {
											m_receiver.msgArrived(msgBuff, msgLen,
													sender, chanInfo.m_channel.getLocalAddress());
										}
										break;
									default:
										if (m_receiver != null) {
											m_receiver.msgArrived(opcode, msgBuff, msgLen,
													sender, chanInfo.m_channel.getLocalAddress());
										}
										break;
									}
								}
							}
						}
						if ((ops & SelectionKey.OP_WRITE) != 0) {
							synchronized (chanInfo.m_sendBuffs) {
								while (!chanInfo.m_sendBuffs.isEmpty()) {
									SendBuffer sendBuff = chanInfo.m_sendBuffs.removeFirst();
									int nsent = chanInfo.m_channel.send(sendBuff.m_buff, sendBuff.m_target);
									if (nsent == 0) {
										System.err.println("ArtNetChannel.run(); Could not send msg");
									}
									releaseSendBuffer(sendBuff.m_buff);
								}
								chanInfo.m_channel.register(m_selector, SelectionKey.OP_READ, chanInfo);
							}
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
	
	public boolean send(ArtNetMsg msg, InetSocketAddress target) throws IOException
	{
		ChannelInfo chanInfo = getChannelInfo(target);
		if (chanInfo == null) {
			System.err.println("ArtNetChannel.send(): No channel for " + target);	// XXX
			return false;
		}
		byte[] msgBuff = new byte[ArtNetConst.MAX_MSG_LEN];
		int msgLen = msg.putData(msgBuff, 0);
		ByteBuffer sendBuff = getSendBuffer();
		if (sendBuff == null) {
			// OOPS -- out of buffers!!
			System.err.println("ArtNetChannel: Out of send buffers for " + msg.m_opcode); // XXX
			return false;
		}
		sendBuff.put(msgBuff, 0, msgLen);
		int nsent = chanInfo.m_channel.send(sendBuff, target);
		if (nsent != 0) {
			return true;
		} else {
			synchronized (chanInfo.m_sendBuffs) {
				chanInfo.m_sendBuffs.add(new SendBuffer(target, sendBuff));
				chanInfo.m_channel.register(m_selector, SelectionKey.OP_READ + SelectionKey.OP_WRITE, chanInfo);
				m_selector.wakeup();
			}
			return true;
		}
	}
	
	private ByteBuffer getSendBuffer()
	{
		synchronized (m_freeSendBuffs) {
			if (!m_freeSendBuffs.isEmpty()) {
				return m_freeSendBuffs.pop();
			} else {
				return null;
			}
		}
	}
	
	private void releaseSendBuffer(ByteBuffer buff)
	{
		synchronized (m_freeSendBuffs) {
			buff.clear();
			m_freeSendBuffs.push(buff);
		}
	}
	
	private ChannelInfo getChannelInfo(InetSocketAddress target)
	{
		for (ChannelInfo ci: m_channels) {
			if (ci.m_cidr.contains(target.getAddress())) {
				return ci;
			}
		}
		return null;
	}
	
	/**
	 * An Art-Net message receiver which prints each incoming message.
	 */
	public static class MsgPrinter implements Receiver
	{
		private final PrintStream m_out;
		private final HexDump m_dump;
		
		/** Create a new printer using System.out. */
		public MsgPrinter()
		{
			this(null);
		}
		
		/**
		 * Create a new printer.
		 * @param out The output stream. If null, use System.out.
		 */
		public MsgPrinter(PrintStream out)
		{
			m_out = out != null ? out : System.out;
			m_dump = new HexDump();
			m_dump.setOutput(m_out);
		}
		
		@Override
		public void msgArrived(ArtNetMsg msg, SocketAddress sender, SocketAddress receiver)
		{
			m_out.println("Rcv " + msg.m_opcode + ": rmt: " + sender + " lcl: " + receiver);
			m_out.println(msg.toString().replace(",", "\n  "));
		}

		@Override
		public void msgArrived(ArtNetOpcode opcode, byte[] buff, int len,
						SocketAddress sender, SocketAddress receiver)
		{
			m_out.println("Rcv " + opcode + ": rmt: " + sender + " lcl: " + receiver + " len: " + len);
			m_dump.dump(buff, 0, len);
		}

		@Override
		public void msgArrived(byte[] msg, int len, SocketAddress sender,
						SocketAddress receiver)
		{
			m_out.println("Rcv ???: rmt: " + sender + " lcl: " + receiver + " len: " + len);
			m_dump.dump(msg, 0, len);
		}
	}
}
