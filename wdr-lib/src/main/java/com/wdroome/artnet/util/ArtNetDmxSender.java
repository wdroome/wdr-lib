package com.wdroome.artnet.util;

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.ArrayList;

import java.io.IOException;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

import com.wdroome.artnet.ArtNetUniv;
import com.wdroome.artnet.ArtNetChannel;
import com.wdroome.artnet.msgs.ArtNetDmx;

/**
 * A simple thread to send ArtNetDmx messages with levels for several universes.
 * Note that efficiency is not a concern. THe client sets the message frequency,
 * but it's expected to be much less than 40.
 */
public class ArtNetDmxSender extends Thread
{
	private static final int MAX_FREQ = 40;
	
	private final AtomicInteger m_freq = new AtomicInteger(0);
	private final Map<ArtNetUniv, byte[]> m_levels = new HashMap<>();
	private final AtomicBoolean m_running = new AtomicBoolean(true);
	
	private final ArtNetChannel m_channel;
	private final boolean m_isPrivateChannel;
	
	/**
	 * Create and start a new DMX sender thread.
	 * @param channel Send messages via this channel. If null, create a private channel
	 * 		and close it when done.
	 * @throws IOException If we cannot create a private channel.
	 */
	public ArtNetDmxSender(ArtNetChannel channel) throws IOException
	{
		if (channel != null) {
			m_channel = channel;
			m_isPrivateChannel = false;
		} else {
			m_channel = new ArtNetChannel();
			m_isPrivateChannel = true;
		}
		start();
	}
	/**
	 * Return the message transmit frequency (msgs/sec).
	 * @return The transmit frequency (msgs/sec).
	 */
	public int getFreq() { return m_freq.get(); }
	
	/**
	 * Set the message transmit frequency.
	 * @param freq The frequency, in msgs/sec. 0 or negative pauses sending.
	 * @return The previous frequency.
	 */
	public int setFreq(int freq)
	{
		if (freq > MAX_FREQ) {
			freq = MAX_FREQ;
		}
		int prevValue = m_freq.get();
		m_freq.set(freq);
		return prevValue;
	}
	
	/**
	 * Shutdown & stop the thread.
	 */
	public void shutdown() { m_running.set(false); }
	
	/**
	 * Return the current levels for all universes. Note this returns copies.
	 * It's expected this will not be called in a tight loop.
	 */
	public Map<ArtNetUniv,byte[]> getLevels()
	{
		Map<ArtNetUniv,byte[]> levelsCopy = new HashMap<>();
		synchronized(m_levels)
		{
			for (ArtNetUniv univ: m_levels.keySet()) {
				byte[] univLevels = m_levels.get(univ);
				if (univLevels != null) {
					levelsCopy.put(univ, Arrays.copyOf(univLevels, univLevels.length));
				}
			}
		}
		return levelsCopy;
	}
	
	/**
	 * Set levels for a block of channels. Previously set levels
	 * for other channels remain the same.
	 * @param univ The universe.
	 * @param startChan Start channel number (1-512), inclusive.
	 * @param endChan Last channel number (1-512), inclusive.
	 * @param level The new level.
	 */
	public void setLevels(ArtNetUniv univ, int startChan, int endChan, int level)
	{
		if (univ == null) {
			throw new IllegalArgumentException("ArtNetDmxSender: null universe.");
		}
		synchronized(m_levels)
		{
			byte[] univLevels = m_levels.get(univ);
			if (univLevels == null) {
				univLevels = new byte[512];
				m_levels.put(univ, univLevels);
			}
			if (level < 0) {
				level = 0;
			} else if (level > 255) {
				level = 255;
			}
			if (startChan <= endChan
					&& startChan >= 1 && startChan <= 512
					&& endChan >= 1 && endChan <= 512) {
				for (int chan = startChan; chan <= endChan; chan++) {
					univLevels[chan-1] = (byte)level;
				}
			} else {
				throw new IllegalArgumentException("ArtNetDmxSender: invalid start/end channels "
						+ startChan + "-" + endChan);
			}
		}
	}
	
	public void run()
	{
		Map<ArtNetUniv, ArtNetDmx> workingMsgs = new HashMap<>();
		int nErrors = 0;
		try {
			while (m_running.get()) {
				int freq = m_freq.get();
				if (freq > 0) {
					synchronized (m_levels) {
						for (ArtNetUniv univ : m_levels.keySet()) {
							ArtNetDmx msg = workingMsgs.get(univ);
							if (msg == null) {
								msg = new ArtNetDmx();
								msg.m_net = univ.m_net;
								msg.m_subUni = univ.subUniv();
								workingMsgs.put(univ, msg);
							}
							byte[] levels = m_levels.get(univ);
							if (levels == null) {
								levels = new byte[512];
							}
							msg.m_data = Arrays.copyOf(levels, levels.length);
							msg.m_dataLen = levels.length;
						}
					}
					for (ArtNetDmx msg: workingMsgs.values()) {
						try {
							msg.incrSeqn();
							if (!m_channel.broadcast(msg)) {
								System.err.println("ArtNetDmxSender: error b'casting");
								nErrors++;
							}
						} catch (IOException e) {
							System.err.println("ArtNetDmxSender: IOException b'casting");
							nErrors++;
						}
					}
				}
				if (nErrors > 10) {
					System.err.println("ArtNetDmxSender shutting down, too many errors.");
					return;
				}
				try {
					Thread.sleep(freq > 0 ? (long)(1000.0/freq) : 500);
				} catch (InterruptedException e) {
					// Ignore this. Just wake up.
				}
			} 
		} finally {
			if (m_isPrivateChannel) {
				m_channel.shutdown();
			}
		}
	}
	
	/**
	 * Simple test sending fixed levels to a few universes.
	 * @param args Ignored.
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException
	{
		ArtNetDmxSender sender = new ArtNetDmxSender(null);
		sender.setLevels(new ArtNetUniv(0), 1, 3, 127);
		sender.setLevels(new ArtNetUniv(1), 4, 6, 255);
		sender.setFreq(2);
	}
}
