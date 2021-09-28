package com.wdroome.osc;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import java.util.List;
import java.util.ArrayList;

import com.wdroome.util.CommandReader;
import com.wdroome.util.MiscUtil;

/**
 * Read OSC commands from an input source, send them to the OSC server, and display the responses.
 * @author wdr
 */
public class OSCCmdReader extends CommandReader implements OSCConnection.MessageHandler
{
	private final OSCConnection m_conn;
	private final List<OSCMessage> m_responses = new ArrayList<>();
	
	/**
	 * Create a client. This c'tor starts the thread.
	 * @param addrPort The address:port for the OSC server.
	 * @param in The command input stream.
	 * @param out The output stream.
	 * @param givePrompt If true, prompt for the next comand.
	 */
	public OSCCmdReader(String addrPort, InputStream in, PrintStream out, boolean givePrompt)
	{
		super(in, out, givePrompt);
		m_conn = makeConnection(addrPort);
		start();
	}

	/**
	 * Create a client using command line arguments. Usage:
	 *     OSCCmdReader ipaddr:port
	 * @param args
	 */
	public OSCCmdReader(String[] args)
	{
		super();
		String addrPort = null;
		if (args.length >= 1) {
			addrPort = args[0];
		}
		m_conn = makeConnection(addrPort);
		start();
	}

	/**
	 * Create an OSCConnection to the OSC server.
	 * @param addrPort The ipaddr:port for the server.
	 * @return The connection.
	 */
	private OSCConnection makeConnection(String addrPort)
	{
		OSCConnection conn = new OSCConnection(addrPort) {
			@Override public void logError(String msg) {
				m_out.println("   ***" + msg);
			}
		};
		conn.setMessageHandler(this);
		// conn.setConnectTimeout(m_connTimeout);
		// conn.setReadTimeout(m_readTimeout);
		return conn;
	}

	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run()
	{
		try {
			m_conn.connect();
		} catch (IOException e) {
			m_out.println("*** IO Error connecting to OSC: " + e);
			return;
		}
		
		String[] parsedCmd;
		while ((parsedCmd = readCmd()) != null) {
			parsedCmd = deleteComments(parsedCmd);
			if (parsedCmd.length <= 0) {
				continue;
			}
			String cmd = parsedCmd[0];
				
			if (cmd.equals("help") || cmd.equals("?")) {
				m_out.println("send osc-method arg1 arg2 ...");
				m_out.println("resp    ## Show responses.");
				m_out.println("clear   ## Clear all responses.");
				m_out.println("quit    ## Bye-bye.");
			} else if (cmd.equals("send")) {
				if (!(parsedCmd.length >= 2)) {
					m_out.println("Usage: send osc-method [str-arg str-arg ...]");
					continue;
				}
				sendCmd(parsedCmd, 1);
			} else if (cmd.startsWith("/")) {
				sendCmd(parsedCmd, 0);
			} else if (cmd.startsWith("resp")) {
				synchronized (m_responses) {
					for (OSCMessage resp: m_responses) {
						System.out.println("  " + resp);
					}
				}
			} else if (cmd.equals("clear")) {
				synchronized (m_responses) {
					m_responses.clear();
				}
			} else if (cmd.equals("quit") || cmd.equals("q")) {
				break;
			} else {
				m_out.println("Unknown command '" + cmd + "'");
			}
		}
	}

	private void sendCmd(String[] parsedCmd, int cmdIndex)
	{
		Object[] args = new Object[parsedCmd.length - cmdIndex - 1];
		for (int i = cmdIndex + 1; i < parsedCmd.length; i++) {
			args[i-cmdIndex-1] = parsedCmd[i];
		}
		OSCMessage msg = new OSCMessage(parsedCmd[cmdIndex], args);
		int preNumResp = getNumResponses();
		try {
			m_conn.sendMessage(msg);
		} catch (IOException e) {
			m_out.println("*** IO Error: " + e);
		}
		MiscUtil.sleep(250);
		int postNumResp = getNumResponses();
		if (postNumResp != preNumResp) {
			m_out.println("   " + (postNumResp - preNumResp) + " new responses.");
		}
	}
	
	private int getNumResponses()
	{
		synchronized (m_responses) {
			return m_responses.size();
		}
	}
	
	/**
	 * Strip comments from a parsed command line.
	 * Specifically, find the first array element
	 * that starts with "#" or "//", and return a new
	 * new array with just the elements before that element.
	 * If no array element starts with "#" or "//",
	 * return the input array.
	 * @param tokens
	 * 		An array representing the arguments of a command.
	 * @return
	 * 		The non-comment arguments.
	 * 		If there are no non-comment arguments,
	 * 		return a 0-length array rather than null.
	 */
	private String[] deleteComments(String[] tokens)
	{
		for (int nKeep = 0; nKeep < tokens.length; nKeep++) {
			String s = tokens[nKeep];
			if (s.startsWith("#") || s.startsWith("//")) {
				String[] ret = new String[nKeep];
				for (int i = 0; i < nKeep; i++) {
					ret[i] = tokens[i];
				}
				return ret;
			}
		}
		return tokens;
	}

	/* (non-Javadoc)
	 * @see OSCConnection.MessageHandlerr#handleOscResponse(OSCMessage)
	 */
	@Override
	public void handleOscResponse(OSCMessage msg)
	{
		// m_out.println("*** OSC response msg arrived: " + msg.toString());
		synchronized (m_responses) {
			m_responses.add(msg);
		}
	}

	/**
	 * Run the OSCCmdReader with the command line arguments.
	 * @param args Normally ipaddr:port
	 */
	public static void main(String[] args)
	{
		new OSCCmdReader(args);
	}
}
