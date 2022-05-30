package com.wdroome.osc;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import java.util.List;
import java.util.ArrayList;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.wdroome.util.CommandReader;
import com.wdroome.util.MiscUtil;
import com.wdroome.json.JSONLexan;
import com.wdroome.json.JSONParseException;
import com.wdroome.json.JSONParser;
import com.wdroome.json.JSONValue_String;
import com.wdroome.json.JSONWriter;
import com.wdroome.json.JSONValue_Object;
import com.wdroome.json.JSONValue_Array;
import com.wdroome.json.JSONValue_ObjectArray;
import com.wdroome.json.JSONValueTypeException;

/**
 * Read OSC commands from an input source, send them to the OSC server, and display the responses.
 * @author wdr
 */
public class OSCCmdReader extends CommandReader implements OSCConnection.MessageHandler
{
	private final List<OSCMessage> m_responses = new ArrayList<>();
	
	private final AtomicReference<OSCConnection> m_conn = new AtomicReference<>(null);
	private String m_addrPort;
	private boolean m_streamResponses = false;
	private AtomicBoolean m_showJson = new AtomicBoolean(false);
	
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
		m_addrPort = addrPort;
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
		m_addrPort = null;
		if (args.length >= 1) {
			m_addrPort = args[0];
		}
		start();
	}

	/**
	 * Create an OSCConnection to the OSC server.
	 * @param addrPort The ipaddr:port for the server.
	 * @throws IOException 
	 */
	private void makeConnection(String addrPort) throws IOException
	{
		OSCConnection conn = new OSCConnection(addrPort) {
			@Override public void logError(String msg) {
				m_out.println("   ***" + msg);
			}
		};
		conn.setMessageHandler(this);
		conn.connect();
		// conn.setConnectTimeout(m_connTimeout);
		// conn.setReadTimeout(m_readTimeout);
		m_conn.set(conn);
	}

	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run()
	{
		try {
			makeConnection(m_addrPort);
		} catch (IOException e) {
			m_out.println("*** IO Error connecting to OSC " + m_addrPort + ": " + e);
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
				m_out.println("   If arg ends in /i32, /i64, /f or /d, send as a number.");
				m_out.println("stream    ## Show responses when they arrive.");
				m_out.println("nostream  ## Save responses instead of streaming them.");
				m_out.println("resp      ## Show saved responses.");
				m_out.println("clear     ## Clear saved responses.");
				m_out.println("reconnect ## Reconnect after disconnection.");
				m_out.println("json      ## Print responses as JSON if they look like json.");
				m_out.println("nojson    ## Don't print responses as JSON.");
				m_out.println("quit      ## Bye-bye.");
			} else if (cmd.equals("send")) {
				if (!(parsedCmd.length >= 2)) {
					m_out.println("Usage: send osc-method [arg arg ...]");
					continue;
				} else {
					sendCmd(parsedCmd, 1);
				}
			} else if (cmd.startsWith("recon")) {
				if (m_conn.get() != null) {
					m_out.println("Already connected to " + m_addrPort);
				} else {
					try {
						makeConnection(m_addrPort);
					} catch (IOException e) {
						m_out.println("*** IO Error connecting to OSC " + m_addrPort + ": " + e);						
					}
				}
			} else if (cmd.startsWith("/")) {
				sendCmd(parsedCmd, 0);
			} else if (cmd.startsWith("resp")) {
				synchronized (m_responses) {
					for (OSCMessage resp: m_responses) {
						printResponse(resp, "  ");
					}
				}
			} else if (cmd.equals("clear")) {
				synchronized (m_responses) {
					m_responses.clear();
				}
			} else if (cmd.equals("stream")) {
				setStreamResponses(true);
			} else if (cmd.equals("nostream")) {
				setStreamResponses(false);
			} else if (cmd.equals("json")) {
				m_showJson.set(true);
			} else if (cmd.equals("nojson")) {
				m_showJson.set(false);
			} else if (cmd.equals("quit") || cmd.equals("q")) {
				break;
			} else {
				m_out.println("Unknown command '" + cmd + "'");
			}
		}
	}
	
	private void setStreamResponses(boolean streamResponses)
	{
		synchronized (m_responses) {
			for (OSCMessage resp: m_responses) {
				printResponse(resp, "");
			}
			m_responses.clear();
			m_streamResponses = streamResponses;
		}
	}

	private void sendCmd(String[] parsedCmd, int cmdIndex)
	{
		Object[] args = new Object[parsedCmd.length - cmdIndex - 1];
		for (int i = cmdIndex + 1; i < parsedCmd.length; i++) {
			String[] argParts = parsedCmd[i].split("/");
			Object arg = parsedCmd[i];
			if (argParts.length == 2) {
				if (argParts[1].equals("i") || argParts[1].equals("i32")) {
					try {
						arg = Integer.valueOf(argParts[0]);
					} catch (Exception e) {
						// fall thru, treat as string arg.
					}
				} else if (argParts[1].equals("i32")) {
					try {
						arg = Long.valueOf(argParts[0]);
					} catch (Exception e) {
						// fall thru, treat as string arg.
					}
				} else if (argParts[1].equals("f")) {
					try {
						arg = Float.valueOf(argParts[0]);
					} catch (Exception e) {
						// fall thru, treat as string arg.
					}
				} else if (argParts[1].equals("d")) {
					try {
						arg = Double.valueOf(argParts[0]);
					} catch (Exception e) {
						// fall thru, treat as string arg.
					}
				}
			}
			args[i-cmdIndex-1] = arg;
		}
		OSCMessage msg = new OSCMessage(parsedCmd[cmdIndex], args);
		int preNumResp = getNumResponses();
		try {
			OSCConnection conn = m_conn.get();
			if (conn != null) {
				conn.sendMessage(msg);
			} else {
				m_out.println("*** Not connected to OSC client");
				return;
			}
		} catch (IOException e) {
			m_out.println("*** IO Error: " + e);
			return;
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
	
	private void printResponse(OSCMessage msg, String prefix)
	{
		if (prefix == null) {
			prefix = "";
		}
		if (m_showJson.get() && msg.getArgs().size() == 1) {
			String arg = msg.getString(0, "");
			if (arg.startsWith("[") || arg.startsWith("{")) {
				try {
					JSONValue_Object jsonArg = JSONParser.parseObject(new JSONLexan(arg), true);
					m_out.println(prefix + "OSCMessage [" + msg.getCreateTS()
											+ "," + msg.getMethod() + "," + msg.getArgTypes() + "]:");
					JSONWriter writer = new JSONWriter(m_out);
					writer.setSorted(true);
					writer.setIndents("   ", "   ");
					jsonArg.writeJSON(writer);
					writer.writeNewline();
					return;
				} catch (Exception e) {
					// Fall true and print as string.
				}
			}
		}
		m_out.println(prefix + msg.toString());
	}

	/* (non-Javadoc)
	 * @see OSCConnection.MessageHandlerr#handleOscResponse(OSCMessage)
	 */
	@Override
	public void handleOscResponse(OSCMessage msg)
	{
		synchronized (m_responses) {
			if (m_streamResponses) {
				if (msg == null) {
					m_out.println(" *** resp: OSC client disconnected.");
				} else {
					printResponse(msg, " *** resp: ");
				}
			} else if (msg != null) {
				m_responses.add(msg);
			}
		}
		if (msg == null) {
			OSCConnection conn = m_conn.get();
			if (conn != null) {
				conn.disconnect();
				m_conn.set(null);
			}
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
