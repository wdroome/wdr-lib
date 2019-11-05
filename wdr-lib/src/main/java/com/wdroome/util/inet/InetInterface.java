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
import com.wdroome.util.MiscUtil;

/**
 * Information about an internet address and its subnet on the local host.
 * This is a convenience class which combines data from
 * java.net.NetworkInterface and java.net.NetworkAddress.
 * It only considers the interfaces which are "up."
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

	/** The host's local address on this interface. */
	public final InetAddress m_address;

	/** The CIDR for the subnet with m_address. */
	public final CIDRAddress m_cidr;
	
	/** The broadcast address for the subnet with m_address. May be null. */
	public final InetAddress m_broadcast;
	
	/**
	 * The hardware address for the interface. Usually a MAC address.
	 * Never null, but may be 0-length if the caller does not have permission
	 * to get the hardware address.
	 */
	public final byte[] m_hardwareAddress;
	
	/** All internet addresses. */
	private static List<InetInterface> g_allInterfaces = null;
	
	/** All internet interfaces with a broadcast address. */
	private static List<InetInterface> g_bcastInterfaces = null;
	
	/**
	 * Create a new InetInterface.
	 * @param ni The Network Interface.
	 * @param ia An address within the ni interface.
	 */
	public InetInterface(NetworkInterface ni, InterfaceAddress ia)
	{
		m_interfaceName = ni.getName();
		m_displayName = ni.getDisplayName();
		m_isLoopback = isLoopback(ni);
		m_hardwareAddress = getHardwareAddress(ni);
		m_address = ia.getAddress();
		m_broadcast = ia.getBroadcast();
		m_cidr = new CIDRAddress(ia.getAddress(), ia.getNetworkPrefixLength());
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
	 * Return all available InetInterfaces which are "up".
	 * The returned list is read-only.
	 * @return All read-only list of all available InetInterfaces.
	 */
	public static synchronized List<InetInterface> getAllInterfaces()
	{
		if (g_allInterfaces == null) {
			List<InetInterface> interfaces = new ArrayList<InetInterface>();
			List<InetInterface> broadcasts = new ArrayList<InetInterface>();
			try {
				Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
				while (nis.hasMoreElements()) {
					NetworkInterface ni = nis.nextElement();
					if (!isUp(ni)) {
						continue;
					}
					for (InterfaceAddress ia: ni.getInterfaceAddresses()) {
						try {
							InetInterface ii = new InetInterface(ni, ia);
							interfaces.add(ii);
							if (ii.m_broadcast != null) {
								broadcasts.add(ii);
							}
						} catch (Exception e) {
							// Ignore. Shouldn't happen.
						}
					}
				}
			} catch (SocketException e1) {
				// Very odd -- shouldn't happen.
			}
			g_allInterfaces = new ImmutableList<InetInterface>(interfaces);
			g_bcastInterfaces = new ImmutableList<InetInterface>(broadcasts);
		}
		return g_allInterfaces;
	}
	
	/**
	 * Get the MAC address for an InetAddress which this machine listens to.
	 * @param inetAddr A (local) internet address.
	 * @return The hardware address (generally the MAC address)
	 * 		for the interface with inetAddr. If no interface has
	 * 		that addresses, or if inetAddr is null, return a new byte[0].
	 */
	public static synchronized byte[] getMacAddress(InetAddress inetAddr)
	{
		if (inetAddr == null) {
			return new byte[0];
		}
		getAllInterfaces();
		for (InetInterface iface: g_allInterfaces) {
			if (iface.m_address.equals(inetAddr)) {
				return iface.m_hardwareAddress;
			}
		}
		for (InetInterface iface: g_bcastInterfaces) {
			if (iface.m_address.equals(inetAddr)) {
				return iface.m_hardwareAddress;
			}
		}
		return new byte[0];
	}
	
	private static boolean isUp(NetworkInterface ni)
	{
		try {
			return ni.isUp();
		} catch (SocketException e) {
			return false;
		}
	}
	
	private static boolean isLoopback(NetworkInterface ni)
	{
		try {
			return ni.isLoopback();
		} catch (SocketException e) {
			return false;
		}
	}
	
	private static byte[] getHardwareAddress(NetworkInterface ni)
	{
		try {
			byte[] hardwareAddr = ni.getHardwareAddress();
			return hardwareAddr != null ? hardwareAddr : new byte[0];
		} catch (SocketException e) {
			return new byte[0];
		}
	}
	
	/**
	 * Return all available InetInterfaces which are "up" and which support broadcast.
	 * The returned list is read-only.
	 * @return All read-only list of all available broadcast InetInterfaces.
	 */
	public static List<InetInterface> getBcastInterfaces()
	{
		getAllInterfaces();
		return g_bcastInterfaces;
	}
	
	/**
	 * 
	 */
	public static synchronized void rescan()
	{
		g_allInterfaces = null;
		g_bcastInterfaces = null;
	}
	
	/**
	 * Print all interfaces on this host.
	 * @param args
	 */
	public static void main(String[] args)
	{
		System.out.println("All Interfaces:");
		for (InetInterface iface: InetInterface.getAllInterfaces()) {
			System.out.println("  " + iface + " mac: " + MiscUtil.bytesToHex(iface.m_hardwareAddress));
		}
		System.out.println("Broadcast Interfaces:");
		for (InetInterface iface: InetInterface.getBcastInterfaces()) {
			System.out.println("  " + iface.m_broadcast.getHostAddress() + " " + iface.m_cidr
						+ " mac: " + MiscUtil.bytesToHex(iface.m_hardwareAddress));
		}
		for (String arg: args) {
			try {
				InetAddress inetAddr = InetAddress.getByName(arg);
				byte[] mac = getMacAddress(inetAddr);
				System.out.println(arg + ": " + (mac.length > 0 ? MiscUtil.bytesToHex(mac) : "[NOT FOUND]"));
			} catch (Exception e) {
				System.err.println(arg + ": " + e);
				e.printStackTrace();
			}
		}
	}
}
