package com.wdroome.midi;

import java.util.ArrayList;

import javax.sound.midi.*;

/**
 * Attempt to verify that Sysex messages work properly.
 * Unfortunately, I cannot get this class to send
 * sysex messages at all.
 * @author wdr
 */
public class VerifySysex
{
	/**
	 * @param args
	 * @throws MidiUnavailableException 
	 * @throws InvalidMidiDataException 
	 * @throws ArrayIndexOutOfBoundsException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args)
			throws MidiUnavailableException, ArrayIndexOutOfBoundsException,
					InvalidMidiDataException, InterruptedException
	{
		ArrayList<MidiTools.Device> trans = new ArrayList<>();
		ArrayList<MidiTools.Device> rcvr = new ArrayList<>();
		for (MidiDevice.Info info: MidiTools.getMidiDeviceInfo()) {
        	try {
        		MidiTools.Device d = new MidiTools.Device(MidiSystem.getMidiDevice(info));
        		if (d.m_isPort && d.m_isTransmitter) {
        			trans.add(d);
        		}
        		if (d.m_isPort && d.m_isReceiver) {
        			rcvr.add(d);
        		}
        	} catch (Exception e) {
        		// Ignore bad info.
        	}
        }
		for (MidiTools.Device d: trans) {
			System.out.println("T: " + d.m_cookedDescription);
			d.setReceiver(new Rcvr(d.m_cookedDescription));
		}
		MSCMessage msc = new MSCMessage();
		msc.m_deviceId = 0;
		msc.m_cmdFormat = MSCCmdFormat.LIGHTING_GENERAL;
		msc.m_cmd = MSCCommand.GO;
		msc.m_qnum = "1";
		SysexMessage sysex = msc.makeSysexMsg();
		MSCMessage msctest = MSCMessage.make(sysex, null, null);
		if (msctest == null) {
			System.out.println("OOPS!!!");
		} else {
			System.out.println("Regen msc: " + msc);
		}
		ShortMessage noteon = new ShortMessage(ShortMessage.NOTE_ON, 3, 10, 20);
		SysexMessage xsysex = new SysexMessage();
		for (MidiTools.Device d: rcvr) {
			System.out.println("R: " + d.m_cookedDescription);
			System.out.println("Sending MSC message to " + d.m_cookedDescription);
			d.send(sysex, System.currentTimeMillis());
			d.send(noteon, System.currentTimeMillis());
			d.send(xsysex, System.currentTimeMillis());
			Thread.sleep(1000);
		}
		Thread.sleep(10000);
		System.out.println("Done");
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
				MSCMessage msc = MSCMessage.make((SysexMessage)m, null, null);
				if (msc != null) {
					System.out.print(msc);
				} else {
					System.out.print(String.format("Sysex/%x:", m.getStatus()));
					for (byte b: ((SysexMessage)m).getData()) {
						System.out.print(String.format(" %x", (int)(b & 0xff)));
					}
				}
				System.out.println(" @ " + ts);
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
