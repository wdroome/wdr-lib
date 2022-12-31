package com.wdroome.apps.eos2qlab;

import java.io.InputStream;
import java.io.PrintStream;

import com.wdroome.util.MiscUtil;

public class ReadResponse
{
	private final PrintStream m_out;
	private final InputStream m_in;
	
	public ReadResponse(InputStream in, PrintStream out)
	{
		m_out = (out != null) ? out : System.out;
		m_in = (in != null) ? in : System.in;		
	}
	
	public ReadResponse()
	{
		this(null, null);
	}
	
	public void println(String msg)
	{
		m_out.println(msg);
	}
	
	public void print(String msg)
	{
		m_out.print(msg);
	}
	
	public void flush()
	{
		m_out.flush();
	}
	
	public String getResponse(String msg)
	{
		if (msg != null && !msg.isBlank()) {
			m_out.print(msg);
			if (!msg.endsWith(" ")) {
				m_out.print(" ");
			} 
		}
		String line = MiscUtil.readLine(m_in);
		if (line == null) {
			return null;
		}
		return line.trim();
	}
	
	public Integer getIntResponse(String msg, int def, int min, int max)
	{
		while (true) {
			String respStr = getResponse(msg);
			if (respStr == null) {
				return null;
			} else if (respStr.isBlank() && def >= min && def <= max) {
				return def;
			} else if (Commands.isCmd(respStr, Commands.QUIT_CMD)) {
				return null;
			} else {
				try {
					int resp = Integer.parseInt(respStr);
					if (resp >= min && resp <= max) {
						return resp;
					}
					m_out.println("Enter a number between " + min + " and " + max);
				} catch (Exception e) {
					m_out.println("Enter a number between " + min + " and " + max);
				}
			}
		}
	}


}
