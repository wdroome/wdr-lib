package com.wdroome.osc;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;

import com.wdroome.util.ImmutableList;

/**
 * An OSC messages (version 1,1)
 * @author wdr
 */
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
		this(method, null);
	}
	
	/**
	 * Create a new OSC message.
	 * @param method The method.
	 * @param args The arguments for the message. See {@link addArg(Object)}.
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
	 * Create an OSCMessage from raw bytes returned by a Byte iterator.
	 * @param iter Returns the bytes of the raw message.
	 * @param logError If not null, call logError.accept(message)
	 * 		if there is an unknown argument format.
	 * @throws NoSuchElementException If there aren't enough bytes in the iterator.
	 */
	public OSCMessage(Iterator<Byte> iter, Consumer<String> logError)
	{
		m_createTS = System.currentTimeMillis();
		m_method = OSCUtil.getOSCString(iter);
		String argTypes = OSCUtil.getOSCString(iter);
		int argCnt = argTypes.length();
		if (argCnt > 0) {
			m_args = new ArrayList<>(argCnt);
		}
		for (int iArg = 0; iArg < argCnt; iArg++) {
			char c = argTypes.charAt(iArg);
			switch (c) {
			case OSCUtil.OSC_STR_ARG_FMT_CHAR:
				addArg(OSCUtil.getOSCString(iter));
				break;
			case OSCUtil.OSC_INT32_ARG_FMT_CHAR:
				addArg(OSCUtil.getOSCInt32(iter));
				break;
			case OSCUtil.OSC_INT64_ARG_FMT_CHAR:
				addArg(OSCUtil.getOSCInt64(iter));
				break;
			case OSCUtil.OSC_FLOAT_ARG_FMT_CHAR:
				addArg(OSCUtil.getOSCFloat32(iter));
				break;
			case OSCUtil.OSC_DOUBLE_ARG_FMT_CHAR:
				addArg(OSCUtil.getOSCDouble64(iter));
				break;
			case OSCUtil.OSC_CHAR_ARG_FMT_CHAR:
				addArg(OSCUtil.getOSCChar(iter));
				break;
			case OSCUtil.OSC_BLOB_ARG_FMT_CHAR:
				addArg(OSCUtil.getOSCBlob(iter));
				break;
			case OSCUtil.OSC_TIME_TAG_ARG_FMT_CHAR:
				addArg(OSCUtil.getOSCInt64(iter));
				break;
			case OSCUtil.OSC_TRUE_ARG_FMT_CHAR:
				addArg(Boolean.TRUE);
				break;
			case OSCUtil.OSC_FALSE_ARG_FMT_CHAR:
				addArg(Boolean.FALSE);
				break;
			case OSCUtil.OSC_NULL_ARG_FMT_CHAR:
				break;
			case OSCUtil.OSC_IMPULSE_ARG_FMT_CHAR:
				break;
			default:
				if (logError != null && !(c == OSCUtil.OSC_ARG_FMT_HEADER_CHAR && iArg == 0)) {
					logError.accept("Listener: unexpected OSC arg format '" + c
							+ "' in '" + argTypes + "' " + iArg);						
				}
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
	 * Add an IMPULSE argument.
	 */
	public void addImpulseArg()
	{
		if (m_argTypes == null) {
			m_argTypes = OSCUtil.OSC_IMPULSE_ARG_FMT;
		} else {
			m_argTypes += OSCUtil.OSC_IMPULSE_ARG_FMT;
		}
	}
	
	/**
	 * Add a TIME_TAG argument.
	 * @param timeTag The time tag argument.
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
	 * 		If no argumnenbts, return "" rather than null.
	 */
	public String getArgTypes()
	{
		return m_argTypes != null ? m_argTypes : "";
	}
	
	/**
	 * Return the type of an arugment.
	 * @param index The argument index.
	 * @return The argument type letter, or "?" if that argument doesn't exist.
	 */
	public char getArgType(int index)
	{
		String types = getArgTypes();
		return index < types.length() ? types.charAt(index) : '?';
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
		return m_args != null ? new ImmutableList<Object>(m_args) : List.of();
	}
	
	/**
	 * Return the string value of an argument.
	 * @param index The argument index.
	 * @param def The value to return of the argument doesn't exist.
	 * @return The string value of the argument at index, or def.
	 */
	public String getString(int index, String def)
	{
		if (m_args == null || index >= m_args.size()) {
			return def;
		}
		Object v = m_args.get(index);
		if (v instanceof String) {
			return (String)v;
		} else {
			return v.toString();
		}
	}
	
	/**
	 * Return a numeric argument as a long.
	 * @param index The argument index.
	 * @param def The default value.
	 * @return The long value of the argument, or def if it isn't numeric.
	 */
	public long getLong(int index, long def)
	{
		if (m_args == null || index >= m_args.size()) {
			return def;
		}
		Object v = m_args.get(index);
		if (v instanceof Number) {
			return ((Number)v).longValue();
		} else {
			return def;
		}
	}
	
	/**
	 * Return a numeric argument as a double.
	 * @param index The argument index.
	 * @param def The default value.
	 * @return The double value of the argument, or def if it isn't numeric.
	 */
	public double getDouble(int index, double def)
	{
		if (m_args == null || index >= m_args.size()) {
			return def;
		}
		Object v = m_args.get(index);
		if (v instanceof Number) {
			return ((Number)v).doubleValue();
		} else {
			return def;
		}
	}
	
	/**
	 * Return a numeric argument as a double.
	 * @param index The argument index.
	 * @param def The default value.
	 * @return The double value of the argument, or def if it isn't numeric.
	 */
	public boolean getBoolean(int index, boolean def)
	{
		if (m_args == null || index >= m_args.size()) {
			return def;
		}
		Object v = m_args.get(index);
		if (v instanceof Boolean) {
			return (Boolean)v;
		} else {
			return def;
		}
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
