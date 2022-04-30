package com.wdroome.osc.eos;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.wdroome.osc.OSCConnection;
import com.wdroome.osc.OSCMessage;

public class QueryEOS extends OSCConnection implements OSCConnection.MessageHandler
{
	private long m_timeoutMS = 5000;

	public QueryEOS(InetSocketAddress addr)
	{
		super(addr);
		// TODO Auto-generated constructor stub
	}

	public QueryEOS(String addr, int port) throws IllegalArgumentException
	{
		super(addr, port);
		// TODO Auto-generated constructor stub
	}

	public QueryEOS(String addrPort) throws IllegalArgumentException
	{
		super(addrPort);
		// TODO Auto-generated constructor stub
	}

	/**
	 * MessageHandler interface -- process message from EOS server.
	 */
	@Override
	public void handleOscResponse(OSCMessage msg)
	{
		// TODO Auto-generated method stub
		// XXX -- do we need this??????
	}
	
	public long getTimeoutMS() {
		return m_timeoutMS;
	}

	public void setTimeoutMS(long timeoutMS) {
		this.m_timeoutMS = timeoutMS;
	}

	public String getVersion() throws IOException
	{
		if (!isConnected()) {
			connect();
		}
		final ArrayBlockingQueue<OSCMessage> replies = new ArrayBlockingQueue<>(10);
		ReplyHandler replyHandler;
		replyHandler = sendMessage(new OSCMessage(EOSCmds.GET_VERSION_CMD), EOSCmds.GET_VERSION_RESP,
					(msg) -> {
							try { replies.put(msg); } catch (Exception e) {}
							return true;
						});
		String version = "(UNKNOWN)";
		while (true) {
			try {
				OSCMessage msg = replies.poll(m_timeoutMS, TimeUnit.MILLISECONDS);
				if (msg.getArgTypes().startsWith("s")) {
					Object arg = msg.getArgs().get(0);
					if (arg instanceof String) {
						version = (String)arg;
						break;
					}
				}
			} catch (Exception e) {
				// Usually this is timeout on the poll().
				break;
			}
		}
		dropReplyHandler(replyHandler);
		return version;
	}
	
	public static void main(String[] args) throws IOException
	{
		QueryEOS queryEOS = new QueryEOS(args[0]);
		System.out.println("Version: " + queryEOS.getVersion());
	}
}
