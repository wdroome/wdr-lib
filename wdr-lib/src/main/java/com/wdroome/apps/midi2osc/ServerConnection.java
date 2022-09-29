package com.wdroome.apps.midi2osc;

import java.io.PrintStream;
import java.io.IOException;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import java.net.InetSocketAddress;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.wdroome.osc.OSCConnection;
import com.wdroome.osc.OSCMessage;

import com.wdroome.json.JSONValue_Array;
import com.wdroome.json.JSONValue_String;
import com.wdroome.json.JSONValue_Number;
import com.wdroome.json.JSONValue;

/**
 * A thread that manages the connection to an OSC server.
 * To send a message to the server, a client adds an Action to a FIFO queue.
 * The thread takes Actions from the queue and sends them to the server.
 * @author wdr
 */
public class ServerConnection extends Thread implements OSCConnection.MessageHandler
{
	public static final int ACTION_QUEUE_SIZE = 400;
	
	private final ServerInfo m_serverInfo;
	private final Parameters m_params;
	private final Variables m_variables;
	
	private boolean m_sim;
	private boolean m_show;
	
	private final long m_startTS = System.currentTimeMillis();
	
	private final ArrayBlockingQueue<Action> m_actionQueue
								= new ArrayBlockingQueue<>(ACTION_QUEUE_SIZE);
	private final AtomicBoolean m_running = new AtomicBoolean(true);
	private final AtomicBoolean m_lostServerConnection = new AtomicBoolean(false);

	// The String key is the encoding of a TypeChanKey.
	private Map<String, Action> m_lastActions = new HashMap<>();
	private Map<String, Action> m_pendingActions = new HashMap<>();
	int m_minNewValueWaitMS = -1;	// min value for actions in m_pendingActions
	
	private PrintStream m_out = System.out;

	private OSCConnection m_conn = null;
	private InetSocketAddress m_serverSocketAddr = null;
	private String m_serverNameAddr = null;
	
	public ServerConnection(ServerInfo serverInfo, Parameters params, Variables variables)
	{
		m_serverInfo = serverInfo;
		m_params = params;
		m_variables = variables;
		setName("ServerConn/" + serverInfo.m_name);
		// setDaemon(true);
		
		if (m_serverInfo.m_sim) {
			m_sim = true;
			m_show = true;
		} else {
			m_sim = false;
			m_show = m_serverInfo.m_show;
		}
	}

	@Override
	public void run()
	{
		boolean doRetryWait = false;
		int nRetryAttempts = 0;
		while (m_running.get()) {
			if (m_lostServerConnection.get()) {
				// We had a connection & lost it. Clean up & delete the old connection.
				if (m_conn != null) {	// Shouldn't be null, but just in case ...
					System.out.println("*** Lost connection to OSC server " + m_serverNameAddr);
					try {
						m_conn.disconnect();
					} catch (Exception e1) {
						// ignore disconnect error.
					}
					m_conn = null;
				}
				m_lostServerConnection.set(false);
				
				// Stop if the configuration didn't enable retry.
				if (m_serverInfo.m_reconnectWaitMS <= 0) {
					m_running.set(false);
					return;
				}
				
				// Clear any queued actions.
				m_actionQueue.clear();
				m_pendingActions.clear();
				m_lastActions.clear();
				m_minNewValueWaitMS = -1;
				doRetryWait = true;
			}
			if (doRetryWait) {
				try {
					Thread.sleep(m_serverInfo.m_reconnectWaitMS);
				} catch (Exception e) {
					// Stop waiting if we get an INTERRUPT.
				}
				doRetryWait = false;
			}
			if (m_sim) {
				m_serverNameAddr = m_serverInfo.m_name + "@sim";
			} else if (m_conn == null) {
				if (m_params.isVerbose()) {
					System.out.println("  Try connect to \"" + m_serverInfo.m_name + "\"");
				}
				connect();
				if (m_conn != null) {
					System.out.println("  Connected to OSC server \"" + m_serverNameAddr + "\"");
					nRetryAttempts = 0;
				} else if (m_serverInfo.m_reconnectWaitMS > 0) {
					doRetryWait = true;
					if (nRetryAttempts == 0) {
						System.out.println("  Trying to connect to OSC server \"" + m_serverInfo.m_name + "\"");
					}
					nRetryAttempts += 1;
					continue;
				} else {
					System.out.println("  Cannot connect to OSC server \""+ m_serverInfo.m_name
									+ "\". Simulating connection instead.");
					m_show = true;
					m_sim = true;
					m_serverNameAddr = m_serverInfo.m_name + "@(sim)";
				}
			}
			Action action;
			try {
				action = m_actionQueue.poll(calcPollWait(), TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				action = null;
			}
			// If the server disconnected while we were waiting, go back to the top.
			// That will retry the connection if appropriate.
			if (m_lostServerConnection.get()) {
				continue;
			}
			if (action != null) {
				Action lastAction = m_lastActions.get(action.m_actionSpec.m_encodedTCK);
				if (lastAction != null) {
					long curTS = System.currentTimeMillis();
					int newValueWaitMS = lastAction.getNewValueWaitMS();
					if (action.m_data2 == lastAction.m_data2
							&& (action.m_arriveTS - lastAction.m_arriveTS) < lastAction.getSameValueSkipMS()) {
						action = null;
					} else if (curTS < lastAction.getSentTS() + newValueWaitMS) {
						action.setHoldTS(lastAction.getSentTS() + newValueWaitMS);
						m_pendingActions.put(action.m_actionSpec.m_encodedTCK, action);
						if (m_minNewValueWaitMS < 0 || newValueWaitMS < m_minNewValueWaitMS) {
							m_minNewValueWaitMS = newValueWaitMS;
						}
						if (false) {
							System.out.println("XXX hold " + action.m_actionSpec.m_encodedTCK + " lastSentTS="
									+ lastAction.getSentTS() + " holdTS=" + action.getHoldTS() + " curTS="
									+ System.currentTimeMillis());
						}
						action = null;
					}
				}
				if (action != null) {
					sendMsg(action);
				}
			}
			for (Iterator<Action> iter = m_pendingActions.values().iterator(); iter.hasNext(); ) {
				Action pending = iter.next();
				long curTS = System.currentTimeMillis();
				if (curTS >= pending.getHoldTS()) {
					if (m_params.isVerbose()) {
						System.out.println("Send pending msg " + pending.m_actionSpec.m_encodedTCK
								+ " holdTS=" + pending.getHoldTS()
								+ " curTS=" + curTS);
					}
					sendMsg(pending);
					iter.remove();
				}
			}
			if (m_pendingActions.isEmpty()) {
				m_minNewValueWaitMS = -1;
			}
		}
	}
	
	/**
	 * Establish a connection to the OSC server. Try the configured addresses until one succeeds,
	 * or we hit our overall time limit. If successful, set m_conn, m_serverSocketAddress
	 * and m_serverNameAddr.
	 */
	private void connect()
	{
		for (InetSocketAddress addr: m_serverInfo.m_ipaddrs) {
			OSCConnection conn = new OSCConnection(addr);
			conn.setConnectTimeout(m_serverInfo.m_connectTimeoutMS);
			conn.setMessageHandler(this);
			try {
				conn.connect();
				m_conn = conn;
				m_serverSocketAddr = addr;
				m_serverNameAddr = m_serverInfo.m_name
						+ "@" + m_serverSocketAddr.getAddress().getHostAddress()
						+ ":" + m_serverSocketAddr.getPort();
				break;
			} catch (IOException e) {
				try {
					m_conn.disconnect();
				} catch (Exception e1) {
					// ignore disconnect error.
				}
			}
		}
	}
	
	private void sendMsg(Action action)
	{
		for (JSONValue_Array msgSpec: action.m_actionSpec.m_cmdTemplate) {
			OSCMessage oscMsg = new OSCMessage(msgSpec.getString(0));
			int msgSpecSize = msgSpec.size();
			for (int iArg = 1; iArg < msgSpecSize; iArg++) {
				JSONValue argJson = msgSpec.get(iArg);
				if (argJson instanceof JSONValue_String) {
					oscMsg.addArg(m_variables.replaceVariables(((JSONValue_String)argJson).m_value,
														action.m_actionSpec.m_tck, action.m_data2, -1));
				} else if (argJson instanceof JSONValue_Number) {
					double val = ((JSONValue_Number)argJson).m_value;
					if (val == (int)val) {
						oscMsg.addArg(Integer.valueOf((int)val));
					} else {
						oscMsg.addArg(Float.valueOf((float)val));
					}
				} else if (argJson instanceof JSONValue_Array) {
					// Legacy!!!
					String transform = ((JSONValue_Array)argJson).getString(0);
					if (transform != null) {
						oscMsg.addArg(m_variables.applyTransform(action.m_data2, transform));
					} else {
						oscMsg.addArg(action.m_data2 + "");
					}
				} else {
					oscMsg.addArg(argJson);
				}
			}
			if (m_show) {
				System.out.println("SendCmd @"
							+ String.format("%.3f", (System.currentTimeMillis()-m_startTS)/1000.0)
							+ " " + m_serverNameAddr
							+ ": " + oscMsg);
			}
			if (m_conn != null) {
				try {
					m_conn.sendMessage(oscMsg);
				} catch (IOException e) {
					if (m_out != null) {
						m_out.println("*** IO Error sending to OSC: " + e);
					}
				} 
			}
		}
		action.setSentTS(System.currentTimeMillis());
		m_lastActions.put(action.m_actionSpec.m_encodedTCK, action);
	}
	
	private long calcPollWait()
	{
		long wait = 2000;
		if (!m_pendingActions.isEmpty()) {
			wait = m_serverInfo.m_newValueWaitMS/2;
		}
		if (wait < 20) {
			wait = 20;	// Sanity check
		}
		return wait;
	}
	
	/**
	 * Queue an action to send to the OSC server.
	 * If the queue is full, discard the action.
	 * If the queue is full, something is seriously wrong -- the the server died
	 * -- so there's no point in waiting.
	 * @param action The OSC action to be send to the server.
	 */
	public void addActionToQueue(Action action)
	{
		m_actionQueue.offer(action);
	}

	/* (non-Javadoc)
	 * @see com.wdroome.osc.OSCConnection.MessageHandler(OSCMessage)
	 */
	@Override
	public void handleOscResponse(OSCMessage msg)
	{
		if (msg == null) {
			m_lostServerConnection.set(true);
			this.interrupt();
		} else if (m_serverInfo.m_showResponses && m_out != null) {
			m_out.println("*** From " + m_serverNameAddr + ": " + msg.toString());
		}
	}
}
