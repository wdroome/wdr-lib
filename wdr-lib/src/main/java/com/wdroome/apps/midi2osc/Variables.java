package com.wdroome.apps.midi2osc;

import com.wdroome.json.JSONValue;
import com.wdroome.json.JSONValue_Array;
import com.wdroome.json.JSONValue_Object;
import com.wdroome.osc.OSCUtil;

public class Variables
{
	private final JSONValue_Object m_transforms;
	
	public static final double[] DEF_SCALE = new double[] {0, 127};
	public static final double[] DEF_CLIP = new double[] {0, 127};
	
	public Variables(JSONValue_Object transforms)
	{
		m_transforms = transforms != null ? transforms : new JSONValue_Object();
	}

	/**
	 * Replace variables in an OSC command or argument.
	 * @param str The command or argument.
	 * @param tck The type, key & channel values, if not null.
	 * @param data2 The data2 value, if not negative.
	 * @param data1Map That data1map value, if not negative.
	 * @return An Object corresponding to the string with the substituted values.
	 * 		Normally it returns a string unless the transform says use binary integer or float.
	 */
	public Object replaceVariables(String str, TypeChanKey tck, int data2, int data1Map)
	{
		if (str.indexOf('$') < 0) {
			return str;
		}
		int len = str.length();
		StringBuilder ret = new StringBuilder(len + 10);
		for (int i = 0; i < len; i++) {
			char c = str.charAt(i);
			if (c != '$') {
				ret.append(c);
				continue;
			}
			if (i+1 < len && str.charAt(i+1) != '{') {
				ret.append('$');
				continue;
			}
			int iname = i+2;
			int iend = str.indexOf('}', iname);
			if (iend < 0) {
				iend = len;
			}
			String rawVar = str.substring(i, iend+1);
			String varname = str.substring(iname, iend);
			int icolon = varname.indexOf(':');
			String transformName = null;
			if (icolon > 0) {
				transformName = varname.substring(icolon+1);
				varname = varname.substring(0, icolon);
			}
			int numVal;
			boolean replace = true;
			if (tck != null && varname.equals("chan")) {
				numVal = tck.m_chan;
			} else if (tck != null && (varname.equals("data1") || varname.equals("key"))) {
				numVal = tck.m_key;
			} else if (data2 >= 0 && (varname.equals("data2") || varname.equals("data") || varname.equals(""))) {
				numVal = data2;
			} else if (data1Map >= 0 && varname.equals("data1-map")) {
				numVal = data1Map;
			} else {
				numVal = 0;
				replace = false;
				// System.out.println("XXX: replace = false " + rawVar + " " + varname);
			}
			if (!replace) {
				ret.append(rawVar);
			} else if (transformName == null) {
				ret.append(numVal);
			} else {
				Object transformed = applyTransform(numVal, transformName);
				if (!(transformed instanceof String) && i == 0 && iend+1 >= len) {
					return transformed;
				}
				ret.append(transformed.toString());
			}
			i = iend;
		}
		return ret.toString();
	}

	public Object applyTransform(int value, String transformName)
	{
		JSONValue xtrans = m_transforms != null
							? m_transforms.get(transformName) : null;
		if (xtrans instanceof JSONValue_Array && ((JSONValue_Array)xtrans).size() >= 2) {
			JSONValue_Array transform = (JSONValue_Array)xtrans;
			double min = transform.getNumber(0, 0);
			double max = transform.getNumber(1, 127);
			double scaledValue = ((double)value/127.0) * (max - min) + min;
			if (transform.size() >= 3) {
				String fmt = transform.getString(2);
				if (fmt != null) {
					return String.format(fmt, scaledValue);
				}
			}
			return Float.valueOf((float)scaledValue);
		} else if (xtrans instanceof JSONValue_Object) {
			JSONValue_Object transform = (JSONValue_Object)xtrans;
			double[] scale = Midi2OscUtil.get2Array(transform, Midi2OscUtil.FN_SCALE, DEF_SCALE);
			double scaledValue = ((double)value/127.0) * (scale[1] - scale[0]) + scale[0];
			scaledValue += transform.getNumber(Midi2OscUtil.FN_ADD, 0);
			scaledValue *= transform.getNumber(Midi2OscUtil.FN_MULT, 1);
			double[] clip = Midi2OscUtil.get2Array(transform, Midi2OscUtil.FN_CLIP, DEF_CLIP);
			if (scaledValue < clip[0]) {
				scaledValue = clip[0];
			} else if (scaledValue > clip[1]) {
				scaledValue = clip[1];
			}
			String fmt = transform.getString(Midi2OscUtil.FN_FMT, "%.0f");
			if (fmt.isBlank() || fmt.equals(OSCUtil.OSC_STR_ARG_FMT)) {
				fmt = (scaledValue == (long)scaledValue) ? "%.0f" : "%f";
			}
			if (fmt.startsWith("%")) {
				return String.format(fmt, scaledValue);
			} else if (fmt.equals(OSCUtil.OSC_INT32_ARG_FMT)) {
				return Integer.valueOf((int)scaledValue);
			} else if (fmt.equals(OSCUtil.OSC_FLOAT_ARG_FMT)) {
				return Double.valueOf(scaledValue);
			} else if (fmt.equals(OSCUtil.OSC_INT64_ARG_FMT)) {
				return Long.valueOf((long)scaledValue);
			} else {
				return scaledValue+"";
			}
		} else {
			return value+"";
		}
	}
}
