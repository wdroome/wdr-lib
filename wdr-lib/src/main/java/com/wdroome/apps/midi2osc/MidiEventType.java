package com.wdroome.apps.midi2osc;

import javax.sound.midi.ShortMessage;

public enum MidiEventType
{
	ON, OFF, CC, PC, OTHER;
	
	/**
	 * Return the MidiEventType for a MIDI message.
	 * @param msg The midi message.
	 * @return The event type, or OTHER if not a type we handle.
	 */
	public static MidiEventType typeOf(ShortMessage msg)
	{
		switch (msg.getCommand()) {
		case ShortMessage.NOTE_ON: return ON;
		case ShortMessage.NOTE_OFF: return OFF;
		case ShortMessage.CONTROL_CHANGE: return CC;
		case ShortMessage.PROGRAM_CHANGE: return PC;
		default: return OTHER;
		}
	}
}
