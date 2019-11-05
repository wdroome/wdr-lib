package com.wdroome.util.inet;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

/**
 * Utility command to reduce a set of CIDRs
 * by combining adjacent CIDRs and removing overlapping CIDRs.
 * @author wdr
 */
public class CIDRCombiner
{

	/**
	 * If any CIDRs in a set can be combined or removed,
	 * output the reduced set in numerical orde, one per line.
	 * If not, just output the incoming CIDRs in numerical order. 
	 * @param args
	 * 		The CIDRs to combine.
	 * 		An argument may have more than one CIDR separated by white space or commas.
	 * 		"-" means read CIDRs from stdin. An input line may have more than one CIDR
	 * 		separated by white space or commas. Ignore blank lines and lines starting with "#".
	 * @throws IOException On I/O error reading standard input (shouldn't happen).
	 */
	public static void main(String[] args) throws IOException
	{
		CIDRSet cidrSet = new CIDRSet();
		
		if (args == null || args.length == 0) {
			args = new String[] {"-"};
		}
		for (String cidrs: args) {
			if (cidrs.equals("-")) {
				readStdin(cidrSet);
			} else {
				try {
					cidrSet.addCidrs(cidrs);
				} catch (UnknownHostException e) {
					System.err.println(e.getMessage());
				}
			}
		}
		int nInputCidrs = cidrSet.size();
		if (!cidrSet.freeze(true)) {
			System.out.println(nInputCidrs + " CIDRs in: No CIDRs can be combined.");
		} else {
			System.out.println(nInputCidrs + " CIDRs in => " + cidrSet.size() + " CIDRs out");
		}
		CIDRAddress[] cidrArray = cidrSet.getCIDRs();
		Arrays.sort(cidrArray, new CIDRAddress.AddressByteComparator());
		for (CIDRAddress cidr: cidrArray) {
			System.out.println("\t" + cidr);
		}
	}

	private static void readStdin(CIDRSet cidrSet) throws IOException
	{
		LineNumberReader rdr = new LineNumberReader(new InputStreamReader(System.in));
		String line;
		while ((line = rdr.readLine()) != null) {
			line = line.trim();
			if (line.equals("") || line.startsWith("#")) {
				continue;
			}
			cidrSet.addCidrs(line);
		}
	}
}
