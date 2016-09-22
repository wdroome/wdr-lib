package com.wdroome.midi;

/**
 * The MIDI Show Control "general" and "sound" command codes.
 * This does not include the 2-phase commit commands.
 * @author wdr
 */
public enum MSCCommand
{
	RESERVED(0x0),
	GO(0x01),
	STOP(0x02),
	RESUME(0x03),
	TIMED_GO(0x04),
	LOAD(0x05),
	SET(0x06),
	FIRE(0x07),
	ALL_OFF(0x08),
	RESTORE(0x09),
	RESET(0x0a),
	GO_OFF(0x0b),
	
	GO_JAM_CLOCK(0x10, "GO/JAM_CLOCK"),
	STANDBY_PLUS(0x11, "STANDBY_+"),
	STANDBY_MINUS(0x12, "STANDBY_-"),
	SEQUENCE_PLUS(0x13, "SEQUENCE_+"),
	SEQUENCE_MINUS(0x14, "SEQUENCE_-"),
	START_CLOCK(0x15),
	STOP_CLOCK(0x16),
	ZERO_CLOCK(0x17),
	SET_CLOCK(0x18),
	MTC_CHASE_ON(0x19),
	MTC_CHASE_OFF(0x1a),
	OPEN_CUE_LIST(0x1b),
	CLOSE_CUE_LIST(0x1c),
	OPEN_CUE_PATH(0x1d),
	CLOSE_CUE_PATH(0x1e),
	;
	
	private final int m_code;
	private final String m_name;
	
	MSCCommand(int code) { this(code, null); }
	
	MSCCommand(int code, String name)
	{
		m_code = code;
		if (name == null || name.equals("")) {
			name = super.toString();
		}
		m_name = name;
	}
	
	/**
	 * Return the code value for this command.
	 * @return The command code.
	 */
	public int getCode() { return m_code; }
	
	/**
	 * Return the preferred text name for this command.
	 * @return The text name.
	 */
	public String getName() { return m_name; }
	
	/**
	 * Return the command for a code number.
	 * @param code A command code number.
	 * @return The command  for code, or RESERVED if code is not valid.
	 */
	public static MSCCommand fromCode(int code)
	{
		// Slower than an indexed array, but easier to implement!
		for (MSCCommand fmt: values()) {
			if (code == fmt.getCode()) {
				return fmt;
			}
		}
		return RESERVED;
	}
	
	/**
	 * Print all commands, for testing.
	 * @param args
	 */
	public static void main(String[] args)
	{
		for (MSCCommand fmt: MSCCommand.values()) {
			MSCCommand x = MSCCommand.fromCode(fmt.getCode());
			System.out.println(String.format("%02x: %s [%s]%s",
					fmt.getCode(), fmt.getName(), x.toString(),
					(x == fmt ? "" : " **** ERROR IN fromCode()! ****")));
		}
	}
}
