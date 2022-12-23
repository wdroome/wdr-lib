package com.wdroome.artnet;

import java.util.concurrent.atomic.AtomicBoolean;

import java.io.IOException;
import java.io.PrintStream;

import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;

import com.wdroome.artnet.msgs.ArtNetMsg;
import com.wdroome.util.HexDump;
import com.wdroome.util.inet.InetInterface;
import com.wdroome.util.inet.InetUtil;

/**
 * A simple Thread that listens for Art-Net messages and passes them to a handler.
 * Art-Net (TM) Designed by and Copyright Artistic License Holdings Ltd.
 * <br>
 * NOTE: The thread is normally a daemon, so it will automatically stop
 * when all non-daemon threads have terminated. To prevent that, call setDaemon(false)
 * on this object.
 * @author wdr
 */
public class ArtNetListener extends Thread
{
	/**
	 * Art-Net message handler.
	 * These methods are called when Art-Net messages arrive.
	 * NOTE: The address part of "receiver" is the wildcard address -- 0.0.0.0.
	 * As far as I can tell, java does not make the destination address
	 * in the UDP packet header available to the code.
	 * @author wdr
	 */
	public interface Receiver
	{
		/**
		 * Called when a new Art-Net message arrives.
		 * @param chan The ArtNetListener which received the message.
		 * @param msg The message.
		 * @param sender The remote address.
		 * @param receiver The local address.
		 */
		public void msgArrived(ArtNetListener chan, ArtNetMsg msg,
								InetSocketAddress sender, InetSocketAddress receiver);
		
		/**
		 * Called when a new Art-Net message arrives,
		 * but we do not have a message type for the opcode.
		 * @param chan The ArtNetListener which received the message.
		 * @param opcode The art-net opcode.
		 * @param buff The raw bytes of the message.
		 * @param offset Offset of the message in the buffer.
		 * @param len The length of the message.
		 * @param sender The remote address.
		 * @param receiver The local address.
		 */
		public void msgArrived(ArtNetListener chan,
							ArtNetOpcode opcode, byte[] buff, int offset, int len,
							InetSocketAddress sender, InetSocketAddress receiver);
		
		/**
		 * Called when a non-Art-Net message arrives.
		 * @param chan The ArtNetListener which received the message.
		 * @param buff The raw bytes of the message.
		 * @param offset Offset of the message in the buffer.
		 * @param len The length of the message.
		 * @param sender The remote address.
		 * @param receiver The local address.
		 */
		public void msgArrived(ArtNetListener chan, byte[] buff, int offset, int len,
							InetSocketAddress sender, InetSocketAddress receiver); 
	}

	private final Receiver m_receiver;
	private DatagramSocket m_readSocket;

	// True if the thread is running.
	private final AtomicBoolean m_running = new AtomicBoolean(true);

	/**
	 * Create a new Art-Net message listener.
	 * @param receiver
	 * 		The message handler.
	 * @param listenPort
	 * 		The port to listen to. If 0, listen on the default Art-Net port.
	 * @throws IOException
	 * 		As thrown by DatagramSocket.
	 */
	public ArtNetListener(Receiver receiver, int listenPort) throws IOException
	{
		setName("ArtNetListener." + listenPort);
		setDaemon(true);
		m_receiver = receiver;
		if (listenPort <= 0) {
			listenPort = ArtNetConst.ARTNET_PORT;
		}
		m_readSocket = new DatagramSocket(listenPort);
		m_readSocket.setBroadcast(true);
		start();
	}
	
	/**
	 * Create a new Art-Net message listener on the default Art-Net port.
	 * @param receiver
	 * 		The message handler.
	 * @throws IOException
	 * 		As thrown by DatagramSocket.
	 */
	public ArtNetListener(Receiver receiver) throws IOException
	{
		this(receiver, ArtNetConst.ARTNET_PORT);
	}
	
	/**
	 * Listen for incoming messages and call the handler when they arrive.
	 */
	@Override
	public void run()
	{
		byte[] msgBuff = new byte[ArtNetConst.MAX_MSG_LEN];
		DatagramPacket msgPacket = new DatagramPacket(msgBuff, msgBuff.length);
		try {
			while (m_running.get()) {
				m_readSocket.receive(msgPacket);
				int offset = msgPacket.getOffset();
				int len = msgPacket.getLength();
				SocketAddress xsender = msgPacket.getSocketAddress();
				SocketAddress xreceiver = m_readSocket.getLocalSocketAddress();
				if (!(xsender instanceof InetSocketAddress && xreceiver instanceof InetSocketAddress)) {
					// Ignore non-ipv4 messages.
					continue;
				}
				InetSocketAddress sender = (InetSocketAddress)xsender;
				InetSocketAddress receiver = (InetSocketAddress)xreceiver;
				ArtNetMsg msg = ArtNetMsg.make(msgBuff, offset, len, sender);
				if (msg != null) {
					if (m_receiver != null) {
						m_receiver.msgArrived(this, msg, sender, receiver);
					}
					if (false) {
						System.out.println("ArtNetListener RCV op:" + msg.m_opcode
								+ " src:" + sender + " dest:" + receiver);
					}
				} else {
					ArtNetOpcode opcode = ArtNetMsg.getOpcode(msgBuff, offset, len);
					switch (opcode) {
					case Invalid:
						if (m_receiver != null) {
							m_receiver.msgArrived(this, msgBuff, offset, len, sender, receiver);
						}
						break;
					default:
						if (m_receiver != null) {
							m_receiver.msgArrived(this, opcode, msgBuff, offset, len, sender, receiver);
						}
						break;
					}
				}
			}
		} catch (IOException e) {
			if (m_running.get()) {
				e.printStackTrace();
			}
		} finally {
			try {
				m_readSocket.disconnect();
			} catch (Exception e2) {
				System.err.println("ArtNetListener.run: Error disconnecting socket: " + e2);
			}
			try {
				m_readSocket.close();
				// System.err.println("XXX: ArtNetListener close " + ci.m_port + " " + ci.m_channel);
			} catch (Exception e2) {
				System.err.println("ArtNetListener.run: Error closing socket: " + e2);
			}
		}
		// System.err.println("XXX: ArtNetListener.run: Exiting");
	}
	
	/**
	 * Stop the listener thread.
	 */
	public void shutdown()
	{
		m_running.set(false);
		interrupt();
		try {
			this.join();
		} catch (Exception e) {
			System.err.println("ArtNetListener.shutdown: Error waiting for Listener to stop: " + e);
		}
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
		public void msgArrived(ArtNetListener chan, ArtNetMsg msg,
							InetSocketAddress sender, InetSocketAddress receiver)
		{
			m_out.println(msgPrefix() + "Rcv " + msg.m_opcode
							+ ": rmt: " + sender + " lcl: " + receiver);
			msg.print(m_out, "");
		}

		@Override
		public void msgArrived(ArtNetListener chan, ArtNetOpcode opcode, byte[] buff, int offset, int len,
							InetSocketAddress sender, InetSocketAddress receiver)
		{
			m_out.println(msgPrefix() + "Rcv " + opcode
							+ ": rmt: " + sender + " lcl: "
							+ receiver + " len: " + len);
			m_dump.dump(buff, 0, len);
		}

		@Override
		public void msgArrived(ArtNetListener chan, byte[] buff, int offset, int len,
							InetSocketAddress sender, InetSocketAddress receiver)
		{
			m_out.println(msgPrefix() + "Rcv ???: rmt: " + sender + " lcl: "
							+ receiver + " len: " + len);
			m_dump.dump(buff, 0, len);
		}
	}
	
	public static void main(String[] args) throws IOException, InterruptedException
	{
		System.out.println("Listening for Art-Net messages. Use control-C to stop.");
		new ArtNetListener(new MsgPrinter());
		Thread.sleep(60*60*1000);
	}
}
