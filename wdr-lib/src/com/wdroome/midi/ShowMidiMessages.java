package com.wdroome.midi;

import javax.sound.midi.Receiver;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;

/**
 * Listen to the messages sent by all MIDI ports,
 * and print them when they arrive.
 * Test class.
 * @author wdr
 */
public class ShowMidiMessages
{
	/**
	 * Listen to the messages sent by all MIDI ports,
	 * and print them when they arrive.
	 * @param args
	 */
	public static void main(String[] args)
	{
        try {
            System.out.println("MIDI Devices:");
            for (MidiTools.Device device : MidiTools.getDevices()) {
				System.out.println(" " + device.m_cookedDescription + ":"
							+ (device.m_isPort ? " port" : "")
							+ (device.m_isTransmitter ? " transmitter" : "")
							+ (device.m_isReceiver ? " receiver" : ""));
				if (device.m_isPort && device.m_isTransmitter) {
					device.setReceiver(new Rcvr(device.m_cookedDescription));
				}
            }
            Thread.sleep(60*60*1000);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
	}

	private static class Rcvr implements Receiver
	{
		private String m_name;
		
		private Rcvr(String name) {m_name = name;}
		
		@Override
		public void send(MidiMessage m, long ts)
		{
			System.out.print(m_name + ": ");
			if (m instanceof ShortMessage) {
				ShortMessage sm = (ShortMessage)m;
				String cmdName;
				switch (sm.getCommand()) {
				case ShortMessage.NOTE_ON: cmdName = "NOTE_ON"; break;
				case ShortMessage.NOTE_OFF: cmdName = "NOTE_OFF"; break;
				case ShortMessage.CONTROL_CHANGE: cmdName = "CC"; break;
				case ShortMessage.PITCH_BEND: cmdName = "CC"; break;
				case ShortMessage.PROGRAM_CHANGE: cmdName = "CC"; break;
				default: cmdName = "0x" + Integer.toHexString(sm.getCommand()); break;
				}
				System.out.println(cmdName
						+ " status=" + Integer.toHexString(m.getStatus())
						+ " data=" + sm.getData1() + " " + sm.getData2()
						+ " chan=" + sm.getChannel()
						+ " @ " + ts);
			} else if (m instanceof SysexMessage) {
				System.out.print(String.format("Sysex/%x:", m.getStatus()));
				for (byte b: ((SysexMessage)m).getData()) {
					System.out.print(String.format(" %x", (int)(b & 0xff)));
				}
				System.out.println(" @ " + ts);
				MSCMessage msc = MSCMessage.make((SysexMessage)m, null, null);
				if (msc != null) {
					System.out.println("    " + msc);
				}
			} else {
				byte[] msg = m.getMessage();
				System.out.print(String.format("Other/%x:", m.getStatus()));
				for (byte b: msg) {
					System.out.print(String.format(" %x", (int)(b & 0xff)));
				}
				System.out.println(" @ " + ts);
			}
		}
		
		@Override
		public void close()
		{
			System.out.println(m_name + ": closed");
		}
	}
}
