package com.wdroome.util.inet;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import com.wdroome.util.ImmutableList;

/**
 * Information about an internet address and its subnet on the local host.
 * This is a convenience class with data from java.net.NetworkInterface
 * and java.net.NetworkAddress.
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

	/** A local address on this interface. */
	public final InetAddress m_address;

	/** The CIDR for the subnet with m_address. */
	public final CIDRAddress m_cidr;
	
	/** The broadcast address for the subnet with m_address. May be null. */
	public final InetAddress m_broadcast;
	
	/** All internet addresses. */
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
	 * Return the local address followed by the interface name and the mask length.
	 */
	@Override
	public String toString()
	{
		return m_address.getHostAddress() + "[" + m_interfaceName + "/"
					+ (m_cidr != null ?  m_cidr.getMaskLen() : 0) + "]";
	}
	
	/**
	 * Return all available InetInterfaces.
	 * The returned list is read-only.
	 * @return All read-only list of all available InetInterfaces.
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
			g_inetInterfaces = new ImmutableList<InetInterface>(interfaces);
		}
		return g_inetInterfaces;
	}
	
	/**
	 * Print all interfaces on this host.
	 * @param args
	 */
	public static void main(String[] args)
	{
		for (InetInterface iface: InetInterface.getAllInterfaces()) {
			System.out.println(iface);
		}
	}
}
