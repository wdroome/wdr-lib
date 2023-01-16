package com.wdroome.artnet.msgs;

public class RdmParamData 
{
	public final int m_paramIdCode;
	public final RdmParamId m_paramId;
	public final byte[] m_data;
	
	public RdmParamData(RdmParamId paramId, byte[] data)
	{
		m_paramIdCode = paramId.getCode();
		m_paramId = paramId;
		m_data = data;
	}
	
	public RdmParamData(int paramIdCode, byte[] data)
	{
		m_paramIdCode = paramIdCode;
		m_paramId = RdmParamId.getParamId(paramIdCode);
		m_data = data;
	}
	
	public String paramNameOrCode()
	{
		if (m_paramId != RdmParamId.UNKNOWN_PARAM_ID) {
			return m_paramId.name();
		} else {
			return "0x" + Integer.toHexString(m_paramIdCode);
		}
	}
}
