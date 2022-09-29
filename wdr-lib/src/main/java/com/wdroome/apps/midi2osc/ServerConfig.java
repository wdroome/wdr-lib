package com.wdroome.apps.midi2osc;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import com.wdroome.json.JSONValue;
import com.wdroome.json.JSONValue_String;
import com.wdroome.json.JSONValue_Array;
import com.wdroome.json.JSONValue_StringArray;
import com.wdroome.json.JSONValue_Object;

import com.wdroome.util.inet.InetUtil;

/**
 * Static configuration information for the OSC servers.
 * This does not include the actual connection to the server.
 * @author wdr
 */
public class ServerConfig implements Iterable<ServerInfo>
{	
	private final Map<String,ServerInfo> m_serverMap = new HashMap<>();
	
	private final ServerInfo m_defServer;
	
	public ServerConfig(JSONValue_Array servers, Parameters params)
	{
		int defTimeout = params.getIntParam(Midi2OscUtil.FN_CONNECT_TIMEOUT_MS,
											Midi2OscUtil.DEF_CONNECT_TIMEOUT_MS);
		int defReconnectWait = params.getIntParam(Midi2OscUtil.FN_RECONNECT_WAIT_MS,
											Midi2OscUtil.DEF_RECONNECT_WAIT_MS);
		boolean defSim = params.getBooleanParam(Midi2OscUtil.FN_SIM, false);
		boolean defShow = params.getBooleanParam(Midi2OscUtil.FN_SHOW, false);
		boolean defShowResponses = params.getBooleanParam(Midi2OscUtil.FN_SHOW_RESPONSES, false);
		int defNewValueWaitMS = params.getIntParam(Midi2OscUtil.FN_NEW_VALUE_WAIT_MS,
											Midi2OscUtil.DEF_NEW_VALUE_WAIT_MS);
		int defSameValueSkipMS = params.getIntParam(Midi2OscUtil.FN_SAME_VALUE_SKIP_MS,
											Midi2OscUtil.DEF_SAME_VALUE_SKIP_MS);
		ServerInfo lastServer = null;
		if (servers != null) {
			for (int iServer = 0; iServer < servers.size(); iServer++) {
				JSONValue jval = servers.get(iServer);
				if (!(jval instanceof JSONValue_Object)) {
					continue;
				}
				JSONValue_Object serverConfig = (JSONValue_Object)jval;
				String name = serverConfig.getString(Midi2OscUtil.FN_NAME, "");
				if (name.isBlank()) {
					System.err.println("Missing " + Midi2OscUtil.FN_NAME + " in "
							+ Midi2OscUtil.FN_OSC_SERVERS + "[" + iServer + "].");
					continue;
				}
				List<InetSocketAddress> ipaddrs = new ArrayList<>();
				for (String addr: new JSONValue_StringArray(
						serverConfig.getArray(Midi2OscUtil.FN_IPADDRS, null), true)) {
					try {
						ipaddrs.add(InetUtil.parseAddrPort(addr));
					} catch (Exception e) {
						System.err.println("Invalid IP socket address '" + addr + "' in "
								+ Midi2OscUtil.FN_OSC_SERVERS + "[" + iServer + "].");
					}
				}
				if (ipaddrs.isEmpty()) {
					System.err.println("No valid IP addresses for OSC server '" + name + "' in "
							+ Midi2OscUtil.FN_OSC_SERVERS + "[" + iServer + "].");
					continue;
				}
				int connectTimeoutMS = (int)serverConfig.getNumber(Midi2OscUtil.FN_CONNECT_TIMEOUT_MS,
												defTimeout);
				int reconnectWaitMS = (int)serverConfig.getNumber(Midi2OscUtil.FN_RECONNECT_WAIT_MS,
												defReconnectWait);
				if (reconnectWaitMS > 0 && reconnectWaitMS < Midi2OscUtil.MIN_RECONNECT_WAIT_MS) {
					reconnectWaitMS = Midi2OscUtil.MIN_RECONNECT_WAIT_MS;
				}
				boolean show = serverConfig.getBoolean(Midi2OscUtil.FN_SHOW, defShow);
				boolean showResponses = serverConfig.getBoolean(Midi2OscUtil.FN_SHOW_RESPONSES, defShowResponses);
				boolean sim = serverConfig.getBoolean(Midi2OscUtil.FN_SIM, defSim);
				if (sim) {
					show = true;
				}
				int newValueWaitMS = (int)serverConfig.getNumber(Midi2OscUtil.FN_NEW_VALUE_WAIT_MS,
												defNewValueWaitMS);
				int sameValueSkipMS = (int)serverConfig.getNumber(Midi2OscUtil.FN_SAME_VALUE_SKIP_MS,
												defSameValueSkipMS);
				lastServer = new ServerInfo(name, ipaddrs, connectTimeoutMS,reconnectWaitMS,
													show, showResponses, sim,
													newValueWaitMS, sameValueSkipMS);
				m_serverMap.put(name, lastServer);
				List<String> unknown = serverConfig.findInvalidKeys(
						List.of(Midi2OscUtil.FN_NEW_VALUE_WAIT_MS,
								Midi2OscUtil.FN_SAME_VALUE_SKIP_MS,
								Midi2OscUtil.FN_CONNECT_TIMEOUT_MS,
								Midi2OscUtil.FN_RECONNECT_WAIT_MS,
								Midi2OscUtil.FN_SHOW,
								Midi2OscUtil.FN_SHOW_RESPONSES,
								Midi2OscUtil.FN_SIM,
								Midi2OscUtil.FN_NAME,
								Midi2OscUtil.FN_IPADDRS),
						List.of("^" + Midi2OscUtil.FN_COMMENT_PREFIX + ".*"));
				if (unknown != null) {
					System.err.println("WARNING: Unrecognized fields in \""
									+ Midi2OscUtil.FN_OSC_SERVERS + "\": " + unknown);
				}
			}
		}
		if (m_serverMap.isEmpty()) {
			throw new IllegalArgumentException("No OSC servers specified in "
								+ Midi2OscUtil.FN_OSC_SERVERS + ".");
		}
		m_defServer = (m_serverMap.size() == 1) ? lastServer : null;
	}
	
	public boolean contains(String server)
	{
		return m_serverMap.containsKey(server);
	}
	
	public ServerInfo getServerInfo(String server)
	{
		if (server == null || server.isBlank()) {
			return m_defServer;
		} else {
			return m_serverMap.get(server);
		}
	}
	
	public List<String> serverNames()
	{
		return new ArrayList<String>(m_serverMap.keySet());
	}

	@Override
	public Iterator<ServerInfo> iterator()
	{
		return m_serverMap.values().iterator();
	}

	@Override
	public String toString() {
		return "ServerConfig[" + m_serverMap + "]";
	}
}
