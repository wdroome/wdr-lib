package com.wdroome.osc.eos;

import java.io.IOException;
import java.net.InetSocketAddress;

import java.util.List;
import java.util.ArrayList;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.wdroome.osc.OSCConnection;
import com.wdroome.osc.OSCMessage;
import com.wdroome.osc.OSCUtil;

public class QueryEOS extends OSCConnection implements OSCConnection.MessageHandler
{
	public static final String GET_VERSION_METHOD = "/eos/get/version";
	public static final String GET_VERSION_RESP = "/eos/out/get/version";
	
	public static final String GET_CUELIST_COUNT_METHOD = "/eos/get/cuelist/count";
	public static final String GET_CUELIST_COUNT_RESP = "/eos/out/get/cuelist/count";

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
		replyHandler = sendMessage(new OSCMessage(GET_VERSION_METHOD), GET_VERSION_RESP, replies);
		String version = "(UNKNOWN)";
		while (true) {
			try {
				OSCMessage msg = replies.poll(500, TimeUnit.MILLISECONDS);
				if (msg.getArgType(0) == OSCUtil.OSC_STR_ARG_FMT_CHAR) {
					version = msg.getString(0, version);
					break;					
				}
			} catch (Exception e) {
				// Usually this is timeout on the poll().
				break;
			}
		}
		dropReplyHandler(replyHandler);
		return version;
	}
	
	public List<EOSCuelistInfo> getCuelists() throws IOException
	{
		int nCuelists = getCuelistCount();
		List<EOSCuelistInfo> cuelists = new ArrayList<>();
		for (int iCuelist = 0; iCuelist < nCuelists; iCuelist++) {
			String method = EOSCuelistInfo.getMethod(iCuelist);
			String replyPat = EOSCuelistInfo.getReplyPat(iCuelist);
			final ArrayBlockingQueue<OSCMessage> replies = new ArrayBlockingQueue<>(10);
			ReplyHandler replyHandler;
			replyHandler = sendMessage(new OSCMessage(method), replyPat, replies);
			EOSCuelistInfo cuelist = new EOSCuelistInfo(iCuelist, replies, this,
												replyHandler, m_timeoutMS);
			if (cuelist.isValid()) {
				cuelists.add(cuelist);
			}
		}
		return cuelists;
	}
	
	public int getCuelistCount() throws IOException
	{
		if (!isConnected()) {
			connect();
		}
		final ArrayBlockingQueue<OSCMessage> replies = new ArrayBlockingQueue<>(10);
		ReplyHandler replyHandler;
		replyHandler = sendMessage(new OSCMessage(GET_CUELIST_COUNT_METHOD), GET_CUELIST_COUNT_RESP, replies);
		int count = -1;
		while (true) {
			try {
				OSCMessage msg = replies.poll(m_timeoutMS, TimeUnit.MILLISECONDS);
				if (msg.getArgType(0) == OSCUtil.OSC_INT32_ARG_FMT_CHAR) {
					count = (int)msg.getLong(0, count);
					break;					
				}
			} catch (Exception e) {
				// Usually this is timeout on the poll().
				break;
			}
		}
		dropReplyHandler(replyHandler);
		return count;		
	}
	
	public static void main(String[] args) throws IOException
	{
		QueryEOS queryEOS = new QueryEOS(args[0]);
		System.out.println("Version: " + queryEOS.getVersion());
		System.out.println("Cuelist count: " + queryEOS.getCuelistCount());
		System.out.println("Cuelists: " + queryEOS.getCuelists());
	}
}
