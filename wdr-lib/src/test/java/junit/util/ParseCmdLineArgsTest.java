package test.junit.util;

import static org.junit.Assert.*;

import org.junit.Test;

import com.wdroome.util.ParseCmdLineArgs;

/**
 * @author wdr
 *
 */
public class ParseCmdLineArgsTest
{

	@Test
	public void test1()
	{
		String line = "'This is arg 1' 'This is arg 2'";
		String[] expect = new String[] {"This is arg 1", "This is arg 2"};
		ParseCmdLineArgs parser = new ParseCmdLineArgs(line);
		String[] args = parser.getRemainingArgs();
		assertArrayEquals(expect, args);
	}

	@Test
	public void test2()
	{
		String line = "\"This is arg 1\" 'This is arg 2'";
		String[] expect = new String[] {"This is arg 1", "This is arg 2"};
		ParseCmdLineArgs parser = new ParseCmdLineArgs(line);
		String[] args = parser.getRemainingArgs();
		assertArrayEquals(expect, args);
	}

	@Test
	public void test3()
	{
		String line = "\"This is 'arg 1'\" 'This is arg 2'";
		String[] expect = new String[] {"This is 'arg 1'", "This is arg 2"};
		ParseCmdLineArgs parser = new ParseCmdLineArgs(line);
		String[] args = parser.getRemainingArgs();
		assertArrayEquals(expect, args);
	}

	@Test
	public void test4()
	{
		String line = "\"This is \\\"arg 1\\\"\" 'This is \"arg 2\"";
		String[] expect = new String[] {"This is \"arg 1\"", "This is \"arg 2\""};
		ParseCmdLineArgs parser = new ParseCmdLineArgs(line);
		String[] args = parser.getRemainingArgs();
		assertArrayEquals(expect, args);
	}
}
