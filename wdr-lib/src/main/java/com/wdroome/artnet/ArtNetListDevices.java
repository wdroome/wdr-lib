package com.wdroome.artnet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.EnumMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.io.File;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.wdroome.util.CommandReader;
import com.wdroome.util.EnumFinder;

import com.wdroome.json.JSONValueTypeException;
import com.wdroome.json.JSONParseException;

import com.wdroome.artnet.msgs.RdmParamId;
import com.wdroome.artnet.msgs.RdmParamResp;
import com.wdroome.artnet.msgs.RdmProductCategories;
import com.wdroome.artnet.msgs.RdmPacket;

import com.wdroome.artnet.util.ArtNetTestNode;

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
	private ArtNetChannel m_channel = null;
	private ArtNetManager m_manager = null;
	
	public static void main(String[] args) throws JSONParseException, JSONValueTypeException, IOException
	{
		ArtNetListDevices listDevs = new ArtNetListDevices(args);
	}

	public ArtNetListDevices(String[] args)
			throws IOException, JSONParseException, JSONValueTypeException
	{
		List<String> argList = new ArrayList<>();
		if (args != null) {
			for (String arg: args) {
				argList.add(arg);
			}
		}

		boolean useTestNode = false;
		File testNodeParamFile = null;
		for (ListIterator<String> iter = argList.listIterator(); iter.hasNext(); ) {
			String arg = iter.next();
			if (arg.startsWith("-testnode")) {
				arg = arg.substring("-testnode".length());
				if (arg.startsWith("=")) {
					testNodeParamFile = new File(arg.substring(1));
				}
				useTestNode = true;
				iter.remove();
			}
		}
		ArtNetTestNode testNode = null;
		if (useTestNode) {
			m_channel = new ArtNetChannel();
			testNode = new ArtNetTestNode(m_channel, null, testNodeParamFile, testNodeParamFile);
		}

		try {
			m_manager = makeManager(argList);
			if (argList.size() >= 1 && argList.get(0).startsWith("-i")) {
				argList.remove(0);
				new ReadCmds(argList);
			} else {
				ArrayList<String> errors = new ArrayList<>();
				Map<ACN_UID, RdmDevice> deviceMap = m_manager.getDeviceMap(errors);
				if (argList.isEmpty()) {
					System.out.println("Found " + deviceMap.size() + " RDM Devices.");
					prtErrors(errors, null);
					prtDevices(RdmDevice.sortByAddr(deviceMap.values()), System.out, null);
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
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (m_manager != null) {
				try {
					m_manager.close();
					m_manager = null;
				} catch (Exception e) {
					// ignore
				}
			}
		}
	}
	
	private void prtErrors(List<String> errors, PrintStream out)
	{
		if (out == null) {
			out = System.out;
		}
		if (errors != null && !errors.isEmpty()) {
			out.println("Errors:");
			for (String err: errors) {
				out.println("   " + err);
			}				
		}
	}
	
	/**
	 * Pretty-print the device information for direct human consumption.
	 * @param devices Information for all RDM devices.
	 * @param out The output stream. If null, use stdout.
	 */
	private void prtDevices(List<RdmDevice> devices, PrintStream out, List<Integer> devNums)
	{
		if (out == null) {
			out = System.out;
		}
		if (devNums == null || devNums.isEmpty()) {
			devNums = makeIntList(1, devices.size());
		}
		String indent = "    ";
		// for (RdmDevice devInfo: devices) {
		for (int iDev: devNums) {
			if (!(iDev >= 1 && iDev <= devices.size())) {
				out.println("Device " + iDev + ": Invalid device number.");
				continue;
			}
			RdmDevice devInfo = devices.get(iDev-1);
			out.println();
			out.println("Device " + iDev + "  [" + devInfo.m_uid + "]:");
			String devLabel = devInfo.getDeviceLabel();
			out.println(indent + devInfo.m_manufacturer + "/" + devInfo.m_model
						+ "  (" + devInfo.getCategoryName() + ")"
						+ (devLabel != null && !devLabel.isBlank() ? (" \"" + devLabel + "\"") : ""));
			
			if (devInfo.getDmxStartAddr() > 0 || devInfo.getDmxFootprint() > 0) {
				out.println(indent + "dmx addresses: " + devInfo.getDmxStartAddr()
							+ "-" + (devInfo.getDmxStartAddr()
										+ devInfo.getDmxFootprint() - 1)
							+ " univ: " + devInfo.m_nodePort);
			} else {
				out.println(indent + "univ: " + devInfo.m_nodePort);
			}
			out.println(indent + "dmx config " + devInfo.getPersonalityDesc());
			out.println(indent + "version: " + devInfo.m_softwareVersionLabel
							+ " #subdevs: " + devInfo.getDeviceInfo().m_numSubDevs
							+ " #sensors: " + devInfo.getDeviceInfo().m_numSensors
							+ " hours: " + devInfo.getDeviceHours()
							);
			try {
				Map<Integer,String> slotDescs = devInfo.getSlotDescs();
				if (!slotDescs.isEmpty()) {
					out.print(indent + "slots: ");
					int lineLen = indent.length() + 6;
					String sep = " ";
					for (Map.Entry<Integer,String> ent: slotDescs.entrySet()) {
						String s = ent.getKey() + ": " + ent.getValue();
						if (lineLen + s.length() > 75) {
							out.println();
							out.print(indent + indent);
							lineLen = 2*indent.length();
							sep = "";
						}
						out.print(sep + s);
						lineLen += s.length() + sep.length();
						sep = " ";
					}
					out.println();
				}
			} catch (IOException e1) {
				out.println("Error getting slotdescs: " + e1);
			}
			
			if (!devInfo.m_personalities.isEmpty()) {
				out.println(indent + "available configurations:");
				for (int iPers: devInfo.m_personalities.keySet()) {
					out.println(indent + indent + devInfo.getPersonalityDesc(iPers));
				}
			}
			if (!devInfo.m_sensorDefs.isEmpty()) {
				out.println(indent + "sensors:");
				for (RdmParamResp.SensorDef sensorDef: devInfo.m_sensorDefs.values()) {
					out.println(indent + indent + sensorDef);
					out.println(indent + indent + devInfo.getSensorValue(sensorDef.m_sensorNum));
				}
			}
			if (!devInfo.m_stdParamIds.isEmpty() || !devInfo.m_otherParamIds.isEmpty()) {
				out.print(indent + "supported parameters:");
				int lineLen = 1000;
				String sep = "";
				for (RdmParamId pid: devInfo.m_stdParamIds) {
					String s = pid.toString();
					if (lineLen + s.length() > 75) {
						out.println();
						out.print(indent + indent);
						lineLen = 2*indent.length();
						sep = "";
					}
					out.print(sep + s);
					lineLen += s.length() + sep.length();
					sep = " ";
				}
				for (int pid: devInfo.m_otherParamIds) {
					String s = "0x" + Integer.toHexString(pid);
					if (lineLen + s.length() > 75) {
						out.println();
						out.print(indent + indent);
						lineLen = 2*indent.length();
						sep = "";
					}
					out.print(sep + s);
					lineLen += s.length() + sep.length();
					sep = " ";
				}
				out.println();
			}
		}		
	}
	
	/**
	 * Print a one-line summary of all devices.
	 * @param devices The devices.
	 * @param out The output stream.
	 */
	private void listDevices(List<RdmDevice> devices, PrintStream out, List<Integer> devNums)
	{
		if (out == null) {
			out = System.out;
		}
		if (devNums == null || devNums.isEmpty()) {
			devNums = makeIntList(1, devices.size());
		}
		for (int iDev: devNums) {
			if (!(iDev >= 1 && iDev <= devices.size())) {
				out.println(iDev + ": Invalid device number");
				continue;
			}
			RdmDevice dev = devices.get(iDev-1);
			int startAddr = dev.getDmxStartAddr();
			int endAddr = startAddr + dev.getDmxFootprint() - 1;
			out.println(iDev + ":"
					+ " " + dev.m_uid
					+ " " + dev.m_manufacturer + "/" + dev.m_model
					+ " " + startAddr + "-" + endAddr
					+ " " + dev.m_nodePort
					);
		}
	}
	
	/**
	 * Print the primary fields in tab-sep format, with column names in the first line.
	 * @param deviceMap The discovered devices.
	 * @param errors The errors encountered when discovering the devices.
	 * 		If there were no errors, this will be null or empty.
	 */
	private void prtTabSep(Map<ACN_UID, RdmDevice> deviceMap, List<String> errors)
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
	private void cmpFile(String fname, Map<ACN_UID, RdmDevice> deviceMap, List<String> errors)
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
	
	private void prtUidBasics(PrintStream out, String prefix, ACN_UID uid, ColNameMap devInfo)
	{
		String prefixFmt = "%-8s ";
		out.println(String.format(prefixFmt, prefix) + uid + ":"
						+ " addr=" + fmtDmxAddr(devInfo) + "/" + devInfo.get(ColName.Univ)
						+ " make=" + devInfo.get(ColName.MakeName)
									+ "/" + devInfo.get(ColName.ModelName)
					);
	}
	
	private String fmtDmxAddr(ColNameMap devInfo)
	{
		String s = devInfo.get(ColName.DmxAddr);
		try {
			return String.format("%03d", Integer.parseInt(s));
		} catch (Exception e) {
			return s;
		}
	}
	
	private List<String> cmpDevInfo(ColNameMap curInfo, ColNameMap prevInfo)
	{
		ArrayList<String> diffs = new ArrayList<>();
		for (ColName col: ColName.values()) {
			cmpCol(diffs, col, curInfo, prevInfo);
		}
		return diffs;
	}
	
	private void cmpCol(List<String> diffs, ColName col, ColNameMap curInfo, ColNameMap prevInfo)
	{
		String cur = curInfo.get(col);
		String prev = prevInfo.get(col);
		if (!cur.equals(prev)) {
			diffs.add(col + ": now=" + cur + " prev=" + prev);
		}
	}
	
	private Map<ACN_UID, ColNameMap> readCmpFile(String fname)
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
		 * Get the value for a column. Never return null, and substitute for missing values.
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
		map.put(ColName.DmxAddr, deviceInfo.getDeviceInfo().m_startAddr + "");
		map.put(ColName.DmxSlots, deviceInfo.getDeviceInfo().m_dmxFootprint + "");
		map.put(ColName.ConfigNum, deviceInfo.getDeviceInfo().m_currentPersonality + "");
		map.put(ColName.Version, deviceInfo.m_softwareVersionLabel);
		map.put(ColName.CategoryId, String.format("0x%04x", deviceInfo.getDeviceInfo().m_category));
		map.put(ColName.CategoryName, deviceInfo.getCategoryName());
		map.put(ColName.MakeId, String.format("0x%04x", deviceInfo.m_uid.getManufacturer()));
		map.put(ColName.MakeName, deviceInfo.m_manufacturer);
		map.put(ColName.ModelId, String.format("0x%04x", deviceInfo.getDeviceInfo().m_model));
		map.put(ColName.ModelName, deviceInfo.m_model);
		map.put(ColName.ConfigName, deviceInfo.getPersonalityDesc());
		return map;
	}
	
	private ArtNetManager makeManager(List<String> args) throws IOException
	{
		ArtNetManager mgr = new ArtNetManager(m_channel);
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
	
	private boolean addDevice(RdmDevice rdmDevice, List<RdmDevice> list)
	{
		if (!listContains(rdmDevice, list)) {
			list.add(rdmDevice);
			return true;
		} else {
			return false;
		}
	}
	
	private boolean addDevNumber(int devNum, List<Integer> list)
	{
		if (!list.contains(devNum)) {
			list.add(devNum);
			return true;
		} else {
			return false;
		}
	}
	
	private boolean listContains(RdmDevice rdmDevice, List<RdmDevice> list)
	{
		for (RdmDevice listDev: list) {
			if (rdmDevice.m_uid.equals(listDev.m_uid)) {
				return true;
			}
		}
		return false;
	}
	
	private enum Command implements EnumFinder.AltNames
	{
		PRINT("PRT", null),
		LIST("LS", null),
		NODES(),
		ADDRESS("DMX", "[new-dmx-address]"),
		CONFIG("PERSONALITY", "[new-personality-number]"),
		NAME((String)null, "[new-device-label]"),
		IDENTIFY((String)null, "[on|off]"),
		SORT((String)null, "make | node | addr | uid"),
		SELECT(),
		ADD(),
		REFRESH(),
		HELP("?", null),
		QUIT();
		
		private final String[] m_altNames;
		private final String m_args;
		
		private Command() { m_altNames = null; m_args = ""; }
		
		private Command(String altName, String args) { this(new String[] {altName}, args); }
		
		private Command(String[] altNames, String args) { m_altNames = altNames; m_args = args; }
		
		@Override
		public String toString() { return name().toLowerCase(); }
		
		@Override
		public String[] altNames() { return m_altNames; }
		
		public String help()
		{
			return name().toLowerCase() + (m_args != null && !m_args.isBlank() ? (" " + m_args) : "");
		}
	}
	
	private final EnumFinder<Command> m_cmdFinder = new EnumFinder<>(Command.values());
	
	private List<String> array2List(String[] arr)
	{
		ArrayList<String> list = new ArrayList<>();
		for (String s: arr) {
			list.add(s);
		}
		return list;
	}
	
	private List<Integer> makeIntList(int from, int to)
	{
		List<Integer> list = new ArrayList<>();
		for (int i = from; i <= to; i++) {
			list.add(i);
		}
		return list;
	}
	
	private int[] parseFromTo(String fromToStr)
	{
		String[] fromToArr = fromToStr.split("-", 2);
		int from;
		int to;
		if (fromToArr.length == 1) {
			from = Integer.parseInt(fromToArr[0]);
			to = from;
		} else {
			from = Integer.parseInt(fromToArr[0]);
			to = Integer.parseInt(fromToArr[1]);
		}
		return new int[] {from, to};
	}
	
	private class ReadCmds extends CommandReader
	{
		private List<RdmDevice> m_allDevices;
		private List<Integer> m_selectedDevNums = null;
		
		public ReadCmds(List<String> args) throws IOException
		{
			super(args);
			ArrayList<String> errors = new ArrayList<>();
			m_waitFlag = true;
			Map<ACN_UID, RdmDevice> deviceMap = m_manager.getDeviceMap(errors);
			m_allDevices = RdmDevice.sortByAddr(deviceMap.values());
			m_out.println("Found " + m_allDevices.size() + " RDM Devices.");
			m_selectedDevNums = makeIntList(1, m_allDevices.size());
			prtErrors(errors, m_out);
			startAndWaitForCompletion();
		}
		
		@Override
		public void run()
		{
			String[] argsArr;
			while ((argsArr = readCmd()) != null) {
				List<String> args = array2List(argsArr);
				if (args.isEmpty()) {
					continue;	// blank line
				}
				List<Integer> devNums = parseDevList(args, false);
				if (devNums.isEmpty()) {
					devNums = m_selectedDevNums;
				}
				if (args.isEmpty()) {
					m_out.println("Missing command");
					continue;
				}
				Command cmd = m_cmdFinder.find(args.get(0));
				if (cmd == null) {
					Set<Command> matches = m_cmdFinder.findMatches(args.get(0));
					if (matches.isEmpty()) {
						m_out.println("Unknown command \"" + args.get(0) + "\"");
					} else {
						m_out.println("Ambiquous command \"" + args.get(0) + "\". Could be " + matches);
					}
					continue;
				}
				args.remove(0);
				switch (cmd) {
				case QUIT:
					return;
				case REFRESH:
					m_out.println("Refreshing device list ....");
					List<String> errors = new ArrayList<>();
					try {
						Map<ACN_UID, RdmDevice> deviceMap = m_manager.getDeviceMap(errors);
						m_allDevices = RdmDevice.sortByAddr(deviceMap.values());
					} catch (IOException e) {
						errors.add(e.toString());
					}
					m_selectedDevNums = makeIntList(1, m_allDevices.size());
					m_out.println("Found " + m_allDevices.size() + " RDM devices.");
					prtErrors(errors, m_out);
					break;
				case SORT:
					doSort(args);
					break;
				case PRINT:
					prtDevices(m_allDevices, m_out, devNums);
					break;
				case LIST:
					listDevices(m_allDevices, m_out, devNums);
					break;
				case SELECT:
					m_selectedDevNums = devNums;
					break;
				case ADD:
					for (int devNum: devNums) {
					 	addDevNumber(devNum, m_selectedDevNums);
					}
					break;
				case NODES:
					Set<ArtNetNode> uniqueNodes = m_manager.getUniqueNodes();
					m_out.println(uniqueNodes.size() + " Unique Nodes:");
					String indent = "    ";
					for (ArtNetNode node : uniqueNodes) {
						m_out.println(indent + node.toString().replaceAll("\n", "\n" + indent));
					}
					m_out.println();
					break;
				case NAME:
					doName(devNums, args);
					break;
				case ADDRESS:
					doAddress(devNums, args);
					break;
				case CONFIG:
					doConfig(devNums, args);
					break;
				case IDENTIFY:
					doIdentify(devNums, args);
					break;
				case HELP:
					m_out.println("Commands:");
					for (Command c: Command.values()) {
						m_out.println("  " + c.help());
					}
					break;
				default:
					m_out.println("XXX: Unimplemented command!!");
					break;
				}
			}
		}

		private void doName(List<Integer> devNums, List<String> args)
		{
			if (devNums.isEmpty()) {
				m_out.println("No Devices selected");
			} else if (args.isEmpty()) {
				for (int iDev: devNums) {
					m_out.println(iDev + ": \"" + m_allDevices.get(iDev-1).getDeviceLabel() + "\"");
				}
			} else if (devNums.size() > 1) {
				m_out.println("More than 1 device selected.");
			} else {
				StringBuilder name = new StringBuilder();
				String sep = "";
				for (String v: args) {
					name.append(sep);
					name.append(v);
					sep = " ";
				}
				if (!m_allDevices.get(devNums.get(0)-1).setDeviceLabel(name.toString())) {
					m_out.println("Set device label failed.");
				}
			}
		}

		private void doAddress(List<Integer> devNums, List<String> args)
		{
			if (devNums.isEmpty()) {
				m_out.println("No Devices selected");
			} else if (args.isEmpty()) {
				for (int iDev: devNums) {
					RdmDevice dev = m_allDevices.get(iDev-1);
					int startAddr = dev.getDmxStartAddr();
					m_out.println(iDev + ": " + startAddr
							+ "-" + (startAddr + dev.getDmxFootprint()-1)
							+ " @" + dev.m_nodePort.m_port);
				}
			} else if (devNums.size() > 1) {
				m_out.println("More than 1 device selected.");
			} else {
				RdmDevice dev = m_allDevices.get(devNums.get(0) - 1);
				int newAddr;
				try {
					newAddr = Integer.parseInt(args.get(0));
					if (!(newAddr >= 1 && newAddr + dev.getDmxFootprint()-1 <= 512)) {
						m_out.println("Illegal address " + newAddr);
					} else if (!dev.setDmxAddress(newAddr)) {
						m_out.println("Set dmx address failed.");
					}
				} catch (NumberFormatException e) {
					m_out.println("Illegal address " + args.get(0));
				}
			}
		}
		
		private void doConfig(List<Integer> devNums, List<String>args)
		{
			if (devNums.isEmpty()) {
				m_out.println("No Devices selected");
			} else if (args.isEmpty()) {
				for (int iDev: devNums) {
					RdmDevice dev = m_allDevices.get(iDev-1);
					m_out.println(iDev + ": " + dev.getPersonality() + " " + dev.getPersonalityDesc());
				}
			} else if (devNums.size() > 1) {
				m_out.println("More than 1 device selected.");
			} else {
				RdmDevice dev = m_allDevices.get(devNums.get(0) - 1);
				int newConfig;
				try {
					newConfig = Integer.parseInt(args.get(0));
					if (!(newConfig >= 1 && newConfig <= dev.getNumPersonalities())) {
						m_out.println("Illegal configuration number " + newConfig);
					} else if (!dev.setPersonality(newConfig)) {
						m_out.println("Set configuration failed.");
					}
				} catch (NumberFormatException e) {
					m_out.println("Illegal configuration number " + args.get(0));
				}
			}
		}
		
		private void doIdentify(List<Integer> devNums, List<String> args)
		{
			if (devNums.isEmpty()) {
				m_out.println("No Devices selected");
			} else if (args.isEmpty()) {
				for (int iDev: devNums) {
					m_out.println(iDev + ": " +
								(m_allDevices.get(iDev-1).getIdentifyDevice() ? "on" : "off"));
				}
			} else {
				String arg = args.get(0).trim().toLowerCase();
				boolean on = arg.equals("on") || arg.startsWith("1");
				for (int iDev: devNums) {
					if (!m_allDevices.get(iDev-1).setIdentifyDevice(on)) {
						m_out.println(iDev + ": Set IDENTIFY failed.");
					}
				}
			}
		}
		
		private void doSort(List<String> args)
		{
			if (args.isEmpty()) {
				args.add("make");
			}
			String spec = args.get(0).toLowerCase();
			if (spec.startsWith("ma")) {
				Collections.sort(m_allDevices);
			} else if (spec.startsWith("ui")) {
				Collections.sort(m_allDevices, new RdmDevice.CompareUID());
			} else if (spec.startsWith("no")) {
				Collections.sort(m_allDevices, new RdmDevice.CompareNodePort());
			} else if (spec.startsWith("ad") || spec.startsWith("dm")) {
				Collections.sort(m_allDevices, new RdmDevice.CompareAddress());
			} else {
				m_out.println("Unknown sort order. Try make, uid, node, or addr");
			}
		}
		
		private List<Integer> parseDevList(List<String> args, boolean parseAll)
		{
			List<Integer> devNumList = new ArrayList<>();
			for (Iterator<String> iter = args.iterator(); iter.hasNext(); ) {
				String arg = iter.next();
				try {
					if (m_cmdFinder.find(arg) != null) {
						break;
					} else if (arg.equals("*")) {
						// * => all devices.
						for (int iDev = 1; iDev <= m_allDevices.size(); iDev++) {
							addDevNumber(iDev, devNumList);
						}
					} else if (arg.matches("[0-9]+")) {
						// Device number in list, 1-n.
						int iDev = Integer.parseInt(arg);
						if (iDev >= 1 && iDev <= m_allDevices.size()) {
							addDevNumber(iDev, devNumList);
						} else {
							m_out.println("Invalid device number " + arg);
						}
					} else if (arg.matches("[0-9]+-[0-9]+")) {
						// fromdev-todev
						int[] fromTo = parseFromTo(arg);
						for (int iDev = fromTo[0]; iDev <= fromTo[1]; iDev++) {
							addDevNumber(iDev, devNumList);
						}
					} else if (arg.matches("[0-9a-fA-F]+:\\*") || arg.matches("[0-9a-fA-F]+")) {
						// Vendorcast UID.
						String[] makeModel = arg.split(":", 2);
						ACN_UID uid = ACN_UID.vendorcastUid(Integer.parseInt(makeModel[0], 16));
						for (int iDev = 1; iDev <= m_allDevices.size(); iDev++) {
							if (m_allDevices.get(iDev-1).m_uid.matches(uid)) {
								addDevNumber(iDev, devNumList);
							}
						}
					} else if (arg.matches("[0-9a-fA-F]+:[0-9a-fA-F]+")) {
						// Specific UID.
						ACN_UID uid = new ACN_UID(arg);
						for (int iDev = 1; iDev <= m_allDevices.size(); iDev++) {
							if (m_allDevices.get(iDev-1).m_uid.matches(uid)) {
								addDevNumber(iDev, devNumList);
							}
						}
					} else if (arg.matches(".+=.*")) {
						String[] typeValue = arg.split("=", 2);
						if (typeValue[0].toLowerCase().startsWith("u")) {
							// Univ=##  or ##.##  or ##.##.##
							ArtNetPort anPort = new ArtNetPort(typeValue[1]);
							for (int iDev = 1; iDev <= m_allDevices.size(); iDev++) {
								if (m_allDevices.get(iDev-1).m_nodePort.m_port.equals(anPort)) {
									addDevNumber(iDev, devNumList);
								}
							}
						} else if (typeValue[0].toLowerCase().startsWith("a")
								|| typeValue[0].toLowerCase().startsWith("d")) {
							// addr=low[-high] or dmx=low[-high]
							int[] fromToAddr = parseFromTo(typeValue[1]);
							for (int iDev = 1; iDev <= m_allDevices.size(); iDev++) {
								int startAddr = m_allDevices.get(iDev-1).getDmxStartAddr();
								if (startAddr >= fromToAddr[0] && startAddr <= fromToAddr[1]) {
									addDevNumber(iDev, devNumList);
								}
							}
						} else {
							m_out.println("Unknown device spec \"" + arg + "\"");
						}
					} else if (arg.matches("[^/]+/.*")) {
						// Make/model string
						String[] makeModel = arg.split("/", 2);
						for (int iDev = 1; iDev <= m_allDevices.size(); iDev++) {
							RdmDevice rdmDevice = m_allDevices.get(iDev-1);
							if (makeModel[0].equalsIgnoreCase(rdmDevice .m_manufacturer)
									&& (makeModel[1].isBlank()
											|| rdmDevice .m_model.matches(makeModel[1]))) {
								addDevNumber(iDev, devNumList);
							}
						}
					} else if (!parseAll) {
						m_out.println("Unknown device spec \"" + arg + "\"");
					} else {
						break;
					}
					iter.remove();
				} catch (Exception e) {
					m_out.println("Invalid device spec " + arg);
					iter.remove();
				}
			}
			return devNumList;
		}
	}
}
