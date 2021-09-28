package com.wdroome.osc;

import java.util.List;
import java.util.ArrayList;

import java.net.InetAddress;
import java.io.IOException;
import java.net.Inet4Address;

import com.wdroome.util.inet.CIDRAddress;
import com.wdroome.util.inet.InetInterface;

public class OSCFind implements OSCConnection.MessageHandler
{
	private boolean m_printDotsWhileSearching = false;
	private int m_connectTimeoutMS = 200;
	private TestAddress preConn = null;
	private TestAddress postConn = null;
	
	public interface TestAddress
	{
		public void testAddr(InetAddress addr);
	}
	
	public List<InetAddress> find(int port)
	{
		List<InetAddress> addrs = new ArrayList<>();
		for (InetInterface iface: InetInterface.getAllInterfaces()) {
			if (iface.m_address instanceof Inet4Address) {
				for (InetAddress addr: iface.m_cidr) {
					// System.out.println("XXX: test " + addr);
					if (preConn != null) {
						preConn.testAddr(addr);
					}
					OSCConnection conn = new OSCConnection(addr, port);
					conn.setMessageHandler(this);
					conn.setConnectTimeout(m_connectTimeoutMS);
					try {
						long ts = System.currentTimeMillis();
						conn.connect();
						if (postConn != null) {
							postConn.testAddr(addr);
						}
						System.out.println("XXX Connected to " + addr + " in "
								+ (System.currentTimeMillis() - ts) + "ms");
						addrs.add(addr);
						conn.disconnect();
					} catch (IOException e) {
						continue;
					}
				}
			}
		}
		return addrs;
	}

	public static void main(String[] args)
	{
		OSCFind finder = new OSCFind();
		finder.setPreConn(addr -> System.out.print("."));
		finder.setPostConn(addr -> {
									System.out.println();
									System.out.println("OSC Server: " + addr.getHostAddress());
								});
		List<InetAddress> oscServers = finder.find(8192);
		System.out.println("XXX: " + oscServers);
	}

	@Override
	public void handleOscResponse(OSCMessage msg)
	{
		// TODO Auto-generated method stub
		
	}

	public boolean isPrintDotsWhileSearching() {
		return m_printDotsWhileSearching;
	}

	public void setPrintDotsWhileSearching(boolean m_printDotsWhileSearching) {
		this.m_printDotsWhileSearching = m_printDotsWhileSearching;
	}

	public int getConnectTimeoutMS() {
		return m_connectTimeoutMS;
	}

	public void setConnectTimeoutMS(int connectTimeoutMS) {
		this.m_connectTimeoutMS = connectTimeoutMS >= 10 ? connectTimeoutMS : m_connectTimeoutMS;
	}

	public TestAddress getPreConn() {
		return preConn;
	}

	public void setPreConn(TestAddress preConn) {
		this.preConn = preConn;
	}

	public TestAddress getSucceedConn() {
		return postConn;
	}

	public void setPostConn(TestAddress postConn) {
		this.postConn = postConn;
	}

}
