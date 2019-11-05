package com.wdroome.util;

/**
 *	Thrown when a {@link ByteAOL} does not have enough bytes left
 *	to complete an operation.
 */
public class AOLOverflowException extends Exception
{
	private static final long serialVersionUID = 8914503578477199932L;
	
	public AOLOverflowException() { super(); }
	public AOLOverflowException(String msg) { super(msg); }
}
