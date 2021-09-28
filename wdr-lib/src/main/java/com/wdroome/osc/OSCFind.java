package com.wdroome.osc;

import java.util.List;
import java.util.ArrayList;

import java.net.InetAddress;
import java.io.IOException;
import java.net.Inet4Address;

import com.wdroome.util.MiscUtil;
import com.wdroome.util.inet.CIDRAddress;
import com.wdroome.util.inet.InetInterface;

public class OSCFind implements OSCConnection.MessageHandler
{
	private int m_connectTimeoutMS = 150;
	private PreConnect m_preConn = null;
	private PostConnect m_postConn = null;
	private int m_respWaitMS = 3000;
	
	private InetAddress m_testAddr;
	private List<OSCMessage> m_responses = new ArrayList<>();
	private long m_connectTS;
	
	public interface PreConnect
	{
		public boolean preConnect(InetAddress addr);
	}
	
	public interface PostConnect
	{
		public boolean postConnect(InetAddress addr, List<OSCMessage> responses);
	}
	
	public List<InetAddress> find(int port)
	{
		boolean running = true;
		List<InetAddress> addrs = new ArrayList<>();
		for (InetInterface iface: InetInterface.getAllInterfaces()) {
			if (!running) {
				break;
			}
			if (iface.m_address instanceof Inet4Address) {
				for (InetAddress addr: iface.m_cidr) {
					if (!running) {
						break;
					}
					m_testAddr = addr;
					// System.out.println("XXX: test " + addr);
					if (m_preConn != null) {
						if (!m_preConn.preConnect(addr)) {
							running = false;
							break;
						}
					}
					OSCConnection conn = new OSCConnection(addr, port);
					conn.setMessageHandler(this);
					conn.setConnectTimeout(m_connectTimeoutMS);
					m_responses.clear();
					try {
						long ts = System.currentTimeMillis();
						conn.connect();
						m_connectTS = System.currentTimeMillis();
						System.out.println("XXX Connected to " + addr + " in "
								+ (System.currentTimeMillis() - ts) + "ms");
						if (m_respWaitMS > 0) {
							MiscUtil.sleep(m_respWaitMS);
						}
						addrs.add(addr);
						conn.disconnect();
						MiscUtil.sleep(500);	// Drain any responses
						if (m_postConn != null) {
							m_postConn.postConnect(addr, m_responses);
						}
					} catch (IOException e) {
						continue;
					}
				}
			}
		}
		return addrs;
	}

	@Override
	public void handleOscResponse(OSCMessage msg)
	{
		System.out.println("   RESP "
					+ m_testAddr.getHostAddress()
					+ " in " + (System.currentTimeMillis() - m_connectTS)
					+ "ms: " + msg);
		if (m_responses != null) {
			m_responses.add(msg);
		}
	}

	public int getConnectTimeoutMS() {
		return m_connectTimeoutMS;
	}

	public void setConnectTimeoutMS(int connectTimeoutMS) {
		this.m_connectTimeoutMS = connectTimeoutMS >= 10 ? connectTimeoutMS : m_connectTimeoutMS;
	}

	public PreConnect getPreConn() {
		return m_preConn;
	}

	public void setPreConn(PreConnect preConn) {
		this.m_preConn = preConn;
	}

	public PostConnect getPostConn() {
		return m_postConn;
	}

	public void setPostConn(PostConnect postConn) {
		this.m_postConn = postConn;
	}

	public int getM_respWaitMS() {
		return m_respWaitMS;
	}

	public void setM_respWaitMS(int m_respWaitMS) {
		this.m_respWaitMS = m_respWaitMS;
	}

	public static void main(String[] args)
	{
		OSCFind finder = new OSCFind();
		finder.setPreConn(addr ->  {System.out.print("."); return true;} );
		finder.setPostConn((addr, responses) -> {
									System.out.println();
									System.out.println("OSC Server: " + addr.getHostAddress()
											+ " " + responses.size() + " responses.");
									return true;
								});
		List<InetAddress> oscServers = finder.find(8192);
		System.out.println("XXX: " + oscServers);
	}

}
