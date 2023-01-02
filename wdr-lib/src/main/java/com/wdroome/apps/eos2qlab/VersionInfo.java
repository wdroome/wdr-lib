package com.wdroome.apps.eos2qlab;

import java.io.IOException;
import java.io.PrintStream;

import com.wdroome.osc.eos.QueryEOS;
import com.wdroome.osc.qlab.QueryQLab;

/**
 * Information about the EOS and QLab versions.
 * @author wdr
 */
public class VersionInfo
{
	public static final String UNAVAILABLE_VERSION = "???";
	public static final int UNAVAILABLE_MAJOR_VERSION = -1;
	
	private String m_eosVersion = UNAVAILABLE_VERSION;
	private int m_eosMajorVersion = UNAVAILABLE_MAJOR_VERSION;
	private String m_qlabVersion = UNAVAILABLE_VERSION;
	private int m_qlabMajorVersion = UNAVAILABLE_MAJOR_VERSION;

	/**
	 * Get the EOS & QLab versions. 
	 * @param queryEOS EOS connection or null.
	 * @param queryQLab QLab connection, or null.
	 * @param out If not null, print a warning if either version isn't supported.
	 */
	public VersionInfo(QueryEOS queryEOS, QueryQLab queryQLab, PrintStream out)
	{
		refreshVersion(queryEOS);
		refreshVersion(queryQLab);
		if (out != null) {
			String warn = untestedVersionWarning();
			if (warn != null) {
				out.println(warn);
			}
		}
	}
	
	public void refreshVersion(QueryQLab queryQLab)
	{
		if (queryQLab != null) {
			m_qlabVersion = queryQLab.getVersion();
			m_qlabMajorVersion = queryQLab.getMajorVersion();			
		} else {
			m_qlabVersion = UNAVAILABLE_VERSION;
			m_qlabMajorVersion = UNAVAILABLE_MAJOR_VERSION;
		}
	}
	
	public void refreshVersion(QueryEOS queryEOS)
	{
		if (queryEOS != null) {
			try {
				m_eosVersion = queryEOS.getVersion();
				m_eosMajorVersion = Integer.parseInt(m_eosVersion.replaceAll("\\..*$", ""));
			} catch (IOException e) {
				m_eosVersion = UNAVAILABLE_VERSION;
				m_eosMajorVersion = UNAVAILABLE_MAJOR_VERSION;
			}
		} else {
			m_eosVersion = UNAVAILABLE_VERSION;
			m_eosMajorVersion = UNAVAILABLE_MAJOR_VERSION;
		}
	}
	
	/**
	 * If either major version is untested, return a warning string.
	 * If ok, return null.
	 * @return An "untested version" warning or null.
	 */
	public String untestedVersionWarning()
	{
		StringBuilder b = new StringBuilder();
		String sep = "Untested Version(s): ";
		if (m_eosMajorVersion != UNAVAILABLE_MAJOR_VERSION
				&& m_eosMajorVersion != 3) {
			b.append(sep + "eos:" + m_eosVersion);
			sep = ",";
		}
		if (m_qlabMajorVersion != UNAVAILABLE_MAJOR_VERSION
				&& (m_qlabMajorVersion != 4 && m_qlabMajorVersion != 5)) {
			b.append(sep + "qlab:" + m_qlabVersion);
			sep = ",";
		}
		return !b.isEmpty() ? b.toString() : null;
	}
	
	public boolean isQLab4() { return m_qlabMajorVersion == 4; }
	
	public boolean isQLab5() { return m_qlabMajorVersion == 5; }
	
	public boolean isQLab4or5() { return m_qlabMajorVersion == 4 || m_qlabMajorVersion == 5; }
	
	public boolean isEOS3() { return m_eosMajorVersion == 3; }
	
	@Override
	public String toString()
	{
		return "Versions(EOS=" + m_eosVersion + "," + "QLab=" + m_qlabVersion + ")";
	}

	public String getEosVersion() {
		return m_eosVersion;
	}

	public int getEosMajorVersion() {
		return m_eosMajorVersion;
	}

	public String getQLabVersion() {
		return m_qlabVersion;
	}

	public int getQLabMajorVersion() {
		return m_qlabMajorVersion;
	}
}
