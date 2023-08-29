package com.wdroome.artnet;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Set;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.Comparator;

import com.wdroome.artnet.msgs.RdmParamId;
import com.wdroome.artnet.msgs.RdmParamResp;
import com.wdroome.artnet.msgs.RdmProductCategories;
import com.wdroome.artnet.msgs.ArtNetMsgUtil;
import com.wdroome.artnet.msgs.RdmPacket;

import com.wdroome.util.MiscUtil;

/**
 * Standard information about an RDM device in the network.
 * THe c'tor uses 
 * @author wdr
 */
public class RdmDevice implements Comparable<RdmDevice>
{
	public static final String UNKNOWN_DESC = "???";
	
	public final ACN_UID m_uid;
	public final ArtNetUnivAddr m_univAddr;
	public final String m_manufacturer;
	public final String m_model;
	public final String m_softwareVersionLabel;
	public final TreeMap<Integer, RdmParamResp.PersonalityDesc> m_personalities;
	public final TreeMap<Integer,RdmParamResp.SensorDef> m_sensorDefs;
	public final List<RdmParamId> m_stdParamIds;
	public final List<Integer> m_otherParamIds;
	
	private final ArtNetRdmRequest m_rdmRequest;

	private RdmParamResp.DeviceInfo m_deviceInfo;
	
	/**
	 * Create an object for an RDM device. Get the device's invariant data, like model,
	 * and save that in final member variables.
	 * @param uid The device's unique ID.
	 * @param univAddr The node and port with this device.
	 * @param rdmRequest An object that can send RDM messages to this
	 * 			device and return responses.
	 * @throws IOException
	 */
	public RdmDevice(ACN_UID uid, ArtNetUnivAddr univAddr, ArtNetRdmRequest rdmRequest)
							throws IOException
	{
		m_rdmRequest = rdmRequest;
		m_rdmRequest.resetTimeoutErrors();
		m_uid = uid;
		m_univAddr = univAddr;
		if (m_univAddr == null) {
			throw new IllegalStateException("RdmDevice c'tor: " + m_uid + " no portaddr");
		}
		refreshDeviceInfo();
		
		RdmPacket supportedPidsReply = sendRdmRequest(false, RdmParamId.SUPPORTED_PARAMETERS, null);
		RdmParamResp.PidList supportedPids = supportedPidsReply != null
						? new RdmParamResp.PidList(supportedPidsReply) : null;
		m_stdParamIds = supportedPids != null ? supportedPids.m_stdPids : List.of();
		m_otherParamIds = supportedPids != null ? supportedPids.m_otherPids : List.of();

		String manufacturer = String.format("0x%04x", uid.getManufacturer());
		if (isParamSupported(RdmParamId.MANUFACTURER_LABEL)) {
			RdmPacket manufacturerReply = sendRdmRequest(false, RdmParamId.MANUFACTURER_LABEL, null);
			if (manufacturerReply != null && manufacturerReply.isRespAck()) {
				manufacturer = new RdmParamResp.StringReply(manufacturerReply).m_string;
			} 
		}
		m_manufacturer = manufacturer;
		
		String model = String.format("0x%04x", m_deviceInfo.m_model);
		if (isParamSupported(RdmParamId.DEVICE_MODEL_DESCRIPTION)) {
			RdmPacket modelReply = sendRdmRequest(false, RdmParamId.DEVICE_MODEL_DESCRIPTION, null);
			if (modelReply != null && modelReply.isRespAck()) {
				model = new RdmParamResp.StringReply(modelReply).m_string;
			} 
		}
		m_model = model;

		RdmPacket swVerLabelReply = sendRdmRequest(false, RdmParamId.SOFTWARE_VERSION_LABEL, null);
		String swVerLabel = "[" + m_deviceInfo.m_softwareVersion + "]";
		if (swVerLabelReply != null) {
			swVerLabel = new RdmParamResp.StringReply(swVerLabelReply).m_string;
		}
		m_softwareVersionLabel = swVerLabel;
		
		m_personalities = getPersonalities();
		m_sensorDefs = getSensorDefs();
		
		List<ArtNetRdmRequest.TimeoutError> errors
							= rdmRequest.getTimeoutErrors();
		if (!errors.isEmpty()) {
			/*XXX*/
			System.out.println(errors.size() + " RDM timeout errors: " + errors);
		}
	}
	
	private RdmPacket sendRdmRequest(boolean isSet, RdmParamId paramId, byte[] reqData)
			throws IOException
	{
		// This delay was for investigating a bug in the Netron EN4 interface.
		// MiscUtil.sleep(150); // System.out.println("XXX sleep " + paramId);
		return m_rdmRequest.sendRequest(m_univAddr, m_uid, isSet, paramId, reqData);
	}
	
	private boolean isParamSupported(RdmParamId paramId)
	{
		return paramId.isRequired() || m_stdParamIds.contains(paramId);
	}
	
	private void refreshDeviceInfo() throws IOException
	{
		RdmPacket devInfoReply = sendRdmRequest(false, RdmParamId.DEVICE_INFO, null);
		if (devInfoReply == null || !devInfoReply.isRespAck()) {
			throw new IOException("RdmDevice(" + m_uid + "): get DEVICE_INFO failed");
		} else {
			m_deviceInfo = new RdmParamResp.DeviceInfo(devInfoReply);
		}
	}
	
	private TreeMap<Integer,RdmParamResp.PersonalityDesc> getPersonalities() throws IOException
	{
		TreeMap<Integer,RdmParamResp.PersonalityDesc> personalities = new TreeMap<>();
		boolean ok = isParamSupported(RdmParamId.DMX_PERSONALITY_DESCRIPTION);
		if (ok) {
			for (int iPersonality = 1; iPersonality <= m_deviceInfo.m_nPersonalities; iPersonality++) {
				RdmPacket personalityDescReply = null;
				if (ok) {
					personalityDescReply = sendRdmRequest(false, RdmParamId.DMX_PERSONALITY_DESCRIPTION,
														new byte[] {(byte)iPersonality});
					if (personalityDescReply == null) {
						ok = false;
					}
				}
				RdmParamResp.PersonalityDesc desc;
				if (personalityDescReply != null && personalityDescReply.isRespAck()) {
					desc = new RdmParamResp.PersonalityDesc(personalityDescReply);
				} else {
					desc = new RdmParamResp.PersonalityDesc(iPersonality, 1, RdmDevice.UNKNOWN_DESC);
				}
				personalities.put(iPersonality, desc);
			} 
		}
		return personalities;
	}
	
	private TreeMap<Integer,RdmParamResp.SensorDef> getSensorDefs() throws IOException
	{
		TreeMap<Integer,RdmParamResp.SensorDef> sensorDefs = new TreeMap<>();
		boolean ok = isParamSupported(RdmParamId.SENSOR_DEFINITION);
		if (ok) {
			for (int iSensor = 0; iSensor < m_deviceInfo.m_numSensors; iSensor++) {
				RdmPacket sensorDefReply = null;
				if (ok) {
					sensorDefReply = sendRdmRequest(false, RdmParamId.SENSOR_DEFINITION,
														new byte[] { (byte) iSensor });
				}
				RdmParamResp.SensorDef defn = null;
				if (sensorDefReply != null && sensorDefReply.isRespAck()) {
					defn = new RdmParamResp.SensorDef(sensorDefReply);
					sensorDefs.put(iSensor, defn);
				} else {
					ok = false;
				}
			}
		}
		return sensorDefs;
	}
	
	/**
	 * Get the string descriptions of the device's channels ("slots") for the current personality.
	 * This sends SLOT_DESCRIPTION requests to the device.
	 * @return A map from slot number to description. Slot numbers start with 0.
	 * 		Returns an empty map if the device does not support SLOT_DESCRIPTION requests.
	 * @throws IOException
	 */
	public TreeMap<Integer,String> getSlotDescs() throws IOException
	{
		TreeMap<Integer,String> slotDescs = new TreeMap<>();
		boolean ok = isParamSupported(RdmParamId.SLOT_DESCRIPTION);
		if (ok) {
			for (int iSlot = 0; iSlot < m_deviceInfo.m_dmxFootprint; iSlot++) {
				slotDescs.put(iSlot, RdmDevice.UNKNOWN_DESC);
			}
			for (int iSlot = 0; iSlot < m_deviceInfo.m_dmxFootprint; iSlot++) {
				if (ok) {
					byte[] iSlotAsBytes = new byte[] {(byte)((iSlot >> 8) & 0xff), (byte)(iSlot & 0xff)};
					RdmPacket slotDescReply = sendRdmRequest(false,
													RdmParamId.SLOT_DESCRIPTION, iSlotAsBytes);
					if (slotDescReply != null && slotDescReply.isRespAck()) {
						RdmParamResp.SlotDesc slotDesc = new RdmParamResp.SlotDesc(slotDescReply);
						slotDescs.put(slotDesc.m_number, slotDesc.m_desc);
					} else {
						ok = false;
					}
				}
			} 
		}
		return slotDescs;
	}
	
	/**
	 * Refresh the "DEV_IFNO" information for the device.
	 * This includes DMX address and footprint, personality, etc.
	 * @throws IOException If an IO error occurs when sending the request.
	 */
	public void refresh() throws IOException
	{
		refreshDeviceInfo();
	}

	/**
	 * Return the cached RDM device information (DEV_INFO).
	 * @see #refresh()
	 * @return The cached RDM device information (DEV_INFO).
	 */
	public RdmParamResp.DeviceInfo getDeviceInfo()
	{
		return m_deviceInfo;
	}
	
	/**
	 * Return the cached DMX start address.
	 * @see #refresh()
	 * @return The cached DMX start address.
	 */
	public int getDmxStartAddr()
	{
		return m_deviceInfo.m_startAddr;
	}
	
	/**
	 * Return the cached DMX footprint (number of DMX channels).
	 * @see #refresh()
	 * @return The cached DMX footprint (number of DMX channels).
	 */
	public int getDmxFootprint()
	{
		return m_deviceInfo.m_dmxFootprint;
	}
	
	/**
	 * Test if a string name or description is "unknown" -- that is, null or UNKNOWN_DESC
	 * @param name The name or description.
	 * @return True iff name is the placeholder for "unknown".
	 */
	public boolean isUnknownDesc(String name)
	{
		return name == null || name.isEmpty() || name.equals(UNKNOWN_DESC);
	}
	
	/**
	 * Get a comma-separated list of the non-required Parameter Ids this device supports.
	 * @return A comma-separated list of the non-required Parameter Ids this device supports.
	 */
	public String supportedPids()
	{
		StringBuilder buff = new StringBuilder();
		String sep = "";
		for (RdmParamId pid: m_stdParamIds) {
			buff.append(sep + pid);
			sep = ",";
		}
		for (Integer code: m_otherParamIds) {
			buff.append(sep + "0x" + Integer.toHexString(code));
			sep = ",";
		}
		buff.append("]");
		return buff.toString();
	}
	
	/**
	 * Get the cached personality number.
	 * @see #refresh()
	 * @return The current personality number.
	 */
	public int getPersonality()
	{
		return m_deviceInfo.m_currentPersonality;
	}
	
	/**
	 * Get the cached number of personalities.
	 * @see #refresh()
	 * @return The number of personalities.
	 */
	public int getNumPersonalities()
	{
		return m_deviceInfo.m_nPersonalities;
	}
	
	/**
	 * Get a description the current personality.
	 * @return A description the current personality.
	 */
	public String getPersonalityDesc()
	{
		return getPersonalityDesc(m_deviceInfo.m_currentPersonality, m_deviceInfo.m_dmxFootprint);
	}
	
	/**
	 * Get a description a personality.
	 * @param iPers The desired personality number (staring with 1).
	 * @return A description that personality.
	 */
	public String getPersonalityDesc(int iPers)
	{
		return getPersonalityDesc(iPers, -1);
	}
	
	private String getPersonalityDesc(int iPers, int nSlots)
	{
		StringBuilder buff = new StringBuilder();
		buff.append(iPers + ": ");
		String desc = null;
		if (m_personalities != null) {
			RdmParamResp.PersonalityDesc fullDesc = m_personalities.get(iPers);
			if (fullDesc != null) {
				desc = fullDesc.m_desc;
				nSlots = fullDesc.m_nSlots;
			}
		}
		if (desc == null || desc.isEmpty()) {
			if (nSlots > 0) {
				buff.append(" " + nSlots + " Chan");
			} else {
				buff.append(" " + UNKNOWN_DESC);
			}
		} else {
			desc = desc.replaceFirst("^" + iPers + "[:/ ] *", "");
			if (nSlots > 0 && !desc.matches("^" + nSlots + "[ ]*[Cc][Hh].*")) {
				desc = nSlots + "ch " + desc;
			}
			buff.append(desc);
		}
		return buff.toString();
	}
	
	/**
	 * Get the name of the device category, or UNKNOWN if we can't tell.
	 * @return The name of the device category.
	 */
	public String getCategoryName()
	{
		return RdmProductCategories.getCategoryName(m_deviceInfo.m_category);
	}
	
	/**
	 * Get the value of the DEVICE_HOURS parameter.
	 * @return The value of the DEVICE_HOURS parameter, or -1 if not supported or an error occurs.
	 * @throws IOException If an I/O error occurs.
	 */
	public long getDeviceHours()
	{
		long devHours = -1;
		if (isParamSupported(RdmParamId.DEVICE_HOURS)) {
			try {
				RdmPacket devHoursReply = sendRdmRequest(false, RdmParamId.DEVICE_HOURS, null);
				if (devHoursReply != null && devHoursReply.isRespAck()) {
					devHours = RdmParamResp.unknownEnd32Int(devHoursReply, devHours);
				}
			} catch (Exception e) {
				devHours = -1;
			}		
		}
		return devHours;
	}
	
	/**
	 * Get the value of the DEVICE_LABEL parameter.
	 * @return The value of the DEVICE_LABEL parameter, or null if an error occurs.
	 */
	public String getDeviceLabel()
	{
		String label = null;
		if (isParamSupported(RdmParamId.DEVICE_LABEL)) {
			try {
				RdmPacket devLabelReply = sendRdmRequest(false, RdmParamId.DEVICE_LABEL, null);
				if (devLabelReply != null && devLabelReply.isRespAck()) {
					label = new RdmParamResp.StringReply(devLabelReply).m_string;
				}
			} catch (Exception e) {
				label = null;
			}		
		}
		return label;
	}
	
	/**
	 * Set the DEVICE_LABEL parameter.
	 * @param label The new label.
	 * @return True iff the label was set.
	 */
	public boolean setDeviceLabel(String label)
	{
		boolean succeeded = false;
		if (label == null) {
			label = "";
		}
		if (isParamSupported(RdmParamId.DEVICE_LABEL)) {
			try {
				RdmPacket devLabelReply = sendRdmRequest(true, RdmParamId.DEVICE_LABEL, label.getBytes());
				if (devLabelReply != null && devLabelReply.isRespAck()) {
					succeeded = true;
				}
			} catch (Exception e) {
				succeeded = false;
			}		
		}
		return succeeded;
	}
	
	/**
	 * Get the value of a sensor.
	 * @param iSensor The sensor number.
	 * @return The sensor's value, or null if iSensor isn't a supported sensor
	 * 			or if an error occurs.
	 */
	public RdmParamResp.SensorValue getSensorValue(int iSensor)
	{
		RdmParamResp.SensorValue value = null;
		String sensorName = "";
		RdmParamResp.SensorDef sensorDef = m_sensorDefs.get(iSensor);
		if (sensorDef != null) {
			sensorName = sensorDef.m_desc;
		}
		if (isParamSupported(RdmParamId.SENSOR_VALUE)) {
			try {
				RdmPacket sensorValueReply = sendRdmRequest(false, RdmParamId.SENSOR_VALUE,
												new byte[] {(byte)iSensor});
				if (sensorValueReply != null && sensorValueReply.isRespAck()) {
					value = new RdmParamResp.SensorValue(sensorValueReply, sensorName);
				}
			} catch (Exception e) {
				value = null;
			}		
		}
		return value;
	}
	
	/**
	 * Set the DMX address.
	 * @param dmxAddress The new address.
	 * @return True iff the address was set.
	 */
	public boolean setDmxAddress(int dmxAddress)
	{
		boolean succeeded = false;
		byte[] reqData = new byte[2];
		ArtNetMsgUtil.putBigEndInt16(reqData, 0, dmxAddress);
		if (dmxAddress >= 0 && dmxAddress <= 512) {
			try {
				RdmPacket setDmxReply = sendRdmRequest(true, RdmParamId.DMX_START_ADDRESS,
										reqData);
				if (setDmxReply != null && setDmxReply.isRespAck()) {
					succeeded = true;
				}
			} catch (Exception e) {
				succeeded = false;
			}
			try {
				refresh();
			} catch (IOException e) {
				System.err.println("RdmDevice.setDmxAddress: set succeeded, but refresh failed: " + e);
			}
		}
		return succeeded;
	}
	
	/**
	 * Set the DMX personality.
	 * @param dmxPersonality The new personality number.
	 * @return True iff the personality was set.
	 */
	public boolean setPersonality(int dmxPersonality)
	{
		boolean succeeded = false;
		byte[] reqData = new byte[] {(byte)dmxPersonality};
		try {
			RdmPacket setPersonalityReply = sendRdmRequest(true, RdmParamId.DMX_PERSONALITY, reqData);
			if (setPersonalityReply != null && setPersonalityReply.isRespAck()) {
				succeeded = true;
			}
		} catch (Exception e) {
			succeeded = false;
		}
		try {
			try {
				Thread.sleep(100);
			} catch (Exception e) {
				// ignore
			}
			refresh();
		} catch (IOException e) {
			System.err.println("RdmDevice.setPersonality: set succeeded, but refresh failed: " + e);
		}
		return succeeded;
	}
	
	/**
	 * Get the value of the IDENTIFY_DEVICE parameter.
	 * @return True if the IDENTIFY_DEVICE parameter is ON.
	 * @throws IOException If an I/O error occurs.
	 */
	public boolean getIdentifyDevice()
	{
		boolean on = false;
		if (isParamSupported(RdmParamId.IDENTIFY_DEVICE)) {
			try {
				RdmPacket identifyReply = sendRdmRequest(false, RdmParamId.IDENTIFY_DEVICE, null);
				if (identifyReply != null && identifyReply.isRespAck()
						&& identifyReply.m_paramDataLen >= 1 && identifyReply.m_paramData != null) {
					on = identifyReply.m_paramData[0] != 0;
				}
			} catch (Exception e) {
				on = false;
			}		
		}
		return on;
	}
	
	/**
	 * Set the IDENTIFY_DEVICE parameter.
	 * @param on The new state: true for on, false for off..
	 * @return True iff the parameter was set.
	 */
	public boolean setIdentifyDevice(boolean on)
	{
		boolean succeeded = false;
		byte[] reqData = new byte[] {(byte)(on ? 1 : 0)};
		try {
			RdmPacket setIdentifyReply = sendRdmRequest(true, RdmParamId.IDENTIFY_DEVICE, reqData);
			if (setIdentifyReply != null && setIdentifyReply.isRespAck()) {
				succeeded = true;
			}
		} catch (Exception e) {
			succeeded = false;
		}
		return succeeded;
	}
	
	/**
	 * Broadcast a "Set IDENTIFY_DEVICE off" request to the wildcard UID for all ArtNet ports.
	 * @param manager The ArtNet manager (has the mechanism to send the request).
	 * @return True if all broadcasts succeeded.
	 */
	public static boolean resetIdentifyDevice(ArtNetManager manager)
	{
		boolean succeeded = false;
		byte[] reqData = new byte[] {0};
		if (manager != null) {
			try {
				succeeded = manager.getRdmRequest().bcastRequest(manager.getAllPorts(),
									ACN_UID.BROADCAST_UID, true, RdmParamId.IDENTIFY_DEVICE, reqData);
			} catch (IOException e) {
			}
		}
		return succeeded;
	}

	/**
	 * Compare on Manufacturer, Model, DMX Universe (ArtNet Port) and DMX Start Address.
	 */
	@Override
	public int compareTo(RdmDevice o)
	{
		if (o == null) {
			return 1;
		}
		int cmp = m_manufacturer.compareTo(o.m_manufacturer);
		if (cmp != 0) {
			return cmp;
		}
		cmp = m_model.compareTo(o.m_model);
		if (cmp != 0) {
			return cmp;
		}
		cmp = m_univAddr.m_univ.compareTo(o.m_univAddr.m_univ);
		if (cmp != 0) {
			return cmp;
		}
		return Integer.compare(m_deviceInfo.m_startAddr, o.m_deviceInfo.m_startAddr);
	}

	@Override
	public int hashCode() {
		return ((m_uid == null) ? 0 : m_uid.hashCode());
	}

	/**
	 * RdmDevices are equal iff they have the same UID.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RdmDevice other = (RdmDevice) obj;
		if (m_uid == null) {
			if (other.m_uid != null)
				return false;
		} else if (!m_uid.equals(other.m_uid))
			return false;
		return true;
	}

	/**
	 * Sort a collection of devices on Manufacturer, Model, DMX Port and DMX Address.
	 * @param devices The devices to be sorted. This collection is not changed.
	 * @return The devices in the collection, sorted by Manufacturer, Model, DMX Port and DMX Address.
	 */
	public static List<RdmDevice> sort(Collection<RdmDevice> devices)
	{
		List<RdmDevice> list = new ArrayList<>(devices);
		Collections.sort(list);
		return list;
	}

	/**
	 * Sort a collection of devices on DMX Port and DMX Address.
	 * @param devices The devices to be sorted. This collection is not changed.
	 * @return The devices in the set, sorted by DMX Port and DMX Address.
	 */
	public static List<RdmDevice> sortByAddr(Collection<RdmDevice> devices)
	{
		List<RdmDevice> list = new ArrayList<>(devices);
		Collections.sort(list, new CompareAddress());
		return list;
	}
	
	/**
	 * Compare devices by DMX universe, dmx start address, and finally UID.
	 */
	public static class CompareAddress implements Comparator<RdmDevice>
	{
		@Override
		public int compare(RdmDevice o1, RdmDevice o2)
		{
			if (o1 == null && o2 == null) {
				return 0;
			}
			if (o1 == null) {
				return -1;
			}
			if (o2 == null) {
				return 1;
			}
			int cmp = o1.m_univAddr.m_univ.compareTo(o2.m_univAddr.m_univ);
			if (cmp != 0) {
				return cmp;
			}
			cmp = Integer.compare(o1.getDmxStartAddr(), o2.getDmxStartAddr());
			if (cmp != 0) {
				return cmp;
			}
			return o1.m_uid.compareTo(o2.m_uid);
		}
	}
	
	/**
	 * Compare devices by UID.
	 */
	public static class CompareUID implements Comparator<RdmDevice>
	{
		@Override
		public int compare(RdmDevice o1, RdmDevice o2)
		{
			if (o1 == null && o2 == null) {
				return 0;
			}
			if (o1 == null) {
				return -1;
			}
			if (o2 == null) {
				return 1;
			}
			return o1.m_uid.compareTo(o2.m_uid);
		}
	}
	
	/**
	 * Compare devices by ArtNet Node and Port.
	 */
	public static class CompareNodePort implements Comparator<RdmDevice>
	{
		@Override
		public int compare(RdmDevice o1, RdmDevice o2)
		{
			if (o1 == null && o2 == null) {
				return 0;
			}
			if (o1 == null) {
				return -1;
			}
			if (o2 == null) {
				return 1;
			}
			return o1.m_univAddr.compareTo(o2.m_univAddr);
		}
	}
	
	@Override
	public String toString()
	{
		return	"RdmDevice(" + m_uid
				+ "@" + m_univAddr
				+ getManModel(",")
				+ ",dmx=" + m_deviceInfo.m_startAddr + "-"
						+ (m_deviceInfo.m_startAddr + m_deviceInfo.m_dmxFootprint - 1)
				+ ",config=" + m_deviceInfo.m_currentPersonality + "/"
						+ m_deviceInfo.m_nPersonalities
				+ ",cat=0x" + String.format("%04x", m_deviceInfo.m_category)
						+ "/" + RdmProductCategories.getCategoryName(m_deviceInfo.m_category)
				+ ",model=" + m_deviceInfo.m_model
				+ ",swVers=" + m_softwareVersionLabel
				+ (m_deviceInfo.m_numSubDevs > 0 ? (",#sub=" + m_deviceInfo.m_numSubDevs) : "")
				+ (m_deviceInfo.m_numSensors > 0 ? (",#sensor=" + m_deviceInfo.m_numSensors) : "")
				+ ",pids=" + m_stdParamIds
				+ (!m_otherParamIds.isEmpty() ? (",xpids=" + m_otherParamIds) : "")
				+ (!m_personalities.isEmpty() ? ",personalities=" + m_personalities : "")
				+ (!m_sensorDefs.isEmpty() ? ",sensors=" + m_sensorDefs : "")
				// + (!m_slotDescs.isEmpty() ? ",slots=" + m_slotDescs : "")
				+ ")";
	}
	
	private String getManModel(String prefix)
	{
		if (!isUnknownDesc(m_manufacturer) && !isUnknownDesc(m_model)) {
			return prefix + m_manufacturer + "/" + m_model;
		} else if (!isUnknownDesc(m_manufacturer) && isUnknownDesc(m_model)) {
			return prefix + m_manufacturer + "/" + UNKNOWN_DESC;
		} else if (isUnknownDesc(m_manufacturer) && !isUnknownDesc(m_model)) {
			return prefix + UNKNOWN_DESC + "/" + m_model;
		} else {
			return "";
		}
	}
}
