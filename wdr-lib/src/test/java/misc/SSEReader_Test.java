package test.misc;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;

import com.wdroome.util.SSEEvent;
import com.wdroome.util.SSEReader;

/**
 * @author wdr
 */
public class SSEReader_Test implements SSEReader.EventCB
{
	/* (non-Javadoc)
	 * @see com.lucent.sird.alto.util.SSEReader.EventCB#newSSE(com.lucent.sird.alto.util.SSEReader.Event)
	 */
	@Override
	public void newSSE(SSEEvent event)
	{
		System.out.println("SSE: event='" + event.m_event + "' id='" + event.m_id
						+ "' #dataLines: " + event.m_dataLines.size());
		System.out.println("  data: '" + event.getData("\\n") + "'");
	}

	/* (non-Javadoc)
	 * @see com.lucent.sird.alto.util.SSEReader.EventCB#eofSSE()
	 */
	@Override
	public void eofSSE()
	{
		System.out.println("SSE: EOF");
	}

	/* (non-Javadoc)
	 * @see com.lucent.sird.alto.util.SSEReader.EventCB#errorSSE(java.io.IOException)
	 */
	@Override
	public void errorSSE(IOException e)
	{
		System.out.println("SSE: error " + e);
	}
	
	public static void main(String[] args)
	{
		SSEReader_Test cb = new SSEReader_Test();
		if (args.length >= 1) {
			new SSEReader(System.in, cb);
		} else {
			String input = "data: foo\r\ndata: bar\r\nevent: mine\r\n\r\n";
			byte[] bytes = new byte[3 + input.length()];
			bytes[0] = (byte)0xef;
			bytes[1] = (byte)0xbb;
			bytes[2] = (byte)0xbf;
			for (int i = 0; i < input.length(); i++) {
				bytes[3+i] = (byte)input.charAt(i);
			}
	//		for (int i = 0; i < bytes.length; i++) {
	//			System.out.println(String.format("%d", bytes[i] & 0xff));
	//		}
			ByteArrayInputStream bomIn = new ByteArrayInputStream(bytes);
			new SSEReader(bomIn, cb);
		}
	}
}