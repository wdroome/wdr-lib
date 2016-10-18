package com.wdroome.util.inet;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * @author wdr
 */
public class InetInterface
{
	/** The interface name (see {@link NetworkInterface#getName()}. */
	public final String m_interfaceName;
	
	/** The interface display name (see {@link NetworkInterface#getDisplayName()}. */
	public final String m_displayName;
	
	/** True iff this is a loopback interface. */
	public final boolean m_isLoopback;

	/** The local address on this interface. */
	public final InetAddress m_address;

	/** The CIDR for this interface. */
	public final CIDRAddress m_cidr;
	
	/** The broadcast address on this interface. May be null. */
	public final InetAddress m_broadcast;
	
	/** All network interfaces. */
	private static List<InetInterface> g_inetInterfaces = null;
	
	/**
	 * Create a new InetInterface.
	 * @param ni The Network Interface.
	 * @param ia An address within the ni interface.
	 * @throws SocketException As thrown by NetworkInterface.isLoopback().
	 * @throws UnknownHostException Should not happen. 
	 * 
	 */
	public InetInterface(NetworkInterface ni, InterfaceAddress ia)
			throws SocketException, UnknownHostException
	{
		m_interfaceName = ni.getName();
		m_displayName = ni.getDisplayName();
		m_isLoopback = ni.isLoopback();
		m_address = ia.getAddress();
		m_broadcast = ia.getBroadcast();
		m_cidr = new CIDRAddress(ia.getAddress().getAddress(),
								ia.getNetworkPrefixLength());
	}
	
	/**
	 * Return the local inet address in the same interface as a remote address.
	 * @param rmtAddr The remote address.
	 * @return The local inet address in the same interface as rmtAddr,
	 * 		or null if there is none.
	 */
	public static InetAddress getLocalAddr(InetAddress rmtAddr)
	{
		for (InetInterface iface: getAllInterfaces()) {
			if (iface.m_cidr.contains(rmtAddr)) {
				return iface.m_address;
			}
		}
		return null;
	}
	
	/**
	 * Return all available InetInterfaces.
	 * @return All available InetInterfaces.
	 */
	public static synchronized List<InetInterface> getAllInterfaces()
	{
		if (g_inetInterfaces == null) {
			List<InetInterface> interfaces = new ArrayList<InetInterface>();
			try {
				Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
				while (nis.hasMoreElements()) {
					NetworkInterface ni = nis.nextElement();
					for (InterfaceAddress ia: ni.getInterfaceAddresses()) {
						try {
							interfaces.add(new InetInterface(ni, ia));
						} catch (Exception e) {
							// Ignore. Shouldn't happen.
						}
					}
				}
			} catch (SocketException e1) {
				// Very odd -- shouldn't happen.
			}
			g_inetInterfaces = interfaces;
		}
		return g_inetInterfaces;
	}
}
