package com.wdroome.artnet.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.io.IOException;
import java.io.Closeable;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.Font;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.SwingUtilities;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.SwingConstants;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;

import javax.swing.border.LineBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.CompoundBorder;

import com.wdroome.util.inet.InetUtil;
import com.wdroome.artnet.ArtNetChannel;
import com.wdroome.artnet.ArtNetConst;
import com.wdroome.artnet.ArtNetOpcode;
import com.wdroome.artnet.ArtNetPort;
import com.wdroome.artnet.msgs.ArtNetDmx;
import com.wdroome.artnet.msgs.ArtNetMsg;
import com.wdroome.artnet.msgs.ArtNetPoll;
import com.wdroome.artnet.msgs.ArtNetPollReply;
import com.wdroome.util.swing.JTextAreaErrorLogger;
import com.wdroome.util.IErrorLogger;
import com.wdroome.util.SystemErrorLogger;
import com.wdroome.util.inet.InetInterface;
import com.wdroome.util.swing.SwingAppUtils;

/**
 * Listen for Art-Net DMX messages, and display the levels in a Swing window.
 * This process is an Art-Net node, and responds to ArtNetPolls.
 * Art-Net (TM) Designed by and Copyright Artistic License Holdings Ltd.
 * @author wdr
 */
public class ArtNetMonitorWindow extends JFrame implements Closeable
{
	public static final String TABNAME__ALERTS = "Alerts";
	
	public static final int CHANBOX_HSEP = 2;
	public static final int CHANBOX_VSEP = 4;
	public static final int CHANBOX_TOPBOT_PAD = 2;
	public static final int CHANBOX_LRPAD = 2;
	public static final Color CHANBOX_LINE_COLOR = Color.WHITE;
	public static final int CHANBOX_LINE_WIDTH = 1;
	public static final Color BACKGROUND_COLOR = Color.BLACK;
	public static final Color TEXT_COLOR = Color.WHITE;
	public static final String DEF_FONT = "monospaced-bold-12";

    /** A struct with a DMX message and the time it was received. */
    private static class DmxMsgTS
    {
    	private long m_ts;
    	private ArtNetDmx m_msg;
    	private DmxMsgTS(ArtNetDmx msg)
    	{
    		m_msg = msg;
    		m_ts = System.currentTimeMillis();
    	}
    }
    
    private static class AnPortDisplay
    {
    	private ArtNetPort m_anPort;
    	private int m_numActiveChannels;
    	private JPanel m_chanPanel;
    	private JScrollPane m_scrollPane;
    	private int m_tabIndex;
    	private JLabel[] m_chanValues;
    	private ChanBox[] m_chanBoxes;
    }
	
	private static class ChanBox extends Box
	{
		public ChanBox(JLabel num, JLabel value)
		{
			super(BoxLayout.Y_AXIS);
			setBorder(new CompoundBorder(new LineBorder(CHANBOX_LINE_COLOR, CHANBOX_LINE_WIDTH),
										new EmptyBorder(CHANBOX_TOPBOT_PAD,CHANBOX_LRPAD,
														CHANBOX_TOPBOT_PAD,CHANBOX_LRPAD)));
			num.setAlignmentX(Component.CENTER_ALIGNMENT);
			if (g_chanNumValueFont != null) {
				num.setFont(g_chanNumValueFont);
			}
			value.setAlignmentX(Component.CENTER_ALIGNMENT);
			if (g_chanNumValueFont != null) {
				value.setFont(g_chanNumValueFont);
			}
			num.setForeground(TEXT_COLOR);
			value.setForeground(TEXT_COLOR);
			add(num);
			add(createVerticalStrut(2));
			add(value);
		}
	}
	
	private static class ChanPanel extends JPanel implements Scrollable
	{
		public ChanPanel()
		{
			super();
			setBorder(new EmptyBorder(4,4,4,4));
			// setLayout(new GridLayout(0, 10, CHANBOX_HSEP, CHANBOX_VSEP));
			setLayout(new FlowLayout(FlowLayout.LEFT, CHANBOX_HSEP, CHANBOX_VSEP));
			setBackground(BACKGROUND_COLOR);
		}
		
		@Override
		public Dimension getPreferredScrollableViewportSize() {
			return null;
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
			return 0;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
			return 0;
		}

		@Override
		public boolean getScrollableTracksViewportWidth() {
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight() {
			return false;
		}
	}
	
	private static Font g_chanNumValueFont = Font.decode(DEF_FONT);

    private ArrayList<Integer> m_ipPorts = new ArrayList<>();
	private ArrayList<ArtNetPort> m_anPorts = new ArrayList<>();
	private HashSet<InetAddress> m_bindAddrs = new HashSet<>();
	private Dimension m_chanBoxSize;
	private JTabbedPane m_tabPane;
	
	private ArtNetChannel m_channel = null;
	private boolean m_isSharedChannel = false;
	private Receiver m_myReceiver = null;
	private AtomicBoolean m_running = new AtomicBoolean(true);

	private ArrayList<AtomicReference<DmxMsgTS>> m_lastDmxMsg = new ArrayList<>();
	private ArrayList<AtomicLong> m_numDmxMsgs = new ArrayList<>();
	private AtomicLong m_numBadDmxMsgs = new AtomicLong(0);
	private AtomicLong m_numPollMsgs = new AtomicLong(0);
	
	private AnPortDisplay[] m_anPortDisplays;
	private IErrorLogger m_logger = new SystemErrorLogger();	// C'tor replaces. This ensures it's never null.

	/**
	 * Start as a standalone application.
	 * @param args
	 */
	public static void main(String[] args)
	{
		SwingAppUtils.startSwing(() -> {
			try {
				ArtNetMonitorWindow artNetMonitorWindow = new ArtNetMonitorWindow(args);
				// System.err.println("XXX: post create app");
			} catch (IOException e) {
				System.err.println("Exception at startup: " + e);
				System.exit(1);
			}
		}, args);
		// System.err.println("XXX: main returns");
	}
	
	/**
	 * Start Swing with the node simulator, but share an ArtNetChannel created by the caller.
	 * @param args
	 * @param channel The channel to use. If null, create a channel and close when done.
	 */
	public static class Instance implements Closeable
	{
		private AtomicReference<ArtNetMonitorWindow> m_instance = new AtomicReference<>(null);
		
		public Instance(String[] args, ArtNetChannel channel) throws IOException
		{
			SwingAppUtils.startSwing(() -> {
				try {
					m_instance.set(new ArtNetMonitorWindow(args, channel));
				} catch (IOException e) {
					System.err.println("ArtNetMonitorWindow.Instance: Exception at startup: " + e);
				}
			}, args);
		}
		
		public void close()
		{
			ArtNetMonitorWindow instance = m_instance.getAndSet(null);
			if (instance != null) {
				instance.close();
			}
		}
	}
	
	public ArtNetMonitorWindow(String[] args) throws IOException
	{
		this(args, null);
	}
	
	public ArtNetMonitorWindow(String[] args, ArtNetChannel channel) throws IOException
	{
		super();
		if (channel != null) {
			m_channel = channel;
			m_isSharedChannel = true;
		}
		if (!parseArgs(args)) {
			return;
		}
		init();
	}
	
	private void init() throws IOException
	{
		m_logger = new JTextAreaErrorLogger( /* alert-text-style, alert-text-id */ );
		m_chanBoxSize = new ChanBox(new JLabel("888"), new JLabel("---")).getPreferredSize();
		// System.out.println("XXX: chanbox size: " + m_chanBoxSize);
    	
		StringBuilder title = new StringBuilder();
		title.append("Art-Net Monitor Node, IP ports");
		for (int p: m_ipPorts) {
			title.append(" " + p);
		}
		setTitle(title.toString());
		setSize(400,300);
		m_tabPane = new JTabbedPane();
		
    	m_anPortDisplays = new AnPortDisplay[m_anPorts.size()];
		for (int i = 0; i < m_anPorts.size(); i++) {
			m_anPortDisplays[i] = new AnPortDisplay();
			m_anPortDisplays[i].m_tabIndex = i;
			m_anPortDisplays[i].m_anPort = m_anPorts.get(i);
			makePortTab(m_anPortDisplays[i]);
		}
		
		makeAlertTab(m_tabPane);
		
		getContentPane().add(m_tabPane);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setVisible(true);
		
		m_myReceiver = new Receiver();
    	if (!m_isSharedChannel) {
    		m_channel = new MyArtNetChannel(m_myReceiver, m_ipPorts);
    	} else {
    		m_channel.addReceiver(m_myReceiver);
    	}
    	StringBuilder listenSockets = new StringBuilder();
    	String sep = "";
    	for (InetSocketAddress addr: m_channel.getListenSockets()) {
    		listenSockets.append(sep + InetUtil.toAddrPort(addr));
    		sep = " ";
    	}
    	m_logger.logError("Listening on sockets: " + listenSockets.toString());
		new Monitor();
	}
	
	@Override
	public void close()
	{
		if (m_channel != null) {
			m_channel.dropReceiver(m_myReceiver);
			if (!m_isSharedChannel) {
				m_channel.shutdown();
			}
		}
		dispose();
	}
	
	private boolean parseArgs(String[] args)
	{
		try {
			for (String arg: args) {
				if (arg.startsWith("font=")) {
					g_chanNumValueFont = Font.decode(arg.substring(5));
				} else if (arg.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+")) {
					m_bindAddrs.add(InetAddress.getByName(arg));
				} else if (arg.matches("[0-9]+")) {
					m_ipPorts.add(Integer.valueOf(arg));
				} else if (m_anPorts.size() < ArtNetConst.MAX_PORTS_PER_NODE) {
					m_anPorts.add(new ArtNetPort(arg));
				}
			}
		} catch (Exception e) {
			System.err.println("Usage: ArtNetMonitorWindow [font=name-style-size | ip-bind-addr | ip-port-number | an-port] ...");
			return false;
		}
    	if (m_ipPorts.isEmpty()) {
    		m_ipPorts.add(ArtNetConst.ARTNET_PORT);
    	}
    	if (m_anPorts.isEmpty()) {
    		m_anPorts.add(new ArtNetPort("0.0.0"));
    		m_anPorts.add(new ArtNetPort("0.0.1"));
    	}
    	for (int i = 0; i < m_anPorts.size(); i++) {
			m_lastDmxMsg.add(new AtomicReference<DmxMsgTS>());
			m_numDmxMsgs.add(new AtomicLong());
    	}
		return true;
	}
	
	private void makePortTab(final AnPortDisplay disp)
	{
    	int n = ArtNetConst.MAX_CHANNELS_PER_UNIVERSE;
    	disp.m_chanValues = new JLabel[n];
    	disp.m_chanBoxes = new ChanBox[n];
		disp.m_numActiveChannels = 120; 

		disp.m_chanPanel = new ChanPanel();
		
		for (int i = 0; i < n; i++) {
			JLabel num = new JLabel(""+(i+1));			
			JLabel value = new JLabel("---");
			disp.m_chanValues[i] = value;
			disp.m_chanBoxes[i] = new ChanBox(num, value);
			if (m_chanBoxSize != null) {
				disp.m_chanBoxes[i].setPreferredSize(m_chanBoxSize);
			}
			if (i < disp.m_numActiveChannels) {
				disp.m_chanPanel.add(disp.m_chanBoxes[i]);
			}
		}
		
		if (true) {
			JScrollPane scrollPane = new JScrollPane(disp.m_chanPanel);
			disp.m_scrollPane = scrollPane;
			// scrollPane.setWheelScrollingEnabled(true);
			final JViewport viewport = scrollPane.getViewport();
			viewport.setBackground(BACKGROUND_COLOR);
			viewport.addComponentListener(new ComponentAdapter() {
				public void componentResized(ComponentEvent e) {
					resizeChanBoxPanel(disp);
				}
			});
			scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
			m_tabPane.add(scrollPane);
		} else {
			m_tabPane.add(disp.m_chanPanel);
			disp.m_scrollPane = null;
		}
		setTabTitle(disp.m_tabIndex, disp.m_anPort, 0);
	}
	
	private void makeAlertTab(JTabbedPane tabPane)
	{
		if (m_logger instanceof JTextAreaErrorLogger) {
			tabPane.add(TABNAME__ALERTS,
					((JTextAreaErrorLogger)m_logger).getScrollPane("", ""));
		}
		StringBuilder initMsg = new StringBuilder();
		initMsg.append("\n");
		initMsg.append("    IP Ports:");
		for (int p: m_ipPorts) {
			initMsg.append("  " + p);
		}
		initMsg.append("\n");
		initMsg.append("    Art-Net Ports:");
		for (ArtNetPort p: m_anPorts) {
			initMsg.append("  " + p.toString());
		}
		initMsg.append("\n");
        m_logger.logError("ArtNetMonitorWindow config", 0, initMsg.toString());
	}
    
    private void setTabTitle(int tabIndex, ArtNetPort anPort, long numMsgs)
    {
    	m_tabPane.setTitleAt(tabIndex, "Port: " + anPort + "  #DMX: " + numMsgs);
    }
    
    private void resizeChanBoxPanel(AnPortDisplay disp)
    {
    	if (disp.m_scrollPane != null) {
	    	int vpwid = disp.m_scrollPane.getViewport().getSize().width;
	    	int ncols = (vpwid - 16)/(m_chanBoxSize.width + CHANBOX_HSEP);
			int nrows = (disp.m_numActiveChannels + ncols - 1) / ncols;
	    	LayoutManager layoutMgr = disp.m_chanPanel.getLayout();
	    	if (layoutMgr instanceof GridLayout) {
	    		((GridLayout)layoutMgr).setColumns(ncols);
	    	} else if (layoutMgr instanceof FlowLayout) {
	    		disp.m_chanPanel.setPreferredSize(new Dimension(
	    				4 + CHANBOX_HSEP + ncols*(m_chanBoxSize.width + CHANBOX_HSEP),
	    				4 + CHANBOX_VSEP + nrows*(m_chanBoxSize.height + CHANBOX_VSEP)));
	    	}
	    	
			if (false) {
				System.out.println("XXX Resized " + disp.m_tabIndex
						+ " " + disp.m_scrollPane.getViewport().getSize()
						+ " " + ncols + " " + disp.m_scrollPane.getVerticalScrollBar().getSize());
			}
			if (false) {
				disp.m_chanPanel.setMaximumSize(
						new Dimension(vpwid - 2, CHANBOX_VSEP + nrows * (m_chanBoxSize.height + CHANBOX_VSEP)));
				disp.m_chanPanel.repaint();
			}
    	}
    }
    
    private class Monitor extends Thread
    {
    	public Monitor()
    	{
    		setDaemon(true);
    		setName("ArtNetMonitorWindow.Monitor");
    		start();
    	}

    	@Override
    	public void run()
    	{
    		Updater updater = new Updater();
    		while (m_running.get()) {
    			try {Thread.sleep(200);} catch(Exception e) {}
    			SwingUtilities.invokeLater(updater);
    		}
    	}
    }
    	
    private class Updater implements Runnable
    {
    	@Override
    	public void run()
    	{
    		for (int i = 0; i < m_anPortDisplays.length; i++) {
    			AnPortDisplay disp = m_anPortDisplays[i];
    			setTabTitle(disp.m_tabIndex, disp.m_anPort, m_numDmxMsgs.get(i).get());
    			DmxMsgTS dmx = m_lastDmxMsg.get(i).get();
    			if (dmx != null && dmx.m_msg != null) {
    				for (int c = 0; c < dmx.m_msg.m_dataLen; c++) {
    					disp.m_chanValues[c].setText(String.format("%3d", 0xff & dmx.m_msg.m_data[c]));
    				}
    				if (disp.m_numActiveChannels < dmx.m_msg.m_dataLen) {
    					for (int c = disp.m_numActiveChannels; c < dmx.m_msg.m_dataLen; c++) {
    						disp.m_chanPanel.add(disp.m_chanBoxes[c]);
    					}
    					disp.m_numActiveChannels = dmx.m_msg.m_dataLen;
    					resizeChanBoxPanel(disp);
    				}
    			}
    		}
    	}
    }
    
	/**
	 * Handle incoming Art-Net messages.
	 */
	private class Receiver implements ArtNetChannel.Receiver
	{
		private final List<InetInterface> m_inetInterfaces = InetInterface.getAllInterfaces();
		private final ArtNetPollReply m_reply;
		private InetSocketAddress m_lastDmxSender = null;
		private InetSocketAddress m_lastDmxReceiver = null;
		
		public Receiver()
		{
			m_reply = new ArtNetPollReply();
			m_reply.m_shortName = "ArtNetMonitorWindow";
			m_reply.m_longName = m_reply.m_shortName;
			m_reply.m_style = ArtNetConst.StNode;
			m_reply.m_status2 = 0x0e;	// Supports 15-bit node addresses & DHCP.
			m_reply.m_numPorts = m_anPorts.size();
			for (int i = 0; i < m_anPorts.size(); i++) {
				m_reply.m_netAddr = m_anPorts.get(i).m_net;
				m_reply.m_subNetAddr = m_anPorts.get(i).m_subNet;
				m_reply.m_portTypes[i] = (byte)0x80;
				m_reply.m_goodInput[i] = (byte)0x00;
				m_reply.m_goodOutput[i] = (byte)0x80;
				m_reply.m_swOut[i] = (byte)m_anPorts.get(i).m_universe;
			}
			sendStartupPollReply();
		}

		/* (non-Javadoc)
		 * @see ArtNetChannel.Receiver#msgArrived(ArtNetMsg, InetSocketAddress, InetSocketAddress)
		 */
		@Override
		public void msgArrived(ArtNetChannel chan, ArtNetMsg msg,
								InetSocketAddress sender, InetSocketAddress receiver)
		{
			if (msg instanceof ArtNetPollReply) {
				m_logger.logError("ArtNetMonitorWindow: PollReply " + fromToInetAddrs(sender, receiver),
							1200000, "Received");
			} else if (msg instanceof ArtNetPoll) {
				m_logger.logError("ArtNetMonitorWindow: Poll " + fromToInetAddrs(sender, receiver),
							1200000, "Received from port " + sender.getPort());
				InetSocketAddress bcastAddr = getBcastAddr(sender.getAddress(), ArtNetConst.ARTNET_PORT);
				if (bcastAddr != null) {
					setAddrs(m_reply, sender, receiver.getPort());
					if (m_reply.m_ipAddr != null) {
						try {
							chan.send(m_reply, bcastAddr);
							if (false) {
								System.out.println("XXX: Send PollRep " + m_reply);
								System.out.println("XXX: sent poll reply to " + bcastAddr);
							}
						} catch (IOException e) {
							m_logger.logError("ArtNetMonitorWindow: Poll Reply Send Error",
												60000, "To " + bcastAddr + " " + e);
						}
					} else {
						m_logger.logError("ArtNetMonitorWindow: No Reply Addr in Poll",
								60000, "No Iface for " + bcastAddr);
					}
				} else {
					m_logger.logError("ArtNetMonitorWindow: Poll Reply Err", 60000,
										"No bcast addr for " + InetUtil.toAddrPort(sender));
				}
				m_numPollMsgs.incrementAndGet();
				// XXX: record poll request
			} else if (msg instanceof ArtNetDmx) {
				ArtNetDmx dmx = (ArtNetDmx)msg;
				if (!sender.equals(m_lastDmxSender) || !receiver.equals(m_lastDmxReceiver)) {
					m_logger.logError("ArtNetMonitorWindow: DMX Sender/" + dmx.getPortString()
							+ " " + fromToInetAddrs(sender, receiver),
							120000, "");
					m_lastDmxSender = sender;
					m_lastDmxReceiver = receiver;
				}
				boolean ours = false;
				for (int i = 0; i < m_anPorts.size(); i++) {
					ArtNetPort anPort = m_anPorts.get(i);
					if (dmx.m_subUni == anPort.subUniv() && dmx.m_net == anPort.m_net) {
						DmxMsgTS prev = m_lastDmxMsg.get(i).getAndSet(new DmxMsgTS(dmx));
						m_numDmxMsgs.get(i).incrementAndGet();
						ours = true;
						if (prev != null
								&& dmx.m_sequence != 0
								&& prev.m_msg.m_sequence != 0
								&& ((dmx.m_sequence - prev.m_msg.m_sequence + 256) % 256) > 200) {
							m_logger.logError("ArtNetMonitorWindow: out of sequence dmx msg", 60000,
										dmx.m_sequence + " " + prev.m_msg.m_sequence);
						}
						break;
					}
				}
				if (!ours) {
					m_numBadDmxMsgs.incrementAndGet();
					m_logger.logError("ArtNetMonitorWindow: Incorrect ANPort " +
									new ArtNetPort(dmx.m_net, dmx.m_subUni).toString(), 60000, "");
				}
			} else {
				m_logger.logError("ArtNetMonitorWindow: unexpected msg",
									0, fromToInetAddrs(sender, receiver) + " " + msg);
			}
		}
		
		/**
		 * Return a string with the sender and receiver's IP addresses and ports.
		 * @param sender The sending socket address.
		 * @param receiver The receiving socket address.
		 * @return A string of the form "sender-ipaddr:port -> receiver-ipaddr:port"
		 * Replace sender-port with "*" if it's not the default Art-Net port.
		 */
		private String fromToInetAddrs(InetSocketAddress sender, InetSocketAddress receiver)
		{
			int senderPort = sender.getPort();
			return sender.getAddress().getHostAddress() + ":" 
						+ (senderPort == ArtNetConst.ARTNET_PORT ? senderPort : "*")
					+ "->" + InetUtil.toAddrPort(receiver);
		}

		/* (non-Javadoc)
		 * @see ArtNetChannel.Receiver#msgArrived(ArtNetOpcode, byte[], int, InetSocketAddress, InetSocketAddress)
		 */
		@Override
		public void msgArrived(ArtNetChannel chan, ArtNetOpcode opcode, byte[] buff, int len,
								InetSocketAddress sender, InetSocketAddress receiver)
		{
			m_logger.logError("ArtNetMonitorWindow: unexpected msg",
									0, opcode + " " + sender + "->" + receiver);
		}

		/* (non-Javadoc)
		 * @see ArtNetChannel.Receiver#msgArrived(byte[], int, InetSocketAddress, InetSocketAddress)
		 */
		@Override
		public void msgArrived(ArtNetChannel chan, byte[] msg, int len,
								InetSocketAddress sender, InetSocketAddress receiver)
		{
			m_logger.logError("ArtNetMonitorWindow: non-art-net msg",
									0, "len " + len + " " + sender + "->" + receiver);
		}
		
		/**
		 * Return the broadcast address for the subnet containing a specific address.
		 * @param addr A non-broadcast address.
		 * @param port A port number.
		 * @return The broadcast address for addr's subnet, or null if none is known.
		 */
		private InetSocketAddress getBcastAddr(InetAddress addr, int port)
		{
			for (InetInterface iface: m_inetInterfaces) {
				if (iface.m_cidr != null
						&& iface.m_cidr.contains(addr)
						&& iface.m_broadcast != null
						&& iface.m_broadcast instanceof Inet4Address) {
					return new InetSocketAddress(iface.m_broadcast, port);
				}
			}
			return null;
		}
		
		/**
		 * Set the ip address & port in an Art-Net Poll Reply
		 * to the local address on the subnet which contains an address.
		 * Also copy the local mac address to the reply.
		 * @param addr An IP address.
		 * @param port The port on which the Poll message arrived.
		 */
		private void setAddrs(ArtNetPollReply reply, InetSocketAddress addr, int port)
		{
			for (InetInterface iface: m_inetInterfaces) {
				if (iface.m_address instanceof Inet4Address
						&& iface.m_cidr != null
						&& iface.m_cidr.contains(addr.getAddress())
						&& iface.m_broadcast != null
						&& iface.m_broadcast instanceof Inet4Address) {
					reply.m_ipAddr = (Inet4Address)iface.m_address;
					reply.m_ipPort = port;
					if (iface.m_hardwareAddress != null
							&& reply.m_macAddr.length == iface.m_hardwareAddress.length) {
						for (int i = 0; i < iface.m_hardwareAddress.length; i++) {
							reply.m_macAddr[i] = iface.m_hardwareAddress[i];
						}
					}
					return;
				}
			}
		}
		
		/**
		 * At startup, send ArtNetPollReply messages to all broadcast interfaces.
		 */
		private void sendStartupPollReply()
		{
			for (InetInterface iface: m_inetInterfaces) {
				if (iface.m_address instanceof Inet4Address
						&& !iface.m_isLoopback
						&& iface.m_broadcast != null
						&& iface.m_broadcast instanceof Inet4Address) {
					m_reply.m_ipAddr = (Inet4Address)iface.m_address;
					m_reply.m_ipPort = ArtNetConst.ARTNET_PORT;
					if (iface.m_hardwareAddress != null
							&& m_reply.m_macAddr.length == iface.m_hardwareAddress.length) {
						for (int i = 0; i < iface.m_hardwareAddress.length; i++) {
							m_reply.m_macAddr[i] = iface.m_hardwareAddress[i];
						}
					}
					try {
						m_reply.sendMsg(iface.m_broadcast, ArtNetConst.ARTNET_PORT);
						if (false) {
							System.out.println("XXX: Send init PollRep " + m_reply);
							System.out.println("XXX: sent poll reply to " + iface.m_broadcast);
						}
					} catch (IOException e) {
						m_logger.logError("ArtNetMonitorWindow: Poll Reply Init Send Error",
											60000, "To " + iface.m_broadcast + " " + e);
					}
				}
			}
		}
	}
	
	/**
	 * Custom version of ArtNetChannel.
	 */
	private class MyArtNetChannel extends ArtNetChannel
	{
		public MyArtNetChannel(Receiver receiver, Collection<Integer> listenPorts) throws IOException
		{
			super(receiver, listenPorts);
		}

		@Override
		protected Set<InetAddress> getBindAddrs() throws UnknownHostException
		{
			return !m_bindAddrs.isEmpty() ? m_bindAddrs : super.getBindAddrs();
		}
	}
}
