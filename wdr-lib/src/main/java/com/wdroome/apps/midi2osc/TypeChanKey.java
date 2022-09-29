package com.wdroome.apps.midi2osc;

/**
 * An encoding of the triple {Midi message type, channel, and data1 (or 'key').
 * Also use for wildcards for channel and key.
 */
public class TypeChanKey
{
	public static int ANY_CHAN = -1;
	public static int ANY_KEY = -1;		// This is really "unspecified key"
	
	public final MidiEventType m_type;
	public final int m_chan;
	public final int m_key;
	public final String m_encoded;
	
	private static char SEP_CHAR = '.';
	private static String ANY_CHAN_STR = "*";
	private static String ENCODE_FMT_CHAN = "%s.%d.%d";		// type, channel, key
	private static String ENCODE_FMT_ANYCHAN = "%s.*.%d";	// type, key
	private static String ENCODE_FMT_ANYKEY = "%s.%d";		// type, chan
	
	/**
	 * Create a new triple.
	 * @param type The MIDI message type.
	 * @param chan The MIDI channel (may be ANY_CHAN).
	 * @param key The key ('data1') (may be ANY_KEY).
	 */
	public TypeChanKey(MidiEventType type, int chan, int key)
	{
		m_type = type;
		m_chan = chan;
		m_key = key;
		m_encoded = encode(type, chan, key);
	}
	
	/**
	 * Create a new triple from an encoded string.
	 * @param typeChanKey A String encoding of {type,channel,key} triple.
	 * @throws IllegalArgumentException If typeChanKey is not correctly formatted.
	 */
	public TypeChanKey(String typeChanKey)
	{
		TypeChanKey tck = decode(typeChanKey);
		m_type = tck.m_type;
		m_chan = tck.m_chan;
		m_key = tck.m_key;
		m_encoded = tck.m_encoded;
	}
	
	/**
	 * Return the String encoding of this triple.
	 * @return The String encoding of this triple.
	 */
	public String encode()
	{
		return m_encoded;
	}
	
	/**
	 * Return the String encoding of this triple.
	 * @return The String encoding of this triple.
	 */
	@Override
	public String toString()
	{
		return m_encoded;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + m_chan;
		result = prime * result + m_key;
		result = prime * result + ((m_type == null) ? 0 : m_type.hashCode());
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
		TypeChanKey other = (TypeChanKey) obj;
		if (m_chan != other.m_chan)
			return false;
		if (m_key != other.m_key)
			return false;
		if (m_type != other.m_type)
			return false;
		return true;
	}
	
	/**
	 * Encode a {type,channel,key} triple.
	 * @param type The MIDI message type.
	 * @param chan The MIDI channel (may be ANY_CHAN).
	 * @param key The key ('data1') (may be ANY_KEY).
	 * @return The String encoding of a {type,channel,key} triple.
	 * @throws IllegalArgumentException If both chan and key are "ANY".
	 */
	public static String encode(MidiEventType type, int chan, int key)
	{
		String typeStr = type.toString().toLowerCase();
		if (chan >= 0 && chan <= 15 && key >= 0 && key <= 127) {
			return String.format(ENCODE_FMT_CHAN, typeStr, chan, key);
		} else if (chan >= 0 && chan <= 15) {
			return String.format(ENCODE_FMT_ANYKEY, typeStr, chan);
		} else if (key >= 0 && key <= 127) {
			return String.format(ENCODE_FMT_ANYCHAN, typeStr, key);
		} else {
			throw new IllegalArgumentException("TypeChanKey requires chan or key.");
		}
	}
	
	/**
	 * Decode the String encoding of {type,channel,key} triple.
	 * @param typeChanKey A String encoding of {type,channel,key} triple.
	 * @return An object with the triple.
	 * @throws IllegalArgumentException If typeChanKey is not correctly formatted.
	 */
	public static TypeChanKey decode(String typeChanKey)
	{
		try {
			int iSep1 = typeChanKey.indexOf(SEP_CHAR, 0);
			if (iSep1 <= 0) {
				return null;
			}
			MidiEventType type = MidiEventType.valueOf(typeChanKey.substring(0, iSep1).toUpperCase());
			int iSep2 = typeChanKey.indexOf(SEP_CHAR, iSep1+1);
			int chan;
			int key;
			
			if (iSep2 <= 0) {
				// Unspecified key.
				chan = Integer.parseInt(typeChanKey.substring(iSep1+1));
				key = ANY_KEY;
			} else {
				String chanStr = typeChanKey.substring(iSep1+1, iSep2);
				chan = ANY_CHAN_STR.equals(chanStr) ? ANY_CHAN : Integer.parseInt(chanStr);
				key = Integer.parseInt(typeChanKey.substring(iSep2+1));
			}
			return new TypeChanKey(type, chan, key);
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid type-chan-key encoding \"" + typeChanKey + "\"");
		}
	}
	
	public static void main(String[] args)
	{
		TypeChanKey tkc1a = new TypeChanKey(MidiEventType.CC, 1, 2);
		printTCK("tkc1a", tkc1a);
		TypeChanKey tkc1b = decode(tkc1a.toString());
		printTCK("tkc1b", tkc1b);
		System.out.println("tkc1a == tkc1b: " + tkc1a.equals(tkc1b));
		
		TypeChanKey tkc2a = new TypeChanKey(MidiEventType.ON, ANY_CHAN, 28);
		printTCK("tkc2a", tkc2a);
		TypeChanKey tkc2b = decode(tkc2a.toString());
		printTCK("tkc2b", tkc2b);
		System.out.println("tkc2a == tkc2b: " + tkc2a.equals(tkc2b));
		
		TypeChanKey tkc3a = new TypeChanKey(MidiEventType.OFF, 2, ANY_KEY);
		printTCK("tkc3a", tkc3a);
		TypeChanKey tkc3b = decode(tkc3a.toString());
		printTCK("tkc3b", tkc3b);
		System.out.println("tkc3a == tkc3b: " + tkc3a.equals(tkc3b));
	}
	
	private static void printTCK(String prefix, TypeChanKey tck)
	{
		System.out.println(prefix + ": '" + tck + "' chan=" + tck.m_chan + " key=" + tck.m_key);
	}
}
