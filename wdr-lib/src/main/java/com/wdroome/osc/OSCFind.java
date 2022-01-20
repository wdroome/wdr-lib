package com.wdroome.osc;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import java.net.InetAddress;
import java.io.IOException;
import java.net.Inet4Address;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.wdroome.util.MiscUtil;
import com.wdroome.util.inet.CIDRAddress;
import com.wdroome.util.inet.InetInterface;

/**
 * Find OSC servers on attached interfaces.
 * @author wdr
 */
public class OSCFind
{
	private int m_connectTimeoutMS = 500;
	private PreConnect m_preConn = null;
	private PostConnect m_postConn = null;
	private int m_respWaitMS = 3000;
	
	private final int NUM_WORKERS = 32;
	private final int MAX_TEST_ADDRS = 8192;	// To keep it from running too long ...
	
	private final AtomicBoolean m_finishedAddingAddrs = new AtomicBoolean(false);
		
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
		ArrayBlockingQueue<InetAddress> pendingAddrs = new ArrayBlockingQueue<>(NUM_WORKERS);
		ArrayList<InetAddress> foundAddrs = new ArrayList<>();
		ArrayList<Worker> workers = new ArrayList<>();
		
		for (int i = 0; i < NUM_WORKERS; i++) {
			workers.add(new Worker(i, port, pendingAddrs, foundAddrs));
		}
		int nTestAddrs = 0;
		for (InetInterface iface: InetInterface.getAllInterfaces()) {
			if (!running) {
				break;
			}
			if (iface.m_address instanceof Inet4Address) {
				for (InetAddress addr: iface.m_cidr) {
					if (!running) {
						break;
					}
					if (++nTestAddrs > MAX_TEST_ADDRS) {
						running = false;
						break;
					}
					try {
						pendingAddrs.put(addr);
					} catch (Exception e) {
						running = false;
						break;
					}
				}
			}
		}
		m_finishedAddingAddrs.set(true);
		// System.out.println("XXX: done adding addrs, waiting for workers to finish.");
		for (Worker worker: workers) {
			try {
				worker.join();
			} catch (InterruptedException e) {
			}
		}
		return foundAddrs;
	}
	
	private class Worker extends Thread implements OSCConnection.MessageHandler
	{
		private final BlockingQueue<InetAddress> m_pendingAddrs;
		private final ArrayList<InetAddress> m_foundAddrs;
		private final int m_port;
		private boolean m_running = true;
		private InetAddress m_testAddr;
		private List<OSCMessage> m_responses = new ArrayList<>();
		private long m_connectTS;
		
		public Worker(int workerNum, int port, BlockingQueue<InetAddress> pendingAddrs,
					ArrayList<InetAddress> foundAddrs)
		{
			setName("OSCFind.Worker-" + workerNum);
			setDaemon(true);
			m_port = port;
			m_pendingAddrs = pendingAddrs;
			m_foundAddrs = foundAddrs;
			start();
		}

		@Override
		public void run()
		{
			while (m_running) {
				try {
					m_testAddr = m_pendingAddrs.poll(1000, TimeUnit.MILLISECONDS);
					if (m_testAddr == null) {
						if (m_finishedAddingAddrs.get()) {
							m_running = false;
							break;
						} else {
							continue;
						}
					}
				} catch (InterruptedException e) {
					m_running = false;
					break;
				}
				if (m_preConn != null) {
					if (!m_preConn.preConnect(m_testAddr)) {
						m_running = false;
						break;
					}
				}
				m_responses.clear();
				OSCConnection conn = new OSCConnection(m_testAddr, m_port);
				conn.setMessageHandler(this);
				conn.setConnectTimeout(m_connectTimeoutMS);
				try {
					long ts = System.currentTimeMillis();
					conn.connect();
					m_connectTS = System.currentTimeMillis();
					if (false) {
						System.out.println("XXX Connected to " + m_testAddr + " in " + (System.currentTimeMillis() - ts)
								+ "ms");
					}
					if (m_respWaitMS > 0) {
						MiscUtil.sleep(m_respWaitMS);
					}
					synchronized (m_foundAddrs) {
						m_foundAddrs.add(m_testAddr);
					}
					conn.disconnect();
					MiscUtil.sleep(500);	// Drain any responses
					if (m_postConn != null) {
						m_postConn.postConnect(m_testAddr, m_responses);
					}
				} catch (IOException e) {
					continue;
				}
			}
		}
		
		@Override
		public void handleOscResponse(OSCMessage msg)
		{
			if (false) {
				System.out.println("   RESP " + m_testAddr.getHostAddress() + " in "
						+ (System.currentTimeMillis() - m_connectTS) + "ms: " + msg);
			}
			if (m_responses != null) {
				m_responses.add(msg);
			}
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

	public int geRespWaitMS() {
		return m_respWaitMS;
	}

	public void setRespWaitMS(int m_respWaitMS) {
		this.m_respWaitMS = m_respWaitMS;
	}

	public static void main(String[] args)
	{
		int port;
		try {
			port = Integer.parseInt(args[0]);
		} catch (Exception e) {
			System.err.println("Usage: OSCFind port#");
			return;
		}
		Map<InetAddress,List<OSCMessage>> serverResponses = new HashMap<>();
		OSCFind finder = new OSCFind();
		if (true) {
			finder.setPreConn(addr -> {
				System.out.print(".");
				return true;
			});
		}
		finder.setPostConn((addr, responses) -> {
			synchronized(serverResponses) {
				serverResponses.put(addr, new ArrayList<OSCMessage>(responses));
			}
			return true;
		});

		List<InetAddress> oscServers = finder.find(port);
		System.out.println();
		System.out.println(oscServers.size() + " OSC servers found on port " + port);
		for (InetAddress addr: oscServers) {
			System.out.println("   " + addr.getHostAddress());
		}
		for (Map.Entry<InetAddress, List<OSCMessage>> ent: serverResponses.entrySet()) {
			System.out.println(ent.getKey().getHostAddress() + ":");
			for (OSCMessage msg: ent.getValue()) {
				System.out.println("  " + msg);
			}
		}
	}

}
