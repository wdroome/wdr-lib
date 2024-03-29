package com.wdroome.artnet;

import java.util.Arrays;

/*
 * Represent an ACN UID.
 */
public class ACN_UID implements Comparable<ACN_UID>
{
	public static final int SACN_UID_LENGTH = 6;
	
	public final byte[] m_bytes;

	public static final ACN_UID BROADCAST_UID = new ACN_UID(0xffff, 0xffffffff);
	
	/**
	 * Create a blank (all zero) UID.
	 */
	public ACN_UID()
	{
		this(null, 0);
	}
	
	/**
	 * Create a UID from bytes in an array.
	 * @param src The byte array.
	 * @throws IllegalArgumentException If src is shorter than offset+6.
	 */
	public ACN_UID(byte[] src)
	{
		this(src, 0);
	}
	
	/**
	 * Create a UID from bytes in an array.
	 * @param src The byte array.
	 * @param offset The offset in the array.
	 * @throws IllegalArgumentException If src is shorter than offset+6.
	 */
	public ACN_UID(byte[] src, int offset)
	{
		if (src == null) {
			m_bytes = new byte[SACN_UID_LENGTH];
		} else if (offset + SACN_UID_LENGTH <= src.length) {
			m_bytes = Arrays.copyOfRange(src, offset, offset + SACN_UID_LENGTH);
		} else {
			throw new IllegalArgumentException("ACN_UID incorrect len " + src.length);
		}
	}
	
	/**
	 * Create a UID from a hex string. Acceptable formats are manufacturer:serial-number,
	 * in hex, as in "1900:10abc", or 6 hex bytes separated by colons, as in "19:0:0:1:a:bc",
	 * or a string of 12 hex digits without colons, as in "190000010abc".
	 * @param src The source string.
	 * @throws IllegalArgumentException If the format can't be recognized.
	 * @throws NumberFormatException If components aren't hex numbers.
	 */
	public ACN_UID(String src)
	{
		m_bytes = new byte[SACN_UID_LENGTH];
		if (src != null && !src.isBlank()) {
			String[] parts = src.trim().split(":");
			if (parts.length == 2) {
				int manufacturer = Integer.parseInt(parts[0], 16);
				int serial = Integer.parseInt(parts[1], 16);
				m_bytes[0] = (byte)((manufacturer >> 8) & 0xff);
				m_bytes[1] = (byte)((manufacturer     ) & 0xff);
				m_bytes[2] = (byte)((serial >> 24) & 0xff);
				m_bytes[3] = (byte)((serial >> 16) & 0xff);
				m_bytes[4] = (byte)((serial >>  8) & 0xff);
				m_bytes[5] = (byte)((serial      ) & 0xff);				
			} else if (parts.length == SACN_UID_LENGTH) {
				for (int i = 0; i < SACN_UID_LENGTH; i++) {
					m_bytes[i] = (byte)Integer.parseInt(parts[i], 16);
				}
			} else if (parts.length == 0) {
				long val = Long.parseLong(src, 16);
				m_bytes[0] = (byte)((val >> 40) & 0xff);
				m_bytes[1] = (byte)((val >> 32) & 0xff);
				m_bytes[2] = (byte)((val >> 24) & 0xff);
				m_bytes[3] = (byte)((val >> 16) & 0xff);
				m_bytes[4] = (byte)((val >>  8) & 0xff);
				m_bytes[5] = (byte)((val      ) & 0xff);
			} else {
				throw new IllegalArgumentException("ACN_UID unknown format '" + src + "'");
			}
		}
	}
	
	/**
	 * Create a UID from a manufacturer code and a device serial number.
	 * @param manufacturer The manufacturer's code.
	 * @param serial The device serial number.
	 */
	public ACN_UID(int manufacturer, int serial)
	{
		m_bytes = new byte[SACN_UID_LENGTH];
		m_bytes[0] = (byte)((manufacturer >> 8) & 0xff);
		m_bytes[1] = (byte)((manufacturer     ) & 0xff);
		m_bytes[2] = (byte)((serial >> 24) & 0xff);
		m_bytes[3] = (byte)((serial >> 16) & 0xff);
		m_bytes[4] = (byte)((serial >>  8) & 0xff);
		m_bytes[5] = (byte)((serial      ) & 0xff);
	}
	
	/**
	 * Return a UID to broadcast to all devices from a specific manufacturer.
	 * @param vendor The manufacture's id.
	 * @return A UID to broadcast to all devices from that vendor.
	 */
	public static ACN_UID vendorcastUid(int vendor)
	{
		return new ACN_UID(vendor, 0xffffffff);
	}
	
	/**
	 * Test if this is a full-broadcast UID.
	 * @return true iff this a full-broadcast UID.
	 */
	public boolean isBroadcastUID()
	{
		return equals(BROADCAST_UID);
	}
	
	/**
	 * Test if this is a vendor-cast UID.
	 * @return True iff this is a vendor-cast UID.
	 */
	public boolean isVendorcastUID()
	{
		return getDeviceId() == 0xffffffff;
	}
	
	/**
	 * Test if another UID matches this one.
	 * The other uid may be a wildcard.
	 * @param uid Another uid, possibly a wildcard.
	 * @return True if this uid matches the other.
	 */
	public boolean matches(ACN_UID uid)
	{
		if (uid == null) {
			return false;
		} else if (equals(uid)) {
			return true;
		} else if (uid.isBroadcastUID()) {
			return true;
		} else if (uid.isVendorcastUID()) {
			return getManufacturer() == uid.getManufacturer();
		} else {
			return false;
		}
	}
	
	/**
	 * Get the manufacturer code.
	 * @return The manufacturer code.
	 */
	public int getManufacturer()
	{
		return    (((int)(m_bytes[0] & 0xff)) << 8)
				| (((int)(m_bytes[1] & 0xff))     )
				;
	}
	
	/**
	 * Get the device id.
	 * @return The device id.
	 */
	public int getDeviceId()
	{
		return    (((int)(m_bytes[2] & 0xff)) << 24)
				| (((int)(m_bytes[3] & 0xff)) << 16)
				| (((int)(m_bytes[4] & 0xff)) <<  8)
				| (((int)(m_bytes[5] & 0xff))      )
				;
	}
	
	/**
	 * Compare two UIDs. Compare byte-by-byte, so the UIDs for a manufacturer sort together.
	 */
	@Override
	public int compareTo(ACN_UID o)
	{
		return o != null ? Arrays.compare(m_bytes, o.m_bytes) : 1;
	}

	@Override
	public String toString()
	{
		return Integer.toHexString(getManufacturer()) + ":" + Integer.toHexString(getDeviceId());
		
		/*
		 * Old style, colon hex.
		
			StringBuilder buff = new StringBuilder();
			String sep = "";
			for (byte b: m_bytes) {
				buff.append(sep);
				buff.append(Integer.toHexString(b & 0xff));
				sep = ":";
			}
		return buff.toString();
		*/
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(m_bytes);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ACN_UID other = (ACN_UID) obj;
		if (!Arrays.equals(m_bytes, other.m_bytes))
			return false;
		return true;
	}
	
	/**
	 * Append the UID's bytes to a byte buffer.
	 * @param buff The buffer.
	 * @param off The offset in the buffer.
	 * @return The offset of the next byte in the buffer.
	 */
	public int putUid(byte[] buff, int off)
	{
		if (off + SACN_UID_LENGTH > buff.length) {
			throw new IllegalArgumentException("ACN_UID.putUid(): buffer to short "
						+ off + " " + buff.length);
		}
		for (int i = 0; i < SACN_UID_LENGTH; i++) {
			buff[off++] = m_bytes[i];
		}
		return off;
	}
	
	/**
	 * Append an array of UIDs to a byte buffer.
	 * @param buff The buffer.
	 * @param off The offset in the buffer.
	 * @return The offset of the next byte in the buffer.
	 */
	public static int putUids(byte[] buff, int off, ACN_UID[] uids, int nUids)
	{
		if (uids != null) {
			for (ACN_UID uid : uids) {
				off = uid.putUid(buff, off);
			}
		}
		return off;
	}

	/**
	 * Create an array of UIDs from an array of bytes.
	 * @param src The byte array.
	 * @param offset Starting offset in src.
	 * @param nUids The number of uids.
	 * @return An array of UIDs. Never returns null.
	 */
	public static ACN_UID[] getUids(byte[] src, int offset, int nUids)
	{
		if (nUids < 0 || src == null) {
			return new ACN_UID[0];
		}
		if (offset + nUids * SACN_UID_LENGTH > src.length) {
			throw new IllegalArgumentException("ACN_UID.bytesToUids: not enough bytes "
						+ nUids + " " + (src.length - offset));
		}
		ACN_UID[] uids = new ACN_UID[nUids];
		for (int i = 0; i < nUids; i++) {
			uids[i] = new ACN_UID(src, offset);
			offset += SACN_UID_LENGTH;
		}
		return uids;
	}
	
	/**
	 * For testing, create a dummy UID.
	 * @param root Value for first byte of UID.
	 * @return A dummy UID.
	 */
	public static ACN_UID makeTestUid(int root)
	{
		byte[] src = new byte[SACN_UID_LENGTH];
		for (int i = 0; i < SACN_UID_LENGTH; i++) {
			src[i] = (byte)(root+i);
		}
		return new ACN_UID(src);
	}
}
