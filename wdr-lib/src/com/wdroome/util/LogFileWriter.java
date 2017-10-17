package com.wdroome.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Create a moveable log file: if another process renames the log file,
 * automatically re-create the original file.
 * @author wdr
 */
public class LogFileWriter
{
	private static final long DEF_CHECK_INTVL_MS = 10000;
	
	private final File m_file;
	private FileWriter m_writer;
	private long m_lastCheckTS = 0;
	private long m_checkIntvlMS = DEF_CHECK_INTVL_MS;
	private final String m_endOfLine = System.lineSeparator();

	/**
	 * Create a log file writer.
	 * @param fileName The name of the log file.
	 * 		If it does not exist, create it. If it does exist, append to it.
	 * @throws IOException If we cannot create or open the file for writing.
	 */
	public LogFileWriter(String fileName) throws IOException
	{
		this(new File(fileName), true);
	}
	
	/**
	 * Create a log file writer.
	 * @param file The log file.
	 * 		If it does not exist, create it.
	 * @param append If true, append to the file if it exists.
	 * 		If false, truncate the file.
	 * @throws IOException If we cannot create or open the file for writing.
	 */
	public LogFileWriter(File file, boolean append) throws IOException
	{
		m_file = file;
		m_writer = new FileWriter(m_file, append);
		m_lastCheckTS = System.currentTimeMillis();		
	}

	/**
	 * Write and flush a line to the log file.
	 * @param line The line (without a line terminator).
	 * @return True if the write succeeded; false if there was an IO error.
	 */
	public boolean println(String line)
	{
		if (m_writer == null) {
			// File was closed.
			return false;
		}
		check();
		try {
			m_writer.write(line);
			m_writer.write(m_endOfLine);
			m_writer.flush();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Write and flush a formatted to the log file.
	 * @param format The format string (without a line terminator).
	 * @param args The arguments.
	 * @return True if the write succeeded; false if there was an IO error.
	 * @see String#format(String, Object...)
	 */
	public boolean formatln(String format, Object... args)
	{
		return println(String.format(format, args));
	}
	
	/**
	 * Close the log file. All subsequent writes will return false.
	 */
	public void close()
	{
		if (m_writer != null) {
			try {
				m_writer.close();
				m_writer = null;
			} catch (IOException e) {
			}
		}
	}
	
	/**
	 * Set the interval for testing if the file still exists.
	 * Default is 10 seconds (10,000 milliseconds).
	 * @param millis The file-check interval, in milliseconds.
	 */
	public void setCheckIntvl(long millis)
	{
		m_checkIntvlMS = millis;
	}
	
	/**
	 * Re-create the file if it no longer exists.
	 */
	private void check()
	{
		boolean needCheck;
		if (m_checkIntvlMS <= 0) {
			needCheck = true;
		} else {
			long curTS = System.currentTimeMillis();
			if ((curTS - m_lastCheckTS) < m_checkIntvlMS) {
				needCheck = false;
			} else {
				needCheck = true;
				m_lastCheckTS = curTS;
			}
		}
		if (needCheck && !m_file.exists()) {
			try {
				FileWriter newWriter = new FileWriter(m_file, true);
				m_writer.close();
				m_writer = newWriter;
			} catch (IOException e) {
				// OOPS! Cannot re-create file. Keep using current file.
			}
		}
	}
}
