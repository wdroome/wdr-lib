package test.misc;

import java.io.IOException;
import java.io.File;

import com.wdroome.util.LogFileWriter;

/**
 * @author wdr
 */
public class TestLogFileWriter
{

	/**
	 * 
	 */
	public TestLogFileWriter()
	{
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException
	{
		String file = "/tmp/test-log-file-writer.txt";
		long nLines = 1000;
		long intvlMS = 100;

		if (args.length >= 1) {
			file = args[0];
		}
		if (args.length >= 2) {
			nLines = Long.parseLong(args[1]);
		}
		if (args.length >= 3) {
			intvlMS = Long.parseLong(args[2]);
		}
		
		System.out.println("Writing " + nLines + " lines to " + file + " intvl = " + intvlMS + " ms.");
		
		new File(file).delete();
		
		LogFileWriter log = new LogFileWriter(file);
		for (long n = 0; n < nLines; n++) {
			log.formatln("Line %d at %d", n, System.currentTimeMillis());
			try {Thread.sleep(intvlMS);} catch (Exception e) {};
		}
		log.close();
	}
}
