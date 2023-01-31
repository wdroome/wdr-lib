package com.wdroome.artnet;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import java.io.Closeable;
import java.io.IOException;

import java.net.InetSocketAddress;

import com.wdroome.artnet.msgs.ArtNetMsg;
import com.wdroome.artnet.msgs.ArtNetRdm;
import com.wdroome.artnet.msgs.RdmParamId;
import com.wdroome.artnet.msgs.RdmPacket;

/**
 * Send an RDM request to a device and return the response.
 * Art-Net (TM) Designed by and Copyright Artistic License Holdings Ltd.
 * @author wdr
 */
public class ArtNetRdmRequest implements ArtNetChannel.Receiver, Closeable
{
	private final ArtNetChannel m_channel;
	private final boolean m_isSharedChannel;
	private Map<ACN_UID, ArtNetPortAddr> m_uidMap = null;
	
	private int m_transNum = 0;
	private int m_srcUidManufacturer = 0x6975;
	private ACN_UID m_srcUid = new ACN_UID(0x6975,
										new Random(System.currentTimeMillis()).nextInt(0xffff));
	
	private final AtomicReference<ArrayBlockingQueue<ArtNetRdm>> m_replyQueue = new AtomicReference<>(null);
	private final AtomicReference<ArtNetRdm> m_reqMsg = new AtomicReference<>(null);
	
	private long m_timeoutMS = 4000;
	
	/**
	 * Create an object for sending RDM requests.
	 * @param channel A channel for sending and receiving Art-Net messages.
	 * 			If null, create a channel, and close it when done.
	 * @param uidMap An optional map with the UIDs on each network port.
	 * 			{@link #sendRequest(ACN_UID, boolean, RdmParamId, byte[])}
	 * 			uses this map to locate the UID's node.
	 * @throws IOException If an error occurs when creating the channel.
	 */
	public ArtNetRdmRequest(ArtNetChannel channel, Map<ACN_UID, ArtNetPortAddr> uidMap)
						throws IOException
	{
		if (channel != null) {
			m_channel = channel;
			m_isSharedChannel = true;
		} else {
			// System.out.println("XXX: ArtNetRdmRequest new ArtNetChannel c'tor");
			m_channel = new ArtNetChannel();
			m_isSharedChannel = false;			
		}
		m_uidMap = uidMap;
	}

	/**
	 * Close or disconnect from the channel.
	 */
	@Override
	public void close() throws IOException
	{
		if (m_isSharedChannel) {
			m_channel.dropReceiver(this);
		} else {
			m_channel.shutdown();
		}
	}
	
	/**
	 * Send an RDM request to a device and return the response.
	 * @param ipAddr The INET address of the node with the device.
	 * 				If null, broadcast the request.
	 * @param port The ArtNet port of the node with this device.
	 * @param destUid The device UID.
	 * @param isSet True if this is a SET request, false if it's a GET.
	 * @param paramId The RMD parameter id.
	 * @param requestData The request data. May be null.
	 * @return The RdmPacket with the device's reply, or null if the request timed out.
	 * @throws IOException If an IO error occurs when sending the request.
	 */
	public RdmPacket sendRequest(InetSocketAddress ipAddr, ArtNetPort port, ACN_UID destUid,
									boolean isSet, RdmParamId paramId, byte[] requestData) throws IOException
	{
		ArtNetRdm req = new ArtNetRdm();
		req.m_net = port.m_net;
		req.m_subnetUniv = port.subUniv();
		RdmPacket rdmPacket = new RdmPacket(destUid, isSet ? RdmPacket.CMD_SET : RdmPacket.CMD_GET,
											paramId, requestData);
		rdmPacket.m_transNum = m_transNum++;
		rdmPacket.m_srcUid = m_srcUid;
		req.m_rdmPacket = rdmPacket;
		m_reqMsg.set(req);
		ArrayBlockingQueue<ArtNetRdm> replyQueue = new ArrayBlockingQueue<>(10);
		m_replyQueue.set(replyQueue);
		m_channel.addReceiver(this);
		if (ipAddr != null) {
			// System.out.println("XXX: send " + ipAddr.getAddress().getHostAddress() + " " + req);
			m_channel.send(req, ipAddr);
		} else {
			m_channel.broadcast(req);
		}
		ArtNetRdm replyMsg = null;
		try {
			replyMsg = replyQueue.poll(m_timeoutMS, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
		}
		if (replyMsg == null) {
			// System.out.println("XXX: ArtNetRdmRequest.sendRequest timeout " + paramId);
		}
		m_channel.dropReceiver(this);
		m_reqMsg.set(null);
		m_replyQueue.set(null);
		if (replyMsg != null  && replyMsg.m_rdmPacket != null && replyMsg.m_rdmPacket.m_msgCount > 0) {
			System.out.println("XXX: sendReq/" + paramId + " msgCount="
								+ replyMsg.m_rdmPacket.m_msgCount);
		}
		return replyMsg != null ? replyMsg.m_rdmPacket : null;
	}
	
	/**
	 * Send an RDM request to a device and return the response.
	 * @param portAddr The ArtNet port and IP address of the node with this device.
	 * @param destUid The device UID.
	 * @param isSet True if this is a SET request, false if it's a GET.
	 * @param paramId The RMD parameter id.
	 * @param requestData The request data. May be null.
	 * @return The RdmPacket with the device's reply, or null if the request timed out.
	 * @throws IOException If an IO error occurs when sending the request.
	 */
	public RdmPacket sendRequest(ArtNetPortAddr portAddr, ACN_UID destUid, boolean isSet,
								 RdmParamId paramId, byte[] requestData) throws IOException
	{
		return sendRequest(portAddr.m_nodeAddr.m_nodeAddr, portAddr.m_port, destUid,
									isSet, paramId, requestData);
	}
	
	/**
	 * Send an RDM request to a device and return the response.
	 * This uses the UID map ({@link #setUidMap(Map)} to find the node address and port for the UID.
	 * @param destUid The device UID.
	 * @param isSet True if this is a SET request, false if it's a GET.
	 * @param paramId The RMD parameter id.
	 * @param requestData The request data. May be null.
	 * @return The RdmPacket with the device's reply, or null if the request timed out
	 * 			or the UID map does not have the node for destUID.
	 * @throws IOException If an IO error occurs when sending the request.
	 * @throws IllegalStateException If there is no UID map.
	 */
	public RdmPacket sendRequest(ACN_UID destUid, boolean isSet, RdmParamId paramId, byte[] requestData)
			throws IOException
	{
		if (m_uidMap == null) {
			throw new IllegalStateException("ArtNetRdmRequest: no uid map");
		}
		ArtNetPortAddr portAddr = m_uidMap.get(destUid);
		if (portAddr == null) {
			return null;
		}
		return sendRequest(portAddr.m_nodeAddr.m_nodeAddr, portAddr.m_port, destUid,
									isSet, paramId, requestData);
	}

	public Map<ACN_UID, ArtNetPortAddr> getUidMap() {
		return m_uidMap;
	}

	public void setUidMap(Map<ACN_UID, ArtNetPortAddr> uidMap) {
		this.m_uidMap = uidMap;
	}

	public long getTimeoutMS() {
		return m_timeoutMS;
	}

	public void setTimeoutMS(long timeoutMS) {
		this.m_timeoutMS = timeoutMS;
	}

	@Override
	public void msgArrived(ArtNetChannel chan, ArtNetMsg msg,
					InetSocketAddress sender, InetSocketAddress receiver)
	{
		if (!(msg instanceof ArtNetRdm)) {
			return;
		}
		ArtNetRdm replyMsg = (ArtNetRdm)msg;
		RdmPacket replyRdm = replyMsg.m_rdmPacket;
		if (replyRdm == null) {
			return;
		}
		ArtNetRdm reqMsg = m_reqMsg.get();
		if (reqMsg == null || reqMsg.m_rdmPacket == null) {
			return;
		}
		RdmPacket reqRdm = reqMsg.m_rdmPacket;
		if (!replyRdm.isReply(reqRdm.m_command)) {
			return;
		}
		if (reqRdm.m_transNum != replyRdm.m_transNum || !reqRdm.m_destUid.equals(replyRdm.m_srcUid)) {
			return;
		}
		ArrayBlockingQueue<ArtNetRdm> queue = m_replyQueue.get();
		if (queue == null) {
			return;
		}
		queue.add(replyMsg);
	}

	@Override
	public void msgArrived(ArtNetChannel chan, ArtNetOpcode opcode,
			byte[] buff, int len, InetSocketAddress sender, InetSocketAddress receiver)
	{
		// Ignore
	}

	@Override
	public void msgArrived(ArtNetChannel chan, byte[] msg, int len,
			InetSocketAddress sender, InetSocketAddress receiver) {
		// Ignore
	}
}
