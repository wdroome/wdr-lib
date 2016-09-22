package com.wdroome.midi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;

/**
 * A MIDI Show Control command.
 * To send an MSC command, create an instance with the default c'tor,
 * set the appropriate data members,
 * call {@link #makeSysexMsg()} to create the corresponding SysexMessage,
 * and then send that message.
 * To decode an incoming MSC command,
 * when you get a SysexMessage which may contain an MSC command,
 * call the static method {@link #make(SysexMessage, int[], MSCCmdFormat[])
 * on that SysexMessage. If it contains a valid MSC command,
 * and if that command passes the filtering rules,
 * make() returns an instance describing the MSC command.
 * If not, make() returns null.
 * @author wdr
 */
public class MSCMessage
{
	public static final byte REAL_TIME_SYSEX_START = (byte)0x7F;
	public static final byte REAL_TIME_SYSEX_END = (byte)0xF7;
	public static final byte MSC_ID = (byte)0x02;
	
	public static final int ALL_CALL_DEVICE_ID = 0x7F;
	
	// Device id.
	public int m_deviceId = 0;
	
	// Command format,
	public MSCCmdFormat m_cmdFormat = MSCCmdFormat.RESERVED;
	
	// Command.
	public MSCCommand m_cmd = MSCCommand.RESERVED;
	
	// Cue number. Digits and "." only.
	public String m_qnum = null;

	// Cue list. Digits and "." only.
	public String m_qlist = null;

	// Cue path. Digits and "." only.
	public String m_qpath = null;
	
	// Time code.
	public MSCTimeCode m_timeCode = null;
	
	// Controller number for SET command.
	public int m_controlNumber = 0;
	
	// Controller value for SET command, or macro number for FIRE command.
	public int m_controlValue = 0;

	/**
	 * Create a new blank MSC command.
	 */
	public MSCMessage()
	{
		// Just use the default values.
	}
	
	@Override
	public String toString()
	{
		StringBuilder b = new StringBuilder(100);
		b.append("MSCMessage{");
		b.append(m_cmdFormat.getName());
		b.append("/");
		b.append(m_cmd.getName());
		b.append(":");
		b.append("dev=");
		b.append(m_deviceId);
		if (m_qnum != null) {
			b.append(",qnum=" + m_qnum);
		}
		if (m_qlist != null) {
			b.append(",qlist=" + m_qlist);
		}
		if (m_qpath != null) {
			b.append(",qpath=" + m_qpath);
		}
		if (m_timeCode != null) {
			b.append(",tc=" + m_timeCode);
		}
		if (m_cmd == MSCCommand.SET) {
			b.append(",control=" + m_controlNumber + "/" + m_controlValue);
		} else if (m_cmd == MSCCommand.FIRE) {
			b.append(",macro=" + m_controlValue);
		}
		b.append("}");
		return b.toString();
	}
	
	/**
	 * Create and return a new MSCMessage with the MSC command data in a SysexMessage.
	 * Return null if the message is not an MSC command,
	 * or if it does not match the filtering tests.
	 * @param sysex A Sysex message, presumably containing an MSC command.
	 * @param myDeviceIds
	 * 		If not null, return a new MSCMessage only if
	 * 		the device id is one of those ids. If null,
	 * 		return a new MSCMessage for any device id.
	 * 		If the incoming device id is ALL_CALL_DEVICE_ID,
	 *		return a new MSCMessage regardless of the myDeviceIds. 
	 * @param myCmdFmts
	 * 		If not null, return a new MSCMessage only if
	 * 		the command format is one of those formats. If null,
	 * 		return a new MSCMessage for any command format.
	 * 		If the incoming command format is MSCCmdFormat.ALL_TYPES,
	 *		return a new MSCMessage regardless of the myCmdFmts. 
	 * @return
	 * 		The MSC command data, or null.
	 */
	public static MSCMessage make(SysexMessage sysex, int[] myDeviceIds, MSCCmdFormat[] myCmdFmts)
	{
		if (sysex == null) {
			return null;
		}
		byte[] data = sysex.getData();
		if (data == null) {
			return null;
		}
		int len = data.length;
		if (len < 6) {
			return null;
		}
		if (sysex.getStatus() != SysexMessage.SYSTEM_EXCLUSIVE
			|| data[0] != REAL_TIME_SYSEX_START
			|| data[2] != MSC_ID) {
			return null;
		}
		int deviceId = data[1] & 0xff;
		if (!isMyDeviceId(deviceId, myDeviceIds)) {
			return null;
		}
		MSCCmdFormat cmdFmt = isMyCmdFmt(data[3] & 0xff, myCmdFmts);
		if (cmdFmt == null) {
			return null;
		}
		MSCCommand cmd = MSCCommand.fromCode(data[4] & 0xff);
		if (cmd == null || cmd == MSCCommand.RESERVED) {
			return null;
		}
		MSCMessage msg = new MSCMessage();
		msg.m_deviceId = deviceId;
		msg.m_cmdFormat = cmdFmt;
		msg.m_cmd = cmd;
		
		ScanInfo scan = new ScanInfo();
		scan.data = data;
		scan.len = len;
		scan.next = 5;
		switch (cmd) {
		case GO:
		case STOP:
		case RESUME:
		case LOAD:
		case GO_OFF:
		case GO_JAM_CLOCK:
			msg.m_qnum = scanQstr(scan);
			msg.m_qlist = scanQstr(scan);
			msg.m_qpath = scanQstr(scan);
			break;
		case TIMED_GO:
			msg.m_timeCode = scanTimeCode(scan);
			if (msg.m_timeCode == null) {
				return null;
			}
			msg.m_qnum = scanQstr(scan);
			msg.m_qlist = scanQstr(scan);
			msg.m_qpath = scanQstr(scan);
			break;
		case SET:
			msg.m_controlNumber = scanInt2(scan);
			msg.m_controlValue = scanInt2(scan);
			msg.m_timeCode = scanTimeCode(scan);
			break;
		case FIRE:
			msg.m_controlValue = scanInt1(scan);
			break;
		case ALL_OFF:
		case RESTORE:
		case RESET:
			break;
		case STANDBY_PLUS:
		case STANDBY_MINUS:
		case SEQUENCE_PLUS:
		case SEQUENCE_MINUS:
		case START_CLOCK:
		case STOP_CLOCK:
		case ZERO_CLOCK:
		case MTC_CHASE_ON:
		case MTC_CHASE_OFF:
			msg.m_qlist = scanQstr(scan);
			break;
		case OPEN_CUE_LIST:
		case CLOSE_CUE_LIST:
			msg.m_qlist = scanQstr(scan);
			if (msg.m_qlist == null) {
				// throw new InvalidMidiDataException("MSC QList is missing.");
				return null;
			}
			break;
		case SET_CLOCK:
			msg.m_timeCode = scanTimeCode(scan);
			if (msg.m_timeCode == null) {
				return null;
			}
			msg.m_qlist = scanQstr(scan);
			break;
		case OPEN_CUE_PATH:
		case CLOSE_CUE_PATH:
			msg.m_qpath = scanQstr(scan);
			break;
		default:
			// throw new InvalidMidiDataException("Unsupported MSC Command Code " + cmd);
			return null;
		}
		
		return msg;
	}
	
	private static class ScanInfo {
		private byte[] data;
		private int next;
		private int len;
	}
	
	private static String scanQstr(ScanInfo scan)
	{
		StringBuilder buff = new StringBuilder(8);
		while (scan.next < scan.len) {
			byte c = scan.data[scan.next++];
			if (c == 0) {
				break;
			}
			buff.append((char)c);
		}
		return buff.length() > 0 ? buff.toString() : null;
	}
	
	private static int scanInt2(ScanInfo scan)
	{
		if (scan.next + 2 > scan.len) {
			return 0;
		}
		int ret = (scan.data[scan.next] &0x7f)
					| ((scan.data[scan.next+1] & 0x7f) << 7);
		scan.next += 2;
		return ret;
	}
	
	private static int scanInt1(ScanInfo scan)
	{
		if (scan.next + 1 > scan.len) {
			return 0;
		}
		int ret = scan.data[scan.next] & 0x7f;
		scan.next += 1;
		return ret;
	}
	
	private static MSCTimeCode scanTimeCode(ScanInfo scan)
	{
		if (scan.next + MSCTimeCode.ENCODED_LENGTH <= scan.len) {
			MSCTimeCode tc = new MSCTimeCode(scan.data, scan.next);
			scan.next += MSCTimeCode.ENCODED_LENGTH;
			return tc;
		}
		return null;
	}
	
	/**
	 * Create and return a Sysex message with the MSC command
	 * defined by the data members.
	 * @return A new Sysex message with the MSC command.
	 * @throws InvalidMidiDataException
	 * 			If there are invalid or missing data fields.
	 * @throws ArrayIndexOutOfBoundsException
	 * 			If the message is too long (only happens if the cue number,
	 * 			list or path are ridiculously long).
	 */
	public SysexMessage makeSysexMsg()
			throws InvalidMidiDataException, ArrayIndexOutOfBoundsException
	{
		byte[] data = new byte[128];
		int len = 0;
		data[len++] = REAL_TIME_SYSEX_START;
		data[len++] = (byte)m_deviceId;
		data[len++] = MSC_ID;
		data[len++] = (byte)m_cmdFormat.getCode();
		
		data[len++] = (byte)m_cmd.getCode();
		switch (m_cmd) {
		case GO:
		case STOP:
		case RESUME:
		case LOAD:
		case GO_OFF:
		case GO_JAM_CLOCK:
			len = appendQStrs(data, len, m_qnum, m_qlist, m_qpath);
			break;
		case TIMED_GO:
			if (m_timeCode == null) {
				throw new InvalidMidiDataException("MSC TIMED_GO requires a time code");
			}
			len = m_timeCode.makeCode(data, len);
			len = appendQStrs(data, len, m_qnum, m_qlist, m_qpath);
			break;
		case SET:
			data[len++] = (byte)((m_controlNumber     ) & 0x7f);
			data[len++] = (byte)((m_controlNumber >> 7) & 0x7f);
			data[len++] = (byte)((m_controlValue      ) & 0x7f);
			data[len++] = (byte)((m_controlValue  >> 7) & 0x7f);
			if (m_timeCode != null) {
				len = m_timeCode.makeCode(data,  len);
			}
			break;
		case FIRE:
			data[len++] = (byte)(m_controlValue & 0x7f);
			break;
		case ALL_OFF:
		case RESTORE:
		case RESET:
			break;
		case STANDBY_PLUS:
		case STANDBY_MINUS:
		case SEQUENCE_PLUS:
		case SEQUENCE_MINUS:
		case START_CLOCK:
		case STOP_CLOCK:
		case ZERO_CLOCK:
		case MTC_CHASE_ON:
		case MTC_CHASE_OFF:
			len = appendQStr(data, len, m_qlist);
			break;
		case OPEN_CUE_LIST:
		case CLOSE_CUE_LIST:
			if (m_qlist == null) {
				throw new InvalidMidiDataException("MSC QList is missing.");
			}
			len = appendQStr(data, len, m_qlist);
			break;
		case SET_CLOCK:
			if (m_timeCode == null) {
				throw new InvalidMidiDataException("MSC time code is missing");
			}
			len = m_timeCode.makeCode(data, len);
			len = appendQStr(data, len, m_qlist);
			break;
		case OPEN_CUE_PATH:
		case CLOSE_CUE_PATH:
			len = appendQStr(data, len, m_qpath);
			break;
		default:
			throw new InvalidMidiDataException("Unsupported MSC Command Code " + m_cmd);
		}
		data[len++] = REAL_TIME_SYSEX_END;
		
		return new SysexMessage(SysexMessage.SYSTEM_EXCLUSIVE, data, len);
	}
	
	private static int appendQStrs(byte[] data, int len, String s1, String s2, String s3)
			throws ArrayIndexOutOfBoundsException, InvalidMidiDataException
	{
		len = appendQStr(data, len, s1);
		if ((s2 != null && !s2.equals("")) || (s3 != null && !s3.equals(""))) {
			data[len++] = 0;
		}
		len = appendQStr(data, len, s2);
		if (s3 != null && !s3.equals("")) {
			data[len++] = 0;
		}
		len = appendQStr(data, len, s3);
		return len;
	}
	
	private static int appendQStr(byte[] data, int len, String s)
			throws InvalidMidiDataException, ArrayIndexOutOfBoundsException
	{
		if (s != null && !s.equals("")) {
			int n = s.length();
			for (int i = 0; i < n; i++) {
				char c = s.charAt(i);
				if (!Character.isDigit(c) && c != '.') {
					throw new InvalidMidiDataException("Invalid MSC Q string character '" + c + "'");
				}
				if (i == 0 && c == '.') {
					// Add a leading 0 if the client forgot.
					data[len++] = '0';
				}
				data[len++] = (byte)c;
			}
		}
		return len;
	}
	
	private static boolean isMyDeviceId(int msgId, int[] myIds)
	{
		if (msgId == ALL_CALL_DEVICE_ID || myIds == null) {
			return true;
		}
		for (int id: myIds) {
			if (msgId == id) {
				return true;
			}
		}
		return false;
	}
	
	private static MSCCmdFormat isMyCmdFmt(int msgCode, MSCCmdFormat[] myCmdFmts)
	{
		MSCCmdFormat msgFmt = MSCCmdFormat.fromCode(msgCode);
		if (msgFmt == MSCCmdFormat.RESERVED) {
			return null;
		}
		if (msgFmt == MSCCmdFormat.ALL_TYPES || myCmdFmts == null) {
			return msgFmt;
		}
		for (MSCCmdFormat fmt: myCmdFmts) {
			if (msgFmt == fmt) {
				return msgFmt;
			}
		}
		return null;
	}
}
