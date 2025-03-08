package com.wdroome.osc;

import java.net.DatagramSocket;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.InetSocketAddress;

import com.wdroome.util.MiscUtil;
import com.wdroome.util.inet.InetUtil;

public class OSCUdpReader extends Thread
{
    private DatagramSocket m_socket;
    private boolean m_running;
    
    public OSCUdpReader(InetSocketAddress sockAddr) throws SocketException
    {
    	m_socket = new DatagramSocket(sockAddr);
    	start();
    }

    public OSCUdpReader(int port) throws SocketException
    {
    	if (port <= 0) {
    		throw new IllegalArgumentException("OSCUdpReader: invalid port " + port);
    	}
        m_socket = new DatagramSocket(port);
        start();
    }

    @Override public void run() {
    	System.out.println("Reading UDP from " + m_socket.getLocalSocketAddress() + ":");
        m_running = true;
        byte[] buf = new byte[8192];

        while (m_running) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
				m_socket.receive(packet);
			} catch (IOException e) {
				System.err.println("OSCUdpReadear: Error reading packet: " + e.toString());
				return;
			}
            
            if (false) {
            	System.out.println("OSCUdpReader: received " + packet.getLength()
            			+ " bytes from " + packet.getAddress().getHostAddress() + ":" + packet.getPort());
            }
            try {
            	OSCMessage msg = new OSCMessage(packet.getData(), packet.getOffset(), packet.getLength(),
            				(err) -> logError(err));
            	System.out.println("OSCUdpReader: msg from " + packet.getAddress().getHostAddress()
            						+ ":" + packet.getPort() + ":");
            	System.out.println("   " + msg);

            } catch (Exception e) {
            	System.err.println("OSCUdpReadear: Error " + e.toString() + "\n"
            			+ "converting OSC msg:\n"
            			+ MiscUtil.bytesToHex(packet.getData(), packet.getOffset(), packet.getLength()));
            }
        }
        m_socket.close();
    }

	/**
	 * Called to print an error message. Child class may override.
	 * The base class prints the message on std err.
	 * @param err The error message.
	 */
	public void logError(String err)
	{
		System.err.println("OSCUdpReader: " + err);
	}

    public static void main(String[] args)
    		throws SocketException, UnknownHostException, IllegalArgumentException
	{
    	String usage = "Usage: OSCUdpReader [ipaddr:port | port]";
    	if (args.length != 1) {
    		System.err.println(usage);
    		return;
    	}
    	if (args[0].contains(":")) {
    		new OSCUdpReader(InetUtil.parseAddrPort(args[0]));
    	} else {
    		new OSCUdpReader(Integer.parseInt(args[0]));
    	}
	}
}
