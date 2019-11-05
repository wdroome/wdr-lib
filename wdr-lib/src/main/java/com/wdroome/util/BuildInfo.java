package com.wdroome.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Map;
import java.util.HashMap;

/**
 * Get "build info" from a resource file, usually in the class jar file.
 * The resource consists of lines of the form "name: value".
 * A line that starts with a blank is a continuation of the previous value.
 * <p>
 * This class is a {@link Map}, where the keys are the build-item names
 * and the values are their values. Use the base class methods to
 * access the items.
 * @author wdr
 */
public class BuildInfo extends HashMap<String,String>
{
	private static final long serialVersionUID = -550419960018462380L;

	/** The default name for the build-info resource. */
	public static final String DEF_BUILD_INFO_RESOURCE = "/WDR-LIB_MANIFEST_INFO.txt";
	
	private static final String NAME_VALUE_SEP = ": ";
	
	/**
	 * Read items from a build-info resource and save them in the Map.
	 * @param resource The name of the build-info resource.
	 * 		If null or "", use {@link #DEF_BUILD_INFO_RESOURCE}.
	 */
	public BuildInfo(String resource)
	{
		super();
		if (resource == null || resource.equals("")) {
			resource = DEF_BUILD_INFO_RESOURCE;
		}
		InputStream istr = getClass().getResourceAsStream(resource);
		if (istr == null) {
			return;
		}
		LineNumberReader rdr = new LineNumberReader(new InputStreamReader(istr));
		String line;
		String prevName = null;
		try {
			while ((line = rdr.readLine()) != null) {
				if (line.trim().equals("")) {
					prevName = null;
				} else if (line.startsWith(" ") || line.startsWith("\t")) {
					if (prevName != null) {
						String cur = get(prevName);
						if (cur != null) {
							cur += line.substring(1);
						}
						put(prevName, cur);
					}
				} else {
					int isep = line.indexOf(NAME_VALUE_SEP);
					if (isep <= 0) {
						prevName = null;
					} else {
						prevName = line.substring(0, isep);
						if (prevName.equals("Name")) {
							prevName = null;
						} else {
							put(prevName, line.substring(isep + NAME_VALUE_SEP.length()));
						}
					}
				}
			}
		} catch (IOException e) {
			// Ignore
		}
		rdr = null;
		try {istr.close();} catch (Exception e) {}
	}

	/**
	 * For testing, print all items in a build-info resource.
	 * @param args The resource name. If omitted, use {@link #DEF_BUILD_INFO_RESOURCE}.
	 */
	public static void main(String[] args)
	{
		BuildInfo buildInfo = new BuildInfo(args.length > 0 ? args[0] : null);
		for (Map.Entry<String,String> ent: buildInfo.entrySet()) {
			System.out.println(ent.getKey() + ": '" + ent.getValue() + "'");
		}
	}
}
