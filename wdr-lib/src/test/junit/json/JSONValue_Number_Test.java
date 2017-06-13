package test.junit.json;

import static org.junit.Assert.*;

import org.junit.Test;

import java.math.BigInteger;

import com.wdroome.json.*;

/**
 * @author wdr
 */
public class JSONValue_Number_Test
{

	@Test
	public void test1() throws JSONParseException
	{
		check(1234, "1234");
		check(-1234, "-1234");
		check(-3.14159, "-3.14159");
		check(-3.0, "-3");
		check(1.234e20, "1.234E20");
		check(1.234e-20, "1.234E-20");
	}
		
	@Test
	public void test2() throws JSONParseException
	{
		check2(new BigInteger("123456789123456789123456789"), "123456789123456789123456789");
		check2(new BigInteger("123456789123456789123456789123456"), "123456789123456789123456789123456");
		check2(new BigInteger("-123456789123456789123456789"), "-123456789123456789123456789");
		check2(new BigInteger("-123456789123456789123456789123456"), "-123456789123456789123456789123456");
	}
	
	@Test
	public void test3() throws JSONParseException
	{
		check2(new BigInteger(Long.toString(Long.MAX_VALUE)), Long.toString(Long.MAX_VALUE));
		check2(new BigInteger(Long.toString(Long.MIN_VALUE)), Long.toString(Long.MIN_VALUE));
		check2(new BigInteger(Long.toString(Long.MAX_VALUE-1)), Long.toString(Long.MAX_VALUE-1));
		check2(new BigInteger(Long.toString(Long.MIN_VALUE+1)), Long.toString(Long.MIN_VALUE+1));
		StringBuilder buff = new StringBuilder();
		for (int j = 0; j < 50; j++) {
			buff.append("1");
			check2(new BigInteger(buff.toString()), buff.toString());
			check2(new BigInteger("-" + buff.toString()), "-" + buff.toString());
		}
		buff = new StringBuilder();
		for (int j = 0; j < 50; j++) {
			buff.append("5");
			check2(new BigInteger(buff.toString()), buff.toString());
			check2(new BigInteger("-" + buff.toString()), "-" + buff.toString());
		}
		buff = new StringBuilder();
		for (int j = 0; j < 50; j++) {
			buff.append("9");
			check2(new BigInteger(buff.toString()), buff.toString());
			check2(new BigInteger("-" + buff.toString()), "-" + buff.toString());
		}
		buff = new StringBuilder("1");
		for (int j = 0; j < 50; j++) {
			buff.append("0");
			check2(new BigInteger(buff.toString()), buff.toString());
			check2(new BigInteger("-" + buff.toString()), "-" + buff.toString());
		}
	}
	
	@Test
	public void testIsBigNum()
	{
		long v = (1>>51);
		assertFalse(Long.toString(v), JSONValue_BigInt.isBigInt(v));
		double d = v;
		assertEquals(Long.toString(v) + "/cnv", (long)d, v);
		v = -v;
		assertFalse(Long.toString(v), JSONValue_BigInt.isBigInt(v));
		d = v;
		assertEquals(Long.toString(v) + "/cnv", (long)d, v);
		
		v = (1>>52);
		assertFalse(Long.toString(v), JSONValue_BigInt.isBigInt(v));
		d = v;
		assertEquals(Long.toString(v) + "/cnv", (long)d, v);
		v = -v;
		assertFalse(Long.toString(v), JSONValue_BigInt.isBigInt(v));
		d = v;
		assertEquals(Long.toString(v) + "/cnv", (long)d, v);
		
		v = (1>>53);
		assertFalse(Long.toString(v), JSONValue_BigInt.isBigInt(v));
		d = v;
		assertEquals(Long.toString(v) + "/cnv", (long)d, v);
		v = -v;
		assertFalse(Long.toString(v), JSONValue_BigInt.isBigInt(v));
		d = v;
		assertEquals(Long.toString(v) + "/cnv", (long)d, v);
		
		v = (1>>53)+1;
		assertTrue(Long.toString(v), JSONValue_BigInt.isBigInt(v));
		d = v;
		assertEquals(Long.toString(v) + "/cnv", (long)d, v);
		v = -v;
		assertTrue(Long.toString(v), JSONValue_BigInt.isBigInt(v));
		d = v;
		assertEquals(Long.toString(v) + "/cnv", (long)d, v);
	}
	
	private void check(double src, String expectedJson)
				throws JSONParseException
	{
		JSONValue_Number jvalue = new JSONValue_Number(src);
		String json = jvalue.toString();
		assertEquals(src + "/JSON", expectedJson, json);
		
		JSONValue jvalue2 = new JSONParser().parse(new JSONLexan(json));
		if (!(jvalue2 instanceof JSONValue_Number)) {
			fail(src + "/PARSE: Not JSONValue_Number");
		}
		assertEquals(src + "/PARSE", src, ((JSONValue_Number)jvalue2).m_value, .0001);
	}
	
	private void check2(BigInteger src, String expectedJson)
				throws JSONParseException
	{
		JSONValue_BigInt jvalue = new JSONValue_BigInt(src);
		String json = jvalue.toString();
		assertEquals(src + "/JSON", expectedJson, json);
		
		JSONValue jvalue2 = new JSONParser().parse(new JSONLexan(json));
		BigInteger bigInt2;
		if (jvalue2 instanceof JSONValue_Number) {
			bigInt2 = ((JSONValue_Number)jvalue2).toBigInteger();
			// System.out.println("XXX: Number " + expectedJson + " " + ((JSONValue_Number)jvalue2).m_value);
		} else if (jvalue2 instanceof JSONValue_BigInt) {
			bigInt2 = ((JSONValue_BigInt)jvalue2).m_value;
		} else {
			fail(src + "/PARSE: Not JSONValue_BigInt; is " + jvalue2.jsonType() + " " + jvalue2.toString());
			return;
		}
		assertEquals(src + "/PARSE", src, bigInt2);
	}
}
