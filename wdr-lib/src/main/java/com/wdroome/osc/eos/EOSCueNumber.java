package com.wdroome.osc.eos;

import java.util.ArrayList;
import java.util.Collections;

/**
 * An EOS cue number. Can also be used for groups and other numbers between 0.001 and 9999.999.
 * @author wdr
 */
public class EOSCueNumber implements Comparable<EOSCueNumber>
{
	private final int m_cuelist;
	private final int m_scaledNumber;	// Cue number multiplied by 10 ** SCALE_EXPONENT
	private final String m_stringNumber;	// Cue number as string
	private final int m_part;
	
	private static final int SCALE_EXPONENT = 3;
	private static final int SCALE_FACTOR = (int)Math.pow(10, SCALE_EXPONENT);
	private static final String FRAC_FMT = "%0" + SCALE_EXPONENT + "d";
	
	/**
	 * Create a Cue Number from a string.
	 * This is more permissive than EOS. For example, it accepts .123.
	 * But the c'tor converts the number into the proper EOS format.
	 * @param cueNumber The string.
	 * @throws NumberFormatException If cueNumber isn't parsable as a cue number.
	 */
	public EOSCueNumber(String cueNumber)
	{
		this(EOSUtil.DEFAULT_CUE_LIST, cueNumber, 0);
	}
	
	/**
	 * Create a Cue Number from a string.
	 * This is more permissive than EOS. For example, it accepts .123.
	 * But the c'tor converts the number into the proper EOS format.
	 * @param cueNumber The string.
	 * @throws NumberFormatException If cueNumber isn't parsable as a cue number.
	 */
	public EOSCueNumber(int cuelist, String cueNumber, int part)
	{
		try {
			int iSlash = cueNumber.indexOf('/');
			if (iSlash > 0) {
				cuelist = Integer.parseInt(cueNumber.substring(0, iSlash));
			}
			cueNumber = cueNumber.substring(iSlash+1);
		} catch (Exception e) {
			//???
		}
		try {
			int iPart = cueNumber.indexOf('p');
			if (iPart < 0) {
				iPart = cueNumber.indexOf('P');
			}
			if (iPart > 0) {
				part = Integer.parseInt(cueNumber.substring(iPart+1));
				cueNumber = cueNumber.substring(0, iPart);
			}
		} catch (Exception e) {
			//???
		}
		m_cuelist = cuelist;
		m_part = part;
		cueNumber = cueNumber.trim();
		int iDot = cueNumber.indexOf('.');
		if (iDot < 0) {
			m_scaledNumber = Integer.parseInt(cueNumber) * SCALE_FACTOR;
		} else {
			double d = Double.parseDouble(cueNumber);
			m_scaledNumber = (int)(d * SCALE_FACTOR);
		}
		int intPart = m_scaledNumber / SCALE_FACTOR;
		int fracPart = m_scaledNumber % SCALE_FACTOR;
		if (fracPart == 0) {
			m_stringNumber = "" + intPart;
		} else {
			StringBuilder buff = new StringBuilder(String.format(FRAC_FMT, fracPart));
			for (int len = SCALE_EXPONENT; len > 0 && buff.charAt(len-1) == '0'; --len) {
				buff.setLength(len-1);
			}
			m_stringNumber = intPart + "." + buff.toString();
		}
	}
	
	/**
	 * Get the cue list number for this cue.
	 * @return The cue list number for this cue.
	 */
	public int getCuelist()
	{
		return m_cuelist;
	}
	
	/**
	 * Get the cue-number part of the cue, as a string.
	 * @return The cue-number part of the cue, as a string.
	 */
	public String getCueNumber()
	{
		return m_stringNumber;
	}
	
	/**
	 * Test if this is a part in a multipart cue.
	 * That is, the part number is greater than 0.
	 * @return True iff this is a part in a multipart cue.
	 */
	public boolean isPart()
	{
		return m_part > 0;
	}
	
	/**
	 * Get the part number of this cue. The "base part" is number 0.
	 * @return This cue's part number, or 0 is this is the base part.
	 */
	public int getPartNumber()
	{
		return m_part;
	}

	/**
	 * Compare two cue numbers.
	 */
	@Override
	public int compareTo(EOSCueNumber o)
	{
		int cmp = Integer.compare(m_cuelist, o.m_cuelist);
		if (cmp != 0) {
			return cmp;
		}
		cmp = Integer.compare(m_scaledNumber, o.m_scaledNumber);
		if (cmp != 0) {
			return cmp;
		}
		return Integer.compare(m_part,  o.m_part);
	}

	/**
	 * Return the cue number portion, without cuelist or part number.
	 */
	@Override
	public String toString()
	{
		return m_stringNumber;
	}
	
	/**
	 * Return the full cue number, with "cuelist/" prefix and "p#" suffix if not part 0.
	 * @return The full cue number.
	 */
	public String toFullString()
	{
		return m_cuelist + "/" + m_stringNumber + (m_part > 0 ? ("p" + m_part) : "");
	}

	@Override
	public int hashCode()
	{
		return m_scaledNumber;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		if (m_scaledNumber != ((EOSCueNumber)obj).m_scaledNumber)
			return false;
		return true;
	}
	
	/**
	 * For testing, create a CueNumber for each argument. Then sort the numbers and compare each pair.
	 * @param args
	 */
	public static void main(String[] args)
	{
		ArrayList<EOSCueNumber> cueNums = new ArrayList<>();
		for (String arg: args) {
			try {
				EOSCueNumber cueNum = new EOSCueNumber(arg);
				System.out.println(arg + " => " + cueNum.toFullString());
				cueNums.add(cueNum);
			} catch (Exception e) {
				System.out.println(arg + " => " + e);
			}
		}
		Collections.sort(cueNums);
		for (EOSCueNumber a: cueNums) {
			System.out.println();
			for (EOSCueNumber b: cueNums) {
				System.out.println(a.toFullString() + "<>" + b.toFullString() + ": " + a.compareTo(b));
			}
		}
	}
}
