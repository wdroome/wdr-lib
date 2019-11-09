package com.wdroome.misc;

import com.wdroome.util.SSESender;

/**
 * @author wdr
 */
public class SSESender_Test
{	
	public static void main(String[] args) throws Exception
	{
		SSESender sse = new SSESender(System.out);
		sse.bufferData("line1");
		sse.bufferData("line2\nline3");
		sse.bufferData("line4\r\n\nline5\n");
		sse.bufferData("");
		sse.sendEvent("my event", "my id");
	}
}
