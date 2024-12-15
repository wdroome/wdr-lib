package com.wdroome.util;

import static org.junit.Assert.*;

import org.junit.Test;
import java.util.Random;

import com.wdroome.util.Base64;

/**
 * @author wdr
 */
public class Base64Test
{
	// Encode and decode binary src. 
	private void encDecTest(String testDesc, byte[] src, String expected)
	{
		byte[] enc = Base64.encodeToBytes(src);
		if (expected != null) {
			assertEquals(testDesc + ".enc", expected, new String(enc));
		}
		
		byte[] dec = Base64.decodeToBytes(enc);
		assertArrayEquals(testDesc + ".dec", src, dec);
	}
	
	// Encode and decode printable src.
	private void encDecTest(String testDesc, String src, String expected)
	{
		byte[] enc = Base64.encodeToBytes(src.getBytes());
		if (expected != null) {
			assertEquals(testDesc + ".enc", expected, new String(enc));
		}
		
		byte[] dec = Base64.decodeToBytes(enc);
		assertEquals(testDesc + ".dec", src, new String(dec));
	}

	@Test
	public void testLong()
	{
		String src =
				  "abcdefghijklmnopqrstuvwxyz\n"
				+ "ABCDEFGHIJKLMNOPQRSTUVYXYZ\n"
				+ "0123456789\n";
		String expectedB64 = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoKQUJDREVGR0hJSktMTU5PUFFSU1RVVllYWVoKMDEyMzQ1Njc4OQo=";
		
		encDecTest("long", src, expectedB64);
	}

	@Test
	public void test1c()
	{
		String src = "{";
		String expectedB64 = "ew==";
		
		encDecTest("1-char", src, expectedB64);
	}

	@Test
	public void test2c()
	{
		String src = "{}";
		String expectedB64 = "e30=";
		
		encDecTest("2-char", src, expectedB64);
	}

	@Test
	public void test3c()
	{
		String src = "{}|";
		String expectedB64 = "e318";
		
		encDecTest("3-char", src, expectedB64);
	}

	@Test
	public void test4c()
	{
		String src = "{}|:";
		String expectedB64 = "e318Og==";
		
		encDecTest("4-char", src, expectedB64);
	}
	
	@Test
	public void testBin()
	{
		byte[] src = new byte[256];
		for (int i = 0; i < src.length; i++) {
			src[i] = (byte)(i & 0xff);
		}
		encDecTest("binary 0-255", src, null);
	}
	
	@Test
	public void testBin2()
	{
		byte[] src = new byte[4096];
		Random rand = new Random();
		rand.nextBytes(src);
		encDecTest("binary random", src, null);
	}
}
