package com.wdroome.artnet.msgs;

/**
 * Base class for parsing the parameter-specific data
 * in the RdmPacket in an ArtNetRdm message.
 * @author wdr
 */
public class RdmParamData 
{
	/** The numeric code for the RDM parameter. */
	public final int m_paramIdCode;
	
	/**
	 * The parameter name for m_paramIdCode, if it's a standard parameter.
	 * Otherwise, RdmParamId.UNKNOWN_PARAM_ID.
	 */
	public final RdmParamId m_paramId;
	
	/** The raw data for this parameter. Can be null. */
	public final byte[] m_data;
	
	/**
	 * Create an object for a standard parameter.
	 * @param paramId The standard parameter.
	 * @param data The data. Can be null.
	 */
	public RdmParamData(RdmParamId paramId, byte[] data)
	{
		m_paramIdCode = paramId.getCode();
		m_paramId = paramId;
		m_data = data;
	}
	
	/**
	 * Create an object for a (possibly) custom parameter.
	 * @param paramId The numeric code for the parameter.
	 * @param data The data. Can be null.
	 */
	public RdmParamData(int paramIdCode, byte[] data)
	{
		m_paramIdCode = paramIdCode;
		m_paramId = RdmParamId.getParamId(paramIdCode);
		m_data = data;
	}
	
	/**
	 * Get the parameter id. For standard parameters, return the parameter name.
	 * For non-standard parameters, return the code as a hex number.
	 * @return
	 */
	public String paramNameOrCode()
	{
		if (m_paramId != RdmParamId.UNKNOWN_PARAM_ID) {
			return m_paramId.name();
		} else {
			return String.format("0X%04x", m_paramIdCode);
		}
	}
}
