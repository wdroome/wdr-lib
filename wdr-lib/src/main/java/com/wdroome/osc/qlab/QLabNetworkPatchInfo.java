package com.wdroome.osc.qlab;

import com.wdroome.json.JSONValue_Object;

/**
 * Information about a Network Patch. This is new in QLab 5.
 * @author wdr
 */
public class QLabNetworkPatchInfo
{
	/** The QLab patch types. To be precise, a subset with the ones we care about. */
	public static enum PatchType {
		OSC_MESSAGE("OSC Message"),
		PLAIN_TEXT("Plain Text"),
		HEX_CODES("Hex Codes"),
		QLAB5("QLab 5"),
		GO_BUTTON_3("Go Button 3"),
		ETC_EOS_FAMILY("ETC Eos Family"),
		
		// Insert more here. The other types will show up as OTHER.
		OTHER("Other");

		private final String m_qlab;
		private PatchType(String qlab) { m_qlab = qlab; }
		
		public String toQLab() { return m_qlab; }
		
		public static PatchType fromQLab(String v)
		{
			for (PatchType type: PatchType.values()) {
				if (type.m_qlab.equalsIgnoreCase(v)) {
					return type;
				}
			}
			return OTHER;
		}
	};
	
	public final int m_number;
	public final String m_uniqueID;
	public final String m_typeAndName;
	public final String m_patchName;
	public final PatchType m_patchType;
	
	private static final String TYPE_NAME_SEP_STR = " - ";
	
	/**
	 * Create a NetworkPatchInfo from the QLab fields.
	 * @param number The patch number, staring with 1.
	 * @param uniqueID The UUID for the patch.
	 * @param nameFld The patch type and name.
	 */
	public QLabNetworkPatchInfo(int number, String uniqueID, String nameFld)
	{
		m_number = number;
		m_uniqueID = uniqueID != null ? uniqueID : "0000";
		m_typeAndName = nameFld = nameFld != null ? nameFld : "";
		int iSep = m_typeAndName.indexOf(TYPE_NAME_SEP_STR);
		if (iSep >= 0) {
			m_patchName = m_typeAndName.substring(iSep + TYPE_NAME_SEP_STR.length()).trim();
			m_patchType = PatchType.fromQLab(m_typeAndName.substring(0, iSep).trim());
		} else {
			m_patchName = m_typeAndName;
			m_patchType = PatchType.OTHER;
		}
	}
	
	/**
	 * Create a NetworkPatchInfo from QLab's reply to a QLabUtil.NETWORK_PATCH_LIST_REQ request.
	 * @param number The patch number, from the position in the returned array.
	 * 		The first patch is #1.
	 * @param json The JSON Object defining this patch.
	 */
	public QLabNetworkPatchInfo(int number, JSONValue_Object json)
	{
		this(number,
			json.getString(QLabUtil.FLD_UNIQUE_ID, ""),
			json.getString(QLabUtil.FLD_NAME, ""));
	}
	
	@Override
	public String toString()
	{
		return "NetworkPatch(" + m_number + ":" + m_patchName
				+ "," + m_patchType.toString()
				+ "," + m_uniqueID + ")";
	}

}
