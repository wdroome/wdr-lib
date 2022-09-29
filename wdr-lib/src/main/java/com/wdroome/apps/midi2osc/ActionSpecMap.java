package com.wdroome.apps.midi2osc;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import javax.sound.midi.ShortMessage;

import java.io.PrintStream;
import java.io.File;
import java.io.FileNotFoundException;

import com.wdroome.json.JSONLexan;
import com.wdroome.json.JSONParseException;
import com.wdroome.json.JSONParser;
import com.wdroome.json.JSONValue;
import com.wdroome.json.JSONValueTypeException;
import com.wdroome.json.JSONValue_String;
import com.wdroome.json.JSONValue_Array;
import com.wdroome.json.JSONValue_Number;
import com.wdroome.json.JSONValue_Object;
import com.wdroome.json.JSONValue_ObjectArray;

public class ActionSpecMap
{
	
	// Action map for named controllers. First key is the controller name.
	// Second key is the encoded TypeChanKey.
	// The list has the ActionSpecs for that controller & TypeChanKey.
	// Pick the one whose data2 value matches the incoming MIDI message.
	private final Map<String, Map<String, List<ActionSpec>>> m_namedControllerMap = new HashMap<>();
	
	// Action map for unspecified controllers. Key is the encoded TypeChanKey.
	// The List is the same as for m_namedControllerMap.
	private final Map<String, List<ActionSpec>> m_unnamedControllerMap = new HashMap<>();
	
	public ActionSpecMap(JSONValue_Array jsonSpecs, ServerConfig serverConfig, Variables variables)
	{
		int iAction = 0;
		for (JSONValue_Object jsonSpec: new JSONValue_ObjectArray(jsonSpecs)) {
			/*
			 * Get midi-controller. May be "", null or omitted.
			 * Get osc-server. If "", null or omitted, use single osc server. Otherwise verify name is legal.
			 * for all items in "actions" object:
			 * 		create ActionSpec
			 * 		add(controller, actionSpec)
			 * if haa data1-map object, for all items
			 * 		osc: save as cmd template
			 * 		#*: ignore
			 * 		other: parse key as TCK. If can't, error.
			 * 				Get data1 & data1-map. For all data1 values,
			 * 				substitute chan, data1 and data1-map, create ActionSpec,
			 * 				and call add(controller, actionSpec).
			 */
			String midiController = jsonSpec.getString(Midi2OscUtil.FN_MIDI_CONTROLLER, null);
			String oscServerName = jsonSpec.getString(Midi2OscUtil.FN_OSC_SERVER, null);
			ServerInfo oscServer = serverConfig.getServerInfo(jsonSpec.getString(Midi2OscUtil.FN_OSC_SERVER, null));
			if (oscServer == null) {
				if (oscServerName != null && !oscServerName.isBlank()) {
					throw new IllegalArgumentException("Undeclared " + Midi2OscUtil.FN_OSC_SERVER + " \""
							+ oscServerName + "\" in " + Midi2OscUtil.FN_ACTIONS
							+ "[" + iAction + "] in config file.");				
				} else {
					throw new IllegalArgumentException("Missing " + Midi2OscUtil.FN_OSC_SERVER + " field in "
							+ Midi2OscUtil.FN_ACTIONS + "[" + iAction + "] in config file.");					
				}
			}
			int nActions = 0;
			JSONValue_Object actions = jsonSpec.getObject(Midi2OscUtil.FN_ACTIONS, null);
			if (actions != null) {
				for (Map.Entry<String, JSONValue> ent: actions.entrySet()) {
					if (ent.getKey().startsWith(Midi2OscUtil.FN_COMMENT_PREFIX)
								|| !(ent.getValue() instanceof JSONValue_Object)) {
						continue;
					}
					JSONValue_Object spec = (JSONValue_Object)ent.getValue();
					TypeChanKey tck = new TypeChanKey(ent.getKey());
					IntList data2List = IntList.LIST_0_127;
					JSONValue data2json = spec.get(Midi2OscUtil.FN_DATA2, null);
					if (data2json != null) {
						try {
							data2List = IntList.makeIntList(data2json);
						} catch (Exception e) {
							throw new IllegalArgumentException("Invalid " + Midi2OscUtil.FN_DATA2
										+ "='" + data2json + "' in " + Midi2OscUtil.FN_ACTIONS
										+ "[" + iAction + "] in config file.");				
						}
					}
					add(midiController, new ActionSpec(oscServer, tck, data2List,
										makeOscCmds(spec, Midi2OscUtil.FN_ACTIONS, iAction),
										(int)spec.getNumber(Midi2OscUtil.FN_NEW_VALUE_WAIT_MS,
												oscServer.m_newValueWaitMS),
										(int)spec.getNumber(Midi2OscUtil.FN_SAME_VALUE_SKIP_MS,
												oscServer.m_sameValueSkipMS)));
					nActions++;
				}
			}
			JSONValue_Object data1Map = jsonSpec.getObject(Midi2OscUtil.FN_DATA1_MAP, null);
			if (data1Map != null) {
				List<JSONValue_Array> oscCmds = makeOscCmds(data1Map, Midi2OscUtil.FN_DATA1_MAP, iAction);
				for (Map.Entry<String,JSONValue> ent: data1Map.entrySet()) {
					String key = ent.getKey();
					if (key.startsWith(Midi2OscUtil.FN_COMMENT_PREFIX)
							|| key.equals(Midi2OscUtil.FN_OSC_CMDS)
							|| key.equals(Midi2OscUtil.FN_OSC_CMD)
							|| !(ent.getValue() instanceof JSONValue_Object)) {
						continue;
					}
					JSONValue_Object spec = (JSONValue_Object)ent.getValue();
					TypeChanKey tckBase = new TypeChanKey(key);
					MapItemData mapItem = getMapItemData(spec, iAction);
					Iterator<Integer> inIter = mapItem.m_data1List.iterator();
					Iterator<Integer> outIter = mapItem.m_data1MapList.iterator();
					while (inIter.hasNext() && outIter.hasNext()) {
						int inVal = inIter.next();
						int outVal = outIter.next();
						List<JSONValue_Array> newOscCmdList = new ArrayList<>();
						TypeChanKey tck = new TypeChanKey(tckBase.m_type, tckBase.m_chan, inVal);
						for (JSONValue_Array oscCmd: oscCmds) {
							JSONValue_Array newOscCmd = new JSONValue_Array();
							newOscCmdList.add(newOscCmd);
							for (JSONValue v: oscCmd) {
								if (v instanceof JSONValue_String) {
									// Just replace ${data1map}.
									Object newValue = variables.replaceVariables(((JSONValue_String)v).m_value,
																		tck, -1, outVal);
									if (newValue instanceof Number) {
										newOscCmd.add(new JSONValue_Number(((Number)newValue).doubleValue()));
									} else {
										newOscCmd.add(new JSONValue_String(newValue.toString()));
									}
								} else {
									newOscCmd.add(v);
								}
							}
						}
						add(midiController, new ActionSpec(oscServer, tck, IntList.LIST_0_127, newOscCmdList,
								(int)spec.getNumber(Midi2OscUtil.FN_NEW_VALUE_WAIT_MS,
										oscServer.m_newValueWaitMS),
								(int)spec.getNumber(Midi2OscUtil.FN_SAME_VALUE_SKIP_MS,
										oscServer.m_sameValueSkipMS)));
						nActions++;
					}
				}
				if (nActions == 0) {
					System.err.println("WARN: No usable actions in " + Midi2OscUtil.FN_ACTIONS
											+ "[" + iAction + "] in config file.");
				}
			}
			List<String> unknown = jsonSpec.findInvalidKeys(
					List.of(Midi2OscUtil.FN_ACTIONS,
							Midi2OscUtil.FN_DATA1_MAP,
							Midi2OscUtil.FN_MIDI_CONTROLLER,
							Midi2OscUtil.FN_OSC_SERVER),
					List.of("^" + Midi2OscUtil.FN_COMMENT_PREFIX + ".*"));
			if (unknown != null) {
				System.err.println("WARNING: Unrecognized fields in \""
								+ Midi2OscUtil.FN_ACTIONS + "\": " + unknown);
			}

			iAction++;
		}
	}
	
	private static List<JSONValue_Array> makeOscCmds(JSONValue_Object container, String containerName, int index)
	{
		List<JSONValue_Array> list = new ArrayList<JSONValue_Array>();
		String fldName = Midi2OscUtil.FN_OSC_CMDS;
		JSONValue_Array oscCmds = container.getArray(fldName, null);
		if (oscCmds == null) {
			fldName = Midi2OscUtil.FN_OSC_CMD;
			oscCmds = container.getArray(fldName, null);
		}
		if (oscCmds == null) {
			throw new IllegalArgumentException("Missing " + Midi2OscUtil.FN_OSC_CMDS + " field in "
					+ containerName + "[" + index + "] in config file.");
		}
		if (oscCmds.size() >= 1 && oscCmds.get(0) instanceof JSONValue_String) {
			// Single command. Make into list of one JSON array.
			list.add(oscCmds);
		} else {
			for (JSONValue jval: oscCmds) {
				if (jval instanceof JSONValue_Array) {
					list.add((JSONValue_Array)jval);
				}
			}
		}
		if (list.isEmpty()) {
			throw new IllegalArgumentException("Invalid " + fldName + " field '"
					+ oscCmds + "' in " + containerName + "[" + index + "] in config file.");			
		}
		// Verify all values in oscCmds are arrays, and the first value in each array is a string.
		for (JSONValue_Array cmds: list) {
			if (cmds.size() <= 0 || !(cmds.get(0) instanceof JSONValue_String)) {
				throw new IllegalArgumentException("Invalid " + fldName + " field '"
						+ oscCmds + "' in " + containerName + "[" + index + "] in config file.");				
			}
		}
		return list;
	}
	
	private static class MapItemData
	{
		private final IntList m_data1List;
		private final IntList m_data1MapList;
		private MapItemData(IntList in, IntList out)
		{
			m_data1List = in;
			m_data1MapList = out;
		}
	}
	
	private MapItemData getMapItemData(JSONValue_Object mapItem, int iAction)
	{
		IntList in;
		IntList out;
		try {
			in = new IntList(mapItem.getString(Midi2OscUtil.FN_DATA1, ""));
			out = new IntList(mapItem.getString(Midi2OscUtil.FN_DATA1_MAP, ""));
		} catch (Exception e) {
			throw new IllegalArgumentException("Incorreclty formatted " + Midi2OscUtil.FN_DATA1 + " or "
								+ Midi2OscUtil.FN_DATA1_MAP + " lists in " + mapItem.toString()
								+ " in " + Midi2OscUtil.FN_DATA1_MAP
								+ " in " + Midi2OscUtil.FN_ACTIONS + "[" + iAction + "]");
		}
		if (in.size() == 0 || out.size() == 0) {
			throw new IllegalArgumentException("Incorreclty formatted " + Midi2OscUtil.FN_DATA1 + " or "
								+ Midi2OscUtil.FN_DATA1_MAP + " lists in " + mapItem.toString()
								+ " in " + Midi2OscUtil.FN_DATA1_MAP
								+ " in " + Midi2OscUtil.FN_ACTIONS + "[" + iAction + "]");
		}
		
		if (in.size() != out.size()) {
			throw new IllegalArgumentException("Different lengths for \"in\" & \"out\" lists in "
							+ mapItem.toString()
							+ " in " + Midi2OscUtil.FN_DATA1_MAP
							+ " in " + Midi2OscUtil.FN_ACTIONS + "[" + iAction + "]");
		}
		return new MapItemData(in, out);
	}
	
	/**
	 * Return a new Action for a Midi message, or null if there is no ActionSpec for this message.
	 * @param controllerName Name of the device sending this message, or null if unknown controller.
	 * @param midiEvent A MIDI message.
	 * @param arriveTS The time when midiEvent arrived.
	 * @return The Action for the Midi message, or null.
	 */
	public Action findAction(String controllerName, ShortMessage midiEvent, long arriveTS)
	{
		if (midiEvent == null) {
			return null;
		}
		MidiEventType type = MidiEventType.typeOf(midiEvent);
		if (type == MidiEventType.OTHER) {
			return null;
		}
		int chan = midiEvent.getChannel();
		int data1 = midiEvent.getData1();
		int data2 = midiEvent.getData2();
		ActionSpec actionSpec = find(controllerName, type, chan, data1, data2);
		if (actionSpec == null) {
			return null;
		}
		return new Action(actionSpec, data2, arriveTS);
	}

	/**
	 * Find the ActionSpec for a MIDI message. This tries several searches.
	 * First, an exact match for the controller name & channel.
	 * Second, the specified controller and any channel.
	 * Third, if controllerName isn't null, a match for the channel and the unspecified controller.
	 * Fourth, if controllerName isn't null, a match for any channel and the unspecified controller.
	 * And if those fail, return null.
	 * 
	 * @param controllerName The MIDI controller name. Normally this is not null or "",
	 * 		because the caller has a MIDI message which should have the controller's name.
	 * 		But since the controller name is not part of the MIDI spec,
	 * 		there may be cases when the caller doesn't have the name.
	 * @param type The MIDI message type.
	 * @param chan The channel on which it arrived. NOTE: Do NOT use ANY_CHAN!
	 * 		The caller has a MIDI message from a controller, so it should have a specific channel.
	 * @param key The data1 from the MIDI message.
	 * @param data2 The data2 from the MIDI message.
	 * @return The ActionSpec which matches the message, or null if none.
	 */
	private ActionSpec find(String controllerName, MidiEventType type, int chan, int key, int data2)
	{
		ActionSpec actionSpec = findExact(controllerName, type, chan, key, data2);
		if (actionSpec != null) {
			return actionSpec;
		}
		actionSpec = findExact(controllerName, type, TypeChanKey.ANY_CHAN, key, data2);
		if (actionSpec != null) {
			return actionSpec;
		}
		if (controllerName != null && !controllerName.isEmpty()) {
			actionSpec = findExact(null, type, chan, key, data2);
			if (actionSpec != null) {
				return actionSpec;
			}
			actionSpec = findExact(null, type, TypeChanKey.ANY_CHAN, key, data2);
			if (actionSpec != null) {
				return actionSpec;
			}
		}
		return null;
	}
	
	/**
	 * Find the ActionSpec which exactly matches a MIDI message.
	 * Unlike find(...), if the exact match fails, do not try any channel or the unspecified controller.
	 * 
	 * @param controllerName The MIDI controller name. May be null or "".
	 * @param type The MIDI message type.
	 * @param chan THe channel on which it arrived. May be ANY_CHAN.
	 * @param key The data1 from the MIDI message.
	 * @param data2 The data2 from the MIDI message.
	 * @return The ActionSpec which matches the message, or null if none.
	 */
	private ActionSpec findExact(String controllerName, MidiEventType type, int chan, int key, int data2)
	{
		Map<String, List<ActionSpec>> byTckMap;
		if (controllerName == null || controllerName.isEmpty()) {
			byTckMap = m_unnamedControllerMap;
		} else {
			byTckMap = m_namedControllerMap.get(controllerName);
		}
		if (byTckMap == null) {
			return null;
		}
		List<ActionSpec> list = byTckMap.get(TypeChanKey.encode(type, chan, key));
		if (list == null) {
			return null;
		}
		for (ActionSpec actionSpec: list) {
			if (actionSpec.matchesData2(data2)) {
				return actionSpec;
			}
		}
		return null;
	}
	
	/**
	 * Add an ActionSpec to the map.
	 * @param controllerName COntroller name. If null or "", use the unnamed-controller map.
	 * @param actionSpec The action spec.
	 */
	private void add(String controllerName, ActionSpec actionSpec)
	{
		Map<String, List<ActionSpec>> byTckMap;
		if (controllerName == null || controllerName.isEmpty()) {
			byTckMap = m_unnamedControllerMap;
		} else {
			byTckMap = m_namedControllerMap.get(controllerName);
			if (byTckMap == null) {
				byTckMap = new HashMap<>();
				m_namedControllerMap.put(controllerName, byTckMap);
			}
		}
		List<ActionSpec> list = byTckMap.get(actionSpec.m_encodedTCK);
		if (list == null) {
			list = new ArrayList<>();
			byTckMap.put(actionSpec.m_encodedTCK, list);
		}
		list.add(actionSpec);
	}
	
	public void printMap(PrintStream out)
	{
		if (out == null) {
			out = System.out;
		}
		for (String controller: sortSet(m_namedControllerMap.keySet())) {
			System.out.println("Actions for MidiController '" + controller + "':");
			Map<String, List<ActionSpec>> specs = m_namedControllerMap.get(controller);
			for (String key: sortSet(specs.keySet())) {
				System.out.println("  " + key + ": " + specs.get(key));
			}
		}
		System.out.println("Actions for unspecified controllers:");
		for (String key: sortSet(m_unnamedControllerMap.keySet())) {
			System.out.println("  " + key + ": " + m_unnamedControllerMap.get(key));
		}
	}
	
	private List<String> sortSet(Set<String> set)
	{
		List<String> list = new ArrayList<>(set);
		Collections.sort(list);
		return list;
	}
	
	public static void main(String[] args)
			throws JSONParseException, JSONValueTypeException, FileNotFoundException
	{
		JSONValue_Object config = JSONParser.parseObject(new JSONLexan(new File(args[0])), true);
		Parameters params = new Parameters(config.getObject(Midi2OscUtil.FN_PARAMS, null), null);
		ServerConfig serverConfig = new ServerConfig(config.getArray(Midi2OscUtil.FN_OSC_SERVERS, null), params);
		Variables variables = new Variables(config.getObject(Midi2OscUtil.FN_TRANSFORMS, null));
		JSONValue_Array actionSpecs = config.getArray(Midi2OscUtil.FN_ACTIONS, null);
		ActionSpecMap map = new ActionSpecMap(actionSpecs, serverConfig, variables);
		map.printMap(System.out);
	}
}
