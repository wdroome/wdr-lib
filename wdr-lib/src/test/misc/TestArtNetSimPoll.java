package test.misc;

import java.io.IOException;
import java.io.PrintStream;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.InetAddress;
import java.net.Inet4Address;

import com.wdroome.artnet.ArtNetConst;
import com.wdroome.artnet.ArtNetMsg;
import com.wdroome.artnet.ArtNetOpcode;
import com.wdroome.artnet.ArtNetPoll;
import com.wdroome.artnet.ArtNetPollReply;
import com.wdroome.artnet.ArtNetChannel;

/**
 * @author wdr
 */
public class TestArtNetSimPoll extends ArtNetChannel.MsgPrinter
{
	private final ArtNetChannel m_chan;
	
	public TestArtNetSimPoll() throws IOException
	{
		this(null);
	}
	
	public TestArtNetSimPoll(int[] ports) throws IOException
	{
		m_chan = new ArtNetChannel(this, ports);
	}

	/* (non-Javadoc)
	 * @see com.wdroome.artnet.ArtNetChannel.MsgPrinter#msgPrefix()
	 */
	@Override
	public String msgPrefix()
	{
		return "TestArtNetPoll ";
	}
	
	public void shutdown() { m_chan.shutdown(); }

	@Override
	public void msgArrived(ArtNetMsg msg, InetSocketAddress sender, InetSocketAddress receiver)
	{
		super.msgArrived(msg, sender, receiver);
		ArtNetPollReply reply = new ArtNetPollReply();
		InetAddress addr = ((InetSocketAddress)receiver).getAddress();
		if (addr instanceof Inet4Address) {
			reply.m_ipAddr = (Inet4Address)addr;
			reply.m_ipPort = ((InetSocketAddress)receiver).getPort();
		}
		try {
			m_chan.send(reply, sender);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException
	{
		TestArtNetSimPoll defChan = new TestArtNetSimPoll();
		
		ArtNetChannel altChan1 = new ArtNetChannel(new ArtNetChannel.MsgPrinter(),
												new int[] {8001});
		ArtNetChannel altChan2 = new ArtNetChannel(new ArtNetChannel.MsgPrinter(),
												new int[] {8002});
		for (int i = 0; i < 2; i++) {
			System.out.println();
			altChan1.send(new ArtNetPoll(),
					new InetSocketAddress("127.0.0.1", ArtNetConst.ARTNET_PORT));
			try {Thread.sleep(1000);} catch (Exception e) {}
			altChan2.send(new ArtNetPoll(),
					new InetSocketAddress("127.0.0.1", ArtNetConst.ARTNET_PORT));
			try {Thread.sleep(1000);} catch (Exception e) {}
		}
		
		defChan.shutdown();
		altChan1.shutdown();
		altChan2.shutdown();
	}
}
