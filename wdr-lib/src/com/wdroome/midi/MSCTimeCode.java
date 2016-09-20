package com.wdroome.midi;

/**
 * The Standard Time Code in some MIDI Show Control messages.
 * @author wdr
 */
public class MSCTimeCode
{
	// The time code type.
	public static enum TimeType {
		FRAME_24(0),
		FRAME_25(1),
		DROP_FRAME_30(2),
		FRAME_30(3);
		
		public final byte m_code;
		
		TimeType(int code) { m_code = (byte)code; }
		
		/**
		 * Return the enum constant for the encoded format.
		 * @param code The encoded format, 0x0 to 0x3.
		 * @return The enum constant for the encoded format.
		 */
		public static TimeType fromCode(byte code)
		{
			if (code == FRAME_24.m_code) {
				return FRAME_24;
			} else if (code == FRAME_25.m_code) {
				return FRAME_25;
			} else if (code == FRAME_30.m_code) {
				return FRAME_30;
			} else if (code == DROP_FRAME_30.m_code) {
				return DROP_FRAME_30;
			} else {
				// OOPS! Shouldn't happen.
				return FRAME_24;
			}
		}
	};
	
	// The time code type.
	public TimeType m_type = TimeType.FRAME_24;
	
	// Hours (0-23).
	public short m_hr = 0;
	
	// Minutes (0-59).
	public short m_min = 0;
	
	// Seconds (0-59).
	public short m_sec = 0;
	
	// Frames (0-29).
	public short m_frame = 0;
	
	// True iff the color-frame bit is on.
	public boolean m_colorFrame = false;
	
	// True iff the negative frame number bit is on.
	public boolean m_negFrames = false;
	
	// m_subFrame is valid iff it is not negative.
	// If negative, use m_status instead.
	public short m_subFrame = 0;
	
	// m_status is valid iff m_subframe is -1.
	// The client must parse the bits.
	public short m_status = 0;

	/**
	 * Create a time code from the raw time code in an MSC message.
	 * @param in The source for the raw time code.
	 * 		If null, set all fields to their default values.
	 * 		The object does not retain a reference to this buffer.
	 * @param offset The starting offset within "in".
	 */
	public MSCTimeCode(byte[] in, int offset)
	{
		if (in != null) {
			int hr = in[offset + 0] & 0xff;
			m_type = TimeType.fromCode((byte) ((hr >> 5) & 0x3));
			m_hr = (short) (hr & 0x1f);
			m_min = (short) (in[offset + 1] & 0x3f);
			m_colorFrame = (in[offset + 1] & 0x40) != 0;
			m_sec = (short) (in[offset + 2] & 0x3f);
			m_frame = (short) (in[offset + 3] & 0x1f);
			m_negFrames = (in[offset + 3] & 0x40) != 0;
			if ((in[offset + 3] & 0x20) != 0) {
				m_subFrame = -1;
				m_status = (short) (in[offset + 4] & 0xff);
			} else {
				m_subFrame = (short) (in[offset + 4] & 0x7f);
				m_status = -1;
			}
		}
	}
	
	/**
	 * Create a blank time code. The client must fill in the fields.
	 */
	public MSCTimeCode()
	{
		// The default values are fine.
	}
	
	/**
	 * Set the 5 bytes starting at out[offset] to the raw time code.
	 * @param out The destination for the raw time code.
	 * @param offset The starting offset within "out".
	 */
	public void makeCode(byte[] out, int offset)
	{
		out[offset+0] = (byte) ((m_type.m_code << 5) | m_hr);
		out[offset+1] = (byte) m_min;
		if (m_colorFrame) {
			out[offset+1] |= 0x40;
		}
		out[offset+2] = (byte) m_sec;
		out[offset+3] = (byte) m_frame;
		if (m_negFrames) {
			out[offset+3] |= 0x40;
		}
		if (m_subFrame < 0) {
			out[offset+3] |= 0x20;
			out[offset+4] = (byte) m_status;
		} else {
			out[offset+4] = (byte) m_subFrame;
		}
	}
}
