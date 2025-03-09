package com.wdroome.osc;

import java.io.IOException;
import java.io.PrintStream;

import java.net.DatagramSocket;
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
    private long m_startTS = System.currentTimeMillis();
    private PrintStream m_out = System.out;
    private PrintStream m_err = System.err;
    private String m_acceptPattern = ".*";
    private long m_nIgnored = 0;
    
    /**
     * Create a reader thread. Client must start the thread.
     * @param sockAddr The address & port to listen on.
     * @throws SocketException If we cann't connect to that address and port.
     */
    public OSCUdpReader(InetSocketAddress sockAddr) throws SocketException
    {
    	m_socket = new DatagramSocket(sockAddr);
    }

    /**
     * Create a reader thread. Client must start the thread.
     * @param port The port to listen on.
     * @throws SocketException If we cannot listen on that port.
     */
    public OSCUdpReader(int port) throws SocketException
    {
    	if (port <= 0) {
    		throw new IllegalArgumentException("OSCUdpReader: invalid port " + port);
    	}
        m_socket = new DatagramSocket(port);
    }

    /**
     * Ignore any message whose method doesn't match this pattern.
     * Pattern must match the full method. Default is ".*".
     * @param m_acceptPattern Regex pattern for method for desired messages.
     */
	public void setAcceptPattern(String acceptPattern) {
		m_acceptPattern = (acceptPattern != null) ? acceptPattern : ".*";
	}
	   
    public String getAcceptPattern() {
		return m_acceptPattern;
	}

	public long getNIgnored() {
		return m_nIgnored;
	}

	public void setOutStream(PrintStream out) {
    	m_out = out;
    }
    
    public void setErrStream(PrintStream err) {
    	m_err = err;
    }

    @Override
    public void run() {
    	System.out.println("Reading UDP from " + m_socket.getLocalSocketAddress() + ":");
        m_running = true;
        byte[] buf = new byte[8192];

        while (m_running) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
				m_socket.receive(packet);
			} catch (IOException e) {
				if (m_err != null) {
					m_err.println("OSCUdpReadear: Error reading packet: " + e.toString());
				}
				return;
			}
            
            if (false) {
            	System.out.println("OSCUdpReader: received " + packet.getLength()
            			+ " bytes from " + packet.getAddress().getHostAddress() + ":" + packet.getPort());
            }
            try {
            	OSCMessage msg = new OSCMessage(packet.getData(), packet.getOffset(), packet.getLength(),
            				(err) -> logError(err));
            	if (msg.getMethod().matches(m_acceptPattern)) {
            		handleOscMessage(msg, packet);
            	} else {
            		m_nIgnored++;
            	}
            } catch (Exception e) {
            	if (m_err != null) {
					m_err.println("OSCUdpReader: Error converting OSC msg:\n" + "    " + e.toString() + "\n"
							+ MiscUtil.bytesToHex(packet.getData(), packet.getOffset(), packet.getLength()));
					e.printStackTrace();  // XXX
				}
            }
        }
        m_socket.close();
    }
    
    /**
     * Called when an acceptable OSC message arrives. The base class prints the message & the sender
     * @param msg The incoming message.
     * @param packet The incoming packet, with the sender's address & port.
     */
    public void handleOscMessage(OSCMessage msg, DatagramPacket packet)
    {
    	if (m_out != null) {
			m_out.println("OSCUdpReader: msg from " + packet.getAddress().getHostAddress()
					+ ":" + packet.getPort()
					+ ":   " + msg.toString(m_startTS));
		}
    }

	/**
	 * Called to print an error message. Child class may override.
	 * The base class prints the message on std err.
	 * @param err The error message.
	 */
	public void logError(String err)
	{
		if (m_err != null) {
			m_err.println("OSCUdpReader: " + err);
		}
	}

    public static void main(String[] args)
    		throws SocketException, UnknownHostException, IllegalArgumentException
	{
    	String usage = "Usage: OSCUdpReader [ipaddr:]port [accept-method-pattern]";
    	if (!(args.length >= 1 && args.length <= 2)) {
    		System.err.println(usage);
    		return;
    	}
    	OSCUdpReader reader;
    	if (args[0].contains(":")) {
    		reader = new OSCUdpReader(InetUtil.parseAddrPort(args[0]));
    	} else {
    		reader = new OSCUdpReader(Integer.parseInt(args[0]));
    	}
    	if (args.length >= 2) {
    		reader.setAcceptPattern(args[1]);
    	}
    	reader.start();
	}
}
