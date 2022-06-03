package com.wdroome.osc.qlab;

import java.util.List;
import java.io.IOException;
import java.util.ArrayList;

import com.wdroome.util.MiscUtil;

public class QLabRepeatTest {

	public static void main(String[] args)
	{
		if (!(args.length >= 1)) {
			System.err.println("Usage: QLabRepeatTest qlab-addr-port [cue-id]");
			return;
		}
		try (QueryQLab queryQLab = new QueryQLab(args[0])) {
			String cueId = (args.length >= 2 && !args[1].isBlank()) ? args[1] : getCueId(queryQLab);
			QLabCueType cueType = queryQLab.getType(cueId);
			QLabUtil.ColorName cueColor = queryQLab.getColorName(cueId);
			if (cueType == QLabCueType.UNKNOWN) {
				System.out.println("Bad cueID " + cueId);
				return;
			}
			System.out.println("Testing cueId " + cueId + " type " + cueType + " color " + cueColor);
			long delayBetweenTests = 1000;
			long minDelay = (args.length >= 3) ? Integer.parseInt(args[2]) : 0;
			long maxDelay = (args.length >= 4) ? Integer.parseInt(args[3]) : 250;
			long delayIncr = 25;
			boolean makeDifferent = false;
			
			System.out.println("Testing two /type calls with varying delays between the calls:");
			for (long delay = minDelay; delay <= maxDelay; delay += delayIncr) {
				MiscUtil.sleep(delayBetweenTests);
				long t0 = System.currentTimeMillis();
				QLabCueType type1 = queryQLab.getType(cueId, makeDifferent ? (delay + "/#1") : null);
				long t1 = System.currentTimeMillis();
				if (delay > 0) {
					MiscUtil.sleep(delay);
				}
				long t2 = System.currentTimeMillis();
				QLabCueType type2 = queryQLab.getType(cueId, makeDifferent ? (delay + "/#2") : null);
				long t3 = System.currentTimeMillis();
				System.out.println("  Delay " + delay + ": "
								+ "#1: " + (type1 == cueType ? "ok" : "err") + " " + (t1-t0) + " ms, "
								+ "#2: " + (type2 == cueType ? "ok" : "err") + " " + (t3-t2) + " ms");
			}
			
			System.out.println("Repeated /type /colorName calls with no delay between:");
			MiscUtil.sleep(delayBetweenTests);
			int nOk = 0;
			int nErr = 0;
			long okTotalMS = 0;
			long errTotalMS = 0;
			for (int i = 0; i < 10; i++) {
				long t0 = System.currentTimeMillis();
				QLabCueType type = queryQLab.getType(cueId);
				long t1 = System.currentTimeMillis();
				QLabUtil.ColorName color = queryQLab.getColorName(cueId);
				long t2 = System.currentTimeMillis();
				if (type == cueType) {
					nOk++;
					okTotalMS += t1 - t0;
				} else {
					nErr++;
					errTotalMS += t2 - t1;
					System.out.println("  type error on set " + i);
				}
				if (color == cueColor) {
					nOk++;
					okTotalMS += t1 - t0;
				} else {
					nErr++;
					errTotalMS += t2 - t1;
					System.out.println("  color error on set " + i);
				}
			}
			System.out.println("  OK: " + nOk + " requests"
						+ ((nOk > 0) ? ", " + (((double)okTotalMS)/nOk + " ms/req") : ""));
			System.out.println("  Err: " + nErr + " requests"
					+ ((nErr > 0) ? ", " + (((double)errTotalMS)/nErr + " ms/req") : ""));			
		} catch (Exception e) {
			System.err.println(e);
		}
	}
	
	private static String getCueId(QueryQLab queryQLab) throws IOException
	{
		List<QLabCuelistCue> allCues = queryQLab.getAllCueLists();
		return allCues.get(0).m_uniqueId;
	}
}
