package com.wdroome.apps.midi2osc;

import java.net.InetSocketAddress;
import java.util.List;

public class ServerInfo
{
	public final String m_name;
	public final List<InetSocketAddress> m_ipaddrs;
	public final int m_connectTimeoutMS;
	public final int m_reconnectWaitMS;
	public final boolean m_show;
	public final boolean m_showResponses;
	public final boolean m_sim;
	public final int m_newValueWaitMS;
	public final int m_sameValueSkipMS;
	
	public ServerInfo(String name, List<InetSocketAddress> ipaddrs,
						int connectTimeoutMS, int reconnectWaitMS,
						boolean show, boolean showResponses, boolean sim,
						int newValueWaitMS, int sameValueSkipMS)
	{
		m_name = name;
		m_ipaddrs = ipaddrs;
		m_connectTimeoutMS = connectTimeoutMS;
		m_reconnectWaitMS = reconnectWaitMS;
		m_show = show;
		m_showResponses = showResponses;
		m_sim = sim;
		m_newValueWaitMS = newValueWaitMS;
		m_sameValueSkipMS = sameValueSkipMS;
	}

	@Override
	public String toString() {
		return "ServerInfo[" + m_name + ", " + m_ipaddrs
				+ ", connectTimeoutMS=" + m_connectTimeoutMS
				+ ", reconnectWaitMS=" + m_reconnectWaitMS
				+ ", show=" + m_show + "/" + m_showResponses + ", sim=" + m_sim
				+ ", new/sameMS=" + m_newValueWaitMS + "/" + m_sameValueSkipMS
				+ "]";
	}	
}
