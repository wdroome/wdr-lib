package com.wdroome.artnet;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Stack;
import java.util.ArrayDeque;
import java.util.Collection;

import java.util.concurrent.atomic.AtomicBoolean;

import java.io.IOException;
import java.io.PrintStream;

import java.net.StandardProtocolFamily;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.StandardSocketOptions;

import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;

import com.wdroome.util.ArrayToList;
import com.wdroome.util.HexDump;
import com.wdroome.util.inet.CIDRAddress;
import com.wdroome.util.inet.InetInterface;

/**
 * A channel for sending and receiving Art-Net messages.
 * @author wdr
 */
public class ArtNetChannel extends Thread
{
	/**
	 * Art-Net message handler.
	 * These methods are called when  Art-Net messages arrive.
	 * @author wdr
	 */
	public interface Receiver
	{
		/**
		 * Called when a new Art-Net message arrives.
		 * @param chan The ArtNetChannel which received the message.
		 * @param msg The message.
		 * @param sender The remote address.
		 * @param receiver The local address.
		 */
		public void msgArrived(ArtNetChannel chan, ArtNetMsg msg,
								InetSocketAddress sender, InetSocketAddress receiver);
		
		/**
		 * Called when a new Art-Net message arrives,
		 * but we do not have a message type for the opcode.
		 * @param chan The ArtNetChannel which received the message.
		 * @param opcode The art-net opcode.
		 * @param buff The raw bytes of the message.
		 * @param len The length of the message.
		 * @param sender The remote address.
		 * @param receiver The local address.
		 */
		public void msgArrived(ArtNetChannel chan, ArtNetOpcode opcode, byte[] buff, int len,
							InetSocketAddress sender, InetSocketAddress receiver);
		
		/**
		 * Called when a non-Art-Net message arrives.
		 * @param chan The ArtNetChannel which received the message.
		 * @param msg The message.
		 * @param len The length of the message.
		 * @param sender The remote address.
		 * @param receiver The local address.
		 */
		public void msgArrived(ArtNetChannel chan, byte[] msg, int len,
							InetSocketAddress sender, InetSocketAddress receiver); 
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
		private final int m_port;
		
		// Must synch on m_sendBuffs when accessing.
		private ArrayDeque<SendBuffer> m_sendBuffs = new ArrayDeque<SendBuffer>();
		
		private ChannelInfo(DatagramChannel channel, int port)
		{
			m_channel = channel;
			m_port = port;
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
	private final AtomicBoolean m_running = new AtomicBoolean(true);

	/**
	 * Create a new channel for sending and receiving Art-Net messages.
	 * @param receiver
	 * 		Methods of this class will be called when messages arrive.
	 * @param ports
	 * 		The ports to listen to. If null or 0-length, listen to
	 * 		the default Art-Net port.
	 * @throws IOException
	 * 		As thrown by Selector.open().
	 */
	public ArtNetChannel(Receiver receiver, Collection<Integer> ports) throws IOException
	{
		m_selector = Selector.open();
		m_receiver = receiver;
		if (ports == null || ports.isEmpty()) {
			ports = ArrayToList.toList(new int[] {ArtNetConst.ARTNET_PORT});
		}
		ArrayList<ChannelInfo> channels = new ArrayList<ChannelInfo>();
		for (InetSocketAddress addr: getLocalInetAddrs(ports)) {
			DatagramChannel chan = null;
			try {
				chan = DatagramChannel.open(StandardProtocolFamily.INET);
				chan.bind(addr);
				chan.configureBlocking(false);
				chan.setOption(StandardSocketOptions.SO_BROADCAST, true);
				ChannelInfo chanInfo = new ChannelInfo(chan, addr.getPort());
				channels.add(chanInfo);
				// System.out.println("XXX: ArtNetChannel: " + port);
			} catch (IOException e) {
				System.err.println("ArtNetChannel(); Could not bind to "
						+ addr.getPort() + " " + e);
				if (chan != null) {
					try {
						chan.close();
					} catch (Exception e2) {
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
	
	/**
	 * Create a new channel for sending and receiving Art-Net messages.
	 * @param receiver
	 * 		Methods of this class will be called when messages arrive.
	 * @param ports
	 * 		The ports to listen to. If null or 0-length, listen to
	 * 		the default Art-Net port.
	 * @throws IOException
	 * 		As thrown by Selector.open().
	 */
	public ArtNetChannel(Receiver receiver, int[] ports) throws IOException
	{
		this(receiver, ArrayToList.toList(ports));
	}
	
	/**
	 * Listen for incoming messages, and send messages if sending is blocked.
	 */
	@Override
	public void run()
	{
		ByteBuffer rcvBuff = ByteBuffer.allocate(ArtNetConst.MAX_MSG_LEN);
		byte[] msgBuff = new byte[ArtNetConst.MAX_MSG_LEN];
		try {
			while (m_running.get()) {
				ArrayList<SelectionKey> removeKeys = new ArrayList<SelectionKey>();
				for (ChannelInfo ci: m_channels) {
					int key = SelectionKey.OP_READ;
					synchronized (ci.m_sendBuffs) {
						if (!ci.m_sendBuffs.isEmpty()) {
							key |= SelectionKey.OP_WRITE;
						}
					}
					ci.m_channel.register(m_selector, key, ci);
				}
				int nsel = m_selector.select(1000);
				if (nsel > 0) {
					removeKeys.clear();
					Set<SelectionKey> keySet = m_selector.selectedKeys();
					for (SelectionKey key: keySet) {
						removeKeys.add(key);
						int ops = key.readyOps();
						Object att = key.attachment();
						if (!(att instanceof ChannelInfo)) {
							continue;
						}
						ChannelInfo chanInfo = (ChannelInfo)att;
						if ((ops & SelectionKey.OP_READ) != 0) {
							SocketAddress xsender;
							SocketAddress xreceiver;
							while ((xsender = chanInfo.m_channel.receive(rcvBuff)) != null) {
								rcvBuff.flip();
								int msgLen = rcvBuff.remaining();
								rcvBuff.get(msgBuff, 0, msgLen);
								rcvBuff.clear();
								ArtNetMsg msg = ArtNetMsg.make(msgBuff, 0, msgLen);
								xreceiver = chanInfo.m_channel.getLocalAddress();
								if (!(xsender instanceof InetSocketAddress && xreceiver instanceof InetSocketAddress)) {
									// Ignore non-ipv4 messages.
									continue;
								}
								InetSocketAddress sender = (InetSocketAddress)xsender;
								InetSocketAddress receiver = (InetSocketAddress)xreceiver;
								if (msg != null) {
									if (m_receiver != null) {
										m_receiver.msgArrived(this, msg, sender, receiver);
									}
								} else {
									ArtNetOpcode opcode = ArtNetMsg.getOpcode(msgBuff, 0, msgLen);
									switch (opcode) {
									case Invalid:
										if (m_receiver != null) {
											m_receiver.msgArrived(this, msgBuff, msgLen, sender, receiver);
										}
										break;
									default:
										if (m_receiver != null) {
											m_receiver.msgArrived(this, opcode, msgBuff, msgLen, sender, receiver);
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
									try {
										int nsent = chanInfo.m_channel.send(sendBuff.m_buff, sendBuff.m_target);
										if (nsent == 0) {
											chanInfo.m_sendBuffs.addFirst(sendBuff);
											break;
										}
									} catch (IOException e) {
										System.err.println("ArtNetChannel.run(): send err: " + e);
									}
									releaseSendBuffer(sendBuff.m_buff);
								}
							}
						}
					}
					for (SelectionKey key: removeKeys) {
						keySet.remove(key);
					}
				}
			}
		} catch (IOException e) {
			if (m_running.get()) {
				e.printStackTrace();
			}
		} finally {
			for (ChannelInfo ci: m_channels) {
				try {ci.m_channel.close();} catch (Exception e2) {}
			}
		}
	}
	
	/**
	 * Stop the listener thread.
	 */
	public void shutdown()
	{
		m_running.set(false);
		m_selector.wakeup();
		for (ChannelInfo ci: m_channels) {
			try {ci.m_channel.close();} catch (Exception e2) {}
		}
	}
	
	/**
	 * Send an Art-Net message.
	 * @param msg The message.
	 * @param target The destination.
	 * @return True iff the message was sent (or is queued to send).
	 * 		Only returns false if we cannot find a network interface
	 * 		to use to send the message.
	 * @throws IOException
	 * 		As thrown by Datagram.send();
	 */
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
		sendBuff.flip();
		if (false) {
			System.out.println("XXX: ArtNetChannel.send " + chanInfo.m_channel.getLocalAddress() + " -> " + target);
			System.out.println("XXX: " + sendBuff.toString());
			new HexDump().dump(msgBuff, 0, msgLen);
		}
		int nsent = chanInfo.m_channel.send(sendBuff, target);
		if (nsent != 0) {
			releaseSendBuffer(sendBuff);
		} else {
			// System.out.println("ArtNetChannel.send(): blocked, using thread.");
			synchronized (chanInfo.m_sendBuffs) {
				chanInfo.m_sendBuffs.add(new SendBuffer(target, sendBuff));
				m_selector.wakeup();
			}
		}
		return true;
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
		int targetPort = target.getPort();
		for (ChannelInfo ci: m_channels) {
			if (ci.m_port == targetPort) {
				return ci;
			}
		}
		if (m_channels.length > 0) {
			return m_channels[0];
		} else {
			return null;
		}
	}
	
	/**
	 * Return all the socket address on which we should listen.
	 * I tried explicitly enumerating the local addresses,
	 * but that caused unexplained problems with send().
	 * So for now, anyway, this returns wildcard addresses for all ports.
	 * @param ports The ports on which to listen.
	 * @return The socket addresses on which to listen.
	 */
	private Set<InetSocketAddress> getLocalInetAddrs(Collection<Integer> ports)
	{
		Set<InetSocketAddress> addrs = new HashSet<>();
		for (int port: ports) {
			if (true) {
				// Use a wildcard address for each port.
				addrs.add(new InetSocketAddress((InetAddress)null, port));
			} else {
				// Explicitly listen to each ipv4 address on that port.
				for (InetInterface iface: InetInterface.getAllInterfaces()) {
					if (iface.m_address instanceof Inet4Address) {
						addrs.add(new InetSocketAddress(iface.m_address, port));
					}
					if (iface.m_broadcast != null && iface.m_broadcast instanceof Inet4Address) {
						addrs.add(new InetSocketAddress(iface.m_broadcast, port));
					}
				}
			}
		}
		return addrs;
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
		
		/**
		 * Return prefix string for each message.
		 * @return the base class method returns "".
		 */
		public String msgPrefix() { return ""; }
		
		@Override
		public void msgArrived(ArtNetChannel chan, ArtNetMsg msg,
							InetSocketAddress sender, InetSocketAddress receiver)
		{
			m_out.println(msgPrefix() + "Rcv " + msg.m_opcode
							+ ": rmt: " + sender + " lcl: " + receiver);
			msg.print(m_out, "");
		}

		@Override
		public void msgArrived(ArtNetChannel chan, ArtNetOpcode opcode, byte[] buff, int len,
							InetSocketAddress sender, InetSocketAddress receiver)
		{
			m_out.println(msgPrefix() + "Rcv " + opcode
							+ ": rmt: " + sender + " lcl: "
							+ receiver + " len: " + len);
			m_dump.dump(buff, 0, len);
		}

		@Override
		public void msgArrived(ArtNetChannel chan, byte[] msg, int len,
							InetSocketAddress sender, InetSocketAddress receiver)
		{
			m_out.println(msgPrefix() + "Rcv ???: rmt: " + sender + " lcl: "
							+ receiver + " len: " + len);
			m_dump.dump(msg, 0, len);
		}
	}
}
