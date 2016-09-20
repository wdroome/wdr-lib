package com.wdroome.midi;

import java.util.ArrayList;
import java.io.PrintStream;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;

import uk.co.xfactorylibrarians.coremidi4j.CoreMidiDeviceProvider;
import uk.co.xfactorylibrarians.coremidi4j.CoreMidiNotification;
import uk.co.xfactorylibrarians.coremidi4j.CoreMidiException;

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
		
		// Return the preferred description of the device.
		@Override
		public String toString() { return m_cookedDescription; }
		
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
	}
	
	/**
	 * Get the working MIDI devices.
	 * @return An array with the working MIDI devices.
	 */
	public static Device[] getDevices()
	{
		ArrayList<Device> devices = new ArrayList<>();
        for (javax.sound.midi.MidiDevice.Info info: CoreMidiDeviceProvider.getMidiDeviceInfo()) {
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
        if (g_coreMidi4jLoaded) {
            System.out.println("CoreMIDI4J native library is running.");
        } else {
            System.out.println("CoreMIDI4J native library is not available.");
        }
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
