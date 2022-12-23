package com.wdroome.artnet;

import static org.junit.Assert.*;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import java.io.*;

import org.junit.Test;

import com.wdroome.artnet.*;
import com.wdroome.artnet.msgs.ArtNetAddress;
import com.wdroome.artnet.msgs.ArtNetDiagData;
import com.wdroome.artnet.msgs.ArtNetDmx;
import com.wdroome.artnet.msgs.ArtNetIpProg;
import com.wdroome.artnet.msgs.ArtNetIpProgReply;
import com.wdroome.artnet.msgs.ArtNetMsg;
import com.wdroome.artnet.msgs.ArtNetPoll;
import com.wdroome.artnet.msgs.ArtNetPollReply;
import com.wdroome.util.HexDump;

/**
 * @author wdr
 */
public class ArtNetMsg_Test
{

	@Test
	public void testPoll() throws UnknownHostException
	{
		ArtNetPoll m = new ArtNetPoll();
		m.m_priority = ArtNetConst.DpMed;
		m.m_talkToMe = 2;
		
		regen("ArtNetPoll", m);
	}

	@Test
	public void testPollReply() throws UnknownHostException
	{
		ArtNetPollReply m = new ArtNetPollReply();
		m.m_ipAddr = (Inet4Address)InetAddress.getByName("10.1.2.3");
		m.m_ipPort = ArtNetConst.ARTNET_PORT;
		m.m_shortName = "enttec1";
		m.m_longName = "Enttec Open DMX Ethernet";
		m.m_swIn  = new byte[]{(byte)0x10, (byte)0x11, (byte)0x12, (byte)0x13};
		m.m_swOut = new byte[]{(byte)0x14, (byte)0x15, (byte)0x16, (byte)0x17};
		m.m_macAddr = new byte[] {(byte)10, (byte)11, (byte)12, (byte)13, (byte)14, (byte)15};
		m.m_status2 = 0x2;
		m.m_bindIpAddr = (Inet4Address)InetAddress.getByName("10.3.2.1");

		regen("ArtNetPollReply", m);
	}
	
	@Test
	public void testDmx()
	{
		ArtNetDmx m = new ArtNetDmx();
		m.m_sequence = 5;
		m.m_net = 0x4;
		m.m_data = new byte[] {
				(byte)(0x00), (byte)(0x20), (byte)(0x40), (byte)(0x60),
				(byte)(0x80), (byte)(0xa0), (byte)(0xc0), (byte)(0xe0),
				(byte)(0xff), (byte)(0x42),
		};
		m.m_dataLen = m.m_data.length;

		regen("ArtNetDmx", m);
	}
	
	@Test
	public void testDmxOdd()
	{
		ArtNetDmx m = new ArtNetDmx();
		m.m_sequence = 5;
		m.m_net = 0x4;
		m.m_data = new byte[] {
				(byte)(0x00), (byte)(0x20), (byte)(0x40), (byte)(0x60),
				(byte)(0x80), (byte)(0xa0), (byte)(0xc0), (byte)(0xe0),
				(byte)(0xff),
		};
		m.m_dataLen = m.m_data.length;

		regen("ArtNetDmx-odd", m);
	}
	
	@Test
	public void testDiagData()
	{
		ArtNetDiagData m = new ArtNetDiagData();
		m.m_priority = ArtNetConst.DpMed;
		m.m_data = "[Diagnostic data here]";

		if (false) {
			System.out.println("min/max size: " + ArtNetDiagData.minSize() + " "
							+ ArtNetDiagData.size());
		}
		regen("ArtNetDiagData", m);
	}
	
	@Test
	public void testIpProg() throws UnknownHostException
	{
		ArtNetIpProg m = new ArtNetIpProg();
		m.m_command = 0x82;
		m.m_ipAddr = (Inet4Address)InetAddress.getByName("10.1.2.3");
		m.m_ipMask = (Inet4Address)InetAddress.getByName("255.0.0.0");
		m.m_ipPort = ArtNetConst.ARTNET_PORT;	
		
		if (false) {
			m.print(System.out, "");
		}
		regen("ArtNetIpProg", m);
	}
	
	@Test
	public void testIpProgReply() throws UnknownHostException
	{
		ArtNetIpProgReply m = new ArtNetIpProgReply();
		m.m_ipAddr = (Inet4Address)InetAddress.getByName("10.1.2.3");
		m.m_ipMask = (Inet4Address)InetAddress.getByName("255.0.0.0");
		m.m_ipPort = ArtNetConst.ARTNET_PORT;
		m.m_status = 0x40;
		
		if (false) {
			m.print(System.out, "");
		}
		regen("ArtNetIpProg", m);
	}
	
	@Test
	public void testAddress() throws UnknownHostException
	{
		ArtNetAddress m = new ArtNetAddress();
		m.m_netAddr = 1;
		m.m_subNetAddr = 2;
		m.m_bindIndex = 3;
		m.m_shortName = "enttec1";
		m.m_longName = "Enttec Open DMX Ethernet";
		m.m_swIn  = new byte[]{(byte)0x10, (byte)0x11, (byte)0x12, (byte)0x13};
		m.m_swOut = new byte[]{(byte)0x14, (byte)0x15, (byte)0x16, (byte)0x17};
		m.m_command = 0x03;
		
		if (false) {
			m.print(System.out, "");
		}
		regen("ArtNetIpProg", m);
	}
	
	public void regen(String descr, ArtNetMsg m)
	{
		ByteArrayOutputStream mPrtBuff = new ByteArrayOutputStream();
		PrintStream mPrt = new PrintStream(mPrtBuff);
		m.print(mPrt, "");
		mPrt.flush();
		String mStr = mPrtBuff.toString();
		
		byte[] buff = new byte[ArtNetConst.MAX_MSG_LEN];
		int len = m.putData(buff, 0);
		ArtNetMsg m2 = ArtNetMsg.make(buff, 0, len, null);

		ByteArrayOutputStream m2PrtBuff = new ByteArrayOutputStream();
		PrintStream m2Prt = new PrintStream(m2PrtBuff);
		m2.print(m2Prt, "");
		m2Prt.flush();
		String m2Str = m2PrtBuff.toString();
		
		if (false) {
			System.out.println(mStr);
			System.out.println(m2Str);
		}
		assertEquals(descr, mStr, m2Str);
	}
	
	@Test
	public void testMakeSocketAddress() throws UnknownHostException
	{
		InetSocketAddress sockAddr = ArtNetMsg.makeSocketAddress("10.0.0.1");
		assertEquals("10.0.0.1:" + ArtNetConst.ARTNET_PORT, sockAddr.toString().replaceAll("^/", ""));
		sockAddr = ArtNetMsg.makeSocketAddress("10.0.0.2:12345");
		assertEquals("10.0.0.2:12345", sockAddr.toString().replaceAll("^/", ""));
	}
}
