package com.wdroome.artnet.msgs;

/**
 * The standard RDM parameter IDs defined by ANSI.
 * Source: ANSI E1.20 - 2010
 * @author wdr
 */
public enum RdmParamId
{
	DISC_UNIQUE_BRANCH(0x0001),
	DISC_MUTE(0x0002),
	DISC_UN_MUTE(0x0003),
	PROXIED_DEVICES(0x0010),
	PROXIED_DEVICE_COUNT(0x0011),
	COMMS_STATUS(0x0015),
	QUEUED_MESSAGE(0x0020),
	STATUS_MESSAGES(0x0030),
	STATUS_ID_DESCRIPTION(0x0031),
	CLEAR_STATUS_ID(0x0032),
	SUB_DEVICE_STATUS_REPORT_THRESHOLD(0x0033),
	SUPPORTED_PARAMETERS(0x0050),
	PARAMETER_DESCRIPTION(0x0051),
	DEVICE_INFO(0x0060),
	PRODUCT_DETAIL_ID_LIST(0x0070),
	DEVICE_MODEL_DESCRIPTION(0x0080),
	MANUFACTURER_LABEL(0x0081),
	DEVICE_LABEL(0x0082),
	FACTORY_DEFAULTS(0x0090),
	LANGUAGE_CAPABILITIES(0x00A0),
	LANGUAGE(0x00B0),
	SOFTWARE_VERSION_LABEL(0x00C0),
	BOOT_SOFTWARE_VERSION_ID(0x00C1),
	BOOT_SOFTWARE_VERSION_LABEL(0x00C2),
	DMX_PERSONALITY(0x00E0),
	DMX_PERSONALITY_DESCRIPTION(0x00E1),
	DMX_START_ADDRESS(0x00F0),
	SLOT_INFO(0x0120),
	SLOT_DESCRIPTION(0x0121),
	DEFAULT_SLOT_VALUE(0x0122),
	SENSOR_DEFINITION(0x0200),
	SENSOR_VALUE(0x0201),
	RECORD_SENSORS(0x0202),
	DEVICE_HOURS(0x0400),
	LAMP_HOURS(0x0401),
	LAMP_STRIKES(0x0402),
	LAMP_STATE(0x0403),
	LAMP_ON_MODE(0x0404),
	DEVICE_POWER_CYCLES(0x0405),
	DISPLAY_INVERT(0x0500),
	DISPLAY_LEVEL(0x0501),
	PAN_INVERT(0x0600),
	TILT_INVERT(0x0601),
	PAN_TILT_SWAP(0x0602),
	REAL_TIME_CLOCK(0x0603),
	IDENTIFY_DEVICE(0x1000),
	RESET_DEVICE(0x1001),
	POWER_STATE(0x1010),
	PERFORM_SELFTEST(0x1020),
	SELF_TEST_DESCRIPTION(0x1021),
	CAPTURE_PRESET(0x1030),
	PRESET_PLAYBACK(0x1031),
	
	/** Unknown parameter id. This code is illegal. */
	UNKNOWN_PARAM_ID(0x10000);
	
	private final int m_code;
	
	/**
	 * Set the message code for the parameter.
	 * @param code The code.
	 */
	private RdmParamId(int code)
	{
		m_code = code;
	}
	
	/**
	 * Get the message code for this parameter.
	 * @return The message code for this parameter.
	 */
	public int getCode() { return m_code; }
	
	/**
	 * Get the Parameter ID for a message code.
	 * @param code The message code.
	 * @return The Parameter ID for code, or UNKNOWN_PARAM_ID if it's not a standard parameter.
	 */
	public static RdmParamId getParamId(int code)
	{
		for (RdmParamId paramId: values()) {
			if (paramId.m_code == code) {
				return paramId;
			}
		}
		return UNKNOWN_PARAM_ID;
	}
	
	/**
	 * Return the name of an RDM parameter.
	 * If it's not a standard parameter code, return "UNKNOWN(xxxx)"
	 * where "xxxx"is the parameter ID in hex.
	 * @param code The parameter ID number.
	 * @return The name of that RDM parameter, or "UNKNOWN(xxxx)".
	 */
	public static String getName(int code)
	{
		RdmParamId paramId = getParamId(code);
		return paramId != UNKNOWN_PARAM_ID
				? paramId.toString()
				: ("UNKNOWN" + "(0x" + Integer.toHexString(code) + ")");
	}
}
