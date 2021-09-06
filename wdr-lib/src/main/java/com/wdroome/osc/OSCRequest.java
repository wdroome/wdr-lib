package com.wdroome.osc;

import java.util.List;
import java.util.ArrayList;

/**
 * A request to send to the OSC server.
 * @author wdr
 */
public class OSCRequest
{
	private final String m_method;
	private final Object[] m_args;		// String, Int, Long, Float or byte[].
	private final long m_createTS;
	
	/**
	 * Create a request with no parameters.
	 * @param method The request method (or address).
	 */
	public OSCRequest(String method)
	{
		m_method = method;
		m_args = null;
		m_createTS = System.currentTimeMillis();
	}
	
	/**
	 * Create a request with an arbitrary set of parameters.
	 * @param method The request method (or address).
	 * @param args
	 * 		The parameters. Legal types are String, Integer and Float.
	 * @throws IllegalArgumentException
	 * 		If any parameter types are not acceptable.
	 */
	public OSCRequest(String method, Object[] args)
	{
		if (args != null) {
			validateArgs(args);
		}
		m_method = method;
		m_args = (args != null && args.length > 0) ? args : null;
		m_createTS = System.currentTimeMillis();
	}
	
	/**
	 * Create a request with one String parameter.
	 * @param method The request method (or address).
	 * @param arg The parameter.
	 */
	public OSCRequest(String method, String arg)
	{
		m_method = method;
		m_args = new Object[] {arg};
		m_createTS = System.currentTimeMillis();
	}
	
	/**
	 * Create a request with one Int32 parameter.
	 * @param method The request method (or address).
	 * @param arg The parameter.
	 */
	public OSCRequest(String method, int arg)
	{
		m_method = method;
		m_args = new Object[] {new Integer(arg)};
		m_createTS = System.currentTimeMillis();
	}
	
	/**
	 * Create a request with one Float parameter.
	 * @param method The request method (or address).
	 * @param arg The parameter.
	 */
	public OSCRequest(String method, float arg)
	{
		m_method = method;
		m_args = new Object[] {new Float(arg)};
		m_createTS = System.currentTimeMillis();
	}
	
	/**
	 * Return the request method.
	 * @return The request method.
	 */
	public String getMethod() { return m_method; }
	
	/**
	 * Return the method name and the optional arguments.
	 * @return The method name and optional argument.
	 */
	public String getMethodArgs()
	{
		if (m_args == null) {
			return m_method;
		} else {
			StringBuilder b = new StringBuilder();
			b.append(m_method);
			for (Object arg: m_args) {
				b.append(' ');
				b.append(arg);
			}
			return b.toString();
		}
	}
	
	/**
	 * Return the parameters, as an Object[].
	 * @return The parameter Object[], or null if no parameters.
	 */
	public Object[] getArgs()
	{
		return m_args;
	}
	
	/**
	 * Append one or more byte arrays, which, when concatenated,
	 * form the OSC-encoded byte representation of this method and its parameters.
	 * @param oscBytes A list to which the byte[] arrays for this request
	 * 		are to appended. If null, create one.
	 * @return
	 * 		The oscByte parameter, if not null, or else the List<>
	 * 		which this method creates.
	 */
	public List<byte[]> getOSCBytes(List<byte[]> oscBytes)
	{
		if (oscBytes == null) {
			oscBytes = new ArrayList<byte[]>();
		}
		oscBytes.add(OSCUtil.toOSCBytes(m_method));
		if (m_args != null) {
			StringBuilder argFmt = new StringBuilder(1 + m_args.length);
			argFmt.append(OSCUtil.OSC_ARG_FMT_HEADER);
			for (Object arg: m_args) {
				if (arg instanceof String) {
					argFmt.append(OSCUtil.OSC_STR_ARG_FMT);
				} else if (arg instanceof Integer) {
					argFmt.append(OSCUtil.OSC_INT32_ARG_FMT);
				} else if (arg instanceof Float) {
					argFmt.append(OSCUtil.OSC_FLOAT_ARG_FMT);
				} else if (arg instanceof Long) {
					argFmt.append(OSCUtil.OSC_INT64_ARG_FMT);
				} else if (arg instanceof byte[]) {
					argFmt.append(OSCUtil.OSC_BLOB_ARG_FMT);
				} else {
					// Shouldn't happen: c'tor should ensure everything is valid.
					throw new IllegalArgumentException(
								"OSCRequest: Unknown argument class "
								+ arg.getClass().getName());
				}
			}
			oscBytes.add(OSCUtil.toOSCBytes(argFmt.toString()));
			for (Object arg: m_args) {
				if (arg instanceof String) {
					oscBytes.add(OSCUtil.toOSCBytes((String) arg));
				} else if (arg instanceof Integer) {
					oscBytes.add(OSCUtil.toOSCBytes((Integer) arg));
				} else if (arg instanceof Float) {
					oscBytes.add(OSCUtil.toOSCBytes((Float) arg));
				} else if (arg instanceof Long) {
					oscBytes.add(OSCUtil.toOSCBytes((Long)arg));
				} else if (arg instanceof byte[]) {
					oscBytes.add(OSCUtil.toOSCBytes(((byte[])arg).length));
					oscBytes.add((byte[]) arg);
				}
			}
		}
		return oscBytes;
	}
	
	/**
	 * Verify that we know how to encode all parameter for a request.
	 * That is, the parameters are all Strings, Integers or Floats.
	 * NOTE: This is called from the c'tor, so local variables have
	 * not been fully initialized.
	 * @param args The list of parameters, as java Objects.
	 */
	private void validateArgs(Object[] args)
	{
		for (Object arg: args) {
			if (arg instanceof String) {
			} else if (arg instanceof Integer) {
			} else if (arg instanceof Float) {
			} else if (arg instanceof Long) {
			} else if (arg instanceof byte[]) {
			} else {
				throw new IllegalArgumentException(
							"OSCRequest: Unknown argument class "
							+ arg.getClass().getName());
			}
		}
	}
	
	/**
	 * Return the time, in milliseconds, since this request object was created.
	 * @param curTS
	 * 		Current time, in millisecs.
	 * 		If 0, use System current time.
	 * @return
	 * 		Waiting time, in milliseconds.
	 */
	public long waitTime(long curTS)
	{
		if (curTS == 0) {
			curTS = System.currentTimeMillis();
		}
		return curTS - m_createTS;
	}
	
	/**
	 * Return the time, in milliseconds, since this request object was created.
	 * @return
	 * 		Waiting time, in milliseconds.
	 */
	public long waitTime()
	{
		return waitTime(0);
	}
}
