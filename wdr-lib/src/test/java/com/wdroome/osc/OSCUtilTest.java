package com.wdroome.osc;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.List;

public class OSCUtilTest
{
	@Test
	public void testParseCmd()
	{
		String cmd = "/zero/one/two/three/four/five";
		List<String> actualTokens = OSCUtil.parseMethod(cmd, "XXX", 0, 3, 5, 6);
		List<String> expectedTokens = List.of("zero", "three", "five", "XXX");
		assertEquals(expectedTokens, actualTokens);
	}
}
