// Tab == 4 spaces.

 //================================================================
 //  Alcatel-Lucent provides the software in this file for the
 //  purpose of obtaining feedback.  It may be copied and modified.
 // 
 //  Alcatel-Lucent is not liable for any consequence of loading
 //  or running this software or any modified version thereof.
 //================================================================

package com.wdroome.util;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;

/**
 *	Define a common {@link Timer} object that can used by a variety of clients.
 *<p>
 *	You normally use the static "schedule()" methods. For example,
 *	the following runs the indicated code in 3 seconds:
 *<pre>
 *     CommonTimer.schedule(new TimerTask(){public void run(){...}}, 3000);
 *</pre>
 *	Because the timer is shared, the action routines should be designed to
 *	run quickly, preferably without blocking.
 */
public class CommonTimer
{
	// Timer is created on demand by getTimer().
	private static Timer g_timer = null;
	private static Boolean g_timerCreateLock = new Boolean(true);

	// Parameters for creating the Timer.
	private static boolean g_isDeamon = true;
	private static String g_threadName = CommonTimer.class.getName();

	/** Dummy no-args constructor. */
	private CommonTimer() {}

	/**
	 *	Return the common Timer.
	 */
	public static Timer getTimer()
	{
		if (g_timer == null) {
			synchronized (g_timerCreateLock) {
				if (g_timer == null) {
					g_timer = new Timer(g_threadName, g_isDeamon);
				}
			}
		}
		return g_timer;
	}

	/** Short for getTimer().schedule(task, time). */
	public static void schedule(TimerTask task, Date time)
		{ getTimer().schedule(task, time); }

	/** Short for getTimer().schedule(task, delay). */
	public static void schedule(TimerTask task, long delay)
		{ getTimer().schedule(task, delay); }

	/** Short for getTimer().schedule(task, delay, period). */
	public static void schedule(TimerTask task, long delay, long period)
		{ getTimer().schedule(task, delay, period); }

	/** Short for getTimer().schedule(task, firstTime, period). */
	public static void schedule(TimerTask task, Date firstTime, long period)
		{ getTimer().schedule(task, firstTime, period); }

	/** Short for getTimer().scheduleAtFixedRate(task, delay, period). */
	public static void scheduleAtFixedRate(TimerTask task, long delay, long period)
		{ getTimer().scheduleAtFixedRate(task, delay, period); }

	/** Short for getTimer().scheduleAtFixedRate(task, firstTime, period). */
	public static void scheduleAtFixedRate(TimerTask task, Date firstTime, long period)
		{ getTimer().scheduleAtFixedRate(task, firstTime, period); }
}
