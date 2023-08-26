package com.wdroome.artnet.legacy;

import java.util.List;
import java.util.TreeMap;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.Comparator;

import com.wdroome.artnet.msgs.RdmParamId;
import com.wdroome.artnet.msgs.RdmParamResp;
import com.wdroome.artnet.msgs.RdmProductCategories;
import com.wdroome.artnet.*;

/**
 * Standard information about an RDM device in the network.
 * @author wdr
 */
public class RdmDevice implements Comparable<RdmDevice>
{
	public static final String UNKNOWN_DESC = "???";
	
	public final ACN_UID m_uid;
	public final ArtNetUnivAddr m_nodePort;
	public final RdmParamResp.DeviceInfo m_deviceInfo;
	public final String m_manufacturer;
	public final String m_model;
	public final String m_softwareVersionLabel;
	public final TreeMap<Integer, RdmParamResp.PersonalityDesc> m_personalities;
	public final TreeMap<Integer, String> m_slotDescs;
	public final TreeMap<Integer,RdmParamResp.SensorDef> m_sensorDefs;
	public final long m_deviceHours;
	public final List<RdmParamId> m_stdParamIds;
	public final List<Integer> m_otherParamIds;
	
	public RdmDevice(ACN_UID uid,
					ArtNetUnivAddr nodePort,
					RdmParamResp.DeviceInfo deviceInfo,
					TreeMap<Integer, RdmParamResp.PersonalityDesc> personalities,
					TreeMap<Integer, String> slotDescs,
					String manufacturer,
					String model,
					String softwareVersionLabel,
					long deviceHours,
					TreeMap<Integer,RdmParamResp.SensorDef> sensorDefs,
					RdmParamResp.PidList supportedPids)
	{
		m_uid = uid;
		m_nodePort = nodePort;
		m_deviceInfo = deviceInfo;
		m_personalities = personalities != null ? personalities : new TreeMap<>();
		m_slotDescs = slotDescs != null ? slotDescs : new TreeMap<>();
		m_manufacturer = manufacturer;
		m_model = model;
		if (softwareVersionLabel != null) {
			softwareVersionLabel = softwareVersionLabel.trim();
		}
		if (softwareVersionLabel.isBlank()) {
			softwareVersionLabel = "" + m_deviceInfo.m_softwareVersion;
		}
		m_softwareVersionLabel = softwareVersionLabel;
		m_deviceHours = deviceHours;
		m_sensorDefs = sensorDefs != null ? sensorDefs : new TreeMap<>();
		m_stdParamIds = supportedPids != null ? supportedPids.m_stdPids : List.of();
		m_otherParamIds = supportedPids != null ? supportedPids.m_otherPids : List.of();
	}
	
	@Override
	public String toString()
	{
		return	"RdmDevice(" + m_uid
				+ "@" + m_nodePort
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
				+ (m_deviceHours >= 0 ? "devHrs=" + m_deviceHours : "")
				+ ",pids=" + m_stdParamIds
				+ (!m_otherParamIds.isEmpty() ? (",xpids=" + m_otherParamIds) : "")
				+ (!m_personalities.isEmpty() ? ",personalities=" + m_slotDescs : "")
				+ (!m_slotDescs.isEmpty() ? ",slots=" + m_slotDescs : "")
				+ (!m_sensorDefs.isEmpty() ? ",sensors=" + m_sensorDefs : "")
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
	 * @return
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
		cmp = m_model.compareTo(o.m_manufacturer);
		if (cmp != 0) {
			return cmp;
		}
		cmp = m_nodePort.m_univ.compareTo(o.m_nodePort.m_univ);
		if (cmp != 0) {
			return cmp;
		}
		return Integer.compare(m_deviceInfo.m_startAddr, o.m_deviceInfo.m_startAddr);
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
	 * Compare devices by DMX universe and address.
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
			int cmp = o1.m_nodePort.m_univ.compareTo(o2.m_nodePort.m_univ);
			if (cmp != 0) {
				return cmp;
			}
			return Integer.compare(o1.m_deviceInfo.m_startAddr, o2.m_deviceInfo.m_startAddr);
		}
	}
}
