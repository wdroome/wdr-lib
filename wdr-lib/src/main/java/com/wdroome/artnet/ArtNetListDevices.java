package com.wdroome.artnet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ListIterator;

import java.io.IOException;
import java.io.FileReader;
import java.io.PrintStream;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.wdroome.artnet.msgs.RdmParamId;
import com.wdroome.artnet.msgs.RdmProductCategories;

/**
 * Get the standard information for all RDM devices in the DMX network,
 * and display it in various formats. This is a "main application"
 * rather than a class used by other tools.
 * Art-Net (TM) Designed by and Copyright Artistic License Holdings Ltd.
 * @author wdr
 */
public class ArtNetListDevices
{
	public static void main(String[] args)
	{
		List<String> argList = new ArrayList<>();
		if (args != null) {
			for (String arg: args) {
				argList.add(arg);
			}
		}
		try (ArtNetManager manager = makeManager(argList)) {
			ArrayList<String> errors = new ArrayList<>();
			Map<ACN_UID, RdmDevice> deviceMap = manager.getDeviceMap(errors);
			if (argList.isEmpty()) {
				prtDevices(deviceMap, errors);
			} else {
				String cmd = argList.get(0);
				if (cmd.equals("-tab")) {
					prtTabSep(deviceMap, errors);
				} else if (cmd.equals("-cmp")) {
					if (argList.size() < 2) {
						System.err.println("Usage error: -cmp filename");
					} else {
						cmpFile(argList.get(1), deviceMap, errors);
					}
				} else {
					System.err.println("Unknown argument \"" + cmd + "\"");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	/**
	 * Pretty-print the device information for direct human consumption.
	 * @param deviceMap Information for all RDM devices.
	 * @param errors Any errors that occurred while getting the device information.
	 */
	private static void prtDevices(Map<ACN_UID, RdmDevice> deviceMap, List<String> errors)
	{
		if (!errors.isEmpty()) {
			System.out.println("errors: " + errors);
		}
		System.out.println(deviceMap.size() + " devices:");
		int iDev = 0;
		String indent = "    ";
		for (RdmDevice devInfo: RdmDevice.sort(deviceMap.values())) {
			iDev++;
			System.out.println();
			System.out.println("Device " + iDev + "  [" + devInfo.m_uid + "]:");
			System.out.println(indent + devInfo.m_manufacturer + "/" + devInfo.m_model + "  ("
						+ RdmProductCategories.getCategoryName(devInfo.m_deviceInfo.m_category) + ")");
			
			if (devInfo.m_deviceInfo.m_startAddr > 0 || devInfo.m_deviceInfo.m_dmxFootprint > 0) {
				System.out.println(indent + "dmx addresses: " + devInfo.m_deviceInfo.m_startAddr
							+ "-" + (devInfo.m_deviceInfo.m_startAddr
										+ devInfo.m_deviceInfo.m_dmxFootprint - 1)
							+ " univ: " + devInfo.m_nodePort);
			} else {
				System.out.println(indent + "univ: " + devInfo.m_nodePort);
			}
			System.out.println(indent + "dmx config " + devInfo.getPersonalityDesc());
			System.out.println(indent + "version: " + devInfo.m_softwareVersionLabel);
			if (!devInfo.m_slotDescs.isEmpty()) {
				System.out.print(indent + "slots: ");
				int lineLen = indent.length() + 6;
				String sep = " ";
				for (Map.Entry<Integer,String> ent: devInfo.m_slotDescs.entrySet()) {
					String s = ent.getKey() + ": " + ent.getValue();
					if (lineLen + s.length() > 75) {
						System.out.println();
						System.out.print(indent + indent);
						lineLen = 2*indent.length();
						sep = "";
					}
					System.out.print(sep + s);
					lineLen += s.length() + sep.length();
					sep = " ";
				}
				System.out.println();
			}
			if (!devInfo.m_personalities.isEmpty()) {
				System.out.println(indent + "available configurations:");
				for (int iPers: devInfo.m_personalities.keySet()) {
					System.out.println(indent + indent + devInfo.getPersonalityDesc(iPers));
				}
			}
			if (!devInfo.m_stdParamIds.isEmpty() || !devInfo.m_otherParamIds.isEmpty()) {
				System.out.print(indent + "supported parameters:");
				int lineLen = 1000;
				String sep = "";
				for (RdmParamId pid: devInfo.m_stdParamIds) {
					String s = pid.toString();
					if (lineLen + s.length() > 75) {
						System.out.println();
						System.out.print(indent + indent);
						lineLen = 2*indent.length();
						sep = "";
					}
					System.out.print(sep + s);
					lineLen += s.length() + sep.length();
					sep = " ";
				}
				for (int pid: devInfo.m_otherParamIds) {
					String s = "0x" + Integer.toHexString(pid);
					if (lineLen + s.length() > 75) {
						System.out.println();
						System.out.print(indent + indent);
						lineLen = 2*indent.length();
						sep = "";
					}
					System.out.print(sep + s);
					lineLen += s.length() + sep.length();
					sep = " ";
				}
				System.out.println();
			}
		}		
	}
	
	private static void prtTabSep(Map<ACN_UID, RdmDevice> deviceMap, List<String> errors)
	{
		if (!errors.isEmpty()) {
			System.err.println("errors: " + errors);
		}
		PrintStream out = System.out;
		String sep = "";
		for (String colName: COL_NAMES) {
			out.print(sep + colName);
			sep = "\t";
		}
		out.println();
		sep = "\t";
		for (Map.Entry<ACN_UID, RdmDevice> ent: deviceMap.entrySet()) {
			RdmDevice dev = ent.getValue();
			out.print(dev.m_uid);
			out.print(sep);
			out.print(dev.m_nodePort.m_port);
			out.print(sep);
			out.print(dev.m_deviceInfo.m_startAddr);
			out.print(sep);
			out.print(dev.m_deviceInfo.m_dmxFootprint);
			out.print(sep);
			out.print(dev.m_deviceInfo.m_currentPersonality);
			out.print(sep);
			out.print(dev.m_softwareVersionLabel);
			out.print(sep);
			out.print(String.format("0x%04x", dev.m_deviceInfo.m_category));
			out.print(sep);
			out.print(dev.getCategoryName());
			out.print(sep);
			out.print(String.format("0x%04x", dev.m_uid.getManufacturer()));
			out.print(sep);
			out.print(dev.m_manufacturer);
			out.print(sep);
			out.print(String.format("0x%04x", dev.m_deviceInfo.m_model));
			out.print(sep);
			out.print(dev.m_model);
			out.print(sep);
			out.print(dev.getPersonalityDesc());
			out.println();
		}
	}
	
	private static void cmpFile(String fname, Map<ACN_UID, RdmDevice> deviceMap, List<String> errors)
	{
		
	}
	
	private static String[] COL_NAMES = new String[] {
			"UID",
			"Univ",
			"DMX-Addr",
			"DMX-Slots",
			"ConfigNum",
			"Version",
			"CategoryID",
			"CategoryName",
			"MakeID",
			"MakeName",
			"ModelID",
			"ModelName",
			"ConfigName"
		};
	
	private static ArtNetManager makeManager(List<String> args) throws IOException
	{
		ArtNetManager mgr = new ArtNetManager();
		Long longVal;
		List<InetAddress> pollAddrs = new ArrayList<>();
		for (ListIterator<String> iter = args.listIterator(); iter.hasNext(); ) {
			String arg = iter.next();
			if ((longVal = parseNumValueArg("-poll=", arg)) != null) {
				mgr.setPollReplyWaitMS(longVal);
				iter.remove();
			} else if ((longVal = parseNumValueArg("-tod=", arg)) != null) {
				mgr.setTodDataWaitMS(longVal);
				iter.remove();
			} else if (arg.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+")) {
				pollAddrs.add(InetAddress.getByName(arg));
				iter.remove();
			}
		}
		if (!pollAddrs.isEmpty()) {
			mgr.setInetAddrs(pollAddrs);
		}
		return mgr;
	}
	
	private static Long parseNumValueArg(String prefix, String arg)
	{
		if (arg != null && arg.startsWith(prefix)) {
			return Long.parseLong(arg.substring(prefix.length()));
		} else {
			return null;
		}
	}
}
