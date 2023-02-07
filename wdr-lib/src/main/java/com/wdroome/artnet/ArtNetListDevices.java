package com.wdroome.artnet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.EnumMap;
import java.util.Set;
import java.util.TreeSet;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.PrintStream;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.wdroome.artnet.msgs.RdmParamId;
import com.wdroome.artnet.msgs.RdmProductCategories;

import com.wdroome.util.ArrayToList;

/**
 * Get the standard information for all RDM devices in the DMX network,
 * and display it in various formats. This is a "main application"
 * rather than a class used by other tools.
 * Art-Net (TM) Designed by and Copyright Artistic License Holdings Ltd.
 * @author wdr
 */
public class ArtNetListDevices
{
	private static boolean g_printOkDevs = false;

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
	
	/**
	 * Print the primary fields in tab-sep format, with column names in the first line.
	 * @param deviceMap The discovered devices.
	 * @param errors The errors encountered when discovering the devices.
	 * 		If there were no errors, this will be null or empty.
	 */
	private static void prtTabSep(Map<ACN_UID, RdmDevice> deviceMap, List<String> errors)
	{
		if (!errors.isEmpty()) {
			System.err.println("errors: " + errors);
		}
		PrintStream out = System.out;
		String sep = "";
		for (ColName col: ColName.values()) {
			out.print(sep + col);
			sep = "\t";
		}
		out.println();
		sep = "\t";
		for (Map.Entry<ACN_UID, RdmDevice> ent: deviceMap.entrySet()) {
			sep = "";
			ColNameMap flds = makeDeviceFldMap(ent.getValue());
			for (ColName col: ColName.values()) {
				String val = flds.get(col);
				out.print(sep);
				out.print(val != null ? val : "");
				sep = "\t";
			}
			out.println();
		}
	}
	
	/**
	 * Compare the current device configuration against a previous configuration saved by
	 * {@link #prtTabSep(Map, List)}.
	 * @param fname A tab-sep file previously created by {@link #prtTabSep(Map, List)}.
	 * @param deviceMap The discovered devices.
	 * @param errors The errors encountered when discovering the devices.
	 * 		If there were no errors, this will be null or empty.
	 */
	private static void cmpFile(String fname, Map<ACN_UID, RdmDevice> deviceMap, List<String> errors)
	{
		Map<ACN_UID, ColNameMap> fileUidMap = readCmpFile(fname);
		if (fileUidMap == null) {
			return;
		}
		PrintStream out = System.out;
		Set<ACN_UID> allUids = new TreeSet<>(fileUidMap.keySet());
		allUids.addAll(deviceMap.keySet());
		int nNew = 0;
		int nMissing = 0;
		int nChanged = 0;
		int nOk = 0;
		for (ACN_UID uid: allUids) {
			ColNameMap fileValues = fileUidMap.get(uid);
			ColNameMap devValues = makeDeviceFldMap(deviceMap.get(uid));
			if (fileValues == null) {
				prtUidBasics(out, "NEW", uid, devValues);
				nNew++;
			} else if (devValues == null) {
				prtUidBasics(out, "MISSING", uid, fileValues);
				nMissing++;
			} else {
				List<String> diffs = cmpDevInfo(devValues, fileValues);
				if (!diffs.isEmpty()) {
					prtUidBasics(out, "CHANGED", uid, devValues);
					nChanged++;
					for (String change: diffs) {
						out.println("      " + change);
					}
				} else {
					if (g_printOkDevs) {
						prtUidBasics(out, "OK", uid, devValues);
					}
					nOk++;
				}
			}
		}
		out.println("Ok: " + nOk + "  Changed: " + nChanged
					+ "  New: " + nNew + "  Missing: " + nMissing);
	}
	
	private static void prtUidBasics(PrintStream out, String prefix, ACN_UID uid, ColNameMap devInfo)
	{
		String prefixFmt = "%-8s ";
		out.println(String.format(prefixFmt, prefix) + uid + ":"
						+ " addr=" + fmtDmxAddr(devInfo) + "/" + devInfo.get(ColName.Univ)
						+ " make=" + devInfo.get(ColName.MakeName)
									+ "/" + devInfo.get(ColName.ModelName)
					);
	}
	
	private static String fmtDmxAddr(ColNameMap devInfo)
	{
		String s = devInfo.get(ColName.DmxAddr);
		try {
			return String.format("%03d", Integer.parseInt(s));
		} catch (Exception e) {
			return s;
		}
	}
	
	private static List<String> cmpDevInfo(ColNameMap curInfo, ColNameMap prevInfo)
	{
		ArrayList<String> diffs = new ArrayList<>();
		for (ColName col: ColName.values()) {
			cmpCol(diffs, col, curInfo, prevInfo);
		}
		return diffs;
	}
	
	private static void cmpCol(List<String> diffs, ColName col, ColNameMap curInfo, ColNameMap prevInfo)
	{
		String cur = curInfo.get(col);
		String prev = prevInfo.get(col);
		if (!cur.equals(prev)) {
			diffs.add(col + ": now=" + cur + " prev=" + prev);
		}
	}
	
	private static Map<ACN_UID, ColNameMap> readCmpFile(String fname)
	{
		Map<ACN_UID, ColNameMap> uidMap = new HashMap<>();
		try (LineNumberReader rdr = new LineNumberReader(new FileReader(fname))) {
			String line;
			ColName[] colNames = null;
			int iUidCol = -1;
			while ((line = rdr.readLine()) != null) {
				if (line.isBlank() || line.trim().startsWith("#")) {
					continue;
				}
				String[] lineFlds = line.split("\t");
				if (colNames == null) {
					// Header line.
					colNames = new ColName[lineFlds.length];
					for (int i = 0; i < lineFlds.length; i++) {
						try {
							colNames[i] = ColName.valueOf(lineFlds[i]);
							if (colNames[i] == ColName.UID) {
								iUidCol = i;
							}
						} catch (Exception e) {
							System.err.println("File \"" + fname + "\": Unknown header name \""
												+ lineFlds[i] + "\"");
							return null;
						}
					}
					if (iUidCol < 0) {
						System.out.println("File \"" + fname + "\": No " + ColName.UID + " column.");
						return null;
					}
				} else {
					// UID value line.
					ColNameMap uidFlds = new ColNameMap();
					int n = Math.min(lineFlds.length, colNames.length); 
					for (int i = 0; i < n; i++) {
						uidFlds.put(colNames[i], lineFlds[i]);
					}
					try {
						uidMap.put(new ACN_UID(lineFlds[iUidCol]), uidFlds);
					} catch (Exception e) {
						System.err.println("File \"" + fname + "\": Invalid "
										+ ColName.UID + " in line " + rdr.getLineNumber() + ".");
					}
				}
			}
			return uidMap;
		} catch (IOException e) {
			System.err.println("Cannot read file \"" + fname + "\"");
			return null;
		}		
	}
	
	/**
	 * The columns in a tab-sep file created by {@link ArtNetListDevices#prtTabSep(Map, List)}.
	 */
	private static enum ColName {
		UID,
		Univ,
		DmxAddr,
		DmxSlots,
		ConfigNum,
		Version,
		CategoryId,
		CategoryName,
		MakeId,
		MakeName,
		ModelId,
		ModelName,
		ConfigName
	};
	
	/**
	 * Map column names to String values. The extension punts for missing columns.
	 */
	private static class ColNameMap extends EnumMap<ColName,String>
	{
		private ColNameMap()
		{
			super(ColName.class);
		}
		
		/**
		 * Get the value for a column. Never return null, and substiture for missing values.
		 */
		@Override
		@SuppressWarnings("incomplete-switch")
		public String get(Object xkey)
		{
			if (!(xkey instanceof ColName)) {
				return null;
			}
			ColName colName = (ColName)xkey;
			String v = super.get(colName);
			if (v != null && !v.isBlank()) {
				return v;
			}
			switch (colName)
			{
			case MakeName: v = super.get(ColName.MakeId); break;
			case ModelName: v = super.get(ColName.ModelId); break;
			case ConfigName: v = super.get(ColName.ConfigNum); break;
			case CategoryName: v = super.get(ColName.CategoryId); break;
			}
			if (v == null) {
				v = "???";
			}
			return v;
		}
	}
	
	private static ColNameMap makeDeviceFldMap(RdmDevice deviceInfo)
	{
		if (deviceInfo == null) {
			return null;
		}
		ColNameMap map = new ColNameMap();
		map.put(ColName.UID, deviceInfo.m_uid.toString());		
		map.put(ColName.Univ, deviceInfo.m_nodePort.m_port.toString());
		map.put(ColName.DmxAddr, deviceInfo.m_deviceInfo.m_startAddr + "");
		map.put(ColName.DmxSlots, deviceInfo.m_deviceInfo.m_dmxFootprint + "");
		map.put(ColName.ConfigNum, deviceInfo.m_deviceInfo.m_currentPersonality + "");
		map.put(ColName.Version, deviceInfo.m_softwareVersionLabel);
		map.put(ColName.CategoryId, String.format("0x%04x", deviceInfo.m_deviceInfo.m_category));
		map.put(ColName.CategoryName, deviceInfo.getCategoryName());
		map.put(ColName.MakeId, String.format("0x%04x", deviceInfo.m_uid.getManufacturer()));
		map.put(ColName.MakeName, deviceInfo.m_manufacturer);
		map.put(ColName.ModelId, String.format("0x%04x", deviceInfo.m_deviceInfo.m_model));
		map.put(ColName.ModelName, deviceInfo.m_model);
		map.put(ColName.ConfigName, deviceInfo.getPersonalityDesc());
		return map;
	}
	
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
			} else if (arg.matches("-ok")) {
				g_printOkDevs = true;
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
