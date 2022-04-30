package com.wdroome.osc.eos;

import java.util.ArrayList;
import java.util.Collections;

/**
 * An EOS cue number. Can also be used for groups and other numbers between 0.001 and 9999.999.
 * @author wdr
 */
public class EOSCueNumber implements Comparable<EOSCueNumber>
{
	private final int m_scaledNumber;	// Cue number multiplied by 10 ** SCALE_EXPONENT
	private final String m_stringFmt;
	
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
			m_stringFmt = "" + intPart;
		} else {
			StringBuilder buff = new StringBuilder(String.format(FRAC_FMT, fracPart));
			for (int len = SCALE_EXPONENT; len > 0 && buff.charAt(len-1) == '0'; --len) {
				buff.setLength(len-1);
			}
			m_stringFmt = intPart + "." + buff.toString();
		}
	}

	/**
	 * Compare two cue numbers.
	 */
	@Override
	public int compareTo(EOSCueNumber o)
	{
		return Integer.compare(m_scaledNumber, o.m_scaledNumber);
	}

	/**
	 * Return the cue number in proper EOS format.
	 */
	@Override
	public String toString()
	{
		return m_stringFmt;
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
	 * FOr testing, create a CueNumber for each argument. Then sort the numbers and compare each pair.
	 * @param args
	 */
	public static void main(String[] args)
	{
		ArrayList<EOSCueNumber> cueNums = new ArrayList<>();
		for (String arg: args) {
			try {
				EOSCueNumber cueNum = new EOSCueNumber(arg);
				System.out.println(arg + " => " + cueNum);
				cueNums.add(cueNum);
			} catch (Exception e) {
				System.out.println(arg + " => " + e);
			}
		}
		Collections.sort(cueNums);
		for (EOSCueNumber a: cueNums) {
			System.out.println();
			for (EOSCueNumber b: cueNums) {
				System.out.println(a + "<>" + b + ": " + a.compareTo(b));
			}
		}
	}
}
