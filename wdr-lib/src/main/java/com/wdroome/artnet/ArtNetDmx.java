package com.wdroome.artnet;

import java.io.PrintStream;

import com.wdroome.util.ByteAOL;
import com.wdroome.util.StringUtils;

/**
 * An Art-Net DMX data message.
 * @author wdr
 */
public class ArtNetDmx extends ArtNetMsg
{	
	public int m_protoVers = ArtNetConst.PROTO_VERS;
	public int m_sequence = 0;
	public int m_physical = 0;
	public int m_subUni = 0;
	public int m_net = 0;
	public int m_dataLen = 0;
	
	/** The DMX values. Only the first m_dataLen bytes are valid. */
	public byte[] m_data = null;

	/**
	 * Create a message with the default field values.
	 */
	public ArtNetDmx()
	{
		super(ArtNetOpcode.OpDmx);
	}
	
	/**
	 * Create a message from bytes received from a message.
	 * @param buff The message buffer.
	 * @param off The starting offset of the data within buff.
	 * @param length The length of the data.
	 * @throws IllegalArgumentException
	 * 		If the message is too short or it does not have the correct op code.
	 */
	public ArtNetDmx(byte[] buff, int off, int length)
	{
		super(ArtNetOpcode.OpDmx);
		update(buff, off, length);
	}
	
	/**
	 * Replace the contents of an existing ArtNetDmx object
	 * with the data received from a message.
	 * This has the same effect as new ArtNetDmx(buff,off,length),
	 * but it avoids the overhead of allocating a new ArtNetDmx
	 * and a new data buffer.
	 * @param buff The message buffer.
	 * @param off The starting offset of the data within buff.
	 * @param length The length of the data.
	 * @throws IllegalArgumentException
	 * 		If the message is too short or it does not have the correct op code.
	 */
	public void update(byte[] buff, int off, int length)
	{
		if (length < minSize()) {
			throw new IllegalArgumentException("ArtNetDmx: short msg " + length);
		}
		ArtNetOpcode opcode = getOpcode(buff, off, length);
		if (opcode != ArtNetOpcode.OpDmx) {
			throw new IllegalArgumentException("ArtNetDmx: wrong opcode " + opcode);
		}
		off += ArtNetConst.HDR_OPCODE_LENGTH;
		m_protoVers = getProtoVers(buff, off);
		off += ArtNetConst.PROTO_VERS_LENGTH;
		m_sequence = buff[off++] & 0xff;
		m_physical = buff[off++] & 0xff;
		m_subUni = buff[off++] & 0xff;
		m_net = buff[off++] & 0xff;
		m_dataLen = getBigEndInt16(buff, off);
		off += 2;
		if (m_dataLen > length - off) {
			m_dataLen = length - off;
		}
		if (m_dataLen > 0) {
			if (m_data == null || m_data.length < m_dataLen) {
				m_data = new byte[m_dataLen];
			}
			copyBytes(m_data, 0, buff, off, m_dataLen);
		}		
	}
	
	/**
	 * Return the maximum length of an ArtNetDmx message.
	 * @return The maximum length of an ArtNetDmx message.
	 */
	public static int size()
	{
		return minSize()
				+ ArtNetConst.MAX_CHANNELS_PER_UNIVERSE;		// data
	}
	
	/**
	 * Return the minimum length of an ArtNetDiagData message.
	 * @return The minimum length of an ArtNetDiagData message.
	 */
	public static int minSize()
	{
		return ArtNetConst.HDR_OPCODE_LENGTH
				+ ArtNetConst.PROTO_VERS_LENGTH		// protoVers
				+ 1			// sequence
				+ 1			// physical
				+ 1			// subUni
				+ 1			// net
				+ 2;		// dataLen		
	}
	
	/**
	 * Increment the sequence number in the range 1 to 0xff.
	 */
	public void incrSeqn()
	{
		if (m_sequence == 0 | m_sequence == 0xff) {
			m_sequence = 1;
		} else {
			m_sequence++;
		}
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
		buff[off++] = (byte)m_sequence;
		buff[off++] = (byte)m_physical;
		buff[off++] = (byte)m_subUni;
		buff[off++] = (byte)m_net;
		putBigEndInt16(buff, off, (m_dataLen & 1) == 0 ? m_dataLen : m_dataLen+1);
		off += 2;
		copyBytes(buff, off, m_data, 0, m_dataLen);
		off += m_dataLen;
		if ((m_dataLen & 1) != 0) {
			// Art-Net spec says length must be even.
			buff[off] = 0;
			off += 1;
		}
		return off;
	}
	
	@Override
	public String toString()
	{
		StringBuilder b = new StringBuilder(300);
		b.append("ArtNetDmx{");
		append(b, "protoVers", m_protoVers);
		appendHex(b, "seqn", m_sequence);
		appendHex(b, "phys", m_physical);
		appendHex(b, "subUni", m_subUni);
		appendHex(b, "net", m_net);
		append(b, "dataLen", m_dataLen);
		for (int i = 0; i < m_dataLen; i++) {
			b.append(Integer.toHexString(m_data[i] & 0xff));
			b.append(',');
		}
		b.append('}');
		return b.toString();
	}
	
	/**
	 * Pretty-print the message.
	 * @param out The output stream.
	 * @param linePrefix A prefix for each line.
	 */
	@Override
	public void print(PrintStream out, String linePrefix)
	{
		if (linePrefix == null) {
			linePrefix = "";
		}
		int nPerLine = 16;
		int n = (m_dataLen + 1) & ~0x1; 
		out.print(linePrefix + "ArtNetDmx port: " + toPortString(m_net, m_subUni)
					+ " seqn: " + m_sequence + " #chan: " + n);
		for (int i = 0; i < n; i++) {
			if ((i % nPerLine) == 0) {
				out.println();
				out.print(linePrefix);
			}
			out.print(String.format(" %3d", (i < m_dataLen ? m_data[i] & 0xff : 0)));
		}
		out.println();
	}
}
