package com.wdroome.artnet.util;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.Font;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

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

import com.wdroome.artnet.ArtNetChannel;
import com.wdroome.artnet.ArtNetConst;
import com.wdroome.artnet.ArtNetDmx;
import com.wdroome.artnet.ArtNetPort;
import com.wdroome.util.swing.JTextAreaErrorLogger;
import com.wdroome.util.IErrorLogger;
import com.wdroome.util.SystemErrorLogger;
import com.wdroome.util.swing.SwingAppUtils;

public class ArtNetMonitorWindow extends JFrame
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
    	private JTabbedPane m_tabPane;
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
			// setLayout(new FlowLayout(FlowLayout.LEFT, CHANBOX_HSEP, CHANBOX_VSEP));
			setBorder(new EmptyBorder(4,4,4,4));
			setLayout(new GridLayout(0, 10, CHANBOX_HSEP, CHANBOX_VSEP));
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
	private Dimension m_chanBoxSize;
	
	private ArtNetChannel m_channel;
	private AtomicBoolean m_running = new AtomicBoolean(true);

	private ArrayList<AtomicReference<DmxMsgTS>> m_lastDmxMsg = new ArrayList<>();
	private ArrayList<AtomicLong> m_numDmxMsgs = new ArrayList<>();
	private AtomicLong m_numBadDmxMsgs = new AtomicLong(0);
	private AtomicLong m_numPollMsgs = new AtomicLong(0);
	
	private AnPortDisplay[] m_anPortDisplays;
	private IErrorLogger m_logger = new SystemErrorLogger();	// C'tor replaces. This ensures it's never null.

	public static void main(String[] args)
	{
		SwingAppUtils.startSwing(() -> new ArtNetMonitorWindow(args), args);
	}
	
	public ArtNetMonitorWindow(String[] args)
	{
		super();
		if (!parseArgs(args)) {
			return;
		}
		init();
	}
	
	private void init()
	{
		m_logger = new JTextAreaErrorLogger( /* alert-text-style, alert-text-id */ );
		m_chanBoxSize = new ChanBox(new JLabel("888"), new JLabel("---")).getPreferredSize();
		// System.out.println("XXX: chanbox size: " + m_chanBoxSize);
		setSize(400,300);
		JTabbedPane tabPane = new JTabbedPane();
		
    	m_anPortDisplays = new AnPortDisplay[m_anPorts.size()];
		for (int i = 0; i < m_anPorts.size(); i++) {
			m_anPortDisplays[i] = new AnPortDisplay();
			m_anPortDisplays[i].m_tabPane = tabPane;
			m_anPortDisplays[i].m_tabIndex = i;
			m_anPortDisplays[i].m_anPort = m_anPorts.get(i);
			makePortTab(m_anPortDisplays[i]);
		}
		
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
		
		getContentPane().add(tabPane);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setVisible(true);
	}
	
	private boolean parseArgs(String[] args)
	{
		try {
			for (String arg: args) {
				if (arg.startsWith("font=")) {
					g_chanNumValueFont = Font.decode(arg.substring(5));
				} else if (arg.matches("[0-9]+")) {
					m_ipPorts.add(Integer.valueOf(arg));
				} else if (m_anPorts.size() < ArtNetConst.MAX_PORTS_PER_NODE) {
					m_anPorts.add(new ArtNetPort(arg));
				}
			}
		} catch (Exception e) {
			System.err.println("Usage: ArtNetMonitorWindow [font=name-style-size | ip-port-number | an-port] ...");
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
    	
		StringBuilder title = new StringBuilder();
		title.append("Art-Net Monitor Node, IP ports");
		for (int p: m_ipPorts) {
			title.append(" " + p);
		}
		setTitle(title.toString());
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
				// disp.m_chanBoxes[i].setMaximumSize(m_chanBoxSize);
			}
			if (i < disp.m_numActiveChannels) {
				disp.m_chanPanel.add(disp.m_chanBoxes[i]);
			}
		}
		
		if (true) {
			JScrollPane scrollPane = new JScrollPane(disp.m_chanPanel);
			disp.m_scrollPane = scrollPane;
			scrollPane.setWheelScrollingEnabled(true);
			final JViewport viewport = scrollPane.getViewport();
			viewport.setBackground(BACKGROUND_COLOR);
			viewport.addComponentListener(new ComponentAdapter() {
				public void componentResized(ComponentEvent e) {
					resizeChanBoxPanel(disp);
				}
			});
			// scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			// scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
			disp.m_tabPane.add(scrollPane);
		} else {
			disp.m_tabPane.add(disp.m_chanPanel);
			disp.m_scrollPane = null;
		}
		setTabTitle(disp.m_tabPane, disp.m_tabIndex, disp.m_anPort, 0);
	}
    
    private static void setTabTitle(JTabbedPane tabPane, int tabIndex, ArtNetPort anPort, long numMsgs)
    {
    	tabPane.setTitleAt(tabIndex, "Port: " + anPort + "  #DMX: " + numMsgs);
    }
    
    private void resizeChanBoxPanel(AnPortDisplay disp)
    {
    	if (disp.m_scrollPane != null) {
	    	int vpwid = disp.m_scrollPane.getViewport().getSize().width;
	    	int ncols = (vpwid - 16)/(m_chanBoxSize.width + CHANBOX_HSEP);
	    	LayoutManager layoutMgr = disp.m_chanPanel.getLayout();
	    	if (layoutMgr instanceof GridLayout) {
	    		((GridLayout)layoutMgr).setColumns(ncols);
	    	}
	    	
			if (false) {
				System.out.println("XXX Resized " + disp.m_tabIndex + " " + disp.m_scrollPane.getViewport().getSize()
						+ " " + ncols);
			}
			if (false) {
				int nrows = (disp.m_numActiveChannels + 1) / ncols;
				disp.m_chanPanel.setMaximumSize(
						new Dimension(vpwid - 2, CHANBOX_VSEP + nrows * (m_chanBoxSize.height + CHANBOX_VSEP)));
				disp.m_chanPanel.repaint();
			}
    	}
    }
}
