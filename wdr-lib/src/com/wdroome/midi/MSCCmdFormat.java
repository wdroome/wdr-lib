package com.wdroome.midi;

/**
 * The MIDI Show Control command format codes.
 * @author wdr
 */
public enum MSCCmdFormat
{
	RESERVED(0x0),
	LIGHTING_GENERAL(0x1),
	MOVING_LIGHTS(0x2),
	COLOR_CHANGERS(0x3),
	STROBES(0x4),
	LASERS(0x5),
	CHASERS(0x6),
	
	SOUND_GENERAL(0x10),
	MUSIC(0x11),
	CD_PLAYERS(0x12, "CD Players"),
	EPROM_PLAYBACK(0x13, "EPROM Playback"),
	AUDIO_TAPE_MACHINES(0x14),
	INTERCOMS(0x15),
	AMPLIFIERS(0x16),
	AUDIO_EFFECTS_DEVICES(0x17),
	EQUALIZERS(0x18),
	
	MACHINERY_GENERAL(0x20),
	RIGGING(0x21),
	FLYS(0x22),
	LIFTS(0x23),
	TURNTABLES(0x24),
	TRUSSES(0x25),
	ROBOTS(0x26),
	ANIMATION(0x27),
	FLOATS(0x28),
	BREAKAWAYS(0x29),
	BARGES(0x2A),
	
	VIDEO_GENERAL(0x30),
	VIDEO_TAPE_MACHINES(0x31),
	VIDEO_CASSETTE_MACHINES(0x32),
	VIDEO_DISC_PLAYERS(0x33),
	VIDEO_SWITCHERS(0x34),
	VIDEO_EFFECTS(0x35),
	VIDEO_CHARACTER_GENERATORS(0x36),
	VIDEO_STILL_STORES(0x37),
	VIDEO_MONITORS(0x38),
	
	PROJECTION_GENERAL(0x40),
	FILM_PROJECTORS(0x41),
	SLIDE_PROJECTORS(0x42),
	VIDEO_PROJECTORS(0x43),
	DISSOLVERS(0x44),
	SHUTTER_CONTROLS(0x45),
	
	PROCESS_CONTROL_GENERAL(0x50),
	HYDRAULIC_OIL(0x51),
	H2O(0x52, "H2O"),
	CO2(0x53, "CO2"),
	COMPRESSED_AIR(0x54),
	NATURAL_GAS(0x55),
	FOG(0x56),
	SMOKE(0x57),
	CRACKED_HAZE(0x58),
	
	PYRO_GENERAL(0x60),
	FIREWORKS(0x61),
	EXPLOSIONS(0x62),
	FLAME(0x63),
	SMOKE_POTS(0x64),
	
	ALL_TYPES(0x7f, "All-types")
	;
	
	private final int m_code;
	private final String m_name;
	
	MSCCmdFormat(int code) { this(code, null); }
	
	MSCCmdFormat(int code, String name)
	{
		m_code = code;
		if (name == null || name.equals("")) {
			String label = super.toString();
			int n = label.length();
			StringBuilder b = new StringBuilder(n);
			boolean atWordStart = true;
			for (int i = 0; i < n; i++) {
				char c = label.charAt(i);
				if (c == '_') {
					b.append(' ');
					atWordStart = true;
				} else if (atWordStart) {
					b.append(Character.toUpperCase(c));
					atWordStart = false;
				} else {
					b.append(Character.toLowerCase(c));
				}
			}
			name = b.toString().replaceAll(" General$", " (General Cat.)");
		}
		m_name = name;
	}
	
	/**
	 * Return the code value for this command format.
	 * @return The command code.
	 */
	public int getCode() { return m_code; }
	
	/**
	 * Return the preferred text name for this command format.
	 * @return The text name.
	 */
	public String getName() { return m_name; }
	
	/**
	 * Return the command format for a code number.
	 * @param code A command format code number.
	 * @return The command format for code, or RESERVED if code is not valid.
	 */
	public static MSCCmdFormat fromCode(int code)
	{
		// Slower than an indexed array, but easier to implement!
		for (MSCCmdFormat fmt: values()) {
			if (code == fmt.getCode()) {
				return fmt;
			}
		}
		return RESERVED;
	}
	
	/**
	 * Print all command formats, for testing.
	 * @param args Command line args. Ignored.
	 */
	public static void main(String[] args)
	{
		for (MSCCmdFormat fmt: MSCCmdFormat.values()) {
			MSCCmdFormat x = MSCCmdFormat.fromCode(fmt.getCode());
			System.out.println(String.format("%02x: %s [%s]%s",
					fmt.getCode(), fmt.getName(), x.toString(),
					(x == fmt ? "" : " **** ERROR IN fromCode()! ****")));
		}
	}
}
