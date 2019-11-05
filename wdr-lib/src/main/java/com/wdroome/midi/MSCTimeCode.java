package com.wdroome.midi;

/**
 * The Standard Time Code used in some MIDI Show Control messages.
 * @author wdr
 */
public class MSCTimeCode
{
	/**
	 * The encoded length of a Standard Time Code.
	 */
	public static int ENCODED_LENGTH = 5;
	
	// The time code type.
	public static enum TimeType {
		FRAME_24(0),
		FRAME_25(1),
		DROP_FRAME_30(2),
		FRAME_30(3);
		
		private final byte m_code;
		
		TimeType(int code) { m_code = (byte)code; }
		
		/**
		 * Return the enum constant for the encoded time type.
		 * @param code The encoded time type, 0x0 to 0x3.
		 * @return The enum constant for the encoded time type.
		 */
		private static TimeType fromCode(byte code)
		{
			for (TimeType tt: values()) {
				if (code == tt.m_code) {
					return tt;
				}
			}
			return FRAME_24;
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
	
	// m_status is valid iff m_subframe is negative.
	// The client must parse the status bits.
	public short m_status = 0;

	/**
	 * Create a time code from the encoded time code bytes in an MSC message.
	 * The client must ensure that the ENCODED_LENGTH bytes
	 * starting at in[offset] are within the array bounds.
	 * @param in The source for the encoded time code.
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
	 * Create a blank time code. The client must set the various data members.
	 */
	public MSCTimeCode()
	{
		this(null, 0);
	}
	
	/**
	 * Set the ENCODED_LENGTH bytes starting at out[offset] to the encoded time code.
	 * @param out The destination for the encoded time code.
	 * @param offset The starting offset within "out".
	 * @return The offset of the byte after the last time code byte.
	 */
	public int makeCode(byte[] out, int offset)
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
		return offset + ENCODED_LENGTH;
	}
	
	@Override
	public String toString()
	{
		return
			m_type.name() + "["
			+ m_hr + ":" + m_min + ":" + m_sec + ":" + m_frame
			+ ((m_subFrame >= 0) ? 
				(":" + m_subFrame) : ("/status=" + m_status))
			+ "]";
	}
}
