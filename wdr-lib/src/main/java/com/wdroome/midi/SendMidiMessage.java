package com.wdroome.midi;

import java.util.ArrayList;

import javax.sound.midi.*;

/**
 * Utility class to send a midi message.
 * @author wdr
 */
public class SendMidiMessage
{
	/**
	 * @param args Command line arguments: cc|on|off data1 data2 [channel].
	 * @throws MidiUnavailableException If an error occurs.
	 * @throws InvalidMidiDataException  If an error occurs.
	 * @throws ArrayIndexOutOfBoundsException  If an error occurs.
	 * @throws InterruptedException  If an error occurs.
	 */
	public static void main(String[] args)
			throws MidiUnavailableException, ArrayIndexOutOfBoundsException,
					InvalidMidiDataException, InterruptedException
	{
		if (args.length < 3) {
			System.out.println("Usage: SendMidiMessage cc|on|off data1 data2 [channel]");
			System.exit(1);
		}
		ArrayList<MidiTools.Device> rcvr = new ArrayList<>();
		for (MidiDevice.Info info: MidiTools.getMidiDeviceInfo()) {
        	try {
        		MidiTools.Device d = new MidiTools.Device(MidiSystem.getMidiDevice(info));
        		if (d.m_isPort && d.m_isReceiver) {
        			rcvr.add(d);
        		}
        	} catch (Exception e) {
        		// Ignore bad info.
        	}
        }
		int cmd;
		String type = args[0].toLowerCase();
		if (type.equals("on")) {
			cmd = ShortMessage.NOTE_ON;
		} else if (type.equals("off")) {
			cmd = ShortMessage.NOTE_OFF;
		} else {
			cmd = ShortMessage.CONTROL_CHANGE;
		}
		int data1 = Integer.parseInt(args[1]);
		int data2 = Integer.parseInt(args[2]);
		int chan = (args.length >= 4) ? Integer.parseInt(args[3]) : 0;
		ShortMessage msg = new ShortMessage(cmd, chan, data1, data2);
	
		for (MidiTools.Device d: rcvr) {
			System.out.println("R: " + d.m_cookedDescription);
			System.out.println("Sending message to " + d.m_cookedDescription);
			d.send(msg, System.currentTimeMillis());
		}
		Thread.sleep(1000);
	}
}
