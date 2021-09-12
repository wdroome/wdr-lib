package com.wdroome.osc;

import java.util.List;
import java.util.ArrayList;

import com.wdroome.util.ImmutableList;

public class OSCMessage
{
	private final String m_method;
	private String m_argTypes = null;  // Arg type specifier, without leading "."
	private ArrayList<Object> m_args = null;
	private final long m_createTS;
	
	/**
	 * Create a new OSC message.
	 * @param method The method.
	 */
	public OSCMessage(String method)
	{
		m_method = method;
		m_createTS = System.currentTimeMillis();
	}
	
	/**
	 * Create a new OSC message.
	 * @param method The method.
	 * @param args The argments for the message. See {@link addArg(Object)}.
	 */
	public OSCMessage(String method, Object[] args)
	{
		m_method = method;
		m_createTS = System.currentTimeMillis();
		if (args != null) {
			for (Object arg: args) {
				addArg(arg);
			}
		}
	}
	
	/**
	 * Return the system time stamp when this message was created or received.
	 * @return The system time stamp when this message was created or received.
	 */
	public long getCreateTS()
	{
		return m_createTS;
	}
	
	/**
	 * Return the request method.
	 * @return The request method.
	 */
	public String getMethod() {
		return m_method;
	}

	/**
	 * Add an argument.
	 * @param arg The argument. Infer the type from the object type.
	 * @throws IllegalArgumentException If arg isn't an acceptable type.
	 */
	public void addArg(Object arg)
	{
		String fmt = OSCUtil.getArgFormatSpec(arg);
		if (m_argTypes == null) {
			m_argTypes = fmt;
			m_args = new ArrayList<>();
		} else {
			m_argTypes += fmt;
		}
		m_args.add(arg);
	}
	
	/**
	 * Add a TIME_TAG argument.
	 * @param timeTag The time tah argument.
	 */
	public void addTimeTagArg(long timeTag)
	{
		if (m_argTypes == null) {
			m_argTypes = OSCUtil.OSC_TIME_TAG_ARG_FMT;
		} else {
			m_argTypes += OSCUtil.OSC_TIME_TAG_ARG_FMT;
		}
		m_args.add(Long.valueOf(timeTag));
	}
	
	/**
	 * Return the types of the arguments, or "" if no args.
	 * @return A String with a letter for each argument giving the type:
	 * 		{@link OSCUtil#OSC_STR_ARG_FMT}, etc.
	 */
	public String getArgTypes()
	{
		return m_argTypes != null ? m_argTypes : "";
	}
	
	/**
	 * Return the number of arguments.
	 * @return The number of arguments.
	 */
	public int size()
	{
		return m_args != null ? m_args.size() : 0;
	}
	
	/**
	 * Return an immutable list with the arguments.
	 * If no arguments, return an empty list.
	 * @return The arguments as an immutable List.
	 */
	public List<Object> getArgs()
	{
		return m_args != null ? new ImmutableList(m_args) : List.of();
	}
	
	/**
	 * Append one or more byte arrays, which, when concatenated,
	 * form the OSC-encoded byte representation of this method and its arguments.
	 * @param oscBytes A list to which the byte[] arrays for this request
	 * 		are to appended. If null, create one.
	 * @return
	 * 		The oscByte parameter, if not null, or else the List
	 * 		which this method creates.
	 */
	public List<byte[]> getOSCBytes(List<byte[]> oscBytes)
	{
		if (oscBytes == null) {
			oscBytes = new ArrayList<>();
		}
		oscBytes.add(OSCUtil.toOSCBytes(m_method));
		if (m_args != null) {
			oscBytes.add(OSCUtil.toOSCBytes(OSCUtil.OSC_ARG_FMT_HEADER + m_argTypes));
			int iArg = 0;
			for (Object arg: m_args) {
				oscBytes.add(OSCUtil.getArgByteArray(m_argTypes.charAt(iArg), arg));
				iArg++;
			}
		}
		return oscBytes;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder(100);
		b.append("OSCMessage [");
		b.append(m_createTS);
		b.append(',');
		b.append(m_method);
		if (m_args != null) {
			b.append(',');
			b.append(m_argTypes);
			b.append(',');
			b.append(m_args);
		}
		b.append(']');
		return b.toString();
	}
}
