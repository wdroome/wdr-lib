package com.wdroome.artnet;

/**
 * The Art-Net opcodes.
 * @author wdr
 */
public enum ArtNetOpcode
{
	Invalid(0x0),
	OpPoll(0x2000),
	OpPollReply(0x2100),
	OpDiagData(0x2300),
	OpCommand(0x2400),
	OpDmx(0x5000),
	OpNzs(0x5100),
	OpSync(0x5200),
	OpAddress(0x6000),
	OpInput(0x7000),
	OpTodRequest(0x8000),
	OpTodData(0x8100),
	OpTodControl(0x8200),
	OpRdm(0x8300),
	OpRdmSub(0x8400),
	OpVideoSetup(0xa010),
	OpVideoPalette(0xa020),
	OpVideoData(0xa040),
	OpFirmwareMaster(0xf200),
	OpFirmwareReply(0xf300),
	OpFileTnMaster(0xf400),
	OpFileFnMaster(0xf500),
	OpFileFnReply(0xf600),
	OpIpProg(0xf800),
	OpIpProgReply(0xf900),
	OpMedia(0x9000),
	OpMediaPatch(0x9100),
	OpMediaControl(0x9200),
	OpMediaContrlReply(0x9300),
	OpTimeCode(0x9700),
	OpTimeSync(0x9800),
	OpTrigger(0x9900),
	OpDirectory(0x9a00),
	OpDirectoryReply(0x9b00),
	;
	
	private final int m_number;
	private final byte[] m_bytes;	// number in little-endian format.
	
	public final static int SIZE = 2;
		
	ArtNetOpcode(int number)
	{
		m_number = number;
		m_bytes = new byte[SIZE];
		m_bytes[0] = (byte)((m_number     ) & 0xff);
		m_bytes[1] = (byte)((m_number >> 8) & 0xff);
	}
	
	/**
	 * Return the code number for this opcode.
	 * @return The opcode number.
	 */
	public int getNumber() { return m_number; }
	
	/**
	 * Return the byte encoding for this opcode.
	 * @return The opcode as a 2-byte little-endian array.
	 */
	public byte[] getBytes() { return m_bytes; }
	
	/**
	 * Return the text name for this opcode.
	 * @return The text name.
	 */
	public String getName() { return toString(); }
	
	/**
	 * Copy the encoded opcode into a buffer.
	 * @param buff The buffer.
	 * @param off The offset into the buffer.
	 * @return The offset of the next byte after the opcode.
	 */
	public int putBytes(byte[] buff, int off)
	{
		buff[off++] = m_bytes[0];
		buff[off++] = m_bytes[1];
		return off;
	}
	
	/**
	 * Return the opcode for an opcode number.
	 * @param number A opcode number.
	 * @return The opcode, or Invalid if number is not valid.
	 */
	public static ArtNetOpcode fromNumber(int number)
	{
		// Slower than an indexed array, but easier to implement!
		for (ArtNetOpcode fmt: values()) {
			if (number == fmt.getNumber()) {
				return fmt;
			}
		}
		return Invalid;
	}
	
	/**
	 * Return the opcode for encoded opcode bytes.
	 * @param buff A buffer with encoded opcode number.
	 * @param off The offset of the opcode
	 * @return The opcode, or Invalid if number is not valid.
	 */
	public static ArtNetOpcode fromBytes(byte[] buff, int off)
	{
		return fromNumber(((buff[off+1] & 0xff) << 8) | (buff[off] & 0xff));
	}
	
	/**
	 * Print all opcodes, for testing.
	 * @param args
	 */
	public static void main(String[] args)
	{
		for (ArtNetOpcode fmt: ArtNetOpcode.values()) {
			ArtNetOpcode x = ArtNetOpcode.fromNumber(fmt.getNumber());
			ArtNetOpcode y = ArtNetOpcode.fromBytes(fmt.getBytes(), 0);
			System.out.println(String.format("%04x: %s [%02x%02x]%s%s",
					fmt.getNumber(), fmt.getName(),
					(fmt.getBytes()[0]), (fmt.getBytes()[1]),
					(x == fmt ? "" : " **** ERROR IN fromNumber()! ****"),
					(y == fmt ? "" : " **** ERROR IN fromBytes()! ****")));
		}
	}
}
