package com.wdroome.apps.midi2osc;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import javax.sound.midi.Receiver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;

import com.wdroome.json.JSONFieldMissingException;
import com.wdroome.json.JSONLexan;
import com.wdroome.json.JSONParseException;
import com.wdroome.json.JSONParser;
import com.wdroome.json.JSONValueTypeException;
import com.wdroome.json.JSONValue_Array;
import com.wdroome.json.JSONValue_Object;
import com.wdroome.midi.MSCMessage;
import com.wdroome.midi.MidiTools;

public class Midi2Osc
{
	public static String DEF_CONFIG_FILE_NAME = "midi2osc-config.json";
	
	private String m_configFileName = null;
	private Parameters m_params = null;
	private Variables m_variables = null;
	private ServerConfig m_serverConfig = null;
	private ActionSpecMap m_actionSpecMap = null;
	
	private final Map<String, ServerConnection> m_senders = new HashMap<>();

	private boolean m_verbose = false;

	public static void main(String[] args)
	{
		System.out.println("*** Use control-C to stop. ***");
		new Midi2Osc(args);
	}
	
	public Midi2Osc(String[] args)
	{
		ArrayList<Parameters.NameValue> paramOverrides = new ArrayList<>();
		String configFileName = DEF_CONFIG_FILE_NAME;
		for (String arg: args) {
			int iEquals;
			if (arg.startsWith("-") && (iEquals = arg.indexOf("=")) > 0) {
				paramOverrides.add(new Parameters.NameValue(arg.substring(1,iEquals), arg.substring(iEquals+1)));
			} else {
				configFileName = arg;
			}
		}
		m_configFileName = configFileName;
		try {
			JSONValue_Object config = JSONParser.parseObject(new JSONLexan(new File(m_configFileName)), true);
			m_params = new Parameters(config.getObject(Midi2OscUtil.FN_PARAMS, null), paramOverrides);
			m_verbose = m_params.isVerbose();
			m_serverConfig = new ServerConfig(config.getArray(Midi2OscUtil.FN_OSC_SERVERS, null), m_params);
			m_variables = new Variables(config.getObject(Midi2OscUtil.FN_TRANSFORMS, null));
			m_actionSpecMap = new ActionSpecMap(config.getArray(Midi2OscUtil.FN_ACTIONS),
												m_serverConfig, m_variables);
			if (m_verbose) {
				System.out.println(m_params.toString());
				System.out.println("OSC Server Info:");
				for (String serverName: m_serverConfig.serverNames()) {
					System.out.println("  " + m_serverConfig.getServerInfo(serverName));
				}
				m_actionSpecMap.printMap(System.out);
			}
		} catch (Exception e) {
			System.err.println("Error in configration file \"" + m_configFileName + "\":");
			System.err.println(e);
			return;
		}
		
		for (ServerInfo serverInfo: m_serverConfig) {
			ServerConnection serverConn = new ServerConnection(serverInfo, m_params, m_variables);
			serverConn.start();
			m_senders.put(serverInfo.m_name, serverConn);
		}
		
        for (MidiTools.Device device : MidiTools.getDevices()) {
			if (m_verbose) {
				System.out.println(" " + device.m_cookedDescription + ":" + (device.m_isPort ? " port" : "")
						+ (device.m_isTransmitter ? " transmitter" : "")
						+ (device.m_isReceiver ? " receiver" : ""));
			}
			if (device.m_isPort && device.m_isTransmitter) {
				try {
					System.out.println("  Listening to MIDI controller \"" + device.m_cookedDescription + "\".");
					device.setReceiver(new Rcvr(device.m_cookedDescription));
				} catch (MidiUnavailableException e) {
					// Shouldn't happen ... BUT ...
					System.err.println("Midi2Osc: Cannot create Receiver for '"
							+ device.m_cookedDescription + "': " + e);
				}
			}
        }
	}
	
	public Midi2Osc(String configFileName)
			throws JSONParseException, JSONValueTypeException,
					JSONFieldMissingException, IOException
	{
		this(new String[] {configFileName});
	}

	private class Rcvr implements Receiver
	{
		private String m_controllerName;
		
		private Rcvr(String controllerName)
		{
			m_controllerName = controllerName;
			if (m_verbose) {
				System.out.println("Start MidiMessage Receiver: " + m_controllerName);
			}
		}
		
		@Override
		public void send(MidiMessage m, long arriveTS)
		{
			if (m instanceof ShortMessage) {
				ShortMessage sm = (ShortMessage)m;
				if (m_verbose) {
					System.out.println(" \"" + m_controllerName + "\": MidiMsg(" + sm.getChannel() + ","
										+ MidiEventType.typeOf(sm) + ","
										+ sm.getData1() + "," + sm.getData2()
										+ ")");
				}
				Action action = m_actionSpecMap.findAction(m_controllerName, sm, System.currentTimeMillis());
				if (action != null) {
					if (m_verbose) {
						System.out.println("    " + action);
					}
					ServerConnection serverConn = m_senders.get(action.m_actionSpec.m_oscServer.m_name);
					if (serverConn != null) {
						serverConn.addActionToQueue(action);
					} else {
						// OOPS! No server with that name. Shouldn't happen.
						System.err.println("*** Action for undefined server \""
										+ action.m_actionSpec.m_oscServer + "\"");
					}
				}
			}
		}
		
		@Override
		public void close()
		{
			System.out.println(m_controllerName + ": closed");
		}
	}
}
