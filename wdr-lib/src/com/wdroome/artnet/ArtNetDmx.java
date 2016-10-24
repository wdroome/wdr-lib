package com.wdroome.artnet;

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
	 */
	public ArtNetDmx(byte[] buff, int off, int length)
	{
		super(ArtNetOpcode.OpDmx);
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
			m_data = new byte[m_dataLen];
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
				+ 1			// sequence
				+ 1			// physical
				+ 1			// subUni
				+ 1			// net
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
		buff[off++] = (byte)m_sequence;
		buff[off++] = (byte)m_physical;
		buff[off++] = (byte)m_subUni;
		buff[off++] = (byte)m_net;
		putBigEndInt16(buff, off, (m_dataLen & 1) == 0 ? m_dataLen : m_dataLen+1);
		off += 2;
		copyBytes(buff, off, m_data, 0, m_dataLen);
		off += m_dataLen;
		if ((m_dataLen & 1) != 0) {
			zeroBytes(buff, off, 1);
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
	
	public static void main(String[] args)
	{
		ArtNetDmx m = new ArtNetDmx();
		m.m_sequence = 5;
		m.m_net = 0x4;
		m.m_data = new byte[] {
				(byte)(0x00), (byte)(0x20), (byte)(0x40), (byte)(0x60),
				(byte)(0x80), (byte)(0xa0), (byte)(0xc0), (byte)(0xe0),
				(byte)(0xff),
		};
		m.m_dataLen = m.m_data.length;
		System.out.println("min/max size: " + ArtNetDmx.minSize()
						+ " " + ArtNetDmx.size());
		System.out.println(m.toString().replace(",", "\n  "));
		byte[] buff = new byte[ArtNetDmx.size()];
		int mlen = m.putData(buff, 0);
		String x = new ByteAOL(buff, 0, mlen).toHex();
		System.out.println(x);
		ArtNetDmx m2 = new ArtNetDmx(buff, 0, mlen);
		System.out.println(m2.toString().replace(",", "\n  "));
	}
}