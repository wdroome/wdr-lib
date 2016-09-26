package com.wdroome.midi;

import java.util.ArrayList;
import java.io.PrintStream;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;

import uk.co.xfactorylibrarians.coremidi4j.CoreMidiDeviceProvider;

/**
 * Common static methods for handling MIDI messages.
 * @author wdr
 */
public class MidiTools
{
	private static final String CORE4J_NAME_PREFIX = "CoreMIDI4J - ";
	
	private static final boolean g_coreMidi4jLoaded;
	
	static {
		boolean loaded = false;
		try {
            Class.forName(CoreMidiDeviceProvider.class.getName());
            loaded = true;
		} catch (Exception e) {
			loaded = false;
		} catch (Error e) {
			loaded = false;
		}
		g_coreMidi4jLoaded = loaded;
	}

	/**
	 * Information about a MIDI device.
	 */
	public static class Device
	{
		// The device itself.
		public final MidiDevice m_device;
	
		// The preferred name or description of the device.
		// This is derived from MidiDevice.Info.getDescription(),
		// but may be different.
		public final String m_cookedDescription;
		
		// True iff this device is a port (not sequencer or synth).
		public final boolean m_isPort;
		
		// True iff this device can transmit incoming MIDI messages to the program.
		public final boolean m_isTransmitter;
		
		// True iff this device can receive outgoing MIDI messages from the program.
		public final boolean m_isReceiver;
		
		private Transmitter m_inTransmitter = null;
		
		private Receiver m_outReceiver = null;
		
		/**
		 * Create a new instance from a MidiDevice.
		 * @param device The underlying device (retained in m_device).
		 */
		public Device(MidiDevice device)
		{
			m_device = device;
			m_cookedDescription = getDescription(m_device);
			boolean isPort = true;
			if (device instanceof Synthesizer) {
				isPort = false;
			}
			if (device instanceof Sequencer) {
				isPort = false;
			}
			m_isPort = isPort;
			m_isTransmitter = device.getMaxTransmitters() != 0;
			m_isReceiver = device.getMaxReceivers() != 0;
		}
		
		// Return the preferred description of the device.
		@Override
		public String toString() { return m_cookedDescription; }
		
		/**
		 * Specify an incoming-message receiver
		 * for the messages which this device sends.
		 * receiver.send() will be called whenever this device
		 * sends a message.
		 * Any previous receiver will be removed.
		 * @param receiver The receiver.
		 * @throws MidiUnavailableException
		 * 		If something goes wrong.
		 */
		public synchronized void setReceiver(Receiver receiver)
					throws MidiUnavailableException
		{
			if (m_inTransmitter != null) {
				m_inTransmitter.close();
				m_inTransmitter = null;
			}
			m_device.open();
			m_inTransmitter = m_device.getTransmitter();
			m_inTransmitter.setReceiver(receiver);
		}
		
		/**
		 * Return true iff an incoming-message receiver has been set. 
		 * @return True iff an incoming-message receiver has been set.
		 */
		public synchronized boolean isOpenReceive()
		{
			return m_inTransmitter != null;
		}
		
		/**
		 * Close and remove the incoming-message receiver.
		 * Quietly return if there was no receiver.
		 */
		public synchronized void closeReceiver()
		{
			if (m_inTransmitter != null) {
				m_inTransmitter.close();
				m_inTransmitter = null;
			}
		}
		
		/**
		 * Send a MIDI message.
		 * @param msg The message.
		 * @param timestamp the timestamp.
		 * @throws MidiUnavailableException
		 * 		If something goes wrong.
		 */
		public synchronized void send(MidiMessage msg, long timestamp)
				throws MidiUnavailableException
		{
			if (m_outReceiver == null) {
				m_outReceiver = m_device.getReceiver();
			}
			m_device.open();
			m_outReceiver.send(msg, timestamp);
		}
		
		/**
		 * Return true iff the device has been open for sending messages.
		 * @return True iff the device has been open for sending messages.
		 */
		public synchronized boolean isOpenTransmitter()
		{
			return m_outReceiver != null;
		}
		
		/**
		 * Close the device for sending messages.
		 * Quietly return if it was not open.
		 */
		public synchronized void closeTransmitter()
		{
			if (m_outReceiver != null) {
				m_outReceiver.close();
				m_outReceiver = null;
			}
		}
		
		/**
		 * Close the device, both for transmitting and receiving.
		 */
		public synchronized void close()
		{
			closeReceiver();
			closeTransmitter();
			m_device.close();
		}
	}
	
	/**
	 * Get the raw information for the working MIDI devices.
	 * This uses the CoreMidi4j extensions if they are available.
	 * If not, it uses the device information returned by MidiSystem.
	 * @return The raw information for the working MIDI devices.
	 */
	public static MidiDevice.Info[] getMidiDeviceInfo()
	{
		if (g_coreMidi4jLoaded) {
			return CoreMidiDeviceProvider.getMidiDeviceInfo();
		} else {
			return MidiSystem.getMidiDeviceInfo();
		}
	}
	
	/**
	 * Get the extended descriptions of the working MIDI devices.
	 * @return An array with the working MIDI devices.
	 */
	public static Device[] getDevices()
	{
		ArrayList<Device> devices = new ArrayList<>();
        for (MidiDevice.Info info: getMidiDeviceInfo()) {
        	try {
        		devices.add(new Device(MidiSystem.getMidiDevice(info)));
        	} catch (Exception e) {
        		// Ignore bad info.
        	}
        }
        return devices.toArray(new Device[devices.size()]);
	}
	
	/**
	 * If this is a Mac, and if the CoreMIDI4J sysex-patch package is not loaded,
	 * return a string warning the user about the problem.
	 * Return null if there is no problem.
	 */
	public static String checkSysexPatch()
	{
		if (!g_coreMidi4jLoaded
				&& System.getProperty("os.name", "").startsWith("Mac")) {
			return "WARNING: MIDI Sysex & Show Control Messages may not work properly."
					+ " If they don't, include the coremidi4j library"
					+ " in your classpath.";
		} else {
			return null;
		}
	}
	
	/**
	 * If this is a Mac, and if the CoreMIDI4J sysex-patch package is not loaded,
	 * print a string warning the user about the problem.
	 * Return true iff there is no problem.
	 * @param out Where the warning is to be printed. If null, use System.err.
	 * @return True iff a warning was printed.
	 * @see #checkSysexPatch()
	 */
	public static boolean warnSysexPatch(PrintStream out)
	{
		String msg = checkSysexPatch();
		if (msg == null) {
			return false;
		} else {
			if (out == null) {
				out = System.err;
			}
			out.println(msg);
			return true;
		}
	}
    
    /**
     * Return the original description for a MIDI device,
     * by removing the changes made by CoreMIDI4j.
     */
    private static String getDescription(MidiDevice dev)
    {
    	if (dev == null) {
    		return "";
    	}
    	MidiDevice.Info info = dev.getDeviceInfo();
    	if (info == null) {
    		return "";
    	}
    	String desc = info.getDescription();
    	if (desc == null) {
    		desc = "";
    	}
    	String name = info.getName();
    	if (name == null) {
    		name = "";
    	}
    	if (g_coreMidi4jLoaded && name.startsWith(CORE4J_NAME_PREFIX)) {
    		String suffix = name.substring(CORE4J_NAME_PREFIX.length());
    		if (suffix.length() > 0) {
    			if (desc.equals("")) {
    				desc = suffix;
    			} else if (!desc.equals(suffix) && !desc.endsWith(suffix)) {
    				desc += " " + suffix;
    			}
    		}
    	}
    	if (desc.equals("")) {
    		desc = name;
    	}
    	return desc;
    }
	
    /**
     * For testing, print a summary of all devices.
     */
    public static void main(String[] args) throws Exception
    {
        warnSysexPatch(System.out);
        try {
            System.out.println("MIDI Devices:");
            for (Device device : getDevices()) {
				System.out.println(" " + device.m_cookedDescription + ":"
							+ (device.m_isPort ? " port" : "")
							+ (device.m_isTransmitter ? " transmitter" : "")
							+ (device.m_isReceiver ? " receiver" : ""));
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }
}
