package com.wdroome.artnet;

import java.net.Inet4Address;

import com.wdroome.artnet.msgs.*;

/**
 * The Art-Net opcodes.
 * Art-Net (TM) Designed by and Copyright Artistic License Holdings Ltd.
 * @author wdr
 */
public enum ArtNetOpcode
{
	Invalid(0x0),
	OpPoll(0x2000, (buff, off, len, addr) -> new ArtNetPoll(buff, off, len, addr)),
	OpPollReply(0x2100, (buff, off, len, addr) -> new ArtNetPollReply(buff, off, len, addr)),
	OpDiagData(0x2300, (buff, off, len, addr) -> new ArtNetDiagData(buff, off, len, addr)),
	OpCommand(0x2400),
	OpDmx(0x5000, (buff, off, len, addr) -> new ArtNetDmx(buff, off, len, addr)),
	OpNzs(0x5100),
	OpSync(0x5200),
	OpAddress(0x6000, (buff, off, len, addr) -> new ArtNetAddress(buff, off, len, addr)),
	OpInput(0x7000),
	OpTodRequest(0x8000, (buff, off, len, addr) -> new ArtNetTodRequest(buff, off, len, addr)),
	OpTodData(0x8100, (buff, off, len, addr) -> new ArtNetTodData(buff, off, len, addr)),
	OpTodControl(0x8200, (buff, off, len, addr) -> new ArtNetTodControl(buff, off, len, addr)),
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
	OpIpProg(0xf800, (buff, off, len, addr) -> new ArtNetIpProg(buff, off, len, addr)),
	OpIpProgReply(0xf900, (buff, off, len, addr) -> new ArtNetIpProgReply(buff, off, len, addr)),
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
	private final ArtNetMsg.MsgMaker m_msgMaker;	// Create this message type from a byte buffer.
	
	public final static int SIZE = 2;
		
	ArtNetOpcode(int number, ArtNetMsg.MsgMaker msgMaker)
	{
		m_number = number;
		m_bytes = new byte[SIZE];
		m_bytes[0] = (byte)((m_number     ) & 0xff);
		m_bytes[1] = (byte)((m_number >> 8) & 0xff);
		m_msgMaker = msgMaker;
	}
	
	ArtNetOpcode(int number)
	{
		this(number, null);
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
	 * Create a message of this type from a byte array.
	 * @param buff The message buffer.
	 * @param off The offset of the message within buff.
	 * @param length The length of the message.
	 * @param fromAddr The sender's IP address. May be null.
	 * @return The message, or null if there is no maker for this type.
	 */
	public ArtNetMsg makeMsg(byte[] buff, int off, int length, Inet4Address fromAddr)
	{
		if (m_msgMaker != null) {
			return m_msgMaker.make(buff, off, length, fromAddr);
		} else {
			return null;
		}
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
