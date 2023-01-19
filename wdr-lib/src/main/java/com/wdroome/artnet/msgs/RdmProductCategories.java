package com.wdroome.artnet.msgs;

import java.util.Map;
import java.util.HashMap;

/**
 * Device product category codes for RDM devices.
 * See "ANSI E1.20 – 2010, Entertainment Technology—RDM – Remote Device Management over DMX512 Networks."
 * @author wdr
 */
public class RdmProductCategories
{			
	private static class CodeName {
		private int m_code;
		private String m_name;
		private CodeName(int code, String name) {
			m_code = code;
			m_name = name;
		}
	}
	
	//
	// Codes from Table A-5 in
	// "ANSI E1.20 – 2010, Entertainment Technology—RDM – Remote Device Management over DMX512 Networks"
	//
	private static final CodeName[] g_codeNames = new CodeName[] {
			new CodeName(0x0000, "NOT_DECLARED"),
			new CodeName(0x0100, "FIXTURE"),
			new CodeName(0x0101, "FIXTURE_FIXED"),
			new CodeName(0x0102, "FIXTURE_MOVING_YOKE"),
			new CodeName(0x0103, "FIXTURE_MOVING_MIRROR"),
			new CodeName(0x01FF, "FIXTURE_OTHER"),
			new CodeName(0x0200, "FIXTURE_ACCESSORY"),
			new CodeName(0x0201, "FIXTURE_ACCESSORY_COLOR"),
			new CodeName(0x0202, "FIXTURE_ACCESSORY_YOKE"),
			new CodeName(0x0203, "FIXTURE_ACCESSORY_MIRROR"),
			new CodeName(0x0204, "FIXTURE_ACCESSORY_EFFECT"),
			new CodeName(0x0205, "FIXTURE_ACCESSORY_BEAM"),
			new CodeName(0x02FF, "FIXTURE_ACCESSORY_OTHER"),
			new CodeName(0x0300, "PROJECTOR"),
			new CodeName(0x0301, "PROJECTOR_FIXED"),
			new CodeName(0x0302, "PROJECTOR_MOVING_YOKE"),
			new CodeName(0x0303, "PROJECTOR_MOVING_MIRROR"),
			new CodeName(0x03FF, "PROJECTOR_OTHER"),
			new CodeName(0x0400, "ATMOSPHERIC"),
			new CodeName(0x0401, "ATMOSPHERIC_EFFECT"),
			new CodeName(0x0402, "ATMOSPHERIC_PYRO"),
			new CodeName(0x04FF, "ATMOSPHERIC_OTHER"),
			new CodeName(0x0500, "DIMMER"),
			new CodeName(0x0501, "DIMMER_AC_INCANDESCENT"),
			new CodeName(0x0502, "DIMMER_AC_FLUORESCENT"),
			new CodeName(0x0503, "DIMMER_AC_COLDCATHODE"),
			new CodeName(0x0505, "DIMMER_AC_ELV"),
			new CodeName(0x0506, "DIMMER_AC_OTHER"),
			new CodeName(0x0507, "DIMMER_DC_LEVEL"),
			new CodeName(0x0508, "DIMMER_DC_PWM"),
			new CodeName(0x0509, "DIMMER_CS_LED"),
			new CodeName(0x05FF, "DIMMER_OTHER"),
			new CodeName(0x0600, "POWER"),
			new CodeName(0x0601, "POWER_CONTROL"),
			new CodeName(0x0602, "POWER_SOURCE"),
			new CodeName(0x06FF, "POWER_OTHER"),
			new CodeName(0x0700, "SCENIC"),
			new CodeName(0x0701, "SCENIC_DRIVE"),
			new CodeName(0x07FF, "SCENIC_OTHER"),
			new CodeName(0x0800, "DATA"),
			new CodeName(0x0801, "DATA_DISTRIBUTION"),
			new CodeName(0x0802, "DATA_CONVERSION"),
			new CodeName(0x08FF, "DATA_OTHER"),
			new CodeName(0x0900, "AV"),
			new CodeName(0x0901, "AV_AUDIO"),
			new CodeName(0x0902, "AV_VIDEO"),
			new CodeName(0x09FF, "AV_OTHER"),
			new CodeName(0x0A00, "MONITOR"),
			new CodeName(0x0A01, "MONITOR_ACLINEPOWER"),
			new CodeName(0x0A02, "MONITOR_DCPOWER"),
			new CodeName(0x0A03, "MONITOR_ENVIRONMENTAL"),
			new CodeName(0x0AFF, "MONITOR_OTHER"),
			new CodeName(0x7000, "CONTROL"),
			new CodeName(0x7001, "CONTROL_CONTROLLER"),
			new CodeName(0x7002, "CONTROL_BACKUPDEVICE"),
			new CodeName(0x70FF, "CONTROL_OTHER"),
			new CodeName(0x7100, "TEST"),
			new CodeName(0x7101, "TEST_EQUIPMENT"),
			new CodeName(0x71FF, "TEST_EQUIPMENT_OTHER"),
			new CodeName(0x7FFF, "OTHER"),
		};
	
	private static Map<Integer, String> g_codes2names = new HashMap<>();
	private static Map<String, Integer> g_names2codes = new HashMap<>();
	static {
		for (CodeName codeName: g_codeNames) {
			g_codes2names.put(codeName.m_code, codeName.m_name);
			g_names2codes.put(codeName.m_name, codeName.m_code);
		}
	}
	
	/**
	 * Get the product category name for a code.
	 * @param code A product category code.
	 * @return The name of that code.
	 */
	public static String getCategoryName(int code)
	{
		String name = g_codes2names.get(code);
		if (name != null) {
			return name;
		}
		name = g_codes2names.get((code & 0xff00) | 0xff);
		if (name != null) {
			return name;
		}
		return "UNKNOWN";
	}
	
	/**
	 * Return the code for a product category name.
	 * @param name The name, from Table A-5. The "PRODUCT_CATEGORY_" prefix is optional.
	 * @return The category code for "name", or 0 if unknown.
	 */
	public static int getCode(String name)
	{
		name = name.toUpperCase();
		for (String prefix: new String[] {"PRODUCT_", "CATEGORY_"}) {
			if (name.startsWith(prefix)) {
				name = name.substring(prefix.length());
			}
		}
		Integer code = g_names2codes.get(name);
		return code != null ? code : 0;
	}
}
