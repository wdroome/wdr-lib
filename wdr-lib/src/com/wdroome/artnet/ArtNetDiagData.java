package com.wdroome.artnet;

import com.wdroome.util.ByteAOL;
import com.wdroome.util.StringUtils;

/**
 * An Art-Net Diagnostic Data message.
 * @author wdr
 */
public class ArtNetDiagData extends ArtNetMsg
{	
	public int m_protoVers = ArtNetConst.PROTO_VERS;
	public int m_priority = 0;
	public String m_data = "";

	/**
	 * Create a message with the default field values.
	 */
	public ArtNetDiagData()
	{
		super(ArtNetOpcode.OpDiagData);
	}
	
	/**
	 * Create a message from bytes received from a message.
	 * @param buff The message buffer.
	 * @param off The starting offset of the data within buff.
	 * @param length The length of the data.
	 */
	public ArtNetDiagData(byte[] buff, int off, int length)
	{
		super(ArtNetOpcode.OpDiagData);
		if (length < minSize()) {
			throw new IllegalArgumentException("ArtNetDiagData: short msg " + length);
		}
		ArtNetOpcode opcode = getOpcode(buff, off, length);
		if (opcode != ArtNetOpcode.OpDiagData) {
			throw new IllegalArgumentException("ArtNetDiagData: wrong opcode " + opcode);
		}
		off += ArtNetConst.HDR_OPCODE_LENGTH;
		m_protoVers = getProtoVers(buff, off);
		off += ArtNetConst.PROTO_VERS_LENGTH;
		off += 1;		// filler
		m_priority = buff[off++] & 0xff;
		off += 2;		// filler
		int dataLen = getBigEndInt16(buff, off);
		off += 2;
		if (dataLen > length - off) {
			dataLen = length - off;
		}
		if (dataLen > 0) {
			m_data = StringUtils.makeString(buff, off, dataLen);
		}
	}
	
	/**
	 * Return the maximum length of an ArtNetDiagData message.
	 * @return The maximum length of an ArtNetDiagData message.
	 */
	public static int size()
	{
		return minSize()
				+ 512;		// data
	}
	
	/**
	 * Return the minimum length of an ArtNetDiagData message.
	 * @return The minimum length of an ArtNetDiagData message.
	 */
	public static int minSize()
	{
		return ArtNetConst.HDR_OPCODE_LENGTH
				+ ArtNetConst.PROTO_VERS_LENGTH		// protoVers
				+ 1			// filler
				+ 1			// priority
				+ 2			// filler
				+ 2;		// dataLen		
	}
	
	/**
	 * Copy the data for the message into a buffer.
	 * @param buff The buffer.
	 * @param off The offset in buff. Must have space for size() bytes.
	 * @return The length of the message.
	 */
	public int putData(byte[] buff, int off)
	{
		off += putHeader(buff, off, m_protoVers);
		buff[off++] = 0;	// filler
		buff[off++] = (byte)m_priority;
		buff[off++] = 0;	// filler
		buff[off++] = 0;	// filler
		int dataLen = (m_data != null) ? m_data.length() : 0;
		putBigEndInt16(buff, off, dataLen + 1);	// +1 is for the null term.
		off += 2;
		copyBytes(buff, off, m_data.getBytes(), 0, dataLen);
		off += dataLen;
		buff[off] = 0;
		off++;
		return off;
	}
	
	@Override
	public String toString()
	{
		StringBuilder b = new StringBuilder(300);
		b.append("ArtNetDiagData{");
		append(b, "protoVers", m_protoVers);
		appendHex(b, "priority", m_priority);
		append(b, "data", m_data);
		b.append('}');
		return b.toString();
	}
}
