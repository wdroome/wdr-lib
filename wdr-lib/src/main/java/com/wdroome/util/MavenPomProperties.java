package com.wdroome.util;

import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;

/**
 * Get the pom properties -- version, artifactIf, etc -- of the Maven projects
 * included in the JVM's classpath.
 * 
 * @author wdr
 */
public class MavenPomProperties
{
	public static final String MAVEN_POM_PROP_FILE_SUFFIX = "/pom.properties";
	public static final String MAVEN_POM_PROP_VERSION = "version=";
	public static final String MAVEN_POM_PROP_GROUP_ID = "groupId=";
	public static final String MAVEN_POM_PROP_ARTIFACT_ID = "artifactId=";
	
	private final List<Props> m_props = new ArrayList<>();
	
	/**
	 * The pom properties for a Maven project in the classpath.
	 */
	public static class Props
	{
		/** Artifact ID. Not null or empty. */
		public final String m_artifactId;

		/** Group ID, or "". */
		public final String m_groupId;

		/** Version, or "". */
		public final String m_version;

		/** POM update date, or "". */
		public final String m_pomDate;
		
		public Props(String resourceName)
		{
			String groupId = "";
			String artifactId = "";
			String version = "";
			String date = "";
			
			InputStream istr = MiscUtil.class.getResourceAsStream(resourceName);
			if (istr != null) {
				try {
					String line;
					while ((line = MiscUtil.readLine(istr)) != null) {
						if (line.startsWith(MAVEN_POM_PROP_GROUP_ID)) {
							groupId = line.substring(MAVEN_POM_PROP_GROUP_ID.length());
						} else if (line.startsWith(MAVEN_POM_PROP_ARTIFACT_ID)) {
							artifactId = line.substring(MAVEN_POM_PROP_ARTIFACT_ID.length());
						} else if (line.startsWith(MAVEN_POM_PROP_VERSION)) {
							version = line.substring(MAVEN_POM_PROP_VERSION.length());
						} else if (isDate(line)) {
							date = line.substring(1);
						}
					} 
				} finally {
					try {istr.close();} catch (Exception e) {}
				}
			}
			m_groupId = groupId;
			m_artifactId = artifactId;
			m_version = version;
			m_pomDate = date;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((m_artifactId == null) ? 0 : m_artifactId.hashCode());
			result = prime * result + ((m_pomDate == null) ? 0 : m_pomDate.hashCode());
			result = prime * result + ((m_groupId == null) ? 0 : m_groupId.hashCode());
			result = prime * result + ((m_version == null) ? 0 : m_version.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Props other = (Props) obj;
			if (m_artifactId == null) {
				if (other.m_artifactId != null)
					return false;
			} else if (!m_artifactId.equals(other.m_artifactId))
				return false;
			if (m_pomDate == null) {
				if (other.m_pomDate != null)
					return false;
			} else if (!m_pomDate.equals(other.m_pomDate))
				return false;
			if (m_groupId == null) {
				if (other.m_groupId != null)
					return false;
			} else if (!m_groupId.equals(other.m_groupId))
				return false;
			if (m_version == null) {
				if (other.m_version != null)
					return false;
			} else if (!m_version.equals(other.m_version))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "Props [m_groupId=" + m_groupId + ", m_artifactId=" + m_artifactId + ", m_version=" + m_version
					+ ", m_pomDate=" + m_pomDate + "]";
		}

		private static final String[] g_dateLinePrefixes = new String[] {
				"#Mon ",
				"#Tue ",
				"#Wed ",
				"#Thu ",
				"#Fri ",
				"#Sat ",
				"#Sun ",
			};
		
		private static boolean isDate(String line)
		{
			for (String prefix: g_dateLinePrefixes) {
				if (line.startsWith(prefix)) {
					return true;
				}
			}
			return false;
		}
	}
	
	/**
	 * Get the pom properties of all Maven packages.
	 */
	public MavenPomProperties()
	{
		for (String f: MiscUtil.getClassPathFileNames()) {
			if (f.endsWith(MAVEN_POM_PROP_FILE_SUFFIX)) {
				if (!f.startsWith("/")) {
					f = "/" + f;
				}
				Props props = new Props(f);
				if (props.m_artifactId != null && !props.m_artifactId.isEmpty()) {
					m_props.add(props);
				}
			}
		}
	}
	
	/**
	 * Return a List of the pom proprties of all Maven packages
	 * in the classpath jar file(s).
	 * @return A List of the pom proprties of all Maven packages
	 * 			in the classpath jar file(s).
	 */
	public List<Props> getProps()
	{
		return new ImmutableList<Props>(m_props);
	}
	
	@Override
	public String toString() {
		return "MavenPomProperties [m_props=" + m_props + "]";
	}

	public static void main(String[] args)
	{
		MavenPomProperties pomProps = new MavenPomProperties();
		for (Props props: pomProps.getProps()) {
			System.out.println(props.m_groupId + "/" + props.m_artifactId
					+ ": ver=" + props.m_version + " pomDate=" + props.m_pomDate);
		}
	}
}
