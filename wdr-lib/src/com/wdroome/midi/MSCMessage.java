package com.wdroome.midi;

import javax.sound.midi.SysexMessage;

/**
 * @author wdr
 */
public class MSCMessage
{
	// Separate classes for these? MSCCmdFormat, MSCCommand
	// enum for cmd formats, with name & number
	// enum with cmd types, with name & number
	
	public short m_deviceId = 0;
	public short m_cmdFormat = 0;	// XXX
	public short m_cmd = 0; // XXX
	public String m_qnum = null;
	public String m_qlist = null;
	public String m_qpath = null;
	
	public MSCTimeCode m_timeCode = null;
	public int m_controlNumber = 0;
	public int m_controlValue = 0;

	/**
	 * 
	 */
	public MSCMessage()
	{
		// TODO Auto-generated constructor stub
	}
	
	/*

	public static MSCMessage make(SysexMessage sysex, int[] deviceIds, int[] cmdFmts)
	{
		verify MSC
		verify cmdfmt
		verify deviceId
		parse & return
	}
	 */

}
