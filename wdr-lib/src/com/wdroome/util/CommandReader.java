package com.wdroome.util;

import java.io.InputStream;
import java.io.PrintStream;
import java.io.FileNotFoundException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.util.Queue;
import java.util.LinkedList;

/**
 *	Base class for a "manager" that reads commands from an InputStream.
 *	The base class extends Thread, and provides methods for reading
 *	commands from the user and writing responses to the user.
 *	The derived class must define run(). Typically that method
 *	loops over a call to readCmd().
 */
public abstract class CommandReader extends Thread
{
	protected boolean m_givePrompt = true;
	protected InputStream m_in = System.in;
	protected boolean m_inEOF = false;
	protected PrintStream m_out = System.out;
	protected boolean m_waitFlag = false;
	
	private Queue<String> m_initialCmds = new LinkedList<String>();
		
	/** Create a CommandReader. */
	public CommandReader(InputStream in, PrintStream out, boolean givePrompt)
	{
		this.m_in = in;
		this.m_out = out;
		this.m_givePrompt = givePrompt;
	}
	
	/** Create a basic CommandReader: stdin, stdout, and prompting. */
	public CommandReader() { this(System.in, System.out, true); }
	
	/**
	 *	Create a CommandReader from command-line args. Usage:
	 *<pre>
	 *    [-cmd=initial-cmd] [-noprompt] [-wait] [input-file-name [output-file-name]]
	 *</pre>
	 *	If no args, read stdin and write to stdout, and give a prompt.
	 *	A "-" for a file name means use stdin or out.
	 *	If input is stdin, we give a prompt unless you specify -noprompt.
	 *	The -cmd= arguments give commands to run before reading commands
	 *	from stdin or a file.
	 *	If output-file-name starts with "+", strip the "+" and append to the file.
	 *	-wait means wait until the thread has finished reading the commands
	 *	and has stopped. You might use this to read initialization commands at startup.
	 *	If -wait is set, set the wait flag in the object. After starting the reader,
	 *	use waitForEOF() to wait for the reader to finish.
	 */
	public CommandReader(String[] args)
	{
		if (getState() != Thread.State.NEW) {
			System.err.println("ComamndReader.processMainArgs(): Must call method before starting thread.");
			return;
		}
		if (args == null) {
			args = new String[0];
		}
		m_givePrompt = true;
		m_waitFlag = false;
		String inFile = "-";
		String outFile = "-";
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-noprompt")) {
				m_givePrompt = false;
			} else if (args[i].equals("-wait")) {
				m_waitFlag = true;
			} else if (args[i].startsWith("-cmd=")) {
				m_initialCmds.add(args[i].substring(5));
			} else {
				inFile = args[i];
				if (i+1 <= args.length-1) {
					outFile = args[i+1];
				}
			}
		}
		if (inFile.equals("-")) {
			m_in = System.in;
		} else {
			try {
				m_in = new BufferedInputStream(new FileInputStream(inFile));
				m_givePrompt = false;
			} catch (FileNotFoundException e) {
				System.err.println("CommandReader.main(): Can't open " + inFile
							+ " for reading: " + e.getMessage());
				return;
			}
		}
		if (outFile.equals("-")) {
			m_out = System.out;
		} else {
			try {
				boolean append = false;
				if (outFile.startsWith("+")) {
					append = true;
					outFile = outFile.substring(1);
				}
				m_out = new PrintStream(new FileOutputStream(outFile, append));
			} catch (FileNotFoundException e) {
				System.err.println("CommandReader.main(): Can't open " + outFile
							+ " for writing: " + e.getMessage());
				return;
			}
		}
	}
	
	/**
	 *	Start the Thread. If the wait flag is set, wait for the thread to finish.
	 */
	protected void startAndWaitForCompletion()
	{
		start();
		if (m_waitFlag) {
			while (true) {
				try {
					join();
					return;
				} catch (Exception e) {
					// System.out.println("Join ex " + e.getMessage());
				}
			}
		}
	}
	
	private StringBuilder lastCmdLine = null;
	private static final String SPLIT_ARGS_REGEX = "[ \t\r\n]+";

	/**
	 *	Read a command from input, trim leading/trailing whitespace,
	 *	and return array with blank-sep tokens.
	 *	If a line ends with "\\", automatically read the next line.
	 *	For EOF, return null.
	 *	For a blank line, return a zero-length array.
	 */
	protected String[] readCmd()
	{
		if (!m_initialCmds.isEmpty()) {
			String line = m_initialCmds.remove();
			if (!line.equals("")) {
				return line.split(SPLIT_ARGS_REGEX);
			} else {
				return new String[0];
			}
		}

		if (m_inEOF) {
			return null;
		}

		lastCmdLine = new StringBuilder(100);
		boolean readNextLine = true;
		while (readNextLine) {
			synchronized (this) {
				if (m_givePrompt && m_out != null) {
					m_out.print(lastCmdLine.length() == 0 ? "* " : "> ");
					m_out.flush();
				}
			}
			String line = MiscUtil.readLine(m_in);
			if (line == null) {
				m_inEOF = true;
				break;
			}
			if (line.endsWith("\\")) {
				line = line.substring(0, line.length()-1);
			} else {
				readNextLine = false;
			}
			lastCmdLine.append(line);
		}
		String fullLine = lastCmdLine.toString().trim();
		if (!fullLine.equals("")) {
			return fullLine.split(SPLIT_ARGS_REGEX);
		} else if (m_inEOF) {
			return null;
		} else {
			return new String[0];
		}
	}
	
	/**
	 *	Return the arguments of last command read by readCmd() as one String.
	 */
	public String lastCmdArgs()
	{
		if (lastCmdLine == null) {
			return "";
		} else {
			String[] x = lastCmdLine.toString().split(SPLIT_ARGS_REGEX, 2);
			if (x != null && x.length >= 2)
				return x[1];
			else
				return "";
		}
	}
	
	/**
	 *	If cmd[iArg] exists and is a valid boolean spec, return it as a Boolean.
	 *	If there is no such arg, return null.
	 */
	protected Boolean getBoolArg(String[] cmd, int iArg)
	{
		if (iArg >= cmd.length) {
			return null;
		} else {
			String v = cmd[iArg].toLowerCase();
			if (v.equals("t") || v.equals("true") || v.equals("1")) {
				return new Boolean(true);
			} else if (v.equals("f") || v.equals("false") || v.equals("0")) {
				return new Boolean(false);
			} else if (v.equals("-") || v.equals("")) {
				return null;
			} else {
				warn("Please use true (t) or false (f).");
				return null;
			}
		}
	}
		
	/** Display warning message to user. */
	public synchronized void warn(String m)
	{
		if (m_out != null && m_givePrompt)
			m_out.println(m);
	}
	
	/** Display informational message to user. */
	public synchronized void info(String m)
	{
		if (m_out != null)
			m_out.println(m);
	}
}
