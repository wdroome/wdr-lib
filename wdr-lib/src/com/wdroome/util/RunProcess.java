package com.wdroome.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import java.util.List;
import java.util.ArrayList;

/**
 * Run a process, capture it's output, and wait for it to complete.
 * This assumes the output is text (e.g., lines of strings).
 * For convenience, this merges standard error with standard output,
 * and returns them as one stream.
 * @author wdr
 */
public class RunProcess
{
	private final ArrayList<String> m_output;
	private final int m_exitCode;

	/**
	 * Start the process, read its output, and wait for it to complete.
	 * @param cmdArray The command name and its arguments.
	 * @param dir If not null, run the command in this directory.
	 * @throws IOException If the process could not be started.
	 */
	public RunProcess(String[] cmdArray, File dir)
			throws IOException
	{
		this(new ArrayToList<String>(cmdArray), dir);
	}
	
	/**
	 * Start the process, read its output, and wait for it to complete.
	 * @param cmdArray The command name and its arguments.
	 * @param dir If not null, run the command in this directory.
	 * @throws IOException If the process could not be started.
	 */
	public RunProcess(List<String> cmdArray, File dir)
			throws IOException
	{
		ProcessBuilder builder = new ProcessBuilder(cmdArray);
		if (dir != null) {
			builder.directory(dir);
		}
		builder.redirectErrorStream(true);
		Process proc = builder.start();  

		BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		m_output = new ArrayList<String>();
		String line;
		while ((line = in.readLine()) != null) {
			m_output.add(line);
		}
		while (proc.isAlive()) {
			try { proc.waitFor(); } catch (Exception e) {}
		}
		m_exitCode = proc.exitValue();
	}
	
	/**
	 * Return the lines of process's combined standard output and standard error.
	 * @return The process's combined standard output and standard error.
	 * 		Never returns null, but the List may be empty.
	 */
	public List<String> getOutput()
	{
		return m_output;
	}
	
	/**
	 * Return the first output line.
	 * @param def The default value if there is no output.
	 * @return The first output line, or def if none.
	 */
	public String getFirstLine(String def)
	{
		return m_output.size() >= 1 ? m_output.get(0) : def;
	}
	
	/**
	 * Return the process's exit code.
	 * @return The process's exit code.
	 */
	public int getExitCode()
	{
		return m_exitCode;
	}

	/**
	 * For testing, run the command in the arguments and print the results.
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException
	{
		RunProcess rp = new RunProcess(args, null);
		System.out.println("Exit code: " + rp.getExitCode());
		System.out.println("\nOutput:\n" + rp.getOutput());
		System.out.println("\nFirst Line: " + rp.getFirstLine("[DEF]"));
	}

}
