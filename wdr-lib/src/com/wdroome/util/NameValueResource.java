package com.wdroome.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Map;
import java.util.HashMap;

/**
 * Get Name/Value pairs from a resource file, usually in a class jar file.
 * The resource consists of lines of the form "name: value".
 * A line that starts with a blank is a continuation of the previous value.
 * <p>
 * This class is a {@link Map}, where the keys are the build-item names
 * and the values are their values. The c'tor reads the name/value pairs
 * and saves them in the Map. After that, clients can access the
 * Map methods to get the names and values.
 * 
 * @see Class#getResource(String)
 * @author wdr
 */
public class NameValueResource extends HashMap<String,String>
{
	private static final long serialVersionUID = -550419960018462380L;
	
	private static final String NAME_VALUE_SEP = ": ";
	
	/**
	 * Read items from a build-info resource and save them in the Map.
	 * @param resource The name of the build-info resource.
	 * 		If this resource does not exist,
	 * 		or if the parameter is null or "",
	 *		quietly create an empty map.
	 */
	public NameValueResource(String resource)
	{
		super();
		if (resource == null || resource.equals("")) {
			return;
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
		NameValueResource buildInfo = new NameValueResource(args.length > 0 ? args[0] : null);
		for (Map.Entry<String,String> ent: buildInfo.entrySet()) {
			System.out.println(ent.getKey() + ": '" + ent.getValue() + "'");
		}
	}
}
