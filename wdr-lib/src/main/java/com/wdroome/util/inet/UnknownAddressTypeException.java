package com.wdroome.util.inet;

import java.net.UnknownHostException;

/**
 * A refinement of UnknownHostException for the case of an unknown address type.
 * @see EndpointAddress
 * @see CIDRAddress
 * @author wdr
 */
public class UnknownAddressTypeException extends UnknownHostException
{
	private static final long serialVersionUID = -2481904114991611125L;
	
	private final String m_addr;
	private final String m_addrType;
	
	/**
	 * Create a new exception.
	 * @param addrType The unknown address type.
	 * @param addr The full address.
	 */
	public UnknownAddressTypeException(String addrType, String addr)
	{
		super(((addrType != null) ? (addrType + ":") : "") + addr);
		m_addr = addr;
		m_addrType = addrType;
	}
	
	/**
	 * Return the unknown address type.
	 * @return The unknown address type.
	 */
	public String getAddrType()
	{
		return m_addrType;
	}
	
	/**
	 * Return the address with the unknown address type.
	 * @return The address with the unknown address type.
	 */
	public String getAddress()
	{
		return ((m_addrType != null) ? (m_addrType + ":") : "") + m_addr;
	}
	
	/**
	 * Return a detailed description of the error.
	 */
	@Override
	public String toString()
	{
		return "Unknown address type \'" + m_addrType + "\' in \'" + m_addr + "\'";
	}
}
